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
| I2C SDA | 21 | shared ADS1/ADS2 + chamber environment sensor |
| I2C SCL | 22 | shared ADS1/ADS2 + chamber environment sensor |
| MQ power relay | 25 | one active-low relay, all eight MQ |
| speaker / alert signal | 26 | reserved; 8-ohm speaker must not be driven directly without a suitable electrical interface |
| front ultrasonic TRIG | 23 | front obstacle ranging |
| front ultrasonic ECHO | 39 | input only; 5 V HC-SR04-style ECHO requires level reduction to <=3.3 V |
| battery voltage ADC | 33 | ADC1; divider required |
| IR fall-edge sensor | 35 | lower front edge |
| MAX4466 sound level | 36 | ADC1 |
| LDR/LM393 | 34 | digital threshold input |
| internal electronics/battery temperature | 14 | reserved one-wire digital temperature input; default implementation target is DS18B20 unless the installed sensor model is changed |

### I2C analytical bus

- ADS1115 #1: `0x48`: MQ2 A0, MQ4 A1, MQ5 A2, MQ8 A3.
- ADS1115 #2: `0x49`: MQ3 A0, MQ7 A1, MQ9 A2, MQ135 A3.
- Chamber temperature / pressure sensor: BME280/BMP280 at `0x76` or `0x77`.
- ADS1115 supply remains a fixed **5.0 V design value**. The 3.3 V ESP32 I2C domain and 5 V ADS domain use the existing bidirectional I2C level-shifter arrangement.

## Always-on electrical loads without GPIO

- 5 V 40 x 40 mm chamber fan: always on with robot power.
- Four LEDs: always on with robot power.
- DC/DC converter: no GPIO.
- BMS: protection/power path; no GPIO required for 0.10.

## Battery / Estimated Energy Model

The 0.10 design uses two complementary sources:

1. **Measured battery voltage** via GPIO33 and a divider (initial reference design: 47 kOhm top / 10 kOhm bottom for a 4S pack; final calibration belongs to the actual assembled unit).
2. **Estimated Energy Model (EEM)** in Android. Firmware supplies only compact state/basis telemetry; Android integrates configured current estimates for base electronics, fan, LEDs, ESP32-CAM state, MQ heater state and normalized motor activity.

EEM is explicitly an estimate, not coulomb counting. UI must identify it as estimated until a current sensor is added. Battery chemistry, nominal capacity and initial SOC are developer-configurable parameters.

Recommended minimal firmware basis fields:

- `battery.voltage_v`, `battery.raw_mv`/ADC quality flag.
- `energy.uptime_ms`.
- `energy.drive_eq_ms` (equivalent full-power drive duration derived from left/right command magnitude).
- `energy.mq_on_ms` cumulative.

Android combines these with configured load currents and voltage/SOC anchoring. EEM state persists locally so a temporary UI restart does not reset the estimate.

## Temperature separation

Two temperatures must never be conflated:

- **Chamber/environment temperature** from BME/BMP on I2C: scientific ChemCore covariate and stored with each chemical session.
- **Internal robot/battery temperature** on GPIO14: equipment-health telemetry only; shown in the driving/video UI and excluded from the ChemCore environmental vector.

## Proximity / fall-warning policy

- IR lower-front sensor: `fall_edge_warning`.
- Front ultrasonic sensor: `obstacle_distance_cm` and warning state.
- 0.10 initially treats both as operator warnings. Autonomous braking based on these sensors remains disabled until field thresholds and false-positive/false-negative behavior are physically validated.

## Firmware work packages

### F0 — compact baseline and pin freeze
- Six-GPIO L298N drive with dedicated ENA/ENB PWM.
- Preserve API 4 compatibility while extending JSON additively.
- Update hardware profile ID.

### F1 — compact hardware telemetry
- Add battery ADC driver on GPIO33.
- Add ultrasonic driver on GPIO23/39 with bounded timeout and non-blocking sampling cadence.
- Reserve/implement compact GPIO14 internal-temperature driver without heavy external libraries.
- Keep chamber BME/BMP + ADS direct-I2C compact drivers.
- Add EEM basis counters only; no battery-percentage model in firmware.

