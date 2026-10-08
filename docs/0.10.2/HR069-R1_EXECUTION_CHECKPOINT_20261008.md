# HazRob 0.10.2 / HR069-R1 — execution checkpoint (2026-10-08)

## Authority and immutable starting point
- Repository: sohpany/SERVOC-Android
- ONLY write branch: `release/hazrob-0.10.2-rc2`. Do not create a branch or merge the abandoned work branch.
- Baseline HEAD verified: `9b07abf1d31256aa369574614352bc9e3914c330`.
- Requested release: Android `0.10.2`, incremental engineering revision `HR069-R1`, Main ESP32 matching revision, API4 preserved, camera unchanged unless contract requires change.
- Revision commit IDs, tests, and unverified physical items MUST be appended below. No signed APK or flash package is considered released without CI + physical checks.

## Approved requirements (seven packages, updated)
1. ECHO: firmware TRIG23/ECHO39 interrupt capture; measurement and application telemetry at **150 ms nominal** in Direct and Remote; distinguish invalid/stale measurements; display in Control/Status; no invented new readings.
2. IR: GPIO35, **HIGH = GROUND / LOW = NO GROUND**. No software verification of toggling (operator will calibrate physically); the safety option is user-selectable and persisted, default OFF, with local and Remote status/control.
3. Controls: pivot-screen buttons invert only, plus separate reversing checkbox for ALL non-tank modalities (buttons, joystick, wheel); spring-to-center both independent tank sliders on UP/CANCEL; joystick must mix X and Y diagonally; remain safe on backgrounding and E-STOP.
4. Tracking: last **fresh unique measurement** for user-selected chemical; use target-specific chemical evidence, never indiscriminately general gas channels or stale reused snapshots; show arrow only when spatial gradient/pose quality is sufficient and explain missing arrow.
5. Sites/sessions: delete session AND its own user measurement data/derived records and backed-up archive, no impact on reference bank or other sites. Delete site only when empty; align restore and index/tombstone behavior.
6. Reports: ONLY one-field-session SmartReport or full-site-all-sessions SmartReport; NO arbitrarily selected multi-session subset. Central report tab selects Site then a Session or ALL site, with PDF/XLSX; session tab routes to same exporter; eliminate legacy operator report fallback; fix SAF noHistory/async/large-XLSX errors and validate files.
7. Brand: user-provided hexagon/target logo, export as transparent-background **PNG** (PNJ interpreted as PNG), preserve clean color and visual shape, use high resolution in entry UI and launcher. Don't break UI palette or SmartReport.
8. Status tab: show ECHO, IR and ALL installed devices, separate installed/OK/FAULT/STALE/NOT CALIBRATED and unknown, without treating unavailable sensors as safe.

## Delivery phases — update after every verified milestone
- P0: Baseline, requirements, checkpoint: COMPLETE / SHA ba19faf, subsequent corrections reflected here.
- P1: Firmware build_revision HR069-R1, 150ms Direct/Remote, Echo status, IR HIGH-ground, persisted selectable auto-stop: CODE CHANGED / CI+FIELD PENDING.
- P2: Spring return sliders, pivot button inversion and option for all non-tank controls: CODE CHANGED / CI+FIELD PENDING. Joystick diagonal runtime needs field acceptance.
- P3: Hardware status and selected-chemical unique MQ sample timestamp protection: CODE CHANGED / CI+FIELD PENDING.
- P4: Atomic user session delete with protected developer references, SAF tombstones; Developer vs USER role separation: CODE CHANGED / DB TEST PENDING.
- P5: Site/single-session scope UI and asynchronous site PDF/XLSX + ZIP-on-disk: CODE CHANGED / Android BUILD+EXPORT TEST PENDING.
- P6: Deterministic transparent PNG generator (1024×1024), launcher/splash references and source test: CODE CHANGED; CI + on-phone visual approval PENDING.
- P7: README updated, Android `versionCode 27` WORKING (approved baseline still 26), legacy contract assertions migrated; end-to-end build, firmware, physical and PDF/XLSX verification PENDING.

## Safeguards
- Direct AP and Remote MQTT are independent transports of the same normalized API4 fields.
- E-STOP, 500ms firmware dead-man stop, and navigation indicators must not regress. Physical ECHO 5V to GPIO39 must be level shifted; IR is on GPIO35/3.3V.
- The sensor's observed field RAW remained HIGH at floor height <4cm and at >70cm lifted. The operator specifically removed automatic verification; HIGH will be interpreted as ground without evidence that the detector is calibrated. **This is a residual physical hazard** until operator validates ground vs 70cm raised.
- SmartReport M4/PPM quantitative gating preserved; never average independent field-session PPM or turn ranking into probability.
- Deletion must never destroy developer/calibration/reference bank records or foreign data. Back up before destructive operations.
- Build should not publish an artifact until test passes; all physical-only acceptance gates remain explicitly PENDING.

