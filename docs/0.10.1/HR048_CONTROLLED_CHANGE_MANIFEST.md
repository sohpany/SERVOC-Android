# HazRob 0.10.1 — HR-0101-048 Controlled Change Manifest

Status: **CONTROLLED REMEDIATION / SOFTWARE REVALIDATION IN PROGRESS / NOT YET FIELD-ACCEPTED**

- Product/version: `HazRob 0.10.1`
- Change request: `HR-0101-048`
- Active working branch: `dev/hazrob-0.10.1`
- Version bump: **NONE**
- Historical controlled baseline: `702ddcf9cef20f1212719902acbc275bda7f3f58`
- Previous field-distribution source: HR047 `5aa65eca113969cb8ec49dc5850076c327fdea5f`
- Branch-control decision: after initial isolation, the user directed all continuing remediation to remain on `dev/hazrob-0.10.1`; no additional development branch is authorized for this work.
- Acceptance rule: no new field baseline until software/build gates and applicable physical gates pass on one immutable source commit.

## 1. Approved remediation scope

| Area | Controlled change | Status |
|---|---|---|
| Drive mapping | Keep canonical differential mixer; map logical LEFT to L298N B and logical RIGHT to L298N A | Implemented; physical re-acceptance pending |
| Direct-drive transport | Capability-gated UDP fast lane on port 4210/version 1; HTTP `/drive` remains compatibility fallback | Implemented; build/field verification required |
| Direct-drive safety | Fixed 8-byte packet, checksum, sequence ordering, endpoint resynchronization, redundant zero-stop burst | Implemented; build/field verification required |
| Lost-command watchdog | Reduce motor stale-command neutralization from 1500 ms to 500 ms; explicit joystick zero remains immediate | Implemented; build/field verification required |
| Direct drive ownership | Suppress repeated idle `0,0` while retaining non-zero 20 Hz hold refresh and immediate touch-change dispatch | Implemented |
| MQ warning | Use firmware-normalized `Rs/R0=1.0` as operational fallback when separate Android baseline is absent | Implemented; field verification pending |
| Alert GPIO26 | Correct logical alert assertion but keep `ALERT_OUTPUT_INSTALLED=false` until safe external driver is confirmed | Implemented; hardware output intentionally fail-closed |
| Remote diagnosis | Show staged Config/Wi-Fi/IP/DNS/Time/Internet/TLS/MQTT/ACK/Telemetry readiness and firmware detail | Implemented; field first-failure capture pending |
| Analysis UI | Separate Screening outputs from M4-gated reportable PPM | Implemented; no M4 bypass |
| IR/ECHO/Sound | No speculative firmware fix before correct electrical interfacing; document authoritative pins and voltage rules | Physical rewiring/test pending |
| ADS VDD | Keep software acquisition assumption at 5 V but classify it as requiring physical confirmation | Measurement pending |
| LFL/UFL seed | APK build verifies the pinned seed SHA and packages the generated asset; repository-level offline source closure remains separate | Runtime APK offline after successful controlled build; source-build fetch dependency remains |

## 2. Drive-performance contract

The requested operating profile prioritizes fast, strong maneuvering at a frequently selected 100% speed limit while retaining fail-closed loss-of-command behavior.

The canonical joystick law remains:

- `Left = Y + X`
- `Right = Y - X`
- normalized to `[-100,+100]`.

Consequences at the 100% speed limit:

- straight forward: both sides can receive `+100`;
- forward-right arc: left remains the faster side while right is reduced;
- forward-left arc: right remains the faster side while left is reduced;
- deliberate pivot-right: logical `(Left=+100, Right=-100)`;
- deliberate pivot-left: logical `(Left=-100, Right=+100)`.

No acceleration ramp, artificial low-speed filter, or software torque reduction is introduced. `100` maps to full configured PWM authority. Physical wheel speed/torque remain limited by the battery, wiring, L298N voltage/current capability and the motors.

### 2.1 Low-latency Direct transport

Repeated TCP/HTTP request-response cycles can serialize joystick commands and produce noticeable lag. HR048 therefore adds a local UDP drive path:

- UDP port: `4210`;
- protocol version: `1`;
- packet size: exactly 8 bytes;
- magic: `H`,`R`;
- 16-bit sequence number;
- signed int8 Left and Right commands, clamped to `-100..+100`;
- XOR checksum over bytes 0..6.

The Android application enables this path **only after `/diag` advertises the matching UDP capability**. With older firmware, Android automatically retains the existing HTTP `/drive` path. Remote Internet control remains MQTT and preserves the same logical Left/Right semantics.

The firmware drains queued drive datagrams multiple times per main-loop iteration so motor commands have priority over slower telemetry and ancillary tasks. A new Android UDP source endpoint is allowed to resynchronize the sequence state immediately, preventing an app restart from producing an avoidable one-second steering delay.

### 2.2 Stop and link-loss behavior

- Joystick release sends zero immediately; it does not wait for the watchdog.
- Direct UDP zero uses a three-datagram burst to reduce the effect of an isolated packet loss.
- E-stop remains latched and retains its HTTP/MQTT safety path.
- If the entire command stream disappears, `CONTROL_COMMAND_TIMEOUT_MS=500` neutralizes non-zero motor output. At the normal 20 Hz hold refresh this represents ten missed refresh intervals.

## 3. Files intentionally changed by HR048

Android/application includes, as applicable:

