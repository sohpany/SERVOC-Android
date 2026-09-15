#!/usr/bin/env python3
"""Fail-closed HazRob 0.10.1 integration contract for HR-0101-055.

Direct must retain the proven local architecture (SoftAP + HTTP + local MJPEG).
Remote must remain present as a separate STA/MQTT/WSS path. Hardware authority
and current sensor wiring are not allowed to regress.
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
    remote = text("firmware/HazRob/RemoteLink.cpp")
    cam = text("firmware/HazRob-CAM/HazRob-CAM.ino")
    cam_remote = text("firmware/HazRob-CAM/RemoteVideo.cpp")
    local = text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    robot_api = text("app/src/main/java/com/servoc/app/communication/RobotApi.java")

    # Release identity and current hardware authority.
    require(cfg, 'FW[]="HazRob 0.10.1"', "master identity")
    require(cam, 'FW[]="HazRob-CAM 0.10.1"', "camera identity")
    for needle in [
        'HW_PROFILE[]="hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2"',
        'DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32',
        'DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27',
        'SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49',
        'ENV_SDA=13,ENV_SCL=14',
        'RELAY_MQ=25','ALERT_PIN=26','SOUND_PIN=36','IR_PIN=35','LDR_PIN=34',
        'BATTERY_PIN=33','ULTRA_TRIG_PIN=23','ULTRA_ECHO_PIN=39',
    ]:
        require(cfg, needle, "hardware authority")

    # Direct network contract: same fixed local addresses and HTTP control.
    require(local, 'ROBOT_HOST="192.168.4.1"', "Android direct robot host")
    require(local, 'CAMERA_HOST="192.168.4.2"', "Android direct camera host")
    require(master, '#include <WebServer.h>', "Direct HTTP server")
    require(master, 'WebServer web(80)', "Direct HTTP server")
    require(master, 'WiFi.mode(WIFI_AP_STA)', "AP retained alongside remote STA")
    require(master, 'IPAddress ip(192,168,4,1)', "SoftAP fixed IP")
    require(master, 'WiFi.softAP(Cfg::SSID)', "SoftAP start")
    require(master, 'web.on("/drive"', "HTTP drive endpoint")
    require(master, 'web.on("/motion/stop"', "HTTP stop endpoint")
    require(master, 'web.on("/diag"', "HTTP diagnostics endpoint")
    require(master, '\"drive_udp\":{\"installed\":false', "HTTP fallback advertisement")
    forbid(master, '#include <WiFiUdp.h>', "Direct must not require UDP")
    forbid(master, 'driveUdp.begin(', "Direct must not start UDP drive")
    require(robot_api, 'directFastDrive=u!=null&&u.optBoolean("installed",false)', "Android capability fallback")
    require(robot_api, 'getText("/drive?l="+left+"&r="+right,450)', "Android HTTP drive fallback")

    # Remote path remains present and independent.
    require(master, '#include "RemoteLink.h"', "Remote link retained")
    require(master, '#include "RemoteMqtt.h"', "Remote MQTT retained")
    require(master, 'remoteBegin();', "Remote link runtime")
    require(master, 'remoteMqttBegin();', "Remote MQTT runtime")
    require(master, 'remoteUpdate();', "Remote link loop")
    require(master, 'remoteMqttUpdate();', "Remote MQTT loop")
    require(master, 'WiFi.AP.enableNAPT', "Camera internet routing")
    for endpoint in ["/remote/status","/remote/scan","/remote/provision","/remote/retry","/remote/clear"]:
        require(master, f'web.on("{endpoint}"', f"remote maintenance endpoint {endpoint}")
    require(mqtt, 't=="drive"', "Remote drive command")
    require(mqtt, 't=="estop"', "Remote estop command")
    require(remote, 'remoteInternetReady', "Remote internet state")

    # Camera: local MJPEG retains priority, RemoteVideo remains available.
    require(cam, '#include "RemoteVideo.h"', "Remote camera retained")
    require(cam, 'IPAddress ip(192,168,4,2)', "camera local IP")
    require(cam, 'gw(192,168,4,1)', "camera local gateway")
    require(cam, 'WebServer web(80)', "camera status server")
    require(cam, 'WiFiServer mjpeg(81)', "local MJPEG server")
    require(cam, 'c.pixel_format=PIXFORMAT_RGB565', "camera proven capture format")
    require(cam, 'frame2jpg_cb(f,JQ,outJpg,&s)', "software JPEG encoder")
    require(cam, 'remoteVideoSetLocalActive(true)', "local video priority")
    require(cam, 'remoteVideoSetLocalActive(false)', "local video release")
    require(cam, 'remoteVideoBegin(captureJpeg)', "remote video runtime")
    require(cam_remote, 'video_publish_token', "remote camera credential path")

    print("HazRob 0.10.1 HR-0101-055 integration contract: PASS")
    print("Direct: SoftAP + HTTP drive + local MJPEG")
    print("Remote: retained via STA/MQTT/WSS with NAPT when internet-ready")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
