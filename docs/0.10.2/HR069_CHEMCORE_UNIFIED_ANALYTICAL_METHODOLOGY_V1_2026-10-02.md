# HazRob 0.10.2 / HR069 — ChemCore Unified Analytical Methodology v1

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Status: AGREED ANALYTICAL DESIGN

## Executive structure

The ChemCore analytical system is organized into four primary pillars:

1. **Taxonomy** — define what every catalogue entry is chemically and operationally.
2. **Evidence Matrix** — define what every installed sensor and every available evidence source can legitimately say about each catalogue entry.
3. **Analytical Engine** — convert live signals into family, candidate, identity and quantitation states using route-specific evidence fusion.
4. **Experimental Validation** — test, correct, promote, demote or replace knowledge/model relations using controlled HR069 evidence.

The 38-stage implementation plan is subordinate to these four pillars. It remains the execution checklist, but every stage must map to one pillar and all outputs must feed the central versioned knowledge base.

Canonical lifecycle:

`Taxonomy -> Evidence Matrix -> Analytical Engine -> Experimental Validation -> knowledge/model revision -> regression -> release`

Canonical inference path inside the analytical engine:

`SIGNAL -> FAMILY -> SENSOR-ROLE CORROBORATION -> CURVES -> FINGERPRINT -> TEMPORAL/QSPR -> OPEN-SET/OOD -> INTERFERENCE/MIXTURE -> IDENTITY LEVEL -> PPM GATE`

---

# PILLAR 1 — TAXONOMY

## Objective

Create a stable, revisable 144-entry chemical knowledge index that separates molecular identity from sensor behavior and analytical route.

Each catalogue entry must carry:
- stable catalogue index;
- canonical name and formula;
- CAS/PubChem linkage where available;
- structural chemical family;
- sensor-response family;
- hazard/functional family;
- analytical object type;
- analytical route;
- current hardware-suitability state;
- identity ceiling under current HR069 evidence.

## Structural family

Examples:
- alcohol
- aldehyde
- ketone
- alkane / alkene / alkyne
- aromatic hydrocarbon
- ester / ether
- carboxylic acid
- organosulfur / sulfur oxide / sulfide
- nitrogen oxide / amine / nitrile / hydride
- inorganic hydride
- hydrogen halide / elemental halogen / inorganic halide
- peroxide / oxidizer
- refrigerant / fluorocarbon
- mixture / aggregate / source-headspace class where no single molecular structure exists.

Structural classification must not confuse molecular oxides with salts: NO2 is a nitrogen oxide, not nitrate; SO2 is sulfur oxide, not sulfate; CO2 is carbon oxide, not carbonate.

## Sensor-response family

Operational families may differ from structural chemistry:
- alcohol/VOC
- oxygenated VOC
- combustible hydrocarbon
- aromatic VOC
- sulfur/reducing
- nitrogen-containing
- reducing gas
- oxidizing/reactive
- acid/reactive
- weak/unsupported-current-array
- aggregate air-quality / mixture / source headspace.

## Analytical object type

Controlled types:
- `PURE_ANALYTE`
- `MIXTURE`
- `AGGREGATE_METRIC`
- `SOURCE_MATERIAL_HEADSPACE`

Examples:
- formaldehyde = pure analyte;
- formalin = source material/headspace problem;
- LPG/natural gas/town gas = mixtures;
- smoke/TVOC/NOx/LEL = aggregate/operational classes.

## Analytical route

Each entry gets one primary route:
- `DEDICATED_ANCHOR`
- `TARGET_MQ`
- `BROAD_ARRAY`
- `REACTIVE_OR_UNSUPPORTED`
- `MIXTURE_OR_HEADSPACE`

The route is revisable when new hardware or new HR069 evidence is added.

---

# PILLAR 2 — EVIDENCE MATRIX

## Objective

Build the central `144 x 8` sensor-analyte matrix (1,152 pairs) plus all non-MQ evidence layers. The matrix is the knowledge substrate used by ChemCore; it is not merely documentation.

## Sensor-analyte role

Every sensor/analyte pair receives one role:
- `PRIMARY_ANCHOR`
- `SECONDARY_TARGET`
- `CORROBORATIVE`
- `CROSS_RESPONSE`
- `RESEARCH_ONLY`
- `HEATER_DEPENDENT`
- `UNSUPPORTED`

A sensor is not globally specialist or broad. Its role depends on the analyte.

