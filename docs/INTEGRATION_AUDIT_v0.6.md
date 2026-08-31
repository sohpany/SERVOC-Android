# HazRob 0.6 Integration Audit

Date: 2026-08-31
Branch: `dev/hazrob-0.6`
Application ID: `com.servoc.app`
Version: `0.6.0` (`versionCode 7`)
Status: **software release candidate; physical-system acceptance pending**

## 1. Release purpose and acceptance boundary

HazRob 0.6 is the migration release from the older Servoc-oriented ESP32/application contract to the compact HazRob master firmware API v3. It also moves computationally intensive analysis/evidence handling to Android, separates normal operator workflow from developer-only experiments, removes the structural 144-compound limit from the chemical knowledge database, and establishes time-linked sensor/media evidence infrastructure.

The software acceptance path includes protocol normalization, role separation, unified measurement workflow, dynamic chemical/exposure storage, scientific dataset integrity, analytical validity guards, time-series/sensor-health tooling, phone-side video recording, experimental flame evidence, advisory gas-direction estimation, reports, immutable reference-bank export and optional direct publication of approved developer records to the dedicated GitHub reference branch.

The release does **not** claim hardware certification, intrinsically safe/explosion-protected construction, compliance-grade gas quantification, autonomous plume following or autonomous return on link loss. Those require separate physical validation/design controls.

## 2. System boundary

### Master ESP32

Source of truth: `firmware/HazRob/`

Four Arduino tabs only:
- `HazRob.ino`
- `Config.h`
- `Hardware.h`
- `Hardware.cpp`

Master responsibilities:
- differential servo command generation;
- acceleration, deceleration and reverse braking;
- immediate STOP and 1000 ms local command-loss failsafe;
- exclusive active-low MQ relay groups;
- six installed MQ ADC channels;
- direct BME280/BMP280 I2C acquisition;
- measurement warm-up/acquisition sequencing;
- NVS settings;
- compact HTTP API v3.

The master does **not** perform gas identification, PPM/exposure analysis, video processing or cloud/repository transport.

Arduino IDE verification reported for the current four-tab firmware:
- program storage: 1,012,743 bytes / 1,310,720 bytes (77%);
- dynamic-memory globals: 49,564 bytes / 327,680 bytes (15%);
- available local-variable memory: 278,116 bytes.

### ESP32-CAM

The camera remains independent. The master stores the camera host address but does not proxy video. Android controls camera stream/flash directly. A single Android-side MJPEG path can feed display, optional H.264/MP4 recording and experimental flame evidence without changing the frozen ESP32-CAM imaging configuration.

### Android

Android owns UI, protocol normalization, chemical analytics, scientific/reference data, developer experiments/calibration, local persistence, time-series processing, sensor health, media evidence, experimental vision, gas-direction assistance, reporting and optional GitHub reference publication.

## 3. Firmware/API compatibility

Master AP identity remains `Servoc` for compatibility with the existing ESP32-CAM network configuration. Master default IP is `192.168.4.1`; camera default IP is `192.168.4.2`.

API v3 endpoints retained by Android:
- `/drive?l=&r=`
- `/stop`
- `/relay?g=`
- `/measure?cmd=start|cancel`
- `/data`
- `/diag`
- `/cfg`

`RobotProtocolV3` normalizes compact `/diag` + `/data` payloads into the application snapshot. UI and analytics do not depend directly on compact wire names.

Firmware measurement state mapping:
- `0` -> `idle`
- `1` -> `warmup`
- `2` -> `measuring`
- `3` -> `ready`
- `4` -> `error`

Protocol regression tests pass in CI using a real JVM `org.json` implementation supplied only to unit tests; runtime Android continues to use the platform implementation.

## 4. Roles and UI separation

Startup requires role selection.

Normal user:
- Control
- Measurement & Analysis
- History & Reports
- Settings

Developer:
- all normal-user tabs;
- Experiments & Calibration.

Developer PIN for the current prototype workflow is `1166`. It is session-only and is explicitly treated as a UI/workflow barrier, not as cryptographic protection. The Android manifest exports only `MainActivity`; the developer laboratory is not exposed as an independently launchable activity.

The previous separate sensor/chemical/measurement workflows are consolidated into one Measurement & Analysis tab. The obsolete v0.5 `SensorsTab`, `ChemicalsTab` and `PhysicalTab` source files were removed before the final validation build, and CI confirmed there is no hidden runtime dependency on them.

## 5. Chemical knowledge and occupational-exposure data

SQLite database schema version: `7`.

The knowledge model is no longer structurally limited to 144 entries. Legacy IDs are retained only where compatibility with original analytical models is required. Dynamic chemical records can be addressed by database UID, CAS or exact name/synonym.

Bundled pinned official datasets:

### NIOSH chemical identity/CAS snapshot
- records: 652;
- dataset SHA-256: `7498bd3d8037dd77fb49d3a957d8856ea9f37e48dbc0df3020799e422967dc69`;
- source SHA-256: `f39fbc4fcfcdfe84c331fcdb1f637e1c8a2a1ed3cb8e7c597f6a23844fcf2f52`.

### NIOSH IDLH snapshot
- records: 402;
- dataset SHA-256: `9a4f4d06c493c0c539dd5b9926449975b844a34f82451d6ec792ac952b903151`;
- source SHA-256: `5de043036d2c21e59b8f75cbe6f994430fc8b39c54a813adbad9e00def2bf3eb`.

### OSHA Table Z-1 snapshot
- source substance rows: 432;
- normalized limit records: 638;
- dataset SHA-256: `4dc62e78ef823071c9a998b35730acfe14d11a6a77a2975f3b762f2301c588fd`;
- source SHA-256: `4da220e066c07da7bb2cdb6181325a1ea5444a1c2b716ede8c5c0262d8a80599`.

The OSHA importer excludes non-OSHA comparison columns such as NIOSH REL, Cal/OSHA PEL and ACGIH TLV instead of mixing authorities. Exposure matching prioritizes CAS/identifier evidence and only falls back to an unambiguous exact-name match.

The schema and scientific registry are prepared for additional authorities/sources, but the list above is the pinned exposure/identity content bundled and verified by the current 0.6 CI path; future NIOSH REL/EU/NIST/PubChem enrichment must retain separate provenance/version records.

## 6. Analytical validity policy

Chemical knowledge coverage and MQ analytical coverage are separate concepts.

Installed direct MQ channels:
- MQ-2
- MQ-3
- MQ-4
- MQ-7
- MQ-8
- MQ-135

MQ-5 and MQ-9 remain library expansion slots only.

Legacy/default `R0=10` values are **not** accepted as proof of physical reference-gas calibration. `ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ` is disabled. A generic/default-R0 path cannot produce authoritative quantitative screening or exposure risk.

Quantitative PPM requires a valid sensor/compound calibration path plus domain checks. Otherwise results remain identification/fingerprint/research evidence. No model becomes quantitative solely because several algorithms agree.

The pinned MQDataScience catalogue remains a registry of 76 algorithms/models; registration, Android implementation and validation are separate states.

## 7. Developer experiments and Reference Bank

Reference experiments are hidden from normal users.

The developer can identify the reference chemical by:
- dynamic database UID;
- CAS number;
- exact chemical name/synonym.

Reference capture is therefore no longer restricted to legacy IDs 1-144. New captures store the dynamic `chemical_uid`; a legacy ID is retained only when the selected record has one. Existing capture rows migrate non-destructively.

Developer functions include:
- reference MQ cycle;
- known-reference concentration capture;
- environmental context capture;
- clean-air baseline capture;
- drift check;
- local approval;
- immutable reference-bank JSON export with SHA-256;
- PDF/XLSX export;
- optional direct GitHub publication of an already APPROVED record.

Direct publication targets branch `reference-bank` under `reference_bank/approved_experiments/`. The developer supplies a fine-grained repository credential for the current screen/session only; HazRob does not embed or persist that credential. Publication filenames are canonical and derived from immutable experiment ID/timestamp rather than user-controlled file text. The GitHub create request intentionally omits an existing-file SHA, so a duplicate canonical path is rejected instead of silently overwriting an approved record. Unit tests cover canonical naming, credential-free request payloads and path-traversal rejection.

The `reference-bank` branch exists in the repository. End-to-end publication using a developer credential remains a physical/user acceptance test because CI deliberately has read-only repository permissions and does not inject a write credential into the APK test path.

## 8. Sensor time-series, transient features and health

Android records measurement telemetry and supports transient/sensor-health evidence including baseline, peak/delta, slope, T50/T90, integrated response, noise, saturation/stuck/unstable states and clean-air drift comparison.

These functions support model development and validation; they do not convert an uncalibrated MQ channel into a compliant quantitative instrument.

## 9. Media evidence and visual flame analysis

The existing ESP32-CAM remains the source. Android can:
- display the MJPEG stream;
- control camera flash through the camera API;
- encode the received stream to H.264/MP4 on the phone;
- retain frame/timing evidence;
- export finalized recordings to the Android media store;
- run experimental temporal visual-flame evidence analysis and store event snapshots/metadata.

When media capture occurs during an active MQ measurement session, the evidence layer reuses that active `session_id` so video/flame evidence can later be correlated with environmental/MQ/motion samples. Outside an active measurement, it creates an independent media evidence session. This time correlation does **not** allow visual evidence to validate chemical PPM or exposure calculations.

Visual-flame analysis is experimental/assistive in 0.6 and does not issue drive commands or safety trips.

## 10. Motion history and gas-source direction assistance

0.6 stores motion-command breadcrumbs and synchronized analytical evidence. The gas-direction estimator can use movement history plus compound-relevant MQ response changes to estimate a relative direction/bearing for a selected compound.

