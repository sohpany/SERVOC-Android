# HazRob ChemCore / AMCI Master Development Plan

**Status:** ACTIVE — canonical continuation document  
**Branch:** `dev/hazrob-0.10-chemcore-foundation`  
**Baseline:** `dev/hazrob-0.9.3-rc2-l298n-drive`  
**Current sampling profile:** `hazrob-chemcore-chamber-v2`  
**Purpose:** transform HazRob from a mobile multi-gas screening robot into a scientifically controlled, uncertainty-aware chemometric analytical platform while retaining the present eight-MQ array and existing environmental/ADC hardware.

> Continuation rule: do not restart the design from earlier discussions. Continue from the first incomplete work package and update the progress ledger after material implementation milestones.

---

## 1. System definition

HazRob is decomposed into three coupled layers:

1. **HazRob Mobile Platform** — motion, video, remote operation, fail-safe, spatial reconnaissance and sampling-position control.
2. **ChemCore** — controlled acquisition, QC, canonical feature extraction, molecular/physical knowledge, evidence fusion, compound identification, analyte-specific quantification, uncertainty, provenance and reporting.
3. **AMCI (Adaptive Multisensor Chemical Interrogation)** — decision layer that decides whether evidence is sufficient or whether the system should extend exposure, wait for stability, observe recovery, repeat exposure, obtain a new baseline or request spatial confirmation.

The analytical objective is **not elemental decomposition and not GC-MS-equivalent identification**. HazRob is developed as a cross-reactive electronic-nose / chemometric analytical system whose eight MQ channels are interpreted jointly with time-domain response, environmental state, molecular/physical constraints, literature/manufacturer evidence and device-specific validation.

---

## 2. Retained sensing hardware

The first ChemCore line deliberately keeps the existing analytical hardware:

- MQ-2
- MQ-3
- MQ-4
- MQ-5
- MQ-7
- MQ-8
- MQ-9
- MQ-135
- two ADS1115 ADCs
- BME280/BMP280 environmental path
- ESP32 + Android architecture
- current experiment/calibration/provenance framework

No new chemical sensor is required for the first ChemCore research line.

---

## 3. Standard controlled sampling chamber v2

The adopted chamber is fixed as a versioned analytical covariate.

### 3.1 Geometry

- internal envelope: **90 × 40 × 150 mm**
- gross volume: **0.540 L**
- only eight MQ sensing caps project into the analytical volume
- estimated cap displacement: **23 cm³**
- effective estimated gas volume: **0.517 L**
- chamber cross-section: **0.0036 m²**
- centered front inlet: **40 × 20 mm = 0.0008 m²**
- rear extractor fan: **40 × 40 mm, 5 V, 0.28 A**
- centered rear aperture: approximately **28 × 28 mm**

### 3.2 Adopted engineering flow factor

Until physical airflow metrology is performed, Chamber v2 uses a fixed engineering estimate:

- `Q_est = 0.00110 m³/s = 1.10 L/s`
- engineering uncertainty: `±0.40 L/s`
- superficial chamber velocity: approximately **0.31 m/s**
- inlet velocity: approximately **1.38 m/s**
- effective nominal residence time: approximately **0.47 s**
- status: `FLOW_ESTIMATED`

This value may be treated as a fixed covariate only for the unchanged Chamber v2 geometry. It must never be described as measured/calibrated flow.

Full chamber specification is maintained in `docs/CHEMCORE_CHAMBER_STANDARD_V2.md`.

### 3.3 Change-control

Changing fan, fan voltage, inlet/outlet geometry, sensor-cap arrangement, chamber geometry or measured flow creates a new SamplingProfile revision and prevents silent reuse of flow-dependent calibration validity.

---

## 4. Scientific operating modes

### 4.1 SURVEY

Robot may move. Purpose: anomaly screening, plume localization, trends and warning. Results remain screening-level.

### 4.2 CHEMCORE_ANALYSIS

Robot must be stationary. Controlled sequence:

`CONDITION -> BASELINE -> EXPOSURE -> STABILITY -> FEATURE_EXTRACTION -> IDENTITY -> OPEN_SET -> QUANTIFICATION -> CONSISTENCY/MIXTURE_GUARD -> AMCI_DECISION -> REPORT -> RECOVERY`

### 4.3 AMCI_CONFIRMATION

When evidence is not decisive, AMCI may request:

- extended exposure;
- continued stability observation;
- recovery observation;
- repeated exposure;
- new baseline;
- spatial confirmation;
- `UNKNOWN` / insufficient evidence if ambiguity remains.

---

## 5. Evidence hierarchy and result vocabulary

Every analytical claim must retain evidence provenance:

