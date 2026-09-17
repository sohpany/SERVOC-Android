# Changelog

## 0.10.1 HR-0101-066 — 2026-09-18
- Made the already-associated HazRob Wi-Fi route the Android Direct authority; temporary `WifiNetworkSpecifier` is now an explicit fallback instead of a 250 ms recurring recovery mechanism.
- Preserved Direct as boot/default and dominant control mode; Remote now requires configured WAN + persisted developer authorization + an explicit Remote request.
- Added developer Remote authorization controls and Main ESP32 enforcement; Direct transition still stops motion and removes MQTT/NAPT/STA remote transport while preserving WAN configuration.
- Kept ESP32-CAM STA-only at `192.168.4.2`, forced flash GPIO4 OFF at boot and added camera reset-reason diagnostics.
- Kept live MQ readings independent from timed analysis sessions; changed live value text to grey for the light UI and made elevated/high gas headings red while retaining phone alarm tones.
- Recorded the downward digital IR channel's 8 cm physical ground/cliff reference in firmware telemetry.
- Enlarged the drive joystick by 20%, removed its software dead-zone, prevented parent touch interception during active control, and retained the latest touch command across in-flight sends.
- Updated controlled source audits and added `docs/0.10.1/HR066_DIRECT_REMOTE_LIVE_UI_CONTROL_MANIFEST.md`.

## 0.10.0 — 2026-09-09
- Promoted HazRob to the first major **ChemCore / AMCI** release: Android `0.10.0` (versionCode 20), master ESP32 `HazRob 0.10.0` / API v4 and ESP32-CAM `HazRob-CAM 0.10.0`.
- Preserved the validated physical wiring revision `hazrob-0.9.3-1r8mq-alert-ldr-l298n-v1`; release version and hardware revision are intentionally independent.
- Locked propulsion to **L298N differential 4WD** on GPIO18/27 (left) and GPIO19/32 (right); servo drive remains disabled.
- Preserved one active-LOW GPIO25 relay powering all eight MQ modules and dual ADS1115 acquisition at 0x48/0x49.
- Fixed ADS1115 supply provenance at the known 5 V HazRob rail; retained 860 SPS, GAIN_TWOTHIRDS and four-conversion per-channel averaging.
- Added scan-cycle/channel timing provenance for the sequential eight-channel ADC acquisition path.
- Added the canonical `chemcore-features/1` pipeline: 17 features/channel × 8 MQ channels = 136 features.
- Added controlled ChemCore session state, live stability gating, stationary-analysis interlock, exact session lineage and conservative result states including UNKNOWN/INSUFFICIENT_EVIDENCE/POSSIBLE_MIXTURE_OR_INTERFERENCE.
- Added molecular/physical intelligence, descriptor contracts, physical feasibility gates, QSPR-MOX research infrastructure, hierarchical candidate reduction, inverse full-array solver, open-set/Mahalanobis infrastructure, uncertainty/entropy and AMCI adaptive measurement decisions.
- Embedded 652 occupational/chemical identities; 615 are molecularly resolved and 37 are explicitly curated as unresolved/material/mixture/category records rather than forced into false molecular identities.
- Added independent, signed, versioned ChemCore Stable library updates with staging, SHA-256, RSA signature verification, compatibility checks, atomic activation and rollback.
- Added unified end-user Update action in Status; developer mode retains research-library management, source override and rollback controls.
- Added exact ChemCore provenance to PDF/XLSX reporting: session, outcome/evidence, model, feature schema, sampling profile and library version/hash/source commit.
- Standardized chamber profile `hazrob-chemcore-chamber-v2` for the 90×40×150 mm pull-through chamber and documented the engineering flow prior without mislabeling it as measured calibration.
- Hardened MQTT remote state with retained Last Will while preserving session/sequence/nonce/TTL/application-ACK command semantics.
- Hardened ESP32-CAM remote WSS reconnect behavior and Render JPEG relay bounds/freshness diagnostics.
- Reworked ESP32-CAM source for a smaller, focused firmware surface; ChemCore/molecular inference remains Android-side.
- Added firmware CI Flash-headroom enforcement and retained fail-closed Android/scientific dataset regression gates.
- Expanded the ChemCore research bibliography with recent 2024–2026 peer-reviewed work on descriptor-driven sensing, transient preprocessing, mixtures, drift, open-set recognition, uncertainty/selective classification and calibration transfer.

## 0.9.0 — 2026-09-04
- Unified HazRob MQ power around hardware profile `hazrob-0.9-1r8mq-v1`.
- Replaced the previous multi-bank relay model with one active-LOW relay on GPIO25 powering all eight installed MQ modules simultaneously.
- Defined the only valid MQ power states as `0=OFF` and `1=ON`; removed operational use of masks 2/3 and R1/R2 group selection.
- Preserved dual ADS1115 acquisition independently: ADS1 `0x48` handles MQ2/MQ4/MQ5/MQ8 and ADS2 `0x49` handles MQ3/MQ7/MQ9/MQ135.
- Updated master firmware telemetry to publish a single relay object with ON/OFF state and shared `power_ms`.
- Hardened local HTTP `/relay` and remote MQTT relay commands to reject any state other than 0 or 1.
- Updated Android hardware profile, relay planner, remote command path, protocol normalization, status UI, measurement UI and developer experiment UI to the same single-relay model.
- Updated saved-session descriptions and developer reference-capture metadata to record shared MQ power and GPIO25 traceability.
- Updated PDF and XLSX reports to record MQ Power ON/OFF, GPIO25 and the shared power duration rather than R1/R2 rows.
- Updated release-audit assertions and regression tests for the one-relay architecture.
- Updated project and firmware documentation to remove stale multi-relay topology.
- ESP32-CAM remains a separate subsystem and is not controlled by the MQ power relay.

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
