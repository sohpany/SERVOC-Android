# HazRob Android

HazRob is the Android operator, evidence and analytical application for the HazRob remote hazard-inspection robot.

## HazRob 0.9 system

- Android application: `com.servoc.app`, HazRob `0.9.0` development line.
- Master ESP32 uses **HazRob API v4** and remains the authority for drive I/O, local safety behavior, MQ power, ADS acquisition and direct telemetry.
- ESP32-CAM remains a separate camera subsystem at `192.168.4.2`, with local MJPEG and optional remote WSS video.
- Local robot AP is `HazRob`; master ESP32 is `192.168.4.1`.
- Android supports Direct Wi-Fi and secure remote MQTT/TLS operation.
- USER and DEVELOPER workflows remain separated; developer mode exposes experiments, calibration, remote profiles and reference-bank tools.

## Authoritative MQ hardware profile

Hardware profile: `hazrob-0.9-1r8mq-v1`.

All eight installed MQ sensors share **one active-LOW power relay on GPIO25**. There are no separate R1/R2 MQ power banks in this profile.

- `0` = MQ Power OFF — all eight sensors/heaters off.
- `1` = MQ Power ON — all eight sensors/heaters on.
- Any relay state other than `0` or `1` is invalid.

ADS acquisition remains split independently of power:

- ADS1 `0x48`: MQ2=A0, MQ4=A1, MQ5=A2, MQ8=A3.
- ADS2 `0x49`: MQ3=A0, MQ7=A1, MQ9=A2, MQ135=A3.

GPIO26, GPIO27 and GPIO32 are not MQ relay channels in the authoritative 0.9 profile.

The Android measurement UI, developer experiment UI, status display, MQTT command layer, PDF reports and XLSX reports all use the same single-relay model.

## Other installed channels

- MG995 continuous servo left: GPIO18.
- MG995 continuous servo right: GPIO19.
- MAX4466 microphone: GPIO36, electrical RMS and peak-to-peak only unless separately calibrated for dB SPL.
- IR digital warning input: GPIO35, active LOW.
- BME280/BMP280: I2C auto-detection at `0x76/0x77`.
- GPIO33 / GPIO34 / GPIO39: reserved auxiliary inputs.

## Measurement and analytical validity

Firmware acquires physical signals; Android owns chemical evidence fusion, candidate ranking, calibration models, PPM interpretation, exposure references, time-series analysis and reporting.

Default firmware `R0=10.0` values are placeholders and are not device-specific calibration. Quantitative PPM requires a validated sensor/compound calibration path. MQ7/MQ9 quantitative use additionally requires a validated heater protocol; shared 5 V ON/OFF power alone does not establish the manufacturer heater cycle.

## Reports and traceability

Saved sessions and exported reports record the unified MQ power state as **ON/OFF on GPIO25**, alongside the hardware profile, ADS status and measured data. Developer reference captures also record the shared MQ power topology and actual warm-up information.

## Remote operation

The master ESP32 preserves the HazRob AP while optionally joining a WAN/modem network as STA. When Internet readiness is validated it can provide NAPT to ESP32-CAM. Robot remote control uses MQTT/TLS; remote camera video uses the separate WSS relay path.

MQ commands are strict on both paths: only `0` and `1` are accepted.

## Firmware source of truth

Master firmware is under `firmware/HazRob/`:

- `HazRob.ino`
- `Config.h`
- `Hardware.h`
- `Hardware.cpp`
- remote connectivity tabs and protected local secret tab where applicable

ESP32-CAM firmware is maintained separately under `firmware/HazRob-CAM/`.

## Scientific data and reference bank

Scientific identity/hazard coverage is not equivalent to analytical sensor coverage. Approved developer experiments can be exported as immutable reference-bank JSON records and optionally published to the dedicated GitHub reference-bank workflow. Production credentials are not committed to the repository.

## Power validation

All eight MQ heaters now start from the same relay. Validate the actual 5 V rail under simultaneous MQ load plus ESP32, ADS boards, camera and other loads. Servo and MQ heater current must not be routed through the ESP32 regulator or thin logic jumpers.
