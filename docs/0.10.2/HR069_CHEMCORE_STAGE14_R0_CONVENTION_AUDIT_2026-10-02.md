# HazRob 0.10.2 / HR069 — ChemCore Stage 14 R0-Convention Audit

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; all current curve families that use `Rs/R0`.

## Objective

Prevent a numerically available `R0` from being treated as if it were automatically compatible with every external/manufacturer/research response curve.

## HR069 device-side R0 state

The active Android calibration path uses a device-specific normalization baseline:

- manual developer-entered R0 when present; otherwise
- accepted field clean-air fallback from `FieldR0Baseline`.

`ManualR0Calibration.apply()` explicitly records:

- `r0_calibrated=true` when a positive device R0 exists;
- `r0_source`, session, UTC and provenance metadata;
- `r0_reference_validated=false`.

Therefore **device normalization is not reference-gas validation**.

### Current field baseline

`FieldR0Baseline` is based on three independent clean-air sessions on 2026-09-13:

- 216 samples total;
- 30 s time series per session;
- geometric aggregation in log space;
- no T/RH/P compensation claimed;
- no reference-gas validation claimed.

This baseline is useful for repeatable HR069 fingerprints and relative response tracking, but it cannot by itself certify absolute ppm transfer from an external curve.

## Source-side R0 problem

The present registries store equations in `Rs/R0` form but do not yet store a normalized structured `R0 definition contract` for every source relation. Depending on the source, `R0` may represent a resistance measured under a specified calibration gas concentration, clean air, a source-specific graph normalization, or another datasheet convention.

Until the exact source convention is structured and shown compatible with HR069, the relation is treated as a **reference prior**, not a device-specific quantitative calibration.

## Mandatory R0 metadata for each curve relation

Future Evidence Matrix records must support:

- `r0_definition_type`
- `r0_reference_gas`
- `r0_reference_concentration_ppm`
- `r0_reference_temperature_c`
- `r0_reference_rh_pct`
- `r0_reference_o2_pct`
- `r0_heater_protocol`
- `r0_load_resistance_context`
- `r0_source_text`
- `r0_hr069_compatibility_state`

Allowed compatibility states:

- `SOURCE_R0_COMPATIBLE_VALIDATED`
- `SOURCE_R0_TRANSFER_REQUIRED`
- `SOURCE_R0_DEFINITION_INCOMPLETE`
- `SOURCE_R0_INCOMPATIBLE`
- `NOT_APPLICABLE_NO_CURVE`

## Audit result for current curve-bearing catalogue entries

Current state is deliberately conservative:

| Catalogue condition | R0 audit state | Quantitative implication |
|---|---|---|
| 14 entries with one or more registered numeric relations | `SOURCE_R0_TRANSFER_REQUIRED` and/or `SOURCE_R0_DEFINITION_INCOMPLETE` | May support M1/M2 comparison inside other domain gates; cannot create reportable M4 ppm by source alone |
| 130 entries with no numeric curve | `NOT_APPLICABLE_NO_CURVE` | No curve/R0 transfer claim exists |
| HR069 field/manual R0 values | `DEVICE_NORMALIZATION_AVAILABLE` | Supports repeatable Rs/R0 fingerprints; not proof of external curve transfer |

No current external relation is promoted by this audit to `SOURCE_R0_COMPATIBLE_VALIDATED` merely because it is a manufacturer or peer-reviewed relation.

## Analytical rule

For a source model `f_source(C)` and HR069 observation `Rs/R0_HR069`, exact transfer is allowed only when the R0 conventions and operating context have been shown compatible. Otherwise use an explicitly validated transfer model such as:

`log10(Rs/R0_source) = alpha + beta * log10(Rs/R0_HR069)`

or a full HR069-specific calibration relation.

`alpha=0, beta=1` must never be assumed solely because both variables are named `Rs/R0`.

## Catalogue accounting

- Reviewed: 144/144.
- Curve-bearing entries requiring R0 transfer/definition audit: 14.
- No-curve entries: 130, `NOT_APPLICABLE_WITH_REASON`.
- External relations currently certified as HR069 R0-compatible M4: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 15 assigns HR069 transfer state for each curve-bearing material based on this R0 audit, provenance, heater validity, domain status and existing experimental evidence.