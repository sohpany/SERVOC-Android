# HazRob 0.10.2 / HR069 — ChemCore Stage 19 Family-Fingerprint Layer

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries.

## Objective

Introduce an explicit family-level fingerprint layer between raw SIGNAL evidence and exact-compound identity. The family layer must improve routing without fabricating a family vector where no defensible training evidence exists.

## Principle

Exact-compound competition over all 144 entries is not the first analytical decision. The preferred route is:

`SIGNAL -> SENSOR-RESPONSE FAMILY -> FAMILY FINGERPRINT/AFFINITY -> COMPOUND SUBSET -> EXACT-COMPOUND EVIDENCE`

Structural taxonomy and sensor-response taxonomy remain separate. Family routing is based on response behavior plus chemistry, not structural labels alone.

## Family objects

The following operational family classes are retained from Stage 3 as routing objects where applicable:

- `ALCOHOL_VOC`
- `OXYGENATED_VOC`
- `COMBUSTIBLE_HYDROCARBON`
- `AROMATIC_VOC`
- `HALOGENATED_ORGANIC`
- `SULFUR_REDUCING`
- `NITROGEN_CONTAINING_TOXIC_OR_VOC`
- `SMALL_INORGANIC_REDUCING_EXPLORATORY`
- `CO_TARGET_PATTERN`
- `AMMONIA_TARGET_PATTERN`
- `HYDROGEN_TARGET_PATTERN`
- `BROAD_AIR_QUALITY_GAS`
- `REACTIVE_COMPATIBILITY_REVIEW`
- `UNSUPPORTED_CURRENT_MQ_ARRAY`
- mixture/operational families handled through their object type rather than forced into a pure-analyte family.

These are routing families, not claims of formal chemical taxonomy or selectivity.

## Family-fingerprint evidence states

Every catalogue entry maps to a family object and the family object carries one of:

- `FAMILY_EMPIRICAL_VALIDATED`
- `FAMILY_EMPIRICAL_PROVISIONAL`
- `FAMILY_MODEL_DERIVED_PROVISIONAL`
- `FAMILY_AFFINITY_RULE_ONLY`
- `FAMILY_HARDWARE_UNSUPPORTED`

At Stage 19 no family is promoted to `FAMILY_EMPIRICAL_VALIDATED` because no independent HR069 family training/validation campaign has yet been accepted.

## Construction rules

### 1. Never average unrelated exact-compound templates blindly

A family vector must not be the arithmetic mean of all member compounds unless the training design and validation demonstrate that such a centroid is meaningful. Molecular diversity, concentration dependence and differing sensor roles can make a naive centroid misleading.

### 2. Concentration is a nuisance variable unless explicitly modeled

A family classifier should learn or compare response shape after appropriate normalization/feature treatment, or use concentration-conditioned templates. High concentration of one family member must not become a shortcut for family identity.

### 3. Anchor sensors inform but do not define the whole family

Examples:

- MQ3 alcohol evidence can strongly route toward `ALCOHOL_VOC`, but cross-response conflict must remain visible.
- MQ8 can anchor a hydrogen hypothesis but other reducing gases may affect the same array.
- MQ135 ammonia evidence can route toward NH3 when in-domain, but broad air-quality responses are not equivalent to NH3 identity.

### 4. Unsupported/reactive families fail closed

`REACTIVE_COMPATIBILITY_REVIEW` and `UNSUPPORTED_CURRENT_MQ_ARRAY` cannot obtain a synthetic family fingerprint merely because QSPR or broad MQ channels can produce a numerical similarity.

### 5. Mixtures remain separate

LPG, Smoke, Natural Gas, CNG, Town Gas, TVOC, NOx and LEL-like operational classes are not used as pure-family centroids. They retain mixture/aggregate semantics.

## Initial family evidence available now

The current model-derived exact-compound priors can provide **provisional training/reference points** for only a subset of families:

- reducing/target gases: CO, H2, CH4, NH3;
- alcohol/oxygenated: Ethanol and Acetone;
- aromatic: Benzene and Toluene;
- combustible hydrocarbon: Propane, n-Hexane, CH4;
- broad air-quality/cross-response: CO2;
- operational classes: LPG and Smoke.

This does not constitute an empirically validated family fingerprint. It only means these families have some source-backed model coverage with which to prototype the routing layer.

## Family score design contract

Future runtime may compute a family score from:

- eligible anchor/secondary sensor roles;
- normalized static array shape;
- cross-channel curve consistency where meaningful;
- temporal feature evidence;
- model-derived or empirical family fingerprints;
- QSPR/chemical-class plausibility only as a supporting prior;
- OOD distance and contradiction penalties.

A family score is a routing compatibility score, not a probability unless separately calibrated.

## Required output

The family layer returns:

- `family_id`
- `family_state`
- `family_compatibility_score`
- `supporting_channels`
- `contradicting_channels`
- `evidence_types`
- `ood_state`
- `member_candidate_ids[]`

Only members of plausible families proceed to the exact-compound competition, while a low family fit may return `UNKNOWN_FAMILY`.

## Catalogue accounting

- Reviewed: 144/144 through the Stage 2/3/4 taxonomy linkage.
- Entries routed to an explicit response-family object: 144/144.
- HR069 empirically validated numeric family fingerprints: 0.
- Families with some model-derived member evidence: provisional only.
- Unsupported/reactive routes remain fail-closed rather than synthetically fingerprinted.

## Stage result

**COMPLETE — 144/144.**

Stage 20 reviews exact-compound fingerprint authority separately so family evidence cannot silently promote a compound to M3.