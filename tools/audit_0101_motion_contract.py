#!/usr/bin/env python3
"""Fail-closed source contract for HazRob 0.10.2 motion control.

This audit prevents reintroduction of retired acceleration/retrace/link-hold shaping while
preserving the independent 500 ms stale-command neutral stop, explicit E-stop, tunable
travel-relative differential steering, low-latency Direct/Remote command paths, the four operator
drive modes, and the current HR069 warning-only IR/ultrasonic policy. Automatic hazard movement
blocking is forced OFF; hazard warnings remain active and E-stop/watchdog remain independent.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path: str) -> str:
    p = ROOT / path
    if not p.is_file(): raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")

def require(src: str, needle: str, label: str) -> None:
    if needle not in src: raise AssertionError(f"{label}: missing {needle!r}")

def forbid(src: str, needle: str, label: str) -> None:
    if needle in src: raise AssertionError(f"{label}: retired marker still present {needle!r}")

def main() -> int:
    cfg=read("firmware/HazRob/Config.h");hw=read("firmware/HazRob/Hardware.cpp");master=read("firmware/HazRob/HazRob.ino");mqtt=read("firmware/HazRob/RemoteMqtt.cpp")
    safety=read("firmware/HazRob/SafetyInterlock.cpp");safety_options=read("firmware/HazRob/SafetyOptions.cpp");main_activity=read("app/src/main/java/com/servoc/app/MainActivity.java")
    joystick=read("app/src/main/java/com/servoc/app/control/JoystickView.java");wheel=read("app/src/main/java/com/servoc/app/control/SteeringWheelView.java");wheel_mixer=read("app/src/main/java/com/servoc/app/control/SteeringWheelMixer.java");direction_pad=read("app/src/main/java/com/servoc/app/control/DirectionPadView.java")
    drive_mode=read("app/src/main/java/com/servoc/app/control/DriveMode.java");discrete=read("app/src/main/java/com/servoc/app/control/DiscreteDrive.java");discrete_test=read("app/src/test/java/com/servoc/app/control/DiscreteDriveTest.java")
    mixer=read("app/src/main/java/com/servoc/app/control/DriveMixer.java");mixer_test=read("app/src/test/java/com/servoc/app/control/DriveMixerTest.java");limiter=read("app/src/main/java/com/servoc/app/control/DriveSpeedLimiter.java")
    robot_api=read("app/src/main/java/com/servoc/app/communication/RobotApi.java");control=read("app/src/main/java/com/servoc/app/ui/ControlTab.java");status=read("app/src/main/java/com/servoc/app/ui/StatusTab.java");remote=read("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")

    require(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","bounded stale-command policy")
    for retired in ("DRIVE_LEASE_MS","LINK_DETECT_MS","LINK_HOLD_MS","RETRACE_MS","RETRACE_MAX_SPEED","ACCEL=","DECEL=","REVERSE="): forbid(cfg,retired,"Config.h")
    require(hw,"void driveSet(int l,int r){","driveSet entry");require(hw,"bool forwardRequested=(requestedL+requestedR)>0.1f;","forward motion classification");require(hw,"bool forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop);","front hazard block remains gated by disabled HR069 auto-stop authority");require(hw,"Alerts::setIr(irWarning||ultraWarning)","hazard warnings independent of auto-stop")
    require(safety_options,'safetyNv.putBool("autoStop",false)',"persisted auto-stop forced off");require(safety_options,'return false;',"runtime auto-stop authority forced off");require(safety_options,'if(persist)safetyNv.putBool("autoStop",false)',"compatibility setter cannot re-enable auto-stop");require(cfg,"ULTRA_WARN_CM=10.0f","ultrasonic warning threshold");require(cfg,"ULTRA_STOP_CM=5.0f","ultrasonic stop-zone telemetry threshold")
    require(hw,"ultraCm=NAN;ultraWarning=false;ultraStop=false;ultraTimeoutCount++;hazardAlert();","ultrasonic timeout clears warning/stop and records timeout");require(hw,"if(forwardBlocked&&forwardRequested){curL=curR=0;}else{curL=requestedL;curR=requestedR;}","forward-only block implementation remains inert unless future authority explicitly enables it");require(hw,"cmdAt=millis();staleStop=false;driveWrite();","drive watchdog refresh");require(hw,"n-cmdAt>Cfg::CONTROL_COMMAND_TIMEOUT_MS","stale stop timeout");require(hw,"staleStop=true;driveWrite();","stale stop neutral output")
    for retired in ("float ramp(","beginRetrace","retraceUpdate","historyUpdate","clearHistory","struct Hist","linkWatchUpdate","beginLinkHold","recoverLink","linkHold","linkLost","retracing","RETRACE","failsafe","hold_remaining_ms"): forbid(hw,retired,"Hardware.cpp")

    require(safety,"void safetyLatchEstop(){estopLatched=true;}","software E-stop latch");require(master,'web.on("/estop",[]{safetyLatchEstop();emergencyStop();',"Direct E-stop");require(master,'web.on("/estop/reset",[]{motionStop();safetyResetEstop();',"Direct E-stop reset");require(mqtt,'t=="estop"',"Remote E-stop");require(mqtt,'t=="estop_reset"',"Remote E-stop reset")

    require(limiter,"DEFAULT_PERCENT = 100","operator default speed")
    forbid(joystick,"DEAD_ZONE","joystick software dead zone");require(joystick,"magnitude <= 1.0e-6","joystick center guard");require(joystick,"requestDisallowInterceptTouchEvent(true)","joystick touch ownership");require(joystick,"DriveMixer.mix(x, y, turn, pivot)","joystick canonical tunable mixer");require(joystick,"resetInternal(true)","joystick release neutral");forbid(joystick,"z *= z","joystick quadratic shaping")
    require(wheel,"SteeringWheelMixer.mix(x,y,turn,pivot)","wheel delegates through canonical tunable mixer wrapper");require(wheel,"resetInternal(true)","wheel release neutral");require(wheel_mixer,"return DriveMixer.mix(x,y,turnPercent,pivotPercent)","wheel mixer delegates to DriveMixer")
    require(drive_mode,'TANK("tank","مزدوج")',"tank mode");require(drive_mode,'WHEEL("wheel","العجلة")',"wheel mode");require(drive_mode,'JOYSTICK("joystick","Joystick")',"joystick mode");require(drive_mode,'BUTTONS("buttons","أزرار")',"buttons mode")
    require(direction_pad,"ACTION_DOWN","buttons press-and-hold start");require(direction_pad,"ACTION_UP||a==MotionEvent.ACTION_CANCEL","buttons release/cancel stop")
    for marker in ("FORWARD,100,100","REVERSE,-100,-100","PIVOT_RIGHT,60,-60","PIVOT_LEFT,-60,60","FORWARD_RIGHT,100,40","FORWARD_LEFT,40,100","REVERSE_RIGHT,-100,-40","REVERSE_LEFT,-40,-100","STOP,0,0"): require(discrete_test,marker,"discrete drive regression")
    require(discrete,"case FORWARD_RIGHT:return new DriveMixer.Command(100,turn)","tunable forward-right command");require(discrete,"case FORWARD_LEFT:return new DriveMixer.Command(turn,100)","tunable forward-left command");require(discrete,"case PIVOT_RIGHT:return new DriveMixer.Command(pivot,-pivot)","tunable pivot-right command");require(discrete,"case PIVOT_LEFT:return new DriveMixer.Command(-pivot,pivot)","tunable pivot-left command")

    require(mixer,"TRANSLATION_DEADZONE = 0.08","translation deadzone authority");require(mixer,"double pivot=x*pivotMax;","bounded tunable pivot mix");require(mixer,"double ratio=1.0-(1.0-minInnerRatio)*Math.abs(x);","progressive tunable inside-side ratio");require(mixer,"double full=y;","travel magnitude authority");require(mixer,"double slowed=y*ratio;","inside-side slowdown");require(mixer,"if(x>0)return new Command(pct(full),pct(slowed));","right steering slows right side");require(mixer,"if(x<0)return new Command(pct(slowed),pct(full));","left steering slows left side")
    for marker in ("defaultCardinalTravelAndPivotArePredictable","defaultRightSteeringSlowsRightSideProgressively","defaultLeftSteeringSlowsLeftSideProgressively","defaultFullSteeringLockKeepsSlowedSideAtFortyPercent","reverseKeepsSameScreenDirectionRule","defaultPureHorizontalInputIsLimitedToSixtyPercentPivot","operatorTurnPercentControlsInnerSideAtFullLock","operatorPivotPercentControlsInPlaceRotation","tuningValuesAreClampedToPercentDomain","inputsAreBoundedToUnitCommandDomain"): require(mixer_test,marker,"drive steering/tuning regression")

    require(control,'"السرعة "+DriveSpeedLimiter.DEFAULT_PERCENT+"%"',"speed badge default");require(control,"controlSize=UiKit.dp(c,landscape?170:180)","landscape/portrait drive control sizing");require(control,"driveMode=driveMode.next()","four-mode cycle");require(control,"DriveMode.TANK","tank UI");require(control,"DriveMode.WHEEL","wheel UI");require(control,"DriveMode.JOYSTICK","joystick UI");require(control,"DriveMode.BUTTONS","buttons UI");require(control,"applyLandscapeFullscreenChrome","landscape driving fullscreen")
    forbid(control,'"السرعة 50%"',"retired 50-percent badge");forbid(control,'"RETRACE"',"retired retrace UI");forbid(control,'"LINK LOSS"',"retired link-hold UI");require(control,'x.optBoolean("stale_stop",false)',"stale-stop UI");forbid(status,'optBoolean("lost"',"retired link-loss status")

    require(robot_api,"DIRECT_KEEPALIVE_MS=150L","Direct moving keepalive");require(robot_api,"DIRECT_DRIVE_TIMEOUT_MS=250","Direct request timeout");require(main_activity,"private void queueDriveSend()","immediate drive dispatcher");require(main_activity,"queueDriveSend();","drive-command immediate dispatch");require(main_activity,"driveDirty.set(true)","latest touch command retention");require(main_activity,"while(entered&&driveDirty.get())","no dropped touch update loop");require(main_activity,"0,50,TimeUnit.MILLISECONDS","20 Hz drive refresh")
    forbid(main_activity,"private void stopOperatorMotion()","retired automatic-stop helper");forbid(main_activity,"10 Hz operator command cadence","retired 10 Hz scheduler");forbid(main_activity,'optBoolean("retrace"',"retired retrace status");forbid(main_activity,'optBoolean("hold"',"retired link-hold status")
    require(remote,'p.put("left",left);p.put("right",right);command("drive",p,RemoteDrivePolicy.COMMAND_TTL_MS,false,0);',"Remote fire-and-forget drive");require(mqtt,"if(!fresh(j))","Remote TTL freshness");require(mqtt,"if(q<=lastSeq)","Remote sequence rejection")

    print("HazRob 0.10.2 motion contract: PASS")
    print("modes=tank,wheel,joystick,buttons; watchdog=500ms; direct keepalive=150ms; tunable turn/pivot; hazard auto-stop=FORCED-OFF; warnings always active; physical acceptance=PENDING")
    return 0

if __name__ == "__main__": raise SystemExit(main())