# HazRob 0.10.2 / HR069 — ChemCore Stage 20 Exact-Compound Fingerprint Review

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries.

## Objective

Determine which current fingerprint records can support exact-compound identity and define their present authority ceiling.

## Current exact-compound inventory

### A. Model-derived pure-analyte numeric priors — 11

- Carbon Monoxide
- Ammonia
- Carbon Dioxide
- Benzene
- Hydrogen
- Methane
- Propane
- n-Hexane
- Toluene
- Ethanol
- Acetone

These templates are generated from source-backed response models in `ResearchIdentificationFingerprintRegistry`. They use `sampleCount=0`, per-channel uncertainty proxies and concentration-conditioned templates. They are accepted as research identity priors but are not HR069 empirical M3 fingerprints and are not M4.

### B. External-transfer evidence without an exact HR069 numeric vector — 5

- Hydrogen Sulfide
- Acetaldehyde
- Ethylene
- Methanol
- Ethyl Acetate

These records can guide transfer experiments and family/candidate plausibility but cannot be classified as an exact HR069 fingerprint until a defensible local numeric vector is available.

### C. Operational/composite templates — excluded from exact-compound authority

- LPG
- Smoke

They have model-derived numeric templates but remain operational/composite classes. They do not count as exact-compound fingerprints.

### D. Remaining entries

The remaining pure analytes have no accepted static exact-compound fingerprint in the current branch. Mixture/aggregate entries are not forced into exact-compound semantics.

## Exact-compound identity ceiling

At Stage 20:

- model-derived pure-analyte prior -> maximum provisional candidate/identity-reference evidence, not independently validated M3;
- external transfer nonnumeric -> supporting transfer evidence only;
- HR069 empirical fingerprint with independent validation -> none currently accepted;
- covariance/open-set validated exact-compound M3 fingerprint -> none currently accepted;
- M4 quantitative authority from fingerprint alone -> prohibited.

## Minimum requirements for future M3 exact-compound promotion

An exact-compound fingerprint must document:

1. analyte identity and source preparation;
2. controlled concentration/exposure protocol;
3. independent HR069 sessions across multiple days/blocks;
4. T/RH/P, heater state, R0 source and sensor-health metadata;
5. repeatability and within-class variance;
6. separation from chemically/sensorially neighboring classes;
7. held-out validation with session-level splits;
8. open-set/OOD testing against unrelated compounds and mixtures;
9. no leakage from repeated samples of the same exposure into train/test;
10. decision threshold and uncertainty documented before release.

## Concentration-conditioned identity

Where fingerprint shape changes with concentration, exact identity may use a concentration-conditioned manifold/grid rather than one fixed centroid. However, concentration grids derived from the same source curves are not statistically independent evidence from those curves and must retain the same evidence lineage.

## Specialist interaction

A model-derived exact-compound fingerprint may corroborate a specialist/anchor signal but cannot erase a specialist conflict. Conversely, a specialist channel cannot promote an otherwise poor or OOD fingerprint silently. Stage 29 will formalize this conflict gate.

## Catalogue accounting

- Reviewed: 144/144.
- Pure-analyte model-derived numeric priors: 11.
- Pure-analyte external-transfer/non-numeric evidence: 5.
- Operational/composite numeric templates: 2, not exact-compound fingerprints.
- Accepted static HR069 empirical M3 exact-compound fingerprints: 0.
- M4 exact-compound fingerprints: 0.
- All remaining entries retain an explicit fingerprint evidence gap or non-pure N/A state.

## Stage result

**COMPLETE — 144/144.**

Stage 21 audits covariance readiness and prevents deterministic/model-derived templates from being assigned fabricated statistical covariance.