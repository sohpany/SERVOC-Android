# HazRob 0.10.2 / HR069 — ChemCore Analytical Remediation Plan

Date: 2026-10-01
Branch: `release/hazrob-0.10.2-rc2`
Original baseline: `7342941d1680d6bfdbb404c78c3b93332e8c38ef`
Scope: ChemCore analytical processing, candidate ranking, uncertainty, fingerprints, PubChem/QSPR, open-set/OOD, environmental-domain handling and validation semantics.
Status: ACTIVE — canonical resumable implementation ledger.

## Scientific operating rule

No remediation step may promote external/reference evidence to M4, manufacture missing MQ channels, convert ranking weights into calibrated probabilities, invent temperature/humidity compensation coefficients, or produce reportable ppm without HR069 device-specific reference-gas validation. Every software change must preserve fail-closed behavior and must have a regression test and/or a source-contract audit. Changes that require new physical evidence are explicitly marked BLOCKED BY EXPERIMENT rather than being filled with assumptions.

## Current phase matrix

| Phase | Objective | Status | Main commits / evidence |
|---|---|---|---|
| 0 | Freeze baseline and create resumable ledger | COMPLETE | baseline `7342941d...`; ledger creation `d1e25eca...` |
| 1 | Correct inverse-solver ranking comparability and ranking semantics | IMPLEMENTED; cumulative CI pending | solver `106b62ec...`; test `a9473c46...`; contract repair `9ca6986a...` |
| 2 | Centralize and declare response uncertainty without inventing precision | IMPLEMENTED; cumulative CI pending | policy `feea6c1c...`; reference models `4373f48e...`; solver `0e7f089c...`; test `cd047c82...`; audit `6e95cb87...`; fingerprint registry `9f6b0f53...` |
| 3 | Fail-closed T/RH domain handling without generic compensation | IMPLEMENTED; cumulative CI pending | domain policy `73aae2c8...`; power-law `42c11e73...`; piecewise `2c59e248...`; inference gate `a11164d2...`; tests `eafe72ba...`, `d69d3859...`; audit `9eef96bb...` |
| 4 | Strengthen fingerprint distance/covariance handling | EVIDENCE-GATED | current 13 source-backed numeric families are model-derived templates (`sampleCount=0`); covariance must not be invented |
| 5 | Use 17×8 temporal features for identity evidence | EVIDENCE-GATED | feature extraction exists; activation requires a versioned independently validated temporal identity artifact |
| 6 | Correct open-set Mahalanobis missing-feature mathematics | IMPLEMENTED; cumulative CI pending | model `f6d49073...`; regression `ff18083b...`; audit `13dd9470...` |
| 7 | Clarify mixture/interference policy and revalidate numeric thresholds | SEMANTICS IMPLEMENTED; NUMERIC RECALIBRATION BLOCKED BY EXPERIMENT | policy `02d10517...`; test `ff6a5998...`; numeric values unchanged |
| 8 | Reassess PubChem/QSPR role and validation | POLICY VERIFIED; further promotion blocked | QSPR remains M1, leave-compound-out validated, non-reportable, one correlated evidence lineage |
| 9 | End-to-end qualification | IN PROGRESS | RC2 cumulative run `36922796623` pending; global internal workflow separately blocked by non-ChemCore Remote release-contract failure |

## Phase 1 — inverse solver ranking and semantics

### Problem

The old solver optimized one shared concentration correctly, but ranked candidates using raw accumulated weighted chi-square. Candidates with more usable MQ channels therefore accumulated more residual terms and could be ranked below narrower hypotheses even when per-channel standardized fit quality was identical. The compatibility field `posterior` was also vulnerable to being misread as a calibrated probability.

### Implemented correction

