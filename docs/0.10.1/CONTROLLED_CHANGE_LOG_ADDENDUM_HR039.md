# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-039

## HR-0101-039 — Relay boot-safe initialization and post-HR038 full-system consistency verification

Date: 2026-09-12

### Scope
This controlled change permanently combines the previously verified HR-0101-038 Remote diagnostic/authentication hardening with an earliest-possible software OFF assertion for the main ESP32 MQ relay output.

The release line remains **HazRob 0.10.1**. The user's isolated reference to 0.10.0 during this work was not used to roll these fixes back to an older release branch.

### Authoritative executable source verified
- branch: `dev/hazrob-0.10.1`
- executable source SHA: `0470798a865bd63a460cfea013b8716aa140ed1e`
- relay boot-safe helper finalized in `0b4674e45552c04b2af22436254fc3a8590feb4f`
- earliest setup invocation added in `0470798a865bd63a460cfea013b8716aa140ed1e`

Documentation commits after the executable SHA do not change the verified application or firmware executable source.

## Relay electrical/software contract
Canonical main-ESP32 relay contract remains:
- GPIO: `25`
- active state: `LOW`
- inactive/OFF state: `HIGH`
- normal relay command semantics are unchanged: mask `0` = OFF, mask `1` = ON.

The new `relayBootSafeInit()` performs, in this order:
1. preload `GPIO25` output latch to the inactive/HIGH state;
2. change `GPIO25` direction to OUTPUT;
3. assert the inactive/HIGH state again.

`setup()` calls `relayBootSafeInit()` before `hwBegin()`, Wi-Fi/AP initialization, Remote initialization, MQTT initialization or the local HTTP server. `hwBegin()` subsequently calls the existing `relayRaw(false)` path, synchronizing the software MQ-power state with the already inactive physical output.

This reduces the software-controlled startup transition window and prevents a deliberate LOW pulse from being introduced when GPIO25 becomes an output.

### Hardware level-shifter assumption
The approved intended interface is a **dedicated non-inverting 3.3 V / 5 V bidirectional logic-level-shifter channel**, used only as a relay logic translator:

`ESP32 GPIO25 <-> LV channel | level shifter | HV channel <-> Relay IN`

with:
- LV reference = 3.3 V;
- HV reference = 5 V;
- common ground;
- relay module powered according to its rated supply;
- level-shifter pull-ups holding the logic line inactive/high while GPIO25 is not yet under application control.

This relay connection is **not** the HazRob I2C bus. Relay IN shall not be connected to SDA GPIO21, SCL GPIO22, ADS1115, or any other I2C device. The phrase “I2C” in this relay context refers only to the common BSS138-style level-shifter module normally sold for I2C logic translation.

### Reset/ROM limitation
Application firmware cannot control GPIO25 before the ESP32 reaches `setup()`. Therefore the pre-setup reset/ROM interval is an electrical responsibility of the level-shifter/pull-up interface. HR-0101-039 controls the earliest application-visible point; it does not claim to supersede correct hardware biasing.

## Remote connectivity hardening retained from HR-0101-038
No HR-0101-039 change reverses or bypasses HR-0101-038. The current source retains:
- Android verified TLS MQTT 3.1.1 connection;
- detailed CONNACK code/reason propagation;
- Android SUBACK validation for all required subscriptions;
- main ESP32 staged Wi-Fi/DNS/NTP/bootstrap/TLS/CONNACK/SUBACK diagnostics;
- main ESP32 command-topic SUBACK validation;
- role-scoped Render bootstrap;
- canonical broker `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud:8883`;
- robot identity `hazrob-001`;
- topic root `hazrob/hazrob-001/`;
- bootstrap protocol 2 and service version 0.10.1.

Production `BOOTSTRAP_TOKEN` was previously explicitly aligned to the 0.10.1 client value `hazrob-prototype-bootstrap-v1` without changing HiveMQ passwords or video tokens.

## HiveMQ/Render authorization state
The operator-visible HiveMQ console was cross-checked and shows:
- `hazrob_robot` -> `hazrob-001-all`;
- `hazrob_app` -> `hazrob-001-all`;
- permission: Publish and Subscribe;
- topic filter: `hazrob/hazrob-001/#`.

The stale permission description mentioning “HazRob 0.9” is metadata only and is not part of topic matching or authorization.

The latest controlled production diagnostic before HR-0101-039 proved:
- application TLS/MQTT CONNECT: PASS, CONNACK 0;
- robot TLS/MQTT CONNECT: PASS, CONNACK 0;
- app subscriptions to `ack`, `telemetry`, `status`: PASS;
- robot subscription to `cmd`: PASS;
- app publish `cmd` -> robot diagnostic receive: PASS;
- robot publish `ack` -> app diagnostic receive: PASS;
- robot publish `telemetry` -> app diagnostic receive: PASS;
- retained `status` path: PASS and restored after test.

