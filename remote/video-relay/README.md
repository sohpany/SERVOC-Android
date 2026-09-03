# HazRob 0.9 Remote Video Relay

This service is the Internet-facing JPEG relay for the ESP32-CAM remote path. Direct/local MJPEG on `192.168.4.2:81` remains independent.

## Endpoints

- Publisher: `wss://<worker>/hazrob/video/publish/<robot-id>`
- Viewer: `wss://<worker>/hazrob/video/view/<robot-id>`
- Health: `https://<worker>/health`

Publisher and viewer use separate Bearer tokens. Store them as Cloudflare Worker secrets named `PUBLISH_TOKEN` and `VIEW_TOKEN`; never commit secret values.

The Durable Object is keyed by robot ID. Only one publisher is accepted per robot; a new publisher replaces the old one. Viewer sockets receive binary JPEG WebSocket messages. Non-JPEG payloads, oversized frames and text from the publisher are discarded. Slow viewers are skipped when their send buffer exceeds the configured backpressure threshold, so camera capture is not blocked by a slow phone connection.

The ESP32-CAM `RemoteVideoConfig.h` uses the publisher endpoint/token and a trusted CA certificate. The Android developer settings use the viewer endpoint/token. These two tokens should be different.

## Deployment

Install Wrangler, authenticate the intended Cloudflare account, then from this directory set the two secrets and deploy. The repository intentionally contains no deployed hostname, token or production certificate.

After deployment, provision the resulting WSS hostname/path and CA into the local flashing copy of `firmware/HazRob-CAM/RemoteVideoConfig.h`; configure the viewer WSS URL and viewer token through HazRob developer settings. Do not mark remote video field-ready until an end-to-end publisher/viewer test succeeds over the actual cellular modem.
