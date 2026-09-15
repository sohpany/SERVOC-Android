# HR-0101-053 — Camera Baseline Restore + ENV Coherence Release

Status: IN PROGRESS — source/build validation required; physical field acceptance required.
Version remains: HazRob 0.10.1 / Android versionCode 21.

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

HR053 supersedes the experimental local-camera recovery behavior introduced under HR051/HR052 where it conflicts with this restored baseline. Remote-video code remains present but must not redefine the direct/local endpoint contract.

## ENV / pressure authority
The environmental sensor is directly wired to the main ESP32 and remains intentionally isolated from the analytical ADS bus:
- ENV: SDA GPIO13, SCL GPIO14, 100 kHz, ESP32 second hardware I2C controller `TwoWire(1)`.
- ADS1115 #1/#2 remain SDA GPIO21 / SCL GPIO22, 400 kHz.
- BME280: chip ID 0x60; BMP280: supported IDs 0x56/0x57/0x58.
- probe addresses: 0x76 and 0x77.
- firmware raw telemetry keys remain `t`, `h`, `p`, `type`, `addr`, `chip_id`, `status`, ACK flags and read-failure metadata.
- Android `RobotProtocolV4` remains the normalization authority to `temperature`, `humidity`, `pressure`, `type`, `address`, `chip_id`, `status_code`.
- Settings UI must display `status_code`, sensor type, address, chip ID and ACK state instead of hiding failures behind `NOT READY`.

No software claim is made that a physically connected module has ACKed until field telemetry proves it. A valid software/build result is separate from physical acceptance.

## Cross-system invariants retained
- HazRob 0.10.1 / API 4 / hardware profile `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2`.
- L298N authority unchanged: LEFT=B EN17/IN19/IN32; RIGHT=A EN16/IN18/IN27.
- accepted R0 campaign remains `FIELD-R0-3SESSION-20260913`.
- M4 quantitative gate remains fail-closed.
- ADS acquisition authority remains unchanged.
- cliff/IR semantics remain unchanged.

## Required validation before package handoff
1. consolidated HR048/49/50/53 source gate PASS from one exact commit;
2. Android unit tests PASS and signed release APK PASS from that commit;
3. ESP32 master compile PASS with pinned ESP32 core 3.3.11;
4. ESP32-CAM compile PASS with pinned ESP32 core 3.3.11;
5. APK, ESP32 source ZIP and ESP32-CAM source ZIP all trace to the same commit;
6. SHA-256 manifests captured;
7. field acceptance still required for ENV ACK/pressure and direct camera stream.

## Field acceptance
ENV: with direct 3.3 V wiring, verify `/data` or normalized telemetry reports either a valid BME/BMP pressure or an explicit ACK/chip/status reason. Direct camera: verify port 80 status/root and MJPEG frames on port 81 before evaluating Remote video.
