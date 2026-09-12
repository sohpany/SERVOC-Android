# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-040

## HR-0101-040 — ADS/session diagnostics, environment recovery, phone audible alerts, refresh controls, and report consistency

Date: 2026-09-12

### Field trigger
A powered Direct-mode field test established that the Android application remained connected and IR telemetry was live, while MQ cards (including MQ9/MQ135) showed `Raw 0`, no valid ADC voltage/Rs values, a 30-second analysis session appeared static and ended in error, the environmental panel was not updating, and ADS status was not `OK`. MQ-module indicator LEDs reacted to smoke/alcohol, but that digital/module indication does not prove the HazRob analog acquisition path `MQ AO -> ADS1115 -> ESP32` is valid.

The requested operator changes were therefore treated as one integrated 0.10.1 correction, not as isolated UI patches.

### Authoritative executable source verified
- branch: `dev/hazrob-0.10.1`
- executable source SHA: `9ed5582a55865ac2ef2aa506324ce3a9fbab7a81`
- Android final verification deploy: `dep-dairjp3m8hqs73e13m40`
- firmware final verification deploy: `dep-dairjrvjf6qs73cdf94g`

Documentation added after that SHA does not alter the verified application or firmware executable source.

## ADS1115 diagnostics and acquisition contract
The canonical analytical bus remains:
- SDA = GPIO21;
- SCL = GPIO22;
- ADS1 = `0x48`, intended address strap `ADDR -> GND`;
- ADS2 = `0x49`, intended address strap `ADDR -> VDD`;
- ADS supply reference in the acquisition model = 5 V;
- MQ1 group on ADS1: MQ2/A0, MQ4/A1, MQ5/A2, MQ8/A3;
- MQ2 group on ADS2: MQ3/A0, MQ7/A1, MQ9/A2, MQ135/A3.

The Android measurement workspace now reports each converter explicitly as `OK` or `not detected` and identifies the expected address strap. A sensor card whose assigned ADS is missing now reports the ADS index/address and points the operator to VDD/GND, SDA/SCL and ADDR instead of presenting a misleading generic invalid reading.

Firmware retains the existing periodic ADS reprobe, so an ADS that becomes valid after startup may recover without an ESP32 reboot.

Electrical note: powering the ADS board from 5 V does not authorize 5 V on ESP32 SDA/SCL. The final wiring must keep ESP32 I2C logic at a safe 3.3 V level, using the approved level-shifter arrangement where needed.

## Measurement-session state machine correction
The previous session telemetry exposed configured `duration_s` only. The UI therefore appeared stuck at `30 s`, although the firmware timer was running.

HR-0101-040 adds explicit session telemetry:
- `elapsed_s`;
- `remaining_s`;
- `valid_channels`;
- `error`.

The firmware now differentiates these preflight/runtime failures:
- `mq_power_off`;
- `ads_missing` when neither ADS1 nor ADS2 is available;
- `power_changed` if MQ power changes during a measurement;
- `no_valid_samples` if the window completes without a usable MQ sample.

Partial acquisition remains permitted: if one ADS is present, a session may proceed and succeed using the valid channels. If both converters are absent, Direct `/measure?cmd=start` returns HTTP 409 and Remote `measure_start` returns a negative ACK rather than pretending that the session started.

The Android workspace displays a live elapsed/remaining countdown and an Arabic explanation of the final error.

## Environmental sensor recovery
The BME280/BMP280 chamber sensor was previously discovered during startup only. When no supported environmental sensor is present, firmware now retries discovery approximately every three seconds on the existing I2C bus. This allows a valid BME/BMP connection to recover without requiring an ESP32 restart.

No environmental address contract was changed: supported addresses remain `0x76` / `0x77` and the existing chip-ID validation remains authoritative.

## Phone-side audible warnings
The reserved robot alert output GPIO26 remains electrically unclaimed as an installed speaker output; HR-0101-040 does not drive an 8-ohm speaker directly from ESP32 GPIO.

A phone-side audible warning layer was added using the Android alarm audio stream:
- operational warning tone for gas-response warning/elevated conditions;
- more urgent tone cadence for danger conditions;
- IR/proximity warning may also cause a phone warning tone.

Scientific boundary is unchanged: MQ response is not presented as calibrated ppm/IDLH solely because an MQ module LED or raw response changes. Gas-response tones require valid analog acquisition and the existing operational clean-air baseline logic. If no valid baseline exists, the UI now says so explicitly rather than implying that warning evaluation is fully armed.

## R0 / calibration wording correction
Firmware default R0 values remain placeholders and are marked `r0_calibrated=false`. The measurement UI no longer labels such channels generically as “calibrated.” It now distinguishes `R0 calibrated` from `R0 not calibrated`, preserving the scientific calibration contract.

## Manual refresh control
A visible `Refresh` control is now wrapped around every main application tab. In the Measurements workspace the wrapper remains present while switching among Physical / Measurement / Analysis subtabs.

Manual refresh performs a real `RobotApi.snapshot()` acquisition rather than merely repainting cached values. The existing periodic telemetry loop remains active; the button is an operator-requested forced refresh, not a replacement for live telemetry.

## Report review and correction
Report paths were reviewed against the new diagnostics.

### PDF
The analytical PDF summary now includes:
- ADS1 `0x48` state;
- ADS2 `0x49` state;
- environmental sensor state/type;
- measurement state and error reason;
- existing MQ power/session/scientific provenance information.

