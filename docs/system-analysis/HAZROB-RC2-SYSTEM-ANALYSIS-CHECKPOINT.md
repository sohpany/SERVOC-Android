# HazRob RC2 Full System Analysis — Checkpoint

## Tracking
- GitHub issue: #8 — `HazRob RC2 — Full System Architecture & Systems Analysis Report`
- Working documentation branch: `docs/hazrob-rc2-system-analysis`
- Governing RC2 baseline at task start: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`

## Source inventory discovered
### Personal GitHub installation (`sohpany`)
1. `sohpany/SERVOC-Android` — primary application/firmware/analytical codebase; RC2 governing source.
2. `sohpany/HazRob` — project repository; project-level material and references.
3. `sohpany/HazRob-Lab` — laboratory/experimental repository; experimental/reference data and tooling.

### Organization GitHub installation (`HazRob`)
4. `HazRob/build-harness` — organization-level build harness; CI/build/release orchestration and cross-repository dependency.

## Mandatory organization-scope rule
The analysis MUST include the complete accessible contents of the `HazRob` GitHub organization installation as a first-class source tier, not merely the personal repositories. Every accessible organization repository, branch/material relevant to RC2, harness script, workflow, configuration, build artifact definition, reference file and cross-repository relationship must be inventoried and mapped. Organization content must be clearly distinguished from personal-repository runtime code, laboratory/reference material and historical content.

## Phases and status
- [x] P1a — Freeze RC2 baseline
- [x] P1b — Create tracking issue and working documentation branch
- [x] P1c — Discover connected personal and organization repositories
- [ ] P1d — Inspect repository metadata/heads and top-level trees, including all accessible `HazRob` organization contents
- [ ] P2 — Full RC2 file tree and subsystem classification
- [ ] P3 — Application/UI/domain architecture analysis
- [ ] P4 — ChemCore/analytical/data/reference architecture analysis
- [ ] P5 — Firmware/hardware/protocol/connectivity architecture analysis
- [ ] P6 — Storage/databases/reports/update/security/CI-harness analysis
- [ ] P7 — Cross-system dependencies, flows, failure modes and risk review
- [ ] P8 — Recommendations, target architecture and remediation roadmap
- [ ] P9 — DOCX/PDF authoring, diagrams and visual QA

## Resume rule
If interrupted, first read issue #8 and this file. Resume from the first unchecked item. Do not use a newer implementation as the governing baseline unless the user explicitly asks to rebase the report.

## Analytical rules
- Separate verified code facts from inferred architecture and recommendations.
- Record file path and repository for every major component.
- Record repository owner (`sohpany` personal vs `HazRob` organization), branch/ref, path, role, dependencies and consumers for every major file/group.
- Tag historical/reference-only branches and data so they are not confused with RC2 runtime behavior.
- Keep the deferred Open-Set/OOD experimental-validation task separate, referencing it only as existing technical debt/open validation work.
- Do not modify runtime behavior during this documentation task.
