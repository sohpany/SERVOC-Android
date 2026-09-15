# HazRob 0.10.1 — Canonical GPIO Authority

Status: **HR-0101-050 SOURCE AUTHORITY — SOFTWARE/BUILD VALIDATION PENDING; PHYSICAL ACCEPTANCE PENDING**  
Change control: `HR-0101-010`, `HR-0101-019`, `HR-0101-020`, steering remediation `HR-0101-048`, field-operability hardening `HR-0101-050`.  
Primary code sources: `firmware/HazRob/Config.h`, `firmware/HazRob/Hardware.cpp`, `firmware/HazRob/Alerts.cpp`, `firmware/HazRob-CAM/HazRob-CAM.ino`.

This is the human-readable GPIO authority for the current HazRob 0.10.1 source. HR048 keeps the canonical differential-drive mixer and maps logical left to L298N channel B and logical right to channel A. HR050 does not change that drive mapping. HR050 separates the environmental BME280/BMP280 onto the ESP32 second hardware I2C controller and redefines the lower-front IR channel as a downward ground-presence / cliff-warning sensor. Physical acceptance remains mandatory after flashing.

## 1. Main ESP32 — DOIT ESP32 DEVKIT V1 / ESP32-WROOM profile

Hardware profile: `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2`

| GPIO | Direction / capability | HazRob function | External connection | Electrical / integration rule |
|---:|---|---|---|---|
| 17 | Digital output + PWM | `DRIVE_L_EN` | L298N ENB | Logical left-side speed PWM. Remove the ENB jumper when ESP32 controls ENB. |
| 19 | Digital output | `DRIVE_L_IN1` | L298N IN3 | Logical left direction. |
| 32 | Digital output | `DRIVE_L_IN2` | L298N IN4 | Logical left direction. |
| 16 | Digital output + PWM | `DRIVE_R_EN` | L298N ENA | Logical right-side speed PWM. Remove the ENA jumper when ESP32 controls ENA. |
| 18 | Digital output | `DRIVE_R_IN1` | L298N IN1 | Logical right direction. |
| 27 | Digital output | `DRIVE_R_IN2` | L298N IN2 | Logical right direction. |
| 21 | I2C bidirectional | ADS `SDA` | ADS1115 #1/#2 only | Analytical I2C, 400 kHz; ESP32 logic domain 3.3 V. |
| 22 | I2C bidirectional | ADS `SCL` | ADS1115 #1/#2 only | Analytical I2C, 400 kHz; ESP32 logic domain 3.3 V. |
| 13 | I2C bidirectional | `ENV_SDA` | BME280/BMP280 SDA | Dedicated **ENV I2C** bus, 100 kHz, 3.3 V logic domain. |
| 14 | I2C bidirectional | `ENV_SCL` | BME280/BMP280 SCL | Dedicated **ENV I2C** bus, 100 kHz. No longer reserved for an internal-temperature sensor. |
| 25 | Digital output | `RELAY_MQ` | MQ power relay input | One relay controls all eight MQ modules. Active LOW: LOW=ON, HIGH=OFF. |
| 26 | Digital output | `ALERT_PIN` | Safe external alert-driver signal only | `ALERT_OUTPUT_INSTALLED=false` until a safe driver is physically confirmed. Never drive an ~8-ohm speaker directly from ESP32 GPIO. |
| 36 | **Input only**, ADC1 | `SOUND_PIN` | MAX4466 analog OUT | Analog signal. Do not route through a bidirectional I2C level shifter. |
| 35 | **Input only** | `IR_PIN` | Downward lower-front IR module output | Ground-presence / **cliff** input. Candidate ground-detected polarity is LOW and must be field-confirmed. GPIO input must remain <=3.3 V. |
| 34 | **Input only** | `LDR_PIN` | LM393 LDR digital OUT | Threshold state only, not quantitative lux. Input <=3.3 V. |
| 33 | ADC1 input | `BATTERY_PIN` | Battery divider output | Battery never connects directly to GPIO. Reference divider 47 kΩ / 10 kΩ; final calibration required. |
| 23 | Digital output | `ULTRA_TRIG_PIN` | Front ultrasonic TRIG | 3.3 V trigger. |
| 39 | **Input only** | `ULTRA_ECHO_PIN` | Front ultrasonic ECHO | Typical 5 V ECHO must be reduced to <=3.3 V with an appropriate divider/interface. |

