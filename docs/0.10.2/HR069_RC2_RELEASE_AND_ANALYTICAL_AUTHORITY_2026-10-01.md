# HazRob release and analytical authority — 2026-10-01

## Decision status

**ACTIVE / CANONICAL**

This record establishes the authoritative HazRob release and analytical baseline as of 2026-10-01.

## 1. Authoritative application release

The latest and only active application/release line is:

- Release candidate: **RC2**
- Android version: **0.10.2**
- Engineering revision: **HR069**
- versionCode: **26**
- source repository: `sohpany/SERVOC-Android`
- source branch: `release/hazrob-0.10.2-rc2`
- RC2 application baseline SHA before this documentation-only commit: `22820dc365ef634e9b9e5e0d685ffdb53dcbbe35`
- database schema: **9**
- Main ESP32 API: **HazRob API v4 / firmware 0.10.2**

All future production, analytical, calibration, report, APK and firmware work must be reconciled against this line unless a later release is explicitly approved in a new authority record.

## 2. 0.10.3 / HR070 withdrawal

**HazRob 0.10.3 / HR070 is CANCELLED, WITHDRAWN and NON-AUTHORITATIVE.**

The branch `release/hazrob-0.10.3-hr070` and any organization-side HR070 branch/artifact are retained only as historical/recovery evidence until manually deleted. They must not be used as:

- a release source;
- a merge base for new HazRob work;
- an APK/firmware build target;
- a ChemCore scientific authority;
- a calibration authority;
- an analytical-library authority;
- a report-methodology authority.

No HR070-only change is accepted into the active product merely because it is newer by version number. Any useful change must be reviewed and explicitly reintroduced into RC2/HR069 or a later formally approved successor.

## 3. Analytical-system authority model

The analytical system is intentionally divided into distinct authorities.

### 3.1 Production inference authority

The production analytical inference engine is the **ChemCore implementation inside RC2/HR069** under:

`app/src/main/java/com/servoc/app/chemcore/`

This includes the controlled inference, open-set/OOD rejection, evidence fusion, accepted-fingerprint identity path, reference-response fitting, mixture/interference guards, session dynamics, confidence handling and M4 quantitative gating.

### 3.2 Canonical current analytical evidence corpus

The richest and most current qualified analytical-data corpus present in the active RC2 source is:

`research_data/hr069_analytical_library/`

**This directory is adopted as the canonical current analytical evidence corpus for RC2/HR069.**

It is richer than HR070 and currently richer than the synchronized subset in `sohpany/HazRob-Lab` because it contains the latest HR069 transfer/evidence additions as well as the previously synchronized core assets.

### 3.3 Research and validation environment

`sohpany/HazRob-Lab` remains the independent research, calibration, validation and scientific-development environment. It is not the production runtime and is not allowed to override RC2 automatically.

New research should be staged and validated in HazRob-Lab where practical, then synchronized into the canonical RC2 analytical corpus only with provenance, schema compatibility and evidence-level controls. Until the current RC2 additions are fully synchronized back to HazRob-Lab, the RC2 corpus remains the current data authority.

### 3.4 Organization build/qualification authority

`HazRob/build-harness` is a build/qualification harness only. It is **not** the application source tree and **not** the scientific data source of truth. It may qualify an explicitly pinned RC2 source SHA and preserve build/test evidence.

## 4. Canonical RC2 analytical corpus — verified content

The adopted HR069 corpus contains the following major evidence layers.

### Chemical identity layers

- 144-entry HazRob legacy analytical catalogue.
- 652-record pinned NIOSH/molecular identity layer.
- 615 resolved molecular records and 37 unresolved records in the current molecular-resolution status.
- evidence provenance and explicit unresolved/mapping queues rather than fabricated identities.

### MQ response and model layers

- **43** MQ research response curves with provenance and declared validity domains.
- **76** MQDataScience model definitions in the research model catalogue.
- 14 Android-native model ports, 12 upstream-verified and 2 explicitly marked approximations; the remaining models stay research/lab models until qualified for runtime use.

### Accepted numeric identification fingerprints

`identification_fingerprint_priors_v1.json` contains:

- **11 pure-analyte numeric fingerprint families**: Carbon Monoxide, Ammonia, Carbon Dioxide, Benzene, Hydrogen, Methane, Propane, n-Hexane, Toluene, Ethanol and Acetone.
- **2 operational-class numeric fingerprint families**: LPG and Smoke.
- **13 total numeric fingerprint families**.
- vector space: `log10(Rs/R0)`.
- physical HR069 sensor order: `MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135`.
- concentration template grid: 10/25/50/100/200/300/500/1000/3000/5000/10000 ppm, with each sensor/analyte channel limited to its source-supported domain.