- **M0 — Catalog only**
- **M1 — Molecular/QSPR inferred**
- **M2 — Manufacturer/literature supported**
- **M3 — Array-consistent identification**
- **M4 — HazRob device calibrated/validated**

Operational result vocabulary:

- `SCREENING`
- `IDENTIFIED`
- `QUANTIFIED`
- `UNKNOWN`
- `INSUFFICIENT_EVIDENCE`
- `OUT_OF_VALIDATED_DOMAIN`
- `POSSIBLE_MIXTURE_OR_INTERFERENCE`
- `INVALID_MEASUREMENT`

Molecular or literature evidence must never be mislabeled as device calibration. Occupational quantitative interpretation is enabled only when the measurement has the required calibrated evidence and valid domain.

---

## 6. Canonical analytical signal chain

`ADS raw -> voltage -> Rs -> Rs/R0 -> QC -> baseline-normalized time series -> canonical ChemCore feature vector -> family/compound hypotheses -> inverse molecular fit/evidence fusion -> OOD/UNKNOWN gate -> analyte-specific quantification -> mixture residual test -> uncertainty -> AMCI -> report`

### 6.1 Canonical feature contract

New ChemCore reference/training/runtime paths use one feature pipeline. The current feature contract preserves signed response and includes per-channel candidates such as:

- valid fraction;
- baseline and steady-state log ratio;
- signed and absolute response delta;
- min/max/peak;
- response and stability slopes;
- T10/T50/T90;
- signed/absolute AUC;
- baseline noise and SNR;
- recovery features when available.

Cross-sensor features are added only under controlled feature-selection/versioning rules.

### 6.2 Timing

The scientific data model retains session/scan/channel timing sufficient to reconstruct the sequential ADS1115 acquisition order and to resample onto a common grid when required.

---

## 7. Molecular Intelligence Layer

The >600-record occupational/chemical knowledge base becomes a provenance-rich molecular knowledge system. Presence in the knowledge base does **not** imply analytical detectability.

Target fields include:

- CAS, PubChem CID, formula, canonical/isomeric SMILES, InChI/InChIKey;
- chemical family and functional groups;
- molecular/exact mass;
- boiling/melting data where relevant;
- vapor pressure with conditions;
- ideal/experimental gas density and relative vapor density;
- diffusion/kinetic diameter/Henry data where authoritative;
- ionization energy, electron affinity, dipole, polarizability and other documented electronic/reactivity descriptors;
- LEL/UEL and other safety properties kept distinct from analytical evidence;
- source, method, units, T/P conditions, quality and uncertainty metadata.

Derived quantities such as gas density or ppm-to-mg/m³ are explicitly marked calculated and retain formula-version provenance.

### 7.1 QSPR-MOX

Research target:

`response = f(sensor_id, molecular_descriptors, concentration, T, RH, sampling_profile)`

Predicted curves are priors only. They remain M1 unless independent evidence promotes the claim.

### 7.2 Inverse Molecular Solver

For each candidate compound, solve for the single concentration that best jointly explains the complete available MQ array under evidence-weighted uncertainty. A candidate must explain the array consistently rather than winning by one favorable sensor.

### 7.3 Hierarchical reasoning

Preferred path:

`chemical anomaly -> chemical family -> candidate compounds -> joint-array fit -> open-set decision -> quantification`

The 76-model registry remains a research benchmark laboratory rather than a claim that all 76 algorithms are operational.

---

## 8. Open-set, uncertainty and mixture policy

The system must be able to refuse a named answer.

Open-set thresholds are calibrated from independent known/held-out unknown data rather than selected by visual convenience. Candidate posterior entropy, Mahalanobis/OOD distance and residual structure can contribute to the decision.

Before reporting a pure-compound concentration, compare the observed array response with the proposed pure-compound response. Structured residuals trigger `POSSIBLE_MIXTURE_OR_INTERFERENCE` instead of forcing a pure result.

---

## 9. Quantification architecture

1. retain no-extrapolation empirical device calibration;
2. compare single-sensor and multisensor concentration models using independent sessions;
3. weight contributors by calibration quality, evidence tier, domain, sensor health and drift;
4. report prediction intervals when validated;
5. derive LOD/LOQ from HazRob measurements, not marketing datasheets.

---

## 10. Database and storage separation

Device-local operational data and remotely distributable scientific reference knowledge must be separated.

### 10.1 Local operational database

`servoc.db` remains authoritative for telemetry, local experiments, session state, device calibration workflow and device-specific records. It must never be replaced by a remote knowledge snapshot.

### 10.2 Reference knowledge store

ChemCore Stable Library is a separately versioned, read-mostly reference store (`chemcore_knowledge` logical store) containing approved chemical/molecular/model/reference artifacts. It can be replaced atomically by a new verified snapshot without touching local telemetry.