Examples:
- MQ3 may be primary/target evidence for ethanol but cross-response for another gas.
- MQ8 may be primary/target evidence for H2 but cross-response for ethanol/CO/methane.
- MQ135 may be target evidence for NH3 but broad/cross-response for other VOCs.

## Evidence layers attached to each material/pair

The matrix must reference, not duplicate, the following evidence objects:

### Curves
For every available relation:
- source ID;
- source authority/tier;
- mathematical model;
- valid concentration domain;
- T/RH domain if declared;
- heater requirement;
- R0 convention;
- uncertainty semantics;
- HR069 transfer status;
- target vs graph/cross-response role;
- active/retired/superseded status.

### Fingerprints
For every family or exact-compound fingerprint:
- channels used;
- source/provenance;
- concentration/domain;
- model-derived vs empirical;
- sample count;
- covariance availability/readiness;
- temporal schema link;
- open-set threshold status;
- HR069 validation tier.

### PubChem/QSPR
For each molecular entry:
- CAS/CID mapping;
- descriptor completeness;
- applicability-domain status;
- sensor-QSPR model availability;
- evidence lineage;
- M1/non-reportable status unless later promoted by independent HR069 evidence.

### Temporal evidence
For each family/analyte where available:
- time-series availability;
- 17 features x 8 sensors schema;
- model version;
- session-level validation state;
- independence lineage relative to static fingerprint.

### Hardware suitability / gaps
For each material:
- adequate / partial / unsupported / compatibility-review;
- need/benefit of dedicated sensor or alternate technology;
- incompatibility/reactivity constraints;
- maximum identity ceiling with current hardware.

## Evidence precedence and provenance

No evidence value is stored without provenance and revision state.

Typical precedence:
1. HR069 M4 quantitative evidence;
2. HR069 M3 identity evidence;
3. manufacturer target/operating evidence;
4. peer-reviewed/direct literature response;
5. accepted external datasets;
6. manufacturer graph cross-response;
7. research/community/digitized relations;
8. PubChem/QSPR molecular prior/model-transfer.

Conflicts are retained explicitly rather than erased.

## Knowledge revision states

- `KNOWLEDGE_PRIOR`
- `SOURCE_BACKED_M1_M2`
- `HR069_OBSERVED`
- `HR069_M3_VALIDATED`
- `HR069_M4_VALIDATED`
- `CONFLICTED`
- `REJECTED_OR_SUPERSEDED`

Experiments may promote, demote, replace or supersede any evidence relation without changing catalogue identity keys.

---

# PILLAR 3 — ANALYTICAL ENGINE

## Objective

Use the Taxonomy and Evidence Matrix to select the correct route for each live measurement instead of applying one universal classifier to all 144 entries.

## 1. Data-quality pre-gate

Before inference, verify:
- ADC/raw validity;
- sensor availability and health;
- power/heater state;
- R0 validity, age and provenance;
- finite/positive Rs and Rs/R0;
- warm-up/stabilization state;
- saturation/missing channels;
- T/RH/P availability where required;
- time-series completeness for temporal inference;
- firmware/app/library/model version.

Mandatory physical prerequisites fail closed; invalid evidence is unavailable rather than merely down-weighted.

## 2. Progressive result states

ChemCore must preserve all stages of interpretation:

- `SIGNAL` — real response; no identity claim.
- `AFFINITY` — compatible family/compound pattern with incomplete evidence.
- `CANDIDATE` — passed principal analytical gates but not final identity validation.
- `IDENTIFIED` — accepted named identity at active M3 standard.
- `QUANTIFIED` — M4-valid reportable concentration.
- `ANALYTICALLY_REJECTED` — observed hypothesis retained with explicit rejection reason.
- `UNKNOWN_OOD` — no known class is acceptable.

## 3. Family-first routing

Default hierarchy:

`144 entries -> sensor-response family -> structural/functional subgroup -> exact candidates`

Exact-compound competition is only enabled when family evidence is sufficient or when a valid dedicated/target anchor legitimately bypasses broader family ambiguity.

Example alcohol route:

`MQ3/alcohol-like SIGNAL -> corroboration from broad channels -> ALCOHOL family -> alcohol candidate curves -> fingerprint -> temporal/QSPR -> OOD/conflict -> identity -> analyte-specific M4 ppm`

Example n-heptane route:

`combustible/VOC SIGNAL -> aliphatic hydrocarbon family -> broad-array pattern -> candidate curves where available -> family/exact fingerprints -> temporal + QSPR -> OOD -> probable/exact identity -> M4 gate`

