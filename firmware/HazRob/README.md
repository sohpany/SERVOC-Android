# HazRob ESP32 firmware — 0.7.1 hardware integration

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
- Adafruit Unified Sensor dependencies required by the environment libraries

## I2C topology

ESP32 uses GPIO21=SDA and GPIO22=SCL. BME280/BMP280 is on the 3.3 V/LV side. The two ADS1115 boards are powered from the 5 V domain and share the bus through a **bidirectional I2C level shifter**:

- ADS1115 #1: `0x48`
- ADS1115 #2: `0x49`

ADS1115 PGA is `GAIN_TWOTHIRDS` (±6.144 V full-scale). This setting does **not** permit an analog input above the ADS1115 supply rail. MQ AO must remain within the 0–5 V domain when an ADS board is powered at 5 V.

## Four MQ relay rows

The 4-channel relay board is active LOW. Every relay switches +5 V to **two MQ modules only**; sensor grounds remain common and AO stays wired to the corresponding ADS1115 input.

| Row | Relay GPIO | ADS channel mapping |
| --- | --- | --- |
| R1 | GPIO25 | MQ4 → ADS1 A0, MQ5 → ADS1 A1 |
| R2 | GPIO26 | MQ7 → ADS1 A2, MQ9 → ADS1 A3 |
| R3 | GPIO27 | MQ2 → ADS2 A0, MQ8 → ADS2 A1 |
| R4 | GPIO32 | MQ3 → ADS2 A2, MQ135 → ADS2 A3 |

The temporary 5 V / 4 A supply budget is protected in software: **no more than two rows (four MQ sensors) may be powered at one time**. `/relay?mask=0..15` returns HTTP 409 if more than two bits are requested. `/relay?g=0..4` remains available for one-row manual compatibility.

A normal full MQ measurement uses two phases:

1. R1+R2 (`mask=3`) → warm-up → sample four channels.
2. R3+R4 (`mask=12`) → warm-up → sample the other four channels.

Gas-source tracking may choose another one- or two-row combination depending on the analyte fingerprint plan, but it cannot override the two-row power limit.

## Other sensors

- MAX4466 OUT: GPIO36 / ADC1. Firmware reports RMS and peak-to-peak voltage only; it does not claim calibrated dB SPL.
- IR digital module: GPIO35. Telemetry/warning only; it does not stop or block motion.
- Digital LDR/LM393 module: GPIO34. Threshold state only; it is not a lux meter.
- Environment sensor: BME280/BMP280 on I2C.
- GPIO33 and GPIO39 remain spare inputs in this baseline; GPIO23 remains available for a future peripheral such as HC-SR04 TRIG if required.

## Drive safety and retrace

GPIO18/GPIO19 drive the two continuous MG995 servos. Acceleration/deceleration/reversal ramps remain local. The Android app sends a 250 ms drive heartbeat. If the master receives no drive command for `FAILSAFE=1000 ms`, it stops first and can replay the last 60 s movement history in reverse at a 40% speed cap. An explicit `/stop` clears history and suppresses retrace.

Retrace is dead-reckoning only. There is no wheel odometry, IMU, drop detector or automatic IR collision interlock; validate it only in a controlled open test area before relying on it.

## Analytical boundary

Firmware reports acquisition values (`raw`, `Vadc`, `Vao`, `Rs`, `Rs/R0`) and performs no gas identity/source-direction/PPM/exposure analysis. Gas identity and direction remain in Android.

The current `R0=10` entries are placeholders, **not device-specific calibration**. The JSON explicitly reports `r0_calibrated=false`.

MQ7/MQ9 quantitative use still requires a validated heater-phase protocol. Simple relay 5 V ON/OFF does not establish the manufacturer heater cycle, so these channels remain relative/fingerprint evidence until dedicated heater control and calibration are validated.

## Power validation before field use

With the temporary 5 V / 4 A buck converter, verify the 5 V rail under the worst intended simultaneous load: two MG995 servos, two MQ rows, relay coils and electronics. Use star power distribution/common ground and do not route servo current through the ESP32 regulator. Brownout or a significant 5 V sag means a larger dedicated 5 V supply is required before continued testing.
