# HazRob 0.10.0 — Hardware / Firmware / App / Connectivity Integration Plan

Status: active implementation plan for the 0.10.0 experimental major release.

## Release priorities

1. **First deliverable:** compact master ESP32 firmware 0.10.0.
2. Target compiled Flash use: **<=70%** where practical; hard release ceiling: **<=80%** of the configured app partition.
3. Then update Android, direct/remote protocol handling, HiveMQ telemetry use, ESP32-CAM/Render compatibility and the final release package.
4. Keep ChemCore, AMCI, molecular libraries, energy estimation and UI-heavy logic on Android whenever possible; ESP32 remains the deterministic hardware/telemetry/safety layer.

## Frozen 0.10 GPIO map

| Function | ESP32 GPIO | Notes |
| --- | ---: | --- |
| L298N ENA | 16 | PWM, left-side speed |
| L298N IN1 | 18 | left direction |
| L298N IN2 | 27 | left direction |
| L298N ENB | 17 | PWM, right-side speed |
| L298N IN3 | 19 | right direction |
| L298N IN4 | 32 | right direction |
| I2C SDA | 21 | shared ADS1/ADS2 + chamber environment sensor; future lux sensor may share this bus |
| I2C SCL | 22 | shared ADS1/ADS2 + chamber environment sensor; future lux sensor may share this bus |
| MQ power relay | 25 | one active-low relay, all eight MQ |
| speaker / alert signal | 26 | reserved; 8-ohm speaker not installed and must not be driven directly |
| front ultrasonic TRIG | 23 | front obstacle ranging |
| front ultrasonic ECHO | 39 | input only; 5 V HC-SR04-style ECHO requires level reduction to <=3.3 V |
| battery voltage ADC | 33 | ADC1; divider required |
| IR fall-edge sensor | 35 | lower front edge |
| MAX4466 sound level | 36 | ADC1 |
| LDR/LM393 DO | 34 | digital light-threshold state only; not quantitative lux |
| internal electronics/battery temperature | 14 | **reserved only** until the actual sensor is purchased; no driver/type assumption in 0.10 |

Hardware profile: `hazrob-0.10-1r8mq-l298n6-batt-ultra-ldr-v1`.

### I2C analytical/physical bus

- ADS1115 #1: `0x48`: MQ2 A0, MQ4 A1, MQ5 A2, MQ8 A3.
- ADS1115 #2: `0x49`: MQ3 A0, MQ7 A1, MQ9 A2, MQ135 A3.
- Chamber temperature / pressure sensor: BME280/BMP280 at `0x76` or `0x77`.
- ADS1115 supply remains a fixed **5.0 V design value**. The 3.3 V ESP32 I2C domain and 5 V ADS domain use the existing bidirectional I2C level-shifter arrangement.
- Future quantitative illuminance sensor: preferred `BH1750`, typically `0x23` or `0x5C`, sharing SDA/SCL with no extra GPIO. It is not marked installed until physically added.

### Light-measurement semantics

The installed three-pin LDR/LM393 module exposes only `DO/GND/VCC`, so it provides a comparator threshold state (dark/light relative to the adjustable threshold). Firmware must report `quantitative_lux=false` and `lux=null`; no conversion from this digital state to lux is permitted.

When a quantitative sensor such as BH1750 is installed, the physical measurement becomes **illuminance** in `lux`. `Lumen (lm)` is luminous flux and must not be reported as if HazRob measured it at the robot location.

## Always-on electrical loads without GPIO

- 5 V 40 x 40 mm chamber fan: always on with robot power.
- Four LEDs: always on with robot power.
- DC/DC converter: no GPIO.
- 4S 40 A BMS: protection/power path; no GPIO required for 0.10.

## Battery / Estimated Energy Model

The 0.10 design uses two complementary sources:

1. **Measured battery voltage** via GPIO33 and a divider (initial reference design: 47 kOhm top / 10 kOhm bottom for a 4S pack; final calibration belongs to the actual assembled unit).
2. **Estimated Energy Model (EEM)** in Android. Firmware supplies only compact state/basis telemetry; Android integrates configured current estimates for base electronics, fan, LEDs, ESP32-CAM state, MQ heater state and normalized motor activity.

EEM is explicitly an estimate, not coulomb counting. UI must identify it as estimated until a current sensor is added. Battery chemistry, nominal capacity and initial SOC are developer-configurable parameters.

Minimal firmware basis fields:

- `battery.v`, `battery.raw` plus quality/installation state as needed.
- `energy_basis.uptime_ms`.
- `energy_basis.drive_eq_ms` — equivalent full-power drive duration derived from left/right command magnitude.
- `energy_basis.mq_on_ms` cumulative.

Android combines these with configured load currents and voltage/SOC anchoring. EEM state persists locally so a UI/process restart does not silently reset the estimate.

## Temperature separation

Two temperature domains are defined, but only one is currently installed:

- **Chamber/environment temperature** from BME/BMP on I2C: scientific ChemCore covariate stored with each chemical session.
- **Internal robot/battery temperature**: future equipment-health telemetry on reserved GPIO14. Until a sensor is installed, firmware reports `installed=false` / `c=null`; the Android HUD must show unavailable rather than fabricate a value. It must never be substituted for chamber temperature.

## Proximity / fall-warning policy

