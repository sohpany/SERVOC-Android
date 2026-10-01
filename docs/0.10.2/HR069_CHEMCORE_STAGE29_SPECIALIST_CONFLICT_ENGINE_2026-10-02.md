# HazRob 0.10.2 / HR069 — Stage 29 Specialist-Conflict Engine

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: identity-promotion veto based on Stage-7 anchor/specialist roles.

## Objective
Prevent a globally well-fitting candidate from being promoted to named identity when an independent primary specialist channel strongly supports a competing analyte hypothesis.

This stage is a veto/hold layer. It does not add candidate score, does not alter posterior/model ranking, and does not select an analyte.

## Runtime specialist map
`ChemCoreSpecialistRoleRegistry` transcribes the Stage-7 anchor/specialist map into runtime code.

### Primary anchors
- CO -> MQ7
- NH3 -> MQ135
- H2 -> MQ8
- CH4 -> MQ4
- Ethanol -> MQ3

### Secondary targets
- CO -> MQ9
- H2 -> MQ135
- CH4 -> MQ2, MQ5, MQ9
- Propane -> MQ2, MQ5
- Toluene -> MQ135
- LPG -> MQ2, MQ5
- Smoke -> MQ2, MQ135

A generic manufacturer graph response is not a primary specialist role.

## Strong specialist support
A primary specialist may participate in the conflict gate only when all of the following hold:

1. the candidate is solvable;
2. candidate-wide standardized residual RMS <= 3.0;
3. the primary-anchor evidence is inside its declared model domain;
4. the anchor observation is PRIMARY quality and M2 or higher;
5. absolute standardized anchor residual <= 1.5;
6. the candidate has at least two direct/reference M2+ contributors, **or** at least one M4 calibrated contributor.

Rule 6 is essential: a one-channel M2 hypothesis cannot manufacture a specialist veto merely because continuous concentration inversion can fit one monotonic curve exactly.

## Verdicts
`ChemCoreSpecialistConflictEngine` returns:

- `NO_SPECIALIST_EVIDENCE`
- `TOP_SPECIALIST_SUPPORTED`
- `SPECIALIST_CHALLENGER`
- `CROSS_SPECIALIST_CONFLICT`

## Decision consequences
- `TOP_SPECIALIST_SUPPORTED`: no identity hold.
- `SPECIALIST_CHALLENGER`: named identity is held; runtime outcome becomes `INSUFFICIENT_EVIDENCE` if no stronger earlier gate has already decided the session.
- `CROSS_SPECIALIST_CONFLICT`: named identity is held and runtime outcome becomes `POSSIBLE_MIXTURE_OR_INTERFERENCE` if no stronger earlier gate has already decided the session.

The gate is evaluated after common-C ranking/mixture consistency and before `IDENTIFIED` / `QUANTIFIED` promotion.

## Fingerprint bypass prevention
When `holdNamedIdentity=true`, `ChemCoreInferenceEngine` returns the held result directly and does not allow `AcceptedFingerprintIdentityEngine` to re-promote the session through a model-derived reference fingerprint.

## Ethanol/Hydrogen regression
A regression case explicitly represents the blind-challenge pattern:

- Hydrogen top-ranked with compatible MQ8 primary anchor;
- Ethanol lower-ranked but multi-channel eligible with compatible MQ3 primary anchor.

The expected Stage-29 result is `CROSS_SPECIALIST_CONFLICT`, not forced Hydrogen and not forced Ethanol.

This is deliberately analyte-generic. The test is a regression fixture, not a rule that tunes the engine to make the known ethanol challenge pass.

## Additional regression cases
Tests also verify that:

- single-channel exact-fit ethanol cannot create a veto;
- a strong specialist challenger can hold an unanchored top candidate;
- a top candidate with a compatible primary anchor and no competing specialist is not held;
- a genuinely M4-calibrated single-sensor specialist may participate in a conflict because its evidence is device/reference calibrated.

## Status
Stage 29: IMPLEMENTED. Final COMPLETE status is recorded after the RC2 unit-test workflow passes the specialist-conflict and upstream lineage regression suite.
