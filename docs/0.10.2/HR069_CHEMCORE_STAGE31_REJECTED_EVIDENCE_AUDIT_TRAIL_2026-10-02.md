# HazRob 0.10.2 / HR069 — Stage 31 Rejected-Evidence Audit Trail

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: analytical provenance of hypotheses/evidence that are observed but not promoted.

## Objective
Prevent live or intermediate chemical signals from silently disappearing when later analytical gates reject, downgrade or withhold them.

A rejection record is diagnostic. It does **not** mean the chemical is proven absent.

## Canonical contract
`ChemCoreRejectedEvidenceAuditTrail` version `chemcore-rejected-evidence/1` records:

- chemical UID / label when available;
- sensor key when the rejection is sensor-specific;
- analytical stage;
- structured rejection reason;
- source/model identifiers;
- optional diagnostic value;
- explanatory note.

## Canonical rejection reasons
- `OUT_OF_VALIDATED_DOMAIN`
- `HEATER_PROTOCOL_INVALID`
- `INSUFFICIENT_CONTRIBUTORS`
- `SPECIALIST_CONFLICT`
- `OPEN_SET_OOD`
- `UNSUPPORTED_HARDWARE`
- `MODEL_UNAVAILABLE`
- `INVALID_CONCENTRATION_DOMAIN`
- `DISPLACED_BY_HIGHER_PRECEDENCE`
- `SINGLE_SENSOR_SCREENING_ONLY`
- `LIVE_HYPOTHESIS_NOT_CONFIRMED`
- `UNKNOWN`

## Analytical stages
- `LIVE`
- `RESPONSE_MODEL`
- `JOINT_FIT`
- `SPECIALIST_GATE`
- `OPEN_SET`
- `HARDWARE_GATE`
- `SOURCE_PRECEDENCE`
- `FINAL_DECISION`

## Automatic collection
The audit collector derives records from existing solver/gate outputs:

- unsolved candidate with no model -> `MODEL_UNAVAILABLE`;
- invalid concentration domain -> `INVALID_CONCENTRATION_DOMAIN`;
- insufficient overlapping channels -> `INSUFFICIENT_CONTRIBUTORS`;
- solvable candidate using an out-of-domain contributor -> `OUT_OF_VALIDATED_DOMAIN`;
- M2 candidate lacking >=2 direct/reference contributors and no M4 support -> `SINGLE_SENSOR_SCREENING_ONLY`;
- Stage-29 identity hold -> `SPECIALIST_CONFLICT`;
- rejected open-set gate -> `OPEN_SET_OOD` while retaining the top hypothesis for audit.

## Pre-solver exclusions
Some evidence is rejected before the inverse solver and therefore cannot be reconstructed from solver output. Explicit constructors preserve these exclusions:

- heater-cycle invalid channel -> `HEATER_PROTOCOL_INVALID`;
- required/specialized hardware absent -> `UNSUPPORTED_HARDWARE`;
- lower-precedence source displaced by an accepted higher-precedence source -> `DISPLACED_BY_HIGHER_PRECEDENCE`.

This design avoids inventing a rejection after the fact; the caller records the exclusion at the point where it occurs.

## Live-to-deep reconciliation
`liveHypothesisNotConfirmed(...)` preserves a named live hypothesis when completed-session analysis does not confirm it.

Examples:

- live CO/CO2/H2 suspicion followed by open-set rejection -> live hypothesis remains visible with `OPEN_SET_OOD`;
- live named hypothesis followed by domain rejection -> `OUT_OF_VALIDATED_DOMAIN`;
- live identity followed by mixture/specialist conflict -> retained with the relevant final analytical hold rather than disappearing.

## Reporting semantics
The eventual report section should be titled along the lines of:

**Signals observed but analytically rejected / withheld**

It must not phrase these rows as confirmed detections, confirmed absences, or quantitative exposure results.

Direct serialization into final PDF/Excel/JSON presentation is reserved for Stage 38, together with the progressive-state and OOD presentation contract.

## Tests
Regression tests cover:

- insufficient contributors;
- single-sensor screening-only evidence;
- out-of-domain evidence;
- explicit heater/hardware exclusions;
- open-set rejection retaining the top hypothesis;
- Stage-29 specialist conflict;
- live named hypothesis not confirmed by completed-session analysis.

## Status
Stage 31: IMPLEMENTED. Final COMPLETE status is recorded after the RC2 unit-test workflow including the rejected-evidence regression suite passes.
