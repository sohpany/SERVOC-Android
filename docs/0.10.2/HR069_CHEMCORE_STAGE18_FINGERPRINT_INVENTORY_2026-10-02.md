# HazRob 0.10.2 / HR069 — ChemCore Stage 18 Fingerprint Inventory

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; static and trainable fingerprint evidence available in the current branch.

## Objective

Inventory fingerprint evidence without conflating model-derived templates, external transfer evidence, user/HR069 empirical fingerprints, family fingerprints, and mixture/operational-class fingerprints.

## Runtime fingerprint representation

`ChemicalFingerprint` stores:

- `chemicalId`
- `label`
- `sampleCount`
- `meanLogRatio[]`
- `sigmaLogRatio[]`

in `log10(Rs/R0)` space.

`FingerprintTrainer` can construct empirical fingerprints from repeated ratio captures and computes per-channel sample mean and sample standard deviation in log space. This is a diagonal-uncertainty representation; it does not estimate a full covariance matrix.

`FingerprintClassifier` computes a standardized per-channel distance and uses available contributors only. Heater-cycled MQ7/MQ9 channels are fail-closed for research-prior classification when heater-cycle validity is unavailable.

## Static model-derived research-prior library

`ResearchIdentificationFingerprintRegistry` generates deterministic concentration templates from accepted numeric response relations. These are **source/model-derived expectations**, not HR069 empirical fingerprints.

Important properties:

- release: `0.10.2`
- hardware label: `HR069`
- evidence tier: `RESEARCH_EXTERNAL`
- identity state: `ACCEPTED_FOR_IDENTIFICATION`
- `M4=false`
- all generated templates use `sampleCount=0`
- concentration grid: 10, 25, 50, 100, 200, 300, 500, 1000, 3000, 5000, 10000 ppm, but a template/channel is generated only where at least one eligible source relation is in domain.
- source precedence inside each channel is literature first, then eligible GasEngine relation, then ResearchCurveRegistry.

### Pure-analyte numeric-prior coverage — 11 chemicals

- 20 Carbon Monoxide
- 21 Ammonia
- 23 Carbon Dioxide
- 34 Benzene
- 75 Hydrogen
- 76 Methane
- 78 Propane
- 84 n-Hexane
- 95 Toluene
- 103 Ethanol
- 110 Acetone

### Operational/composite numeric-prior coverage — 2 catalogue entries

- 135 LPG
- 139 Smoke

These must remain operational-class fingerprints; they cannot be promoted to universal pure-analyte ppm fingerprints.

### Important omission relative to curve inventory

NOx (141) has a linear literature response relation but is not currently included in `ResearchIdentificationFingerprintRegistry.NUMERIC_ANALYTES`. Therefore Stage 18 records it as **curve available / static fingerprint not generated** rather than silently treating its curve as a fingerprint.

## External transfer evidence without a defensible static HR069 numeric vector

The registry explicitly tracks several targets as `TRANSFER_DATA_AVAILABLE` while refusing to invent an HR069 numeric fingerprint:

- 4 Hydrogen Sulfide — manufacturer sulfide-family affinity plus noninstalled dedicated-sensor references; exact HR069 H2S vector pending.
- 54 Acetaldehyde — external known-concentration MOS-array evidence; exact HR069 vector pending.
- 89 Ethylene — source-backed known-concentration evidence including MQ3 work; raw/tabulated exact response transfer still pending.
- 102 Methanol — peer-reviewed multi-MQ methanol/ethanol mixture study includes all eight HR069 MQ models, but exact current-protocol HR069 numeric vectors are pending.
- 115 Ethyl Acetate — external known-concentration MOS/MQ135 evidence; exact multi-channel HR069 vector pending.

These records are transfer/reference evidence, not numeric fingerprints and not M4.

## HR069 empirical fingerprint status

The branch contains a generic empirical training path (`FingerprintTrainer`) but the audited static library does **not** contain a validated HR069 empirical fingerprint set with independent repeated sessions, concentration labels, provenance, and covariance-ready sample counts for the 144 catalogue entries.

Therefore:

- static HR069 empirical exact-compound fingerprints accepted as M3: **0** at this stage;
- model-derived static numeric priors: **13 catalogue entries** (11 pure + 2 operational);
- explicit transfer-data targets without numeric HR069 vectors: **5 additional entries**;
- remaining catalogue entries without static numeric fingerprint or explicit transfer target in this registry: **126**.

Absence means `FINGERPRINT_EVIDENCE_GAP`, not zero sensor response.

## Covariance status

Current `ChemicalFingerprint` stores per-channel sigma only. No full sensor covariance matrix is stored.

Model-derived templates use `sampleCount=0`; therefore covariance must not be fabricated from their deterministic concentration grid.

Future covariance readiness requires independent HR069 sessions. Policy remains:

- minimum >=20 independent sessions for any covariance estimate;
- preferably >=30;
- independence is by acquisition session/experimental replicate, not by time samples within one exposure trace.

## Fingerprint evidence classes to preserve

Every fingerprint record in the future Evidence Matrix shall carry one of:

- `EXACT_COMPOUND_MODEL_DERIVED`
- `EXACT_COMPOUND_HR069_EMPIRICAL`
- `FAMILY_MODEL_DERIVED`
- `FAMILY_HR069_EMPIRICAL`
- `OPERATIONAL_CLASS_MODEL_DERIVED`
- `OPERATIONAL_CLASS_HR069_EMPIRICAL`
- `EXTERNAL_TRANSFER_NONNUMERIC`
- `SOURCE_PENDING`

and metadata for:

- source/provenance
- evidence tier
- channel mask / contributor count
- concentration anchor or grid
- sample count
- sigma availability
- covariance availability
- environment/heater context
- HR069 transfer state
- identity use ceiling
- M4 state

## Identity-use policy

1. Model-derived numeric fingerprints may support family/candidate identity ranking but remain research/reference priors.
2. Exact HR069 empirical fingerprints may only be promoted after independent validation and open-set testing.
3. Family fingerprints and exact-compound fingerprints must remain separate objects.
4. Mixture/operational fingerprints remain separate from pure analytes.
5. External transfer evidence without exact HR069 numeric vectors is retained as evidence, not converted into synthetic local vectors.
6. Fingerprint identity evidence never creates reportable ppm by itself.

## Catalogue accounting

- Reviewed: 144/144.
- Static model-derived numeric fingerprint coverage: 13.
- Additional explicit transfer-evidence/no-numeric-vector entries: 5.
- No static numeric/explicit-transfer fingerprint in the current research registry: 126.
- Validated static HR069 empirical M3 fingerprints: 0.
- Full covariance fingerprints: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 19 will define the family-fingerprint layer. It will not average unrelated chemicals blindly; family templates will be generated/validated only where the taxonomy and available evidence can support a meaningful family response model, with missing families left as evidence gaps.