#!/usr/bin/env python3
"""Fail-closed source contract for HazRob 0.10.1 motion control.

This audit prevents reintroduction of retired acceleration/retrace/link-hold shaping while
preserving the independent 500 ms stale-command neutral stop, explicit E-stop, travel-relative
differential steering, and the low-latency Direct/Remote command paths.
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

    require(hw, "void driveSet(int l,int r){curL=lim(l);curR=lim(r);cmdAt=millis();staleStop=false;driveWrite();}", "direct driveSet")
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
    require(joystick, "DEAD_ZONE = 0.025", "joystick dead zone")
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
    forbid(control, '"السرعة 50%"', "retired 50-percent badge")
    forbid(control, '"RETRACE"', "retired retrace UI")
    forbid(control, '"LINK LOSS"', "retired link-hold UI")
    require(control, 'x.optBoolean("stale_stop",false)', "stale-stop UI")
    forbid(status, 'optBoolean("lost"', "retired link-loss status")

    require(robot_api, "DIRECT_KEEPALIVE_MS=150L", "Direct moving keepalive")
    require(robot_api, "DIRECT_DRIVE_TIMEOUT_MS=250", "Direct request timeout")
    require(main_activity, "private void queueDriveSend()", "immediate drive dispatcher")
    require(main_activity, "queueDriveSend();", "drive-command immediate dispatch")
    require(main_activity, "0,50,TimeUnit.MILLISECONDS", "20 Hz drive refresh")
    forbid(main_activity, "private void stopOperatorMotion()", "retired automatic-stop helper")
    forbid(main_activity, "10 Hz operator command cadence", "retired 10 Hz scheduler")
    forbid(main_activity, 'optBoolean("retrace"', "retired retrace status")
    forbid(main_activity, 'optBoolean("hold"', "retired link-hold status")

    require(remote, 'p.put("left",left);p.put("right",right);command("drive",p,RemoteDrivePolicy.COMMAND_TTL_MS,false,0);', "Remote fire-and-forget drive")
    require(mqtt, "if(!fresh(j))", "Remote TTL freshness")
    require(mqtt, "if(q<=lastSeq)", "Remote sequence rejection")

    print("HazRob 0.10.1 motion contract: PASS")
    print("watchdog=500ms; direct keepalive=150ms; direct timeout=250ms; travel-relative physical-side steering; physical acceptance=PENDING")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
