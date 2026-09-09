# ChemCore / AMCI Checkpoint C3 — Runtime + Dynamic Scientific Library

**Date:** 2026-09-09  
**Branch:** `dev/hazrob-0.10-chemcore-foundation`  
**Status:** IMPLEMENTATION ACTIVE — do not restart these work items from design stage

## Completed in this checkpoint

### Independently updateable scientific library

- Stable/Research library channels separated from `servoc.db`.
- Semantic library versioning and signed manifest contract implemented.
- HTTPS-only manifest/payload transport implemented.
- SHA-256 payload verification, Stable RSA signature requirement, compatibility checks, staged activation and previous-version rollback implemented.
- Developer UI added for update-source configuration, update check, verified install and rollback.
- Updates are blocked during active measurement/analysis.
- Embedded APK knowledge remains fallback when no remote Stable snapshot is active.
- Analytical result envelope upgraded to retain library version, payload SHA-256 and source commit.
- Stable publication builder and secret-backed GitHub Actions publication workflow added.
- Dedicated publication-key provisioning/rotation rules documented. Private key is never bundled with the APK or committed to the repository.

### Human-error-resistant experiment promotion path

- Private Research lane defined.
- Candidate promotion lifecycle fixed as:

  `CANDIDATE -> REVIEWED -> QC_PASSED -> APPROVED_FOR_PUBLISHING -> IMMUTABLE_STABLE_SNAPSHOT`

- Online assisted scientific review is mandatory before promotion of manually defined compound/reference results.
- Structural candidate validator and Candidate-QC workflow implemented. Automated QC is not scientific approval.

### Controlled ChemCore acquisition/runtime

- TelemetryRecorder now exposes the exact active measurement-session identity to ChemCore without duplicating telemetry.
- A controlled ChemCore capture can record baseline + exposure independently of the legacy ESP32 averaging state.
- Controlled capture is stored as a normal auditable `measurement_sessions` record and uses ~200 ms persistence sampling while active.
- SessionSeries gained exact-session reanalysis to prevent 'latest row' races.
- Canonical feature extraction adapter converts an exact persisted session into `chemcore-features/1`, with deterministic feature SHA-256 and environmental summary.
- Research-v1 multi-channel live stability gate added. It requires sufficient valid coverage and stationarity across at least 6/8 MQ channels. Thresholds remain engineering research thresholds pending physical validation.
- ChemCore state machine gained an explicit `EVIDENCE_INSUFFICIENT` path; failed evidence gates no longer require false identity or generic system error.
- Guided developer ChemCore analysis UI added:
  - stationary robot interlock;
  - MQ power confirmation;
  - conditioning time + live stability gate;
  - controlled baseline capture;
  - explicit operator confirmation at physical sample introduction;
  - timed exposure with automatic stability-based extension up to a research maximum;
  - exact-session feature extraction;
  - conservative legacy manufacturer-prior screening;
  - no identity promotion when no validated open-set model is bound;
  - library provenance attached to result;
  - recovery prompt and stability observation;
  - safe abort/telemetry finalization.

## Molecular seed status

The first full PubChem enrichment run produced:

- target identities: 652
- resolved molecular identities: 615
- unresolved/ambiguous occupational entries: 37
- retrieval errors: 0

The original workflow generated and validated the dataset but failed only at the final git push because the research branch advanced during the long build. The workflow has now been changed to safely rebase the generated-data commit onto the current branch head and retry a normal fast-forward push; it never force-pushes.

The 37 unresolved entries must not be guessed into molecular identities. Many are mixtures, materials, isomer groups or occupational categories and require a separate curation/type system.

## Remaining software work after C3

1. Confirm current CI is green after the new controlled-analysis integration.
2. Confirm the fixed PubChem seed workflow successfully pins the 652-record snapshot and manifest.
3. Add explicit classification of unresolved knowledge entries (`PURE_COMPOUND`, `MIXTURE`, `MATERIAL`, `ISOMER_GROUP`, `OCCUPATIONAL_CATEGORY`, `UNRESOLVED`).
4. Bind Stable reference-store readers into all runtime candidate/model providers; embedded/reference fallback must be deterministic.
5. Build authoritative physical/electronic descriptor enrichment beyond the PubChem structural seed.
6. Train/evaluate QSPR-MOX with traceable manufacturer/literature/device datasets and leave-compound-out validation.
7. Calibrate and bind open-set models/thresholds; until then ChemCore must not call a screening candidate a validated identity.
8. Integrate AMCI posterior/entropy actions after feature/inference stages, including controlled repeat/recovery/spatial confirmation.
9. Add recovery-segment persistence/features without contaminating the baseline/exposure feature window.
10. Add ChemCore result/library/model provenance to final PDF/XLSX reports and the normal analysis UI.
11. Implement DriftCore/history and model-validity aging.
12. Perform physical metrology gates (ADC, RL, R0, ADS VDD, fan/motor/blank, MQ7/MQ9 heater protocol) before M4 claims.

## Scientific validity rule

C3 improves repeatability, traceability and software safety. It does **not** convert unvalidated MQ/QSPR/manufacturer priors into calibrated chemical measurements. M4 remains blocked until device-specific metrology/calibration and independent validation are completed.
