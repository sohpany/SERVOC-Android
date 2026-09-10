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
- Status: **COMPLETE**.

### HR-0101-002 — Unify release identities
- Components: Android, ESP32, ESP32-CAM, video relay.
- Changes: Android `versionName=0.10.1`, `versionCode=21`; release audit identity updated; main firmware identity updated; camera firmware identity updated; relay package/service identity updated.
- Compatibility impact: version metadata only, except where coupled to bootstrap-contract changes below.
- Status: **IMPLEMENTED / whole-system release verification pending**.

### HR-0101-003 — Quarantine public distribution during integration
- Components: firmware and application publication workflows.
- Reason: prevent partially integrated development commits from becoming public update artifacts.
- Change: application and firmware public publication are gated on the explicit `release/hazrob-0.10.1-ready` branch.
- Status: **COMPLETE**.

### HR-0101-004 — Role-scope remote bootstrap credentials
- Components: Android, main ESP32, ESP32-CAM, Render relay/bootstrap.
- Reason: prior integration mixed credential purposes; ESP32-CAM could fall back to an MQTT password while the video publisher endpoint required a distinct publish token.
- Contract:
  - `android` -> operator MQTT credentials + video VIEW token.
  - `esp32` -> robot MQTT credentials only.
  - `esp32-cam` -> video PUBLISH token only.
  - `legacy` -> temporary compatibility bridge for already-deployed 0.10.0 clients.
- Security principle: passwordless operator UX does not mean unauthenticated network control; credentials are provisioned automatically and remain role-scoped.
- Status: **IMPLEMENTED / live Render deployment verification pending**.

### HR-0101-005 — ESP32 remote MQTT bootstrap alignment
- Component: main ESP32 firmware.
- Change: ESP32 identifies itself as `X-HazRob-Client: esp32`, validates role and robot ID, and refreshes robot-scoped MQTT credentials through the controlled HTTPS bootstrap path.
- Status: **IMPLEMENTED / compile and live broker verification pending**.

### HR-0101-006 — ESP32-CAM remote publish-token alignment
- Component: ESP32-CAM firmware.
- Change: camera identifies itself as `X-HazRob-Client: esp32-cam`, retrieves `video_publish_token`, does not use MQTT password as the intended video credential, and invalidates a stale publisher credential after HTTP 401 before re-bootstrap.
- Status: **IMPLEMENTED / compile and live relay verification pending**.

### HR-0101-007 — Relay contract test
- Components: Render video relay source + CI.
- Coverage: health contract, role-scoped bootstrap, unauthorized rejection, publisher WebSocket, viewer WebSocket, binary JPEG forwarding, publisher-online status, frame count and byte-count reporting.
- Verified CI: workflow run `34420399016` completed successfully on source commit `24e70b024acca448b5b9689c4c74b3ffc12ac9d0`.
- Status: **SOURCE/CI PASS / live deployment still pending**.

### HR-0101-008 — Controlled audit baseline created
- Components: project governance/documentation.
- Change: this controlled log is the mandatory record for subsequent 0.10.1 changes.
- Status: **COMPLETE**.

### HR-0101-009 — Active release identity and distribution normalization
- Components: GitHub Actions, Android runtime identity, active architecture/release documentation, main/camera cloud-profile documentation.
- Original finding: active sources contained operational 0.10.0/0.9 naming; firmware ZIP/artifact names and APK publication gate were stale; runtime notification and documentation had retired version markers.
- Corrections now present:
  - firmware source ZIPs and artifacts use `0.10.1`;
  - application and firmware publication gates use `release/hazrob-0.10.1-ready`;
  - runtime link notification derives version from `BuildConfig.VERSION_NAME`;
  - root README, architecture and master firmware guide identify 0.10.1;
  - main cloud profile and ESP32-CAM remote-video profile comments identify 0.10.1.
- Compatibility impact: release-control/identity only; no intended protocol or GPIO change.
- Verification: source inspection complete; fail-closed release-contract gate added under HR-0101-012; CI execution pending due HR-0101-013.
- Status: **IMPLEMENTED / final automated sweep pending**.

