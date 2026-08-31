# HazRob ESP32 firmware — API v3

This directory is the source-of-truth four-tab Arduino project verified on **DOIT ESP32 DEVKIT V1** before the HazRob 0.6 Android migration.

## Verified Arduino result

- Program storage: `1,012,743 bytes (77%)` of `1,310,720`.
- Global variables: `49,564 bytes (15%)` of `327,680`.
- Free dynamic-memory budget reported by the Arduino linker: `278,116 bytes`.

## Network

- Local AP SSID: `Servoc` (retained temporarily for ESP32-CAM compatibility).
- Master ESP32: `192.168.4.1/24`.
- Default ESP32-CAM address stored by the master: `192.168.4.2`.
- NVS namespace remains `servoc` to preserve prior camera/neutral/timing preferences.
- The master does not proxy camera video or flash control. HazRob Android communicates with ESP32-CAM directly.

## API v3

Commands retained:

- `GET /drive?l=-100..100&r=-100..100`
- `GET /stop`
- `GET /relay?g=0..3`
- `GET /measure?cmd=start|cancel`
- `GET /cfg` and `cam/speed/nl/nr/warm/measure` query parameters

Compact telemetry:

- `GET /diag` → firmware/API/heap/client/failsafe/environment-hardware diagnostics.
- `GET /data` → continuous environment data, active relay group, live MQ acquisition and measurement-session averages.

Android 0.6 combines `/diag` and `/data` through `RobotProtocolV3`; UI/analytics code must not depend directly on compact wire keys.

## Hardware policy

- Drive: GPIO18/GPIO19 continuous servos with local acceleration/deceleration/reversal braking and a 1 s command fail-safe.
- Environment: BME280/BMP280 directly on I2C SDA21/SCL22, continuously sampled and never relay-controlled.
- MQ direct ADC channels: MQ-2 GPIO34, MQ-3 GPIO39, MQ-4 GPIO33, MQ-7 GPIO32, MQ-8 GPIO36, MQ-135 GPIO35.
- Relay groups: GPIO25/26/23, active LOW, mutually exclusive.
- MUX selectors 13/14/16/17 are reserved only; no MUX signal path is active in this baseline.

## Analytical boundary

The firmware transmits acquisition values (`raw`, `Vadc`, `Vao`, `Rs`, `Rs/R0`). It performs no gas identification, PPM conversion or exposure interpretation. Current `R0=10` entries are retained legacy/default values and are **not evidence of device-specific reference-gas calibration**.
