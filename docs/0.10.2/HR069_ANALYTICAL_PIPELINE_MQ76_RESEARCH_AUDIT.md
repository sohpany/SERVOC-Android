# HazRob 0.10.2 / HR069 — Analytical Pipeline, MQ76 and Research Hardening Audit

Date: 2026-09-29
Branch: `release/hazrob-0.10.2`
Scope: analytical path from physical acquisition through candidate ranking/quantification, complete MQDataScience 76-model research integration, manufacturer operating constraints, and research-derived enhancement plan.

## 1. Executive finding

The current 0.10.2 / HR069 analytical architecture is scientifically conservative in the correct places: it separates raw MQ acquisition, evidence-weighted legacy screening, ChemCore multi-sensor inference, open-set rejection, mixture/interference guards and M4 quantitative reportability. The most important limitation is not lack of algorithms; it is evidence quality and physical validity of the sensor responses.

All 76 MQDataScience models are now integrated into the HazRob research layer through a pinned upstream bridge. Fourteen remain dependency-free Android-native calibration ports. This distinction is deliberate: the upstream catalogue was designed to model/predict sensor response using time plus optional temperature and relative humidity; it is not scientifically valid to reinterpret every time-series model as an Rs/R0-to-ppm calibration curve.

The highest-priority hardware-scientific gap is MQ-7/MQ-9 heater control. Winsen specifies a 60 s 5.0 V high-temperature phase and a 90 s 1.5 V low-temperature phase. HR069 presently powers all eight MQ heaters from one common 5 V relay, so MQ-7/MQ-9 cannot currently satisfy the manufacturer heater protocol. Existing Android analysis already marks those two heater-validity flags false; that protection must remain until dedicated heater control exists.

## 2. Current end-to-end analytical path

### 2.1 Physical acquisition

Installed array:

- ADS1115 0x48: MQ-2, MQ-4, MQ-5, MQ-8.
- ADS1115 0x49: MQ-3, MQ-7, MQ-9, MQ-135.
- BME280/BMP280 environmental channel is on a separate I2C controller/bus.
- One common MQ power relay drives the complete eight-sensor bank.

Firmware acquisition currently uses ADS1115 single-ended readings, four conversions per MQ sample, nominal 860 SPS configuration and a +/-6.144 V full-scale assumption. The averaged voltage is converted to sensor resistance using:

`Rs = RL * (Vc - Vout) / Vout`

and then normalized as:

`ratio = Rs / R0`

The installed R0 values are field clean-air baselines derived from three sessions (216 samples total); they are explicitly not reference-gas M4 calibration.

### 2.2 Telemetry normalization

Android prefers the completed-session averaged Rs/R0 values when a measurement session is in `ready` state; otherwise it uses currently valid live ratios. The canonical MQ order is:

`MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135`.

### 2.3 Evidence-weighted screening engine

`ValidatedGasEngine` combines:

1. accepted device/reference calibration curves when available;
2. manufacturer-target curves;
3. manufacturer graph cross-responses;
4. registered research/digitized curves at deliberately low weight.

It operates in log concentration space and produces a weighted median concentration hypothesis plus agreement, coverage, evidence quality, score and contributor provenance. Research curves remain fingerprint evidence and are not allowed to silently become selective safety-grade ppm measurements.

### 2.4 Canonical ChemCore time-domain fingerprint

`ChemCoreFeaturePipeline` consumes sample-by-sensor Rs/R0 traces and transforms valid samples to `log10(Rs/R0)`. It currently emits 17 features per sensor:

- valid fraction;
- baseline and steady response;
- signed and absolute delta;
- min, max and peak;
- initial and stability slopes;
- t10, t50 and t90;
- signed and absolute AUC;
- baseline noise;
- SNR.

The schema is frozen as `chemcore-features/1`. Any environmental or dynamic feature expansion must create a versioned successor rather than silently changing feature order or meaning.

### 2.5 Joint inverse-array inference

`InverseMolecularSolver` searches for one concentration that jointly explains all available sensor responses for each chemical hypothesis. It does not simply invert eight curves independently. It uses evidence-tier reliability, response uncertainty, environmental/domain validity, residuals and contributor count, then normalizes candidate scores into comparative posteriors.

