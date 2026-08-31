# HazRob 0.6 Architecture

HazRob 0.6 separates deterministic robot control from computationally intensive analysis.

## System boundary

**Master ESP32** owns real-time actuator I/O, local drive fail-safe, acceleration/deceleration/reversal handling, relay exclusivity, direct ADC/I2C acquisition and compact HTTP telemetry. It does not identify gases or calculate exposure risk.

**ESP32-CAM** remains an independent camera subsystem. Android talks to it directly for MJPEG/video and flash control; the master ESP32 does not proxy the stream.

**HazRob Android** is the analytical and operator platform: normalized telemetry, measurement sessions, chemical evidence, calibration/fingerprints, database, media, reports and future on-device vision.

## Android packages

- `auth/` — session role gate (`USER` / `DEVELOPER`).
- `communication/` — robot/camera transport and API-v3 normalization.
- `control/` — joystick/heartbeat; all safety-critical loss-of-command behavior remains local to ESP32.
- `measurement/` — time-series recording, transient features and future sensor-health/drift analysis.
- `navigation/` — motion breadcrumbs and future odometry/source-localization models. 0.6 records commands but does not autonomously replay them.
- `camera/` — ESP32-CAM stream/flash and the expansion point for recording/vision.
- `chemical/` — dynamic chemical knowledge repository plus evidence-weighted analytical models.
- `exposure/` — authority/jurisdiction-aware exposure interpretation.
- `calibration/` — device-specific reference-gas calibration and quality metadata.
- `fingerprint/` — trainable chemical signatures/classifiers.
- `models/` — algorithm registry/runtime status, including the pinned MQDataScience 76-model catalogue.
- `analytics/` — evidence-fusion and result snapshots.
- `data/` — offline-first SQLite storage for measurements, experiments, scientific sources, media/event metadata and reference publication queue.
- `report/` — PDF/XLSX/reference-bank exports.
- `ui/` — control, unified measurement/analysis, reports, developer laboratory and settings.

## API v3 boundary

The ESP32 wire contract is deliberately compact:

- `/diag` — firmware/API/heap/client/failsafe/environment-hardware diagnostics.
- `/data` — environment values, relay group, live MQ values and measurement averages.

`RobotProtocolV3` is the only compatibility/normalization layer. UI and analysis consume the normalized application snapshot rather than compact wire keys.

## User roles

Normal users see control, unified measurement/analysis, reports and settings. Developer mode requires the session PIN and additionally exposes experiments/calibration/reference-bank tooling. The PIN is a UI/workflow barrier only and is never used to protect repository credentials or cryptographic material.

## Chemical knowledge versus analytical coverage

The database is no longer structurally limited to 144 entries. Chemical identity/hazard/exposure records can grow independently. A database entry does not imply sensor support. Analytical results explicitly distinguish validated quantitative calibration from identification/fingerprint/research-only evidence.

Current legacy/default MQ `R0` values are not treated as proof of physical reference-gas calibration. Generic manufacturer/research curves may contribute identification evidence but do not by themselves authorize quantitative PPM or exposure-risk output.

## Offline-first rule

Robot operation, measurement capture, local analysis and report generation must remain functional without Internet access. External scientific-source synchronization, cloud backup and GitHub reference publication are optional layers and must not sit in the drive-control path.

## Expansion points

The v0.6 schema reserves clean boundaries for larger chemical/exposure datasets, media recording, flame/smoke inference, thermal sensing, MUX expansion, ESP-NOW nodes, 4G/VPN transport, gas-plume/source localization and safe return-to-link functions. These capabilities are added only through their owning layer rather than by inflating the ESP32 firmware.
