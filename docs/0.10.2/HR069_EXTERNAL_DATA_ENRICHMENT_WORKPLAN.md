# HazRob 0.10.2 / HR069 — External analytical-data enrichment workplan

Status ledger created: 2026-09-29  
Last updated: 2026-09-29  
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
| P0 | Freeze source baseline and workplan | **DONE** | This document; branch/release identity fixed above. |
| P1 | External-source registry and provenance | **DONE / EXPANDING** | `research_data/external_mq_fingerprints/manifest.json` schema 1.3.0; canonical/mirror weighting, Food Freshness access state and request IDs are recorded. Continue source-by-source additions. |
| P2 | Food Freshness E-Nose ZIP acquisition | **BLOCKED-REMOTE / RETRYABLE** | Public Zenodo archive + Kaggle mirror recorded. Current execution environment failed/rate-limited. `fetch_sources.py` now retries both endpoints and verifies published MD5 `008b8ccb62d7a7d51ff9ffcfd698a1e5`. Raw ZIP is not yet acquired. |
| P3 | Access/license/data requests | **DRAFTED / MAIL CONNECTOR REQUIRED** | Request ledger and four drafts exist. REQ-001/002/003 have verified recipients; REQ-004 recipient still needs independent verification. No message is marked sent. |
| P4 | 43 MQ response curves in laboratory knowledge | **DONE** | `research_data/hr069_analytical_library/mq_research_curves_43.json` and HazRob-Lab `calibration/priors/mq_research_curves_43.json`; exact count/provenance/domain audit added. |
| P5 | 76 MQDataScience model catalogue in laboratory knowledge | **DONE** | `research_data/hr069_analytical_library/mq_models_76.json` and HazRob-Lab `models/mq76/catalog.json`; 76 adopted/research-integrated, 14 Android-native, 12 verified, 2 approximations, 62 lab-integrated Android-port-pending. |
| P6 | External fingerprints / known-concentration / PPM evidence normalization | **IN PROGRESS** | HazRob-Lab `datasets/external/registry.json` created; response-curve/known-concentration sources mapped. Raw Food/RoboSense/Frontiers/Visual-Smell files remain acquisition items. |
| P7 | ChemCore update payload integration | **CANDIDATE PATH IMPLEMENTED** | `tools/build_hr069_research_priors.py` converts the 43 curves into non-M4 runtime priors and overlays six peer-reviewed MQ135 curves without overlapping same-sensor double counting. Stable snapshot packaging now accepts/hashes response priors, 76 model artifacts and external reference artifacts. |
| P8 | On-phone update path | **RUNTIME READY / ONE-TIME APK KEY BLOCKER** | HTTPS updater, signed verification, staging, rollback and Arabic UI already exist. New `chemcore-library-publish-0102.yml` targets HR069. Current APK source still bundles `NOT_PROVISIONED` publication key, so a one-time keyed APK rebuild/install is required before remote ChemCore libraries can be activated. See `HR069_CHEMCORE_UPDATE_READINESS.md`. |
| P9 | HR069 analytical-engine consumption | **PARTIAL / ACTIVE** | `ValidatedGasEngine` already consumes M4, peer-reviewed literature, inherited curves and MQDataScience curves in evidence tiers. Updateable ChemCore response priors are supported by `ChemCoreStableKnowledgeReader`; production publication waits on P8 key provisioning. |
| P10 | Automated scientific integrity audit | **IMPLEMENTED / CI INFRASTRUCTURE BLOCKED** | Audit now locks 144/652 identities, 43 curves, 76 models, Food Freshness overlap/hash, duplicate evidence, legacy UID mapping and no-external-M4 rule. Personal-repo Actions currently terminate before steps execute, indicating runner/quota/infrastructure rather than an observed audit assertion failure. Organization qualification is P12. |
| P11 | HazRob Lab synchronization | **INITIAL SYNC DONE** | Lab workplan corrected; 43 curves, 76 models and external source registry are committed under `sohpany/HazRob-Lab`. Continue raw/derived artifact ingestion there. |
| P12 | HazRob Organization qualification mirror | **PENDING FINAL SOURCE SHA** | Update `HazRob/build-harness` to the final 0.10.2/HR069 source head after remaining source corrections, then run organization qualification/build. |

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
- install is fail-closed during measurement/analysis and rollback remains available;
- the APK contains the public verification key matching the protected publication private key.

## Resume protocol after interruption

1. Read this file first.
2. Read `research_data/external_mq_fingerprints/manifest.json` and `access_requests/REQUEST_LEDGER.md`.
3. Read `docs/0.10.2/HR069_CHEMCORE_UPDATE_READINESS.md`.
4. Read latest commits on `release/hazrob-0.10.2` after the baseline SHA above.
5. Continue the first phase whose state is `IN PROGRESS`, `BLOCKED-REMOTE / RETRYABLE`, `MAIL CONNECTOR REQUIRED`, or `PENDING`.
6. After each completed phase, update this ledger in the same commit or immediately following commit.
7. Never infer that a mail was sent, a dataset downloaded, a signing key was provisioned, or a test passed without recorded evidence.
