#!/usr/bin/env python3
"""Fail-closed cross-component regression gate for HazRob 0.10.1 HR048/060 carried into HR066.

This retained historical gate verifies that accepted HR048 drive, gas-warning, alert and diagnostic
semantics remain compatible with the current HR066 Direct-first / CAM STA-only / developer-authorized
Remote contract. It must track the current implementation instead of freezing superseded HR065 labels.
Physical wheel/RF acceptance remains separate.
"""
from pathlib import Path
import importlib.util

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


def run_hr065_baseline() -> None:
    spec = importlib.util.spec_from_file_location("hazrob_0101_hr065_audit", BASELINE_AUDIT)
    if spec is None or spec.loader is None:
        raise AssertionError("cannot load HR065 integration audit")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    rc = module.main()
    if rc not in (None, 0):
        raise AssertionError(f"HR065 integration audit returned {rc}")


def main() -> int:
    run_hr065_baseline()

    cfg = text("firmware/HazRob/Config.h")
    profile = text("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")
    joy = text("app/src/main/java/com/servoc/app/control/JoystickView.java")
    mixer = text("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    mixer_test = text("app/src/test/java/com/servoc/app/control/DriveMixerTest.java")
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

    # Product version remains 0.10.1; HR066 is a controlled change set, not a 0.10.2 bump.
    require(cfg, 'FW[]="HazRob 0.10.1"', "firmware release identity")
    require(cfg, 'SSID[]="HazRob"', "Main AP SSID authority")
    require(cfg, 'CAM[]="192.168.4.2"', "camera canonical host")
    forbid(cfg, "0.10.2", "firmware release identity")
    forbid(profile, "0.10.2", "Android hardware profile")
    require(doc, "no version bump", "HR048 documentation")

    # Canonical differential mixer + the controlled HR048 hardware-boundary side map.
    require(joy, "DriveMixer.Command command=DriveMixer.mix(x,y);", "joystick pure mixer binding")
    require(mixer, "double steer=y<0?-x:x;", "travel-relative steering equation")
    require(mixer, "double left=y+steer;", "canonical logical left equation")
    require(mixer, "double right=y-steer;", "canonical logical right equation")
    require(mixer_test, "forwardSteeringSlowsInsidePhysicalSide", "forward steering regression")
    require(mixer_test, "reverseSteeringUsesSameInsidePhysicalSide", "reverse steering regression")
    require(cfg, "DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32", "firmware logical left")
    require(cfg, "DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27", "firmware logical right")
    require(cfg, "DRIVE_L_INVERT=false,DRIVE_R_INVERT=false", "drive inversion contract")
    require(cfg, "int speed=100", "firmware full-scale default")
    require(profile, "DRIVE_L_EN=17", "Android logical left")
    require(profile, "DRIVE_R_EN=16", "Android logical right")
    require(gpio_doc, "LEFT → channel B", "GPIO authority logical left")
    require(gpio_doc, "RIGHT → channel A", "GPIO authority logical right")
    require(remote, 'p.put("left",left);p.put("right",right);command("drive",p,RemoteDrivePolicy.COMMAND_TTL_MS,false,0);', "Remote left/right parity")

    # Idle Direct does not continuously overwrite independent commands; non-zero motion is refreshed.
    require(api, "DIRECT_KEEPALIVE_MS=150L", "Direct keepalive policy")
    require(api, "if(left==0&&right==0)return;", "Direct idle-zero ownership")
    require(api, "if(now-lastDirectDriveAt<DIRECT_KEEPALIVE_MS)return;", "Direct non-zero refresh bound")

    # MQ operational warning remains separate from quantitative M4 claims.
    require(warning, "R0_NORMALIZED_FALLBACK = 1.0", "MQ fallback baseline")
    require(warning, "ELEVATED_DELTA = 0.20", "MQ elevated threshold")
    require(warning, "HIGH_DELTA = 0.40", "MQ high threshold")
    require(warning, "usedFallbackBaseline", "MQ warning provenance")

    # Alert GPIO is fail-closed electrically until a safe driver is explicitly declared installed.
    require(cfg, "ALERT_OUTPUT_INSTALLED=false", "alert hardware gate")
    require(alerts, "Cfg::ALERT_OUTPUT_INSTALLED&&activeLevel!=NONE", "alert output gating")
    require(alerts, "output_asserted", "alert telemetry assertion state")

    # Remote diagnostic UI and firmware expose the same ordered readiness stages.
    for key in ("configured", "wifi", "ip_ok", "dns", "time", "internet", "tls", "mqtt", "command_ack", "telemetry", "remote_ready", "detail"):
        require(remote_link, f'\\"{key}\\"', f"firmware remote status key {key}")
    for key in ("configured", "wifi", "ip_ok", "dns", "time", "internet", "tls", "mqtt", "command_ack", "telemetry", "remote_ready", "detail"):
        require(remote_ui, f'\"{key}\"', f"Android remote diagnostic key {key}")

    # Screening remains visible while reportable PPM remains M4-gated.
    require(analysis_ui, "Screening", "analysis screening label")
    require(analysis_ui, "Quantitative PPM", "analysis quantitative label")
    require(analysis_ui, "WITHHELD", "analysis withheld state")

    # Flammability seed retirement remains blocked until the pinned bundle exists offline.
    require(bootstrap, 'ASSET="generated/flammability/niosh_npg_flammability.json"', "flammability bundled asset contract")
    require(bootstrap, 'PINNED_SHA256="457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434"', "flammability pinned hash")
    require(doc, "the primary seed service is retained until then.", "flammability retirement gate")

    print("HazRob 0.10.1 HR-0101-048/060 -> HR066 cross-component integration contract: PASS")
    print("HR066 network authority preserved; HR048 drive map + travel-relative mixer + forward/reverse steering verified.")
    print("Physical field acceptance: PENDING")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print("HazRob 0.10.1 HR048/060 -> HR066 integration contract: FAIL:", exc)
        raise SystemExit(1)
