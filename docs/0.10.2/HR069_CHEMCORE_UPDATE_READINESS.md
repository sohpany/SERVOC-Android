# HazRob 0.10.2 / HR069 — ChemCore in-app update readiness

Date: 2026-09-30
Target: `release/hazrob-0.10.2` / `HR069`
Status: **P4 COMPLETE ✅**

## Executive status

P4 adopts an **experimental integrity mode** appropriate for the current prototype stage. ChemCore scientific-library updates remain independent from APK updates and require **no password, no RSA key, no publication secret and no user cryptographic configuration**.

The updater contract is:

- HTTPS-only manifest discovery and payload download;
- bounded manifest/payload sizes;
- manifest schema v2;
- explicit `payload_schema`, `data_revision`, `evidence_revision` and `source_commit_sha`;
- SHA-256 payload integrity verification;
- app-version and HR069 sampling-profile compatibility checks;
- semantic validation of payload schema, revisions, record counts and evidence policy;
- staged download followed by re-verification and atomic activation;
- previous-library rollback with SHA-256 and semantic re-verification;
- update/rollback blocked while warm-up, measurement or analysis is active;
- user-facing update action in the normal Settings tab;
- developer-only endpoint override and rollback controls;
- fully offline ChemCore runtime after a library is installed.

Digital publication signing is intentionally **deferred until production hardening**. The prototype therefore does not contain a publication public-key asset or a publication-key provider, and the 0.10.2 publication workflow requires no private-key GitHub secret.

## User workflow

For the normal user the flow is deliberately simple:

1. Open **الإعدادات**.
2. Select **التحقق من تحديثات منظومة التحليل**.
3. If a newer compatible library exists and no measurement/analysis is active, the app downloads, verifies and installs it.
4. ChemCore then uses the installed snapshot offline; no APK reinstall is required.

No passwords or key-management steps are exposed to the user.

## HR069 scientific payload currently prepared

The 0.10.2 branch has a reproducible packaging path containing:

- 652 chemical identity records;
- 615 molecularly resolved records and 37 explicitly curated non-QSPR identities;
- 43 pinned sensor-response curves plus six preferred literature overlays, packaged without promotion to M4;
- the 76-model analytical catalogue with provenance/status retained;
- external MQ/MOS reference provenance;
- explicit evidence policy requiring device-specific HR069 validation for M4.

The canonical Stable payload records a deterministic `data_revision` based on approved source-file hashes and an explicit `evidence_revision` (`hazrob-evidence-policy/1`).

## Scientific acceptance gates retained

Simplifying update security does **not** relax analytical evidence rules:

- external/reference/QSPR evidence remains at its applicable non-M4 tier;
- packaging a record never promotes it to M4;
- quantitative M4 evidence still requires accepted HR069 device-specific calibration/validation;
- identity confidence remains distinct from PPM confidence/reportability.

## Production hardening later

Before HazRob becomes a production/public deployment, publication authenticity can be upgraded to signed manifests or another managed trust mechanism. That is deliberately outside the current prototype P4 scope and must not block experimental development.

## Validation closure

P4 is closed after successful internal validation at source commit `d4ac539eef5a75fb689869b9ab5af1f6d6780674` on the Railway validation builder.

Confirmed gates:

- `audit_0104_chemcore_library.py`: PASS;
- experimental updater contract: **HTTPS + SHA-256 + compatibility + semantic validation + atomic activation + rollback; no passwords/keys**;
- Android unit tests: PASS;
- Android lint: PASS;
- signed internal Release APK assembly and signer digest verification: PASS;
- Main ESP32 HR069 compile: PASS;
- ESP32-CAM HR069 compile: PASS.

The internal APK is validation-only and remains non-exported before P13.

## Resume checkpoint

P4 is complete. Development continues from the analytical-engine/evidence-fusion phases on the same `release/hazrob-0.10.2` / `HR069` baseline. Production signing remains explicitly deferred and must not be reintroduced into the experimental updater path unless the project enters production hardening.
