# HazRob 0.10.2 / HR069-R1 — engineering continuation and QA ledger (2026-10-09)

**Status:** IN DEVELOPMENT — WORKING DIAGNOSTIC ONLY / NOT PHYSICALLY ACCEPTED / NOT RELEASE APPROVED.
**Repository:** `sohpany/SERVOC-Android`.
**Only permitted write branch:** `release/hazrob-0.10.2-rc2`.
**User-provided resumption commit:** `1a2a145209b45c8ec1ae7033594e13c888a0cde2`.
**Prior functioning source:** Android vCode28 `8b89e4c27fa7fda4cc25b294f9a973a2a1a2a60a`; Main ESP32 Arduino `e6a14bc570c335b824c153c7cd7fd2677b9bd005`.
**Continued Android engineering identity:** versionName `0.10.2`, revision `HR069-R1`, **diagnostic versionCode 29** after QA source changes. Not an accepted/published APK.
**Main ESP32:** HR069-R1 / API4, functional source unchanged. **ESP32-CAM:** HR069 unchanged.
**Embedded ChemCore Stable:** `1.0.0-hr069r1`, 652 identities, 49 non-M4 research priors, 0 approved M4; unchanged.
**Controlled production authority:** historical HR069 Android vCode26 remains unchanged.

## A. Resumption reconciliation

- Read `README.md` and `docs/0.10.2/HR069-R1_EXECUTION_CHECKPOINT_20261008.md`.
- Git compare against the specified resumption commit found three later documentation-only commits touching `README.md` and `firmware/HazRob/README.md` with no earlier-code backport.
- Existing CI evidence for Android v28 at `8b89e4c2`: [diagnostic APK](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726140) and [Android/Main/CAM integration](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726087) reported SUCCESS. This evidence is for **v28 only**, not the newly modified v29 sources.
- Original physical, platform, science and human review gates are not automatically cleared.

## B. Implemented corrective source changes

| Order | Source commit | File / purpose | Approval |
| --- | --- | --- | --- |
| 1 | `baab9c6801a8a6beccf2466c1fe1255e86382198` | `AcceptedR0Normalizer.java`: explicit POWER_OFF, ADC_UNAVAILABLE or MISSING_SOURCE prevents strict live/replayed MQ evidence and cannot receive a positive quality weight even when a contradictory validity flag says true. Diagnostic numeric ratio may remain visible but not credited as chemical evidence. | Software QA pending |
| 2 | `bd5115130ceba2fca0a712b1370af3bd4d9fb2f2` | `AcceptedR0NormalizerEvidenceTest.java`: contradictory valid/status cases; nested quality status; support-only inconsistent VALID case. | CI pending |
| 3 | `5f25d11330d7263873c3851a179ab982f6df226d` | `DeveloperR0Tab.java`: correct electrical scale **kΩ** for both firmware-derived Rs and user-entered R0, not Ω. Explain firmware model assumption RL=10 kΩ, 1000 Ω=1 kΩ, and physical RL verification for ready-made four-pin MQ modules. | UI/device acceptance pending |
| 4 | `606b732b736581cbb67c5e93933dcf5c4fb0cb5a` | `HR069R1R0ElectricalUnitsContractTest.java`: guards firmware algebra/UI resistance scale and distinguishes model assumption from measured resistor. | CI pending |
| 5 | `41dba5f781903c977cf4c08e024ae8b3a7b0b7bd` | `app/build.gradle`: increment diagnostic Android versionCode **28 → 29** to distinguish these new, as-yet-unaccepted app bytes from previously delivered v28. VersionName/revision remain 0.10.2/HR069-R1. | CI pending |

**Electrical note:** Main ESP32 `Hardware.cpp` uses `Rs = Cfg::RL * (Cfg::MQ_VCC - AO) / AO`; `Config.h` sets `RL=10.0f`, `MQ_VCC=5.0f`. The *numerical* Rs and R0 scale is kΩ if the installed RL is actually 10 kΩ. We have NOT measured RL on the physical MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135 breakout boards; a different installed load resistor means reported Rs/R0 electrical quantities may be biased. Do not automatically convert existing saved R0 or claim reference-gas calibration. Check and document actual board load-resistor values before scientific M4.

## C. Acceptance matrix

