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
8. A source-backed model-derived identification fingerprint is a research prior, not an empirical HR069 fingerprint. Unsupported compounds remain explicitly pending rather than receiving fabricated vectors.

## Phase ledger

| Phase | Deliverable | State | Evidence / next action |
|---|---|---|---|
| P0 | Freeze source baseline and workplan | **DONE** | This document; branch/release identity fixed above. |
| P1 | External-source registry and provenance | **DONE / EXPANDING** | `research_data/external_mq_fingerprints/manifest.json` schema 1.3.0; canonical/mirror weighting, Food Freshness access state and request IDs are recorded. Continue source-by-source additions. |
| P2 | Food Freshness E-Nose ZIP acquisition | **DONE / DERIVATION ACTIVE** | `HazRob/build-harness` acquired the canonical Zenodo ZIP and verified published MD5 `008b8ccb62d7a7d51ff9ffcfd698a1e5`; recorded SHA-256 is `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`. Raw archive stays outside source Git pending explicit redistribution rights. HR069-overlap feature derivation workflow is available. |
| P3 | Access/license/data requests | **SENT / AWAITING RESPONSES** | Research-use requests were sent for Food Freshness, RoboFusion/RoboSense and the VOC dataset with explicit citation/attribution commitment. Remaining unverified recipient(s) stay unsent until independently confirmed. |
| P4 | 43 MQ response curves in laboratory knowledge | **DONE** | `research_data/hr069_analytical_library/mq_research_curves_43.json` and HazRob-Lab `calibration/priors/mq_research_curves_43.json`; exact count/provenance/domain audit added. |
| P5 | 76 MQDataScience model catalogue in laboratory knowledge | **DONE** | `research_data/hr069_analytical_library/mq_models_76.json` and HazRob-Lab `models/mq76/catalog.json`; 76 adopted/research-integrated, 14 Android-native, 12 verified, 2 approximations, 62 lab-integrated Android-port-pending. |
| P6 | External fingerprints / known-concentration / PPM evidence normalization | **ACTIVE — IDENTIFICATION PRIORS IMPLEMENTED** | `ResearchIdentificationFingerprintRegistry` now builds source-backed `log10(Rs/R0)` identification templates for 11 pure analytes across the installed eight-MQ order and a concentration grid, with literature > accepted GasEngine > research-curve priority. Food/spoilage targets without defensible exact HR069 numeric relations remain transfer/pending/affinity-only. |
| P7 | ChemCore update payload integration | **CANDIDATE PATH IMPLEMENTED** | `tools/build_hr069_research_priors.py` converts the 43 curves into non-M4 runtime priors and overlays six peer-reviewed MQ135 curves without overlapping same-sensor double counting. Stable snapshot packaging accepts/hashes response priors, 76 model artifacts and external reference artifacts. Next: include the identification-prior registry in a signed Stable package after qualification. |
| P8 | On-phone update path | **RUNTIME READY / ONE-TIME APK KEY BLOCKER** | HTTPS updater, signed verification, staging, rollback and Arabic UI already exist. `chemcore-library-publish-0102.yml` targets HR069. Current APK source still bundles `NOT_PROVISIONED` publication key, so a one-time keyed APK rebuild/install is required before remote ChemCore libraries can be activated. See `HR069_CHEMCORE_UPDATE_READINESS.md`. |
| P9 | HR069 analytical-engine consumption | **ACTIVE** | `ValidatedGasEngine` consumes M4, peer-reviewed literature, inherited curves and MQDataScience curves. `FingerprintClassifier.classifyResearchPriors(...)` now exposes the separate research identification-fingerprint path and collapses concentration templates to one best match per chemical; MQ7/MQ9 fail closed unless heater-cycle validity is explicitly true. |
| P10 | Automated scientific integrity audit | **SOURCE TESTS ADDED / ORG QUALIFICATION REQUIRED** | New fingerprint tests lock non-M4 status, 11 declared numeric analytes, food/spoilage non-fabrication policy, one-best-match-per-chemical behavior and heater-cycle masking. Re-run `HazRob/build-harness` Android qualification against the final source SHA after this checkpoint. |
| P11 | HazRob Lab synchronization | **INITIAL SYNC DONE** | Lab workplan corrected; 43 curves, 76 models and external source registry are committed under `sohpany/HazRob-Lab`. Next sync should include identification-prior metadata and derived external fingerprints. |
| P12 | HazRob Organization qualification mirror | **NEXT** | Point `HazRob/build-harness` to the final 0.10.2/HR069 source head and run organization qualification. Do not trigger full RC build merely for this research-library checkpoint. |

