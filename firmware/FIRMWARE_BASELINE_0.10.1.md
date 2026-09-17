# HazRob 0.10.1 firmware baseline

This document defines the canonical firmware baseline for HazRob 0.10.1.

## Authority

The authoritative source directories are:

- `firmware/HazRob`
- `firmware/HazRob-CAM`

Network behavior must be derived from these firmware sources. Later experimental firmware must not be used as a network authority for this baseline. Hardware changes may be carried forward only when they are explicitly represented in the canonical source.

## Fixed network contract

- Main SSID: `HazRob`
- Main IP: `192.168.4.1`
- Camera IP: `192.168.4.2`
- Main boot profile: DIRECT / AP-only
- Camera profile: STA-only
- The camera must never advertise an `ESP_*` SoftAP
- REMOTE activation is explicit and does not change the fixed local addresses
- Wi-Fi sleep is disabled on both controllers

## Main hardware contract

- LEFT drive: GPIO17, GPIO19, GPIO32
- RIGHT drive: GPIO16, GPIO18, GPIO27
- ADS1115 I2C: GPIO21 / GPIO22
- Environmental I2C: GPIO13 / GPIO14
- MQ relay: GPIO25
- Alert logic output: GPIO26
- Sound: GPIO36
- IR: GPIO35
- LDR: GPIO34
- Battery ADC: GPIO33
- Ultrasonic TRIG/ECHO: GPIO23 / GPIO39

## Camera contract

- AI-Thinker camera mapping
- QVGA 320×240
- RGB565 capture with software JPEG encoding
- local MJPEG on `192.168.4.2:81/stream`
- local stream has priority over remote WSS publishing
- authenticated TLS/WSS remote publishing uses the same validated ISRG Root X1 trust anchor as the main controller

## Acceptance gates

A firmware set is accepted as this baseline only when:

1. Both sketches compile with the pinned Arduino-ESP32 core.
2. Static audit confirms the fixed SSID/IP/mode and GPIO contracts.
3. Main source contains an explicit SSID identity check, not only an AP-IP check.
4. Camera source contains no SoftAP creation path.
5. Both embedded root certificates parse as valid X.509 certificates.
6. No internal source comment or identifier carries an experimental modification number.
7. Main and camera source hashes are recorded with the released package.
8. Field test confirms: only `HazRob` is advertised, main responds at `.1`, camera status responds at `.2`, and MJPEG produces complete JPEG frames.

## Control and video continuity

The baseline keeps the existing HTTP direct-control protocol. The main loop services HTTP continuously with Wi-Fi sleep disabled and a 500 ms command timeout. The camera local stream uses TCP no-delay, QVGA frames, a dedicated streaming task and local priority over remote video. These measures reduce avoidable latency and contention; field RF conditions and power quality still remain external factors and must be verified on the assembled robot.
