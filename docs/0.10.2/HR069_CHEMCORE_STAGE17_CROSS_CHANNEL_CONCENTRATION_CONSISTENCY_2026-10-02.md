# HazRob 0.10.2 / HR069 — ChemCore Stage 17 Cross-Channel Concentration-Consistency Model

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; common-concentration fitting for current curve-bearing candidates.

## Objective

Replace the weak question “how many sensors can be inverted for this candidate?” with the stronger question:

> Do the eligible, in-domain sensor models for this candidate agree on one physically coherent concentration?

This prevents a candidate from winning merely because several correlated MQ channels respond or because a single monotonic curve can be inverted exactly.

## Per-channel quantities

For sensor `i`, candidate `j` and observed ratio `r_i`:

`y_i = log10(r_i)`

For a response model predicting ratio from concentration:

`m_ij(C) = log10(ratio_predicted_ij(C))`

Standardized residual:

`z_ij(C) = (y_i - m_ij(C)) / sigma_ij`

where `sigma_ij` is an evidence/validation uncertainty proxy until HR069 empirical residual variance is available.

## Common-C objective

For eligible contributors at concentration `C`:

`L_j(C) = sum_i(w_ij * z_ij(C)^2) / sum_i(w_ij)`

and

`C*_j = argmin_C L_j(C)`

subject to:

- the relation is in its own source concentration domain;
- heater/environment requirements are valid;
- source precedence has already removed duplicate models for the same sensor/analyte evidence lane;
- at least two eligible channels are present for a multi-channel identity fit;
- later Stage 27 correlation groups may reduce effective independence even when physical sensor count is >2.

No fixed score threshold is frozen at this knowledge stage; experimental validation will tune thresholds.

## Independent inversion consistency

For diagnostics, each valid channel may also produce its own inverse estimate `C_i*`. Store:

- `log10(C_i*)`
- weighted median log concentration
- median absolute deviation / robust spread
- min/max concentration span
- number of eligible contributors
- number of effective independence groups when Stage 27 becomes available

A low residual on one channel is not equivalent to cross-channel consistency.

## Current curve topology

### Multi-channel numeric candidates

Current registries provide >=2 distinct sensor channels for:

- 20 Carbon Monoxide — up to 8 sensor channels across current registries
- 75 Hydrogen — 5 sensor channels across current registries
- 76 Methane — 7 sensor channels across current registries
- 103 Ethanol — 7 sensor channels across current registries
- 135 LPG — 7 sensor channels; mixture/operational class, not universal pure-gas ppm
- 139 Smoke — 2 sensor channels; composite operational class, not universal pure-gas ppm

These six candidates are eligible for cross-channel common-C/operational-consistency analysis, subject to source precedence and domain gates.

### Single-channel numeric candidates

Current registries provide only one distinct numeric sensor channel for:

- 21 Ammonia — MQ135
- 23 Carbon Dioxide — MQ135
- 34 Benzene — MQ3
- 78 Propane — MQ2
- 84 n-Hexane — MQ3
- 95 Toluene — MQ135
- 110 Acetone — MQ135
- 141 NOx — MQ135

These are **not** allowed to simulate multi-channel confirmation. They may remain useful as specialist/target, affinity, family, or reference evidence according to their sensor role and source tier.

## Single-channel rule

Single-channel candidates enter a separate lane:

`SINGLE_SENSOR_SCREENING`

unless later evidence supplies either:

- an accepted specialist/anchor pathway with its own validation ceiling, or
- an independent fingerprint/temporal/dedicated-sensor evidence stream.

An exact inverse match on one monotonic curve does not by itself promote identity.

## Mixture/aggregate rule

For LPG and Smoke, common-C should be interpreted as **operational response consistency**, not a universal molecular concentration. Composition changes can shift response. They therefore cannot become pure-analyte M4 solely through this common-C solver.

## Conflict outputs

The model must preserve:

- `COMMON_C_CONSISTENT`
- `COMMON_C_WEAK`
- `CROSS_CHANNEL_INCONSISTENT`
- `SINGLE_SENSOR_SCREENING`
- `INSUFFICIENT_INDEPENDENT_CHANNELS`
- `DOMAIN_FRAGMENTED`

`CROSS_CHANNEL_INCONSISTENT` is negative analytical evidence and must not be discarded.

## Relationship to the ethanol blind challenge

The methodology is deliberately not tuned to force ethanol to win that specific challenge. The challenge demonstrated why convergence across channels and source-compatibility are necessary: several H2 channels produced similar inverse concentrations while several ethanol source models disagreed strongly. Future HR069 calibration may change those relations, but the common-C logic itself remains analyte-neutral.

## Catalogue accounting

- Reviewed: 144/144.
- Multi-channel numeric candidates: 6.
- Single-channel numeric candidates: 8.
- No numeric curve: 130.
- Candidate identity promoted solely from one exact inverse fit: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 18 inventories all available fingerprints and distinguishes exact-compound, family, class, external-transfer, deterministic/model-derived and HR069 empirical fingerprints before hierarchical identity logic is designed.