### Measured/derived external fingerprint layers

- Food Freshness: **59 measured headspace/session fingerprints** over the HR069-overlap array; retained as headspace/class evidence, not relabelled as pure analytes.
- Multimodal MQ transfer set: **6400 samples**, four classes (`NoGas`, `Smoke`, `Perfume`, `Mixture`) and six HR069-overlap MQ channels.
- UCI MQ135 low-concentration transfer set: **18 analyte/concentration records** across six analytes at 0.05/0.10/0.20 ppm, five replicates per analyte/concentration; one HR069-overlap channel, therefore transfer/reference evidence rather than direct multi-channel identity.
- RoboFusion real operational gas-leak transfer set: **69,120 total rows**, with **42,704 original/non-interpolated rows** retained for canonical prototypes across six HR069-overlap MQ channels; operational `Normal` and `Hazard_1` classes only, with no invented chemical identity.

### Same-sensor peer-reviewed identity evidence

The current same-sensor peer-reviewed evidence registry provides accepted identity support for at least these legacy IDs:

- Toluene (95)
- Methanol (102)
- Ethanol (103)
- 1-Propanol (104)
- Isopropanol (105)
- 1-Butanol (106)
- Acetone (110)
- Ethyl Acetate (115)

It also tracks exact extended identities including Ethylene glycol, Benzaldehyde and 1-Octanol, plus review queues for ambiguous/isomer mappings.

### Additional transfer evidence retained in the canonical corpus

- **Methanol**: peer-reviewed same-array study with all eight HR069 MQ models represented in the source array; 168 recordings, 11 sensors and 401 samples/sensor/recording reported; accepted as transfer identity evidence, not HR069 M4.
- **Ethylene**: exact MQ3 known-concentration study at 10/25/40/50 ppm across 0/33/64% RH; accepted transfer evidence pending reproducible numeric source response and HR069 transfer validation.
- **1-Propanol**: peer-reviewed same-array evidence with seven of eight HR069 MQ channels, 10–100 ppm domain and 10 independent repeats per VOC; accepted identity transfer evidence pending raw numeric vectors.
- **Kononov MOX numeric transfer batch**: numeric cross-device matrices for n-Heptane, o-Xylene and Ethylbenzene across multiple concentration levels and multi-state MOX measurements; transfer evidence only until source-to-HR069 validation.
- experiment/source index covering exact/superset HR069 MQ arrays, mixture experiments, CO studies, food/headspace datasets and same-model evidence priorities.

## 5. Runtime scientific rules

Adoption of the richest corpus does **not** relax scientific gates.

1. External/source-backed evidence remains `RESEARCH_EXTERNAL` unless separately qualified.
2. External evidence does not self-promote to `M4_HAZROB_VALIDATED`.
3. Accepted fingerprint identity may support `IDENTIFIED / M3_ARRAY_CONSISTENT` only after the open-set/OOD gate and required channel/separation criteria.
4. Fingerprint identity does not create reportable occupational/safety PPM.
5. Source-backed PPM relations may be used for candidate fitting/ranking/provisional reference hypotheses only inside their declared domains.
6. Reportable quantitative PPM requires HR069 device/reference M4 calibration and all existing quantitative gates.
7. MQ7/MQ9 manufacturer-curve use remains fail-closed while the installed common heater rail cannot satisfy their required cyclic heater protocol.
8. Missing sensor channels must remain missing; no source-to-HR069 channel may be fabricated or substituted.
9. Headspace, food, mixture, smoke and operational hazard classes remain class/mixture evidence and must not be silently converted into pure-chemical identities.
10. Duplicate/mirror datasets do not count as independent evidence.

## 6. Authority order from this checkpoint forward

When records conflict, use this order:

1. **Release identity:** `release/hazrob-0.10.2-rc2` / 0.10.2 / HR069.
2. **Production analytical runtime:** RC2 ChemCore.
3. **Current canonical analytical evidence corpus:** RC2 `research_data/hr069_analytical_library/`.
4. **Scientific research/staging environment:** `sohpany/HazRob-Lab`.
5. **Build/qualification evidence:** `HazRob/build-harness`.
6. Historical branches including 0.10.3/HR070 are non-authoritative.

## 7. Change-control rule

No analytical dataset, fingerprint, response curve, model or calibration may replace this baseline silently. A future change must record:

- source/provenance;
- file/content hash where applicable;
- analyte/class mapping;
- sensor/channel mapping;
- concentration and environmental domain;
- evidence tier;
- M4 status;
- runtime eligibility;
- validation/test result;
- revision/date.

This document supersedes any prior statement that treats 0.10.3/HR070 as the latest HazRob release or `HazRob/build-harness` as the scientific source of truth.