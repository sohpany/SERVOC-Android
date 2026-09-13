# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-046

**Change ID:** HR-0101-046  
**Title:** Unified expanded chemical analytical laboratory, analysis UI and session-bound reporting  
**Version:** HazRob 0.10.1 / API 4  
**Executable source baseline:** `6ab64a33a829fa7fd87166aeb0a2e1cbec4b1afc`  
**Status:** **SOURCE/BUILD PASS — ANALYTICAL SOFTWARE UNIFICATION PASS — PHYSICAL M4/ENV VALIDATION PENDING**

## 1. Objective

Unify the HazRob analytical chemistry workflow around one authoritative ChemCore v3 path, bind the accepted three-session HR-0101-045 clean-air R0 standard to live and persisted analysis without requiring immediate ESP32 reflashing, remove obsolete parallel analysis paths, expose the logarithmic/curve mathematics and residual evidence in the operator interface, and make analytical results directly auditable in session-bound reports.

HR-0101-046 does not claim universal chemical selectivity or metrological certification. Quantitative ppm remains withheld unless the complete M4 device/reference-gas, identity/open-set, consistency and validated-domain gates pass.

## 2. Work phases and implementation

### Phase A — R0 authority synchronization without physical teardown

Added `AcceptedR0Normalizer` (`hazrob-r0-normalizer/1`) as the application-side normalization authority. The preferred calculation is now:

`Rs/R0 = telemetry Rs / accepted HR-0101-045 R0`

The telemetry-provided `ratio` is used only as a compatibility fallback when physical `Rs` is unavailable. This permits the accepted HR-0101-045 baseline to be used immediately even while the installed ESP32 still contains older embedded R0 constants. Firmware synchronization can therefore be deferred until convenient physical access.

`ChemCoreSessionFeatureAdapter` records normalization provenance, accepted-R0 sample count and telemetry-ratio fallback count for the exact persisted session.

`SessionSeries` also rebinds the read-only analytical/report view of every recorded MQ sample to the accepted HR-0101-045 R0 whenever `Rs` is present. Raw database bytes remain unchanged. This prevents PDF/XLSX plots and tables from silently using a stale firmware ratio while the physical ESP32 reflash is deferred.

### Phase B — Single ChemCore analytical authority

The operator-facing `التحليل` surface is now ChemCore v3 session-bound only. The obsolete parallel `AnalysisActivity` and `MeasurementAnalysisTab` paths were removed from the controlled 0.10.1 branch so a single measurement cannot be interpreted by two competing analytical engines in the user interface.

The live MQ view still provides acquisition diagnostics, but chemical identification/quantification is not inferred from an uncontrolled instantaneous legacy path.

### Phase C — Expanded mathematical presentation

Added `ChemCoreAnalysisPresenter` (`chemcore-analysis-presentation/1`) to present the same persisted ChemCore decision consistently in the operator and developer laboratory surfaces.

The interface explicitly exposes:

- ranked candidates and posterior weights;
- runner-up posterior and separation margin;
- standardized residual RMS;
- contributing sensor count;
- coverage, evidence quality and inter-sensor residual agreement;
- M4-calibrated contributor count;
- validated-domain state;
- per-sensor `log10` residuals;
- screening-fit concentration hypothesis with an explicit non-reportable warning unless the final M4 gate passes.

The mathematical model is displayed as:

`log10(Rs/R0) = m × log10(C) + c`

`C = 10^((log10(Rs/R0) − c) / m)`

Joint inference selects a common concentration hypothesis by minimizing the weighted standardized residual set across available response curves. Posterior candidate weights are normalized from the candidate log scores. Residual agreement is retained as an evidence-consistency diagnostic rather than represented as a calibrated probability of identity.

### Phase D — M4 gate hardening and database-backed readiness

`ChemCoreLabQuality` now treats the accepted HR-0101-045 R0 campaign as the normalization readiness authority and separately counts M4-capable sensors from active calibration curves whose provenance contains an accepted machine-readable `CalibrationValidationMarker` plus a recorded reference gas.

A semantic weakness in `ChemCoreResultEnvelope.CandidateSummary.hasDeviceQuantitativeSupport()` was corrected. Candidate-level device quantitative support now requires:

`contributors > 0 AND calibratedContributors == contributors AND allContributorsInsideDomain == true`

Thus one calibrated curve inside a multi-curve candidate can no longer be displayed as complete device-validated quantitative support.

### Phase E — Session-bound analytical report lineage

The controlled ChemCore workflow now attaches the exact lab-quality/calibration/environment snapshot before saving the analytical decision and automatically creates a reportable `experiments` record bound to the same `session_id` with `reference_kind=chemcore_controlled_analysis` and `analysis_version=chemcore-v3-session-bound`.

The controlled result surface records the capture sample count/duration, feature SHA-256, R0 normalization provenance, candidate ranking, equations and residuals.

### Phase F — Expanded PDF/XLSX report semantics

The PDF analytical report now includes the HR-0101-045 R0 standard, PPM reportability and withholding reason, runner-up posterior and margin, lab gate snapshot, M4 contributor count, validated-domain state, mathematical equations and explicit `screen-fit` wording for non-reportable fitted concentration hypotheses.

