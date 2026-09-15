#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def require(haystack: str, needle: str, label: str) -> None:
    if needle not in haystack:
        raise AssertionError(f"HR051 missing {label}: {needle}")

def main() -> int:
    cam = text("firmware/HazRob-CAM/HazRob-CAM.ino")
    remote = text("app/src/main/java/com/servoc/app/camera/RemoteJpegWebSocketClient.java")
    settings = text("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
    control = text("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    manifest = text("app/src/main/AndroidManifest.xml")
    config = text("firmware/HazRob/Config.h")
    change = text("docs/0.10.1/HR051_CAMERA_RECOVERY_MANIFEST.md")

    for needle, label in [
        ('PWDN=32,RST=-1,XCLK=0,SIOD=26,SIOC=27', 'AI-Thinker control pins'),
        ('Y9=35,Y8=34,Y7=39,Y6=36,Y5=21,Y4=19,Y3=18,Y2=5', 'AI-Thinker data pins'),
        ('VS=25,HR=23,PC=22', 'AI-Thinker sync pins'),
        ('IPAddress ip(192,168,4,2)', 'camera static IP'),
        ('gw(192,168,4,1)', 'camera gateway'),
        ('WiFiServer http(80),mjpeg(81)', 'camera local ports'),
    ]:
        require(cam, needle, label)

    for needle, label in [
        ('Serial.begin(115200)', 'serial boot diagnostics'),
        ('psramFound()', 'PSRAM detection'),
        ('CAMERA_FB_IN_DRAM', 'DRAM framebuffer fallback'),
        ('JPG_CAP_DRAM', 'DRAM JPEG buffer'),
        ('fb_memory', 'framebuffer memory status'),
        ('wifi_connected', 'Wi-Fi status'),
        ('camera_error', 'camera error status'),
        ('sensor', 'sensor identity status'),
        ('http://192.168.4.2:81/stream', 'serial stream endpoint'),
    ]:
        require(cam, needle, label)

    require(control, '"http://"+h+":81/stream"', 'Android canonical MJPEG URL')
    require(control, '"http://"+h+"/stream"', 'Android port-80 redirect fallback')
    require(manifest, 'android:usesCleartextTraffic="true"', 'local HTTP cleartext allowance')

    for needle, label in [
        ('CONNECT_TIMEOUT_MS=4000', 'remote-video TCP timeout'),
        ('plain.connect(new InetSocketAddress(host,port),CONNECT_TIMEOUT_MS)', 'bounded TCP connect'),
        ('createSocket(plain,host,port,true)', 'TLS layered over bounded TCP socket'),
        ('setEndpointIdentificationAlgorithm("HTTPS")', 'TLS hostname verification'),
    ]:
        require(remote, needle, label)

    for needle, label in [
        ('"camera_error"', 'app camera error detail'),
        ('"fb_memory"', 'app memory detail'),
        ('"sensor"', 'app sensor detail'),
        ('"clients"', 'master AP client diagnostic'),
        ('GPIO0-GND', 'boot-mode operator hint'),
        ('cam="192.168.4.2"', 'safe camera host fallback'),
    ]:
        require(settings, needle, label)

    for needle, label in [
        ('DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32', 'left drive authority'),
        ('DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27', 'right drive authority'),
        ('ENV_SDA=13,ENV_SCL=14', 'ENV bus authority'),
        ('SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49', 'ADS bus authority'),
        ('R0_SESSION[]="FIELD-R0-3SESSION-20260913"', 'R0 authority'),
    ]:
        require(config, needle, label)

    require(change, 'HR-0101-051', 'controlled change manifest')
    print('HR-0101-051 camera recovery contract: PASS')
    return 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HR-0101-051 camera recovery contract: FAIL: {exc}")
        raise SystemExit(1)
