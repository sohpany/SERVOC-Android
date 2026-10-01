# HR069 ChemCore — Stage 10 Hardware Gap Register

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Population: canonical catalogue indices 0..143 (144/144 reviewed)
Dependency: Stage 9 hardware-suitability map

## Rule

This register describes physical/selectivity gaps only. A software score, longer warm-up, curve fit, fingerprint or QSPR model cannot create a sensing capability that the installed hardware does not physically support. The agreed global session warm-up is 300 s; MQ7/MQ9 heater-cycle validity remains a separate requirement.

## Gap classes and explicit catalogue coverage

### NO_PRIMARY_HARDWARE_GAP — LOW
Indices: `21,75,76,103`
Count: 4
Resolution class: `VALIDATE_EXISTING_ARRAY`
Meaning: a primary installed MQ path exists; remaining work is calibration, transfer, selectivity validation, uncertainty and release qualification.

### HEATER_PROTOCOL_AND_SELECTIVITY_GAP — HIGH
Indices: `20`
Count: 1
Resolution class: `IMPLEMENT_OR_VERIFY_MQ7_MQ9_CYCLE_AND_VALIDATE_CO_ROUTE`
Meaning: CO target hardware exists, but heater-cycle validity and selectivity must be explicit before target evidence is considered available.

### ANCHOR_SELECTIVITY_GAP — MEDIUM
Indices: `78,95`
Count: 2
Resolution class: `VALIDATE_CURRENT_TARGET_RELATION_OR_ADD_ORTHOGONAL_SENSOR`
Meaning: manufacturer target evidence exists but the current role matrix does not provide a strong primary anchor.

### SELECTIVITY_GAP — MEDIUM
Indices: `1-9,23,25-41,48,50-59,62-74,77,79-94,96-102,104-122`
Count: 94
Resolution class: `VALIDATE_ARRAY_FINGERPRINT_OR_ADD_ORTHOGONAL_SENSOR`
Meaning: broad-array response may support family/candidate inference; exact identity requires validated fingerprints/temporal evidence or an orthogonal sensing technology.

### COMPATIBILITY_AND_DEDICATED_SENSING_GAP — HIGH
Indices: `0,10-19,42-47,60-61`
Count: 19
Resolution class: `COMPATIBILITY_REVIEW_PLUS_APPROPRIATE_DEDICATED_OR_ORTHOGONAL_SENSOR`
Meaning: do not experimentally expose the MQ array merely to fill this gap. Establish material compatibility and an appropriate sensing route first.

### PRIMARY_SENSING_TECHNOLOGY_GAP — HIGH
Indices: `22,24,49,123-134`
Count: 15
Resolution class: `ALTERNATIVE_OR_DEDICATED_SENSING_TECHNOLOGY_REQUIRED`
Meaning: the installed MQ array lacks a defensible primary sensing route; software weighting and warm-up cannot substitute for missing physical capability.

### MIXTURE_COMPONENT_RESOLUTION_GAP — MEDIUM
Indices: `135-143`
Count: 9
Resolution class: `VALIDATE_MIXTURE_FINGERPRINT_AND_COMPONENT_SEMANTICS`
Meaning: the array may characterize aggregate/mixture patterns; component-specific identity and quantitation require separate validation.

## Coverage validation

The seven sets above are mutually exclusive and cover all catalogue indices 0..143 exactly once.

Counts: `4 + 1 + 2 + 94 + 19 + 15 + 9 = 144`.

Stage state for all covered indices: `REVIEWED_ACCEPTED`.

## Interpretation guardrail

`NO_PRIMARY_HARDWARE_GAP` does not mean M3/M4 is already validated. It only means no additional primary sensing technology is currently required before analytical validation can proceed. `SELECTIVITY_GAP` and `PARTIAL` hardware status explicitly permit useful screening/family inference while withholding unsupported exact identity claims.
