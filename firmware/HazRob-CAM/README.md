# HazRob ESP32-CAM 0.7.1

Standalone AI-Thinker-style ESP32-CAM firmware for HazRob.

## Required migration

The camera must be flashed with this sketch (or another firmware using the same network/API contract). Updating the Android APK cannot change an old ESP32-CAM sketch that is still searching only for the old `Servoc` SSID. A camera that never joins the master ESP32 Wi-Fi will have both video and flash unavailable.

This 0.7.1 firmware alternates automatically between the open SSIDs `HazRob` and `Servoc` during migration. The final master network is `HazRob`.

## Network contract

- Preferred AP: `HazRob`; legacy fallback: `Servoc`.
- Static camera IP: `192.168.4.2`.
- Gateway/master ESP32: `192.168.4.1`.
- Control/status: HTTP port 80.
- MJPEG: port 81, canonical path `/stream`.
- `GET /stream` on port 80 redirects to the canonical stream.

Status and control endpoints:

- `/` -> firmware identity.
- `/status` -> JSON containing firmware, SSID, IP, sensor PID, 320x240 resolution, format, FPS, RSSI and flash state.
- `/flash?mode=off|auto|on`
- `/flash?on=0|1`
- `/led?on=0|1`
- `/led?state=0|1`
- `/control?var=led_intensity&val=0..255`
- `/control?var=flash&val=0|1`
- `/control?var=led&val=0|1`

Android 0.7.1 intentionally uses ON/OFF only. `auto` remains a compatibility mode and keeps the lamp physically OFF until an ambient-light policy is implemented.

## Camera pipeline

- AI-Thinker pin map; flash GPIO4.
- XCLK 20 MHz.
- RGB565 QVGA 320x240, one PSRAM framebuffer, `CAMERA_GRAB_WHEN_EMPTY`.
- Software JPEG conversion, quality 35, into a fixed PSRAM buffer.
- Framebuffer returned before Wi-Fi transmission.
- Wi-Fi sleep disabled.

The GC2145 is identified by PID `0x2145` (8517 decimal) in `/status`. No undocumented GC2145 register-windowing writes are used.

Classic ESP32 + GC2145 performs RGB capture + software JPEG + Wi-Fi; do not assume 20-30 FPS. The Android client uses actual incoming frame timestamps when creating MP4.

## First field test

After flashing, power the main ESP32 first and connect the phone to `HazRob`. Then power ESP32-CAM. In HazRob settings, the robot should respond at `192.168.4.1` and the camera at `192.168.4.2`. The control screen should progress from `CAM: اتصال...` to `CAM: بث مباشر`. If it remains in retry, test `/status` before investigating the video decoder.

MP4 recording is independent: files are exported to `Movies/HazRob` and are not attached to measurement sessions or PDF/Excel reports.