## Progress log
- 2026-10-08 P0: verified RC2 head `9b07abf1`; reviewed firmware, Android protocol, controls, database, library/restore, report writers, CI, branding. Confirmed app forces HIGH=ground and firmware formerly LOW=ground; fixed by planning unified HIGH-ground with physical validation. Confirmed smart site exporter always includes every session; report tab currently chooses a recent experiment; SITE export Activity has noHistory=true. Original branding script overwrites legacy exporters.
- Initial review confirmed no existing unique firmware build id, MQTT telemetry default 500ms, direct poll default 250ms, firmware Echo period 150ms; a change to all three is required.
## Operator corrections after initial plan (2026-10-08)
- Removed IR “verified” / high-low-transition requirements in Main firmware, Android normalization and Control/Status indicators at explicit operator request. Software trusts firmware’s debounced raw polarity (HIGH = ground), without a claim of calibrated sensor health.
- Re-enabled NVS-backed `safetyAutoStopEnabled()` / `safetyAutoStopSet()`. Direct GET/SET `/safety/auto_stop` and Remote MQTT commands share the same persisted state. The Settings checkbox issues real API requests, reads the actual result and does NOT force OFF when applying other settings.
- Removed user-facing `syncUserLibrary()`/folder/restore controls and implicit site creation/close sync from `FieldSessionsTab`. Operator sites/sessions remain SQLite-local, not published to GitHub. Local SAF backup/restore code stays separate from central science publication.
- Separated telemetry capture role from merely having unlocked Developer. Explicit developer-initiated averaging is tagged in `MeasurementSessionRegistry`; `ExperimentQueries.recentDeveloper` and `isDeveloperReference` guard the developer list and publish actions, while `recentUser` scopes operator reports.
- Updated `README.md` with active HR069-R1, Main firmware revision, camera unchanged, and safety/role caveats.
- Release/RC2 audit scripts required controlled updates because previous audits enforced `IR_GROUND_ACTIVE=LOW`, forced-stop OFF, and fixed HR069. Do not bypass tests; update assertions to the new explicit approved contract.
- No reliable physical sensor response, APK acceptance, Excel export, or downloadable production firmware confirmed yet.

## Current handoff
- Most recent documented implementation baseline prior to this checkpoint: README commit `7f2496360dabe0cdf71eab607e538e249e0dfc8c`.
- Only write branch: `release/hazrob-0.10.2-rc2`.
- Next: address any genuine GitHub Actions build/test failures and update remaining historical audit contracts; verify Android/Main ESP32 compilation and export fixtures; validate local/developer role isolation and no central USER uploads. Merge no branches.
- Asset P6: generate **faithful** transparent exterior PNG from the supplied approved hexagon-target image, not a stylistically modified substitute.
- Final gate: IR physical polarity and accurate Echo measurements on device, manual on/off forward interlock, Direct/Remote, PDF/XLSX openability, E-STOP, motor-deadman. Do not label those tests PASS without device or real CI logs.


## CR-20261008-006 checkpoint — Developer publication / ChemCore library update audit
- Registered scientific-library compatibility and destination review: [`CR-20261008-006`](CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md), baseline `9eeeab979c4c1bc350c789a67d7dd85cdd1b3d68`.
- Authority reconciled against build-harness `CURRENT_RELEASE_AUTHORITY.md` and mandatory governance: only `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_analytical_library/` is canonical for analytical material.
- Confirmed current `GitHubReferencePublisher.java` writes to a superseded organization library location; rejected any claim that this provides canonical publication. No live upload, reference promotion or change to user files executed.
- Confirmed `CHEMCORE_LIBRARY_MANIFEST_URL` empty in production Gradle and legacy publication workflows retired. Read-only Stable updater is designed but its end-to-end runtime distribution remains UNVERIFIED.
- Implemented dedicated developer-session update interlock at SHA `5d4c545e7152f4e1bb8779b526268a00b69003e0` and accompanying unit regression at SHA `e0c47aed51b873a793b393480be37baf7e247f31`: neither firmware-idle controlled captures nor active local recordings may activate a new Stable package.
- No older-than-0.10.2/HR069 code used; no new branch. APK/Main firmware/physical/Stable end-to-end release gates all PENDING.