- `app/src/main/java/com/servoc/app/communication/RobotApi.java`
- `app/src/main/java/com/servoc/app/communication/DirectDriveDatagram.java`
- `app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java`
- `app/src/main/java/com/servoc/app/measurement/InstantMqWarningEngine.java`
- `app/src/main/java/com/servoc/app/ui/AnalyticalLabTab.java`
- `app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java`
- `app/src/test/java/com/servoc/app/communication/DirectDriveDatagramTest.java`
- `app/src/test/java/com/servoc/app/communication/RobotProtocolV4AlertTest.java`
- `app/src/test/java/com/servoc/app/hardware/HazRobHardwareProfileTest.java`
- `app/src/test/java/com/servoc/app/measurement/GasAlertCoordinatorTest.java`

Main ESP32 firmware includes:

- `firmware/HazRob/HazRob.ino`
- `firmware/HazRob/Config.h`
- `firmware/HazRob/Alerts.cpp`

Controlled documentation/audits/build includes:

- `docs/0.10.1/FIELD_INTEGRATION_REMEDIATION_HR048.md`
- `docs/0.10.1/GPIO_AUTHORITY.md`
- `docs/0.10.1/HR048_CONTROLLED_CHANGE_MANIFEST.md`
- `tools/audit_0101_integration_contract.py`
- `tools/audit_hr048_drive_contract.py`
- `tools/audit_hr048_integration_contract.py`
- `tools/build_hr048_validation.sh`

No ESP32-CAM runtime behavior is changed by the drive-performance remediation.

## 4. Invariants that must not change

- Version remains `0.10.1`; versionCode remains 21 unless a separate distribution decision explicitly changes it.
- Direct robot address remains `192.168.4.1`; ESP32-CAM direct address remains `192.168.4.2`.
- I2C remains SDA GPIO21 / SCL GPIO22.
- ADS addresses remain `0x48` and `0x49`.
- MQ relay remains GPIO25 active LOW.
- Accepted field R0 campaign and values are not recalibrated by this remediation.
- M4 quantitative gate is not weakened or bypassed.
- Remote MQTT preserves logical `left/right`; no UI-only steering inversion is permitted.
- GPIO26 never directly drives the measured low-impedance speaker.
- IR/ECHO/Sound are not reclassified as I2C devices.
- HTTP `/drive` remains available as a compatibility/diagnostic path even when UDP fast drive is supported.

## 5. Software/build verification gate

One immutable commit must produce all applicable PASS results:

1. HR048 drive contract, including UDP protocol parity, 500 ms watchdog and corrected Left/Right mapping;
2. cross-component 0.10.1 integration audit;
3. motion-control regression audit;
4. HR047 gas-safety regression audit;
5. all Android `:app:testDebugUnitTest` tests;
6. Android `:app:assembleRelease`;
7. APK signer SHA-256 = `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`;
8. pinned LFL/UFL seed SHA-256 = `457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434` during APK construction;
9. main ESP32 compile using controlled `esp32:esp32@3.3.11`, FQBN `esp32:esp32:esp32:PartitionScheme=huge_app`;
10. ESP32-CAM regression compile using the controlled core and FQBN `esp32:esp32:esp32cam`;
11. SHA-256 manifests for distributed test artifacts.

A software/build PASS does not satisfy the physical field gate.

## 6. Physical field gate

### Drive

First test with wheels lifted. Confirm side/direction isolation, then move to floor testing. Required behaviors:

- forward and reverse respond without perceptible command queuing;
- small right/left inputs at high forward speed produce immediate differential-speed arcs;
- deliberate pivot-right and pivot-left rotate in the requested direction;
- releasing the joystick stops immediately;
- loss of command stream reaches neutral no later than the 500 ms watchdog target;
- `/diag.drive_udp.packets` increments during Direct fast-drive operation and rejected-packet count does not rise continuously.

Any weak/non-reversing side, voltage collapse or reset under high-load pivot is treated as electrical/power/mechanical until proven otherwise. Software must not command beyond 100% PWM to conceal a power-stage limitation.

### Sensors

- MAX4466 OUT direct to the approved analog path at GPIO36 and within ESP32-safe voltage.
- IR output to GPIO35 at <=3.3 V logic.
- Ultrasonic TRIG GPIO23; ECHO GPIO39 reduced to <=3.3 V with appropriate interface.
- ENV/BME/BMP and ADS I2C ACKs tested separately.
- ADS1115 assembled VDD measured and recorded before analytical physical acceptance.

### MQ/alerts

- Heater warm-up completed.
- Controlled clean-air response recorded.
- Controlled smoke/gas challenge confirms live response and phone-side alert behavior.
- External GPIO26 alert hardware remains disabled until safe driver topology is physically approved.

### Remote

Capture `/remote/status` and validate MQTT command -> ACK -> telemetry round-trip before declaring Remote PASS. Direct UDP does not replace Remote MQTT.

## 7. LFL/UFL build/runtime distinction

The pinned seed identity remains:

`457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`

The controlled Android build verifies this hash before packaging the generated flammability asset into the APK. Therefore a successfully built APK can use its packaged dataset offline at runtime. The repository source still relies on the controlled seed endpoint during a clean build when the generated asset is absent; eliminating that **build-time** dependency remains a separate reproducibility improvement. The primary `hazrob-0101-flammability-seed` service must therefore remain available until the exact pinned source asset is committed or another equally reproducible source-build mechanism is accepted.

This dataset is unrelated to offline camera flame recognition.

## 8. Acceptance labels

Use only these states:

- **IMPLEMENTED** — source change exists.
- **SOFTWARE PASS** — static/unit/build gate passed for one immutable commit.
- **PHYSICAL PASS** — measured field test passed.
- **PENDING** — evidence not yet collected.
- **BLOCKED** — a prerequisite is missing.

Do not label drive performance PHYSICAL PASS merely because APK/Firmware compilation succeeds.
