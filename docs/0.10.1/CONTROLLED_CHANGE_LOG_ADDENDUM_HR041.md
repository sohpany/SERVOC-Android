# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-041

## HR-0101-041 — Live measurement/session separation and conservative sensor-power diagnostics

Date: 2026-09-12

### Field requirement
The operator clarified that the Measurements > Live tab is a continuous monitoring surface. MQ readings must remain live whenever MQ power and the acquisition hardware are available, regardless of whether a timed analytical session has been started. A timed session exists only to capture and aggregate data for a particular place/time/experiment.

The operator also requested a useful explanation when readings fail, including the possibility of weak sensor power. Because HazRob does not currently measure the dedicated 5 V sensor rail directly, this change deliberately distinguishes an inferred electrical suspicion from a confirmed low-voltage measurement.

## Authoritative executable source verified
- Branch: `dev/hazrob-0.10.1`
- Executable/source SHA: `0846be98e4e5b3a92b8983537c9a574396cb4435`
- Android verification deploy: `dep-dairsenqj5pc73bb9ad0`
- Firmware verification deploy after clean build cache: `dep-dairtc5g1s2s738hblvg`

Documentation commits after this SHA do not alter the verified application or firmware executable source.

## Live measurement contract
The existing main ESP32 firmware already satisfies the required acquisition architecture:
- `hwUpdate()` continuously calls `mqUpdate()`;
- `mqUpdate()` continuously rotates through the MQ channels;
- `readMQ()` is gated by MQ power state and the relevant ADS availability, not by measurement-session state;
- normal telemetry exports the instantaneous MQ object through `mqJson(false)`;
- the measurement-session object separately exports session averages through `mqJson(true)`.

Therefore HR-0101-041 does not change the ESP32 sampling state machine. Instead, the Android contract is made explicit and test-protected: session state may be idle, measuring, ready, or error while valid live MQ telemetry continues to be displayed independently.

The UI now labels the live path explicitly as independent from the recording session. When the session is idle it states that recording is stopped while live reading continues.

## Sensor-power / signal diagnostic contract
A new Android diagnostic engine `LiveSensorDiagnostic` derives the operator-facing state from:
- MQ power relay state and power-on age;
- ADS1/ADS2 presence;
- per-channel MQ validity from live telemetry.

Diagnostic states include:
- `mq_power_off` — exact software power state;
- `ads_missing` — both ADS converters unavailable;
- `ads_partial` — one ADS unavailable while the other produces valid live channels;
- `waiting_samples` — short settling window immediately after MQ power-on;
- `suspected_sensor_power_or_ground` — both ADS converters respond but no MQ channel is valid after the settling window;
- `partial_signal_fault` — some available MQ channels are valid and some are not;
- `ads_partial_no_signal` — one ADS is absent and the available ADS channels also produce no valid MQ signal;
- `live_ok` — all available MQ channels are valid.

### Safety / scientific wording rule
`power_cause_confirmed` remains `false` for inferred diagnostic states because HazRob 0.10.1 has no direct ADC measurement of the dedicated 5 V sensor rail.

The application is therefore allowed to say, for example:
- "suspected sensor power / ground / AO path";
- "check VDD/GND, AO and I2C";

but it must not claim "sensor supply voltage is low" as a confirmed cause solely because MQ values are zero or invalid.

A future hardware revision may add a divided sensor-rail voltage input. Only then may a measured rail voltage and threshold produce a confirmed low-voltage diagnosis.

## Per-channel live UI behavior
Each MQ card now follows this order:
1. disconnected transport -> no live data;
2. MQ power OFF -> explicit MQ-power state;
3. assigned ADS unavailable -> explicit ADS address/connection diagnosis;
4. ADS available but channel invalid -> AO/power/GND diagnostic, with low-voltage wording kept as unconfirmed;
5. valid channel -> instantaneous Raw / Vadc / Vao / Rs / Rs-R0 remains visible even before a session is started;
6. warm-up affects interpretation/readiness, not visibility of the instantaneous live reading.

The first few seconds after power-on are treated as `waiting_samples` rather than immediately reporting a power fault.

## Session semantics
The timed session remains a recording/aggregation operation only:
- starting a session does not switch on the underlying live acquisition mechanism;
- cancelling or completing a session does not switch off live acquisition;
- a session error does not invalidate otherwise valid instantaneous live telemetry;
- `no_valid_samples` refers to the session aggregation window and the UI directs the operator to the separate live-acquisition diagnostic for the probable hardware path issue.

## Protocol normalization
`RobotProtocolV4` now produces a normalized `sensor_power` object alongside the existing independent `mq` and `measurement` objects.

Normalized `sensor_power` fields include:
- `code`;
- `severity`;
- `message_ar`;
- `valid_channels`;
- `available_channels`;
- `expected_channels`;
- `power_cause_confirmed`;
- `diagnostic_basis = inferred`;
- `live_independent_of_session = true`.

This normalization applies identically to Direct and Remote because both transports feed the same Android protocol-normalization layer.

## Report consistency
The analytical PDF summary now records the live MQ diagnostic code and valid/available channel count, and explicitly marks the power diagnosis as inferred unless a future direct rail-voltage measurement proves otherwise.

