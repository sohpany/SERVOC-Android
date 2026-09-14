# HazRob 0.10.1 — Canonical GPIO Authority

Status: **HR-0101-048 REMEDIATION AUTHORITY — BUILD VALIDATION REQUIRED; PHYSICAL RE-ACCEPTANCE PENDING**  
Change control: `HR-0101-010`; relay notation clarification: `HR-0101-019`; audit-readable aliases: `HR-0101-020`; field steering correction: `HR-0101-048`  
Primary code sources: `firmware/HazRob/Config.h`, `firmware/HazRob/Hardware.cpp`, `firmware/HazRob/Alerts.cpp`, `firmware/HazRob-CAM/HazRob-CAM.ino`.

This document is the human-readable GPIO authority for the HR-0101-048 remediation branch of HazRob 0.10.1. The original HR047 release baseline remains historical until HR048 is build-validated and physically re-accepted. The 2026-09-14 field test confirmed reversed steering handedness while straight forward and reverse remained correct. HR048 therefore preserves the canonical joystick mixer and applies a controlled logical-side swap at the L298N channel boundary as the remediation candidate. Final wheels-up side-isolation validation is still required after flashing the remediation firmware; the mapping is not a physical-acceptance claim until that gate passes.

## 1. Main ESP32 — DOIT ESP32 DEVKIT V1 / ESP32-WROOM profile

Hardware profile: `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1`

| GPIO | Direction / capability | HazRob 0.10.1 HR048 function | External connection | Electrical / integration rule |
|---:|---|---|---|---|
| 17 | Digital output + PWM | `DRIVE_L_EN` | L298N ENB | **Logical left-side** speed PWM in the HR048 remediation candidate. Remove the ENB jumper when ESP32 controls ENB. |
| 19 | Digital output | `DRIVE_L_IN1` | L298N IN3 | Logical left-side direction in the HR048 candidate; not speed PWM. |
| 32 | Digital output | `DRIVE_L_IN2` | L298N IN4 | Logical left-side direction in the HR048 candidate; not speed PWM. |
| 16 | Digital output + PWM | `DRIVE_R_EN` | L298N ENA | **Logical right-side** speed PWM in the HR048 remediation candidate. Remove the ENA jumper when ESP32 controls ENA. |
| 18 | Digital output | `DRIVE_R_IN1` | L298N IN1 | Logical right-side direction in the HR048 candidate; not speed PWM. |
| 27 | Digital output | `DRIVE_R_IN2` | L298N IN2 | Logical right-side direction in the HR048 candidate; not speed PWM. |
| 21 | I2C bidirectional | `SDA` | ADS1115 #1/#2 + BME/BMP bus | 3.3 V ESP32 logic. I2C clock is 400 kHz. Do not allow pull-ups to 5 V on the ESP32 side. |
| 22 | I2C bidirectional | `SCL` | ADS1115 #1/#2 + BME/BMP bus | 3.3 V ESP32 logic. I2C clock is 400 kHz. Do not allow pull-ups to 5 V on the ESP32 side. |
| 25 | Digital output | `RELAY_MQ` | MQ power relay input | One relay controls all eight MQ modules. Active LOW: LOW=ON, HIGH=OFF. |
| 26 | Digital output | `ALERT_PIN` | Reserved alert-driver signal | Logical alert output is implemented, but `ALERT_OUTPUT_INSTALLED=false` until a safe driver/interface is physically confirmed. Never drive an 8-ohm speaker directly from ESP32 GPIO. |
| 36 | **Input only**, ADC1 | `SOUND_PIN` | MAX4466 analog output | Analog signal path. Do **not** route through a bidirectional I2C level shifter. Signal must remain in the ESP32-safe input range. |
| 35 | **Input only** | `IR_PIN` | Lower-front IR/fall-edge module output | Active LOW. Signal presented to GPIO must not exceed 3.3 V. |
| 34 | **Input only** | `LDR_PIN` | LM393 LDR digital output | Threshold state only, not quantitative lux. Signal presented to GPIO must not exceed 3.3 V. |
| 33 | ADC1 input | `BATTERY_PIN` | Battery divider output | Battery must never connect directly to GPIO. Reference divider is 47 kΩ top / 10 kΩ bottom; final assembled divider and ADC require calibration. |
| 23 | Digital output | `ULTRA_TRIG_PIN` | Front ultrasonic TRIG | 3.3 V trigger output. |
| 39 | **Input only** | `ULTRA_ECHO_PIN` | Front ultrasonic ECHO | Typical 5 V ECHO must be reduced to <=3.3 V with an appropriate divider/unidirectional interface; a bidirectional I2C level shifter is not the preferred ECHO interface. |
| 14 | Reserved | `INTERNAL_TEMP_RESERVED_PIN` | No installed sensor in 0.10.1 | Reserved only. Do not assume a sensor, bus, or driver until hardware is formally selected. |

### Canonical machine-readable aliases — HR048 remediation

`DRIVE_L_EN=GPIO17; DRIVE_L_IN1=GPIO19; DRIVE_L_IN2=GPIO32; DRIVE_R_EN=GPIO16; DRIVE_R_IN1=GPIO18; DRIVE_R_IN2=GPIO27; SDA=GPIO21; SCL=GPIO22; RELAY_MQ=GPIO25; ALERT_PIN=GPIO26; SOUND_PIN=GPIO36; IR_PIN=GPIO35; LDR_PIN=GPIO34; BATTERY_PIN=GPIO33; ULTRA_TRIG_PIN=GPIO23; ULTRA_ECHO_PIN=GPIO39; INTERNAL_TEMP_RESERVED_PIN=GPIO14.`

### L298N 4WD topology and steering semantics

