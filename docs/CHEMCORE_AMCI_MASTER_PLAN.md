# HazRob ChemCore / AMCI Master Development Plan

**Status:** ACTIVE
**Branch:** `dev/hazrob-0.10-chemcore-foundation`
**Baseline:** `dev/hazrob-0.9.3-rc2-l298n-drive`
**Purpose:** transform HazRob from a mobile multi-gas screening robot into a scientifically controlled, uncertainty-aware chemometric analytical platform while retaining the current eight-MQ sensor array and existing environmental/ADC hardware.

---

## 1. System definition

HazRob is decomposed into three layers:

1. **HazRob Mobile Platform** — motion, video, remote control, fail-safe and spatial reconnaissance.
2. **ChemCore** — controlled acquisition, feature extraction, molecular/physical knowledge, evidence fusion, compound identification, analyte-specific quantification, uncertainty and traceability.
3. **AMCI (Adaptive Multisensor Chemical Interrogation)** — decision layer that determines whether the current evidence is sufficient or whether the system must extend exposure, wait for stability, observe recovery, repeat a measurement, or move to a new spatial sampling location.

The analytical objective is not elemental decomposition and not GC-MS-equivalent identification. The scientific model is a **cross-reactive electronic nose / chemometric sensor array** in which the complete spatiotemporal response of eight MQ channels is interpreted jointly with environmental, physical, molecular and evidence-domain constraints.

---

## 2. Current retained sensing hardware

The initial ChemCore development deliberately retains the current installed analytical sensing hardware:

- MQ-2
- MQ-3
- MQ-4
- MQ-5
- MQ-7
- MQ-8
- MQ-9
- MQ-135
- dual ADS1115 external ADCs
- BME280/BMP280 environmental sensing path
- current ESP32 and Android architecture
- current reference experiment/calibration database and provenance framework

No new chemical sensor is required for the first ChemCore release line.

---

## 3. Controlled sampling chamber — current physical specification

### 3.1 Chamber geometry

Declared chamber internal envelope for the first prototype:

- width: **9 cm**
- height: **4 cm**
- length: **15 cm**
- nominal gross volume: **540 cm³ = 0.54 L**
- front air inlet: **4 × 4 cm = 16 cm²**
- chamber cross-section: **9 × 4 cm = 36 cm²**
- rear extraction fan: **40 × 40 mm, 5 V DC, 0.28 A**, brushless axial fan
- nominal electrical fan input: **1.4 W**

The fan is to be installed at the rear as an extractor so that sample air is pulled through the chamber rather than pushed across it by the fan motor.

### 3.2 Provisional airflow engineering estimate

The exact fan model and certified airflow are not visible on the supplied label, therefore airflow **must not be treated as calibrated yet**. A comparable Xinbinming/XBMI 40×40×20 mm family is published in the approximate free-air range of 5.74–15.68 CFM. This is used only as a provisional engineering envelope until direct measurement.

For that provisional range:

- volumetric flow Q ≈ **2.71–7.40 L/s** free-air
- ideal mean velocity through the 4×4 cm inlet ≈ **1.69–4.63 m/s**
- ideal mean velocity through the 9×4 cm chamber cross-section ≈ **0.75–2.06 m/s**
- ideal gross chamber residence time V/Q ≈ **0.073–0.199 s**

These are **upper-bound/free-air calculations**, not validated in-chamber values. Sensor bodies, grills, wiring, wall friction, inlet contraction and pressure losses can materially reduce flow.

### 3.3 Mandatory later measurement

Before any flow-dependent model is promoted to validated status, measure actual in-chamber mean air velocity and calculate:

`Q = v × A`

and

`tau = V_effective / Q`

The database must distinguish:

- `FLOW_UNCALIBRATED`
- `FLOW_ESTIMATED`
- `FLOW_MEASURED`
- `FLOW_CALIBRATED`

No quantitative result may claim flow compensation when the sampling profile is uncalibrated.

---

## 4. Scientific operating modes

### 4.1 SURVEY

Robot may move. Purpose is anomaly detection, plume localization, trend comparison and operator warning. Results remain screening-level.

### 4.2 CHEMCORE_ANALYSIS

Robot must stop. Drive commands are inhibited or strongly restricted. Fan operates under the declared sampling profile. The analytical cycle becomes:

