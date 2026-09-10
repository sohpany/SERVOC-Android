#!/usr/bin/env python3
"""Fail-closed static integration contract for the HazRob 0.10.1 release baseline.

This audit is deliberately cross-component. It catches release-identity, GPIO,
Direct/Remote protocol, bootstrap-role, scientific-data and distribution drift
that per-module compilation cannot detect. It does not replace runtime/end-to-end testing.
"""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]


def path_of(path: str) -> Path:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required file: {path}")
    return p


def text(path: str) -> str:
    return path_of(path).read_text(encoding="utf-8")


def json_file(path: str):
    return json.loads(text(path))


def sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with path_of(path).open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def require(hay: str, needle: str, where: str) -> None:
    if needle not in hay:
        raise AssertionError(f"{where}: missing {needle!r}")


def forbid(hay: str, needle: str, where: str) -> None:
    if needle in hay:
        raise AssertionError(f"{where}: forbidden stale marker {needle!r}")


def java_string_array_count(src: str, field: str, where: str) -> int:
    m = re.search(rf'public\s+static\s+final\s+String\[\]\s+{re.escape(field)}\s*=\s*\{{(.*?)\}}\s*;', src, re.S)
    if not m:
        raise AssertionError(f"{where}: cannot locate String[] {field}")
    return len(re.findall(r'"(?:\\.|[^"\\])*"', m.group(1)))


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
    control_tab = text("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    status_tab = text("app/src/main/java/com/servoc/app/ui/StatusTab.java")
    video_readiness = text("app/src/main/java/com/servoc/app/camera/RemoteVideoReadiness.java")

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
        "probe", "drive", "motion_stop", "estop", "estop_reset", "sensor_off", "heartbeat",
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

    # E-stop is a latched cross-layer contract, including explicit reset and UI inhibition.
    require(master, 'path=="/estop/reset"', "Direct E-stop reset endpoint")
    require(mqtt, 't=="estop_reset"', "Remote E-stop reset handler")
    require(remote_conn, 'command("estop_reset"', "Android Remote E-stop reset command")
    require(robot_api, 'public void emergencyReset()', "Android Direct/Remote E-stop reset API")
    require(control_tab, 'setOnLongClickListener', "E-stop intentional reset gesture")
    require(control_tab, 'if(estopLatched){host.setDriveCommand(0,0);return;}', "E-stop joystick inhibition")
    require(control_tab, 'host.robotApi().emergencyReset()', "E-stop UI reset path")

    # Local and Remote configuration must share one validation/persistence implementation.
    require(hardware_h, "struct SettingsPatch", "shared settings patch")
    require(hardware_h, "applySettings", "shared settings application")
    require(master, "applySettings(patch,true)", "local /cfg shared settings path")
    require(mqtt, "applySettings(patch,true)", "remote config_set shared settings path")
    require(mqtt, 't=="config_get"', "remote config_get")
    require(mqtt, 't=="config_set"', "remote config_set")

    # 5) Direct HTTP API core parity.
    direct_required = {
        "/drive", "/motion/stop", "/stop", "/estop", "/estop/reset", "/sensor/off", "/heartbeat",
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

    # Render publisher status is the remote-camera status authority; WSS first-frame remains the full video diagnostic.
    require(service_profile, 'VIDEO_STATUS_HTTPS=', "Render camera status coordinate")
    require(video_readiness, 'public static Result probeStatus(SharedPreferences prefs)', "lightweight Render camera status probe")
    require(video_readiness, 'RemoteServiceProfile.VIDEO_STATUS_HTTPS', "Render camera status endpoint use")
    require(status_tab, 'RemoteVideoReadiness.probeStatus(host.preferences())', "StatusTab Render camera authority")
    forbid(status_tab, 'remoteCameraStatus()', "StatusTab obsolete MQTT camera status")

    # 7) Scientific workflow authority: active generators target 0.10.1, while public Stable
    # publication remains release-gated and cannot accept arbitrary payload/count declarations.
    stable_wf = text(".github/workflows/chemcore-library-publish.yml")
    molecular_wf = text(".github/workflows/chemcore-molecular-seed.yml")
    curation_wf = text(".github/workflows/chemcore-unresolved-curation.yml")
    candidate_wf = text(".github/workflows/chemcore-candidate-qc.yml")
    pin_wf = text(".github/workflows/pin-scientific-data.yml")
    for src, label in [
        (stable_wf, "ChemCore Stable workflow"),
        (molecular_wf, "molecular workflow"),
        (curation_wf, "curation workflow"),
        (candidate_wf, "candidate workflow"),
        (pin_wf, "scientific pin workflow"),
    ]:
        forbid(src, "dev/hazrob-0.10-chemcore-foundation", label)
        forbid(src, "dev/hazrob-0.6", label)

    for src, label in [
        (molecular_wf, "molecular workflow"),
        (curation_wf, "curation workflow"),
        (candidate_wf, "candidate workflow"),
        (pin_wf, "scientific pin workflow"),
    ]:
        require(src, "dev/hazrob-0.10.1", label)

    require(stable_wf, "release/hazrob-0.10.1-ready", "ChemCore Stable release gate")
    require(stable_wf, "--payload-format json --min-app-version 0.10.1", "ChemCore Stable publication contract")
    require(stable_wf, "Validate canonical snapshot and derive publication counts", "ChemCore count derivation")
    forbid(stable_wf, "payload_path:", "ChemCore Stable manual inputs")
    forbid(stable_wf, "chemical_count:", "ChemCore Stable manual inputs")
    forbid(stable_wf, "model_count:", "ChemCore Stable manual inputs")
    if re.search(r'rm\s+-rf\s+app/src/main/assets/generated\s*(?:\n|$)', pin_wf):
        raise AssertionError("scientific pin workflow: broad generated/ wipe is forbidden")
    require(pin_wf, "rm -rf app/src/main/assets/generated/niosh app/src/main/assets/generated/exposure", "scientific pin isolation")
    require(pin_wf, "git add app/src/main/assets/generated/niosh app/src/main/assets/generated/exposure", "scientific pin commit isolation")

    # 8) Manifest compatibility is fail-closed for the schema and Stable transport actually
    # consumed by the 0.10.1 runtime.
    manifest_src = text("app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManifest.java")
    require(manifest_src, "schemaVersion != MANIFEST_SCHEMA", "ChemCore manifest schema gate")
    require(manifest_src, 'stable payload_format must be json', "ChemCore Stable JSON gate")
    require(manifest_src, 'SHA256withRSA', "ChemCore Stable signature gate")

    # 9) Priority analytical catalogue and research model catalogue must be real arrays, not
    # descriptive constants. Formula/name rows remain positionally aligned.
    chemical_catalog = text("app/src/main/java/com/servoc/app/chemical/ChemicalCatalog.java")
    model_catalog = text("app/src/main/java/com/servoc/app/models/MqDataScience76Catalog.java")
    chemical_names = java_string_array_count(chemical_catalog, "NAMES", "ChemicalCatalog")
    chemical_formulas = java_string_array_count(chemical_catalog, "FORMULAS", "ChemicalCatalog")
    model_names = java_string_array_count(model_catalog, "NAMES", "MqDataScience76Catalog")
    if chemical_names != 144:
        raise AssertionError(f"ChemicalCatalog: expected 144 priority entries, got {chemical_names}")
    if chemical_formulas != chemical_names:
        raise AssertionError(f"ChemicalCatalog: formula/name length mismatch {chemical_formulas}!={chemical_names}")
    if model_names != 76:
        raise AssertionError(f"MqDataScience76Catalog: expected 76 models, got {model_names}")
    require(chemical_catalog, "COUNT = NAMES.length", "ChemicalCatalog derived count")
    require(model_catalog, "COUNT=NAMES.length", "MqDataScience76Catalog derived count")

    # 10) Pinned scientific data: exact release-baseline counts, provenance and payload hashes.
    niosh = json_file("app/src/main/assets/generated/niosh/niosh_chemicals.json")
    niosh_m = json_file("app/src/main/assets/generated/niosh/manifest.json")
    idlh = json_file("app/src/main/assets/generated/exposure/niosh_idlh.json")
    idlh_m = json_file("app/src/main/assets/generated/exposure/manifest.json")
    osha = json_file("app/src/main/assets/generated/exposure/osha_z1/osha_z1.json")
    osha_m = json_file("app/src/main/assets/generated/exposure/osha_z1/manifest.json")
    datasets = [
        ("NIOSH identity", niosh, niosh_m, "app/src/main/assets/generated/niosh/niosh_chemicals.json", 652),
        ("NIOSH IDLH", idlh, idlh_m, "app/src/main/assets/generated/exposure/niosh_idlh.json", 402),
        ("OSHA Z-1", osha, osha_m, "app/src/main/assets/generated/exposure/osha_z1/osha_z1.json", 638),
    ]
    for label, payload, manifest, payload_path, count in datasets:
        if payload.get("record_count") != count or manifest.get("record_count") != count:
            raise AssertionError(f"{label}: expected record_count {count}")
        if manifest.get("authoritative") is not True:
            raise AssertionError(f"{label}: manifest is not authoritative")
        actual = sha256_file(payload_path)
        if manifest.get("sha256") != actual:
            raise AssertionError(f"{label}: payload SHA-256 mismatch")
    if osha_m.get("substance_rows") != 432:
        raise AssertionError(f"OSHA Z-1: expected 432 substance_rows, got {osha_m.get('substance_rows')}")

    # 11) ChemCore molecular identity layer remains 652 = 615 resolved + 37 curated unresolved.
    molecular = json_file("app/src/main/assets/generated/chemcore/molecular_descriptors.json")
    molecular_m = json_file("app/src/main/assets/generated/chemcore/manifest.json")
    curation = json_file("app/src/main/assets/generated/chemcore/unresolved_curation.json")
    molecular_sha = sha256_file("app/src/main/assets/generated/chemcore/molecular_descriptors.json")
    if molecular.get("record_count") != 652 or len(molecular.get("records", [])) != 652:
        raise AssertionError("ChemCore molecular seed: expected 652 identities")
    if molecular.get("resolved_count") != 615 or molecular.get("unresolved_count") != 37:
        raise AssertionError("ChemCore molecular seed: expected 615 resolved / 37 unresolved")
    if molecular.get("error_count") != 0:
        raise AssertionError("ChemCore molecular seed: retrieval error_count must be zero")
    if molecular_m.get("record_count") != 652 or molecular_m.get("resolved_count") != 615 or molecular_m.get("unresolved_count") != 37:
        raise AssertionError("ChemCore molecular manifest counts drifted")
    if molecular_m.get("sha256") != molecular_sha:
        raise AssertionError("ChemCore molecular manifest SHA-256 mismatch")
    if molecular_m.get("source_identity_sha256") != niosh_m.get("sha256"):
        raise AssertionError("ChemCore molecular seed no longer identifies the pinned NIOSH identity source")
    if curation.get("schema") != "chemcore-unresolved-curation-v1" or curation.get("record_count") != 37:
        raise AssertionError("ChemCore unresolved curation count/schema drifted")
    if curation.get("source_seed_sha256") != molecular_sha:
        raise AssertionError("ChemCore unresolved curation is not bound to the pinned molecular seed")
    if any(r.get("molecular_qspr_eligible") is not False for r in curation.get("records", [])):
        raise AssertionError("ChemCore unresolved identity was silently made molecular-QSPR eligible")

    print("HazRob 0.10.1 cross-component integration contract: PASS")
    print("Remote commands:", ", ".join(sorted(emitted)))
    print(f"Scientific catalogues: priority={chemical_names}, MQDataScience models={model_names}")
    print("Pinned data: NIOSH=652, IDLH=402, OSHA-Z1=638/432 substances, ChemCore=652/615/37")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, json.JSONDecodeError) as exc:
        print(f"HazRob 0.10.1 integration contract: FAIL: {exc}")
        raise SystemExit(1)
