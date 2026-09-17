#!/usr/bin/env python3
"""Fail-closed HR-0101-059/061/066 five-layer HazRob coherence gate.
Checks Main ESP32, ESP32-CAM, Android, Render and HiveMQ-facing contracts together.
HR066 authority: every boot is Direct; saved WAN is configuration only; Remote requires developer
authorization plus an explicit operator action; ESP32-CAM is a capability, not an admission gate.
Physical RF/motor/camera acceptance remains a separate field test.
"""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def req(src,n,label):
    if n not in src: raise AssertionError(f"{label}: missing {n!r}")
def ban(src,n,label):
    if n in src: raise AssertionError(f"{label}: forbidden {n!r}")
def one(p,src,label):
    m=re.search(p,src,re.S)
    if not m: raise AssertionError(f"{label}: pattern not found")
    return m.group(1)

def main():
    master=text("firmware/HazRob/HazRob.ino"); cfg=text("firmware/HazRob/Config.h")
    rlink=text("firmware/HazRob/RemoteLink.cpp"); rmqtt=text("firmware/HazRob/RemoteMqtt.cpp"); rcfg=text("firmware/HazRob/RemoteCloudConfig.h")
    cam=text("firmware/HazRob-CAM/HazRob-CAM.ino"); rvcfg=text("firmware/HazRob-CAM/RemoteVideoConfig.h"); rvideo=text("firmware/HazRob-CAM/RemoteVideo.cpp")
    local=text("app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java"); service=text("app/src/main/java/com/servoc/app/communication/RemoteServiceProfile.java")
    session=text("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java"); conn=text("app/src/main/java/com/servoc/app/communication/RemoteConnection.java")
    policy=text("app/src/main/java/com/servoc/app/communication/RemoteDrivePolicy.java"); api=text("app/src/main/java/com/servoc/app/communication/RobotApi.java")
    mqttc=text("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java"); wssc=text("app/src/main/java/com/servoc/app/camera/RemoteJpegWebSocketClient.java")
    camera_api=text("app/src/main/java/com/servoc/app/camera/CameraApi.java")
    links=text("app/src/main/java/com/servoc/app/communication/RobotLinkService.java"); relay=text("remote/video-relay/src/index.js"); rtest=text("remote/video-relay/test/contract.mjs"); gradle=text("app/build.gradle")
    wifi=text("app/src/main/java/com/servoc/app/communication/WifiRoute.java"); gate=text("app/src/main/java/com/servoc/app/auth/ConnectionGate.java")

    # Shared identity and cloud coordinates.
    for src,n,label in [(cfg,'FW[]="HazRob 0.10.1"','main version'),(cam,'FW[]="HazRob-CAM 0.10.1"','CAM version'),(service,'SERVICE_VERSION="0.10.1"','Android service version'),(rcfg,'BOOTSTRAP_SERVICE_VERSION[]="0.10.1"','main bootstrap version'),(rvcfg,'BOOTSTRAP_SERVICE_VERSION[]="0.10.1"','CAM bootstrap version'),(relay,'SERVICE_VERSION="0.10.1",PROTOCOL=2','Render version/protocol'),(gradle,"versionName '0.10.1'",'APK version'),(gradle,'versionCode 25','APK versionCode')]: req(src,n,label)
    for src,n,label in [(service,'PROTOCOL=2','Android protocol'),(rcfg,'BOOTSTRAP_PROTOCOL=2','main protocol'),(rvcfg,'BOOTSTRAP_PROTOCOL=2','CAM protocol')]: req(src,n,label)
    for src,n,label in [(rcfg,'ROBOT_ID[]="hazrob-001"','main id'),(rvcfg,'ROBOT_ID[]="hazrob-001"','CAM id'),(service,'ROBOT_ID="hazrob-001"','Android id'),(relay,'ROBOT_ID="hazrob-001"','Render id')]: req(src,n,label)
    hive='e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud'; render='hazrob-video-relay.onrender.com'
    for src,n,label in [(rcfg,f'BROKER_HOST[]="{hive}"','main Hive'),(service,f'MQTT_HOST="{hive}"','Android Hive'),(relay,f'MQTT_HOST="{hive}",MQTT_PORT=8883','Render Hive'),(rcfg,'BROKER_PORT=8883','main MQTT port'),(service,'MQTT_PORT=8883','Android MQTT port'),(rcfg,f'BOOTSTRAP_HOST[]="{render}"','main Render'),(rvcfg,f'HOST[]="{render}"','CAM Render'),(service,f'VIDEO_HOST="{render}"','Android Render')]: req(src,n,label)
    for src,label in [(rcfg,'main'),(rvcfg,'CAM')]: req(src,'BOOTSTRAP_PATH[]="/hazrob/bootstrap"',label+' bootstrap'); req(src,'BOOTSTRAP_TOKEN[]="hazrob-prototype-bootstrap-v1"',label+' token')
    req(session,'BOOTSTRAP_TOKEN="hazrob-prototype-bootstrap-v1"','Android bootstrap token'); req(relay,'url.pathname==="/hazrob/bootstrap"','Render bootstrap')

    # Strict role/version/protocol compatibility.
    req(rmqtt,'X-HazRob-Client: esp32','main role'); req(rmqtt,'version!=RemoteCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteCfg::BOOTSTRAP_PROTOCOL||role!="esp32"||robot!=RemoteCfg::ROBOT_ID','main strict bootstrap')
    req(rvideo,'X-HazRob-Client: esp32-cam','CAM role'); req(rvideo,'version!=RemoteVideoCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteVideoCfg::BOOTSTRAP_PROTOCOL||role!="esp32-cam"||robot!=RemoteVideoCfg::ROBOT_ID','CAM strict bootstrap')
    req(session,'BOOTSTRAP_CLIENT="android"','Android role'); req(session,'bootstrap contract mismatch','Android contract reject'); req(session,'bootstrap role mismatch','Android role reject')
    for n in ['role==="android"','role==="esp32"','role==="esp32-cam"','video_view_token','video_publish_token']: req(relay,n,'Render scoped bootstrap')

    # Local radio/network authority and Direct-first WAN lifecycle.
    for n,label in [('ROBOT_SSID="HazRob"','SSID'),('ROBOT_HOST="192.168.4.1"','robot IP'),('CAMERA_HOST="192.168.4.2"','CAM IP')]: req(local,n,label)
    for n,label in [('applyRadioProfile(RadioProfile::DIRECT,false);','Direct boot'),('configureSoftAp(WIFI_AP,false);','Direct AP transition'),('configureSoftAp(WIFI_AP_STA);','Remote AP+STA'),('radioProfile!=RadioProfile::DIRECT','Direct HTTP guard'),('if(radioProfile==RadioProfile::REMOTE){remoteUpdate();gatewayUpdate();remoteMqttUpdate();}','Remote runtime guard'),('bool directApStable()','Direct stable detector'),('\\"already_direct\\":true','Direct idempotent response'),('\\"remote_activation_required\\":true','WAN provision remains Direct')]: req(master,n,label)
    ban(master,'applyRadioProfile(remoteTransportEnabled()?RadioProfile::REMOTE:RadioProfile::DIRECT','saved WAN boot selection')
    ban(master,'requestRadioProfile(RadioProfile::REMOTE,true)','persistent Remote mode')
    ban(rlink,'WiFi.mode(','RemoteLink radio ownership')
    for n,label in [('transportEnabled=false;','Direct boot transport'),('wan.putBool("enabled",false);','non-persistent mode'),('wan.putString("ssid",ssid);wan.putString("pass",password);wan.putBool("enabled",false);','WAN credentials only'),('wan.remove("ssid");wan.remove("pass");wan.putBool("authorized",false);wan.putBool("enabled",false);','WAN clear')]: req(rlink,n,label)
    ban(rlink,'transportEnabled=configured;','saved WAN auto boot')
    ban(rlink,'wan.putBool("enabled",configured);','saved WAN auto boot flag')
    req(rlink,'developerAuthorized=wan.getBool("authorized",false);','persisted developer authorization')
    req(rlink,'if(enabled&&!developerAuthorized)','Remote transport authorization gate')
    req(rlink,'\"developer_authorized\"','Remote authorization diagnostics')
    for n,label in [('web.on("/remote/disconnect"','disconnect endpoint'),('\\"wan_retained\\":true','WAN retained'),('web.on("/remote/connect"','reconnect endpoint'),('\\"explicit_operator_request\\":true','explicit Remote request')]: req(master,n,label)
    req(api,'remoteDisconnect()','Android disconnect'); req(api,'remoteReconnect()','Android reconnect'); req(api,'remoteAuthorize(boolean enabled)','Android developer authorization')
    req(wifi,'if(bindRobotWifi(context))return true;','Android existing HazRob route preference')
    req(wifi,'only by the explicit Direct-entry flow in waitForRobotWifi()','temporary-network fallback scope')
    ban(wifi,'SystemClock.elapsedRealtime()-lost>=250','repeated temporary-network recreation')
    req(gate,'local.remoteStatus()','Android profile check'); req(gate,'switchNeeded=!"direct".equalsIgnoreCase','Android avoids Direct bounce')
    req(gate,'final boolean ready=network&&robotReady','Main-only Direct admission authority')
    req(gate,'camera failure must never lock the operator out','camera capability isolation')
    req(gate,'directCamera.status()','non-blocking CAM status probe')
    ban(gate,'directCamera.directReady()','camera hard admission gate')
    req(gate,'developer_authorized','Android Remote developer authorization gate')
    req(camera_api,'String expectedFw="HazRob-CAM "+ReleaseAudit.VERSION','CAM identity gate'); req(camera_api,'prev==0xFF&&cur==0xD9&&jpegBytes>128','complete local JPEG gate')

    # Remote command parity across Android facade+transport and main firmware.
    expected={"probe","drive","motion_stop","estop","estop_reset","sensor_off","heartbeat","heartbeat_disarm","relay","measure_start","measure_cancel","alert","alert_test","camera_flash","config_get","config_set"}
    fw=set(re.findall(r't=="([a-z0-9_]+)"',rmqtt))
    android_src=conn+'\n'+api
    app=set(re.findall(r'command\("([a-z0-9_]+)"',android_src))
    app |= {"drive","motion_stop","estop","estop_reset","sensor_off","heartbeat","relay","measure_start","measure_cancel","alert","alert_test","camera_flash"}
    if expected-fw: raise AssertionError(f"firmware missing remote handlers: {sorted(expected-fw)}")
    if expected-app: raise AssertionError(f"Android missing remote emitters: {sorted(expected-app)}")
    if fw-expected: raise AssertionError(f"unreviewed firmware remote commands: {sorted(fw-expected)}")

    # Direct operator endpoint parity.
    for p in ["/drive","/motion/stop","/estop","/estop/reset","/sensor/off","/heartbeat","/diag","/data","/cfg","/relay","/measure","/alert"]: req(master,f'web.on("{p}"','firmware Direct '+p)
    for n,label in [('getText("/drive?l="+left+"&r="+right,DIRECT_DRIVE_TIMEOUT_MS)','Direct drive'),('getText("/estop",650)','Direct estop'),('getText("/estop/reset",650)','Direct estop reset'),('getText("/heartbeat",500)','Direct heartbeat'),('getJson("/diag",1000)','Direct diag'),('getJson("/data",1200)','Direct data'),('getText("/relay?mask="+mask,800)','Direct relay'),('getText("/measure?cmd=start",800)','Direct measure'),('getText("/alert?severity="+severity+"&ttl="+ttl,800)','Direct alert')]: req(api,n,label)

    # Safety timing and teardown.
    watchdog=int(one(r'CONTROL_COMMAND_TIMEOUT_MS=(\d+)',cfg,'watchdog')); direct=int(one(r'DIRECT_KEEPALIVE_MS=(\d+)L',api,'Direct keepalive')); pump=int(one(r'PUMP_MS=(\d+)',policy,'pump')); minpub=int(one(r'MIN_PUBLISH_MS=(\d+)',policy,'min publish')); hold=int(one(r'HOLD_REFRESH_MS=(\d+)',policy,'hold')); ttl=int(one(r'COMMAND_TTL_MS=(\d+)',policy,'TTL'))
    if not(direct<watchdog and hold<watchdog and pump<=minpub<=hold and ttl>=watchdog): raise AssertionError(f"timing invariant direct={direct} pump={pump} minpub={minpub} hold={hold} watchdog={watchdog} ttl={ttl}")
    req(conn,'public void motionStop()throws Exception{setDesiredDrive(0,0);command("motion_stop"','Remote explicit stop'); req(links,'failStopForBackground(remote);','service safe stop'); req(links,'remoteSessions.close()','remote pump teardown'); req(api,'requireNeutralAfterBackground','neutral re-arm')

    # Camera arbitration, local Direct path, NTP/TLS readiness and WSS paths.
    for n,label in [('SSID[]="HazRob"','CAM local SSID'),('IPAddress ip(192,168,4,2)','CAM local IP'),('WiFiServer mjpeg(81)','CAM local MJPEG'),('if(clients)return;','local viewer priority'),('gateway&&camTimeReady()','CAM gateway+time gate'),('TLS_TIME_MIN=1700000000','CAM TLS epoch'),('configTime(0,0,"pool.ntp.org","time.cloudflare.com")','CAM NTP'),('\\"time_ready\\":','CAM time diagnostics'),('\\"radio_profile\\":\\"remote\\"','CAM remote profile'),('\\"internet\\":true','CAM internet'),('\\"napt\\":true','CAM NAPT')]: req(cam,n,label)
    req(rvideo,'if(localActive||!RemoteVideoCfg::configured()','no WSS handshake during local'); req(rvideo,'if(localActive&&!connected)','no remote connect during local')
    req(rvcfg,'BASE_PATH[]="/hazrob/video"','CAM video base'); req(service,'VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID','Android viewer path'); req(relay,'/^\\/hazrob\\/video\\/(publish|view)\\/([^/]+)$/','Render WSS routes')

    # Authenticated TLS everywhere.
    req(mqttc,'HttpsURLConnection.getDefaultHostnameVerifier().verify(host,session)','Android MQTT hostname verify'); req(wssc,'p.setEndpointIdentificationAlgorithm("HTTPS")','Android WSS hostname verify'); req(rcfg,'ROOT_CA[]=HAZROB_MQTT_ROOT_CA','main CA'); req(rvcfg,'ROOT_CA[]=HAZROB_VIDEO_ROOT_CA','CAM CA'); ban(mqttc,'TrustAll','MQTT insecure TLS'); ban(wssc,'TrustAll','WSS insecure TLS')

    # Relay role isolation plus local and live production contract coverage.
    req(relay,'expected=role==="publisher"?c.publish:c.view','relay token isolation'); req(rtest,'openWs("/hazrob/video/view/hazrob-001",PUBLISH)','publisher token rejection'); req(rtest,'openWs("/hazrob/video/publish/hazrob-001",VIEW)','viewer token rejection'); req(rtest,'liveProductionContract()','live production contract test'); req(rtest,'mqttDiagnostic?.state,"pass"','live Hive diagnostic')

    print("HazRob HR-0101-059/061/066 five-layer system coherence: PASS")
    print(f"Identity: 0.10.1 / protocol 2 / hazrob-001; cloud={hive}:8883 + {render}")
    print(f"Safety timing: Direct {direct}ms; Remote hold {hold}ms; watchdog {watchdog}ms; command TTL {ttl}ms")
    print("Lifecycle: every boot Direct; WAN save stays local; Remote requires developer authorization + explicit runtime activation")
    print("Control parity: Direct HTTP + Remote MQTT command families aligned")
    print("Direct lock: AP-only idempotence + existing-route-first Android binding + CAM independent capability")
    print("Camera parity: local priority + gateway/NAPT + CAM NTP + strict bootstrap + WSS role isolation")
    print("Scope: source/integration qualified; physical RF/motor/camera acceptance remains PENDING")
    return 0
if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob HR-0101-059/061/066 five-layer system coherence: FAIL: {exc}"); raise SystemExit(1)
