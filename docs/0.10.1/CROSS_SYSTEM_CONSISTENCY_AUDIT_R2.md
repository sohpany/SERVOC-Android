# HazRob 0.10.1 — Cross-System Consistency Audit R2

Controlled change: **HR-0101-037**  
Tested executable baseline: `771e545caf39607564c6f2d049d86715fcce7fc1`  
Permanent marker: `baseline/hazrob-0.10.1-software`

## Executive conclusion
The tested HazRob 0.10.1 software baseline is mutually consistent across Android, main ESP32, ESP32-CAM, production Render and HiveMQ at source-contract/build/authentication level. Android, main ESP32 and ESP32-CAM were built from the same exact source baseline. Production Render is running that exact source and reports version 0.10.1. HiveMQ accepts both the application and robot identities over TLS with CONNACK 0.

No executable change was introduced by this R2 report. The remaining gates are live/physical E2E validation rather than known source-contract mismatches.

## End-to-end architecture

### Direct mode
`Android -> HazRob Wi-Fi -> main ESP32` for drive, stop, measurement, configuration and diagnostics.

`Android -> HazRob Wi-Fi -> ESP32-CAM` for local MJPEG/status/flash.

Canonical addresses:
- main ESP32: `192.168.4.1`;
- ESP32-CAM: `192.168.4.2`.

### Remote mode
`Android -> HiveMQ TLS:8883 -> main ESP32` for robot command/ACK/telemetry/status.

`Android/main ESP32/ESP32-CAM -> Render /hazrob/bootstrap` for role-scoped runtime credentials.

`ESP32-CAM -> Render WSS publisher -> Android WSS viewer` for remote JPEG video.

Render is not in the drive/control data path.

## Contract matrix

| Contract | Android | Main ESP32 | ESP32-CAM | Render | HiveMQ | R2 result |
|---|---|---|---|---|---|---|
| Release identity | 0.10.1 | 0.10.1 | 0.10.1 | 0.10.1 | transport | PASS |
| Protocol generation | 2 where bootstrap applies | 2 | 2 | 2 | MQTT 3.1.1 transport | PASS |
| Robot identity | `hazrob-001` | `hazrob-001` | `hazrob-001` | same room/bootstrap id | same topic root | PASS |
| Direct master address | `192.168.4.1` | owns address | gateway | n/a | n/a | PASS |
| Direct camera address | `192.168.4.2` | camera default | owns address | n/a | n/a | PASS |
| Drive range | -100..100 | accepts same | n/a | not in path | transports | PASS |
| Drive response policy | linear, 100% default, immediate + 20 Hz refresh | direct PWM | n/a | not in path | transports | PASS |
| Stale command boundary | refresh stream | 1500 ms bounded neutralization | n/a | n/a | n/a | PASS |
| E-STOP | explicit UI/API/reset | latched explicit reset | n/a | n/a | transports remote command | PASS |
| MQTT host | canonical HiveMQ | same | n/a | credentials/diagnostics | canonical broker | PASS |
| MQTT TLS port | 8883 | 8883 | n/a | diagnostic client 8883 | 8883 | PASS |
| Command topic | publishes robot command root | subscribes same root | n/a | n/a | routes | PASS |
| Replies | consumes ACK/telemetry/status | publishes same | n/a | n/a | routes | PASS |
| Freshness envelope | session/seq/nonce/TTL | validates envelope | n/a | n/a | transparent | PASS |
| Bootstrap endpoint | `/hazrob/bootstrap` | same | same | implements | n/a | PASS |
| Role credential | app MQTT + VIEW | robot MQTT | PUBLISH | role-scoped issuer | authenticates app/robot | PASS |
| Local CAM status | consumes `/status` | n/a | serves `/status` | n/a | n/a | PASS |
| Local CAM stream | consumes canonical stream | n/a | port 81 `/stream` | n/a | n/a | PASS |
| Local CAM flash | canonical `/flash?mode=` | remote command proxy when applicable | implements canonical route | n/a | remote command transport | PASS |
| Remote video | WSS VIEW | n/a | WSS PUBLISH | relays/status authority | n/a | PASS |
| Local-video priority | consumes local stream | n/a | remote capture pauses while local client active | preserves publisher path | n/a | PASS |

