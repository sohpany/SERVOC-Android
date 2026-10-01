# HazRob 0.10.2 / HR069 — Developer Workspace Rebuild

Status: ACTIVE — developer workspace source rebuild complete; qualification pass continues.
Branch: `release/hazrob-0.10.2`

This document is the execution log for the developer-workspace rebuild requested after the HR069 analytical-library expansion.

## Governing decisions

- Do not build the final APK until all affected stages have been re-qualified.
- GitHub is the canonical scientific-library platform. Render is not part of the ChemCore/library update path.
- Canonical private scientific repository: `HazRob/build-harness`.
- Operator library downloads must remain read-only and must not require a developer credential. A public GitHub distribution surface is therefore required before the final build; the organization currently exposes only the private `build-harness` repository.
- Developer uploads are allowed only from the developer workspace.
- GitHub upload credentials are never embedded in the APK. Device persistence uses AES-GCM with an Android Keystore key.
- R0 is physical robot/sensor-set calibration. It remains local and is excluded from central scientific publication.
- External/transfer PPM evidence is PARTIAL and must not be presented as HR069 M4.
- M4 is the gate for reportable quantitative PPM interpretation.
- MQ-7/MQ-9 cannot create an operational HR069 M4 curve until the required cyclic heater hardware is physically installed and qualified.

## Re-opened qualification stages

| Stage | Status | Scope |
|---|---|---|
| P0 | CLOSED | Baseline/release guard remains fixed. |
| P1 | IN REVIEW | Consolidate and classify new modifications. |
| P2 | SOURCE COMPLETE | App + HR069 + CAM + Direct/Remote protocol compatibility reviewed and reconciled; runtime/build qualification remains deferred to P12/P13. |
| P3 | SOURCE COMPLETE | Site/field/measurement session lineage, UTC + monotonic timestamps, exact experiment/capture linkage, R0 locality and canonical reference publication were reviewed and hardened; runtime qualification remains deferred. |
| P4 | BLOCKED EXTERNALLY | GitHub central library is implemented; public read-only GitHub distribution surface is still required. |
| P5 | REOPENED | Chemical-analysis engines against expanded library. |
| P6 | REOPENED | Evidence fusion/confidence regression. |
| P7 | TARGETED REVIEW | Live detection/alarm regression. |
| P8 | TARGETED REVIEW | Post-identification tracking regression. |
| P9 | REOPENED | Deep-session analysis regression. |
| P10 | REOPENED | PPM, M4, exposure limits and reportability gates. |
| P11 | REOPENED | PDF/Excel/reporting regression after M4/library changes. |
| P12 | PENDING | Integrated qualification after reopened stages close. |
| P13 | LOCKED | Final APK/build only after P1–P12 closure. |

## Developer workspace

Top-level developer tabs:

1. Experiments
2. Analytical Lab
3. Connection
4. Central Library

The duplicate Robot sub-tab has been removed.

### DEV-00 — Security and GitHub distribution

- [x] Android Keystore encrypted developer GitHub credential store.
- [x] Upload action wired to stored credential with first-use provisioning.
- [x] Routine session-token field removed from experiment workflow.
- [x] Render application/ChemCore update endpoints disabled; update discovery fails closed.
- [x] Approved-reference publication targets `HazRob/build-harness`.
- [x] Published approved experiments strip device-local R0 values.
- [ ] Public read-only GitHub distribution endpoint before final APK. **External blocker.**
- [ ] Review/remove the separate legacy build-time flammability recovery URL before P13. It is not an application/library update endpoint.

### DEV-01 — Experiments — SOURCE COMPLETE

- [x] Search by Arabic/English name or CAS.
- [x] Sortable chemical matrix: Chemical | Fingerprint | PPM | M4.
- [x] Statuses are read-only and tied to the activated library/evidence revision.
- [x] `✓ / ◐ / —` distinguishes READY, transfer/partial and absent evidence.
- [x] Suggested next experiment/priority.
- [x] R0 moved out of routine experiment flow into device maintenance.
- [x] MQ power/warm time read from telemetry.
- [x] Secure GitHub publication from developer mode.
- [x] Scientific approval, immutable raw publication, derived reference and publication audit preserved.
- [x] M4 requires explicit `HR069_M4` provenance; transfer PPM cannot become M4.
- [x] MQ-7/MQ-9 cannot create operational M4 while cyclic-heater hardware is absent.

Key commits: `71e0dcc3`, `3fe3295c`, `de878b5b`, `001df3b7`.

### DEV-02 — Analytical Lab — SOURCE COMPLETE

- [x] `Readiness and Results` renamed to `Results and Diagnostics`.
- [x] Readiness condensed to an operational status strip/card.
- [x] Qualitative Screening remains independent from M4 quantitative gating.
- [x] Presentation layer withholds reportable PPM unless the final quantitative gate passes.
- [x] Lab M4 counters use the same explicit HR069 M4 provenance as the experiment matrix.

Key commits: `cb90827a`, `5d1ec65f`, `60d0063d`.

### DEV-03 — Central Library — SOURCE COMPLETE / P4 EXTERNAL DEPENDENCIES OPEN

