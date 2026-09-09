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

`RemoteVideo.cpp` publishes binary JPEG frames through WSS to the Render relay.

For the normal HazRob 0.10 field build, **do not create `RemoteVideoSecrets.h` and do not paste a token or certificate into Arduino IDE**. The camera obtains its publisher credential at runtime from the authenticated Render bootstrap endpoint and removes spaces, tabs and line breaks before authentication. It prefers a dedicated `video_publish_token` when the relay provides one; the current conference prototype falls back to the same normalized bootstrap MQTT credential, which Render is configured to accept as the camera publisher credential. The viewer token remains separate.

The trusted public root CA is embedded in `RemoteVideoConfig.h`. `RemoteVideoSecrets.example.h` is retained only as an optional controlled fallback for diagnostics and contains no live secret.

If Render rejects a WSS publisher token with HTTP 401, the camera invalidates its cached runtime credential and obtains a fresh one on the next reconnect attempt. Connection recovery uses bounded exponential backoff from 2 to 30 seconds rather than an aggressive fixed reconnect loop.

The master ESP32 provides the outbound Internet route via NAPT when its STA/WAN path is ready. Loss of remote video does not disable local MJPEG.

## Capacity policy

The 0.10 sketch contains no robot drive code, no ChemCore, no molecular database and no analytical models. GitHub Actions compiles the exact source, records Flash/RAM usage and packages this directory as the `HazRob-ESP32-CAM-v0.10.0-source` release artifact.

## Field test

Power the master ESP32 first, then ESP32-CAM. Verify `http://192.168.4.2/status` and the local stream before testing WSS/Render. In `/status`, `remote_video.credential_ready=true` confirms that the camera successfully obtained a runtime publisher credential; `remote_video.connected=true` confirms the WSS publisher connection.

Phone-side MP4 recording remains an Android function and is separate from camera firmware.
