# Servoc_Main3 v0.5 environmental-sensor patch

This directory records the files changed for the HazRob v0.5 ESP32 integration. It is intentionally a patch set rather than claiming that unchanged legacy files were rewritten.

## Changes

- I2C remains SDA = GPIO21 and SCL = GPIO22.
- Probe 0x76 and 0x77 at boot.
- Read chip ID register 0xD0.
- BME280 (0x60): temperature, humidity and pressure.
- BMP280 (0x58): temperature and pressure; humidity is unavailable/null.
- `/diag` adds `firmware`, `api_version` and an `env` object containing detection state, I2C address, chip ID and sensor type.
- Legacy `bme` JSON is retained for backward compatibility with older HazRob builds.
- Environmental sensing remains continuous and independent of `/measure?cmd=start`; START controls the MQ relay-row cycle only.

## Library dependency

Install **Adafruit BMP280 Library** and **Adafruit BusIO** in addition to the libraries already required by Servoc_Main3 (including Adafruit BME280 and ESP32Servo).

## Full delivery archive

The full firmware delivery archive is `Servoc_Main3_v0.5.zip` with SHA-256:

`81ce13f873dc815f3826f40bf46d7d4dd2517724a2a2ee144df80fe16b466924`

The unchanged legacy files remain part of that delivery archive. This patch directory exists to make the v0.5 change set auditable in GitHub.
