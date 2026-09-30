# HazRob 0.10.2 / HR069 — P5 → P6 analytical evidence checkpoint

Date: 2026-09-30
Branch: `release/hazrob-0.10.2`
Baseline: `HR069`

## P5 closure state

The typed analytical-evidence foundation is accepted as the P5 phase baseline after the internal validation build at `d4ac539eef5a75fb689869b9ab5af1f6d6780674` passed:

- `audit_0105_analytical_engines.py`;
- Android unit tests;
- Android lint;
- signed internal Release assembly;
- Main ESP32 and ESP32-CAM compilation.

P5 emits `chemcore-evidence/1` observations with explicit evidence type, tier, source, model/family identifiers, sensor breadth, domain status, quantitative capability and `independenceGroup`. Fingerprint evidence remains identity-only unless a separate M4 quantitative calibration exists.

## P6 first implementation slice

P6 starts by correcting confidence fusion so correlated channels are not treated as independent proofs.

Rules implemented in `ChemCoreConfidenceMatrix.score(chemicalUid, evidence)`:

1. Confidence contributors are de-duplicated by `independenceGroup`.
2. If one independence group contains several evidence tiers, only its strongest tier contributes.
3. Sensor breadth is retained separately as a small supporting term; it does not create new independent evidence groups.
4. Evidence belonging to another chemical candidate does not contribute.
5. Out-of-domain evidence does not add positive confidence.
6. M0 catalog presence adds no analytical confidence.
7. M3 array-consistent evidence remains in the existing non-M4 supported-evidence weighting bucket until independent HR069 data justify a separately calibrated weight.
8. M4 remains the only tier eligible for device-validated quantitative interpretation; this P6 change does not promote M1/M2/M3 evidence.

The legacy count-based scoring API is retained temporarily for compatibility, but new P6 integration must use typed evidence.

## Required next P6 work

- route the candidate-level typed evidence list into the runtime confidence calculation;
- expose independent-evidence count separately from sensor breadth in audit/provenance output;
- calibrate confidence/decision thresholds on independent HR069 sessions rather than interpreting the operational score as a probability;
- preserve open-set, mixture/interference and M4 quantification gates as independent fail-safe controls;
- remove remaining live-path uses of raw contributor counts after typed fusion is fully integrated.
