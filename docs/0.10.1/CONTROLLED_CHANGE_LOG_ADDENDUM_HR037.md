# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-037

## HR-0101-037 — Permanent 0.10.1 Software Baseline Verification

### Purpose
Establish the motion-control, compact-camera, Android and Remote integration changes implemented through HR-0101-034/035/036 as the permanent software baseline for HazRob 0.10.1. This change is documentation/control-plane only and does not modify executable behavior, GPIO allocation, network coordinates, safety semantics or scientific-data content.

### Authoritative tested executable baseline
- Source commit: `771e545caf39607564c6f2d049d86715fcce7fc1`.
- Permanent marker branch: `baseline/hazrob-0.10.1-software`.
- Active integration branch at verification time: `dev/hazrob-0.10.1`.
- Commits after the tested source baseline that contain this HR-0101-037 documentation are documentation-only unless separately recorded under a later controlled change.

### Permanent 0.10.1 behavior retained
- Android operator drive scale defaults to 100%.
- Joystick shaping is linear after the controlled small dead-zone; the retired quadratic `z*=z` response is absent.
- Joystick changes are transmitted immediately with a 20 Hz refresh stream.
- Activity/tab lifecycle transitions do not by themselves issue an operator-motion STOP.
- Main ESP32 applies accepted left/right commands directly to L298N PWM; retired acceleration/deceleration/reversal ramp logic and automatic retrace/history/link-hold motion logic remain deleted from source.
- Main ESP32 retains one bounded stale-command neutralization boundary of 1500 ms and the independent E-STOP contract.
- ESP32-CAM uses the compact local HTTP implementation and canonical 0.10.1 endpoints only; retired local camera compatibility paths remain removed.
- Render remains outside the robot-drive data plane. Remote drive/control is Android <-> HiveMQ <-> main ESP32; Render provides role-scoped bootstrap plus remote-video relay/status.

### GPIO/hardware authority
No GPIO changed in HR-0101-037. The controlled drive mapping remains:
- Left: ENA GPIO16, IN1 GPIO18, IN2 GPIO27.
- Right: ENB GPIO17, IN3 GPIO19, IN4 GPIO32.
- ENA/ENB jumpers are removed when GPIO16/GPIO17 provide PWM.
- GPIO25 remains the MQ relay-control output; its electrical field verification is intentionally deferred and excluded from this software-baseline PASS.

### Exact-head Android verification
Independent Render builder `hazrob-0101-build` checked out exact source `771e545caf39607564c6f2d049d86715fcce7fc1` and completed:
- cross-component integration contract: PASS;
- `:app:testDebugUnitTest`: PASS;
- `:app:assembleRelease`: PASS;
- release APK SHA-256: `10627b69184841f0ed435d17f2b980f9659193c4078044bbfdc5edbd8b38eadf`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

### Exact-head firmware verification
Independent Render builder `hazrob-0101-firmware-build` checked out the same exact source and completed the integration contract plus both Arduino builds:
- main ESP32: PASS, 1,083,613 bytes program (34%), 50,148 bytes global RAM (15%);
- ESP32-CAM: PASS, 1,118,771 bytes program (35%), 67,836 bytes global RAM (20%).

The compact-camera implementation therefore retains the verified reduction from the previous accepted 1,155,611-byte camera binary: 36,840 bytes program storage, approximately 3.19%, without removing the required local or Remote video paths.

### Exact-head Render verification
The dedicated relay-contract builder checked out the same source `771e545caf39607564c6f2d049d86715fcce7fc1` and completed:
- `npm run check`: PASS;
- `npm test`: `HazRob 0.10.1 relay contract: PASS`;
- dependency audit: 0 reported npm vulnerabilities in the verification build.

Production `hazrob-video-relay` is bound to `dev/hazrob-0.10.1`, deployed exact source `771e545caf39607564c6f2d049d86715fcce7fc1`, and reached `live` state with runtime identity `hazrob-video-relay 0.10.1`.

### HiveMQ evidence
Production Render startup diagnostics on the exact baseline proved successful TLS/MQTT authentication to the canonical HiveMQ Cloud broker for both identities:
- application identity: TLS `true`, MQTT `true`, CONNACK `0`, accepted;
- robot identity: TLS `true`, MQTT `true`, CONNACK `0`, accepted.

This proves broker reachability, TLS trust and credential authentication. It does not by itself prove every topic ACL or the complete powered Android -> broker -> ESP32 command/ACK/telemetry round-trip.

### Consistency classification
- Source contract consistency: PASS.
- Android exact-head tests/build/signing: PASS.
- Main ESP32 exact-head compile: PASS.
- ESP32-CAM exact-head compile: PASS.
- Render relay contract: PASS.
- Production Render branch/source/runtime alignment: PASS.
- HiveMQ TLS/authentication for app and robot identities: PASS.
- HiveMQ topic ACL verification: PENDING.
- Powered Remote command/ACK/telemetry E2E: PENDING.
- Physical Direct motion/rapid-reversal/latency tests: PENDING.
- Live ESP32-CAM local/Remote first-decoded-frame field test: PENDING.
- MQ relay electrical field test: DEFERRED by operator pending GPIO25 measurements.

### Release status
**HazRob 0.10.1 PERMANENT SOFTWARE BASELINE: PASS.**

This does not declare the whole release `FINAL`. Final-release promotion remains gated by the physical/live tests listed above or an explicit controlled acceptance of any remaining limitation.