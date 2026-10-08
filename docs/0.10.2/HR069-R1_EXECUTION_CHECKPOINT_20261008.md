# HazRob 0.10.2 / HR069-R1 — execution checkpoint (2026-10-08)

## Authority and immutable starting point
- Repository: sohpany/SERVOC-Android
- ONLY write branch: `release/hazrob-0.10.2-rc2`. Do not create a branch or merge the abandoned work branch.
- Baseline HEAD verified: `9b07abf1d31256aa369574614352bc9e3914c330`.
- Requested release: Android `0.10.2`, incremental engineering revision `HR069-R1`, Main ESP32 matching revision, API4 preserved, camera unchanged unless contract requires change.
- Revision commit IDs, tests, and unverified physical items MUST be appended below. No signed APK or flash package is considered released without CI + physical checks.

## Approved requirements (seven packages, updated)
1. ECHO: firmware TRIG23/ECHO39 interrupt capture; measurement and application telemetry at **150 ms nominal** in Direct and Remote; distinguish invalid/stale measurements; display in Control/Status; no invented new readings.
2. IR: GPIO35, **LOW means NO GROUND** (HIGH means ground present *only after sensor physical validation*); distinguish stuck/nonresponsive detector from proven ground, and expose IR/ground states. No untested automatic stop enable.
3. Controls: pivot-screen buttons invert only, plus separate reversing checkbox for ALL non-tank modalities (buttons, joystick, wheel); spring-to-center both independent tank sliders on UP/CANCEL; joystick must mix X and Y diagonally; remain safe on backgrounding and E-STOP.
4. Tracking: last **fresh unique measurement** for user-selected chemical; use target-specific chemical evidence, never indiscriminately general gas channels or stale reused snapshots; show arrow only when spatial gradient/pose quality is sufficient and explain missing arrow.
5. Sites/sessions: delete session AND its own user measurement data/derived records and backed-up archive, no impact on reference bank or other sites. Delete site only when empty; align restore and index/tombstone behavior.
6. Reports: ONLY one-field-session SmartReport or full-site-all-sessions SmartReport; NO arbitrarily selected multi-session subset. Central report tab selects Site then a Session or ALL site, with PDF/XLSX; session tab routes to same exporter; eliminate legacy operator report fallback; fix SAF noHistory/async/large-XLSX errors and validate files.
7. Brand: user-provided hexagon/target logo, export as transparent-background **PNG** (PNJ interpreted as PNG), preserve clean color and visual shape, use high resolution in entry UI and launcher. Don't break UI palette or SmartReport.
8. Status tab: show ECHO, IR and ALL installed devices, separate installed/OK/FAULT/STALE/NOT CALIBRATED and unknown, without treating unavailable sensors as safe.

## Delivery phases — update after every verified milestone
- P0: Baseline, requirements, checkpoint: STARTED (this document).
- P1: Safe version stamping, 150ms telemetry and Echo/IR semantics: PENDING.
- P2: Drive sliders, pivot button, inverse-options, joystick: PENDING.
- P3: Status and target-gas tracking: PENDING.
- P4: destructive user-session deletion with archive integrity: PENDING.
- P5: unified full-site/single-session SmartReport and crash fixes: PENDING.
- P6: PNG brand asset and entry/launcher: PENDING.
- P7: regression/unit/integration/build/release verification, README and handover: PENDING.

## Safeguards
- Direct AP and Remote MQTT are independent transports of the same normalized API4 fields.
- E-STOP, 500ms firmware dead-man stop, and navigation indicators must not regress. Physical ECHO 5V to GPIO39 must be level shifted; IR is on GPIO35/3.3V.
- The sensor's observed field RAW remained HIGH at floor height <4cm and at >70cm lifted. A HIGH reading is **not demonstrated to be safe**; show unvalidated/untrusted until physical switch test passes.
- SmartReport M4/PPM quantitative gating preserved; never average independent field-session PPM or turn ranking into probability.
- Deletion must never destroy developer/calibration/reference bank records or foreign data. Back up before destructive operations.
- Build should not publish an artifact until test passes; all physical-only acceptance gates remain explicitly PENDING.

## Progress log
- 2026-10-08 P0: verified RC2 head `9b07abf1`; reviewed firmware, Android protocol, controls, database, library/restore, report writers, CI, branding. Confirmed app forces HIGH=ground and firmware formerly LOW=ground; fixed by planning unified HIGH-ground with physical validation. Confirmed smart site exporter always includes every session; report tab currently chooses a recent experiment; SITE export Activity has noHistory=true. Original branding script overwrites legacy exporters.
- Initial review confirmed no existing unique firmware build id, MQTT telemetry default 500ms, direct poll default 250ms, firmware Echo period 150ms; a change to all three is required.
- Awaiting implementation logs below.
