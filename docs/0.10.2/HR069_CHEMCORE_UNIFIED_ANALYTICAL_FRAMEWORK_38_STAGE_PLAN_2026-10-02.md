# HazRob 0.10.2 / HR069 — ChemCore Unified Analytical Framework

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Baseline at plan creation: `79c9f9f84cad3abd6a127a4c10af62dd1295f681`
Scope: the 144-entry HazRob chemical catalogue, the installed HR069 MQ array, analytical evidence routing, curves, fingerprints, temporal features, PubChem/QSPR, open-set/OOD, mixtures/interferences, identity and reportable concentration.
Status: MASTER EXECUTION PLAN — execution is controlled by per-stage gates and a 144-entry progress ledger.

## Final objective

Build a scientifically defensible chemical-analysis system that does not force every analyte through one algorithm. ChemCore must determine what the installed hardware can physically support, classify the chemical family before exact identity when appropriate, distinguish primary/secondary/corroborative/cross-response evidence, exploit curves and fingerprints without double counting, preserve initial signals that are later rejected, reject unsupported/out-of-domain hypotheses, distinguish pure analytes from mixtures/source-material headspace, and only produce reportable ppm after HR069-specific M4 validation.

Canonical inference target:

`Signal -> Family -> Sensor-role corroboration -> Curve consistency -> Fingerprint -> Temporal/QSPR -> Open-set/OOD -> Interference/mixture -> Identity level -> Quantitation gate`

The system must be able to return a scientifically useful partial result such as `ALIPHATIC_HYDROCARBON / EXACT_IDENTITY_UNRESOLVED` or `CO-LIKE_SIGNAL / ANALYTICALLY_REJECTED_OUT_OF_DOMAIN` instead of manufacturing a named compound.

## Governing invariants

1. No literature/manufacturer/research curve self-promotes to HR069 M4.
2. No QSPR-only result may create reportable ppm or M3/M4 identity.
3. No unsupported sensor/analyte pair may gain evidence merely because a mathematical curve can be fit.
4. `TARGET`, `GRAPH_RESPONSE`, `COMMUNITY/RESEARCH`, and empirical HR069 evidence remain distinct.
5. A single-channel exact fit is not automatically stronger than a multisensor fit; identifiability and sensor role are separate dimensions.
6. Correlated MOS responses are not counted as independent confirmations.
7. Heater-protocol-dependent channels are invalid when the required heater phase/cycle is absent or unknown.
8. Out-of-domain and boundary-hugging fits are surfaced explicitly and cannot silently become ppm.
9. Initial/live signals are retained even when later rejected analytically, with the rejection reason.
10. Pure analytes, mixtures, aggregate metrics and source-material headspaces are separate analytical object types.
11. Missing T/RH/P, R0 provenance, sensor-health or acquisition-quality fields fail closed where the active model declares them mandatory.
12. Every material-level value in the central analytical matrix must carry provenance, rationale and review status; blank fields cannot be silently interpreted as negative evidence.
13. Software ranking weights remain compatibility/ranking quantities, never calibrated presence probabilities unless a separate validation proves otherwise.
14. Training/tuning and final validation units are separated by independent sessions, not timepoints from the same exposure.

## Controlled vocabulary

### Sensor-analyte role

- `PRIMARY_ANCHOR` — documented primary/target relation capable of anchoring identity when other gates permit.
- `SECONDARY_TARGET` — documented target relation but lower selectivity or multi-target behavior.
- `CORROBORATIVE` — useful array-pattern support; not independently selective.
- `CROSS_RESPONSE` — known response/interference; may support or contradict but cannot independently establish identity.
- `RESEARCH_ONLY` — research/community/external relation not yet promoted for operational identity.
- `HEATER_DEPENDENT` — evidence usable only under the exact required heater protocol/phase.
- `UNSUPPORTED` — no defensible physical/evidence basis in the installed array.

### Analytical route