### 2.6 Decision, open-set and mixture gates

`ChemCoreInferenceEngine` combines the inverse solution with open-set rejection and mixture/interference consistency. M1-M3 fitted concentrations may help rank hypotheses but are not exposed as an operational measurement. A reportable concentration requires `QUANTIFIED` and complete M4 device/reference validation of all quantitative contributors inside their validated domains.

This distinction is retained and must not be weakened by the new model catalogue.

## 3. Full MQDataScience 76-model integration

### 3.1 Upstream authority

Pinned upstream source:

- Repository: `abcdaaaaaaaaa/MQDataScience`
- Commit: `e88f876140587a708ee84a4d4d3acbd5720274ee`
- Source: `DataScience/Requirements/PredictData.py`
- Git blob SHA-1: `bacf95948714dbe76f09c57e3db14cbc60616580`

The upstream runner defines 76 models and selects the best research model using cross-validated R2, with TimeSeriesSplit for temporal models and KFold for other models. Its input abstraction is an independent/time axis plus response and optional temperature/RH.

### 3.2 HazRob role separation

The complete catalogue is now classified into four scientific roles:

- Models 1-11: environmental/time nuisance-response models.
- Models 12-49: classical, robust, nonlinear, piecewise, local and spline curve research.
- Models 50-63: temporal dynamics, smoothing, autoregressive/state-space/Kalman research.
- Models 64-76: machine-learning regression research.

Every model is now integrated in the pinned developer/lab layer. The existing 14 Android-native ports remain available for local dependency-free calibration research; 12 are audited upstream-equivalent for the current one-dimensional adaptation and Huber/Theil-Sen remain explicitly marked approximations.

### 3.3 New pinned research bridge

`tools/hazrob_mq76_upstream.py` now provides a controlled way to execute the exact 76-model upstream catalogue from HazRob development data. It:

- verifies the pinned upstream Git blob before import;
- accepts time/independent variable, response and optional temperature/RH;
- executes all 76 upstream models;
- preserves success/failure status for every model;
- exports selected model, CV R2, prediction surface and source provenance;
- labels the result as research evidence only.

Dependency versions are pinned in `tools/mq76-requirements.txt` to match the upstream research environment.

### 3.4 Scientific restriction

Do not automatically deploy the model with the highest upstream CV R2 as a quantitative field model. HazRob qualification must additionally require independent/session-level validation, prediction-error metrics in the concentration domain, physical plausibility, concentration-domain validity, uncertainty, environmental validity and M4 reference-gas evidence.

Time-series models are valuable for drift, stabilization, forecasting and response-shape characterization; they must not be misrepresented as concentration calibration equations.

## 4. Manufacturer-derived operating constraints

Official Winsen manuals/pages for the installed MQ family consistently specify a standard reference test environment near 20 +/- 2 C and 55 +/- 5 %RH, initial preheat of at least/over 48 hours and oxygen near 21%, not below 18%, because oxygen concentration affects initial value, sensitivity and repeatability. These values are now represented by `MqManufacturerOperatingContract` as operating/test metadata, not calibration coefficients.

Manufacturer target envelopes retained by HazRob include:

- MQ-2: flammable gas/smoke, typically 300-10000 ppm for flammable-gas target range.
- MQ-3B: ethanol/alcohol 25-500 ppm.
- MQ-4: methane 300-10000 ppm.
- MQ-5: LPG/propane/methane 300-10000 ppm.
- MQ-7B: CO 10-500 ppm with mandatory 5.0 V/1.5 V heater phases.
- MQ-8: hydrogen 100-1000 ppm.
- MQ-9B: CO 10-500 ppm and methane 300-10000 ppm with mandatory 5.0 V/1.5 V heater phases.
- MQ135: ammonia/toluene/hydrogen/smoke reference detection range 10-1000 ppm; sulfide and benzene-series responses require compound-specific validation before selective quantification.

Winsen also states that bare sensors are tested but not necessarily calibrated at shipment and recommends aging/calibration after transport/storage once the sensor becomes stable. HazRob must therefore treat R0 and reference-gas calibration as device-specific, time-stamped evidence.

## 5. Research findings that materially improve HazRob

### P0 — physical validity before algorithm expansion

