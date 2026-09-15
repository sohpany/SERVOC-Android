#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,needle,label):
    if needle not in src: raise AssertionError(f"HR054 missing {label}: {needle}")
def ban(src,needle,label):
    if needle in src: raise AssertionError(f"HR054 forbidden {label}: {needle}")

def main():
    cam=text("firmware/HazRob-CAM/HazRob-CAM.ino")
    rvcfg=text("firmware/HazRob-CAM/RemoteVideoConfig.h")
    rvcpp=text("firmware/HazRob-CAM/RemoteVideo.cpp")
    master=text("firmware/HazRob/HazRob.ino")
    rlink=text("firmware/HazRob/RemoteLink.cpp")
    cfg=text("firmware/HazRob/Config.h")
    hw=text("firmware/HazRob/Hardware.cpp")
    proto=text("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    service=text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")
    session=text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")
    viewer=text("app/src/main/java/com/servoc/app/camera/RemoteJpegWebSocketClient.java")
    settings=text("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    connui=text("app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java")
    control=text("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    relay=text("remote/video-relay/src/index.js")
    hr053=text("docs/0.10.1/HR053_SYSTEM_COHERENCE_MANIFEST.md")
    hr054=text("docs/0.10.1/HR054_WIFI_STABILITY_MANIFEST.md")

    for n,l in [
        ('WebServer web(80); WiFiServer mjpeg(81)','camera local ports'),('IPAddress ip(192,168,4,2)','camera IP'),
        ('c.pixel_format=PIXFORMAT_RGB565','RGB565'),('c.frame_size=FRAMESIZE_QVGA','QVGA'),('c.fb_count=1','single framebuffer'),
        ('c.grab_mode=CAMERA_GRAB_WHEN_EMPTY','grab mode'),('c.fb_location=CAMERA_FB_IN_PSRAM','PSRAM framebuffer'),
        ('frame2jpg_cb(f,JQ,outJpg,&s)','software JPEG'),('constexpr uint8_t LED=4,JQ=35','JPEG quality'),
        ('remoteVideoSetLocalActive(true)','local arbitration enter'),('remoteVideoSetLocalActive(false)','local arbitration exit')]: req(cam,n,l)
    ban(cam,'PIXFORMAT_JPEG','native JPEG path');ban(cam,'CAMERA_FB_IN_DRAM','DRAM fallback')
    req(control,'"http://"+h+":81/stream"','Android local stream URL')

    for n,l in [
        ('MAX_CONNECT_ATTEMPTS=2','bounded WAN attempts'),('WiFi.setAutoReconnect(false)','STA autoreconnect disabled'),
        ('enterLocalSafeHold()','local-safe hold'),('cloudDetail="wifi_local_safe"','visible local-safe state'),
        ('bool remoteRetry()','explicit WAN retry'),('\\"retry_hold\\"','retry-hold telemetry'),
        ('\\"connect_attempts\\"','attempt telemetry'),('\\"ap_clients\\"','AP client telemetry'),('\\"wifi_channel\\"','channel telemetry')]: req(rlink,n,l)
    ban(rlink,'if(configured&&WiFi.status()!=WL_CONNECTED&&now-connectAt>=RETRY_MS)startSta();','unbounded reconnect loop')
    for n,l in [('WiFi.setAutoReconnect(false)','master deterministic radio policy'),('path=="/remote/retry"','retry endpoint'),('drive_active','scan block during UDP drive')]: req(master,n,l)
    for n,l in [('مسح WAN والعودة لشبكة محلية ثابتة','local-safe UI action'),('retry_hold','hold visibility'),('wifi_channel','channel visibility'),('ap_clients','AP clients visibility')]: req(connui,n,l)

    for n,l in [('HOST[]="hazrob-video-relay.onrender.com"','camera Render host'),('BASE_PATH[]="/hazrob/video"','video base path'),('ROBOT_ID[]="hazrob-001"','camera robot id'),('BOOTSTRAP_PATH[]="/hazrob/bootstrap"','bootstrap path'),('PORT=443','TLS port')]: req(rvcfg,n,l)
    for n,l in [('String path=String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID','publisher path'),('X-HazRob-Client: esp32-cam','camera role'),('if(localActive)','pause remote on local')]: req(rvcpp,n,l)
    for n,l in [('ROBOT_ID="hazrob-001"','Android robot id'),('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Android Hive host'),('VIDEO_HOST="hazrob-video-relay.onrender.com"','Android Render host'),('VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID','viewer path'),('BOOTSTRAP_HTTPS="https://"+VIDEO_HOST+"/hazrob/bootstrap"','Android bootstrap')]: req(service,n,l)
    req(session,'Control readiness intentionally excludes the independent video token/path','control/video independence')
    req(viewer,'if(!"wss".equalsIgnoreCase(u.getScheme()))','WSS enforcement')
    for n,l in [('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Render Hive host'),('ROBOT_ID="hazrob-001"','Render robot id'),('url.pathname==="/hazrob/bootstrap"','Render bootstrap'),('/^\\/hazrob\\/video\\/(publish|view)\\/([^/]+)$/','Render routes'),('role==="esp32-cam"','Render camera role'),('video_publish_token','publish token'),('video_view_token','view token')]: req(relay,n,l)

    for n,l in [('ENV_SDA=13,ENV_SCL=14','ENV pins'),('ENV_I2C_HZ=100000','ENV speed'),('SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49','ADS separation'),('R0_SESSION[]="FIELD-R0-3SESSION-20260913"','R0 authority')]: req(cfg,n,l)
    for n,l in [('TwoWire EnvWire(1)','ENV I2C controller'),('EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)','ENV init'),('envAck76=envProbe(0x76)','0x76 probe'),('envAck77=envProbe(0x77)','0x77 probe'),('id==0x60?1','BME ID'),('id==0x56||id==0x57||id==0x58','BMP IDs'),('\\"p\\":','pressure telemetry')]: req(hw,n,l)
    for n,l in [('finiteAny(e,"p","pressure")','pressure normalization'),('x.put("status_code",status)','status normalization'),('x.put("ack_0x76",ack76)','ACK76 normalization'),('x.put("ack_0x77",ack77)','ACK77 normalization')]: req(proto,n,l)
    for n,l in [('optString("status_code",env.optString("status","NOT READY"))','ENV status UI'),('optString("address","—")','address UI'),('optString("chip_id","—")','chip UI')]: req(settings,n,l)

    req(hr053,'HR-0101-053','HR053 manifest');req(hr054,'HR-0101-054','HR054 manifest')
    print('HR-0101-054 local-WiFi/camera/ENV/Render/Hive/app coherence contract: PASS')
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HR-0101-054 local-WiFi/camera/ENV/Render/Hive/app coherence contract: FAIL: {exc}")
        raise SystemExit(1)