- Joint one-concentration fitting in `log10(Rs/R0)` is preserved.
- Cross-candidate fit loss is now `chi2 / weightSum`.
- Grid search, local refinement and final `logScore` use the same contributor-normalized loss.
- `posterior` remains serialized for compatibility but is explicitly defined as `relative_softmax_model_weight_not_calibrated_probability`.
- Evidence breadth/independence remain separate confidence dimensions rather than being hidden inside the residual fit score.
- Regression test proves equal per-channel fit quality receives equal fit score despite 2 vs 4 contributing channels.

Commits:
- `106b62ecd76d282bed2d8aa984c167a35d7a6721`
- `a9473c46c8f5040c7eac46149025668be0f0285e`
- stale source-contract expectation repaired scientifically in `9ca6986a7fc86100b64fc5f6221714d7e8add134`.

Validation note: Android unit tests in RC2 test run `36920346150` reached `Run Android unit tests: success`. The cumulative latest-runtime validation remains Phase-9 work because later phases changed additional analytical code.

## Phase 2 — uncertainty policy

### Scientific conclusion

Very high R² in literature curves cannot be converted into a response sigma without residual/replicate variance. Therefore no smaller sigma was inferred from R² and no source was promoted in precision.

### Implemented correction

Created `ChemCoreUncertaintyPolicy` with explicit semantics:

`research_log10_response_sigma_proxy_not_calibrated_prediction_interval`

Existing 0.10.2 values are preserved numerically:
- peer-reviewed power relation: 0.20 log10
- linear literature transfer: 0.25
- manufacturer primary: 0.22
- manufacturer secondary/graph: 0.27
- research/external curve: 0.32
- solver fallback: 0.20
- numerical sigma floor: 0.02
- out-of-domain widening multiplier: 1.80

The policy is now used by accepted reference-response fitting, the inverse solver and source-backed research fingerprint generation. A unit test explicitly forbids treating R² as sigma evidence.

Commits:
- policy `feea6c1c2697dbebf81687d9a1a7ca5d6a114817`
- accepted reference model `4373f48ee42c0d3a1066a626ec256bd1965e79e8`
- solver `0e7f089c5a7c13291b499c88e44a833e3c175c53`
- test `cd047c82fdd938ecc91d6399df5376b8a640788c`
- source audit `6e95cb87c3497c60ea22c2d795ad11d4458043f4`
- research fingerprint registry `9f6b0f53d13023a87c78aa3895d590761b00a430`

Remaining scientific limitation: these are still source-class proxies. Sensor×analyte×concentration×environment uncertainty should replace them only after replicated HR069 measurements support it.

## Phase 3 — environmental domain

### Problem

`PowerLawResponseModel` and `PiecewiseLogResponseModel` previously checked a declared T/RH range only when the runtime T/RH value was finite. If a required environmental measurement was missing, `insideDomain` could remain true. In addition, the M2→M3 promotion path did not require all response contributors to be inside their declared domains.

### Implemented correction

Created `ChemCoreEnvironmentDomainPolicy` with semantics:

`declared_environment_domain_gate_no_generic_compensation`

Rules:
- if a model declares a finite temperature domain, temperature is required;
- if a model declares a finite RH domain, RH is required;
- missing required environment ⇒ `insideDomain=false`;
- measured value outside a declared domain ⇒ `insideDomain=false`;
- a model that declares no T/RH range does not receive an invented range;
- no generic environmental compensation coefficient is activated.

Inference hardening:
- M3 reference-backed promotion now requires `allContributorsInsideDomain`;
- with accepted open-set evidence but any contributor out of domain, result becomes `OUT_OF_VALIDATED_DOMAIN` and named identity/reportable concentration are withheld;
- corroborative residual calculations now use the same centralized uncertainty policy.

Commits:
- policy `73aae2c8d1f6ee6ee2e2bd0632652e7e61c400b5`
- power-law integration `42c11e737888fcbcd2e09e8ccaa9b86510c678ab`
- piecewise integration `2c59e2486910f51f80062b2b8d83cc2fbc23d59b`
- environment policy test `eafe72ba6653c409de983f4d5feed156e33e44d2`
- inference gate `a11164d2ea4ce1d937284491e2b849b920a3b3c1`
- domain-gating regression `d69d3859cca1baf6cc6bec776b98f2687a956dec`
- audit contract `9eef96bb15c930fd7bcac7fea19abb65899aa55e`

