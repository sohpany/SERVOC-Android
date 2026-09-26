# HazRob 0.10.2 / HR069 — MQTT/TLS service profile

## Service topology

- **Primary MQTT broker:** EMQX Cloud Serverless, TLS port `8883`.
- **Contingency broker:** HiveMQ Cloud Serverless. It is retained as a fallback and must not be removed from controlled recovery documentation.
- **Bootstrap and remote video relay:** Railway.
- Robot identity: `hazrob-001`.
- MQTT namespace: `hazrob/hazrob-001/#`.
- Wire protocol: `2`.

The production Android and Main ESP32 profiles use the same EMQX host defined in `RemoteServiceProfile.java` and `RemoteCloudConfig.h`. Broker credentials are role-scoped and obtained through the Railway bootstrap service. Secrets must never be committed.

## MQTT topics

- App -> robot commands: `hazrob/hazrob-001/cmd`
- Robot -> app ACK: `hazrob/hazrob-001/ack`
- Robot -> app telemetry: `hazrob/hazrob-001/telemetry`
- Robot -> app retained status: `hazrob/hazrob-001/status`

## Primary EMQX contract

Both Android and Main ESP32 must use TLS with certificate verification on port 8883. Android and robot use separate credentials. Remote readiness requires successful TLS, MQTT authentication, topic authorization, a fresh command/ACK exchange and live telemetry; Internet reachability alone is insufficient.

## HiveMQ contingency contract

HiveMQ is retained only as a controlled contingency path. A switch to HiveMQ must be explicit: configure the broker host/port and corresponding role-scoped credentials in the deployment/runtime configuration. Do not silently change the canonical Android or firmware production defaults away from EMQX. The same robot ID, topic namespace, TLS verification, protocol 2 envelope, TTL/sequence/nonce and authorization rules apply.

The relay accepts `MQTT_HOST` / `MQTT_PORT` as provider-neutral deployment variables. Legacy `EMQX_HOST` / `EMQX_PORT` aliases remain supported for compatibility.

## Railway bootstrap/video contract

Railway is the current bootstrap and video-relay authority for 0.10.2/HR069. It returns role-scoped runtime credentials/tokens and exposes HTTPS/WSS endpoints. The Android app, Main ESP32 and ESP32-CAM must agree on service version `0.10.2`, protocol `2` and robot ID `hazrob-001`.

## Acceptance

Remote is accepted only after:
1. Main ESP32 obtains WAN connectivity and valid time.
2. TLS validates the selected MQTT broker.
3. Main and Android authenticate independently.
4. Topic ACLs permit only the required command/ACK/telemetry/status flow.
5. Android sends a fresh probe with session/sequence/nonce/TTL and receives the matching ACK.
6. Live telemetry is received.
7. ESP32-CAM authenticates to the Railway relay and publishes JPEG frames.
8. Android receives the authenticated remote video stream.
9. E-stop, command TTL/watchdog and Direct-mode safety remain effective.

A cloud dashboard status or Internet ping is not an end-to-end acceptance test.
