# HazRob 0.10.2 / HR069 — ChemCore Analytical Remediation Plan

Date: 2026-10-01
Branch: `release/hazrob-0.10.2-rc2`
Baseline commit: `7342941d1680d6bfdbb404c78c3b93332e8c38ef`
Scope: ChemCore analytical processing, candidate ranking, uncertainty, fingerprints, QSPR, open-set/OOD, environmental correction and validation semantics.
Status: ACTIVE — resumable implementation ledger.

## Scientific operating rule

No remediation step may promote external/reference evidence to M4, manufacture missing MQ channels, convert ranking weights into calibrated probabilities, or produce reportable ppm without HR069 device-specific validation. Every implementation step requires: (1) documented scientific rationale, (2) regression test or explicit qualification requirement, (3) commit SHA recorded below, and (4) fail-closed behavior when the evidence needed by that step is unavailable.

## Priority sequence

| Phase | Objective | Primary risk addressed | Status | Commit / evidence |
|---|---|---|---|---|
| 0 | Freeze baseline and create resumable remediation ledger | Loss of analytical provenance / interrupted work | COMPLETE | baseline `7342941d...`; ledger `d1e25eca...` |
| 1 | Correct inverse-solver ranking comparability and ranking semantics | Candidate-count bias; `posterior` misinterpretation | IMPLEMENTED / CI PENDING | solver `106b62ec...`; regression test `a9473c46...`; Actions runs `36920346181`, `36920346150` pending |
| 2 | Calibrate/declare model uncertainty (`sigma`) by evidence source and domain | Heuristic uncertainty can dominate ranking | AUDIT STARTED | literature/reference audit started; no runtime change yet |
| 3 | Integrate environmental T/RH handling without invented coefficients | MOS response bias across environment | NOT STARTED | pending |
| 4 | Strengthen fingerprint distance model and concentration-template handling | Correlated MQ channels / diagonal-distance overconfidence | NOT STARTED | pending |
| 5 | Integrate transient/session features into identity evidence under a versioned schema | Static Rs/R0 discards discriminatory kinetics | NOT STARTED | pending |
| 6 | Harden open-set/OOD missing-feature mathematics and threshold calibration | False accepts / incorrect subspace distances | NOT STARTED | pending |
| 7 | Revalidate mixture/interference guard thresholds and semantics | Pure-gas model over-reporting mixtures | NOT STARTED | pending |
| 8 | Reassess PubChem/QSPR role, applicability domain and independent validation | QSPR treated as independent empirical evidence | NOT STARTED | pending |
| 9 | End-to-end analytical regression, audit outputs and release decision | Cross-layer regressions | NOT STARTED | pending |

## Phase 1 — inverse solver ranking and semantics

### Observed issue

`InverseMolecularSolver` previously minimized weighted chi-square over one common concentration and then used raw accumulated `chi2` in `logScore`. Candidates supported by different numbers of sensors were therefore compared on different-dimensional residual sums. A candidate with fewer contributing channels could receive a systematic ranking advantage even when per-channel standardized fit quality was no better.

The field named `posterior` is produced by a softmax of relative model scores. It is an internal relative ranking weight, not a calibrated probability of chemical presence.

### Implemented design

1. Preserved the joint one-concentration fit in `log10(Rs/R0)` space.
2. Added a contributor-normalized weighted fit loss: `chi2 / weightSum`.
3. The grid search, local refinement and final `logScore` now use the same normalized fit loss rather than raw accumulated chi-square.
4. Evidence breadth and independence remain separate quantities handled by the existing evidence/confidence gates rather than being silently mixed into fit quality.
5. Preserved the compatibility field `posterior`, but added explicit runtime semantics: `relative_softmax_model_weight_not_calibrated_probability`.
6. Added a regression case in which two hypotheses have equivalent per-channel standardized fit but different contributor counts. Their fit score/ranking weight must be equal rather than penalizing the broader hypothesis merely for having more valid channels.
7. M3/M4 gates and reportable-ppm rules were not changed.

### Phase-1 acceptance criteria

- Existing concentration-recovery regression remains green.
- Minimum-contributor fail-closed regression remains green.
- New contributor-count comparability regression is green.
- Ranking weights remain finite, normalized to 1 across solvable candidates, and are documented as non-probabilistic.
- No change enables reportable ppm without M4.

### Phase-1 validation state

- Static/source review: COMPLETE.
- Solver implementation commit: `106b62ecd76d282bed2d8aa984c167a35d7a6721`.
- Regression-test commit: `a9473c46c8f5040c7eac46149025668be0f0285e`.
- GitHub Actions automatically triggered because `app/**` changed.
- `Validate HazRob 0.10.2 HR069 Development` run `36920346181`: pending at last check.
- `Build HazRob RC2 Test APK` run `36920346150`: pending at last check.
- Phase 1 MUST NOT be marked COMPLETE until the unit/regression workflow is green. If either run fails, inspect the failing job/log and repair before Phase 2 runtime changes.