Important limitation: this is domain gating, not T/RH compensation. `EnvironmentalCompensationModel` must remain inactive until controlled HR069 experiments establish sensor-specific coefficients.

## Phase 4 — fingerprints

Current accepted research priors are deterministic concentration templates generated from source-backed response relations. `ResearchIdentificationFingerprintRegistry` creates them with `sampleCount=0`; therefore they do not contain replicated HR069 covariance evidence. The current standardized diagonal distance remains the defensible fallback for these priors.

Decision: DO NOT manufacture covariance from model-derived channels. A covariance-aware identity metric may be activated only for fingerprint artifacts carrying enough independent replicated observations to estimate a stable covariance matrix. Until then fingerprint evidence remains M3 identity-only after open-set acceptance and never produces reportable ppm.

## Phase 5 — temporal identity features

ChemCore already extracts 17 features per sensor (136 for the eight HR069 MQ channels), but those features must not be injected into candidate identity ranking without a versioned model artifact and independent validation. Static and temporal features derived from the same exposure must share provenance/independence semantics to avoid double counting.

Status: EVIDENCE-GATED. Software plumbing exists; scientific activation requires controlled labeled HR069 sessions.

## Phase 6 — open-set / OOD missing features

### Problem

The old missing-feature path extracted a principal block from the full inverse covariance (precision) matrix. In general:

`(Sigma^-1)_SS != (Sigma_SS)^-1`

so this is not the correct Mahalanobis metric for the observed marginal feature subspace.

### Implemented correction

`MahalanobisModel` now retains/reconstructs the covariance matrix. For partial runtime vectors it:
1. extracts the covariance marginal for observed features;
2. inverts that marginal;
3. evaluates the quadratic form in the observed space;
4. retains the existing dimension-coverage scaling only for approximate compatibility with thresholds calibrated on full-dimensional vectors.

Semantics are explicitly recorded as:

`marginal_covariance_subspace_with_dimension_coverage_scaling`

Regression with an analytically known 2×2 correlated covariance proves the result differs from the old incorrect precision-block calculation. The analytical source-contract audit now also forbids restoring the precision principal-block shortcut.

Commits:
- model `f6d49073ac2f1ce1d228768e0e94473c5ccb2fa6`
- regression `ff18083b4f9dc840349898c1bbcbbd53b83689ba`
- source-contract audit `13dd9470876d1c76f169988db286d937b9d0e4e9`

Remaining limitation: the D/d coverage scaling is an interoperability approximation. The preferred final state is threshold calibration on independent known/unknown validation vectors for the actual missingness patterns expected in production.

## Phase 7 — mixtures / interference

### Preserved analytical meaning

`POSSIBLE_MIXTURE_OR_INTERFERENCE` means the best pure-compound hypothesis leaves an excessive standardized multisensor residual. It does not mean ChemCore has decomposed the mixture or identified its components.

After Phase 1, the candidate value historically called `posterior` is explicitly a relative softmax ranking weight. Therefore the mixture guard now records:

`research_policy_thresholds_not_calibrated_presence_probabilities`

The existing numeric policy is frozen, not re-fitted:
- max standardized residual RMS = 2.50
- minimum top relative ranking weight = 0.80
- minimum ranking-weight margin = 0.25

No numeric value was changed. The result API keeps `topPosterior` / `secondPosterior` only for compatibility and exposes ranking-weight accessors/semantics. Tests assert that a mixture warning is not component identification and that ranking weights are not presence probabilities.

Commits:
- policy semantics `02d105179bcb69227537e44dad1aa76129ca1126`
- regression/policy test `ff6a5998e9a841a86da61b69da37c7d84f468fe5`