- `DEDICATED_ANCHOR`
- `TARGET_MQ`
- `BROAD_ARRAY`
- `REACTIVE_OR_UNSUPPORTED`
- `MIXTURE_OR_HEADSPACE`

### Result progression

- `SIGNAL` — real sensor response; no compound claim.
- `AFFINITY` — similarity to a family/compound hypothesis but incomplete evidence.
- `CANDIDATE` — passed principal analytical gates but not final identity criteria.
- `IDENTIFIED` — validated M3-level named identity according to the active route.
- `QUANTIFIED` — M4-valid reportable concentration with stated domain/uncertainty.
- `ANALYTICALLY_REJECTED` — observed signal/hypothesis retained with explicit rejection reason.
- `UNKNOWN_OOD` — no known class is acceptable.

## Mandatory per-material record

Every one of the 144 catalogue entries must eventually have:

- catalogue index, canonical name, formula, CAS when available;
- structural family, sensor-response family, hazard/functional family, analytical route;
- object type: pure analyte / mixture / aggregate metric / source-material headspace;
- installed-hardware suitability: adequate / partial / unsupported / compatibility-review;
- identity ceiling under current HR069 hardware/evidence;
- eight MQ channel records, each with role, evidence source, evidence tier, valid concentration domain, heater requirement, T/RH domain, expected response direction, uncertainty semantics, R0 convention, transfer status, known interferents, fingerprint contribution and temporal-model status;
- curve inventory and source precedence;
- fingerprint inventory and provenance;
- PubChem/QSPR eligibility, applicability-domain status and independence lineage;
- open-set/OOD requirements;
- mixture/interference notes;
- M3 validation state;
- M4 quantitative-validation state;
- unresolved evidence gaps and hardware gaps.

## Uniform execution protocol for every stage

Every stage MUST follow the same mechanism:

1. **Input freeze** — record branch/head, catalogue version, source registries and any experimental artifacts used.
2. **Rule declaration** — write the classification/calculation rule before applying it to the 144 entries.
3. **Source precedence** — manufacturer/authoritative documentation, peer-reviewed literature, accepted external datasets, controlled HR069 evidence, research/community evidence; do not silently merge conflicting tiers.
4. **144-entry pass** — every chemical receives one of `REVIEWED_ACCEPTED`, `REVIEWED_GAP`, `NOT_APPLICABLE_WITH_REASON`, or `BLOCKED_BY_EVIDENCE` for the stage.
5. **No inferred blanks** — unknown remains unknown; no negative claim is generated from absent data.
6. **Conflict handling** — conflicting evidence is retained as a conflict record with source IDs; no hand-picked winner without declared precedence/justification.
7. **Automated validation where possible** — schema, counts, uniqueness, domain bounds, CAS/index linkage, curve/fingerprint references, controlled vocabulary and orphan-reference checks.
8. **Regression protection** — any runtime-affecting stage adds tests for representative anchor, broad-array, unsupported/reactive, mixture and unknown/OOD cases.
9. **Stage report** — record counts: `reviewed/144`, `accepted/144`, `gaps/144`, `blocked/144`, `not-applicable/144`, plus changed files and commit SHA.
10. **Definition of Done gate** — a stage is COMPLETE only when 144/144 entries have an explicit stage state, automated checks pass, conflicts/gaps are documented, and no evidence-gated claim is promoted by assumption.

## Workstream A — catalogue, taxonomy and routing foundation

### Stage 1 — Freeze and census the 144-entry catalogue
Goal: establish the immutable working list and unique index/name/formula mapping used by all later stages.
Deliverable: canonical 144-entry inventory and checksum/version metadata.
DoD: exactly 144 unique catalogue rows; no duplicate index; all later artifacts key to this inventory.

