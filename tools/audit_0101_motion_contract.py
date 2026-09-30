#!/usr/bin/env python3
"""Fail-closed source contract for HazRob 0.10.2 motion control.

This audit prevents reintroduction of retired acceleration/retrace/link-hold shaping while
preserving the independent 500 ms stale-command neutral stop, explicit E-stop, travel-relative
differential steering, low-latency Direct/Remote command paths, and the current operator-selectable
IR/ultrasonic forward auto-stop policy. Hazard warnings remain active regardless of auto-stop state.
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
        raise AssertionError(f"{label}: retired marker still present {needle!r}")


def main() -> int:
    cfg = read("firmware/HazRob/Config.h")
    hw = read("firmware/HazRob/Hardware.cpp")
    master = read("firmware/HazRob/HazRob.ino")
    mqtt = read("firmware/HazRob/RemoteMqtt.cpp")
    safety = read("firmware/HazRob/SafetyInterlock.cpp")
    safety_options = read("firmware/HazRob/SafetyOptions.cpp")
    main_activity = read("app/src/main/java/com/servoc/app/MainActivity.java")
    joystick = read("app/src/main/java/com/servoc/app/control/JoystickView.java")
    mixer = read("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    mixer_test = read("app/src/test/java/com/servoc/app/control/DriveMixerTest.java")
    limiter = read("app/src/main/java/com/servoc/app/control/DriveSpeedLimiter.java")
    robot_api = read("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    control = read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    status = read("app/src/main/java/com/servoc/app/ui/StatusTab.java")
    remote = read("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")

    require(cfg, "CONTROL_COMMAND_TIMEOUT_MS=500", "bounded stale-command policy")
    for retired in (
        "DRIVE_LEASE_MS", "LINK_DETECT_MS", "LINK_HOLD_MS", "RETRACE_MS",
        "RETRACE_MAX_SPEED", "ACCEL=", "DECEL=", "REVERSE=",
    ):
        forbid(cfg, retired, "Config.h")

    require(hw, "void driveSet(int l,int r){", "driveSet entry")
    require(hw, "bool forwardRequested=(requestedL+requestedR)>0.1f;", "forward motion classification")
    require(hw, "bool forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop);", "operator-selectable front hazard interlock")
    require(hw, "Alerts::setIr(irWarning||ultraWarning)", "hazard warnings independent of auto-stop")
    require(safety_options, 'getBool("autoStop",false)', "auto-stop default off")
    require(safety_options, 'putBool("autoStop",enabled)', "auto-stop persisted by operator choice")
    require(cfg, "ULTRA_WARN_CM=10.0f", "ultrasonic warning threshold")
    require(cfg, "ULTRA_STOP_CM=5.0f", "ultrasonic forward-stop threshold")
    require(hw, "ultraWarning=false;ultraStop=false;ultraState=0;hazardAlert();", "ultrasonic timeout auto-clear")
    require(hw, "if(forwardBlocked&&forwardRequested){curL=curR=0;}else{curL=requestedL;curR=requestedR;}", "forward-only hazard block")
    require(hw, "cmdAt=millis();staleStop=false;driveWrite();", "drive watchdog refresh")
    require(hw, "n-cmdAt>Cfg::CONTROL_COMMAND_TIMEOUT_MS", "stale stop timeout")
    require(hw, "staleStop=true;driveWrite();", "stale stop neutral output")
    for retired in (
        "float ramp(", "beginRetrace", "retraceUpdate", "historyUpdate", "clearHistory",
        "struct Hist", "linkWatchUpdate", "beginLinkHold", "recoverLink", "linkHold",
        "linkLost", "retracing", "RETRACE", "failsafe", "hold_remaining_ms",
    ):
        forbid(hw, retired, "Hardware.cpp")

    require(safety, "void safetyLatchEstop(){estopLatched=true;}", "software E-stop latch")
    require(master, 'web.on("/estop",[]{safetyLatchEstop();emergencyStop();', "Direct E-stop")
    require(master, 'web.on("/estop/reset",[]{motionStop();safetyResetEstop();', "Direct E-stop reset")
    require(mqtt, 't=="estop"', "Remote E-stop")
    require(mqtt, 't=="estop_reset"', "Remote E-stop reset")

    require(limiter, "DEFAULT_PERCENT = 100", "operator default speed")
    forbid(joystick, "DEAD_ZONE", "joystick software dead zone")
    require(joystick, "magnitude <= 1.0e-6", "center numerical guard")
    require(joystick, "requestDisallowInterceptTouchEvent(true)", "touch ownership")
    require(joystick, "DriveMixer.mix(x, y)", "joystick uses canonical mixer")
    require(joystick, "resetInternal(true)", "touch release/cancel neutral")
    forbid(joystick, "z *= z", "joystick quadratic shaping")
    require(mixer, "double steer=y<0?-x:x;", "travel-relative steering contribution")
    require(mixer, "double left=y+steer;", "left differential mix")
    require(mixer, "double right=y-steer;", "right differential mix")
    require(mixer, "Math.max(1.0,Math.max(Math.abs(left),Math.abs(right)))", "full-scale normalization")
    for marker in (
        "purePivotUsesCounterRotation",
        "forwardSteeringSlowsInsidePhysicalSide",
        "reverseSteeringUsesSameInsidePhysicalSide",
        "samePhysicalSideIsInsideForwardAndReverse",
    ):
        require(mixer_test, marker, "drive steering regression")
    require(control, '"السرعة "+DriveSpeedLimiter.DEFAULT_PERCENT+"%"', "speed badge default")
    require(control, "Math.round(baseJoystickDp*1.20f)", "20 percent larger joystick")
    forbid(control, '"السرعة 50%"', "retired 50-percent badge")
    forbid(control, '"RETRACE"', "retired retrace UI")
    forbid(control, '"LINK LOSS"', "retired link-hold UI")
    require(control, 'x.optBoolean("stale_stop",false)', "stale-stop UI")
    forbid(status, 'optBoolean("lost"', "retired link-loss status")

    require(robot_api, "DIRECT_KEEPALIVE_MS=150L", "Direct moving keepalive")
    require(robot_api, "DIRECT_DRIVE_TIMEOUT_MS=250", "Direct request timeout")
    require(main_activity, "private void queueDriveSend()", "immediate drive dispatcher")
    require(main_activity, "queueDriveSend();", "drive-command immediate dispatch")
    require(main_activity, "driveDirty.set(true)", "latest touch command retention")
    require(main_activity, "while(entered&&driveDirty.get())", "no dropped touch update loop")
    require(main_activity, "0,50,TimeUnit.MILLISECONDS", "20 Hz drive refresh")
    forbid(main_activity, "private void stopOperatorMotion()", "retired automatic-stop helper")
    forbid(main_activity, "10 Hz operator command cadence", "retired 10 Hz scheduler")
    forbid(main_activity, 'optBoolean("retrace"', "retired retrace status")
    forbid(main_activity, 'optBoolean("hold"', "retired link-hold status")

    require(remote, 'p.put("left",left);p.put("right",right);command("drive",p,RemoteDrivePolicy.COMMAND_TTL_MS,false,0);', "Remote fire-and-forget drive")
    require(mqtt, "if(!fresh(j))", "Remote TTL freshness")
    require(mqtt, "if(q<=lastSeq)", "Remote sequence rejection")

    print("HazRob 0.10.2 motion contract: PASS")
    print("watchdog=500ms; direct keepalive=150ms; direct timeout=250ms; optional hazard auto-stop; warnings always active; physical acceptance=PENDING")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
