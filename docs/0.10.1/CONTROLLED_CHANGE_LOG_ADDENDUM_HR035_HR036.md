# HazRob 0.10.1 — Controlled Change Log Addendum

This addendum is part of the controlled 0.10.1 integration record and records HR-0101-035 and HR-0101-036. It supplements `CONTROLLED_CHANGE_LOG.md` without changing the historical wording of prior entries.

## HR-0101-035 — Permanent firmware-footprint and canonical camera-contract optimization

### Reason
Reduce actual ESP32/ESP32-CAM Flash footprint without deleting required HazRob 0.10.1 functions or weakening the Direct/Remote transport contracts. Source minification alone is not counted as a Flash optimization because comments and whitespace are removed by compilation and do not materially reduce the binary.

### Scope
Android camera client/operator presentation, ESP32-CAM local HTTP server, camera documentation and exact-source build verification. Main ESP32 GPIO, drive mapping, Direct HTTP command semantics, HiveMQ topics/TLS, Render role-scoped bootstrap, and safety contracts remain unchanged.

### Permanent source changes
- Android Settings now states the controlled 100% default drive scale and the authoritative L298N six-wire mapping: ENA=GPIO16, ENB=GPIO17, IN1/IN2=GPIO18/27, IN3/IN4=GPIO19/32, with ENA/ENB jumpers removed for PWM control.
- Android `CameraApi` uses only the canonical 0.10.1 local camera routes: `/status`, port-81 `/stream`, port-80 `/stream` redirect and `/flash?mode=off|auto|on`.
- Retired camera compatibility probes `/led`, `/control`, `/mjpeg`, bare port-81 `/`, and `/flash?on=` were removed from Android source rather than retained as fallback paths.
- `ControlTab` local video startup now tries only the two canonical stream URLs.
- ESP32-CAM no longer links Arduino `WebServer`; its port-80 control/status plane is implemented with a compact raw `WiFiServer` parser while port 81 remains the MJPEG server.
- ESP32-CAM retains `/`, `/status`, `/stream` redirect and `/flash?mode=`; retired `/led` and `/control` routes were removed.
- Camera initialization still prefers native JPEG and preserves the RGB565 + `frame2jpg_cb` fallback. No camera GPIO or frame dimensions changed.
- Local MJPEG, Remote WSS publishing, role-scoped Render bootstrap, SNTP/TLS gate, flash GPIO4 and PSRAM JPEG buffer remain present.
- The trusted MQTT root-CA source blob is unchanged in the accepted baseline. A transient attempted compile-warning cleanup was detected before release verification and reverted exactly; no accepted build or release artifact was produced from the transient altered CA text.

### Implementation commits
- `c3454ce3b0c8b6ef512b96174c07c7fdb01540ca` — align Android Settings with 0.10.1 drive contract.
- `1bb1cc75d0f6abf4b651bde6f3135381e4752640` — canonical Android camera endpoints.
- `b6b19084e331da2b051eec48f02a8c2cf8e9180c` — replace ESP32-CAM `WebServer` with compact HTTP server.
- `5715520e77e806c290410bbef3f8eef9e86e518b` — restore the trusted MQTT CA blob exactly after the rejected warning-cleanup experiment.
- `4dad3490a63c5f91467d89b798b9304271db3c5b` — remove retired local stream fallbacks from `ControlTab`.
- `f7a9fe6a33ef3b0048613ec033be384f05ef580f` — align ESP32-CAM documentation with the canonical compact contract.

### Exact-source verification baseline
Baseline: `f7a9fe6a33ef3b0048613ec033be384f05ef580f`.

