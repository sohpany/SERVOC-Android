# HR069 ChemCore — Stage 11 Complete Curve Inventory

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Population: catalogue indices 0..143
Status: COMPLETE — 144/144 explicitly reviewed

## Scope and guardrail

This stage inventories registered response relations only. Presence of a curve does not imply HR069 M4 calibration, selective identity, or reportable ppm. Absence of a curve is retained as an explicit knowledge gap rather than filled by interpolation or QSPR.

## Registry census

### GasEngine inherited power-law table
Source file: `GasEngine.java`
Registered relations: **39**
Unique catalogue chemicals: **13**
Per-chemical relation counts:
- 20 Carbon Monoxide: 8
- 21 Ammonia: 1
- 23 Carbon Dioxide: 1
- 34 Benzene: 1
- 75 Hydrogen: 4
- 76 Methane: 5
- 78 Propane: 1
- 84 n-Hexane: 1
- 95 Toluene: 1
- 103 Ethanol: 7
- 110 Acetone: 1
- 135 LPG: 7
- 139 Smoke: 1

### LiteratureCurveRegistry
Source file: `LiteratureCurveRegistry.java`
Registered relations: **9**
Unique catalogue chemicals: **8**
Per-chemical relation counts:
- 20 Carbon Monoxide: 2
- 21 Ammonia: 1
- 23 Carbon Dioxide: 1
- 75 Hydrogen: 1
- 76 Methane: 1
- 95 Toluene: 1
- 103 Ethanol: 1
- 110 Acetone: 1

These are peer-reviewed/reference relations and remain non-M4 until HR069-specific validation.

### LinearLiteratureResponseRegistry
Source file: `LinearLiteratureResponseRegistry.java`
Registered relations: **1**
Unique catalogue chemicals: **1**
- 141 NOx: MQ135 linear relation, 1–150 ppm, source conditions preserved.

### ResearchCurveRegistry
Source file: `ResearchCurveRegistry.java`
Registered relations: **43**
Unique catalogue chemicals: **13**
Per-chemical relation counts:
- 20 Carbon Monoxide: 8
- 21 Ammonia: 1
- 23 Carbon Dioxide: 1
- 34 Benzene: 1
- 75 Hydrogen: 5
- 76 Methane: 7
- 78 Propane: 1
- 84 n-Hexane: 1
- 95 Toluene: 1
- 103 Ethanol: 7
- 110 Acetone: 1
- 135 LPG: 7
- 139 Smoke: 2

Research curves are retained as comparison/research evidence and do not self-promote above higher-provenance relations.

## Unique catalogue coverage across all curve registries

Chemicals with at least one registered relation: **14/144**

Indices:
`20,21,23,34,75,76,78,84,95,103,110,135,139,141`

Chemicals with no registered direct response relation: **130/144**

Indices:
`0-19,22,24-33,35-74,77,79-83,85-94,96-102,104-109,111-134,136-138,140,142-143`

Stage state:
- curve-covered 14 entries: `REVIEWED_ACCEPTED`
- no-curve 130 entries: `REVIEWED_GAP`

## Important overlap / double-counting warning

The 39 GasEngine, 9 literature and 43 research relations are not independent evidence counts. Many describe the same sensor/analyte relation from different source families. Stage 12 will establish provenance and precedence per pair so duplicate descendants cannot be counted as independent confirmation.

## Current conclusions

1. The present curve library is concentrated in a small set of target/legacy chemicals.
2. Broad 144-material coverage cannot be achieved scientifically by curve fitting alone.
3. Family-first inference, fingerprints, temporal features, PubChem/QSPR and experimental validation remain essential for the remaining catalogue.
4. No curve in this inventory is automatically M4/reportable ppm.
