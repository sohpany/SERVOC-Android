# HazRob 0.10.1 — Cross-System Consistency Audit

Controlled change: **HR-0101-036**  
Verified source baseline: `f7a9fe6a33ef3b0048613ec033be384f05ef580f`  
Documentation continuation: commits after that baseline are documentation-only unless separately recorded.

## Executive result

HazRob 0.10.1 is source-consistent across Android, main ESP32, ESP32-CAM, Render and HiveMQ for the controlled Direct and Remote contracts. Independent Android and firmware builds pass, the Render relay contract passes, the production Render service is aligned to `dev/hazrob-0.10.1`, and production diagnostics prove successful HiveMQ TLS/MQTT authentication for both application and robot identities.

Remaining validation is physical/live E2E: powered robot command/ACK/telemetry through HiveMQ, live camera publisher/viewer through production Render, Direct/Remote latency measurements, and the separately deferred MQ relay electrical test.

## System matrix

| Contract | Android | Main ESP32 | ESP32-CAM | Render | HiveMQ | Result |
|---|---|---|---|---|---|---|
| Release/service generation | 0.10.1 | 0.10.1 | 0.10.1 | 0.10.1 / protocol 2 | transport only | PASS |
| Robot ID | `hazrob-001` | `hazrob-001` | `hazrob-001` | `hazrob-001` | topic root uses same ID | PASS |
| Direct master IP | `192.168.4.1` | AP/master `192.168.4.1` | gateway `192.168.4.1` | n/a | n/a | PASS |
| Direct camera IP | `192.168.4.2` | configured CAM host | static `192.168.4.2` | n/a | n/a | PASS |
| Direct drive API | emits `/drive?l=&r=` | handles `/drive` | n/a | n/a | n/a | PASS |
| Direct stop/E-stop | emits controlled endpoints | handles controlled endpoints | n/a | n/a | n/a | PASS |
| Drive output | -100..100 operator command | direct L298N PWM | n/a | not in data plane | not in local data plane | PASS |
| Stale command boundary | refresh at 20 Hz | 1500 ms stream-loss neutralization | n/a | n/a | n/a | PASS |
| MQTT host/port | canonical HiveMQ :8883 | same host :8883 | n/a for control | supplies role creds | TLS broker :8883 | PASS |
| MQTT command root | publishes `hazrob/<id>/cmd` | subscribes same | n/a | bootstrap only | routes topics | PASS |
| MQTT replies | subscribes ack/telemetry/status | publishes ack/telemetry/status | n/a | bootstrap only | routes topics | PASS |
| MQTT freshness | session/seq/nonce/TTL | validates same envelope | n/a | n/a | transport | PASS |
| Bootstrap host/path | Render `/hazrob/bootstrap` | same | same | implements endpoint | n/a | PASS |
| Bootstrap service contract | requires 0.10.1 / 2 | requires 0.10.1 / 2 | requires 0.10.1 / 2 | returns 0.10.1 / 2 | n/a | PASS |
| Credential role | app MQTT + VIEW token | robot MQTT | PUBLISH token | role-scoped issuer | app/robot auth | PASS |
| Local camera status | `/status` | optional camera diagnostics/proxy actions | implements `/status` | n/a | n/a | PASS |
| Local camera video | port81 `/stream` | n/a | serves MJPEG | n/a | n/a | PASS |
| Camera flash | `/flash?mode=` or remote command | remote forwards canonical `/flash?mode=` | implements canonical route | n/a | remote command transport | PASS |
| Remote video publish | receives viewer URL/token | not video data plane | WSS binary JPEG publisher | authenticated publisher relay | n/a | PASS |
| Remote video view | WSS viewer | n/a | publisher | authenticated viewer relay/status | n/a | PASS |
| Camera priority | local viewer uses canonical stream | n/a | pauses remote capture while local active | accepts remote when available | n/a | PASS |
| Render in drive path | no | no | no | no; bootstrap/video only | drive transport is broker | PASS |

## L298N authority

The motion/footprint changes do not alter physical pin authority:

- Left: ENA GPIO16, IN1 GPIO18, IN2 GPIO27.
- Right: ENB GPIO17, IN3 GPIO19, IN4 GPIO32.
- ENA/ENB jumpers removed when ESP32 PWM drives GPIO16/17.
- GPIO25 remains the separate MQ relay-control output; its physical relay test is deferred.

## Camera compacting result

ESP32-CAM program storage changed from 1,155,611 bytes to 1,118,771 bytes, a reduction of 36,840 bytes (~3.19%). Global RAM changed from 68,220 to 67,836 bytes.

The reduction came from removing Arduino `WebServer` and retired compatibility HTTP paths, not from deleting comments/whitespace. Required 0.10.1 features remain: native-JPEG-first camera initialization, RGB565/software-JPEG fallback, local MJPEG, flash, status, Render bootstrap, TLS-gated WSS publishing, PSRAM buffer and local-stream priority.

The main ESP32 remains 1,083,613 bytes program / 50,148 bytes global RAM. It already uses a compact raw local HTTP server and a custom MQTT/TLS path. Removing Wi-Fi/TLS/MQTT/analytical functionality merely to reduce the binary is not authorized as a footprint optimization.

## Exact-source verification

### Android
- Source: `f7a9fe6a33ef3b0048613ec033be384f05ef580f`.
- `:app:testDebugUnitTest`: PASS.
- `:app:assembleRelease`: PASS.
- APK signer SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- APK SHA-256: `caef147fbad899d48a4337e05594b920230a94721df16113cf7b76a24fa34d71`.

### Firmware
- Source: same `f7a9fe6a33ef3b0048613ec033be384f05ef580f`.
- cross-component integration audit: PASS.
- Main ESP32: compile PASS, 1,083,613 bytes program / 50,148 bytes RAM.
- ESP32-CAM: compile PASS, 1,118,771 bytes program / 67,836 bytes RAM.

### Render relay
- Source: same `f7a9fe6a33ef3b0048613ec033be384f05ef580f`.
- `npm run check`: PASS.
- `npm test`: `HazRob 0.10.1 relay contract: PASS`.
- dependency audit: zero vulnerabilities during the verification build.

## Production Render/HiveMQ evidence

Production `hazrob-video-relay` is now configured on branch `dev/hazrob-0.10.1`, superseding the earlier blocker that recorded `dev/hazrob-0.9`.

The production runtime reports `HazRob video relay 0.10.1`. Production startup diagnostics have shown:

- application credential: TLS true, MQTT true, CONNACK 0 / accepted;
- robot credential: TLS true, MQTT true, CONNACK 0 / accepted.

No `remote/video-relay` source file changed between production source commit `144dc0aeb17470cfe3298ec578423888e6318b52` and verification baseline `f7a9fe6a33ef3b0048613ec033be384f05ef580f`. The production relay therefore remains source-compatible with the newer Android/firmware-only commits.

## Remaining gates

1. Wheels-up Direct drive: 0->command, command->0, forward<->reverse, pivot and low-speed response.
2. Direct latency measurement from joystick event to main-ESP32 receipt/PWM write.
3. Remote live Android -> HiveMQ -> main ESP32 command/ACK/telemetry round-trip and latency/jitter measurement.
4. Live ESP32-CAM -> production Render publisher and Android viewer first-decoded-JPEG test.
5. Reconnect tests for Wi-Fi, HiveMQ and Render without retrace or analytical-side-effect regression.
6. HiveMQ administrative topic ACL verification where console/API access is available.
7. MQ relay electrical verification after GPIO25 measurements; intentionally excluded from this audit's physical PASS state.

## Final classification

**SOURCE CONSISTENCY: PASS**  
**BUILD CONSISTENCY: PASS**  
**RENDER CONTRACT: PASS**  
**PRODUCTION RENDER BRANCH/VERSION: PASS**  
**HIVEMQ TLS/AUTHENTICATION: PASS**  
**HIVEMQ TOPIC ACL + PHYSICAL REMOTE E2E: PENDING**  
**PHYSICAL MOTION/CAMERA FIELD VALIDATION: PENDING**
