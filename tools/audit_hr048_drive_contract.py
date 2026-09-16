#!/usr/bin/env python3
"""HR-0101-048/060 steering/safety contract carried forward on HazRob 0.10.1.

The gate locks the differential-drive geometry across the Android joystick, pure mixer,
main firmware side mapping, Direct HTTP and Remote MQTT transports. It explicitly covers
forward/reverse diagonals so steering handedness cannot silently regress.
Physical wheel acceptance remains a post-flash gate.
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
    x=max(-1.0,min(1.0,x));y=max(-1.0,min(1.0,y))
    l=y+x;r=y-x;q=max(1.0,abs(l),abs(r))
    return round(l/q*100),round(r/q*100)

def main():
    cfg=txt("firmware/HazRob/Config.h")
    hw=txt("firmware/HazRob/Hardware.cpp")
    ino=txt("firmware/HazRob/HazRob.ino")
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")
    mixer=txt("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    mixer_test=txt("app/src/test/java/com/servoc/app/control/DriveMixerTest.java")
    limiter_test=txt("app/src/test/java/com/servoc/app/control/DriveSpeedLimiterTest.java")
    api=txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    remote_policy=txt("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    profile=txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")

    # One executable mixer authority. X is yaw: +X always rotates nose/camera right.
    req(joy,"DriveMixer.Command command=DriveMixer.mix(x,y);","joystick uses audited mixer")
    req(mixer,"double left=y+x;","mixer logical left")
    req(mixer,"double right=y-x;","mixer logical right")
    req(mixer,"Math.max(1.0,Math.max(Math.abs(left),Math.abs(right)))","mixer normalization")

    expected={
        "stop":((0,0),(0,0)),
        "forward":((0,1),(100,100)),
        "reverse":((0,-1),(-100,-100)),
        "right_pivot":((1,0),(100,-100)),
        "left_pivot":((-1,0),(-100,100)),
        "forward_right":((.25,.75),(100,50)),
        "forward_left":((-.25,.75),(50,100)),
        "reverse_right":((.25,-.75),(-50,-100)),
        "reverse_left":((-.25,-.75),(-100,-50)),
    }
    for name,(xy,want) in expected.items():
        got=mix(*xy)
        if got!=want: raise AssertionError(f"{name} mix invalid: got={got} want={want}")
    # Right yaw remains right yaw in reverse; left yaw remains left yaw in reverse.
    if not (mix(.25,.75)[0]-mix(.25,.75)[1]>0 and mix(.25,-.75)[0]-mix(.25,-.75)[1]>0):
        raise AssertionError("right-yaw handedness changes between forward/reverse")
    if not (mix(-.25,.75)[0]-mix(-.25,.75)[1]<0 and mix(-.25,-.75)[0]-mix(-.25,-.75)[1]<0):
        raise AssertionError("left-yaw handedness changes between forward/reverse")

    for marker in ["forwardSteeringIsCorrect","reverseSteeringKeepsYawHandedness","rightAndLeftYawSignsAreConsistentAcrossForwardAndReverse","normalizationPreservesFullScaleWithoutClippingCurvature"]:
        req(mixer_test,marker,"JUnit steering matrix")
    req(limiter_test,"defaultIsFullScale","100-percent operator default")
    req(limiter_test,"assertEquals(100,DriveSpeedLimiter.scale(100,100))","100-percent forward full scale")
    req(limiter_test,"assertEquals(-100,DriveSpeedLimiter.scale(-100,100))","100-percent reverse full scale")

    req(cfg,"DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32","logical LEFT -> L298N B")
    req(cfg,"DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27","logical RIGHT -> L298N A")
    req(cfg,"DRIVE_L_INVERT=false,DRIVE_R_INVERT=false","no speculative direction inversion")
    req(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","lost-command watchdog")
    req(cfg,"int speed=100","firmware full-scale default")
    req(profile,"DRIVE_L_EN=17","Android profile left EN")
    req(profile,"DRIVE_R_EN=16","Android profile right EN")

    req(hw,"void driveSet(int l,int r){curL=lim(l);curR=lim(r);cmdAt=millis();staleStop=false;driveWrite();}","firmware direct drive")
    req(hw,"motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT)","left output")
    req(hw,"motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT)","right output")

    # Direct authority is HTTP only; no local UDP competes with it.
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

    print("HazRob 0.10.1 HR-0101-048/060 steering contract: PASS")
    print("matrix: F=(100,100) R=(-100,-100) FR=(100,50) FL=(50,100) RR=(-50,-100) RL=(-100,-50) pivotR=(100,-100) pivotL=(-100,100)")
    print("semantics: +X = right yaw of robot nose/camera in both forward and reverse; speed default=100%; physical acceptance=PENDING")

    if os.environ.get("HR048_SKIP_INTEGRATION") != "1":
        from audit_hr048_integration_contract import main as integration_main
        if integration_main() not in (None,0):
            raise AssertionError("HR048 cross-component integration audit returned non-zero")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-048/060 steering/integration gate: FAIL:",e)
        raise SystemExit(1)
