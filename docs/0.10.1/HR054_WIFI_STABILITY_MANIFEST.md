# HR-0101-054 — Local Wi-Fi Stability Remediation

Status: **SOURCE CHANGE / BUILD VALIDATION PENDING — PHYSICAL FIELD ACCEPTANCE PENDING**  
Version: HazRob 0.10.1 / Android versionCode 21.

## Field trigger
On 2026-09-15 the operator reported that both direct robot driving and ESP32-CAM video failed in the same field test. Because both functions depend on the main ESP32 `HazRob` SoftAP, the shared Wi-Fi layer became the primary common-cause suspect.

## Root-cause evidence
The last local-only network baseline before remote WAN support used `WIFI_AP` with main ESP32 `192.168.4.1` and ESP32-CAM reservation `192.168.4.2`. Commit `9844e9fa20a50ccd4104b7bbb3bbd0b8fb127751` changed the main ESP32 to `WIFI_AP_STA` so one radio could host HazRob while also associating to an external WAN.

Before HR054, `RemoteLink.cpp` explicitly executed a `WiFi.disconnect()` / `WiFi.begin()` cycle whenever configured WAN was disconnected and seven seconds had elapsed. If saved WAN credentials were unavailable or unstable, that loop could continue indefinitely. In AP+STA mode, STA association/scanning and channel changes share the same 2.4 GHz radio as the HazRob SoftAP, so the repeated retry policy could disturb both the operator phone and ESP32-CAM at the same time.

The prioritized UDP direct-drive receiver added later under HR048 did not introduce AP+STA. HR054 therefore retains UDP and fixes the common radio policy instead of rolling back low-latency driving.

## Controlled remediation
1. WAN association attempts are bounded to two explicit attempts.
2. ESP32 automatic STA reconnect is disabled; reconnect ownership stays with HazRob firmware.
3. After failed bounded attempts the firmware enters `wifi_local_safe` and stops new background WAN attempts.
4. `remoteProvision()` and explicit `remoteRetry()` can deliberately restart WAN association.
5. `remoteClear()` remains available and now has an operator-visible developer control: **مسح WAN والعودة لشبكة محلية ثابتة**.
6. `/remote/status` exposes `retry_hold`, `connect_attempts`, `ap_clients`, and `wifi_channel` for field diagnosis.
7. Robot-side Wi-Fi discovery is rejected while recent UDP drive traffic is active, preventing an intentional scan from competing with active direct driving.
8. WAN provision/retry/clear paths call `motionStop()` before changing radio state.

## Invariants retained
- HazRob SoftAP remains `192.168.4.1/24`.
- ESP32-CAM local address remains `192.168.4.2`.
- Direct UDP drive remains port 4210 protocol version 1 with HTTP fallback.
- Remote MQTT/HiveMQ control contract remains unchanged.
- Render WSS video contract remains unchanged.
- HR053 camera baseline and local/remote capture arbitration remain unchanged.
- GPIO, L298N, ADS, MQ/R0, ENV, and M4 quantitative-gate authorities are unchanged.

## Required validation
Software/build acceptance requires the consolidated release gate, Android unit/release build, main ESP32 compile, and ESP32-CAM compile to pass from one executable source commit.

Physical field acceptance remains mandatory and must be performed with the robot mechanically safe (wheels lifted or drive power isolated initially):
1. Boot with an unavailable saved WAN and confirm retries stop at the bounded limit and status enters `wifi_local_safe`.
2. Confirm the phone remains associated to HazRob and direct diagnostics remain reachable.
3. Confirm ESP32-CAM associates to HazRob and local MJPEG is reachable.
4. Confirm direct drive commands are received consistently after safe bench verification.
5. Provision an available WAN and verify remote MQTT control and Render video still become ready.
6. Verify WAN loss eventually enters local-safe hold rather than producing an indefinite seven-second reconnect loop.

Build results and physical results must be recorded separately. No build result is a substitute for the operator field test.
