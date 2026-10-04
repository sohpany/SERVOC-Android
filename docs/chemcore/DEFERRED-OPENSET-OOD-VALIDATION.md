# Deferred task — Open-Set / OOD experimental validation

Status: **DEFERRED intentionally**

Reason: The ChemCore analytical system report is currently under active review. This task is documented separately to avoid mixing system-analysis review with new experimental-validation work or introducing unrelated changes during that review.

## Task to resume later

Perform a dedicated experimental validation campaign for ChemCore Open-Set / OOD behavior on HR069, including:

1. Build a held-out validation dataset containing known in-distribution chemicals and deliberately unknown/out-of-distribution samples.
2. Validate the Mahalanobis/Open-Set threshold independently from the training distribution.
3. Measure acceptance, rejection, false-accept, and false-reject rates.
4. Confirm minimum feature/channel coverage requirements and fail-closed behavior when coverage is insufficient.
5. Validate the interaction between accepted OOD status and multi-channel fingerprint promotion to `M3_ARRAY_CONSISTENT`.
6. Confirm that OOD acceptance does not itself grant M4 or reportable PPM authority.
7. Document the selected threshold, dataset split, test protocol, metrics, and model-card provenance.
8. Only after independent validation, promote the threshold provenance from empirical/runtime policy to `INDEPENDENT_VALIDATION_THRESHOLD` where justified.

## Explicitly excluded for now

- No numerical Open-Set/OOD threshold changes.
- No additional M3 threshold tuning.
- No M4 claims.
- No PPM curve changes.
- No mixture-model changes.
- No code changes while the current system-analysis report is being reviewed unless separately authorized.

## Resume condition

Resume only when the current ChemCore system-analysis report review is complete or when explicitly requested by the project owner.
