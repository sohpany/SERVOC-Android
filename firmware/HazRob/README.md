# HazRob master ESP32 firmware — 0.10.0

Target: **DOIT ESP32 DEVKIT V1** · API v4 · hardware profile `hazrob-0.9.3-1r8mq-alert-ldr-l298n-v1`.

## Design boundary

The master firmware is intentionally compact and deterministic. It performs hardware I/O, acquisition, drive safety, local API and remote transport. **ChemCore molecular intelligence, open-set inference, QSPR/AMCI and scientific-library data remain on Android.**

The 0.10 implementation uses compact direct-I2C ADS1115 and BME280/BMP280 drivers and therefore does **not** require the former Adafruit ADS/BME/BMP libraries or ESP32Servo.

## Network

- SoftAP: `HazRob`.
- master: `192.168.4.1/24`.
- ESP32-CAM: `192.168.4.2/24`.
- Wi-Fi mode: AP+STA; direct control remains available while WAN is configured.
- NAPT is enabled only when the STA side has validated Internet connectivity.

## 4WD motion — L298N, no servo propulsion

| Side | GPIO | Motors |
|---|---|---|
| Left | 18 / 27 | two motors in parallel |
| Right | 19 / 32 | two motors in parallel |

ENA/ENB remain enabled on the L298N; PWM is applied on the direction inputs. Firmware retains ramping, a 450 ms drive lease, STOP/E-STOP, 900 ms link-loss detection, a 3 s recovery hold and bounded retrace. If final physical polarity differs, use the side inversion flag rather than remapping the API.

## MQ / ADS topology

One active-LOW relay on GPIO25 powers all eight MQ modules simultaneously. Valid power states are strictly `0=OFF` and `1=ON`.

| ADS1115 | Address | Mapping |
|---|---|---|
| ADS1 | `0x48` | MQ2 A0 · MQ4 A1 · MQ5 A2 · MQ8 A3 |
| ADS2 | `0x49` | MQ3 A0 · MQ7 A1 · MQ9 A2 · MQ135 A3 |

Both ADS1115 boards use the fixed regulated **5 V** HazRob rail. Acquisition is configured for ±6.144 V PGA full-scale, 860 SPS and four conversions per channel. Eight channels are scanned sequentially; telemetry includes `sample_us`, `scan_cycle`, `channel_offset_us` and `age_us`.

The firmware currently carries `RL=10` and `R0=10` engineering/default values. These are **not device calibration** and cannot authorize M4 quantitative interpretation. MQ7/MQ9 quantitative use additionally requires a validated heater-cycle protocol.

## Other I/O

- GPIO26: alert logic/PWM output; low-impedance speaker requires an external driver.
- GPIO36: MAX4466 electrical RMS/P2P channel, not calibrated dB SPL.
- GPIO35: IR digital warning, active LOW.
- GPIO34: LDR/LM393 digital threshold, not quantitative lux.
- GPIO21/22: I2C SDA/SCL.
- environment sensor: BME280 (`0x60`) or BMP280 (`0x56/0x57/0x58`) auto-detected at `0x76/0x77` by the compact direct-I2C driver.

## API v4

Local HTTP endpoints remain: `/drive`, `/stop`, `/motion/stop`, `/estop`, `/sensor/off`, `/heartbeat`, `/relay`, `/measure`, `/alert`, `/data`, `/diag`, `/cfg`, plus developer remote provisioning/status endpoints.

`/relay` accepts only 0 or 1. Direct and remote drive commands share the same heartbeat/failsafe semantics.

## Remote path

MQTT 3.1.1 over TLS uses provisioned credentials/CA outside source control. Commands carry session/sequence/nonce/TTL and application ACK semantics. A retained Last Will publishes an unavailable state after an unexpected disconnect. The master also provides Internet routing to ESP32-CAM when WAN is ready.

## Capacity policy

GitHub Actions compiles the exact source and records actual Flash/RAM use. Release CI rejects a master or camera image above the configured Flash budget. The metric is **compiled binary size**; source comments or shorter variable names are not counted as capacity savings.

## Safety / scientific boundary

HazRob is a research prototype and is not Ex-certified/intrinsically safe. Firmware reports electrical sensor evidence; compound identity, quantitative validity, occupational limits and uncertainty are decided by the Android analytical layer under its evidence gates.