## External-source priority order

1. Exact/superset HR069 MQ arrays with raw time-series or repeated fingerprints.
2. Same MQ sensor model with known gas concentration and explicit conditions.
3. Manufacturer response tables/graphs and peer-reviewed digitized `Rs/R0` relations.
4. Other MOS arrays useful for drift, mixtures, transient features, environmental compensation and open-set validation.
5. Simulated/model-derived datasets only for software/algorithm regression and identification priors; never empirical sensor evidence.

## Food Freshness acquisition protocol

Canonical DOI: `10.5281/zenodo.17285312`.  
Expected archive: `Food-Freshness-E-Nose-Dataset.zip`.  
Published MD5: `008b8ccb62d7a7d51ff9ffcfd698a1e5`.  
Verified SHA-256: `fde5a3836fe9ddb04fee90102b9c1a5816f48a78d37bf7125074e66a3f3b5f5a`.  
Sensor array: MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135; therefore all eight installed HR069 MQ models are represented, plus MQ6.

Acquisition/derivation rules:

1. Verify published MD5 and compute SHA-256 — completed for the acquired archive.
2. Save raw archive only in ignored/private research cache unless redistribution license is explicit.
3. Inventory every contained file and schema before transformation.
4. Preserve raw timestamps/classes/conditions.
5. Derive HR069-overlap features without substituting MQ6 for a missing HR069 channel.
6. Store derived normalized fingerprint artifacts with source hash and transformation version.
7. Mark the output `RESEARCH_EXTERNAL`, never M4.
8. Do not relabel whole-food/headspace classes as pure-compound fingerprints without independent chemical labels.

## Identification fingerprint checkpoint — 2026-09-29

Implementation series through pre-ledger commit `3ea58cc358ecd6548f99a1e4205787da0d5e9240`:

- Added `app/src/main/java/com/servoc/app/fingerprint/ResearchIdentificationFingerprintRegistry.java`.
- Added `research_data/hr069_analytical_library/identification_fingerprint_priors_v1.json`.
- Extended `FingerprintClassifier` with `classifyResearchPriors(...)`, separate from user/empirical fingerprints.
- Added `ResearchIdentificationFingerprintRegistryTest`.
- Numeric source-backed research priors currently cover 11 pure analytes: CO, NH3, CO2, benzene, H2, CH4, propane, n-hexane, toluene, ethanol and acetone.
- Food/spoilage subset with numeric model-derived priors: ethanol, acetone, CO2, NH3 and CH4.
- Food/spoilage compounds intentionally NOT assigned fabricated exact numeric fingerprints: H2S (manufacturer affinity only), acetaldehyde (source pending), ethylene (source pending), methanol (transfer data available), ethyl acetate (transfer data available).
- Template vector space is `log10(Rs/R0)` in physical HR069 sensor order: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135.
- Concentration grid is 10/25/50/100/200/300/500/1000/3000/5000/10000 ppm, but each sensor/analyte channel exists only inside its own source-backed domain; no extrapolation is generated.
- Source priority per physical channel: peer-reviewed literature > accepted registered GasEngine relation > MQDataScience/datasheet-derived research curve.
- These templates are `RESEARCH_EXTERNAL`, `m4=false`, and `sampleCount=0` by design. Device-specific experimental fingerprints continue to use the empirical trainer/database path.

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
- identification-prior registry integrity tests pass;
- external evidence cannot claim M4;
- duplicate sources are not double-counted;
- payload manifest is signed and compatible with app 0.10.2;
- install is fail-closed during measurement/analysis and rollback remains available;
- the APK contains the public verification key matching the protected publication private key.

## Resume protocol after interruption

1. Read this file first.
2. Read `research_data/external_mq_fingerprints/manifest.json` and `access_requests/REQUEST_LEDGER.md`.
3. Read `research_data/hr069_analytical_library/identification_fingerprint_priors_v1.json`.
4. Read `docs/0.10.2/HR069_CHEMCORE_UPDATE_READINESS.md`.
5. Read latest commits on `release/hazrob-0.10.2` after the baseline SHA above.
6. Continue from P6/P7: ingest source-backed external compound fingerprints and package the identification priors into ChemCore after qualification.
7. After each completed phase, update this ledger in the same commit or immediately following commit.
8. Never infer that a mail was sent, a dataset downloaded, a signing key was provisioned, or a test passed without recorded evidence.