### Excel
The existing XLSX hardware/acquisition sheet already records ADS1/ADS2 state, environment and MQ-power context; its structure was intentionally preserved to avoid unnecessary report-schema churn.

### Raw JSON
The Raw export continues to preserve the complete captured payload, including the new measurement diagnostic fields.

No report is permitted to convert an uncalibrated MQ response into an unsupported quantitative exposure claim.

## Direct / Remote transport parity
Measurement preflight semantics were reviewed across both transports:
- Direct: `/measure?cmd=start` invokes the same firmware `measureStart()` and returns failure when preflight fails;
- Remote: `measure_start` invokes the same `measureStart()` and returns negative ACK on preflight failure;
- telemetry carries the same measurement object to Android in both modes;
- `RobotProtocolV4` preserves ADS status, countdown, error and valid-channel count.

No MQTT topic, broker host, robot ID, bootstrap role, Render video contract, camera path or GPIO allocation was changed by HR-0101-040.

## Automated verification
A new Android unit test `RobotProtocolV4DiagnosticsTest` verifies:
- ADS1/ADS2 state normalization;
- `any_ready` / `all_ready` status;
- measurement duration/elapsed/remaining preservation;
- valid-channel count preservation;
- explicit `ads_missing` error preservation.

Both independent exact-source builders executed the cross-component 0.10.1 integration audit before substantive build steps and returned:

`HazRob 0.10.1 cross-component integration contract: PASS`

### Android exact-source verification — PASS
Builder: `hazrob-0101-build`
Deploy: `dep-dairjp3m8hqs73e13m40`
Source: `9ed5582a55865ac2ef2aa506324ce3a9fbab7a81`

Results:
- cross-component integration contract: PASS;
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, including the new HR-0101-040 diagnostics tests;
- `:app:assembleRelease`: BUILD SUCCESSFUL;
- versionName: `0.10.1`;
- versionCode: `21`;
- APK SHA-256: `fcd633deb966ae71bee25f7767a00847b5e2a376b4a6ae3f40b74ba7438dfe9d`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`;
- deploy status: `live`.

### Firmware exact-source verification — PASS
Builder: `hazrob-0101-firmware-build`
Deploy: `dep-dairjrvjf6qs73cdf94g`
Source: `9ed5582a55865ac2ef2aa506324ce3a9fbab7a81`

Results:
- cross-component integration contract: PASS;
- main ESP32 compile/link: PASS;
- main ESP32 program: **1,087,653 bytes (34%)**;
- main ESP32 global RAM: **50,172 bytes (15%)**, 277,508 bytes remaining;
- ESP32-CAM compile/link: PASS;
- ESP32-CAM program: **1,118,771 bytes (35%)**;
- ESP32-CAM global RAM: **67,836 bytes (20%)**, 259,844 bytes remaining;
- deploy status: `live`.

ESP32-CAM executable source is functionally unchanged by HR-0101-040; it was rebuilt to prove whole-package compatibility.

## Render / HiveMQ consistency
Production Render/HiveMQ command/video contracts are unchanged by HR-0101-040. The production relay service remains live on the previously validated server deployment. HR-0101-038 live diagnostics already proved application and robot TLS/MQTT authentication, required subscriptions, bidirectional command/ACK/telemetry publishing, and retained status authorization. HR-0101-040 does not change broker credentials, ACL topic filters, bootstrap token semantics or video authorization.

## Known non-blocking warnings
Existing build warnings remain:
- constant certificate-string `strstr(...,"BEGIN CERTIFICATE")` warning in cloud/video configuration;
- deprecated operations on ESP32-CAM `volatile` diagnostic counters;
- Android/Gradle deprecation warnings that do not fail the current build.

These warnings predate HR-0101-040 and are not evidence of ADS, measurement-session, report, GPIO or transport inconsistency.

## Physical validation still required
Software/source/build consistency is proven. Hardware behavior still requires the assembled robot for:
- confirm ADS1 is detected at `0x48` with its intended address strap;
- confirm ADS2 is detected at `0x49` with its intended address strap;
- verify ADS VDD and safe SDA/SCL voltage levels;
- obtain non-zero valid AO/ADC/Rs/Rs-R0 telemetry from the appropriate MQ channels;
- confirm BME/BMP hot-reprobe recovery on the physical I2C bus;
- capture a valid clean-air operational baseline before expecting gas-response warning tones;
- confirm phone warning tone audibility on the target handset;
- run a full measurement session and confirm visible countdown, valid-channel count and READY completion;
- rerun Remote end-to-end against the physical ESP32 after the new firmware is flashed.

## Release classification
**HR-0101-040 SOURCE CONSISTENCY: PASS**

**ANDROID UNIT TESTS / RELEASE BUILD / SIGNING: PASS**

**MAIN ESP32 BUILD: PASS**

**ESP32-CAM BUILD: PASS**

**DIRECT/REMOTE MEASUREMENT CONTRACT PARITY: PASS**

**REPORT CONTRACT REVIEW: PASS**

**RENDER/HIVEMQ CONTRACT: UNCHANGED / PREVIOUS LIVE VERIFICATION RETAINED**

**PHYSICAL ADS/MQ/BME/PHONE-AUDIO VERIFICATION: PENDING**

HazRob 0.10.1 HR-0101-040 is therefore saved as the controlled software baseline prepared for the next field test and, after that, the requested distribution package.