Raw saved payloads retain the complete normalized `sensor_power` object. Existing scientific rules remain unchanged: an electrical diagnostic does not promote an uncalibrated MQ response to a quantitative occupational-exposure conclusion.

## Automated tests added
`LiveSensorDiagnosticTest` locks the following behavior:
- live MQ acquisition remains `live_ok` even when the independent session state is `error`;
- both ADS converters present with zero valid MQ channels produces only `suspected_sensor_power_or_ground`, not a confirmed low-voltage claim;
- the initial power-on interval produces `waiting_samples` before declaring a signal fault;
- one missing ADS does not suppress valid live channels from the other ADS.

## Cross-system compatibility review
Comparison from the HR-0101-040 executable source (`9ed5582a55865ac2ef2aa506324ce3a9fbab7a81`) to the HR-0101-041 executable source (`0846be98e4e5b3a92b8983537c9a574396cb4435`) confirms no changes to:
- main ESP32 firmware source;
- ESP32-CAM firmware source;
- GPIO allocation;
- I2C addresses or electrical contract;
- MQTT topics / robot ID / broker coordinates;
- Render bootstrap/video-relay source;
- HiveMQ authentication or ACL configuration.

Changed executable/test areas are limited to Android normalization, live UI diagnostics, report summary, the new diagnostic engine, and its tests.

Both exact-source builders executed `tools/audit_0101_integration_contract.py` on SHA `0846be98e4e5b3a92b8983537c9a574396cb4435` and returned:

`HazRob 0.10.1 cross-component integration contract: PASS`

## Android exact-source verification — PASS
Builder: `hazrob-0101-build`
Deploy: `dep-dairsenqj5pc73bb9ad0`
Source: `0846be98e4e5b3a92b8983537c9a574396cb4435`

Results:
- cross-component integration contract: PASS;
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, including HR-0101-041 tests;
- `:app:assembleRelease`: BUILD SUCCESSFUL;
- versionName: `0.10.1`;
- versionCode: `21`;
- APK SHA-256: `709bdeb585bb18ca0354a2659850a5c514b9dc9f4987e7a43613a059b60d94f6`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`;
- deploy status: `live`.

## Firmware exact-source compatibility build — PASS
Builder: `hazrob-0101-firmware-build`
Final deploy: `dep-dairtc5g1s2s738hblvg`
Source: `0846be98e4e5b3a92b8983537c9a574396cb4435`

The first firmware-builder attempt `dep-dairsgjm8hqs73e20d3g` stopped before compilation because cached Arduino CLI configuration already existed (`arduino-cli config init`). This is classified as an infrastructure/cache false negative, not a code failure. The build was repeated with Clear Build Cache and passed.

Final results:
- cross-component integration contract: PASS;
- main ESP32 compile/link: PASS;
- main ESP32 program: **1,087,653 bytes (34%)**;
- main ESP32 global RAM: **50,172 bytes (15%)**, 277,508 bytes remaining;
- ESP32-CAM compile/link: PASS;
- ESP32-CAM program: **1,118,771 bytes (35%)**;
- ESP32-CAM global RAM: **67,836 bytes (20%)**, 259,844 bytes remaining;
- deploy status: `live`.

These firmware sizes are unchanged from HR-0101-040 because HR-0101-041 introduces no firmware executable changes.

## Remote-system impact
HR-0101-041 does not alter Render or HiveMQ contracts. The production relay remains on the previously verified deployment, and the HR-0101-038 authentication/ACL evidence remains applicable. The new `sensor_power` state is derived in Android from the same normalized telemetry received in Direct or Remote mode; no new MQTT command or topic is introduced.

## Physical validation still required
Software/source/build compatibility is proven. The following still require the assembled robot:
- verify continuous Raw/Vadc/Vao/Rs/Rs-R0 updates with no session running;
- verify those readings continue while a session is running and after it completes/cancels;
- verify actual ADS1/ADS2 discovery and MQ channel validity;
- intentionally reduce or disturb sensor power only under a controlled electrical test and confirm the app reports a conservative *suspected* power/ground fault rather than falsely claiming measured low voltage;
- if confirmed low-voltage diagnosis is desired in a later revision, add an approved divided ADC measurement of the sensor 5 V rail.

## Release classification
**HR-0101-041 LIVE/SESSION CONTRACT: PASS**

**ANDROID UNIT TESTS / RELEASE BUILD / SIGNING: PASS**

**CROSS-SYSTEM INTEGRATION AUDIT: PASS**

**MAIN ESP32 COMPATIBILITY BUILD: PASS**

**ESP32-CAM COMPATIBILITY BUILD: PASS**

**REPORT DIAGNOSTIC CONSISTENCY: PASS**

**RENDER/HIVEMQ CONTRACT: UNCHANGED / PREVIOUS LIVE VERIFICATION RETAINED**

**PHYSICAL LIVE-READING / POWER-FAULT VALIDATION: PENDING**

HazRob 0.10.1 HR-0101-041 is saved as the controlled software baseline prepared for the next field test and subsequent modified-file package.