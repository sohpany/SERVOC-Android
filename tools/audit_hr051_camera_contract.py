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
    cfg=text("firmware/HazRob/Config.h")
    hw=text("firmware/HazRob/Hardware.cpp")
    proto=text("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
    settings=text("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    control=text("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    change=text("docs/0.10.1/HR053_SYSTEM_COHERENCE_MANIFEST.md")

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
        ('web.on("/stream"','stream redirect')]: req(cam,n,l)
    ban(cam,'PIXFORMAT_JPEG','native JPEG path')
    ban(cam,'CAMERA_FB_IN_DRAM','DRAM fallback')
    req(control,'"http://"+h+":81/stream"','Android stream URL')

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
    print('HR-0101-053 camera/ENV coherence contract: PASS')
    return 0

if __name__ == "__main__":
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f"HR-0101-053 camera/ENV coherence contract: FAIL: {exc}")
        raise SystemExit(1)
