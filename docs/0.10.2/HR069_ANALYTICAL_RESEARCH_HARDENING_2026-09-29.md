# HazRob 0.10.2 / HR069 — Analytical Research Hardening

Date: 2026-09-29
Branch: `release/hazrob-0.10.2`
Status: source implementation / research hardening; physical field qualification still required

## 1. Manufacturer constraints promoted into software authority

### MQ-7B / MQ-9B cyclic heater protocol

Current Winsen documentation specifies a cyclic heater protocol for MQ-7B and MQ-9B:

- high-temperature heater: 5.0 V ±0.1 V for 60 s ±1 s;
- low-temperature heater: 1.5 V ±0.1 V for 90 s ±1 s;
- manufacturer standard-test environment is approximately 20 °C ±2 °C and 55% ±5% RH;
- published device documentation requires long preheating before standard characterization.

HR069 physically exposes one common 5 V switched MQ heater rail. It therefore cannot claim that MQ-7/MQ-9 are operating under the manufacturer cyclic protocol. `HazRobHardwareProfile` is now the software authority for this physical capability and `AnalyticsSnapshot` derives heater-validity from that profile. MQ-7/MQ-9 evidence is withheld from heater-qualified inference until a dedicated cyclic heater driver is physically installed and qualified.

This change deliberately does not alter the release hardware ID or pretend that a hardware modification has already occurred.

## 2. Full MQDataScience 76-model catalogue: use all, preserve semantics

The pinned upstream source is `abcdaaaaaaaaa/MQDataScience` v6.0.0 / commit `e88f876140587a708ee84a4d4d3acbd5720274ee`, with the exact `PredictData.py` Git blob verified before execution.

The complete 76-model catalogue is now treated in four scientific roles:

1. models 1–11: environmental/time nuisance-response and compensation research;
2. models 12–49: curve, robust and nonlinear regression research;
3. models 50–63: temporal dynamics, response/recovery and stabilization research;
4. models 64–76: machine-learning regression research.

`tools/hazrob_mq76_upstream.py` now:

- verifies the complete ordered 1..76 catalogue before execution;
- runs every model in the pinned upstream catalogue;
- records all successes/failures and cross-validation results;
- ranks models inside their scientific role rather than treating every method as the same kind of calibration equation;
- extracts finite successful prediction surfaces;
- computes p10/p50/p90 cross-model spread globally and per role as an **uncalibrated model-disagreement diagnostic**.

The disagreement spread is explicitly marked `reportable_prediction_interval=false` and `m4_evidence=false`. It cannot be used as a confidence interval until coverage is validated on independent HazRob data.

`tools/qualify_mq76_result.py` now verifies the role accounting, full 76-model status, selected-model role/priority, and the fail-closed uncertainty flags before an output can even become a `QUALIFICATION_CANDIDATE`. It still sets `activation_eligible=false` and `m4_granted=false`.

## 3. Longitudinal drift

Long-term MOX studies show that sensor drift must be evaluated longitudinally rather than assuming a calibration is time-invariant. HazRob now includes `ChemCoreAnchorDriftMonitor`.

The monitor compares independent clean-air/reference-anchor sessions in log10 response space and reports:

- median multiplicative shift per channel;
- median absolute deviation;
- `STABLE`, `WATCH`, `RECALIBRATION_REQUIRED`, or `INSUFFICIENT_EVIDENCE`.

No universal drift threshold is hard-coded from literature. The caller must supply the policy and its provenance. The monitor never rewrites R0 or an accepted calibration automatically. A recalibration verdict requires a new versioned calibration rather than alteration of historical evidence.

## 4. Transient response and preprocessing

Recent MOS electronic-nose review literature reinforces that array channels are cross-reactive and that baseline, transient and dynamic signal information should be retained before pattern recognition. HazRob's canonical `chemcore-features/1` already retains 17 time-domain features per sensor including baseline, steady response, signed/absolute change, peak, slopes, t10/t50/t90, AUC, noise and SNR.

Decision: preserve the current feature schema for 0.10.2 rather than silently changing a trained-feature contract. A future `chemcore-features/2` should be developed only with controlled exposure/recovery phase data and explicit schema/model migration.

## 5. Complex mixtures and interference

Peer-reviewed MOS-array work on complex VOC mixtures reports that mixture response may depart strongly from simple pure-gas/power-law behavior. Therefore HazRob must not infer a binary/ternary mixture by adding pure-gas curves.

