#!/usr/bin/env python3
"""Fail-closed cross-component integration gate for HazRob 0.10.1 HR-0101-048.

The baseline 0.10.1 integration contract is executed unchanged for every contract except the one
intentional HR048 delta: logical drive-side association. That legacy pin expectation is normalized
in-memory only while the baseline audit runs. The real HR048 mapping is then checked independently
against firmware, Android profile, joystick/Direct/Remote behavior and the controlled documentation.

This avoids weakening the established integration gate while making the field-remediation delta
explicit and auditable. It does not claim physical acceptance.
"""
from pathlib import Path
import importlib.util
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_AUDIT = ROOT / "tools/audit_0101_integration_contract.py"


def text(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")


def require(src: str, needle: str, label: str) -> None:
    if needle not in src:
        raise AssertionError(f"{label}: missing {needle!r}")


def forbid(src: str, needle: str, label: str) -> None:
    if needle in src:
        raise AssertionError(f"{label}: forbidden {needle!r}")


def run_baseline_with_intentional_drive_delta() -> None:
    spec = importlib.util.spec_from_file_location("hazrob_0101_baseline_audit", BASELINE_AUDIT)
    if spec is None or spec.loader is None:
        raise AssertionError("cannot load baseline integration audit")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    original_text = module.text

    new_left = 'DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32'
    new_right = 'DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27'
    legacy_left = 'DRIVE_L_EN=16,DRIVE_L_IN1=18,DRIVE_L_IN2=27'
    legacy_right = 'DRIVE_R_EN=17,DRIVE_R_IN1=19,DRIVE_R_IN2=32'

    actual_cfg = original_text("firmware/HazRob/Config.h")
    if actual_cfg.count(new_left) != 1 or actual_cfg.count(new_right) != 1:
        raise AssertionError("HR048 drive map is not uniquely present before baseline compatibility audit")
    forbid(actual_cfg, legacy_left, "actual HR048 Config left mapping")
    forbid(actual_cfg, legacy_right, "actual HR048 Config right mapping")

    def compatible_text(path: str) -> str:
        src = original_text(path)
        if path == "firmware/HazRob/Config.h":
            src = src.replace(new_left, legacy_left).replace(new_right, legacy_right)
        return src

    module.text = compatible_text
    rc = module.main()
    if rc not in (None, 0):
        raise AssertionError(f"baseline integration audit returned {rc}")


def main() -> int:
    run_baseline_with_intentional_drive_delta()

    cfg = text("firmware/HazRob/Config.h")
    profile = text("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")
    joy = text("app/src/main/java/com/servoc/app/control/JoystickView.java")
    api = text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote = text("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    warning = text("app/src/main/java/com/servoc/app/measurement/InstantMqWarningEngine.java")
    alerts = text("firmware/HazRob/Alerts.cpp")
    remote_link = text("firmware/HazRob/RemoteLink.cpp")
    remote_ui = text("app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java")
    analysis_ui = text("app/src/main/java/com/servoc/app/ui/AnalyticalLabTab.java")
    bootstrap = text("app/src/main/java/com/servoc/app/exposure/GasFlammabilitySeedBootstrap.java")
    doc = text("docs/0.10.1/FIELD_INTEGRATION_REMEDIATION_HR048.md")
    gpio_doc = text("docs/0.10.1/GPIO_AUTHORITY.md")

    # Release identity remains 0.10.1. HR048 is a controlled change request, not 0.10.2.
    require(cfg, 'FW[]="HazRob 0.10.1"', "firmware release identity")
    forbid(cfg, "0.10.2", "firmware release identity")
    forbid(profile, "0.10.2", "Android hardware profile")
    require(doc, "no version bump", "HR048 documentation")

    # Intentional steering remediation: canonical mixer + one hardware-boundary swap only.
    require(joy, "double l = y + x, r = y - x;", "canonical joystick mixer")
    require(cfg, "DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32", "firmware logical left")
    require(cfg, "DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27", "firmware logical right")
    require(cfg, "DRIVE_L_INVERT=false,DRIVE_R_INVERT=false", "drive inversion contract")
    require(profile, "DRIVE_L_EN=17", "Android logical left")
    require(profile, "DRIVE_R_EN=16", "Android logical right")
    require(gpio_doc, "LEFT → channel B", "GPIO authority logical left")
    require(gpio_doc, "RIGHT → channel A", "GPIO authority logical right")
    require(remote, 'p.put("left",left);p.put("right",right);command("drive",p,700,false,0);', "Remote left/right parity")

    # Idle Android Direct transport must not continuously overwrite independent diagnostic commands.
    require(api, "if(left==0&&right==0&&lastDirectDriveLeft==0&&lastDirectDriveRight==0)return;", "Direct idle-zero ownership")
    require(api, "Non-zero commands still refresh at 20 Hz", "Direct ownership documentation")

    # MQ operational warning remains separate from quantitative M4 claims.
    require(warning, "R0_NORMALIZED_FALLBACK = 1.0", "MQ fallback baseline")
    require(warning, "ELEVATED_DELTA = 0.20", "MQ elevated threshold")
    require(warning, "HIGH_DELTA = 0.40", "MQ high threshold")
    require(warning, "usedFallbackBaseline", "MQ warning provenance")

    # Alert GPIO is fail-closed electrically until a safe driver is explicitly declared installed.
    require(cfg, "ALERT_OUTPUT_INSTALLED=false", "alert hardware gate")
    require(alerts, "Cfg::ALERT_OUTPUT_INSTALLED&&activeLevel!=NONE", "alert output gating")
    require(alerts, "output_asserted", "alert telemetry assertion state")

    # Remote diagnostic UI and firmware must expose the same ordered readiness stages.
    for key in ("configured", "wifi", "ip_ok", "dns", "time", "internet", "tls", "mqtt", "command_ack", "telemetry", "remote_ready", "detail"):
        require(remote_link, f'\\"{key}\\"', f"firmware remote status key {key}")
    for key in ("configured", "wifi", "ip_ok", "dns", "time", "internet", "tls", "mqtt", "command_ack", "telemetry", "remote_ready", "detail"):
        require(remote_ui, f'\"{key}\"', f"Android remote diagnostic key {key}")

    # Screening remains visible while reportable PPM remains M4-gated.
    require(analysis_ui, "Screening", "analysis screening label")
    require(analysis_ui, "Quantitative PPM", "analysis quantitative label")
    require(analysis_ui, "WITHHELD", "analysis withheld state")

    # Flammability seed retirement is explicitly blocked until the pinned bundle exists offline.
    require(bootstrap, 'ASSET="generated/flammability/niosh_npg_flammability.json"', "flammability bundled asset contract")
    require(bootstrap, 'PINNED_SHA256="457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434"', "flammability pinned hash")
    require(doc, "primary `hazrob-0101-flammability-seed` service must not be deleted yet", "flammability retirement gate")

    print("HazRob 0.10.1 HR-0101-048 cross-component integration contract: PASS")
    print("Baseline integration coverage preserved; intentional drive-map delta verified separately.")
    print("Physical field acceptance: PENDING")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print("HazRob 0.10.1 HR-0101-048 integration contract: FAIL:", exc)
        raise SystemExit(1)
