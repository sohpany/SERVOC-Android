# HazRob 0.10.1 — Controlled Change Addendum HR-0101-044

**Change ID:** HR-0101-044  
**Title:** Analytical chemical laboratory completion before final calibration campaigns  
**Version:** HazRob 0.10.1 / API 4  
**Status:** SOURCE IMPLEMENTED — BUILD VERIFICATION PENDING — CALIBRATION/FIELD VALIDATION PENDING

## 1. Objective

Complete the HazRob analytical-chemistry software path before the next physical calibration sessions so that future sessions are used to populate and validate calibration evidence rather than to discover missing software architecture.

This change does **not** claim that the eight cross-sensitive MQ channels selectively identify every library compound, and it does not convert the accepted clean-air R0 baseline into reference-gas ppm calibration. HazRob remains an electronic-nose / ranked-evidence system with explicit UNKNOWN, ambiguity, interference and quantitative-withholding gates.

## 2. Controlled scope

### 2.1 Laboratory readiness and calibration state

Added `ChemCoreLabQuality` (`chemcore-lab-quality/1`) to separate:

- hardware/configuration readiness;
- current capture readiness;
- field clean-air R0 normalization readiness;
- environmental T/RH/P availability;
- device/reference-gas M4 calibration availability.

Configuration requires both ADS1115 devices, all eight R0 normalization values and a stationary robot. Capture additionally requires MQ power and at least six valid live MQ channels. Missing environmental data is recorded as an advisory that withholds environmental correction; it is not silently substituted with assumed T/RH/P.

### 2.2 ChemCore decision envelope v3

`ChemCoreResultEnvelope` is upgraded to `chemcore-result/3` while keeping backward-compatible constructors. Session-bound results now preserve:

- candidate rank;
- posterior;
- screening-fit concentration hypothesis;
- standardized residual RMS;
- contributor count;
- evidence tier;
- channel coverage;
- inter-sensor residual-agreement diagnostic;
- evidence-quality diagnostic;
- number of calibrated contributing curves;
- validated-domain state;
- per-sensor log10 residuals;
- runner-up posterior and separation margin;
- explicit PPM-reportability flag and withholding reason;
- laboratory quality / calibration gate snapshot.

A single contributing MQ channel has no defined **inter-sensor** agreement value; it can provide evidence, but it cannot display a synthetic 100% multisensor agreement.

### 2.3 Hardened inverse inference and M4 gate

The joint inverse solver continues to use logarithmic/power-law response models and screening-fit concentration hypotheses for ranking. HR-0101-044 tightens the operational quantitative rule:

- M4 is not granted merely because one contributing curve is device-calibrated.
- For a joint quantitative result, **all contributing quantitative response curves** must be device/reference-gas calibrated.
- The open-set identity gate must accept the observation.
- The fitted solution must remain inside every contributing validated concentration domain.
- If calibrated evidence exists but the solution is outside the validated domain, the outcome is `OUT_OF_VALIDATED_DOMAIN`, not an extrapolated ppm.
- M1–M3 fitted ppm values remain internal screening/fit hypotheses and are withheld from the reportable quantitative result.
- `InverseMolecularSolver.Result.hasDeviceValidatedQuantitativeSupport()` is aligned to the same rule so no secondary caller can interpret partial M4 evidence as complete quantitative support.

The candidate agreement metric is a residual-consistency diagnostic, not a calibrated probability of identity.

### 2.4 Analytical laboratory user interface

Added `AnalyticalLabTab` around the existing controlled ChemCore session state machine. The developer workspace now exposes **المختبر** with two internal surfaces:

1. the existing controlled CONDITION → BASELINE → EXPOSURE → FEATURE/DECISION → RECOVERY protocol;
2. a laboratory readiness/results dashboard.

The dashboard exposes ADS state, MQ channel count, R0 normalization count, M4/reference calibration count, blockers/advisories, detailed BME/BMP diagnostics, T/RH/P, environmental-correction availability, the last session-bound ChemCore decision, PPM gate and ranked candidate metrics.

The existing tested ChemCore state machine is retained rather than replaced.

### 2.5 Session-bound audit persistence

`ChemCoreDecisionRepository.attachLabQuality(...)` binds the laboratory readiness/calibration/environment snapshot to the latest analytical decision of the **same measurement session**. It changes only the JSON audit envelope and does not rewrite the scientific decision columns.

