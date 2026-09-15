#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,needle,label):
    if needle not in src: raise AssertionError(f"HR056 missing {label}: {needle}")
def ban(src,needle,label):
    if needle in src: raise AssertionError(f"HR056 forbidden {label}: {needle}")

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
    wifi=text("app/src/main/java/com/servoc/app/communication/WifiRoute.java")
    relay=text("remote/video-relay/src/index.js")

    # Proven local camera path.
    for n,l in [
        ('WebServer web(80); WiFiServer mjpeg(81)','camera local ports'),('IPAddress ip(192,168,4,2)','camera IP'),
        ('c.pixel_format=PIXFORMAT_RGB565','RGB565'),('c.frame_size=FRAMESIZE_QVGA','QVGA'),('c.fb_count=1','single framebuffer'),
        ('c.grab_mode=CAMERA_GRAB_WHEN_EMPTY','grab mode'),('c.fb_location=CAMERA_FB_IN_PSRAM','PSRAM framebuffer'),
        ('frame2jpg_cb(f,JQ,outJpg,&s)','software JPEG'),('constexpr uint8_t LED=4,JQ=35','JPEG quality'),
        ('remoteVideoSetLocalActive(true)','local arbitration enter'),('remoteVideoSetLocalActive(false)','local arbitration exit')]: req(cam,n,l)
    ban(cam,'PIXFORMAT_JPEG','native JPEG path');ban(cam,'CAMERA_FB_IN_DRAM','DRAM fallback')
    req(control,'"http://"+h+":81/stream"','Android local stream URL')

    # HR056 split radio authority: Direct is AP-only; Remote alone owns STA/WAN/MQTT/NAPT.
    for n,l in [
        ('enum class RadioProfile:uint8_t{DIRECT=0,REMOTE=1}','radio state machine'),
        ('configureSoftAp(WIFI_AP);','Direct AP-only'),('configureSoftAp(WIFI_AP_STA);','Remote AP+STA'),
        ('remoteSetTransportEnabled(false,persistMode)','Direct WAN disable'),('remoteMqttDisconnect();','Direct MQTT stop'),
        ('WiFi.AP.enableNAPT(false)','Direct NAPT stop'),('WiFi.AP.enableNAPT(ready)','Remote NAPT readiness')]: req(master,n,l)
    for n,l in [
        ('MAX_CONNECT_ATTEMPTS=2','bounded WAN attempts'),('WiFi.setAutoReconnect(false)','STA autoreconnect disabled'),
        ('enterLocalSafeHold()','local-safe hold'),('cloudDetail="wifi_local_safe"','visible local-safe state'),
        ('bool remoteRetry()','explicit WAN retry'),('\\"retry_hold\\"','retry-hold telemetry'),
        ('\\"connect_attempts\\"','attempt telemetry'),('\\"ap_clients\\"','AP client telemetry'),('\\"wifi_channel\\"','channel telemetry')]: req(rlink,n,l)
    ban(rlink,'WiFi.mode(','RemoteLink radio ownership')
    req(wifi,'WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID)','Android local-only HazRob request')
    req(connui,'لا يتم استكشاف الشبكات من ESP32 في Direct','Direct no-scan developer UX')
    req(connui,'مسح WAN والعودة إلى Direct ثابت','Direct recovery UI')
    ban(connui,'scanNetworks()','stale Direct Wi-Fi scan action')

    # Remote video is gated by the main gateway and cannot start or capture against a local viewer.
    for n,l in [('HOST[]="hazrob-video-relay.onrender.com"','camera Render host'),('BASE_PATH[]="/hazrob/video"','video base path'),('ROBOT_ID[]="hazrob-001"','camera robot id'),('BOOTSTRAP_PATH[]="/hazrob/bootstrap"','bootstrap path'),('PORT=443','TLS port')]: req(rvcfg,n,l)
    for n,l in [
        ('String path=String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID','publisher path'),
        ('X-HazRob-Client: esp32-cam','camera role'),('if(localActive||!RemoteVideoCfg::configured()','no WSS handshake during local view'),
        ('if(localActive&&!connected)','no WSS connection attempt during local view')]: req(rvcpp,n,l)
    req(cam,'gatewayRemoteReady()','gateway gate')
    req(cam,'\\"radio_profile\\":\\"remote\\"','Remote profile requirement')
    req(cam,'\\"internet\\":true','Internet requirement')
    req(cam,'\\"napt\\":true','NAPT requirement')
    req(cam,'if(clients)return;','no gateway polling during local MJPEG')

    # Cloud coordinates remain identical across camera, Android and Render.
    for n,l in [('ROBOT_ID="hazrob-001"','Android robot id'),('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Android Hive host'),('VIDEO_HOST="hazrob-video-relay.onrender.com"','Android Render host'),('VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID','viewer path'),('BOOTSTRAP_HTTPS="https://"+VIDEO_HOST+"/hazrob/bootstrap"','Android bootstrap')]: req(service,n,l)
    req(session,'Control readiness intentionally excludes the independent video token/path','control/video independence')
    req(viewer,'if(!"wss".equalsIgnoreCase(u.getScheme()))','WSS enforcement')
    for n,l in [('MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud"','Render Hive host'),('ROBOT_ID="hazrob-001"','Render robot id'),('url.pathname==="/hazrob/bootstrap"','Render bootstrap'),('/^\\/hazrob\\/video\\/(publish|view)\\/([^/]+)$/','Render routes'),('role==="esp32-cam"','Render camera role'),('video_publish_token','publish token'),('video_view_token','view token')]: req(relay,n,l)

    # ENV/ADS authority must remain untouched by connection remediation.
    for n,l in [('ENV_SDA=13,ENV_SCL=14','ENV pins'),('ENV_I2C_HZ=100000','ENV speed'),('SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49','ADS separation'),('R0_SESSION[]="FIELD-R0-3SESSION-20260913"','R0 authority')]: req(cfg,n,l)
    for n,l in [('TwoWire EnvWire(1)','ENV I2C controller'),('EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)','ENV init'),('envAck76=envProbe(0x76)','0x76 probe'),('envAck77=envProbe(0x77)','0x77 probe'),('id==0x60?1','BME ID'),('id==0x56||id==0x57||id==0x58','BMP IDs'),('\\"p\\":','pressure telemetry')]: req(hw,n,l)
    for n,l in [('finiteAny(e,"p","pressure")','pressure normalization'),('x.put("status_code",status)','status normalization'),('x.put("ack_0x76",ack76)','ACK76 normalization'),('x.put("ack_0x77",ack77)','ACK77 normalization')]: req(proto,n,l)
    for n,l in [('optString("status_code",env.optString("status","NOT READY"))','ENV status UI'),('optString("address","—")','address UI'),('optString("chip_id","—")','chip UI')]: req(settings,n,l)

    print('HR-0101-056 local-WiFi/camera/ENV/Render/Hive/app coherence contract: PASS')
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HR-0101-056 local-WiFi/camera/ENV/Render/Hive/app coherence contract: FAIL: {exc}")
        raise SystemExit(1)
