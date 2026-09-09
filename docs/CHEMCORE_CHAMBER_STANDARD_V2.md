# ChemCore Standard Sampling Chamber v2

**Status:** ADOPTED AS ESTIMATED ENGINEERING PROFILE  
**Date:** 2026-09-09  
**Profile ID:** `hazrob-chemcore-chamber-v2`  
**Flow status:** `FLOW_ESTIMATED` — not measured/calibrated

## 1. Fixed mechanical geometry

- Internal chamber envelope: **90 × 40 × 150 mm**
- Gross volume: **540 cm³ = 0.540 L**
- Gross chamber cross-section: **90 × 40 mm = 36 cm² = 0.0036 m²**
- Front inlet: **40 × 20 mm**, centered = **8 cm² = 0.0008 m²**
- Rear extractor: **40 × 40 mm, 5 V, 0.28 A**
- Rear wall outlet aperture: approximately **28 × 28 mm**, centered over the fan. This is ~49% of the 40 × 40 mm fan-face area and is preferred over exposing one asymmetric half of the fan.
- Only the eight MQ sensing caps project into the chamber. Boards, ADCs, wiring and power electronics remain outside the analytical volume.

## 2. Sensor-cap displacement estimate

The installed MQ family is mechanically close to two Winsen package families:

- common larger MQ packages such as MQ-2/MQ-4/MQ-5/MQ-8/MQ135: nominal outer diameter about **19 mm**; published overall package height for these variants is 24.2 mm including pins.
- compact B-family packages such as MQ-3B/MQ-7B/MQ-9B: approximately **16.7–17 mm diameter × 9.4 mm body height**.

Because only the caps protrude into the chamber, a conservative aggregate displaced-volume engineering allowance of **23 cm³** is adopted for the eight caps. The exact value is deliberately assigned a small geometric uncertainty because the displacement is only ~4.3% of gross chamber volume.

Therefore:

`V_effective ≈ 540 - 23 = 517 cm³ = 0.517 L`

This is an engineering geometry correction, not a gas-flow calibration.

## 3. Fan proxy and fixed engineering flow factor

The exact supplied fan model is not legible enough to obtain a certified fan curve. A closely matched published 40 × 40 × 10 mm brushless axial fan at **5 V / 0.28 A / 1.4 W** is rated at approximately **6.66 CFM free-air** and 4.55 mm-H2O static pressure. This electrical match is used only as a proxy.

Proxy free-air flow:

`6.66 CFM × 0.471947 L/s per CFM ≈ 3.14 L/s`

The standard chamber intentionally restricts both inlet/outlet geometry and does not assume free-air operation. A conservative system-flow factor of approximately 35% of the proxy free-air value is adopted:

`Q_est ≈ 3.14 × 0.35 ≈ 1.10 L/s`

Stored engineering value:

- `Q_est = 0.00110 m³/s = 1.10 L/s`
- engineering uncertainty: `±0.00040 m³/s = ±0.40 L/s`
- status remains `FLOW_ESTIMATED`

The broad uncertainty is intentional and prevents this value from being presented as measured metrology.

## 4. Derived standard sampling quantities

Using the fixed profile:

### Inlet mean velocity

`v_inlet = Q / A_inlet`

`v_inlet = 0.00110 / 0.0008 ≈ 1.375 m/s`

### Chamber superficial mean velocity

`v_chamber = Q / A_chamber`

`v_chamber = 0.00110 / 0.0036 ≈ 0.306 m/s`

The value used analytically as the standard fixed estimated chamber-flow factor is therefore:

**`v_chamber,est = 0.31 m/s`**

with the underlying Q uncertainty retained in the profile.

### Effective nominal residence time

`tau_eff = V_effective / Q`

`tau_eff = 0.000517 / 0.00110 ≈ 0.47 s`

This implies approximately 2.13 effective chamber volumes per second under the engineering estimate. Real gas exchange is not assumed to be ideal plug flow; local recirculation and wake regions around the sensor caps remain possible.

## 5. Scientific interpretation rule

This standardized geometry may be treated as a **fixed covariate** for ChemCore experiments performed with the same physical chamber revision. It may be used for repeatable model development and between-session comparison as long as every such session references `hazrob-chemcore-chamber-v2`.

However:

- the value must remain labeled `FLOW_ESTIMATED`;
- it must not be promoted to `FLOW_MEASURED` or `FLOW_CALIBRATED` without physical airflow metrology;
- changing inlet size, outlet aperture, fan, fan voltage, sensor-cap arrangement or chamber geometry creates a new SamplingProfile version and invalidates direct reuse of flow-dependent calibration validity.

## 6. Source basis

Mechanical sensor package references:

- Winsen MQ-2 product/manual: https://www.winsen-sensor.com/product/mq-2.html
- Winsen MQ135 product/manual: https://www.winsen-sensor.com/product/mq135.html
- Winsen MQ-3B product/manual: https://www.winsen-sensor.com/product/mq-3b.html
- Winsen MQ-7B product/manual: https://www.winsen-sensor.com/product/mq-7b.html
- Winsen MQ-9B product/manual: https://www.winsen-sensor.com/product/mq-9b.html

Comparable fan engineering proxy:

- Provento 40 × 40 × 10 mm DC brushless fan table, 5 V / 0.28 A / 1.40 W / 6.66 CFM: https://www.proventofan.com/axial-fan/dc-brushless-fan/

## 7. Implementation checkpoint

Implemented in `SamplingProfile.hazRobStandardChamberV2()` with unit tests for:

- gross and effective volume;
- reduced inlet area;
- fixed `FLOW_ESTIMATED` Q;
- inlet velocity;
- superficial chamber velocity;
- effective residence time;
- non-promotion to calibrated status.

This document is the continuation reference for the chamber v2 geometry. Do not revert to the original 40 × 40 mm inlet or full fan-face outlet in later ChemCore work unless a new sampling-profile revision is explicitly created.
