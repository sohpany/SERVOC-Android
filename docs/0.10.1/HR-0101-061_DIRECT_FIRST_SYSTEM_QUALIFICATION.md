# HazRob 0.10.1 — HR-0101-061 Direct-First System Qualification

Status: **SOURCE / INTEGRATION QUALIFIED — PHYSICAL FIELD ACCEPTANCE PENDING**

This document records the controlled architecture and qualification evidence for HR-0101-061. It does not claim physical RF, motor, camera, power or environmental acceptance until the actual robot is exercised with the candidate artifacts.

## 1. Authoritative connection lifecycle

| State / action | Main ESP32 radio | Local HTTP | ESP32-CAM local MJPEG | MQTT / HiveMQ | NAPT | Render/WSS | WAN credentials | Next reboot |
|---|---|---|---|---|---|---|---|---|
| BOOT | AP only | Enabled | Enabled after CAM joins HazRob | Off | Off | Off | Retained if previously saved | DIRECT |
| DIRECT | AP only | Enabled | Enabled | Off | Off | Off | Retained | DIRECT |
| WAN PROVISION | AP only | Enabled | Remains enabled | Off | Off | Off | Saved/updated | DIRECT |
| EXPLICIT REMOTE | AP+STA | Local drive rejected | CAM remains on HazRob; remote WSS gated | Enabled when WAN ready | Enabled when Internet ready | Enabled when gateway + CAM time are ready | Reused | DIRECT |
| RETURN DIRECT | AP only | Enabled | Local priority restored | Off | Off | Off | Retained | DIRECT |
| WAN CLEAR | AP only | Enabled | Enabled | Off | Off | Off | Deleted | DIRECT |

**Every power-on/restart starts DIRECT.** Saved SSID/password are configuration data only; they never authorize automatic Remote mode.

## 2. DIRECT authority

Main ESP32 creates SSID `HazRob` at `192.168.4.1` and runs AP-only. Direct control and telemetry use HTTP. MQTT, STA/WAN and NAPT are disabled. Re-selecting Direct while it is already healthy is radio-idempotent and must not recreate or bounce the SoftAP.

Transitioning from Remote to Direct performs motion stop first, disconnects MQTT, disables NAPT, disables WAN transport/STA and preserves the existing HazRob AP association when healthy.

## 3. ESP32-CAM Direct contract

ESP32-CAM is a STA on `HazRob` with canonical static address `192.168.4.2`, gateway `192.168.4.1`, HTTP status on port 80 and MJPEG on port 81. The CAM retries local Wi-Fi association after loss.

Android does not admit Direct merely because the main ESP32 responds. Direct admission requires all of the following:

1. Android acquires and maintains the local-only `HazRob` network.
2. Main ESP32 `192.168.4.1` returns the controlled HazRob 0.10.1 identity.
3. ESP32-CAM `192.168.4.2/status` reports `camera_ok=true`.
4. CAM firmware identity equals `HazRob-CAM 0.10.1` through the shared release version authority.
5. CAM reports canonical IP `192.168.4.2`.
6. Android opens `192.168.4.2:81/stream` and observes one **complete JPEG frame** from SOI `FF D8` through EOI `FF D9` before Direct is declared ready.

After admission the driving workspace uses local MJPEG directly. Local flash/control requests use the CAM HTTP interface. Render, HiveMQ and Internet are not dependencies of this path.

## 4. WAN provisioning authority

WAN configuration is a Direct-only maintenance action. Saving SSID/password does not switch radio profile, does not release Android from HazRob, does not start STA, MQTT, NAPT or Render, and does not interrupt local CAM/drive authority.

Firmware rejects `/remote/provision` outside Direct with `select_direct_first`.

The operator must subsequently choose **Remote / Internet control** explicitly to activate the saved WAN for the current runtime.

## 5. Explicit REMOTE activation

When the operator explicitly chooses Internet/Remote control:

1. Motion is stopped.
2. Main ESP32 changes from AP-only to AP+STA while retaining the HazRob AP.
3. Saved WAN credentials are used for STA association.
4. Internet readiness is established through IP/DNS/time checks.
5. MQTT/TLS connects to HiveMQ for commands, acknowledgements and telemetry.
6. NAPT is enabled only when Internet readiness is true.
7. ESP32-CAM remains locally associated with HazRob and sees gateway readiness from the main ESP32.
8. CAM WSS starts only when the gateway reports `radio_profile=remote`, `internet=true`, `napt=true` **and** the CAM has its own valid NTP clock for TLS certificate-time validation.
9. Android releases its local-only HazRob route and uses normal Internet for Remote MQTT + WSS viewing.

Remote mode is runtime-only. A later reboot returns to Direct even though WAN credentials remain stored.

## 6. Remote disconnect, reconnect and WAN clear

