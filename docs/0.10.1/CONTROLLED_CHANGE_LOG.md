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
- Status: IMPLEMENTED / pending confirmed CI result.

### HR-0101-008 — Controlled audit baseline created
- Components: project governance/documentation.
- Change: this controlled log is now the mandatory record for subsequent 0.10.1 changes.
- Status: COMPLETE.

## Current repository delta from the 0.10.1 branch baseline

At the time this control log was established, the 0.10.1 branch was **13 commits ahead** of the selected baseline ancestor and had no commits behind it. The delta affected these integration files:

- `.github/workflows/build-firmware.yml`
- `.github/workflows/validate-video-relay.yml`
- `app/build.gradle`
- `app/src/main/java/com/servoc/app/ReleaseAudit.java`
- `app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java`
- `app/src/test/java/com/servoc/app/ReleaseAuditTest.java`
- `firmware/HazRob/Config.h`
- `firmware/HazRob/RemoteMqtt.cpp`
- `firmware/HazRob-CAM/HazRob-CAM.ino`
- `firmware/HazRob-CAM/RemoteVideo.cpp`
- `remote/video-relay/package.json`
- `remote/video-relay/src/index.js`
- `remote/video-relay/test/contract.mjs`

## Mandatory next audit blocks before release approval

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
