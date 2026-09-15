#!/usr/bin/env python3
"""HR-0101-049 fail-closed contract: smooth Direct drive + responsive Remote control.

Static gate only. It verifies that Android, ESP32 and transport timing policies agree.
Physical steering/latency and WAN tests remain separate field gates.
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
    udp=txt("app/src/main/java/com/servoc/app/communication/DirectDriveDatagram.java")
    udp_test=txt("app/src/test/java/com/servoc/app/communication/DirectDriveDatagramTest.java")
    remote=txt("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    policy=txt("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    policy_test=txt("app/src/test/java/com/servoc/app/communication/RemoteDrivePolicyTest.java")
    mqtt_client=txt("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java")
    ino=txt("firmware/HazRob/HazRob.ino")
    cfg=txt("firmware/HazRob/Config.h")
    mqtt=txt("firmware/HazRob/RemoteMqtt.cpp")
    joy=txt("app/src/main/java/com/servoc/app/control/JoystickView.java")

    # Canonical differential mixer and full-power maneuver authority remain unchanged.
    req(joy,"double l = y + x, r = y - x;","joystick mixer")
    req(joy,"Math.round(l / q * 100)","left full-scale output")
    req(joy,"Math.round(r / q * 100)","right full-scale output")

    # Direct fast lane: capability-gated binary UDP with checksum/sequence and HTTP fallback.
    for s in ("PORT = 4210","VERSION = 1","SIZE = 8","b[7]=(byte)x"):
        req(udp,s,"Android Direct UDP contract")
    req(udp_test,"packetLayoutAndChecksumAreStable","Direct UDP unit test")
    req(api,"observeDirectCapabilities","Direct capability negotiation")
    req(api,"DatagramSocket","Direct UDP sender")
    req(api,"DirectDriveDatagram.encode","Direct UDP encoder binding")
    req(api,"sendStopBurst()","Direct stop redundancy")
    req(api,'getText("/drive?l="+left+"&r="+right,450)',"legacy Direct HTTP fallback")
    req(ino,"WiFiUDP driveUdp","ESP32 UDP listener")
    req(ino,"DRIVE_UDP_PORT=4210","ESP32 UDP port")
    req(ino,"DRIVE_UDP_VERSION=1","ESP32 UDP version")
    req(ino,"driveUdpNewer","ESP32 stale/out-of-order rejection")
    req(ino,"check!=b[7]","ESP32 checksum validation")
    req(ino,"c.setTimeout(80)","bounded local HTTP stall")
    req(ino,"driveUdpUpdate();hwUpdate();delay(5)","drive service during Wi-Fi scan")
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

    # TLS/MQTT control path is latency-prioritized while preserving security and critical ACKs.
    req(mqtt_client,"s.setTcpNoDelay(true)","Android TLS TCP_NODELAY")
    req(mqtt_client,"s.setKeepAlive(true)","Android TLS keepalive")
    req(mqtt,"tls.setNoDelay(true)","ESP32 TLS TCP_NODELAY")
    req(mqtt,"CONTROL_TELEMETRY_MS=500","Remote telemetry load ceiling")
    req(mqtt,'t=="drive"||t=="heartbeat"||t=="alert"',"fire-and-forget ACK suppression set")
    req(mqtt,"if(!quietSuccess)ack(s,q,n,ok,p,err)","critical/error ACK retention")
    req(mqtt,"while(tls.available())processPacket()","incoming commands before telemetry")
    forbid(remote,'command("drive",p,700,false,0)',"retired synchronous Remote drive path")

    print("HazRob 0.10.1 HR-0101-049 control/remote contract: PASS")
    print("Direct=UDP/4210 v1 + HTTP fallback; Remote=latest-command 25Hz max / 120ms hold refresh; telemetry=2Hz; watchdog=500ms")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.1 HR-0101-049 control/remote gate: FAIL:",e)
        raise SystemExit(1)
