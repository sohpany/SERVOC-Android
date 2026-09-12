# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-038

## HR-0101-038 — Remote authentication/authorization diagnostic hardening

### Purpose
Determine, by live controlled tests rather than assumption, why a HazRob Remote session can appear to fail authentication, and harden Android/main-ESP32 diagnostics so that future failures distinguish bootstrap, TLS, MQTT authentication and MQTT subscription authorization stages.

This change does **not** alter GPIO allocation, drive-command semantics, E-STOP semantics, camera pin mapping, analytical acquisition, MQTT topic names, robot identity, broker host/port, or the role model established earlier in 0.10.1.

### Authoritative executable head for HR-0101-038
- Latest executable source after HR-0101-038 code changes: `b6f16120c7be56470869d4588babcc1c74145c25`.
- Active branch: `dev/hazrob-0.10.1`.
- Production Render service: `hazrob-video-relay` (`srv-dad3p8gn74is73d52aj0`).
- Canonical broker: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud:8883`.
- Robot/topic root: `hazrob-001` / `hazrob/hazrob-001/`.

### Implemented commits
1. `08e959a81a7c9fc9b8aad5b2137f9f15540c2bf5` — Render live MQTT credential, SUBACK, topic round-trip and retained-status diagnostic.
2. `5f11291dce522ac9266df76e56a77482c157e3f6` — expose main-ESP32 remote cloud failure detail API.
3. `b8da3ef91b5f9d3250f022010055b4bf487ab099` — include staged WAN/cloud diagnostic detail in ESP32 status.
4. `b83178b5700f091f8964d0096dd832522a9d72ef` — main ESP32 validates MQTT SUBACK and reports bootstrap/TLS/CONNACK/SUBACK stages.
5. `d1a95fdbba5a4de46d484bf8991a8cc45b81113e` — Android validates SUBACK and preserves MQTT CONNACK rejection reason.
6. `b6f16120c7be56470869d4588babcc1c74145c25` — Android preserves the actual remote connection/bootstrap failure text instead of reducing it to an exception class name.

### Live production authentication test — PASS
Production Render was restarted against the existing production HiveMQ credentials without changing either HiveMQ password. On current credentials, both identities completed verified TLS and MQTT CONNECT with CONNACK 0:

- application identity: `tls=true`, `mqtt=true`, `connack=0`, `accepted`;
- robot identity: `tls=true`, `mqtt=true`, `connack=0`, `accepted`.

**Conclusion:** the current HiveMQ passwords stored in Render are valid. Password rotation is not indicated by this evidence.

### Live HiveMQ authorization / ACL test — PASS
A controlled diagnostic client was added to the production relay and executed once at service startup. It uses the actual role credentials and actual HazRob topic root, while avoiding motion commands. The final production diagnostic result after bootstrap-token alignment was:

```text
state=pass
connect.app=true
connect.robot=true
subscribe.app_ack=true
subscribe.app_telemetry=true
subscribe.app_status=true
subscribe.robot_cmd=true
roundtrip.app_publish_cmd_robot_receive=true
roundtrip.robot_publish_ack_app_receive=true
roundtrip.robot_publish_telemetry_app_receive=true
retained_status.tested=true
retained_status.restored=true
```

The robot-side diagnostic CONNECT includes the retained LWT/status behavior used by the real ESP32. The retained status test saves/restores the previously retained status (or clears the diagnostic retained value if none existed), so the test does not intentionally leave diagnostic broker state behind.

**Conclusion:** the tested HiveMQ topic ACLs, SUBSCRIBE permissions, core PUBLISH permissions and retained-status permission are currently valid. No manual HiveMQ ACL modification is required on this evidence.

### Bootstrap token production alignment
The Android and main-ESP32 0.10.1 clients use the controlled prototype bootstrap token `hazrob-prototype-bootstrap-v1`. During HR-0101-038, the production Render environment variable `BOOTSTRAP_TOKEN` was explicitly merged/set to that same authoritative value. No HiveMQ password, video token or unrelated environment variable was changed.

This removes bootstrap-token drift as a remaining configuration ambiguity for the current 0.10.1 clients. The production service redeployed after the environment update and returned to `live`.

### Production Render verification after token alignment — PASS
Production deploy `dep-daiod367bikc739fmn9g`:
- source: `b6f16120c7be56470869d4588babcc1c74145c25`;
- build: PASS;
- runtime identity: HazRob video relay 0.10.1;
- status: `live`;
- application HiveMQ credential diagnostic: PASS / CONNACK 0;
- robot HiveMQ credential diagnostic: PASS / CONNACK 0;
- complete ACL/topic round-trip/retained diagnostic: PASS.

### Render relay source-contract verification — PASS
Independent relay-contract service `hazrob-relay-contract-0101` was rebuilt after the HR-0101-038 relay change.
- source tested: `d1a95fdbba5a4de46d484bf8991a8cc45b81113e` (relay source is unchanged by the later Android-only commit);
- JavaScript syntax/contract check: PASS;
- `HazRob 0.10.1 relay contract: PASS`;
- dependency audit: 0 reported vulnerabilities.

### Android diagnostic hardening
`SecureMqtt311Client` now:
- preserves the MQTT CONNACK reason/code instead of returning only a generic rejection;
- waits for and validates SUBACK for every subscription;
- distinguishes SUBACK timeout, disconnect-before-SUBACK, ACL rejection (`0x80`) and unexpected granted QoS.

`RemoteSessionManager` now preserves those detailed messages through readiness/probe results instead of reducing the final result to only the Java exception class.

### Android exact-head build verification — PASS
Independent Android builder `hazrob-0101-build`, deploy `dep-daiobk95efls73eb7q60`, checked out exact source `b6f16120c7be56470869d4588babcc1c74145c25` and completed:
- cross-component integration contract: PASS;
- `:app:testDebugUnitTest`: PASS (`BUILD SUCCESSFUL`);
- `:app:assembleRelease`: PASS (`BUILD SUCCESSFUL`);
- release APK SHA-256: `3efbaff08180793ad0c79f0a7af58ce768ad1532ad1ecbf56c05b37ab1df438e`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`;
- version identity remains `0.10.1` / versionCode `21`.