`CONDITION -> BASELINE -> EXPOSURE -> STABILITY_CHECK -> FEATURE_EXTRACTION -> IDENTITY -> OPEN_SET_GATE -> QUANTIFICATION -> CONSISTENCY_CHECK -> REPORT -> RECOVERY`

### 4.3 AMCI_CONFIRMATION

If identity/quantification uncertainty is excessive, AMCI requests one of:

- extended exposure
- stability wait
- recovery observation
- repeated exposure
- new baseline
- spatial confirmation at another robot position
- abort / UNKNOWN if evidence remains insufficient

---

## 5. Evidence hierarchy

ChemCore must never blur theoretical support and device validation. Every analytical claim receives an evidence tier:

- **M0 — Catalog only:** identity/exposure knowledge exists; no usable sensor-response evidence.
- **M1 — Molecularly inferred:** QSPR/physical/molecular prior supports a possible response; not calibrated.
- **M2 — Literature/manufacturer supported:** source-backed sensor response or curve exists but is not device-specific.
- **M3 — Array-consistent:** multiple current channels jointly support the identity inside model domain.
- **M4 — HazRob calibrated/validated:** device-specific reference experiments establish the operational identity and/or concentration model.

Quantitative occupational interpretation is enabled only when the relevant measurement meets its required operational tier and validity domain.

---

## 6. Core scientific architecture

### 6.1 Unified signal chain

`ADS raw -> voltage -> Rs -> Rs/R0 -> QC -> baseline-normalized trace -> synchronized feature vector -> identity models -> OOD/UNKNOWN gate -> analyte-specific quantification -> uncertainty -> exposure interpretation`

### 6.2 Unified ChemCore feature pipeline

The existing multiple definitions of fingerprint/temporal features are to be replaced by one canonical pipeline used by reference creation, training, runtime analysis and reporting.

Candidate per-sensor features:

- valid fraction
- baseline log-ratio
- steady-state log-ratio
- signed delta from baseline
- absolute delta
- min
- max
- peak
- initial response slope
- late slope / stability slope
- time to 10%
- time to 50%
- time to 90%
- signed AUC
- absolute AUC
- baseline noise SD
- SNR
- recovery 50% / 90% when recovery data exist

Cross-sensor derived features are added only under controlled feature-selection rules, e.g. common-mode rejected response and selected pairwise ratios/differences.

### 6.3 Time alignment

Because the eight MQ channels are scanned sequentially, the scientific data model must store enough timing information to reconstruct acquisition order:

- session timestamp
- scan cycle id or scan timestamp
- channel timestamp/offset where available

The analytical pipeline may resample to a common time grid when necessary.

---

## 7. Molecular Intelligence Layer

The >600-record occupational/chemical knowledge base is to be extended into a molecular/physical knowledge system. Existence in the knowledge base does **not** imply detectability.

### 7.1 Chemical digital twin fields

For each compound, support provenance-tagged values for:

**Identity/structure**
- CAS
- PubChem CID
- formula
- canonical/isomeric SMILES
- InChI / InChIKey
- chemical family
- functional groups
- molecular fingerprints/descriptors

**Molecular/physical**
- molar mass
- exact mass
- boiling point
- melting point where relevant
- vapor pressure with temperature relation/conditions
- critical properties when available
- gas density or computable density basis
- relative vapor density to air
- diffusion coefficient when authoritative data exist
- kinetic diameter when authoritative data exist
- Henry constant where relevant

**Electronic/reactivity descriptors**
- ionization energy
- electron affinity
- dipole moment
- polarizability
- electrophilicity / hardness descriptors where method is documented
- HOMO/LUMO only with calculation/source provenance

**Safety/occupational**
- LEL/UEL
- flash point / autoignition when relevant
- NIOSH/OSHA limits kept in their own evidence layer

Each property must carry unit, conditions, source, method and uncertainty/quality metadata when available.

### 7.2 Derived physical calculations

ChemCore may derive quantities such as ideal-gas density and ppm<->mg/m³ conversion using current T/P, but derived values must be marked as calculated and must retain formula/provenance version.

### 7.3 QSPR-MOX predictor

Research model target:

`response = f(sensor_id, molecular_descriptors, concentration, T, RH, sampling_profile)`

Predicted curves are **priors**, never equivalent to calibrated curves. Model uncertainty must increase outside the descriptor/training domain.

### 7.4 Inverse molecular solver

