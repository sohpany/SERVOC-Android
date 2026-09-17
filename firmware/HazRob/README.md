# HazRob main ESP32 firmware — 0.10.1

Hardware target: DOIT ESP32 DEVKIT V1 / ESP32-WROOM-compatible board.

This directory is the canonical main-controller source for HazRob 0.10.1. The network contract is defined by the source in this directory and is not inherited from later experimental firmware.

## Network contract

- SoftAP SSID: `HazRob`
- Main ESP32: `192.168.4.1/24`
- ESP32-CAM: `192.168.4.2/24`
- Boot profile: DIRECT
- DIRECT: AP-only + local HTTP control/telemetry + local camera
- REMOTE: AP+STA, entered only by an explicit operator command
- Saved WAN credentials never auto-enable REMOTE after boot
- NAPT is enabled only after the WAN side passes the firmware Internet-readiness checks
- Wi-Fi sleep is disabled
- The AP health check verifies mode, IP address and the exact SSID `HazRob`

## 4WD drive mapping

Two motors on each physical side are driven in parallel through the L298N.

| Function | GPIO |
|---|---:|
| Logical LEFT PWM | 17 |
| LEFT IN1 | 19 |
| LEFT IN2 | 32 |
| Logical RIGHT PWM | 16 |
| RIGHT IN1 | 18 |
| RIGHT IN2 | 27 |

Direction inversion is disabled on both sides. Direct control is accepted through `/drive?l=&r=`. The firmware applies a 500 ms command timeout and stops motion when control commands cease.

## I2C and sensors

| Function | GPIO / address |
|---|---|
| ADS1115 bus SDA/SCL | 21 / 22 |
| ADS1 / ADS2 | `0x48` / `0x49` |
| Environmental bus SDA/SCL | 13 / 14 |
| MQ power relay | 25 |
| Alert logic output | 26 |
| MAX4466 | 36 / VP |
| IR cliff sensor | 35 |
| LDR digital output | 34 |
| Battery ADC | 33 |
| Ultrasonic TRIG | 23 |
| Ultrasonic ECHO | 39 / VN |

ADS acquisition is configured for 5 V ADS1115 supply, ±6.144 V full scale, 860 SPS and four conversions per channel.

MQ mapping:

- ADS1: MQ2, MQ4, MQ5, MQ8
- ADS2: MQ3, MQ7, MQ9, MQ135

The field clean-air R0 values carried by `Config.h` are device baselines. They are not reference-gas certification and do not by themselves authorize quantitative ppm interpretation.

## Local API

The main HTTP server runs on port 80. Core endpoints include `/`, `/drive`, `/stop`, `/motion/stop`, `/estop`, `/estop/reset`, `/heartbeat`, `/relay`, `/measure`, `/alert`, `/data`, `/diag`, `/cfg`, `/connection/direct`, `/connection/remote` and the remote provisioning/status endpoints.

## Remote transport

Remote MQTT uses TLS. The robot-scoped MQTT identity is obtained from the bootstrap service. The same HazRob AP remains the local authority while REMOTE is active in AP+STA mode.

## Build target

Recommended Arduino-ESP32 core: 3.3.11.

Manual board target: ESP32 Dev Module with Huge APP partitioning, matching the project build workflow.
