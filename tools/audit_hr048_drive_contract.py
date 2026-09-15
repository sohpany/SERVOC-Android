#!/usr/bin/env python3
"""HR-0101-048 fail-closed drive contract after 2026-09-14 field steering finding.

The operator coordinate convention is canonical: +Y forward, +X right.
For a differential drive, a right command must produce logical L > R.
Field behavior showed reversed steering handedness while straight motion remained correct,
so HR048 tests the controlled remediation candidate logical LEFT -> L298N B and RIGHT -> A.
Direct local drive additionally has a capability-gated UDP fast lane; legacy HTTP remains fallback.
Physical acceptance still requires the post-flash wheels-up gate.

When invoked directly by the legacy HR048 Render gate this script also runs the HR048
cross-component integration contract. Full validation may set HR048_SKIP_INTEGRATION=1 and run
that integration audit as a separately captured artifact.
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
    udp=txt("app/src/main/java/com/servoc/app/communication/DirectDriveDatagram.java")
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

    # Low-latency Direct mode contract. Android enables it only after firmware advertises the same
    # version/port in /diag; HTTP therefore remains a compatibility fallback for older firmware.
    req(udp,"PORT = 4210","Android UDP port")
    req(udp,"VERSION = 1","Android UDP protocol version")
    req(udp,"SIZE = 8","Android UDP packet size")
    req(ino,"DRIVE_UDP_PORT=4210","firmware UDP port")
    req(ino,"DRIVE_UDP_VERSION=1","firmware UDP protocol version")
    req(ino,"driveUdp.begin(DRIVE_UDP_PORT)","firmware UDP listener")
    req(ino,"b[0]!='H'||b[1]!='R'||b[2]!=DRIVE_UDP_VERSION||check!=b[7]","firmware UDP packet validation")
    req(ino,"driveUdp.remoteIP()","UDP source tracking")
    req(ino,"port!=driveUdpPeerPort","UDP sequence resync on app endpoint change")
    req(ino,'\\"drive_udp\\":{\\"installed\\":true,\\"port\\":',"firmware capability advertisement")
    req(api,"directFastDrive","Android capability gate")
    req(api,"DirectDriveDatagram.encode","Android UDP encoder use")
    req(api,"sendStopBurst()","redundant immediate zero burst")
    req(api,"getText(\"/drive?l=\"+left+\"&r=\"+right,450)","legacy HTTP fallback")

    req(api,"if(left==0&&right==0&&lastDirectDriveLeft==0&&lastDirectDriveRight==0)return;","direct idle zero suppression")
    req(remote,"RemoteDrivePolicy.COMMAND_TTL_MS","remote drive policy binding")
    req(remote,"publishedDriveRevision","remote latest-command coalescing")
    req(remote_policy,"MIN_PUBLISH_MS=40","remote publish ceiling")

    print("HazRob 0.10.1 HR-0101-048 drive contract: PASS")
    print("right-pivot=(+100,-100); mapping LEFT=B RIGHT=A; UDP/4210 v1; watchdog=500ms; physical acceptance=PENDING")

    if os.environ.get("HR048_SKIP_INTEGRATION") != "1":
        from audit_hr048_integration_contract import main as integration_main
        if integration_main() not in (None,0):
            raise AssertionError("HR048 cross-component integration audit returned non-zero")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-048 drive/integration gate: FAIL:",e)
        raise SystemExit(1)