### HR-0101-010 — Canonical GPIO authority and L298N wiring correction
- Components: main ESP32 firmware documentation, root README, architecture, canonical GPIO map.
- Original finding: an active README described PWM on direction inputs, conflicting with the actual six-GPIO firmware.
- Authoritative L298N contract: GPIO16=ENA/left PWM, GPIO18=IN1, GPIO27=IN2, GPIO17=ENB/right PWM, GPIO19=IN3, GPIO32=IN4. ENA/ENB jumpers are removed when controlled by ESP32.
- Canonical document: `docs/0.10.1/GPIO_AUTHORITY.md`.
- Electrical review recorded there: GPIO34/35/36/39 input-only use is valid; sound/battery use ADC1; no active SPI-flash GPIO6–11; active drive lines avoid common ESP32 strapping pins; 5 V signals/pull-ups must not be applied directly to ESP32 GPIO.
- ESP32-CAM occupied AI-Thinker camera pins are also recorded and are not available as free external GPIO.
- Verification: firmware/source-to-document comparison completed; compile and physical wiring confirmation remain release gates.
- Status: **IMPLEMENTED / source mapping PASS / compile + physical confirmation pending**.

### HR-0101-011 — Direct/Remote configuration parity
- Components: Android `RobotApi`, main ESP32 local HTTP configuration path, `Hardware.h/.cpp`, `RemoteMqtt.cpp`.
- Original finding: Android Remote mode emitted `config_get` and `config_set`, while the ESP32 MQTT dispatcher returned `unknown_command`.
- Correction:
  - shared `SettingsPatch` contract created;
  - local `/cfg` and remote `config_set` use the same `applySettings(..., true)` validation/persistence path;
  - remote `config_get` returns the same settings JSON contract;
  - static cross-component command audit added.
- Relevant commits include `d7cf0cb6ebf38987f0569c3fd6243840996a184d`, `87ae77f1972e1a92dca7c7eeb7fdf5c28498f2d5`, `2f063bceb550f636ddd31d647d87327e16498b14`, and `35dd6320c4ff151f898f71a70cd405e0473d0607`.
- Status: **IMPLEMENTED / source parity verified / compile + live MQTT round-trip pending**.

### HR-0101-012 — Fail-closed cross-component 0.10.1 release contract
- Components: `tools/audit_0101_integration_contract.py`, Android build workflow, firmware build workflow.
- Reason: module-specific compilation alone cannot detect cross-component drift.
- Gate checks:
  - Android/master/camera/relay release identities and Android versionCode;
  - active release artifact names and release-branch gates;
  - fixed Direct addresses and non-blocking Direct admission;
  - canonical GPIO/hardware-profile allocation;
  - Android MQTT command emitters vs ESP32 handlers;
  - local/remote configuration shared semantics;
  - required local HTTP endpoints;
  - Android/ESP32/ESP32-CAM HiveMQ/robot-ID/service coordinates;
  - role-scoped bootstrap and separate video publish/view credentials;
  - stale operational 0.10.0/0.9 markers in critical active files.
- Both Android and firmware workflows invoke this gate before their substantive build stages.
- Commits include `3b719f473ad97f9a5ba306fad527cb0eff8e43cd`, `67e57548086f65e6e6f02be90b7976d0293a855b`, and the Android workflow gate commit `b1730c7facdbfed779f5620ccba9b44fff2d8187`.
- Status: **IMPLEMENTED / automated execution pending HR-0101-013 resolution**.

### HR-0101-013 — GitHub Actions pre-run execution blocker
- Components: GitHub Actions execution environment; affects Android and firmware verification.
- Finding: multiple recent Android/Firmware workflow runs conclude `failure` before any workflow step executes. Representative runs show `steps=[]` and `runner_id=0`, so no checkout, audit, compile, unit test or signing step actually ran.
- Representative runs:
  - Firmware `34422870283`, head `c9bbac98c0fddb5f33a2a5bcf9e4387f48de5e35`;
  - Android `34422844093`, head `35dd6320c4ff151f898f71a70cd405e0473d0607`;
  - Android exact-head attempt `34428925515`, original job `102720032002`, and rerun job `102728903468`; both failed before steps/logs were allocated.
- Interpretation rule: these runs are **not** to be reported as code compile/test failures, and equally are **not** evidence of a passing build.
- Exact external cause is not yet proven; no speculative code workaround is authorized solely to mask this condition.
- Impact: GitHub Actions remains unavailable as the final native CI evidence; independent Render builders now provide actual compile/test evidence under HR-0101-025.
- Status: **OPEN / infrastructure execution blocker; source builds independently verified**.

