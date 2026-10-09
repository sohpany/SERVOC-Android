# HazRob main ESP32 firmware — 0.10.2 / HR069-R1 WORKING

## Latest verified Main ESP32 firmware + compatible Android APK — 2026-10-09

**WORKING / UNAPPROVED, not field-qualified.** The latest **matched** firmware and Android app were built from the identical source commit `6281d3eb7c1c961ce63b0b5e4ae0792874626720`, branch `release/hazrob-0.10.2-rc2`. The older vCode28 / firmware `e6a14bc` evidence has been superseded for testing; do not flash it by mistake.

| Component | Current latest diagnostic version | Download | File SHA-256 |
|---|---|---|---|
| **Main ESP32 firmware** | **HazRob 0.10.2 / HR069-R1**, API v4 | [source + compiled firmware ZIP / artifact 11590647043](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034/artifacts/11590647043) | `28474851e87ea431f5beb38f03ff0c016625960ac9dd129132128d53043f55d6` (inner ZIP) |
| **Android APK** | **HazRob 0.10.2 / HR069-R1**, `versionCode 30`, `com.servoc.app` | [signed test APK / artifact 11590871098](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815/artifacts/11590871098) | `87226e1749d632e2016332a2a204fec09705eb27ebf378f500a9f5a3b8886039` (APK) |
| **ESP32-CAM** | **0.10.2 / HR069**, unchanged | no reflashing for Main ESP32 updates | separate firmware |

GitHub Actions encloses artifacts in a ZIP. For Android, extract `HazRob-0.10.2-HR069-R1-RC2-WORKING-359-6281d3eb7c1c961ce63b0b5e4ae0792874626720.apk`. For ESP32, extract the Arduino source `HazRob/HazRob.ino` and all companion `.cpp`/`.h` files; compile/upload on ESP32 Dev Module with ESP32 Arduino core `3.3.11`, **Huge APP** partitioning. The signed APK should be installed over the existing application rather than uninstalling it first, if signatures match, to retain local records.

**Successful matching CI runs:** [firmware + Android source and host-replay](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034), [Android validation](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936746), [APK signed-build](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815). Real-device acceptance remains pending; previous production-release authority has **not** been replaced.

**Main firmware changes in this pair:** Echo HC-SR04 `TRIG=GPIO23`, `ECHO=GPIO39`, sampling interval 150 ms, timeout 25,000 µs; differentiated missing rising/falling edge, stuck-HIGH and invalid-pulse diagnostics, with invalid distances reported as invalid rather than old/stale distance. BME/BMP environmental sensor reads at nominal 1000 ms and reprobes/restarts its bus after repeated failures; **wired directly to ESP32 GPIO13 SDA / GPIO14 SCL** with no external voltage-level converter, I²C hub or ADS1115 in its signal path. Its dedicated native ESP32 hardware I²C is independent from the MQ ADS1115 GPIO21/22 bus. IR remains **HIGH=ground / LOW=cliff**; forward auto-stop is selectable by the operator and defaults OFF. Physical GPIO voltages, sensor operation and motion fail-safe tests must be validated on the robot.

[Latest pair manifest and tests in root README](../../README.md) · [hardware replay and test matrix](../../docs/0.10.2/HR069-R1_INTERNAL_HARDWARE_AND_DEVELOPER_TESTS_20261009.md).

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
