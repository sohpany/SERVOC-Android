# HazRob 0.10.2 / HR069-R1 — 2026-10-09 field feedback (10 findings)

**Only write branch:** `release/hazrob-0.10.2-rc2` (no older versions used).
**Previous validated Android source:** vCode29 `41dba5f781903c977cf4c08e024ae8b3a7b0b7bd`.
**New diagnostic Android source:** vCode30 `2e63981b9e2aeabfea8751d5a422a99df8ec4e36`; validation workflows must PASS before distribution.
**Main ESP32:** HR069-R1 unchanged. ESP32-CAM HR069 unchanged. ChemCore Stable `1.0.0-hr069r1` unchanged.
**Release authority:** no field acceptance, no production promotion, no robot flashing.

## Evidence from four field screenshots (operator observations)

- Direct connected; heartbeat 184 ms.
- ADS1/ADS2 OK; MQ power OFF, so MQ curve/R0/chemical performance cannot be concluded from this capture.
- IR shows ground present; this does not prove LOW toggles during a lift test.
- ECHO invalid and monotonically increasing timeouts in Status (2113) and driving screen (3686); real pulse validity requires electrical diagnosis.
- ENV fluctuates/missing; microphone RMS was visible; reports had 11 stored measurements, while selected chart lacked a linked analyzable session.
- Drive tank behavior reported as needing movement of both controls and speed not following the finger precisely.

## Correction matrix

| # | Finding | Changes to vCode30 | Remaining gate |
|---|---|---|---|
| 1 | Echo invalid/inadequate on driving HUD; IR diagnostics | ControlTab now displays explicit invalid Echo/timeouts on the driving HUD; StatusTab shows pulse duration and IR raw level/HIGH=ground | **NOT FIXED HARDWARE**: test TRIG GPIO23, level shifted ECHO GPIO39, 5V/ground, rising/falling edges, timeout; verify IR HIGH on ground and LOW on lift. Do not falsify valid distance or reverse polarity. |
| 2 | Sites should be a dropdown, sessions a dependent dropdown | FieldSessionsTab includes two hierarchical selectors; session changes with site. Only selected session actions are expanded | Phone UI/SQLite persistence tests; validate creating/deleting/renaming if supported; local user data only |
| 3 | BME appears/disappears | **User clarified readings must remain live/dynamic.** Removed experiment with a 5s cached last-valid BME reading; now show only current sensor measurements or explicit unavailable state. Firmware environmental sampling is 1000 ms; unchanged. | Reproduce I2C faults, record ENV `status_code`, `read_failures`, `chip_id`, `last_ok_age_ms`, SDA13/SCL14 under motor/MQ loads. Never smooth, replace, or fake chemical-analysis environmental inputs. |
| 4 | Smart report design | ReportsTab only lists diagrams actually supported by stored time series or ChemCore decision. Empty white chart panel hidden when no evidence; chart rendering moved to worker | Full PDF/XLSX redesign, readable page layout, field/site export phone-open tests still **PENDING** |
| 5 | Severe app freezes | MeasurementsTab refreshes visible pane only; expensive 144/candidates list rebuild throttled to 750 ms; background chart rendering | Profile actual app ANR/StrictMode, DB query paths, Wi-Fi/video performance under all channels |
| 6 | Two tank controls don't behave smoothly/independently | Each slider's individual update is passed as (left,right) pair, source contract tested; 20Hz operator command cadence **restored** after intermediate CI audit found its accidental regression. | Physical bench verify single-slider +20/+50/+100, reverse and independent left/right, PWM on GPIO17/16, loaded motor supply; no claim this is fully fixed without field test |
| 7 | Audio readings flash | Display-only moving average for MAX4466 RMS and peak-to-peak; underlying raw acquisition/recorded telemetry unchanged | Examine microphone signal and calibration; do NOT present uncalibrated RMS as certified dB SPL |
| 8 | Duplicate local R0 editing | Removed redundant R0 edit dialog/button from developer Experiments; retain device Baseline/Drift health check. Developer `معايرة R0` tab is single maintenance authority. | Confirm tab access and stored R0 values unchanged |
| 9 | Analytics should begin with candidate shortlist | Moved candidate summary to top of operator Analysis pane; fixed duplicate appended candidate rows on selection | Qualify ranking/unknown/mixtures with known and blind gases; no fabricated identification or quantitative ppm |
| 10 | Analytical chart dropdown and panel inert | Dynamic chart choices filtered by actual selected dataset, async chart render, explanatory unavailable status, chart view hidden when no data | Valid stored series/decision chart render, screenshot/cross-app usage; quantitative and environmental plot validity |

## Strict BME live-data rule

1. Dynamic real readings only: BME/BMP sampling interval is `Cfg::ENV_PERIOD=1000` ms, while Android polls data every 150 ms. UI can redisplay the same *current sample* during this interval, but should not substitute an earlier sample when the firmware reports `ok=false`.
2. When a failure occurs, show a reason and a sensor-health fault; no held last value, no synthetic temperature/RH/P, no smoothing of BME values.
3. A failure in the BME environment bus should not damage the separate ADS1115 analytical bus, but actual wiring/power must still be checked.
4. The audio RMS display averaging is **not** a BME smoothing algorithm and is not a traceable sound pressure measurement.

## Build/test discipline

- Incremented `app/build.gradle` diagnostic Android `versionCode 29 → 30`; `versionName 0.10.2`, revision HR069-R1 unchanged.
- Added `app/src/test/java/com/servoc/app/HR069R1FieldUXRegressionsTest.java` to lock no stale BME, pane visibility performance, site-session scope, chart data gating, duplicate R0 removal, and Echo HUD.
- Pre-vCode30 CI failed because an experimental 100 ms motor scheduling modification violated `audit_0102_rc2_runtime_contract.py` required 50 ms / 20Hz drive contract. This was corrected in `2e63981b9e2aeabfea8751d5a422a99df8ec4e36`. Earlier FAIL is not proof that this latest source fails; await exact-source CI.
- Latest source must pass Android unit tests, RC2 audit, signed artifact, firmware Main and CAM compilation (untouched source), then real hardware/app testing before any distribution.
- No deployment changes to offline Railway WAN services and no alteration to local library/field session provenance.

**Important:** Android visual fixes cannot establish electrical Echo correctness, battery calibration, BME I2C reliability, gas detection accuracy, or real loaded motor control. This is an **intermediate diagnostic checkpoint**, not a release sign-off.
