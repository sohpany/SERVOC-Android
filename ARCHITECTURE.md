# HazRob 0.6 Architecture

HazRob 0.6 separates deterministic robot control from computationally intensive analysis, evidence handling and scientific data management.

## System boundary

**Master ESP32** owns real-time actuator I/O, local drive fail-safe, acceleration/deceleration/reversal handling, relay exclusivity, direct ADC/I2C acquisition and compact HTTP telemetry. It does not identify gases, calculate exposure risk, process video or perform cloud transport.

**ESP32-CAM** remains an independent camera subsystem. Android talks to it directly for stream display and flash control; the master ESP32 does not proxy the stream and its camera configuration remains outside the HazRob master firmware.

**HazRob Android** owns telemetry normalization, operator workflow, chemical analysis, developer experiments/calibration, scientific reference data, local persistence, media evidence, visual analysis, reporting and optional reference-bank publication.

## Android packages

- `auth/` — session role gate (`USER` / `DEVELOPER`).
- `communication/` — robot transport and API-v3 normalization.
- `control/` — joystick/heartbeat; safety-critical command-loss behavior remains local to ESP32.
- `measurement/` — telemetry recording, transient features and sensor-health/drift analysis.
- `navigation/` — motion history plus compound-specific relative gas-direction estimation; no autonomous replay/drive in 0.6.
- `camera/` — ESP32-CAM stream/flash plus H.264/MP4 recording.
- `vision/` — experimental visual-flame evidence and overlays; it does not issue drive or safety commands.
- `chemical/` — dynamic chemical knowledge repository plus evidence-weighted analytical models.
- `exposure/` — authority/jurisdiction-aware exposure data.
- `calibration/` — device-specific reference-gas calibration and quality metadata.
- `fingerprint/` — trainable chemical signatures/classifiers.
- `models/` — algorithm registry/runtime status, including the pinned MQDataScience 76-model catalogue.
- `analytics/` — evidence fusion and result snapshots.
- `data/` — offline-first SQLite storage for measurements, experiments, scientific sources, time-series, media and event evidence.
- `github/` — optional developer-session publication of approved immutable reference records.
- `report/` — PDF/XLSX/reference-bank exports.
- `ui/` — control, unified measurement/analysis, reports, developer laboratory and settings.

## API v3 boundary

The ESP32 wire contract is deliberately compact:

- `/diag` — firmware/API/heap/client/failsafe/environment-hardware diagnostics.
- `/data` — environment values, relay group, live MQ values and measurement averages.

`RobotProtocolV3` is the normalization layer. UI and analytics consume the normalized application snapshot rather than compact wire keys.

## User roles

Normal users see control, unified measurement/analysis, reports and settings. Developer mode requires the session PIN and additionally exposes experiments/calibration/reference-bank tooling. The PIN is a UI/workflow barrier only and is not used to protect repository credentials or cryptographic material. The Android manifest exposes only `MainActivity`; developer laboratory classes are not independently exported activities.

## Chemical knowledge versus analytical coverage

The database is no longer structurally limited to 144 entries. Chemical identity/hazard/exposure records can grow independently. A database entry does not imply sensor support. Analytical results distinguish validated quantitative calibration from identification/fingerprint/research-only evidence.

Current legacy/default MQ `R0` values are not treated as proof of physical reference-gas calibration. Generic manufacturer/research curves may contribute identification evidence but do not by themselves authorize quantitative PPM or exposure-risk output.

## Evidence/session model

MQ acquisition is stored as a `measurement_session` with time-linked telemetry. Camera recording or a persistent visual-flame event reuses the currently active MQ session when one exists; otherwise it creates an independent media-evidence session. This provides temporal correlation without allowing video evidence to alter chemical PPM calculations.

The gas-direction assistant consumes normalized analytical evidence, relevant MQ channels, motion commands and time history to estimate a relative bearing for a user-selected compound. Its output is advisory and is never converted to drive commands in 0.6.

## Offline-first rule

Robot operation, measurement capture, local analysis, media recording and report generation remain functional without Internet access. GitHub publication is optional and outside the drive-control path.

## Reference-bank rule

Only locally `APPROVED` developer experiments can be sent to GitHub. Publication targets the dedicated `reference-bank` branch. The path is derived from canonical experiment identity/timestamp, the request contains no repository credential, and session-supplied credentials are not persisted. Existing approved files are not silently overwritten.

## Deferred expansion

Thermal sensing, physical MUX expansion, ESP-NOW nodes, 4G/VPN transport, autonomous gas-plume/source following and safe autonomous return-to-link remain outside the 0.6 acceptance path. The current motion/evidence schema intentionally preserves the information needed to research those functions later.
