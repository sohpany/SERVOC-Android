#!/usr/bin/env python3
"""Fail-closed source contract for HazRob 0.10.1 HR-0101-034 motion control.

This audit prevents reintroduction of retired acceleration/retrace/link-hold motion
policy while preserving the independent stale-command neutral stop and explicit E-stop.
It is a source contract only; it does not replace wheels-up or remote field testing.
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
    limiter = read("app/src/main/java/com/servoc/app/control/DriveSpeedLimiter.java")
    control = read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    status = read("app/src/main/java/com/servoc/app/ui/StatusTab.java")
    remote = read("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")

    # Firmware normal motion is direct command -> PWM. Retired shaping/retrace code must be gone.
    require(cfg, "CONTROL_COMMAND_TIMEOUT_MS=1500", "bounded stale-command policy")
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
    require(hw, "link health: diagnostic only", "link/motion decoupling")
    forbid(hw, "resetMeasurement();relayRaw(false);beginRetrace", "link-loss analytical side effects")

    # E-stop remains a separate explicit latch and is not part of speed shaping.
    require(safety, "void safetyLatchEstop(){estopLatched=true;}", "software E-stop latch")
    require(master, 'path=="/estop"', "Direct E-stop")
    require(master, 'path=="/estop/reset"', "Direct E-stop reset")
    require(mqtt, 't=="estop"', "Remote E-stop")
    require(mqtt, 't=="estop_reset"', "Remote E-stop reset")

    # Android operator input is full-scale by default and linear after a small dead zone.
    require(limiter, "DEFAULT_PERCENT = 100", "operator default speed")
    require(joystick, "DEAD_ZONE = 0.03", "joystick dead zone")
    forbid(joystick, "z *= z", "joystick quadratic shaping")
    require(control, '"السرعة "+DriveSpeedLimiter.DEFAULT_PERCENT+"%"', "speed badge default")
    forbid(control, '"السرعة 50%"', "retired 50-percent badge")
    forbid(control, '"RETRACE"', "retired retrace UI")
    forbid(control, '"LINK LOSS"', "retired link-hold UI")
    require(control, 'x.optBoolean("stale_stop",false)', "stale-stop UI")
    forbid(status, 'optBoolean("lost"', "retired link-loss status")

    # Joystick changes dispatch immediately and a 20 Hz stream refreshes the current command.
    require(main_activity, "private void queueDriveSend()", "immediate drive dispatcher")
    require(main_activity, "queueDriveSend();", "drive-command immediate dispatch")
    require(main_activity, "0,50,TimeUnit.MILLISECONDS", "20 Hz drive refresh")
    forbid(main_activity, "private void stopOperatorMotion()", "retired automatic-stop helper")
    forbid(main_activity, "stopCamera();stopOperatorMotion();", "tab-change auto stop")
    forbid(main_activity, "controlTab.stopCamera();stopOperatorMotion();", "lifecycle auto stop")
    forbid(main_activity, "10 Hz operator command cadence", "retired 10 Hz scheduler")
    forbid(main_activity, 'optBoolean("retrace"', "retired retrace status")
    forbid(main_activity, 'optBoolean("hold"', "retired link-hold status")

    # Remote freshness/replay protections remain; they do not shape motor speed.
    require(remote, 'command("drive",p,700,false,0)', "Remote fire-and-forget drive")
    require(mqtt, "if(!fresh(j))", "Remote TTL freshness")
    require(mqtt, "if(q<=lastSeq)", "Remote sequence rejection")

    print("HazRob 0.10.1 HR-0101-034 motion contract: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
