# HazRob 0.9 — MQTT/TLS service profile

Selected pilot service: **HiveMQ Cloud Serverless**.

As of 2026-09-03 the Serverless plan is free for development/testing, supports MQTT 3.1/3.1.1/5, TLS/SSL and WebSockets, with up to 100 connections and 10 GB monthly traffic. It has no uptime SLA, so HazRob 0.9 treats it as a pilot/development remote service rather than a guaranteed safety service.

## HazRob identity

Recommended first robot ID:

`HAZROB-001`

All MQTT traffic for this robot stays under:

`hazrob/HAZROB-001/#`

Topics used by HazRob 0.9:

- App → robot commands: `hazrob/HAZROB-001/cmd`
- Robot → app ACK: `hazrob/HAZROB-001/ack`
- Robot → app telemetry: `hazrob/HAZROB-001/telemetry`
- Robot → app status: `hazrob/HAZROB-001/status`
- Camera status: `hazrob/HAZROB-001/camera/status`

## HiveMQ Cloud Serverless setup

1. Create one free Serverless cluster.
2. Open **Access Management**.
3. Create a custom permission named `hazrob-001`:
   - Topic Filter: `hazrob/HAZROB-001/#`
   - Permission Type: **Publish and Subscribe**
4. Create two access credentials and assign the `hazrob-001` permission to each:
   - `hazrob-robot-001` — used only by the Master ESP32.
   - `hazrob-app-001` — used only by the Android application.
5. Use different strong passwords for the two credentials.

The free Serverless plan associates one permission with each credential. Using two independent credentials still lets either side be revoked without changing the other, while the custom topic filter prevents access to other HazRob robot namespaces.

## Master ESP32 values

Copy the cluster connection values into a **private flashing copy** of:

`firmware/HazRob/RemoteCloudConfig.h`

Set:

- `BROKER_HOST` — HiveMQ cluster hostname.
- `BROKER_PORT` — normally `8883`.
- `ROBOT_ID` — `HAZROB-001`.
- `USERNAME` — robot credential username.
- `PASSWORD` — robot credential password.
- `ROOT_CA` — PEM root CA that validates the exact broker hostname.

Never use `setInsecure()` and never commit the password or private provisioning copy.

## Android values

Developer role → Settings → `Remote MQTT/TLS`:

- Broker = same HiveMQ cluster hostname.
- Port = `8883`.
- Robot ID = `HAZROB-001`.
- Username = app credential username.
- Password = app credential password.

Android stores the username/password using its encrypted credential store.

## Acceptance test when the modem is available

Remote is accepted only after all of these pass:

1. Master joins modem Wi-Fi.
2. DHCP/IP is valid.
3. DNS works.
4. Network time is valid.
5. Master TLS verifies the broker.
6. Master MQTT authenticates.
7. Android TLS/MQTT authenticates independently.
8. Android sends a fresh `probe` with session/sequence/nonce/TTL.
9. Master returns the matching ACK.
10. Android receives live telemetry.

A Wi-Fi association or an Internet ping alone never marks HazRob Remote ready.
