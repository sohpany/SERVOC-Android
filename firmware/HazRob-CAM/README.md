# HazRob ESP32-CAM

Standalone AI-Thinker-style ESP32-CAM firmware intended for the camera module used by HazRob.

## Network contract

- Joins the open `HazRob` AP automatically.
- Static IP: `192.168.4.2`.
- Gateway/master: `192.168.4.1`.
- Control/status: port 80.
- MJPEG stream: `http://192.168.4.2:81/stream` (port 81 accepts the stream connection; the path is intentionally compatible with the Android client).

Endpoints:

- `/status`
- `/flash?mode=off|auto|on`
- `/control?var=led_intensity&val=0..255` compatibility fallback

`auto` is currently a stored mode with the physical flash kept OFF; no ambient-light policy is claimed.

## Camera pipeline

- AI-Thinker pin map.
- XCLK 20 MHz.
- `RGB565`, QVGA 320×240, one framebuffer, `CAMERA_GRAB_WHEN_EMPTY`.
- Software JPEG conversion into a fixed PSRAM buffer before network transmission.
- Camera framebuffer is returned immediately after JPEG conversion, before Wi-Fi transmission.
- Wi-Fi sleep disabled.

The GC2145 sensor can be identified from `/status` by PID `0x2145` (decimal 8517). The sketch deliberately does not hard-code undocumented GC2145 register-windowing writes. Sensor-windowing/Kconfig tuning should be added only after validating the exact esp32-camera/core revision on the physical module.

Classic ESP32 + GC2145 does not provide hardware JPEG in this pipeline. QVGA RGB capture + software JPEG + Wi-Fi is the limiting path; a stable single-digit FPS range can be realistic, while 20–30 FPS should not be assumed.
