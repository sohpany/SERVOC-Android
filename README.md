# HazRob

HazRob is the Android operator, sensing, evidence and analytical application for the HazRob remote hazard-inspection robot. **HazRob 0.8.0 is the current main baseline and `main` is the source of truth for future development.**

## Current baseline — HazRob 0.8.0

- Android package: `com.servoc.app`, versionCode `13`, versionName `0.8.0`.
- Master controller: DOIT ESP32 DEVKIT V1, project API v3.
- Camera: independent ESP32-CAM subsystem with MJPEG streaming and flash control.
- Local Wi-Fi SSID: `HazRob`; master address `192.168.4.1`; camera address `192.168.4.2`; SoftAP DHCP clients start at `192.168.4.100` so the camera address remains reserved.
- Camera display, recording, flash control, experimental visual-flame detection and the advisory gas-direction arrow remain available. Video recording is independent from MQ measurement reports and is not embedded in PDF/XLSX analytical reports.
- Normal `USER` mode is separated from the PIN-protected `DEVELOPER` laboratory for calibration, controlled exposures, reference captures and model comparison.

## Installed MQ / ADS1115 hardware

HazRob 0.8 uses two manually controlled MQ power groups and two ADS1115 converters:

| Group | Relay | ADC | Channels |
| --- | --- | --- | --- |
| R1 | GPIO25 | ADS1115 #1 `0x48` | A0 MQ-2, A1 MQ-4, A2 MQ-5, A3 MQ-8 |
| R2 | GPIO26 | ADS1115 #2 `0x49` | A0 MQ-3, A1 MQ-7, A2 MQ-9, A3 MQ-135 |
| R3 | GPIO27 | Reserved | Future expansion |
| R4 | GPIO32 | Reserved | Future expansion |

MQ power-group selection is manual (`OFF`, `R1`, `R2`, or `R1+R2`). The measurement/averaging operation does **not** automatically switch the relays. Warm-up duration is operator-defined and recorded with developer reference captures. MQ-7/MQ-9 quantitative use remains conditional on a validated heater-cycle protocol; simple group ON/OFF control is not treated as proof of the required heater cycle.

Environmental acquisition through BME280/BMP280 is independent of MQ relay switching. Other physical channels are exposed only when actually installed and available.

## Analytical and scientific library

HazRob separates **chemical knowledge** from **analytical measurability**. The presence of a chemical identity, exposure limit or hazard record does not mean the installed MQ array can uniquely identify or quantitatively measure that chemical.

The current baseline contains:

- A legacy analytical-priority catalog of 144 gases/compounds with MQ affinity/evidence matrices and curve eligibility metadata.
- An offline scientific identity/safety layer seeded from authoritative datasets: 652 NIOSH chemical identity records, 402 NIOSH IDLH records, and 638 normalized OSHA Table Z-1 exposure-limit records covering 432 substances. Each generated dataset carries provenance and SHA-256 metadata and is validated by CI before the APK build.
- Winsen manufacturer documentation for the eight installed MQ sensors.
- MQSensorsLib curve/calibration provenance.
- MQDataScience v6.0.0 pinned to commit `e88f876140587a708ee84a4d4d3acbd5720274ee`. All 76 upstream models remain adopted by the project; Android runtime support is tracked separately as `UPSTREAM-VERIFIED`, `APPROXIMATION`, or `PORT PENDING` so adoption is never confused with implementation status.
- Developer calibration/reference captures including known concentration when available, `Rs`, manual `R0`, `Rs/R0`, temperature, relative humidity, relay mask, hardware profile, warm-up duration and experimental notes.

Legacy/default `R0=10` values are placeholders, not physical calibration certificates. Quantitative PPM and exposure-risk interpretation require an appropriate validated sensor/compound calibration path and domain checks. Otherwise HazRob reports screening/fingerprint/research evidence without fabricating reference-grade concentration values.

## Research integrity and reproducibility

The research bibliography and source-provenance record are maintained in [`REFERENCES_APA.md`](REFERENCES_APA.md). It includes manufacturer manuals, NIOSH/OSHA sources, standards, software/model provenance and peer-reviewed literature on MQ/MOX sensing, calibration, ADC/resistance measurement, sensor fusion, IoT and machine learning.

For a paper, thesis or conference contribution, record the exact HazRob version/commit used, the sensor hardware profile, calibration method, reference-gas procedure when applicable, environmental conditions, preprocessing/model version and validation method. Manufacturer curves or literature regressions should not be described as traceable individual-sensor calibration unless that calibration was actually performed and documented.

## Firmware source of truth

The master firmware is stored under `firmware/HazRob/` as four Arduino tabs:

- `HazRob.ino`
- `Config.h`
- `Hardware.h`
- `Hardware.cpp`

The ESP32-CAM firmware is stored separately under `firmware/HazRob-CAM/`. Android and firmware CI workflows both follow `main`.

## Reports and experiments

PDF/XLSX analytical reports record the actual HazRob 0.8 hardware mapping, sensor/ADC channel, measurement mode, `Rs`, `R0`, `Rs/R0`, calibration state and supporting analytical evidence. Developer reference experiments can be retained as structured records for reproducibility. Camera video remains a separate operational function and is not treated as analytical concentration evidence.

## Repository policy

`main` is the current HazRob baseline. Historical development branches remain available for audit and recovery but are not sources for new work. New changes should start from `main`, use short-lived `fix/...` or `feature/...` branches when needed, pass CI, and then return to `main`.

## Key documentation

- `REFERENCES_APA.md` — research bibliography and source provenance.
- `docs/GAS_MODEL_RESEARCH.md` — gas-model research notes and evidence boundaries.
- `ARCHITECTURE.md` — application/system architecture.
- `CHANGELOG.md` — historical development record.