- **Disconnect Remote**: motion stop, MQTT/NAPT/STA off, AP-only Direct, WAN retained.
- **Activate/Reconnect Remote using saved WAN**: explicit operator command; AP+STA and cloud path are rebuilt for this runtime only.
- **Clear WAN**: destructive operation with confirmation; deletes SSID/password and remains Direct.

## 7. Steering / 4WD differential-drive authority

The application uses one normalized arcade/differential mixer:

`Left = Y + X`

`Right = Y - X`

The pair is normalized/clamped to `[-100,+100]`. Speed limit authority remains 100% for the requested operating profile.

Required behavior:

| Joystick state | Left | Right | Vehicle behavior |
|---|---:|---:|---|
| Forward | +100 | +100 | Straight forward |
| Reverse | -100 | -100 | Straight reverse |
| Pure right | +100 | -100 | Pivot/yaw right |
| Pure left | -100 | +100 | Pivot/yaw left |
| Forward-right example | +100 | +50 | Right side slower; forward arc right |
| Forward-left example | +50 | +100 | Left side slower; forward arc left |
| Reverse-right example | -50 | -100 | Nose/camera yaws right while reversing |
| Reverse-left example | -100 | -50 | Nose/camera yaws left while reversing |

The application UI uses the shared `DriveMixer`; JUnit and release audits cover the forward, reverse, diagonal, pivot, normalization and neutral cases.

## 8. Safety timing

- Direct keepalive: 250 ms
- Remote drive pump: 40 ms
- Remote hold refresh: 120 ms
- Firmware drive watchdog: 500 ms
- Remote command TTL: 900 ms

Motion stop is required before profile transitions. Android background/service teardown uses safe-stop and neutral re-arm behavior.

## 9. Cloud/service coherence

Controlled identity:

- Version: `0.10.1`
- Protocol: `2`
- Robot ID: `hazrob-001`
- MQTT: TLS/8883 on the controlled HiveMQ host
- Render video relay: controlled HazRob relay host
- Bootstrap: `/hazrob/bootstrap`
- Video publish/view roles are isolated

Role-scoped bootstrap remains strict for `android`, `esp32`, and `esp32-cam`. Control readiness and video readiness are independent so a video fault does not falsely validate or invalidate MQTT command authority.

## 10. Preliminary qualification evidence before documentation commit

Pre-documentation executable head: `dbff1906cfa5ce40f785a0b9ae930991b31ad8cc`.

The following completed successfully on that controlled source state:

- HR-0101-061 Direct-first connection contract: PASS
- HR-0101-059/061 five-layer system coherence: PASS
- HR048/060 steering matrix and semantics: PASS
- HR049/060 control/remote integration: PASS
- HR050 field-operability contract: PASS
- HR051/061 local Wi-Fi / camera / ENV / Render / Hive / app coherence: PASS
- Gas-safety / R0 consolidated release gates: PASS
- Android `testDebugUnitTest`: PASS
- Android `assembleRelease`: PASS
- APK signer verification: PASS
- Main ESP32 compile: PASS — 1,122,273 bytes program (35%), 50,692 bytes globals (15%)
- ESP32-CAM compile: PASS — 1,157,867 bytes program (36%), 68,244 bytes globals (20%)

Known compiler warnings at this stage are non-blocking technical debt already present in controlled paths: CA-string optimizer address warning, unused internal I2C helpers, and deprecated volatile increment/assignment warnings in CAM counters. They do not replace field testing.

## 11. Final exact-head qualification requirement

Because this documentation commit changes the repository head, the release is not final merely from the evidence above. The final candidate must be re-qualified from one exact Git SHA through:

1. consolidated Release Gate;
2. Android unit tests + signed release build;
3. main ESP32 compile;
4. ESP32-CAM compile;
5. relay local contract;
6. production Render deployment from the same SHA;
7. live Render/HiveMQ/bootstrap contract after production deployment;
8. artifact build-info and SHA-256 verification.

Only artifacts produced by that exact final SHA may be delivered as the HR061 field-test candidate.

## 12. Physical acceptance still required

Software/cloud qualification cannot prove RF coexistence, power integrity, actual wheel polarity, motor-driver current behavior, camera sensor stability or real WAN loss/recovery on the assembled robot. Final field acceptance therefore remains pending and must include at minimum:

- cold boot confirms Direct with saved WAN present;
- phone joins HazRob;
- CAM joins HazRob and Direct admission waits for a complete local JPEG;
- simultaneous local MJPEG + joystick drive + turn/pivot + E-stop;
- forward/reverse right/left steering matrix on real wheels;
- WAN save while Direct without local drop;
- explicit Remote transition;
- simultaneous MQTT control + WSS video;
- return Direct without deleting WAN;
- reboot returns Direct;
- WAN clear behavior;
- WAN loss and recovery during explicit Remote;
- sensor/relay/measurement/alert functions under both authorized transports where applicable.

Until those physical checks pass, HR061 is a **field-test candidate**, not a fully field-accepted release.
