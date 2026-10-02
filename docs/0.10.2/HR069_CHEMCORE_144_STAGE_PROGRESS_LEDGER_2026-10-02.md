# HazRob 0.10.2 / HR069 — ChemCore 144-Entry Stage Progress Ledger

Date opened: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Master plan: `HR069_CHEMCORE_UNIFIED_ANALYTICAL_FRAMEWORK_38_STAGE_PLAN_2026-10-02.md`
Analytical methodology: `HR069_CHEMCORE_UNIFIED_ANALYTICAL_METHODOLOGY_V1_2026-10-02.md`
Knowledge governance: `HR069_CHEMCORE_KNOWLEDGE_BASE_GOVERNANCE_2026-10-02.md`
Catalogue inventory: `HR069_CHEMCORE_144_CATALOG_STAGE1_INVENTORY_2026-10-02.csv`
Early-build flexibility: `HR069_CHEMCORE_EARLY_BUILD_FLEXIBILITY_ADDENDUM_2026-10-02.md`

## Counting rule
Every material-level stage accounts for all 144 catalogue entries. Sensor/analyte stages account for all 1,152 pairs (144 x 8). Unknown/absent evidence is never interpreted as negative evidence.

Agreed general session warm-up: `300 s`. MQ7/MQ9 special heater-cycle validity remains separate when source/manufacturer evidence requires it.

Current development policy: `EARLY_BUILD_EXPLORATORY`. Diagnostic OOD, domain, drift and conflict states normally preserve candidate visibility; M4/reportable ppm remains a later device-specific validation state.

