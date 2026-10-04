# HazRob RC2 Full System Analysis — Checkpoint

## Tracking
- GitHub issue: #8 — `HazRob RC2 — Full System Architecture & Systems Analysis Report`
- Working documentation branch: `docs/hazrob-rc2-system-analysis`
- Governing RC2 analysis baseline frozen at task start: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
- Separate controlled executable/release authority must be reported from `HazRob/build-harness/CURRENT_RELEASE_AUTHORITY.md`; branch HEAD movement alone is not release authority.

## Source inventory discovered
### Personal GitHub installation (`sohpany`)
1. `sohpany/SERVOC-Android` — primary Android, firmware, ChemCore, scientific corpus, reports, update logic, tests and tools; latest RC2 analysis source.
2. `sohpany/HazRob` — minimal project-description repository; not an RC2 runtime authority.
3. `sohpany/HazRob-Lab` — FROZEN / historical scientific provenance only since 2026-10-01. New scientific authority moved to the production repository.

### Organization GitHub installation (`HazRob`)
4. `HazRob/build-harness` — governance/build/test/qualification/release-evidence repository. It is not the production runtime repository and not the canonical scientific library.

## Mandatory organization-scope rule
The analysis MUST include the complete accessible contents of the `HazRob` GitHub organization installation as a first-class source tier, not merely the personal repositories. Every accessible organization repository, branch/material relevant to RC2, harness script, workflow, configuration, build artifact definition, reference file and cross-repository relationship must be inventoried and mapped. Organization content must be clearly distinguished from personal-repository runtime code, laboratory/reference material and historical content.

## Verified authority boundaries
- Latest RC2 engineering state analyzed: `sohpany/SERVOC-Android@5250ea769615ec82c5dc944576ea3042736b3db2`.
- Controlled executable/release approval is a separate governance concept determined by organization release-authority records.
- Canonical active analytical corpus: `sohpany/SERVOC-Android/research_data/hr069_analytical_library/`.
- `HazRob-Lab` is frozen historical provenance only.
- `HazRob/build-harness` provides governance/build/test/qualification/release evidence only; workflows cannot autonomously promote release/scientific authority.
- Mandatory lifecycle: `REGISTER -> IMPLEMENT CONTROLLED -> TEST -> TRIAL/VALIDATE -> REVIEW -> APPROVE -> PROMOTE/RELEASE -> IMMUTABLE MANIFEST`.

## Major findings already verified
1. **Safety-policy divergence:** current RC2 firmware keeps IR/cliff and ultrasonic warning telemetry active but forces automatic IR/ultrasonic motion stopping OFF. E-STOP and stale-command watchdog remain independent. Android drive controls also do not implement an independent directional hazard block. Treat current behavior as warning-only unless/until a controlled safety CR restores and validates directional firmware interlocking.
2. **MQ7/MQ9 quantitative limitation:** the hardware profile states the required cyclic-heater driver is not installed; manufacturer heater-cycle validity therefore fails closed for MQ7/MQ9.
3. **R0 semantics are correctly separated from M4:** field clean-air R0 is normalization provenance, not reference-gas quantitative calibration.
4. **Remote-security technical debt:** prototype bootstrap credentials are embedded in prototype clients/firmware. Remote transport itself uses encrypted channels and role separation, but device/bootstrap identity must be replaced before production-grade security claims. Sensitive credential values are intentionally excluded from this analysis record.
5. **Release-signing technical debt:** Android build configuration uses prototype signing material and prototype key management for debug/release. This is not production key management.
6. **Android cleartext scope:** global cleartext traffic is enabled although cleartext is primarily required for the Direct local AP. Recommend network-security configuration scoped to local robot/camera hosts.
7. **Authority-document drift:** some status/architecture documents contain older branch/baseline language than `CURRENT_RELEASE_AUTHORITY.md`. Recommend one machine-readable release-authority manifest and generate human status documents from it.
8. **Build provenance debt:** build configuration retains a content-addressed fallback that can recover a controlled flammability seed from a legacy APK if the source asset is absent. Eliminate this external legacy build dependency after controlled vendoring/provenance reconciliation.
9. **Strong storage lineage:** SAF user library separates user-owned field archives from developer/calibration/reference tables; session archives use immutable IDs and strict link restoration.
10. **Strong ChemCore semantic gates:** M3 remains identity-only, M4 is required for reportable quantitative ppm, OOD fail-closed behavior is explicit, evidence fusion de-duplicates correlated lineages, and SmartReport enforces the ChemCore reporting contract.

## Phases and status
- [x] P1a — Freeze RC2 baseline
- [x] P1b — Create tracking issue and working documentation branch
- [x] P1c — Discover connected personal and organization repositories
- [x] P1d — Inspect repository metadata/heads and top-level trees, including accessible `HazRob` organization contents
- [x] P2 — Full RC2 production/governed file tree and subsystem classification
- [x] P3 — Application/UI/domain architecture analysis
- [x] P4 — ChemCore/analytical/data/reference architecture analysis
- [x] P5 — Firmware/hardware/protocol/connectivity architecture analysis
- [x] P6 — Storage/databases/reports/update/security/CI-harness analysis
- [ ] P7 — Cross-system dependencies, flows, failure modes and risk review (in progress)
- [ ] P8 — Recommendations, target architecture and remediation roadmap
- [ ] P9 — DOCX/PDF authoring, diagrams and visual QA

## Planned report diagrams
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

## Resume rule
If interrupted, first read issue #8 and this file. Resume from the first unchecked item. Do not use a newer implementation as the governing analysis baseline unless the user explicitly asks to rebase the report.

## Analytical rules
- Separate verified code facts from inferred architecture and recommendations.
- Record file path and repository for every major component.
- Record repository owner, branch/ref, path, role, dependencies and consumers for every major file/group.
- Tag historical/reference-only branches and data so they are not confused with RC2 runtime behavior.
- Keep the deferred Open-Set/OOD experimental-validation task separate, referencing it only as existing technical debt/open validation work.
- Never reproduce credentials/secrets in the report; describe security findings without exposing secret values.
- Do not modify runtime behavior during this documentation task.
