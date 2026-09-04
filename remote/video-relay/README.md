# HazRob 0.9 Remote Video Relay

This service is the Internet-facing JPEG relay for the ESP32-CAM remote path. Direct/local MJPEG on `192.168.4.2:81` remains independent.

Selected deployment platform: **Render** as a Node.js WebSocket web service. Render provides the public TLS/WSS endpoint; HazRob keeps HiveMQ responsible for control and telemetry while this service handles camera JPEG frames only.

## Production endpoints

- Public service: `https://hazrob-video-relay.onrender.com`
- Publisher: `wss://hazrob-video-relay.onrender.com/hazrob/video/publish/hazrob-001`
- Viewer: `wss://hazrob-video-relay.onrender.com/hazrob/video/view/hazrob-001`
- Health: `https://hazrob-video-relay.onrender.com/health`
- Authenticated robot-video status: `https://hazrob-video-relay.onrender.com/hazrob/video/status/hazrob-001`

Publisher and viewer use separate Bearer tokens. Configure Render environment variables named:

- `PUBLISH_TOKEN`
- `VIEW_TOKEN`

Never commit their values. The ESP32-CAM uses the publisher token; the Android app uses the viewer token.

The status endpoint requires the viewer Bearer token and reports whether the selected robot currently has a live publisher. This allows the Android Remote entry gate to verify the actual video path before admitting the operator to Remote mode.

## Runtime behavior

The relay keeps an in-memory room per robot ID. One publisher is accepted per robot; a new publisher replaces the previous publisher. Any number of viewers may subscribe to the same robot within service capacity.

Only binary JPEG frames are forwarded. Invalid JPEG payloads, oversized frames and publisher text frames are discarded. Slow viewers are skipped when their WebSocket send buffer exceeds the configured backpressure limit, preventing one slow client from blocking camera ingestion. The service also sends periodic WebSocket pings and cleans up disconnected clients.

Service restarts are expected during deployments. ESP32-CAM and Android clients must reconnect automatically; no video state is persisted on the server.

## Render deployment

Repository: `sohpany/SERVOC-Android`

Branch: `dev/hazrob-0.9`

Service root directory:

`remote/video-relay`

Build command:

`npm install`

Start command:

`npm start`

Health check path:

`/health`

Render Auto-Deploy is enabled on commit. Changes under this service directory therefore deploy automatically from the selected branch.

The repository intentionally contains no production token or private certificate material.

## Field acceptance

Do not mark remote video field-ready until all of these succeed over the actual 4G path:

1. ESP32-CAM reaches the relay through the selected modem/NAPT path.
2. TLS validates `hazrob-video-relay.onrender.com`.
3. Publisher authentication succeeds.
4. Android viewer authentication succeeds.
5. Authenticated status reports `publisher_online=true`.
6. First JPEG frame is received.
7. Continuous viewing remains stable under cellular latency.
8. Local MJPEG remains functional when Direct mode is selected.
9. Recording, flame evidence and remote flash still operate.

A successful cloud deployment is not a substitute for this end-to-end field test.