| Workstream | Software status | Required qualification test |
| --- | --- | --- |
| R0 phone persistence and historical replay | Prior v28 CI PASSED; new contradictory-status/units changes require fresh v29 CI | Verify source, physical RL (each board), stable rails, heated MQ baseline, fresh session revision, reinstall/back-up behavior, and unchanged archived ratios |
| ChemCore Stable and reference governance | Embedded Stable present; developer staging is non-runtime | Science admission, approved manifest signature/authenticity, a real read-only HTTPS endpoint, end-to-end update and rollback; USER uploads prohibited; no M4 promotion without reference-gas validation |
| Chemical analysis, identification and tracking | Source contracts and prior simulated tests exist | Known/unknown mixture blind challenges; chemical-target-specific fresh timestamps; OOD/UNKNOWN and gradient/direction gating; true reference-gas concentrations |
| SmartReport PDF/XLSX | Previous v28 build/test PASSED; missing analysis is not invented | Export field session and full site; verify PDF page count/readability, XLSX zip/XML structure, open in two Android applications, source/R0/M4 lineage, deletion/archive behavior |
| Echo/IR and motion | Nominal 150 ms, HIGH=ground, Echo warning 10 cm and optional forward block 5 cm; operator auto-stop defaults OFF | With wheels elevated first, verify Echo 5 V-to-3.3 V level shift, 5/10cm thresholds, distance freshness, IR floor/lift transitions and manual tuning; then toggle auto-stop and test forward/reverse/pivot/E-STOP/watchdog |
| Direct network and camera | Main AP `HazRob`, 192.168.4.1 and CAM .2; code and old CI available | Without internet, real AP discovery, camera STA/MJPEG, 150ms transport cadence as measured, controls/loss-of-focus/stop and packet loss |
| Remote network and camera | EMQX primary, HiveMQ contingency, Railway bootstrap/video architecture documented | Live TLS/MQTT auth/ACL, command TTL/replay rejection and ACK; video WSS frame continuity; controlled failover, link loss/deadman and return to Direct |
| Battery/BME/environment | Present as source contracts; previous physical BME fault unresolved | Test voltage under all 8 MQ heater loads + motors, BME bus SDA13/SCL14, ADC 5V correctness and temperature/RH provenance |

## D. Release discipline and next checkpoint

1. Keep only `release/hazrob-0.10.2-rc2` as the modification target. No HR067/HR068/HR050 source, branch creation or unauthorized merges.
2. Verify a single SHA of new **vCode29** with Android JUnit + runtime/source audit, ESP32 Main/CAM compile, ChemCore challenges and signed APK packaging. CI success is necessary but not sufficient.
3. Record GitHub Actions run IDs, exact SHA, signature and binary SHA-256 before distributing any new artifact. Do not circulate intermediate artifacts created from vCode28 with post-v28 source changes.
4. Do **not** flash firmware for an Android-only QA fix; Main firmware and CAM are unchanged. Arduino ZIP from `e6a14bc5` remains the verified source handoff.
5. Physically qualify the assembled robot and Android reporting before any production promotion. Explicit project-owner approval is still required.

**Next QA result:** PENDING — add exact final v29 workflow IDs, conclusions, checksums and any failures; do not infer a PASS from the job being queued or running.

## E. Remote infrastructure verification — 2026-10-09 (READ-ONLY)

**This section records actual connected Railway account state, not merely Android/firmware source contracts.** No Railway mutation/deployment or GitHub branch switch performed.

- Project: `spirited-youthfulness`, production. Railway `environment_status` reported **OFFLINE** for both `HazRob-Video-Relay-0102-BranchFix` and `HazRob-Video-Relay-0102`. All four services in that Railway environment were offline at inspection.
- Main ESP32 `RemoteCloudConfig.h` points to the BranchFix Railway bootstrap hostname; this matches a configured Railway service domain on port 8080. Domain configuration by itself does not prove a running service.
- BranchFix latest settled deployment `d4aef271-9d73-4643-b911-8eb06fffcef2` has state **FAILED** (2026-09-24). Build logs state **"Script start.sh not found"** and **"Railpack could not determine how to build the app"**, and show repository root contents rather than the expected Node.js service subdirectory. The currently displayed Railway config sets source `sohpany/SERVOC-Android`, branch `release/hazrob-0.10.2` and root `remote/video-relay`, with `npm start` and `/health`. These config values do not retroactively repair the failed deployment.
- Current RC2 `remote/video-relay/package.json` is a Node.js >=20 service with `npm start`, `npm test`. It is **not** qualified as an operational WAN relay merely because code and tests exist.
- **REMOTE END-TO-END = BLOCKED/UNVERIFIED.** No functioning TLS bootstrap, MQTT command/ACK, MQTT telemetry/ACL, WSS video or controlled failover was demonstrated. EMQX remains the configured primary MQTT broker and HiveMQ is a contingency according to source; neither has been verified live by this inspection.
- Security review required before production: investigate tracked static bootstrap authentication material and use provisioned, rotatable per-role credentials, without exposing actual token values in documentation. No credential was changed or published in this session.
- Corrective operations **not executed**: changing Railway source branch, deploying production, reverting to older source, altering cloud credentials, or rebooting physical robot. These require the separate controlled remote-service verification/deployment and acceptance sequence.