For each candidate compound c, estimate the concentration C that minimizes disagreement between observed and predicted/calibrated multisensor response. The candidate score must incorporate sensor/model uncertainty rather than simple unweighted averaging.

### 7.5 Bayesian evidence fusion

Combine:

- observed sensor likelihood
- manufacturer/literature evidence
- molecular/QSPR prior
- environmental and phase plausibility
- calibration evidence
- open-set distance

while preserving evidence provenance and the distinction between inference and validation.

---

## 8. Classification architecture

### 8.1 Hierarchical classification

Prefer:

`chemical anomaly -> family -> compound -> quantitative model`

rather than a single flat 600-class classifier.

### 8.2 Baseline model bench

Use the existing 76-model research registry as a controlled benchmark library, not as a claim that 76 algorithms are operational. The first validated comparison set should include interpretable and strong structured-data baselines such as PCA diagnostics, LDA/PLS-DA, SVM-RBF and tree/boosting ensembles where dependencies permit.

### 8.3 Open-set recognition

Runtime must be allowed to return:

- `UNKNOWN`
- `INSUFFICIENT_EVIDENCE`
- `OUT_OF_VALIDATED_DOMAIN`
- `POSSIBLE_MIXTURE_OR_INTERFERENCE`

No classifier is permitted to force every sample into a known chemical.

---

## 9. Quantification architecture

### 9.1 Direct empirical calibration

Retain and strengthen current no-extrapolation empirical log-log interpolation/curve logic.

### 9.2 Multisensor concentration model

After identity is supported, compare single-sensor and multisensor concentration models using independent sessions.

### 9.3 Uncertainty-aware consensus

Contributor weights should depend on calibration error, sensor health, environmental domain, range and drift state. Quantitative reporting should eventually include prediction intervals.

### 9.4 Mixture guard

Before assigning a pure-compound concentration, compare the observed array vector with the response expected from the proposed pure compound. Large structured residuals trigger `POSSIBLE_MIXTURE_OR_INTERFERENCE` rather than forcing a pure-gas result.

---

## 10. Database development

ChemCore schema target includes:

- sampling profiles
- chamber geometry/revision
- airflow status/value/unit/uncertainty
- molecular structures
- provenance-rich chemical properties
- molecular descriptor sets
- model artifacts/model cards
- model validity domains
- analytical decisions with evidence tier
- channel timing metadata
- QA state and promotion status

Existing experiment/reference/calibration provenance remains preserved.

---

## 11. Validation and scientific quality rules

Experiments remain important but are repositioned as calibration, validation and model correction rather than the sole source of intelligence.

Mandatory rules:

- split train/validation/test by independent experiment/session/day, never random rows from one exposure
- keep manufacturer/literature/QSPR/device-calibrated curves distinct
- derive LOD/LOQ from HazRob data, not datasheet marketing values
- characterize ADC gain/offset using known reference voltages
- measure actual RL for each MQ module before quantitative publication
- validate actual ADS supply/reference domain
- characterize fan-on/fan-off blank effect
- characterize motors-on/motors-off interference
- perform chamber blank/outgassing tests
- track sensor drift and calibration age

---

## 12. Development work packages and acceptance criteria

### WP0 — Research branch and persistent roadmap
**Status: COMPLETE**
- create dedicated branch `dev/hazrob-0.10-chemcore-foundation`
- create this canonical plan
- establish progress ledger

**Acceptance:** branch exists and roadmap is committed.

### WP1 — ChemCore Foundation / unified feature contract
**Status: IN PROGRESS**
- introduce canonical `ChemCoreFeaturePipeline`
- preserve signed temporal response
- remove feature-definition ambiguity from new code paths
- add explicit analytical result/status vocabulary
- unit-test deterministic feature extraction

**Acceptance:** one canonical feature contract exists with tests; legacy paths may remain temporarily but must not be used to create new ChemCore reference artifacts.

### WP2 — Sampling Profile and Chamber Metrology
**Status: PLANNED**
- create `SamplingProfile` model
- persist geometry: 9×4×15 cm, 4×4 cm inlet, rear 40 mm extractor
- persist fan electrical specification 5 V / 0.28 A
- support unknown/estimated/measured/calibrated airflow status
- calculate chamber volume, cross-sectional velocity and residence time when Q is known

**Acceptance:** every ChemCore analysis session references a versioned sampling profile.