## 4. Curve analysis

Curves answer whether the observed response magnitude is compatible with an analyte inside the source/model domain.

For sensor i and analyte j:

`r_obs = Rs/R0`

`z_ij(C) = [log10(r_obs) - log10(r_pred(C))] / sigma_ij`

For a candidate, fit one common concentration across usable channels and evaluate contributor-normalized standardized loss.

Retain for every channel:
- best compatible C*;
- residual/z;
- source/tier;
- domain status;
- boundary status;
- heater/environment validity;
- R0 convention;
- transfer status.

Rules:
- exact single-channel inversion is weak evidence;
- cross-channel common-concentration consistency is more informative than isolated fits;
- out-of-domain fits cannot create reportable ppm;
- boundary-hugging solutions are flagged `EDGE_OF_DOMAIN`;
- correlated channels do not count as fully independent confirmations.

## 5. Fingerprint analysis

Fingerprints test shape/pattern rather than concentration alone.

Static vector:

`x = [log10(Rs/R0)_MQ2, ..., log10(Rs/R0)_MQ135]`

Use two layers where available:
- family fingerprint;
- exact-compound fingerprint.

Rules:
- family and exact fingerprints have separate thresholds;
- model-derived templates remain diagonal unless empirical covariance exists;
- sufficient contributors and acceptable absolute distance are required;
- lowest distance alone is not enough;
- missing-feature handling uses correct covariance/subspace math;
- fingerprint and curve descendants from the same source share lineage to avoid double counting.

## 6. Temporal evidence

Validated temporal features may discriminate compounds with similar steady-state patterns:
- rise/recovery slopes;
- t10/t50/t90;
- AUC;
- peak/plateau ratios;
- recovery behavior;
- other fixed features from the 17 x 8 schema.

Temporal evidence is inactive for named identity until independently validated and shares an independence group with same-session static evidence where appropriate.

## 7. PubChem/QSPR

QSPR is a molecular plausibility/model-transfer layer.

It can:
- support family/subgroup plausibility;
- rank broad-array candidates lacking exact specialist channels;
- provide predicted response priors inside its applicability domain.

It cannot by itself:
- create M3/M4 identity;
- create reportable ppm;
- override stronger direct evidence on the same channel.

Leave-compound-out/held-out-compound validation and applicability-domain checks are mandatory.

## 8. Evidence independence and weighting

Conceptual contribution:

`W_ij = W_role * W_tier * W_domain * W_quality * W_transfer * W_independence`

The engine distinguishes:
- physical channel count;
- evidence-source count;
- effective independent evidence groups.

Numerical weights are research-policy values until experimentally validated and are never tuned solely to make one known challenge pass.

## 9. Specialist conflict

If a specialist/target channel and broad-array evidence disagree, emit `SPECIALIST_CONFLICT` and downgrade the decision rather than forcing a winner.

Possible actions:
- hold at AFFINITY/CANDIDATE;
- require fingerprint agreement;
- invoke validated temporal evidence;
- invoke OOD/open-set rejection;
- report the conflict explicitly.

## 10. Open-set / OOD

Relative ranking does not imply acceptable identity. Reject all known candidates when family/fingerprint/curve/model fit is globally inadequate.

Result may be:
- `UNKNOWN_OOD`;
- or a broader family-only result such as `ALIPHATIC_HYDROCARBON / EXACT_IDENTITY_UNRESOLVED`.

## 11. Mixture and interference

A poor pure-compound fit may indicate mixture/interference but does not identify mixture components automatically.

Permitted states:
- `POSSIBLE_MIXTURE_OR_INTERFERENCE`
- `PRIMARY_ANALYTE_WITH_INTERFERENCE`

Mixture decomposition requires a separately validated model.

## 12. Rejected-evidence audit trail

Initial/live hypotheses never disappear silently.

For each rejected hypothesis retain:
- initial signal source;
- family/candidate;
- stage reached;
- rejection reason;
- curves/fingerprints consulted;
- domain/heater/transfer state;
- conflict evidence;
- final state.

Standard rejection reasons include:
- `OUT_OF_VALIDATED_RESPONSE_DOMAIN`
- `EDGE_OF_DOMAIN`
- `HEATER_PROTOCOL_INVALID`
- `INSUFFICIENT_CONTRIBUTORS`
- `SPECIALIST_CONFLICT`
- `FINGERPRINT_DISTANCE_FAIL`
- `OPEN_SET_REJECT`
- `UNSUPPORTED_HARDWARE`
- `DATA_QUALITY_FAIL`
- `TRANSFER_NOT_VALIDATED`

