# ChemCore Versioned Knowledge Library and Online Approval Architecture

**Status:** ADOPTED DESIGN / IMPLEMENTATION STARTED  
**Branch:** `dev/hazrob-0.10-chemcore-foundation`  
**Date:** 2026-09-09

## 1. Purpose

ChemCore knowledge will evolve through hundreds of experiments, literature/model revisions, molecular-property enrichment and calibration updates. The Android APK must therefore be decoupled from the scientific knowledge-library release cycle.

The application remains the analytical engine. A separately versioned **ChemCore Knowledge Library** is synchronized on demand when Internet access is available.

This architecture also formalizes the intended human-in-the-loop approval workflow: reference experiments and manually identified compounds are reviewed during an online development/review session before they are promoted into the published reference library. The application itself must not permit an unreviewed manual entry to silently become stable analytical evidence.

## 2. Separation of concerns

### 2.1 Application / firmware

Versioned independently. Contains:

- acquisition and signal-processing code;
- ChemCore feature contract;
- QSPR/inverse-solver/open-set/AMCI algorithms;
- schema readers and compatibility logic;
- an embedded fallback knowledge snapshot sufficient for offline operation.

An APK update is required only when code, schema compatibility, firmware contract or analytical algorithms change.

### 2.2 Research knowledge lane

Private and auditable. Contains candidate/experimental material such as:

- raw or derived reference-session artifacts;
- proposed compound identity mappings;
- candidate calibration points/curves;
- draft molecular properties and source mappings;
- QA findings and rejected records.

States:

`CANDIDATE -> REVIEWED -> QC_PASSED -> APPROVED_FOR_PUBLISHING`

A rejected or incomplete item never enters the stable library.

### 2.3 Stable knowledge lane

Contains only approved, publishable scientific records. It is distributed to the application as immutable, versioned snapshots.

Recommended distribution target is a separate read-only public data repository (or an equivalent HTTPS object endpoint) containing no source-code secrets or credentials. The current application repository remains private. **No GitHub personal access token or repository write credential is ever embedded in the APK.**

If stable data must remain private, use a controlled HTTPS distribution endpoint; do not place a private GitHub token in the application.

## 3. Online experiment-review and promotion workflow

For every manually defined or experimentally promoted material:

1. Acquire the experiment under a versioned SamplingProfile.
2. Preserve raw telemetry/session identifiers and relevant reference conditions.
3. Review chemical identity and nomenclature:
   - canonical name;
   - CAS / PubChem CID / ChemCore UID;
   - formula / structure identifiers where available;
   - chemical family;
   - reference source and concentration method.
4. Review instrumentation metadata:
   - MQ module/channel;
   - R0/RL state;
   - temperature/RH/pressure;
   - chamber profile;
   - exposure/recovery timing;
   - ADC status and validity.
5. Review analytical evidence and feature artifacts.
6. Assign evidence/status without promotion inflation (`M0`..`M4`).
7. Submit candidate record to the research lane.
8. Run automated QA/CI: schema, referential integrity, domain checks, duplicate/conflict checks, hashes and model tests.
9. Human review approves or rejects promotion.
10. Build a new immutable Stable Library snapshot.
11. Publish manifest + payload + changelog.
12. Android clients can then discover and install that version.

No step permits a user-entered name alone to authorize quantitative evidence.

## 4. Library versioning

Application, firmware and scientific library versions are independent.

Example:

- App: `0.10.0`
- Firmware: `0.9.3-rc2`
- ChemCore Library: `1.3.17`
- Knowledge source commit: `abcdef...`

Use semantic versioning for the knowledge library:

- PATCH: additions/corrections compatible with the same schema and analytical contracts;
- MINOR: backward-compatible new data/model artifacts;
- MAJOR: incompatible schema/evidence semantics requiring a compatible application.

Every analytical report must record the exact app, firmware, sampling-profile, feature-schema and ChemCore Library versions used.

## 5. Immutable snapshot distribution

Do **not** synchronize individual SQL rows from the Internet into the operational database.

Publish one immutable package per Stable Library version. The logical package contains:

- `chemcore_knowledge.sqlite` (reference-only database, preferred mature format), or an explicitly versioned equivalent payload;
- `manifest.json`;
- `sources.json` / provenance metadata where not already normalized in the reference DB;
- `model_cards.json` or versioned model artifacts as applicable;
- `CHANGELOG.json`.

The **reference library database must be separate from `servoc.db`**, because `servoc.db` contains device-local telemetry, experiments and operational state that must never be replaced by a remote snapshot.

## 6. Manifest contract v1

Required fields:

- `library_version`
- `schema_version`
- `channel` (`stable` or `research`)
- `created_utc`
- `source_commit_sha`
- `payload_file`
- `payload_format`
- `payload_sha256`
- `payload_size_bytes`
- `chemical_count`
- `source_count`
- `model_count`
- `calibration_count`
- `min_app_version`
- `max_app_version` when compatibility has an upper bound
- `feature_schema_version`
- `sampling_profile_compatibility`
- `signature_algorithm`
- `signature` when the channel requires signed publication

Manifest fields affecting trust or compatibility must be covered by the publication signature, not merely the payload hash.

## 7. Verification and atomic installation

Stable installation flow:

`CHECK -> DOWNLOAD TO STAGING -> SIZE CHECK -> SHA-256 -> SIGNATURE -> MANIFEST/SCHEMA VALIDATION -> REFERENTIAL INTEGRITY -> COMPATIBILITY -> READ-ONLY OPEN TEST -> ATOMIC ACTIVATE`

Rules:

- Never update while a measurement/ChemCore analysis session is active.
- Never modify the active library in place.
- Keep the previous known-good library until the new snapshot passes every gate.
- Installation failure leaves the current active library untouched.
- Retain enough metadata for rollback and audit.
- Refuse downgrade by default unless developer mode explicitly authorizes rollback.
- A stable library should require a cryptographic publication signature once the production public key is provisioned in the application. SHA-256 alone provides integrity against accidental corruption but not publisher authenticity.

## 8. Android user experience

Recommended developer/library page:

- Installed Library version
- Available Library version
- Publication date
- Source commit
- chemical/model/calibration counts
- changelog summary
- trust/signature status
- compatibility status
- last successful update time

Actions:

- `Check for updates`
- `Download and install`
- `View changes`
- developer-only `Rollback to previous validated snapshot`

The app may check a small remote manifest automatically when Internet connectivity exists, but installation should not occur during an active analytical session.

## 9. Offline and failure behavior

The robot remains operational without Internet connectivity.

Priority:

1. verified installed external Stable Library;
2. embedded fallback library bundled with the APK.

Research/candidate data never supersedes Stable data in normal user operation.

If the remote endpoint, GitHub or network is unavailable, ChemCore uses the last verified local snapshot without changing evidence semantics.

## 10. Security rules

- no GitHub PAT/token in APK;
- no remote writes from ordinary application clients;
- HTTPS only;
- stable channel requires pinned publication trust policy;
- payload SHA-256 and signed manifest;
- explicit min/max compatible application versions;
- immutable package names/versions;
- rollback protection;
- source commit and build provenance retained;
- update process fail-closed.

## 11. Reproducibility and reporting

Each PDF/XLSX/experiment export must include:

- app version;
- firmware version;
- hardware profile;
- SamplingProfile;
- ChemCore feature-schema version;
- ChemCore Library version;
- library source commit/hash;
- model artifact/version/hash used by the decision.

This allows an old analytical result to be reconstructed even after later library updates.

## 12. Acceptance criteria for the sync subsystem

The feature is not operationally complete until all of the following are demonstrated:

1. the app can query a remote manifest without repository credentials;
2. the app can compare Semantic Library versions and compatibility;
3. a valid payload installs into staging and activates atomically;
4. a corrupted hash is rejected;
5. an invalid/unknown signature is rejected on the Stable channel;
6. an incompatible schema/app version is rejected;
7. an update is blocked during an active measurement;
8. simulated interruption leaves the previous library usable;
9. rollback restores the previous verified snapshot in developer mode;
10. reports record the active library version/hash;
11. the research lane cannot be silently treated as Stable/M4 evidence.

## 13. Implementation sequence

### K1 — Contracts and trust model
- manifest parser/validator;
- semantic-version comparator;
- hash/signature verifier contract;
- stable/research channel policy.

### K2 — Separate reference store
- establish `chemcore_knowledge` read-only reference store separate from local `servoc.db`;
- embedded fallback snapshot;
- active/staged/previous metadata.

### K3 — Remote updater
- HTTPS manifest check;
- staged payload download;
- compatibility and integrity validation;
- atomic activation/rollback.

### K4 — UI
- Update button;
- installed/available versions;
- changelog and trust state;
- update interlock while measurement is active.

### K5 — Publication CI
- generate stable snapshot;
- validate record/model/calibration provenance;
- generate manifest/changelog;
- sign manifest with offline/CI protected publication key;
- publish immutable release artifact.

### K6 — Reporting integration
- library version/hash in every analytical result/report;
- source commit/model provenance.

## 14. Change-control

Any alteration to manifest schema, signature/trust policy, stable-vs-research promotion semantics, reference-store schema or evidence promotion rules requires an explicit revision of this document and compatibility review.
