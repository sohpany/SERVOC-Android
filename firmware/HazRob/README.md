# HazRob ESP32 firmware — hardware-sync baseline

This is the four-tab Arduino project for the **DOIT ESP32 DEVKIT V1** master controller.

## Network

- SoftAP SSID: `HazRob`, open network.
- Master ESP32: `192.168.4.1/24`.
- ESP32-CAM: `192.168.4.2/24`.
- NVS namespace remains `servoc` so existing neutral/speed/camera/timing preferences are preserved.
- Wi-Fi sleep is disabled and the AP watchdog restores the expected AP/IP if needed.

## Required Arduino libraries

- ESP32Servo
- Adafruit ADS1X15
- Adafruit BME280 Library
- Adafruit BMP280 Library
- Adafruit Unified Sensor dependencies required by the Adafruit environment libraries

## I2C topology

ESP32 uses GPIO21=SDA and GPIO22=SCL. BME280/BMP280 is on the 3.3 V side. Two ADS1115 boards are on the 5 V side through a bidirectional I2C level shifter:

- ADS1115 #1: `0x48`
- ADS1115 #2: `0x49`

ADS1115 PGA is `GAIN_TWOTHIRDS` (±6.144 V full-scale). This does **not** permit an analog input above the ADS1115 supply rail; MQ AO must remain within the 0–5 V domain when the ADS boards are powered at 5 V.

## MQ banks

Only two relay channels currently power MQ heaters; relay 3/4 remain reserved.

- Relay 1 / GPIO25 / ADS1: MQ2=A0, MQ4=A1, MQ5=A2, MQ8=A3
- Relay 2 / GPIO26 / ADS2: MQ3=A0, MQ7=A1, MQ9=A2, MQ135=A3
- Relay 3 / GPIO27: reserved
- Relay 4 / GPIO32: reserved

The relay board is active LOW. `/relay?g=0..4` remains available, and `/relay?mask=0..15` adds multi-relay control. `/data` retains the legacy `relay` field when exactly one relay is active and adds `relayMask` for the real bit mask.

## Other sensors

- MAX4466 OUT: GPIO36 (ADC1). Firmware reports RMS voltage and peak-to-peak voltage only; it does not claim calibrated dB SPL.
- IR digital module: GPIO35. It is telemetry/warning only and does not stop or block drive commands.
- Environment sensor: BME280/BMP280 on I2C.

## Drive safety

GPIO18/GPIO19 drive the two continuous MG995 servos. Acceleration/deceleration/reversal ramps remain local. The Android app sends a 250 ms drive heartbeat. If the master receives no drive command for `FAILSAFE=1000 ms`, it stops first and can replay the last 60 s command history in reverse at a 40% speed cap. An explicit `/stop` clears history and suppresses retrace.

Retrace is dead-reckoning only. It has no wheel odometry, IMU, drop detector or automatic IR collision stop and therefore requires controlled field validation before unattended use.

## Analytical boundary

Firmware reports acquisition values (`raw`, `Vadc`, `Vao`, `Rs`, `Rs/R0`) and performs no gas identity/source-direction/PPM/exposure analysis. Gas-source direction remains in Android. Current `R0=10` values are placeholders, not device-specific calibration.

MQ7/MQ9 quantitative use still requires a validated heater-phase protocol. Simple relay 5 V ON/OFF does not establish that protocol, so these channels must remain fingerprint/relative evidence until heater control and calibration are validated.
