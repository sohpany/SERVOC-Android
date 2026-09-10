#!/usr/bin/env python3
"""Fail-closed static integration contract for the HazRob 0.10.1 release baseline.

This is intentionally cross-component: it detects drift that per-module compilation cannot.
It does not replace runtime/end-to-end tests.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")

def require(hay: str, needle: str, where: str) -> None:
    if needle not in hay:
        raise AssertionError(f"{where}: missing {needle!r}")

def main() -> int:
    gradle = text("app/build.gradle")
    audit = text("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    local = text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    robot_api = text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote_conn = text("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    master_cfg = text("firmware/HazRob/Config.h")
    master = text("firmware/HazRob/HazRob.ino")
    mqtt = text("firmware/HazRob/RemoteMqtt.cpp")
    cam = text("firmware/HazRob-CAM/HazRob-CAM.ino")
    relay_pkg = text("remote/video-relay/package.json")
    relay = text("remote/video-relay/src/index.js")

    # One active release identity across executable components.
    for needle, src, label in [
        ("versionName '0.10.1'", gradle, "Android Gradle"),
        ('VERSION="0.10.1"', audit, "Android ReleaseAudit"),
        ('FW[]="HazRob 0.10.1"', master_cfg, "master firmware"),
        ('FW[]="HazRob-CAM 0.10.1"', cam, "camera firmware"),
        ('"version": "0.10.1"', relay_pkg, "relay package"),
        ('SERVICE_VERSION="0.10.1"', relay, "relay runtime"),
    ]:
        require(src, needle, label)

    # Fixed Direct network contract must agree between Android and firmware.
    require(local, 'ROBOT_HOST="192.168.4.1"', "Android local contract")
    require(local, 'CAMERA_HOST="192.168.4.2"', "Android local contract")
    require(master, 'IPAddress ip(192,168,4,1)', "master SoftAP")
    require(master_cfg, 'CAM[]="192.168.4.2"', "master camera default")
    require(cam, 'IPAddress ip(192,168,4,2)', "camera static IP")
    require(cam, 'gw(192,168,4,1)', "camera gateway")

    # Canonical GPIO contract. These assertions deliberately fail if a pin changes
    # without a coordinated 0.10.1 hardware-profile review.
    gpio_needles = [
        'DRIVE_L_EN=16,DRIVE_L_IN1=18,DRIVE_L_IN2=27',
        'DRIVE_R_EN=17,DRIVE_R_IN1=19,DRIVE_R_IN2=32',
        'SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49',
        'RELAY_MQ=25',
        'ALERT_PIN=26',
        'SOUND_PIN=36',
        'IR_PIN=35',
        'LDR_PIN=34',
        'BATTERY_PIN=33',
        'ULTRA_TRIG_PIN=23',
        'ULTRA_ECHO_PIN=39',
        'INTERNAL_TEMP_RESERVED_PIN=14',
    ]
    for needle in gpio_needles:
        require(master_cfg, needle, "master GPIO contract")

    # Android-issued MQTT command types must all be handled by master firmware.
    android_remote = robot_api + "\n" + remote_conn
    emitted = set(re.findall(r'command\("([a-z0-9_]+)"', android_remote))
    # Methods that call typed helpers instead of command() directly still resolve to
    # the string literals in RemoteConnection and are therefore present in emitted.
    expected = {
        "probe", "drive", "motion_stop", "estop", "sensor_off", "heartbeat",
        "heartbeat_disarm", "relay", "measure_start", "measure_cancel", "alert",
        "alert_test", "camera_flash", "config_get", "config_set",
    }
    missing_android = expected - emitted
    if missing_android:
        raise AssertionError(f"Android remote contract missing expected emitters: {sorted(missing_android)}")
    handled = set(re.findall(r't=="([a-z0-9_]+)"', mqtt))
    missing_fw = emitted - handled
    if missing_fw:
        raise AssertionError(f"ESP32 RemoteMqtt lacks Android-issued commands: {sorted(missing_fw)}")

    # Direct HTTP API core parity: endpoints used by RobotApi must exist in the master.
    direct_required = {
        "/drive", "/motion/stop", "/stop", "/estop", "/sensor/off", "/heartbeat",
        "/diag", "/data", "/cfg", "/remote/status", "/remote/scan",
        "/remote/provision", "/remote/clear", "/relay", "/measure", "/alert",
    }
    for endpoint in sorted(direct_required):
        require(master, f'path=="{endpoint}"', f"master HTTP endpoint {endpoint}")

    # Role-scoped bootstrap contract; user UX can be passwordless while transport is not anonymous.
    for role in ("android", "esp32", "esp32-cam", "legacy"):
        require(relay, f'role==="{role}"', f"relay bootstrap role {role}")
    require(mqtt, 'X-HazRob-Client: esp32', "master bootstrap role")
    require(text("firmware/HazRob-CAM/RemoteVideo.cpp"), 'X-HazRob-Client: esp32-cam', "camera bootstrap role")
    require(text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java"), 'BOOTSTRAP_CLIENT="android"', "Android bootstrap role")

    print("HazRob 0.10.1 cross-component integration contract: PASS")
    print("Remote commands:", ", ".join(sorted(emitted)))
    return 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob 0.10.1 integration contract: FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
