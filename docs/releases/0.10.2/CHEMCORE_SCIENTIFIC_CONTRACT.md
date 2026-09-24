# ChemCore Scientific Contract — 0.10.2

Reviewed from HR067 commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

## Canonical feature pipeline
`ChemCoreFeaturePipeline` is the canonical cross-reactive MQ time-domain feature contract.

- schema: `chemcore-features/1`
- input: samples × sensors of valid positive Rs/R0
- transform: `log10(Rs/R0)`
- 17 features per sensor
- real sample time is used when valid; otherwise the implementation can fall back to sample-index time

Per-sensor features include valid fraction, baseline/steady/peak response, signed and absolute delta, min/max, initial and stability slopes, t10/t50/t90, signed/absolute AUC, baseline noise and SNR.

0.10.2 must not introduce an alternate incompatible fingerprint definition without a schema/version migration.

## Decision vocabulary
ChemCore outcomes:
- SCREENING
- IDENTIFIED
- QUANTIFIED
- UNKNOWN
- INSUFFICIENT_EVIDENCE
- OUT_OF_VALIDATED_DOMAIN
- POSSIBLE_MIXTURE_OR_INTERFERENCE
- INVALID_MEASUREMENT

Evidence tiers:
- M0 catalog only
- M1 molecularly inferred
- M2 literature/manufacturer supported
- M3 array consistent
- M4 HazRob validated

Named identity is permitted only for IDENTIFIED or QUANTIFIED outcomes.

Quantitative occupational interpretation is permitted only when:
`outcome == QUANTIFIED && evidence_tier == M4_HAZROB_VALIDATED`.

## ChemCore schema
ChemCore uses an idempotent schema bootstrap separate from legacy DB migrations. HR067 ChemCore schema version is 4.

Preserve:
- sampling profiles and chamber geometry/flow metadata;
- session-to-sampling-profile binding;
- per-channel timing;
- molecular structures/properties/descriptors;
- sensor-response priors with validity domains and evidence tiers;
- model artifacts with feature schema, metrics, validity domain and SHA-256;
- feature references bound to experiments/sessions;
- session-bound decisions including prediction interval and OOD score.

## Sampling and geometry
Sampling profile identity is scientific provenance. Chamber geometry, internal displacement, flow status/flow estimate and uncertainty must not be silently changed between reference training and runtime inference.

## 0.10.2 hold points
- Do not alter feature definitions or their order.
- Do not alter M0–M4 semantics.
- Do not relax M4 requirement for occupational quantitative interpretation.
- Do not replace physical timestamps with synthetic timing when valid physical timestamps exist.
- Any future sampling-profile or chamber-geometry change requires explicit versioning and validation.