The existing `MixtureResidualGuard` remains a conservative guard: it can say that a pure-compound hypothesis is inconsistent, but it does not claim to identify mixture components.

`tools/validate_mixture_campaign.py` was added to make mixture research experimentally defensible. A valid campaign requires:

- explicit hardware and sampling-profile identity;
- declared chemical components;
- independent blank runs;
- replicated pure-component controls;
- replicated binary or ternary mixture compositions;
- unique session IDs and run IDs;
- explicit concentration metadata;
- temperature/RH metadata when the strict environment option is used;
- recovery runs are tracked and warned when absent.

The validator always emits `runtime_activation_eligible=false` and `m4_granted=false`. Mixture models remain research-only until independently validated.

## 6. Inter-unit variability and sensor-instance identity

Published calibration-transfer work shows that nominally identical MOX arrays can differ between physical units. HazRob's M4 policy is already device/reference-calibration based, but HR069 still lacks a persistent physical `sensor_instance_uid` bound to every calibration/model artifact.

Decision: do not fabricate an identity from an MQ channel name. Adding real sensor-instance lifecycle tracking requires a developer workflow for installation/replacement and persistent provenance. Because the HR069 database/release schema is already frozen at v9, this is a controlled next-revision change unless it can be added without weakening backward compatibility.

## 7. Selective classification, open set and coverage

Current research on drifted gas arrays supports reporting coverage together with accepted-sample accuracy and deferring low-confidence samples. HazRob already has:

- an open-set gate;
- explicit `UNKNOWN` / `INSUFFICIENT_EVIDENCE` outcomes;
- M0–M4 evidence tiers;
- AMCI extension/repeat/recovery decisions;
- a quantitative gate requiring M4 and in-domain evidence.

Next step: calibrate acceptance/coverage thresholds on independent longitudinal HazRob sessions rather than selecting them from a single development dataset.

## 8. Predictive uncertainty

Recent uncertainty-aware sensor-array research supports ensemble disagreement and reliability-aware decisions. The MQ76 bridge now exposes model disagreement, but it is deliberately not called calibrated uncertainty.

To populate reportable `predictionLow/predictionHigh`, HazRob still needs independent calibration of empirical interval coverage across concentration, temperature/RH, time and sensor age. Until then, the interval remains unavailable rather than invented.

## 9. Scientific boundaries retained

None of the research hardening changes permit any of the following shortcuts:

- a high CV R² cannot grant analyte identity;
- a manufacturer graph cannot become device-specific quantitative calibration;
- a research curve cannot bypass M4;
- model disagreement cannot be relabeled a confidence interval;
- a pure-gas model cannot be assumed additive in mixtures;
- drift detection cannot silently overwrite historical calibration;
- MQ-7/MQ-9 cannot be heater-qualified without the physical cyclic driver;
- literature benchmark accuracy cannot be transferred to HazRob.

Occupational quantitative interpretation remains permitted only after HazRob-specific M4 validation, inside the validated concentration/environment/sampling domain and under the applicable heater protocol.

## 10. Reference basis

Manufacturer authority:
- Zhengzhou Winsen Electronics Technology Co., Ltd. MQ-7B product/manual: cyclic 5.0 V/1.5 V heater, 60 s/90 s timing, 10–500 ppm CO, standard test environment.
- Zhengzhou Winsen Electronics Technology Co., Ltd. MQ-9B product/manual: cyclic 5.0 V/1.5 V heater, 60 s/90 s timing, CO and CH4 ranges, standard test environment.

Research basis already tracked in the project reference supplement:
- Wörner, Eimler & Pein-Hackelbusch (2025), *Scientific Data*: one-year MOX drift dataset.
- Vajdi (2025), *Engineering Applications of Artificial Intelligence*: MOS e-nose preprocessing review.
- Singh et al. (2024), *Microchimica Acta*: complex VOC mixtures and cross-reactive MOX arrays.
- Sumanto, Triyana & Kusumaatmaja (2026 issue), *Instrumentation Science & Technology*: calibration transfer / inter-unit variability.
- Du et al. (2026), *Journal of Chemometrics*: coverage-aware selective classification under drift.
- Eo et al. (2026), *Advanced Science*: uncertainty-aware deep ensembles for chemical sensor arrays.

These studies justify methods and validation requirements; their performance numbers are not HazRob performance claims.
