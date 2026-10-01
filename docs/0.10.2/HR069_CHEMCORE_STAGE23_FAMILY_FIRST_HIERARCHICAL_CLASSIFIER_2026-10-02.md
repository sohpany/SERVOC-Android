# HazRob 0.10.2 / HR069 — ChemCore Stage 23 Family-First Hierarchical Classifier

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: unified classifier design for all 144 catalogue entries.

## Objective

Replace flat 144-way candidate competition with a staged analytical router that first determines whether the observation is compatible with one or more response families, then compares exact compounds only inside scientifically plausible branches.

## Canonical decision graph

`SESSION QUALITY`
-> `SIGNAL`
-> `FAMILY ROUTING`
-> `FAMILY EVIDENCE GATE`
-> `EXACT-COMPOUND CANDIDATE SET`
-> `CURVE / COMMON-C TEST`
-> `EXACT FINGERPRINT`
-> `TEMPORAL + QSPR SUPPORT`
-> `SPECIALIST CONFLICT / CORRELATION`
-> `OOD / UNKNOWN CHECK`
-> `MIXTURE / INTERFERENCE CHECK`
-> `IDENTITY STATE`
-> `M4 QUANTITATION GATE`

This graph is hierarchical but not irreversible: an exact-candidate conflict may return the inference to a broader family or `UNKNOWN` state rather than forcing a winner.

## Layer 0 — Session/data-quality gate

Before chemistry inference, validate:

- general 300 s warm-up completed;
- sensor/channel validity;
- R0 availability/provenance;
- power/ADC validity;
- heater-cycle state for MQ7/MQ9 where relevant;
- saturation/missing channels;
- time-series timestamp quality for temporal analysis;
- environment fields when required by the evidence model.

A failed quality gate may still permit raw SIGNAL display but limits higher analytical states.

## Layer 1 — SIGNAL

A SIGNAL is a measurable response change or channel-level affinity. It does not assert a named gas.

Output examples:

- reducing-gas-like response
- alcohol-sensitive channel response
- broad VOC response
- CO-target channel response

Live named hints, if displayed, must be labeled provisional and retain their later analytical disposition.

## Layer 2 — FAMILY ROUTING

Use:

- Stage 3 response-family taxonomy;
- sensor/analyte roles from Stage 6;
- specialist/anchor channels from Stage 7;
- broad-array shape;
- provisional family fingerprints from Stage 19;
- object/hardware compatibility.

Return one or more plausible families rather than a mandatory single family. Family scores are compatibility scores, not probabilities.

If current hardware is unsuitable for a family/analyte, route to `HARDWARE_UNSUPPORTED_OR_COMPATIBILITY_REVIEW` rather than manufacturing a family match.

## Layer 3 — FAMILY EVIDENCE GATE

Possible states:

- `FAMILY_SUPPORTED`
- `MULTIPLE_FAMILIES_PLAUSIBLE`
- `FAMILY_WEAK`
- `UNKNOWN_FAMILY`
- `HARDWARE_UNSUPPORTED_OR_COMPATIBILITY_REVIEW`

Only supported/plausible branches feed exact-compound ranking.

## Layer 4 — Candidate-set construction

Candidates are drawn from:

- catalogue members of plausible response/structural families;
- specialist/anchor hypotheses supported by valid target channels;
- exact compounds with accepted fingerprint/curve evidence relevant to the observation;
- mixture/operational objects when the pattern indicates a composite source.

No candidate enters merely because it exists in PubChem/QSPR.

## Layer 5 — Candidate evidence bundle

For each candidate `j`, construct a structured bundle rather than a single flat score:

- `anchor_evidence`
- `secondary_target_evidence`
- `corroborative_array_evidence`
- `cross_response_evidence`
- `curve_common_c_state`
- `exact_fingerprint_state`
- `temporal_state`
- `qspr_state`
- `domain_state`
- `transfer_state`
- `object_type`
- `negative/contradictory_evidence`
- `evidence_lineage_ids`

Stage 27 later provides explicit independence/correlation groups before final fusion.

## Layer 6 — Curve / common-C logic

- Multi-channel numeric candidates: test a common concentration using Stage 17.
- Single-channel numeric candidates: remain `SINGLE_SENSOR_SCREENING` unless independent specialist/fingerprint/temporal evidence supports promotion.
- Edge fits and OOD models obey Stage 16.
- Duplicate curve lineages never count as independent confirmations.

