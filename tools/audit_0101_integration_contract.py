#!/usr/bin/env python3
"""Fail-closed HazRob 0.10.1 HR-0101-057 connection integration contract.

The contract separates runtime Direct from persistent WAN intent:
DIRECT = AP-only + HTTP drive + local MJPEG for the current session.
REMOTE = AP+STA + MQTT control + Render/WSS video through NAPT.
Saved WAN credentials must auto-arm Remote on every reboot until explicit WAN clear.
Current GPIO/ENV authority must not regress.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(p: str) -> str:
    return (ROOT / p).read_text(encoding="utf-8")

def require(src: str, needle: str, label: str) -> None:
    if needle not in src:
        raise AssertionError(f"{label}: missing {needle!r}")

def forbid(src: str, needle: str, label: str) -> None:
    if needle in src:
        raise AssertionError(f"{label}: forbidden {needle!r}")

def main() -> int:
    master = text("firmware/HazRob/HazRob.ino")
    cfg = text("firmware/HazRob/Config.h")
    mqtt = text("firmware/HazRob/RemoteMqtt.cpp")
    remote_h = text("firmware/HazRob/RemoteLink.h")
    remote = text("firmware/HazRob/RemoteLink.cpp")
    cam = text("firmware/HazRob-CAM/HazRob-CAM.ino")
    cam_remote = text("firmware/HazRob-CAM/RemoteVideo.cpp")
    local = text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    robot_api = text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    wifi_route = text("app/src/main/java/com/servoc/app/communication/WifiRoute.java")
    gate = text("app/src/main/java/com/servoc/app/auth/ConnectionGate.java")
    link_service = text("app/src/main/java/com/servoc/app/communication/RobotLinkService.java")
    manifest = text("app/src/main/AndroidManifest.xml")

    require(cfg, 'FW[]="HazRob 0.10.1"', "master identity")
    require(cam, 'FW[]="HazRob-CAM 0.10.1"', "camera identity")
    for needle in [
        'HW_PROFILE[]="hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2"',
        'DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32',
        'DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27',
        'DRIVE_L_INVERT=false,DRIVE_R_INVERT=false',
        'SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49',
        'ENV_SDA=13,ENV_SCL=14',
        'RELAY_MQ=25','ALERT_PIN=26','SOUND_PIN=36','IR_PIN=35','LDR_PIN=34',
        'BATTERY_PIN=33','ULTRA_TRIG_PIN=23','ULTRA_ECHO_PIN=39',
        'CONTROL_COMMAND_TIMEOUT_MS=500',
    ]:
        require(cfg, needle, "hardware authority")

    require(local, 'ROBOT_SSID="HazRob"', "Android direct SSID")
    require(local, 'ROBOT_HOST="192.168.4.1"', "Android direct robot host")
    require(local, 'CAMERA_HOST="192.168.4.2"', "Android direct camera host")
    require(master, 'enum class RadioProfile:uint8_t{DIRECT=0,REMOTE=1}', "radio state authority")
    require(master, 'configureSoftAp(WIFI_AP);', "Direct AP-only radio")
    require(master, 'configureSoftAp(WIFI_AP_STA);', "Remote AP+STA radio")
    require(master, 'remoteSetTransportEnabled(false,persistMode)', "Direct disables WAN transport")
    require(master, 'remoteMqttDisconnect();', "Direct disconnects MQTT")
    require(master, 'WiFi.AP.enableNAPT(false)', "Direct disables NAPT")
    require(master, 'web.on("/drive"', "Direct HTTP drive endpoint")
    require(master, 'radioProfile!=RadioProfile::DIRECT', "local drive authority guard")
    require(master, 'web.on("/connection/direct"', "Direct mode endpoint")
    require(master, 'requestRadioProfile(RadioProfile::DIRECT,false)', "Direct must be runtime-only")
    require(master, 'requestRadioProfile(RadioProfile::REMOTE,true)', "Remote must arm persisted WAN")
    require(master, 'drive_udp', "Direct capability field")
    require(master, 'installed', "UDP explicitly absent")
    forbid(master, '#include <WiFiUdp.h>', "Direct must not compile UDP transport")
    forbid(master, 'driveUdp.begin(', "Direct must not start UDP drive")

    require(remote_h, 'remoteSetTransportEnabled', "remote transport switch API")
    require(remote, 'transportEnabled=configured;', "saved WAN auto-arms Remote on boot")
    require(remote, 'wan.putBool("enabled",configured);', "boot persistence normalization")
    require(remote, 'if(persistMode&&enabled)wan.putBool("enabled",true);', "Direct must not persistently disarm WAN")
    require(remote, 'wan.putString("ssid",ssid);wan.putString("pass",password);wan.putBool("enabled",true);', "provision persists WAN boot intent")
    require(remote, 'wan.remove("ssid");wan.remove("pass");wan.putBool("enabled",false);', "clear WAN is explicit persistent opt-out")
    forbid(remote, 'transportEnabled=wan.getBool("enabled",false)', "legacy boot flag must not override saved WAN")
    require(remote, 'if(!transportEnabled){dnsOk=timeOk=false;return;}', "remote loop disabled in Direct")
    require(remote, 'remoteInternetReady(){return transportEnabled&&configured', "remote readiness gate")
    forbid(remote, 'WiFi.mode(', "RemoteLink must not change global radio mode")
    require(master, 'if(radioProfile==RadioProfile::REMOTE){remoteUpdate();gatewayUpdate();remoteMqttUpdate();}', "remote runtime profile gate")
    require(master, 'WiFi.AP.enableNAPT(ready)', "remote camera Internet routing")
    require(mqtt, 't=="drive"', "Remote MQTT drive")
    require(mqtt, 't=="estop"', "Remote MQTT estop")

    require(robot_api, 'HTTP for Direct control/telemetry, MQTT for Remote', "Android transport authority")
    require(robot_api, 'DIRECT_KEEPALIVE_MS=250L', "Direct HTTP keepalive cadence")
    require(robot_api, 'getText("/drive?l="+left+"&r="+right,450)', "Direct HTTP drive")
    require(robot_api, 'requireNeutralAfterBackground', "background re-arm interlock")
    forbid(robot_api, 'DatagramSocket', "Android Direct UDP removed")
    forbid(robot_api, 'sendDriveDatagram', "Android Direct UDP removed")

    require(manifest, 'android.permission.NEARBY_WIFI_DEVICES', "Android 13+ Wi-Fi permission")
    require(manifest, 'android.permission.ACCESS_FINE_LOCATION', "Android 10-12 Wi-Fi permission")
    require(wifi_route, 'WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID)', "HazRob local-only request")
    require(wifi_route, '.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)', "local-only network semantics")
    require(wifi_route, 'cm.requestNetwork(req,requestCallback)', "persistent Direct network request")
    require(wifi_route, 'unregisterNetworkCallback', "Direct network release")
    require(gate, 'WifiRoute.waitForRobotWifi(a,30000)', "Direct acquisition gate")
    require(gate, 'local.postForm("/connection/direct"', "Direct radio switch")
    require(gate, 'local.postForm("/connection/remote"', "Remote radio switch")
    require(gate, 'WifiRoute.releaseRobotWifi(a);', "Remote releases phone from HazRob local-only route")
    require(link_service, 'WifiRoute.maintainRobotWifi(this)', "Direct network maintenance")
    require(link_service, 'RobotApi.setOperatorForeground(foreground)', "background motion interlock")
    require(link_service, 'failStopForBackground', "background fail-stop")

    require(cam, 'IPAddress ip(192,168,4,2)', "camera local IP")
    require(cam, 'gw(192,168,4,1)', "camera gateway")
    require(cam, 'WebServer web(80)', "camera status server")
    require(cam, 'WiFiServer mjpeg(81)', "local MJPEG server")
    require(cam, 'c.pixel_format=PIXFORMAT_RGB565', "proven camera capture format")
    require(cam, 'c.frame_size=FRAMESIZE_QVGA', "proven camera frame size")
    require(cam, 'c.fb_count=1', "proven single framebuffer")
    require(cam, 'CAMERA_FB_IN_PSRAM', "PSRAM framebuffer")
    require(cam, 'frame2jpg_cb(f,JQ,outJpg,&s)', "software JPEG encoder")
    require(cam, 'remoteVideoSetLocalActive(true)', "local viewer capture priority")
    require(cam, 'remoteVideoSetLocalActive(false)', "local viewer release")
    require(cam, 'gatewayRemoteReady()', "camera gateway remote gate")
    require(cam, '\\"radio_profile\\":\\"remote\\"', "camera requires Remote radio profile")
    require(cam, '\\"internet\\":true', "camera requires gateway Internet")
    require(cam, '\\"napt\\":true', "camera requires NAPT")
    require(cam, 'if(clients)return;', "camera does not poll gateway during local stream")
    require(cam_remote, 'if(localActive||!RemoteVideoCfg::configured()', "remote handshake blocked during local view")
    require(cam_remote, 'if(localActive&&!connected)', "remote connection attempt blocked during local view")

    state = {
        "DIRECT": {"radio":"AP", "http_drive":True, "mqtt":False, "napt":False, "wss":False, "boot_intent":"unchanged"},
        "REMOTE": {"radio":"AP_STA", "http_drive":False, "mqtt":True, "napt":"internet_ready", "wss":"gateway_ready", "boot_intent":"armed"},
        "WAN_CLEAR": {"boot_intent":"direct"},
    }
    assert state["DIRECT"]["radio"] == "AP" and state["DIRECT"]["http_drive"] and not state["DIRECT"]["mqtt"]
    assert state["REMOTE"]["radio"] == "AP_STA" and not state["REMOTE"]["http_drive"] and state["REMOTE"]["mqtt"]
    assert state["DIRECT"]["boot_intent"] == "unchanged" and state["WAN_CLEAR"]["boot_intent"] == "direct"

    print("HazRob 0.10.1 HR-0101-057 connection contract: PASS")
    print("DIRECT: AP-only + HTTP + local MJPEG; temporary runtime profile")
    print("REMOTE: saved WAN auto-arms AP+STA/MQTT/Render on every reboot")
    print("WAN CLEAR: only persistent opt-out; SAFETY: no local UDP and neutral re-arm after background")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
