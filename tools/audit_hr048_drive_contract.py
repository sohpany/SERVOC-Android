#!/usr/bin/env python3
"""HR-0101-048 fail-closed drive contract after 2026-09-14 field steering finding.

The operator coordinate convention is canonical: +Y forward, +X right.
For a differential drive, a right command must produce logical L > R.
Field evidence showed the previous logical L/R hardware association was reversed,
so logical LEFT now maps to L298N channel B and logical RIGHT to channel A.
"""
from pathlib import Path
import re

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
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")
    api=txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    profile=txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")

    # Canonical joystick semantics: rightward +X increases logical left side and reduces right side.
    req(joy,"double l = y + x, r = y - x;","joystick differential mixer")
    l,r=mix(.25,.75)
    if not (l>r>0): raise AssertionError(f"forward-right mix invalid: L={l}, R={r}")
    l,r=mix(-.25,.75)
    if not (r>l>0): raise AssertionError(f"forward-left mix invalid: L={l}, R={r}")
    if mix(1.0,0.0)!=(100,-100): raise AssertionError("right pivot mix invalid")
    if mix(-1.0,0.0)!=(-100,100): raise AssertionError("left pivot mix invalid")

    # Field-corrected logical side association. Forward/reverse remain unchanged because both sides
    # receive equal signs; only steering/pivot handedness changes.
    req(cfg,"DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32","logical LEFT -> L298N B")
    req(cfg,"DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27","logical RIGHT -> L298N A")
    req(cfg,"DRIVE_L_INVERT=false,DRIVE_R_INVERT=false","no speculative direction inversion")
    req(profile,"DRIVE_L_EN=17","Android profile left EN")
    req(profile,"DRIVE_R_EN=16","Android profile right EN")

    # Firmware remains direct logical command -> channel output; no hidden turn inversion/shaping.
    req(hw,"void driveSet(int l,int r){curL=lim(l);curR=lim(r);cmdAt=millis();staleStop=false;driveWrite();}","firmware direct drive")
    req(hw,"motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT)","left output")
    req(hw,"motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT)","right output")

    # Direct idle ownership fix: send zero transition once, then allow independent diagnostic commands.
    req(api,"if(left==0&&right==0&&lastDirectDriveLeft==0&&lastDirectDriveRight==0)return;","direct idle zero suppression")
    # Remote must preserve exactly the same logical left/right semantics.
    req(remote,'p.put("left",left);p.put("right",right);command("drive",p,700,false,0);',"remote drive parity")

    print("HazRob 0.10.1 HR-0101-048 drive contract: PASS")
    print("right-pivot logical=(+100,-100); field hardware mapping: LEFT=B, RIGHT=A")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-048 drive contract: FAIL:",e)
        raise SystemExit(1)
