# Analytical Preservation Map — 0.10.2

Source of truth reviewed: HR067 commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

| Component | 0.10.2 action | Reason |
|---|---|---|
| ValidatedGasEngine | KEEP EXACT initially | Evidence-weighted MQ engine and quantitative gating |
| CandidateRanker | KEEP EXACT initially | Identity ranking separated from hazard severity |
| AnalyticsSnapshot | KEEP + REVIEW heater flags | Preserves analytical payload; heater-validity flags require physical/firmware confirmation |
| ReportExporter | KEEP EXACT initially | Advanced fingerprint, ChemCore, provenance and safety reporting |
| ExperimentsDb v8 | KEEP / additive migration only | Contains experiments, captures, models, calibration, reference artifacts, chemical/exposure/evidence/session/telemetry tables |
| SqliteCalibrationProvider | KEEP EXACT initially | Quantitative estimate requires active curve; referenceValidated requires reference gas + QC acceptance |
| CalibrationQualityPolicy | KEEP EXACT initially | Calibration quality gates must not be weakened |
| ExposureRepository | KEEP EXACT initially | Exposure limits remain a separate authority-aware layer |
| FingerprintClassifier | KEEP EXACT initially | User-trained fingerprint identification |
| FingerprintTrainer | KEEP EXACT initially | Training path for reference fingerprints |
| ChemicalSeedImporter | KEEP | Scientific identity provenance/import validation |
| ExposureSeedImporter | KEEP | Exposure dataset import validation |
| Generated NIOSH/OSHA assets | VALIDATE BEFORE REFRESH | Later snapshots have new hashes/timestamps and must be validated before replacement |

## Database invariants
Database version at the HR067 baseline is 8. The 0.10.2 migration must preserve existing user data and must not recreate the database destructively.

Critical persisted structures include:
- experiments and approved/reference metadata;
- fingerprints and captures;
- analytical models;
- calibration curves and calibration points;
- reference artifacts;
- chemicals, synonyms and scientific sources;
- exposure limits and sensor evidence;
- measurement sessions and telemetry samples;
- media assets, events and reference queue.

## Quantitative invariant
A numerical concentration must not be promoted to a safety/exposure result solely from fingerprint agreement. Device/reference calibration and the applicable scientific/domain gates remain mandatory.

## Review item
`AnalyticsSnapshot` contains explicit MQ7/MQ9 heater-validation state. This is preserved for now and marked for firmware/hardware verification rather than silently changed.
