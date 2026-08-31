# HazRob Android

HazRob is the Android operator and analytical application for the remote hazard-inspection robot.

## HazRob 0.6 core

- Android application: `com.servoc.app`, versionCode `7`, versionName `0.6.0`.
- Master ESP32 firmware API v3 is kept small and deterministic: drive, STOP/failsafe, relay control, MQ acquisition, BME/BMP acquisition, measurement sequencing and settings.
- ESP32-CAM remains an independent camera/flash subsystem; video is not coupled to measurement records in the 0.6 core.
- Session role gate: normal `USER` mode and PIN-protected `DEVELOPER` mode. Developer tools are hidden from normal users.
- One unified **Measurement & Analysis** workflow replaces separate sensor/chemical/measurement tabs.
- Offline-first SQLite knowledge database is no longer structurally limited to 144 chemicals.
- Bundled pinned scientific datasets currently include 652 NIOSH chemical identity/CAS records, 402 NIOSH IDLH records and 638 OSHA Table Z-1 exposure-limit records, each with source provenance and SHA-256 manifests.
- Developer reference captures accept dynamic database UID, CAS or exact chemical name rather than being limited to legacy IDs 1–144.
- MQ time-series/feature extraction and sensor-health/drift support are retained for developer validation workflows.
- PDF/XLSX measurement export and developer reference-bank JSON export remain local/offline.

## Analytical validity

A chemical record in the knowledge database does **not** imply that the installed MQ array can identify or quantify that substance. Analytical coverage is tracked separately from chemical/hazard knowledge.

Legacy/default MQ `R0=10` values are not treated as physical reference-gas calibration. Quantitative PPM and exposure-risk interpretation are permitted only when a sensor/compound path has suitable validated calibration and the reading lies within its stated domain. Otherwise HazRob reports identification/fingerprint/research evidence without fabricated PPM.

## Current robot sensors

Installed direct MQ channels: MQ-2, MQ-3, MQ-4, MQ-7, MQ-8 and MQ-135. MQ-5 and MQ-9 remain library expansion slots. BME280/BMP280 environmental acquisition is direct I²C and independent of MQ relay switching.

## Firmware source of truth

The current compact master firmware is stored under `firmware/HazRob/` as four Arduino tabs:

- `HazRob.ino`
- `Config.h`
- `Hardware.h`
- `Hardware.cpp`

The local Wi-Fi SSID remains `Servoc` for compatibility with the existing ESP32-CAM setup. The project/application identity remains HazRob.

## Scientific references

Scientific-library design, source provenance and the wider research bibliography are documented in `docs/SCIENTIFIC_LIBRARY_v0.6.md`, `docs/GAS_MODEL_RESEARCH.md` and `REFERENCES_APA.md`.

## Deferred beyond the 0.6 core

Video recording, flame/smoke AI, 4G/VPN transport, autonomous gas-source tracking, autonomous return-on-link-loss, thermal sensing and direct GitHub publishing are separate development layers. They are intentionally not placed in the measurement/control critical path of this core release.
