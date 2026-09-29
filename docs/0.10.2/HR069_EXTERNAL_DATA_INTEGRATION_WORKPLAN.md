# HazRob 0.10.2 / HR069 — External analytical data integration workplan

Status: **ACTIVE / RESUMABLE**  
Created: 2026-09-29  
Controlled production branch: `release/hazrob-0.10.2`  
Starting source baseline: `28c5bdd8f39537cc7cc242f6e47027a4e3ea0aaa`

## Objective

Use credible external MQ/MOS datasets, response curves, known-concentration series and published analytical models to reduce unnecessary discovery experiments while preserving HazRob's evidence hierarchy. External data may be used immediately as research evidence, priors, classifier/fingerprint benchmarks and transfer-validation targets. External data must **never** be labelled HR069 M4 unless independently validated on the physical HR069 array using accepted reference-gas/calibration procedures.

## Non-negotiable scientific rules

1. Raw/external evidence is tagged `RESEARCH_EXTERNAL` or an equivalent non-M4 tier.
2. M4 means **device-specific HR069 reference-gas validation**, not literature quality.
3. For duplicate curves on the same sensor/analyte, prefer the highest-provenance curve inside its declared domain. Do **not** average regression coefficients.
4. Independent compatible concentration predictions may be combined in `log10(ppm)` with evidence-weighted robust consensus.
5. Mirrors/derivative copies receive zero independent evidence weight until provenance/equivalence is demonstrated.
6. No extrapolation outside a published response-curve domain unless an explicitly validated HazRob model permits it.
7. Every imported artifact keeps DOI/URL, author/owner, license/permission status, hashes and transformation history.
8. A dataset may be used for research/feature engineering before transfer validation, but its numerical values must not silently become occupational-safety PPM.

## Baseline already integrated

- 144-entry legacy analytical catalogue.
- 652-record pinned NIOSH chemical identity seed.
- 43 MQDataScience/datasheet-derived `Rs/R0` research curves in `ResearchCurveRegistry`.
- 76 adopted MQDataScience model definitions; Android runtime and pinned-lab execution status are tracked separately.
- Six peer-reviewed MQ-135 `Rs/R0` literature curves from Umbu (2023): acetone, toluene, alcohol/ethanol mapping, CO2, ammonia and CO.
- External evidence registry covering known-concentration series, same/similar-array fingerprints and manufacturer evidence.
- `ValidatedGasEngine` evidence hierarchy: device M4 -> peer-reviewed curves -> manufacturer/community curves -> low-weight research graph fits.
- ChemCore Stable update client/manager/UI already supports signed HTTPS library updates independently of APK installation.

## Work phases and durable checkpoints

### P0 — Governance and recovery ledger — DONE
- [x] Create this resumable workplan on the controlled 0.10.2 / HR069 branch.
- [x] Preserve the starting commit SHA.
- [x] Define evidence and duplicate-source policy.

### P1 — External dataset acquisition and permissions — IN PROGRESS
- [x] Register Food Freshness Electronic Nose Dataset (Zenodo `10.5281/zenodo.17285312`).
- [x] Verify its sensor set contains MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135; all eight HR069 MQ models are represented.
- [x] Record archive name `Food-Freshness-E-Nose-Dataset.zip` and published MD5 `008b8ccb62d7a7d51ff9ffcfd698a1e5`.
- [x] Send research reuse/license clarification email to creator Mehrab Mahdian on 2026-09-29; Gmail message id `1a0edd57f5239d2f`.
- [ ] Acquire the Food Freshness ZIP bytes and verify MD5/SHA-256. Current ChatGPT execution environment could reach the Zenodo record but could not retrieve the binary archive; retrieval script/workflow is therefore the next executable path. **Do not mark downloaded until hash verification succeeds.**
- [ ] Extract file inventory/schema and generate derived feature manifest while retaining raw archive unchanged.
- [ ] Verify RoboSense/RoboFusion canonical raw-data relationship and ingest available CC0 files.
- [ ] Send/record any additional author requests for datasets whose raw files, permissions, or experimental metadata remain unavailable.

### P2 — Analytical seed unification — PLANNED
- [ ] Generate one machine-readable HR069 analytical seed containing all 43 research curves with full provenance/domain metadata.
- [ ] Include the six preferred peer-reviewed literature curves and their R2/domain information.
- [ ] Register all 76 MQDataScience model names, index, research role and Android runtime status.
- [ ] Add external known-concentration series (ppm/ppb) as research evidence records.
- [ ] Add fingerprint-dataset descriptors and, where raw data is available, derived fingerprint/feature artifacts with source hashes.
- [ ] Ensure higher-provenance literature data supersedes duplicate lower-tier same-channel equations without double counting.