### Canonical machine-readable aliases — HR050

`DRIVE_L_EN=GPIO17; DRIVE_L_IN1=GPIO19; DRIVE_L_IN2=GPIO32; DRIVE_R_EN=GPIO16; DRIVE_R_IN1=GPIO18; DRIVE_R_IN2=GPIO27; SDA=GPIO21; SCL=GPIO22; ENV_SDA=GPIO13; ENV_SCL=GPIO14; RELAY_MQ=GPIO25; ALERT_PIN=GPIO26; SOUND_PIN=GPIO36; IR_PIN=GPIO35; LDR_PIN=GPIO34; BATTERY_PIN=GPIO33; ULTRA_TRIG_PIN=GPIO23; ULTRA_ECHO_PIN=GPIO39.`

Legacy audit-readable physical L298N aliases, which are physical channel names rather than logical side names: `ENA=GPIO16; ENB=GPIO17; IN1=18; IN2=27; IN3=19; IN4=32.`

## 2. L298N 4WD topology and steering semantics

- Physical channel A remains `ENA=GPIO16`, `IN1=GPIO18`, `IN2=GPIO27`, outputs OUT1/OUT2.
- Physical channel B remains `ENB=GPIO17`, `IN3=GPIO19`, `IN4=GPIO32`, outputs OUT3/OUT4.
- Controlled association remains **LEFT → channel B** and **RIGHT → channel A**.
- Joystick convention remains `+Y=forward`, `+X=right`; mixer remains `Left=Y+X`, `Right=Y-X` with normalization to ±100.
- Forward-right requires logical Left faster than logical Right; forward-left requires logical Right faster than logical Left.
- Pivot-right requires `(Left>0, Right<0)` and pivot-left `(Left<0, Right>0)`.
- ESP32 motor-control wiring remains six GPIO lines: 16, 18, 27, 17, 19, 32.
- **Remove the ENA jumper** when GPIO16 supplies PWM to ENA.
- **Remove the ENB jumper** when GPIO17 supplies PWM to ENB.
- ESP32, L298N logic, sensors and interfaces require common signal ground.
- Never back-feed the ESP32 through the L298N logic rail.

## 3. I2C topology — separated under HR050

### 3.1 Analytical I2C

Bus: `SDA GPIO21`, `SCL GPIO22`, 400 kHz.

| Device | Address | Channels / purpose |
|---|---:|---|
| ADS1115 #1 | `0x48` | A0 MQ2; A1 MQ4; A2 MQ5; A3 MQ8 |
| ADS1115 #2 | `0x49` | A0 MQ3; A1 MQ7; A2 MQ9; A3 MQ135 |

Software acquisition remains ADS1115 PGA ±6.144 V, 860 SPS and four conversions averaged per channel. The software contract still assumes ADS1115 VDD=5 V; physical acceptance must measure the assembled ADS supply before treating that assumption as hardware fact. A 3.3 V SDA/SCL idle level does not by itself establish ADS VDD.

### 3.2 ENV I2C

Bus: `ENV_SDA GPIO13`, `ENV_SCL GPIO14`, 100 kHz, second ESP32 hardware I2C controller (`TwoWire(1)`).

| Device | Address | Purpose |
|---|---:|---|
| BME280 | auto-detect `0x76` or `0x77` | Temperature, humidity, pressure |
| BMP280 | auto-detect `0x76` or `0x77` | Temperature, pressure; humidity unavailable |

