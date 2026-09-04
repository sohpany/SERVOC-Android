# HazRob 0.9 — Remote Internet Provisioning

This document defines the production remote-service boundary for HazRob 0.9. Public service coordinates are committed. Production secrets are not.

## Production services

- MQTT/TLS: **HiveMQ Cloud Serverless**
  - host: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud`
  - port: `8883`
- JPEG/WSS relay: **Render Web Service**
  - base: `https://hazrob-video-relay.onrender.com`
  - health: `https://hazrob-video-relay.onrender.com/health`
- Robot identity: `hazrob-001`

Render `/health` has been verified with `ok=true`, `platform=render` and `authConfigured=true`.

## 1. Architecture

Direct mode remains unchanged:

- Phone ↔ HazRob AP ↔ Master ESP32 `192.168.4.1`
- ESP32-CAM `192.168.4.2`
- local HTTP API 4 + MJPEG `:81`

Remote mode adds outbound-only Internet paths:

- Master ESP32 STA → modem/router → HiveMQ MQTT/TLS
- Android → Internet → HiveMQ MQTT/TLS
- ESP32-CAM → Master NAPT → modem/router → Render WSS
- Android → Internet → Render WSS

No public IP, inbound modem port-forward or direct Internet exposure of the ESP32 HTTP server is required.

## 2. Master ESP32 private flashing profile

Public profile: `firmware/HazRob/RemoteCloudConfig.h`

Pinned values:

- broker host above
- port `8883`
- `ROBOT_ID = "hazrob-001"`
- username `hazrob_robot`

Private local file for field flashing:

`firmware/HazRob/RemoteCloudSecrets.h`

Create it from `RemoteCloudSecrets.example.h` and provide:

- HiveMQ password for `hazrob_robot`
- trusted root CA that validates the HiveMQ hostname

The private file is ignored by Git. `setInsecure()` is not used.

The Master keeps its AP while joining the modem as STA. After Wi-Fi/IP, DNS and usable network time are available it establishes MQTT/TLS. When WAN is ready it enables NAPT for HazRob AP clients, allowing ESP32-CAM to reach the Internet without changing `192.168.4.2`.

## 3. Android MQTT profile

Public broker/robot coordinates are seeded automatically by `RemoteServiceProfile`.

The developer role supplies once on the installed device:

- MQTT username `hazrob_app`
- its HiveMQ password

The credentials are stored encrypted through Android Keystore. They are not committed into the APK source tree.

Remote entry verifies TLS/MQTT, a fresh nonce ACK and live telemetry. A Wi-Fi association alone is never Remote ready.

## 4. ESP32-CAM private flashing profile

Public profile: `firmware/HazRob-CAM/RemoteVideoConfig.h`

Pinned values:

- host `hazrob-video-relay.onrender.com`
- port `443`
- base path `/hazrob/video`
- robot `hazrob-001`

Private local file for field flashing:

`firmware/HazRob-CAM/RemoteVideoSecrets.h`

Create it from `RemoteVideoSecrets.example.h` and provide:

- Render `PUBLISH_TOKEN`
- trusted root CA that validates `hazrob-video-relay.onrender.com`

Default cellular publishing is approximately 4 FPS (`FRAME_INTERVAL_MS=250`). Local MJPEG remains independent.

## 5. Render video relay

Repository service: `remote/video-relay/`

Production routes:

- Publisher: `wss://hazrob-video-relay.onrender.com/hazrob/video/publish/hazrob-001`
- Viewer: `wss://hazrob-video-relay.onrender.com/hazrob/video/view/hazrob-001`
- Health: `https://hazrob-video-relay.onrender.com/health`
- Authenticated status: `https://hazrob-video-relay.onrender.com/hazrob/video/status/hazrob-001`

Render environment variables:

- `PUBLISH_TOKEN`
- `VIEW_TOKEN`

They must remain different and private.

The status endpoint requires the viewer Bearer token and reports whether the robot publisher is online. The relay accepts binary JPEG frames only, keeps one publisher per robot, supports multiple viewers and skips slow viewers rather than blocking camera capture.

Render Auto-Deploy follows commits to the selected `dev/hazrob-0.9` service directory.

## 6. Android video profile

The viewer WSS URL is public and seeded automatically:

`wss://hazrob-video-relay.onrender.com/hazrob/video/view/hazrob-001`

The developer role supplies once on the installed device:

- Render `VIEW_TOKEN`

The token is encrypted through Android Keystore.

Before the main Remote UI opens, Android verifies:

1. the authenticated Render status endpoint,
2. `publisher_online=true`,
3. successful WSS viewer authentication/upgrade.

In Direct mode, Control uses local MJPEG. In Remote mode it uses WSS. Recording and flame analysis consume the same JPEG path in both modes.

## 7. Remote command safety

MQTT commands carry:

- random session ID,
- monotonically increasing sequence,
- nonce,
- send time,
- TTL.

Firmware rejects expired/out-of-order commands. Motion commands are not retained. Motion stop, measurement cancel and MQ power remain separate safety domains.

Remote transport ownership is fail-closed: if MQTT drops, the application does not silently fall back to local HTTP. Returning to Direct requires an explicit mode transition.

`RobotLinkService` reconnects Remote MQTT with bounded backoff: 2 s, 4 s, 8 s, 16 s, then 30 s maximum. A new socket is not accepted as a control session until a fresh probe is acknowledged.

## 8. Camera flash in Remote mode

Android sends `camera_flash` through MQTT. Master ESP32 validates the command and forwards it on the private HazRob LAN to ESP32-CAM `/flash`. Direct mode calls the camera locally.

## 9. Remaining private provisioning before field test

These items intentionally remain outside Git and must be supplied before flashing/testing:

1. `hazrob_robot` HiveMQ password in local `RemoteCloudSecrets.h`.
2. HiveMQ trusted root CA in the same local file.
3. Render `PUBLISH_TOKEN` in local `RemoteVideoSecrets.h`.
4. Render trusted root CA in the same local file.
5. `hazrob_app` HiveMQ password entered once in Android developer settings.
6. Render `VIEW_TOKEN` entered once in Android developer settings.
7. Actual modem/router SSID and password entered through the Remote connection gate during commissioning.

Do not send these secrets through Git, source code, reports or logs.

## 10. Field commissioning order

1. Build/flash the 0.9 Direct-capable firmware and confirm Direct mode.
2. Create the two private firmware secret headers from their example templates.
3. Flash Master with HiveMQ private profile.
4. Flash ESP32-CAM with Render publisher private profile.
5. While locally connected to HazRob, select Internet control and enter modem SSID/password.
6. Confirm Master WAN, DNS/time, TLS, MQTT and NAPT.
7. Enter Android `hazrob_app` password and `VIEW_TOKEN` once under developer settings.
8. Re-enter Internet control and require all readiness stages, including remote video, to pass.
9. Verify remote drive, telemetry, measurements, video, recording, flame overlay and flash.
10. Interrupt cellular/MQTT deliberately and verify no HTTP fallback, safe motion behavior, reconnect and fresh session acceptance.
11. Measure latency, recovery and data consumption before field deployment.

A successful CI build or cloud deployment is not a substitute for the actual modem/4G end-to-end test.