**Dedicated MQ-7/MQ-9 heater control.** The present common 5 V heater rail cannot implement their manufacturer 60 s high / 90 s low cycle. Until hardware changes, preserve heater-invalid status and do not use those channels for manufacturer-conformant quantitative CO/CH4 inference.

**Preheat versus session warm-up.** Manufacturer initial aging/preheat (>48 h) and the current 60 s operational session warm-up are different concepts. Track cumulative conditioning/last power-off separately from per-session stabilization.

**Oxygen-domain validity.** Manufacturer characterization assumes approximately normal oxygen and warns below 18% O2. HazRob has no oxygen channel in HR069. Therefore manufacturer-equivalent quantitative claims must be withheld in potentially oxygen-deficient atmospheres unless an O2 measurement or controlled-air assumption is available.

### P1 — analytical robustness

**Environmental compensation.** Temperature and RH materially affect MOX responses. HazRob already has a calibration-derived environmental compensation scaffold and a BME channel. Develop per-sensor/per-analyte compensation only from controlled calibration data; do not insert generic coefficients. Preserve pressure as provenance/context until an experimentally supported pressure model exists.

**Drift surveillance.** A 2025 one-year MOX dataset demonstrates persistent long-term drift. Add longitudinal clean-air/reference anchors, baseline-control charts and time-split validation. Never silently overwrite historical R0 or M4 curves; create a new calibration/model revision.

**Open-set + drift together.** Recent work treats unknown-gas rejection and drift adaptation jointly. HazRob already has open-set gates; future drift adaptation should therefore be evaluated against both known-class accuracy and false acceptance of unknowns.

**Uncertainty-aware output.** Deep-ensemble work shows the utility of predictive uncertainty for simultaneous classification and concentration regression. HazRob should add calibrated uncertainty intervals / ensemble disagreement to candidate and concentration results. High uncertainty should yield `RETEST`, `EXTEND_SAMPLE`, `UNKNOWN` or `INSUFFICIENT_EVIDENCE`, not a forced identity.

**Coverage-aware reporting.** Recent selective-classification work shows that accuracy is misleading without coverage. HazRob reports should pair accepted-result performance with rejection/coverage rate and anchor validity.

**Mixture-specific experiments.** Published MOX-array experiments show strong cross-interference and departure from pure-gas power-law behavior in mixtures. Keep the current mixture residual guard, and build dedicated binary/ternary mixture experiments rather than assuming additive pure-gas responses.

**Inter-unit/sensor-replacement transfer.** Identical MOX sensor units vary. Calibration-transfer research supports explicit cross-unit modelling. HazRob should bind M4 calibrations to physical sensor/module identity and invalidate or transfer-qualify them when a sensor or module is replaced.

### P2 — feature/model expansion after sufficient data

**ChemCore feature schema v2.** Do not modify v1 in place. Candidate v2 research fields include synchronized temperature, RH, environmental deltas, heater phase, sampling-profile/flow state, response/recovery kinetics and drift-anchor deviation. Pressure can be recorded as a contextual feature but should not receive a causal correction coefficient without controlled evidence.

**Dynamic/transient features.** Literature supports transient slopes, derivative peaks, response/recovery timing and flow-modulated signatures as useful discriminators. HazRob v1 already contains several kinetic features; v2 can add recovery features and controlled flow-state features when the physical sampling system is repeatable.

**Model ensembles and ML.** KNN/RF/boosting/deep models can improve mixture classification in specific datasets, but published accuracy cannot be transferred to HazRob. These models should enter HazRob only after controlled HazRob training and external/session holdout validation.

**Molecular/QSPR priors.** Descriptor-based gas-sensing research supports the existing QSPR/molecular-prior layer as M1 research evidence. It may improve candidate generation but must remain below empirical manufacturer/literature evidence and M4 device validation.

## 6. Validation policy for the 76 models

For each sensor/analyte/research task, retain all model results rather than only the winner. Qualification should record at minimum model version/source, training sessions, held-out sessions, concentration levels, temperature/RH domain, sampling profile, sample count, CV strategy, R2, log-RMSE/MAE, residual structure, extrapolation behavior, prediction interval/uncertainty, drift split performance and failure status.

Selection hierarchy:

1. reject models that violate physical/domain constraints;
2. reject leakage-prone or invalid CV results;
3. compare external/session holdout error, not training fit;
4. prefer stable lower-complexity models when performance is statistically indistinguishable;
5. require explicit model-card provenance and artifact SHA-256;
6. activate only after scientific review;
7. retain M4/open-set/reportability gates independently of model fit quality.

## 7. Implemented changes in HR069 research hardening

- Added complete 76-model scientific role registry.
- Preserved the distinction between Android-native execution and full research-layer integration.
- Added a pinned upstream 76-model execution bridge and matching dependency lock file.
- Added unit tests that require all 76 models to have a defined scientific role while preserving the audited 14 native Android ports.
- Added manufacturer operating/test-condition contract for all eight installed MQ sensors.
- Centralized MQ-7/MQ-9 heater-cycle validity through that manufacturer contract.
- Added tests for eight-sensor manufacturer metadata and the MQ-7/MQ-9 two-level heater cycle.

## 8. Required next engineering work

The next controlled implementation pass should focus on, in order:

1. dedicated MQ-7 and MQ-9 heater-driver architecture or formal exclusion of their quantitative target roles;
2. sensor-instance identity and calibration binding;
3. long-term drift/anchor monitor;
4. uncertainty/coverage contract and report fields;
5. ChemCore feature schema v2 research branch;
6. mixture experiment schema and controlled binary-mixture programme;
7. evaluation harness that imports 76-model lab outputs into versioned ChemCore model cards only after qualification.

Do not modify field quantitative gates simply because additional algorithms are available.

## 9. Primary external evidence reviewed

Manufacturer authority:

- Winsen MQ-2 product/manual: https://www.winsen-sensor.com/product/mq-2.html
- Winsen MQ-3B product/manual: https://www.winsen-sensor.com/product/mq-3b.html
- Winsen MQ-4 product/manual: https://www.winsen-sensor.com/product/mq-4.html
- Winsen MQ-5 product/manual: https://www.winsen-sensor.com/product/mq-5.html
- Winsen MQ-7B product/manual: https://www.winsen-sensor.com/product/mq-7b.html
- Winsen MQ-8 product/manual: https://www.winsen-sensor.com/product/mq-8.html
- Winsen MQ-9B product/manual: https://www.winsen-sensor.com/product/mq-9b.html
- Winsen MQ135 product/manual: https://www.winsen-sensor.com/product/mq135.html

Research evidence:

- Singh et al. (2024), Microchimica Acta 191:196, complex VOC mixtures and ML. DOI: 10.1007/s00604-024-06258-8
- Guo et al. (2024), Chemical Engineering Journal 492:152280, gas-sensing descriptors and ML. DOI: 10.1016/j.cej.2024.152280
- Metal oxide semiconductor based electronic nose data pre-processing review (2025), Engineering Applications of Artificial Intelligence 149:110540. DOI: 10.1016/j.engappai.2025.110540
- Worner et al. (2025), Scientific Data 12:1628, one-year MOX drift dataset. DOI: 10.1038/s41597-025-05993-8
- Sumanto et al. (2025/2026 issue), calibration transfer for inter-unit variability. DOI: 10.1080/10739149.2025.2504478
- Liu et al. (2026), Knowledge-Based Systems 331:114767, open-set recognition plus drift compensation. DOI: 10.1016/j.knosys.2025.114767
- Eo et al. (2026), Advanced Science, uncertainty-aware deep ensembles. DOI: 10.1002/advs.76134
- Du et al. (2026), Journal of Chemometrics, coverage-aware calibration/selective classification. DOI: 10.1002/cem.70148
- Review of drift compensation in MOS arrays (2026), Chemosensors 14(6):143.
- Gas discrimination of single and binary mixtures (2026), Green Energy & Environment 11(4):1037-1048. DOI: 10.1016/j.gee.2025.12.006

## 10. Non-claims

This audit does not claim that published model accuracies apply to HazRob. It does not claim that all 76 models are Android-native. It does not claim M4 quantitative calibration from field R0. It does not claim that manufacturer curves establish selectivity for graph-only cross-responses. It does not authorize oxygen-deficient quantitative use without validation. It does not authorize MQ-7/MQ-9 quantitative use under the current single fixed-voltage heater rail.
