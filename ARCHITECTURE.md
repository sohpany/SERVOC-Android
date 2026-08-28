# SERVOC Android Architecture

SERVOC is developed as independent functional packages, analogous to the tab-based separation used in the ESP32 firmware.

- `communication/` — robot network/API transport.
- `control/` — joystick, drive commands and Android-side heartbeat.
- `camera/` — ESP32-CAM stream and future vision input.
- `chemical/` — 144-chemical catalog, MQ curves, PPM consensus.
- `exposure/` — TWA/STEL/Ceiling/IDLH/LEL screening library.
- `measurements/` — physical measurement catalog.
- `calibration/` — versioned R0/calibration metadata.
- `fingerprint/` — trainable chemical signatures and classifier.
- `simulation/` — explicitly synthetic offline development data.
- `analytics/` — analytical-library metadata/versioning.
- `data/` — offline experiments, metadata and fingerprints.
- `cloud/` — future Google Drive backup/sync.
- `vision/` — future object/flame/smoke AI.
- `thermal/` — future thermal matrix/rendering/hotspots.

Safety-critical actuator failsafe remains on the ESP32. Android supplies commands/heartbeat and analytics but is not the sole safety layer.
