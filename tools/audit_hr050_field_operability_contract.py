#!/usr/bin/env python3
"""Fail-closed HR-0101-050 field-operability/reproducibility contract gate."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def txt(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing HR050 file: {path}")
    return p.read_text(encoding="utf-8")


def req(src: str, needle: str, label: str) -> None:
    if needle not in src:
        raise AssertionError(f"{label}: missing {needle!r}")


def forbid(src: str, needle: str, label: str) -> None:
    if needle in src:
        raise AssertionError(f"{label}: forbidden {needle!r}")


def main() -> int:
    cfg = txt("firmware/HazRob/Config.h")
    hw = txt("firmware/HazRob/Hardware.cpp")
    profile = txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")
    remote = txt("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")
    mqtt = txt("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java")
    entry = txt("app/src/main/java/com/servoc/app/auth/ConnectionGate.java")
    control_tab = txt("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    measurements = txt("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java")
    instant = txt("app/src/main/java/com/servoc/app/measurement/InstantMqWarningEngine.java")
    gas_alert = txt("app/src/main/java/com/servoc/app/measurement/GasAlertCoordinator.java")
    main_activity = txt("app/src/main/java/com/servoc/app/MainActivity.java")
    host = txt("app/src/main/java/com/servoc/app/ui/HazRobHost.java")
    settings = txt("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    video_cfg = txt("firmware/HazRob-CAM/RemoteVideoConfig.h")
    video = txt("firmware/HazRob-CAM/RemoteVideo.cpp")
    gradle = txt("app/build.gradle")
    fw_flow = txt(".github/workflows/build-firmware.yml")
    apk_flow = txt(".github/workflows/build-apk.yml")

    for needle in ["ENV_SDA=13", "ENV_SCL=14", "SDA=21,SCL=22", "ENV_I2C_HZ=100000"]:
        req(cfg, needle, "ENV/ADS GPIO authority")
    req(hw, "TwoWire EnvWire(1)", "second ESP32 I2C controller")
    req(hw, "EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)", "ENV bus startup")
    req(hw, "envAck76=envProbe(0x76)", "ENV bus probe")
    req(profile, "ENV_I2C_SDA_GPIO=13", "Android ENV SDA")
    req(profile, "ENV_I2C_SCL_GPIO=14", "Android ENV SCL")

    req(cfg, "IR_GROUND_ACTIVE=LOW", "IR ground polarity candidate")
    req(cfg, "IR_CLIFF_ASSERT_SAMPLES=3", "IR persistence")
    req(cfg, "IR_REFERENCE_HEIGHT_CM=8.0f", "IR 8 cm physical reference")
    req(hw, "groundNow=irLevel==Cfg::IR_GROUND_ACTIVE", "IR ground semantics")
    req(hw, "irWarning=true", "IR cliff assertion")
    req(hw, '\\"cliff_warning\\"', "IR telemetry cliff state")
    req(hw, '\\"ground_detected\\"', "IR telemetry ground state")
    req(control_tab, 'irBadge.setText(cliff?"⚠ حافة":ground?"أرض ●":"أرض ؟")', "drive cliff UX")
    req(control_tab, 'optBoolean("cliff_warning",ir.optBoolean("warning",false))', "drive cliff telemetry authority")

    req(remote, "BOOTSTRAP_CONNECT_TIMEOUT_MS=5000", "bootstrap connect bound")
    req(remote, "BOOTSTRAP_READ_TIMEOUT_MS=8000", "bootstrap read bound")
    req(remote, "public boolean videoProfileConfigured()", "video readiness separation")
    service_body = remote.split("public boolean serviceProfileConfigured(){",1)[1].split("}\n",1)[0]
    forbid(service_body, "RemoteVideoCredentialStore", "control readiness video coupling")
    req(mqtt, "tcp.connect(new InetSocketAddress(host,port),CONNECT_TIMEOUT_MS)", "bounded MQTT TCP connect")
    req(mqtt, "broker hostname verification failed", "verified MQTT TLS")
    req(entry, "إلغاء الاتصال", "cancellable remote entry")
    req(entry, 'local.postForm("/connection/remote"', "explicit Direct-to-Remote handoff")
    req(entry, 'WifiRoute.releaseRobotWifi(a)', "release local-only network for Remote")
    req(entry, "بعد إعادة تشغيل الروبوت يكون Direct افتراضيًا", "Direct-first remote diagnostic UX")
    req(entry, "يجب تفعيل Remote محليًا", "explicit Remote activation diagnostic UX")
    req(entry, 'developer_authorized', "developer Remote authorization admission")

    req(instant, "RESPONSE_MIN_DELTA = 0.05", "response visibility floor")
    req(instant, "Level{UNAVAILABLE,NORMAL,RESPONSE,ELEVATED,HIGH}", "response state")
    req(instant, "3.0*Math.abs(noise)", "noise-aware response threshold")
    forbid(gas_alert, "Level.RESPONSE", "RESPONSE must not escalate gas alarm")
    req(measurements, "data.setTextColor(0xFF616161)", "live-reading grey text on light UI")
    req(measurements, "warn.level==InstantMqWarningEngine.Level.ELEVATED", "elevated live warning state")
    req(measurements, "head.setBackgroundColor(0xFFD32F2F)", "elevated gas red sensor heading")
    req(measurements, "التسجيل متوقف والقراءة الحية مستمرة", "live acquisition independent of timed session")
    req(main_activity, "phoneAlertTone.update(phoneLevel,now)", "phone audible warning path")
    req(main_activity, 'String[] names={"القيادة","الجلسات","القياسات","التقارير","الحالة","الإعدادات","المطور"}', "reports navigation")
    req(main_activity, "new ReportsTab(this,this)", "reports integration")
    req(host, "void testPhoneAlert();", "phone alert host contract")
    req(settings, "اختبار صوت الهاتف", "phone alert UI test")

    req(video_cfg, "FRAME_INTERVAL_MIN_MS=55", "remote video fast target")
    req(video_cfg, "FRAME_INTERVAL_MAX_MS=120", "remote video backoff ceiling")
    req(video, "if(w+6>=interval)interval=min(RemoteVideoCfg::FRAME_INTERVAL_MAX_MS", "adaptive video slowdown")
    req(video, "interval=max(RemoteVideoCfg::FRAME_INTERVAL_MIN_MS,interval-RemoteVideoCfg::FRAME_INTERVAL_STEP_MS)", "adaptive video recovery")
    forbid(video, "std::queue", "remote video backlog")

    req(gradle, "HAZROB_FLAMMABILITY_SHA256 = '457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434'", "controlled flammability digest")
    req(gradle, "HAZROB_ACCEPTED_APK_URL", "content-addressed accepted artifact source")
    req(gradle, "HAZROB_ACCEPTED_APK_ASSET", "accepted artifact seed path")
    req(gradle, "ZipFile z = new ZipFile(acceptedApk)", "accepted APK extraction")
    req(gradle, "Unable to recover controlled HazRob flammability snapshot from accepted APK", "fail-closed recovery")
    req(gradle, "HazRob flammability seed SHA-256 mismatch", "post-extraction digest gate")
    forbid(gradle, "HAZROB_FLAMMABILITY_URL", "live seed service must not be a build input")
    forbid(gradle, "build_niosh_flammability_seed.py", "live NIOSH regeneration must not occur in Gradle")

    req(fw_flow, "esp32:esp32@3.3.11", "pinned ESP32 Arduino core")
    req(fw_flow, "audit_0101_firmware_baseline.py", "firmware baseline release gate")
    req(apk_flow, "audit_hr047_gas_safety_contract.py", "APK consolidated release gate")

    print("HazRob HR-0101-050 field-operability contract: PASS")
    print("ENV: dedicated GPIO13/14 I2C; ADS remains GPIO21/22")
    print("IR: persistent loss-of-ground cliff warning with explicit drive-screen semantics")
    print("Remote: bounded verified TLS/MQTT; explicit profile handoff; control admission independent from video")
    print("MQ: RESPONSE visibility remains below warning/quantitative authority")
    print("Video: inline adaptive 55..120 ms pacing; nominal good-link target ~18.2 fps")
    print("Build reproducibility: controlled flammability bytes recovered only from accepted APK and SHA-256 verified; no live regeneration")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob HR-0101-050 field-operability gate: FAIL: {exc}")
        raise SystemExit(1)