---

## 11. Versioned scientific library synchronization

ChemCore knowledge will evolve much faster than the APK. Therefore the application and library have independent release cycles.

Canonical architecture is defined in `docs/CHEMCORE_LIBRARY_SYNC_ARCHITECTURE.md`.

### 11.1 Research and Stable lanes

**Research lane:** candidate experiments, proposed identity mappings, unapproved curves/properties and QA findings. Private/auditable.

**Stable lane:** only approved scientific records distributed to normal applications.

Promotion path:

`CANDIDATE -> REVIEWED -> QC_PASSED -> APPROVED_FOR_PUBLISHING -> IMMUTABLE_STABLE_SNAPSHOT`

Manual identity or experiment results must be reviewed before promotion. An application user cannot directly convert a free-text manual entry into stable analytical evidence.

### 11.2 Online assisted approval

Reference experiments and manual compound definitions are expected to be reviewed during an online project-development session before GitHub promotion. Review includes chemical identity, CAS/UID mapping, reference concentration/method, session provenance, sampling profile, sensor/calibration state, evidence tier and automated QA results.

### 11.3 Update model

The Android client exposes a library update function when Internet is available. It checks a small remote manifest and can install a newer compatible immutable snapshot without requiring a new APK.

Rules:

- no GitHub token/PAT inside the APK;
- separate public read-only data endpoint/repository or controlled HTTPS distribution service;
- stable snapshot requires manifest/payload integrity checks and production publication signature;
- download to staging;
- verify size, SHA-256, signature, schema, compatibility and referential integrity;
- never update during an active measurement/analysis;
- atomically activate only after all gates pass;
- keep previous verified snapshot for developer rollback;
- embedded APK library remains offline fallback;
- research data never silently overrides Stable data.

### 11.4 Reproducibility

Every analytical report must record:

- app version;
- firmware version;
- hardware profile;
- SamplingProfile;
- feature-schema version;
- ChemCore Library version;
- library source commit/hash;
- model version/hash.

---

## 12. Scientific quality rules

Mandatory controls:

- split training/validation/test by independent experiment/session/day;
- use leave-compound-out / held-out-family tests for QSPR generalization where applicable;
- maintain manufacturer/literature/QSPR/device-calibrated evidence as separate tiers;
- characterize ADC gain/offset with known voltages;
- measure actual RL per MQ before quantitative publication;
- validate ADS supply/reference domain;
- characterize fan-on/off, motors-on/off and chamber blank effects;
- track R0, baseline, noise, sensor age and calibration age;
- treat MQ7/MQ9 quantitative claims as invalid until heater protocol is appropriately implemented/validated.

---

## 13. Development work packages

### WP0 — Research branch and persistent roadmap
**Status: COMPLETE**

Dedicated branch and canonical documentation exist.

### WP1 — Canonical ChemCore feature contract
**Status: CORE IMPLEMENTED / INTEGRATION REMAINS**

Canonical feature extraction and deterministic tests exist. Remaining work is removal/bypass of legacy feature definitions from all new ChemCore reference/runtime paths.

### WP2 — Sampling Profile / Chamber v2
**Status: CORE COMPLETE; PHYSICAL FLOW VALIDATION DEFERRED**

Chamber v2 geometry, effective volume, estimated Q/velocity/residence time, flow-status semantics and tests are implemented. Exact cap coordinates and future physical flow measurement remain controlled follow-up items.

### WP3 — ChemCore session state machine
**Status: CORE IMPLEMENTED / LIVE INTEGRATION IN PROGRESS**

Condition/baseline/exposure/stability/recovery control and stationary-analysis interlock exist. Remaining work: make this the actual live measurement path in UI/telemetry rather than a parallel engine.

### WP4 — Open-set / uncertainty
**Status: CORE IMPLEMENTED / THRESHOLD VALIDATION PENDING**

Unknown/OOD infrastructure and calibration helpers exist. Remaining work: independent threshold datasets and operational integration.

### WP5 — Molecular Intelligence
**Status: IN PROGRESS**

Schema, repository, PubChem seed/import pipeline, descriptor contract and coverage tooling exist. Remaining work: complete/pin/QA the full molecular enrichment dataset and extend authoritative physical/electronic coverage.

### WP6 — Physics / molecular priors
**Status: FOUNDATION IMPLEMENTED**

Ideal-gas transforms, physical plausibility hooks and response-prior interfaces exist. Remaining work: populate authoritative property coverage and validate domain behavior.

### WP7 — QSPR-MOX
**Status: RESEARCH BASELINE IMPLEMENTED / SCIENTIFIC TRAINING PENDING**

Model contract and leave-compound-out validation foundation exist. Remaining work: assemble traceable manufacturer/literature/device datasets, train, compare and quantify generalization uncertainty.

