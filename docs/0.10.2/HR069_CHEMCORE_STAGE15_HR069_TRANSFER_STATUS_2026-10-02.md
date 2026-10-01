# HazRob 0.10.2 / HR069 — ChemCore Stage 15 HR069 Transfer-Status Assignment

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; curve-transfer state for the 14 curve-bearing entries.

## Objective

Separate three questions that must not be conflated:

1. Is an external/source response relation scientifically usable as prior evidence?
2. Is that relation transferable to the installed HR069 sensor/channel?
3. Is the relation validated for reportable quantitative ppm on HR069?

## Transfer-state vocabulary

- `SOURCE_PRIOR_ACCEPTED_TRANSFER_PENDING`
- `RESEARCH_PRIOR_TRANSFER_PENDING`
- `HR069_TRANSFER_VALIDATED_NON_M4`
- `HR069_M4_REFERENCE_VALIDATED`
- `TRANSFER_INCOMPATIBLE`
- `NOT_APPLICABLE_NO_CURVE`

A relation may support candidate ranking while remaining `TRANSFER_PENDING`.

## Current assignment

### A. Higher-provenance source priors — transfer still pending

The following catalogue entries have at least one peer-reviewed exact-model or literature relation in the current registries:

- 20 Carbon Monoxide
- 21 Ammonia
- 23 Carbon Dioxide
- 75 Hydrogen
- 76 Methane
- 95 Toluene
- 103 Ethanol
- 110 Acetone
- 141 NOx

State: `SOURCE_PRIOR_ACCEPTED_TRANSFER_PENDING`.

Reason: source-backed relations may be used as M1/M2 reference evidence inside their source domains, but Stage 14 found no structured proof that the source R0 convention and the current HR069 device-specific R0 are quantitatively interchangeable. None is promoted to M4 by literature provenance alone.

### B. Community/research/inherited curve priors — transfer pending

The remaining curve-bearing catalogue entries currently depend on inherited/research relations without an accepted higher-precedence exact source relation for the exact candidate:

- 34 Benzene
- 78 Propane
- 84 n-Hexane
- 135 LPG
- 139 Smoke

State: `RESEARCH_PRIOR_TRANSFER_PENDING`.

These may support family/fingerprint/candidate analysis where otherwise eligible, but do not create selective identity or reportable ppm.

### C. No registered numeric curve

The other 130 catalogue entries:

State: `NOT_APPLICABLE_NO_CURVE`.

This means “no current numeric response relation”, not “sensor cannot respond”. Their analysis may proceed through taxonomy, broad-array fingerprints, external transfer evidence, temporal features, QSPR, dedicated-sensor compatibility or experimental validation.

## Relationship to accepted transfer fingerprints

Existing third-party transfer fingerprint assets are useful research identity/reference evidence but do not change curve-transfer state to M4. Current project documentation explicitly keeps external transfer assets out of local reference-artifact authority and states that they do not self-promote to device/reference-gas validation.

Therefore:

- source-backed fingerprint transfer ≠ curve transfer validation;
- source known-concentration dataset ≠ HR069 M4;
- exact installed sensor model in a paper ≠ calibration certificate for the individual HR069 sensor.

## Promotion requirements

A curve can advance to `HR069_TRANSFER_VALIDATED_NON_M4` only after controlled HR069 data show acceptable source-to-device transfer under documented conditions, including R0 convention, heater state, concentration domain and environment.

A curve can advance to `HR069_M4_REFERENCE_VALIDATED` only after the M4 campaign requirements are met with known reference concentrations and uncertainty characterization.

Candidate transfer form may be tested in log space:

`log10(Rs/R0_source) = alpha + beta * log10(Rs/R0_HR069)`

with `alpha`, `beta`, residual error and applicability domain estimated from controlled data. Identity transfer and quantitative calibration remain separate validations.

## Catalogue accounting

- Reviewed: 144/144.
- `SOURCE_PRIOR_ACCEPTED_TRANSFER_PENDING`: 9.
- `RESEARCH_PRIOR_TRANSFER_PENDING`: 5.
- `NOT_APPLICABLE_NO_CURVE`: 130.
- `HR069_TRANSFER_VALIDATED_NON_M4`: 0 from curve evidence alone.
- `HR069_M4_REFERENCE_VALIDATED`: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 16 applies boundary/out-of-domain behavior uniformly to these relations. Stage 17 then defines cross-channel concentration consistency without allowing single-channel overfit to masquerade as multi-sensor confirmation.