### 2.6 Calibration campaign quality control

Existing `CalibrationQualityPolicy.hazRobDefault()` remains a configurable **project research safeguard**, not a regulatory calibration standard. Its current default acceptance gate is:

- at least 3 distinct known concentrations;
- at least 3 stabilized replicates per concentration;
- maximum within-level CV of 15%;
- concentration span of at least 4×;
- diagnostic log-power R² of at least 0.90;
- baseline phase required;
- recovery phase required;
- heater-protocol metadata required;
- extrapolation disallowed.

`CalibrationQualityReport` now serializes the complete QC evidence as `hazrob-calibration-qc/1`.

### 2.7 Explicit M4 calibration publication gate

Added `CalibrationValidationMarker` (`hazrob-calibration-publication/1`) and `CalibrationCurvePublisher`.

A calibration curve may be retained inactive for scientific audit when QC fails. It may become an active M4-capable device/reference curve only when:

- the calibration campaign passes the configured QC policy;
- a reference-gas identity is recorded;
- activation is explicitly requested.

`SqliteCalibrationProvider` no longer treats a non-empty `reference_gas` field as sufficient proof of M4 validation. It requires the machine-readable accepted QC marker. Older/legacy active curves can still provide screening interpolation but return `referenceValidated=false`, preventing them from silently enabling quantitative occupational interpretation.

## 3. Relationship to HR-0101-043

The accepted field clean-air R0 values from `FIELD-1789255293867` remain device-specific normalization baselines and are not altered by HR-0101-044. They establish the installed array's Rs/R0 reference state. HR-0101-044 supplies the separate pathway by which future reference-gas campaigns can promote eligible sensor/analyte response curves to M4 after QC.

## 4. Automated tests added / extended

- `ChemCoreLabQualityTest`
  - clean-air R0 enables fingerprint normalization but not M4 reference calibration;
  - missing environment is an advisory rather than a synthetic environmental value;
  - missing ADS / robot motion closes the configuration gate.
- `ChemCoreResultEnvelopeV3Test`
  - one sensor cannot create inter-sensor agreement;
  - screening PPM is withheld;
  - residual evidence and separation margin remain auditable.
- `CalibrationValidationMarkerTest`
  - a complete 3-level × 3-replicate controlled campaign can generate an accepted QC marker;
  - legacy plain-text curve provenance cannot count as M4;
  - incomplete campaigns cannot generate accepted M4 provenance.
- `ChemCoreM4CompletenessGateTest`
  - one M4 curve plus one literature/manufacturer curve in the same joint fit remains `IDENTIFIED`/M3 with PPM withheld;
  - a fully M4-calibrated joint fit inside the accepted open-set/domain gates may become `QUANTIFIED`.

Existing cross-component integration, Android unit and release tests remain mandatory.

## 5. Required build verification

HR-0101-044 cannot be marked source/build PASS until the **same final source SHA** passes:

1. HazRob 0.10.1 cross-component integration contract;
2. Android unit tests, including the new laboratory/calibration tests;
3. Android release APK assembly and signer verification;
4. ESP32 master firmware compile;
5. ESP32-CAM firmware compile.

## 6. Remaining physical/calibration work after source/build PASS

The intended post-build sequence is:

1. Flash the final ESP32 master firmware and install the matching Android APK.
2. Verify BME/BMP detection and live T/RH/P plus I²C diagnostic status.
3. Repeat a very-clean-air session to field-validate the HR-0101-043 R0 baselines (`Rs/R0` expected to center approximately around 1.0 subject to drift/stability gates).
4. Run one or more controlled reference-gas campaigns using the QC policy above for sensor/analyte pairs selected for quantitative validation.
5. Upload the session results; rejected campaigns remain evidence but do not activate M4 curves.
6. Re-run identity/open-set/quantitative validation after sufficient reference datasets exist.

Only after those physical campaigns may the relevant calibration pairs and overall field validation be marked PASS.

## 7. Non-regression constraints

- No GPIO reassignment.
- No change to HazRob 0.10.1/API4 identity.
- No intentional change to drive, camera or remote-connectivity behavior.
- No assumed environmental values.
- No automatic extrapolation beyond validated curve domains.
- No automatic promotion of field-clean-air R0 to reference-gas calibration.
- No quantitative occupational ppm result without complete M4 + identity-domain acceptance.
