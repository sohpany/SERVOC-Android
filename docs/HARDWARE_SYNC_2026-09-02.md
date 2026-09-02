# HazRob hardware synchronization audit — 2026-09-02

Target baseline: `dev/hazrob-0.6` at `9a20831d0ff6a8a5ef49b5fd58c344fd5ed7ecbd`.

## Repository comparison

| Area | Previous repository state | Hardware-sync state |
|---|---|---|
| Master AP | `Servoc` | `HazRob`, 192.168.4.1 |
| Camera | Address stored only; no camera sketch in repository | New standalone `firmware/HazRob-CAM`, station 192.168.4.2, port 80 control + port 81 MJPEG |
| MQ count | 6 channels | 8 channels: MQ2/3/4/5/7/8/9/135 |
| MQ ADC | Direct ESP32 ADC + divider field | 2× ADS1115 over I2C, addresses 0x48/0x49 |
| MQ power | 3 mutually exclusive relay groups | 2 MQ banks, 4 sensors per bank; relay 3/4 reserved |
| Relay API | `/relay?g=0..3` | backward-compatible `g=0..4` plus `/relay?mask=0..15` |
| I2C | BME/BMP only | BME/BMP + two ADS1115, SDA21/SCL22, external 5↔3.3 V I2C level shifter |
| Sound | absent | MAX4466 on GPIO36, RMS/P2P voltage telemetry |
| IR | absent | GPIO35 warning telemetry only; no motor interlock |
| Drive loss | stop/failsafe only | stop first + reverse replay of up to 60 s history after missing drive heartbeat |
| Gas-source tracking | Android estimator exists | remains Android-only; no direction algorithm added to ESP32 |
| Android MJPEG | byte-by-byte read + decode on stream worker | buffered 8 KiB read + separate latest-frame decode worker |

## Android compatibility changes included

`RobotProtocolV3` now treats the new fields as additive API-v3 extensions. The legacy `relay` field is retained by firmware when exactly one relay is active, while `relayMask` is authoritative for multi-bank states. This avoids forcing a protocol-version break for the installed v0.6.2 application model.

`RobotApi` exposes `relayMask(0..15)` for a later tracking-power policy. The current gas-direction algorithm remains in `GasDirectionEstimator`; no duplicate localization logic was added to firmware.

## Required Android changes not yet enabled automatically

1. **IR overlay:** protocol data now contains `ir.warning`, but the control UI should add a visible warning badge/overlay. It must not automatically stop motion because the current requirement is warning-only.
2. **Tracking bank selection:** when a chemical is selected, Android can map `MqSensorAffinity.mask` to MQ bank 1/2 and request `/relay?mask=`. This should not be automatic until the 5 V rail is load-tested, because enabling both banks means eight MQ heaters while both MG995 servos can also draw current.
3. **Sound display:** MAX4466 values are volts, not calibrated dB SPL. The PhysicalCatalog `Noise` field must remain unpopulated until microphone gain/reference calibration is established.

## Electrical/measurement validation gates

- Bench-test the temporary 5 V / 4 A supply under worst expected servo transients and selected MQ-bank combinations. Do not infer adequate transient margin from nominal current alone.
- ADS1115 #1 ADDR=GND (`0x48`), ADS1115 #2 ADDR=VDD (`0x49`). Both 5 V-side SDA/SCL lines pass through one bidirectional I2C level shifter; BME/BMP stays on the ESP32 3.3 V side.
- `GAIN_TWOTHIRDS` gives ±6.144 V ADC full-scale but does not override the ADS1115 input absolute maximum; MQ AO must not exceed the ADS supply rail.
- Verify IR polarity on the physical module. Current default is active LOW.
- `R0=10` is a placeholder. Device-specific clean-air/reference-gas calibration remains mandatory before quantitative claims.
- MQ7/MQ9 need a validated heater cycle for quantitative use; simple 5 V relay ON/OFF is not that cycle.

## Retrace validation gates

The reverse-history feature reconstructs the command path, not measured pose. It has no odometry, IMU, cliff detector or automatic IR collision response. Before field use, test straight motion, arcs, in-place turns, mixed commands, command dropout, reconnection during retrace, emergency stop, brownout and obstacles in a controlled area. An explicit `/stop` clears the history and suppresses retrace.

## ESP32-CAM validation gates

The repository previously had no camera source. The new sketch implements the agreed network contract and QVGA RGB565 → software-JPEG pipeline. The exact GC2145 windowing-register optimization is intentionally not hard-coded because it is driver/core-revision-sensitive. Validate `/status` PID first; PID `0x2145` confirms GC2145. Measure actual source FPS after flashing before tuning Android further.

## Build/release policy

This synchronization is isolated from the stable `dev/hazrob-0.6` branch. Android versionCode/versionName are intentionally unchanged; this is an integration/validation branch, not a new installable release. After CI passes and the physical Arduino/ESP-CAM builds are validated, merge or cherry-pick into the next HazRob application release and bump versionCode for an upgrade-safe APK.