### F2 — firmware size / regression gate
- Compile DOIT ESP32 DEVKIT V1.
- Report actual binary bytes, Flash %, global RAM.
- Soft target <=70%; hard CI failure >80%.
- Remove unused/duplicate JSON/endpoints or heavy helpers before accepting >70%.
- Verify 6-GPIO drive, relay, ADS, BME/BMP, IR, LDR, microphone, battery, ultrasonic and alert pin compile together.

### F3 — direct/local compatibility
- `/data` and `/diag` expose additive `battery`, `internal_temp`, `proximity` and `energy_basis` records.
- Existing `/drive`, `/heartbeat`, relay, measurement and fail-safe semantics remain unchanged.

### F4 — HiveMQ / remote compatibility
- No broker architecture change required.
- Existing MQTT TLS/LWT/session-seq-nonce-TTL command path remains.
- Remote telemetry carries the same normalized fields as direct `/data`.
- Verify payload remains bounded and app parser tolerates missing optional fields from older firmware.

### F5 — ESP32-CAM / Render
- No battery or temperature computation on ESP32-CAM.
- Camera keeps local stream + remote WSS publisher responsibilities only.
- Render relay remains video/bootstrap infrastructure; no need to carry main ESP32 telemetry through Render.
- Verify 0.10 version/protocol compatibility after master firmware freeze.

## Android work packages

### A1 — protocol/profile
- Update `HazRobHardwareProfile` to the frozen 0.10 GPIO map.
- Extend RobotProtocolV4 normalization for battery, internal temperature, obstacle distance and EEM basis.
- Keep chamber environmental data separate from device-health temperature.

### A2 — driving/video HUD
- Floating phone-style battery icon with estimated remaining percentage.
- Floating internal-temperature icon/value.
- Add warning overlays for front obstacle and fall-edge IR states without obscuring video controls.
- Battery indicator must visually distinguish estimated SOC when no current sensor is present.

### A3 — Estimated Energy Model
- Developer-configurable battery chemistry, nominal capacity and estimated load-current table.
- Integrate base load + fan + four LEDs + camera state + MQ heater state + normalized drive load.
- Fuse/anchor estimate with measured pack voltage without presenting it as laboratory-grade SOC.
- Persist model state and reset/reinitialize on explicit battery-change/full-charge action.

### A4 — telemetry / reports
- Store battery voltage, EEM SOC, internal temperature and proximity warning telemetry.
- Chemical analysis reports use chamber T/P(/RH) only; internal temperature appears under device health/provenance, not analytical environment.

## Platform work packages

### C1 — HiveMQ
- Verify additive telemetry fields and current QoS0 + application ACK model.
- Retained LWT remains the authoritative abrupt-disconnect status.

### C2 — Render
- Keep video relay focused on JPEG WSS and bootstrap.
- Verify health/version after final 0.10 production deploy.

### C3 — direct/remote parity
- The Android UI and data model must consume the same normalized states regardless of Direct Wi-Fi or Remote MQTT transport.
- No feature may silently exist only in one transport mode.

## Release acceptance checklist

- Master ESP32 compiled Flash <=80% (target <=70%).
- ESP32-CAM CI green.
- Android APK CI green.
- Six-GPIO L298N drive tested on the physical base.
- Direct and remote telemetry contain matching normalized fields.
- Battery and internal temperature HUD work in both modes.
- Render 0.10 relay live and healthy.
- HiveMQ remote command/telemetry/LWT path verified.
- README and hardware profile documentation synchronized with the actual wiring.

## Open physical parameters (do not invent)

- Actual battery chemistry and nominal capacity for EEM calibration.
- Exact installed internal-temperature sensor model if different from DS18B20.
- Actual battery divider resistor values after assembly and ADC correction factor.
- Ultrasonic warning thresholds after physical testing.
- Speaker remains reserved on GPIO26 but not electrically installed until a safe interface is added.
