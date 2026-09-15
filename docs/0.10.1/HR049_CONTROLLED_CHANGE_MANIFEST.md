# HazRob 0.10.1 — HR-0101-049 Controlled Change Manifest

**Change:** Smooth / low-latency drive and Remote control integration  
**Branch:** `dev/hazrob-0.10.1` only — no new development branch  
**Status:** **SOFTWARE / BUILD PASS — DIRECT AND REMOTE PHYSICAL FIELD ACCEPTANCE PENDING**  
**Artifact source commit:** `42dc0568f8afb3fa96175b149007ac079b98f643`

> This document update is documentation-only and was committed after the validated binaries. The authoritative application and firmware artifacts below were all built from the artifact source commit above.

## 1. Trigger

Field use after the HR048 Android build reported very slow and defective drive response while the robot was still running the older ESP32 firmware. The operator requirement was then made explicit: HazRob will commonly be driven at 100% speed and must retain fast, strong differential maneuver authority with minimal command latency in both Direct and Remote modes.

HR049 treats latency as a control-system property, not as a UI cosmetic issue.

## 2. Design constraints

- Preserve canonical differential mixing: `L = Y + X`, `R = Y - X`, normalized to ±100.
- Preserve HR048 physical mapping: logical LEFT -> L298N B; logical RIGHT -> L298N A.
- Do not add acceleration/ramp shaping that delays operator commands.
- Do not weaken latched E-STOP.
- Do not allow queued historical drive commands to execute after a newer command exists.
- Stop/release has higher priority than normal rate limiting.
- A lost command stream must neutralize drive automatically.
- Direct control must not depend on WAN, Render, HiveMQ or DNS.
- Remote control remains TLS + MQTT and must not use an insecure fallback.

## 3. Direct control architecture

### 3.1 Fast lane

Continuous local drive uses a capability-gated UDP datagram:

- port: `4210`
- protocol version: `1`
- packet size: `8 bytes`
- fields: magic `HR`, version, uint16 sequence, signed int8 left, signed int8 right, XOR checksum
- accepted drive range: `-100..100`
- firmware rejects malformed, bad-checksum and stale/out-of-order packets
- source IP/UDP port change permits a new Android sender sequence

Android enables UDP only after `/diag` advertises the matching `drive_udp` capability. HTTP `/drive` remains a compatibility fallback for pre-HR049 firmware.

Joystick release sends a redundant zero burst. Repeated idle zeros are then suppressed so browser diagnostics are not continuously overwritten.

### 3.2 Firmware servicing

`driveUdpUpdate()` is serviced repeatedly in the main loop. Wi-Fi scanning continues to call both `driveUdpUpdate()` and `hwUpdate()` while waiting. Local HTTP read timeout is bounded to 80 ms and POST body wait to 150 ms so a slow diagnostic/configuration request cannot monopolize the control loop for the former 250/300 ms windows.

The motor watchdog remains `CONTROL_COMMAND_TIMEOUT_MS = 500 ms`. It is a loss-of-stream safety neutralizer, not a speed ramp.

## 4. Remote control architecture

### 4.1 Android latest-command pump

`RemoteConnection.drive()` no longer performs a TLS/MQTT write on every UI call. It updates only the newest desired L/R state and increments a revision.

A dedicated single-thread drive pump evaluates that newest state:

- pump wake-up: `20 ms`
- maximum drive publish rate: one packet per `40 ms` = 25 Hz
- unchanged moving command refresh: `120 ms`
- drive command TTL: `900 ms`
- a changed zero/stop transition bypasses the normal publish-rate delay

If TLS stalls, no queue of old steering positions is created: when the pump becomes available it reads the newest desired state.

### 4.2 Critical commands

- `motion_stop`: MQTT command with ACK, 1500 ms wait budget
- `estop`: MQTT command with ACK, latched in firmware
- `estop_reset`: MQTT command with ACK
- relay, measurement and configuration actions remain ACK-confirmed

Successful high-rate `drive`, `heartbeat` and alert-refresh packets are not ACKed by the ESP32. Validation failures for those packets are still ACKed as errors. This removes redundant robot-to-broker traffic without weakening critical-command confirmation.

### 4.3 TLS and telemetry

- Android SSLSocket: TCP_NODELAY + TCP keepalive
- ESP32 WiFiClientSecure: TCP no-delay
- incoming MQTT packets are processed before periodic telemetry
- full Remote telemetry cadence is reduced from 250 ms to 500 ms (2 Hz), leaving more TLS/broker bandwidth and MCU time for control
- Remote probe records command ACK round-trip time and telemetry age in readiness detail

