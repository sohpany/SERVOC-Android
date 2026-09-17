#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,l):
    if n not in s: raise AssertionError(f'{l}: missing {n!r}')
def forbid(s,n,l):
    if n in s: raise AssertionError(f'{l}: forbidden {n!r}')
def main():
    m=text('firmware/HazRob/HazRob.ino'); c=text('firmware/HazRob/Config.h'); cam=text('firmware/HazRob-CAM/HazRob-CAM.ino')
    api=text('app/src/main/java/com/servoc/app/camera/CameraApi.java'); local=text('app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java')
    req(c,'FW[]="HazRob 0.10.1 HR065"','Main identity'); req(c,'BUILD_ID[]="HR-0101-065-MAIN"','Main build ID')
    req(c,'SSID[]="HazRob"','Main AP authority'); req(c,'CAM[]="192.168.4.2"','CAM canonical IP')
    req(m,'enum class RadioProfile:uint8_t{DIRECT=0,REMOTE=1}','radio profiles'); req(m,'applyRadioProfile(RadioProfile::DIRECT,false);','Direct boot')
    req(m,'configureSoftAp(WIFI_AP,false);','Direct AP-only'); req(m,'configureSoftAp(WIFI_AP_STA);','explicit Remote AP+STA')
    req(m,'remoteMqttDisconnect();','Direct MQTT shutdown'); req(m,'WiFi.AP.enableNAPT(false)','Direct NAPT shutdown')
    req(cam,'FW[]="HazRob-CAM 0.10.1 HR065"','CAM identity'); req(cam,'BUILD_ID[]="HR-0101-065-CAM"','CAM build ID')
    req(cam,'WiFi.mode(WIFI_STA)','CAM Arduino STA-only'); req(cam,'esp_wifi_set_mode(WIFI_MODE_STA)','CAM ESP-IDF STA-only')
    req(cam,'WiFi.setAutoReconnect(false)','CAM controlled reconnect'); req(cam,'void radioGuard()','CAM radio guard')
    forbid(cam,'WiFi.softAP(','CAM must never create AP'); forbid(cam,'WIFI_AP','CAM must contain no AP/APSTA mode')
    req(local,'ROBOT_SSID="HazRob"','Android SSID'); req(local,'ROBOT_HOST="192.168.4.1"','Android Main IP'); req(local,'CAMERA_HOST="192.168.4.2"','Android CAM IP')
    req(api,'EXPECTED_FW="HazRob-CAM 0.10.1 HR065"','Android exact CAM firmware gate'); req(api,'EXPECTED_BUILD="HR-0101-065-CAM"','Android exact CAM build gate')
    req(api,'j.optInt("wifi_mode",-1)!=1','Android requires STA mode'); req(api,'localStreamReady()','Android requires real JPEG')
    for n in ['DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32','DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27','DRIVE_L_INVERT=false,DRIVE_R_INVERT=false']:
        req(c,n,'drive authority')
    print('PASS HR-0101-065: Main Direct authority + CAM fail-closed STA-only + exact runtime identity + drive mapping')
    return 0
if __name__=='__main__': raise SystemExit(main())
