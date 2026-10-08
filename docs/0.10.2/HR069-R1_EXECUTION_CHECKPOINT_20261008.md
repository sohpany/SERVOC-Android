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
- P6: PNG brand asset and entry/launcher: PENDING.
- P7: README updated, selected legacy tests/audits migrated to HR069-R1; GitHub Actions initially failed on obsolete pinned contract assertions; compile/physical gates PENDING.

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

