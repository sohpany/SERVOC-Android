#!/usr/bin/env python3
"""Current fail-closed runtime contract for HazRob 0.10.2 / HR069 RC2."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path: str) -> str:
    p = ROOT / path
    if not p.is_file(): raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")

def require(src: str, needle: str, label: str) -> None:
    if needle not in src: raise AssertionError(f"{label}: missing {needle!r}")

def forbid(src: str, needle: str, label: str) -> None:
    if needle in src: raise AssertionError(f"{label}: forbidden {needle!r}")

def main() -> int:
    release=read("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    drive_mode=read("app/src/main/java/com/servoc/app/control/DriveMode.java")
    tank=read("app/src/main/java/com/servoc/app/control/DualTankDriveControl.java")
    wheel=read("app/src/main/java/com/servoc/app/control/SteeringWheelMixer.java")
    wheel_view=read("app/src/main/java/com/servoc/app/control/SteeringWheelView.java")
    estop_policy=read("app/src/main/java/com/servoc/app/control/EmergencyStopPressPolicy.java")
    control=read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    main=read("app/src/main/java/com/servoc/app/MainActivity.java")
    robot_api=read("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    protocol=read("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    physical_ui=read("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java")
    settings=read("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    echo_badge=read("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java")
    r0=read("app/src/main/java/com/servoc/app/calibration/AcceptedR0Normalizer.java")
    lab_quality=read("app/src/main/java/com/servoc/app/chemcore/ChemCoreLabQuality.java")
    cfg=read("firmware/HazRob/Config.h")
    hw=read("firmware/HazRob/Hardware.cpp")
    master=read("firmware/HazRob/HazRob.ino")
    safety=read("firmware/HazRob/SafetyOptions.cpp")

    require(release,'VERSION="0.10.2"',"release version")
    require(release,'ENGINEERING_REVISION="HR069"',"engineering revision")
    require(cfg,'FW[]="HazRob 0.10.2"',"firmware identity")
    require(cfg,'API=4',"API4 identity")

    require(release,"IR_WARNING_ONLY=true","IR warning-only release flag")
    require(release,"IR_OPTIONAL_AUTO_STOP=false","IR auto-stop disabled")
    require(release,"ULTRASONIC_OPTIONAL_AUTO_STOP=false","ultrasonic auto-stop disabled")
    require(safety,'safetyNv.putBool("autoStop",false)',"persisted auto-stop forced off")
    require(safety,"return false;","runtime auto-stop forced off")
    require(settings,"التوقف التلقائي للحركة: ملغي — تنبيه فقط","operator safety UI")
    require(settings,"safetyAutoStop.setEnabled(false)","operator cannot re-enable auto-stop")
    require(master,'web.on("/estop"',"Direct E-STOP retained")
    require(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","drive watchdog retained")

    require(cfg,"ULTRA_TRIG_PIN=23","ultrasonic TRIG GPIO")
    require(cfg,"ULTRA_ECHO_PIN=39","ultrasonic ECHO GPIO")
    require(cfg,"ULTRA_PERIOD=150","ultrasonic sample period")
    require(cfg,"ULTRA_TIMEOUT_US=25000","ultrasonic timeout")
    require(hw,"void IRAM_ATTR ultraEchoIsr()","ultrasonic edge ISR")
    require(hw,"attachInterrupt(digitalPinToInterrupt(Cfg::ULTRA_ECHO_PIN),ultraEchoIsr,CHANGE)","ultrasonic interrupt installation")
    require(hw,"ultraCm=d*0.01715f","ultrasonic pulse-to-cm conversion")
    require(hw,'\\"pulse_us\\":',"ultrasonic pulse diagnostics")
    require(hw,'\\"timeouts\\":',"ultrasonic timeout diagnostics")
    require(echo_badge,"ECHO TIMEOUT ×","operator ultrasonic timeout visibility")
    require(echo_badge,'"ECHO %.1f cm"',"operator ultrasonic distance visibility")

    # Four operator drive presentations.
    require(drive_mode,'TANK("tank","مزدوج")',"tank mode")
    require(drive_mode,'JOYSTICK("joystick","Joystick")',"joystick mode")
    require(drive_mode,'WHEEL("wheel","العجلة")',"wheel mode")
    require(drive_mode,'BUTTONS("buttons","أزرار")',"buttons mode")
    require(tank,"setValue(Math.round((mid-y)*100f/half),true)","tank analog speed mapping")
    require(control,"host.setDriveCommand(tankLeftRaw,tankRightRaw)","tank direct independent speed authority")
    require(control,"driveMode==DriveMode.WHEEL","wheel exposed in control UI")
    require(control,"wheel.reset()","wheel release/reset contract")
    require(wheel,"if(x>0)return new DriveMixer.Command(pct(slowed),pct(full))","wheel right slows left side")
    require(wheel,"if(x<0)return new DriveMixer.Command(pct(full),pct(slowed))","wheel left slows right side")
    require(wheel,"MIN_SLOWED_RATIO = 0.40","wheel 40 percent inner floor")
    require(wheel,"PIVOT_MAX = 0.50","wheel 50 percent pivot cap")
    require(wheel_view,"SteeringWheelMixer.mix(x,y)","wheel view uses dedicated requested mixer")

    # E-STOP: 2 s hold to latch, ordinary tap to reset once latched.
    require(estop_policy,"ARM_HOLD_MS = 2000L","E-STOP two-second hold")
    require(control,"postDelayed(estopHoldAction,EmergencyStopPressPolicy.ARM_HOLD_MS)","E-STOP hold gesture")
    require(control,"shouldResetOnTap(estopLatched)","E-STOP tap reset gesture")
    require(control,"0xFFC62828","E-STOP latched red")
    require(control,"0xFF2E7D32","E-STOP ready green")

    require(main,"setDriveCommand(int l,int r)","coordinator preserves left/right ordering")
    require(robot_api,'getText("/drive?l="+left+"&r="+right',"Direct left/right transport ordering")
    require(cfg,"DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32","left L298N channel")
    require(cfg,"DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27","right L298N channel")
    require(cfg,"DRIVE_L_INVERT=false,DRIVE_R_INVERT=false","current field direction mapping")

    require(r0,"ManualR0Calibration.get(sensorKey)","active R0 authority")
    require(lab_quality,"ManualR0Calibration.has(hw.key)","ChemCore R0 readiness authority")
    require(protocol,"ManualR0Calibration.apply(hw.key,x)","telemetry R0 normalization")

    require(hw,'\\"db_spl\\":null',"microphone quantitative SPL deferred")
    require(hw,'\\"calibrated_db_spl\\":false',"microphone not calibrated")
    require(hw,'\\"gain_reference\\":null',"microphone gain unquantified")
    require(hw,"trimmer_endpoint_fixed_unquantified","microphone trimmer state provenance")
    forbid(hw,"AMP_GAIN=25.0f","unverified fixed microphone gain")
    require(physical_ui,"dB SPL غير معاير","operator microphone calibration state")

    require(robot_api,"DIRECT_KEEPALIVE_MS=150L","Direct moving keepalive")
    require(robot_api,"DIRECT_DRIVE_TIMEOUT_MS=250","Direct drive timeout")
    require(main,"0,50,TimeUnit.MILLISECONDS","20 Hz drive refresh")

    print("HazRob 0.10.2 / HR069 RC2 runtime contract: PASS")
    print("drive=tank+joystick+wheel+buttons; wheel=right-slows-left/left-slows-right; pivot=50%; estop=hold2s/tap-reset")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
