# HazRob 0.10.2 / HR069-R1 — Field Acceptance Checklist (WORKING, not yet approved)

**Branch:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2`.
**Android corrective build:** `versionName 0.10.2`, `versionCode 28` — only use an APK verified against the matching CI source SHA and signing certificate. **Do not assume the previous vCode27 APK includes the R0 replay fix.**
**Main ESP32 firmware:** HR069-R1 API v4, Arduino ESP32 core 3.3.11, partition Huge APP. **Flash while motors cannot drive the robot off a surface.**
**CAM:** separate previously paired 0.10.2 / HR069 controller; do not needlessly flash it.

| ID | Bench/phone test | Required observation | Status |
| --- | --- | --- | --- |
| P01 | Voltage under full 8 MQ + both ADS + Wi-Fi load | Regulated rails remain within module tolerances; no ESP32 brownout/reset | PENDING |
| P02 | BME280 on GPIO13/GPIO14 I²C (separate bus) | Actual temperature, relative humidity and pressure show `env.ok=true`, sensible values and stable addressing | PENDING |
| P03 | Echo trigger GPIO23, ECHO GPIO39 via ≤3.3V divider | `/diag.device.proximity.valid=true` on known obstacles, measured distances at ~5/10/20cm, `timeouts` not steadily increasing | PENDING |
| P04 | Ultrasonic no-return / unplugged | `cm=null` or invalid status, not a fictitious 800+cm valid measurement | PENDING |
| P05 | MH-B IR powered 3.3V, OUT GPIO35, mounted <4cm to ground | Floor: raw HIGH, `ground_detected=true`; lifted >70cm: LOW, `cliff_warning=true`. Manually set potentiometer; both tests mandatory | PENDING |
| P06 | Safety options auto-stop OFF then ON | Choice persists after ESP32 reboot. ON blocks **forward** on active IR cliff or Echo stop-zone; reverse/pivot still possible. E-STOP/watchdog work independently | PENDING |
| P07 | Left/right 4WD motor command, inversion | All four motors correct; pivot button swaps intended direction only when chosen; dual tank sliders remain uncrossed | PENDING |
| P08 | Vertical tank + Joystick | Releasing fingers sends 0 motor command; diagonal Joystick permits simultaneous forward & turn | PENDING |
| P09 | App leave/screen rotate/SAF file picker | Motors return to (0,0) and independent motion-stop sends; live measurement need not terminate | PENDING |
| P10 | Start/finish a local USER Site/Field Session | USER session and its telemetry remain local, not eligible for scientific central upload; delete removes its subordinate data only | PENDING |
| P11 | Save one R0 override, all eight, close/reopen app | Correct values and UTC/session lineage survive; new live `rs/r0` uses new R0; no direct firmware R0 rewrite | PENDING |
| P12 | Try R0 update during active acquisition | Save/reset blocked until the measuring session ends | PENDING |
| P13 | Re-analyze an old recorded session after updating R0 | Old per-sample R0, ratios, charts, temporal ppm/M4 (if applicable) and hashes remain unchanged; no retroactive current-R0 rebinding | PENDING |
| P14 | Device offline from internet | Embedded ChemCore Stable 1.0.0-hr069r1 available; model priors research-only, not M4 ppm | PENDING |
| P15 | SmartReport of one Field Session | PDF and XLSX open successfully, charts reflect recorded data; XLSX works even if no derived ChemCore experiment | PENDING |
| P16 | SmartReport of Site with multiple Field Sessions | Includes **all** subordinate sessions, no pooled ppm, both PDF/XLSX open successfully | PENDING |
| P17 | Tracking selected chemical with actual fresh MQ samples while translating | Arrow only when measured concentration gradient supports a bearing; no artificial arrow at rest; fresh MCU sample stamps progress | PENDING |
| P18 | Direct and explicitly enabled Remote | Local AP connectivity and camera work; Remote commands/telemetry tested; 150ms nominal polling does not create phantom fresh sensor data | PENDING |
| P19 | Delete/rename session/site, offline backup if configured | Database rows consistent, reference experiments protected, deleted sites do not reappear from prior local archive | PENDING |

## R0 clean-air validation protocol

1. Use a ventilated safe space away from solvent, gasoline/diesel exhaust, cleaning fumes, aerosol sprays and open flame.
2. Verify stable **5V sensor rail** under all eight MQ heaters, correct GND, both ADS1115 online, BME environment data valid. Do not start a new calibration on a low battery or after sensor power cycles until stabilized.
3. Warm the array per sensor manufacturer protocol, and verify `Rs` stability over multiple windows; never equate burn-in completion with immediate warm-up completion.
4. Record each channel: `MQ key, Rs (Ω), R0 selected (Ω), Vcc, RH%, temperature°C, time UTC, chamber state, session ID, number of samples, duration`. **R0 is protocol-defined**; do not automatically assume `R0 = instantaneous Rs` unless it matches the selected field-baseline convention and your documented protocol.
5. Developer → **معايرة R0**: insert the selected eight *positive finite* R0 values, then save all as one revision. Screenshot the values/source/UTC, close and reopen app and verify the same values.
6. Compare a post-save known stable clean-air MQ snapshot: `Rs / new R0` must equal the displayed normalized `ratio` within rounding. Archived sessions must preserve their *own* R0/ratio.
7. R0 alone is **not** a reference-gas M4 calibration and cannot certify chemical identity/PPM, flammability or occupational exposure compliance.

## Firmware and APK provenance

- `/diag.fw` banner must remain `HazRob 0.10.2`; `/diag.build_revision` must read `HR069-R1`. A phone version alone does not establish which firmware was actually flashed.
- Device-local R0 lives under app `SharedPreferences servoc`. **Do not uninstall the previous app** before installing the same-signed APK update. Uninstalling may erase R0, user sessions and device preferences without separate backup.
- GitHub Actions artifacts use one SHA per build. The Arduino ZIP may be built from a different *documentation-only* SHA; compare actual firmware source bytes before assuming correspondence.
- FAIL any safety check conservatively. Do not operate near unprotected edges or in potentially explosive atmospheres based only on MQ prototype readings.
- Photograph/log the exact failed observation; do not mark untested hardware checks PASS based on emulator or CI.
