# HazRob master ESP32 firmware — 0.9

Target: **DOIT ESP32 DEVKIT V1** master controller.

## Local network

- SoftAP SSID: `HazRob`.
- Master ESP32: `192.168.4.1/24`.
- ESP32-CAM: `192.168.4.2/24`.
- NVS namespace remains `servoc`.
- Wi-Fi mode is AP+STA. Direct/local control remains available through the AP while WAN is configured.
- When STA has validated Internet connectivity, the master enables NAPT so HazRob AP clients, including ESP32-CAM, can reach the Internet through the modem.

## Required Arduino libraries

- ESP32Servo
- Adafruit ADS1X15
- Adafruit BME280 Library
- Adafruit BMP280 Library
- Adafruit Unified Sensor dependencies required by the environment libraries

## I2C topology

ESP32 uses GPIO21=SDA and GPIO22=SCL. BME280/BMP280 is on the 3.3 V/LV side. ADS1115 boards are powered from the 5 V domain and share the bus through a bidirectional I2C level shifter.

- ADS1115 #1: `0x48`
- ADS1115 #2: `0x49`

## Authoritative MQ topology: one relay / eight sensors

HazRob 0.9 uses **one active-LOW relay on GPIO25** to power all eight installed MQ modules/heaters simultaneously.

| MQ Power | GPIO | ADS1115 | Channels |
| --- | --- | --- | --- |
| One shared relay | GPIO25 | ADS1 `0x48` | MQ2 → A0, MQ4 → A1, MQ5 → A2, MQ8 → A3 |
| Same shared relay | GPIO25 | ADS2 `0x49` | MQ3 → A0, MQ7 → A1, MQ9 → A2, MQ135 → A3 |

GPIO26, GPIO27 and GPIO32 are **not MQ relay channels** in this hardware profile.

Valid MQ power state is strictly:

- `0` = all eight MQ OFF
- `1` = all eight MQ ON

There are no R1/R2 groups and no multi-relay mask in the authoritative `hazrob-0.9-1r8mq-v1` profile.

## Other installed interfaces

- Left continuous MG995: GPIO18.
- Right continuous MG995: GPIO19.
- MAX4466 microphone: GPIO36. Reported sound values are electrical RMS / peak-to-peak, not calibrated dB SPL.
- IR digital sensor: GPIO35, active LOW, warning telemetry only.
- GPIO33 / GPIO34 / GPIO39 remain auxiliary inputs.
- Environment sensor: BME280/BMP280 on I2C when detected.

## Direct API4 behavior

Local HTTP API remains available on the master:

- `/drive`
- `/motion/stop`
- `/estop`
- `/sensor/off`
- `/heartbeat`
- `/relay?mask=0|1`
- `/measure`
- `/data`
- `/diag`
- `/cfg`

`/relay` rejects any value other than exactly `0` or `1`.

Motion stop and MQ power control remain independent. Backgrounding the Android app stops motion; it does not automatically switch off an active MQ warm-up session.

## Remote 4G path

1. The master keeps the HazRob AP alive and joins the modem network as STA.
2. WAN readiness validates Wi-Fi/IP, DNS and usable time before TLS.
3. Secure MQTT uses a trusted root CA; firmware does not use `setInsecure()`.
4. Commands carry session ID, monotonically increasing sequence, TTL and nonce.
5. ACK, telemetry and status are published over MQTT.
6. MQ relay commands accept only `0` or `1` remotely as well as locally.
7. The master can route a remote camera-flash command to ESP32-CAM over the HazRob LAN.

`RemoteCloudConfig.h` is credential-free in the repository. Broker credentials and CA belong only in the protected flashing copy/build process.

## ESP32-CAM Internet route

ESP32-CAM remains at `192.168.4.2`. When WAN is ready, the master NAPT path provides outbound Internet while local MJPEG remains independently available.

## Analytical boundary

Firmware acquires and reports sensor values. Chemical identity, evidence fusion, PPM models, exposure interpretation, time-series analysis and source-direction logic remain in the Android application.

The `R0=10.0` values in `Config.h` are placeholders, not device-specific calibration.

MQ7/MQ9 quantitative use still requires a validated heater-phase/calibration protocol. A shared 5 V relay path does not by itself establish the manufacturer heater cycle.

## Power validation

Because all eight MQ modules now power together, validate the actual 5 V rail under the simultaneous load of all MQ heaters plus relay coil, ESP32, ADS boards, ESP32-CAM and the servo power domain. Use proper distribution wiring and common ground; do not route MQ or servo current through the ESP32 regulator or thin logic jumpers.
