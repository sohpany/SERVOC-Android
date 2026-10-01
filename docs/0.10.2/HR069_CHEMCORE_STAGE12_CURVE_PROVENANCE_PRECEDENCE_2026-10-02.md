# HR069 ChemCore — Stage 12 Curve Provenance and Precedence Audit

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Population: 144/144 reviewed
Dependency: Stage 11 curve inventory

## Objective

Prevent duplicate or lower-authority response relations from being counted as independent evidence, while retaining conflicting/lower-tier curves for audit and research comparison.

## Source families

1. `PEER_REVIEWED_EXACT_MODEL` — `LiteratureCurveRegistry` and `LinearLiteratureResponseRegistry` inside their declared sensor/analyte/environment/concentration domain.
2. `MANUFACTURER_ROLE_EVIDENCE` — `ManufacturerEvidenceMatrix` TARGET or GRAPH_RESPONSE. This establishes sensor/analyte role and source domain, but is not automatically a numerical curve when no equation is registered.
3. `INHERITED_GASENGINE_CURVE` — legacy numerical relation in `GasEngine`; usable only subject to manufacturer-role eligibility, operating envelope and later HR069 transfer audit.
4. `RESEARCH_COMPARISON_CURVE` — `ResearchCurveRegistry`; retained as fallback/comparison evidence and never allowed to silently override a higher-authority accepted relation on the same sensor/analyte pair.

## Numerical precedence for the same sensor/analyte pair

`PEER_REVIEWED_EXACT_MODEL > eligible INHERITED_GASENGINE_CURVE > RESEARCH_COMPARISON_CURVE`

`LinearLiteratureResponseRegistry` is co-equal with peer-reviewed exact-model evidence but is selected only for the relation form it represents; it is not converted into a power law merely for software convenience.

Manufacturer TARGET/GRAPH evidence is orthogonal role metadata. TARGET increases the legitimacy of using an otherwise eligible relation; GRAPH_RESPONSE denotes cross-response and does not make the pair selective.

## Independence rule

Multiple curve records for the same sensor/analyte relation are one evidence lineage unless an independent HR069 experiment establishes separate empirical evidence. Lower-tier duplicates remain visible in audit output but contribute zero additional independent-confirmation count.

Example: MQ135/Ethanol in GasEngine + LiteratureCurveRegistry + ResearchCurveRegistry is not three independent confirmations. It is one sensor/analyte lineage with multiple competing/reference relations and one selected numerical authority according to precedence/domain.

## Domain rule

Precedence applies only when the higher-priority relation is eligible and the observation is inside its declared domain. A higher-tier relation that is unavailable because of concentration domain, environment requirement, heater protocol or missing required metadata does not authorize extrapolation. The next eligible source may be consulted, with the fallback recorded explicitly.

## Heater rule

For MQ7/MQ9, manufacturer-derived or literature relations remain unavailable when the required heater-cycle state is invalid/unknown. The agreed general 300 s session warm-up is separate and does not satisfy this condition by itself.

## Current 144-entry audit state

Curve-bearing catalogue entries: `20,21,23,34,75,76,78,84,95,103,110,135,139,141` — `REVIEWED_ACCEPTED` for provenance/precedence policy application.

No-curve catalogue entries: `0-19,22,24-33,35-74,77,79-83,85-94,96-102,104-109,111-134,136-138,140,142-143` — `NOT_APPLICABLE_WITH_REASON` for curve precedence because no registered direct response relation exists.

Coverage: 144/144.

## Pair-level examples of precedence

- MQ8/H2: peer-reviewed literature relation outranks duplicate inherited/research relations when in-domain.
- MQ7/CO: peer-reviewed literature relation outranks duplicate inherited/research relations, but only with valid heater protocol.
- MQ4/CH4: peer-reviewed literature relation outranks inherited/research duplicates when in-domain.
- MQ135 Acetone/Toluene/Ethanol/CO2/NH3/CO: peer-reviewed literature relations outrank duplicate lower-tier numerical relations on the same pair when in-domain.
- MQ135/NOx: linear peer-reviewed relation is retained in native linear form and is not replaced by a generic power-law surrogate.

## Guardrails

- Source precedence never promotes a relation to M4.
- R2 is not treated as uncertainty.
- A fallback source must be recorded as fallback, not silently substituted.
- Conflicting source relations remain stored for later Stage 15 HR069 transfer analysis.
- Cross-response graph evidence can support/contradict a hypothesis but cannot independently establish identity.

Stage 12 result: COMPLETE.