## Layer 7 — Fingerprint logic

Use family fingerprint first for routing, then exact-compound fingerprint for within-family discrimination.

Evidence precedence is not “fingerprint always wins”; a fingerprint is evaluated jointly with:

- source/HR069 transfer status;
- sample count;
- model-derived vs empirical provenance;
- contributor count;
- distance and separation;
- future covariance/open-set validation.

## Layer 8 — Temporal and QSPR support

Temporal features may strengthen or contradict a candidate only after a validated temporal model exists.

QSPR provides molecular/model-transfer plausibility and can help especially where direct sensor models are absent, but:

- it is not reportable ppm;
- it cannot bypass hardware incompatibility;
- it cannot be double-counted as independent evidence when trained from the same response curves used elsewhere.

## Layer 9 — Specialist conflict and independence

When an anchor/specialist channel conflicts with a broad-array hypothesis, do not silently choose the side with more physical sensors.

Route to a conflict state for Stage 29 resolution. Correlated MQ channels may represent fewer effective independent evidence groups than their sensor count.

## Layer 10 — Open-set/OOD

Before exact identity promotion, ask whether the observation is sufficiently compatible with any known family/compound model.

If not:

- `UNKNOWN_FAMILY`, or
- `KNOWN_FAMILY_UNKNOWN_COMPOUND`, or
- `OUT_OF_DISTRIBUTION`

is preferred over a forced known-class winner.

## Layer 11 — Mixture/interference

If no single compound explains the eligible evidence and structured residuals remain, test:

- `PRIMARY_ANALYTE_WITH_INTERFERENCE`
- `MIXTURE_SUSPECTED`
- operational source classes

without claiming deconvolution unless a validated mixture model exists.

## Layer 12 — Identity-state output

Unified progressive states:

- `SIGNAL`
- `AFFINITY`
- `CANDIDATE`
- `IDENTIFIED`
- `QUANTIFIED`

with parallel terminal/diagnostic states such as:

- `INSUFFICIENT_EVIDENCE`
- `ANALYTICALLY_REJECTED`
- `UNKNOWN`
- `OOD`
- `MIXTURE_SUSPECTED`
- `HARDWARE_UNSUPPORTED`

A candidate may move down as contradictory evidence arrives; progression is not one-way.

## Layer 13 — M4 PPM gate

PPM is reportable only from an HR069 device-specific M4-valid quantitative path for the identified analyte/operational metric and current domain. Identity and quantitation remain separate gates.

Provisional/reference ppm may be retained internally or displayed with explicit non-reportable status where scientifically useful, but must never be presented as validated exposure concentration.

## Special handling examples

### Alcohol-like observation

`SIGNAL -> ALCOHOL_VOC -> MQ3/array corroboration -> alcohol-family curves -> exact alcohol candidates -> exact fingerprint -> temporal/QSPR -> OOD -> identity -> analyte-specific M4 ppm`

### n-Heptane-like observation

No dedicated anchor: `COMBUSTIBLE_HYDROCARBON -> aliphatic subgroup -> broad-array/fingerprint/temporal/QSPR -> Hexane/Heptane/Octane competition -> OOD -> candidate/identity ceiling`.

### NO2 / SO2 / H2O2-like observation with current hardware

Hardware compatibility gate may terminate at `HARDWARE_UNSUPPORTED_OR_COMPATIBILITY_REVIEW`; QSPR or unrelated MQ cross-response cannot manufacture selective identity.

### NH3-like observation

MQ135 target evidence may create a strong specialist branch, but single-channel curve fit remains distinct from multi-channel confirmation and PPM still requires M4.

## Catalogue accounting

- Reviewed: 144/144 through the frozen catalogue and Stages 2–10 routing metadata.
- Every catalogue entry has an analytical route into the hierarchy or an explicit unsupported/mixture route.
- Flat mandatory 144-way winner: prohibited.
- Unknown/OOD terminal option: required.
- Family and exact-compound evidence: explicitly separated.

## Stage result

**COMPLETE — 144/144.**

Stage 24 audits PubChem molecular-record coverage and determines which catalogue entries can legitimately participate in molecular/QSPR support.