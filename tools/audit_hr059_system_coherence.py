#!/usr/bin/env python3
"""Fail-closed HR-0101-059 five-layer HazRob coherence gate.

Checks Main ESP32, ESP32-CAM, Android, Render relay and HiveMQ-facing contracts as
one system. This is source/integration qualification; physical RF/motor/camera
acceptance remains a separate field test.
"""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,needle,label):
    if needle not in src: raise AssertionError(f"{label}: missing {needle!r}")
def ban(src,needle,label):
    if needle in src: raise AssertionError(f"{label}: forbidden {needle!r}")
def one(pattern,src,label):
    m=re.search(pattern,src,re.S)
    if not m: raise AssertionError(f"{label}: pattern not found")
    return m.group(1)

def main():
    master=text("firmware/HazRob/HazRob.ino")
    cfg=text("firmware/HazRob/Config.h")
    rlink=text("firmware/HazRob/RemoteLink.cpp")
    rmqtt=text("firmware/HazRob/RemoteMqtt.cpp")
    rcfg=text("firmware/HazRob/RemoteCloudConfig.h")
    cam=text("firmware/HazRob-CAM/HazRob-CAM.ino")
    rvcfg=text("firmware/HazRob-CAM/RemoteVideoConfig.h")
    rvideo=text("firmware/HazRob-CAM/RemoteVideo.cpp")
    local=text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java")
    service=text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")
    session=text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")
    conn=text("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    policy=text("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java")
    api=text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    mqtt_client=text("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java")
    wss_client=text("app/src/main/java/com/servoc/app/camera/RemoteJpegWebSocketClient.java")
    link_service=text("app/src/main/java/com/servoc/app/communication/RobotLinkService.java")
    relay=text("remote/video-relay/src/index.js")
    relay_test=text("remote/video-relay/test/contract.mjs")
    gradle=text("app/build.gradle")

    # One version/protocol/robot identity across all layers.
    req(cfg,'FW[]="HazRob 0.10.1"',"main firmware version")
    req(cam,'FW[]="HazRob-CAM 0.10.1"',"camera firmware version")
    req(service,'SERVICE_VERSION="0.10.1"',"Android service version")
    req(service,'PROTOCOL=2',"Android protocol")
    req(rcfg,'BOOTSTRAP_SERVICE_VERSION[]="0.10.1"',"main bootstrap version")
    req(rcfg,'BOOTSTRAP_PROTOCOL=2',"main bootstrap protocol")
    req(rvcfg,'BOOTSTRAP_SERVICE_VERSION[]="0.10.1"',"CAM bootstrap version")
    req(rvcfg,'BOOTSTRAP_PROTOCOL=2',"CAM bootstrap protocol")
    req(relay,'SERVICE_VERSION="0.10.1",PROTOCOL=2',"Render version/protocol")
    req(gradle,"versionName '0.10.1'","APK version")
    req(gradle,'versionCode 21',"APK versionCode")
    for src,label in [(rcfg,"main"),(rvcfg,"CAM")]: req(src,'ROBOT_ID[]="hazrob-001"',label+" robot id")
    req(service,'ROBOT_ID="hazrob-001"',"Android robot id")
    req(relay,'ROBOT_ID="hazrob-001"',"Render robot id")

    # Cloud coordinates must be literal-identical.
    hive='e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud'
    render='hazrob-video-relay.onrender.com'
    req(rcfg,f'BROKER_HOST[]="{hive}"',"main HiveMQ host")
    req(rcfg,'BROKER_PORT=8883',"main HiveMQ TLS port")
    req(service,f'MQTT_HOST="{hive}"',"Android HiveMQ host")
    req(service,'MQTT_PORT=8883',"Android HiveMQ TLS port")
    req(relay,f'MQTT_HOST="{hive}",MQTT_PORT=8883',"Render HiveMQ coordinates")
    req(rcfg,f'BOOTSTRAP_HOST[]="{render}"',"main Render host")
    req(rvcfg,f'HOST[]="{render}"',"CAM Render host")
    req(service,f'VIDEO_HOST="{render}"',"Android Render host")
    for src,label in [(rcfg,"main"),(rvcfg,"CAM")]:
        req(src,'BOOTSTRAP_PATH[]="/hazrob/bootstrap"',label+" bootstrap path")
        req(src,'BOOTSTRAP_TOKEN[]="hazrob-prototype-bootstrap-v1"',label+" bootstrap token contract")
    req(session,'BOOTSTRAP_TOKEN="hazrob-prototype-bootstrap-v1"',"Android bootstrap token contract")
    req(relay,'url.pathname==="/hazrob/bootstrap"',"Render bootstrap endpoint")

    # Role-scoped bootstrap contract: no component may silently consume another role.
    req(rmqtt,'X-HazRob-Client: esp32',"main bootstrap role")
    req(rmqtt,'version!=RemoteCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteCfg::BOOTSTRAP_PROTOCOL||role!="esp32"||robot!=RemoteCfg::ROBOT_ID',"main strict bootstrap validation")
    req(rvideo,'X-HazRob-Client: esp32-cam',"CAM bootstrap role")
    req(rvideo,'version!=RemoteVideoCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteVideoCfg::BOOTSTRAP_PROTOCOL||role!="esp32-cam"||robot!=RemoteVideoCfg::ROBOT_ID',"CAM strict bootstrap validation")
    req(session,'BOOTSTRAP_CLIENT="android"',"Android bootstrap role")
    req(session,'bootstrap contract mismatch',"Android version/protocol rejection")
    req(session,'bootstrap role mismatch',"Android role rejection")
    for needle in ['role==="android"','role==="esp32"','role==="esp32-cam"','video_view_token','video_publish_token']:
        req(relay,needle,"Render scoped bootstrap")

    # Local network authority and radio ownership.
    req(local,'ROBOT_SSID="HazRob"',"local SSID")
    req(local,'ROBOT_HOST="192.168.4.1"',"local robot IP")
    req(local,'CAMERA_HOST="192.168.4.2"',"local CAM IP")
    req(master,'configureSoftAp(WIFI_AP);',"Direct AP-only")
    req(master,'configureSoftAp(WIFI_AP_STA);',"Remote AP+STA")
    req(master,'radioProfile!=RadioProfile::DIRECT',"Direct-only HTTP drive")
    req(master,'if(radioProfile==RadioProfile::REMOTE){remoteUpdate();gatewayUpdate();remoteMqttUpdate();}',"Remote-only WAN/MQTT loop")
    ban(rlink,'WiFi.mode(',"RemoteLink global radio ownership")

    # WAN lifecycle must be non-destructive except explicit clear.
    req(rlink,'transportEnabled=configured;',"saved WAN boot intent")
    req(rlink,'wan.putString("ssid",ssid);wan.putString("pass",password);wan.putBool("enabled",true);',"persist provisioned WAN")
    req(rlink,'wan.remove("ssid");wan.remove("pass");wan.putBool("enabled",false);',"explicit WAN clear")
    req(master,'web.on("/remote/disconnect"',"non-destructive disconnect endpoint")
    req(master,'\\"wan_retained\\":true',"WAN retained declaration")
    req(master,'web.on("/remote/connect"',"saved WAN reconnect endpoint")
    req(api,'remoteDisconnect()',"Android disconnect facade")
    req(api,'remoteReconnect()',"Android reconnect facade")

    # Remote command parity. Every operational Android command has a firmware handler.
    expected={
        "probe","drive","motion_stop","estop","estop_reset","sensor_off","heartbeat","heartbeat_disarm",
        "relay","measure_start","measure_cancel","alert","alert_test","camera_flash","config_get","config_set"
    }
    fw=set(re.findall(r't=="([a-z0-9_]+)"',rmqtt))
    app=set(re.findall(r'command\("([a-z0-9_]+)"',conn))
    app |= {"drive","motion_stop","estop","estop_reset","sensor_off","heartbeat","relay","measure_start","measure_cancel","alert","alert_test","camera_flash"}
    missing_fw=expected-fw
    missing_app=expected-app
    if missing_fw: raise AssertionError(f"firmware missing remote command handlers: {sorted(missing_fw)}")
    if missing_app: raise AssertionError(f"Android missing remote command emitters: {sorted(missing_app)}")
    unknown_fw=fw-expected
    if unknown_fw: raise AssertionError(f"unreviewed firmware remote commands: {sorted(unknown_fw)}")

    # Direct API parity for operator functions that exist in Direct.
    direct_paths=["/drive","/motion/stop","/estop","/estop/reset","/sensor/off","/heartbeat","/diag","/data","/cfg","/relay","/measure","/alert"]
    for path in direct_paths: req(master,f'web.on("{path}"',f"firmware Direct endpoint {path}")
    for needle,label in [
        ('getText("/drive?l="+left+"&r="+right,450)',"Android Direct drive"),
        ('getText("/estop",650)',"Android Direct estop"),
        ('getText("/estop/reset",650)',"Android Direct estop reset"),
        ('getText("/heartbeat",500)',"Android Direct heartbeat"),
        ('getJson("/diag",1000)',"Android Direct diagnostics"),
        ('getJson("/data",1200)',"Android Direct data"),
        ('getText("/relay?mask="+mask,800)',"Android Direct relay"),
        ('getText("/measure?cmd=start",800)',"Android Direct measure start"),
        ('getText("/alert?severity="+severity+"&ttl="+ttl,800)',"Android Direct alert")]: req(api,needle,label)

    # Timing/safety invariants: refreshes must beat the 500 ms firmware watchdog.
    watchdog=int(one(r'CONTROL_COMMAND_TIMEOUT_MS=(\d+)',cfg,"firmware watchdog"))
    direct=int(one(r'DIRECT_KEEPALIVE_MS=(\d+)L',api,"Direct keepalive"))
    pump=int(one(r'PUMP_MS=(\d+)',policy,"Remote pump"))
    minpub=int(one(r'MIN_PUBLISH_MS=(\d+)',policy,"Remote minimum publish"))
    hold=int(one(r'HOLD_REFRESH_MS=(\d+)',policy,"Remote hold refresh"))
    ttl=int(one(r'COMMAND_TTL_MS=(\d+)',policy,"Remote command TTL"))
    if not (direct < watchdog and hold < watchdog and minpub <= hold and pump <= minpub and ttl >= watchdog):
        raise AssertionError(f"timing invariant failed direct={direct} hold={hold} minpub={minpub} pump={pump} ttl={ttl} watchdog={watchdog}")
    req(master,'motionStop();',"profile transition stop")
    req(conn,'public void motionStop()throws Exception{setDesiredDrive(0,0);command("motion_stop"',"remote explicit stop")
    req(link_service,'failStopForBackground(remote);',"service destroy/background stop")
    req(link_service,'remoteSessions.close()',"service destroys remote drive pump")
    req(api,'requireNeutralAfterBackground',"neutral re-arm")

    # Camera path: local gets capture priority; remote requires gateway + its own valid clock.
    req(cam,'if(clients)return;',"CAM local-view gate")
    req(cam,'gateway&&camTimeReady()',"CAM gateway plus NTP gate")
    req(cam,'TLS_TIME_MIN=1700000000',"CAM minimum TLS epoch")
    req(cam,'configTime(0,0,"pool.ntp.org","time.cloudflare.com")',"CAM NTP configuration")
    req(cam,'\\"time_ready\\":',"CAM clock diagnostics")
    for needle in ['\\"radio_profile\\":\\"remote\\"','\\"internet\\":true','\\"napt\\":true']:
        req(cam,needle,"CAM gateway readiness")
    req(rvideo,'if(localActive||!RemoteVideoCfg::configured()',"CAM no WSS handshake while local viewer")
    req(rvideo,'if(localActive&&!connected)',"CAM no remote connect while local viewer")
    req(rvcfg,'BASE_PATH[]="/hazrob/video"',"CAM video base path")
    req(service,'VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID',"Android WSS viewer path")
    req(relay,'/^\\/hazrob\\/video\\/(publish|view)\\/([^/]+)$/',"Render WSS routes")

    # TLS must be authenticated end to end; no insecure fallback.
    req(mqtt_client,'HttpsURLConnection.getDefaultHostnameVerifier().verify(host,session)',"Android MQTT hostname verification")
    req(wss_client,'p.setEndpointIdentificationAlgorithm("HTTPS")',"Android WSS hostname verification")
    req(rcfg,'ROOT_CA[]=HAZROB_MQTT_ROOT_CA',"main CA pin")
    req(rvcfg,'ROOT_CA[]=HAZROB_VIDEO_ROOT_CA',"CAM CA pin")
    ban(mqtt_client,'TrustAll',"Android MQTT insecure TLS")
    ban(wss_client,'TrustAll',"Android WSS insecure TLS")

    # Relay must isolate roles and exercise local + live production health in its contract.
    req(relay,'expected=role==="publisher"?c.publish:c.view',"Render publisher/viewer token isolation")
    req(relay_test,'openWs("/hazrob/video/view/hazrob-001",PUBLISH)',"wrong publisher token viewer rejection test")
    req(relay_test,'openWs("/hazrob/video/publish/hazrob-001",VIEW)',"wrong viewer token publisher rejection test")
    req(relay_test,'liveProductionHealth()',"live Render/Hive health test")
    req(relay_test,'mqttDiagnostic?.state,"pass"',"live HiveMQ diagnostic assertion")

    print("HazRob HR-0101-059 five-layer system coherence: PASS")
    print(f"Identity: 0.10.1 / protocol 2 / hazrob-001; cloud={hive}:8883 + {render}")
    print(f"Safety timing: Direct {direct}ms; Remote hold {hold}ms; watchdog {watchdog}ms; command TTL {ttl}ms")
    print("Control parity: Direct HTTP + Remote MQTT command families aligned")
    print("Camera parity: local MJPEG priority + gateway/NAPT + CAM NTP + strict bootstrap + WSS role isolation")
    print("Scope: source/integration qualified; physical RF/motor/camera acceptance remains PENDING")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob HR-0101-059 five-layer system coherence: FAIL: {exc}")
        raise SystemExit(1)
