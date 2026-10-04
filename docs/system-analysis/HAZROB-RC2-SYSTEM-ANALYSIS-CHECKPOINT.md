# HazRob RC2 Full System Analysis — Checkpoint

## Tracking
- GitHub issue: #8 — `HazRob RC2 — Full System Architecture & Systems Analysis Report`
- Working documentation branch: `docs/hazrob-rc2-system-analysis`
- Governing RC2 analysis baseline frozen at task start: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
- Controlled executable/release authority is reported separately from `HazRob/build-harness/CURRENT_RELEASE_AUTHORITY.md`; branch HEAD movement alone is not release authority.

## Source inventory analyzed
### Personal GitHub installation (`sohpany`)
1. `sohpany/SERVOC-Android` — primary Android, firmware, ChemCore, scientific corpus, reports, update logic, tests and tools; latest RC2 analysis source.
2. `sohpany/HazRob` — minimal project-description repository; not an RC2 runtime authority.
3. `sohpany/HazRob-Lab` — FROZEN / historical scientific provenance only since 2026-10-01. New scientific authority moved to the production repository.

### Organization GitHub installation (`HazRob`)
4. `HazRob/build-harness` — governance/build/test/qualification/release-evidence repository. It is not the production runtime repository and not the canonical scientific library.

## Verified authority boundaries
- Latest RC2 engineering state analyzed: `sohpany/SERVOC-Android@5250ea769615ec82c5dc944576ea3042736b3db2`.
- Controlled executable/release approval is a separate governance concept determined by organization release-authority records.
- Controlled engineering-approved executable source recorded during analysis: `43a01d86c260353a77f1a45f808f22cf46997e06`.
- Canonical active analytical corpus: `sohpany/SERVOC-Android/research_data/hr069_analytical_library/`.
- `HazRob-Lab` is frozen historical provenance only.
- `HazRob/build-harness` provides governance/build/test/qualification/release evidence only; workflows cannot autonomously promote release/scientific authority.
- Mandatory lifecycle: `REGISTER -> IMPLEMENT CONTROLLED -> TEST -> TRIAL/VALIDATE -> REVIEW -> APPROVE -> PROMOTE/RELEASE -> IMMUTABLE MANIFEST`.

## Major findings captured in the report
1. Safety-policy divergence: IR/cliff and ultrasonic telemetry warnings remain active while automatic IR/ultrasonic motion stopping is disabled in current RC2 firmware; E-STOP and stale-command watchdog remain independent.
2. MQ7/MQ9 quantitative limitation: required cyclic-heater driver is not installed, so manufacturer heater-cycle quantitative validity fails closed.
3. R0 normalization is explicitly separated from M4/reference-gas calibration.
4. Remote-security technical debt: prototype bootstrap identity/credential material must be replaced before production-grade security claims. Secret values are intentionally excluded from the report.
5. Release-signing technical debt: prototype signing/key management is not a production signing architecture.
6. Android cleartext scope is broader than required for Direct AP operation.
7. Authority-document drift exists between some older status documents and the current release-authority record.
8. Build provenance retains a controlled fallback dependency on a legacy APK for one seed if the local source asset is absent.
9. SAF field-session storage has strong immutable-ID lineage but can be strengthened with archive integrity manifests/hashes.
10. ChemCore maintains strong semantic gates: M3 identity-only, M4 required for reportable quantitative ppm, OOD fail-closed, evidence-lineage de-duplication, and SmartReport contract enforcement.

## Phases and final status
- [x] P1a — Freeze RC2 baseline
- [x] P1b — Create tracking issue and working documentation branch
- [x] P1c — Discover connected personal and organization repositories
- [x] P1d — Inspect repository metadata/heads and top-level trees, including accessible `HazRob` organization contents
- [x] P2 — Full RC2 production/governed file tree and subsystem classification
- [x] P3 — Application/UI/domain architecture analysis
- [x] P4 — ChemCore/analytical/data/reference architecture analysis
- [x] P5 — Firmware/hardware/protocol/connectivity architecture analysis
- [x] P6 — Storage/databases/reports/update/security/CI-harness analysis
- [x] P7 — Cross-system dependencies, flows, failure modes and risk review
- [x] P8 — Recommendations, target architecture and remediation roadmap
- [x] P9 — DOCX/PDF authoring, 15 diagrams, rendering and visual QA

## Final deliverables
- Word: `HazRob_RC2_Full_System_Analysis_0.10.2_HR069.docx`
  - SHA-256: `16e7bce999d3770db5b175a785f43d8797719dadb79518975a2fe776be17c4f0`
- PDF: `HazRob_RC2_Full_System_Analysis_0.10.2_HR069.pdf`
  - SHA-256: `daef9efe72c22fb2c148f0a93ae616a20e56678f6357cdfb8f83934d078d2295`
  - Final page count: 77
- Architecture/process diagrams: 15
- File/component inventory records: 487
- Visual QA: all 77 rendered pages reviewed; section header/footer linkage defect was corrected and the document re-rendered before release.

## Report diagram set
1. Repository/authority topology.
2. Full deployment/component architecture.
3. Direct-mode command/telemetry/video flow.
4. Remote-mode TLS/MQTT/WSS flow.
5. HR069 hardware GPIO/I2C/ADC/power map.
6. Acquisition and normalization pipeline.
7. Measurement/session/database lineage.
8. ChemCore state-machine and analytical pipeline.
9. M0-M4 evidence + OOD + quantitative gate.
10. User/developer/storage boundary.
11. SmartReport/export contract flow.
12. Safety command/fail-safe hierarchy.
13. APK vs ChemCore-library update paths.
14. Scientific-data authority/ingestion flow.
15. Change-control/CI/release-authority lifecycle.

## Resume / maintenance rule
The analysis task is complete. For any future revision, read issue #8 and this checkpoint first. Keep `5250ea769615ec82c5dc944576ea3042736b3db2` as the historical analysis baseline unless the user explicitly requests rebasing the report to a later controlled RC2/RC3 state. Any future report revision must preserve the distinction between engineering branch state and controlled release authority.

## Analytical rules retained
- Separate verified code facts from inferred architecture and recommendations.
- Record repository/path/role/consumer relationships for major components.
- Distinguish runtime, harness/governance, scientific/reference, and historical material.
- Keep the deferred Open-Set/OOD experimental-validation task separate.
- Never reproduce credentials/secrets in the report.
- Runtime behavior was not modified as part of this documentation task.
