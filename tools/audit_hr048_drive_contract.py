#!/usr/bin/env python3
"""HR-0101-048 steering/safety contract, carried forward under HR-0101-056.

HR048 originally added a capability-gated UDP fast lane after the 2026-09-14 steering finding.
HR056 deliberately retires that local UDP transport and restores the proven Direct HTTP authority
while preserving the HR048 steering mapping, watchdog and Remote drive policy. Physical steering
acceptance remains a post-flash gate.
"""
from pathlib import Path
import os

ROOT=Path(__file__).resolve().parents[1]

def txt(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,s,label):
    if s not in src: raise AssertionError(f"{label}: missing {s!r}")
def forbid(src,s,label):
    if s in src: raise AssertionError(f"{label}: forbidden {s!r}")

def mix(x,y):
    l=y+x; r=y-x; q=max(1.0,abs(l),abs(r));
    return round(l/q*100),round(r/q*100)

def main():
    cfg=txt("firmware/HazRob/Config.h")
    hw=txt("firmware/HazRob/Hardware.cpp")
    ino=txt("firmware/HazRob/HazRob.ino")
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")
    api=txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    remote_policy=txt("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    profile=txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")

    req(joy,"double l = y + x, r = y - x;","joystick differential mixer")
    l,r=mix(.25,.75)
    if not (l>r>0): raise AssertionError(f"forward-right mix invalid: L={l}, R={r}")
    l,r=mix(-.25,.75)
    if not (r>l>0): raise AssertionError(f"forward-left mix invalid: L={l}, R={r}")
    if mix(1.0,0.0)!=(100,-100): raise AssertionError("right pivot mix invalid")
    if mix(-1.0,0.0)!=(-100,100): raise AssertionError("left pivot mix invalid")

    req(cfg,"DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32","logical LEFT -> L298N B")
    req(cfg,"DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27","logical RIGHT -> L298N A")
    req(cfg,"DRIVE_L_INVERT=false,DRIVE_R_INVERT=false","no speculative direction inversion")
    req(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","lost-command watchdog")
    req(profile,"DRIVE_L_EN=17","Android profile left EN")
    req(profile,"DRIVE_R_EN=16","Android profile right EN")

    req(hw,"void driveSet(int l,int r){curL=lim(l);curR=lim(r);cmdAt=millis();staleStop=false;driveWrite();}","firmware direct drive")
    req(hw,"motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT)","left output")
    req(hw,"motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT)","right output")

    # HR056 Direct authority: HTTP only. UDP artifacts may remain as historical source files, but
    # neither active firmware nor RobotApi may compile/use the transport.
    req(ino,'web.on("/drive"',"Direct HTTP drive endpoint")
    req(ino,'radioProfile!=RadioProfile::DIRECT',"Direct/Remote drive authority guard")
    req(ino,'\\"drive_udp\\":{\\"installed\\":false',"firmware advertises local UDP disabled")
    forbid(ino,'#include <WiFiUdp.h>',"active firmware local UDP")
    forbid(ino,'driveUdp.begin(',"active firmware local UDP listener")
    req(api,'DIRECT_KEEPALIVE_MS=250L',"Direct HTTP keepalive limit")
    req(api,'getText("/drive?l="+left+"&r="+right,450)',"Direct HTTP drive")
    req(api,'requireNeutralAfterBackground',"background neutral re-arm")
    forbid(api,'DatagramSocket',"Android active local UDP")
    forbid(api,'sendDriveDatagram',"Android active local UDP sender")

    req(remote,"RemoteDrivePolicy.COMMAND_TTL_MS","remote drive policy binding")
    req(remote,"publishedDriveRevision","remote latest-command coalescing")
    req(remote_policy,"MIN_PUBLISH_MS=40","remote publish ceiling")

    print("HazRob 0.10.1 HR-0101-048 steering contract under HR056: PASS")
    print("right-pivot=(+100,-100); mapping LEFT=B RIGHT=A; Direct=HTTP; watchdog=500ms; Remote=MQTT; physical acceptance=PENDING")

    if os.environ.get("HR048_SKIP_INTEGRATION") != "1":
        from audit_hr048_integration_contract import main as integration_main
        if integration_main() not in (None,0):
            raise AssertionError("HR048 cross-component integration audit returned non-zero")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-048 steering/integration gate: FAIL:",e)
        raise SystemExit(1)
