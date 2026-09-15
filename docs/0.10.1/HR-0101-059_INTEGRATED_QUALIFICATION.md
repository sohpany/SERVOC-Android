# HazRob 0.10.1 — HR-0101-059 Integrated Qualification Record

Date: 2026-09-16 (Asia/Aden)  
Branch: `dev/hazrob-0.10.1`  
Release state: **INTEGRATION QUALIFIED / PHYSICAL FIELD ACCEPTANCE PENDING**

## 1. Purpose

This record qualifies HazRob 0.10.1 as one coordinated system rather than as independent modules. The scope covers:

- main ESP32 firmware;
- ESP32-CAM firmware;
- Android application;
- Render relay/bootstrap service;
- HiveMQ MQTT broker contract;
- Direct/local and Remote/internet control paths;
- camera local MJPEG and Remote WSS paths;
- safety timing, role isolation, version/protocol identity, and WAN lifecycle.

This record deliberately does **not** claim physical acceptance. RF coexistence, real motors, actual camera hardware, real sensor I/O and end-to-end operation on the assembled robot remain field tests.

## 2. Qualification findings and corrective actions

The qualification review found three release-relevant gaps that were corrected before this record was created:

1. **ESP32-CAM TLS clock readiness** — the current camera source could reach the Remote TLS path without first proving a certificate-validating wall clock. HR059 restores a CAM-local NTP gate and exposes `time_ready`/epoch diagnostics. Remote WSS admission now requires both main-gateway readiness and CAM time readiness.
2. **ESP32-CAM bootstrap compatibility** — the camera validated its role, robot identity and token but did not fail closed on bootstrap `version`/`protocol`. HR059 requires exact `0.10.1 / protocol 2 / esp32-cam / hazrob-001` compatibility before accepting the publisher token.
3. **Android service teardown** — `RobotLinkService.onDestroy()` relied on the firmware watchdog as the final protection for an interrupted Remote drive session. HR059 adds an explicit safe-stop and closes the Remote session/drive pump before service teardown.

These corrections do not alter the canonical GPIO map, L298N direction mapping, R0 authority, ADS/ENV I2C allocation, or the separation between Direct and Remote control authorities.

## 3. Canonical cross-component identity

| Contract | Main ESP32 | ESP32-CAM | Android | Render/Hive |
|---|---|---|---|---|
| Release | HazRob 0.10.1 | HazRob-CAM 0.10.1 | versionName 0.10.1 / versionCode 21 | relay 0.10.1 |
| Protocol | 2 bootstrap contract | 2 bootstrap contract | 2 | 2 |
| Robot ID | `hazrob-001` | `hazrob-001` | `hazrob-001` | `hazrob-001` |
| Local robot IP | 192.168.4.1 | gateway 192.168.4.1 | 192.168.4.1 | n/a |
| Local camera IP | n/a | 192.168.4.2 | 192.168.4.2 | n/a |
| HiveMQ | canonical host :8883 TLS | n/a | same canonical host :8883 TLS | same canonical host :8883 TLS diagnostics |
| Render | bootstrap client `esp32` | bootstrap client `esp32-cam` | bootstrap client `android` | `hazrob-video-relay.onrender.com` |

