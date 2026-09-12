# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-042

## HR-0101-042 — Environmental-sensor diagnostics, quality-gated MQ fingerprint analysis, measurement UI, and advanced analytical reports

Date: 2026-09-13
Release line: **HazRob 0.10.1**
Active branch: `dev/hazrob-0.10.1`
Status: **IMPLEMENTED IN SOURCE / EXECUTABLE VERIFICATION PENDING**

> This document is intentionally created before final executable verification. No PASS classification shall be issued until the Android tests/build, main-ESP32 compile, integration audit, and report-contract review have completed on one exact source SHA.

## 1. Field triggers

The controlled change addresses three linked field requirements rather than treating them as independent UI patches:

1. The BME280/BMP280 chamber environmental sensor did not produce temperature/humidity/pressure values in the recorded field session even though both ADS1115 devices were operational on the same I2C bus and the LV-side SDA/SCL level was measured at approximately 3.37 V.
2. The chemical-analysis layer required a scientifically safer use of the eight-MQ fingerprint, logarithmic/power-law response curves, calibration provenance, and the existing 76/144 compound knowledge structures.
3. The Measurements/Analysis workspace and exported session reports required the same quality gates and provenance as the underlying analytical engine.

The corrections are implemented as one integration change so that Firmware -> API4 normalization -> Android analysis -> UI -> saved analytical JSON -> PDF/XLSX all preserve the same interpretation.

## 2. Version and compatibility decision

- Application version remains `0.10.1` / versionCode `21`.
- Main firmware remains `HazRob 0.10.1` / API4.
- No GPIO allocation is changed.
- No ADS1115 address/channel allocation is changed.
- No MQTT topic, robot identity, HiveMQ broker coordinate, Render video path, bootstrap role, camera endpoint, or drive-control semantic is intentionally changed.
- Existing HR-0101-040 manual refresh behavior is retained.
- Existing HR-0101-041 live-MQ / timed-session separation is retained.

## 3. Environmental sensor diagnostic extension

### Existing contract retained

The chamber environmental sensor remains on the common I2C bus:
- SDA = GPIO21;
- SCL = GPIO22;
- supported environmental addresses = `0x76` / `0x77`;
- BME280 chip ID = `0x60`;
- supported BMP280 IDs remain `0x56`, `0x57`, `0x58`;
- the approximately three-second hot-reprobe behavior introduced before HR-0101-042 is retained.

HR-0101-042 does **not** introduce a separate SDA/SCL pair and does not attribute the observed field failure to sharing the bus with ADS1115.

### New diagnostic states

Firmware now distinguishes the environmental failure path instead of collapsing it into `OFF/NONE`:
- `searching`;
- `no_ack`;
- `chip_id_read_failed`;
- `unsupported_chip_id`;
- `calibration_read_failed`;
- `configuration_failed`;
- `initial_read_failed`;
- `read_failed`;
- `invalid_measurement`;
- `runtime_read_failed_reprobe`;
- `ok`.

Additional telemetry records:
- ACK state at `0x76`;
- ACK state at `0x77`;
- current and last responding address;
- current and last chip ID;
- consecutive read-failure count;
- age of the last successful environmental read;
- reprobe interval.

If a previously operational BME/BMP subsequently fails repeated runtime reads, firmware invalidates the environmental readings and returns to the reprobe path rather than indefinitely presenting stale data.

### Android normalization

`RobotProtocolV4` carries the diagnostic state into the normalized `env` object, including `status_code`, I2C ACK evidence, address/chip identity, failure count, and reprobe interval. This is additive to API4 and keeps existing temperature/humidity/pressure fields compatible.

## 4. Chemical-analysis quality model

### Scientific boundary

The installed eight-MQ array is treated as a **cross-sensitive electronic-nose screening array**. HR-0101-042 does not claim that eight MQ sensors are 144 selective chemical analyzers.

The engine may rank candidate identities from the evidence library, but:
- `UNKNOWN` and `AMBIGUOUS` are valid outputs;
- a leading candidate is a screening/fingerprint candidate, not definitive laboratory identification;
- quantitative PPM remains gated by accepted device/reference calibration;
- an uncalibrated generic/digitized curve cannot become an occupational-exposure conclusion merely because it fits the current fingerprint.

### Single-sensor agreement correction

Previously, a chemical supported by one contributor could appear with 100% agreement because no conflicting contributor existed. This is scientifically misleading.

