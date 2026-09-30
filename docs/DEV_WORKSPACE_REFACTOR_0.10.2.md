# HazRob 0.10.2 / HR069 — Developer Workspace Refactor

Status: ACTIVE
Branch: `release/hazrob-0.10.2`
APK/build gate: CLOSED until P13.

## Fixed architecture decisions

1. Developer workspace tabs are: Experiments, Analytical Lab, Connection, Central Library.
2. Duplicate Robot developer subtab is removed; operator robot functions stay in normal user UI.
3. Central scientific source of truth is the HazRob GitHub organization library.
4. Render is not an update source for ChemCore. Library distribution is GitHub-only.
5. R0 is device/sensor-local calibration state. It is never promoted as distributable central scientific knowledge and never overwrites another device's R0.
6. Approved experiments and derived fingerprint/calibration references may be published only from developer mode.
7. GitHub upload credential is never embedded in the APK. It is provisioned on the developer device and stored encrypted with a device-bound Android Keystore key.
8. User library updates remain read-only and require no developer credential.
9. External/transfer PPM evidence does not imply M4. M4 is a separate HR069 quantitative gate.
10. No build, APK, CI qualification or final release acceptance until all affected phases are re-audited.

## Developer refactor work packages

- DEV-01: inventory every developer button/text and remove duplication/dead setup prose.
- DEV-02: Chemical Experiment Matrix with live name/CAS search and sortable `Material | Fingerprint | PPM | M4` status columns.
- DEV-03: move R0 from daily experiment flow to local maintenance/calibration status.
- DEV-04: rename Analytical Lab secondary page from Readiness/Results to Results/Diagnostics; readiness becomes compact status.
- DEV-05: reduce MQDataScience 76-model wall of text to compact runtime status + optional detail.
- DEV-06: connection tab keeps only advanced diagnostics not duplicated for operators.
- DEV-07: Central Library dashboard with scientific coverage statistics.
- DEV-08: synchronize material status from central-library index/revision.
- DEV-09: library distribution/update telemetry dashboard.
- DEV-10: platform status/usage dashboard without embedding provider secrets in APK.
- DEV-11: compact library change log.
- DEV-12: RTL/contrast/action-state/error-message cleanup.
- DEV-13: scientific policy guards for R0, transfer PPM and M4.
- DEV-14: source/contract regression suite; build remains deferred.

## Reopened release phases after the refactor

| Phase | State | Scope |
|---|---|---|
| P0 | CLOSED | Baseline/release freeze remains valid. |
| P1 | REOPENED | Collect/classify all new modifications. |
| P2 | REOPENED | App + HR069 + CAM + protocol compatibility. |
| P3 | TARGETED REVIEW | Sessions, timestamps and experiment lineage. |
| P4 | REOPENED | ChemCore central library + GitHub-only distribution/update. |
| P5 | REOPENED | Chemical analysis engines against expanded library. |
| P6 | REGRESSION | Evidence Fusion / confidence after evidence changes. |
| P7 | REGRESSION | Instant sensing/alarm isolation. |
| P8 | REGRESSION | Post-identification tracking isolation. |
| P9 | REOPENED | Deep session analysis. |
| P10 | REOPENED | PPM + occupational limits + M4 reportability gates. |
| P11 | REOPENED | UI, PDF/Excel and developer workspace/report integration. |
| P12 | PENDING | Full-system qualification after affected phases pass. |
| P13 | LOCKED | Final APK/build only after P12 closure. |

## Execution log

- 2026-09-30: duplicate Robot developer subtab removed.
- 2026-09-30: central publisher target changed to `HazRob/build-harness`.
- 2026-09-30: Android Keystore encrypted developer GitHub credential store added; no token embedded in APK.
- 2026-09-30: this refactor plan and release-regression map established.
