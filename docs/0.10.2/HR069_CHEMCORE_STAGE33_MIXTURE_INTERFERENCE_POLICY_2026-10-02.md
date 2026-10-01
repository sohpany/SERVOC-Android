# HR069 ChemCore Stage 33 — Mixture / Interference Policy

Status: COMPLETE — diagnostic implementation

## Goal
Distinguish a poor pure-compound explanation from a credible single-candidate fit without pretending that the MQ array can already decompose arbitrary mixtures into validated components.

## Existing runtime mechanism
`MixtureResidualGuard` evaluates the ranked pure-compound solutions using:
- standardized residual RMS,
- top relative model-ranking weight,
- separation from the runner-up.

Current research-policy defaults remain:
- maximum standardized residual RMS: 2.50,
- minimum top ranking weight: 0.80,
- minimum top/runner-up margin: 0.25.

These are research-policy thresholds, not calibrated probabilities.

## Result semantics
- `SINGLE_CANDIDATE_CONSISTENT`: the pure hypothesis is internally consistent enough for continued analysis.
- `POSSIBLE_MIXTURE_OR_INTERFERENCE`: the best pure model leaves substantial unexplained array response.
- `AMBIGUOUS_IDENTITY`: several candidates remain insufficiently separated.
- `INSUFFICIENT_EVIDENCE`: no usable pure-compound explanation.

## Early-build rule
The mixture/interference flag is diagnostic. It keeps the candidate list visible and does not claim component identities or component ppm values.

## Future validation
Controlled pure-gas and mixture experiments may later revise these thresholds or justify a true component-decomposition model. No such decomposition is activated in Stage 33.
