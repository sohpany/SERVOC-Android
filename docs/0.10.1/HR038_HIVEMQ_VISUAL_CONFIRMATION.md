# HR-0101-038 — HiveMQ console and post-token verification

Date: 2026-09-12

This note supplements `CONTROLLED_CHANGE_LOG_ADDENDUM_HR038.md` and records the final operator-visible HiveMQ configuration cross-check plus a fresh production rerun after explicitly aligning the Render bootstrap token.

## HiveMQ console visual cross-check — PASS

The HiveMQ Cloud Access Management screen was inspected during the controlled test session and showed:

- credential `hazrob_robot` -> permission `hazrob-001-all`;
- credential `hazrob_app` -> permission `hazrob-001-all`;
- permission `hazrob-001-all` -> `Publish and Subscribe`;
- topic filter -> `hazrob/hazrob-001/#`.

The permission description still says `HazRob 0.9 robot and app`. This is a stale human-readable description only; it does not change the permission name, topic filter, publish/subscribe grant, robot identity or protocol behavior. It may be renamed later for housekeeping but is not a connection blocker.

This visual configuration matches the 0.10.1 runtime topic root used by Android and the main ESP32.

## Bootstrap token alignment — COMPLETE

Production Render environment variable `BOOTSTRAP_TOKEN` was merged/set to the same value compiled into the controlled 0.10.1 Android/main-ESP32 clients:

`hazrob-prototype-bootstrap-v1`

No HiveMQ password, MQTT username, video token or unrelated Render environment variable was changed by this operation.

The environment update triggered production deploy:

- deploy: `dep-daip180ae00c73fd99dg`;
- source: `8a80094a0c4c34fa9eb5747637ed1a384277945a` (documentation-only head; executable relay source remains the HR-0101-038 implementation);
- status: `live`.

## Fresh production HiveMQ test after token alignment — PASS

Startup diagnostics on the new production instance returned:

- application identity: TLS true, MQTT true, CONNACK 0 / accepted;
- robot identity: TLS true, MQTT true, CONNACK 0 / accepted;
- ACL diagnostic state: `pass`;
- Android-side subscriptions permitted: `ack`, `telemetry`, `status`;
- robot-side subscription permitted: `cmd`;
- application publish `cmd` -> robot receive: PASS;
- robot publish `ack` -> application receive: PASS;
- robot publish `telemetry` -> application receive: PASS;
- retained robot `status` test: PASS;
- retained status restoration after diagnostic: PASS.

## Classification

The following are now excluded as the cause of the observed Remote connection problem:

- wrong current `hazrob_app` password;
- wrong current `hazrob_robot` password;
- HiveMQ TLS/CONNECT rejection;
- HiveMQ topic ACL rejection for the tested HazRob topic tree;
- retained-status permission rejection;
- production Render bootstrap-token mismatch.

The remaining evidence must come from the powered main ESP32 and Android Remote session: external Wi-Fi association, DNS, NTP, ESP32 -> Render HTTPS bootstrap, ESP32 -> HiveMQ TLS/MQTT/SUBACK, then real Android `probe` ACK and telemetry round-trip.

Status: **PASS — Render/HiveMQ authentication, authorization, retained path and bootstrap-token alignment. Physical E2E remains pending.**