### P3 — Lab/reference database population — PLANNED
- [ ] Import scientific sources into `scientific_sources`.
- [ ] Import response equations/domains into `sensor_evidence`.
- [ ] Populate `models`/reference artifacts with model metadata and derived fingerprint artifacts as appropriate.
- [ ] Preserve raw external data separately from derived data; never overwrite source observations.
- [ ] Add idempotent importer/bootstrap and integrity hash so repeated launches do not duplicate records.

### P4 — Engine consumption — PLANNED
- [ ] Make the analytical runtime consume the unified external analytical seed/library rather than relying only on compile-time constants.
- [ ] Preserve current fail-closed quantitative gating.
- [ ] Keep external fingerprints available for candidate ranking/open-set/classification even before M4 transfer calibration.
- [ ] Keep occupational PPM/exposure interpretation gated to validated quantitative evidence.

### P5 — Updatable ChemCore package — PLANNED
- [ ] Ensure curves, model metadata, fingerprints and reference evidence are represented in a signed ChemCore Stable payload.
- [ ] Extend snapshot/publisher counts and integrity tests for these analytical objects.
- [ ] Verify compatibility with app `0.10.2` and the build-pinned endpoint `https://hazrob-app-updates.onrender.com/chemcore/manifest.json`.
- [ ] Confirm the phone UI path: ChemCore Library -> Check update -> Download/install verified Stable update -> rollback.
- [ ] Document that data/model artifacts can update through ChemCore, while changes to Android Java/Kotlin executable code still require an APK update.

### P6 — Validation and experiment reduction — PLANNED
- [ ] Produce matrix: `NO NEW DISCOVERY / TRANSFER VALIDATION / M4 ONLY / FINGERPRINT DISCOVERY / UNSUPPORTED-CURRENT-ARRAY` for all 144 legacy entries.
- [ ] Extend the same evidence mapping to the 652 NIOSH identities where sensor relevance exists.
- [ ] Prioritize physical experiments only where external evidence is absent, incompatible, contradictory or requires HR069 transfer validation.

### P7 — Build-harness synchronization — PLANNED
- [ ] Point `HazRob/build-harness` to the final source commit from this campaign.
- [ ] Add analytical-seed and ChemCore update integrity audits to qualification.
- [ ] Do not fire RC build triggers until the scientific integration passes all checks.

## External source status ledger

| Source | Sensors relevant to HR069 | Data status | Permission/license | Planned use |
|---|---|---|---|---|
| Food Freshness Electronic Nose, Zenodo 17285312 | 8/8 HR069 + MQ6 | archive publicly listed; binary retrieval pending hash verification | license clarification requested from creator | exact-array fingerprint/transient features/classification |
| MultimodalGasData 2022 | 6/8 | canonical public data | CC BY 4.0 | smoke/perfume/mixture/no-gas benchmark |
| TakMashhido Gas Sensor Measurements | 6/8 | public | MIT; probable derivative | audit/mirror only; no independent weighting |
| UCI Gas Sensor Array Low-Concentration | MQ135 | public | CC BY 4.0 | known-concentration response shape / transfer methods |
| RoboSense / RoboFusion | 6/8 | canonical raw-data relationship under verification | CC0 in repository | industrial hazards / temporal classification / transfer validation |
| Umbu 2023 MQ135 curves | MQ135 | equations integrated | peer-reviewed article | preferred generic Rs/R0 prior inside published domain |

## Resume instructions after interruption

1. Read this file first.
2. Read branch head of `release/hazrob-0.10.2` and compare it with the latest checkpoint commit recorded below.
3. Never restart a completed phase without checking its files/hashes.
4. Continue from the first unchecked task under the earliest active phase.
5. After each material change, commit with prefix `0.10.2 HR069:` and append the new commit and result to the checkpoint log below.
6. At campaign end, update `HazRob/build-harness` only after the production branch passes audits.

## Checkpoint log

| UTC date | Commit | Checkpoint | Result |
|---|---|---|---|
| 2026-09-29 | `28c5bdd8f39537cc7cc242f6e47027a4e3ea0aaa` | starting baseline | 144/652 coverage planner and external-evidence counts locked in tests |
| 2026-09-29 | pending | P0 workplan | this durable execution ledger created |
