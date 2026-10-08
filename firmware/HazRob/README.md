# HazRob main ESP32 firmware — 0.10.2 / HR069-R1 WORKING

## Latest documented Main ESP32 source / matched Android app (2026-10-08)

**LATEST DELIVERED WORKING Arduino sketch:** HazRob **0.10.2 / HR069-R1**, from Git commit `e6a14bc570c335b824c153c7cd7fd2677b9bd005`. The Arduino source ZIP `HazRob-0.10.2-HR069-R1-ESP32-Arduino.zip` has SHA-256 `6532c3495de23b1d4917ad64882519d3cc9c0cb42fd66634dc92059ba560058e`.

**LATEST DELIVERED WORKING Android app:** HazRob **0.10.2 / HR069-R1**, **versionCode 28**, APK source commit `8b89e4c27fa7fda4cc25b294f9a973a2a1a2a60a`, APK SHA-256 `022ba04594cd7b7735e2fd67a37586ee4b9e235eaf5e7e799e170b1e42d19618`. Embedded ChemCore Stable: `1.0.0-hr069r1`; locally editable R0 remains on Android. ESP32-CAM is still 0.10.2/HR069.

**Verified compatibility of the source revisions:** `firmware/HazRob/` is the identical Git tree `12c8b5fe3c903d320b12e19a9c58d9a94c8cf2f0` in the two delivery commits and documentation baseline. The firmware source did not change between the Arduino ZIP and v28 APK deliveries. Both are the **latest delivered diagnostic versions**; neither has passed real-robot physical validation or been promoted to an approved field/production release.

**Build evidence:** [Arduino compiler and ZIP](https://github.com/sohpany/SERVOC-Android/actions/runs/37852367741) · [v28 APK](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726140). All ongoing engineering continues on **`release/hazrob-0.10.2-rc2` only**; do not flash HR068 or earlier firmware in place of this source.

---

Hardware target: DOIT ESP32 DEVKIT V1 / ESP32-WROOM-compatible board.

This directory contains the Main ESP32 firmware in the current 0.10.2 / HR069-R1 working branch `release/hazrob-0.10.2-rc2`. This is *not* an approved new production firmware until bench/physical acceptance. Pair with Android 0.10.2 / HR069-R1 and API4. ESP32-CAM firmware is a separate module.

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

Manual board target: **ESP32 Dev Module** with **Huge APP** partitioning, matching the current RC2 build workflow. Open `HazRob/HazRob.ino` from the Arduino sketch folder, install ESP32 board core **3.3.11**, choose your real serial port, verify/compile, then upload. There is no .bin flashing prerequisite for this ZIP. Take the robot wheels off the ground for initial tests. Verify the 3.3V logic-level echo input before power-on.

## Engineering revision / physical safety

- Firmware banner stays `HazRob 0.10.2`; diagnostic `/diag.build_revision` identifies `HR069-R1`.
- IR polarity: **HIGH = ground present; LOW = no ground**. The user will adjust the IR sensor manually. The software no longer requires a prior HIGH/LOW transition; a stuck HIGH can therefore falsely indicate ground. Do not rely on it before testing at floor height and raised height.
- Operator-selectable forward auto-stop is retained in nonvolatile ESP32 storage, default OFF before first use. Emergency stop and the control-command watchdog remain independent.
- HC-SR04 TRIG23 and ECHO39: shift any 5V echo output safely to 3.3V at the input pin; never connect 5V directly to GPIO39.
- Nominal ultrasonic telemetry/Direct/Remote refresh is 150ms. Actual rates depend on MQTT/network congestion and valid echoes.

## R0 authority

Device R0 for ChemCore is editable **on the Android application** in Developer → R0 calibration. Eight individual overrides or a single eight-channel batch are persisted locally; the fallback field clean-air baseline is preserved. This changes new normalized measurements via `Rs/R0`; it does not program new R0 values into ESP32, does not replace reference-gas ppm curve calibration, and does not overwrite the independently versioned ChemCore Stable library.

**Never flash firmware from any HR050/HR067/HR068 working tree as a substitute for the HR069-R1 sketch.**
