# HazRob 0.10.1 — Accepted three-session field clean-air R0 baseline

Controlled change: **HR-0101-045**  
Status: **SOURCE/BUILD PASS — FIELD R0 ACCEPTANCE PASS**

## 1. Acceptance campaign

The operational device-specific clean-air normalization baseline is derived from three independent 30.0 s HazRob field sessions collected on 2026-09-13 under the accepted clean-air field protocol.

| Report | Session | UTC | Samples | Window | MQ power age |
|---|---|---|---:|---:|---:|
| `HazRob_Report_000011_20260913_040059.xlsx` | `FIELD-1789261215761` | `2026-09-13T01:00:47Z` | 73 | 30.0 s | 554629 ms |
| `HazRob_Report_000012_20260913_041047.xlsx` | `FIELD-1789261807307` | `2026-09-13T01:10:38Z` | 70 | 30.0 s | 1145386 ms |
| `HazRob_Report_000013_20260913_042053.xlsx` | `FIELD-1789262410944` | `2026-09-13T01:20:43Z` | 73 | 30.0 s | 1750055 ms |

Campaign ID: `FIELD-R0-3SESSION-20260913`  
Total samples: **216**.  
All three reports show both ADS1115 devices available and all **8/8 MQ channels valid**.

The field operator accepted the outdoor clean-air and wind conditions as meeting the campaign criteria. Wind suitability is therefore recorded as controlled external field-protocol context. It is not an onboard environmental measurement.

## 2. Environmental boundary

The onboard environmental channel was unavailable in all three reports (`Environment NOT READY`; no T/RH/P values). Therefore:

- `R0_ENVIRONMENT_COMPENSATED=false` remains mandatory;
- the accepted wind/weather condition does not imply mathematical T/RH/P compensation;
- meteorological observations may be retained as external context when available, but must not be represented as BME/BMP measurements;
- `R0_REFERENCE_GAS_VALIDATED=false` remains mandatory because clean-air normalization is not a reference-gas ppm calibration.

## 3. Aggregation method

The prior single-session baseline was embedded in the reports, allowing the recorded `Rs/R0` time series to be converted back to sensor resistance using:

`Rs = (Rs/R0)_recorded × R0_previous`

The project analytical pipeline operates in log space. To prevent the minor 70/73/73 sample-count difference from weighting one independent session more than another, the accepted campaign baseline uses a two-stage equal-session estimator:

1. calculate the geometric mean of `Rs` within each complete 30.0 s session;
2. calculate the geometric mean of the three session geometric means.

Thus each independent field session has equal statistical weight.

## 4. Per-session geometric means and repeatability

| Sensor | Session 11 Rs GM (kΩ) | Session 12 Rs GM (kΩ) | Session 13 Rs GM (kΩ) | Between-session CV |
|---|---:|---:|---:|---:|
| MQ2 | 92.767585524 | 90.713267411 | 91.732716417 | 1.1197% |
| MQ3 | 21.089109554 | 19.700877688 | 21.305303844 | 4.2063% |
| MQ4 | 64.050945470 | 65.433189613 | 66.345118126 | 1.7695% |
| MQ5 | 252.208724013 | 251.075244890 | 252.612721877 | 0.3163% |
| MQ7 | 158.662531242 | 159.645163586 | 163.537533139 | 1.6052% |
| MQ8 | 56.222777105 | 54.114876863 | 54.352490731 | 2.1031% |
| MQ9 | 69.619322048 | 69.344587797 | 71.017799456 | 1.2820% |
| MQ135 | 212.634925040 | 212.773394518 | 214.312884226 | 0.4368% |

Within-session CVs were below 1.5% for every sensor in every session. The largest between-session CV was MQ3 at 4.2063%, while all other channels were approximately 0.32–2.10%. This is accepted as strong field repeatability for the device normalization baseline.

## 5. Accepted device R0 values

| Sensor | Accepted R0 (kΩ) |
|---|---:|
| MQ2 | **91.7340228444** |
| MQ3 | **20.6860315768** |
| MQ4 | **65.2695876771** |
| MQ5 | **251.9647223909** |
| MQ7 | **160.6013515297** |
| MQ8 | **54.8886824742** |
| MQ9 | **69.9900854903** |
| MQ135 | **213.2390474819** |

These eight values supersede the single-session operational R0 values from `FIELD-1789255293867`.

## 6. Scientific status

The campaign is accepted as **FIELD PASS for device-specific clean-air R0 normalization and repeatability**.

This acceptance does **not** establish:

- manufacturer/reference-gas validation;
- universal chemical selectivity;
- validated quantitative ppm curves;
- environmental compensation in the absence of measured T/RH/P.

Quantitative ppm remains withheld unless the relevant device/reference-gas M4 calibration, identity/open-set, quality-control, and validated-domain gates pass independently.

## 7. Source implementation and build verification

HazRob remains **0.10.1 / API 4**. No GPIO or hardware-profile change is introduced.

The accepted baseline is implemented in:

- `firmware/HazRob/Config.h`;
- `app/src/main/java/com/servoc/app/calibration/FieldR0Baseline.java`;
- `app/src/test/java/com/servoc/app/calibration/FieldR0BaselineTest.java`.

Verified executable/source commit: `197918e583e60f9369194137cc2ca098ecf75e90`.

- Android controlled deploy `dep-daivpde7bikc73a8dcng`: **live / PASS**.
- Android unit tests, release assembly, lint vital, and signer continuity: **PASS**.
- APK SHA-256: `cd961801351e5ee0b413074767887fe9d8943d56236846f1c148f3d7a5c035d6`.
- Firmware clean-cache controlled deploy `dep-daivr095efls73f3tcj0`: **live / PASS**.
- Main ESP32 compile: **PASS**, 1,091,105 bytes program / 50,204 bytes globals.
- ESP32-CAM compatibility compile: **PASS**, 1,118,771 bytes program / 67,836 bytes globals.

The physical ESP32 does not acquire the new embedded constants until it is eventually reflashed. This does not invalidate the three accepted reports: their complete time-series ratios and prior R0 provenance allow reconstruction of `Rs` and derivation of the accepted campaign baseline. Reflash may therefore be deferred until physical access to the ESP32 is convenient.

Subsequent closeout commits are documentation-only and do not alter the verified executable source.
