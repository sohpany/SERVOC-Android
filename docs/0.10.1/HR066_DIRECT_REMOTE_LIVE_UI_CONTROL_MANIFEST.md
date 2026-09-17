# HR-0101-066 — Direct-network stability, Remote authority, live measurement UX and drive responsiveness

Date: 2026-09-18  
Release line: HazRob 0.10.1  
Scope: Android + Main ESP32 + ESP32-CAM + controlled source audits  
Physical acceptance: PENDING

## 1. Change intent

HR-0101-066 consolidates field observations from the 0.10.1 v24 test build:

- Android could request a temporary `HazRob` network even while already associated with the real Main ESP32 AP, producing duplicate-name UI and repeated temporary-network prompts.
- Direct must remain the default and dominant control authority.
- Remote must require stored WAN configuration, explicit developer authorization, and an explicit Remote request.
- ESP32-CAM must remain STA-only and must never create an AP.
- Camera flash GPIO4 must boot deterministically OFF and expose reset diagnostics.
- Live MQ readings must remain available outside measurement sessions and must remain legible in the light HazRob branding.
- Elevated gas response must produce a red sensor heading while the existing phone warning tone remains active.
- The downward digital IR sensor uses an 8 cm physical installation/calibration reference for ground-loss/cliff semantics.
- The drive joystick requires a 20% larger diameter and immediate linear response without a software dead-zone.

## 2. Direct Wi-Fi authority

Canonical network topology remains:

- Main ESP32: sole AP `HazRob`, `192.168.4.1`.
- ESP32-CAM: STA-only client, static `192.168.4.2`.
- Android: client of the Main ESP32 AP.

Android now binds to an already-associated `192.168.4.0/24` Wi-Fi first. `WifiNetworkSpecifier` is only used from the explicit Direct-entry fallback path when no current HazRob route exists. The 250 ms link keeper no longer creates/recreates temporary-network requests.

Direct transition no longer performs `releaseRobotWifi() -> restartRobotWifiRequest()`. It preserves the current association while Main changes AP+STA to AP-only.

## 3. Remote authority contract

Remote activation now requires all three conditions:

1. WAN credentials are configured.
2. Developer authorization is stored in Main ESP32 NVS.
3. Remote is explicitly requested.

Main ESP32 rejects `/connection/remote` and `/remote/connect` when developer authorization is absent. Developer authorization is controlled from the PIN-protected Developer > Connection workspace through `/remote/authorize`.

Direct always has priority. A Direct request stops motion and **immediately revokes MQTT/NAPT/STA remote command authority before the deferred radio-profile switch**, while preserving saved WAN credentials and the developer authorization state. This closes the short transition window in which a remote command could otherwise arrive after the local operator selected Direct.

Clearing WAN also clears developer authorization.

## 4. ESP32-CAM

The CAM firmware remains `WIFI_STA` only and contains no production `softAP` path.

At boot:

- GPIO4 is configured as output.
- GPIO4 is explicitly driven LOW.
- flash mode is reset to OFF.

`/status` now exposes `reset_reason` from `esp_reset_reason()` to assist diagnosis of repeated reboot/brownout behavior.

## 5. Live measurements and alarms

Live sensor acquisition remains independent of the timed measurement session. The session controls the controlled capture duration used for analysis; it does not start or stop the live telemetry path.

MQ detail text is explicitly dark grey (`#616161`) so the light branding cannot render white values on white/light-grey cards.

Operational MQ warning presentation:

- ELEVATED: red sensor heading.
- HIGH: dark red sensor heading + pulse.
- Existing phone `STREAM_ALARM` warning tone remains driven by `GasAlertCoordinator`.

These operational response warnings remain distinct from calibrated/reportable ppm, IDLH and LFL decisions.

## 6. IR cliff semantics

The installed downward digital IR channel is not a distance sensor. The code records an 8.0 cm physical reference height:

`IR_REFERENCE_HEIGHT_CM=8.0f`

The alarm rule remains persistent loss of the calibrated ground-detected state:

- ground detected at the installed reference geometry -> normal.
- persistent loss of ground -> cliff/fall warning.

