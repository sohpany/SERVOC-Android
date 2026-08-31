# HazRob v0.5 integration audit

## Scope

This audit covers the Android application, the previously deployed Servoc_Main3 ESP32 control firmware, the v0.5 environmental-sensor patch, and the existing ESP32-CAM HTTP/MJPEG interface.

## Robot control contract

HazRob uses the existing ESP32 endpoints without changing the drive protocol:

- `GET /drive?l=-100..100&r=-100..100`
- `GET /stop`
- `GET /diag`
- `GET /data`
- `GET /measure?cmd=start|cancel`
- `GET /relay?g=0..3`
- `GET /cfg` and configuration query parameters

Drive heartbeat is sent every 250 ms while the app is foreground. The ESP32 retains the local 1 s communication fail-safe, acceleration/deceleration ramping, reverse braking, and immediate STOP behavior.

## Wi-Fi

Default robot host: `192.168.4.1`. Default camera host: `192.168.4.2`. HazRob binds local robot/camera traffic to the connected Wi-Fi network where Android exposes one, so cellular Internet does not divert private-LAN control traffic. Robot and camera must be reachable on the same Wi-Fi/LAN.

## Environmental sensor

BME280/BMP280 is not relay-controlled and does not depend on the MQ START command. It is read continuously over I2C using SDA GPIO21 and SCL GPIO22. Firmware v0.5 reports chip-ID diagnostics and supports BME280 (0x60) or BMP280 (0x58) at 0x76/0x77.

## MQ and relays

Current deployed direct ADC map contains six physical channels: MQ-2, MQ-3, MQ-4, MQ-7, MQ-8 and MQ-135. The Android analytical library exposes eight MQ slots so MQ-5/MQ-9 can be added later without redesigning the app. Manual relay operation cancels the automatic MQ measurement cycle before energizing a row. The firmware turns all relay rows off before enabling a new row.

## Camera

MJPEG stream: `http://<camera-host>:81/stream`. Flash control in HazRob v0.5 first uses the historical SERVOC contract `/flash?mode=off|auto|on`, then common CameraWebServer fallbacks. Camera streaming is isolated from the robot drive heartbeat.

## Application UI and data

The user-facing v0.5 application is Arabic/RTL with six operational tabs: control, sensors, chemicals, physical measurements, experiments and settings. The 76-model research catalog is no longer printed as an operational list; only a compact scientific-library summary is available on demand.

Experiment database upgrades from schema v4 to v5 non-destructively by adding approval metadata. Approved experiments can be exported as immutable JSON reference records with SHA-256 hashes for `reference_bank/approved_experiments/`. No GitHub access token is embedded in the APK.

## Reports

PDF/XLSX exports are generated for one selected experiment. They include inspection metadata, physical/environmental measurements, MQ acquisition values and a 144-row chemical table. XLSX uses the five HazRob screening colors. PDF includes the complete 144-row table with the corresponding risk presentation. Gray/unavailable rows are retained rather than hidden.

## Known constraints

- Firmware/hardware field testing is still required after flashing the v0.5 ESP32 build.
- MQ cross-sensitivity is not gas selectivity; analytical outputs remain constrained by provenance and validation rules.
- MQ-7/MQ-9 heater-cycle validity requires the appropriate hardware/firmware protocol before quantitative use of those channels.
- HazRob screening colors are project interpretation logic, not a certification by NIOSH/OSHA or an Ex certification.
- Direct GitHub upload from Android is intentionally not implemented with a static token; approved records are exported for controlled repository ingestion, with OAuth/GitHub App integration reserved for a later version.
