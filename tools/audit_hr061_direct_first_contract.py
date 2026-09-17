#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]

def read(rel):
    p=ROOT/rel
    if not p.exists():
        raise AssertionError(f'missing controlled file: {rel}')
    return p.read_text(errors='ignore')

def require(text, token, label):
    if token not in text:
        raise AssertionError(f'{label}: required token missing: {token}')

def forbid(text, token, label):
    if token in text:
        raise AssertionError(f'{label}: forbidden token present: {token}')

def main():
    master=read('firmware/HazRob/HazRob.ino')
    cfg=read('firmware/HazRob/Config.h')
    cam=read('firmware/HazRob-CAM/HazRob-CAM.ino')
    local=read('app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java')
    route=read('app/src/main/java/com/servoc/app/communication/WifiRoute.java')
    service=read('app/src/main/java/com/servoc/app/communication/RobotLinkService.java')
    gate=read('app/src/main/java/com/servoc/app/auth/ConnectionGate.java')
    camera_api=read('app/src/main/java/com/servoc/app/camera/CameraApi.java')

    require(cfg, '"HazRob"', 'master SSID')
    require(master, 'WIFI_AP', 'master Direct radio mode')
    require(master, 'configureSoftAp', 'master SoftAP authority')
    require(master, 'if(directApStable())', 'repeat-Direct no-bounce guard')
    require(master, 'configureSoftAp(WIFI_AP,false);', 'AP-preserving Direct recovery')
    require(master, 'if(millis()-wifiAt<5000)return;', 'bounded firmware Wi-Fi health check')

    require(cam, 'WIFI_STA', 'camera STA mode')
    require(cam, '192,168,4,2', 'camera canonical IP')
    require(cam, '192,168,4,1', 'camera canonical gateway')
    forbid(cam, 'WiFi.softAP(', 'camera production SoftAP')
    forbid(cam, 'WIFI_AP_STA', 'camera production AP+STA')
    forbid(cam, 'WIFI_AP)', 'camera production AP mode')

    require(local, 'ROBOT_SSID="HazRob"', 'Android SSID contract')
    require(local, 'ROBOT_HOST="192.168.4.1"', 'Android robot host')
    require(local, 'CAMERA_HOST="192.168.4.2"', 'Android camera host')
    require(route, 'setSsid(LocalNetworkContract.ROBOT_SSID)', 'Android Direct Wi-Fi request')
    require(route, 'bindProcessToNetwork', 'Android local route pinning')
    require(route, 'WifiManager.WIFI_MODE_FULL_LOW_LATENCY', 'Android Direct Wi-Fi lock')
    require(route, 'wifiLock.acquire()', 'Android Wi-Fi lock acquisition')
    require(route, 'onLost(Network network)', 'Android loss callback')
    require(route, 'SystemClock.elapsedRealtime()-lost>=250', 'Android fast reconnect')
    require(route, 'requestCallback==null', 'long-lived exact-SSID request guard')
    require(route, 'LocalNetworkContract.isRobotSubnetAddress', 'Android robot-subnet validation')
    require(service, 'worker.scheduleAtFixedRate(this::linkTick,0,250,TimeUnit.MILLISECONDS)', 'foreground link keeper cadence')
    require(service, 'WifiRoute.maintainRobotWifi(this)', 'foreground Direct route maintenance')

    # Direct admission authority is Main ESP32 only. Camera is diagnostic/capability state and must not lock out the app.
    require(gate, 'final boolean ready=network&&robotReady', 'Main-only Direct admission')
    forbid(gate, 'final boolean ready=accepted', 'camera-gated Direct admission')
    forbid(gate, 'cameraDeadline=SystemClock.elapsedRealtime()+20000L', 'blocking camera admission wait')
    require(gate, 'camera_preflight_ready', 'independent camera preflight state')
    require(gate, 'ControlTab owns it', 'non-competing MJPEG ownership')
    require(gate, 'حالة ESP32-CAM مستقلة ولا تمنع دخول التطبيق', 'operator recovery wording')
    require(camera_api, 'camera /status unreachable at ', 'camera transport diagnostic')
    require(camera_api, 'camera initialization failed • error=', 'camera initialization diagnostic')

    print('HAZROB DIRECT-FIRST CONTRACT: PASS')
    print('  boot/default: DIRECT')
    print('  visible SSID authority: master ESP32 / HazRob')
    print('  robot: 192.168.4.1')
    print('  camera: STA-only / 192.168.4.2; capability state, not admission gate')
    print('  firmware: healthy Direct AP is not bounced by repeated Direct selection')
    print('  Android: persistent exact-SSID request + Wi-Fi lock + 250 ms route maintenance/recovery')
    return 0

if __name__=='__main__':
    try:
        raise SystemExit(main())
    except AssertionError as e:
        print(f'HAZROB DIRECT-FIRST CONTRACT: FAIL: {e}', file=sys.stderr)
        raise SystemExit(1)
