# HazRob 0.10.1 — HR-0101-049 Controlled Change Manifest

**Change:** Smooth / low-latency drive and Remote control integration  
**Branch:** `dev/hazrob-0.10.1` only — no new development branch  
**Status:** IN PROGRESS — source changes implemented; software/build and field acceptance must be recorded below before PASS.

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

## 6. Acceptance gates

### Software gates

- [ ] HR048 drive regression gate PASS
- [ ] HR049 control/remote static gate PASS
- [ ] Android unit tests PASS
- [ ] Android release APK build PASS
- [ ] APK signing fingerprint verified
- [ ] ESP32 main firmware compile PASS
- [ ] ESP32-CAM compile PASS (regression)
- [ ] application and both firmware artifacts built from the same final source commit

### Direct physical gate

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

### Remote field gate

- [ ] robot `/remote/status`: wifi -> ip -> dns -> time -> internet -> tls -> mqtt ready
- [ ] Android Remote probe receives ACK and fresh telemetry
- [ ] steering changes do not replay an old backlog after network jitter
- [ ] stop is ACK-confirmed
- [ ] E-STOP remains latched and requires explicit reset
- [ ] temporary WAN interruption results in safe drive neutralization, followed by automatic session recovery
- [ ] Remote readiness reports plausible ACK RTT and telemetry age

## 7. Safety / scientific non-regression

HR049 does not change MQ R0 values, exposure limits, ChemCore/M4 rules, sensor calibration authority, GPIO mapping, or gas-warning thresholds. It changes control transport scheduling and network load only.

## 8. Approval rule

Do **not** mark HR049 `PHYSICAL PASS` from compilation, unit tests, or static audits. Final acceptance requires the Direct and Remote field gates above on the assembled robot.