## 2026-10-08 — continued execution after project-owner approval

- Developer and user session isolation: both `DeveloperExperimentsTab` and legacy unused `ExperimentsTab` use persisted `DEVELOPER` role, explicit developer capture origin and `ReferenceLineageGuard`; ordinary user sessions stay local and cannot publish.
- Reference submission destination switched from obsolete `HazRob/build-harness/library` to *non-runtime* `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_developer_submissions/`. Submission filenames include session and payload hash. The publisher validates raw developer lineage, local approval, immutable source integrity, derived source lineage and derived SHA-256.
- Developer UI labels send action **scientific review submission only** and records `STAGED_FOR_REVIEW` rather than `PUBLISHED`. It does not alter the canonical accepted corpus or the installed Stable library. No actual reference upload performed in this session.
- CR-20261008-006 identifies the missing science-admission pipeline and public read-only Stable manifest endpoint. The retired GitHub workflows remain retired. ChemCore updates cannot be called fully operational until an approved release is published and installed end-to-end.
- Branding step generates a 1024px alpha PNG with opaque black *inside* the golden hexagon and transparent black *outside*. `AndroidManifest.xml` and `ConnectionGate.java` reference `ic_hazrob_hr069_r1`. The Gradle branding task tracks the generator output. Legacy ReportExporter/XlsxReportWriter source mutation was removed from `tools/apply_hazrob_branding.py`.
- Android working build identity: `versionName 0.10.2`, `versionCode 27`, `ReleaseAudit.ENGINEERING_REVISION HR069-R1`. Main ESP32 API4/firmware banner 0.10.2 with separate build_revision HR069-R1, ESP32-CAM unchanged. This working code is NOT a new approved release.
- CI before these fixes exposed 3 unit contract problems (ReleaseAudit revision, invalid MQ channel tracking reason, obsolete report UI assertions) and 2 compile errors from the unused ExperimentsTab legacy publisher signature. These were corrected; re-run CI and retain all failures in audit trail.
- Resume directly on the same RC2 branch. Check latest HEAD and `docs/0.10.2/CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md`. Do not touch versions older than 0.10.2/HR069, do not create branches, do not promote or publish unqualified data.

## Continuation checkpoint — Android/firmware validation, report safety and brand

- Operator release approval **NOT granted**: approved source + APK remain the earlier controlled 0.10.2/HR069 vCode26 set. Working Android vCode27/HR069-R1 must remain labeled DIAGNOSTIC until all gates and user owner approval.
- Android logo: alpha-transparent exterior 1024px PNG generated deterministically; `ic_hazrob_hr069_r1.png` selected in AndroidManifest and ConnectionGate. The generator runs in Gradle preBuild and is declared as input/output. The helper no longer silently rewrites legacy report exporters. Android JUnit PNG test was adjusted to avoid `java.awt.image`/ `javax.imageio` absent on Android unit-test compile classpath.
- Major report defect remediated in source: `MainActivity.requestExport` routes an explicitly selected USER experiment to its own linked Field Session SmartReport; laboratory standalone exports remain Developer-only. `MeasurementsTab` now uses the latest completed actual measurement-session ID, not an unrelated last experiment. `requestFieldSessionExport` and `SiteSummaryExportActivity` do not assemble report bundles on the Android main/UI thread before SAF.
- Drive focus-loss safety: `MainActivity.onPause` now neutralizes motor commands to (0,0), resets control UI and requests independent motion stop; it does NOT stop ongoing sensor experiments. Firmware watchdog/E-STOP remain independent. Unit contract `HR069R1LifecycleDeadmanContractTest` added.
- Additional regression `HR069R1UnifiedReportEntryContractTest` locks explicit field linkage, nonblocking PDF/XLSX export picker and no fallback to stale last experiment.
- Operator audit `tools/audit_hr069_operator_ui_contract.py` corrected from obsolete **latching** tank expectation to operator-approved spring-return, plus HIGH-ground polarity and HR069-R1 report title.
- CI run 37847710897/37847710818 on obsolete intermediate SHA a879fae5 failed at Android JUnit test compile due AWT imports; fixed at SHA 270ba6ff. Source audit intermediate run 37847710795 failed at the historical latching-tank assertion; fixed at SHA 52ae4179. Intermediate failures are retained in Actions history; subsequent CI is required.
- GitHub Actions builds and ESP32/ESP32-CAM compiles for **latest current RC2 SHA** must complete before claiming any software acceptance. Physical robot trials (IR ground/cliff manual adjustment; Echo 5cm/10cm/near/far; 150ms cadence; dual tank release; joystick diagonal; loss of foreground; Direct/Remote and E-STOP), PDF/XLSX openability and on-phone PNG visual approval remain mandatory and PENDING.

