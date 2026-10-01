# HR069 ChemCore Stage 38 — End-to-End Reporting, Regression and Release Trace

Status: IMPLEMENTED — pending final CI qualification of the latest head

## Goal
Expose one auditable analytical trace without adding a second classifier.

## Trace contents
`ChemCoreAnalyticalTrace` composes existing artifacts into one JSON object containing:
- development mode (`EARLY_BUILD_EXPLORATORY`),
- analysis path,
- progressive result state,
- full ChemCore result envelope,
- rejected-evidence audit trail,
- object semantics (pure analyte / mixture / aggregate),
- flexible identity-ceiling status,
- next experimental campaign and guidance.

## Analysis path
`QUALITY -> SIGNAL -> FAMILY -> SENSOR_ROLES -> CURVES_COMMON_C -> FINGERPRINT -> TEMPORAL_QSPR -> OOD_DIAGNOSTIC -> INTERFERENCE_MIXTURE -> IDENTITY -> PPM`

## Regression coverage
Existing and newly added unit tests collectively cover:
- anchor/specialist conflict,
- broad-array screening,
- unsupported hardware semantics,
- mixture/aggregate semantics,
- heater-dependent evidence handling,
- out-of-domain/OOD diagnostics,
- rejected live/analytical hypotheses,
- M4-gated quantitative semantics,
- 144/144 identity-ceiling coverage,
- 144/144 experimental-campaign coverage,
- sensor-health warning vs invalid-session behavior.

`ChemCoreAnalyticalTraceTest` adds direct checks for Ethanol screening, LPG mixture semantics and unsupported Oxygen audit retention.

## Early-build release policy
The current build remains exploratory. Diagnostic warnings do not generally hide candidates. `QUANTIFIED` / reportable occupational ppm remains reserved for M4 evidence.

## Remaining external scientific work
Software completion of the 38-stage framework does not mean the chemistry has been fully validated. Controlled HR069 experiments remain the mechanism for revising curves, fingerprints, thresholds, transfer functions and identity ceilings.