### Main ESP32 diagnostic hardening
The main firmware now exposes staged cloud failure details through its remote status, including representative values:
- `wifi_not_configured`, `wifi_connecting`, `dns_wait`, `time_wait`;
- `bootstrap_start`, `bootstrap_refresh`, `bootstrap_tls_failed`, `bootstrap_http_failed`, `bootstrap_timeout`, `bootstrap_contract_mismatch`, `bootstrap_ok`;
- `broker_tls_connect`, `broker_tls_ok`, `broker_tls_failed`;
- `mqtt_connack_timeout`, `mqtt_connack_malformed`, `mqtt_connack_<code>`, `mqtt_connected`;
- `mqtt_suback_timeout`, `mqtt_suback_malformed`, `mqtt_suback_mismatch`, `mqtt_suback_rejected`, `mqtt_subscribed`;
- `mqtt_ready`, `mqtt_disconnected`.

The ESP32 now waits for the broker SUBACK to the command-topic subscription and treats an ACL rejection or missing/invalid SUBACK as a failed MQTT session instead of silently assuming subscription success.

### Firmware build verification — PASS
The first firmware-builder attempt `dep-daio8lh594qs739ifubg` did **not** reach compilation: the cached Arduino CLI environment already contained `~/.arduino15/arduino-cli.yaml`, causing `arduino-cli config init` to exit before compile. The cross-component audit had already passed. This is recorded as a builder/cache infrastructure false negative, not as a firmware compilation failure.

The build was repeated with Render Clear Build Cache as deploy `dep-daio9fvqj5pc73aucpg0` and completed `live`:
- source compiled: `d1a95fdbba5a4de46d484bf8991a8cc45b81113e`;
- cross-component integration contract: PASS;
- main ESP32 compile: PASS — 1,086,637 bytes program (34%), 50,164 bytes global RAM (15%);
- ESP32-CAM compile: PASS — 1,118,771 bytes program (35%), 67,836 bytes global RAM (20%).

Compared with the pre-HR-0101-038 permanent baseline, the main ESP32 diagnostic/SUBACK hardening adds 3,024 program bytes and 16 global-RAM bytes. ESP32-CAM is unchanged.

A Git comparison from `d1a95fdb...` to final executable head `b6f16120...` shows exactly one changed file, `RemoteSessionManager.java`; therefore the firmware source compiled in the successful build is byte-for-byte unaffected by the later Android-only commit.

### Cold-start timeout decision
The main ESP32 bootstrap currently uses a short bounded blocking window (approximately 4 seconds per attempt) and retries from the existing Remote MQTT retry loop. Android already has a much longer background bootstrap read window for Render free-tier cold start.

HR-0101-038 deliberately does **not** extend the ESP32 blocking bootstrap window to tens of seconds without field evidence, because doing so in the current single-loop architecture could delay other firmware processing. Instead the firmware now reports the exact bootstrap stage. A future timeout/non-blocking-bootstrap change shall be controlled separately if live device evidence identifies bootstrap cold start as the remaining failure.

### Root-cause classification after assistant-run tests
**Excluded by live test:**
- Render -> HiveMQ TLS failure;
- wrong current HiveMQ application password;
- wrong current HiveMQ robot password;
- MQTT CONNECT authorization failure;
- application SUBSCRIBE ACL for `ack`, `telemetry`, `status`;
- robot SUBSCRIBE ACL for `cmd`;
- application PUBLISH path to `cmd`;
- robot PUBLISH paths to `ack` and `telemetry`;
- robot retained-status/LWT permission;
- production bootstrap-token configuration drift after explicit alignment.

**Still requires powered-device E2E evidence:**
- main ESP32 association to the configured external Wi-Fi;
- DNS and NTP readiness on the physical ESP32;
- ESP32 -> Render HTTPS bootstrap on the physical network path;
- ESP32 -> HiveMQ TLS/MQTT session using the runtime-returned credential;
- real Android -> HiveMQ -> physical ESP32 `probe` ACK and telemetry round-trip;
- live latency/jitter/reconnect behavior.

The new status `detail` and Android error propagation make those remaining stages directly observable without another speculative password/ACL change.

### Operator action classification
At the close of these tests:
- **No HiveMQ password change is requested.**
- **No HiveMQ ACL change is requested.**
- **No manual Render secret change is requested; the bootstrap token was aligned programmatically.**
- The next mandatory evidence is the powered-device Remote E2E run using the HR-0101-038 firmware and Android build.

### Status
**HR-0101-038 SOURCE/BUILD/PRODUCTION-RENDER/HIVEMQ-AUTH/HIVEMQ-ACL: PASS.**

**PHYSICAL ESP32 WAN/BOOTSTRAP + ANDROID↔HIVEMQ↔ESP32 E2E: PENDING.**

HazRob 0.10.1 remains a controlled software baseline and is not promoted to `FINAL` by this change alone.
