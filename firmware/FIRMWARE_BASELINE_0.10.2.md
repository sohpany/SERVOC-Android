# HazRob Firmware Baseline — 0.10.2 / HR069

This document records the canonical firmware authority for the HazRob 0.10.2 / HR069 field-test baseline. Runtime firmware banners remain `HazRob 0.10.2` and `HazRob-CAM 0.10.2`; `HR069` is the engineering/hardware-profile revision.

## Main ESP32

- Runtime firmware: `HazRob 0.10.2`
- API: `4`
- Hardware profile: `hazrob-0.10.2-hr069-1r8mq-l298n6-batt-ultra-ir-ldr-env2-v2`
- Direct boot profile: AP-only
- Direct SSID: `HazRob`
- Main IP: `192.168.4.1`
- Camera IP authority: `192.168.4.2`
- Wi-Fi sleep: disabled
- Main requested TX-power ceiling: 20 dBm (`esp_wifi_set_max_tx_power(80)`; actual RF output remains PHY/regulatory/supply/antenna limited)
- Remote profile: explicit operator activation only; AP+STA

### L298N 4WD

- LEFT: EN `GPIO17`, IN1 `GPIO19`, IN2 `GPIO32`
- RIGHT: EN `GPIO16`, IN1 `GPIO18`, IN2 `GPIO27`
- Direction inversion: false / false
- Drive command timeout: 500 ms

### Sensor and I/O authority

- ADS1115 analytical I2C: SDA `GPIO21`, SCL `GPIO22`
- ENV I2C: SDA `GPIO13`, SCL `GPIO14`
- MQ power relay: `GPIO25`
- Alert logic output: `GPIO26`
- Sound ADC: `GPIO36`
- IR ground/cliff input: `GPIO35`, active LOW
- LDR digital threshold: `GPIO34`
- Battery ADC: `GPIO33`
- Ultrasonic TRIG: `GPIO23`
- Ultrasonic ECHO: `GPIO39`; physical echo must be reduced to <=3.3 V
- IR physical reference height: 8 cm
- IR cliff assert: 3 samples; clear: 2 samples
- Ultrasonic warning: <=10 cm
- Ultrasonic forward stop: <=5 cm

### MQ baseline

- Eight MQ channels on two ADS1115 devices
- ADS assumption: VDD 5 V, PGA +/-6.144 V, 860 SPS, 4 conversions/channel
- R0 source: controlled field clean-air normalization from three sessions on 2026-09-13
- This R0 is not manufacturer/reference-gas validation for quantitative ppm curves

## ESP32-CAM

- Runtime firmware: `HazRob-CAM 0.10.2`
- Network mode: STA-only; camera must never create an AP
- SSID: `HazRob`
- Static IP: `192.168.4.2`
- Gateway: `192.168.4.1`
- Local HTTP status: `http://192.168.4.2/status`
- Local MJPEG: `http://192.168.4.2:81/stream`
- Capture: QVGA 320x240, RGB565
- JPEG path: software `frame2jpg`
- Current software JPEG quality parameter: 35
- PSRAM required
- `/status` exposes PID, sensor classification, FPS, encoding time, JPEG size, errors and RSSI
- Known sensor PID mapping in firmware: `0x2145 -> GC2145`, `0x26 -> OV2640`

## Remote authority

- Production bootstrap/video relay: Railway `hazrob-video-relay-0102-branchfix-production.up.railway.app`
- Bootstrap protocol: 2
- Primary MQTT: EMQX TLS port 8883
- HiveMQ: controlled contingency only, not automatic hot failover in HR069
- CAM remote video pacing range: 55–120 ms per frame request interval

## Build authority

- Arduino ESP32 core: `esp32:esp32@3.3.11`
- Main FQBN: `esp32:esp32:esp32:PartitionScheme=huge_app`
- CAM FQBN: `esp32:esp32:esp32cam`
- Build must pass `tools/audit_0101_firmware_baseline.py`
- Release package must contain Main and CAM binaries plus `SHA256SUMS.txt`

## Field acceptance before release qualification

1. Erase both ESP32 flash memories when replacing an unknown prior build.
2. Confirm Main advertises only `HazRob` in Direct mode and answers at `192.168.4.1`.
3. Confirm CAM never advertises an AP and answers at `192.168.4.2`.
4. Verify `/status` and local MJPEG stream.
5. Verify drive direction, E-stop/watchdog, IR cliff behavior and ultrasonic warning/forward-stop thresholds.
6. Verify MQ/ADS/environment telemetry and session recording.
7. Verify explicit Remote activation, MQTT command/ACK/telemetry and Railway video relay.

Source of truth is the code on `release/hazrob-0.10.2`; this document is descriptive and must remain enforced by the firmware audit and CI.
