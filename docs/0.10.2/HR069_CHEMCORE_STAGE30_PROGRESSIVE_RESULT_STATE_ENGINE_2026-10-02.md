# HazRob 0.10.2 / HR069 — Stage 30 Progressive Result-State Engine

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: ChemCore inference semantics only.

## Objective
Standardize the strongest scientifically defensible state reached by a session without treating posterior/model weight or the Analytical Confidence Index as the result state itself.

## Canonical states
`ChemCoreProgressiveResultState` version `chemcore-progressive-state/1` defines:

- `INVALID`
- `SIGNAL`
- `AFFINITY`
- `CANDIDATE`
- `IDENTIFIED`
- `QUANTIFIED`
- `ANALYTICALLY_REJECTED`
- `UNKNOWN_OOD`

The ordinary ladder is:

`SIGNAL -> AFFINITY -> CANDIDATE -> IDENTIFIED -> QUANTIFIED`

`ANALYTICALLY_REJECTED` and `UNKNOWN_OOD` are explicit terminal branches, not low numerical confidence values.

## Mapping from ChemCore outcome
- `INVALID_MEASUREMENT` -> `INVALID`, ladder 0.
- valid signal with no defensible candidate -> `SIGNAL`, ladder 1.
- M1 molecularly inferred candidate -> `AFFINITY`, ladder 2.
- M2+ supported candidate without accepted named identity -> `CANDIDATE`, ladder 3.
- `IDENTIFIED` -> `IDENTIFIED`, ladder 4.
- `QUANTIFIED` -> `QUANTIFIED`, ladder 5.
- `OUT_OF_VALIDATED_DOMAIN` -> `ANALYTICALLY_REJECTED`, retaining the furthest ordinary ladder level reached diagnostically.
- `UNKNOWN` / open-set rejection -> `UNKNOWN_OOD`.
- `POSSIBLE_MIXTURE_OR_INTERFERENCE` -> terminal `CANDIDATE`; a mixture suspicion is not a named identity.
- `INSUFFICIENT_EVIDENCE` -> terminal at the ordinary ladder level actually supported (signal, affinity or candidate).

## Semantics
The progressive state does not use candidate posterior as a promotion rule. A high posterior cannot turn an M1 affinity into an identified compound, cannot bypass OOD/domain rejection, and cannot authorize ppm.

Evidence tier, posterior/model weight, Analytical Confidence Index and progressive state therefore remain separate quantities.

## Reporting boundary
Stage 30 provides the canonical state projector and regression tests. Direct serialization into the final PDF/Excel/JSON presentation contract is reserved for Stage 38 so result-schema changes are made once, together with rejected-evidence and OOD reporting.

## Tests
Regression tests cover:

- all ordinary ladder levels;
- explicit `ANALYTICALLY_REJECTED` and `UNKNOWN_OOD` terminal branches;
- mixture/interference remaining at candidate level;
- insufficient evidence stopping at affinity or candidate depending on the actual evidence tier;
- posterior not being used as a result-state shortcut.

## Status
Stage 30: IMPLEMENTED. Final COMPLETE status is recorded after the RC2 workflow including `ChemCoreProgressiveResultStateTest` passes.
