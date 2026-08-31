# HazRob 0.6 — Assistive algorithm interpretability

This document defines the semantics of the non-quantitative assistive algorithms included in HazRob 0.6. It exists to prevent UI labels or later model changes from overstating what an internal score means.

## 1. Visual flame evidence

### Role

The 0.6 visual pipeline is an **experimental, explainable screening model**. It is not a calibrated probability model and is not used for motor control, emergency-stop logic, chemical PPM validation or certification claims.

### Frame model

`FlameColorModel` evaluates downsampled camera pixels using combined RGB and YCbCr rules derived from the rule families described by Celik & Demirel (Fire Safety Journal, 2009, DOI `10.1016/j.firesaf.2008.05.005`) and Mahmoud & Ren (Mathematical Problems in Engineering, 2018, DOI `10.1155/2018/7612487`).

A pixel contributes to the candidate mask only when all three groups pass:

- RGB: `R > 110`, `R > G >= B`, `R-B >= 50`
- chroma: `Y > Cb`, `Cr > Cb`, `Cr-Cb >= 40`
- relative-to-frame: `Y >= mean(Y)`, `Cb <= mean(Cb)`, `Cr >= mean(Cr)`

The normalized candidate area and bright-candidate area are transformed by saturating exponential functions and combined as:

`frameEvidence = 0.72 * (1-exp(-candidateRatio/0.035)) + 0.28 * (1-exp(-brightRatio/0.020))`

clamped to `[0,1]`.

**This value is an evidence score, not a probability of fire.**

### Temporal layer

`FlameFrameAnalyzer` analyzes at most about one frame every 180 ms and skips frames if the analysis worker is busy so vision cannot deliberately queue behind robot-control/network work.

The current live tracker parameters are:

- enter evidence threshold: `0.62`
- exit threshold: `0.34`
- EMA alpha: `0.28`
- minimum positive frames: `4`
- minimum evidence duration: `750 ms`
- clear persistence window: `1200 ms`

States are `CLEAR`, `CANDIDATE`, `PERSISTENT`. One frame alone cannot create persistent evidence.

### Auditable outputs

For persistent events HazRob can retain:

- raw camera JPEG snapshot
- SHA-256 and byte count of the snapshot
- candidate bounding box
- frame evidence score
- candidate-pixel ratios
- temporal EMA/state
- UTC observation time
- measurement/media session link

The event should be described as **visual flame evidence** rather than a confirmed fire unless a future validated multimodal model establishes stronger semantics.

## 2. MQ sensor-health screening

### Role

`SensorHealthEngine` separates acquisition/electrical quality from chemical interpretation. A health warning does not identify a gas, and a chemically changing atmosphere must not be automatically mislabeled as sensor failure.

The current installed channels are MQ-2, MQ-3, MQ-4, MQ-7, MQ-8 and MQ-135. Health statistics use the latest 24 valid expected samples per channel.

### Current engineering screening thresholds

These are **HazRob 0.6 engineering thresholds**, not manufacturer acceptance limits or internationally standardized pass/fail values. They must be re-estimated from controlled datasets before any certification claim.

- `MISSING`: at least 3 consecutive expected acquisitions absent while that relay group is active.
- `INVALID`: at least 3 consecutive expected records have invalid ADC/Rs/ratio data.
- `SATURATED`: at least 2 recent hits with 12-bit raw ADC `<= 8` or `>= 4087`.
- `STUCK`: at least 16 valid samples with raw range `<= 1` ADC count.
- `UNSTABLE`: at least 10 valid samples with raw coefficient of variation `> 0.12`.
- `DRIFT_WARNING`: only after an explicit developer clean-air comparison, absolute `Rs/R0` baseline shift `>= 30%`.
- `HEALTHY`: enough valid acquisition data and no stronger screening state; this does **not** mean chemically calibrated.

### Drift policy

Baseline drift is never inferred from an arbitrary field exposure. A developer must explicitly designate a completed READY measurement as clean air to capture a baseline or to evaluate drift against a previously approved baseline.

The 30% threshold is a conservative project screening trigger only. Future controlled clean-air/repeatability datasets should estimate per-sensor distributions and replace global thresholds with sensor/module-specific limits where justified.

## 3. Gas-source direction aid

### Role

`GasDirectionEstimator` provides a **relative bearing hypothesis**, not absolute source localization. It never sends drive commands. It uses the currently selected chemical to choose target-relevant MQ channels and correlates recent change in their response with commanded robot motion.

### Target-specific response score

For each relevant MQ channel with baseline ratio `b` and current ratio `r`, the positive response feature is:

`response = ln(b/r)`

Positive responses contribute to a combined chemical-specific evidence score. The baseline adapts very slowly so a rapid plume increase is not immediately absorbed into baseline.

### Direction votes

When the robot has sufficient commanded translation and the target score changes enough, a recent motion direction receives a weighted vote:

- rising evidence votes toward the travel direction
- falling evidence votes in the opposite direction with reduced weight
- no translation produces no direction vote

Votes are retained for 30 seconds and exponentially age with a 16-second decay constant.

### Heading limitation

HazRob 0.6 has no wheel encoder/IMU odometry in this path. Heading is therefore dead-reckoned from differential drive commands using an engineering placeholder maximum turn rate of `1.35 rad/s`. This parameter is not a measured robot kinematic constant and is a principal source of bearing uncertainty.

### Interpretable output metrics

The estimator exposes:

- `voteCount`: number of recent usable directional votes
- `coherence`: magnitude of the resultant vote vector divided by total vote weight, `[0,1]`
- `evidenceStrength`: normalized accumulated vote weight, `[0,1]`
- `confidence = coherence * evidenceStrength`

`confidence` is an **internal consistency score**, not a probability that the gas source physically lies at that bearing. Direction is withheld when confidence is below `0.18`.

For future autonomous source localization, command-derived heading should be replaced by measured odometry/IMU, and the model should incorporate airflow/plume intermittency and obstacle-aware navigation.

## 4. Cross-algorithm evidence rule

No assistive score in this document may be silently converted into PPM, exposure risk, fire probability or autonomous motion authorization. HazRob keeps these evidence classes separate:

- chemical quantitative evidence — requires valid calibration/domain
- chemical identification/fingerprint evidence — candidate-ranking support
- sensor-health evidence — acquisition-quality support
- visual flame evidence — camera screening support
- gas-direction evidence — operator navigation aid

Future fusion models must retain each contributing source, model version and domain so that a combined result remains traceable.
