# HazRob ESP32-CAM — 0.10.1

Target: AI-Thinker-style ESP32-CAM. This controller is intentionally isolated from robot motion and chemical inference: it provides local video/flash status and the remote WSS JPEG publisher only.

## Network contract

- Wi-Fi SSID: `HazRob`.
- Static IP: `192.168.4.2`.
- Gateway/master ESP32: `192.168.4.1`.
- Compact HTTP status/control server: port 80 using `WiFiServer`.
- Local MJPEG: port 81, canonical `/stream`.
- `GET /stream` on port 80 redirects to port 81.

Canonical 0.10.1 endpoints are `/`, `/status`, `/stream`, and `/flash?mode=off|auto|on`. Retired `/led`, `/control`, `/mjpeg`, and `/flash?on=` compatibility routes are not part of the 0.10.1 field contract.

## Camera pipeline

- AI-Thinker pin map; flash GPIO4.
- XCLK 20 MHz.
- QVGA 320×240.
- Native sensor JPEG is attempted first with two PSRAM framebuffers and latest-frame capture.
- If native JPEG initialization fails, firmware falls back to RGB565 capture plus `frame2jpg_cb` software JPEG conversion.
- A fixed 160 KiB PSRAM JPEG buffer is used by local and remote paths.
- JPEG structural validation occurs before transmission.
- Local MJPEG and remote WSS share a camera mutex; Remote WSS capture pauses while a local MJPEG viewer is active so the local stream remains latency-priority.
- Wi-Fi sleep is disabled.

`/status` reports firmware ID, camera/PSRAM state, PID/sensor label, dimensions, capture format, JPEG quality, measured FPS, JPEG size, capture/encode error counters, stream clients, RSSI, flash state and remote-video diagnostics.

## Remote video

`RemoteVideo.cpp` publishes binary JPEG frames through authenticated WSS to `hazrob-video-relay.onrender.com`.

For HazRob 0.10.1, the camera identifies itself to the authenticated bootstrap endpoint as `esp32-cam` and accepts the role-scoped `video_publish_token`. It does not use the robot MQTT password as its intended video credential. The Android viewer receives a separate view token.

The trusted public root CA is embedded in `RemoteVideoConfig.h`. `RemoteVideoSecrets.example.h` is optional fallback-only and contains no live secret.

If Render rejects a WSS publisher token with HTTP 401, the camera invalidates its runtime credential and obtains a fresh publisher token on the next reconnect attempt. Connection recovery uses bounded exponential backoff from 2 to 30 seconds.

Remote TLS startup is gated on station Wi-Fi plus a valid SNTP wall clock. The master ESP32 provides the outbound Internet route via NAPT when its STA/WAN path is ready. Loss of remote video does not disable local MJPEG.

## Flash footprint policy

The 0.10.1 field source intentionally uses a compact raw HTTP server rather than Arduino `WebServer`. This is a binary-footprint optimization, not source minification: comments and whitespace do not materially change compiled Flash usage.

The camera firmware contains no robot drive code, ChemCore, molecular database, or analytical models. Build verification records compiled Flash/RAM usage and packages this directory as the `HazRob-ESP32-CAM-v0.10.1-source` artifact.

## Field test

Power the master ESP32 first, then ESP32-CAM. Verify `http://192.168.4.2/status`, `http://192.168.4.2:81/stream`, and `/flash?mode=on|off` before testing WSS/Render. In `/status`, `remote_video.credential_ready=true` confirms a runtime publisher credential and `remote_video.connected=true` confirms the WSS publisher connection.

Phone-side MP4 recording remains an Android function and is separate from camera firmware.
