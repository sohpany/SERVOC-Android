# HR-0101-053 — Camera Baseline Restore + ENV / Render / Hive / App Coherence Release

Status: **SOFTWARE / RELEASE-GATE / APK / FIRMWARE BUILD PASS — PHYSICAL FIELD ACCEPTANCE PENDING**  
Version: HazRob 0.10.1 / Android versionCode 21.  
Validated artifact source commit: `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.

## Trigger
Field test on 2026-09-15 reported two independent symptoms after recent controlled changes:
1. ESP32-CAM direct/local video no longer streamed although power was confirmed good.
2. Directly wired BME280/BMP280 environmental pressure did not report a valid pressure reading.

## Camera authority
Per operator instruction, direct ESP32-CAM behavior is restored to the last field-working local baseline from HazRob-CAM 0.10.0 while retaining the 0.10.1 identity and the current RemoteVideo component:
- AI-Thinker pin map unchanged.
- STA SSID `HazRob`; static IP `192.168.4.2/24`; gateway `192.168.4.1`.
- `WebServer` on port 80.
- `WiFiServer` MJPEG on port 81.
- QVGA 320x240 capture.
- `PIXFORMAT_RGB565` capture with software `frame2jpg_cb` JPEG conversion.
- JPEG conversion quality JQ=35.
- one framebuffer, `CAMERA_GRAB_WHEN_EMPTY`, framebuffer and JPEG buffer in PSRAM.
- direct stream endpoint `http://192.168.4.2:81/stream` and port-80 `/stream` redirect.
- local flash compatibility endpoints retained.
- no native-JPEG-first path and no DRAM fallback are authoritative under HR053.

HR053 supersedes the experimental local-camera recovery behavior introduced under HR051/HR052 where it conflicts with this restored baseline.

### Local / remote capture arbitration
Commit `7a09ff5354c22d98ff3588bd114b590969adcd43` closes the one integration gap found during cross-system review. When a local MJPEG client is active, `streamTask()` now calls `remoteVideoSetLocalActive(true)` and clears it on every local-stream exit path. `RemoteVideo.cpp` pauses remote capture while this flag is active. This prevents local MJPEG and Render WSS publishing from competing for the shared camera capture path and preserves local-stream priority.

## Render / HiveMQ / Android remote-video authority
The current software contract was reviewed end-to-end and is enforced by the HR053 audit:
- ESP32-CAM Render host: `hazrob-video-relay.onrender.com`, TLS port 443.
- video base path: `/hazrob/video`.
- robot ID: `hazrob-001`.
- camera publisher path: `/hazrob/video/publish/hazrob-001`.
- Android viewer path: `wss://hazrob-video-relay.onrender.com/hazrob/video/view/hazrob-001`.
- bootstrap endpoint: `https://hazrob-video-relay.onrender.com/hazrob/bootstrap`.
- ESP32-CAM bootstrap role is `esp32-cam` and receives only the video publish token.
- Android bootstrap role is `android` and receives the MQTT application credentials plus the video view token.
- publisher and viewer tokens are role-separated by the relay.
- Android remote video requires WSS and hostname-verified TLS.
- control readiness intentionally remains independent of video readiness.

HiveMQ is the remote control / ACK / telemetry / status transport, not the JPEG transport. The authoritative broker remains `e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud:8883`; Render WSS carries the video frames. The relay's recorded ACL diagnostic passed application and robot TLS/MQTT connection, subscriptions, command round-trip, ACK, telemetry and retained-status restoration. The `remote/video-relay` implementation used by the live Render service is unchanged by the local-camera restoration.

## ENV / pressure authority
The environmental sensor is directly wired to the main ESP32 and remains intentionally isolated from the analytical ADS bus:
- ENV: SDA GPIO13, SCL GPIO14, 100 kHz, ESP32 second hardware I2C controller `TwoWire(1)`.
- ADS1115 #1/#2 remain SDA GPIO21 / SCL GPIO22, 400 kHz.
- BME280: chip ID 0x60; BMP280: supported IDs 0x56/0x57/0x58.
- probe addresses: 0x76 and 0x77.
- firmware raw telemetry keys remain `t`, `h`, `p`, `type`, `addr`, `chip_id`, `status`, ACK flags and read-failure metadata.
- Android `RobotProtocolV4` remains the normalization authority to `temperature`, `humidity`, `pressure`, `type`, `address`, `chip_id`, `status_code`.
- Settings UI displays `status_code`, sensor type, address, chip ID and ACK state instead of hiding failures behind `NOT READY`.

