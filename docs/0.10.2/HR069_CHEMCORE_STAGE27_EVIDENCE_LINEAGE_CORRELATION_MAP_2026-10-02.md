# HazRob 0.10.2 / HR069 — Stage 27 Evidence-Lineage and Correlation Map

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: ChemCore analytical evidence provenance and dependence.

## Why this stage is necessary
`independenceGroup` prevents duplicate copies of one immediate evidence source from being counted repeatedly. It does not by itself describe upstream derivation. Different engines can emit different groups while still being built from the same scientific information.

This matters directly for the ethanol challenge: a source-backed MQ curve, a deterministic fingerprint generated from the response-curve library, and a QSPR model trained from that same accepted M2 library must not be described as three independent confirmations.

## Canonical lineage classes
`ChemCoreEvidenceLineagePolicy` defines the following evidence kinds:

1. `HR069_DEVICE_CALIBRATION`
2. `HR069_DEVICE_EXPERIMENTAL_FINGERPRINT`
3. `DIRECT_EXTERNAL_REFERENCE`
4. `MODEL_DERIVED_REFERENCE_FINGERPRINT`
5. `QSPR_MODEL_TRANSFER`
6. `EXTERNAL_REFERENCE_FINGERPRINT`
7. `CORROBORATIVE_COPY`
8. `UNKNOWN`

## Upstream dependencies
Two explicit upstream libraries are identified:

- `UPSTREAM:ACCEPTED_REFERENCE_RESPONSE_LIBRARY`
- `UPSTREAM:PUBCHEM_MOLECULAR_DESCRIPTORS`

A model-derived research fingerprint depends on the accepted reference-response library.
A QSPR model-transfer prediction depends on both the accepted reference-response library and PubChem molecular descriptors.

PubChem therefore adds molecular explanatory information but does not make the QSPR response prediction an independent gas measurement.

## Pairwise correlation relations
The policy distinguishes:

- `SAME_IMMEDIATE_EVIDENCE`
- `SAME_DIRECT_SOURCE`
- `COMMON_DERIVED_REFERENCE_LIBRARY`
- `DERIVED_MODEL_VS_REFERENCE_SOURCE`
- `SAME_DEVICE_CAMPAIGN`
- `INDEPENDENT`
- `INDETERMINATE`

Unknown provenance is deliberately not promoted to independence.

## Important decisions
### Direct external references
Two direct literature/manufacturer/research observations can be independent when they have distinct, nonblank source lineages. Multiple channels or copies from the same source are not automatically independent.

### Model-derived reference fingerprints
`SOURCE_BACKED_REFERENCE_FINGERPRINT` generated from `ResearchIdentificationFingerprintRegistry` is explicitly classified as a derivative of the accepted reference-response library, not an independent experiment.

### QSPR
QSPR is classified as model-transfer evidence derived from the accepted M2 response library plus PubChem descriptors. Different MQ QSPR sensor models do not create independent experimental confirmation merely because they are different sensors.

### HR069 experimental evidence
A genuinely independent HR069 experimental fingerprint/calibration campaign can form a device-local lineage distinct from external reference evidence. Two device-local observations sharing the same campaign/source revision are correlated.

### Corroborative sensor patterns
Degraded/corroborative observations retain the immediate independence group of their underlying model; degraded data cannot manufacture a new independent source.

## Stage boundary
Stage 27 maps dependence but does not yet assign the final confidence contribution for each relation. That is the responsibility of Stage 28 — Unified Evidence-Weighting Policy.

The required Stage-28 rule is therefore explicit: confidence fusion must operate on lineage-aware evidence, not merely count distinct `independenceGroup` strings.

## Tests
Tests cover:

- independence of distinct direct reference sources;
- correlation of repeated direct-source evidence;
- common upstream lineage of QSPR and model-derived fingerprints;
- correlation between derived models and direct reference-library evidence;
- potential independence of a separate HR069 empirical campaign from an external reference.

## Status
Stage 27: COMPLETE once the RC2 unit-test workflow confirms the lineage implementation. No evidence weight has been altered in this stage.
