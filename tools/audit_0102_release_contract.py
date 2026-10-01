#!/usr/bin/env python3
"""Canonical HazRob 0.10.2 / HR069 release contract.

This is the authoritative cross-layer source audit for the 0.10.2 baseline.
Historical HR-named audits remain supplemental provenance/compatibility gates.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def txt(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def req(source: str, needle: str, label: str) -> None:
    if needle not in source:
        raise AssertionError(f"{label}: missing {needle!r}")


def forbid(source: str, needle: str, label: str) -> None:
    if needle in source:
        raise AssertionError(f"{label}: forbidden {needle!r}")


def main() -> int:
    manifest = txt("app/src/main/AndroidManifest.xml")
    app_lifecycle = txt("app/src/main/java/com/servoc/app/HazRobApplication.java")
    main_activity = txt("app/src/main/java/com/servoc/app/MainActivity.java")
    entry = txt("app/src/main/java/com/servoc/app/EntryActivity.java")
    release = txt("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    db = txt("app/src/main/java/com/servoc/app/data/ExperimentsDb.java")
    experiment_queries = txt("app/src/main/java/com/servoc/app/data/ExperimentQueries.java")
    hardware_profile = txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")
    measurements_tab = txt("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java")
    analytical_lab = txt("app/src/main/java/com/servoc/app/ui/AnalyticalLabTab.java")
    settings_tab = txt("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    robot_api = txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote_connection = txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    robot_protocol_v4 = txt("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    telemetry_recorder = txt("app/src/main/java/com/servoc/app/measurement/TelemetryRecorder.java")
    echo_badge = txt("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java")
    gas_badge = txt("app/src/main/java/com/servoc/app/ui/GasHazardBadge.java")
    physical_ui = txt("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java")
    mic_calibration = txt("app/src/main/java/com/servoc/app/calibration/MicrophoneDbCalibration.java")
    hw = txt("firmware/HazRob/Hardware.cpp")
    cfg = txt("firmware/HazRob/Config.h")
    ino = txt("firmware/HazRob/HazRob.ino")
    remote_mqtt = txt("firmware/HazRob/RemoteMqtt.cpp")
    safety_options = txt("firmware/HazRob/SafetyOptions.cpp")
    user_library = txt("app/src/main/java/com/servoc/app/field/HazRobUserLibrary.java")
    archive = txt("app/src/main/java/com/servoc/app/field/FieldSessionArchive.java")
    report = txt("app/src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")
    host = txt("app/src/main/java/com/servoc/app/ui/HazRobHost.java")
    sessions_tab = txt("app/src/main/java/com/servoc/app/ui/FieldSessionsTab.java")

    # Canonical product identity.
    req(release, 'VERSION="0.10.2"', "app release identity")
    req(release, 'ENGINEERING_REVISION="HR069"', "engineering revision authority")
    req(release, "IR_WARNING_ONLY=true", "release audit IR warning-only semantics")
    req(release, "IR_OPTIONAL_AUTO_STOP=false", "release audit IR auto-stop disabled")
    req(release, "ULTRASONIC_OPTIONAL_AUTO_STOP=false", "release audit ultrasonic auto-stop disabled")
    req(release, "SAFETY_WARNINGS_ALWAYS_ACTIVE=true", "release audit warning independence")
    req(
        hardware_profile,
        'ID="hazrob-0.10.2-hr069-1r8mq-l298n6-batt-ultra-ir-ldr-env2-v2"',
        "Android HR069 hardware profile identity",
    )
    req(measurements_tab, '"HazRob "+ReleaseAudit.VERSION+" acquisition session', "measurement provenance release identity")
    req(analytical_lab, '"المختبر الكيميائي التحليلي الموسع • HazRob "+ReleaseAudit.VERSION', "analytical lab release identity")
    forbid(hardware_profile, "0.10.1", "stale hardware profile identity")
    forbid(measurements_tab, "0.10.1", "stale measurement identity")
    forbid(analytical_lab, "0.10.1", "stale analytical lab identity")

    # Database authority must agree with the actual SQLiteOpenHelper schema.
    req(release, "DATABASE_SCHEMA=9", "release database schema")
    req(db, "DB_VERSION=9", "database implementation schema")
    req(db, "if(oldVersion<9)createV9UserFieldTables(db);", "schema-9 migration")

    # User-facing navigation, landscape driving presentation and durable field library.
    req(
        main_activity,
        'String[] names={"القيادة","الجلسات","القياسات","التقارير","الحالة","الإعدادات","المطور"}',
        "0.10.2 navigation",
    )
    req(manifest, 'android:name=".HazRobApplication"', "landscape lifecycle application registration")
    req(app_lifecycle, "root.getChildAt(1).setVisibility(View.GONE)", "landscape navigation tabs hidden")
    req(app_lifecycle, "root.getChildAt(0).setVisibility(View.VISIBLE)", "landscape global safety status retained")
    req(app_lifecycle, "root.getChildAt(3).setVisibility(View.GONE)", "landscape footer hidden")
    req(app_lifecycle, "box.getChildAt(0).setVisibility(View.GONE)", "landscape generic refresh row hidden")
    req(app_lifecycle, "((HazRobHost)activity).setDriveCommand(0,0)", "rotation/background drive neutralization")
    req(app_lifecycle, "installEchoDistanceBadge(activity,pageGroup)", "landscape ultrasonic distance overlay")
    req(echo_badge, 'optDouble("distance_cm",proximity.optDouble("cm"', "normalized ultrasonic distance telemetry")
    req(echo_badge, "cm<=WARNING_CM?0xFFFF5252:0xFF66BB6A", "ultrasonic 10 cm color threshold")
    req(app_lifecycle, "installGasHazardBadge(activity,pageGroup)", "landscape gas hazard overlay")
    req(gas_badge, 'alert.optInt("gas",0)', "gas overlay level source")
    req(gas_badge, "⚠ خطر غاز مرتفع", "high gas driving warning")
    req(entry, "showFirstRunLibrarySetup()", "first-run durable library")
    req(entry, "HazRobUserLibrary.createPickerIntent()", "SAF library picker")
    req(user_library, "FieldSessionArchive.exportSession", "persistent source archive")
    req(user_library, "FieldSessionArchive.importSession", "persistent source restore")

    # Physical measurements UI and MAX4466 one-point reference-meter compensation.
    req(app_lifecycle, "PhysicalMeasurementsUiEnhancer.install(activity)", "physical measurements UI enhancer installed")
    req(physical_ui, "setStretchAllColumns(false)", "physical table uncontrolled stretch disabled")
    req(physical_ui, "setShrinkAllColumns(false)", "physical table character-by-character shrink disabled")
    req(physical_ui, "WEIGHTS={0.30f,0.18f,0.16f,0.36f}", "physical table responsive column weights")
    req(physical_ui, "tv.setSingleLine(true)", "physical value/unit no character wrapping")
    req(physical_ui, "tv.setMinWidth(dp(44))", "physical unit minimum width")
    req(robot_protocol_v4, 'x.put("db_spl",db)', "legacy/provided MAX4466 dB survives API4 normalization")
    req(mic_calibration, 'PREF_OFFSET_DB="microphone_db_offset_db"', "microphone offset preference authority")
    req(mic_calibration, "20.0*Math.log10(ratio)", "microphone RMS-to-dBFS basis")
    req(mic_calibration, "referenceDb-hazRobRawDb", "reference-meter offset equation")
    req(mic_calibration, "raw+offsetDb(p)", "microphone compensation equation")
    req(settings_tab, "تعويض الميكروفون (dB)", "operator microphone compensation field")
    req(settings_tab, "قراءة جهاز القياس المعاير − قراءة HazRob الخام", "operator microphone offset guidance")
    req(settings_tab, "saveMicrophoneOffset()", "operator microphone offset persistence")
    req(physical_ui, "MicrophoneDbCalibration.rawDbFs(sound)", "physical UI raw dBFS derivation")
    req(physical_ui, "MicrophoneDbCalibration.compensate(rawDb,offset)", "physical UI saved offset application")
    req(physical_ui, "معوض بمرجع خارجي", "physical UI compensation provenance")
    req(hw, '\\"rms_v\\":', "firmware publishes MAX4466 RMS voltage")
    req(hw, '\\"p2p_v\\":', "firmware publishes MAX4466 peak-to-peak voltage")
    req(hw, '\\"db_spl\\":null', "firmware does not claim unverified SPL")
    req(hw, '\\"gain_reference\\":null', "firmware does not assume MAX4466 gain")
    forbid(hw, "AMP_GAIN=25.0f", "fixed 25x microphone gain assumption")
    forbid(physical_ui, "25×", "fixed 25x microphone gain display")

    # P2 measurement/telemetry contract: normalized state must survive acquisition and persistence.
    req(hw, 'const char* measurementStateName()', "firmware canonical measurement state names")
    req(hw, '\\"state\\":\\""+String(measurementStateName())', "firmware measurement state JSON quoting")
    req(robot_protocol_v4, 'x.put("state",state)', "Android measurement state normalization")
    req(robot_protocol_v4, 'x.put("state_code",code)', "Android measurement state-code normalization")
    req(robot_protocol_v4, 'x.put("valid_channels"', "Android measurement valid-channel normalization")
    req(telemetry_recorder, 'state.put("measurement_state",measurementState)', "session persists canonical measurement state")
    req(telemetry_recorder, 'state.put("measurement_state_code",stateCode)', "session persists canonical measurement state code")
    req(telemetry_recorder, 'state.put("measurement_valid_channels"', "session persists channel validity")
    req(telemetry_recorder, 'state.put("sensor_power",sensorPower)', "session persists live sensor-power diagnostic")

    # P2 ENV contract: analytical ADS and chamber environment use separate I2C controllers end-to-end.
    req(cfg, "SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49", "ADS analytical bus authority")
    req(cfg, "ENV_SDA=13,ENV_SCL=14", "dedicated ENV bus authority")
    req(hw, "TwoWire EnvWire(1)", "dedicated ESP32 ENV I2C controller")
    req(hw, "EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)", "ENV bus initialization")
    req(hw, "EnvWire.setClock(Cfg::ENV_I2C_HZ)", "ENV bus clock")
    req(hw, "EnvWire.requestFrom((int)a,(int)n)", "ENV reads use EnvWire")
    req(hw, "b[i]=EnvWire.read()", "ENV data bytes use EnvWire")
    req(hw, "envRd(envAddr,0xF7", "BME/BMP measurement read uses ENV helper")

    # P2 Direct/Remote parity: both paths must converge on the same normalized Android snapshot.
    req(robot_api, "return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight)", "Direct normalized snapshot")
    req(remote_mqtt, 'void publishTelemetry(){String j="{\\\"diag\\\":"+diagJson()+",\\\"data\\\":"+dataJson()', "Remote publishes raw diag+data pair")
    req(remote_connection, 'JSONObject diag=sourceDiag==null?new JSONObject():new JSONObject(sourceDiag.toString())', "Remote preserves and isolates diagnostics before normalization")
    req(remote_connection, 'return RobotProtocolV4.normalize(diag,data,left,right)', "Remote normalized snapshot")
    req(remote_connection, 'command("measure_start"', "Remote measurement-start parity")
    req(remote_connection, 'command("measure_cancel"', "Remote measurement-cancel parity")
    req(remote_connection, 'command("relay"', "Remote MQ power parity")
    req(robot_api, 'getText("/measure?cmd=start"', "Direct measurement-start parity")
    req(robot_api, 'getText("/measure?cmd=cancel"', "Direct measurement-cancel parity")

    # Safety telemetry must survive Android normalization and session persistence.
    req(robot_protocol_v4, 'drive.put("safety_auto_stop",safetyAutoStop)', "auto-stop state survives normalization")
    req(robot_protocol_v4, 'drive.put("forward_hazard",forwardHazard)', "hazard state survives normalization")
    req(robot_protocol_v4, 'drive.put("forward_blocked",forwardBlocked)', "block state survives normalization")
    req(robot_protocol_v4, 'drive.put("forward_block_reason",forwardBlockReason)', "block reason survives normalization")
    req(robot_protocol_v4, 'proximity.put("stop_zone"', "ultrasonic stop-zone survives normalization")
    req(robot_protocol_v4, 'x.put("cliff_warning",cliff)', "IR cliff warning survives normalization")
    req(robot_protocol_v4, 'x.put("ground_detected"', "IR ground state survives normalization")
    req(telemetry_recorder, 'state.put("forward_hazard"', "session persists forward hazard")
    req(telemetry_recorder, 'state.put("forward_blocked"', "session persists actual drive block")

    # Source-integrity guards for files previously affected by literal escape insertion.
    forbid(main_activity, r';\nimport ', "MainActivity literal-newline import corruption")
    forbid(main_activity, r'}\n    @Override', "MainActivity literal-newline method corruption")
    forbid(main_activity, r'pendingFieldSessionId=\"\"', "MainActivity escaped-empty-string corruption")
    forbid(host, r';\n    /**', "HazRobHost literal-newline corruption")
    forbid(archive, r'Exception {\n', "FieldSessionArchive literal-newline corruption")

    # User archive isolation and exact report lineage.
    req(archive, "m.operator_role='USER'", "user archive isolation")
    req(archive, "m.kind LIKE 'field_%'", "field archive isolation")
    forbid(archive, "chemcore_feature_references", "scientific reference isolation")
    req(archive, "FROM experiments WHERE session_id=?", "derived report source backup")
    req(archive, 'w.delete("experiments","session_id=?"', "idempotent report source restore")
    req(report, "measurementSessionIds(fs)", "field report exact measurement binding")
    req(report, "ExperimentQueries.bySessionId(db,sid)", "field report exact experiment binding")
    forbid(report, "rawQuery(", "field report SQL ownership")
    forbid(report, ";\\nimport ", "field report literal-newline source corruption")
    req(experiment_queries, "WHERE session_id=? ORDER BY id", "exact experiment query")
    forbid(report, "ORDER BY ABS", "field report timestamp fallback forbidden")
    forbid(experiment_queries, "ORDER BY ABS", "experiment query timestamp fallback forbidden")
    req(sessions_tab, 'requestFieldSessionExport("pdf",q.id)', "field PDF action")
    req(sessions_tab, 'requestFieldSessionExport("xlsx",q.id)', "field XLSX action")
    req(main_activity, "if(requestCode==EXPORT_FIELD_PDF||requestCode==EXPORT_FIELD_XLSX)", "field export result callback")
    req(main_activity, "FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals)", "field PDF result writer")
    req(main_activity, "FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals)", "field XLSX result writer")

    # Current physical hazard recognition is warning-only; automatic movement blocking is disabled.
    req(cfg, "ULTRA_WARN_CM=10.0f", "collision warning threshold")
    req(cfg, "ULTRA_STOP_CM=5.0f", "collision stop-zone threshold")
    req(cfg, "IR_GROUND_ACTIVE=LOW", "IR ground-detected polarity contract")
    req(cfg, "IR_FAIL_SAFE_STARTUP=true", "IR warning fail-safe startup policy")
    req(hw, "irWarning=Cfg::IR_FAIL_SAFE_STARTUP,irGroundDetected=false", "IR startup warning gate")
    req(hw, "Alerts::setIr(irWarning||ultraWarning)", "hazard warnings remain active")
    req(hw, "forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop)", "drive interlock remains gated by disabled auto-stop authority")
    req(hw, "forward_hazard", "hazard telemetry independent of blocking")
    req(hw, "safety_auto_stop", "auto-stop telemetry")
    req(hw, "forward_blocked", "actual block telemetry")
    req(safety_options, 'safetyNv.begin("hazsafe",false)', "dedicated persistent safety namespace")
    req(safety_options, 'safetyNv.putBool("autoStop",false)', "persisted auto-stop forced off")
    req(safety_options, "return false;", "runtime auto-stop authority forced off")
    req(ino, 'web.on("/safety/auto_stop"', "Direct compatibility auto-stop endpoint")
    req(ino, "warnings_active", "Direct warnings remain active")
    req(ino, "estop_independent", "Direct E-STOP independence declaration")
    req(ino, "watchdog_independent", "Direct watchdog independence declaration")
    req(remote_mqtt, 't=="safety_auto_stop_get"', "Remote compatibility auto-stop read command")
    req(remote_mqtt, 't=="safety_auto_stop_set"', "Remote compatibility auto-stop write command")
    req(robot_api, "safetyAutoStopStatus()", "Android auto-stop read facade")
    req(robot_api, "setSafetyAutoStop(boolean enabled)", "Android compatibility auto-stop write facade")
    req(settings_tab, "التوقف التلقائي للحركة: ملغي — تنبيه فقط", "operator warning-only safety setting")
    req(settings_tab, "حساسا IR وEcho يعرضان التحذيرات فقط ولا يمنعان حركة المركبة", "operator warning-only explanation")

    print("HazRob 0.10.2 / HR069 canonical release contract: PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob 0.10.2 / HR069 canonical release contract: FAIL: {exc}")
        raise SystemExit(1)
