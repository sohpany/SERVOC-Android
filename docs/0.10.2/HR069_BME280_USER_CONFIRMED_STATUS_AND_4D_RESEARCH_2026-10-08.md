# HazRob HR069 — User-confirmed BME280 operational update and four-dimensional MQ analytics review
**Date:** 2026-10-08  
**Release context:** 0.10.2 / HR069 / RC2  
**Document type:** Operator observation + architecture research review  
**Change record:** CR-20261008-012  
**Scientific status:** DOCUMENTED_OBSERVATION_AND_RESEARCH / NOT_M3_OR_M4_VALIDATION

## A. BME280 status — new superseding observation

- **2026-10-08 owner/operator confirmation:** "حساس BME280 يعمل بشكل ممتاز جدا" (“The BME280 sensor is working very well”).
- **Status:** `USER_CONFIRMED_OPERATIONAL`.
- **Historical correction:** Earlier investigation notes that suspected a non-working BME280 are superseded as current-operational-status statements. Preserve their historical provenance; do not silently erase previous troubleshooting.
- **Measurements available in principle:** temperature (°C), relative humidity (%RH) and pressure (hPa), when BME280 readings are valid and correctly timestamped.
- **Evidence source:** user first-hand report in HazRob research conversation dated 2026-10-08. No recorded BME280 values, raw instrument logs, model number readback, accuracy/reference comparison or digital-firmware diagnostic were supplied as part of this documentation step.
- **Limit of claim:** working normally != experimentally proven accuracy/traceability or validated environmental compensation for individual MQ channels.
- **Device/runtime change:** none. Do not modify firmware hardware-status flags or calibration models merely because the user updated operational status.

## B. Scientific clarification: what 4D currently means

The pinned upstream MQDataScience v6.0.0 implementation (`abcdaaaaaaaaa/MQDataScience` commit `e88f876140587a708ee84a4d4d3acbd5720274ee`, `DataScience/Requirements/PredictData.py`) defines:
- `RegressionData(t,y,t_surface,temp=None,rh=None)`;
- predictor tuple `(t, temperature, RH)` using feature key `all`;
- dependent response `y`.
Hence the **four observed variables** are `(time, T, RH, response)`, often called a *time-dependent 4D response surface*. It is **not** by itself a gas-composition/ppm calibration manifold.

A different, desirable *chemical calibration* four-coordinate dataset is `(gas concentration C, temperature T, relative humidity RH, log10(Rs/R0))`, where the fitted response surface is:

`y_sensor,analyte(C,T,RH) = E[log10(Rs/R0) | C,T,RH, sensor, analyte, heater, R0 convention, sampling profile]`

Adding explicit time-dependent transient behavior creates the measured tuple `(t,C,T,RH,y)` (five variables) while analyte class, channel, heater phase, flow etc. are additional model conditions. High-dimensional feature models are valid, but dimensional labels should define variables rather than imply chemical identification.

## C. What the actual repository contains as of this review

### MQDataScience library
- `library/research/hr069/mq_models_76.json` in `HazRob/build-harness`: 76 registered research regression models, indexed 0..75; first 11 relate to environmental/time response.
- `app/src/main/java/com/servoc/app/models/MqDataScience76Catalog.java`: exact ordered registered names; model 0 `Multi-Variable Regression (Time + Temperature + Relative Humidity)`; models 4–6 include quadratic/interaction surfaces.
- `app/src/main/java/com/servoc/app/models/MqDataScienceModelProfile.java`, `Mq76ModelRoleRegistry.java`, `ModelRuntimeRegistry.java`: role/availability metadata.
- `tools/hazrob_mq76_upstream.py`: pinned developer/lab execution bridge; input CSV columns `time_s,response`, optional `temperature_c,rh_pct`; checks upstream 76 models.
- Android runtime: 14 1D native calibration-family ports (12 upstream-verified for adapted family and 2 approximations), other 62 remain laboratory-bridge available and not Android native.

### Actual source-derived curves and environmental responses
- `library/research/hr069/mq_research_curves_43.json` in organization repository: 43 **source/model-derived 1D concentration-response relations** across installed models; classification `RESEARCH_EXTERNAL`, not M4. These are not 43 multidimensional calibration experiments.
- `app/src/main/java/com/servoc/app/chemical/EnvironmentalResponseModel.java`: environmental response factors fitted/digitized from MQDataScience at humidity anchors (normally 33% and 85%; MQ-9 has 30%, 60% and 85%), with temperature dependence `a(RH)+c(RH)*exp(b(RH)*T)` and interpolation across humidity anchors. These are research curves, not separately measured HR069 reference-gas grids.
- `ResearchEnvironmentalCorrection.java`: research-only correction factor, explicit out-of-domain rejection at T outside -10..50 °C and RH outside documented anchors. The source assigns identical coefficients to MQ-2 and MQ-135; preserve provenance and investigate before device-specific assumption.
- `EnvironmentalCompensationModel.java`: provides a parametric, per-sensor calibration-derived form in log-ratio space but **defaults to identity/no compensation** until valid fitted coefficients are provided.
- `ChemCoreEnvironmentDomainPolicy.java`: environment admissibility gate; **does not numerically correct** MQ response in production without calibrated model coefficients.

