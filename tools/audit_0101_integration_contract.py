#!/usr/bin/env python3
"""Fail-closed HazRob 0.10.1 HR-0101-061 connection integration contract.

Authoritative lifecycle:
BOOT/DIRECT = AP-only + HTTP drive/telemetry + local MJPEG. Every reboot starts here.
REMOTE = AP+STA + MQTT control + Render/WSS video through NAPT, entered only by explicit operator action.
Saved WAN credentials are configuration data only and must never auto-arm Remote on boot.
WAN provisioning must remain local/Direct and must not disconnect HazRob or ESP32-CAM.
Direct readiness requires the main ESP32 plus ESP32-CAM identity/status and a real local MJPEG frame.
REMOTE DISCONNECT returns to Direct while retaining WAN credentials.
REMOTE RECONNECT reuses saved WAN for the current runtime only.
WAN CLEAR is the only destructive credential operation.
Current GPIO/ENV, steering, safety and cloud contracts must not regress.
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

def between(src: str, start: str, end: str, label: str) -> str:
    if start not in src or end not in src:
        raise AssertionError(f"{label}: missing boundary")
    return src.split(start,1)[1].split(end,1)[0]

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
    camera_api = text("app/src/main/java/com/servoc/app/camera/CameraApi.java")
    setup_ui = text("app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java")
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
    require(master, 'applyRadioProfile(RadioProfile::DIRECT,false);', "every boot must enter Direct")
    forbid(master, 'applyRadioProfile(remoteTransportEnabled()?RadioProfile::REMOTE:RadioProfile::DIRECT', "saved WAN must not select boot mode")
    require(master, 'configureSoftAp(WIFI_AP,false);', "Direct AP-only transition without forced AP restart")
    require(master, 'configureSoftAp(WIFI_AP_STA);', "Remote AP+STA radio")
    require(master, 'bool directApStable()', "Direct stable-state detector")
    require(master, 'if(directApStable())', "Direct idempotence gate")
    require(master, '\\"already_direct\\":true', "Direct no-op response")
    require(master, 'remoteSetTransportEnabled(false,persistMode)', "Direct disables WAN transport")
    require(master, 'remoteSetTransportEnabled(false,false)', "stable Direct explicitly keeps WAN transport off")
    require(master, 'remoteMqttDisconnect();', "Direct disconnects MQTT")
    require(master, 'WiFi.AP.enableNAPT(false)', "Direct disables NAPT")
    require(master, 'web.on("/drive"', "Direct HTTP drive endpoint")
    require(master, 'radioProfile!=RadioProfile::DIRECT', "local drive authority guard")
    require(master, 'web.on("/connection/direct"', "Direct mode endpoint")
    require(master, 'requestRadioProfile(RadioProfile::DIRECT,false)', "Direct runtime authority")
    require(master, 'requestRadioProfile(RadioProfile::REMOTE,false)', "Remote must be explicit runtime activation")
    forbid(master, 'requestRadioProfile(RadioProfile::REMOTE,true)', "Remote mode must never persist across reboot")
    require(master, '\\"boot_default\\":\\"direct\\"', "diagnostic Direct boot authority")
    require(master, 'drive_udp', "Direct capability field")
    require(master, 'installed', "UDP explicitly absent")
    forbid(master, '#include <WiFiUdp.h>', "Direct must not compile UDP transport")
    forbid(master, 'driveUdp.begin(', "Direct must not start UDP drive")

    require(master, 'web.on("/remote/disconnect"', "runtime remote disconnect endpoint")
    require(master, 'web.on("/remote/connect"', "saved-WAN reconnect endpoint")
    disconnect_block = between(master, 'web.on("/remote/disconnect"', 'web.on("/remote/connect"', "remote disconnect block")
    require(disconnect_block, 'motionStop();', "disconnect immediate motion stop")
    require(disconnect_block, '\\"wan_retained\\":true', "disconnect declares retained WAN")
    require(disconnect_block, 'requestRadioProfile(RadioProfile::DIRECT,false)', "disconnect Direct switch")
    forbid(disconnect_block, 'remoteClear()', "disconnect must never clear WAN")
    connect_block = between(master, 'web.on("/remote/connect"', 'web.on("/remote/status"', "remote reconnect block")
    require(connect_block, 'remoteConfigured()', "reconnect requires saved WAN")
    require(connect_block, 'motionStop();', "reconnect transition motion stop")
    require(connect_block, 'requestRadioProfile(RadioProfile::REMOTE,false)', "reconnect reuses saved WAN for runtime only")
    require(connect_block, '\\"explicit_operator_request\\":true', "reconnect must be explicitly operator initiated")

    provision_fw = between(master, 'web.on("/remote/provision"', 'web.on("/remote/retry"', "firmware WAN provision block")
    require(provision_fw, 'remoteProvision(s,p)', "firmware stores WAN credentials")
    require(provision_fw, '\\"radio_profile\\":\\"direct\\"', "WAN save must remain Direct")
    require(provision_fw, '\\"remote_activation_required\\":true', "WAN save requires later explicit Remote")
    forbid(provision_fw, 'requestRadioProfile(RadioProfile::REMOTE', "WAN save must not activate Remote")

    require(remote_h, 'remoteSetTransportEnabled', "remote transport switch API")
    require(remote, 'transportEnabled=false;', "boot must keep WAN transport disabled")
    require(remote, 'wan.putBool("enabled",false);', "legacy mode flag normalized Direct")
    forbid(remote, 'transportEnabled=configured;', "saved WAN must not auto-arm Remote")
    forbid(remote, 'wan.putBool("enabled",configured);', "saved WAN must not become boot mode")
    forbid(remote, 'if(persistMode&&enabled)wan.putBool("enabled",true);', "runtime Remote must not persist")
    require(remote, 'wan.putString("ssid",ssid);wan.putString("pass",password);wan.putBool("enabled",false);', "provision persists credentials but not Remote mode")
    require(remote, 'wan.remove("ssid");wan.remove("pass");wan.putBool("enabled",false);', "clear WAN is explicit credential destruction")
    require(remote, '(void)persistMode', "mode persistence deliberately disabled")
    require(remote, 'if(!transportEnabled){dnsOk=timeOk=false;return;}', "remote loop disabled in Direct")
    require(remote, 'remoteInternetReady(){return transportEnabled&&configured', "remote readiness gate")
    require(remote, '\\"boot_default\\":\\"direct\\"', "remote status Direct boot authority")
    forbid(remote, 'WiFi.mode(', "RemoteLink must not change global radio mode")
    require(master, 'if(radioProfile==RadioProfile::REMOTE){remoteUpdate();gatewayUpdate();remoteMqttUpdate();}', "remote runtime profile gate")
    require(master, 'WiFi.AP.enableNAPT(ready)', "remote camera Internet routing")
    require(mqtt, 't=="drive"', "Remote MQTT drive")
    require(mqtt, 't=="estop"', "Remote MQTT estop")

    require(robot_api, 'HTTP for Direct control/telemetry, MQTT for Remote', "Android transport authority")
    require(robot_api, 'DIRECT_KEEPALIVE_MS=250L', "Direct HTTP keepalive cadence")
    require(robot_api, 'getText("/drive?l="+left+"&r="+right,450)', "Direct HTTP drive")
    require(robot_api, 'requireNeutralAfterBackground', "background re-arm interlock")
    require(robot_api, 'postForm("/remote/disconnect","",1500)', "Android disconnect API")
    require(robot_api, 'postForm("/remote/connect","",1800)', "Android reconnect API")
    require(robot_api, 'postForm("/remote/clear","confirm=1",1500)', "Android destructive WAN clear API")
    forbid(robot_api, 'DatagramSocket', "Android Direct UDP removed")
    forbid(robot_api, 'sendDriveDatagram', "Android Direct UDP removed")

    require(setup_ui, '"فصل Remote مع الاحتفاظ بـ WAN"', "disconnect UI")
    require(setup_ui, '"إعادة الاتصال بـ WAN المحفوظ"', "reconnect UI")
    require(setup_ui, '"مسح WAN نهائيًا"', "destructive WAN reset UI")
    require(setup_ui, 'host.robotApi().remoteDisconnect()', "disconnect UI action")
    require(setup_ui, 'host.robotApi().remoteReconnect()', "reconnect UI action")
    require(setup_ui, 'host.robotApi().remoteClear()', "WAN clear UI action")
    require(setup_ui, 'new AlertDialog.Builder', "WAN clear confirmation")

    require(manifest, 'android.permission.NEARBY_WIFI_DEVICES', "Android 13+ Wi-Fi permission")
    require(manifest, 'android.permission.ACCESS_FINE_LOCATION', "Android 10-12 Wi-Fi permission")
    require(wifi_route, 'WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID)', "HazRob local-only request")
    require(wifi_route, '.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)', "local-only network semantics")
    require(wifi_route, 'cm.requestNetwork(req,requestCallback)', "persistent Direct network request")
    require(wifi_route, 'unregisterNetworkCallback', "Direct network release")
    require(wifi_route, 'restartRobotWifiRequest', "Direct request explicit re-acquisition")
    require(wifi_route, 'requestLostAt', "Direct lost-network recovery state")
    require(gate, 'WifiRoute.waitForRobotWifi(a,30000)', "Direct acquisition gate")
    require(gate, 'local.remoteStatus()', "Direct checks current radio profile before switching")
    require(gate, 'switchNeeded=!"direct".equalsIgnoreCase', "Direct avoids needless Direct->Direct radio bounce")
    require(gate, 'local.postForm("/connection/direct"', "Direct radio switch")
    require(gate, 'WifiRoute.restartRobotWifiRequest(a)', "Android re-acquires HazRob after real profile switch")
    require(gate, 'WifiRoute.waitForRobotWifi(a,15000)', "Direct re-acquisition window")
    require(gate, 'SystemClock.elapsedRealtime()+12000L', "Direct main verification window")
    require(gate, 'CameraApi directCamera=new CameraApi(LocalNetworkContract.CAMERA_HOST)', "Direct camera gate")
    require(gate, 'SystemClock.elapsedRealtime()+20000L', "Direct camera readiness window")
    require(gate, 'directCamera.directReady()', "Direct requires CAM status plus a real JPEG")
    require(gate, 'local.postForm("/connection/remote"', "Remote explicit radio switch")
    require(gate, 'WifiRoute.releaseRobotWifi(a);', "Remote releases phone from HazRob local-only route")

    provision_app = between(gate, 'private void provision(', 'private void pollRobotRemote()', "Android WAN provision block")
    require(provision_app, 'local.remoteProvision(ssid,password)', "Android saves WAN locally")
    require(provision_app, 'putString("connection_mode","direct")', "WAN save keeps app in Direct")
    require(provision_app, 'WifiRoute.maintainRobotWifi(a)', "WAN save retains HazRob route")
    forbid(provision_app, 'WifiRoute.releaseRobotWifi(a)', "WAN save must not release HazRob")
    forbid(provision_app, 'pollRobotRemote()', "WAN save must not auto-enter Remote")

    require(camera_api, 'boolean directReady()', "camera Direct readiness API")
    require(camera_api, 'HazRob-CAM 0.10.1', "camera Direct firmware identity")
    require(camera_api, 'LocalNetworkContract.CAMERA_HOST', "camera Direct fixed IP")
    require(camera_api, 'probeMjpegFrame()', "camera Direct first-frame requirement")
    require(camera_api, '0xFF&&cur==0xD8', "camera JPEG SOI validation")
    require(camera_api, 'prev==0xFF&&cur==0xD9', "camera JPEG EOI validation")

    require(link_service, 'WifiRoute.maintainRobotWifi(this)', "Direct network maintenance")
    require(link_service, 'RobotApi.setOperatorForeground(foreground)', "background motion interlock")
    require(link_service, 'failStopForBackground', "background fail-stop")
    require(link_service, 'remoteSessions.close()', "remote drive pump teardown")

    require(cam, 'SSID[]="HazRob"', "camera joins main HazRob AP")
    require(cam, 'IPAddress ip(192,168,4,2)', "camera local IP")
    require(cam, 'gw(192,168,4,1)', "camera gateway")
    require(cam, 'WebServer web(80)', "camera status server")
    require(cam, 'WiFiServer mjpeg(81)', "local MJPEG server")
    require(cam, 'WiFi.mode(WIFI_STA)', "camera is STA on HazRob")
    require(cam, 'WiFi.begin(SSID)', "camera automatic HazRob association")
    require(cam, 'millis()-wifiAt>4500', "camera local reconnect loop")
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
    require(cam, 'gateway&&camTimeReady()', "camera requires its own valid TLS clock")
    require(cam, 'configTime(0,0,"pool.ntp.org","time.cloudflare.com")', "camera NTP synchronization")
    require(cam, 'if(clients)return;', "camera does not poll gateway during local stream")
    require(cam_remote, 'if(localActive||!RemoteVideoCfg::configured()', "remote handshake blocked during local view")
    require(cam_remote, 'if(localActive&&!connected)', "remote connection attempt blocked during local view")
    require(cam_remote, 'version!=RemoteVideoCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteVideoCfg::BOOTSTRAP_PROTOCOL||role!="esp32-cam"||robot!=RemoteVideoCfg::ROBOT_ID', "strict CAM bootstrap compatibility")

    from audit_hr059_system_coherence import main as coherence_main
    rc=coherence_main()
    if rc not in (None,0):
        raise AssertionError(f"HR059 five-layer coherence returned {rc}")

    state = {
        "BOOT": {"radio":"AP", "profile":"DIRECT", "uses_saved_wan":False},
        "DIRECT": {"radio":"AP", "http_drive":True, "mqtt":False, "napt":False, "local_camera":True, "next_boot":"DIRECT", "idempotent":True},
        "WAN_PROVISION": {"radio":"AP", "credentials_saved":True, "remote_activated":False, "next_boot":"DIRECT"},
        "REMOTE": {"radio":"AP_STA", "http_drive":False, "mqtt":True, "napt":"internet_ready", "wss":"gateway_and_cam_time_ready", "activation":"explicit", "next_boot":"DIRECT"},
        "REMOTE_DISCONNECT": {"radio":"AP", "wan_retained":True, "next_boot":"DIRECT"},
        "REMOTE_RECONNECT": {"radio":"AP_STA", "uses_saved_wan":True, "next_boot":"DIRECT"},
        "WAN_CLEAR": {"wan_retained":False, "next_boot":"DIRECT"},
    }
    assert state["BOOT"]["profile"] == "DIRECT" and not state["BOOT"]["uses_saved_wan"]
    assert state["DIRECT"]["radio"] == "AP" and state["DIRECT"]["http_drive"] and not state["DIRECT"]["mqtt"] and state["DIRECT"]["local_camera"] and state["DIRECT"]["idempotent"]
    assert state["WAN_PROVISION"]["credentials_saved"] and not state["WAN_PROVISION"]["remote_activated"]
    assert state["REMOTE"]["radio"] == "AP_STA" and not state["REMOTE"]["http_drive"] and state["REMOTE"]["mqtt"] and state["REMOTE"]["activation"] == "explicit"
    assert state["REMOTE_DISCONNECT"]["wan_retained"] and state["REMOTE_DISCONNECT"]["next_boot"] == "DIRECT"
    assert state["REMOTE_RECONNECT"]["uses_saved_wan"] and state["REMOTE_RECONNECT"]["next_boot"] == "DIRECT" and state["WAN_CLEAR"]["next_boot"] == "DIRECT"

    print("HazRob 0.10.1 HR-0101-061 Direct-first connection contract: PASS")
    print("BOOT/DIRECT: AP-only + HTTP + verified local CAM/MJPEG; saved WAN never changes boot mode")
    print("WAN PROVISION: credentials saved locally while HazRob and camera remain Direct")
    print("REMOTE: explicit operator action only; AP+STA/MQTT/NAPT/Render enabled for current runtime")
    print("RETURN DIRECT: MQTT/NAPT/STA off, WAN retained; every reboot returns Direct")
    print("WAN CLEAR: only destructive credential action; SAFETY: motion stop before transitions/service teardown")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
