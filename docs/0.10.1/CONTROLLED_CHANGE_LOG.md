# HazRob 0.10.1 — Controlled Change Log

Status: **INTEGRATION / NOT YET RELEASED**  
Authoritative branch: `dev/hazrob-0.10.1`  
Baseline ancestor: `dc96b6c7b862c44ac1e2266c1430e6f44e14e186`  
Control rule: every subsequent change to HazRob 0.10.1 must be committed on this branch (or a temporary review branch merged into it) and recorded here before release approval.

## Change-control convention

Every change receives a sequential identifier `HR-0101-NNN` and must record: reason, affected component(s), affected files, compatibility impact, verification performed, and final status.

No file may be treated as an authoritative 0.10.1 artifact solely because its filename says 0.10.1. Release authority requires consistency with this log, the integration baseline, and the final release checklist.

## Authoritative 0.10.1 scope

The release baseline shall cover and cross-check all of the following as one integrated system:

- Android application.
- Main ESP32 firmware.
- ESP32-CAM firmware.
- Latest authoritative GPIO/pin allocation and electrical assumptions.
- Direct/local Wi-Fi transport and control contracts.
- Remote/internet transport and control contracts.
- Render video relay/bootstrap service configuration and runtime contract.
- HiveMQ broker identities, topic contracts, TLS behavior, command/ACK/telemetry/status behavior.
- ChemCore analytical engine and advanced chemical-analysis libraries.
- Expanded chemical/exposure databases and generated scientific data artifacts.
- CI/build workflows, publication/distribution workflows, version metadata, and release artifacts.

Secrets and credentials themselves are not to be committed to source control. Only their required variable names, roles, validation rules, and deployment mapping are version-controlled.

## Recorded changes

### HR-0101-001 — Establish 0.10.1 integration branch
- Reason: stop parallel modification of 0.9.x / 0.10.0 and establish a single controlled integration target.
- Baseline: branch created from the latest 0.10 source state at `dc96b6c7b862c44ac1e2266c1430e6f44e14e186`.
- Status: COMPLETE.

### HR-0101-002 — Unify release identities
- Components: Android, ESP32, ESP32-CAM, video relay.
- Changes: Android `versionName=0.10.1`, `versionCode=21`; release audit identity updated; main firmware identity updated; camera firmware identity updated; relay package/service identity updated.
- Compatibility impact: version metadata only, except where coupled to the bootstrap contract changes below.
- Status: COMPLETE / pending whole-system verification.

### HR-0101-003 — Quarantine firmware public distribution during integration
- Component: GitHub Actions firmware workflow.
- Reason: prior workflow could publish a partially integrated firmware build from development branches under the old 0.10.0 naming.
- Change: firmware compilation remains enabled; public distribution is gated until an explicit 0.10.1 release-ready branch is used.
- Status: COMPLETE.

### HR-0101-004 — Role-scope remote bootstrap credentials
- Components: Android, main ESP32, ESP32-CAM, Render relay/bootstrap.
- Reason: the prior integration mixed credential purposes; ESP32-CAM could fall back to an MQTT password while the video publisher endpoint required a distinct publish token.
- New contract:
  - `android` -> operator MQTT credentials + video VIEW token.
  - `esp32` -> robot MQTT credentials only.
  - `esp32-cam` -> video PUBLISH token only.
  - `legacy` -> temporary compatibility bridge for already-deployed 0.10.0 clients.
- Security principle: passwordless operator UX does not mean unauthenticated network control; credentials are provisioned automatically and remain role-scoped.
- Status: IMPLEMENTED / pending live deployment verification.

### HR-0101-005 — ESP32 remote MQTT bootstrap alignment
- Component: main ESP32 firmware.
- Change: ESP32 identifies itself as `X-HazRob-Client: esp32`; validates role and robot ID; refreshes remote MQTT credentials through the controlled bootstrap path.
- Status: IMPLEMENTED / pending firmware CI and live broker verification.

### HR-0101-006 — ESP32-CAM remote publish-token alignment
- Component: ESP32-CAM firmware.
- Change: camera identifies itself as `X-HazRob-Client: esp32-cam`; retrieves `video_publish_token`; no longer relies on MQTT password as the intended video publishing credential; clears stale token after HTTP 401 and retries bootstrap.
- Status: IMPLEMENTED / pending firmware CI and live relay verification.

### HR-0101-007 — Relay contract test
- Components: Render video relay source + CI.
- Test coverage: health contract, role-scoped bootstrap, unauthorized rejection, publisher WebSocket, viewer WebSocket, binary JPEG forwarding, publisher-online status, frame count and byte-count reporting.
- Status: IMPLEMENTED / relay CI PASS; live deployment verification still pending.

