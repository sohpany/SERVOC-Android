# HazRob 0.10.2 / HR069 — External analytical-data enrichment workplan

Status ledger created: 2026-09-29
Scope: `release/hazrob-0.10.2` only
Baseline at plan creation: `28c5bdd8f39537cc7cc242f6e47027a4e3ea0aaa`

## Objective

Increase HazRob analytical coverage by ingesting every scientifically useful external MQ/MOS dataset, response curve, fingerprint, known-concentration series and published PPM relation that can be traced to a source, while preserving the distinction between external research evidence and HR069 device-specific M4 validation. The work is intentionally resumable: every phase below has a durable state and an explicit next action.

## Non-negotiable scientific rules

1. External data may be used immediately as `RESEARCH_EXTERNAL`, M1/M2/M3 prior/fingerprint/transfer evidence even when HR069 recalibration is still required.
2. External data can never self-promote to `M4_HAZROB_VALIDATED`. M4 requires accepted HR069 device-specific reference-gas validation under a valid sampling/heater/environmental protocol.
3. Do not average heterogeneous regression coefficients. For duplicate sensor/analyte relations, prefer the highest-provenance relation inside its declared domain. Independent compatible concentration predictions are combined in `log10(ppm)` space using robust evidence weighting.
4. Never extrapolate a literature/manufacturer curve outside its documented domain unless a later HR069 experiment explicitly validates the extension.
5. Preserve raw observations, source DOI/URL, author, license/access state, retrieval hash and transformation history. Derived fingerprints never overwrite raw data.
6. Mirrors/derivatives of the same dataset have zero independent evidence weight until independence is demonstrated.
7. Research-use access without a redistribution license permits local analysis and derived metadata only. Raw third-party archives are not committed to Git unless redistribution permission/license is explicit.

## Phase ledger

| Phase | Deliverable | State | Evidence / next action |
|---|---|---|---|
| P0 | Freeze source baseline and workplan | DONE | This document; branch/release identity fixed above. |
| P1 | External-source registry and provenance | DONE / EXPANDING | `research_data/external_mq_fingerprints/manifest.json`; continue source-by-source additions. |
| P2 | Food Freshness E-Nose ZIP acquisition | BLOCKED-REMOTE / RETRYABLE | Public Zenodo archive exists; direct retrieval attempt on 2026-09-29 returned remote/rate-limit failure in the current execution environment. Fetch tool supports explicit research-cache download and MD5 verification. Kaggle mirror is also recorded. |
| P3 | Access/license/data requests | DRAFTED | Request templates stored under `research_data/external_mq_fingerprints/access_requests/`. Sending requires an authenticated mail connector; sent/response timestamps must be recorded in the request ledger. |
| P4 | 43 MQ response curves in laboratory knowledge | IN PROGRESS | Canonical runtime source is `ResearchCurveRegistry.CURVES`; create portable lab JSON with exact coefficients/provenance and audit count=43. |
| P5 | 76 MQDataScience model catalogue in laboratory knowledge | IN PROGRESS | All 76 are adopted/research-integrated; portable model registry must retain Android-native/verified/approximation/bridge status. |
| P6 | External fingerprints / known-concentration / PPM evidence normalization | IN PROGRESS | Normalize into a common research schema with source, sensor, analyte/class, concentration, units, sampling conditions and transfer constraints. |
| P7 | ChemCore update payload integration | IN PROGRESS | Stable snapshot supports `response_priors`, `model_artifacts`, `calibrations`, `reference_artifacts`; package research priors without elevating their evidence tier. |
| P8 | On-phone update path | PARTIAL / FIX REQUIRED | Runtime updater, signature verification, staging, rollback and UI already exist. Publication workflow is stale-gated to the old 0.10.1-ready branch and must be retargeted to `release/hazrob-0.10.2`. |
| P9 | HR069 analytical-engine consumption | IN PROGRESS | `ValidatedGasEngine` already consumes M4, peer-reviewed literature, inherited curves and MQDataScience curves in evidence tiers. Remote ChemCore priors must remain compatible with runtime feature/sampling contracts. |
| P10 | Automated scientific integrity audit | IN PROGRESS | Lock curve/model/source counts, hashes, no-M4-from-external rule, no duplicate evidence and updater compatibility in tests/CI. |
| P11 | HazRob Lab synchronization | IN PROGRESS | Mirror portable research registries and provenance ledger into `sohpany/HazRob-Lab`; production runtime remains `SERVOC-Android`. |
| P12 | HazRob Organization qualification mirror | PENDING AFTER SOURCE HEAD CHANGES | Update `HazRob/build-harness` source SHA only after the source branch passes audits. |

