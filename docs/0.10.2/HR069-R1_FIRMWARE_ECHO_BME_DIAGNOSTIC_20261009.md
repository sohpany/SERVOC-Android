# HazRob 0.10.2 / HR069-R1 — Echo/BME firmware diagnostic continuation

Date: 2026-10-09
Repository: `sohpany/SERVOC-Android`
**Only modified branch:** `release/hazrob-0.10.2-rc2`
**Status: WORKING, UNAPPROVED. Software CI and physical verification must be recorded before firmware handoff.**
Previous known Android test build 30 source `fa5914beb3b0bce1d5d6cac43d4a300bd284837f` (no new Android implementation changes required for this firmware telemetry extension).
Main firmware remains `0.10.2 / HR069-R1`, API4; ESP32-CAM remains HR069 unchanged.
Firmware diagnostic functional source through commit `26d1d8c83e581beac6485707fa3871d12d7c7ff1`.
Latest source audit updates through `def5e67f807009affe9eb520a08a2f83e12c0641`.

## Why this firmware correction is needed

Field images displayed incrementing Echo timeouts (Status and driving), intermittent BME readings, and drive control not moving each motor side independently. Earlier Android vCode30 compilation validated app/firmware source but the ESP32 firmware was not then modified or physically re-tested. A build success cannot be described as repair/field certification.

## Changes to Main ESP32 firmware (Hardware.cpp only)

### Echo HC-SR04 on TRIG GPIO23 / ECHO GPIO39

- Preserve ISR (CHANGE), nominal 150ms ping, 25ms pulse timeout, `distance_cm = pulse_us × 0.01715`, 10cm warning and 5cm stop-zone semantics.
- Distinguish missing rising edge, missing falling edge, ECHO held HIGH prior to trigger, and invalid pulse duration.
- Preserve null/invalid measured distance on timeout; invalidate stale last-pulse duration for failures.
- Extend `/data` and `/diag` `device.proximity` telemetry with `status`, `echo_level`, `no_rise`, `no_fall`, `stuck_high`, `invalid_pulse`, `last_ok_age_ms`. Existing fields/API4 remain backward compatible. Fields are firmware-side raw diagnostics and not all are projected into the legacy Android normalized object yet.
- **No evidence yet of actual 5V trigger signal, good ECHO level shift, sensor power or valid real echoes.** Changing the code alone cannot fix absent or incorrect electrical pulse signals.

### BME/BMP environmental sensor on SDA GPIO13 / SCL GPIO14

- Preserve 1000ms live sampling and current-only values. **No stale BME readings, smoothing or fabricated data.**
- Bound dedicated `EnvWire` timeout to 25ms.
- Only after the existing three consecutive read failures, restart the dedicated BME I²C bus, then retry probe using existing 3000ms backoff.
- Increment `total_read_errors` and `bus_recoveries` and expose `i2c_timeout_ms` in `env` diagnostics.
- Main analytical `Wire` bus GPIO21/22 and ADS1115 addresses 0x48/0x49 are unchanged. Calibration, ChemCore and gas sensor R0 are unchanged.

### Motor/IR design kept under control

- The Main L298N driver already accepts separate signed `driveSet(left,right)` commands and writes PWM independently to the two output channels.
- Do not invent a firmware motor correction without electrical measurement or a reproducible Motor command/ack/PWM mismatch. The 500ms movement watchdog, 20Hz Android dispatch, and E-stop firmware latch remain unchanged.
- IR GPIO35 remains HIGH = ground detected / LOW = no ground, manually adjusted by operator. Automatic forward stop remains operator-selectable with default OFF per existing approved contract.

## Test gate matrix

| Gate | Current evidence | Remaining acceptance criterion |
| --- | --- | --- |
| Syntax/unit/contract | CI jobs initiated from branch commits; final exact run result must be appended | Main firmware compiles with pinned Arduino-ESP32 3.3.11; all Android/source/audit regressions PASS |
| Echo electrical | No physical reading yet | Verify sensor VCC and common ground; scope/meter TRIG GPIO23 and safe ECHO level <=3.3V on GPIO39; check distance 5cm,10cm,25cm,50cm; inspect `/data.device.proximity.status` while moving target |
| Echo no signal | Existing screenshot timeouts only | No-rise vs no-fall vs stuck-HIGH diagnosis matches measured physical edge state |
| BME reliability | No physical reading after patch | Verify actual 0x76/0x77, read failures, sensor ID 0x60 BME280, 1Hz readings, environmental power noise loaded/unloaded, recovery without impacting ADS channels |
| Motor slider/IR | Previous reported failure persists until field run | Raise wheels, test left-only and right-only +20/+50/+100 then reverse; verify 0 on release, PWM GPIO17/16, check watchdog and E-stop; HIGH ground, LOW lifted |
| Direct and Remote | Prior software only; remote Railway offline | Test direct Wi-Fi control/camera, TLS/WAN separately; avoid unsafe uncontrolled remote motor testing |
| Release packaging | Existing Android diagnostic vCode30 separate artifact | Rebuild Arduino source ZIP, record SHA-256 and compiled SHA, no production promotion without field sign-off |

## Exact operator diagnostic procedure

1. With robot safely supported and drive disabled/neutral, power ESP32 and connect to SSID `HazRob` (Direct).
2. Read `http://192.168.4.1/data` and `http://192.168.4.1/diag`. Do not assume a diagnostic field is in the Android UI if `RobotProtocolV4` has not mapped it.
3. Inspect `device.proximity.status` and `device.proximity.echo_level`: `no_rising_edge` suggests no accepted rise, `no_falling_edge` rise without fall, `echo_high_before_trigger` held-HIGH input. Hardware measurements are necessary to isolate sensor vs divider vs wiring.
4. Inspect `env.status`, `env.read_failures`, `env.total_read_errors`, `env.bus_recoveries` and `env.bus.{sda,scl,hz}`. Environmental `ok=false` must inhibit current analytical environmental correction.
5. Log readings together with battery rail voltages and loads; collect 5 minutes under unloaded conditions and a separate, controlled loaded condition before deciding on any more invasive firmware change.

## Engineering record

| Commit | Description |
| --- | --- |
| `dfd9d6e1d67cf1ba4a5881fd7597264b44fe1631` | Echo ISR outcome diagnostics and safer invalid-input reporting |
| `93b20bdf7d1059864d7d595bd47666db2163239a` | BME dedicated I²C bounded recovery and error counters |
| `26d1d8c83e581beac6485707fa3871d12d7c7ff1` | Correct Echo status JSON string closing |
| `a906b995082880bb1cc1fe34afbd703e51174739` | Update legacy motion contract to assert substantive Echo safety behavior |
| `88b052dfb657fcd8b6f1c4973279ae1ccfad05b4` | Lock Echo/BME source contracts in current RC2 audit |
| `def5e67f807009affe9eb520a08a2f83e12c0641` | Fix Python motion-audit syntax |

## CI (record success or failure, do not assume)

- [Main Arduino ZIP validation for firmware source](https://github.com/sohpany/SERVOC-Android/actions/runs/37869589364) (source through `26d1d8c8`).
- [Current source + Android + firmware CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37869761288), functional firmware unchanged since `26d1d8c8`.
- [Internal source audit and Android build](https://github.com/sohpany/SERVOC-Android/actions/runs/37869761302).
- Historical passing vCode30 source `fa5914beb3b0bce1d5d6cac43d4a300bd284837f` does not prove this newer firmware repair.

**Explicitly blocked:** calling Echo fixed, BME stable, motor sliders field-tested, or firmware safe for final operation without the above field measurements.
