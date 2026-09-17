#!/usr/bin/env python3
"""Compatibility control/remote gate for HazRob 0.10.1.

Direct is AP-only + persistent low-latency HTTP control. Remote keeps the latest-command MQTT
architecture and bounded publish policy. Steering geometry remains delegated to DriveMixer.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def txt(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,s,label):
    if s not in src: raise AssertionError(f"{label}: missing {s!r}")
def forbid(src,s,label):
    if s in src: raise AssertionError(f"{label}: forbidden {s!r}")

def main():
    api=txt("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    wifi=txt("app/src/main/java/com/servoc/app/communication/WifiRoute.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    policy=txt("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    policy_test=txt("app/src/test/java/com/servoc/app/communication/RemoteDrivePolicyTest.java")
    mqtt_client=txt("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java")
    ino=txt("firmware/HazRob/HazRob.ino")
    cfg=txt("firmware/HazRob/Config.h")
    mqtt=txt("firmware/HazRob/RemoteMqtt.cpp")
    link=txt("firmware/HazRob/RemoteLink.cpp")
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")
    mixer=txt("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    mixer_test=txt("app/src/test/java/com/servoc/app/control/DriveMixerTest.java")

    req(joy,"DriveMixer.mix(x, y)","joystick mixer authority")
    forbid(joy,"DEAD_ZONE","joystick software dead zone")
    req(joy,"magnitude <= 1.0e-6","joystick center guard")
    req(mixer,"double steer=y<0?-x:x;","travel-relative steering")
    req(mixer,"double left=y+steer;","left differential equation")
    req(mixer,"double right=y-steer;","right differential equation")
    req(mixer_test,"reverseSteeringUsesSameInsidePhysicalSide","reverse steering regression")
    req(mixer_test,"samePhysicalSideIsInsideForwardAndReverse","physical-side steering regression")
    req(mixer_test,"purePivotUsesCounterRotation","pivot regression")

    # Direct: AP-only + HTTP drive, persistent Android route, no local UDP authority.
    # Check semantic markers separately so harmless source formatting cannot break the release gate.
    req(ino,'bool directApStable(){',"Direct AP health helper")
    req(ino,'radioProfile==RadioProfile::DIRECT',"Direct profile authority")
    req(ino,'WiFi.getMode()==WIFI_AP',"Direct AP-only mode")
    req(ino,'WiFi.softAPSSID()==Cfg::SSID',"Direct exact SSID health")
    req(ino,'configureSoftAp(WIFI_AP,false);',"Remote-to-Direct AP-preserving transition")
    req(ino,'if(directApStable())',"Direct idempotence")
    req(ino,'web.on("/drive"',"Direct HTTP drive endpoint")
    req(ino,'radioProfile!=RadioProfile::DIRECT',"dual-authority drive rejection")
    req(ino,'\\"drive_udp\\":{\\"installed\\":false',"UDP disabled capability")
    forbid(ino,'#include <WiFiUdp.h>',"active local UDP transport")
    req(api,'DIRECT_KEEPALIVE_MS=150L',"Direct HTTP keepalive cadence")
    req(api,'DIRECT_DRIVE_TIMEOUT_MS=250',"Direct HTTP request bound")
    req(api,'getText("/drive?l="+left+"&r="+right,DIRECT_DRIVE_TIMEOUT_MS)',"Direct HTTP sender")
    forbid(api,'DatagramSocket',"Android active local UDP")
    req(wifi,'WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID)',"Direct exact-SSID request")
    req(wifi,'WifiManager.WIFI_MODE_FULL_LOW_LATENCY',"Direct Wi-Fi performance lock")
    req(wifi,'wifiLock.acquire()',"Direct Wi-Fi lock acquisition")
    req(wifi,'onLost(Network network)',"Direct network-loss observer")
    req(wifi,'if(bindRobotWifi(context))return true;',"existing Direct route preference")
    req(wifi,'only by the explicit Direct-entry flow in waitForRobotWifi()',"temporary request explicit fallback")
    forbid(wifi,'SystemClock.elapsedRealtime()-lost>=250',"repeated temporary-network recreation")
    req(wifi,'LocalNetworkContract.isRobotSubnetAddress',"robot-subnet route validation")
    req(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","lost-command motor watchdog")
    req(ino,'remoteDeveloperAuthorized()',"developer Remote authorization gate")
    req(ino,'web.on("/remote/authorize"',"developer Remote authorization endpoint")
    req(link,'wan.getBool("authorized",false)',"persisted developer authorization load")
    req(link,'wan.putBool("authorized",enabled)',"persisted developer authorization save")
    req(link,'if(enabled&&!developerAuthorized)',"transport-level developer authorization gate")
    req(ino,'requestRadioProfile(RadioProfile::DIRECT,false)',"Direct priority transition")
    req(ino,'\\\"remote_authority_revoked\\\":true',"Direct takeover acknowledgement")
    req(ino,'remoteSetTransportEnabled(false,false);',"immediate Remote transport revocation")

    # Remote latest-command architecture: no backlog, bounded publish rate, hold refresh.
    for s in ("PUMP_MS=20","MIN_PUBLISH_MS=40","HOLD_REFRESH_MS=120","COMMAND_TTL_MS=900"):
        req(policy,s,"Remote drive timing policy")
    req(policy_test,"stopTransitionBypassesRateLimit","Remote stop-priority test")
    req(policy_test,"movingChangesAreRateLimitedToTwentyFiveHz","Remote rate-limit test")
    req(policy_test,"unchangedMotionGetsHoldRefresh","Remote hold-refresh test")
    req(remote,"driveRevision","Remote latest-command revision")
    req(remote,"publishedDriveRevision","Remote coalescing state")
    req(remote,"RemoteDrivePolicy.due","Remote policy binding")
    req(remote,'command("motion_stop",new JSONObject(),1500,true,1500)',"confirmed Remote stop")
    req(remote,'command("estop",new JSONObject(),3000,true,2500)',"confirmed Remote E-stop")
    req(remote,"lastAckRttMs","Remote ACK RTT instrumentation")

    req(mqtt_client,"s.setTcpNoDelay(true)","Android TLS TCP_NODELAY")
    req(mqtt_client,"s.setKeepAlive(true)","Android TLS keepalive")
    req(mqtt,"tls.setNoDelay(true)","ESP32 TLS TCP_NODELAY")
    req(mqtt,"CONTROL_TELEMETRY_MS=500","Remote telemetry load ceiling")
    req(mqtt,'t=="drive"||t=="heartbeat"||t=="alert"',"fire-and-forget ACK suppression set")
    req(mqtt,"if(!quietSuccess)ack(s,q,n,ok,p,err)","critical/error ACK retention")
    req(mqtt,"while(tls.available())processPacket()","incoming commands before telemetry")

    print("HazRob 0.10.1 control/remote contract: PASS")
    print("Direct=existing HazRob route + HTTP 150ms keepalive / 250ms timeout; Remote=developer-authorized + explicit request + latest-command 25Hz max; watchdog=500ms")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 control/remote gate: FAIL:",e)
        raise SystemExit(1)
