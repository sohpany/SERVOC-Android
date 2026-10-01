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

The four provisional-identity-capable entries are the current primary-anchor routes: Ammonia, Hydrogen, Methane and Ethanol.

## Important semantics
This is a knowledge-map status, not a final scientific certification and not a runtime veto. Controlled experiments can promote or demote any entry.

`UNSUPPORTED` means the installed MQ array currently lacks a defensible primary selective route. It does not mean the array can never show incidental response.

No entry is labelled M4-quantifiable by Stage 35. M4 remains a later device-specific calibration/validation result.

## Implementation
`ChemCoreIdentityCeilingMap` provides the fixed 144 mapping. `ChemCoreIdentityCeilingMapTest` iterates all 144 entries and verifies the exact distribution and the non-gating policy.
