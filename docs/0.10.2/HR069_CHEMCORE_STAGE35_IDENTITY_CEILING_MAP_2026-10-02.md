# HR069 ChemCore Stage 35 — Flexible Identity Ceiling Map

Status: COMPLETE — 144/144

## Purpose
Record the strongest analytical-development state currently supported by the installed HR069 sensing routes without turning the map into a runtime prohibition.

## Current 144-panel distribution
- `PROVISIONAL_IDENTITY_CAPABLE`: 4
- `CANDIDATE`: 106
- `AFFINITY`: 19
- `UNSUPPORTED`: 15
- Total: 144

The four `PROVISIONAL_IDENTITY_CAPABLE` entries are Ammonia, Hydrogen, Methane and Ethanol because their current hardware suitability is `ADEQUATE_WITH_CONDITIONS`.

There are **five** Stage-7 primary specialist routes overall: CO/MQ7, NH3/MQ135, H2/MQ8, CH4/MQ4 and Ethanol/MQ3. Carbon Monoxide deliberately remains at `CANDIDATE` in this map despite MQ7 being a primary specialist channel, because the present MQ7 heater/selectivity evidence is not yet hardware-complete. Thus **specialist role and identity ceiling are separate dimensions**.

## Important semantics
This is a knowledge-map status, not a final scientific certification and not a runtime veto. Controlled experiments can promote or demote any entry.

`UNSUPPORTED` means the installed MQ array currently lacks a defensible primary selective route. It does not mean the array can never show incidental response.

No entry is labelled M4-quantifiable by Stage 35. M4 remains a later device-specific calibration/validation result.

## Implementation
`ChemCoreIdentityCeilingMap` provides the fixed 144 mapping. `ChemCoreIdentityCeilingMapTest` iterates all 144 entries and verifies the exact distribution and the non-gating policy.
