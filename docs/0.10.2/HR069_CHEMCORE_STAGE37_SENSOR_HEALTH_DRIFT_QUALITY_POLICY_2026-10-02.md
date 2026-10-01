# HR069 ChemCore Stage 37 — Sensor Health, Drift and Data-Quality Policy

Status: COMPLETE — early-build policy

## Existing health layer reused
The existing `SensorHealthEngine` already monitors the installed HR069 MQ channels for:
- missing data,
- invalid ADC/Rs data,
- saturation,
- stuck response,
- unstable/noisy response,
- clean-air baseline drift,
- insufficient samples / unknown state.

Clean-air drift is evaluated only against an approved baseline, not inferred from arbitrary gas exposure.

## Early-build policy
Hard invalidation is deliberately narrow:
- no usable MQ channel remains, or
- the measurement itself contains no valid MQ ratio data.

The following normally remain diagnostics / quality weights during exploratory development:
- individual missing channel,
- unstable/noisy channel,
- drift warning,
- saturation/stuck warning where other channels remain usable,
- missing environmental context,
- incomplete general warm-up,
- heater-specific evidence not eligible for a particular source model.

Source/model-specific code may still decline to use an ineligible channel for a particular curve. That does not automatically suppress the whole session.

## Warm-up
General operational stabilization remains 300 s. MQ7/MQ9 heater-cycle requirements remain separate when their manufacturer/source evidence is used.

## Implementation
`ChemCoreEarlyBuildQualityPolicy` returns `USABLE`, `USABLE_WITH_WARNINGS`, or `INVALID`. Its tests confirm that health warnings remain analyzable while sessions with no usable channel are invalid.