### Existing dynamic model
- `ChemCoreFeaturePipeline.java`: version `chemcore-features/1`, 17 features × up to 8 MQ sensors, defined without silently adding external environmental inputs.
- `ChemCoreSessionDynamics.java`: supplementary recovery, correlation and environment summaries, not automatic validated identity contributions.

## D. Sensor+environment integration concept — proposed, not active
1. Use BME280 valid timestamped T/RH to establish whether candidate curves are evaluated inside their declared environmental validity domain; record pressure as context.
2. Link MQ sample timestamps with nearest-in-time BME280 observations; mark stale/missing environmental records, instead of inserting 20 °C / 55% RH defaults.
3. Research an external evidence-only 4-variable response-surface model on **per-analyte known-ppm HR069 exposures**, using measured `(C,T,RH,y)` across controlled environmental grid and concentrations. Keep it `M1/RESEARCH` until independently qualified.
4. Fit and compare response surfaces:
   - source-factor × log-power-law baseline (domain-limited and source-specific);
   - generalized additive model `GAM` or tensor-product spline;
   - nonlinear interaction polynomial `T × RH`, `logC × T`, `logC × RH`;
   - Gaussian-process regression for lower-sample experiments with uncertainty, or gradient boosted regression for adequate data volume.
5. For time-dependent 4D (t,T,RH,response), evaluate stabilization, baseline drift and recovery separately; do not invert it to `ppm` unless explicitly trained on known concentration labels.
6. Include leakage-proof splits by independent exposure **session/day**, concentration levels, chemical UID, and environment blocks when appropriate. Treat samples within a single exposure as correlated, not independent.
7. Add a versioned `chemcore-features/2` experimental proposal if synchronised environmental dynamic features are introduced; never mutate `chemcore-features/1` meaning/order.

## E. Identified model metadata discrepancies requiring a separate controlled code review
- Index 4 (`Full Quadratic Surface Regression`): `Mq76ModelRoleRegistry.usesTime` returns true but `MqDataScienceModelProfile.needsOrderedTime` returns false.
- Index 5 (`Multi-Variable Quadratic Regression`): `MqDataScienceModelProfile.needsHumidity` returns true but `Mq76ModelRoleRegistry.usesHumidity` returns false.
- For indices 61/62, temperature and humidity requirements differ across registries as well. These could reflect distinct semantics but should be reconciled against the pinned upstream model constructors before automated scheduling/gating.
No production code is changed in this documentation record.

## F. Non-negotiable scientific limits
- A four-dimensional plotted/predicted response is not a compound fingerprint unless trained and validated with compound identity.
- No chemical separation or molecular spectrum can be recovered from broad MQ responses just by adding axes.
- BME280 operational status enables *measured environmental context*, not automatically correct MQ concentration.
- If temperature and RH are constant across an experiment, their coefficients cannot be identified reliably from that experiment; repeat with controlled environment variation.
- Correct transfer of the MQ7/MQ9 published response surfaces still depends on separately verified heater-state validity.
- `QUANTIFIED` / reportable ppm is reserved for HR069-specific M4 reference-gas calibration evidence.

## G. Source links
- MQDataScience pinned upstream: https://github.com/abcdaaaaaaaaa/MQDataScience/tree/e88f876140587a708ee84a4d4d3acbd5720274ee
- Catalog: https://github.com/HazRob/build-harness/blob/main/library/research/hr069/mq_models_76.json
- Source curves: https://github.com/HazRob/build-harness/blob/main/library/research/hr069/mq_research_curves_43.json
- HR069 MQ76 audit: `docs/0.10.2/HR069_ANALYTICAL_PIPELINE_MQ76_RESEARCH_AUDIT.md`
- HR069 environmental validation requirements: `docs/0.10.2/HR069_CHEMCORE_EXPERIMENTAL_VALIDATION_REQUIREMENTS_2026-10-01.md`
- General background: Han et al., *Journal of Materials Chemistry C* (2025), DOI: 10.1039/D4TC05220J.

**Review status:** Documentation added on existing RC2 branch. No physical BME280 validation tests or multivariate model benchmarking were performed.