### WP8 — Inverse Molecular Solver / evidence fusion
**Status: CORE IMPLEMENTED / COVERAGE EXPANSION PENDING**

Joint-array candidate fitting exists. Remaining work: expand candidate response models and integrate hierarchy/open-set/physical priors in the live path.

### WP9 — AMCI
**Status: CORE IMPLEMENTED / ACTION INTEGRATION PENDING**

Decision and information-gain foundations exist. Remaining work: wire actions to real measurement transitions and controlled robot confirmation behavior.

### WP10 — Mixture/interference guard
**Status: CORE IMPLEMENTED / THRESHOLD VALIDATION PENDING**

Residual guard exists. Remaining work: pure/interference/mixture validation and threshold/model refinement.

### WP11 — Scientific reporting/model cards
**Status: PARTIAL**

Result envelopes, model cards and artifact provenance exist. Remaining work: integrate ChemCore status/evidence/library/model provenance into UI, PDF and XLSX.

### WP11A — Versioned Knowledge Library Sync
**Status: IMPLEMENTATION STARTED**

Implemented foundations:

- independent semantic library versions;
- manifest v1 contract;
- stable/research channel semantics;
- fail-closed payload SHA-256 and RSA signature verifier contract;
- separate versioned reference snapshot store outside `servoc.db`;
- atomic version activation and previous-snapshot rollback foundation;
- contract tests for signature/tamper/version behavior.

Remaining:

- HTTPS manifest/download client;
- production public-key provisioning and publication-key CI process;
- stable reference-store schema/read adapter;
- embedded fallback snapshot selection;
- update UI and active-measurement interlock;
- changelog display;
- publication CI and immutable distribution endpoint;
- report integration of library version/hash.

### WP12 — Bench/device metrology
**Status: BLOCKED ON PHYSICAL MEASUREMENTS**

Pending: ADC reference-voltage validation, actual RL/R0, ADS VDD verification, fan/motor/blank interference tests, heater protocol validation and selected reference-gas work.

Estimated Chamber v2 airflow is no longer a blocker for software/model development but remains `FLOW_ESTIMATED` until measured.

### WP13 — Research Freeze v1.0
**Status: PLANNED**

Requires CI-green live ChemCore integration, pinned molecular/reference library, evaluated QSPR/solver/open-set behavior, physical metrology, selected M4 validation, blind independent validation, frozen commits/models/library/BOM/geometry and paper-ready limitations.

---

## 14. Progress ledger

### 2026-09-09 — Checkpoint C0: architecture foundation

- ChemCore research branch created from HazRob 0.9.3-rc2.
- ChemCore/AMCI scientific architecture and evidence hierarchy established.
- Initial chamber concept documented.

### 2026-09-09 — Checkpoint C1: ChemCore computational foundation

- canonical ChemCore feature pipeline implemented;
- explicit result/evidence vocabulary introduced;
- ChemCore schema/repositories added;
- molecular seed/import/descriptor foundations added;
- physical calculations and feasibility gates added;
- QSPR-MOX research baseline added;
- inverse molecular solver implemented;
- open-set, AMCI and mixture-guard foundations implemented;
- model cards/artifact repositories created;
- Chamber v2 adopted and implemented with fixed estimated flow covariate.

### 2026-09-09 — Checkpoint C2: dynamic knowledge-library architecture

Decision adopted: scientific knowledge will be updateable independently of the APK because hundreds of experiments and reference revisions are expected over time.

Completed at this checkpoint:

- `docs/CHEMCORE_LIBRARY_SYNC_ARCHITECTURE.md` added;
- research-vs-stable promotion workflow formalized;
- online assisted review/promotion rule documented;
- stable application updates explicitly separated from local `servoc.db`;
- `ChemCoreLibraryVersion` added;
- signed `ChemCoreLibraryManifest` v1 added;
- `ChemCoreLibraryVerifier` added with SHA-256 and stable-channel RSA signature enforcement;
- `ChemCoreLibraryStore` added with staged immutable package activation, rollback metadata and measurement interlock input;
- tests added for semantic versions, unsigned-research policy, stable signature requirement and payload tamper rejection.

**Next software execution focus:** complete WP11A remote updater/publication pipeline in parallel with WP3 live ChemCore integration and WP5 molecular dataset completion.

---

## 15. Global change-control rule

Any modification that changes chamber geometry, sensor placement, fan/flow, MQ module revision, RL, heater protocol, ADC configuration, ChemCore feature definition, evidence semantics, reference-store schema, library manifest/trust policy or model validity domain must increment the relevant profile/schema/model/library version and must not silently reuse prior calibration validity.

This document is the canonical development continuation point for ChemCore/AMCI.