No software claim is made that a physically connected module has ACKed until field telemetry proves it. A valid software/build result is separate from physical acceptance.

## Cross-system invariants retained
- HazRob 0.10.1 / API 4 / hardware profile `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2`.
- L298N authority unchanged: LEFT=B EN17/IN19/IN32; RIGHT=A EN16/IN18/IN27.
- accepted R0 campaign remains `FIELD-R0-3SESSION-20260913`.
- M4 quantitative gate remains fail-closed.
- ADS acquisition authority remains unchanged.
- cliff/IR semantics remain unchanged.

## Validation record — artifact source `43671aac5837b1f508dc9a6f7e6aeb38223b950d`

### Release gate
- Cross-component integration contract: PASS.
- HR048: PASS.
- HR049: PASS.
- HR050: PASS.
- `HR-0101-053 camera/ENV/Render/Hive/app coherence contract`: PASS.
- HR047 consolidated gas-safety integration gate: PASS.
- accepted R0 authority synchronized.

The historical aggregator filename still invokes `audit_hr051_camera_contract.py`; its current contents are the HR053 compatibility gate and explicitly enforce local-camera baseline, local/remote arbitration, Render host/routes/bootstrap, HiveMQ broker/robot identity, Android WSS viewer path and ENV coherence. The historical filename does not restore superseded HR051 behavior.

### Android
- source commit: `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.
- `:app:testDebugUnitTest`: PASS.
- `:app:assembleRelease`: PASS.
- lintVital: PASS.
- flammability seed SHA-256 verified: `457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`.
- APK SHA-256: `b6ec45844737e1b14c4f2775841917d959b8c35a31821e0476e0d458f53894dd`.
- signing certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

### ESP32 main firmware
- source commit: `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.
- ESP32 Arduino platform actually installed by builder: 3.3.11.
- FQBN: `esp32:esp32:esp32:PartitionScheme=huge_app`.
- compile: PASS.
- program storage: 1,097,621 bytes (34%).
- dynamic globals: 50,764 bytes (15%); 276,916 bytes remain for local variables.

### ESP32-CAM firmware
- source commit: `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.
- ESP32 Arduino platform actually installed by builder: 3.3.11.
- FQBN: `esp32:esp32:esp32cam`.
- compile: PASS.
- program storage: 1,140,753 bytes (36%).
- dynamic globals: 67,904 bytes (20%); 259,776 bytes remain for local variables.
- the 36-byte increase versus the superseded `f720b56...` camera build is consistent with the added local/remote arbitration calls.
- builder produced normal, bootloader, partition and merged BIN artifacts.

### Packaging
- Android service status: LIVE from `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.
- Firmware service status: LIVE from `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.
- source ZIPs generated for ESP32 and ESP32-CAM after removal of runtime secret files.
- firmware `SHA256SUMS.txt` generated by the builder for published firmware artifacts.
- firmware `build-info.txt` records source `43671aac5837b1f508dc9a6f7e6aeb38223b950d`.

## Review conclusion
The `f720b56...` handoff is superseded. The validated HR053 artifact authority is now `43671aac...`, which includes the restored direct-camera baseline plus the local/Render arbitration fix and an explicit automated contract spanning ESP32-CAM, Render relay, HiveMQ identity, Android remote viewer/bootstrap and ENV telemetry.

The package is coherent at source, route/protocol, credential-role, GPIO-authority, Android-normalization and compile/package levels. Main ESP32 acquisition logic, L298N mapping, MQ/R0 authority, ADS topology, M4 gate and cliff semantics were not changed by this video-coherence remediation.

## Physical field acceptance still required
1. **ENV** — direct 3.3 V wiring: VCC→3V3, GND→GND, SDA→GPIO13, SCL→GPIO14. Verify normalized telemetry reports either a valid pressure or an explicit ACK/chip/status reason. Confirm the actual module ACKs on 0x76 or 0x77 and that chip ID is supported.
2. **Direct camera** — after flashing the HR053 ESP32-CAM firmware, verify port 80 responds and MJPEG frames are delivered on `192.168.4.2:81/stream`.
3. **Remote camera** — with no local viewer active, verify ESP32-CAM publisher becomes online on Render and Android receives decodable WSS JPEG frames. While a local viewer is active, verify remote capture pauses and resumes after the local stream closes.
4. **Remote control** — verify HiveMQ command ACK and telemetry remain operational regardless of video availability.
5. Physical success must be recorded separately; software/build PASS does not substitute for hardware field acceptance.