Canonical HiveMQ host: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud` port 8883.  
Canonical Render host: `hazrob-video-relay.onrender.com`.

## 4. Radio and transport state contract

### Direct

- main ESP32 radio: `WIFI_AP` only;
- SSID: `HazRob`;
- robot: `192.168.4.1`;
- camera: `192.168.4.2`;
- robot control: HTTP only;
- camera: local MJPEG, port 81;
- Android: `WifiNetworkSpecifier` local-only ownership of HazRob network;
- MQTT: disabled/disconnected;
- STA/WAN: disabled;
- NAPT: disabled;
- Wi-Fi scanning during normal Direct operation: disabled;
- local camera capture receives priority over Remote video capture.

### Remote

- main ESP32 radio: `WIFI_AP_STA`;
- WAN: saved credentials, bounded attempts, no uncontrolled auto-reconnect loop;
- control: HiveMQ MQTT;
- NAPT: enabled only after Internet-readiness gate;
- ESP32-CAM WSS: admitted only when main reports `radio_profile=remote`, `internet=true`, `napt=true` **and** CAM has a valid NTP clock;
- Android releases its forced local HazRob route before cloud bootstrap/MQTT;
- Direct `/drive` authority is rejected outside Direct mode.

### WAN lifecycle

- **Remote disconnect**: immediate motion stop, AP-only runtime profile, MQTT/NAPT/STA disabled, WAN credentials retained.
- **Remote reconnect**: immediate neutral state, saved WAN reused without reprovisioning.
- **WAN clear**: the only destructive operation; clears stored WAN credentials and makes subsequent boot Direct-only until reprovisioned.

## 5. Control-function parity

The HR059 gate compares the Android command family with main-firmware MQTT handlers and fails closed if either side drifts. Verified Remote command family:

`probe`, `drive`, `motion_stop`, `estop`, `estop_reset`, `sensor_off`, `heartbeat`, `heartbeat_disarm`, `relay`, `measure_start`, `measure_cancel`, `alert`, `alert_test`, `camera_flash`, `config_get`, `config_set`.

Direct operator functions are independently matched to main HTTP endpoints for drive, stop, E-stop/reset, heartbeat, diagnostics, data, configuration, relay, measurement and alert control.

Camera video is independent of control admission: failure or absence of the video path must not make MQTT control unavailable.

## 6. Safety timing qualification

Verified source invariants:

- firmware command watchdog: **500 ms**;
- Direct unchanged-motion refresh: **250 ms**;
- Remote drive pump period: **40 ms**;
- Remote minimum publish interval: **40 ms**;
- Remote hold refresh: **120 ms**;
- Remote command TTL: **900 ms**.

The refresh intervals remain below the 500 ms firmware watchdog. Profile changes issue motion stop first. Android backgrounding requires a neutral re-arm before motion can resume. HR059 additionally performs an explicit fail-stop and closes the Remote drive pump on service teardown.

## 7. Camera-path qualification

### Local path

- AI Thinker pin authority unchanged;
- static IP 192.168.4.2;
- QVGA 320×240;
- RGB565 capture;
- software JPEG `frame2jpg_cb`;
- JPEG quality 35;
- single PSRAM framebuffer;
- `CAMERA_GRAB_WHEN_EMPTY`;
- local MJPEG server port 81;
- local viewer arbitrates capture ahead of Remote WSS.

### Remote path

- canonical Render TLS host and CA validation;
- strict bootstrap role `esp32-cam`;
- exact service version/protocol/robot validation;
- publisher token is role-scoped and is not the Android viewer token;
- RFC6455 WebSocket acceptance validation;
- adaptive frame pacing 55–120 ms;
- no application frame queue/backlog;
- gateway readiness + CAM NTP readiness required before TLS/WSS startup.

## 8. Render/HiveMQ live qualification evidence

Fresh production diagnostics were run during HR059 qualification. Evidence showed:

- Android/application MQTT account: TLS/MQTT accepted, CONNACK 0;
- robot MQTT account: TLS/MQTT accepted, CONNACK 0;
- application subscriptions to ACK/telemetry/status accepted;
- robot subscription to command topic accepted;
- application command reached the robot-side subscription;
- robot ACK reached application-side subscription;
- robot telemetry reached application-side subscription;
- retained status write/read/restore contract passed.

The extended Relay contract also performed a live production Bootstrap verification for all three roles without printing secret values:

- `android`: exact version/protocol/robot/client; MQTT credentials + video VIEW token; no PUBLISH token;
- `esp32`: exact version/protocol/robot/client; robot MQTT credentials; no video VIEW/PUBLISH token;
- `esp32-cam`: exact version/protocol/robot/client; video PUBLISH token; no MQTT password or VIEW token.

Local Relay tests additionally verified unauthorized rejection, cross-role token rejection, publisher/viewer WSS admission and binary JPEG forwarding.

Representative successful Relay contract deployment: `dep-daksqnbl550s73aoh74g`, source `88ae294a1dbfc02e9fee4bca306e4f18a1aaef77`.

## 9. Automated qualification gates

`tools/audit_hr059_system_coherence.py` is the fail-closed five-layer gate. It checks:

- release/version/protocol/robot identity;
- canonical HiveMQ/Render coordinates;
- scoped Bootstrap roles and compatibility rejection;
- Direct/Remote radio ownership;
- non-destructive disconnect/reconnect and destructive WAN clear semantics;
- Android↔ESP32 Remote command parity;
- Direct HTTP endpoint parity;
- safety timing inequalities;
- service teardown safe-stop;
- CAM local/Remote arbitration;
- CAM NTP/TLS gate;
- authenticated TLS/hostname verification;
- Render role isolation and live production health coverage.

The general integration gate invokes HR059, so a release/build cannot pass merely because one module compiles.

At source `dbf5f496bd4e9d2b102ee9cec7ecdab71e98967d` the consolidated gate reported:

- HR059 five-layer coherence PASS;
- HR059 connection contract PASS;
- HR048 steering PASS;
- HR049 control/Remote PASS;
- HR050 field-operability PASS;
- HR051 camera/ENV/Render/Hive/app coherence PASS;
- HR047 gas-safety PASS;
- R0 application↔firmware authority synchronized.

Representative release-gate deployment: `dep-daksp561egvs73bp58eg`, LIVE.

## 10. Build qualification

### Android

At source `dbf5f496bd4e9d2b102ee9cec7ecdab71e98967d`:

- HR059 integration gate PASS;
- `:app:testDebugUnitTest` PASS;
- `:app:assembleRelease` PASS;
- release signing verification PASS;
- signer SHA-256 continuity PASS.

Representative Android build deployment: `dep-dakspe7f3r2c738i00ig`, LIVE.

No APK is authorized for user delivery solely by this build record; field acceptance remains separate.

### Main ESP32

At the same functional source head:

- compile PASS using `esp32:esp32:esp32:PartitionScheme=huge_app`;
- program storage: 1,121,649 bytes (35%);
- global RAM: 50,692 bytes (15%); 276,988 bytes remain.

### ESP32-CAM

At the same functional source head:

- compile PASS using `esp32:esp32:esp32cam`;
- program storage: 1,157,867 bytes (36%);
- global RAM: 68,244 bytes (20%); 259,436 bytes remain.

Representative firmware build deployment: `dep-dakspg67bikc738ikblg`, LIVE.

## 11. Compiler/build observations

The build is successful. The following non-blocking technical-debt warnings remain documented rather than being changed immediately before field validation:

- compiler optimization warns that the static root-CA marker check resolves to a non-null pointer expression;
- two unused main-firmware I²C helper functions (`wr8`, `rd8`);
- ESP32-CAM diagnostic counters use `volatile` increments that the active C++ toolchain marks deprecated;
- Android reports existing deprecated/unchecked API notices and Gradle deprecation warnings for future Gradle 10 compatibility.

None of these warnings changed code generation into an error or invalidated the contracts above. They should be cleaned in a controlled maintenance change, not mixed into the field-acceptance baseline without need.

## 12. Scientific/sensor authority not changed by HR059

- GPIO authority is unchanged;
- ENV remains GPIO13/14 on separate `TwoWire(1)`;
- ADS remains GPIO21/22 at addresses 0x48/0x49;
- R0 campaign remains `FIELD-R0-3SESSION-20260913`;
- clean-air R0 is not reference-gas quantitative validation;
- quantitative ppm reporting remains fail-closed until its applicable M4/reference-gas authority is satisfied.

## 13. Required physical field acceptance — still PENDING

The software/cloud qualification is necessary but not sufficient for release. The assembled robot must still pass the following real-hardware matrix:

1. **Direct simultaneous control + video**: continuous local MJPEG while driving forward/reverse/turn/pivot, stop and E-stop; verify no RF dropouts or delayed commands.
2. **Direct lifecycle safety**: background the app while moving, verify immediate stop, foreground again and verify neutral re-arm.
3. **Remote simultaneous control + video**: WSS camera stream while MQTT drive, stop/E-stop, relay, measurement, alert, configuration and telemetry/ACK operate concurrently.
4. **Cold boot Remote camera**: prove CAM transitions `time_ready=false → true`, then Bootstrap/TLS/WSS succeeds without insecure time bypass.
5. **WAN loss/recovery**: remove WAN during Remote control, verify safe motion timeout and transparent diagnostics; recover or switch to Direct without stale authority.
6. **WAN disconnect/reconnect semantics**: prove non-destructive disconnect retains credentials and reconnect reuses them; prove WAN clear alone deletes them.
7. **RF/channel coexistence**: exercise typical router channels 1/6/11 and confirm AP/STA channel behavior does not destabilize phone or CAM during Remote operation.
8. **Hardware outputs**: verify L298N left/right mapping, PWM, E-stop latch/reset, MQ relay GPIO25, alert GPIO26 and sensor telemetry on actual wiring.
9. **Camera endurance**: run Direct and Remote video separately and with control for an extended session; inspect frame interval, disconnect/retry counters and memory stability.

Only after this matrix passes should the release be described as physically accepted and user-distribution artifacts be authorized.

## 14. Qualification conclusion

**Software/source coherence: PASS.**  
**Android build/unit-test/signing coherence: PASS.**  
**Main ESP32 compile/integration coherence: PASS.**  
**ESP32-CAM compile/integration coherence: PASS.**  
**Render/HiveMQ live contract and scoped Bootstrap coherence: PASS.**  
**Physical assembled-robot acceptance: PENDING.**

Accordingly, HR-0101-059 is suitable as the controlled candidate for field acceptance, but it is not yet a field-release authorization and no APK/ZIP should be distributed merely on the basis of this record.