### WP3 — ChemCore session state machine
**Status: PLANNED**
- add CONDITION/BASELINE/EXPOSURE/STABILITY/RECOVERY states
- add stationary-analysis interlock
- preserve current manual override behavior outside ChemCore mode

**Acceptance:** a ChemCore session cannot silently collapse into the old single averaging phase.

### WP4 — Open-set / uncertainty foundation
**Status: PLANNED**
- introduce UNKNOWN and out-of-domain decisions
- add baseline anomaly score
- add candidate uncertainty/result provenance structure

**Acceptance:** unknown synthetic/out-of-domain inputs are not forced into a named compound.

### WP5 — Molecular Intelligence schema
**Status: PLANNED**
- add provenance-rich property tables
- separate stored vs calculated properties
- add structure/descriptors table
- add source/method/conditions fields

**Acceptance:** at least the legacy 144 catalogue and imported NIOSH/PubChem-linked identities can receive molecular descriptors without altering exposure tables.

### WP6 — Physics/Molecular prior engine
**Status: PLANNED**
- molecular feasibility gates
- ideal-gas transforms
- volatility/phase plausibility hooks
- sensor-response prior interface

**Acceptance:** molecular priors can rank or reject hypotheses without being mislabeled as calibrated sensor results.

### WP7 — QSPR-MOX research engine
**Status: PLANNED**
- build descriptor matrix ingestion
- model-training/evaluation contract
- prediction uncertainty and domain checks
- predicted sensor-response envelope

**Acceptance:** predicted curves are tagged `MODEL_PREDICTED` and can never promote quantitative status without calibration evidence.

### WP8 — Inverse Molecular Solver / Bayesian fusion
**Status: PLANNED**
- fit candidate concentration against the full array
- uncertainty-weighted residual scoring
- combine molecular/literature/calibration evidence

**Acceptance:** solver produces auditable candidate scores and can return insufficient evidence.

### WP9 — AMCI adaptive measurement controller
**Status: PLANNED**
- stability-driven extension
- recovery/repeat decision
- entropy/information-gain hooks
- spatial confirmation request

**Acceptance:** AMCI can request more information rather than forcing an answer.

### WP10 — Mixture/interference guard
**Status: PLANNED**
- pure-compound residual test
- structured residual threshold
- mixture/interference result state

**Acceptance:** a deliberately incompatible composite signal is not reported as a high-confidence pure compound.

### WP11 — Scientific reporting/model cards
**Status: PLANNED**
- evidence tier
- model source/version/hash
- validation domain
- calibration range
- uncertainty
- sampling profile
- candidate alternatives/OOD state

**Acceptance:** PDF/XLSX outputs can distinguish screening, inferred identity, validated identity and quantitative result.

### WP12 — Bench and chamber validation
**Status: BLOCKED ON PHYSICAL MEASUREMENTS**
- airflow measurement
- chamber blank test
- ADC reference-voltage validation
- fan electrical/interference test
- motor interference test
- RL measurement
- R0/device calibration work

**Acceptance:** measured values replace provisional assumptions in the research freeze.

### WP13 — Research Freeze v1.0
**Status: PLANNED**
- blind independent validation
- version/commit freeze
- BOM and geometry freeze
- final model cards
- paper-ready metrics and limitations

---

## 13. Progress ledger

### 2026-09-09 — Checkpoint C0

**Completed**
- dedicated ChemCore branch created from HazRob 0.9.3-rc2
- chamber dimensions formalized: 9×4×15 cm, 0.54 L gross volume
- inlet formalized: 4×4 cm
- fan electrical label recorded: 5 V, 0.28 A, 40×40 mm, rear extraction configuration
- provisional airflow envelope calculated but explicitly classified as uncalibrated
- master architecture, evidence hierarchy, work packages and acceptance criteria documented

**Next executable work**
- WP1: implement canonical ChemCore feature pipeline and tests

**Do not restart from earlier design discussions.** Future work must update this ledger and continue from the first incomplete work package unless a deliberate architecture change is documented here.

---

## 14. Change-control rule

Any modification that changes chamber geometry, sensor placement, fan, airflow, MQ module revision, RL, heater protocol, ADC configuration or analytical feature definition must increment the relevant hardware/sampling/model profile and must not silently reuse old calibration validity.

This document is the canonical development continuation point for ChemCore/AMCI.