## Exact-baseline verification evidence

### Android
Source `771e545caf39607564c6f2d049d86715fcce7fc1`:
- cross-component integration contract PASS;
- debug unit tests PASS;
- release assembly PASS;
- APK SHA-256 `10627b69184841f0ed435d17f2b980f9659193c4078044bbfdc5edbd8b38eadf`;
- signer SHA-256 `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

### Main ESP32
Same source:
- integration contract PASS;
- compile PASS;
- program storage 1,083,613 bytes / 34%;
- global RAM 50,148 bytes / 15%.

### ESP32-CAM
Same source:
- compile PASS;
- program storage 1,118,771 bytes / 35%;
- global RAM 67,836 bytes / 20%;
- verified program reduction versus the prior 1,155,611-byte baseline: 36,840 bytes (~3.19%).

### Render relay contract
Same source:
- JavaScript syntax/contract check PASS;
- relay contract test PASS;
- dependency audit reported 0 vulnerabilities.

### Production Render
Service `hazrob-video-relay`:
- source branch `dev/hazrob-0.10.1`;
- deployed exact source `771e545caf39607564c6f2d049d86715fcce7fc1`;
- deploy status `live`;
- runtime identity `HazRob video relay 0.10.1`.

### HiveMQ
Canonical broker: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud:8883`.

Production startup diagnostics on the exact Render baseline:
- application identity: TLS true / MQTT true / CONNACK 0 / accepted;
- robot identity: TLS true / MQTT true / CONNACK 0 / accepted.

This is positive evidence for broker reachability, TLS trust and credential authentication. Topic-level ACL enforcement remains a separate live administrative/E2E gate.

## Permanent GPIO authority
HR-0101-037 changes no GPIO:
- left L298N: ENA GPIO16, IN1 GPIO18, IN2 GPIO27;
- right L298N: ENB GPIO17, IN3 GPIO19, IN4 GPIO32;
- MQ relay control: GPIO25;
- I2C: SDA21 / SCL22;
- camera AI-Thinker map unchanged.

## Known non-blocking build warnings
Current successful builds may still report compiler/deprecation warnings, including the certificate presence-check warning and volatile increment deprecation in camera counters. They do not currently fail the build and are not hidden by this audit. Any later cleanup must preserve the trusted CA bytes and receive its own controlled change record.

## Remaining field gates
1. Wheels-up Direct motion response, low-speed response, pivot and rapid forward/reverse behavior, including current/temperature observation.
2. Measured Direct joystick-event -> ESP32 receive -> PWM latency.
3. Powered Android -> HiveMQ -> main ESP32 command/ACK/telemetry E2E, including latency and jitter.
4. HiveMQ topic-level ACL verification.
5. Live ESP32-CAM -> production Render publisher -> Android viewer first-decoded-JPEG test.
6. Wi-Fi/MQTT/Render reconnect tests with no retrace or analytical side effects.
7. MQ relay GPIO25/electrical test after operator measurements; explicitly deferred and excluded from the present result.

## R2 classification
**PERMANENT 0.10.1 SOFTWARE BASELINE: PASS**  
**STATIC CROSS-SYSTEM CONSISTENCY: PASS**  
**EXACT-HEAD ANDROID/FIRMWARE BUILD: PASS**  
**PRODUCTION RENDER ALIGNMENT: PASS**  
**HIVEMQ TLS/AUTHENTICATION: PASS**  
**HIVEMQ TOPIC ACL + POWERED REMOTE E2E: PENDING**  
**PHYSICAL MOTION/CAMERA FIELD VALIDATION: PENDING**

HazRob 0.10.1 must not be promoted to `FINAL` until the pending mandatory field gates pass or are explicitly accepted through a subsequent controlled change.