Telemetry exposes `reference_height_cm` for UI and traceability.

## 7. Joystick responsiveness

The on-camera drive joystick diameter is exactly 1.20× the prior layout size:

- portrait: 150 dp -> 180 dp.
- landscape: 120 dp -> 144 dp.

The operator touch path now has:

- no software dead-zone;
- no acceleration ramp;
- no reverse delay;
- no steering smoothing;
- parent touch interception blocked while the joystick owns the active pointer;
- latest-command retention so touch updates arriving during an in-flight send are dispatched immediately after it, rather than waiting for the periodic refresh.

The independent firmware 500 ms stale-command neutral stop and E-STOP remain unchanged.

## 8. Controlled files changed

Android:
- `app/src/main/java/com/servoc/app/communication/WifiRoute.java`
- `app/src/main/java/com/servoc/app/auth/ConnectionGate.java`
- `app/src/main/java/com/servoc/app/communication/RobotApi.java`
- `app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java`
- `app/src/main/java/com/servoc/app/control/JoystickView.java`
- `app/src/main/java/com/servoc/app/ui/ControlTab.java`
- `app/src/main/java/com/servoc/app/MainActivity.java`
- `app/src/main/java/com/servoc/app/ui/MeasurementsTab.java`

Main ESP32:
- `firmware/HazRob/HazRob.ino`
- `firmware/HazRob/RemoteLink.cpp`
- `firmware/HazRob/RemoteLink.h`
- `firmware/HazRob/Config.h`
- `firmware/HazRob/Hardware.cpp`

ESP32-CAM:
- `firmware/HazRob-CAM/HazRob-CAM.ino`

Audits:
- `tools/audit_hr061_direct_first_contract.py`
- `tools/audit_0101_motion_contract.py`
- `tools/audit_hr049_control_remote_contract.py`
- `tools/audit_hr050_field_operability_contract.py`
- `tools/audit_hr065_camera_contract.py`

## 9. Acceptance status

Automated source-contract validation: **PASS** on the controlled Railway build. The Direct-first, integration, motion, control/remote, HR050 field-operability, HR065 camera/network, HR047 gas-safety, and firmware-baseline gates all passed.

Android unit tests: **PASS** (`:app:testDebugUnitTest`).  
Android release build: **PASS** (`:app:assembleRelease`).  
Main ESP32 compile: **PASS** with pinned `esp32:esp32@3.3.11`.  
ESP32-CAM compile: **PASS** with pinned `esp32:esp32@3.3.11`.  
APK signature verification: **PASS**, APK Signature Scheme v2, certificate SHA-256 `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

Controlled deployment: Railway deployment `4f15df75-46f7-4269-a91b-d2b025d060cf`, build commit `992954601f9f803292e770a6533f7aa2b0780dc2`, status **SUCCESS**.

Artifacts:
- `HazRob-0.10.1-v25-TEST.apk` — SHA-256 `7db41f6d2b56f39398948054abc5ce6d7f29c2152368ae122bf19fa664adb831`.
- `HazRob-ESP32-Main-0.10.1-HR066.zip` — SHA-256 `512567bf76198a3c186f564eaa982b42da678ad65d1a4bacad63995390dc8f91`.
- `HazRob-ESP32-CAM-0.10.1-HR066.zip` — SHA-256 `0d0d851b1b6acc78273f9696a237dc333cb2ae91136effc5e2b8325476526b83`.

GitHub Actions note: the `ubuntu-latest` job failed before any workflow step was assigned (`runner_id=0`, zero executed steps) on two attempts; therefore this is not recorded as a source/test failure. The controlled Railway build executed the source gates, both firmware compiles, Android unit tests, release build and signing verification successfully.

Physical acceptance remains **PENDING**:
- Main ESP32: flash + field control test.
- ESP32-CAM: flash + stable 5 V test; if the 1 s ON / 1 s OFF flash symptom persists, capture serial/reset evidence and inspect `reset_reason`.
- Direct/Remote takeover, live sensor alarms and IR cliff behavior require final robot-side acceptance after flashing this HR066 firmware.