### HR-0101-008 — Controlled audit baseline created
- Components: project governance/documentation.
- Change: this controlled log is now the mandatory record for subsequent 0.10.1 changes.
- Status: COMPLETE.

### HR-0101-009 — Active release identity and distribution normalization
- Components: GitHub Actions, Android runtime identity, active architecture/release documentation, firmware documentation.
- Finding: active 0.10.1 sources still contain operational 0.10.0/0.9 naming. Firmware source ZIPs/artifacts and publication commit text are still named 0.10.0; Android public APK distribution is still gated on the retired `dev/hazrob-0.10-chemcore-foundation` branch; `RobotLinkService` still displays `HazRob 0.10.0`; active README/architecture comments contain stale release identities.
- Compatibility impact: naming/distribution/runtime-identification inconsistency; no intended protocol change.
- Required correction: normalize active artifacts and release gates to 0.10.1 while preserving explicitly historical changelog entries as history.
- Verification required: repository sweep + Android/Firmware CI + artifact filename inspection.
- Status: IDENTIFIED / CORRECTION PENDING.

### HR-0101-010 — Canonical GPIO authority and L298N wiring correction
- Components: main ESP32 firmware documentation, root README, final GPIO map.
- Finding: root README states that ENA/ENB remain enabled and PWM is applied to direction inputs, which conflicts with authoritative 0.10.1 firmware. Firmware defines dedicated speed-enable pins and `Hardware.cpp` applies PWM on those enable pins.
- Authoritative L298N contract: GPIO16=ENA/left PWM, GPIO18=IN1, GPIO27=IN2, GPIO17=ENB/right PWM, GPIO19=IN3, GPIO32=IN4. ENA/ENB jumpers must be removed when controlled by the ESP32.
- Required correction: produce one canonical GPIO authority document derived from firmware; align all active documentation with it; verify electrical/input-only/ADC/I2C constraints and ESP32-CAM occupied pins.
- Verification required: source-to-document pin comparison + firmware compilation.
- Status: IDENTIFIED / CORRECTION PENDING.

### HR-0101-011 — Remote configuration command parity
- Components: Android `RobotApi`, master ESP32 `RemoteMqtt.cpp`.
- Finding: Android Remote mode sends `config_get` and `config_set`, but the current ESP32 MQTT command dispatcher has no handlers for either command and therefore returns `unknown_command`.
- Compatibility impact: remote configuration is not functionally equivalent to local `/cfg`; speed/camera/warmup/measurement settings can fail over Internet control.
- Required correction: implement and test MQTT `config_get`/`config_set` parity using the same validation/persistence semantics as the local `/cfg` contract.
- Verification required: command contract test, firmware CI, Android CI, remote round-trip verification.
- Status: IDENTIFIED / CORRECTION PENDING.

## Mandatory audit blocks before release approval

1. Repository-wide version sweep: eliminate active 0.10.0/0.9.x runtime identifiers from authoritative 0.10.1 artifacts while preserving only historical documentation where intentional.
2. GPIO authority review: derive one canonical pin table from firmware and check collisions, input-only constraints, ADC/I2C/PWM suitability, boot-strapping pins, voltage-domain assumptions, and actual hardware wiring.
3. Direct transport audit: Android <-> ESP32 HTTP/API contract, command lease, heartbeat, emergency stop, motion safety, camera local MJPEG/HTTP endpoints, and reconnect behavior.
4. Remote transport audit: Android <-> HiveMQ <-> ESP32 command/ACK/telemetry/status, Render bootstrap, ESP32-CAM publish, Android viewer, TLS certificates, topic names, session/sequence/nonce/TTL semantics.
5. Render deployment audit: branch binding, service start command, required environment-variable names, health/bootstrap/video status responses, and live deployment version.
6. HiveMQ audit: operator vs robot identities, ACL/topic separation if available, TLS 8883, retained LWT/status behavior, and command/ACK round-trip.
7. ChemCore audit: manifest, signature validation, generated molecular descriptors, unresolved curation, runtime loading/fallback behavior, schema/version compatibility.
8. Expanded chemical/exposure database audit: NIOSH chemical library, NIOSH IDLH, OSHA Z-1 and any additional sources; record counts, identifiers/CAS normalization, units, duplicate resolution, provenance, manifests and hashes.
9. Build and test audit: Android unit/build checks; ESP32 compile; ESP32-CAM compile; relay contract; scientific-data workflows; update-distribution workflow.
10. Final release manifest: exact commit SHA, artifact hashes, firmware filenames, APK identity, database/library manifest hashes, Render live version, verified HiveMQ contract, GPIO table, and known limitations.

## Release rule

HazRob 0.10.1 shall not be declared FINAL until every mandatory audit block above is recorded as PASS or explicitly documented as an accepted limitation. Any corrective change discovered during verification receives the next `HR-0101-NNN` identifier and is committed before testing resumes.
