# HazRob 0.10.2 / HR069 — Developer Workspace Rebuild

Status: ACTIVE
Branch: `release/hazrob-0.10.2`

This document is the execution log for the developer-workspace rebuild requested after the HR069 analytical-library expansion.

## Governing decisions

- Do not build the final APK until all affected stages have been re-qualified.
- GitHub is the canonical scientific-library platform. Render is not part of the ChemCore/library update path.
- Canonical private scientific repository: `HazRob/build-harness`.
- Operator library downloads must remain read-only and must not require a developer credential. A public GitHub distribution surface is therefore required before the final build; the current organization has only the private `build-harness` repository.
- Developer uploads are allowed only from the developer workspace.
- GitHub upload credentials must never be embedded in the APK. Device-side persistence uses Android Keystore encryption.
- R0 is device/sensor calibration. It is local to a physical HazRob sensor set and is not distributed as central scientific knowledge.
- External/transfer PPM evidence must not be presented as HR069 M4.
- M4 remains the gate for reportable quantitative PPM interpretation.

## Re-opened qualification stages

| Stage | Status | Scope |
|---|---|---|
| P0 | CLOSED | Baseline/release guard remains fixed. |
| P1 | REOPENED | Consolidate and classify new modifications. |
| P2 | REOPENED | App + HR069 + CAM + protocol compatibility. |
| P3 | TARGETED REVIEW | Sessions, timestamps and scientific lineage. |
| P4 | REOPENED | ChemCore central library, GitHub-only distribution and synchronization. |
| P5 | REOPENED | Chemical-analysis engines against expanded library. |
| P6 | REOPENED | Evidence fusion/confidence regression. |
| P7 | TARGETED REVIEW | Live detection/alarm regression. |
| P8 | TARGETED REVIEW | Post-identification tracking regression. |
| P9 | REOPENED | Deep-session analysis regression. |
| P10 | REOPENED | PPM, M4, exposure limits and reportability gates. |
| P11 | REOPENED | Developer UI, PDF/Excel and reporting. |
| P12 | PENDING | Integrated qualification after reopened stages close. |
| P13 | LOCKED | Final APK/build only after P1–P12 closure. |

## Developer workspace target

Top-level developer tabs:

1. Experiments
2. Analytical Lab
3. Connection
4. Central Library

The duplicate Robot sub-tab has already been removed.

### DEV-00 — Security and GitHub distribution

- [x] Add Android Keystore encrypted developer GitHub credential store.
- [ ] Wire upload action to stored credential; first-use provisioning only.
- [ ] Remove session token field from routine experiment workflow.
- [ ] Remove Render from ChemCore/library update configuration.
- [ ] Establish public read-only GitHub library distribution endpoint before final APK.

### DEV-01 — Experiments

- [ ] Search by Arabic/English name or CAS.
- [ ] Sortable chemical matrix: Chemical | Fingerprint | PPM | M4.
- [ ] Statuses are read-only and synchronized from the active central-library revision.
- [ ] Distinguish verified, transfer/partial and absent quantitative evidence.
- [ ] Prioritize next useful experiment.
- [ ] Move R0 out of the routine experiment path into device calibration/maintenance.
- [ ] Read MQ power/warm time from telemetry when available instead of requiring manual entry.
- [ ] Preserve scientific approval, immutable raw record, derived reference and publication audit.

### DEV-02 — Analytical Lab

- [ ] Rename `Readiness and Results` to `Results and Diagnostics`.
- [ ] Reduce readiness to operational status strip.
- [ ] Keep qualitative screening available independently from M4 quantitative gating.
- [ ] Surface candidate, posterior/support, residual/OOD/mixture and quantitative result only when reportable.

### DEV-03 — Central Library

- [ ] Replace duplicate operator-update controls with central-library administration/dashboard.
- [ ] Coverage metrics: total, fingerprint, PPM evidence, M4, tested/unverified, untested, exposure limits, derived references.
- [ ] Distribution/update metrics.
- [ ] Platform health/quotas summary without embedding service secrets.
- [ ] Concise scientific-library changelog.

### DEV-04 — Connection

- [ ] Keep only developer diagnostics and advanced transport controls.
- [ ] Remove functionality already exposed to normal users.

## Validation policy

No final Gradle build, APK release or P13 closure is allowed while this document contains unresolved functional items affecting P1–P12. Source-level changes may be committed and statically reviewed during the rebuild.
