# HR-0101-051 — Camera Recovery & Diagnostics

**Release line:** HazRob 0.10.1  
**Branch:** `dev/hazrob-0.10.1`  
**Status:** IN PROGRESS — CAMERA RECOVERY / VALIDATION PENDING

## Trigger
Field report: ESP32-CAM is powered correctly but no camera image is available. Source review of the accepted HR050 candidate found no local endpoint mismatch: camera firmware serves `192.168.4.2:81/stream`, Android requests the same endpoint, cleartext HTTP is enabled, and the AI-Thinker pin map matches Espressif's canonical mapping. Two robustness gaps remain: the camera firmware hard-fails when PSRAM is not detected, and the Android remote WSS viewer creates TLS sockets without an explicit TCP connect timeout.

## Controlled scope
1. Add ESP32-CAM serial boot diagnostics at 115200 baud: PSRAM, camera init error, sensor PID/model, Wi-Fi state/IP and stream endpoints.
2. Preserve the canonical AI-Thinker pin map and QVGA stream, but allow a local DRAM single-framebuffer fallback when PSRAM is unavailable instead of refusing camera initialization outright.
3. Keep local camera IP `192.168.4.2`, master gateway `192.168.4.1`, HTTP status port 80 and MJPEG port 81.
4. Expand `/status` with Wi-Fi connection state and framebuffer-memory mode so field diagnosis distinguishes boot/camera/Wi-Fi/stream failures.
5. Harden Android remote WSS with an explicit TCP connect timeout before TLS wrapping, preserving hostname verification and the Android trust store.
6. Improve operator camera-test diagnostics so `/status` failures report camera error/PSRAM/sensor/IP rather than only CAM OK/OFFLINE.
7. Add an HR051 camera contract audit and require compile/unit-test validation before producing replacement artifacts.

## Protected authorities / non-scope
- No changes to motor GPIO/mixer/watchdog.
- No changes to MQ R0, M4, ChemCore or quantitative-analysis authority.
- No changes to ENV GPIO13/14 or ADS GPIO21/22.
- No insecure TLS fallback.
- No change to the camera's canonical AI-Thinker pin map.

## Field acceptance after flashing
1. Remove any GPIO0-to-GND programming jumper before normal boot, then reset/power-cycle the camera.
2. Main ESP32 `/diag` AP client count should include the phone and ESP32-CAM when both are associated.
3. `http://192.168.4.2/status` must respond and identify `HazRob-CAM 0.10.1`.
4. `camera_ok=true`; if false, record `camera_error`, `psram`, `fb_memory`, PID and sensor model.
5. `http://192.168.4.2:81/stream` must display local MJPEG before remote video is evaluated.
6. Remote publisher/viewer is evaluated only after local stream acceptance.
