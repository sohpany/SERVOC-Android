# HazRob 0.9 Architecture

HazRob 0.9 separates deterministic robot I/O from Android-side analysis, evidence handling, reporting and scientific data management.

## System boundary

**Master ESP32** owns drive actuator I/O, local motion safety behavior, the single MQ power relay, dual ADS1115 acquisition, BME/BMP acquisition, MAX4466/IR inputs, local HTTP API4, WAN/NAPT and outbound MQTT/TLS.

**ESP32-CAM** is a separate camera subsystem at `192.168.4.2`. It provides local MJPEG and can publish JPEG frames through the remote WSS relay while preserving local operation.

**HazRob Android** owns telemetry normalization, operator workflow, chemical analysis, developer experiments/calibration, scientific reference data, persistence, media evidence, reporting, guest authorization and optional reference-bank publication.

## Authoritative MQ power boundary

Hardware profile `hazrob-0.9-1r8mq-v1` has exactly **one active-LOW MQ power relay on GPIO25**.

- OFF / `mask=0`: all eight MQ sensors off.
- ON / `mask=1`: all eight MQ sensors on.
- `mask=2`, `mask=3` and any other relay-mask value are invalid.

The power relay is independent from the two ADC devices:

- ADS1 `0x48`: MQ2, MQ4, MQ5, MQ8.
- ADS2 `0x49`: MQ3, MQ7, MQ9, MQ135.

Android and ESP32 use the same OFF/ON model. ADS health never implies a separate power bank.

## Android packages

- `auth/` — USER/DEVELOPER entry boundary.
- `communication/` — Direct HTTP, remote MQTT/TLS, API4 normalization and guest remote authorization.
- `hardware/` — authoritative physical HazRob topology.
- `control/` — operator drive commands and heartbeat coordination.
- `measurement/` — telemetry recording, transient features and sensor-health/drift analysis.
- `navigation/` — motion history and compound-specific relative direction research.
- `camera/` — local ESP32-CAM stream/flash, recording and remote video client.
- `vision/` — experimental visual evidence; it does not issue drive or PPM decisions.
- `chemical/` — dynamic chemical knowledge and evidence models.
- `exposure/` — authority/jurisdiction-aware exposure data.
- `calibration/` — device/reference-gas calibration metadata.
- `models/` — model registry/runtime status including MQDataScience catalogue.
- `analytics/` — evidence fusion and result snapshots.
- `data/` — offline-first SQLite storage.
- `github/` — developer reference-bank publication.
- `report/` — PDF/XLSX/reference-bank export.
- `ui/` — control, measurements, status, experiments and settings.

## API v4 boundary

Local HTTP remains available at `192.168.4.1` with `/diag`, `/data`, drive/safety endpoints, `/relay`, `/measure`, `/cfg`, WAN provisioning and guest authorization endpoints.

`RobotProtocolV4` normalizes Direct and Remote telemetry into one application snapshot. UI, analysis and reports consume that normalized structure.

The normalized relay object is a single state with `mask` 0/1, `on`, `power_ms` and one channel. Each normalized MQ channel maps to relay 1 while retaining its own ADS/channel identity.

## Remote boundary

Remote robot commands use MQTT/TLS with session ID, monotonic sequence, nonce and TTL. A relay command is accepted only when its state is exactly 0 or 1. Remote camera video is a separate WSS path and does not alter robot MQTT credentials or MQ power semantics.

WAN provisioning remains a local bootstrap operation because the ESP32 must receive the modem/network credentials before the remote path can exist.

## User roles

Normal and developer workflows share the same physical robot topology. Developer mode additionally exposes experiment/calibration/reference-bank and remote profile controls. Guest remote authorization is issued by a developer and can later be revoked by the robot.

## Measurement and evidence model

MQ power selection is manual. Starting averaging does not silently change the relay. A valid measurement must preserve the same shared MQ power state for the sampling window.

Saved telemetry and reports include shared MQ power state, GPIO25 traceability, hardware profile, ADS state and sensor values. Camera/media evidence remains logically separate from chemical quantification.

## Scientific validity

Chemical database coverage does not imply installed-sensor analytical coverage. Firmware `R0=10.0` values are placeholders and cannot authorize quantitative PPM. MQ7/MQ9 require a validated heater protocol before quantitative use.

## Offline-first rule

Direct robot operation, measurement, local analysis and report generation remain available without Internet. Remote MQTT, remote WSS video and optional GitHub publication are additional transport layers, not dependencies of local control.
