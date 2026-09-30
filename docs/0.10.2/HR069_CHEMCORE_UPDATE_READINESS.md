# HazRob 0.10.2 / HR069 — ChemCore in-app update readiness

Date: 2026-09-30
Target: `release/hazrob-0.10.2` / `HR069`

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

## Resume checkpoint

P4 should be considered complete only after the branch build passes its source audit, unit tests, lint and internal release assembly with the experimental updater contract. No final APK is exported during this development plan; internal builds remain required for validation.