### Stage 2 — Structural chemical taxonomy
Goal: classify each entry into chemically meaningful structural families (alcohol, aldehyde, ketone, alkane, aromatic hydrocarbon, ester, ether, sulfur compound, nitrogen compound, oxide, hydride, halogenated organic, acid gas, refrigerant, etc.).
Guidance: molecular taxonomy must not confuse molecular oxides with salts (for example NO2 is nitrogen oxide, not nitrate; SO2 is sulfur oxide, not sulfate).

### Stage 3 — Sensor-response family taxonomy
Goal: assign sensor-behavior families such as alcohol/VOC, combustible hydrocarbon, aromatic VOC, oxygenated VOC, sulfur/reducing, nitrogen-containing, oxidizing/reactive, acid/reactive, unsupported-current-array, aggregate-air-quality.
Guidance: this family is operational and may differ from structural family.

### Stage 4 — Object-type taxonomy
Goal: distinguish `PURE_ANALYTE`, `MIXTURE`, `AGGREGATE_METRIC`, `SOURCE_MATERIAL_HEADSPACE`.
Guidance: formalin is not formaldehyde gas; LPG/natural gas/smoke/TVOC are not pure analytes.

### Stage 5 — Analytical-route assignment
Goal: assign one primary analytical route to each material: DEDICATED_ANCHOR, TARGET_MQ, BROAD_ARRAY, REACTIVE_OR_UNSUPPORTED, or MIXTURE_OR_HEADSPACE.
Guidance: route is based on physical/evidence suitability, not a desired output.

## Workstream B — sensor/analyte evidence matrix

### Stage 6 — Build the 144 x 8 sensor-role matrix
Goal: classify all 1,152 sensor/analyte pairs using the controlled role vocabulary.
Guidance: classify the pair, not merely the sensor. MQ135 may be target evidence for NH3 yet only cross-response/support for another analyte.

### Stage 7 — Anchor and specialist map
Goal: identify valid primary/secondary anchors and explicit absence of anchors.
Guidance: lack of an anchor is not failure; it routes the analyte to broad-array/fingerprint reasoning.

### Stage 8 — Heater-protocol dependency map
Goal: flag pairs whose evidence is conditional on heater phase/cycle (notably MQ7/MQ9 relations).
Guidance: heater-invalid evidence is unavailable, not merely down-weighted.

### Stage 9 — Hardware-suitability and compatibility review
Goal: classify current HR069 physical support as adequate, partial, unsupported, or compatibility-review for each chemical.
Guidance: highly oxidizing/corrosive/reactive materials must not be experimentally exposed to the array merely to fill a matrix cell.

### Stage 10 — Hardware-gap register
Goal: identify which analytes require or materially benefit from dedicated sensors/other technologies (for example PH3, H2S, NO2, SO2, HCHO where applicable).
Guidance: this is a design-gap register, not an instruction to purchase or expose hardware.

## Workstream C — curves, quantitative relations and transfer

### Stage 11 — Complete curve inventory
Goal: index every manufacturer, literature, linear-literature, research/community and accepted response relation by sensor/analyte pair.

### Stage 12 — Curve provenance and precedence audit
Goal: declare source authority, direct vs cross-response status and precedence when duplicate/conflicting curves exist.
Guidance: lower-tier curve may remain visible for comparison but cannot silently override higher-tier accepted evidence.

### Stage 13 — Curve-domain audit
Goal: capture valid concentration range, T/RH domain, heater requirements and any declared source conditions.

### Stage 14 — R0 convention audit
Goal: record how each source defines R0 and whether its normalization is directly compatible with HR069 field R0.

### Stage 15 — HR069 transfer-status assignment
Goal: classify each usable relation as `DIRECTLY_COMPATIBLE`, `TRANSFER_REQUIRED`, `INCOMPATIBLE`, or `NOT_TESTED`.
Guidance: do not tune transfer parameters from one uncontrolled challenge.

### Stage 16 — Boundary/out-of-domain policy application
Goal: apply consistent flags for edge-hugging optima, extrapolation and unavailable domains.
Guidance: outside-domain values may be retained diagnostically but cannot create reportable ppm.

