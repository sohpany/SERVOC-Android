# HR-0101-054 — Local Wi-Fi Stability Remediation

Status: **SOFTWARE / RELEASE-GATE / APK / FIRMWARE BUILD PASS — PHYSICAL FIELD ACCEPTANCE PENDING**  
Version: HazRob 0.10.1 / Android versionCode 21.  
Validated executable artifact source: `480cf7a4f2b0dc0b4eea12f073706c8dce33ef09`.

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

## Software validation — artifact source `480cf7a4f2b0dc0b4eea12f073706c8dce33ef09`
- Cross-component integration contract: PASS.
- `HR-0101-054 local-WiFi/camera/ENV/Render/Hive/app coherence contract`: PASS.
- Consolidated HR047 gas-safety integration gate: PASS.
- Android unit tests: PASS.
- Android release build: PASS and LIVE.
- Android APK SHA-256: `333eb068dcf5eca17b9e35218c4fcd552c3b363a458c83777f95e5cba65f5c87`.
- Android signing certificate SHA-256 remains `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- ESP32 Arduino platform: 3.3.11.
- Main ESP32 compile: PASS; program 1,099,101 bytes (34%); globals 50,764 bytes (15%), 276,916 bytes remaining.
- ESP32-CAM compile: PASS; program 1,140,753 bytes (36%); globals 67,904 bytes (20%), 259,776 bytes remaining.
- Firmware artifact service: LIVE.

Warnings reported by the compiler are pre-existing warning classes (`ROOT_CA` address comparison, unused helper functions, and volatile increment deprecations) and are not compile failures. HR054 introduced no new build-stopping error.

## Physical field acceptance still required
Physical field acceptance remains mandatory and must be performed with the robot mechanically safe (wheels lifted or drive power isolated initially):
1. Install the HR054 APK and flash the HR054 main ESP32 and ESP32-CAM artifacts built from the validated source commit above.
2. Connect the phone directly to `HazRob`, open Developer → Connection, and use **مسح WAN والعودة لشبكة محلية ثابتة**. This intentionally removes saved external-WAN settings for the isolation test.
3. Reboot the main ESP32 and ESP32-CAM. Confirm the phone remains associated to HazRob and direct diagnostics remain reachable.
4. Confirm ESP32-CAM associates to HazRob and local `/status` plus MJPEG `192.168.4.2:81/stream` are reachable.
5. Confirm direct drive commands are received consistently with the wheels lifted. Inspect UDP packet counters before placing the robot on the floor.
6. Only after local camera and driving pass, provision an available WAN and verify remote MQTT control and Render video.
7. With an unavailable WAN, verify the robot stops after the bounded attempts and reports `wifi_local_safe`, instead of running the former indefinite seven-second reconnect loop.

If direct camera and driving still fail while WAN is cleared and no reconnect loop is active, HR054 is not sufficient. The next controlled diagnostic shall separate radio modes explicitly: Direct=`WIFI_AP` only; Remote=`WIFI_AP_STA`. This escalation is intentionally deferred until the field isolation test proves AP+STA itself, rather than the retry churn, is the remaining cause.

Build results and physical results are separate authorities. The package is software/build accepted, not physically field accepted.