HR-0101-042 therefore defines:
- one contributor -> inter-sensor Agreement = `0%` / not defined for UI/report purposes;
- at least two accepted contributors -> Agreement is calculated from dispersion in log10 concentration space.

The UI and reports explicitly identify single-contributor Agreement as unavailable rather than displaying 100%.

### New quality dimensions

Each evidence-bearing candidate now records independently:
- `agreement`;
- `coverage_pct`;
- `evidence_quality_pct`;
- `candidate_score_pct`;
- number of contributors;
- primary-target contributors;
- device-calibrated contributors;
- manufacturer cross-response contributors;
- community/research cross-response contributors;
- rejected contributors;
- quantitative/non-quantitative status;
- confidence tier;
- evidence note.

Identity ranking is driven by analytical evidence quality rather than hazard severity. Hazard/exposure interpretation remains a separate operation and is unavailable when the result is not quantitatively validated.

### Initial decision gate

`AnalysisDecision` introduces three screening outcomes:
- `UNKNOWN`;
- `AMBIGUOUS`;
- `CANDIDATE`.

The current software thresholds are deliberately classified as **initial engineering thresholds**, not universally validated chemical-identification limits. They shall be tuned against device-specific clean-air, controlled challenge, and reference-method datasets.

Current gate logic:
- fewer than two valid MQ channels -> `UNKNOWN`;
- top evidence has fewer than two contributors -> `UNKNOWN`;
- top score below the initial low-evidence threshold -> `UNKNOWN`;
- insufficient top/runner separation -> `AMBIGUOUS`;
- sufficiently strong and separated multi-sensor result -> `CANDIDATE`.

The saved analysis records the top score, runner-up score and separation margin for reproducibility.

## 5. Curves, logarithms, and calibration contract

The existing curve infrastructure remains compatible with manufacturer/research power-law and log-log response representations. The scientific relation remains conceptually equivalent to:

`log10(Rs/R0) = m * log10(C) + c`

or a provenance-equivalent power-law representation.

Rules retained/enforced:
- no unsupported extrapolation outside a declared calibration/curve domain;
- device-specific reference calibration has higher evidence weight than generic inherited curves;
- manufacturer target evidence is stronger than manufacturer cross-response;
- community/digitized research fits are fingerprint evidence unless validated on the HazRob device;
- MQ7/MQ9 heater-cycle limitations remain explicit;
- default firmware `R0 = 10 kOhm` values remain placeholders and are not promoted to calibrated values.

### Clean-air baseline next step

No new R0 values are fabricated by HR-0101-042.

The operator will perform a clean-air run after flashing the verified build. The resulting report will be used to establish the device-specific clean-air fingerprint/baseline. R0 conversion will then follow the documented sensor-specific clean-air ratio/reference-gas semantics instead of assuming `R0 = Rs_air` for all sensors.

## 6. Measurement / Analysis UI

### Physical/environment section

The environmental cards now expose actionable causes, for example:
- no ACK at either environmental address;
- address ACK exists but chip ID is unsupported;
- chip-ID read failure;
- calibration-register read failure;
- configuration-register failure;
- runtime read failure with automatic reprobe.

### Live MQ section

The existing independent live acquisition remains visible before/during/after timed sessions. The live summary additionally shows:
- valid/available MQ channels;
- count of channels currently classified as unstable or drift-warning by the existing `SensorHealthEngine`;
- per-channel R0 calibrated/not-calibrated state.

### Analysis section

The analysis workspace now shows:
- session/snapshot decision (`UNKNOWN`, `AMBIGUOUS`, leading screening candidate);
- valid MQ channels out of 8;
- calibrated R0 channels out of 8;
- environmental-data availability;
- top candidate score;
- separation margin;
- ranked chemical candidates;
- formula and CAS when present;
- candidate score;
- coverage;
- evidence quality;
- agreement or single-contributor N/A;
- fingerprint vs quantitative mode;
- contributor count and evidence label.

## 7. Session-bound analytical JSON

The saved analytical payload is versioned as:

`mq-fingerprint-v2-hr042`

Candidate rows are deterministically sorted before persistence so that UI, Raw export, PDF and XLSX can reproduce the same ranking.

The top-level quality object records:
- decision outcome;
- valid-channel count;
- calibrated-R0 count;
- environmental availability;
- quantitative gate state;
- top score;
- runner-up score;
- separation margin;
- top chemical ID;
- top contributor count;
- explanatory note.