### Stage 17 — Cross-channel concentration-consistency model
Goal: evaluate whether multiple accepted relations for one candidate support a common concentration.
Guidance: use log-concentration dispersion/standardized residuals; do not reward many correlated channels as independent proof.

## Workstream D — fingerprints, family inference and temporal evidence

### Stage 18 — Fingerprint inventory
Goal: index all current numeric/model-derived/external/empirical fingerprints, their channels, concentration, source and sample count.

### Stage 19 — Family-fingerprint layer
Goal: create family-level reference patterns where evidence supports them (alcohol/VOC, aliphatic hydrocarbon, aromatic VOC, etc.).
Guidance: family fingerprints may be broader than exact-compound fingerprints and must have separate thresholds.

### Stage 20 — Exact-compound fingerprint review
Goal: determine which of the 144 entries have defensible exact-compound fingerprint evidence and its current tier/limitations.

### Stage 21 — Fingerprint covariance readiness
Goal: mark which empirical artifacts satisfy the independent-session floor for covariance; model-derived templates remain diagonal-only.

### Stage 22 — Temporal-feature readiness
Goal: record availability and validation status of 17 x 8 temporal features by analyte/family.
Guidance: same-session static and temporal features share an independence lineage to prevent double counting.

### Stage 23 — Family-first hierarchical classifier design
Goal: formalize `signal -> family -> subgroup -> compound` routing and define when exact-compound competition is permitted.

## Workstream E — molecular/QSPR, independence and fusion

### Stage 24 — PubChem molecular-record coverage
Goal: audit 144/144 for resolved CAS/CID/descriptors and explicit non-molecular/mixture exclusions.

### Stage 25 — QSPR applicability-domain audit
Goal: determine per chemical whether descriptors, concentration and environmental space are inside the validated model domain.

### Stage 26 — QSPR leakage/leave-compound-out audit
Goal: ensure candidate evaluation can be assessed without self-training leakage; use leave-compound-out and held-out-compound logic.

### Stage 27 — Evidence-lineage and correlation map
Goal: assign independence groups to curves, fingerprints, QSPR, static response and temporal features so duplicated descendants of one source are not counted as independent evidence.

### Stage 28 — Unified evidence-weighting policy
Goal: define weights/fusion terms for role, evidence tier, domain validity, quality, transfer and independence.
Guidance: numerical weights are not finalized from intuition; defaults remain research policy until validated.

### Stage 29 — Specialist-conflict engine
Goal: detect contradictions between anchor/specialist evidence and broad-array/family evidence and downgrade the decision instead of forcing a winner.

## Workstream F — inference semantics, OOD and mixtures

### Stage 30 — Progressive result-state engine
Goal: implement/standardize SIGNAL -> AFFINITY -> CANDIDATE -> IDENTIFIED -> QUANTIFIED plus ANALYTICALLY_REJECTED and UNKNOWN_OOD.

### Stage 31 — Rejected-evidence audit trail
Goal: preserve live/initial hypotheses that disappear after analysis, with reasons such as OUT_OF_DOMAIN, HEATER_INVALID, INSUFFICIENT_CONTRIBUTORS, SPECIALIST_CONFLICT, OOD or UNSUPPORTED_HARDWARE.

### Stage 32 — Open-set/OOD routing
Goal: calibrate/define the known-vs-unknown gate and prevent forced classification when no known family/compound fits adequately.

### Stage 33 — Mixture/interference model
Goal: separate `pure hypothesis inadequate` from actual component decomposition; support `PRIMARY_ANALYTE_WITH_INTERFERENCE` and `POSSIBLE_MIXTURE_OR_INTERFERENCE` without claiming unvalidated mixture identities.

### Stage 34 — Source-material/headspace semantics
Goal: handle formalin, smoke, LPG, natural gas and other source/mixture classes separately from pure-analyte ppm models.

