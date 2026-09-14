# HazRob 0.10.1 — HR-0101-048 Field Integration Remediation

Status: **CONTROLLED REMEDIATION — SOFTWARE REVALIDATION IN PROGRESS; PHYSICAL RE-ACCEPTANCE REQUIRED**

Release identity: `HazRob 0.10.1` — **no version bump**  
Active working branch: `dev/hazrob-0.10.1`  
Historical controlled baseline: `702ddcf9cef20f1212719902acbc275bda7f3f58`  
Previous field-distribution source: HR047 `5aa65eca113969cb8ec49dc5850076c327fdea5f`

The initial HR048 work was isolated temporarily. By explicit project decision, continuing remediation is now performed directly on `dev/hazrob-0.10.1`; no additional development branch is authorized for this change set.

## 1. Trigger and change-control rule

Physical testing after the previous field distribution exposed integration failures that source/build gates alone did not close:

1. forward and reverse motion worked but steering handedness was reversed;
2. the installed app/firmware combination showed poor steering behavior;
3. a later application-only test against older firmware showed severe control latency and poor responsiveness;
4. MQ immediate-warning behavior required remediation;
5. Remote field readiness needed staged diagnostics;
6. IR, ultrasonic ECHO and sound required correction of their physical interface assumptions.

No new field baseline is accepted until application, firmware and applicable physical gates below pass from one immutable source commit.

## 2. Drive mapping and maneuvering

### 2.1 Canonical mixer retained

The operator coordinate convention remains:

- `+Y = forward`
- `+X = right`
- `Left = Y + X`
- `Right = Y - X`

After normalization to `-100..+100`, this gives strong differential-drive authority without an artificial acceleration ramp. At a configured speed limit of 100%, straight motion can command both sides at full PWM; a forward turn slows the inside side relative to the outside side; a deliberate lateral joystick command at zero forward demand produces a full pivot with opposite signs.

### 2.2 Corrected physical-side association

Field evidence showed reversed steering while equal-sign forward/reverse remained correct. HR048 therefore maps:

- logical **LEFT** -> L298N channel B: `ENB GPIO17`, `IN3 GPIO19`, `IN4 GPIO32`;
- logical **RIGHT** -> L298N channel A: `ENA GPIO16`, `IN1 GPIO18`, `IN2 GPIO27`;
- both direction inversions remain `false`.

This changes steering handedness without changing straight forward/reverse polarity. It remains subject to physical wheels-up acceptance.

### 2.3 Direct-mode latency remediation

The former Direct path sent every joystick update as a complete HTTP request to `/drive`. Android also serialized drive dispatch with a single in-flight guard. A slow HTTP response could therefore reduce the effective command update rate substantially even though the scheduler targeted 20 Hz.

HR048 adds a capability-gated low-latency UDP fast lane for **Direct local drive only**:

- destination `192.168.4.1:4210`;
- protocol version `1`;
- fixed 8-byte packet;
- bytes 0..1 magic `H`,`R`;
- byte 2 protocol version;
- bytes 3..4 little-endian 16-bit sequence;
- bytes 5..6 signed Left/Right commands in `-100..+100`;
- byte 7 XOR checksum of bytes 0..6.

The firmware advertises the capability in `/diag.drive_udp`. Android enables UDP only when port and version match. Older firmware automatically remains on HTTP, so compatibility is fail-safe rather than assumed.

The firmware services the UDP receiver repeatedly around other loop work. Valid commands call the existing `driveSet()` path directly; there is no new acceleration filter. Packet sequence order is enforced, and a changed Android source IP/UDP port resets sequence ownership immediately so an application restart does not create an avoidable one-second delay.

### 2.4 Stop and lost-command response

- A joystick zero transition is sent immediately.
- Direct UDP stop is transmitted as a three-packet zero burst to reduce sensitivity to a single lost datagram.
- Repeated idle `0,0` remains suppressed after the zero transition so an idle app does not overwrite independent diagnostic commands.
- Non-zero hold commands retain the 20 Hz continuity stream, while touch changes dispatch immediately.
- `CONTROL_COMMAND_TIMEOUT_MS` is reduced from 1500 ms to **500 ms**. This is not a smoothing delay; it is a fail-safe watchdog used only when the command stream disappears.
- E-stop remains separately latched and uses the established safety path.

### 2.5 Performance boundary

Software command 100 corresponds to full configured PWM authority. If the vehicle remains mechanically weak at 100%, software must not command values above 100 to conceal power-stage limitations. Check battery voltage under load, L298N input/output voltage drop, wiring, connector resistance and motor current—particularly during pivots, where current demand can be highest.

## 3. Mandatory drive field gate

First test with wheels lifted, then floor test.

Wheels-up isolation:

- `L=+50, R=0`
- `L=0, R=+50`
- `L=-50, R=0`
- `L=0, R=-50`
- `L=+50, R=-50`
- `L=-50, R=+50`

Then verify:

1. immediate forward response;
2. immediate reverse response;
3. forward-right and forward-left arcs at high speed;
4. pivot right and pivot left;
5. immediate stop on joystick release;
6. no stale-command motion beyond the 500 ms watchdog if the command stream is intentionally interrupted;
7. `/diag.drive_udp.packets` increments during Direct drive and `rejected` does not rise continuously.

A weak or non-reversing side remains an electrical/mechanical finding until proven otherwise.

## 4. MQ immediate warning

The Android warning layer previously depended on a separately persisted clean-air baseline. HR048 retains a captured baseline when available and otherwise uses the firmware-normalized `Rs/R0 = 1.0` as the operational fallback. The initial 20% ELEVATED and 40% HIGH response deltas remain unchanged so the remediation does not simultaneously alter threshold sensitivity.

This remains an operational sensor-response alarm only. It does not establish validated ppm, IDLH, LFL or exact chemical identity.

## 5. Alert GPIO26

Alert logic now follows the logical alert state when a safe output interface is explicitly installed. The physical output remains fail-closed:

- GPIO26 is logic-level only;
- an approximately 8-ohm speaker must never be connected directly to an ESP32 GPIO;
- `ALERT_OUTPUT_INSTALLED=false` remains default until an external transistor/MOSFET/amplifier or appropriate active-buzzer interface is confirmed.

## 6. IR / ultrasonic / sound

Authoritative non-I2C paths are:

- MAX4466 `OUT -> GPIO36` analog ADC1;
- IR digital output `-> GPIO35` at ESP32-safe logic level;
- ultrasonic `TRIG -> GPIO23`;
- ultrasonic `ECHO -> GPIO39`, reduced from 5 V to `<=3.3 V` with an appropriate unidirectional interface/divider.

A bidirectional BSS138 I2C level shifter is not an appropriate analog path for MAX4466. Ultrasonic timing remains subject to a post-rewire field test because ECHO acquisition shares the main firmware loop.

## 7. Remote control

The developer connection screen exposes the staged robot readiness chain:

`configured -> wifi -> ip_ok -> dns -> time -> internet -> tls -> mqtt -> command_ack -> telemetry -> remote_ready`

The firmware detail is also displayed. Remote Internet drive remains MQTT; the new UDP transport is **Direct-mode only** and does not alter the Remote security/transport contract.

## 8. Analysis UI / M4

HR048 explicitly separates:

- Screening: normalized `Rs/R0` fingerprint, ranked candidates, posterior, residuals and screening fit after a valid controlled session;
- Quantitative PPM: reportable only after the independent M4 evidence gate.

No M4 gate is bypassed.

## 9. LFL/UFL build/runtime status

Pinned seed SHA-256:

`457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`

The controlled Android build verifies this SHA before packaging the generated flammability asset. A successfully built APK therefore carries the dataset for offline runtime use. However, a clean source build still depends on obtaining the pinned seed when the generated asset is absent. Eliminating that build-time network dependency remains a separate reproducibility closure item; the primary seed service is retained until then.

The LFL/UFL seed is unrelated to offline camera flame recognition.

## 10. ADS1115 supply assumption

The acquisition contract currently assumes ADS1115 VDD = 5 V, PGA `+/-6.144 V`, 860 SPS and four conversions/channel. This is a software configuration assumption requiring physical confirmation on the assembled board. A 3.3 V SDA/SCL level alone does not prove ADS VDD.

## 11. Verification gates

One immutable source commit must pass:

1. HR048 drive contract including corrected side mapping, UDP parity, sequence/checksum behavior and 500 ms watchdog;
2. cross-component 0.10.1 integration audit;
3. motion-control audit;
4. HR047 gas-safety regression audit;
5. Android unit tests including UDP packet-layout tests;
6. Android release assembly;
7. APK signer verification;
8. pinned LFL/UFL seed SHA verification during build;
9. main ESP32 compile with controlled ESP32 core;
10. ESP32-CAM regression compile;
11. SHA-256 manifests for distributed artifacts;
12. physical drive gate;
13. corrected IR/ECHO/sound wiring test;
14. Remote command/ACK/telemetry field test;
15. MQ response/phone-alert field test after warm-up.

A software/build PASS is not a physical PASS.

## 12. Current acceptance state

- Version identity: **0.10.1 retained**.
- Active development location: **`dev/hazrob-0.10.1` only**.
- Canonical drive mixer: **retained**.
- Logical side mapping: **implemented; physical acceptance pending**.
- Direct UDP fast lane: **implemented; revalidation pending at time of this document update**.
- 500 ms lost-command watchdog: **implemented; field timing pending**.
- Direct idle-zero ownership fix: **implemented**.
- MQ fallback warning: **implemented; field verification pending**.
- GPIO26 alert logic: **implemented; hardware output intentionally disabled pending safe driver**.
- Remote diagnostic visibility: **implemented; field capture pending**.
- Screening vs M4 presentation: **implemented**.
- IR/ECHO/sound: **physical rewiring pending**.
- ADS VDD: **physical confirmation pending**.

No new 0.10.1 field baseline is declared until the build gates pass on the final documented commit and the applicable physical tests are completed.
