# HazRob 0.10.2 / HR069 — ChemCore Experimental Validation Requirements

Date: 2026-10-01
Status: CONTROLLED RESEARCH PROTOCOL REQUIREMENTS
Applies to: HR069 analytical array (MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135)
Purpose: define the evidence required before currently gated ChemCore features may be promoted.

## 1. Governing principles

1. Software fit quality is not a substitute for metrological validation.
2. A literature/manufacturer curve, PubChem/QSPR prediction, model-derived fingerprint or transfer dataset never self-promotes to HR069 M4.
3. M4 requires device-specific HR069 measurements at known reference concentration with traceable preparation/source and preserved environmental/acquisition provenance.
4. Training, tuning and final validation observations must be separated by independent exposure/session units; time points from one exposure are not independent replicates.
5. Measurements from the same prepared atmosphere/session must not be counted as multiple independent experiments merely because they contain many timestamps or multiple MQ channels.
6. Blanks, recovery, sensor health, heater state/protocol, T/RH/P and sampling profile must be retained with every controlled exposure.
7. No missing MQ channel may be synthesized for empirical validation.
8. Quantitative validation should use certified/reference gas generation or another independently traceable concentration method. Uncontrolled liquid evaporation/headspace estimates are unsuitable as M4 concentration standards.
9. Tests involving flammable/toxic gases require an appropriate controlled laboratory/chamber, ventilation, ignition control, exposure controls and trained supervision.

## 2. Independent replicate definition

An independent replicate is a separately executed exposure cycle with its own baseline, exposure and recovery sequence. Multiple ADC readings within one exposure characterize within-session noise/kinetics but do not increase the independent replicate count.

For covariance, uncertainty and threshold estimation, replicate counts are reported at both levels:
- `n_sessions`: independent exposure sessions;
- `n_timepoints`: within-session observations.

Only `n_sessions` is used as the primary independent sample count for inferential validation.

## 3. M4 concentration calibration gate

For a sensor/analyte relation to become HR069 M4 quantitative evidence:

- use at least 5 non-zero concentration levels spanning the intended reportable domain, preferably approximately log-spaced;
- include a zero/clean-air blank in each experimental block;
- minimum 5 independent exposure sessions per non-zero concentration is the initial acceptance floor; 8–10 is preferred for estimating uncertainty and drift;
- distribute sessions across at least 3 experimental days/blocks where practical so same-day repeatability is not mistaken for longer-term reproducibility;
- randomize or counterbalance concentration order where technically safe and feasible;
- document gas/reference source, preparation method, chamber effective volume/flow, exposure time, recovery time, T/RH/P, heater protocol, sensor/module identity, R0/calibration state and firmware/app/library versions;
- estimate residual error in `log10(Rs/R0)` directly from independent sessions rather than from curve R² alone;
- define a validated concentration domain and prohibit extrapolation beyond it;
- validate the final model on independent sessions not used to select coefficients/model form;
- report bias, RMSE/MAE in both log-response and concentration space where meaningful, and coverage/uncertainty of the reportable domain.

A curve that fits source/manufacturer points perfectly remains M2 until this HR069 gate is satisfied.

## 4. Environmental T/RH compensation gate

No generic T/RH correction may be enabled from datasheet intuition alone. To fit `EnvironmentalCompensationModel` for a specific MQ channel/analyte or justified sensor-level response family:

- use controlled combinations of temperature and relative humidity that cover the intended operating envelope;
- initial research design should include at least 3 temperature levels and 3 RH levels, including the normal reference region, producing a minimum 3×3 environmental grid where chamber capability permits;
- repeat environmental cells across more than one analyte concentration so environmental terms are not confounded with concentration;
- use independent sessions and retain a held-out subset or held-out experimental blocks for validation;
- fit compensation in log-response space and test whether T, RH and interaction/quadratic terms materially improve independent validation error;
- reject coefficients that improve training fit but worsen held-out performance;
- record the exact valid T/RH/concentration domain of the compensation model.

Until this evidence exists, ChemCore should only use environmental variables for declared-domain gating and QSPR covariates, not numeric correction of MQ ratios.

## 5. Empirical fingerprint covariance gate

The current source-backed numeric fingerprint families are deterministic/model-derived templates and carry no empirical covariance (`sampleCount=0`). They must remain on the diagonal standardized-distance fallback.

A covariance-aware fingerprint may be activated only when:

- the fingerprint is built from repeated independent HR069 exposure sessions under the same declared sampling profile;
- the analyte/reference concentration and environmental domain are documented;
- there are enough independent sessions to estimate correlation without a singular/unstable covariance matrix;
- for an 8-channel static fingerprint, at least 20 independent sessions is the initial floor and 30+ is preferred for a stable regularized covariance estimate;
- covariance estimation uses shrinkage/regularization when justified and records the method/parameter;
- the classifier is evaluated on independent sessions not used to estimate the fingerprint mean/covariance;
- performance is compared against the current diagonal baseline; covariance is adopted only if independent known/unknown discrimination improves without unacceptable false acceptance.

Model-derived concentration templates must never be treated as replicated samples for covariance estimation.

## 6. Temporal identity model gate

ChemCore extracts 17 features per sensor (136 dimensions for 8 MQ channels). High dimension creates a strong overfitting risk.