## Stage ledger
| Stage | Name | Status | Coverage / result | Evidence |
|---:|---|---|---|---|
| 1 | Freeze and census 144-entry catalogue | COMPLETE | 144/144 | `HR069_CHEMCORE_144_CATALOG_STAGE1_INVENTORY_2026-10-02.csv` |
| 2 | Structural chemical taxonomy | COMPLETE | 144/144; 135 molecular + 9 operational/non-molecular | `HR069_CHEMCORE_STAGE2_STRUCTURAL_TAXONOMY_2026-10-02.csv` |
| 3 | Sensor-response family taxonomy | COMPLETE | 144/144 | `HR069_CHEMCORE_STAGE3_SENSOR_RESPONSE_TAXONOMY_2026-10-02.csv` |
| 4 | Object-type taxonomy | COMPLETE | 144/144; 135 pure + 6 mixtures + 3 aggregates | `HR069_CHEMCORE_STAGE4_OBJECT_TYPE_TAXONOMY_2026-10-02.csv` |
| 5 | Analytical-route assignment | COMPLETE | 144/144; 7 TARGET_MQ, 94 BROAD_ARRAY, 34 REACTIVE/UNSUPPORTED, 9 MIXTURE/HEADSPACE | `HR069_CHEMCORE_STAGE5_ANALYTICAL_ROUTE_ASSIGNMENT_2026-10-02.csv` |
| 6 | 144 x 8 sensor-role matrix | COMPLETE | 1152/1152 pairs | `HR069_CHEMCORE_STAGE6_SENSOR_ANALYTE_ROLE_MATRIX_2026-10-02.csv` |
| 7 | Anchor and specialist map | COMPLETE | 144/144; 5 primary anchors + 12 secondary targets | `HR069_CHEMCORE_STAGE7_ANCHOR_SPECIALIST_MAP_2026-10-02.csv` |
| 8 | Heater-protocol dependency map | COMPLETE | 144/144; MQ7/MQ9 dependency isolated from 300 s general warm-up | `HR069_CHEMCORE_STAGE8_HEATER_PROTOCOL_DEPENDENCY_2026-10-02.csv` |
| 9 | Hardware suitability | COMPLETE | 144/144; 4 adequate-with-conditions, 106 partial, 19 compatibility review, 15 unsupported | `HR069_CHEMCORE_STAGE9_HARDWARE_SUITABILITY_2026-10-02.csv` |
| 10 | Hardware-gap register | COMPLETE | 144/144; no silent hardware gaps | `HR069_CHEMCORE_STAGE10_HARDWARE_GAP_REGISTER_2026-10-02.md` |
| 11 | Complete curve inventory | COMPLETE | 14/144 curve-bearing; 130 without direct registered curve | `HR069_CHEMCORE_STAGE11_CURVE_INVENTORY_2026-10-02.md` |
| 12 | Curve provenance and precedence | COMPLETE | 144/144 policy-routed; duplicate-source relations not independent | `HR069_CHEMCORE_STAGE12_CURVE_PROVENANCE_PRECEDENCE_2026-10-02.md` |
| 13 | Curve-domain audit | COMPLETE | 144/144; no silent extrapolation | `HR069_CHEMCORE_STAGE13_CURVE_DOMAIN_AUDIT_2026-10-02.md` |
| 14 | R0 convention audit | COMPLETE | 144/144; field R0 separated from reference-gas validation | `HR069_CHEMCORE_STAGE14_R0_CONVENTION_AUDIT_2026-10-02.md` |
| 15 | HR069 transfer-status assignment | COMPLETE | 144/144; 9 higher-provenance + 5 research priors transfer-pending; 0 M4 | `HR069_CHEMCORE_STAGE15_HR069_TRANSFER_STATUS_2026-10-02.md` |
| 16 | Boundary / out-of-domain policy | COMPLETE | 144/144; diagnostic retention, no reportable extrapolated ppm | `HR069_CHEMCORE_STAGE16_BOUNDARY_OUT_OF_DOMAIN_POLICY_2026-10-02.md` |
| 17 | Cross-channel concentration consistency | COMPLETE | 6 multi-channel numeric candidates; 8 single-channel screening lane | `HR069_CHEMCORE_STAGE17_CROSS_CHANNEL_CONCENTRATION_CONSISTENCY_2026-10-02.md` |
| 18 | Fingerprint inventory | COMPLETE | 144/144; 13 model-derived numeric + 5 transfer evidence; 0 local empirical M3 | `HR069_CHEMCORE_STAGE18_FINGERPRINT_INVENTORY_2026-10-02.md` |
| 19 | Family-fingerprint layer | COMPLETE | 144/144 family routing; no naive centroid averaging | `HR069_CHEMCORE_STAGE19_FAMILY_FINGERPRINT_LAYER_2026-10-02.md` |
| 20 | Exact-compound fingerprint review | COMPLETE | 144/144; exact identity authority separated from family evidence | `HR069_CHEMCORE_STAGE20_EXACT_COMPOUND_FINGERPRINT_REVIEW_2026-10-02.md` |
| 21 | Fingerprint covariance readiness | COMPLETE | 0/144 covariance-ready; empirical sessions required later | `HR069_CHEMCORE_STAGE21_FINGERPRINT_COVARIANCE_READINESS_2026-10-02.md` |
| 22 | Temporal-feature readiness | COMPLETE | global 17 x 8 = 136 feature schema; 0 validated named temporal models yet | `HR069_CHEMCORE_STAGE22_TEMPORAL_FEATURE_READINESS_2026-10-02.md` |
| 23 | Family-first hierarchical classifier | COMPLETE | 144/144 routes; no mandatory flat 144-way winner | `HR069_CHEMCORE_STAGE23_FAMILY_FIRST_HIERARCHICAL_CLASSIFIER_2026-10-02.md` |
| 24 | PubChem molecular-record coverage | IN PROGRESS | 144/144 audit contract exists; exact initialized-device import counts remain to be recorded | `HR069_CHEMCORE_STAGE24_PUBCHEM_COVERAGE_CHECKPOINT_2026-10-02.md` |
| 25 | QSPR applicability domain | COMPLETE | 144/144 routed by eligibility; sensor-specific descriptor AD | `HR069_CHEMCORE_STAGE25_QSPR_APPLICABILITY_DOMAIN_AUDIT_2026-10-02.md` |
| 26 | QSPR leakage / leave-compound-out | COMPLETE | whole-compound LCO + pooled/worst-fold checks | `HR069_CHEMCORE_STAGE26_QSPR_LEAKAGE_LCO_AUDIT_2026-10-02.md` |
| 27 | Evidence lineage and correlation | COMPLETE | 144/144 policy-routed; derivation does not equal independence | `HR069_CHEMCORE_STAGE27_EVIDENCE_LINEAGE_CORRELATION_MAP_2026-10-02.md` |
| 28 | Unified evidence weighting | COMPLETE | immediate-group dedup -> lineage dedup -> capped correlated corroboration | `HR069_CHEMCORE_STAGE28_UNIFIED_EVIDENCE_WEIGHTING_POLICY_2026-10-02.md` |
| 29 | Specialist-conflict engine | COMPLETE | anchor/target conflict recorded; single-channel M2 exact fit cannot create specialist veto | `HR069_CHEMCORE_STAGE29_SPECIALIST_CONFLICT_ENGINE_2026-10-02.md` |
| 30 | Progressive result-state engine | COMPLETE | SIGNAL/AFFINITY/CANDIDATE/IDENTIFIED/QUANTIFIED + diagnostic branches | `HR069_CHEMCORE_STAGE30_PROGRESSIVE_RESULT_STATE_ENGINE_2026-10-02.md` |
| 31 | Rejected-evidence audit trail | COMPLETE | live/curve/domain/heater/specialist/OOD rejections retained diagnostically | `HR069_CHEMCORE_STAGE31_REJECTED_EVIDENCE_AUDIT_TRAIL_2026-10-02.md` |
| 32 | Open-set / OOD routing | COMPLETE | early-build OOD is diagnostic; solvable candidates remain visible as SCREENING | `HR069_CHEMCORE_STAGE32_EXPLORATORY_OPEN_SET_ROUTING_2026-10-02.md` |
| 33 | Mixture / interference model | COMPLETE | pure-hypothesis adequacy diagnostics; no unvalidated component decomposition | `HR069_CHEMCORE_STAGE33_MIXTURE_INTERFERENCE_POLICY_2026-10-02.md` |
| 34 | Source-material / headspace semantics | COMPLETE | 144/144; 135 pure, 6 mixture, 3 aggregate; pure-ppm semantics blocked only where semantically invalid | `HR069_CHEMCORE_STAGE34_SOURCE_MATERIAL_HEADSPACE_SEMANTICS_2026-10-02.md` |
| 35 | Identity ceiling / validation-state map | COMPLETE | 144/144; 4 provisional-identity-capable, 106 candidate, 19 affinity, 15 unsupported; map is not runtime gate | `HR069_CHEMCORE_STAGE35_IDENTITY_CEILING_MAP_2026-10-02.md` |
| 36 | M3/M4 experimental campaign matrix | COMPLETE | 144/144; 5 target-anchor, 96 broad-array pure, 9 mixture/aggregate, 19 compatibility, 15 hardware-gap campaigns | `HR069_CHEMCORE_STAGE36_EXPERIMENTAL_CAMPAIGN_MAP_2026-10-02.md` |
| 37 | Sensor health, drift and quality policy | COMPLETE | early-build hard invalidation only when no usable MQ data remain; other issues normally diagnostic/weighted | `HR069_CHEMCORE_STAGE37_SENSOR_HEALTH_DRIFT_QUALITY_POLICY_2026-10-02.md` |
| 38 | End-to-end reporting, regression and release trace | COMPLETE | analytical trace + representative regression; RC2 qualification passed | `HR069_CHEMCORE_STAGE38_END_TO_END_REPORTING_REGRESSION_2026-10-02.md` |