This supports cases where CO/CO2-like live signals later disappear from final candidates: they remain visible as analytically rejected evidence with reason.

## 13. Identity and ppm separation

Identity and quantitation are separate.

Reportable ppm requires HR069 M4 evidence:
- known-reference calibration;
- validated concentration domain;
- required heater/environmental conditions;
- current R0 provenance;
- independent validation;
- stated uncertainty.

A liquid concentration such as ethanol 96% is not gas-phase ppm.

---

# PILLAR 4 — EXPERIMENTAL VALIDATION

## Objective

Use controlled HR069 experiments to test the current knowledge/model, not merely to fill missing cells. Experiments are the mechanism for correcting the knowledge base and promoting evidence tiers.

## Experimental feedback loop

For every controlled challenge:
1. freeze current knowledge/model version;
2. preserve raw session data and provenance;
3. run analysis blind to ground truth where possible;
4. lock SIGNAL/AFFINITY/CANDIDATE/IDENTIFIED/QUANTIFIED and rejected hypotheses;
5. reveal ground truth after result lock;
6. attribute failure to one or more layers: sensor suitability, curve source, transfer, fingerprint, temporal model, QSPR, evidence correlation, OOD, mixture/interference or data quality;
7. change the smallest justified knowledge/model component;
8. rerun affected-family and global regression suites;
9. version the replacement evidence/model;
10. promote only when performance generalizes to independent sessions/compounds.

## Validation levels

### M3 identity validation
Requires independent labeled sessions, family/compound confusion evaluation, open-set known/unknown testing and route-specific acceptance thresholds.

### M4 quantitative validation
Requires device-specific HR069 known-reference concentrations, multi-level calibration, independent sessions, domain definition and uncertainty.

Initial quantitative campaign requirements remain:
- at least 5 non-zero concentration levels;
- clean-air blank in each block;
- at least 5 independent sessions per level as initial floor, preferably 8–10;
- preferably at least 3 days/blocks;
- documented chamber/source/flow/exposure/recovery/T/RH/P/heater/R0/hardware/app/firmware/library provenance;
- independent final validation.

### Environmental validation
T/RH compensation is fitted only after controlled multi-level environment x concentration data demonstrate independent benefit.

### Fingerprint covariance validation
Covariance-aware static fingerprinting requires sufficient independent HR069 sessions; model-derived templates do not count as replicates.

### Temporal validation
Temporal models use session-level train/validation/test splits; timepoints from one exposure are not independent samples.

### QSPR validation
Use chemically diverse compounds, leave-compound-out validation, applicability-domain analysis and held-out compounds. QSPR promotion remains separate from numerical fit quality.

### Mixture/interference validation
Use pure components plus controlled mixtures at multiple composition and total-concentration levels; do not assume linear sensor additivity.

## Revision rule

The knowledge base is deliberately flexible. Experimental evidence may:
- promote a sensor/analyte relation;
- demote a previously assumed relation;
- revise role classification;
- change valid domains;
- replace/transfer a curve;
- create an empirical fingerprint;
- alter OOD thresholds;
- alter family routing;
- add/remove a hardware gap;
- change identity ceiling;
- invalidate dependent runtime models.

All revisions are versioned and previous evidence remains available for audit.

---

# Mapping of the 38 execution stages to the four pillars

## Taxonomy
Stages 1–5.

## Evidence Matrix
Stages 6–22 plus 24–27.

## Analytical Engine
Stages 23, 28–35, 37–38.

## Experimental Validation
Stage 36 plus all evidence-gated promotions in stages 15, 19–22, 25–29, 32–35, 37–38.

The mapping is intentionally iterative: validation may send a record back to Taxonomy or Evidence Matrix for revision.

---

# Final acceptance criterion

ChemCore is considered scientifically mature only when it can, for every one of the 144 catalogue entries:

1. state whether current HR069 hardware can meaningfully sense it;
2. explain which channels are primary, secondary, corroborative, cross-response or unsupported;
3. identify the applicable family and analytical route;
4. select only valid curves/fingerprints/QSPR/temporal evidence;
5. preserve conflicts and rejected hypotheses;
6. reject unsupported/OOD cases without forced naming;
7. separate pure compounds from mixtures/headspace/aggregate metrics;
8. state the maximum defensible identity level;
9. produce ppm only when M4 validation exists;
10. expose provenance and model/library version for every final decision.
