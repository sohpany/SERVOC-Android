# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-045

**Change ID:** HR-0101-045  
**Title:** Three-session field clean-air R0 acceptance baseline  
**Version:** HazRob 0.10.1 / API 4  
**Status:** **SOURCE/BUILD PASS — FIELD R0 ACCEPTANCE PASS**

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

Until the ESP32 is reflashed, it will continue to emit normalized ratios using its currently installed older R0 constants. The controlled source and matching Android build contain the new standard; hardware synchronization can be performed when physical access is convenient.

ESP32-CAM does not require an R0-related logic change.

## 8. Scientific boundary

HR-0101-045 closes the clean-air **R0 field repeatability/acceptance** requirement only. It does not activate quantitative ppm reporting.

PPM remains gated by the HR-0101-044 M4 requirements, including accepted device/reference-gas calibration, complete contributing-curve validation, QC/publication acceptance, open-set identity acceptance, and operation inside the validated concentration domain.

## 9. Source/build verification

Verified exact source commit: `197918e583e60f9369194137cc2ca098ecf75e90`.

### Android controlled builder

- Deploy: `dep-daivpde7bikc73a8dcng` — **live**.
- Cross-component HazRob 0.10.1 integration contract: **PASS**.
- `:app:testDebugUnitTest`: **PASS**, including the updated `FieldR0BaselineTest`.
- `:app:assembleRelease`: **PASS**.
- `lintVitalRelease`: **PASS**.
- APK SHA-256: `cd961801351e5ee0b413074767887fe9d8943d56236846f1c148f3d7a5c035d6`.
- Signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0` — signing continuity **PASS**.
- Version identity remains `versionName=0.10.1`, `versionCode=21`.

### Firmware controlled builder

An initial cached deployment `dep-daivperm8hqs73eeruag` stopped before compilation because `arduino-cli config init` encountered a pre-existing cached configuration file. This was a builder-cache state issue, not a source/compile failure. A clean-cache exact-source redeploy was therefore executed.

- Clean-cache deploy: `dep-daivr095efls73f3tcj0` — **live**.
- Cross-component HazRob 0.10.1 integration contract: **PASS**.
- Arduino CLI: `1.5.1`.
- ESP32 Arduino core: `esp32:esp32@3.3.11`.
- Main ESP32 target `esp32:esp32:esp32:PartitionScheme=huge_app`: **PASS**.
  - Program storage: **1,091,105 bytes (34%)**.
  - Globals: **50,204 bytes (15%)**; **277,476 bytes** left for local variables.
- ESP32-CAM target `esp32:esp32:esp32cam`: **PASS**.
  - Program storage: **1,118,771 bytes (35%)**.
  - Globals: **67,836 bytes (20%)**; **259,844 bytes** left for local variables.
- Existing non-blocking warnings in remote cloud/video code and the unused helper remain; no HR-0101-045 R0 compilation error is present.

The documentation closeout commit(s) after `197918e...` are documentation-only and do not modify the verified executable source.

GitHub Actions remains non-authoritative while the previously documented pre-run infrastructure blocker HR-0101-013 persists.
