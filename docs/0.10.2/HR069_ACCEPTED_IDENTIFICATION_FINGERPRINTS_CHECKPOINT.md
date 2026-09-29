# HR069 Accepted Identification Fingerprints — checkpoint

Date: 2026-09-29  
Release: 0.10.2  
Hardware revision: HR069  
Branch: `release/hazrob-0.10.2`

## Decision

Available uploaded/external measurements and source-backed response relations are accepted for **identification use now**. They may be transfer-recalibrated or replaced by stronger HR069 measurements later.

This decision does **not** make external fingerprints device/reference calibration. M4 remains false for these references, and reportable/safety-grade PPM still requires the existing ChemCore M4 quantitative gate.

## Implemented

1. `ResearchIdentificationFingerprintRegistry` now records `ACCEPTED_FOR_IDENTIFICATION` while preserving `RESEARCH_EXTERNAL` and `M4=false`.
2. `AcceptedFingerprintIdentityEngine` is wired into `ChemCoreInferenceEngine.analyze`.
3. Accepted fingerprint matching uses the installed HR069 order: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135.
4. MQ7/MQ9 remain fail-closed unless manufacturer heater-cycle validity is explicitly true.
5. A fingerprint can promote only `SCREENING` or `INSUFFICIENT_EVIDENCE` to `IDENTIFIED / M3_ARRAY_CONSISTENT`.
6. It cannot override validated `UNKNOWN`, mixture/interference, out-of-domain, invalid-measurement, or existing quantified outcomes.
7. Fingerprint promotion never creates M4 and sets no reportable PPM.
8. Food Freshness measured headspace fingerprints are preserved separately as 59 accepted class/headspace references. They are not relabelled as pure chemicals.

## Identity gate v1

- engine: `accepted-id-fp-v1`
- minimum overlapping HR069 channels: 2
- maximum standardized fingerprint distance: 1.50
- separation: score margin >= 5 points OR runner-up distance margin >= 0.20
- source-backed template space: `log10(Rs/R0)`
- Food Freshness headspace vector space: geomean-normalized measured cross-sensor shape; separate from the pure-analyte Rs/R0 classifier

## Current pure-analyte numeric reference coverage

The source-backed numeric fingerprint registry currently includes 11 individual analytes:

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

Not every analyte will pass named-identity promotion on every measurement. The runtime still requires at least two overlapping independent HR069 channels and a separated match. Single-channel priors remain screening evidence until more transfer/HR069 data are available.

## Food/spoilage transfer targets retained but not fabricated

- H2S: manufacturer affinity only; exact HR069 numeric fingerprint pending
- Acetaldehyde: source pending
- Ethylene: source pending
- Methanol: external transfer measurements accepted; exact present-protocol HR069 vector pending
- Ethyl acetate: external known-concentration transfer measurements accepted; exact HR069 vector pending

## Food Freshness source preservation

Source DOI: `10.5281/zenodo.17285312`  
Archive MD5: `008b8ccb62d7a7d51ff9ffcfd698a1e5`  
Archive SHA-256: `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`  
Derived accepted fingerprint asset: `research_data/hr069_analytical_library/food_freshness_headspace_fingerprints_v1.json`

The asset contains 59 measured headspace/session vectors over the eight HR069-overlap MQ channels.

## Code/files

- `app/src/main/java/com/servoc/app/fingerprint/ResearchIdentificationFingerprintRegistry.java`
- `app/src/main/java/com/servoc/app/fingerprint/FingerprintClassifier.java`
- `app/src/main/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngine.java`
- `app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java`
- `app/src/test/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngineTest.java`
- `research_data/hr069_analytical_library/identification_fingerprint_priors_v1.json`
- `research_data/hr069_analytical_library/food_freshness_headspace_fingerprints_v1.json`

## Qualification status

Code integration committed. Unit/qualification workflow must pass before this checkpoint is treated as release-qualified. Record the workflow run and final source SHA below after qualification.

- qualification: PENDING
- qualified source SHA: PENDING
- workflow run: PENDING

## Next work

1. Convert any newly obtained external pure-analyte arrays into exact HR069-order transfer vectors with provenance.
2. Add Methanol and Ethyl Acetate to direct fingerprint classification only when the raw/source measurements can be mapped without inventing missing channels.
3. Seek/derive Acetaldehyde, Ethylene and H2S source-backed vectors.
4. Recalibrate accepted references with HR069 M4 experiments as reference gases become available.
5. Keep original external records immutable; revisions create new transfer-calibration versions rather than overwriting source evidence.
