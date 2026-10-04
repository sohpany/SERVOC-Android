#!/usr/bin/env python3
"""Compatibility steering/safety gate for HazRob 0.10.2 / HR069 RC2.

The gate locks the current tunable differential-drive geometry across the Android joystick,
wheel wrapper, pure mixer, main-firmware side mapping, Direct HTTP and Remote MQTT transports.
Automatic IR/ultrasonic movement blocking remains forced OFF in HR069 RC2; warnings, E-stop and
the stale-command watchdog remain independent. Physical wheel acceptance remains a post-flash gate.
"""
from pathlib import Path
import os

ROOT=Path(__file__).resolve().parents[1]

def txt(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,s,label):
    if s not in src: raise AssertionError(f"{label}: missing {s!r}")
def forbid(src,s,label):
    if s in src: raise AssertionError(f"{label}: forbidden {s!r}")

def mix(x,y,turn=40,pivot=60):
    x=max(-1.0,min(1.0,x));y=max(-1.0,min(1.0,y))
    turn=max(0,min(100,turn))/100.0;pivot=max(0,min(100,pivot))/100.0
    if abs(y)<=0.08:
        p=x*pivot
        return round(p*100),round(-p*100)
    ratio=1.0-(1.0-turn)*abs(x)
    full=y;slowed=y*ratio
    if x>0: l,r=full,slowed
    elif x<0: l,r=slowed,full
    else: l=r=full
    return round(l*100),round(r*100)

