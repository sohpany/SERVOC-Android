# HazRob 0.10.2 / HR069 — Smart Chemical Reporting Engine

Status: ACTIVE — source implementation substantially complete
Branch: `release/hazrob-0.10.2`
Starting HEAD: `8a860f10f0f719b902fe9b44c7cf46f565a66977`
Build policy: **NO APK / NO release build until the user finishes all modification packages.**

## Objective

Replace fixed report templates with an evidence-driven reporting engine that plans each field-session and site report from the actual analytical evidence recorded by HazRob. The engine must exploit the current HR069 analytical stack without overstating chemical identity or concentration.

## Scientific reporting rules

1. Keep four concepts separate:
   - catalogue/array **coverage capability**;
   - **session-specific supporting evidence**;
   - accepted **named identity**;
   - **M4 reportable quantitative concentration**.
2. Candidate posterior/ranking weight is not a calibrated identity probability.
3. The existing analytical-confidence index is a positive-evidence support index, not a detection probability. Report bands must preserve that semantic explicitly.
4. The 144-panel must show all materials, but absence of candidate evidence must never be reported as a negative analytical finding when the method cannot support such a conclusion.
5. No PPM, occupational exposure comparison or flammability conclusion may be promoted unless the existing ChemCore/M4 gates permit it.
6. Do not merge independent measurement-session time series into an artificial session curve.
7. Every visual must have a source and a reason for inclusion. Missing evidence removes or substitutes a visual; it does not generate synthetic data.
8. A site report compares independent field sessions and recurring patterns; it does not average incompatible PPM results.
9. The user-facing **Tracking** function is operational only and is excluded from all PDF/site reports. Reports retain only minimum scientific provenance needed to identify the model/library/method and measurement session that produced the decision.
10. Candidate hazard context is permitted for **screening prioritization only**. `screening_fit_ppm` may be compared with directly comparable ppm OELs and LFL/UFL references, but this does not create an occupational exceedance, compliance determination, or LFL alarm. Only M4-reportable ppm can enter the validated gas-safety path.

## Report architecture

### Field-session report — A4 landscape, 4–5 pages

**Page 1 — Executive analytical decision**
- Site/session identity and measurement basis.
- Report mode: Signal / Screening / Identified / Quantified / Open-set-Mixture.
- Top chemical result, analytical confidence, posterior/ranking weight, separation margin, OOD and entropy.
- Measurement-quality and evidence gates.
- Quantitative/M4 state and immediate safety state.
- Candidate hazard strip showing toxicity / flammability context with explicit M4 vs EST basis.
- Automatically generated decision notes and limitations.

**Page 2 — Sensor evidence and fingerprint**
- Dynamic selection from Rs/R0 time response, fingerprint vector/radar, SNR, response kinetics, persistence/AUC and environment-response matrix.
- If time-series evidence is absent, decision-quality and evidence-breadth views replace empty plots.

**Page 3 — Chemical identity, candidate hazards and 144-material evidence map**
- Candidate evidence matrix.
- Candidate chemical properties: name, formula, CAS, chemical class.
- Candidate concentration basis: `M4_MEASURED_REPORTABLE` or `SCREENING_FIT_ESTIMATE`.
- Candidate toxicity context: controlling directly comparable ppm reference, authority/type and percentage of reference.
- Candidate flammability context: LFL/UFL availability and %LFL context.
- 144-material session-evidence matrix.
- Top supported materials with session support index and reliability band.
- Explicit distinction between catalogue support and session evidence.

**Page 4 — Quantitation / safety / uncertainty**
- Quantified sessions: PPM interval when recorded, exposure ratios, LFL/health reference context and environment.
- Non-quantified sessions: open-set/OOD, ambiguity, entropy/separation, measurement-quality limitations and environmental context.
- No false compliance conclusion.