## F. New diagnostic v29 validation job references (not acceptance)

Exact **functional** source SHA: `41dba5f781903c977cf4c08e024ae8b3a7b0b7bd`, Android `versionCode 29`. The later documentation-only commit does not modify the tested Android/firmware sources.

- Diagnostic APK / JUnit job: https://github.com/sohpany/SERVOC-Android/actions/runs/37864587662
- Internal Android validation job: https://github.com/sohpany/SERVOC-Android/actions/runs/37864587673
- Android + Main ESP32 + CAM build/QA: https://github.com/sohpany/SERVOC-Android/actions/runs/37864587660

All three runs were **IN PROGRESS** at the last observation during this checkpoint. Do not mark them PASSED or claim an installable v29 artifact until their completed results, resulting APK hash, signer fingerprint and manifest are inspected. Earlier superseded intermediate commits were automatically cancelled by CI concurrency and must not be interpreted as release test results.

 
## G. Verified v29 CI results — 2026-10-09

All three workflows below have **COMPLETED SUCCESS** for the identical **functional** source SHA `41dba5f781903c977cf4c08e024ae8b3a7b0b7bd`:

| CI run | Result | Evidence |
| --- | --- | --- |
| [37864587662](https://github.com/sohpany/SERVOC-Android/actions/runs/37864587662) | SUCCESS | Runtime source contract, Android JUnit (including new MQ health and kΩ contracts), ChemCore challenge report staging, `:app:assembleDebug`, apksigner verification, upload of working diagnostic APK |
| [37864587673](https://github.com/sohpany/SERVOC-Android/actions/runs/37864587673) | SUCCESS | Android internal build/validation |
| [37864587660](https://github.com/sohpany/SERVOC-Android/actions/runs/37864587660) | SUCCESS | Source and Android tests, Main ESP32 and ESP32-CAM firmware compilation and diagnostic packaging |

### Diagnostic Android APK, never production authority
- APK artifact: `HazRob-0.10.2-HR069-R1-RC2-WORKING-339-41dba5f781903c977cf4c08e024ae8b3a7b0b7bd.apk`.
- GitHub Actions artifact ID **11586928997**, retained on run [37864587662](https://github.com/sohpany/SERVOC-Android/actions/runs/37864587662).
- SHA-256 from CI's published `ARTIFACT-MANIFEST.txt` and `SHA256SUMS.txt`: `abcf6ab2dd38acebce1e0b65ec385fe05365bd94f1db91723ea5ee50b385b6b7`.
- Android application ID `com.servoc.app`; `versionName=0.10.2`, `versionCode=29`.
- Signing certificate SHA-256 `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`, unchanged from the v28 diagnostic certificate. CI reports `apk_signer_verification=PASS`.
- All artifact hashes here come from CI logs, **not an independent physical-device install or byte-for-byte external download**. Reverify downloaded files with SHA-256.

### Recompiled diagnostic firmware, no new functional firmware modification
- Main ESP32 and CAM compiles both PASSED in run [37864587660](https://github.com/sohpany/SERVOC-Android/actions/runs/37864587660) with Arduino-ESP32 core 3.3.11.
- The generic validation workflow created a Main ESP32 ZIP SHA-256 `33eaaf97e37bf1ae4382c77713b50d8f193866587eea6045ae6c08911da81ab0` and CAM ZIP SHA-256 `4324881d7e8138438aade303d6b6bdfc4cf74efec84a66cd400dc239223c9c81`. These archives are marked WORKING/UNAPPROVED and bear the historical HR069 wrapper prefix even though the unchanged Main ESP32 configuration declares **HR069-R1**. Do not confuse ZIP wrapper labels with actual firmware engineering revision.
- For Arduino IDE source, previously verified conventional `HazRob/HazRob.ino` ZIP at source `e6a14bc5...` and hash `6532c3495de23b1d4917ad64882519d3cc9c0cb42fd66634dc92059ba560058e` remains the original source handoff. **No Main or CAM firmware change or reflashing is required for these Android-only corrections.**

**Release gates still blocked:** measured board RL and R0/reproducibility, M4/reference-gas calibration, actual gas identity and tracking blind challenge, operator IR/Echo mechanical and electrical checks, direct/remote network hardware trials (Railway currently offline), and phone-opened session/site PDF/XLSX. Owner-controlled promotion remains explicitly NOT AUTHORIZED.
