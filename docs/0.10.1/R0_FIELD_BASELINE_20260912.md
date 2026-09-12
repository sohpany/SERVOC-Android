# HazRob 0.10.1 — Field clean-air R0 baseline

Controlled change: **HR-0101-043**  
Status: **SOURCE IMPLEMENTED — FINAL BUILD REVERIFICATION PENDING — FIELD VALIDATION PENDING**

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

## Verification history

A preliminary integrated build before the final raw-firmware provenance correction passed the HazRob cross-component audit, all Android unit tests, Android release assembly, and both ESP32 master / ESP32-CAM compilation. The final provenance correction changes metadata only; both controlled builders must be rerun on the same final branch head before source/build verification is closed.

## Remaining acceptance tests

1. Final-head Android unit tests and release APK build pass.
2. Final-head ESP32 master and ESP32-CAM firmware compile pass.
3. A new very-clean-air session after flashing the final firmware yields approximately `Rs/R0 ≈ 1.0` on all eight channels; deviations are evaluated by the stability gate rather than forced to exactly 1.0.
4. The new report exposes `field_clean_air_r0` provenance and does not promote it to `reference-gas validated` ppm calibration.
5. ENV diagnostics are reviewed in the same physical session because environmental compensation remains unavailable until a valid BME/BMP measurement is present.