## 5. Source components changed / governed

Android:
- `DirectDriveDatagram.java`
- `RobotApi.java`
- `RemoteDrivePolicy.java`
- `RemoteConnection.java`
- `SecureMqtt311Client.java`
- corresponding unit tests

ESP32:
- `HazRob.ino`
- `Config.h` / existing 500 ms drive watchdog contract
- `RemoteMqtt.cpp`

Verification:
- `tools/audit_hr048_drive_contract.py`
- `tools/audit_hr049_control_remote_contract.py`
- consolidated `audit_hr047_gas_safety_contract.py` now makes HR048 and HR049 mandatory release gates

## 6. Software/build verification record

All accepted build results below came from artifact source commit `42dc0568f8afb3fa96175b149007ac079b98f643`.

- [x] Baseline 0.10.1 cross-component integration contract PASS
- [x] HR048 drive regression gate PASS
- [x] HR049 control/remote static gate PASS
- [x] HR047 gas-safety regression gate PASS
- [x] Android unit tests PASS (`:app:testDebugUnitTest`, Gradle BUILD SUCCESSFUL)
- [x] Android release APK build PASS (`:app:assembleRelease`, Gradle BUILD SUCCESSFUL)
- [x] Controlled LFL/UFL seed SHA-256 verified: `457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`
- [x] APK SHA-256: `b00e4c906e3cc96793559fdc931fb5355d2c44dd0f7941016cedcf6a429e48d0`
- [x] APK signer SHA-256 verified: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`
- [x] ESP32 main firmware compile PASS using ESP32 core 3.3.11
  - program: `1,096,381 bytes (34%)`
  - globals: `50,660 bytes (15%)`; `277,020 bytes` remaining
- [x] ESP32-CAM compile PASS using ESP32 core 3.3.11
  - program: `1,118,803 bytes (35%)`
  - globals: `67,836 bytes (20%)`; `259,844 bytes` remaining
- [x] application, ESP32 main and ESP32-CAM artifacts were all built from the same artifact source commit

### Build notes

The first Android build attempt stopped before Java compilation because the controlled flammability-seed service was waking from a cold start. No source or scientific data were changed. The second build retrieved the same pinned seed and verified its required SHA-256 before compilation, after which unit tests and release assembly passed. This infrastructure retry is not classified as a source-code failure.

Compiler warnings remain in pre-existing areas (unused helper / constant CA check in the main firmware and volatile-counter deprecation warnings in ESP32-CAM). They are not build errors and HR049 did not suppress them. They remain cleanup items rather than field-acceptance evidence.

## 7. Direct physical gate — PENDING

Wheels-up first, then floor test:

- [ ] forward and reverse respond without perceptible command backlog
- [ ] full-speed straight command reaches 100/100 logical command
- [ ] forward-right arc gives L > R >= 0
- [ ] forward-left arc gives R > L >= 0
- [ ] right pivot = +100/-100 logical command and correct physical handedness
- [ ] left pivot = -100/+100 logical command and correct physical handedness
- [ ] release stops promptly; no delayed historical command follows release
- [ ] `/diag.drive_udp.packets` increases during Direct control and rejected count does not climb abnormally
- [ ] loss of command stream produces watchdog neutral within the 500 ms contract

## 8. Remote field gate — PENDING

- [ ] robot `/remote/status`: wifi -> ip -> dns -> time -> internet -> tls -> mqtt ready
- [ ] Android Remote probe receives ACK and fresh telemetry
- [ ] steering changes do not replay an old backlog after network jitter
- [ ] stop is ACK-confirmed
- [ ] E-STOP remains latched and requires explicit reset
- [ ] temporary WAN interruption results in safe drive neutralization, followed by automatic session recovery
- [ ] Remote readiness reports plausible ACK RTT and telemetry age

## 9. Safety / scientific non-regression

HR049 does not change MQ R0 values, exposure limits, ChemCore/M4 rules, sensor calibration authority, GPIO mapping, or gas-warning thresholds. It changes control transport scheduling and network load only.

## 10. Approval rule

Do **not** mark HR049 `PHYSICAL PASS` from compilation, unit tests, or static audits. Final acceptance requires the Direct and Remote field gates above on the assembled robot.
