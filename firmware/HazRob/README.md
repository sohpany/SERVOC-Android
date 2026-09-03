# HazRob master ESP32 firmware — 0.9

Target: **DOIT ESP32 DEVKIT V1** master controller.

## Local network

- SoftAP SSID: `HazRob`.
- Master ESP32: `192.168.4.1/24`.
- ESP32-CAM: `192.168.4.2/24`.
- NVS namespace for existing controller settings remains `servoc`.
- Wi-Fi mode is AP+STA in 0.9. Direct/local control remains available through the AP even when WAN is configured.
- When the STA side has validated Internet connectivity, the master enables NAPT so clients on the HazRob AP, including the ESP32-CAM, can reach the Internet through the modem. NAPT is disabled again when WAN is not ready.

## Required Arduino libraries

- ESP32Servo
- Adafruit ADS1X15
- Adafruit BME280 Library
- Adafruit BMP280 Library
- Adafruit Unified Sensor dependencies required by the environment libraries

## I2C topology

ESP32 uses GPIO21=SDA and GPIO22=SCL. BME280/BMP280 is on the 3.3 V/LV side. The ADS1115 boards are powered from the 5 V domain and share the bus through a bidirectional I2C level shifter.

- ADS1115 #1: `0x48`
- ADS1115 #2: `0x49`

ADS1115 PGA is configured by firmware for the installed acquisition path. An ADS analog input must still remain within its actual supply rails; the PGA setting never permits an input above VDD.

## Authoritative MQ topology: two groups / eight sensors

The 4-channel relay board is active LOW. Only R1 and R2 are installed MQ power groups in HazRob 0.9. R3/R4 are reserved.

| MQ power group | Relay | ADS1115 | Channels |
| --- | --- | --- | --- |
| R1 | GPIO25 | ADS1 `0x48` | MQ2 → A0, MQ4 → A1, MQ5 → A2, MQ8 → A3 |
| R2 | GPIO26 | ADS2 `0x49` | MQ3 → A0, MQ7 → A1, MQ9 → A2, MQ135 → A3 |
| R3 | GPIO27 | reserved | not an installed MQ group |
| R4 | GPIO32 | reserved | not an installed MQ group |

All MQ module grounds remain on the common ground. The relay groups switch the positive heater/module supply. AO remains independently connected to its assigned ADS1115 channel.

Valid installed MQ relay mask is `0..3` only:

- `0` = both groups off
- `1` = R1
- `2` = R2
- `3` = R1 + R2

The firmware may power both installed groups concurrently; whether that is acceptable in the field depends on the measured capability of the actual 5 V supply and wiring.

## Other installed interfaces

- Left continuous MG995: GPIO18.
- Right continuous MG995: GPIO19.
- MAX4466 analog microphone: GPIO36. Reported sound values are electrical RMS / peak-to-peak measurements, not calibrated dB SPL.
- IR digital sensor: GPIO35, active LOW, currently used as warning telemetry rather than a certified collision-safety device.
- GPIO33 / GPIO34 / GPIO39 remain auxiliary inputs in the current profile.
- Environment sensor: BME280/BMP280 on I2C when installed/detected.

## Direct API4 safety behavior

Local HTTP API remains available on the master:

- `/drive`
- `/motion/stop`
- `/estop`
- `/sensor/off`
- `/heartbeat`
- `/relay`
- `/measure`
- `/data`
- `/diag`
- `/cfg`

Motion stop and MQ power control are intentionally independent in 0.9. Backgrounding the Android app stops motion; it does not automatically switch off an active MQ warm-up session.

## Remote 4G path

0.9 adds an optional outbound-only WAN path while preserving Direct mode:

1. The master keeps the HazRob AP alive and joins the modem network as STA.
2. WAN readiness checks Wi-Fi/IP, DNS and usable time before TLS is considered ready.
3. Secure MQTT uses a trusted root CA. The firmware does not use `setInsecure()`.
4. Commands carry a session ID, monotonically increasing sequence, TTL and nonce.
5. ACK, telemetry and status are published over MQTT.
6. Motion commands are not retained by the application transport.
7. The master can route a remote camera-flash command to ESP32-CAM over the private HazRob LAN.

`RemoteCloudConfig.h` is deliberately a credential-free template in the repository. Broker host, robot ID, username/password and CA must be provisioned in the private flashing copy or another protected build process. Do not commit production secrets.

## ESP32-CAM Internet route

The ESP32-CAM remains at `192.168.4.2` on the HazRob LAN. When WAN is ready, the master NAPT path gives it outbound Internet access without changing the local address. This is used by the optional WSS JPEG publisher while local MJPEG remains available independently.

## Analytical boundary

Firmware acquires and reports sensor values. Chemical identity, evidence fusion, PPM models, exposure interpretation, time-series analysis and source-direction logic remain in the Android application.

The `R0=10.0` values in `Config.h` are placeholders rather than device-specific clean-air calibration. Device calibration must not be inferred from those defaults.

MQ7/MQ9 quantitative use requires a validated heater-phase/calibration protocol. A simple relay-powered 5 V path alone does not establish the manufacturer heater cycle; until that path is validated, those channels should be treated as relative/fingerprint evidence rather than automatically trusted quantitative measurements.

## Power validation

Do not infer electrical adequacy from the software limit alone. Validate the actual 5 V rail under the intended simultaneous load, including both MG995 servos, the selected MQ groups, relay coils, ESP32, ADS boards and ESP32-CAM. Use common ground with proper star/distribution wiring and do not route servo/MQ heater current through the ESP32 regulator or thin logic jumpers. Significant rail sag or brownout requires correction before field operation.