- [x] Duplicate operator-update controls replaced by central-library dashboard.
- [x] Dashboard sections: Library / Distribution / Platforms / Changelog.
- [x] Coverage metrics distinguish central evidence, local experiments, fingerprint coverage, READY fingerprints, PPM/transfer evidence, strict HR069 M4 and exposure-limit coverage.
- [x] Distribution/update telemetry contract available.
- [x] Platform-health/quota contract available without embedding service secrets.
- [x] Concise scientific-library changelog contract available.
- [x] GitHub-only / fail-closed status is visible.
- [ ] Global update telemetry needs a central collection endpoint after public distribution is enabled.
- [ ] Platform metrics need an Admin Metrics API or equivalent read-only backend.

Key commits: `65babb2e`, `ff901153`, `ab725a9e`, `996f6b94`.

### DEV-04 — Connection — SOURCE COMPLETE

- [x] Focused advanced connection diagnostics.
- [x] Direct/Remote, RSSI, IP, DNS, Time, Internet, TLS, MQTT, ACK and telemetry chain are visible.
- [x] WAN provisioning remains developer-only; ordinary robot controls are not duplicated.
- [x] Optional remembered WAN credentials use AES-GCM with Android Keystore.

Key commits: `6414ae62`, `ca20b1b9`.

### P2 — HR069 / CAM / protocol compatibility — SOURCE COMPLETE

- [x] Main firmware identity remains HazRob `0.10.2`, API4 and HR069 hardware profile.
- [x] ESP32-CAM remains `HazRob-CAM 0.10.2`, station-only on the HazRob AP, with QVGA raw capture feeding MJPEG/WSS; display/enhancement effects stay Android-side.
- [x] IR semantics are fail-safe: ground detected clears the cliff warning; loss of ground asserts the warning after the configured debounce count.
- [x] Ultrasonic telemetry preserves distance in cm, warning at the configured front-warning threshold and forward stop-zone status.
- [x] Forward safety auto-stop boots enabled by default while E-STOP and command-loss watchdog remain independent.
- [x] Direct HTTP commands and Remote MQTT commands expose equivalent drive, stop, E-STOP, relay, measurement, alert, config and safety-auto-stop operations.
- [x] Remote drive refresh policy remains inside the firmware command watchdog lease.
- [x] API4 normalization now preserves actual firmware motor outputs after safety interlocks, while keeping the operator request separately as `requested_left/right`.
- [x] API4 normalization preserves `stale_stop`, command age, forward hazard/block state and E-STOP state.
- [x] Remote telemetry propagates outer `safety.estop_latched` into the normalized snapshot without mutating the raw telemetry envelope.
- [x] Fixed eight-button turns use the approved 60% inner-side drive matrix; in-place pivots remain opposite-sign full commands.

Key commits: `27780e3d`, `5a7542b1`, `4363cf81`, `49a084d4`, `71fd1773`.

### P3 — Sessions, timestamps and scientific lineage — SOURCE COMPLETE

- [x] Modern 0.10.2 / HR069 experiment analysis resolves only the explicit `session_id`; timestamp-nearest fallback is legacy inspection only and is excluded from new reference generation, M4 and reports.
- [x] Measurement sessions persist UTC start/end timestamps and Android monotonic timing metadata; telemetry samples persist `session_t_ms`, monotonic receive time, UTC receive time and a sequence counter.
- [x] Developer reference evidence requires an exact match across experiment, READY-average capture, chemical UID, session ID and known concentration where applicable.
- [x] Reference lineage additionally requires a completed DEVELOPER measurement session with persisted telemetry samples.
- [x] HR069 capture provenance requires the current capture schema and hardware profile; R0 is explicitly device-local/non-distributable.
- [x] Database approval triggers fail closed when reference lineage is invalid.
- [x] Derived fingerprint/calibration artifacts revalidate every source experiment through the exact-lineage guard.
- [x] Canonical GitHub publication performs a second publication-boundary validation and remains append-only.
- [x] User field containers accept only `USER` + `field_*` measurement sessions; developer/scientific sessions cannot be attached accidentally.
- [x] Field-session closure refuses to close around a still-running linked measurement session.
- [x] Site-level intelligence counts recurrence across independent sessions but never averages PPM across sessions.

Key commits: `8d486781`, `35d0eaa8`, `06d08c25`, `502ab02c`, `335cce10`, `e38ce8ae`.

## Scientific invariants introduced by this rebuild

- Central material status never depends on device-local R0.
- External known-concentration evidence may produce PPM `PARTIAL`, never HR069 M4.
- Reportable quantitative status is based on explicit HR069 M4 provenance, not on a generic active-curve flag.
- Active HR069 M4 curves are generated only from approved, session-linked, READY-average HR069 captures with known concentration.
- A channel whose physical heater protocol is not qualified cannot become an operational M4 contributor.
- Modern HR069 scientific lineage is explicit-ID based; time proximity is never accepted as a substitute for a measurement-session identity.
- Independent field measurement sessions remain separate quantitative observations; site summaries may aggregate counts/recurrence but not PPM values.

## Validation policy

No final Gradle build, APK release or P13 closure is allowed while P1–P12 contain unresolved functional or external items. Source-level changes may be committed and statically reviewed during the rebuild. The final build/test/CI pass starts only after the user confirms the modification bundles are complete.