- Physical L298N channel A remains `ENA=GPIO16`, `IN1=GPIO18`, `IN2=GPIO27`, outputs `OUT1/OUT2`.
- Physical L298N channel B remains `ENB=GPIO17`, `IN3=GPIO19`, `IN4=GPIO32`, outputs `OUT3/OUT4`.
- HR048 remediation candidate association: **LEFT → channel B** and **RIGHT → channel A**.
- The joystick coordinate convention remains `+Y=forward`, `+X=right` with the canonical mixer `Left=Y+X`, `Right=Y-X`.
- A forward-right arc therefore requires both sides forward with logical Left faster than logical Right; a forward-left arc requires logical Right faster than logical Left.
- Pivot-right requires logical `(Left>0, Right<0)`; pivot-left requires `(Left<0, Right>0)`.
- This logical channel swap is intended to correct the field-confirmed reversed steering handedness without changing forward/reverse polarity. It does **not** by itself prove that both motors on each physical side are electrically healthy; post-flash side-isolation remains mandatory.
- ESP32 control wires remain exactly six GPIO lines: `16, 18, 27, 17, 19, 32`.
- Remove both ENA/ENB enable jumpers when ENA/ENB are driven by ESP32 PWM.
- Main ESP32, L298N logic interface, sensors, and interface circuits require a common signal ground.
- Never connect the L298N 5 V logic rail to an ESP32 GPIO. Do not back-feed the ESP32 through a peripheral rail.

## 2. Analytical I2C topology

Bus: `SDA GPIO21`, `SCL GPIO22`, 400 kHz.

| Device | Address | Channels / purpose |
|---|---:|---|
| ADS1115 #1 | `0x48` | A0 MQ2; A1 MQ4; A2 MQ5; A3 MQ8 |
| ADS1115 #2 | `0x49` | A0 MQ3; A1 MQ7; A2 MQ9; A3 MQ135 |
| BME280 / BMP280 | auto-detect `0x76` or `0x77` | Chamber temperature/pressure and humidity when BME280 is fitted |
| BH1750 | future only `0x23` or `0x5C` | Preferred future quantitative illuminance sensor; not installed in 0.10.1 |

The **software acquisition contract** is configured for ADS1115 VDD = 5 V, PGA ±6.144 V, 860 SPS and four conversions averaged per channel. HR048 physical acceptance must independently confirm the assembled ADS1115 supply domain before this 5 V assumption is treated as a measured hardware fact. The measured ~3.3 V I2C idle level establishes the ESP32-side logic domain but does not prove ADS VDD. Any 5 V-side I2C pull-ups require a proper level-shifting implementation. The I2C shifter is for I2C devices, not for MAX4466 analog OUT, IR logic merely because it is 5 V powered, or ultrasonic ECHO as a generic substitute for a dedicated level interface.

## 3. ESP32 electrical capability review

- GPIO34, 35, 36, and 39 are input-only and are used only as inputs: **source contract PASS**.
- Sound GPIO36 and battery GPIO33 use ADC1, avoiding ESP32 ADC2/Wi-Fi contention: **source contract PASS**.
- No master runtime signal is assigned to SPI-flash GPIO6–11: **source contract PASS**.
- Active drive/control pins do not use common ESP32 boot-strapping GPIO0/2/4/5/12/15: **source contract PASS**.
- GPIO14 is reserved but not actively driven/read by a selected sensor in 0.10.1.
- GPIO16/17 are valid for the stated DOIT ESP32 DEVKIT V1 / ESP32-WROOM target. A different board profile must not inherit this map without review.
- ESP32 GPIO logic is 3.3 V. No 5 V or higher signal may be applied directly to a GPIO.

## 4. ESP32-CAM — AI-Thinker camera GPIO map

The ESP32-CAM is a separate Wi-Fi node at `192.168.4.2`; there are no direct master-to-camera GPIO/UART control wires in the 0.10.1 architecture.

| GPIO | Camera function in 0.10.1 |
|---:|---|
| 4 | Flash LED |
| 32 | Camera PWDN |
| 0 | XCLK (20 MHz) |
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

These camera pins are occupied by the AI-Thinker camera design and must not be treated as free external GPIO in HazRob 0.10.1. GPIO4 is additionally dedicated to flash LED behavior implemented by firmware.

## 5. Network-address authority related to hardware

- Main ESP32 SoftAP SSID: `HazRob`.
- Main ESP32 local address: `192.168.4.1/24`.
- ESP32-CAM static local address: `192.168.4.2/24`.
- Camera direct HTTP/status port: 80.
- Camera direct MJPEG port: 81.
- The master may provide NAT routing to the ESP32-CAM only after WAN connectivity is validated; this does not change the GPIO contract.

## 6. Safety boundary

HazRob 0.10.1 is a research prototype. This GPIO allocation does not confer intrinsic-safety, Ex, SIL, or hazardous-area certification. Any deployment in a classified atmosphere requires independent electrical/protection engineering and appropriate certified equipment.

## 7. HR048 acceptance rule

Before this remediation mapping replaces the HR047 field baseline, all of the following are required:

1. HR048 source/audit gate passes;
2. Android unit tests and release assembly pass;
3. master ESP32 and ESP32-CAM compile with the controlled toolchain;
4. wheels-up isolation proves logical Left commands only the physical left side and logical Right only the physical right side;
5. positive and negative commands are checked independently per side;
6. forward/reverse, forward-right/forward-left arcs, pivot-right and pivot-left are physically verified at low speed;
7. any remaining weak-side behavior is investigated electrically/mechanically rather than hidden by additional sign swaps;
8. assembled ADS1115 VDD is measured and reconciled with the software acquisition constant before analytical physical acceptance.
