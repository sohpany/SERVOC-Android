# HazRob 0.10.1 — Field clean-air R0 baseline

Controlled change: **HR-0101-043**  
Status: **verification pending**

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

## Scientific status

These values are **device-specific clean-air normalization baselines** for the installed HazRob sensor assembly. They make the field fingerprint `Rs/R0` dimensionless and should place a repeat clean-air measurement near `Rs/R0 = 1.0` for each channel, within normal sensor drift and repeatability.

They are **not** manufacturer/reference-gas validation of any ppm curve. A curve may be treated as quantitative only when its own device-specific calibration record passes the existing `referenceValidated` gate. Environmental compensation is also not claimed because temperature/humidity/pressure data were unavailable during this source session.

## Acceptance tests

1. Android unit tests pass, including `FieldR0BaselineTest`.
2. Android release APK builds successfully.
3. ESP32 master firmware compiles with the new R0 constants; ESP32-CAM remains unaffected.
4. A new very-clean-air session after flashing the firmware yields approximately `Rs/R0 ≈ 1.0` on all eight channels; deviations are evaluated by the stability gate rather than forced to exactly 1.0.
5. Reports expose the calibration provenance and do not promote this clean-air baseline to `reference-gas validated` ppm calibration.
