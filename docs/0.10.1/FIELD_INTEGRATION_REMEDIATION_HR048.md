# HazRob 0.10.1 — HR-0101-048 Field Integration Remediation

Status: **CONTROLLED REMEDIATION CANDIDATE — SOFTWARE/BUILD VALIDATION AND PHYSICAL RE-ACCEPTANCE REQUIRED**

Release identity: `HazRob 0.10.1` — **no version bump**  
Branch: `fix/hr-0101-048-field-remediation`  
Controlled baseline: `dev/hazrob-0.10.1` at `702ddcf9cef20f1212719902acbc275bda7f3f58`  
Previous field-distribution source: HR047 `5aa65eca113969cb8ec49dc5850076c327fdea5f`

## 1. Trigger and change-control rule

Physical testing after loading the HR047 distribution exposed integration failures that were not closed by source/build gates alone:

1. Forward and reverse motion work, but steering/pivot behavior is incorrect and steering handedness is reversed.
2. MQ response is operationally weak and immediate gas alerts are absent.
3. The analysis surface is perceived as withholding all results.
4. Remote control over the Internet does not work in the field.
5. IR, ultrasonic ECHO and sound channels do not work in the current physical wiring; their signals were reported routed through an I2C level shifter while modules were powered from 5 V.

HR047 therefore remains a source/build baseline only. HR048 is handled as a controlled remediation inside 0.10.1. No HR048 code is accepted into `dev/hazrob-0.10.1` until the software gates and the applicable physical gates in this document pass.

## 2. Drive control — controlled remediation candidate

### 2.1 Confirmed field observations

- Forward command: correct.
- Reverse command: correct.
- Right steering command: vehicle turns left.
- Left steering command: vehicle barely turns and wheel behavior is not the expected differential/pivot motion.
- Initial browser-side isolation attempts were performed while the old Android application could still own a 20 Hz drive stream. Those individual browser results are therefore not accepted as proof of physical L298N channel isolation.

### 2.2 Software path review

The operator coordinate convention remains canonical:

- `+Y = forward`
- `+X = right`
- `Left = Y + X`
- `Right = Y - X`

This means a forward-right arc must have both sides forward with logical Left faster than logical Right. Pivot-right is logical `(Left>0, Right<0)`. The Android mixer is therefore not inverted and must not be patched by merely negating X.

The Direct and MQTT/Remote paths preserve the same logical `left/right` values. The correction must therefore be placed at the hardware association boundary if the physical sides are cross-associated.

### 2.3 HR048 mapping candidate

Based on the field-confirmed steering inversion while straight forward/reverse remained correct, HR048 applies the controlled remediation candidate:

- logical **LEFT** -> L298N channel B: `ENB GPIO17`, `IN3 GPIO19`, `IN4 GPIO32`;
- logical **RIGHT** -> L298N channel A: `ENA GPIO16`, `IN1 GPIO18`, `IN2 GPIO27`;
- `DRIVE_L_INVERT=false` and `DRIVE_R_INVERT=false` remain unchanged.

This is deliberately a **remediation candidate, not a physical-acceptance claim**. It changes steering handedness without changing the equal-sign forward/reverse behavior. A weak/non-reversing side after this change must be investigated electrically/mechanically rather than hidden by another software sign swap.

### 2.4 Android drive-ownership defect — fixed

Direct mode previously retransmitted idle `0,0` every 50 ms. An open but idle Android activity could therefore overwrite independent browser/diagnostic drive commands. HR048 sends the zero transition once, then suppresses repeated idle-zero Direct commands until Android itself sends a non-zero drive command again. Non-zero operator commands retain the 20 Hz continuity refresh.

### 2.5 Mandatory post-flash wheels-up gate

With the HR048 candidate installed, wheels lifted and low speed selected, record physical side and direction for:

- `L=+50, R=0`
- `L=0, R=+50`
- `L=-50, R=0`
- `L=0, R=-50`
- `L=+50, R=-50`
- `L=-50, R=+50`

Acceptance requires logical Left to operate only the physical left side, logical Right only the physical right side, positive/negative direction to reverse each side, forward/right and forward/left arcs to have the expected differential speed relationship, and both pivots to rotate in the requested direction.

## 3. MQ immediate warning — remediation implemented

The Android instant-warning layer previously required a separately captured/persisted clean-air baseline. If that baseline was absent, a valid firmware `Rs/R0` response could be treated as NORMAL and a fresh installation could fail to produce an operational gas warning after warm-up.

HR048 changes the operational response alarm rule:

- a captured Android clean-air baseline still takes precedence;
- if absent, the accepted firmware normalization uses `Rs/R0 = 1.0` as the operational fallback baseline;
- the first remediation build retains 20% `ELEVATED` and 40% `HIGH` response deltas to avoid changing multiple variables simultaneously;
- the delta is an MQ response alarm only, not validated ppm, IDLH, LFL or exact chemical identification.

Unit-test coverage requires warning operation after warm-up even when no separate Android baseline has yet been saved.

## 4. Alert GPIO26 — logic corrected, electrical output fail-closed

The prior firmware calculated alert levels but forced GPIO26 LOW on every update. HR048 corrects the logical output path so an installed safe interface can follow an active alert.

The electrical boundary remains fail-closed:

- GPIO26 is a logic output only;
- an approximately 8-ohm speaker must never be connected directly to an ESP32 GPIO;
- `ALERT_OUTPUT_INSTALLED=false` remains the default until a transistor/MOSFET/amplifier or suitable active-buzzer interface is physically confirmed;
- telemetry distinguishes logical alert activity from actual asserted hardware output;
- phone-side audible warning remains independent of the external GPIO driver.