## Phase 2 — uncertainty model

Audit each `sigmaLog10` source (peer-reviewed, linear literature, manufacturer graph, research, QSPR, M4). Replace generic constants only when replicated data support an analyte/sensor/domain-specific estimate. Until then retain explicit source-class uncertainty and label it as a research uncertainty proxy. Do not infer precision from R² alone.

### Phase-2 audit note so far

`LiteratureCurveRegistry` contains several very high R² values, but R² alone does not provide residual variance in `log10(Rs/R0)` and therefore must not be converted into `sigmaLog10`. `LinearLiteratureResponseRegistry` likewise preserves reference environmental conditions but does not contain replicate residual variance. Consequently, no automatic tightening of sigma is scientifically justified from those registries alone. The next Phase-2 step is to centralize the current proxy values and attach explicit provenance/semantics without changing their numeric behavior; analyte/sensor-specific sigma may replace them later only when replicate evidence is available.

## Phase 3 — temperature / humidity

The runtime already measures T/RH and contains `EnvironmentalCompensationModel`, but no generic correction may be enabled. The implementation must distinguish:

- source-curve environmental domain checking;
- HR069 empirical environmental compensation;
- QSPR environmental covariates;
- absence of environmental evidence.

Only HR069 controlled experiments may activate sensor-specific compensation coefficients. Missing T/RH must reduce domain confidence or fail closed where a model requires them; it must not fabricate a corrected ratio.

## Phase 4 — fingerprints

Current source-backed fingerprint matching uses standardized per-channel distance. Review covariance among MQ channels and implement a covariance-aware reference only when replicate counts support a stable covariance estimate. Preserve the present diagonal model as a fallback. Fingerprints remain identity evidence only and cannot create reportable ppm.

## Phase 5 — temporal features

ChemCore already retains 17 features per sensor. Introduce temporal identity evidence only under a versioned feature/model artifact with independent validation. Static and temporal evidence from the same experiment/source must share an independence group where appropriate to prevent double counting.

## Phase 6 — open set / OOD

Review Mahalanobis scoring with missing dimensions. A principal block of the full precision matrix is not generally the inverse covariance of the observed subspace. Replace the heuristic only after regression tests cover complete and partial feature vectors. Thresholds must continue to be selected on independent known-vs-unknown validation sessions.

## Phase 7 — mixtures

Retain the conservative semantic: `POSSIBLE_MIXTURE_OR_INTERFERENCE` means a pure-compound hypothesis fits poorly; it does not identify mixture components. Recalibrate residual/posterior thresholds on controlled pure/mixture campaigns before changing them.

## Phase 8 — PubChem/QSPR

Keep QSPR at M1 unless independently validated evidence justifies otherwise. Current runtime QSPR learns primarily from source-backed M2 response relations; therefore it is a molecular prior/model-transfer layer, not an independent physical measurement. Validate applicability domain, leave-compound-out performance, bias and environmental coverage before expanding its role.

## Phase 9 — final qualification

Run ChemCore unit/regression tests plus analytical integration workflows. Produce a final change matrix stating which scientific claims changed, which did not, and what still requires physical HR069 experiments.

## Resumption protocol

If work is interrupted:

1. read this file first;
2. inspect the latest commit on `release/hazrob-0.10.2-rc2`;
3. resume at the first phase not marked COMPLETE;
4. verify the exact files/SHAs before editing;
5. after each accepted phase, update this ledger with the commit SHA, tests, and any remaining limitations.

## Execution log

- 2026-10-01: baseline confirmed at `7342941d1680d6bfdbb404c78c3b93332e8c38ef`.
- 2026-10-01: analytical review identified ranking comparability, ranking semantics, uncertainty calibration, environmental compensation, correlated fingerprints, temporal-feature utilization and OOD missing-data treatment as the main remediation sequence.
- 2026-10-01: remediation ledger created in commit `d1e25ecae51d97dc2070bcb140270465991a6ee0`.
- 2026-10-01: Phase 1 solver implementation committed as `106b62ecd76d282bed2d8aa984c167a35d7a6721`.
- 2026-10-01: Phase 1 regression coverage committed as `a9473c46c8f5040c7eac46149025668be0f0285e`.
- 2026-10-01: Phase 1 CI triggered automatically; validation/build runs were still pending at the latest inspection, so Phase 1 remains open.
- 2026-10-01: Phase 2 audit started. High R² literature relations were explicitly rejected as a basis for manufacturing smaller sigma values because residual/replicate variance is not available from R² alone.
