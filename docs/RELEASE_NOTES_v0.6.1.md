# HazRob 0.6.1 — Release Candidate Notes

Date: 2026-08-31
Branch: `dev/hazrob-0.6`
Application: `com.servoc.app`
Version: `0.6.1` / versionCode `8`
Database schema: `7`
Master ESP32 protocol: HazRob API v3

## Scope

HazRob 0.6.1 packages the current post-0.6 migration state as a new signed Android build. It retains the compact four-tab master ESP32 architecture and the Android-side analytical, scientific-data, media, developer-laboratory and evidence workflows.

## Current implemented capabilities

- USER/DEVELOPER startup role gate; developer session PIN is `1166`.
- Unified Measurement & Analysis UI replacing the former separate sensor/chemical/physical measurement tabs.
- HazRob master ESP32 API v3 adapter using compact `/diag` + `/data`, while preserving drive/STOP/relay/measurement/config command endpoints.
- SQLite schema v7 with dynamic chemicals, synonyms, source/provenance records, authority-specific exposure limits, time-series measurements, media, events and reference-bank publication audit records.
- Pinned scientific datasets validated in CI: 652 NIOSH CAS-index identities, 402 NIOSH IDLH records, and 638 normalized OSHA Table Z-1 records.
- No global pre-calibrated-MQ assumption; legacy/default `R0=10` cannot independently authorize quantitative PPM or exposure-risk output.
- MQ time-series and transient-response features, clean-air baseline, sensor-health/drift indicators and dynamic developer reference captures.
- H.264/MP4 phone-side recording from the ESP32-CAM MJPEG stream, with measurement-session correlation.
- Experimental temporal visual-flame evidence with event metadata/snapshots; it is not used for drive control or gas-PPM validation.
- Motion breadcrumbs and advisory compound-specific gas-source direction estimation; no autonomous backtracking or plume-following.
- Immutable approved-experiment JSON export and optional direct publication to the dedicated GitHub `reference-bank` branch using a session-only developer-supplied credential.
- Existing application ID and signing certificate retained for upgrade continuity.

## CI validation

GitHub Actions `Build HazRob APK` run `33416169828` / run number `218`:
- pinned scientific dataset validation: PASS
- analytics/unit regression tests: PASS
- signed release APK build: PASS
- signing certificate verification: PASS
- artifact upload: PASS

Validated executable-source commit:
`03d7198e15790bb3aa436be99fe5b31266d9c317`

Release APK SHA-256:
`d71e7be0d9908ae1c3f6db6af04be2f1ea8b2cf87de514b5ade1f3e4c5ceb067`

Signing certificate SHA-256:
`D6:B5:B6:9B:E3:82:F2:AF:A6:DF:4B:46:BA:DE:5B:6F:86:4F:40:60:62:2E:94:90:75:29:3A:C6:20:03:D3:F0`

Artifact:
- `HazRob-v0.6.1-prototype`
- ID `9767135755`
- workflow ZIP size `821437` bytes
- ZIP SHA-256 `613ba69390ea04dc0a71e2ba607c291bb130697932f879ea9ddd47be62075d71`

## Validation boundary

This remains a prototype/research release candidate. Physical validation is still required for robot hardware integration, camera/flash behavior on the actual network, controlled gas-response datasets, calibration validity, flame false-positive/generalization performance, gas-direction trajectories, and reference-bank publication on the intended repository permissions.

Deferred development remains: 4G/VPN remote transport, autonomous return after link loss, autonomous plume-following, thermal fusion, physical analog MUX expansion and ESP-NOW sensor nodes.
