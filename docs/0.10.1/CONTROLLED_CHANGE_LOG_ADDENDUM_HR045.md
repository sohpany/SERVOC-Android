# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-045

**Change ID:** HR-0101-045  
**Title:** Three-session field clean-air R0 acceptance baseline  
**Version:** HazRob 0.10.1 / API 4  
**Status:** FIELD R0 ACCEPTANCE PASS — SOURCE UPDATED — BUILD VERIFICATION PENDING

## 1. Objective

Replace the provisional/single-session operational clean-air R0 baseline with a repeatability-supported baseline derived from three independent field clean-air sessions, while preserving the scientific separation between R0 normalization and device/reference-gas quantitative calibration.

## 2. Accepted field evidence

The accepted campaign is `FIELD-R0-3SESSION-20260913` and contains:

1. `FIELD-1789261215761` — report 11 — 73 samples / 30.0 s;
2. `FIELD-1789261807307` — report 12 — 70 samples / 30.0 s;
3. `FIELD-1789262410944` — report 13 — 73 samples / 30.0 s.

Total: **216 samples** across three approximately 10-minute-separated analysis windows with continuous MQ heater operation. All reports contain valid readings from both ADS1115 devices and all eight installed MQ channels.

The field operator explicitly accepted the outdoor clean-air and wind conditions as meeting the campaign protocol. This is recorded as external field-condition acceptance, not as an onboard environmental measurement.

The reports show the onboard ENV channel unavailable; therefore `R0_ENVIRONMENT_COMPENSATED=false` remains unchanged. Clean-air R0 remains non-reference-gas evidence, so `R0_REFERENCE_GAS_VALIDATED=false` also remains unchanged.

## 3. Estimator

For each MQ channel:

1. reconstruct each sample resistance from the recorded normalized ratio and the previously controlled R0: `Rs = ratio × R0_previous`;
2. compute the geometric mean of `Rs` within each session;
3. compute the geometric mean of the three session geometric means, giving equal weight to each independent session.

This estimator is consistent with HazRob's log-space analytical handling while avoiding unequal weighting from the 70/73/73 sample-count difference.

## 4. Repeatability result

Between-session CV of the three session geometric means:

- MQ2: 1.1197%;
- MQ3: 4.2063%;
- MQ4: 1.7695%;
- MQ5: 0.3163%;
- MQ7: 1.6052%;
- MQ8: 2.1031%;
- MQ9: 1.2820%;
- MQ135: 0.4368%.

Every sensor/session also showed within-session CV below 1.5%. The campaign is therefore accepted as the operational field clean-air normalization baseline.

## 5. Accepted R0 values

| Sensor | R0 (kΩ) |
|---|---:|
| MQ2 | 91.7340228444 |
| MQ3 | 20.6860315768 |
| MQ4 | 65.2695876771 |
| MQ5 | 251.9647223909 |
| MQ7 | 160.6013515297 |
| MQ8 | 54.8886824742 |
| MQ9 | 69.9900854903 |
| MQ135 | 213.2390474819 |

The earlier single-session R0 baseline from `FIELD-1789255293867` is superseded for operational normalization, but retained as traceable historical evidence.

## 6. Source changes

Updated without changing HazRob 0.10.1/API4 identity or GPIO allocation:

- `firmware/HazRob/Config.h` — new embedded R0 constants and three-session provenance;
- `app/src/main/java/com/servoc/app/calibration/FieldR0Baseline.java` — matching Android baseline and campaign metadata;
- `app/src/test/java/com/servoc/app/calibration/FieldR0BaselineTest.java` — regression lock for the accepted campaign and values;
- `docs/0.10.1/R0_FIELD_BASELINE_20260913.md` — analytical evidence and acceptance record.

## 7. Deferred hardware synchronization

Immediate physical reflashing of the installed ESP32 is not required to accept the field campaign. The uploaded reports already contain sufficient traceable data to reconstruct `Rs` and calculate the accepted R0 standard.

Until the ESP32 is reflashed, it will continue to emit normalized ratios using its currently installed older R0 constants. The controlled source and subsequent matching Android build contain the new standard; hardware synchronization can be performed when physical access is convenient.

ESP32-CAM does not require an R0-related logic change.

## 8. Scientific boundary

HR-0101-045 closes the clean-air **R0 field repeatability/acceptance** requirement only. It does not activate quantitative ppm reporting.

PPM remains gated by the HR-0101-044 M4 requirements, including accepted device/reference-gas calibration, complete contributing-curve validation, QC/publication acceptance, open-set identity acceptance, and operation inside the validated concentration domain.

## 9. Required source/build verification

Before HR-0101-045 is marked SOURCE/BUILD PASS, the final exact branch head containing these changes must pass the controlled Render evidence path:

1. HazRob cross-component integration contract;
2. Android unit tests including `FieldR0BaselineTest`;
3. Android release assembly/signing continuity;
4. main ESP32 compile;
5. ESP32-CAM compatibility compile or explicit unchanged-camera compatibility confirmation under the existing whole-system builder.

GitHub Actions remains non-authoritative while the previously documented pre-run infrastructure blocker HR-0101-013 persists.
