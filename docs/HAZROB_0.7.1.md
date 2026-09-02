# HazRob 0.7.1

## Purpose

0.7.1 is the camera and hardware-integration release after field testing 0.7.0 showed no ESP32-CAM video and flash-control failure. It also aligns Android, master ESP32, the four-channel relay board, two ADS1115 converters and the auxiliary sensors with one physical wiring definition.

## Camera recovery boundary

The master ESP32 network is `HazRob`, while an older ESP32-CAM image may still search only for `Servoc`. If the camera never associates with the same AP, both `192.168.4.2:81/stream` and port-80 controls are unreachable. The Android app therefore distinguishes camera-unreachable from command-unsupported instead of treating every failure as an unsupported flash command.

## Camera / video changes

- ESP32-CAM 0.7.1 alternates between open `HazRob` and legacy `Servoc` until connected.
- Static camera IP remains 192.168.4.2; master remains 192.168.4.1.
- Canonical MJPEG endpoint is port 81 `/stream`; port 80 `/stream` redirects to it.
- `/status` reports camera initialization and network/stream diagnostics.
- Multiple flash-control aliases are supported for migration compatibility.
- Android tries multiple MJPEG paths and shows connecting/streaming/retry states instead of a silent black view.
- Android measures the frame rate actually arriving at the phone.
- Flash UI is ON/OFF and distinguishes camera-unreachable from unsupported-control errors.

The current GC2145/classic-ESP32 path is intentionally conservative: QVGA RGB565 capture followed by software JPEG. Do not interpret a successful Android build as proof of camera throughput; field measurements of camera-side conversion time, camera FPS and phone-side FPS are required.

## Standalone MP4 policy

MP4 recording is independent from HazRob measurement sessions and reports. Incoming MJPEG frames are converted to H.264/MP4 with presentation timestamps derived from the actual frame arrival times and exported to `Movies/HazRob` through Android MediaStore.

The encoder may use private temporary working files while recording. After successful MediaStore export those temporary video/index files are deleted, leaving the Gallery MP4 as the persistent recording. If Gallery export fails, the private copy is retained only as a recovery fallback.

The MP4 does **not** create a measurement session, is not attached to PDF/XLSX, and is not saved as a report evidence asset. Flame-analysis events are separate analytical events and may retain their own still-image evidence; they do not own or link the MP4.

## Four-row MQ / relay / ADS architecture

The 4-channel relay board is used as four two-sensor MQ power rows. The temporary power design allows no more than two rows at once.

| Row | Relay | ADS input |
| --- | --- | --- |
| R1 | GPIO25 | MQ4 → ADS1 A0, MQ5 → ADS1 A1 |
| R2 | GPIO26 | MQ7 → ADS1 A2, MQ9 → ADS1 A3 |
| R3 | GPIO27 | MQ2 → ADS2 A0, MQ8 → ADS2 A1 |
| R4 | GPIO32 | MQ3 → ADS2 A2, MQ135 → ADS2 A3 |

ADS1 is 0x48 and ADS2 is 0x49. SDA/SCL are GPIO21/GPIO22. The ADS boards live in the 5 V domain behind the bidirectional I2C level shifter; the BME/BMP sensor can share the ESP32/LV side of the bus.

Both Android and ESP32 enforce the two-row limit. The ESP32 rejects an over-budget `/relay?mask=` request with HTTP 409. A standard eight-channel measurement runs R1+R2 and then R3+R4, each with its own configured warm-up and sampling period.

The app's gas-source planner selects at most two rows according to installed MQ affinity. These selections are operational fingerprint plans, not claims of chemical selectivity or quantitative PPM.

## Auxiliary sensors

- MAX4466 on GPIO36: RMS/P-P voltage only; no dB SPL claim without acoustic calibration.
- IR module on GPIO35: proximity warning only; no drive interlock.
- Digital LDR/LM393 on GPIO34: threshold state only; no lux claim.
- BME280/BMP280: temperature/pressure and humidity when the installed part supports it.
- ADS1115 #1/#2 presence, relay mask, measurement phase and retrace state are exposed in telemetry/reports.

## Analytical limits

Eight MQ channels are normalized in Android: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9 and MQ135. `R0=10` remains a placeholder in the current hardware firmware and is explicitly marked uncalibrated. Therefore Rs/R0 is acquisition/fingerprint data, not automatically a valid PPM result.

MQ7/MQ9 require validated heater-phase control for quantitative use. Relay ON/OFF alone is not treated as proof of a correct heater cycle.

## Report changes

PDF and XLSX include:

- environment readings and sensor identity;
- MAX4466, IR and LDR status;
- ADS1115 #1/#2 presence;
- actual R1–R4 relay mask and two-row power limit;
- per-MQ row, ADS number, channel, Raw/Vao/Rs/Rs-R0 and calibration flag;
- measurement phase/mask;
- chemical evidence/quantitative status without promoting uncalibrated readings to PPM.

Standalone MP4 files are deliberately excluded.

## Version

- Android versionName: 0.7.1
- Android versionCode: 11
- ESP32 API: v3 plus four-row relayMask/ADS1115/IR/LDR/sound/retrace extensions
- ESP32-CAM firmware identity: HazRob-CAM 0.7.1

## Required practical validation sequence

1. With servos and MQ heaters disconnected, program the master ESP32 and verify the `HazRob` AP and `/diag`.
2. Verify ADS1=0x48, ADS2=0x49 and the BME/BMP address on the I2C bus.
3. Verify each relay individually: R1, R2, R3, R4, then permitted two-row combinations. Confirm a three-row mask is rejected.
4. Connect each MQ AO to its documented ADS channel and verify row/ADS/channel identity in `/data` before chemical exposure.
5. Check the 5 V rail under two MQ rows, then under two MQ rows plus servo movement. Stop testing if the rail sags/brownouts occur.
6. Verify MAX4466 RMS/P-P, IR warning-only behavior and LDR threshold telemetry.
7. Run a complete two-phase MQ cycle and confirm all eight channels appear in the result.
8. Program ESP32-CAM 0.7.1, power the master first, then the camera. Confirm `/status` before testing video.
9. Confirm live stream, measured phone FPS and flash ON/OFF.
10. Record a short MP4, stop recording, and verify exactly the exported video appears in `Movies/HazRob` and is absent from measurement reports.
11. Export PDF and XLSX and verify the four-row mapping, ADS status and sensor values.
12. Test retrace only in an open controlled area because it is dead-reckoning and IR remains warning-only.
