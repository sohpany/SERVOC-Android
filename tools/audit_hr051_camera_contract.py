#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(path):
    return (ROOT / path).read_text(encoding="utf-8")

def req(src, needle, label):
    if needle not in src:
        raise AssertionError(f"HR053 missing {label}: {needle}")

def ban(src, needle, label):
    if needle in src:
        raise AssertionError(f"HR053 forbidden {label}: {needle}")

def main():
    cam=text("firmware/HazRob-CAM/HazRob-CAM.ino")
    remote_cfg=text("firmware/HazRob-CAM/RemoteVideoConfig.h")
    remote_cpp=text("firmware/HazRob-CAM/RemoteVideo.cpp")
    cfg=text("firmware/HazRob/Config.h")
    hw=text("firmware/HazRob/Hardware.cpp")
    proto=text("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    service=text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")
    remote_session=text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")
    viewer=text("app/src/main/java/com/servoc/app/camera/RemoteJpegWebSocketClient.java")
    settings=text("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    control=text("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    relay=text("remote/video-relay/src/index.js")
    change=text("docs/0.10.1/HR053_SYSTEM_COHERENCE_MANIFEST.md")

    # Restored direct/local camera baseline.
    for n,l in [
        ('#include <WebServer.h>','WebServer'),
        ('WebServer web(80); WiFiServer mjpeg(81)','local ports'),
        ('IPAddress ip(192,168,4,2)','camera IP'),
        ('c.pixel_format=PIXFORMAT_RGB565','RGB565 capture'),
        ('c.frame_size=FRAMESIZE_QVGA','QVGA'),
        ('c.fb_count=1','single framebuffer'),
        ('c.grab_mode=CAMERA_GRAB_WHEN_EMPTY','grab mode'),
        ('c.fb_location=CAMERA_FB_IN_PSRAM','PSRAM framebuffer'),
        ('frame2jpg_cb(f,JQ,outJpg,&s)','software JPEG'),
        ('constexpr uint8_t LED=4,JQ=35','JPEG quality'),
        ('if(!psOK)return camErr=ESP_ERR_NO_MEM,false','PSRAM requirement'),
        ('web.on("/stream"','stream redirect'),
        ('remoteVideoSetLocalActive(true)','local/remote arbitration enter'),
        ('remoteVideoSetLocalActive(false)','local/remote arbitration exit')]: req(cam,n,l)
    ban(cam,'PIXFORMAT_JPEG','native JPEG path')
    ban(cam,'CAMERA_FB_IN_DRAM','DRAM fallback')
    req(control,'"http://"+h+":81/stream"','Android stream URL')

    # Render video contract must match firmware, relay, and Android exactly.
    for n,l in [
        ('HOST[]="hazrob-video-relay.onrender.com"','camera Render host'),
        ('BASE_PATH[]="/hazrob/video"','camera video base path'),
        ('ROBOT_ID[]="hazrob-001"','camera robot id'),
        ('BOOTSTRAP_PATH[]="/hazrob/bootstrap"','camera bootstrap path'),
        ('PORT=443','camera TLS port')]: req(remote_cfg,n,l)
    for n,l in [
        ('String path=String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID','publisher path'),
        ('X-HazRob-Client: esp32-cam','camera bootstrap role'),
        ('if(localActive)','remote pause while local active')]: req(remote_cpp,n,l)
    for n,l in [
        ('ROBOT_ID="hazrob-001"','Android robot id'),
        ('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Android HiveMQ host'),
        ('VIDEO_HOST="hazrob-video-relay.onrender.com"','Android Render host'),
        ('VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID','Android view path'),
        ('BOOTSTRAP_HTTPS="https://"+VIDEO_HOST+"/hazrob/bootstrap"','Android bootstrap path')]: req(service,n,l)
    for n,l in [
        ('Control readiness intentionally excludes the independent video token/path','control/video independence'),
        ('X-HazRob-Client','bootstrap client header')]: req(remote_session,n,l)
    req(viewer,'if(!"wss".equalsIgnoreCase(u.getScheme()))','Android WSS enforcement')
    for n,l in [
        ('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Render HiveMQ host'),
        ('ROBOT_ID="hazrob-001"','Render robot id'),
        ('url.pathname==="/hazrob/bootstrap"','Render bootstrap route'),
        ('/^\\/hazrob\\/video\\/(publish|view)\\/([^/]+)$/','Render publish/view route'),
        ('role==="esp32-cam"','Render camera role'),
        ('video_publish_token','Render publish token'),
        ('video_view_token','Render view token')]: req(relay,n,l)

    # ENV / pressure coherence retained.
    for n,l in [
        ('ENV_SDA=13,ENV_SCL=14','ENV pins'),
        ('ENV_I2C_HZ=100000','ENV speed'),
        ('SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49','ADS separation'),
        ('R0_SESSION[]="FIELD-R0-3SESSION-20260913"','R0 authority')]: req(cfg,n,l)
    for n,l in [
        ('TwoWire EnvWire(1)','second I2C controller'),
        ('EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)','ENV init'),
        ('envAck76=envProbe(0x76)','0x76 probe'),
        ('envAck77=envProbe(0x77)','0x77 probe'),
        ('id==0x60?1','BME280 ID'),
        ('id==0x56||id==0x57||id==0x58','BMP280 IDs'),
        ('\\"p\\":','pressure telemetry'),
        ('\\"status\\":\\"','ENV status telemetry')]: req(hw,n,l)
    for n,l in [
        ('finiteAny(e,"p","pressure")','pressure normalization'),
        ('x.put("status_code",status)','status normalization'),
        ('x.put("ack_0x76",ack76)','ACK76 normalization'),
        ('x.put("ack_0x77",ack77)','ACK77 normalization')]: req(proto,n,l)
    for n,l in [
        ('optString("status_code",env.optString("status","NOT READY"))','status visibility'),
        ('optString("address","—")','address visibility'),
        ('optString("chip_id","—")','chip visibility'),
        ('optBoolean("ack_0x76",false)','ACK76 visibility'),
        ('optBoolean("ack_0x77",false)','ACK77 visibility')]: req(settings,n,l)

    req(change,'HR-0101-053','manifest')
    print('HR-0101-053 camera/ENV/Render/Hive/app coherence contract: PASS')
    return 0

if __name__ == "__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HR-0101-053 camera/ENV/Render/Hive/app coherence contract: FAIL: {exc}")
        raise SystemExit(1)
