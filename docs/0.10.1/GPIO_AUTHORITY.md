# HazRob 0.10.1 — Canonical GPIO Authority

Status: **AUTHORITATIVE FOR 0.10.1 INTEGRATION**  
Change control: `HR-0101-010`  
Primary code sources: `firmware/HazRob/Config.h`, `firmware/HazRob/Hardware.cpp`, `firmware/HazRob/Alerts.cpp`, `firmware/HazRob-CAM/HazRob-CAM.ino`.

This document is the only human-readable GPIO authority for HazRob 0.10.1. If another active document disagrees with this table, the disagreement is a release defect and must be corrected before release. Historical documents may retain historical mappings only when clearly marked historical.

## 1. Main ESP32 — DOIT ESP32 DEVKIT V1 / ESP32-WROOM profile

Hardware profile: `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1`

| GPIO | Direction / capability | HazRob 0.10.1 function | External connection | Electrical / integration rule |
|---:|---|---|---|---|
| 16 | Digital output + PWM | `DRIVE_L_EN` | L298N ENA | Left-side speed PWM. **Remove the ENA jumper** when ESP32 controls ENA. |
| 18 | Digital output | `DRIVE_L_IN1` | L298N IN1 | Left-side direction only; not the speed-PWM channel. |
| 27 | Digital output | `DRIVE_L_IN2` | L298N IN2 | Left-side direction only; not the speed-PWM channel. |
| 17 | Digital output + PWM | `DRIVE_R_EN` | L298N ENB | Right-side speed PWM. **Remove the ENB jumper** when ESP32 controls ENB. |
| 19 | Digital output | `DRIVE_R_IN1` | L298N IN3 | Right-side direction only; not the speed-PWM channel. |
| 32 | Digital output | `DRIVE_R_IN2` | L298N IN4 | Right-side direction only; not the speed-PWM channel. |
| 21 | I2C bidirectional | `SDA` | ADS1115 #1/#2 + BME/BMP bus | 3.3 V ESP32 logic. I2C clock is 400 kHz. Do not allow pull-ups to 5 V. |
| 22 | I2C bidirectional | `SCL` | ADS1115 #1/#2 + BME/BMP bus | 3.3 V ESP32 logic. I2C clock is 400 kHz. Do not allow pull-ups to 5 V. |
| 25 | Digital output | `RELAY_MQ` | MQ power relay input | One relay controls all eight MQ modules. Active LOW: LOW=ON, HIGH=OFF. |
| 26 | Digital output | `ALERT_PIN` | Reserved alert-driver signal | Alert hardware is not installed in 0.10.1; firmware keeps this GPIO LOW. Never drive an 8-ohm speaker directly from ESP32 GPIO. Use a suitable transistor/driver/amplifier when installed. |
| 36 | **Input only**, ADC1 | `SOUND_PIN` | MAX4466 analog output | 12-bit ESP32 ADC read path, 11 dB attenuation. Signal must remain in the ESP32-safe input range. |
| 35 | **Input only** | `IR_PIN` | Lower-front IR/fall-edge module output | Active LOW. Signal presented to GPIO must not exceed 3.3 V. |
| 34 | **Input only** | `LDR_PIN` | LM393 LDR digital output | Threshold state only, not quantitative lux. Signal presented to GPIO must not exceed 3.3 V. |
| 33 | ADC1 input | `BATTERY_PIN` | Battery divider output | Battery **must never** connect directly to GPIO. Reference divider is 47 kΩ top / 10 kΩ bottom; final assembled divider and ADC must be calibrated. |
| 23 | Digital output | `ULTRA_TRIG_PIN` | Front ultrasonic TRIG | 3.3 V trigger output. |
| 39 | **Input only** | `ULTRA_ECHO_PIN` | Front ultrasonic ECHO | Typical 5 V ECHO must be reduced/level-shifted to <=3.3 V before this GPIO. |
| 14 | Reserved | `INTERNAL_TEMP_RESERVED_PIN` | No installed sensor in 0.10.1 | Reserved only. Do not assume a sensor, bus, or driver until hardware is formally selected. |

