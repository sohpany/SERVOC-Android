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
| 0 | Freeze baseline and create resumable remediation ledger | Loss of analytical provenance / interrupted work | COMPLETE | baseline `7342941d...`; this document |
| 1 | Correct inverse-solver ranking comparability and ranking semantics | Candidate-count bias; `posterior` misinterpretation | IN PROGRESS | pending |
| 2 | Calibrate/declare model uncertainty (`sigma`) by evidence source and domain | Heuristic uncertainty can dominate ranking | NOT STARTED | pending |
| 3 | Integrate environmental T/RH handling without invented coefficients | MOS response bias across environment | NOT STARTED | pending |
| 4 | Strengthen fingerprint distance model and concentration-template handling | Correlated MQ channels / diagonal-distance overconfidence | NOT STARTED | pending |
| 5 | Integrate transient/session features into identity evidence under a versioned schema | Static Rs/R0 discards discriminatory kinetics | NOT STARTED | pending |
| 6 | Harden open-set/OOD missing-feature mathematics and threshold calibration | False accepts / incorrect subspace distances | NOT STARTED | pending |
| 7 | Revalidate mixture/interference guard thresholds and semantics | Pure-gas model over-reporting mixtures | NOT STARTED | pending |
| 8 | Reassess PubChem/QSPR role, applicability domain and independent validation | QSPR treated as independent empirical evidence | NOT STARTED | pending |
| 9 | End-to-end analytical regression, audit outputs and release decision | Cross-layer regressions | NOT STARTED | pending |

## Phase 1 — inverse solver ranking and semantics

### Observed issue

`InverseMolecularSolver` currently minimizes weighted chi-square over one common concentration, which is scientifically preferable to independently inverting each MQ curve. However candidate ranking uses raw accumulated `chi2` in `logScore`. Candidates supported by different numbers of sensors are therefore compared on different-dimensional residual sums. A candidate with fewer contributing channels can receive a systematic ranking advantage even when per-channel standardized fit quality is no better.

The field named `posterior` is produced by a softmax of relative model scores. It is an internal relative ranking weight, not a calibrated probability of chemical presence.

### Phase-1 design decision

1. Preserve the joint one-concentration fit in `log10(Rs/R0)` space.
2. Optimize and rank on a contributor-normalized weighted residual loss (`chi2 / weightSum`) rather than raw residual sum when candidates differ in evidence breadth.
3. Keep evidence breadth and independence as separate quantities handled by the existing evidence/confidence gates rather than silently rewarding or penalizing channel count inside fit quality.
4. Preserve the serialized/API field `posterior` for compatibility, but explicitly define it in code and documentation as `relative model ranking weight`, not calibrated probability.
5. Add a regression test in which two hypotheses have equivalent per-channel standardized fit but different contributor counts; the broader model must no longer be penalized solely because it has more valid channels.
6. Do not change M3/M4 gates in this phase.

### Phase-1 acceptance criteria

- Existing concentration-recovery regression remains green.
- Minimum-contributor fail-closed regression remains green.
- New contributor-count comparability regression is green.
- Ranking weights remain finite, normalized to 1 across solvable candidates, and are documented as non-probabilistic.
- No change enables reportable ppm without M4.

## Phase 2 — uncertainty model

Audit each `sigmaLog10` source (peer-reviewed, linear literature, manufacturer graph, research, QSPR, M4). Replace generic constants only when replicated data support an analyte/sensor/domain-specific estimate. Until then retain explicit source-class uncertainty and label it as a research uncertainty proxy. Do not infer precision from R² alone.

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
- 2026-10-01: Phase 1 started; code/tests pending.
