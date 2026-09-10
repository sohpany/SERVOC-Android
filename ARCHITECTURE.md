# HazRob 0.10.1 Architecture

Status: **controlled integration architecture**.  
Authoritative branch: `dev/hazrob-0.10.1`.  
Controlled changes: `docs/0.10.1/CONTROLLED_CHANGE_LOG.md`.  
Canonical pin map: `docs/0.10.1/GPIO_AUTHORITY.md`.

HazRob 0.10.1 separates deterministic robot I/O and safety from Android-side operator workflow, ChemCore analysis, evidence handling, persistence and reporting. Direct and Remote are two transports for the same robot/application contract; Remote capability must not weaken Direct/offline operation.

## System components

### Main ESP32

The master ESP32 owns:

- L298N 4WD actuator I/O and motion safety;
- one active-LOW MQ power relay;
- dual ADS1115 acquisition for eight MQ channels;
- BME280/BMP280 environment acquisition;
- MAX4466, IR threshold, LDR threshold, battery and ultrasonic channels;
- local HTTP API v4 at `192.168.4.1`;
- SoftAP `HazRob` and optional WAN STA/NAPT;
- outbound MQTT 3.1.1 over TLS for Remote control/ACK/telemetry/status.

The master does **not** perform ChemCore molecular inference, occupational-limit interpretation or heavy database work.

### ESP32-CAM

The AI-Thinker ESP32-CAM is a separate Wi-Fi node at `192.168.4.2`.

- Local HTTP/status: port 80.
- Local MJPEG stream: port 81.
- Remote video: JPEG frames published over authenticated WSS to the Render relay.
- The camera has no direct GPIO/UART control cable to the master in 0.10.1.
- Camera GPIO assignments are occupied according to `docs/0.10.1/GPIO_AUTHORITY.md`.

### Android application

Android owns:

- operator/developer UI;
- Direct vs Remote transport selection;
- API v4 normalization;
- telemetry persistence and session lineage;
- ChemCore/AMCI analysis and evidence gating;
- scientific reference/exposure libraries;
- developer experiments/calibration workflows;
- video viewing/recording and report/export workflows;
- application and signed Stable ChemCore update handling.

## Hardware authority

Hardware profile: `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1`.

### L298N 4WD

Six ESP32 GPIO lines are authoritative:

- GPIO16 = ENA / left speed PWM;
- GPIO18 = IN1;
- GPIO27 = IN2;
- GPIO17 = ENB / right speed PWM;
- GPIO19 = IN3;
- GPIO32 = IN4.

Two motors per side are connected in parallel to the corresponding L298N bridge outputs. ENA/ENB jumpers are removed when GPIO16/GPIO17 control enable. IN1..IN4 are direction lines; speed PWM is not applied to direction inputs.

### MQ/ADS

One active-LOW relay on GPIO25 powers all eight MQ modules. Valid states are only `0=OFF` and `1=ON`.

- ADS1 `0x48`: MQ2, MQ4, MQ5, MQ8.
- ADS2 `0x49`: MQ3, MQ7, MQ9, MQ135.

The ADS devices are fixed to the regulated 5 V sensor rail; the ESP32 I2C logic domain must remain 3.3 V safe. Acquisition uses ±6.144 V full-scale, 860 SPS and four conversions per channel. The eight channels are sequential, not physically simultaneous.

## Direct transport

Direct mode is the primary local/offline path:

- SoftAP SSID: `HazRob`;
- master: `192.168.4.1/24`;
- camera: `192.168.4.2/24`;
- master HTTP: port 80;
- camera HTTP/status: port 80;
- camera MJPEG: port 81.

Android uses the fixed local contract rather than a stale saved robot address. It binds Direct traffic to the Wi-Fi network that actually owns a `192.168.4.x` address so cellular/default routing cannot steal local robot traffic. Entry to the Direct workspace is not blocked by a preflight `/diag` gate; link state is reported after entry and the background link service retries Wi-Fi binding after a failed heartbeat.

Direct control remains usable without Internet.

## Remote transport

Remote mode uses authenticated service identities while presenting a passwordless operator experience.

### Bootstrap roles

The Render bootstrap contract is role-scoped:

- `android` → operator MQTT credentials + video VIEW token;
- `esp32` → robot MQTT credentials only;
- `esp32-cam` → video PUBLISH token only;
- `legacy` → temporary one-release bridge for already-deployed 0.10.0 clients during migration.

The user is not asked to type a HazRob remote password. This does **not** mean anonymous command or video transport.

### MQTT control path

Broker transport is MQTT 3.1.1 over TLS on port 8883.

Root topic: `hazrob/<robot-id>/`

- `cmd` — Android → robot commands;
- `ack` — application-level command acknowledgement;
- `telemetry` — robot telemetry snapshots;
- `status` — retained availability/status + Last Will;
- `camera/status` — camera/relay status consumed by Android when available.

Command envelopes carry session ID, monotonically increasing sequence, nonce, send time and TTL. The robot rejects stale, duplicate/out-of-order or mismatched-session commands. Emergency/safety commands and normal drive commands share the same firmware motion-safety state machine as Direct mode.

### Remote video path

ESP32-CAM publishes binary JPEG frames to the Render WSS relay with a publish-scoped token. Android views through a separate view-scoped token. The relay validates authentication, robot ID, JPEG bounds and publisher/viewer state. Video readiness is not a prerequisite for accepting a control session: control readiness requires MQTT command ACK plus telemetry.

## WAN bootstrap boundary

Robot WAN SSID/password provisioning remains a **local developer operation** because the master needs WAN credentials before Remote can exist. WAN scan/provision/clear endpoints therefore remain local HTTP endpoints even when the application supports Remote operation.

NAPT is enabled only after the master has validated Internet connectivity on STA; this allows the camera to reach the relay through the master while preserving the local AP network.

## ChemCore analytical boundary

The firmware reports electrical/sensor evidence. Android ChemCore decides analytical interpretation.

Canonical flow:

`Acquisition → controlled session → feature extraction → candidate/evidence inference → OOD/UNKNOWN gate → quantification gate → consistency/mixture checks → AMCI decision → report`

The installed knowledge base is broader than the current MQ hardware's validated analytical coverage. Database presence or a literature response curve does not constitute device calibration. M4 quantitative claims remain gated behind device/analyte-specific validation.

## Scientific data architecture

Pinned/generated data currently includes:

- NIOSH chemical identities;
- NIOSH IDLH records;
- OSHA Table Z-1 normalized records;
- ChemCore molecular descriptors and curated unresolved identities;
- controlled analytical-priority/model catalogues and provenance artifacts.

Stable scientific-library publication is separate from APK publication. Packages are hash/signature/compatibility checked and activated atomically with rollback support.

## Build/release boundary

Development builds may compile on `dev/hazrob-0.10.1`, but public APK/firmware distribution is quarantined during integration. Release publication is enabled only from the explicit `release/hazrob-0.10.1-ready` branch after all audit gates pass.

Final release authority requires one final commit SHA with successful Android, master ESP32, ESP32-CAM, relay and scientific-data checks, plus live Render/HiveMQ verification and a release manifest containing artifact hashes.

## Security and safety boundary

- Secrets/private deployment values must not be committed to GitHub; source control records variable names, roles and validation requirements.
- Prototype bootstrap mechanisms are not equivalent to a production identity platform and must be listed as a known limitation until hardened.
- HazRob 0.10.1 is a research prototype, not Ex/intrinsically-safe/SIL-certified equipment.
- Direct/offline safety remains authoritative when Internet, Render, HiveMQ or camera video is unavailable.