Android independent builder:
- `:app:testDebugUnitTest` PASS.
- `:app:assembleRelease` PASS.
- APK signer continuity PASS; certificate SHA-256 `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- APK SHA-256 `caef147fbad899d48a4337e05594b920230a94721df16113cf7b76a24fa34d71`.

Firmware independent builder:
- cross-component integration audit executed before compilation and PASSed.
- Main ESP32 compile PASS: 1,083,613 bytes program (34%), 50,148 bytes global RAM (15%).
- ESP32-CAM compile PASS: 1,118,771 bytes program (35%), 67,836 bytes global RAM (20%).
- Previous accepted ESP32-CAM baseline was 1,155,611 bytes program and 68,220 bytes RAM.
- Net ESP32-CAM program reduction: 36,840 bytes, approximately 3.19%, while retaining native-JPEG plus RGB565/software-JPEG fallback and both local and Remote video paths.
- Main ESP32 program size is intentionally unchanged. Its large binary components are functional Wi-Fi/TLS/MQTT/analytical dependencies; no functionality is removed merely to reduce the displayed size.

Non-blocking warnings remain for certificate-string presence checks and C++ volatile increment deprecation. They do not prevent compilation and are not treated as footprint gains by deleting behavior.

### Status
**SOURCE/INDEPENDENT BUILD PASS / LOCAL CAMERA + REMOTE CAMERA FIELD TEST PENDING**.

---

## HR-0101-036 — Android / ESP32 / ESP32-CAM / Render / HiveMQ consistency audit

### Purpose
Prove that the permanent 0.10.1 motion and footprint changes remain mutually compatible across every live transport component rather than validating each component in isolation.

### Source/contract consistency — PASS
- Release identity: Android, main ESP32, ESP32-CAM and Render relay use HazRob 0.10.1 contracts; Render bootstrap protocol is `2`.
- Robot identity: `hazrob-001` is shared by Android, main ESP32, ESP32-CAM and Render role-scoped bootstrap/video paths.
- Direct network: main ESP32 is `192.168.4.1`; ESP32-CAM is `192.168.4.2`; Android Direct admission and camera clients use those canonical addresses.
- Drive GPIO: main firmware and Android hardware profile remain ENA16/IN1-18/IN2-27 and ENB17/IN3-19/IN4-32. HR-0101-035 changes no GPIO.
- Direct drive: Android `/drive?l=&r=` maps to main ESP32 `/drive`; accepted drive commands reach direct PWM output with the HR-0101-034 1500 ms stale-command boundary and no acceleration/retrace layer.
- Remote drive: Android publishes `hazrob/hazrob-001/cmd`; main ESP32 subscribes to the same root and publishes `ack`, `telemetry` and `status`. Command freshness retains session/sequence/nonce/TTL validation.
- MQTT coordinates: Android and main ESP32 use the same HiveMQ host and TLS port 8883.
- Bootstrap coordinates: Android, main ESP32 and ESP32-CAM use `hazrob-video-relay.onrender.com` and `/hazrob/bootstrap`, and require service version `0.10.1` / protocol `2`.
- Role separation: Android receives operator MQTT credentials plus the video VIEW credential; main ESP32 receives robot MQTT credentials; ESP32-CAM receives the video PUBLISH credential.
- Local camera: Android canonical stream/status/flash routes match the compact ESP32-CAM HTTP implementation.
- Remote camera: ESP32-CAM publishes binary JPEG over authenticated WSS to Render; Android views through the role-scoped WSS viewer endpoint; Render remains the Remote camera-status authority.
- Render is not in the robot drive data plane; Remote drive/control goes Android <-> HiveMQ <-> main ESP32. Render provides bootstrap and video relay/status only.

### Automated build/contract verification — PASS
On exact source `f7a9fe6a33ef3b0048613ec033be384f05ef580f`:
- Android unit tests PASS.
- Android release build/signing PASS.
- Main ESP32 compile PASS.
- ESP32-CAM compile PASS.
- cross-component integration audit PASS.
- Render relay `npm run check` PASS.
- Render relay `npm test` reported `HazRob 0.10.1 relay contract: PASS`.
- Relay dependency audit reported zero npm vulnerabilities in that verification build.

### Production Render alignment — PASS
- Canonical production service: `hazrob-video-relay` (`srv-dad3p8gn74is73d52aj0`).
- Production branch is now `dev/hazrob-0.10.1`; therefore the older HR-0101-014/029 finding that production was bound to `dev/hazrob-0.9` is superseded.
- The current production relay runtime is `hazrob-video-relay 0.10.1`.
- Production relay source under `remote/video-relay` has not changed between production source commit `144dc0aeb17470cfe3298ec578423888e6318b52` and verification baseline `f7a9fe6a33ef3b0048613ec033be384f05ef580f`; therefore the newer Android/firmware-only commits do not require a relay-code redeploy for source parity.

### HiveMQ live evidence — PARTIAL PASS
Production startup diagnostics show both stored application and robot identities successfully negotiate TLS/MQTT with HiveMQ and receive `CONNACK=0`. This proves broker reachability, TLS trust and credential authentication for both identities.

This does **not** by itself prove every administrative topic ACL or a physical Android -> broker -> powered robot command/ACK round-trip. Those remain live E2E field gates.

### Explicitly deferred / not claimed
- Physical wheel response, rapid direction reversal current/thermal behavior and measured Direct/Remote latency remain field tests under HR-0101-034.
- Physical ESP32-CAM local stream, flash and live Remote publisher/viewer verification remain field tests after HR-0101-035.
- HiveMQ topic-ACL enforcement and live command/ACK/telemetry round-trip require the powered robot and are not inferred from CONNECT acceptance.
- MQ relay electrical operation is explicitly deferred pending GPIO25/relay measurements and is not used to judge this audit.
- GitHub Actions runner-allocation blocker HR-0101-013 remains an infrastructure issue; independent builders provide the compile/test evidence above.

### Overall result
**STATIC CROSS-SYSTEM CONSISTENCY PASS / INDEPENDENT BUILD AND RENDER CONTRACT PASS / PRODUCTION RENDER ALIGNMENT PASS / HIVEMQ AUTHENTICATION PASS / PHYSICAL DIRECT+REMOTE E2E FIELD VALIDATION PENDING**.
