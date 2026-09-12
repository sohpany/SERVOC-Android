# HazRob 0.10.1 — Field clean-air R0 baseline

Controlled change: **HR-0101-043**  
Status: **SOURCE/BUILD PASS — FIELD VALIDATION PENDING**

## Source session

- Report: `HazRob_Report_000010_20260913_022226333.xlsx` (user-supplied measurement report)
- Session: `FIELD-1789255293867`
- UTC start: `2026-09-12T23:22:12Z`
- MQ power age reported at export: `190993 ms`
- Time series: `74 samples / 30.1 s`
- Environmental channel: unavailable in this session (`Environment OFF / NONE`)
- Acquisition: eight MQ channels through two ADS1115 devices.

## Aggregation rule

The project already aggregates MQ response in log space. For consistency, the accepted field baseline is the **geometric mean of Rs** across all 74 samples in the clean-air session. The prior firmware used `R0=10.0 kΩ` placeholders, so the session time-series `Rs/R0` values were converted back to Rs by multiplying by 10 kΩ before geometric aggregation.

## Accepted device baselines

| Sensor | R0 (kΩ) |
|---|---:|
| MQ2 | 79.3176036586 |
| MQ3 | 8.7149649220 |
| MQ4 | 36.1454126550 |
| MQ5 | 214.5398913808 |
| MQ7 | 119.2408579279 |
| MQ8 | 35.7836813368 |
| MQ9 | 32.3349994303 |
| MQ135 | 198.8868482045 |

## Implemented semantics

- Firmware uses the eight field R0 values directly when calculating `Rs/R0`.
- Raw firmware MQ JSON declares `r0_calibrated=true`, `r0_source=field_clean_air_r0`, the source session and sample count.
- Raw firmware and Android both keep `r0_reference_validated=false`; the clean-air baseline cannot by itself enable quantitative ppm interpretation.
- Android accepts a persistent manual device R0 as an override, otherwise it falls back to this controlled field baseline.
- Normalized Android state distinguishes `CALIBRATED_FIELD_BASELINE` from `CALIBRATED_MANUAL`, `CALIBRATED_FIRMWARE`, and `UNCALIBRATED`.

## Scientific status

These values are **device-specific clean-air normalization baselines** for the installed HazRob sensor assembly. They make the field fingerprint `Rs/R0` dimensionless and should place a repeat clean-air measurement near `Rs/R0 = 1.0` for each channel, within normal sensor drift and repeatability.

They are **not** manufacturer/reference-gas validation of any ppm curve. A curve may be treated as quantitative only when its own device-specific calibration record passes the existing `referenceValidated` gate. Environmental compensation is also not claimed because temperature/humidity/pressure data were unavailable during this source session.

## Source/build verification

Verified source commit: `48c374660a64f0840e92c3fd142fc8780ba90026`

- Cross-component HazRob 0.10.1 integration contract: **PASS**.
- Android unit tests: **PASS**.
- Android release assembly: **PASS**.
- Android controlled builder deploy: `dep-daiualm7bikc73a3tc3g` — **live**.
- Final test APK SHA-256: `e6836b6304e2915962d72f64de3bf2fdaca8d49c26aa9d52a3bab3ee3125c05e`.
- APK signer SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- ESP32 master firmware compile: **PASS**; 1,091,089 bytes program storage (34%), 50,204 bytes globals (15%).
- ESP32-CAM firmware compile: **PASS**; 1,118,771 bytes program storage (35%), 67,836 bytes globals (20%).
- Firmware controlled builder deploy: `dep-daiuan7qj5pc73bjl8c0` — **live**.
- Existing non-blocking compiler warnings remain in remote cloud/video code and one unused helper; no R0/calibration compile errors were recorded.

The documentation closeout commit is intentionally separate from the verified source commit and changes documentation only; no source code was changed after the verified build.

## Remaining field acceptance tests

1. Flash the final master firmware derived from verified source commit `48c374660a64f0840e92c3fd142fc8780ba90026`.
2. Run a new very-clean-air session after adequate MQ warm-up. The eight channels should center approximately around `Rs/R0 ≈ 1.0`; normal drift is evaluated by the stability gate rather than forcing an exact value.
3. Confirm the exported report exposes `field_clean_air_r0`, the calibration session provenance, and `r0_reference_validated=false`.
4. Review the detailed ENV diagnostics in the same physical session because environmental compensation remains unavailable until a valid BME/BMP measurement is present.
5. Only after this physical repeatability check may HR-0101-043 be marked **FIELD PASS**.
