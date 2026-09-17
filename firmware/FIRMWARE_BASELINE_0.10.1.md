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
- Automatic Wi-Fi reconnect is disabled on both controllers; recovery is firmware-controlled

## Remote integration contract

- Robot identity: `hazrob-001`
- HiveMQ Cloud endpoint: `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud:8883`
- MQTT topics: `hazrob/hazrob-001/cmd`, `/ack`, `/telemetry`, `/status`
- Render endpoint: `hazrob-video-relay.onrender.com:443`
- Bootstrap path: `/hazrob/bootstrap`
- Remote video publish path: `/hazrob/video/publish/hazrob-001`
- Bootstrap service version: `0.10.1`
- Bootstrap protocol: `2`
- Main obtains robot-scoped MQTT credentials from Render and then connects directly to HiveMQ over TLS
- ESP32-CAM obtains its publisher token from Render and publishes JPEG frames over authenticated WSS
- Main TLS connection timeout is 5000 ms; established MQTT socket timeout is 1000 ms
- Camera TLS/WSS connection timeout is 5000 ms; established WSS socket timeout is 1500 ms
- WAN reconnect is automatic while REMOTE remains selected, using bounded exponential backoff up to 60 seconds
- DNS health is de-bounced: a single transient DNS failure does not immediately drop MQTT, NAPT or camera Internet routing
- Camera WSS recovery uses bounded exponential backoff from 2 to 30 seconds

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
- the remote-video worker remains allocated across stop/start transitions to avoid task restart races

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
9. Remote field test confirms Render bootstrap, HiveMQ command/ack/telemetry/status, NAPT, camera bootstrap and continuous WSS video.

## Control and video continuity

The baseline keeps the existing HTTP direct-control protocol. The main loop services HTTP continuously with Wi-Fi sleep disabled and a 500 ms command timeout. The camera local stream uses TCP no-delay, QVGA frames, a dedicated streaming task and local priority over remote video. WAN recovery and DNS hysteresis are designed to avoid unnecessary Remote-path drops. These measures reduce avoidable latency and contention; field RF conditions, upstream Internet quality and power quality remain external factors and must be verified on the assembled robot.
