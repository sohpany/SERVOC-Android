# HR-0101-050 — Field Operability, Sensor Visibility & Remote Readiness Hardening

**Release line:** HazRob 0.10.1  
**Branch:** `dev/hazrob-0.10.1`  
**Status:** IN PROGRESS — SOURCE CHANGES / SOFTWARE VALIDATION PENDING

## Change authority
This controlled change follows HR-0101-049 and does not bump the application or firmware version. It addresses field-operability findings raised during physical sensor and connectivity testing while preserving the accepted 2026-09-13 field clean-air R0 set and the separation between operational screening and M4-gated quantitative analysis.

## Controlled scope
1. Re-purpose the lower-front IR channel as a downward-looking ground-presence / cliff-warning input. Raw polarity remains explicit; warning semantics become `ground not detected`, with persistence filtering to suppress single-sample chatter.
2. Move BME280/BMP280 environmental acquisition onto a dedicated second ESP32 I2C controller using GPIO13 (SDA) and GPIO14 (SCL). ADS1115 #1/#2 remain on GPIO21/22.
3. Harden Remote entry: separate control readiness from video readiness, bound bootstrap/TCP/TLS/MQTT waits, expose stage-specific progress and failure, and keep verified TLS with no insecure fallback.
4. Improve practical field sensor visibility: expose small MQ response deltas as operational response information without falsely promoting them to ppm/IDLH/LFL conclusions; retain higher warning tiers as separate operational alarms.
5. Restore user access to measurement history/reports and add an independent phone-side alert test.
6. Raise remote video target from the current nominal 10 fps ceiling toward >=17 delivered fps under good network conditions using bounded/adaptive pacing rather than an unbounded producer queue.
7. Close release-governance gaps: pin ESP32 Arduino core 3.3.11 and require the consolidated HR047→HR048→HR049→HR050 audit gate in both APK and firmware workflows.

## Explicit non-scope / protected authorities
- No change to accepted MQ R0 values or provenance.
- No claim that field R0 is manufacturer/reference-gas calibration.
- No bypass of M4 for quantitative ppm reporting.
- No change to L298N motor GPIO mapping unless a separate field finding requires a new controlled change.
- No direct drive of an ~8 ohm speaker from GPIO26.
- No insecure MQTT/TLS fallback.

## Planned field gates
### IR cliff
- Ground present -> `ground_detected=true`, no cliff warning.
- Gap/edge -> persistent `cliff_warning=true` and phone warning.
- Dark/reflective surfaces tested for nuisance alarms before any future automatic motor interlock is considered.

### ENV bus
- ADS1/ADS2 remain healthy on GPIO21/22.
- ENV scan reports GPIO13/14, ACK 0x76/0x77, chip ID, model and read-failure state.
- BME280 reports T/RH/P; BMP280 reports T/P with RH unavailable.

### Remote control
- Entry attempt has bounded duration and visible stages.
- Control can become ready even when video is unavailable.
- Robot Wi-Fi association alone is not reported as remote readiness.
- ACK RTT and telemetry age remain plausible.

### Sensor operability
- Raw/voltage/Rs/Rs-R0 data remain available.
- Operator can see response delta/trend even below the warning threshold.
- Phone alert has an independent test and field warnings are audible/visible when conditions are met.

### Video
- No stale-frame backlog.
- Target >=17 delivered fps in a good-network acceptance run, with adaptive fallback under congestion.

### Reproducibility
- ESP32 core pinned to 3.3.11.
- Consolidated controlled audit is mandatory in APK and firmware CI paths.
- Final APK, ESP32 and ESP32-CAM artifacts must be generated from one exact source commit and recorded with SHA-256 hashes.
