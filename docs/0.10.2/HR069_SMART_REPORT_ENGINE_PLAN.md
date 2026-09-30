# HazRob 0.10.2 / HR069 — Smart Chemical Reporting Engine

Status: ACTIVE — implementation in progress
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

## Report architecture

### Field-session report — A4 landscape, 4–5 pages

**Page 1 — Executive analytical decision**
- Site/session identity and measurement lineage.
- Report mode: Signal / Screening / Identified / Quantified / Open-set-Mixture.
- Top chemical result, analytical confidence, posterior/ranking weight, separation margin, OOD and entropy.
- Measurement-quality and evidence gates.
- Quantitative/M4 state and immediate safety state.
- Automatically generated decision notes and limitations.

**Page 2 — Sensor evidence and fingerprint**
- Dynamic selection from: Rs/R0 time response, fingerprint vector/radar, SNR, response kinetics, persistence/AUC, environment-response matrix.
- The planner chooses the most informative available plots for the recorded session.

**Page 3 — Chemical identity and 144-material evidence map**
- Candidate ranking and fit/residual visualization.
- 144-material session-evidence matrix.
- Top supported materials with session support index, evidence breadth and reliability band.
- Explicit distinction between catalogue support and session evidence.

**Page 4 — Quantitation / safety / uncertainty**
- Quantified sessions: PPM interval, exposure ratios, LFL/health reference context, environment.
- Non-quantified sessions: open-set/OOD, ambiguity, entropy/separation, measurement-quality limitations and environmental context.
- No false compliance conclusion.

**Page 5 — Traceability and scientific provenance (conditional)**
- Included when ChemCore evidence/provenance, multiple child measurement sessions, or significant warnings warrant it.
- Exact measurement-session IDs, model/library/version/hash, feature/sampling contract, evidence tier, calibration state and report-plan rationale.

### Site report

- Executive site dashboard.
- All field sessions as independent rows.
- Session analytical state / confidence / evidence / quantitative gate / safety priority.
- Chemical recurrence map across sessions.
- 144-material recurrence/reliability summary: number of sessions with candidate support, accepted identities, and maximum/median support index where available.
- Trend visualizations only for comparable metrics; no pooled concentration calculation across heterogeneous sessions.
- Critical-session and repeated-evidence prioritization.

## Intelligent planning inputs

The planner must inspect:
- usable recorded time series;
- ChemCore outcome and evidence tier;
- candidate count and candidate evidence observations;
- analytical confidence, evidence-group count and sensor breadth;
- posterior separation margin, OOD and normalized entropy;
- measurement/lab quality and calibration gate;
- 144-panel rows and session-specific candidate support;
- PPM reportability and prediction interval;
- gas exposure / LFL payload;
- environmental series availability;
- number of child measurement sessions;
- library/model/sampling/feature provenance.

## Implementation phases

| Phase | Deliverable | State |
|---|---|---|
| RPT-01 | Extend 144 panel with session-specific evidence/reliability semantics | ACTIVE |
| RPT-02 | Upgrade `ReportIntelligence` to a report planner | PENDING |
| RPT-03 | Add advanced report visuals: 144 reliability matrix, evidence breadth, uncertainty/decision quality | PENDING |
| RPT-04 | Rebuild field-session PDF around the intelligent plan | PENDING |
| RPT-05 | Rebuild site summary around recurrence and cross-session evidence | PENDING |
| RPT-06 | Add source-contract tests and final documentation | PENDING |

## Build hold

Implementation commits and source-level contract tests may be added, but **do not run APK/release build or final CI qualification yet**. Final build/qualification is deferred until the user explicitly states that all modification packages are complete.
