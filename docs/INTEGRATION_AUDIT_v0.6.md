# HazRob 0.6 Integration Audit

Date: 2026-08-31
Branch: `dev/hazrob-0.6`
Application ID: `com.servoc.app`
Version: `0.6.0` (`versionCode 7`)

## 1. Release purpose

HazRob 0.6 is the migration release from the older Servoc-oriented ESP32/application contract to the compact HazRob master firmware API v3. The release also separates normal operator workflow from developer-only chemical experiments and removes the structural 144-compound limit from the chemical knowledge database.

This audit covers the **core measurement/control release**. Optional local camera recording and experimental flame evidence are present but isolated from MQ measurement records and are not release-gating analytical functions. 4G/VPN, autonomous gas-source localization, autonomous return-on-link-loss and direct authenticated GitHub publishing remain deferred.

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

The master does **not** perform gas identification, PPM/exposure analysis, video processing or cloud transport.

Arduino IDE verification reported for the current four-tab firmware:
- program storage: 1,012,743 bytes / 1,310,720 bytes (77%);
- dynamic-memory globals: 49,564 bytes / 327,680 bytes (15%);
- available local-variable memory: 278,116 bytes.

### ESP32-CAM

The camera remains independent. The master stores the camera host address but does not proxy video. Android controls camera stream/flash directly. Optional recording and visual flame evidence use a separate camera evidence path; neither is attached to MQ measurement records or used in compound/PPM/exposure computation.

### Android

Android owns UI, protocol normalization, chemical analytics, developer experiments, scientific/reference data, local persistence and reporting.

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

`RobotProtocolV3` normalizes the compact `/diag` + `/data` payloads into the application snapshot. UI/analytics do not depend directly on compact wire names.

Firmware measurement state mapping:
- `0` -> `idle`
- `1` -> `warmup`
- `2` -> `measuring`
- `3` -> `ready`
- `4` -> `error`

Protocol regression tests pass in CI.

## 4. Roles and UI

Startup requires role selection.

Normal user:
- Control
- Measurement & Analysis
- History & Reports
- Settings

Developer:
- all normal-user tabs;
- Experiments & Calibration.

Developer PIN for the current prototype workflow is `1166`. It is session-only and is explicitly treated as a UI/workflow barrier, not as protection for secrets or repository credentials.

The previous separate sensor/chemical/measurement workflows are consolidated into one Measurement & Analysis tab.

## 5. Chemical knowledge database

SQLite database schema version: `7`.

The knowledge model is no longer structurally limited to 144 entries. Legacy IDs are retained only for compatibility with the original analytical models.

Bundled pinned official datasets:

### NIOSH chemical identity/CAS snapshot
- source records: 652;
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

OSHA importer excludes non-OSHA comparison columns such as NIOSH REL, Cal/OSHA PEL and ACGIH TLV rather than mixing authorities.

Exposure matching prioritizes CAS/identifier evidence and only falls back to an unambiguous exact name match.

## 6. Analytical validity policy

Chemical knowledge coverage and MQ analytical coverage are separate concepts.

The installed direct MQ channels are:
- MQ-2
- MQ-3
- MQ-4
- MQ-7
- MQ-8
- MQ-135

MQ-5 and MQ-9 remain library expansion slots only.

Legacy/default `R0=10` values are **not** accepted as proof of physical reference-gas calibration. `ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ` is disabled. A generic/default-R0 path cannot produce authoritative quantitative screening or exposure risk.

Quantitative PPM requires a valid sensor/compound calibration path and domain checks. Otherwise results remain identification/fingerprint/research evidence.

## 7. Developer experiments

Reference experiments are hidden from normal users.

The developer can identify the reference chemical by:
- dynamic database UID;
- CAS number;
- exact chemical name/synonym.

Developer reference capture is therefore no longer restricted to legacy IDs 1-144. New captures store the dynamic `chemical_uid`; legacy ID is retained only when the selected record has one. Existing capture rows are migrated non-destructively.

Developer functions include:
- reference MQ cycle;
- known-reference concentration capture;
- environmental context capture;
- clean-air baseline capture;
- drift check;
- local approval;
- reference-bank JSON export with SHA-256;
- PDF/XLSX export.

Direct authenticated publication to GitHub is deferred; no long-lived GitHub token is embedded in the APK.

## 8. Sensor health and time-series

The Android layer records measurement telemetry and supports transient features/sensor health including baseline, noise, saturation/stuck/unstable states and clean-air drift comparison.

These functions support experimental validation; they do not convert an uncalibrated MQ channel into a compliant quantitative instrument.

## 9. Reports and camera independence

Normal users can save completed READY measurements and export PDF/XLSX reports. Developer reference approval/export controls are intentionally absent from the normal-user reports tab.

Camera recordings and experimental flame evidence have an independent lifecycle and are not embedded into measurement reports or chemical analytics in 0.6.

## 10. CI/release verification

Validated workflow run:
- GitHub Actions run: `33382885466` (Build HazRob APK #174)
- tested head commit: `3feb5ea94dd27b301121332a99135548799713c5`
- scientific dataset validation: PASS
- analytics/protocol unit tests: PASS
- signed release APK build: PASS
- signing-certificate verification: PASS
- artifact upload: PASS

Artifact:
- name: `HazRob-v0.6.0-prototype`
- artifact ID: `9754478767`
- artifact ZIP digest: `dffcf12f65a4258da30994fc58458546456b9d0d98dcfcca70accbd8aad1d29e`
- release APK SHA-256: `071cf7c65dbc2eb0cb256ebdc570a34eb57de373e90f96dee501063f6b6b07cb`

Signing certificate SHA-256:
`D6:B5:B6:9B:E3:82:F2:AF:A6:DF:4B:46:BA:DE:5B:6F:86:4F:40:60:62:2E:94:90:75:29:3A:C6:20:03:D3:F0`

APK signature scheme v2 verifies successfully.

## 11. Remaining hardware validation before release promotion

CI proves compilation, unit-test and signing integrity, not physical sensor calibration or robot field safety. Before promoting the branch to a conference/demo release, perform the hardware acceptance sequence:

1. confirm `Servoc` AP and `192.168.4.1`;
2. verify `/diag`, `/data`, `/cfg` against the physical master;
3. verify ESP32-CAM stream and flash remain unchanged;
4. verify BME/BMP detection at the physical I2C address;
5. verify relay active-low polarity and exclusive groups;
6. verify all six installed MQ channels and measurement READY state;
7. verify immediate STOP and 1000 ms link-loss drive failsafe with wheels safely unloaded;
8. install the signed 0.6 APK over the previous signed build and verify database migration/preservation;
9. perform one normal-user measurement/export and one developer reference-capture/export.

No claim of compliance-grade quantitative gas measurement is made until the physical sensor chain is calibrated and independently validated for the claimed analytes and operating domains.
