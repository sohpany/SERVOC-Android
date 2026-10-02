# HR069 ChemCore — Early-Build Flexibility Addendum

Status: ACTIVE DEVELOPMENT POLICY

This addendum applies to the Unified Analytical Methodology while HazRob remains in the knowledge-building / exploratory experimental phase.

## Principle
The purpose of current analysis is to expose useful sensor behavior and candidate structure so that experiments can improve the knowledge base. Diagnostic gates should therefore explain uncertainty rather than unnecessarily hide information.

## Default early-build behavior
- OOD rejection: retain solvable ranked candidates as `SCREENING` with `OOD_LIKE` diagnostic context.
- Out-of-domain relation: retain the exploratory candidate, flag the affected relation, and withhold validated/reportable ppm.
- Specialist conflict: preserve both the leading candidate and conflicting specialist evidence; do not silently force a winner.
- Drift/noise/missing individual channels: preserve the session where usable channels remain and propagate quality warnings/weights.
- Mixture/interference: preserve candidate list and state that the pure-compound explanation is incomplete; do not invent mixture composition.
- Unsupported hardware: allow incidental response to be studied but do not represent it as selective detection.

## Hard boundaries retained
- A session with no usable MQ measurement remains invalid.
- Relative ranking weights are not chemical-presence probabilities.
- Experimental/screening concentration is not reportable occupational ppm.
- `QUANTIFIED` / reportable ppm remains restricted to M4 device-specific validation.

## Later release mode
Controlled HR069 experiments may justify stricter thresholds or hard gates. Such changes must be versioned and regression-tested rather than silently replacing this development policy.