Production Render service `hazrob-video-relay` remains `live`; HR-0101-039 does not modify relay-server code, broker credentials, topics, bootstrap roles or video authorization.

## Cross-component consistency review
The 0.10.1 cross-component integration audit was executed on exact executable SHA `0470798a865bd63a460cfea013b8716aa140ed1e` by both independent Android and firmware builders and returned:

`HazRob 0.10.1 cross-component integration contract: PASS`

The review confirms no conflicting change in:
- firmware/app release identity;
- main ESP32 GPIO authority;
- L298N direction/PWM assignments;
- actual I2C SDA/SCL allocation;
- Direct HTTP endpoints and command semantics;
- Android Remote command names versus main-ESP32 Remote handlers;
- E-STOP contract;
- MQTT broker/port/robot-id/topic root;
- Render bootstrap roles;
- ESP32-CAM canonical local and Remote video paths;
- scientific MQ/ADS acquisition catalogue and configuration;
- camera GPIO mapping.

Relay boot-safe code is isolated from the drive, I2C, camera and cloud protocol layers. Relay polarity remains active-low and all pre-existing Direct/Remote relay command semantics are unchanged.

## Exact-head Android verification — PASS
Builder: `hazrob-0101-build`
Deploy: `dep-daipa3h5efls73eectgg`
Source: `0470798a865bd63a460cfea013b8716aa140ed1e`

Results:
- cross-component integration audit: PASS;
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL;
- `:app:assembleRelease`: BUILD SUCCESSFUL;
- versionName: `0.10.1`;
- versionCode: `21`;
- APK SHA-256: `e751b7fdd9f3218630715f380c271b63287a1d7a50601edee8017fc95dda136a`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`;
- deploy status: `live`.

## Exact-head firmware verification — PASS
Builder: `hazrob-0101-firmware-build`
Deploy: `dep-daipa0ojo6nc73fqu3ig`
Source: `0470798a865bd63a460cfea013b8716aa140ed1e`
Build cache was explicitly cleared before verification.

Results:
- cross-component integration audit: PASS;
- main ESP32 compile: PASS;
- main ESP32 program: **1,086,673 bytes (34%)**;
- main ESP32 global RAM: **50,164 bytes (15%)**, 277,516 bytes remaining;
- ESP32-CAM compile: PASS;
- ESP32-CAM program: **1,118,771 bytes (35%)**;
- ESP32-CAM global RAM: **67,836 bytes (20%)**, 259,844 bytes remaining;
- deploy status: `live`.

Relative to HR-0101-038, the relay boot-safe addition adds only **36 bytes** of main-ESP32 program storage and **0 bytes** of global RAM. ESP32-CAM binary footprint is unchanged.

## Known non-blocking build warnings
The build still reports previously known warnings, not introduced by the relay change:
- `RemoteCloudConfig.h` / `RemoteVideoConfig.h`: compiler warning around the constant certificate-string `strstr(...,"BEGIN CERTIFICATE")` check;
- ESP32-CAM: deprecated operations on `volatile` diagnostic counters.

These warnings did not prevent compilation and are not evidence of a protocol/GPIO conflict. Certificate source shall not be edited casually because a prior cosmetic-warning cleanup attempt altered certificate text and was reverted.

## Physical verification still required
Software/source/build/cloud consistency is proven, but the following are intentionally not claimed without the powered hardware:
- with the final level-shifter installed, power-cycle/reset must confirm no unintended physical relay energization/click;
- relay LV side should be approximately 3.3 V when OFF and approximately 0 V when ON;
- HV/Relay-IN side should be approximately 5 V when OFF and approximately 0 V when ON, subject to the actual module pull-up/interface topology;
- Direct MQ OFF/ON must physically toggle the relay and MQ power path;
- physical ESP32 WAN association, DNS and NTP readiness;
- physical ESP32 -> Render bootstrap;
- physical ESP32 -> HiveMQ TLS/MQTT/SUBACK;
- real Android -> HiveMQ -> physical ESP32 probe ACK/telemetry and latency/reconnect behavior.

## Release classification
**HR-0101-039 SOURCE CONSISTENCY: PASS**

**ANDROID BUILD/TEST/SIGNING: PASS**

**MAIN ESP32 BUILD: PASS**

**ESP32-CAM BUILD: PASS**

**RENDER/HIVEMQ AUTHENTICATION + ACL: PASS (retained from live HR-0101-038 tests; contracts unchanged by HR-0101-039)**

**RELAY BOOT-SAFE SOFTWARE CONTRACT: IMPLEMENTED + COMPILED**

**PHYSICAL RELAY POWER-CYCLE TEST: PENDING**

**PHYSICAL REMOTE E2E TEST: PENDING**

HazRob 0.10.1 is now prepared as the controlled software baseline for the next requested distribution package. It is not labelled hardware-final until the two pending physical test groups pass.