def main():
    cfg=txt("firmware/HazRob/Config.h")
    hw=txt("firmware/HazRob/Hardware.cpp")
    safety=txt("firmware/HazRob/SafetyOptions.cpp")
    ino=txt("firmware/HazRob/HazRob.ino")
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")
    wheel=txt("app/src/main/java/com/servoc/app/control/SteeringWheelMixer.java")
    tuning=txt("app/src/main/java/com/servoc/app/control/DriveTuning.java")
    mixer=txt("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    mixer_test=txt("app/src/test/java/com/servoc/app/control/DriveMixerTest.java")
    limiter_test=txt("app/src/test/java/com/servoc/app/control/DriveSpeedLimiterTest.java")
    api=txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    remote_policy=txt("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    profile=txt("app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java")

    req(joy,"DriveMixer.mix(x, y, turn, pivot)","joystick uses canonical tunable mixer")
    req(wheel,"return DriveMixer.mix(x,y,turnPercent,pivotPercent)","wheel wrapper delegates to canonical mixer")
    forbid(joy,"DEAD_ZONE","joystick software dead zone")
    req(joy,"magnitude <= 1.0e-6","joystick numerical center guard")
    req(joy,"requestDisallowInterceptTouchEvent(true)","joystick touch ownership")
    req(tuning,"DEFAULT_TURN_PERCENT = 40","default inside-side turn percentage")
    req(tuning,"DEFAULT_PIVOT_PERCENT = 60","default pivot percentage")
    req(mixer,"TRANSLATION_DEADZONE = 0.08","translation deadzone")
    req(mixer,"double pivot=x*pivotMax;","tunable pivot geometry")
    req(mixer,"double ratio=1.0-(1.0-minInnerRatio)*Math.abs(x);","progressive inside-side ratio")
    req(mixer,"if(x>0)return new Command(pct(full),pct(slowed));","right steering mapping")
    req(mixer,"if(x<0)return new Command(pct(slowed),pct(full));","left steering mapping")

    expected={
        "stop":((0,0),(0,0)),
        "forward":((0,1),(100,100)),
        "reverse":((0,-1),(-100,-100)),
        "right_pivot":((1,0),(60,-60)),
        "left_pivot":((-1,0),(-60,60)),
        "forward_right":((.5,.8),(80,56)),
        "forward_left":((-.5,.8),(56,80)),
        "reverse_right":((.5,-.8),(-80,-56)),
        "reverse_left":((-.5,-.8),(-56,-80)),
    }
    for name,(xy,want) in expected.items():
        got=mix(*xy)
        if got!=want: raise AssertionError(f"{name} mix invalid: got={got} want={want}")

    for marker in [
        "defaultCardinalTravelAndPivotArePredictable",
        "defaultRightSteeringSlowsRightSideProgressively",
        "defaultLeftSteeringSlowsLeftSideProgressively",
        "reverseKeepsSameScreenDirectionRule",
        "operatorTurnPercentControlsInnerSideAtFullLock",
        "operatorPivotPercentControlsInPlaceRotation",
        "tuningValuesAreClampedToPercentDomain",
        "inputsAreBoundedToUnitCommandDomain",
    ]:
        req(mixer_test,marker,"JUnit steering/tuning matrix")
    req(limiter_test,"defaultIsFullScale","100-percent operator default")
    req(limiter_test,"assertEquals(100,DriveSpeedLimiter.scale(100,100))","100-percent forward full scale")
    req(limiter_test,"assertEquals(-100,DriveSpeedLimiter.scale(-100,100))","100-percent reverse full scale")

    req(cfg,"DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32","logical LEFT -> L298N B")
    req(cfg,"DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27","logical RIGHT -> L298N A")
    req(cfg,"DRIVE_L_INVERT=false,DRIVE_R_INVERT=false","no direction inversion")
    req(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","lost-command watchdog")
    req(cfg,"int speed=100","firmware full-scale default")
    req(profile,"DRIVE_L_EN=17","Android profile left EN")
    req(profile,"DRIVE_R_EN=16","Android profile right EN")

    req(hw,"void driveSet(int l,int r){","firmware drive entry")
    req(hw,"bool forwardRequested=(requestedL+requestedR)>0.1f;","front safety forward classification")
    req(hw,"bool forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop);","front hazard gate remains behind disabled authority")
    req(hw,"if(forwardBlocked&&forwardRequested){curL=curR=0;}else{curL=requestedL;curR=requestedR;}","forward-only block implementation")
    req(safety,'safetyNv.putBool("autoStop",false)',"automatic hazard-stop persisted OFF")
    req(safety,'return false;',"automatic hazard-stop runtime authority OFF")
    req(safety,'if(persist)safetyNv.putBool("autoStop",false)',"compatibility setter cannot enable auto-stop")
    req(hw,"cmdAt=millis();staleStop=false;driveWrite();","firmware drive watchdog refresh")
    req(hw,"motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT)","left output")
    req(hw,"motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT)","right output")

    req(ino,'web.on("/drive"',"Direct HTTP drive endpoint")
    req(ino,'radioProfile!=RadioProfile::DIRECT',"Direct/Remote drive authority guard")
    req(ino,'\\"drive_udp\\":{\\"installed\\":false',"firmware advertises local UDP disabled")
    forbid(ino,'#include <WiFiUdp.h>',"active firmware local UDP")
    forbid(ino,'driveUdp.begin(',"active firmware local UDP listener")
    req(api,'DIRECT_KEEPALIVE_MS=150L',"Direct HTTP keepalive")
    req(api,'DIRECT_DRIVE_TIMEOUT_MS=250',"Direct HTTP latency bound")
    req(api,'getText("/drive?l="+left+"&r="+right,DIRECT_DRIVE_TIMEOUT_MS)',"Direct HTTP drive")
    req(api,'requireNeutralAfterBackground',"background neutral re-arm")
    forbid(api,'DatagramSocket',"Android active local UDP")

    req(remote,"RemoteDrivePolicy.COMMAND_TTL_MS","remote drive policy binding")
    req(remote,"publishedDriveRevision","remote latest-command coalescing")
    req(remote_policy,"MIN_PUBLISH_MS=40","remote publish ceiling")

    print("HazRob 0.10.2 steering contract: PASS")
    print("tunable inside-side steering (default turn=40%, pivot=60%); hazard auto-stop=FORCED-OFF; warnings active; Direct keepalive=150ms; timeout=250ms; watchdog=500ms; speed default=100%; physical acceptance=PENDING")

    if os.environ.get("HR048_SKIP_INTEGRATION") != "1":
        from audit_hr048_integration_contract import main as integration_main
        if integration_main() not in (None,0):
            raise AssertionError("cross-component integration audit returned non-zero")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.2 steering/integration gate: FAIL:",e)
        raise SystemExit(1)