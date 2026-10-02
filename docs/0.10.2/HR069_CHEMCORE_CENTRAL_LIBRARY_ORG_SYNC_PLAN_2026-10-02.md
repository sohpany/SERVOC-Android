# HR069 ChemCore Central Library ↔ Organization Research Sync Plan

Date: 2026-10-02
Release: HazRob 0.10.2 / RC2 / HR069
Android source branch: `sohpany/SERVOC-Android@release/hazrob-0.10.2-rc2`
Starting Android commit: `9efe2ff123664be261b1f2bff6a9f2fcff07a904`
Canonical organization library: `HazRob/build-harness@main/library/`
Starting organization commit: `2a965674ea9afe153f1507bb132b07b00684a48c`

## Purpose
Synchronize the canonical ChemCore Stable library with all qualified research/reference assets available in the HazRob organization and the current Android source, without silently promoting transfer/research evidence to HR069 M4.

## Source-of-truth model
1. `HazRob/build-harness/library/` is the canonical scientific-library owner.
2. `library/research/hr069/` contains accepted research/transfer evidence.
3. `library/reference-bank/` contains approved experiments and derived references.
4. `library/chemcore/stable/` contains verified stable ChemCore publications.
5. `sohpany/SERVOC-Android` is an application consumer plus source/tooling repository; its embedded assets must be reconciled with the canonical organization library.

## Evidence rules
- Never synthesize missing MQ channels.
- External/cross-device literature remains `RESEARCH_EXTERNAL` or transfer-only unless separately validated on HR069.
- Do not promote external evidence to `M4_HAZROB_VALIDATED`.
- Reportable device-specific PPM remains subject to HR069 M4 quantitative gates.
- Preserve source identity, provenance, source hash, sensor overlap, concentration domain, transfer status, and conflict notes.
- Conflicting evidence is retained and classified; it is not overwritten simply because a newer file exists.

## Classification states
Each relevant asset is classified as exactly one of:
- `CENTRAL_USED` — already represented and actively used by ChemCore Stable/runtime.
- `DUPLICATE_EQUIVALENT` — byte/content equivalent to an already represented source.
- `ORG_NEWER` — organization asset supersedes an older embedded/application copy.
- `ORG_UNMERGED_QUALIFIED` — qualified organization evidence exists but is not represented in the current stable/runtime library.
- `SOURCE_CONFLICT` — source-backed evidence conflicts materially with another accepted source and requires explicit coexistence/precedence semantics.
- `REJECTED_WITH_REASON` — not eligible for Central Stable; reason required.
- `NEEDS_REVIEW` — insufficient evidence to classify safely.

## Execution phases
### P0 — Freeze and checkpoint
- Record starting commits and canonical ownership.
- Create this plan and a machine-readable synchronization ledger.

### P1 — Inventory
- Enumerate canonical `library/research/hr069`, `library/reference-bank`, and `library/chemcore/stable`.
- Enumerate Android embedded/generated ChemCore assets, research assets, registries, extraction tools, and publication/update workflows.

### P2 — Mapping
For every relevant research/reference asset, record:
- source repository/path/hash
- asset type
- analyte/material scope
- MQ sensor scope
- concentration/domain semantics
- evidence tier/M4 status
- matching Central Stable/runtime representation
- classification and proposed action

### P3 — Qualification
- Validate schemas and hashes.
- Verify analyte/CAS mapping and 144-catalog mapping where applicable.
- Verify sensor overlap and missing-channel handling.
- Detect duplicates and source conflicts.
- Confirm Android/runtime consumption path.

### P4 — Controlled promotion
- Promote only qualified assets.
- Preserve provenance and source tier.
- Keep transfer-only evidence explicitly non-M4.
- Update Central Stable metadata/manifest/status index where appropriate.
- Update Android embedded/runtime representation only when required for the integrated APK.

### P5 — Validation
- Central-library integrity/schema tests.
- ChemCore regression/unit tests.
- Candidate-ranking and fingerprint regressions.
- Smart-report integration tests.
- Android build and signing/APK verification.

### P6 — Release
- Record final organization/app commits.
- Record merged/rejected/conflicting counts.
- Record library version/hash.
- Build final integrated APK and record SHA-256.

## Resume protocol
After each material change, update `HR069_CHEMCORE_CENTRAL_LIBRARY_SYNC_LEDGER_2026-10-02.csv` and the checkpoint section below. A resumed session MUST start from the latest committed checkpoint, verify both recorded repository heads, and continue from `NEXT_ACTION`; no completed phase should be recomputed unless a recorded source head changed.

## Checkpoint
- CHECKPOINT_ID: `CLSYNC-0001`
- STATUS: `P0_COMPLETE_P1_IN_PROGRESS`
- ANDROID_HEAD_AT_START: `9efe2ff123664be261b1f2bff6a9f2fcff07a904`
- ORG_HEAD_AT_START: `2a965674ea9afe153f1507bb132b07b00684a48c`
- CANONICAL_LIBRARY_CONFIRMED: `HazRob/build-harness/library/`
- CENTRAL_STABLE_PATH_CONFIRMED: `HazRob/build-harness/library/chemcore/stable/`
- RESEARCH_PATH_CONFIRMED: `HazRob/build-harness/library/research/hr069/`
- NEXT_ACTION: `Complete P1 inventory and populate the synchronization ledger.`

## Release gate
No final APK is considered the requested integrated build until:
1. synchronization ledger has no unclassified relevant assets;
2. every conflict has explicit disposition;
3. ChemCore and smart-report regression tests pass;
4. APK build/signature verification passes;
5. final library/app hashes and commits are recorded.
