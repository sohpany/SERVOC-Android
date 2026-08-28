# SERVOC Android

SERVOC is the Android operator application for the remote hazard-inspection robot. The application is currently branded **HazRob**.

## v0.2 Analytical Core

- ESP32/ESP32-CAM connection and drive heartbeat remain available.
- 144-item chemical catalog migrated from the ESP32 project source.
- 15 physical-measurement definitions migrated from MeasurementCatalog.h.
- 39 MQ curve records and consensus PPM engine migrated from GasProfiles.h / GasAnalyzer.cpp.
- Source-backed exposure screening database migrated from ExposureLimits.h.
- Offline analytical simulator for development without robot hardware.
- Versioned calibration model for future sensor-specific R0 calibration.
- Trainable chemical fingerprint engine using repeated log(Rs/R0) signatures.
- SQLite v2 for experiments, analytics metadata, and fingerprint records.

Quantitative gas readings are not compliance-grade until the complete sensor chain is calibrated and independently validated. Safety-critical motor failsafe remains on ESP32.

## Research references

A complete source-provenance and APA 7th edition bibliography for the sensor manuals, MQ curve libraries, occupational exposure databases, Android/ESP32 implementation references, calibration standards, and wider HazRob engineering standards is maintained in [`REFERENCES_APA.md`](REFERENCES_APA.md).
