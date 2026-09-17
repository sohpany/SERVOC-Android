# HazRob 0.10.1 — HR-0101-065 Golden Firmware Baseline

Status: **SOFTWARE/BUILD ACCEPTED — PHYSICAL FIELD ACCEPTANCE PENDING**

This document freezes the verified HR-0101-065 firmware baseline so later work does not accidentally restore retired Wi-Fi, camera, or drive behavior.

## Authoritative identities

- Runtime firmware baseline commit: `0d935b9aeecec192a71c45520809bfc36c6f37e9`
- Packaging/test commit producing the current separate ZIPs: `cf63e3c9d55dd270f4d2fbe5faf1d192cd446b7a`
- Render deploy: `dep-dallhbrl550s73bmldlg`
- Render status: `LIVE`
- Arduino ESP32 core used by controlled build: `3.3.11`
- Main build identity: `HR-0101-065-MAIN`
- CAM build identity: `HR-0101-065-CAM`

## Frozen network authority — DO NOT REGRESS

1. Main ESP32 is the **only access point**.
2. Main AP SSID is exactly `HazRob`.
3. Main Direct IP is `192.168.4.1`.
4. Every reboot starts in **Direct** mode.
5. Direct mode is AP-only for the Main ESP32.
6. Saved WAN credentials are configuration only and must never auto-activate Remote at boot.
7. Remote mode is entered only by an explicit operator command.
8. Returning to Direct stops motion, disconnects remote MQTT, disables NAPT/WAN transport, and retains WAN credentials unless the user explicitly clears them.
9. ESP32-CAM is **STA-only** and must never call `WiFi.softAP(...)` or advertise `ESP_*`.
10. ESP32-CAM joins `HazRob` at static IP `192.168.4.2`.
11. ESP32-CAM must not contain or expose the stale field identity `ESP_D62F69`.

## Frozen drive authority — HR048

The corrected drive mapping is part of the HR065 baseline and must not be rolled back:

- `DRIVE_L_EN=17`
- `DRIVE_L_IN1=19`
- `DRIVE_L_IN2=32`
- `DRIVE_R_EN=16`
- `DRIVE_R_IN1=18`
- `DRIVE_R_IN2=27`
- `DRIVE_L_INVERT=false`
- `DRIVE_R_INVERT=false`
- Control watchdog: `500 ms`

Validated steering matrix:

- Forward: `(100,100)`
- Reverse: `(-100,-100)`
- Forward-right: `(100,50)`
- Forward-left: `(50,100)`
- Reverse-right: `(-50,-100)`
- Reverse-left: `(-100,-50)`
- Pivot-right: `(100,-100)`
- Pivot-left: `(-100,100)`

## Controlled build results

- Main ESP32 compile: PASS — program `1,122,401 bytes`; global RAM `50,692 bytes`.
- ESP32-CAM compile: PASS — program `1,158,651 bytes`; global RAM `68,252 bytes`.
- HR065 network/integration gate: PASS.
- Motion contract: PASS.
- Relay/remote guard: PASS.
- HR048 steering contract: PASS.
- HR049 control/remote contract: PASS.
- HR050 field-operability contract: PASS.
- HR047 gas-safety integration gate: PASS.
- Cross-component HR048/060 -> HR065 integration contract: PASS.
- Binary identity checks: PASS.

## Authoritative delivery artifacts

### Main ESP32

File: `HazRob-0.10.1-HR065-ESP32-FLASH.zip`

SHA-256:
`62155cb55b46652b4790d063aa96c876082e18aa043ba6c18c33a9f5651d9583`

### ESP32-CAM

File: `HazRob-0.10.1-HR065-CAM-FLASH.zip`

SHA-256:
`c2e3d04c20c6539668850ac4060adcd83f93b92deaa633d915b420207dcf815a`

### Combined archive

File: `HazRob-0.10.1-HR065-ESP32-CAM-FLASH.zip`

SHA-256:
`69d347e22ee84c9607f74271ca65cd06429300735608df50180cf9bf07b17974`

## Flashing control

Before field acceptance, erase the flash completely on BOTH boards, then flash the appropriate merged binary at address `0x0`. The full erase is mandatory to prevent stale generic ESP32-CAM firmware from continuing to advertise an `ESP_*` AP.

## Acceptance state

Software, integration, compile, packaging, and artifact-identity verification are accepted for HR065. Physical RF/camera/steering acceptance remains **PENDING** until the two boards are erased, flashed with the HR065 packages above, and the following are observed on the actual robot:

- `HazRob` is the robot's only intended AP.
- No ESP32-CAM `ESP_*` AP appears.
- Main responds at `192.168.4.1`.
- CAM responds at `192.168.4.2`.
- Forward/reverse/left/right behavior matches the HR048 contract.

## Anti-regression rule

Any future change to Wi-Fi, Direct/Remote authority, ESP32-CAM radio mode, GPIO drive mapping, inversion flags, watchdog timing, or Android camera identity gate must be compared against this file and must explicitly document why the HR065 baseline is being changed. Restoring older network or drive logic by bulk rollback is prohibited.