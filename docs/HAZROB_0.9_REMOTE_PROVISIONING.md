# HazRob 0.9 — Remote 4G / MQTT / WSS provisioning

This document describes the provisioning boundary for HazRob 0.9. It does **not** contain production credentials and the repository intentionally ships with remote service credentials disabled.

Pilot providers selected for 0.9:

- MQTT/TLS: **HiveMQ Cloud Serverless**
- JPEG/WSS relay: **Cloudflare Workers + Durable Objects**

Reserved first robot identity: `hazrob-001`.

## 1. Architecture

Direct mode remains unchanged:

- Phone ↔ HazRob AP ↔ Master ESP32 `192.168.4.1`
- ESP32-CAM `192.168.4.2`
- HTTP API4 + local MJPEG `:81`

Remote mode adds outbound-only Internet paths:

- Master ESP32 STA → 4G modem → MQTT/TLS broker
- Android → Internet → same MQTT/TLS broker
- ESP32-CAM → Master NAPT → 4G modem → WSS video relay
- Android → Internet → WSS video relay

No public IP, inbound modem port-forward or direct Internet exposure of the ESP32 HTTP server is required.

## 2. Master ESP32 private flashing profile

File: `firmware/HazRob/RemoteCloudConfig.h`

The repository template reserves:

- `ROBOT_ID = "hazrob-001"`

Provision in the private flashing copy/build process:

- `BROKER_HOST`
- `BROKER_PORT` (normally TLS port 8883)
- `USERNAME`
- `PASSWORD`
- `ROOT_CA` PEM that validates the broker hostname

The repository must keep the real service credentials blank. `setInsecure()` is not used.

The master keeps its local AP while joining the modem as STA. After Wi-Fi/IP, DNS and usable network time are available, it establishes MQTT/TLS. If WAN is ready it enables NAPT for clients on the HazRob AP, allowing ESP32-CAM to reach the Internet without changing `192.168.4.2`.

Detailed HiveMQ setup: `remote/mqtt/README.md`.

## 3. Android MQTT profile

Available to the **developer role only** in Settings → `Remote MQTT/TLS`.

Set:

- Broker hostname
- Port
- Robot ID `hazrob-001`
- MQTT app username
- MQTT app password

Username/password are stored encrypted through Android Keystore. The ordinary user role does not display these fields.

Before Remote mode is admitted, the connection gate verifies the robot WAN path plus phone-side TLS/MQTT, nonce ACK and live telemetry. A successful Wi-Fi connection alone is not considered Remote ready.

The developer `اختبار` action is a temporary connectivity probe when the application is currently in Direct mode. It opens the cloud session only for the test and releases Remote transport ownership afterward, so testing credentials cannot silently switch the running control path.

## 4. ESP32-CAM private flashing profile

File: `firmware/HazRob-CAM/RemoteVideoConfig.h`

The repository template reserves `ROBOT_ID = "hazrob-001"` and keeps real service credentials disabled.

Provision in the private flashing copy/build process:

- `HOST` — deployed video relay hostname
- `PORT` — normally 443
- `BASE_PATH` — default `/hazrob/video`
- `PUBLISH_TOKEN`
- `ROOT_CA` PEM for the relay hostname

Default cellular publishing target is approximately 4 FPS (`FRAME_INTERVAL_MS=250`) to limit 4G data use. This can be tuned after field testing.

Local MJPEG is independent and remains available in Direct mode.

## 5. Video relay

Repository service: `remote/video-relay/`

Routes:

- Publisher: `wss://<relay>/hazrob/video/publish/hazrob-001`
- Viewer: `wss://<relay>/hazrob/video/view/hazrob-001`
- Health: `https://<relay>/health`

The Cloudflare Durable Object relay expects two secrets configured outside Git:

- `PUBLISH_TOKEN`
- `VIEW_TOKEN`

Use different values for publisher and viewer. The relay accepts binary JPEG frames only, keeps one publisher per robot, supports multiple viewers and drops frames for slow viewers rather than blocking the camera.

A manual GitHub Actions deployment workflow is prepared at `.github/workflows/deploy-video-relay.yml`. It requires Cloudflare account/API credentials and two HazRob video tokens in GitHub Secrets. Normal pushes never deploy the relay.

A separate CI workflow, `.github/workflows/validate-video-relay.yml`, runs Wrangler in dry-run mode and requires no Cloudflare credentials.

The relay code is prepared but **not deployed by the repository build**. Deployment and real service credentials are a separate authenticated provisioning step.

## 6. Android WSS video profile

Available to the **developer role only** in Settings → `Remote Video / WSS`.

Set:

- Viewer URL: `wss://<relay>/hazrob/video/view/hazrob-001`
- Viewer Bearer token (`VIEW_TOKEN`)

The token is encrypted through Android Keystore. In Remote mode the Control tab automatically selects WSS; in Direct mode it selects local MJPEG. Recording and flame-evidence analysis consume the same decoded JPEG frame path in both modes.

## 7. Remote command safety contract

MQTT commands include:

- random session ID
- monotonically increasing sequence
- nonce
- send time
- TTL

Firmware rejects expired/out-of-order commands. Once a session is established, changing to a different session requires a valid `probe` handshake; a delayed Drive/Relay command from an old session cannot switch sessions.

Commands that require acknowledgement fail explicitly if MQTT disconnects before the ACK arrives or if firmware returns `ok=false`. Motion commands are not retained. Motion stop and MQ power remain separate safety domains.

### Fail-closed transport ownership

When the application is operating in Remote mode, Remote transport ownership is tracked independently from the current MQTT socket. If TLS/MQTT drops, `RobotApi` does **not** fall back to local HTTP, even if `192.168.4.1` happens to be reachable. Commands fail until the remote session is restored.

Returning to local HTTP requires an explicit Direct-mode transition. The Direct connection gate releases Remote ownership before MainActivity starts using the local API.

### Reconnect policy

`RobotLinkService` keeps Remote connectivity alive and retries a lost MQTT session with bounded exponential backoff:

- 2 s
- 4 s
- 8 s
- 16 s
- 30 s maximum

A newly connected MQTT socket is not accepted as a usable control session until the robot acknowledges a fresh `probe`, which establishes the new session ID under the firmware anti-replay rules. Backoff resets immediately after successful reconnection.

## 8. Camera flash in Remote mode

Android sends `camera_flash` over MQTT. Master ESP32 validates the command and forwards it over the private HazRob LAN to ESP32-CAM `/flash`. Direct mode continues to call ESP32-CAM locally.

## 9. Field commissioning order

1. Flash the repository/default 0.9 builds first and confirm Direct mode still works.
2. Create/provision the HiveMQ Cloud Serverless service and private MQTT credentials.
3. Deploy/configure the Cloudflare WSS relay and its two tokens.
4. Provision the 4G modem SSID/password from the local HazRob connection.
5. Provision and flash the private Master MQTT profile.
6. Confirm Master `/remote/status`: WAN, TLS, MQTT and NAPT.
7. Configure Android developer MQTT profile and pass the end-to-end Remote gate.
8. Provision and flash the private ESP32-CAM WSS profile.
9. Configure Android viewer WSS profile.
10. Verify Remote video, recording, flame overlay and remote flash over the real cellular link.
11. Force a cellular/MQTT interruption and verify: no local HTTP fallback, command failure during outage, automatic reconnect, fresh probe/session acceptance and recovery of telemetry.
12. Measure latency, disconnect recovery and 4G data consumption before field use.

Do not mark the Remote path field-ready from CI compile success alone; MQTT broker, modem and video relay require an end-to-end test using the actual deployed services.