## 2026-10-08 additional request — editable R0 and conventional Arduino source ZIP

- User requests a *mutable* R0 per each of eight MQ channels, to calibrate after batteries are charged. New developer-only `DeveloperR0Tab` integrated into `DeveloperTab`; independent override/save/reset and one-batch save.
- `ManualR0Calibration` now uses synchronous durable SharedPreferences `commit()` (never reports success if storage fails), an all-or-nothing `saveAll` for all eight positive finite values, per-channel `reset` to the existing accepted clean-air baseline, and UTC/session lineage. Loaded values are reflected in normalized `Rs/R0` on subsequent snapshots and retain `r0_reference_validated=false`.
- `HR069R1EditableR0PersistenceTest` covers single-channel update, reloading after simulated app restart, sensor-specific `Rs/R0` recalculation, atomic eight-channel revision, invalid values, failed commits, and restore fallback.
- R0 stays on the Android phone under `SharedPreferences servoc`, separate from embedded/installed ChemCore Stable. In-place update with same applicationId and signing key preserves R0. Explicitly warn users that *uninstall/reinstall* can erase stored R0 if it has not been backed up.
- One-branch exact-revision packaging workflow `.github/workflows/package-hr069-r1-arduino-source.yml` added. Compiles Main ESP32 with core 3.3.11 then exports ZIP with Arduino-recognized `HazRob/HazRob.ino`, `.h`, `.cpp`, guide and SHA-256. APK remains a distinct artifact validated from the same current source SHA.
- Pending acceptance: latest-sha Android unit tests (including editable R0 and embedded Stable), one-sha Arduino source ZIP and diagnostic APK, manifest SHA/certificate validation, user physical calibration and report-device functional trial. NEVER relabel diagnostic artifacts as qualified production.

## Distribution handoff checkpoint — Arduino ZIP + Android R0/Stable APK (2026-10-08)

**Branch:** `release/hazrob-0.10.2-rc2`; development increment `HR069-R1`. NO newer branch, NO pre-HR069 code. These remain WORKING / UNAPPROVED / physical validation outstanding.

### Conventional Arduino IDE source — verified CI
- GitHub Actions run: https://github.com/sohpany/SERVOC-Android/actions/runs/37852367741
- Exact source commit: `e6a14bc570c335b824c153c7cd7fd2677b9bd005`
- ZIP name: `HazRob-0.10.2-HR069-R1-ESP32-Arduino-e6a14bc570c335b824c153c7cd7fd2677b9bd005.zip`
- ZIP SHA-256: `6532c3495de23b1d4917ad64882519d3cc9c0cb42fd66634dc92059ba560058e`
- ZIP validated by decompression + CRC; contains `HazRob/HazRob.ino`, all project `.h/.cpp`, serial/board instructions and build identity. Compiled with ESP32 Arduino core 3.3.11, board `esp32:esp32:esp32:PartitionScheme=huge_app`; physical serial upload UNTESTED.

### WORKING diagnostic Android APK — verified CI
- GitHub Actions run: https://github.com/sohpany/SERVOC-Android/actions/runs/37852241553
- Exact Android compiled source commit: `42a379bd15b7f12d4f4236383bacb5033d14bae2` (R0 UI and persistence included; later firmware workflow/docs commits do not affect that Android binary)
- Original artifact filename: `HazRob-0.10.2-HR069-R1-RC2-WORKING-317-42a379bd15b7f12d4f4236383bacb5033d14bae2.apk`
- APK SHA-256: `766a7768352a3218a3bbcd7f20f06f053b84b2aa799c334f07a6f6e96bb9692b`
- package name `com.servoc.app`, `versionName 0.10.2`, `versionCode 27`. Signing certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- CI asserts: runtime contracts PASS, Android unit tests PASS, APK build PASS, `apksigner verify` PASS. APK decompressed with CRC PASS.
- APK includes `DeveloperR0Tab`, `ManualR0Calibration.saveAll`, `ChemCoreEmbeddedStableInstaller` in DEX and `assets/generated/chemcore/embedded_stable/manifest.json` + `chemcore-stable-snapshot.json`.
- Embedded Stable: `1.0.0-hr069r1`, `chemcore-stable-snapshot-v1`, 652 chemical identities, 49 non-M4 research response priors, 37 identity-curation records, zero approved device-specific M4 calibrations. Stable payload SHA-256 `94f41ba754a1c239122b82081a91e9656012503cebda0eae6473ed8a4e45d7cf` verified against bundled manifest; this is **research screening knowledge**, not quantitative exposure certification.
- New local editable R0 supports MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135 with atomic positive-finite batch update, single-channel commit, reset/fallback and UTC/session provenance. It persists for in-place signed APK updates; an uninstall may erase internal SharedPreferences and MUST NOT be recommended as a routine upgrade.
- Physical tests still NOT PERFORMED: sensor polarity/response, 150ms transmission, ECHO validity, navigation/driving, real R0 clean-air calibration, battery-load stability, PDF/XLSX phone opening, camera, remote drive. No claim of field safety or M4 quantitative ppm.
- Approved production baseline remains unchanged. Working diagnostic artifacts do not promote themselves. Review/manually flash Main ESP32 and test while wheels raised; after sensor tuning and device validation only consider a release approval.