The existing target catalogue remains 144 compounds. Presence in the catalogue is not equivalent to a validated quantitative curve.

## 8. Reports

### PDF

The PDF now includes an Advanced MQ Fingerprint Analysis page containing:
- algorithm/version identity;
- screening outcome;
- valid channels;
- calibrated R0 channels;
- environmental availability;
- quantitative gate state;
- top and runner-up scores;
- separation margin;
- ENV diagnostic status;
- ranked candidate table with Score / Coverage / Evidence / Agreement / contributor count.

The PDF states that Agreement is not defined for one contributing sensor and must not be displayed as 100%.

### XLSX

The workbook now extends:
- Summary: analysis version/outcome, valid MQ, calibrated R0, top/runner/margin, quantitative gate and ENV status;
- MQ: R0 value, calibration flag and R0 source;
- Analysis: quality-decision block plus ranked candidate table;
- Hardware/status: ENV address, chip ID, ACK evidence and failure count;
- time-series and chart sheets are retained.

## 9. Automated tests added/updated

The source contains tests intended to lock:
- a single sensor cannot produce 100% inter-sensor Agreement;
- identity ranking is evidence/score based rather than hazard based;
- unvalidated PPM/risk cannot become quantitative;
- close top candidates produce `AMBIGUOUS`;
- insufficient channel count produces `UNKNOWN`;
- a sufficiently strong/separated multi-sensor result can pass the screening candidate gate;
- environmental failure cause and I2C evidence survive API4 normalization;
- measurement countdown/error normalization remains intact.

## 10. GitHub Actions infrastructure observation

The repository GitHub Actions `build-apk` job currently terminates before exposing executable steps/logs. The same behavior is present on the previously verified HR-0101-041 executable baseline, which subsequently passed independent Render Android and firmware builders.

Therefore the no-step GitHub Actions failure is classified as a CI-channel/infrastructure condition and **not** as evidence that HR-0101-042 compiles successfully or unsuccessfully.

Final executable classification remains pending independent builders.

## 11. Required executable verification before PASS

The exact final source SHA must complete:

### Android
- cross-component integration audit;
- `:app:testDebugUnitTest`;
- `:app:assembleRelease`;
- versionName/versionCode confirmation;
- release APK hash/signing identity recording.

### Main ESP32
- cross-component integration audit;
- compile/link for main ESP32;
- program and global-RAM footprint recording.

### ESP32-CAM
- compatibility compile/link because the release is shipped as one controlled package, even though HR-0101-042 does not intentionally change camera firmware.

### Report contract
- compile-time consistency of PDF/XLSX exporters;
- preservation of raw analytical provenance;
- no unsupported quantitative exposure wording.

## 12. Required physical validation after executable verification

The assembled robot must then verify:
1. ADS1 at `0x48` and ADS2 at `0x49` remain operational;
2. ENV diagnostic identifies whether `0x76` or `0x77` ACKs;
3. a BME280 should report chip ID `0x60`; a supported BMP280 should report its supported ID;
4. temperature/humidity/pressure values update when the device is valid;
5. disconnect/reconnect or induced read failure returns through the reprobe path without reboot;
6. eight MQ live channels remain independent from the timed session;
7. a complete timed analytical session saves ranked quality metrics and exports matching PDF/XLSX/Raw data;
8. a clean-air stable session is recorded and uploaded for the next controlled baseline/R0 stage.

## 13. Current classification

**SOURCE IMPLEMENTATION: COMPLETE FOR CURRENT HR-0101-042 SCOPE**

**ANDROID EXECUTABLE TEST/BUILD: PENDING**

**MAIN ESP32 COMPILE: PENDING**

**ESP32-CAM COMPATIBILITY COMPILE: PENDING**

**CROSS-COMPONENT AUDIT: PENDING ON FINAL EXACT SHA**

**PHYSICAL ENV VALIDATION: PENDING**

**CLEAN-AIR DEVICE BASELINE: PENDING**

**DEVICE-SPECIFIC R0 CALIBRATION: PENDING — NO VALUES FABRICATED**

**144-COMPOUND DEFINITIVE IDENTIFICATION CLAIM: NOT MADE**

HR-0101-042 must remain in this pending state until executable verification and the required physical test evidence are recorded.
