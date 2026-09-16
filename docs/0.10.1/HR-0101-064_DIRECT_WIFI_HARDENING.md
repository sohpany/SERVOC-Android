# HazRob 0.10.1 — HR-0101-064 Direct Wi-Fi Hardening

Date: 2026-09-16
Parent controlled baseline: HR-0101-063 / b8ffc7daad1a5c53db77b625d24312c7fd9a8f33

## Purpose
Resolve the field symptom where an ESP_* camera network could appear instead of the authoritative HazRob Direct network, while preserving the complete camera/video and Remote architecture.

## Runtime change
ESP32-CAM remains a station/client of the Main ESP32 HazRob SoftAP. HR064 adds low-level ESP-IDF STA enforcement (`esp_wifi_set_mode(WIFI_MODE_STA)`) in addition to Arduino `WiFi.mode(WIFI_STA)`, and a periodic radio guard that restores STA-only mode if the radio mode deviates. The camera firmware contains no `WiFi.softAP()` authority. The controlled camera identity remains exactly `HazRob-CAM 0.10.1` so Android Direct acceptance remains compatible.

## Authoritative network contract
- Every Main ESP32 boot starts DIRECT.
- Main ESP32 is the sole SoftAP authority: SSID `HazRob`, IP `192.168.4.1`.
- ESP32-CAM is STA-only, static IP `192.168.4.2`, gateway `192.168.4.1`.
- Saved WAN credentials are configuration only and never select Remote on boot.
- WAN provisioning is available through the PIN-protected Developer workspace and saves credentials without activating Remote.
- Remote starts only after explicit operator action.
- Direct transition stops motion, disconnects MQTT, disables NAPT/WAN transport, and retains WAN credentials unless the explicit WAN-clear operation is used.
- CAM Remote WSS remains gated by Main REMOTE profile + Internet + NAPT + CAM TLS time readiness.

## Android compatibility
Android Direct acceptance requires Main `192.168.4.1`, CAM `192.168.4.2`, controlled CAM firmware identity `HazRob-CAM 0.10.1`, and a complete local JPEG frame. HR064 intentionally preserves that identity.

## Developer authorization
`RobotConnectionSetupTab` is instantiated only inside `DeveloperTab.unlock()` after the developer PIN gate succeeds. The WAN panel is therefore not part of the normal user workspace.

## Qualification gates
1. Existing fail-closed 0.10.1 integration/coherence audit must PASS.
2. Gas-safety/consolidated release gate must PASS.
3. Main ESP32 must compile for `esp32:esp32:esp32:PartitionScheme=huge_app`.
4. ESP32-CAM must compile for `esp32:esp32:esp32cam`.
5. Android unit tests and Release APK assembly must PASS.
6. APK signature SHA-256 certificate digest must match the controlled signer.
7. Artifacts must be generated from one final Git commit and accompanied by SHA-256 manifests/build provenance.

## Physical field acceptance still required
Cloud compilation and static/integration tests cannot prove RF behavior on the physical boards. After clean flash/erase of both controllers, field acceptance must verify: only `HazRob` is advertised by the robot system; no `ESP_*` CAM AP appears; Main responds at 192.168.4.1; CAM responds at 192.168.4.2; Direct camera MJPEG works; explicit Remote works with configured WAN; selecting Direct terminates Remote and returns to local operation; motion/sensors/video remain operational.
