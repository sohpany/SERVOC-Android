# Changelog

## 0.6.1 — 2026-08-31
- Packaged the current post-0.6 migration state as versionCode `8` / versionName `0.6.1`.
- Preserved the latest API-v3 integration, unified measurement workflow, schema-v7 scientific database, pinned NIOSH/OSHA datasets, MQ validity gates, time-series/health analysis, phone-side video recording, experimental flame evidence, gas-direction assistance and developer Reference Bank publication workflow.
- Removed versioning inconsistency that still labeled the latest signed build as 0.6.0.
- Revalidated pinned scientific datasets, analytics/unit regression tests, signed release build and signing certificate in GitHub Actions run `33416169828`.
- Release APK SHA-256: `d71e7be0d9908ae1c3f6db6af04be2f1ea8b2cf87de514b5ade1f3e4c5ceb067`.

## 0.6.0 — 2026-08-31
- Migrated Android communication to compact HazRob master ESP32 API v3 (`/diag` + `/data`) while retaining the existing drive/STOP/relay/measurement/config endpoints.
- Added startup USER/DEVELOPER role gate; developer laboratory requires session PIN `1166` and is hidden from the normal-user interface.
- Consolidated sensors, compounds and measurements into one `Measurement & Analysis` workflow.
- Upgraded the offline database to schema v7 with dynamic chemical identities, synonyms, authority-specific exposure limits, sensor evidence, time-series sessions, media/events and reference publication audit records.
- Bundled pinned official snapshots: 652 NIOSH CAS-index identities, 402 NIOSH IDLH records and 638 normalized OSHA Table Z-1 limit records with provenance/SHA-256 validation in CI.
- Disabled the former global pre-calibrated-MQ assumption; default/legacy `R0=10` can no longer authorize quantitative PPM or exposure risk.
- Added MQ transient features, clean-air baselines, sensor health/drift assessment and expanded developer reference captures using dynamic chemical UID/CAS/name selection.
- Added H.264/MP4 phone-side video recording from the existing ESP32-CAM MJPEG stream, timing index and MediaStore export.
- Added experimental temporal visual-flame evidence with region overlay, event snapshots and session correlation; it is not used for drive control or PPM validation.
- Added motion breadcrumbs and compound-specific relative gas-source direction assistance; autonomous navigation and backtracking remain disabled.
- Added immutable developer Reference Bank JSON export and optional direct publication of APPROVED records to the dedicated GitHub `reference-bank` branch using a session-only developer-supplied credential.
- Hardened reference publication with canonical filenames and create-only semantics to prevent path traversal and silent overwrite.
- Preserved applicationId `com.servoc.app` and the established signing certificate for upgrade continuity.

## 0.5.0
- Established the Arabic RTL HazRob operator interface and stable prototype signing baseline.
- Added environmental `env` telemetry support with legacy `bme` fallback.
- Added analytical/library audit scaffolding and the original 144-compound compatibility catalogue.

## 0.3.0
- Renamed the app display name to HazRob.
- Added the HazRob application icon.
- Preserved applicationId `com.servoc.app` for upgrade continuity.
- Added analytical provenance and MQ operating-envelope validation.
- Added multi-point empirical calibration, calibration QC, and persisted calibration curves.
- Added MQDataScience comparison models and research-only temperature/RH correction.
- Added analytics regression tests in CI.
- Established a stable prototype signing baseline for iterative v0.3+ APK updates.