**Page 5 — Methodology and scientific provenance (conditional)**
- Included only when analytical provenance, multiple technical measurement sessions, or significant methodological warnings justify a fifth page.
- Shows only the minimum scientific provenance required for auditability: primary measurement-session ID, model/library/version/hash, feature/sampling contract, evidence tier, calibration state and report-plan rationale.
- **No Tracking state, Tracking history, tracked-candidate timeline or Tracking controls are included.**

### Site report

- Executive site dashboard.
- All field sessions as independent rows.
- Session analytical state / confidence / 144 support / quantitative gate / safety priority.
- Candidate hazard context per session, including candidate rank and M4 vs EST basis.
- Chemical recurrence map across sessions.
- 144-material recurrence/reliability summary: supported sessions, accepted identities, M4-quantified sessions and maximum/median support index.
- Site analytical-state distribution.
- Priority table for highest candidate hazard context in each session.
- No pooled concentration calculation across heterogeneous sessions.
- No operational Tracking content.

## Candidate hazard-context contract

Implemented by `CandidateHazardContext`:

- Reads candidate `screening_fit_ppm` only as an analytical fit hypothesis.
- Reads accepted M4 ppm only when `ppm_reportable=true`, outcome is `QUANTIFIED`, and tier is `M4_HAZROB_VALIDATED`.
- Uses `ExposureRepository` so report context inherits the authority-preserving exposure library rather than a duplicated table.
- Uses direct ppm health references for candidate comparisons.
- Preserves but does not convert mass-concentration limits for screening candidates.
- Uses NIOSH NPG LFL/UFL references already imported into the same exposure repository.
- Displays toxicity and flammability as separate hazard dimensions.
- Carries authority, reference type, source ID and revision for the controlling reference.
- Never generates a compliance determination.

## Intelligent planning inputs

The planner inspects:
- usable recorded time series;
- ChemCore outcome and evidence tier;
- candidate count and candidate evidence observations;
- analytical confidence, evidence-group count and sensor breadth;
- posterior separation margin, OOD and normalized entropy;
- measurement/lab quality and calibration gate;
- 144-panel rows and session-specific candidate support;
- PPM reportability and prediction interval;
- gas exposure / LFL payload;
- candidate hazard context from the exposure/flammability library;
- environmental series availability;
- number of child measurement sessions;
- library/model/sampling/feature provenance.

Operational Tracking state is explicitly **not** an input to report content.

## Implementation phases

| Phase | Deliverable | State |
|---|---|---|
| RPT-01 | Extend 144 panel with session-specific evidence/reliability semantics | **DONE — SOURCE** |
| RPT-02 | Upgrade `ReportIntelligence` to an evidence-driven report planner | **DONE — SOURCE** |
| RPT-03 | Advanced visuals: 144 map, evidence breadth, uncertainty, decision quality | **DONE — SOURCE** |
| RPT-04 | Rebuild field-session PDF as smart 4–5 page A4 landscape report | **DONE — SOURCE** |
| RPT-05 | Rebuild site summary around recurrence and cross-session evidence | **DONE — SOURCE** |
| RPT-05A | Candidate chemical properties + OEL/LFL hazard context | **DONE — SOURCE** |
| RPT-06 | Source-contract tests and final documentation | **ACTIVE** |

## Current source checkpoints

- `9ea015071dacd0f0a5dcba6900a77876e9d6ced9` — candidate hazard-context engine.
- `a67f9d01de6dad325d9074d1a6d37e412c1323af` / `0b4234d49b1feb43b6631ab5e2fe72cc91e61a3c` — candidate hazard visuals and chemical-property/OEL labeling.
- `72f3a6ad411fa201868cabdbf711ba08a551c239` — field-session integration.
- `9c3cc90ced608bda92fb15110ebea7224095baed` / `a8b376ab1e965f81b6aa734b250aa9e5a539bcd3` — intelligent site report and candidate-hazard prioritization.

## Build hold

Implementation commits and source-level contract tests may be added, but **do not run APK/release build or final CI qualification yet**. Final build/qualification is deferred until the user explicitly states that all modification packages are complete.