Before temporal features may influence named identity:

- collect labeled baseline/exposure/recovery time series from independent HR069 sessions;
- preserve the exact feature schema and sampling profile;
- use feature selection, dimensionality reduction or a regularized model rather than estimating an unrestricted 136×136 covariance from a small dataset;
- perform session-level splitting: all time points/features from one exposure remain in the same train/validation/test partition;
- maintain independent final test sessions that were not used for feature/model/hyperparameter selection;
- report per-class sensitivity/recall, specificity where defined, confusion matrix, macro metrics and unknown/open-set false acceptance;
- compare temporal+static performance with the static ChemCore baseline;
- static and temporal evidence derived from the same exposure/source must share an independence/provenance group where appropriate so confidence fusion cannot double count them.

## 7. Open-set / OOD threshold gate

Open-set thresholds must be calibrated on independent known-vs-unknown observations, not on training distances alone.

Required structure:
- training-known set: fits feature-domain parameters only;
- validation-known set: estimates known-class retention/false reject;
- validation-unknown set: chemicals/families excluded from training and used to estimate false acceptance;
- final test-known and test-unknown sets where sufficient data exist.

Threshold selection should report:
- known recall;
- unknown recall/rejection rate;
- false reject rate;
- false accept rate;
- balanced accuracy;
- feature coverage/missingness pattern.

For partial feature vectors, threshold validation should include the missingness patterns expected in actual operation. The current dimension-coverage scaling is an interoperability approximation until those pattern-specific validation data exist.

## 8. Mixture/interference gate

`POSSIBLE_MIXTURE_OR_INTERFERENCE` is currently a conservative rejection of an inadequate pure-compound explanation; it is not component decomposition.

Before changing the current residual/ranking thresholds:

- select chemically confusable pairs based on MQ cross-sensitivity and current candidate confusion;
- include each pure component at matched total-concentration levels;
- include binary mixtures across several composition ratios (for example strongly A-dominant, intermediate, approximately equal, intermediate B-dominant, strongly B-dominant) without assuming linear sensor additivity;
- test multiple total concentration levels inside the validated sensor domains;
- use independent replicate sessions and multiple experimental blocks/days;
- evaluate the distribution of standardized residual RMS and relative ranking-weight margins for pure gases versus mixtures/interferences;
- select thresholds using predefined performance targets on validation data, then verify on independent test sessions.

Do not claim mixture component identities until a separately validated mixture-decomposition model exists.

## 9. PubChem/QSPR promotion gate

The current QSPR is M1 because it is largely learned from source-backed M2 response relations rather than an independent HR069 experimental matrix.

Any future promotion requires:

- an HR069 dataset containing multiple chemically diverse compounds with known concentrations and complete molecular descriptor records;
- independent compounds held out from model fitting (leave-compound-out remains mandatory);
- preferably an external/final set of compounds never used for descriptor/model/hyperparameter selection;
- documented applicability domain in molecular-descriptor, concentration and environmental space;
- RMSE, MAE and bias in log-response for each enabled MQ model;
- comparison against simple baselines to demonstrate that molecular descriptors add predictive value;
- confirmation that predictions are not dominated by duplicated points generated from the same source curve;
- evidence-tier promotion decided separately from numerical performance and never inferred automatically by software.

QSPR-only output remains non-reportable concentration evidence unless a separate HR069 quantitative validation gate is passed.

## 10. Experimental data package required for the central library

Every controlled campaign uploaded to the central analytical library should contain:

- immutable campaign/session IDs;
- chemical identity, CAS where applicable, purity/reference source;
- target and independently established concentration;
- concentration uncertainty/method where available;
- blank/exposure/recovery timestamps;
- full per-channel MQ time series: raw ADC, voltage where retained, Rs, accepted R0 provenance, Rs/R0;
- environmental T/RH/P time series;
- heater state/protocol validity;
- sensor/module identities and hardware revision;
- chamber/sampling profile and flow/volume metadata;
- firmware/app/ChemCore/library versions and source commit;
- exclusions/QC flags without deleting original raw observations;
- derived feature/fingerprint artifacts stored separately from raw data with hashes and provenance links.

## 11. Unlock conditions by remediation phase

| Remediation phase | Current state | Evidence needed to unlock |
|---|---|---|
| Phase 2 analyte-specific sigma | proxy only | replicated HR069 residual variance by sensor/analyte/domain |
| Phase 3 numeric T/RH compensation | disabled | controlled multi-level T/RH × concentration HR069 experiments |
| Phase 4 covariance fingerprint | diagonal fallback | ≥20 independent static HR069 sessions per target/domain, preferably ≥30 |
| Phase 5 temporal identity | disabled for named identity | independently validated labeled HR069 time-series model |
| Phase 6 missingness-specific OOD thresholds | generic coverage scaling | known/unknown validation stratified by realistic missingness pattern |
| Phase 7 mixture threshold recalibration | frozen research thresholds | pure + controlled-mixture validation/test campaigns |
| Phase 8 QSPR evidence promotion | M1 only | chemically diverse independent HR069 known-concentration compound set |
| M4 reportable PPM | gated | device-specific known-reference concentration calibration and independent validation |

These are research acceptance gates, not automatic promotion rules. Final scientific acceptance must consider data quality, provenance, bias, uncertainty and independent validation together.
