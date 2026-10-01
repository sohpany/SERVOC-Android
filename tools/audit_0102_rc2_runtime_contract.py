#!/usr/bin/env python3
"""Current fail-closed runtime contract for HazRob 0.10.2 / HR069 RC2.

Historical audits remain in the repository for provenance. This contract validates the
actual RC2 operator/runtime architecture used for internal APK qualification.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
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


def main() -> int:
    release = read("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    drive_mode = read("app/src/main/java/com/servoc/app/control/DriveMode.java")
    tank = read("app/src/main/java/com/servoc/app/control/DualTankDriveControl.java")
    control = read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    main = read("app/src/main/java/com/servoc/app/MainActivity.java")
    robot_api = read("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    protocol = read("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    physical_ui = read("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java")
    settings = read("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    echo_badge = read("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java")
    r0 = read("app/src/main/java/com/servoc/app/calibration/AcceptedR0Normalizer.java")
    lab_quality = read("app/src/main/java/com/servoc/app/chemcore/ChemCoreLabQuality.java")
    cfg = read("firmware/HazRob/Config.h")
    hw = read("firmware/HazRob/Hardware.cpp")
    master = read("firmware/HazRob/HazRob.ino")
    safety = read("firmware/HazRob/SafetyOptions.cpp")

    # Product/runtime identity.
    require(release, 'VERSION="0.10.2"', "release version")
    require(release, 'ENGINEERING_REVISION="HR069"', "engineering revision")
    require(cfg, 'FW[]="HazRob 0.10.2"', "firmware identity")
    require(cfg, 'API=4', "API4 identity")

    # Current operator safety policy: IR/Echo warn, but never auto-stop motion.
    require(release, "IR_WARNING_ONLY=true", "IR warning-only release flag")
    require(release, "IR_OPTIONAL_AUTO_STOP=false", "IR auto-stop disabled")
    require(release, "ULTRASONIC_OPTIONAL_AUTO_STOP=false", "ultrasonic auto-stop disabled")
    require(safety, 'safetyNv.putBool("autoStop",false)', "persisted auto-stop forced off")
    require(safety, "return false;", "runtime auto-stop forced off")
    require(settings, "التوقف التلقائي للحركة: ملغي — تنبيه فقط", "operator safety UI")
    require(settings, "safetyAutoStop.setEnabled(false)", "operator cannot re-enable auto-stop")
    require(master, 'web.on("/estop"', "Direct E-STOP retained")
    require(cfg, "CONTROL_COMMAND_TIMEOUT_MS=500", "drive watchdog retained")

    # HC-SR04: GPIO contract + interrupt edge capture + telemetry diagnostics.
    require(cfg, "ULTRA_TRIG_PIN=23", "ultrasonic TRIG GPIO")
    require(cfg, "ULTRA_ECHO_PIN=39", "ultrasonic ECHO GPIO")
    require(cfg, "ULTRA_PERIOD=150", "ultrasonic sample period")
    require(cfg, "ULTRA_TIMEOUT_US=25000", "ultrasonic timeout")
    require(hw, "void IRAM_ATTR ultraEchoIsr()", "ultrasonic edge ISR")
    require(hw, "attachInterrupt(digitalPinToInterrupt(Cfg::ULTRA_ECHO_PIN),ultraEchoIsr,CHANGE)", "ultrasonic interrupt installation")
    require(hw, "ultraCm=d*0.01715f", "ultrasonic pulse-to-cm conversion")
    require(hw, '\\"pulse_us\\":', "ultrasonic pulse diagnostics")
    require(hw, '\\"timeouts\\":', "ultrasonic timeout diagnostics")
    require(echo_badge, "ECHO TIMEOUT ×", "operator ultrasonic timeout visibility")
    require(echo_badge, '"ECHO %.1f cm"', "operator ultrasonic distance visibility")

    # Tank/sliders: each slider owns one physical side; two positive sliders = four wheels forward.
    require(drive_mode, 'TANK("tank","مزدوج")', "tank mode")
    require(drive_mode, 'JOYSTICK("joystick","Joystick")', "joystick mode")
    require(drive_mode, 'BUTTONS("buttons","أزرار")', "buttons mode")
    forbid(drive_mode, 'WHEEL(', "retired steering-wheel mode")
    require(tank, "setValue(Math.round((mid-y)*100f/half),true)", "tank top-positive/bottom-negative mapping")
    require(tank, "ACTION_UP||a==MotionEvent.ACTION_CANCEL", "tank release neutral")
    require(control, "tankLeft.setListener(v->{tankLeftRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();})", "left slider independent state")
    require(control, "tankRight.setListener(v->{tankRightRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();})", "right slider independent state")
    require(control, "host.setDriveCommand(DriveSpeedLimiter.scale(tankLeftRaw,speedPercent),DriveSpeedLimiter.scale(tankRightRaw,speedPercent))", "tank left/right command ordering")
    require(main, "setDriveCommand(int l,int r)", "coordinator preserves left/right ordering")
    require(robot_api, 'getText("/drive?l="+left+"&r="+right', "Direct left/right transport ordering")
    require(cfg, "DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32", "left L298N channel")
    require(cfg, "DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27", "right L298N channel")
    require(cfg, "DRIVE_L_INVERT=false,DRIVE_R_INVERT=false", "current field direction mapping")

    # Active R0 authority is app manual override, otherwise accepted field clean-air baseline.
    require(r0, "ManualR0Calibration.get(sensorKey)", "active R0 authority")
    require(lab_quality, "ManualR0Calibration.has(hw.key)", "ChemCore R0 readiness authority")
    require(protocol, "ManualR0Calibration.apply(hw.key,x)", "telemetry R0 normalization")

    # MAX4466 calibration remains intentionally deferred: no fabricated firmware SPL/gain.
    require(hw, '\\"db_spl\\":null', "microphone quantitative SPL deferred")
    require(hw, '\\"calibrated_db_spl\\":false', "microphone not calibrated")
    require(hw, '\\"gain_reference\\":null', "microphone gain unquantified")
    require(hw, "trimmer_endpoint_fixed_unquantified", "microphone trimmer state provenance")
    forbid(hw, "AMP_GAIN=25.0f", "unverified fixed microphone gain")
    require(physical_ui, "dB SPL غير معاير", "operator microphone calibration state")

    # Low-latency command path remains bounded inside the firmware watchdog.
    require(robot_api, "DIRECT_KEEPALIVE_MS=150L", "Direct moving keepalive")
    require(robot_api, "DIRECT_DRIVE_TIMEOUT_MS=250", "Direct drive timeout")
    require(main, "0,50,TimeUnit.MILLISECONDS", "20 Hz drive refresh")

    print("HazRob 0.10.2 / HR069 RC2 runtime contract: PASS")
    print("drive=tank+joystick+buttons; tank=independent L/R; echo=interrupt; auto-stop=off; mic-calibration=deferred")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