The ENV bus is deliberately independent from ADS1/ADS2. Firmware diagnostics must report bus pins, ACK state at 0x76/0x77, chip ID, status and read failures. The currently described module is powered from the ESP32 3V3 rail; this document does not infer a wiring fault from a failed probe.

BH1750 remains a future quantitative-light option and is not assigned to either active bus until formally installed.

## 4. IR downward ground / cliff contract

- GPIO35 receives the IR module digital signal and is input-only.
- Candidate electrical interpretation after HR050: LOW = ground detected.
- The firmware does **not** directly equate one sample with a hazard. It asserts `cliff_warning` only after three consecutive samples in which ground is not detected, and clears after two consecutive ground-detected samples.
- Telemetry exposes the raw level, `ground_detected`, and `cliff_warning` separately so field testing can confirm polarity rather than hiding it.
- The current HR050 scope is warning only. It does not automatically stop the motors. Automatic motor interlock requires a separate controlled validation against dark, reflective and irregular surfaces.

## 5. ESP32 electrical capability review

- GPIO34, 35, 36 and 39 are input-only and are used only as inputs.
- Sound GPIO36 and battery GPIO33 are ADC1 inputs, avoiding ADC2/Wi-Fi contention.
- No runtime signal is assigned to SPI-flash GPIO6–11.
- Active drive/control pins avoid common boot-strapping GPIO0/2/4/5/12/15.
- GPIO13/14 are now actively assigned to the environmental I2C controller; they are not an internal-temperature reservation.
- ESP32 GPIO logic is 3.3 V. No 5 V or higher external signal may be applied directly.

## 6. ESP32-CAM — AI-Thinker camera GPIO map

The ESP32-CAM remains a separate Wi-Fi node at `192.168.4.2`; there are no direct master-to-camera GPIO/UART control wires.

| GPIO | Camera function |
|---:|---|
| 4 | Flash LED |
| 32 | Camera PWDN |
| 0 | XCLK 20 MHz |
| 26 | SCCB SIOD |
| 27 | SCCB SIOC |
| 35 | Y9 / D7 |
| 34 | Y8 / D6 |
| 39 | Y7 / D5 |
| 36 | Y6 / D4 |
| 21 | Y5 / D3 |
| 19 | Y4 / D2 |
| 18 | Y3 / D1 |
| 5 | Y2 / D0 |
| 25 | VSYNC |
| 23 | HREF |
| 22 | PCLK |
| -1 | RESET not connected |

These camera pins are occupied by the AI-Thinker camera design and are not free external HazRob GPIO.

## 7. Network-address authority related to hardware

- Main ESP32 SoftAP SSID: `HazRob`.
- Main ESP32 local address: `192.168.4.1/24`.
- ESP32-CAM static local address: `192.168.4.2/24`.
- Camera direct HTTP/status port 80; direct MJPEG port 81.

## 8. Physical acceptance gates

Before HR050 can be marked physically accepted:

1. wheels-up testing confirms logical left/right, forward/reverse, arcs and pivots after flashing;
2. ENV BME/BMP is physically connected to SDA GPIO13 and SCL GPIO14 and is detected without degrading ADS1/ADS2 on GPIO21/22;
3. IR polarity is verified with ground under the sensor and with a real edge/gap, including dark/reflective-surface nuisance checks;
4. ultrasonic ECHO at GPIO39 is verified <=3.3 V;
5. ADS1115 VDD is measured and reconciled with the 5 V acquisition assumption;
6. alert GPIO26 is not connected directly to the low-impedance speaker;
7. software/build gates pass from one exact source commit.

## 9. Safety boundary

HazRob 0.10.1 remains a research prototype. This allocation does not confer intrinsic-safety, Ex, SIL or hazardous-area certification. Classified-atmosphere deployment requires independently engineered and appropriately certified equipment.
