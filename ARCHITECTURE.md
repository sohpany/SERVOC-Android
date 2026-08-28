# SERVOC Android Architecture

SERVOC is developed as independent functional packages, analogous to the tab-based separation used in the ESP32 firmware.

Planned packages:

- `communication/` robot network/API transport
- `control/` joystick, drive commands and Android-side heartbeat
- `camera/` ESP32-CAM stream and camera functions
- `sensors/` raw telemetry models
- `chemical/` gas profiles, curves, PPM and consensus
- `exposure/` TWA/STEL/Ceiling/IDLH screening
- `experiments/` experiment lifecycle and records
- `data/` local database/export
- `cloud/` Google Drive backup/sync
- `vision/` object/flame/smoke AI
- `thermal/` thermal matrix/rendering/hotspots

Safety-critical actuator failsafe remains on the ESP32. Android supplies commands and heartbeat but is not the sole safety layer.