## Continuation — vCode28 corrective revision: archived R0 scientific reproducibility and early SmartReport

**Change target:** Same `0.10.2 / HR069-R1` branch `release/hazrob-0.10.2-rc2`; Android versionCode `28` (the previously delivered working diagnostic was vCode27). Main ESP32 `HR069-R1` sketch remains untouched after the verified Arduino IDE ZIP; no unnecessary re-flash requested.

### Defect A: retroactive recalibration of archived measurements

- Root cause: `ChemCoreSessionFeatureAdapter.extract()` reused `AcceptedR0Normalizer.ratio()` against **current** `ManualR0Calibration.get()` when replaying persisted `telemetry_samples`. `SessionSeries.normalizeMq()` also overwrote saved sample `r0` with the **fixed historic field baseline**, contaminating SmartReport graph/Excel provenance. `ChemCoreQuantitativeTimeSeries.extract()` repeated the current-R0 recalculation even for historical M4 time-series.
- Remediation: Introduced read-only `AcceptedR0Normalizer.recordedRatio/availableRecordedRatio/usesRecordedDeviceR0`, using the positive `rs/r0` **within the exact recorded sample**, or that sample's frozen `ratio` when no saved R0 exists. Missing R0+ratio is *not* manufactured from the phone's current R0.
- All three historical readers now explicitly use the persisted snapshot for strict and corroborative evidence. `SessionSeries.normalizeMq()` keeps the original R0, time, source, provenance and M4 marker intact and adds `historical_calibration_frozen` plus `historical_ratio_source`.
- `DeveloperR0Tab` rejects saves/resets during active firmware acquisition or local ChemCore measurement, to avoid within-session calibration changes. Future live snapshots still normalize using the freshly edited phone-local R0.
- Tests updated/added: `SessionSeriesNormalizationTest`, `HR069R1EditableR0PersistenceTest.changingDeviceR0NeverReinterpretsAnArchivedSession` and `HR069R1HistoricalR0LineageContractTest` guard all three historical readers, live write lock, and no synthetic historical inputs.

### Defect B: Excel blocked when an operator has a field session but no derived ChemCore record

- Removed only the artificial `b.records.isEmpty()` blocker from `FieldSessionReportExporter.writeXlsx`. Existing USER field session identity, telemetry and quality evidence remain available in the 11-tab SmartReport workbook; absent chemical analysis stays explicitly missing, never fabricated.
- Source regression contract `FieldSessionReportExporterContractTest.excelRemainsAvailableBeforeDerivedChemCoreExperiment` prevents reintroducing this check.
- Existing scientific distinction is unchanged: no M4/validated exposure conclusions solely from R0 or M1/M2 response priors.

### Build checkpoints

- Latest functional source SHA as of this checkpoint: `64669f4eb4d67f869db3f745b2332f1e36caa179`; CI build runs:
  - diagnostic APK: https://github.com/sohpany/SERVOC-Android/actions/runs/37853945739
  - internal release APK: https://github.com/sohpany/SERVOC-Android/actions/runs/37853945611
  - combined development audit and firmware recompile: https://github.com/sohpany/SERVOC-Android/actions/runs/37853945618
- **NOT YET ACCEPTED** until at least vCode28 debug APK build, APK signer, all JUnit tests and source audit succeed. Source checkpoint documentation commits may advance branch HEAD without changing compiled application source; compare the exact tested SHA with functional changes, not README SHA alone.
- Both scientific data stores are intentionally unchanged: APK embedded ChemCore Stable `1.0.0-hr069r1` and local user R0 values; no central developer experiments uploaded.
- Physical Android/R0 measurement and exported PDF/XLSX openability are still pending bench trials.

