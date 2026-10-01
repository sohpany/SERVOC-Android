# HR069 ChemCore Stage 36 — Experimental Campaign Map

Status: COMPLETE — 144/144

## Purpose
Assign every fixed-panel entry to a practical next-experiment class while keeping the current build exploratory.

## Distribution
- `TARGET_ANCHOR`: 4
- `BROAD_ARRAY_PURE`: 97
- `MIXTURE_OR_AGGREGATE`: 9
- `COMPATIBILITY_REVIEW`: 19
- `HARDWARE_GAP`: 15
- Total: 144

## Early-build experiment guidance
### TARGET_ANCHOR
Collect clean-air baseline, target-sensor response, supporting-array response and recovery across several concentrations.

### BROAD_ARRAY_PURE
Collect static multi-sensor fingerprints, time-series features and relevant interferents across representative concentrations.

### MIXTURE_OR_AGGREGATE
Collect source-pattern/headspace signatures plus composition/context metadata. Do not force one pure-analyte ppm.

### COMPATIBILITY_REVIEW
First establish safe material/sensor compatibility and whether the current array has a useful reproducible response.

### HARDWARE_GAP
Record incidental array behavior if useful, but prioritize an appropriate additional sensing route before identity/ppm validation.

## Formal validation remains later
The existing experimental-validation protocol remains the promotion standard for M3/M4 when the project reaches that stage (multiple concentration levels, independent sessions/days, environmental recording, held-out validation, open-set and mixture testing). Stage 36 does not require completing that formal campaign before exploratory development continues.

## Implementation
`ChemCoreExperimentalCampaignMap` and its unit test cover all 144 catalog indices deterministically.
