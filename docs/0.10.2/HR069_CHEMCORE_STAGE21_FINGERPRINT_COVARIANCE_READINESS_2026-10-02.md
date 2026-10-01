# HazRob 0.10.2 / HR069 — ChemCore Stage 21 Fingerprint Covariance Readiness

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries.

## Objective

Determine whether any current fingerprint evidence supports an empirical multivariate covariance matrix across the installed MQ channels.

## Current representation

`ChemicalFingerprint` stores per-channel means and per-channel sigma only. `FingerprintTrainer` estimates each channel independently. No full cross-sensor covariance matrix is represented in the current fingerprint object.

The static research-prior fingerprints are deterministic source/model-derived templates and have `sampleCount=0`.

Therefore deterministic concentration-grid templates cannot be reinterpreted as repeated experimental samples to manufacture covariance.

## Readiness states

- `COVARIANCE_READY`
- `DIAGONAL_VARIANCE_ONLY`
- `MODEL_PRIOR_NO_EMPIRICAL_VARIANCE`
- `TRANSFER_NONNUMERIC`
- `NO_FINGERPRINT_DATA`
- `HARDWARE_OR_OBJECT_NOT_READY`

## Current assignment

- 13 model-derived numeric fingerprint entries: `MODEL_PRIOR_NO_EMPIRICAL_VARIANCE` for covariance purposes, regardless of the per-channel uncertainty proxy used by the classifier.
- 5 explicit transfer-evidence/no-numeric-vector entries: `TRANSFER_NONNUMERIC`.
- remaining entries: `NO_FINGERPRINT_DATA` or object/hardware-specific gap.
- `COVARIANCE_READY`: 0/144.

## Minimum empirical requirement

A covariance estimate must use independent HR069 experimental sessions, not repeated time samples from one exposure and not deterministic concentration-grid points.

Policy:

- minimum 20 independent sessions for a covariance estimate;
- preferably >=30 independent sessions;
- sessions should span multiple experimental blocks/days and relevant concentration/environment conditions;
- repeated observations from the same exposure are nested data and must not be counted as independent sessions.

## Missing-feature rule

When a future covariance fingerprint has missing sensor channels, distance must be computed on the observed-feature covariance marginal `Sigma_SS`, not by taking an arbitrary precision-matrix subblock.

Regularization/shrinkage is permitted when justified by sample size but must be recorded as part of the model provenance.

## Relationship to sensor correlation

Empirical covariance is distinct from Stage 27 evidence-lineage/correlation grouping:

- covariance captures observed statistical co-variation within a fingerprint class;
- evidence-lineage correlation prevents multiple source/model relations or strongly related MQ channels from being counted as independent confirmations.

Both are required and must not substitute for each other.

## Catalogue accounting

- Reviewed: 144/144.
- Covariance-ready: 0.
- Model-derived numeric priors lacking empirical covariance: 13.
- Explicit transfer evidence without numeric vector: 5.
- All other entries: fingerprint/covariance evidence gap or not yet hardware/object-ready.

## Stage result

**COMPLETE — 144/144.**

Stage 22 audits temporal-feature readiness using the canonical `chemcore-features/1` contract.