### L298N 4WD topology

- Left motors: two left motors in parallel on L298N `OUT1/OUT2`.
- Right motors: two right motors in parallel on L298N `OUT3/OUT4`.
- ESP32 control wires: exactly six GPIO lines for drive: `16, 18, 27, 17, 19, 32`.
- Speed control: PWM on `ENA=GPIO16` and `ENB=GPIO17`.
- Direction control: `IN1=18`, `IN2=27`, `IN3=19`, `IN4=32`.
- The ENA/ENB enable jumpers are **not installed** when those pins are driven by the ESP32.
- Main ESP32, L298N logic interface, sensors, and interface circuits require a common signal ground.
- Never connect the L298N 5 V logic rail to an ESP32 GPIO. Do not back-feed the ESP32 through a peripheral rail.

## 2. Analytical I2C topology

Bus: `SDA GPIO21`, `SCL GPIO22`, 400 kHz.

| Device | Address | Channels / purpose |
|---|---:|---|
| ADS1115 #1 | `0x48` | A0 MQ2; A1 MQ4; A2 MQ5; A3 MQ8 |
| ADS1115 #2 | `0x49` | A0 MQ3; A1 MQ7; A2 MQ9; A3 MQ135 |
| BME280 / BMP280 | auto-detect `0x76` or `0x77` | Chamber temperature/pressure and humidity when BME280 is fitted |
| BH1750 | future only `0x23` or `0x5C` | Preferred future quantitative illuminance sensor; **not installed in 0.10.1** |

ADS1115 acquisition contract: fixed 5 V ADS supply, PGA ±6.144 V, 860 SPS, four conversions averaged per channel. The I2C voltage domain remains an ESP32 3.3 V logic domain: if an ADS1115 breakout has pull-ups tied to 5 V, move the pull-ups to 3.3 V or use a proper bidirectional level shifter.

## 3. ESP32 electrical capability review

- GPIO34, 35, 36, and 39 are input-only on the ESP32 and are used only as inputs in the current firmware: **PASS**.
- Sound GPIO36 and battery GPIO33 use ADC1, avoiding the classic ESP32 ADC2/Wi-Fi contention: **PASS**.
- No master HazRob runtime signal is assigned to SPI-flash GPIO6–11: **PASS**.
- Active master drive/control pins do not use the common ESP32 boot-strapping GPIO0/2/4/5/12/15: **PASS**.
- GPIO14 is reserved but not actively driven/read by a selected sensor in 0.10.1.
- GPIO16/17 are valid for the stated DOIT ESP32 DEVKIT V1 / ESP32-WROOM target. A different ESP32 board profile must not inherit this map without a new hardware-profile review.
- ESP32 GPIO logic is 3.3 V. No 5 V or 12 V signal may be applied directly to a GPIO.

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

These camera pins are **occupied by the AI-Thinker camera design and must not be treated as free external GPIO** in HazRob 0.10.1. GPIO4 is additionally dedicated to the flash LED behavior implemented by the firmware.

## 5. Network-address authority related to hardware

- Main ESP32 SoftAP SSID: `HazRob`.
- Main ESP32 local address: `192.168.4.1/24`.
- ESP32-CAM static local address: `192.168.4.2/24`.
- Camera direct HTTP/status port: 80.
- Camera direct MJPEG port: 81.
- The master may provide NAT routing to the ESP32-CAM only after WAN connectivity is validated; this does not change the local GPIO contract.

## 6. Safety boundary

HazRob 0.10.1 is a research prototype. This GPIO allocation does not confer intrinsic-safety, Ex, SIL, or hazardous-area certification. Any deployment in a classified atmosphere requires independent electrical/protection engineering and appropriate certified equipment.

## 7. Change rule

Any future GPIO reassignment requires all of the following before it is authoritative:

1. a new sequential `HR-0101-NNN` change record;
2. firmware source update;
3. collision/electrical-capability review;
4. update of this GPIO authority document;
5. master and camera firmware compilation as applicable;
6. physical wiring confirmation before energization.
