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
    drive_mixer=read("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    drive_tuning=read("app/src/main/java/com/servoc/app/control/DriveTuning.java")
    direction=read("app/src/main/java/com/servoc/app/control/DriveDirectionOption.java")
    tank=read("app/src/main/java/com/servoc/app/control/DualTankDriveControl.java")
    wheel=read("app/src/main/java/com/servoc/app/control/SteeringWheelMixer.java")
    wheel_view=read("app/src/main/java/com/servoc/app/control/SteeringWheelView.java")
    joy_view=read("app/src/main/java/com/servoc/app/control/JoystickView.java")
    dpad=read("app/src/main/java/com/servoc/app/control/DirectionPadView.java")
    discrete=read("app/src/main/java/com/servoc/app/control/DiscreteDrive.java")
    estop_policy=read("app/src/main/java/com/servoc/app/control/EmergencyStopPressPolicy.java")
    control=read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    main=read("app/src/main/java/com/servoc/app/MainActivity.java")
    robot_api=read("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    protocol=read("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    physical_ui=read("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java")
    settings=read("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    mic_cal=read("app/src/main/java/com/servoc/app/calibration/MicrophoneDbCalibration.java")
    echo_badge=read("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java")
    r0=read("app/src/main/java/com/servoc/app/calibration/AcceptedR0Normalizer.java")
    lab_quality=read("app/src/main/java/com/servoc/app/chemcore/ChemCoreLabQuality.java")
    cfg=read("firmware/HazRob/Config.h")
    hw=read("firmware/HazRob/Hardware.cpp")
    master=read("firmware/HazRob/HazRob.ino")
    safety=read("firmware/HazRob/SafetyOptions.cpp")

    require(release,'VERSION="0.10.2"',"release version")
    require(release,'ENGINEERING_REVISION="HR069-R1"',"engineering revision")
    require(cfg,'FW[]="HazRob 0.10.2"',"firmware identity")
    require(cfg,'API=4',"API4 identity")

    # IR is a downward cliff sensor: reflection/ground is SAFE; missing reflection is the warning.
    require(release,"IR_WARNING_ONLY=false","IR warning-only release flag")
    require(release,"IR_OPTIONAL_AUTO_STOP=true","IR auto-stop disabled")
    require(cfg,"IR_GROUND_ACTIVE=HIGH","current physical IR ground-active polarity")
    require(hw,"bool groundNow=irLevel==Cfg::IR_GROUND_ACTIVE","IR active signal means ground present")
    require(hw,"irGroundDetected=true;irWarning=false","IR reflection/ground clears cliff warning")
    require(hw,"irGroundDetected=false;irWarning=true","IR missing reflection asserts cliff warning")
    require(hw,'\\"ground_detected\\":"+String(bj(irGroundDetected))',"IR ground telemetry")
    require(hw,'\\"cliff_warning\\":"+String(bj(irWarning))',"IR cliff telemetry")
    require(release,"ULTRASONIC_OPTIONAL_AUTO_STOP=true","ultrasonic auto-stop disabled")
    require(safety,'safetyNv.putBool("autoStop",enabled)',"persisted auto-stop forced off")
    require(safety,"return autoStop;","runtime auto-stop forced off")
    require(settings,"التوقف التلقائي للأمام (IR + Echo)","operator safety UI")
    require(settings,"private void applySafetyAutoStop(boolean enabled)","operator cannot re-enable auto-stop")
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

    # Four operator drive presentations with independent direction reversal except tank.
    require(drive_mode,'TANK("tank","مزدوج")',"tank mode")
    require(drive_mode,'JOYSTICK("joystick","Joystick")',"joystick mode")
    require(drive_mode,'WHEEL("wheel","العجلة")',"wheel mode")
    require(drive_mode,'BUTTONS("buttons","أزرار")',"buttons mode")
    require(tank,"setValue(Math.round((mid-y)*100f/half),true)","tank analog speed mapping")
    require(control,"host.setDriveCommand(tankLeftRaw,tankRightRaw)","tank direct independent speed authority")
    require(control,"driveMode==DriveMode.WHEEL","wheel exposed in control UI")
    require(control,"wheel.reset()","wheel release/reset contract")
    require(direction,"PREF_REVERSE_JOYSTICK","joystick reversal preference")
    require(direction,"PREF_REVERSE_WHEEL","wheel reversal preference")
    require(direction,"PREF_REVERSE_BUTTONS","button reversal preference")
    require(direction,"mode == DriveMode.JOYSTICK || mode == DriveMode.WHEEL || mode == DriveMode.BUTTONS","tank cannot be reversed")
    require(settings,'"العجلة: عكس التوجيه يمين/يسار"',"wheel reversal setting")
    require(settings,'"Joystick: عكس التوجيه يمين/يسار"',"joystick reversal setting")
    require(settings,'"الأزرار: عكس التوجيه يمين/يسار"',"button reversal setting")

    # Turn and pivot speed are operator tunable; defaults remain 40% / 60%.
    require(drive_tuning,"DEFAULT_TURN_PERCENT = 40","default moving-turn inner-side speed")
    require(drive_tuning,"DEFAULT_PIVOT_PERCENT = 60","default in-place pivot speed")
    require(settings,'"سرعة الالتفاف (%)"',"manual turn-speed field")
    require(settings,'"سرعة الدوران في المكان (%)"',"manual pivot-speed field")
    require(settings,'"حفظ سرعات القيادة"',"drive-tuning save button")
    require(settings,"putInt(DriveTuning.PREF_TURN_PERCENT,turn)","turn speed persistence")
    require(settings,"putInt(DriveTuning.PREF_PIVOT_PERCENT,pivot)","pivot speed persistence")
    require(drive_mixer,"mix(double x,double y,int turnPercent,int pivotPercent)","runtime tunable differential mixer")
    require(joy_view,"DriveMixer.mix(x, y, turn, pivot)","joystick consumes runtime tuning")
    require(wheel,"DriveMixer.mix(x,y,turnPercent,pivotPercent)","wheel mixer passes runtime tuning")
    require(wheel_view,"SteeringWheelMixer.mix(x,y,turn,pivot)","wheel view consumes runtime tuning")
    require(dpad,"DiscreteDrive.command(i,turn,pivot)","buttons consume runtime tuning")
    require(discrete,"PIVOT_RIGHT:return new DriveMixer.Command(pivot,-pivot)","tunable right pivot")
    require(discrete,"FORWARD_RIGHT:return new DriveMixer.Command(100,turn)","tunable moving right turn")

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

    # MAX4466: firmware retains raw electrical telemetry; Android applies one-point reference-meter offset.
    require(hw,'\\"db_spl\\":null',"firmware does not claim unverified SPL")
    require(hw,'\\"calibrated_db_spl\\":false',"firmware SPL remains unclaimed")
    require(hw,'\\"gain_reference\\":null',"microphone gain unquantified")
    require(hw,"trimmer_endpoint_fixed_unquantified","microphone trimmer state provenance")
    forbid(hw,"AMP_GAIN=25.0f","unverified fixed microphone gain")
    require(mic_cal,"20.0*Math.log10(ratio)","raw RMS-to-dBFS basis")
    require(mic_cal,"referenceDb-hazRobRawDb","reference-meter offset equation")
    require(mic_cal,"raw+offsetDb(p)","saved microphone offset application")
    require(settings,"تعويض الميكروفون (dB)","operator microphone offset field")
    require(settings,"قراءة جهاز القياس المعاير − قراءة HazRob الخام","operator microphone offset instructions")
    require(physical_ui,"dBFS خام","operator raw microphone calibration state")
    require(physical_ui,"معوض بمرجع خارجي","operator compensated microphone state")
    forbid(physical_ui,"25×","stale fixed microphone gain display")

    require(robot_api,"DIRECT_KEEPALIVE_MS=150L","Direct moving keepalive")
    require(robot_api,"DIRECT_DRIVE_TIMEOUT_MS=250","Direct drive timeout")
    require(main,"0,50,TimeUnit.MILLISECONDS","20 Hz drive refresh")

    print("HazRob 0.10.2 / HR069 RC2 runtime contract: PASS")
    print("drive=tank-direct + joystick/wheel/buttons with independent reversal; turn/pivot=user-tunable 40/60 defaults; IR=ground-safe/no-ground-warning; estop=hold2s/tap-reset; mic=reference-offset")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