### HR-0101-014 — Render production baseline alignment
- Components: Render `hazrob-video-relay`, remote bootstrap, ESP32-CAM WSS publishing, Android WSS viewing, HiveMQ startup diagnostics.
- Live finding:
  - production service `srv-dad3p8gn74is73d52aj0` is still bound to `dev/hazrob-0.9`;
  - current live deployment lineage runs relay version `0.10.0`, while 0.10.1 clients require service version `0.10.1` and protocol `2`;
  - the production hostname is nevertheless the canonical hostname embedded in Android, main ESP32 and ESP32-CAM 0.10.1 profiles;
  - existing Render startup logs proved both application and robot HiveMQ credentials were accepted over TLS with MQTT `CONNACK=0` at the prior deployment time, but this does not prove topic ACL permissions.
- Required 0.10.1 deployment configuration is documented in `docs/0.10.1/RENDER_DEPLOYMENT_CONTRACT.md`, including service/root/build/start settings and environment-variable names without secret values.
- Safety rule: do not redeploy while Render still targets the retired branch; do not copy new source into `dev/hazrob-0.9` as a workaround.
- Tooling limitation: the currently exposed Render connector can create/deploy services and update environment variables, but cannot change the branch of an existing service or read/copy its secret environment values.
- Status: **OPEN RELEASE BLOCKER / production branch alignment still required**.

### HR-0101-015 — Latched software E-stop safety correction
- Components: main ESP32 safety state, Direct API, Remote MQTT, Android control UI/command state.
- Finding: the pre-correction `emergencyStop()` immediately stopped motion, measurement and MQ power but did **not** latch a software E-stop; a subsequent drive command could authorize motion again without an explicit reset.
- Authoritative correction contract is documented in `docs/0.10.1/SAFETY_CONTROL_CONTRACT.md`.
- Required behavior: `estop` latches; drive is inhibited while latched; heartbeat/reconnect cannot clear it; reset is explicit (`/estop/reset` Direct and `estop_reset` Remote), leaves L/R at zero, and requires a subsequent new drive command before movement.
- Android reset must be intentional rather than an ordinary tap, and telemetry shall expose the latch state.
- Safety limitation: this remains a software interlock and is not a certified emergency-stop or substitute for physical power isolation.
- Historical status: **IDENTIFIED / CONTRACT DOCUMENTED**. Implementation closure is recorded under HR-0101-021.

### HR-0101-016 — Remote camera status authority correction
- Components: Android `RemoteConnection`, `StatusTab`, Render video status/WSS path.
- Finding: actual remote video correctly uses authenticated Render `/hazrob/video/status/hazrob-001` plus WSS viewer/first-JPEG validation, while the pre-correction Status tab derived camera state from an MQTT topic with no publisher.
- Authoritative correction contract is documented in `docs/0.10.1/REMOTE_CAMERA_STATUS_CONTRACT.md`.
- Required behavior: Render publisher status is the lightweight status authority; full WSS first-frame/decode remains an explicit diagnostic; camera availability must not become a prerequisite for Remote robot control.
- Historical status: **IDENTIFIED / CONTRACT DOCUMENTED**. Implementation closure is recorded under HR-0101-022 and hardened under HR-0101-024.

### HR-0101-017 — Scientific publication pipeline alignment and fail-closed release gating
- Components: ChemCore publication, molecular seed, unresolved-curation, candidate-QC and scientific-data pinning workflows.
- Reason: scientific-data workflows still referenced retired branches and allowed publication inputs that could bypass the controlled 0.10.1 release contract.
- Corrections:
  - Stable publication is gated to `release/hazrob-0.10.1-ready`;
  - canonical Stable payload is generated by `tools/build_chemcore_stable_snapshot.py` rather than arbitrary manual payload/count inputs;
  - publication validates source SHA, Stable policy, schema, minimum app `0.10.1`, counts, molecular/unresolved state and signature contract;
  - molecular/unresolved/QC workflows target 0.10.1 and preserve unresolved identities as ineligible rather than forcing molecular/QSPR resolution;
  - scientific-data pinning no longer deletes the entire generated asset tree and is restricted to the intended NIOSH/exposure directories.
- Compatibility impact: release/scientific-pipeline hardening; no GPIO or transport change.
- Status: **IMPLEMENTED / final release-workflow execution still subject to HR-0101-013**.