## Key current facts
- Primary anchors currently mapped: CO/MQ7, NH3/MQ135, H2/MQ8, CH4/MQ4, Ethanol/MQ3. Carbon Monoxide remains at `CANDIDATE` identity ceiling despite its primary specialist route because MQ7 heater/selectivity evidence remains incomplete; specialist role and identity ceiling are separate dimensions.
- 300 s is the agreed general operational warm-up. It does not manufacture selectivity or replace MQ7/MQ9 source-specific heater requirements.
- The current build deliberately preserves exploratory candidates under OOD/out-of-domain warnings. `QUANTIFIED` / reportable ppm still requires M4.
- Mixture/source/aggregate entries remain visible as operational classes but do not inherit one-pure-molecule ppm semantics.
- Identity ceilings are planning metadata, not runtime vetoes.
- Experiments are expected to revise the knowledge base; the current matrix is an auditable starting model, not a frozen scientific truth.

## Current software qualification
- RC2 run `36944401941` passed runtime contract, Android unit tests, debug APK build, APK verification and artifact upload at analytical head `c3e2f5b9d6a4bb6499b525d2d36c50554cfea7af`.
- Artifact: `HazRob-0.10.2-HR069-RC2-test`, ID `11201113185`, SHA-256 `c3c3f08d431355f52c2958b64a06a5ceab0242aca42d025422b92bc6f27e18d9`.
- Later commits only update analytical documentation and do not change the qualified analytical code path.
- Internal-validation workflow still contains a separate legacy Remote-diagnostics contract failure; this analytical workstream does not modify the network layer.

## Current execution point
**37 of 38 stages are formally complete.** Stage 24 is the only open stage: record exact PubChem/core-descriptor coverage after the initialized device database imports the molecular seed. The Stage-24 audit implementation already iterates all 144 catalogue entries and reports missing legacy record / non-molecular / no CAS / no structure / incomplete CORE / QSPR-ready states; only the initialized-device counts remain to be captured.
