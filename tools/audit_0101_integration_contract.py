#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def text(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,l):
    if n not in s: raise AssertionError(f'{l}: missing {n!r}')
def forbid(s,n,l):
    if n in s: raise AssertionError(f'{l}: forbidden {n!r}')

def main():
    master=text('firmware/HazRob/HazRob.ino')
    cfg=text('firmware/HazRob/Config.h')
    cam=text('firmware/HazRob-CAM/HazRob-CAM.ino')
    api=text('app/src/main/java/com/servoc/app/camera/CameraApi.java')
    local=text('app/src/main/java/com/servoc/app/communication/LocalNetworkContract.java')
    hw=text('app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java')

    # Canonical firmware identity and Direct network authority.
    req(cfg,'FW[]="HazRob 0.10.2"','Main identity')
    req(cfg,'SSID[]="HazRob"','Main AP authority')
    req(cfg,'CAM[]="192.168.4.2"','CAM canonical IP')
    forbid(cfg,'HR-','Main firmware revision marker')
    forbid(cfg,'HR0','Main firmware revision marker')

    req(master,'enum class RadioProfile:uint8_t{DIRECT=0,REMOTE=1}','radio profiles')
    req(master,'applyRadioProfile(RadioProfile::DIRECT,false);','Direct boot')
    req(master,'configureSoftAp(WIFI_AP,false);','Direct AP-only')
    req(master,'configureSoftAp(WIFI_AP_STA);','explicit Remote AP+STA')
    req(master,'remoteMqttDisconnect();','Direct MQTT shutdown')
    req(master,'WiFi.AP.enableNAPT(false)','Direct NAPT shutdown')
    req(master,'WiFi.softAPSSID()!=Cfg::SSID','Direct SSID health check')

    # Camera is STA-only and identifies as the canonical 0.10.1 camera firmware.
    req(cam,'FW[]="HazRob-CAM 0.10.2"','CAM identity')
    req(cam,'WiFi.mode(WIFI_STA)','CAM Arduino STA-only')
    req(cam,'esp_wifi_set_mode(WIFI_MODE_STA)','CAM ESP-IDF STA-only')
    req(cam,'WiFi.setAutoReconnect(false)','CAM controlled reconnect')
    forbid(cam,'WiFi.softAP(','CAM must never create AP')
    forbid(cam,'WIFI_AP_STA','CAM must contain no AP+STA mode')
    forbid(cam,'HR-','CAM firmware revision marker')
    forbid(cam,'HR0','CAM firmware revision marker')

    # Android Direct contract must match the firmware exactly.
    req(local,'ROBOT_SSID="HazRob"','Android SSID')
    req(local,'ROBOT_HOST="192.168.4.1"','Android Main IP')
    req(local,'CAMERA_HOST="192.168.4.2"','Android CAM IP')
    req(local,'CAMERA_FIRMWARE_BANNER="HazRob-CAM 0.10.2"','Android CAM identity')
    req(api,'LocalNetworkContract.CAMERA_FIRMWARE_BANNER.equals(fw)','Android exact CAM firmware gate')
    req(api,'j.optInt("wifi_mode",-1)!=1','Android requires STA mode')
    req(api,'localStreamReady()','Android requires real JPEG')

    # Drive/GPIO authority is identical in firmware and Android hardware profile.
    for n in [
        'DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32',
        'DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27',
        'DRIVE_L_INVERT=false,DRIVE_R_INVERT=false',
        'SDA=21,SCL=22',
        'ENV_SDA=13,ENV_SCL=14',
    ]:
        req(cfg,n,'firmware hardware authority')
    for n in [
        'DRIVE_L_EN=17','DRIVE_L_IN1=19','DRIVE_L_IN2=32',
        'DRIVE_R_EN=16','DRIVE_R_IN1=18','DRIVE_R_IN2=27',
        'I2C_SDA_GPIO=21','I2C_SCL_GPIO=22',
        'ENV_I2C_SDA_GPIO=13','ENV_I2C_SCL_GPIO=14',
    ]:
        req(hw,n,'Android hardware authority')

    print('HazRob 0.10.2 integration contract: PASS')
    print('Main=HazRob/192.168.4.1; CAM=STA-only/192.168.4.2; GPIO and firmware identities aligned')
    return 0

if __name__=='__main__':
    raise SystemExit(main())
