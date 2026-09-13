# HazRob 0.10.1 — HR-0101-044 Firmware Build Compatibility Record

This record exists to force a controlled same-source rebuild of the ESP32 master firmware, ESP32-CAM firmware and Android application for the HR-0101-044 calibration-candidate baseline.

## Executable-impact statement

HR-0101-044 changes the Android/ChemCore analytical laboratory, calibration quality gates, result envelope and audit/reporting path. It requires **no executable change** to the ESP32 master or ESP32-CAM firmware.

The firmware source present beside this record remains the required matching firmware for HazRob 0.10.1 / API 4. The controlled build must still compile both firmware targets from this exact source commit to prove compatibility.

## Non-regression constraints

- ESP32 master GPIO allocation: unchanged.
- I2C: SDA GPIO21, SCL GPIO22; ADS1115 0x48/0x49; BME/BMP 0x76/0x77.
- MQ relay: GPIO25, active LOW, boot-safe initialization retained.
- Drive PWM/control mapping: unchanged.
- Remote MQTT/bootstrap contract: unchanged.
- ESP32-CAM HTTP/video behavior: unchanged.
- No direct quantitative ppm calculation is moved into firmware; M4/calibration interpretation remains in the controlled Android/ChemCore layer.

## Build gate

For the calibration-candidate baseline, the following must pass from the same source SHA:

1. cross-component integration audit;
2. Android unit tests and signed release APK build;
3. ESP32 master compile;
4. ESP32-CAM compile;
5. firmware flash-headroom policy.

Physical calibration/field validation remains a separate post-build gate.