### HR-0101-018 — ChemCore Stable manifest fail-closed contract
- Components: `ChemCoreLibraryManifest`, library contract tests, Stable publication contract.
- Correction: supported manifest schema must match exactly; Stable channel requires JSON payload format, `SHA256withRSA`, a signature, and minimum compatible app 0.10.1.
- Verification: contract tests cover unsupported schema rejection, Stable SQLite rejection, valid signed JSON acceptance, tamper rejection, and Research-channel compatibility.
- Status: **IMPLEMENTED / Android unit-test baseline PASS through HR-0101-025**.

### HR-0101-019 — GPIO25 relay authority made audit-explicit
- Components: canonical GPIO documentation / integration gate readability.
- Finding: the canonical GPIO table correctly recorded relay GPIO 25, while an audit check expected an explicit `GPIO25` alias and failed before compilation.
- Correction: documentation made the relay alias explicit without changing the physical assignment.
- Compatibility impact: documentation/audit only; no wiring change.
- Status: **COMPLETE**.

### HR-0101-020 — Audit-readable GPIO aliases normalized
- Components: `docs/0.10.1/GPIO_AUTHORITY.md`, integration audit.
- Reason: prevent repeated false negatives where valid table rows used numeric GPIO columns but the audit expected textual `GPIOxx` aliases.
- Correction: canonical aliases were made machine-readable for all controlled pins without changing assignments.
- Verification: cross-component integration contract subsequently passed.
- Status: **COMPLETE**.

### HR-0101-021 — Latched software E-stop implemented end-to-end
- Components: main ESP32 `SafetyInterlock`, local HTTP API, Remote MQTT dispatcher/telemetry, Android `RobotApi` and `ControlTab`.
- Corrections:
  - `/estop` and remote `estop` latch the interlock;
  - Direct/Remote drive is rejected while latched;
  - `/estop/reset` and `estop_reset` perform an explicit reset after stopping motion;
  - telemetry/status expose the latch state;
  - Android zeros staged drive, blocks joystick output while latched, synchronizes from diagnostics, and uses an intentional long press for reset.
- Verification: cross-component integration gate, Android unit tests, Android release compile, main ESP32 compile and ESP32-CAM compile all passed on controlled 0.10.1 baselines; physical behavior remains a field test.
- Safety limitation: software interlock only; not a certified physical emergency stop.
- Status: **SOURCE/BUILD PASS / FIELD TEST PENDING**.

### HR-0101-022 — Render camera status made the sole Remote camera authority
- Components: Android `RemoteConnection`, `StatusTab`, `RemoteVideoReadiness`, Remote admission path.
- Corrections:
  - obsolete MQTT `camera/status` subscription/state path removed;
  - Remote camera dashboard uses authenticated Render `/hazrob/video/status/hazrob-001`;
  - full video readiness remains WSS plus first decodable JPEG;
  - camera readiness remains non-blocking for Remote robot control admission.
- Verification: integration gate and Android builds passed; relay source contract also passes under HR-0101-025.
- Status: **SOURCE/BUILD PASS / LIVE CAMERA FIELD TEST PENDING**.

### HR-0101-023 — Bootstrap service version/protocol contract pinned across clients
- Components: Android session bootstrap, main ESP32 MQTT bootstrap, ESP32-CAM video bootstrap.
- Reason: prevent clients from silently accepting a relay implementation from another protocol generation.
- Contract: 0.10.1 clients require relay service version `0.10.1`, protocol `2`, expected role, and expected robot identity before accepting returned credentials.
- Android cached bootstrap credentials are refreshed when the stored contract identifier no longer matches the active 0.10.1 contract.
- Verification: exact-head Android/main/CAM builds passed under HR-0101-025.
- Status: **IMPLEMENTED / BUILD PASS / PRODUCTION RELAY ALIGNMENT PENDING HR-0101-014**.

### HR-0101-024 — Android Remote service contract centralized and camera checks hardened
- Components: `RemoteServiceProfile`, `RemoteSessionManager`, `RemoteVideoReadiness`.
- Finding: lightweight/full camera status accepted any relay protocol `>=1`, which could report an older relay as service-ready despite 0.10.1 requiring protocol 2.
- Correction:
  - canonical Android service version/protocol are centralized in `RemoteServiceProfile`;
  - bootstrap reuses the same constants;
  - Render camera status requires exact service `hazrob-video-relay`, version `0.10.1`, protocol `2`, and robot ID `hazrob-001`;
  - mismatched service contract fails closed rather than being shown as ready.