Numeric recalibration remains BLOCKED BY EXPERIMENT and requires controlled HR069 pure-gas and mixture campaigns across concentration ratios and environment.

## Phase 8 — PubChem/QSPR

Current scientifically acceptable role is retained:
- molecular descriptors from the curated PubChem layer;
- sensor-specific ridge QSPR in log-response space;
- leave-compound-out validation;
- applicability-envelope checks;
- all QSPR sensor channels share one correlated evidence lineage;
- QSPR is M1 and non-reportable;
- direct M2/M4 response evidence wins on the same sensor;
- QSPR alone cannot create M3/M4 or reportable ppm.

Because the current QSPR is trained primarily from source-backed M2 response relations rather than a large independent HR069 reference-gas matrix, it remains a molecular prior/model-transfer layer. No evidence-tier promotion is authorized.

## Phase 9 — cumulative qualification

Required before this remediation set is called release-qualified:
1. latest-runtime source contract green;
2. all Android unit/regression tests green;
3. RC2 debug APK build green;
4. confirm no M4/reportable-ppm gate was weakened;
5. record final workflow run ID and tested runtime head in this ledger.

Current dedicated RC2 qualification run:
- runtime/test head: `ff6a5998e9a841a86da61b69da37c7d84f468fe5`
- `Build HazRob RC2 Test APK` run `36922796623`: pending at latest check.

Separate global workflow blocker:
- `Build HazRob APK — Internal Validation` run `36922357941` failed before Android/analytical tests in `audit_0102_release_contract.py` because a Remote diagnostics normalization literal was missing: `JSONObject diag=sourceDiag==null?new JSONObject():new JSONObject(sourceDiag.toString())`.
- This is outside ChemCore and occurred before `audit_0105_analytical_engines.py` or Gradle were reached. It is recorded as a non-analytical global release-contract blocker and is not being modified within this ChemCore remediation task.

## Resumption protocol

If work is interrupted:
1. read this file first;
2. inspect the latest commit on `release/hazrob-0.10.2-rc2`;
3. treat `ff6a5998...` as the current cumulative ChemCore runtime/test head unless a later `app/**` commit is documented here;
4. resume first by checking RC2 qualification run `36922796623`;
5. if that run fails, inspect the exact failing unit/build step and repair only the relevant ChemCore regression;
6. do not implement Phase 4 covariance, Phase 5 temporal identity activation, Phase 7 numeric threshold changes, environmental compensation, or QSPR evidence promotion without the required experimental evidence;
7. after every accepted change update this ledger with exact commit SHA and CI evidence.

## Execution log

- `7342941d...` baseline confirmed.
- `d1e25eca...` remediation ledger created.
- `106b62ec...` Phase-1 normalized fit ranking implemented.
- `a9473c46...` Phase-1 contributor-count regression added.
- first validation exposed stale textual audit expectation; no analytical failure. Audit corrected in `9ca6986a...` to enforce normalized ranking semantics.
- RC2 test run `36920346150` reported Android unit tests successful for the Phase-1 code path.
- Phase-2 uncertainty audit rejected R² as a substitute for residual/replicate variance.
- `feea6c1c...` through `9f6b0f53...` centralized uncertainty semantics without tightening numerical sigma.
- `73aae2c8...` through `9eef96bb...` implemented fail-closed declared environmental-domain handling and M3 domain gating without activating generic compensation.
- `f6d49073...` / `ff18083b...` corrected missing-feature Mahalanobis marginal mathematics and added a closed-form regression; `13dd9470...` added the source-contract guard.
- global internal-validation run `36922357941` was blocked by an unrelated Remote release-contract check before ChemCore testing; no Remote code was changed in this task.
- `02d10517...` / `ff6a5998...` clarified mixture/ranking semantics without changing numeric thresholds and triggered dedicated RC2 cumulative qualification run `36922796623`.
