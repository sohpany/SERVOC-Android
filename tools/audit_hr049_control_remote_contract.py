#!/usr/bin/env python3
"""HR-0101-049 control/remote contract carried forward under HR-0101-056.

Direct is deliberately restored to HTTP over an AP-only profile. Remote retains the HR049
latest-command MQTT architecture and latency policy. Physical latency/WAN tests remain field gates.
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
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")

    req(joy,"double l = y + x, r = y - x;","joystick mixer")
    req(joy,"Math.round(l / q * 100)","left full-scale output")
    req(joy,"Math.round(r / q * 100)","right full-scale output")

    # HR056 Direct: AP-only + HTTP drive, OS-requested local-only network, bounded command cadence.
    req(ino,'configureSoftAp(WIFI_AP);',"Direct AP-only profile")
    req(ino,'web.on("/drive"',"Direct HTTP drive endpoint")
    req(ino,'radioProfile!=RadioProfile::DIRECT',"dual-authority drive rejection")
    req(ino,'\\"drive_udp\\":{\\"installed\\":false',"UDP disabled capability")
    forbid(ino,'#include <WiFiUdp.h>',"active local UDP transport")
    req(api,'DIRECT_KEEPALIVE_MS=250L',"Direct HTTP keepalive cadence")
    req(api,'getText("/drive?l="+left+"&r="+right,450)',"Direct HTTP sender")
    forbid(api,'DatagramSocket',"Android active local UDP")
    req(wifi,'WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID)',"Direct local-only Wi-Fi request")
    req(cfg,"CONTROL_COMMAND_TIMEOUT_MS=500","lost-command motor watchdog")

    # Remote latest-command architecture: no command backlog, bounded publish rate, hold refresh.
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
    forbid(remote,'command("drive",p,700,false,0)',"retired synchronous Remote drive path")

    print("HazRob 0.10.1 HR-0101-049 control/remote contract under HR056: PASS")
    print("Direct=AP-only HTTP/250ms keepalive; Remote=latest-command 25Hz max / 120ms hold refresh; telemetry=2Hz; watchdog=500ms")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-049 control/remote gate: FAIL:",e)
        raise SystemExit(1)
