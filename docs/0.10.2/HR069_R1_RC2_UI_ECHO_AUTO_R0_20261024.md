# HazRob 0.10.2 / HR069-R1 RC2 — UI, Echo and auto-R0 changes
Date: 2026-10-24. Original wheeled HazRob only; NOT HazRob Tank.

- MAX4466 display now refreshes at 1000 ms with exponentially smoothed dBFS. A separate high-rate physical refresh does not overwrite it. The firmware's raw data rate has NOT changed.
- Landscape MQ switch moved to bottom driving action strip; clear of direction control.
- ECHO 5 V -> 3.3 V logic level shift does NOT scale pulse width. Range calibration offset in Android affects valid display only and never firmware auto-stop logic. Invalid pulses are diagnostic only, not distances. At 46991 us, idealized echo distance ~806 cm exceeds a standard 4 m HC-SR04 range.
- Developer R0: 30 accepted one-second rounds, all 8 channels required; reference is a device-specific operational clean-air Rs baseline in kΩ, not certified R0. Review and explicit confirmation before an atomic update; source and session/timestamp retained; no retroactive edits to archived measurements.
- Physical display copies independent Tank light-table visual design while keeping the separate original app and data source.
- Build/action tests plus physical phone verification are required before approving a new RC2 APK. No ESP32 firmware changes in this patch. Do not uninstall the original app while diagnosing sessions.