The XLSX report remains session-bound and carries time-series, MQ channels, candidate rows, ChemCore decision/provenance, hardware/environment state and analytical charts. Its `SessionSeries` input is now normalized from recorded `Rs` against the accepted HR-0101-045 R0 before report/chart generation, so report-side `Rs/R0` is consistent with the current analytical authority even before physical firmware synchronization.

## 3. Regression protection added

`AcceptedR0NormalizerTest` verifies that an accepted HR-0101-045 R0 reconstructs the ratio from `Rs` even if the connected firmware reports an older ratio/R0, and that telemetry ratio is only a compatibility fallback.

`ChemCoreAnalysisPresenterTest` verifies that screening-fit ppm remains visibly withheld and that the log/inverse equations, joint-fit explanation, M4 gate and residual evidence are present.

`ChemCoreResultEnvelopeV3Test` explicitly locks the rule that partial M4 contributor coverage is insufficient while complete contributor coverage inside the validated domain may qualify at the candidate-support level.

`SessionSeriesNormalizationTest` verifies that the report view replaces a stale telemetry `ratio/R0` with the accepted HR-0101-045 normalization from `Rs` while retaining `r0_reference_validated=false`.

## 4. Android exact-source verification

Controlled builder: `hazrob-0101-build`  
Deploy: `dep-daj0ah15efls73f5ae5g`  
Source: `6ab64a33a829fa7fd87166aeb0a2e1cbec4b1afc`  
Status: **live**

Verified:

- HazRob 0.10.1 cross-component integration contract: **PASS**;
- `:app:testDebugUnitTest`: **PASS**, including the HR-0101-046 normalization/presentation/report regressions;
- `:app:assembleRelease`: **PASS**;
- `lintVitalRelease`: **PASS**;
- signing continuity: **PASS**;
- versionName: `0.10.1`;
- versionCode: `21`;
- APK SHA-256: `56f4fd8e93c2aa5a6a295e2816d4b27bc2d17735aa74287de4f6d570b062c1f9`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

## 5. Firmware compatibility verification

HR-0101-046 introduced no firmware source logic change after HR-0101-045. Redundant controlled firmware builds were used to verify whole-system compatibility.

### Exact executable-source firmware baseline before report-only/app-report closeout

Deploy: `dep-daj07qg6gcjs738ghr1g`  
Source: `2349fdffa81c6b9079167e828028dc8b2a7659ee`  
Status: **live**

Verified:

- cross-component integration contract: **PASS**;
- main ESP32: **PASS**, 1,091,105 bytes program storage (34%), 50,204 bytes globals (15%), 277,476 bytes remaining for local variables;
- ESP32-CAM: **PASS** under `esp32:esp32:esp32cam` with the existing non-blocking compiler warnings only.

A preceding clean build `dep-daj07168h83s73a1egk0` at source `1311d787de8928232d48f8f1afff6f2f1bb81cb2` also completed **live** with the same main ESP32 size/RAM result and camera compatibility PASS.

Git comparison from `2349fdff...` to final executable Android source `6ab64a33...` contains only:

- `app/src/main/java/com/servoc/app/data/SessionSeries.java`;
- `app/src/test/java/com/servoc/app/data/SessionSeriesNormalizationTest.java`;
- HR-0101-046 documentation/manifest files.

No `firmware/` path changed after the successful exact `2349fdff...` firmware build. Therefore the final HR-0101-046 app/report closeout does not invalidate the verified ESP32 or ESP32-CAM binaries.

## 6. Scientific state after HR-0101-046

The software architecture, mathematical presentation, R0 normalization authority, ChemCore-only analysis surface and session-bound PDF/XLSX reporting path are now closed at the source/build level. The next work is controlled physical validation rather than further architectural discovery.

Still pending before final analytical/metrological acceptance:

1. install/runtime smoke test of the new APK on the actual Android device;
2. physical BME/BMP identification and valid live T/RH/P so environmental correction availability can be assessed without assumptions;
3. controlled reference-gas campaigns for selected analyte/sensor pairs under the existing calibration QC gate (at least 3 concentrations × 3 stabilized replicates, span ≥4×, configured CV/R²/baseline/recovery/heater requirements);
4. post-calibration open-set, ambiguity, interference/mixture and out-of-domain validation using measured evidence;
5. eventual ESP32 reflash to synchronize embedded R0 provenance, although this is not required for application-side HR-0101-045 normalization because the app reconstructs `Rs/R0` from `Rs`.

## 7. Non-regression constraints

- HazRob remains 0.10.1 / API4.
- No GPIO reassignment.
- No drive/camera/remote-control behavior change is introduced by HR-0101-046.
- No assumed T/RH/P.
- No automatic extrapolation beyond validated calibration domains.
- No field clean-air R0 promoted to reference-gas calibration.
- No screening-fit concentration presented as reportable occupational ppm.
- No candidate-level quantitative support unless every contributing quantitative curve is device/reference-gas validated and inside its validated domain.

The documentation closeout commits after executable source `6ab64a33...` are documentation-only and do not alter the verified executable source.
