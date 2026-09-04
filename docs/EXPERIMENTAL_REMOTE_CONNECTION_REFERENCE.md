# HazRob Experimental Remote Connection Reference

Status: **EXPERIMENTAL / TEMPORARY**

This file is the working reference for the current HazRob 0.9.2-rc1 remote-control prototype. Values in this document are intended only for the current experimental setup and may be rotated later.

## MQTT / Robot control

- Broker host: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud`
- Broker port: `8883`
- Robot ID: `hazrob-001`
- Robot MQTT username: `hazrob_robot`
- Android/app MQTT username used by bootstrap flow: `hazrob_app`
- Robot MQTT password: `NOT_RECOVERABLE_FROM_CONNECTED_API`
- Android/app MQTT password: `NOT_RECOVERABLE_FROM_CONNECTED_API`
- MQTT TLS Root CA: `NOT_YET_PINNED_IN_REFERENCE`

Firmware mapping:

- `firmware/HazRob/RemoteCloudConfig.h`
- Secret header field: `HAZROB_MQTT_PASSWORD`
- Secret certificate field: `HAZROB_MQTT_ROOT_CA`

## Remote video relay

- HTTPS service: `https://hazrob-video-relay.onrender.com`
- Publisher WSS: `wss://hazrob-video-relay.onrender.com/hazrob/video/publish/hazrob-001`
- Viewer WSS: `wss://hazrob-video-relay.onrender.com/hazrob/video/view/hazrob-001`
- Health endpoint: `https://hazrob-video-relay.onrender.com/health`
- Status endpoint: `https://hazrob-video-relay.onrender.com/hazrob/video/status/hazrob-001`
- Render `PUBLISH_TOKEN`: `NOT_RECOVERABLE_FROM_CONNECTED_API`
- Render `VIEW_TOKEN`: `NOT_RECOVERABLE_FROM_CONNECTED_API`
- Render `BOOTSTRAP_TOKEN`: `NOT_RECOVERABLE_FROM_CONNECTED_API`
- Video TLS Root CA: `NOT_YET_PINNED_IN_REFERENCE`

ESP32-CAM mapping:

- `firmware/HazRob-CAM/RemoteVideoConfig.h`
- Secret token field: `HAZROB_VIDEO_PUBLISH_TOKEN`
- Secret certificate field: `HAZROB_VIDEO_ROOT_CA`

## Render deployment

- Service: `hazrob-video-relay`
- Service ID: `srv-dad3p8gn74is73d52aj0`
- Workspace ID: `tea-dad3g7m7bikc739gs3p0`
- Repository: `sohpany/SERVOC-Android`
- Render deployment branch currently configured: `dev/hazrob-0.9`
- Root directory: `remote/video-relay`

## Rule for future work

For this experimental prototype, this file is the canonical human-readable reference for remote connection coordinates and temporary credentials. Do not invent a missing secret. When an exact temporary secret is supplied or deliberately rotated, update this file and then use that same value consistently in Render, HiveMQ, Android bootstrap/configuration, and the ESP32/ESP32-CAM local secret headers.

> Note: Git history is persistent. Any plaintext experimental secret added here should be considered exposed within repository history and must be rotated before production use.
