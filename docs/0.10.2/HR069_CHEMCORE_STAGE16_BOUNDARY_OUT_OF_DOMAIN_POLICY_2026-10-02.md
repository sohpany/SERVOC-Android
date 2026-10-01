# HazRob 0.10.2 / HR069 — ChemCore Stage 16 Boundary / Out-of-Domain Policy

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; all registered response models and future Evidence Matrix relations.

## Objective

Ensure a candidate cannot gain apparent support by fitting a response model outside the concentration/environment/heater domain in which that model is valid.

## Existing runtime basis

`SensorOperatingEnvelope` already distinguishes:

- `PRIMARY_TARGET`
- `MANUFACTURER_GRAPH_RESPONSE`
- `COMMUNITY_CROSS_RESPONSE`
- `OUT_OF_RANGE`
- `HEATER_PROTOCOL_REQUIRED`
- `UNKNOWN`

and explicitly states that out-of-range ppm must not be extrapolated and that MQ7/MQ9 require their heater protocol before source-backed use.

Stage 16 extends this into the unified ChemCore decision vocabulary.

## Required domain states

For each model contribution:

- `IN_DOMAIN`
- `EDGE_OF_DOMAIN_LOW`
- `EDGE_OF_DOMAIN_HIGH`
- `OUT_OF_DOMAIN_LOW`
- `OUT_OF_DOMAIN_HIGH`
- `ENVIRONMENT_DOMAIN_UNKNOWN`
- `ENVIRONMENT_OUT_OF_DOMAIN`
- `HEATER_PROTOCOL_INVALID`
- `MODEL_DOMAIN_UNKNOWN`

## Edge definition

A fit is `EDGE_OF_DOMAIN` when the optimized concentration is close enough to a declared lower/upper boundary that the solution is plausibly boundary-seeking rather than internally identified.

The final numeric edge fraction is not frozen in the knowledge stage. It must be selected/validated empirically. Until then, runtime implementation should preserve the continuous distance-to-boundary so future experiments can tune the threshold without rewriting evidence records.

Recommended stored quantity:

`edge_fraction = min((logC-logCmin)/(logCmax-logCmin), (logCmax-logC)/(logCmax-logCmin))`

for log-concentration models.

## Decision effect

- `IN_DOMAIN`: eligible under all other evidence gates.
- `EDGE_OF_DOMAIN_*`: may contribute with an explicit penalty and visible flag; cannot be treated as clean quantitative confirmation merely because residual is low.
- `OUT_OF_DOMAIN_*`: excluded from quantitative fitting and from identity evidence that depends on that numeric relation.
- `HEATER_PROTOCOL_INVALID`: relation unavailable, not merely down-weighted.
- environment failure: relation unavailable if its source defines an environmental domain that is violated.
- unknown environment/domain metadata: permitted only at the evidence tier justified by the source and must carry uncertainty/gap status; never silently promoted.

## Single-channel boundary overfit safeguard

If a one-channel model obtains its best score at or near a boundary, it must not enter the same identity competition as a well-supported multi-channel candidate. State:

`SINGLE_CHANNEL_EDGE_FIT`

This is particularly important for broad-response MQ channels where monotonic inversion can create an apparently exact fit at the allowed endpoint.

## PPM rule

No `OUT_OF_DOMAIN`, extrapolated, or invalid-heater estimate is reportable ppm.

`EDGE_OF_DOMAIN` ppm can be shown only as provisional/reference information with explicit status unless an accepted device-specific M4 calibration explicitly validates that edge region.

## Reporting rule

A candidate rejected by domain rules must remain in the audit trail:

`ANALYTICALLY_REJECTED — OUT_OF_DOMAIN`

or

`ANALYTICALLY_REJECTED — HEATER_PROTOCOL_INVALID`.

It must not silently disappear from a session in which it appeared at SIGNAL/AFFINITY level.

## Catalogue accounting

- Reviewed: 144/144.
- Curve-bearing entries subject to domain policy: 14.
- No-curve entries: 130, policy retained for future relations but currently `NOT_APPLICABLE_WITH_REASON`.
- Silent extrapolation allowed: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 17 defines cross-channel concentration consistency so multiple valid curves for one candidate are tested for convergence on a common concentration rather than counted as independent votes.