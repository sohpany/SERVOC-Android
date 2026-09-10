#!/usr/bin/env python3
"""Fail-closed static integration contract for the HazRob 0.10.1 release baseline.

This audit is deliberately cross-component. It catches release-identity, GPIO,
Direct/Remote protocol, bootstrap-role and distribution drift that per-module
compilation cannot detect. It does not replace runtime/end-to-end testing.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")


def require(hay: str, needle: str, where: str) -> None:
    if needle not in hay:
        raise AssertionError(f"{where}: missing {needle!r}")


def forbid(hay: str, needle: str, where: str) -> None:
    if needle in hay:
        raise AssertionError(f"{where}: forbidden stale marker {needle!r}")


def main() -> int:
    gradle = text("app/build.gradle")
    audit = text("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    local = text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    link_service = text("app/src/main/java/com/servoc/app/communication/RobotLinkService.java")
    connection_gate = text("app/src/main/java/com/servoc/app/auth/ConnectionGate.java")
    robot_api = text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote_conn = text("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    remote_session = text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")
    service_profile = text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")

    master_cfg = text("firmware/HazRob/Config.h")
    master = text("firmware/HazRob/HazRob.ino")
    hardware_h = text("firmware/HazRob/Hardware.h")
    mqtt = text("firmware/HazRob/RemoteMqtt.cpp")
    cloud_cfg = text("firmware/HazRob/RemoteCloudConfig.h")
    cam = text("firmware/HazRob-CAM/HazRob-CAM.ino")
    cam_remote = text("firmware/HazRob-CAM/RemoteVideo.cpp")
    cam_cfg = text("firmware/HazRob-CAM/RemoteVideoConfig.h")

    relay_pkg = text("remote/video-relay/package.json")
    relay = text("remote/video-relay/src/index.js")
    apk_workflow = text(".github/workflows/build-apk.yml")
    fw_workflow = text(".github/workflows/build-firmware.yml")
    root_readme = text("README.md")
    architecture = text("ARCHITECTURE.md")
    gpio_doc = text("docs/0.10.1/GPIO_AUTHORITY.md")

    # 1) One active release identity across executable components and release docs.
    for needle, src, label in [
        ("versionName '0.10.1'", gradle, "Android Gradle"),
        ("versionCode 21", gradle, "Android Gradle"),
        ('VERSION="0.10.1"', audit, "Android ReleaseAudit"),
        ('FW[]="HazRob 0.10.1"', master_cfg, "master firmware"),
        ('FW[]="HazRob-CAM 0.10.1"', cam, "camera firmware"),
        ('"version": "0.10.1"', relay_pkg, "relay package"),
        ('SERVICE_VERSION="0.10.1"', relay, "relay runtime"),
        ("# HazRob 0.10.1", root_readme, "root README"),
        ("# HazRob 0.10.1 Architecture", architecture, "architecture"),
        ("# HazRob 0.10.1 — Canonical GPIO Authority", gpio_doc, "GPIO authority"),
    ]:
        require(src, needle, label)

    # Critical active artifacts must not silently regress to retired release names.
    stale_checks = [
        ("v0.10.0-source", fw_workflow, "firmware workflow"),
        ("HazRob-v0.10.0-firmware", fw_workflow, "firmware workflow"),
        ("dev/hazrob-0.10-chemcore-foundation", apk_workflow, "APK workflow"),
        ("# HazRob 0.9 Architecture", architecture, "architecture"),
        ("HazRob master ESP32 firmware — 0.10.0", text("firmware/HazRob/README.md"), "master README"),
        ("HazRob 0.10 cloud profile", cloud_cfg, "master cloud config"),
        ("HazRob 0.10 Render video profile", cam_cfg, "camera remote config"),
    ]
    for marker, src, label in stale_checks:
        forbid(src, marker, label)

    require(fw_workflow, "HazRob-ESP32-v0.10.1-source.zip", "firmware source artifact")
    require(fw_workflow, "HazRob-ESP32-CAM-v0.10.1-source.zip", "camera source artifact")
    require(fw_workflow, "HazRob-v0.10.1-firmware", "firmware binary artifact")
    require(fw_workflow, "refs/heads/release/hazrob-0.10.1-ready", "firmware release gate")
    require(apk_workflow, "refs/heads/release/hazrob-0.10.1-ready", "APK release gate")
    require(link_service, 'BuildConfig.VERSION_NAME', "runtime notification identity")

    # 2) Fixed Direct network contract must agree between Android and firmware.
    require(local, 'ROBOT_HOST="192.168.4.1"', "Android local contract")
    require(local, 'CAMERA_HOST="192.168.4.2"', "Android local contract")
    require(master, 'IPAddress ip(192,168,4,1)', "master SoftAP")
    require(master_cfg, 'CAM[]="192.168.4.2"', "master camera default")
    require(cam, 'IPAddress ip(192,168,4,2)', "camera static IP")
    require(cam, 'gw(192,168,4,1)', "camera gateway")
    require(connection_gate, '.putString("robot",LocalNetworkContract.ROBOT_HOST)', "Direct stale-host purge")
    require(connection_gate, 'listener.onReady(Mode.DIRECT)', "Direct non-blocking admission")
    forbid(connection_gate, 'diagnosticsRaw()', "Direct admission gate")

    # 3) Canonical GPIO contract. Any change here requires explicit hardware review.
    gpio_needles = [
        'HW_PROFILE[]="hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1"',
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

    for needle in [
        "ENA=GPIO16", "ENB=GPIO17", "IN1=18", "IN2=27", "IN3=19", "IN4=32",
        "GPIO25", "GPIO36", "GPIO35", "GPIO34", "GPIO33", "GPIO23", "GPIO39",
        "GPIO21", "GPIO22",
    ]:
        require(gpio_doc, needle, "GPIO authority document")
    require(gpio_doc, "Remove the ENA jumper", "GPIO authority ENA rule")
    require(gpio_doc, "Remove the ENB jumper", "GPIO authority ENB rule")

    # 4) Android-issued MQTT commands must all be handled by the master.
    android_remote = robot_api + "\n" + remote_conn
    emitted = set(re.findall(r'command\("([a-z0-9_]+)"', android_remote))
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

    # Local and Remote configuration must share one validation/persistence implementation.
    require(hardware_h, "struct SettingsPatch", "shared settings patch")
    require(hardware_h, "applySettings", "shared settings application")
    require(master, "applySettings(patch,true)", "local /cfg shared settings path")
    require(mqtt, "applySettings(patch,true)", "remote config_set shared settings path")
    require(mqtt, 't=="config_get"', "remote config_get")
    require(mqtt, 't=="config_set"', "remote config_set")

    # 5) Direct HTTP API core parity.
    direct_required = {
        "/drive", "/motion/stop", "/stop", "/estop", "/sensor/off", "/heartbeat",
        "/diag", "/data", "/cfg", "/remote/status", "/remote/scan",
        "/remote/provision", "/remote/clear", "/relay", "/measure", "/alert",
    }
    for endpoint in sorted(direct_required):
        require(master, f'path=="{endpoint}"', f"master HTTP endpoint {endpoint}")

    # 6) Remote service coordinates and role-scoped bootstrap must agree.
    hivemq = "e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"
    require(service_profile, f'MQTT_HOST="{hivemq}"', "Android HiveMQ host")
    require(cloud_cfg, f'BROKER_HOST[]="{hivemq}"', "ESP32 HiveMQ host")
    require(service_profile, 'MQTT_PORT=8883', "Android HiveMQ TLS port")
    require(cloud_cfg, 'BROKER_PORT=8883', "ESP32 HiveMQ TLS port")
    require(service_profile, 'ROBOT_ID="hazrob-001"', "Android robot id")
    require(cloud_cfg, 'ROBOT_ID[]="hazrob-001"', "ESP32 robot id")
    require(cam_cfg, 'ROBOT_ID[]="hazrob-001"', "camera robot id")

    for role in ("android", "esp32", "esp32-cam", "legacy"):
        require(relay, f'role==="{role}"', f"relay bootstrap role {role}")
    require(remote_session, 'BOOTSTRAP_CLIENT="android"', "Android bootstrap role")
    require(mqtt, 'X-HazRob-Client: esp32', "master bootstrap role")
    require(cam_remote, 'X-HazRob-Client: esp32-cam', "camera bootstrap role")
    require(cam_remote, 'video_publish_token', "camera publish credential")
    forbid(cam_remote, 'mqtt_password', "camera video credential path")

    require(relay, 'video_view_token:c.view', "Android view-token response")
    require(relay, 'video_publish_token:c.publish', "camera publish-token response")
    require(relay, 'mqtt_user:c.robotUser,mqtt_password:c.robotPass', "robot MQTT response")
    require(relay, 'mqtt_user:c.appUser,mqtt_password:c.appPass', "operator MQTT response")

    print("HazRob 0.10.1 cross-component integration contract: PASS")
    print("Remote commands:", ", ".join(sorted(emitted)))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob 0.10.1 integration contract: FAIL: {exc}")
        raise SystemExit(1)
