#!/usr/bin/env python3
"""Compatibility camera/network gate for the canonical HazRob 0.10.1 runtime.

The camera remains STA-only at 192.168.4.2, with Main as the sole HazRob AP at
192.168.4.1. Camera identity/JPEG validation remains strict when camera capability is used,
but camera readiness must not block entry to the rest of the application.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def req(s,n,l):
    if n not in s: raise AssertionError(f"{l}: missing {n!r}")
def ban(s,n,l):
    if n in s: raise AssertionError(f"{l}: forbidden {n!r}")

def main():
    cam=text("firmware/HazRob-CAM/HazRob-CAM.ino")
    master=text("firmware/HazRob/HazRob.ino")
    cfg=text("firmware/HazRob/Config.h")
    rvcfg=text("firmware/HazRob-CAM/RemoteVideoConfig.h")
    rvideo=text("firmware/HazRob-CAM/RemoteVideo.cpp")
    camera_api=text("app/src/main/java/com/servoc/app/camera/CameraApi.java")
    gate=text("app/src/main/java/com/servoc/app/auth/ConnectionGate.java")
    local=text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    service=text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")

    for s,n,l in [
        (cfg,'FW[]="HazRob 0.10.1"','Main firmware identity'),
        (cfg,'SSID[]="HazRob"','Main AP SSID'),
        (cfg,'CAM[]="192.168.4.2"','Main CAM coordinate'),
        (cam,'SSID[]="HazRob",FW[]="HazRob-CAM 0.10.1"','CAM firmware identity'),
        (local,'ROBOT_SSID="HazRob"','Android SSID'),
        (local,'ROBOT_HOST="192.168.4.1"','Android Main IP'),
        (local,'CAMERA_HOST="192.168.4.2"','Android CAM IP'),
        (local,'CAMERA_FIRMWARE_BANNER="HazRob-CAM 0.10.1"','Android CAM identity')]: req(s,n,l)

    ban(cfg,'FW[]="HazRob 0.10.1 HR','numbered Main runtime identity')
    ban(cam,'FW[]="HazRob-CAM 0.10.1 HR','numbered CAM runtime identity')

    for n,l in [
        ('WiFi.mode(WIFI_STA)','Arduino STA mode'),
        ('esp_wifi_set_mode(WIFI_MODE_STA)','ESP-IDF STA mode'),
        ('WiFi.setAutoReconnect(false)','controlled reconnect'),
        ('void radioGuard()','radio guard'),
        ('WiFi.config(ip,gw,sn,d1,d2)','static CAM network'),
        ('WiFi.begin(SSID)','HazRob join'),
        ('IPAddress ip(192,168,4,2),gw(192,168,4,1)','CAM static IP/gateway')]: req(cam,n,l)
    ban(cam,'WiFi.softAP(','CAM AP creation')
    ban(cam,'WIFI_AP_STA','CAM AP+STA mode')

    for n,l in [
        ('WebServer web(80); WiFiServer mjpeg(81)','CAM HTTP/MJPEG ports'),
        ('c.pixel_format=PIXFORMAT_RGB565','RGB565 capture'),
        ('c.frame_size=FRAMESIZE_QVGA','QVGA capture'),
        ('frame2jpg_cb(f,JQ,outJpg,&s)','software JPEG'),
        ('remoteVideoSetLocalActive(true)','local arbitration enter'),
        ('remoteVideoSetLocalActive(false)','local arbitration exit')]: req(cam,n,l)
    ban(cam,'PIXFORMAT_JPEG','native JPEG path')
    for n,l in [
        ('LocalNetworkContract.CAMERA_FIRMWARE_BANNER.equals(fw)','Android CAM firmware gate'),
        ('j.optInt("wifi_mode",-1)!=1','Android STA-only status gate'),
        ('LocalNetworkContract.CAMERA_HOST.equals(ip)','Android CAM IP gate'),
        ('prev==0xFF&&cur==0xD8','JPEG SOI gate'),
        ('prev==0xFF&&cur==0xD9&&jpegBytes>128','JPEG EOI gate')]: req(camera_api,n,l)
    req(camera_api,'return localStreamReady();','strict camera capability validation')
    req(gate,'final boolean ready=network&&robotReady','Main-only Direct admission')
    ban(gate,'directCamera.directReady()','camera-gated Direct admission')
    req(gate,'directCamera.status()','independent camera status probe')

    for n,l in [
        ('applyRadioProfile(RadioProfile::DIRECT,false);','Direct boot'),
        ('configureSoftAp(WIFI_AP,false);','Direct AP-only'),
        ('configureSoftAp(WIFI_AP_STA);','explicit Remote AP+STA'),
        ('remoteMqttDisconnect();','Direct MQTT shutdown'),
        ('WiFi.AP.enableNAPT(false)','Direct NAPT shutdown')]: req(master,n,l)

    for n,l in [
        ('gatewayRemoteReady()','CAM gateway readiness'),
        ('gateway&&camTimeReady()','gateway/time gate'),
        ('configTime(0,0,"pool.ntp.org","time.cloudflare.com")','CAM NTP'),
        ('TLS_TIME_MIN=1700000000','TLS epoch floor')]: req(cam,n,l)
    for n,l in [
        ('HOST[]="hazrob-video-relay.onrender.com"','Render host'),
        ('BASE_PATH[]="/hazrob/video"','video base path'),
        ('BOOTSTRAP_SERVICE_VERSION[]="0.10.1"','bootstrap version'),
        ('BOOTSTRAP_PROTOCOL=2','bootstrap protocol')]: req(rvcfg,n,l)
    req(rvideo,'js(r,"client")!="esp32-cam"','strict CAM bootstrap client identity')
    req(rvideo,'js(r,"robot")!=RemoteVideoCfg::ROBOT_ID','strict CAM robot identity')
    req(service,'VIDEO_HOST="hazrob-video-relay.onrender.com"','Android Render host')

    for n,l in [
        ('ENV_SDA=13,ENV_SCL=14','ENV pins'),
        ('SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49','ADS bus'),
        ('R0_SESSION[]="FIELD-R0-3SESSION-20260913"','R0 campaign')]: req(cfg,n,l)

    print("HazRob 0.10.1 camera/network contract: PASS")
    print("CAM=STA-only 192.168.4.2; Main sole AP=HazRob/192.168.4.1; camera is capability state, not admission gate; physical acceptance=PENDING")
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob 0.10.1 camera/network contract: FAIL: {exc}")
        raise SystemExit(1)
