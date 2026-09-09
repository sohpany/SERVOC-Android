# ChemCore Research Lane

This directory is the private, auditable intake lane for experiment/reference candidates that may later be promoted into the independently versioned ChemCore Stable Library.

## Promotion rule

No manual chemical definition, calibration point, fingerprint, model or concentration result becomes Stable merely because it was entered in the Android application. The promotion path is:

`CANDIDATE -> REVIEWED -> QC_PASSED -> APPROVED_FOR_PUBLISHING -> IMMUTABLE_STABLE_SNAPSHOT`

A candidate is reviewed during an online project-development session. Review must resolve chemical identity (name/CAS/ChemCore UID), reference concentration and method, measurement/session provenance, sampling profile, hardware/firmware/app versions, feature schema, calibration/R0/RL state, evidence tier and any exclusions or anomalies.

Files under `candidates/` must pass `tools/validate_chemcore_candidate.py`. Automated validation is a structural/QA gate only; it is not scientific approval.

## Stable boundary

Stable publication is performed only through the signed ChemCore publication pipeline. The application installs only compatible Stable snapshots whose payload hash and publication signature verify. Research records remain private and are not distributed to normal users.