- IR lower-front sensor: `fall_edge_warning`.
- Front ultrasonic sensor: `obstacle_distance_cm` and warning state.
- 0.10 initially treats both as operator warnings. Autonomous braking remains disabled until physical threshold and false-positive/false-negative behaviour are validated.

## Firmware work packages

### F0 — compact baseline and pin freeze
- Six-GPIO L298N drive with dedicated ENA/ENB PWM.
- Preserve API 4 compatibility while extending JSON additively.
- Update hardware profile ID.

### F1 — compact hardware telemetry
- Battery ADC driver on GPIO33.
- Ultrasonic driver on GPIO23/39 with bounded timeout and non-blocking cadence.
- GPIO14 reserved only; remove the provisional DS18B20 code until actual hardware is selected.
- LDR/LM393 remains threshold-only on GPIO34; no fake lux.
- Keep chamber BME/BMP + ADS direct-I2C compact drivers.
- Add EEM basis counters only; no battery-percentage model in firmware.

### F2 — firmware size / regression gate
- Compile DOIT ESP32 DEVKIT V1.
- Report actual binary bytes, Flash %, global RAM.
- Soft target <=70%; hard CI failure >80%.
- Remove unused/duplicate endpoints/helpers before accepting >70%.
- Verify 6-GPIO drive, relay, ADS, BME/BMP, IR, LDR threshold, microphone, battery, ultrasonic and reserved alert/temp pins compile together.

### F3 — direct/local compatibility
- `/data` and `/diag` expose additive `battery`, `internal_temp`, `proximity`, `energy_basis` and truthful light state.
- Existing `/drive`, `/heartbeat`, relay, measurement and fail-safe semantics remain unchanged.

### F4 — HiveMQ / remote compatibility
- No broker architecture change required.
- Existing MQTT TLS/LWT/session-seq-nonce-TTL command path remains.
- Remote telemetry carries the same normalized fields as direct `/data`.
- Verify payload remains bounded and app parser tolerates missing optional fields from older firmware.

### F5 — ESP32-CAM / Render
- No battery, temperature or ChemCore computation on ESP32-CAM.
- Camera keeps local stream + remote WSS publisher responsibilities only.
- Render relay remains video/bootstrap infrastructure; master telemetry remains on direct HTTP/HiveMQ.
- Verify 0.10 version/protocol compatibility after master firmware freeze.

## Android work packages

### A1 — protocol/profile
- Update `HazRobHardwareProfile` to the frozen 0.10 GPIO map.
- Extend RobotProtocolV4 normalization for battery, optional internal temperature, obstacle distance, EEM basis and light-state provenance.
- Keep chamber environmental data separate from device-health temperature.

### A2 — driving/video HUD
- Floating phone-style battery icon with estimated remaining percentage.
- Floating internal-temperature icon/value; show unavailable until a sensor is installed.
- Warning overlays for front obstacle and fall-edge IR states without obscuring video controls.
- Battery indicator must distinguish estimated SOC when no current sensor is present.

### A3 — Estimated Energy Model
- Developer-configurable battery chemistry, nominal capacity and estimated load-current table.
- Integrate base load + fan + four LEDs + camera state + MQ heater state + normalized drive load.
- Fuse/anchor estimate with measured pack voltage without presenting it as laboratory-grade SOC.
- Persist model state and provide explicit battery-change/full-charge reinitialization.

### A4 — physical measurements / telemetry / reports
- Store battery voltage, EEM SOC, optional internal temperature, proximity warning and LDR threshold state.
- Physical measurements may expose `Illuminance (lux)` only when a real quantitative lux sensor is installed.
- Remove/disable any implication that current LDR threshold equals lux or that HazRob directly measures luminous flux in lumens.
- Chemical reports use chamber T/P(/RH) only; future internal temperature appears under device health/provenance.

## Platform work packages

### C1 — HiveMQ
- Verify additive telemetry fields and current QoS0 + application ACK model.
- Retained LWT remains the authoritative abrupt-disconnect status.

### C2 — Render
- Keep video relay focused on JPEG WSS and bootstrap.
- Verify branch/deploy/version and service health after final 0.10 production deploy.

### C3 — direct/remote parity
- Android consumes the same normalized states regardless of Direct Wi-Fi or Remote MQTT transport.
- No hardware state may silently exist only in one transport mode.

## Release acceptance checklist

- Master ESP32 compiled Flash <=80% (target <=70%).
- ESP32-CAM CI green.
- Android APK CI green.
- Six-GPIO L298N drive tested on the physical base.
- Direct and remote telemetry contain matching normalized fields.
- Battery HUD works in both modes; internal-temperature HUD correctly handles `not installed`.
- LDR remains threshold-only until quantitative lux hardware is installed.
- Render 0.10 relay live and healthy.
- HiveMQ remote command/telemetry/LWT path verified.
- README and hardware profile documentation synchronized with actual wiring.

## Open physical parameters — do not invent

- Actual battery chemistry and nominal capacity for EEM calibration.
- Actual internal-temperature sensor model after purchase.
- Actual battery divider resistor values after assembly and ADC correction factor.
- Ultrasonic warning thresholds after physical testing.
- Quantitative lux sensor is not installed yet; BH1750 is the preferred future option.
- Speaker remains reserved on GPIO26 but not electrically installed until a safe interface is added.
