# HazRob 0.6 scientific library and evidence policy

## Purpose

HazRob 0.6 treats the Android application as the analytical platform and the ESP32 as the real-time acquisition/control layer. The scientific library is therefore versioned independently from firmware and is designed to grow from the legacy 144-entry catalog to a database containing hundreds or thousands of chemical knowledge records without implying equivalent sensor-detection coverage.

The project separates two concepts:

1. **Chemical knowledge coverage** — identity, CAS/PubChem identifiers, physical properties, exposure limits, hazard metadata and references.
2. **Analytical coverage** — whether the installed sensing chain has valid evidence to identify or quantify that chemical.

A chemical may exist in the knowledge database while remaining analytically `UNSUPPORTED`.

## Evidence tiers

| Tier | Meaning | Examples |
|---|---|---|
| A | Primary manufacturer, regulator or standards-source evidence | Winsen manual, NIOSH/CDC, OSHA, EUR-Lex, NIST, PubChem identity records |
| B | Peer-reviewed experimental/review literature | MOS/e-nose studies, transient-response studies, drift-compensation literature |
| C | HazRob reference experiment that passed an explicit developer validation protocol | reference-gas exposure, validated calibration curve, repeated fingerprint dataset |
| D | Research/community model not independently validated for the physical HazRob chain | inherited/community MQ curves, exploratory models |

No Tier D result may become quantitative merely through algorithmic consensus.

## Analytical status vocabulary

- `QUANTITATIVE_VALIDATED` — device-specific calibration/reference evidence exists, required heater/environment domain is valid, and the concentration lies inside the calibration domain.
- `IDENTIFICATION_SCREENING` — signal pattern is useful for candidate identification/fingerprint comparison but quantitative PPM is withheld.
- `RESEARCH_ONLY` — evidence is useful for model development but has insufficient provenance/validation for field interpretation.
- `UNSUPPORTED` — no defensible sensing path exists for the installed hardware.
- `OUT_OF_DISTRIBUTION` — the acquired pattern does not match the trained/validated domain sufficiently; the system must not force a known label.

## Core sensor references

Primary source family: Zhengzhou Winsen Electronics Technology Co., Ltd., MQ-series manufacturer manuals and sensitivity graphs. Importers must record the **specific manual revision/URL** with every curve rather than citing only the vendor homepage.

Current physical HazRob array:

- MQ-2
- MQ-3
- MQ-4
- MQ-7
- MQ-8
- MQ-135

MQ-5 and MQ-9 remain library slots only until physically installed.

For every imported MQ curve/model store: sensor model, analyte, curve/equation, coefficients, valid PPM range, reference `R0` definition, load resistance assumptions, heater protocol, temperature/RH domain, oxygen assumptions if stated, source identifier and evidence tier.

## MOS/e-nose analytical literature

### Sensor arrays + machine learning

Han, J., Li, H., Cheng, J., Ma, X., & Fu, Y. (2025). *Advances in metal oxide semiconductor gas sensor arrays based on machine learning algorithms*. Journal of Materials Chemistry C, 13, 4285–4303. DOI: `10.1039/D4TC05220J`.

Use: sensor-array pattern recognition, classification/regression algorithm families, cross-sensitivity as multivariate information, environmental compensation concepts and mixture-recognition research direction.

### Static/dynamic preprocessing and transient features

*Metal oxide semiconductor based electronic nose data pre-processing, review*. Engineering Applications of Artificial Intelligence, 149 (2025), 110540. DOI: `10.1016/j.engappai.2025.110540`.

Use: preprocessing architecture, dynamic response features, feature extraction and dimensionality reduction. HazRob 0.6 introduces time-series storage and `SignalFeatures` specifically so future fingerprint models can consume baseline, peak, delta, rise slope, T50/T90 and integrated response rather than a single endpoint value.

### Sensor drift

*A Comprehensive Review of Algorithms for Drift Compensation in Metal Oxide Semiconductor Gas Sensor Arrays*. Chemosensors 14(6), 143 (2026).

Use: long-term drift monitoring, transfer/domain adaptation research and the requirement to distinguish calibration validity from historical model availability.

### Additional 2026 frontier review

Xia, Y., Zhang, H., Shi, X., & Xu, S. (2026). *Frontiers in gas sensing with semiconducting metal oxide array electronic noses*. Dalton Transactions, 55, 4037–4045. DOI: `10.1039/D5DT02516H`.

Use: current research landscape for SMOx e-noses, mixture discrimination and multimodal/ML direction. It is not used as a substitute for manufacturer calibration data.

## Chemical identity and physical-property sources

### PubChem

NIH/NLM/NCBI PubChem and PUG REST: `https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest`

Planned fields: PubChem CID, preferred name, IUPAC name where useful, molecular formula, molecular weight, InChIKey and synonyms. Bulk/seed imports should be performed during library preparation rather than making normal field operation depend on live PubChem requests.

