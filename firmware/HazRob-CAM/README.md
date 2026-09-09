# HazRob ESP32-CAM — 0.10.0

Target: AI-Thinker-style ESP32-CAM. This controller is intentionally isolated from robot motion and chemical inference: it provides local video/flash status and the remote WSS JPEG publisher only.

## Network contract

- Wi-Fi SSID: `HazRob`.
- static IP: `192.168.4.2`.
- gateway/master ESP32: `192.168.4.1`.
- HTTP status/control: port 80.
- local MJPEG: port 81, canonical `/stream`.
- `GET /stream` on port 80 redirects to port 81.

Endpoints: `/`, `/status`, `/stream`, `/flash`, `/led`, `/control`.

## Camera pipeline

- AI-Thinker pin map; flash GPIO4.
- XCLK 20 MHz.
- QVGA 320×240 RGB565 capture in PSRAM.
- one framebuffer, `CAMERA_GRAB_WHEN_EMPTY`.
- software JPEG conversion through `frame2jpg_cb` into a fixed 160 KiB PSRAM buffer.
- JPEG structural validation before transmission.
- local MJPEG and remote WSS share a camera mutex so frame capture is serialized safely.
- Wi-Fi sleep disabled.

`/status` reports firmware ID, camera/PSRAM state, PID/sensor label, dimensions, JPEG quality, measured FPS, encode time, JPEG size, capture/encode error counters, stream clients, RSSI, flash state and remote-video diagnostics.

## Remote video

`RemoteVideo.cpp` publishes binary JPEG frames through WSS to the configured Render relay. Credentials and token are kept outside source control in the protected secrets header. Connection recovery uses bounded backoff rather than an aggressive fixed reconnect loop.

The master ESP32 provides the outbound Internet route via NAPT when its STA/WAN path is ready. Loss of remote video does not disable local MJPEG.

## Capacity policy

The 0.10 sketch was deliberately compacted. It contains no robot drive code, no ChemCore, no molecular database and no analytical models. GitHub Actions compiles the exact source, records Flash/RAM usage and packages this directory as the `HazRob-ESP32-CAM-v0.10.0-source` release artifact.

## Field test

Power the master ESP32 first, then ESP32-CAM. Verify `http://192.168.4.2/status` and the local stream before testing WSS/Render. Phone-side MP4 recording remains an Android function; it is separate from the camera firmware.