This result is advisory only. It is not odometry and is never converted into autonomous drive commands in 0.6. Automatic reverse/backtrack after communication loss remains disabled; the local ESP32 STOP/failsafe remains authoritative.

## 11. CI and release verification

Final executable-source validation workflow:
- GitHub Actions: Build HazRob APK **#215**
- run ID: `33406289224`
- executable-source head commit: `ddd0633c4f9ffde186e40fad85aa60acec13cab7`
- scientific dataset integrity/provenance validation: **PASS**
- analytics/protocol/reference-publication/vision/navigation unit tests: **PASS**
- signed release APK build: **PASS**
- release lint-vital path: **PASS**
- signing-certificate verification: **PASS**
- artifact upload: **PASS**

This audit update follows the validated executable commit and changes documentation only.

Artifact:
- name: `HazRob-v0.6.0-prototype`
- artifact ID: `9763324750`
- artifact size: `821416` bytes (workflow ZIP artifact)
- artifact ZIP digest SHA-256: `35863326f8cbab981c823a93f1043957f3387191d2c0743f27fc348d5a2bfc79`
- release APK SHA-256: `b2c36d30b330ef8ac905fec04ba70759c2cd8373a559226de20d3ba9c4aa5513`

Signing certificate SHA-256:
`D6:B5:B6:9B:E3:82:F2:AF:A6:DF:4B:46:BA:DE:5B:6F:86:4F:40:60:62:2E:94:90:75:29:3A:C6:20:03:D3:F0`

The CI certificate check normalized that value to lowercase/colon-free form and matched it exactly. The release APK signature verifies successfully.

Non-blocking build-tool technical debt observed by CI:
- `WifiRoute.java` uses an Android API marked deprecated; it is retained in 0.6 because it explicitly binds local robot/camera HTTP traffic to Wi-Fi even when cellular data is active, and its lifecycle replacement requires target-device network testing;
- one vision unit test also touches an Android API reported deprecated by the compiler;
- current GitHub Actions dependencies emit Node 20 migration/deprecation warnings, including the already-deprecated `actions/setup-java@v4` action;
- Gradle reports features that should be cleaned before Gradle 10 migration.

None of those warnings caused a unit-test, lint-vital, signing or release-build failure in run #215.

## 12. Remaining physical acceptance before promotion to conference/demo release

CI proves source compilation, deterministic unit-test behavior, dataset integrity and signing continuity. It does not prove physical sensor calibration, camera behavior on the target phone, actuator safety, RF behavior or field generalization of the visual/gas-direction models.

Required acceptance sequence:

1. confirm `Servoc` AP and `192.168.4.1` on the physical master;
2. verify `/diag`, `/data` and `/cfg` against the physical API-v3 ESP32;
3. verify the existing ESP32-CAM stream and flash remain unchanged at the expected camera host;
4. verify BME280/BMP280 detection and actual environmental values at the physical I2C address;
5. verify relay active-low polarity and exclusive relay groups;
6. verify all six installed MQ channels, live values, warm-up/acquisition sequence and `READY` averages;
7. verify immediate STOP and 1000 ms command-loss drive failsafe with wheels safely unloaded/robot restrained;
8. install signed 0.6 over the established signed build and verify schema migration/data preservation;
9. perform a normal USER measurement, save, PDF export and XLSX export;
10. enter DEVELOPER with PIN `1166`, capture one known-reference exposure, approve it and verify immutable local reference JSON/hash;
11. with an intentionally scoped developer credential, publish one approved reference record to `reference-bank` and confirm the stored path/hash/audit record; revoke/discard the credential after the test;
12. record/stop an ESP32-CAM video on the target phone, verify MP4 playback/timing/media-store output and ensure stream/controls remain responsive;
13. trigger controlled non-hazardous visual test scenes to characterize flame-model persistence, false positives and event correlation before any conference claim about fire recognition;
14. verify sensor-health baseline/drift functions using a defined clean-air procedure;
15. validate gas-direction assistance with controlled known-source trajectories before presenting it as more than an experimental direction indicator;
16. perform reference-gas calibration/validation for every analyte for which quantitative PPM is to be claimed, including heater/environment/calibration-domain documentation.

## 13. Standards/certification position

HazRob 0.6 is a research/prototype software release candidate. Its architecture is intentionally compatible with later formal risk analysis, electrical/machinery safety, environmental protection, explosion-protection and laboratory-calibration work, but the current software build must not be represented as certification of the assembled robot.

Before product release, the physical system and claimed measurements require an applicable standards/compliance plan (for example ISO 12100 / IEC 60204-1 machinery/electrical risk controls, IEC 60529 enclosure rating where claimed, IEC 60079-series requirements for explosive atmospheres where applicable, and ISO/IEC 17025-traceable calibration/validation methodology where laboratory-grade quantitative claims are pursued). The exact conformity route depends on intended use, market and final hardware design.
