#!/usr/bin/env python3
from pathlib import Path
import re, subprocess, tempfile, hashlib, sys

ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/"firmware"/"HazRob"
CAM=ROOT/"firmware"/"HazRob-CAM"

def read(path): return path.read_text(encoding="utf-8")

def require(cond,msg):
    if not cond:
        raise SystemExit("FAIL: "+msg)

main_cfg=read(MAIN/"Config.h")
main_ino=read(MAIN/"HazRob.ino")
cam_ino=read(CAM/"HazRob-CAM.ino")
cam_cfg=read(CAM/"RemoteVideoConfig.h")
cam_rv=read(CAM/"RemoteVideo.cpp")
main_cloud=read(MAIN/"RemoteCloudConfig.h")

require('constexpr char SSID[]="HazRob"' in main_cfg,"main SSID is not HazRob")
require('constexpr char FW[]="HazRob 0.10.3"' in main_cfg,"main firmware runtime identity mismatch")
require('constexpr char HW_PROFILE[]="hazrob-0.10.3-hr070-' in main_cfg,"HR070 hardware profile identity missing")
require('192,168,4,1' in main_ino,"main AP IP 192.168.4.1 missing")
require('constexpr char SSID[]="HazRob",FW[]="HazRob-CAM 0.10.3"' in cam_ino,"camera SSID/identity mismatch")
require('IPAddress ip(192,168,4,2),gw(192,168,4,1)' in cam_ino,"camera static IP/gateway mismatch")
require('WiFi.softAPSSID()==Cfg::SSID' in main_ino,"main AP health check does not verify SSID")
require('WiFi.softAP(Cfg::SSID)' in main_ino,"main does not explicitly create HazRob SoftAP")
require('WiFi.mode(WIFI_STA)' in cam_ino and 'esp_wifi_set_mode(WIFI_MODE_STA)' in cam_ino,"camera STA-only enforcement missing")
require('WiFi.softAP(' not in cam_ino and 'WIFI_AP' not in cam_ino,"camera contains an AP creation path")
require('WiFi.setSleep(false)' in main_ino and 'WiFi.setSleep(false)' in cam_ino,"Wi-Fi sleep must be disabled")
require('WiFi.setAutoReconnect(false)' in cam_ino,"camera library auto-reconnect must be disabled")
require('xTaskCreatePinnedToCore(streamTask,"mj",6144,nullptr,2,nullptr,1)' in cam_ino,"local stream task priority contract missing")
require('for(;;)' in cam_rv and 'vTaskDelete(nullptr)' not in cam_rv,"remote video worker must remain persistent")
require('CONTROL_COMMAND_TIMEOUT_MS=500' in main_cfg,"drive command timeout mismatch")

# Main ESP32 Direct AP requests the ESP-IDF maximum Wi-Fi TX power after every radio-profile setup.
# The requested ceiling remains subject to the module PHY, antenna, supply quality and regulatory limits.
require('#include <esp_wifi.h>' in main_ino,"ESP-IDF Wi-Fi TX power API include missing")
require('HAZROB_WIFI_TX_POWER_QDBM=80' in main_ino,"Main ESP32 Wi-Fi TX power target is not 20 dBm")
require('esp_wifi_set_max_tx_power(HAZROB_WIFI_TX_POWER_QDBM)' in main_ino,"Main ESP32 Wi-Fi TX power is not re-applied after AP/profile setup")

pins={
"DRIVE_L_EN=17":"left PWM","DRIVE_L_IN1=19":"left IN1","DRIVE_L_IN2=32":"left IN2",
"DRIVE_R_EN=16":"right PWM","DRIVE_R_IN1=18":"right IN1","DRIVE_R_IN2=27":"right IN2",
"SDA=21,SCL=22":"ADS I2C","ENV_SDA=13,ENV_SCL=14":"ENV I2C",
"RELAY_MQ=25":"MQ relay","ALERT_PIN=26":"alert","SOUND_PIN=36":"sound",
"IR_PIN=35":"IR","LDR_PIN=34":"LDR","BATTERY_PIN=33":"battery",
"ULTRA_TRIG_PIN=23":"ultrasonic trigger","ULTRA_ECHO_PIN=39":"ultrasonic echo",
}
for token,label in pins.items(): require(token in main_cfg,f"{label} hardware contract mismatch")

# HR070 is an engineering/hardware-profile revision, not part of the runtime firmware banners.
require('HazRob 0.10.3 HR' not in main_cfg,"engineering revision leaked into Main runtime firmware banner")
require('HazRob-CAM 0.10.3 HR' not in cam_ino,"engineering revision leaked into CAM runtime firmware banner")

def cert_from(text,macro):
    m=re.search(r'#define '+re.escape(macro)+r' R"PEM\((-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----\n)\)PEM"',text,re.S)
    require(bool(m),f"{macro} PEM missing")
    return m.group(1)

main_ca=cert_from(main_cloud,"HAZROB_MQTT_ROOT_CA")
cam_ca=cert_from(cam_cfg,"HAZROB_VIDEO_ROOT_CA")
require(main_ca==cam_ca,"main and camera trust anchors differ")

with tempfile.NamedTemporaryFile("w",suffix=".pem",delete=False) as f:
    f.write(cam_ca); cert_path=f.name
r=subprocess.run(["openssl","x509","-in",cert_path,"-noout","-subject","-fingerprint","-sha256"],capture_output=True,text=True)
require(r.returncode==0,"camera root CA is not a valid X.509 certificate")

print("PASS: HazRob 0.10.3 / HR070 firmware baseline contract")
print(r.stdout.strip())
for directory in (MAIN,CAM):
    for f in sorted(directory.iterdir()):
        if f.is_file() and f.suffix in {".ino",".cpp",".h"}:
            print(hashlib.sha256(f.read_bytes()).hexdigest(), f.relative_to(ROOT))
