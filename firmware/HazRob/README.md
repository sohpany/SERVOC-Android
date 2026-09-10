# HazRob master ESP32 firmware — 0.10.1

Hardware target: **DOIT ESP32 DEVKIT V1 / ESP32-WROOM-compatible board** · API v4 · hardware profile `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1`.

Canonical GPIO authority: [`../../docs/0.10.1/GPIO_AUTHORITY.md`](../../docs/0.10.1/GPIO_AUTHORITY.md).

## Design boundary

The master firmware is intentionally compact and deterministic. It performs hardware I/O, acquisition, drive safety, local API and remote transport. **ChemCore molecular intelligence, open-set inference, QSPR/AMCI, Estimated Energy Model SOC and scientific-library data remain on Android.**

The 0.10.1 implementation uses compact direct-I2C ADS1115 and BME280/BMP280 drivers and therefore does not require the former Adafruit ADS/BME/BMP libraries or ESP32Servo.

## Arduino / partition setting

0.10.1 firmware is built for the board's **4 MB flash** with **Huge APP** partitioning. ESP32 firmware OTA is not used in this release; firmware is flashed locally. CI enforces an absolute binary-size ceiling so the larger partition is not treated as permission for uncontrolled growth.

**Manual Arduino IDE build for this release:** select **ESP32 Dev Module** as the board and select **Huge APP (3MB No OTA / 1MB SPIFFS, or the equivalent Huge APP entry provided by the installed ESP32 core)**. The physical board may still be a DOIT ESP32 DEVKIT V1; the generic ESP32 Dev Module menu is used because current Arduino-ESP32 DOIT definitions do not expose the required Huge APP partition option.

## Network

- SoftAP: `HazRob`.
- master: `192.168.4.1/24`.
- ESP32-CAM: `192.168.4.2/24`.
- Wi-Fi mode: AP+STA; direct control remains available while WAN is configured.
- NAPT is enabled only when the STA side has validated Internet connectivity.

## 4WD motion — L298N, six GPIO, no servo propulsion

| Function | GPIO |
|---|---:|
| ENA / left speed PWM | **16** |
| IN1 | **18** |
| IN2 | **27** |
| ENB / right speed PWM | **17** |
| IN3 | **19** |
| IN4 | **32** |

Two left motors are wired in parallel to OUT1/OUT2 and two right motors in parallel to OUT3/OUT4. **Remove the ENA and ENB enable jumpers** when those pins are driven from GPIO16/GPIO17. Direction is controlled only by IN1..IN4; PWM speed is applied on ENA/ENB. Firmware retains ramping, a 450 ms drive lease, STOP/E-STOP, 900 ms link-loss detection, a 3 s recovery hold and bounded retrace.

## MQ / ADS topology

One active-LOW relay on GPIO25 powers all eight MQ modules simultaneously. Valid power states are strictly `0=OFF` and `1=ON`.

| ADS1115 | Address | Mapping |
|---|---|---|
| ADS1 | `0x48` | MQ2 A0 · MQ4 A1 · MQ5 A2 · MQ8 A3 |
| ADS2 | `0x49` | MQ3 A0 · MQ7 A1 · MQ9 A2 · MQ135 A3 |

Both ADS1115 boards use the fixed regulated **5 V** HazRob rail. Acquisition is configured for ±6.144 V PGA full-scale, 860 SPS and four conversions per channel. Eight channels are scanned sequentially; telemetry includes `sample_us`, `scan_cycle`, `channel_offset_us` and `age_us`.

The firmware currently carries `RL=10` and `R0=10` engineering/default values. These are **not device calibration** and cannot authorize M4 quantitative interpretation. MQ7/MQ9 quantitative use additionally requires a validated heater-cycle protocol.

## Other I/O

| Function | GPIO | 0.10.1 status |
|---|---:|---|
| Speaker/alert signal | 26 | reserved; 8Ω speaker not installed; do not connect it directly |
| MAX4466 | 36 | electrical RMS/P2P only, not calibrated dB SPL |
| IR fall-edge warning | 35 | active LOW |
| LDR/LM393 DO | 34 | threshold state only; **not lux** |
| Battery divider ADC | 33 | ADC1; initial divider reference 47k/10k |
| Ultrasonic TRIG | 23 | front obstacle ranging |
| Ultrasonic ECHO | 39 | input-only; reduce a 5V echo to <=3.3V |
| Internal temperature | 14 | **reserved only**, no sensor/driver assumed |
| I2C SDA/SCL | 21 / 22 | ADS1 + ADS2 + chamber BME/BMP; 400 kHz |

The chamber environment sensor is BME280 (`0x60`) or BMP280 (`0x56/0x57/0x58`) auto-detected at `0x76/0x77` by the compact direct-I2C driver.

### Light measurement

The installed LDR/LM393 module has only DO/GND/VCC and therefore cannot produce quantitative illuminance. Firmware exposes threshold state, `quantitative_lux=false`, and `lux=null`.

For future quantitative physical measurement, the preferred option is a **BH1750** on the same I2C bus (`0x23` or `0x5C`), requiring no extra GPIO. That future measurement is **illuminance in lux**. HazRob must not label it as luminous flux in lumens.

## Battery / EEM basis

ESP32 supplies compact basis telemetry only:

- measured pack-voltage estimate from GPIO33;
- uptime;
- cumulative equivalent full-power drive time (`drive_eq_ms`);
- cumulative MQ-power time (`mq_on_ms`).

Android computes Estimated Energy Model SOC from those fields plus configured load assumptions for base electronics, fan, four LEDs, camera, MQ heaters and motor activity. It is explicitly an estimate, not coulomb counting.

## API v4

Local HTTP endpoints remain: `/drive`, `/stop`, `/motion/stop`, `/estop`, `/sensor/off`, `/heartbeat`, `/relay`, `/measure`, `/alert`, `/data`, `/diag`, `/cfg`, plus developer remote provisioning/status endpoints.

`/relay` accepts only 0 or 1. Direct and remote drive commands share the same heartbeat/failsafe semantics.

## Remote path

MQTT 3.1.1 over TLS obtains the **robot-scoped** MQTT identity at runtime from the Render bootstrap service over HTTPS. The master identifies itself as `X-HazRob-Client: esp32`, validates the returned role/robot ID, strips whitespace from the returned password, and uses the embedded trusted root CA in `RemoteCloudConfig.h`. Operator credentials and video-view/publish tokens are not part of the master ESP32 role-scoped response. `RemoteCloudSecrets.h` remains only an optional controlled fallback and is removed from release source packaging. Commands carry session/sequence/nonce/TTL and application ACK semantics. A retained Last Will publishes an unavailable state after an unexpected disconnect. The master also provides Internet routing to ESP32-CAM when WAN is ready.

The operator application does not request a HazRob remote password. That passwordless UX does not mean anonymous transport; internal service identities remain authenticated and role-scoped.

## Capacity policy

CI reports both percentage of the configured app partition and the absolute compiled binary size. 0.10.1 policy is:

- partition target `<=70%`, hard ceiling `<=80%`;
- master binary target `<=1,000,000 bytes`, hard ceiling `<=1,150,000 bytes`.

The absolute byte gate remains active even with Huge APP partitioning.

## Safety / scientific boundary

HazRob is a research prototype and is not Ex-certified/intrinsically safe. Firmware reports electrical sensor evidence; compound identity, quantitative validity, occupational limits and uncertainty are decided by the Android analytical layer under its evidence gates.
