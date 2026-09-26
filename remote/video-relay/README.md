# HazRob 0.10.2 / HR069 — Remote bootstrap and video relay

This Node.js service is the Internet-facing bootstrap and JPEG relay for HazRob Remote mode. Direct/local ESP32-CAM MJPEG remains independent.

## Production topology

- Deployment authority: **Railway**.
- Source branch: `release/hazrob-0.10.2`.
- Service root: `remote/video-relay`.
- Service version: `0.10.2`; wire protocol: `2`.
- Primary MQTT: **EMQX Cloud Serverless** over TLS/8883.
- Contingency MQTT: **HiveMQ Cloud Serverless**, retained for controlled fallback only.
- Robot ID: `hazrob-001`.

Canonical Railway coordinates are defined by Android `RemoteServiceProfile.java`, Main ESP32 `RemoteCloudConfig.h`, and ESP32-CAM remote configuration. Do not replace them with the legacy Render endpoint.

## Broker selection

The relay uses provider-neutral `MQTT_HOST` and `MQTT_PORT` when supplied. Compatibility aliases `EMQX_HOST` and `EMQX_PORT` remain accepted. Production should explicitly configure EMQX; HiveMQ is a contingency target, not the canonical default.

Role-scoped MQTT credentials are separate for Android and Main ESP32. Bootstrap, publisher and viewer tokens must remain secret and must not be committed.

## Runtime behavior

The bootstrap endpoint returns only the credentials/tokens required by the authenticated client role. Video uses separate publisher and viewer Bearer tokens. The relay validates JPEG framing and size, applies viewer backpressure, and maintains heartbeat/reconnect behavior.

The health endpoint reports service/protocol identity, bootstrap configuration, MQTT diagnostics and active video room state. MQTT diagnostics verify TLS/authentication and, when configured, command/ACK/telemetry/status ACL flow.

## Acceptance

Remote mode is not accepted solely because Railway or a broker dashboard reports healthy. Acceptance requires an end-to-end test through the actual WAN path: bootstrap, TLS/MQTT authentication, command/ACK, telemetry, retained status, ESP32-CAM publish, Android WSS viewing, reconnect behavior and safety/timeout enforcement.

Render is a legacy deployment path and is not the HR069 production authority.