## 5. IR / ultrasonic / sound — physical interface correction required

These channels are not I2C devices in the authoritative HazRob profile:

- MAX4466 sound `OUT -> GPIO36` as ADC1 analog input;
- IR digital output `-> GPIO35`;
- ultrasonic `TRIG -> GPIO23`;
- ultrasonic `ECHO -> GPIO39` input-only, with a 5 V ECHO reduced to `<=3.3 V` using an appropriate divider/unidirectional interface.

A bidirectional BSS138-style I2C level shifter is not an appropriate analog path for MAX4466 and is not the preferred generic interface for HC-SR04 ECHO. Physical rewiring is required before these channels may be classified as firmware failures.

The ultrasonic non-blocking ECHO implementation also remains subject to a post-rewire timing test because short ECHO pulses share the main firmware loop.

## 6. Remote control — diagnostic remediation, no speculative transport rewrite

The robot firmware already exposes a staged remote status chain. HR048 expands the developer connection screen to display it explicitly:

`configured -> wifi -> ip_ok -> dns -> time -> internet -> tls -> mqtt -> command_ack -> telemetry -> remote_ready`

The firmware `detail` field is displayed as well. The next field test must identify the **first failing stage** before transport behavior is changed. This prevents replacing HiveMQ/Render/TLS/MQTT code without evidence.

The diagnostic UI and firmware keys are required to remain contract-aligned.

## 7. Analysis UI / M4 — presentation corrected without weakening scientific gates

HR048 explicitly separates:

- **Screening:** normalized `Rs/R0` fingerprint, ranked candidates, posterior, residuals and screening fit after a valid controlled session;
- **Quantitative PPM:** reportable only after the independent M4 evidence gate for the relevant candidate/curves.

A withheld PPM must not be presented as “all analysis unavailable.” No M4 gate is bypassed and no screening estimate is relabeled as reportable concentration.

## 8. LFL/UFL flammability library — offline closure still pending

The gas-flammability runtime is designed to import the authoritative NIOSH LFL/UFL seed into local SQLite and then make hazard decisions offline. The pinned seed identity is:

`SHA-256 457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`

Current source still attempts bundled asset `generated/flammability/niosh_npg_flammability.json` first and uses the controlled Render endpoint only as a pinned first-install fallback if the asset is absent. The HR048 branch currently does **not** contain that bundled asset. Therefore the primary `hazrob-0101-flammability-seed` service must not be deleted yet.

Closure rule:

1. place the exact pinned file in `app/src/main/assets/generated/flammability/`;
2. verify its SHA-256 against the pinned value;
3. add a build gate requiring the bundled asset and hash;
4. verify a fresh-install offline bootstrap from the bundled asset;
5. only then may the Render flammability seed service be retired. Duplicate no-op seed services are not runtime authorities, but service cleanup is operational infrastructure work separate from source acceptance.

This LFL/UFL seed is unrelated to camera flame recognition. Visual flame analysis remains an application-side/offline vision subsystem.

## 9. Hardware-assumption status requiring physical confirmation

The source acquisition contract currently assumes ADS1115 VDD = 5 V, PGA `+/-6.144 V`, 860 SPS and four conversions/channel. HR048 documentation now treats the **5 V ADS supply as a software configuration assumption requiring physical confirmation**. A 3.3 V SDA/SCL idle level proves the ESP32-side I2C logic domain, not the ADS board supply voltage.

No ADS supply constant is changed without measuring the assembled board and reviewing the resulting analog conversion assumptions.

## 10. Verification gates

HR048 must pass, from one immutable source commit:

1. HR048 drive contract audit;
2. cross-component integration audit adapted for the intentional HR048 drive-map change;
3. motion-control audit;
4. HR047 gas-safety contract regression audit;
5. Android unit tests;
6. Android release assembly and signer verification;
7. main ESP32 compile with the controlled ESP32 core;
8. ESP32-CAM compile with the controlled ESP32 core;
9. SHA-256 manifest for released test artifacts;
10. physical wheels-up drive gate;
11. corrected IR/ECHO/sound wiring test;
12. remote staged-status capture and command/ACK/telemetry field test;
13. MQ response/phone-alert field test after warm-up.

A build PASS is not a physical PASS. Physical acceptance remains separately recorded.

## 11. Current acceptance state

- Version identity: **0.10.1 retained**.
- Controlled branch isolation from `dev/hazrob-0.10.1`: **PASS**.
- Drive-mixer review: **PASS — canonical mixer retained**.
- HR048 logical side mapping: **implemented candidate; physical acceptance pending**.
- Direct idle-zero ownership fix: **implemented; build/field verification pending**.
- MQ fallback operational warning: **implemented; unit/build/field verification pending**.
- GPIO26 alert logic: **implemented; hardware output intentionally disabled pending safe driver**.
- Remote diagnostic visibility: **implemented; field first-failure capture pending**.
- Screening vs M4 presentation: **implemented; build verification pending**.
- IR/ECHO/sound: **physical rewiring pending**.
- ADS VDD: **software assumes 5 V; physical confirmation pending**.
- LFL/UFL offline bundle: **not yet closed; primary Render seed authority retained**.

No HR048 APK, firmware binary or source package is approved as the new 0.10.1 field baseline until the applicable gates above pass.
