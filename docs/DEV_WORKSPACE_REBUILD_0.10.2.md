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
| P2 | REOPENED | App + HR069 + CAM + protocol compatibility. |
| P3 | TARGETED REVIEW | Sessions, timestamps and scientific lineage. |
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

## Scientific invariants introduced by this rebuild

- Central material status never depends on device-local R0.
- External known-concentration evidence may produce PPM `PARTIAL`, never HR069 M4.
- Reportable quantitative status is based on explicit HR069 M4 provenance, not on a generic active-curve flag.
- Active HR069 M4 curves are generated only from approved, session-linked, READY-average HR069 captures with known concentration.
- A channel whose physical heater protocol is not qualified cannot become an operational M4 contributor.

## Validation policy

No final Gradle build, APK release or P13 closure is allowed while P1–P12 contain unresolved functional or external items. Source-level changes may be committed and statically reviewed during the rebuild. The final build/test/CI pass starts only after the user confirms the modification bundles are complete.
