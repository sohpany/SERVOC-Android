# HR069 ChemCore Stage 34 — Source Material / Headspace Semantics

Status: COMPLETE — 144/144 object semantics mapped

## Fixed-panel distribution
- 135 `PURE_ANALYTE`
- 6 `MIXTURE`: LPG, CNG, Natural Gas, Town Gas, Smoke, R410A
- 3 `AGGREGATE_METRIC`: TVOC, NOx, LEL
- 0 current `SOURCE_MATERIAL_HEADSPACE` entries in the fixed 144 panel

The `SOURCE_MATERIAL_HEADSPACE` type remains supported for future entries such as Formalin, where the source material is not itself the single vapor analyte.

## Runtime rule
`ChemCoreAnalyticalObjectSemantics` maps the fixed catalog indices and prevents mixtures/aggregate metrics from inheriting pure-molecule identity or one-compound ppm semantics.

## Interpretation examples
- LPG: composition-dependent mixture class; do not equate it automatically with pure propane.
- Smoke: operational mixture/source pattern; do not assign one pure-molecule ppm.
- TVOC: aggregate metric, not a molecular species.
- NOx: aggregate operational class, distinct from pure NO or pure NO2 entries.
- LEL: flammability index/metric, not a chemical identity.
- R410A: refrigerant mixture, not one pure molecular species.

## Early-build flexibility
These classes remain visible in screening and family results. Stage 34 changes semantics, not visibility.
