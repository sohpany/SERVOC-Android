# HazRob 0.9 Remote Video Relay

This service is the Internet-facing JPEG relay for the ESP32-CAM remote path. Direct/local MJPEG on `192.168.4.2:81` remains independent.

Selected pilot platform: **Cloudflare Workers + SQLite-backed Durable Objects**. As of 2026-09-03 Durable Objects are available on the Workers Free plan. HazRob uses the WebSocket Hibernation API so idle sockets do not need to keep the object actively executing.

## Endpoints

- Publisher: `wss://<worker>/hazrob/video/publish/hazrob-001`
- Viewer: `wss://<worker>/hazrob/video/view/hazrob-001`
- Health: `https://<worker>/health`

Publisher and viewer use separate Bearer tokens. Store them as Cloudflare Worker secrets named `PUBLISH_TOKEN` and `VIEW_TOKEN`; never commit secret values.

The Durable Object is keyed by robot ID. Only one publisher is accepted per robot; a new publisher replaces the old one. Viewer sockets receive binary JPEG WebSocket messages. Non-JPEG payloads, oversized frames and text from the publisher are discarded. Slow viewers are skipped when their send buffer exceeds the configured backpressure threshold, so camera capture is not blocked by a slow phone connection.

The ESP32-CAM `RemoteVideoConfig.h` uses the publisher endpoint/token and a trusted CA certificate. The Android developer settings use the viewer endpoint/token. These two tokens must be different.

## Repository deployment workflow

Workflow:

`.github/workflows/deploy-video-relay.yml`

It is manual (`workflow_dispatch`) and never deploys on a normal push.

Add these GitHub repository secrets before running it:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
- `HAZROB_VIDEO_PUBLISH_TOKEN`
- `HAZROB_VIDEO_VIEW_TOKEN`

The workflow:

1. Deploys `wrangler.toml` and the `VideoRoom` Durable Object.
2. Installs the publisher token as the Worker secret `PUBLISH_TOKEN`.
3. Installs the viewer token as the Worker secret `VIEW_TOKEN`.

No service token is stored in Git history or in the APK source tree.

## Manual deployment alternative

If GitHub Actions is not used, authenticate Wrangler locally, then from this directory deploy and install the same two Worker secrets. The repository intentionally contains no deployed hostname, token or production certificate.

After deployment, provision the resulting WSS hostname/path and CA into the private flashing copy of `firmware/HazRob-CAM/RemoteVideoConfig.h`; configure the viewer WSS URL and viewer token through HazRob developer settings.

## Field acceptance

Do not mark remote video field-ready until all of these succeed over the actual 4G path:

1. ESP32-CAM reaches the relay through Master NAPT.
2. TLS validates the relay hostname.
3. Publisher authentication succeeds.
4. Android viewer authentication succeeds.
5. First JPEG frame is received.
6. Continuous viewing remains stable under cellular latency.
7. Local MJPEG remains functional when Direct mode is selected.
8. Recording, flame evidence and remote flash still operate.

Cloud compilation/deployment availability is not a substitute for this end-to-end field test.
