# HazRob 0.9.0 — Architecture & Execution Contract

Status: implementation baseline for `dev/hazrob-0.9`.

## 1. Product navigation

User tabs: Drive | Measurements | Status | Settings.
Developer adds: Experiments & Calibration.

Measurements contains: Physical | Live Measurement | Analysis.
Reports/export live at the bottom of Analysis; there is no standalone Reports tab.

All operational pages use compact UI only. Footer on every page:
`HazRob 0.9.0 • تصميم المستشار/ شهاب الصهباني.`

## 2. API 4 safety separation

The firmware MUST keep these actions independent:
- motion stop: stop motors only.
- measurement cancel: cancel averaging/session only.
- sensor power off: switch MQ relays off only.
- emergency stop: stop motion, cancel measurement and switch MQ relays off.

Android Activity lifecycle (`onPause`, file picker, tab changes) MUST NOT switch MQ power off.

Motor command timeout remains a motion-safety event. MQ power is governed by explicit user command or confirmed communication-loss policy, never Activity lifecycle.

## 3. Hardware source of truth

`HazRobHardwareProfile` remains the single application source of truth:
- R1 / ADS1 0x48: MQ2 A0, MQ4 A1, MQ5 A2, MQ8 A3.
- R2 / ADS2 0x49: MQ3 A0, MQ7 A1, MQ9 A2, MQ135 A3.
- R3/R4 reserved.

Sensor-health logic must derive installed channels and relay groups from this profile; no duplicate hard-coded topology is allowed.

## 4. ADS/I2C behavior

ADS detection is recoverable. Missing ADS devices must be re-probed periodically without rebooting the ESP32. `/diag` reports live device presence.

## 5. MQ live measurement UX

Relay controls are available in the Live Measurement screen:
`OFF | R1 | R2 | R1+R2`.

Only powered/installed sensors are promoted as active cards. Each card displays live acquisition fields where available: Raw, Vadc, Vao, Rs, Rs/R0, PPM only when a valid quantitative model exists.

Each powered sensor has an independent warm-up state and timer. UI state: WARMING (gray) -> READY (blue). Timers survive tab changes/background because they derive from device power state/session timestamps, not View lifecycle.

## 6. Instant warning vs chemical analysis

Instant warning is a fast sensor-response warning, not final chemical identification.

Card severity: normal, elevated, high. High state may pulse red while the numeric values remain readable.

User-facing wording is compatibility language, e.g. `استجابة مرتفعة متوافقة مع أول أكسيد الكربون`, not proof of identity from one MQ channel.

Thresholding is profile/calibration based; do not use one global raw ADC threshold.

## 7. Calibration policy

All installed field sensors are treated as calibrated in HazRob 0.9. Calibration values/profile metadata must be persisted and loaded from one application calibration repository. UI must separate:
- electrical signal validity,
- warm-up readiness,
- calibration availability,
- quantitative-model availability.

Do not mark a valid electrical channel invalid merely because a PPM model is unavailable.

## 8. Session engine & analysis

A measurement session records time-series telemetry, not only a final snapshot. Session output supports averaging, trend plots and later re-analysis.

Analysis ranks candidate compounds using evidence strength, confidence, concentration when valid, and hazard context. Results support one active tracking target at a time. Tracking state is shared between Analysis and Drive.

## 9. Reports

PDF/XLSX/Raw export is launched from Analysis. Reports contain charts derived from recorded session data (MQ response/Rs-R0/PPM when valid/environment trends/candidate confidence). Export generation must not depend on Activity staying in foreground.

XLSX is generated to a completed temporary artifact first, validated/closed, then copied to the user-selected URI with truncation semantics.

Legacy records are version-aware; absent new fields must be shown as `unknown/not recorded`, never inferred as hardware failure.

## 10. Remote connectivity

Local transport remains supported.

Master ESP32: Wi-Fi AP+STA.
- AP keeps local HazRob control/fallback.
- STA connects to 4G modem/router for WAN.

Remote control/telemetry transport: MQTT over TLS.
Remote camera transport: JPEG over secure WebSocket (WSS) for 0.9, retaining local MJPEG.

Android transport abstraction:
- `DirectHttpTransport`
- `RemoteMqttTransport`
- `DirectMjpegTransport`
- `RemoteWssJpegTransport`

## 11. Remote entry/provisioning UX

Entry screen presents two operational choices:
- Direct Wi-Fi control.
- Internet control.

First remote setup is local provisioning. The user enters modem SSID/password while locally connected to HazRob. Credentials are sent only to the robot locally and stored on-device; they are not sent to the cloud.

Subsequent Internet entry does not ask for modem credentials unless the user explicitly changes the WAN profile.

Remote readiness progress is stage-based, not timer-based:
1. local/provisioning link when required,
2. Wi-Fi association,
3. DHCP/IP,
4. DNS,
5. time synchronization,
6. TLS,
7. MQTT authentication/subscription,
8. application nonce round-trip ACK,
9. telemetry heartbeat,
10. camera relay/first JPEG (degraded-mode optional).

Control entry requires command round-trip and telemetry. Camera failure may enter `REMOTE READY — VIDEO OFFLINE` rather than blocking control.

## 12. Communication-loss policy

Activity lifecycle is not communication loss.

A real link watchdog/heartbeat determines loss. Motion safety reacts quickly. MQ shutdown is tied to confirmed communication-loss timeout or explicit operator command, not to `onPause()`.

Transport failover must stop motion, establish/verify the new transport/session, then permit new drive commands. Old motion commands are never replayed.

## 13. Remote security baseline

- TLS for remote control.
- unique robot/device credentials.
- ACL-scoped MQTT topics.
- motion commands: non-retained, sequence + TTL.
- explicit remote-enable/provisioning state.
- provisioning secret/PIN or equivalent local authorization.
- no Wi-Fi password in logs/cloud payloads.

## 14. Version target

Product version: HazRob 0.9.0.
Protocol/API target: API 4.
Implementation branch: `dev/hazrob-0.9`.