- Relevant commits: `16625ebe8ec38c405b2f3d803d31c21e8cd39016`, `3db088fad190626dc062f28e661bd1e097a58368`, `c3f6d40816e37722fdb46ba9bef7e6008165a549`.
- Verification on `c3f6d40816e37722fdb46ba9bef7e6008165a549`: cross-component integration contract PASS; Android unit tests PASS; `assembleRelease` PASS; signing continuity PASS; APK SHA-256 `6d89c76e090932e9080e9ef276c56b78db8dabbf4a7e91521958e5bad452ab26`.
- Status: **SOURCE/BUILD PASS / LIVE PRODUCTION CHECK PENDING HR-0101-014**.

### HR-0101-025 — Independent exact-head build and Render relay verification baseline
- Components: Android, main ESP32, ESP32-CAM, Render relay source/runtime and contract test.
- Reason: GitHub Actions is failing before runner allocation under HR-0101-013, so real compile/test evidence was required without misclassifying infrastructure failures as code failures.
- Exact integrated build baseline `9872504443f67114e0a78ae5461d218cb21a45b9`:
  - cross-component integration contract PASS;
  - Android unit tests PASS;
  - Android `assembleRelease` PASS;
  - APK signing certificate continuity PASS;
  - APK SHA-256 `41c4938c93a89990f13b8854f8d3999903fa0b275cba44bf956ea87a0bc2350e`;
  - main ESP32 compile PASS: 1,085,845 bytes program (34%), 50,660 bytes global RAM (15%);
  - ESP32-CAM compile PASS: 1,139,941 bytes program (36%), 67,888 bytes global RAM (20%).
- Firmware builder cache issue: an earlier Render run stopped at `arduino-cli config init` because a cached config already existed; a clean-cache run reached and passed actual compilation. This was a builder-environment issue, not a source compile failure.
- Relay verification on the same 98725044 baseline:
  - independent Render service `hazrob-video-relay-0101-verify` built and started from `dev/hazrob-0.10.1`, logging `hazrob-video-relay@0.10.1` and `HazRob video relay 0.10.1 listening`;
  - independent relay contract service executed `npm run check` and `npm test` and reported `HazRob 0.10.1 relay contract: PASS`;
  - contract coverage includes health version/protocol, role-scoped bootstrap, unauthorized rejection, WSS publisher/viewer, JPEG forwarding and publisher status.
- Production limitation: these verification services do not replace the canonical production relay and intentionally do not copy production secrets. HR-0101-014 remains open.
- Status: **VERIFICATION PASS / PRODUCTION REMOTE E2E STILL PENDING**.

## Mandatory audit blocks before release approval

1. Repository-wide version sweep and active-artifact identity.
2. GPIO authority/electrical-capability review and physical wiring confirmation.
3. Direct transport: Android <-> ESP32 HTTP/API, command lease, heartbeat, STOP/E-STOP, link recovery, camera local MJPEG/HTTP.
4. Remote transport: Android <-> HiveMQ <-> ESP32 command/ACK/telemetry/status plus Render bootstrap, ESP32-CAM publish and Android viewer.
5. Render deployment: branch binding, build/start settings, environment-variable contract, health/bootstrap/video status, live version.
6. HiveMQ: operator vs robot identities, TLS 8883, topic/ACL separation where administratively verifiable, retained LWT/status and command/ACK round-trip.
7. ChemCore: manifests, signatures, molecular descriptor data, curated unresolved identities, runtime loading/fallback, schema/version compatibility and M4 gating.
8. Expanded chemical/exposure data: NIOSH identities, NIOSH IDLH, OSHA Z-1, 144-compound priority catalogue, 76-model research catalogue; identifiers, units, duplicates, provenance and hashes.
9. Build/test: Android tests/signing, ESP32 compile, ESP32-CAM compile, relay contract, scientific-data workflows and distribution controls.
10. Final release manifest: exact source SHA, artifact hashes, firmware/APK identities, dataset/library hashes, live Render version, HiveMQ verification, canonical GPIO table and accepted limitations.

## Release rule

HazRob 0.10.1 shall not be declared **FINAL** until every mandatory audit block is recorded as PASS or explicitly documented as an accepted limitation. Any corrective change receives the next sequential `HR-0101-NNN` identifier before release testing resumes.
