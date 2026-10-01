# HR069 ChemCore Stage 32 — Exploratory Open-Set/OOD Routing

Status: COMPLETE — early-build policy

## Goal
Preserve open-set/OOD information without hiding useful exploratory candidates during the current knowledge-building phase.

## Policy
1. If no candidate is solvable, the result may remain `UNKNOWN` / insufficient evidence.
2. If at least one candidate is solvable and the open-set gate rejects the session, keep the ranked candidate visible as `SCREENING` and append `OOD_LIKE` diagnostic context.
3. OOD rejection blocks promotion to a stronger named identity through the accepted fingerprint path, but it does not erase the candidate shortlist.
4. A candidate using a response model outside its declared domain is retained for exploratory review as `SCREENING` with an `OUT_OF_DOMAIN` diagnostic; validated/reportable ppm remains withheld.
5. `QUANTIFIED` remains restricted to M4 device/reference-calibrated evidence.

## Rationale
The project is still in the knowledge-building and experimental phase. OOD/domain checks are therefore diagnostic controls rather than broad result-suppression mechanisms. Controlled HR069 validation will later determine whether stronger release-time gates are justified.

## Runtime change
`ChemCoreInferenceEngine` now retains solvable OOD/out-of-domain candidates under `SCREENING` instead of converting them automatically to `UNKNOWN`/analytical rejection.

## Scientific boundary
This flexibility does not turn relative model ranking weights into probabilities and does not make exploratory ppm reportable.
