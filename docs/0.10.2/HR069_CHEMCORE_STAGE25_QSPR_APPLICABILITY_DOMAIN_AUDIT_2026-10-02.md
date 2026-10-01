# HazRob 0.10.2 / HR069 — Stage 25 QSPR Applicability-Domain Audit

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: ChemCore molecular/QSPR inference only.

## Decision
QSPR is retained as M1 molecularly inferred evidence only. It is not a calibration curve, does not establish chemical identity by itself, and cannot create reportable PPM.

## Problem identified
The previous QSPR implementation used a single full-feature distance that mixed molecular descriptors, concentration, temperature and relative humidity. Runtime training points are currently synthesized from accepted M2 response relations at a reference condition of 25 °C / 50 % RH. A single combined distance therefore did not provide a clear molecular applicability-domain decision and risked obscuring extrapolation.

## Implemented applicability-domain contract
A new `MolecularDescriptorApplicabilityDomain` is fitted independently for each MQ sensor model from the distinct molecular descriptor vectors of the compounds used to train that sensor-specific QSPR model.

The default descriptor vector remains the ChemCore CORE set:

- MolecularWeight
- TPSA
- HBondDonorCount
- HBondAcceptorCount
- RotatableBondCount
- HeavyAtomCount
- Charge
- Complexity

A target molecule is inside the sensor-specific molecular descriptor domain only when:

1. all required CORE descriptors are finite;
2. every descriptor remains within the observed training-compound min/max envelope; and
3. the RMS standardized descriptor distance is <= 3.0.

This threshold is a conservative research applicability proxy, not a calibrated probability or validated confidence interval.

## Concentration and environment remain separate domains
Descriptor-domain acceptance does not authorize concentration, temperature or humidity extrapolation. Those limits remain controlled by the response-model domain.

Because the present source-backed synthetic QSPR bench is trained at 25 °C / 50 % RH, the production `QsprResponseModelAdapter` now fails closed whenever `QsprMoxModel` reports a prediction outside its training envelope. Numerical out-of-domain predictions may still exist internally for validation diagnostics, but they are not supplied to runtime candidate ranking.

## Runtime integration
`ChemCoreQsprRuntimeRegistry` is now versioned as `pubchem-qspr-mox-runtime/3-ad`.

For each sensor:

- a QSPR model must first pass compound-level validation;
- a sensor-specific molecular descriptor domain is then fitted;
- each candidate molecule is evaluated against that domain;
- the candidate receives that QSPR sensor model only if it is inside the descriptor domain;
- direct/reference evidence still wins over QSPR on the same sensor.

## Scientific ceiling
Passing this applicability-domain gate means only that molecular extrapolation is not rejected by the current training descriptor space. It does not prove MQ detectability, selectivity, presence, concentration or HR069 transfer validity.

## Verification
New tests cover:

- acceptance of an interior molecular descriptor vector;
- rejection outside the descriptor min/max envelope;
- fail-closed behavior when a required descriptor is missing.

The complete RC2 Android unit-test and test-APK workflow passed after these changes at source head `2cda7bd744ad666ec33df6585e85fd0aa10a0329`.

## Stage status
Stage 25: COMPLETE as an applicability-domain policy and runtime gate. Material-specific QSPR availability remains dependent on Stage 24 molecular coverage and on sensor-specific training support.
