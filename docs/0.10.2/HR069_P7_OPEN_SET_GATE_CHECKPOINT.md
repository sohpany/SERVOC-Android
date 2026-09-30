# HazRob 0.10.2 / HR069 — P7 mandatory open-set/OOD decision gate checkpoint

Date: 2026-09-30
Branch: `release/hazrob-0.10.2`
Baseline: `HR069`

## Scope

P7 begins by making the existing open-set/OOD layer a **mandatory fail-closed decision gate** before HazRob is allowed to promote a screening result to a named chemical identity or reportable concentration.

This phase does not claim that an empirical HR069 threshold has already been scientifically validated. Threshold calibration still requires independent known-vs-held-out-unknown HR069 sessions.

## Software contract implemented

1. `AcceptedFingerprintIdentityEngine` may produce a structured fingerprint match as evidence, but it cannot promote that evidence to `IDENTIFIED` unless an explicit `ChemCoreOpenSetGate.Result` exists and is accepted.
2. The legacy two-argument fingerprint fusion entry point is retained only for compatibility and now fails closed: it returns the base result unchanged.
3. Missing open-set/OOD evidence therefore leaves the result at screening/insufficient-evidence level; it cannot silently become a named identity.
4. Explicit open-set rejection remains `UNKNOWN`, including the no-inverse-solver/fingerprint-only path.
5. `UNKNOWN`, `OUT_OF_VALIDATED_DOMAIN`, mixture/interference and other non-promotable outcomes cannot be overwritten by fingerprint fusion.
6. An accepted open-set gate may permit a separated multi-channel source-backed fingerprint to reach M3 identity, but the fingerprint remains non-M4 and cannot create reportable PPM.
7. Quantification remains separately gated by M4 device-specific calibration, accepted identity/OOD state and validated contributor domains.

## Empirical threshold status

The software already provides:

- Mahalanobis feature-domain scoring;
- feature-coverage rejection;
- a validated-threshold factory;
- `OpenSetThresholdCalibrator` protocol `independent-known-vs-heldout-unknown/1`;
- known recall / false-reject rate;
- unknown recall / false-accept rate;
- balanced-accuracy threshold selection.

The next scientific task is to populate these threshold-calibration inputs with independent HR069 experimental sessions. Until that evidence exists, the application must not represent the open-set threshold as empirically qualified for production.

## P7 regression gates

The implementation is required to prove that:

- an exact accepted ethanol fingerprint cannot become a named identity when the open-set result is missing;
- an exact accepted fingerprint cannot override an explicit open-set rejection;
- the no-solver fingerprint path returns `UNKNOWN` when open-set rejects;
- the no-solver fingerprint path remains non-identifying when open-set evidence is absent;
- an accepted open-set result can still permit M3 fingerprint identity;
- fingerprint identity never yields reportable PPM.

## Validation status

Software implementation committed for internal validation. Full P7 scientific closure remains **pending independent HR069 known/unknown threshold-validation sessions** even after the software build/tests pass.
