# HazRob 0.10.2 / HR069 — ChemCore Stage 22 Temporal-Feature Readiness

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; canonical time-domain feature infrastructure and identity-model readiness.

## Objective

Separate **feature extraction readiness** from **validated temporal identity readiness**. The existence of a 136-feature vector does not by itself prove that those features distinguish any chemical.

## Canonical feature contract

`ChemCoreFeaturePipeline` is the canonical contract:

- schema: `chemcore-features/1`
- 17 features per sensor
- 8 installed MQ sensors -> maximum 136 scalar features when all channels are present
- input: time series of positive `Rs/R0` values
- transform: `log10(Rs/R0)` while preserving response sign

Features per sensor:

1. valid fraction
2. baseline log10 ratio
3. steady log10 ratio
4. signed delta
5. absolute delta
6. minimum
7. maximum
8. peak
9. initial slope
10. stability slope
11. t10
12. t50
13. t90
14. signed AUC
15. absolute AUC
16. baseline noise SD
17. SNR of absolute delta over noise

The older `TemporalFeatureExtractor` is a deprecated compatibility adapter that exposes only 8 of these features. New ChemCore work must use the 17-feature canonical contract.

## Readiness split

### Feature extraction

State: `IMPLEMENTED`.

The canonical pipeline can compute features when a session contains a valid sensor time series. It also records whether real physical timestamps were supplied or sample-index time was used.

### Labeled temporal reference library

State: `NOT_VALIDATED`.

No 144-entry set of independently labeled HR069 temporal fingerprints is currently accepted. The model-derived static fingerprint grid contains no experimental kinetics and cannot be used to synthesize t10/t50/t90, slopes, AUC, baseline noise or SNR.

### Temporal identity classifier

State: `NOT_RELEASE_READY`.

No chemical may receive extra identity confidence solely because temporal features exist in a session until a temporal model has been trained and validated with independent HR069 experiments.

## Data-quality requirements for temporal use

A temporal feature vector used for identity should record:

- physical timestamps;
- exposure start / sampling protocol;
- sufficient pre-exposure baseline;
- sufficient response interval;
- recovery interval when recovery features are later added/used;
- channel validity and missingness;
- heater state;
- general 300 s warm-up completion;
- R0 provenance;
- T/RH/P;
- concentration/source protocol;
- session and day/block identifiers.

Sample-index time may support debugging but is not equivalent to physical seconds for kinetic identity.

## Training/validation requirements

Temporal identity models require:

- independent labeled HR069 sessions;
- session-level train/validation/test splitting;
- no splitting of time samples from one exposure across train/test;
- concentration and environment diversity;
- chemically neighboring negative classes;
- open-set/OOD challenge sessions;
- regularization / feature selection appropriate to sample size;
- documented feature schema version.

## Role in unified analysis

Temporal evidence is a later discriminator after family routing and static evidence. It may:

- resolve ambiguous static fingerprints;
- detect inconsistent transient behavior;
- strengthen an exact-compound candidate when independently validated;
- contribute to mixture/interference detection.

It must not override a hardware incompatibility, invalid heater protocol or clearly out-of-domain source model.

## Catalogue accounting

- Reviewed: 144/144.
- Canonical feature extraction infrastructure: available globally when time-series data exist.
- Chemically validated HR069 temporal identity models: 0/144.
- Static model-derived fingerprints with legitimate kinetic features: 0.
- Current state for chemical identity: `TEMPORAL_VALIDATION_PENDING` across the catalogue, with hardware/object-specific N/A where appropriate.

## Stage result

**COMPLETE — 144/144.**

Stage 23 formalizes the family-first hierarchical classifier and defines how family, exact-compound, specialist, curve, fingerprint, temporal, QSPR and OOD evidence interact without being double-counted.