### NIST Chemistry WebBook SRD 69

`https://webbook.nist.gov/chemistry/`

Use: CAS-indexed thermochemical/thermophysical and spectral reference data where available. The database contains data for thousands of compounds; presence in NIST does not imply HazRob sensor detectability.

## Occupational-exposure sources

### NIOSH Pocket Guide to Chemical Hazards

`https://www.cdc.gov/niosh/npg/`

NIOSH states that the NPG presents data for **677 chemicals or substance groupings** and includes RELs, OSHA PEL references and IDLH values. HazRob will use NIOSH data as a primary occupational-hygiene source with each limit stored as a separate authority/type/duration record rather than collapsing values into one number.

The NPG introduction defines NIOSH TWA, STEL and ceiling notation and documents the role of IDLH values. Import code must preserve source notation and `N.D.`/carcinogen/LEL-derived semantics instead of silently converting them to zero.

### OSHA

29 CFR 1910.1000, Air Contaminants: `https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1000`

Use: legally defined PEL/ceiling values and substance-specific standard pointers. Store authority=`OSHA`, jurisdiction=`US Federal`, limit type and source revision separately from NIOSH recommendations.

### European Union

Commission Directive (EU) 2019/1831 and predecessor/current occupational-exposure legislation via EUR-Lex. Example canonical document: `https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L1831`.

Use: 8-hour and short-term OEL values, CAS/EC identifiers and notations such as skin. EU records remain jurisdiction-tagged; they are not merged numerically with NIOSH/OSHA values.

## MQDataScience model registry

HazRob retains the pinned MQDataScience v6.0.0 catalog (76 registered algorithms/models, pinned commit `e88f876140587a708ee84a4d4d3acbd5720274ee`). Registration and runtime implementation are separate states.

Runtime states for 0.6:

- `REGISTERED`
- `ANDROID_NATIVE`
- `VALIDATED_FOR_DATASET`
- `RESEARCH_ONLY`

Only models that are implemented and validated against an appropriate HazRob/reference dataset may contribute to a validated quantitative result.

## Feature families to implement/validate

### Signal conditioning

- ADC/raw sanity checks
- voltage/divider normalization
- `Rs` and `Rs/R0`
- robust baseline estimation
- outlier suppression where justified
- noise metrics
- saturation detection
- missing-channel masks

### Static multivariate features

- per-sensor normalized response
- sensor-to-sensor response ratios
- log-response vectors
- environment-conditioned response
- covariance/correlation features

### Dynamic/transient features

- baseline
- peak/minimum
- delta response
- initial/rise slope
- T50/T90
- time-to-peak
- area under response curve
- recovery slope/time constant
- response/recovery asymmetry
- rolling noise and drift statistics

### Classical chemometrics / ML

Candidate algorithms for validated Android or offline training paths include PCA, LDA/QDA, PLS/PLS-DA, kNN, SVM/SVR, ridge/lasso/elastic-net, robust regression, random forest, extra trees, gradient boosting/XGBoost-class methods, Gaussian processes, MLP and calibrated ensemble/stacking models. Algorithm availability alone never establishes analytical validity.

### Time-series/deep models

Reserved for datasets large enough to justify them: 1D CNN, temporal convolution, recurrent/GRU/LSTM models, attention/transformer representations, autoencoder/contrastive representations for drift and out-of-distribution detection.

## Calibration and experiment requirements

Developer experiments should record at minimum:

- immutable chemical identity and CAS where known
- reference concentration and method/source
- exposure/baseline/recovery phase timestamps
- complete MQ time series
- `T/RH/P`
- heater state/protocol where controllable
- sensor/module identity and calibration version
- repetitions and blank/baseline runs
- computed features and model version
- validation metrics
- approver/time/reference-bank hash

A fitted model must not extrapolate beyond a validated calibration domain unless it is explicitly marked research-only.

## Environmental compensation

BME280/BMP280 measurements are independent direct I2C inputs. Temperature and relative humidity are candidate compensation variables, but compensation equations must be source- and sensor-specific. BMP280 provides no humidity channel; compensation status must therefore be marked partial when RH is required but unavailable.

## Navigation / source localization research scope

0.6 stores motion command breadcrumbs and synchronized sensor time series. Autonomous replay/backtracking is intentionally not enabled by this release because command history is not odometry. Future source-localization work will evaluate plume-tracking, gradient/chemotaxis, casting/surging, probabilistic/Bayesian source localization and infotaxis using measured trajectory/heading/flow information rather than assuming a smooth concentration gradient.

## Versioning rule

Every imported source, curve, limit, training dataset and model must have a stable identifier and provenance. Updating a source creates a new version or revision record; it must not silently mutate a historical experiment or report.
