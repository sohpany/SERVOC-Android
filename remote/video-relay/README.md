# HazRob 0.9 Remote Video Relay

This service is the Internet-facing JPEG relay for the ESP32-CAM remote path. Direct/local MJPEG on `192.168.4.2:81` remains independent.

Selected deployment platform: **Railway** as a persistent Node.js WebSocket service. Railway provides the public TLS/WSS endpoint; HazRob keeps HiveMQ responsible for control and telemetry while this service handles camera JPEG frames only.

## Endpoints

- Publisher: `wss://<railway-domain>/hazrob/video/publish/hazrob-001`
- Viewer: `wss://<railway-domain>/hazrob/video/view/hazrob-001`
- Health: `https://<railway-domain>/health`

Publisher and viewer use separate Bearer tokens. Configure Railway service variables named:

- `PUBLISH_TOKEN`
- `VIEW_TOKEN`

Never commit their values. The ESP32-CAM uses the publisher token; the Android app uses the viewer token.

## Runtime behavior

The relay keeps an in-memory room per robot ID. One publisher is accepted per robot; a new publisher replaces the previous publisher. Any number of viewers may subscribe to the same robot within service capacity.

Only binary JPEG frames are forwarded. Invalid JPEG payloads, oversized frames and publisher text frames are discarded. Slow viewers are skipped when their WebSocket send buffer exceeds the configured backpressure limit, preventing one slow client from blocking camera ingestion. The service also sends periodic WebSocket pings and cleans up disconnected clients.

Service restarts are expected during deployments. ESP32-CAM and Android clients must reconnect automatically; no video state is persisted on the server.

## Railway deployment

Repository: `sohpany/SERVOC-Android`

Branch: `dev/hazrob-0.9`

Service root directory:

`/remote/video-relay`

Railway Railpack detects the Node.js `package.json`. Start command is:

`npm start`

Before the first production test, add the two service variables `PUBLISH_TOKEN` and `VIEW_TOKEN`, deploy, then generate a public Railway domain. Verify `/health` before provisioning the resulting hostname into the camera firmware and Android developer settings.

The repository intentionally contains no production token, deployed hostname or private certificate material.

## Field acceptance

Do not mark remote video field-ready until all of these succeed over the actual 4G path:

1. ESP32-CAM reaches the relay through the selected modem/NAPT path.
2. TLS validates the Railway hostname.
3. Publisher authentication succeeds.
4. Android viewer authentication succeeds.
5. First JPEG frame is received.
6. Continuous viewing remains stable under cellular latency.
7. Local MJPEG remains functional when Direct mode is selected.
8. Recording, flame evidence and remote flash still operate.

A successful cloud deployment is not a substitute for this end-to-end field test.