## External-source priority order

1. Exact/superset HR069 MQ arrays with raw time-series or repeated fingerprints.
2. Same MQ sensor model with known gas concentration and explicit conditions.
3. Manufacturer response tables/graphs and peer-reviewed digitized `Rs/R0` relations.
4. Other MOS arrays useful for drift, mixtures, transient features, environmental compensation and open-set validation.
5. Simulated datasets only for software/algorithm regression tests; never empirical sensor evidence.

## Food Freshness acquisition protocol

Canonical DOI: `10.5281/zenodo.17285312`.
Expected archive: `Food-Freshness-E-Nose-Dataset.zip`.
Published MD5: `008b8ccb62d7a7d51ff9ffcfd698a1e5`.
Sensor array: MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135; therefore all eight installed HR069 MQ models are represented, plus MQ6.

When retrieval succeeds:

1. Verify published MD5 and compute SHA-256.
2. Save raw archive only in ignored research cache unless redistribution license is explicit.
3. Inventory every contained file and schema before transformation.
4. Preserve raw timestamps/classes/conditions.
5. Derive HR069-overlap features without substituting MQ6 for a missing HR069 channel.
6. Store derived normalized fingerprint artifacts with source hash and transformation version.
7. Mark the output `RESEARCH_EXTERNAL`, never M4.

## Common normalized research schema

Every imported analytical observation/derived fingerprint should expose, when available:

- source/dataset ID, DOI/URL, citation and license/access state;
- source file SHA-256 and transformation version;
- sensor model and physical channel mapping;
- analyte identity (CAS where available) or class/mixture label;
- concentration value + unit (`ppm`, `ppb`, etc.) and known/unknown flag;
- raw ADC/voltage/resistance when available;
- `Rs`, `R0`, `Rs/R0`, `log10(Rs/R0)` where derivable without inventing missing circuit constants;
- baseline, delta, peak/end signal, slope/rate, AUC, T50/T90 and recovery features where time-series allow;
- temperature, RH, pressure, flow/chamber protocol and heater protocol where reported;
- replicate count, dispersion/uncertainty and data-quality flags;
- evidence tier and explicit `m4=false` for all third-party data.

## Publication/update acceptance gate

A ChemCore package for the installed 0.10.2 app is publishable only when all of the following pass:

- source branch is exactly `release/hazrob-0.10.2` / HR069;
- scientific source hashes and provenance audit pass;
- 43 research curves are present and count-locked;
- 76 MQDataScience models are present and count-locked;
- external evidence cannot claim M4;
- duplicate sources are not double-counted;
- payload manifest is signed and compatible with app 0.10.2;
- install is fail-closed during measurement/analysis and rollback remains available.

## Resume protocol after interruption

1. Read this file first.
2. Read `research_data/external_mq_fingerprints/manifest.json` and the request ledger.
3. Read latest commits on `release/hazrob-0.10.2` after the baseline SHA above.
4. Continue the first phase whose state is `IN PROGRESS`, `BLOCKED-REMOTE / RETRYABLE`, or `PENDING`.
5. After each completed phase, update this ledger in the same commit or immediately following commit.
6. Never infer that a mail was sent, a dataset downloaded, or a test passed without recorded evidence.