## Workstream G — metrology, quality and validation

### Stage 35 — Identity ceiling and validation-state map
Goal: assign current maximum defensible state per material: SIGNAL/AFFINITY/CANDIDATE/M3-identifiable/M4-quantifiable/unsupported.

### Stage 36 — M3/M4 experimental campaign matrix
Goal: produce the controlled validation requirements per material/family, using the existing experimental-validation protocol for independent sessions, concentration levels, environment, open-set and mixtures.

### Stage 37 — Sensor health, drift and data-quality gate
Goal: integrate R0 age/provenance, heater state, power stability, ADC validity, saturation, missing channels, T/RH/P availability, drift and sensor aging into pre-inference eligibility.

## Workstream H — reporting, regression and release control

### Stage 38 — End-to-end reporting, regression and release qualification
Goal: make the analytical result explain the full path from signal through family/curves/fingerprint/temporal-QSPR/OOD to final identity/ppm, preserve rejected evidence, version all analytical artifacts, and run representative regression + blind-validation suites before release.
Required representative regression classes: anchor analyte, broad-array analyte, unsupported/reactive analyte, mixture/headspace, heater-dependent analyte, out-of-domain case, specialist conflict, unknown/OOD, and M4-gated ppm case.

## Stage completion ledger fields

For every stage, the progress ledger records:

- `stage_id`
- `stage_name`
- `status`
- `branch_head_input`
- `branch_head_output`
- `reviewed_count` / 144
- `accepted_count` / 144
- `gap_count` / 144
- `blocked_count` / 144
- `not_applicable_count` / 144
- `pair_count` where a sensor/analyte matrix applies (expected denominator 1,152)
- `sources_used`
- `files_changed`
- `automated_checks`
- `regression_tests`
- `unresolved_conflicts`
- `evidence_gates`
- `next_stage_dependencies`

A system-level stage still assigns an explicit state to every one of the 144 chemicals; `NOT_APPLICABLE_WITH_REASON` is permitted and counted. Therefore no stage can be marked COMPLETE with silent missing catalogue rows.

## Execution order and gating

The order above is intentional. Stages 1–10 establish what each analyte is and what the hardware can physically support. Stages 11–17 establish the trustworthy numerical-response layer. Stages 18–23 establish pattern/family/temporal evidence. Stages 24–29 establish molecular evidence and non-double-counting fusion. Stages 30–34 define result semantics, OOD and mixtures. Stages 35–37 establish metrological/quality ceilings. Stage 38 integrates and qualifies runtime behavior.

Runtime changes that alter candidate ranking/identity are prohibited before their required knowledge stages are complete enough to avoid hard-coded special cases. Documentation/data construction may proceed while evidence-gated experimental items remain BLOCKED.

## Initial execution status at plan creation

- Catalogue source exists in `ChemicalCatalog` and declares the canonical 144-entry application catalogue.
- Existing code already contains partial structural/operational families in `MqSensorAffinity`, manufacturer target/graph distinctions in `ManufacturerEvidenceMatrix`, operating envelopes in `SensorOperatingEnvelope`, coverage states in `ChemicalCoverage`, accepted curve registries, research fingerprints, PubChem molecular descriptors/QSPR and open-set/mixture infrastructure.
- The prior remediation plan remains authoritative for already-qualified numerical/software fixes; this 38-stage framework extends it into a catalogue-wide analytical knowledge and routing program rather than replacing it.
- Experimental promotion gates remain governed by `HR069_CHEMCORE_EXPERIMENTAL_VALIDATION_REQUIREMENTS_2026-10-01.md`.

## Resumption rule

On any future continuation:
1. read this plan;
2. read the 144-stage progress ledger;
3. verify current branch head;
4. resume the earliest incomplete unblocked stage;
5. never infer completion from code presence alone — completion is ledger + 144 coverage + checks + commit evidence.
