# HazRob ESP32-CAM firmware — 0.10.1

Hardware target: AI-Thinker-style ESP32-CAM.

This directory is the canonical camera-controller source for HazRob 0.10.1.

## Network contract

- Station SSID: `HazRob`
- Static IP: `192.168.4.2`
- Gateway/main ESP32: `192.168.4.1`
- Wi-Fi mode: STA-only
- The camera never creates a SoftAP
- Wi-Fi sleep and library auto-reconnect are disabled; reconnect is controlled by the firmware

## Local camera service

- HTTP status/control: port 80
- MJPEG stream: port 81, `/stream`
- `GET /stream` on port 80 redirects to the port-81 stream
- Local viewing has priority over remote publishing

The local stream uses:

- AI-Thinker pin mapping
- XCLK 20 MHz
- QVGA 320×240
- RGB565 capture
- software `frame2jpg`
- JPEG quality 35
- one PSRAM framebuffer
- 160 KiB JPEG buffer
- structural JPEG validation before transmission

The local MJPEG task is given higher scheduling priority than the camera control loop, while still yielding between frames. TCP no-delay is enabled for the local viewer.

## Remote video

Remote video publishes JPEG frames through authenticated WSS to `hazrob-video-relay.onrender.com`.

Remote publishing is enabled only when the main controller reports REMOTE mode, Internet readiness and NAPT, and the camera has a valid TLS wall clock. A local MJPEG client pauses remote publishing.

The trusted ISRG Root X1 CA used by the camera is the same validated root used by the main controller. Remote video uses bounded reconnect backoff and adaptive frame pacing from 55 ms to 120 ms. The remote video worker remains alive after first creation so stopping and restarting remote publishing cannot lose the task during a transition.

## Flash

GPIO4 controls the onboard flash LED.

Supported mode values are `off`, `auto`, and `on`. In this firmware `auto` is retained as a protocol state; automatic luminance-based switching is not implemented.

## Build target

Recommended Arduino-ESP32 core: 3.3.11.

Manual board target: AI Thinker ESP32-CAM / `esp32cam`.
