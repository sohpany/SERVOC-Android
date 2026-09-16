# HR-0101-060 — Direct Wi-Fi and Steering Qualification

Date: 2026-09-16
Release identity: HazRob 0.10.1 / protocol 2 / robot `hazrob-001`

## Purpose

HR060 qualifies the corrected local Direct connection path and the operator steering mixer as one integrated change. It was opened after field observation that Direct connection could fail together with local camera and drive control. The review identified a stale Android Wi-Fi route / AP transition interaction and then tightened both Android and ESP32 Direct behavior. Steering was re-qualified at the same time so the next field build would not combine an unverified connection change with an unverified motion mapping.

## Direct connection authority

Direct mode is a true local-only radio authority:

- Main ESP32: `WIFI_AP` only.
- Robot AP: `HazRob`, main IP `192.168.4.1`.
- ESP32-CAM local IP: `192.168.4.2`.
- Direct drive transport: HTTP.
- Direct camera transport: local MJPEG.
- MQTT disconnected in Direct.
- NAPT disabled in Direct.
- STA/WAN runtime transport disabled in Direct while saved WAN credentials are retained unless the operator explicitly performs WAN Clear.
- Repeated Direct requests are radio-idempotent and must not bounce a healthy HazRob SoftAP.

Android Direct entry now checks the current radio profile before requesting a transition. If a real Remote→Direct transition is needed, Android releases the stale local NetworkRequest, recreates the Wi-Fi request and waits for HazRob to reappear before accepting the Direct session. `WifiRoute` also recreates its request after `onLost` / unavailable state instead of retaining a dead callback indefinitely.

## Steering authority

The on-screen joystick and its tests share the same pure `DriveMixer` implementation.

Coordinate meaning:

- `+Y` = forward.
- `-Y` = reverse.
- `+X` = yaw robot nose/camera to the right.
- `-X` = yaw robot nose/camera to the left.

Mixer:

`left = Y + X`

`right = Y - X`

The outputs are normalized to `-100..+100` without changing the requested curvature. Operator speed default remains 100%.

Required steering matrix used by the fail-closed tests:

| Operator command | Left | Right | Expected motion |
| --- | ---: | ---: | --- |
| Forward | +100 | +100 | Straight forward |
| Reverse | -100 | -100 | Straight reverse |
| Forward + right | +100 | +50 | Right arc; right side slower |
| Forward + left | +50 | +100 | Left arc; left side slower |
| Reverse + right | -50 | -100 | Nose/camera yaws right while reversing |
| Reverse + left | -100 | -50 | Nose/camera yaws left while reversing |
| Pure right | +100 | -100 | Pivot right; sides counter-rotate |
| Pure left | -100 | +100 | Pivot left; sides counter-rotate |

Firmware hardware mapping remains:

- logical LEFT → L298N channel B: ENB GPIO17, IN3 GPIO19, IN4 GPIO32.
- logical RIGHT → L298N channel A: ENA GPIO16, IN1 GPIO18, IN2 GPIO27.
- no speculative software inversion.

## Integrated test evidence before final packaging

The following were required to pass on one Git commit before user delivery:

1. Five-layer system coherence gate: Android + ESP32 + ESP32-CAM + Render + HiveMQ.
2. HR060 connection contract: Direct AP-only, idempotent AP lock, Android stale-route recovery, non-destructive Remote disconnect/reconnect, WAN Clear as the only destructive opt-out.
3. HR048/060 steering gate and JUnit `DriveMixerTest`, including forward/reverse diagonals and pure pivots.
4. HR049/060 control/remote gate: Direct HTTP 250 ms keepalive; Remote latest-command max 25 Hz / 120 ms hold refresh; firmware watchdog 500 ms; command TTL 900 ms.
5. HR050 field-operability gate and HR051/060 camera recovery gate.
6. Android `testDebugUnitTest`, `assembleRelease`, APK signing verification.
7. ESP32 and ESP32-CAM compile under ESP32 Arduino core 3.3.11.
8. Relay local contract plus live Render/HiveMQ/bootstrap contract for `android`, `esp32`, and `esp32-cam` roles.
9. Production Render relay deployed from the same final Git head before artifact release.

## Qualification boundary

These checks are software/source/build/cloud qualification. They do not substitute for physical acceptance on the assembled robot. Final field acceptance still requires real-hardware verification of:

- Android approval and retention of HazRob local Wi-Fi on the actual phone.
- Direct drive and local MJPEG running simultaneously.
- LEFT/RIGHT wiring and actual motor polarity matching the qualified logical mapping.
- forward/right, forward/left, reverse/right, reverse/left and both pivot directions with wheels safely lifted first.
- E-stop, stale-command stop and neutral re-arm.
- Remote MQTT control and WSS video together.
- WAN loss/recovery and Direct/Remote switching without AP instability.

No software gate is allowed to claim physical acceptance until those tests are performed on the actual HazRob hardware.
