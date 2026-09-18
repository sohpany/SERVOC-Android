# HazRob 0.10.1 — Measurement, Analysis & Reporting Development Execution Plan

**Document ID:** HZR-0101-ANL-EXEC-001  
**Baseline:** HazRob 0.10.1 / HR066  
**Created:** 2026-09-18  
**Status:** ACTIVE — execution reference  
**Purpose:** Persistent recovery/checkpoint document for the complete scientific development of Measurement, Analysis and Reports. This file MUST be updated at the end of every completed stage so interrupted work resumes from the first incomplete stage and never restarts completed work without a documented regression reason.

## 1. Governing rules
1. HR066 is the implementation baseline unless a later checkpoint explicitly supersedes it.
2. Do not begin a stage until its prerequisites are complete.
3. A stage is not COMPLETE merely because code was written. COMPLETE requires code + tests + integration/audit + documentation + commit SHA.
4. Every stage completion must update this file in the same work cycle.
5. Existing scientific gates remain fail-closed: no reportable PPM without complete M4 quantitative gate; UNKNOWN must remain possible; mixture/interference and OOD must not be forced into a single identity; TWA/STEL duration semantics must be preserved.
6. UI, PDF and XLSX must consume the same persisted analytical result contract.
7. ChemCoreFeaturePipeline is to become the canonical feature definition; duplicate feature/fingerprint definitions must not silently drive scientific decisions.
8. The operational 144-compound panel and the extended scientific knowledge base remain distinct concepts.
9. HazRob 0.10.1 is runtime measurement/analysis/reporting. HazRob Lab remains the future library/model development platform.
10. No stage may silently change firmware/network/motion contracts unrelated to this scope.

## 2. Status legend
- NOT_STARTED — no implementation work accepted.
- IN_PROGRESS — implementation underway; do not duplicate on resume.
- BLOCKED — blocked; blocker must be documented.
- VALIDATION — implementation complete, acceptance tests pending.
- COMPLETE — acceptance criteria passed and checkpoint committed.
- REOPENED — previously complete stage reopened due to documented regression/change.

## 3. Master execution sequence

### Stage 0 — Baseline freeze and recovery checkpoint
**Status:** COMPLETE  
**Goal:** Establish HR066 as the known baseline and create this persistent execution ledger.
**Tasks:**
- Record scope and governing scientific rules.
- Record ordered stages and completion protocol.
- Use this file as the mandatory resume point after any interruption.
**Acceptance:** Reference document committed on the active 0.10.1 branch.
**Completion record:** This document creation commit.

### Stage 1 — Analytical Contract v4 specification
**Status:** COMPLETE  
**Goal:** Define one canonical session-bound result consumed by UI/reports.
**Tasks:**
- Specify chemcore-result/4 fields.
- Define measurement quality, screening_144, candidates, accepted findings, safety findings, methodology, source_refs, feature summary, quality gates and provenance.
- Define backward compatibility/migration from chemcore-result/3 and legacy analytics.
- Define null/withheld semantics and reason codes.
**Acceptance:** Contract schema documented; serialization/deserialization tests; no scientific gate regression.

### Stage 2 — Canonical feature pipeline consolidation
**Status:** IN_PROGRESS  
**Prerequisite:** Stage 1.
**Goal:** Make ChemCoreFeaturePipeline the single scientific feature contract.
**Tasks:**
- Inventory TemporalFeatureExtractor, SignalFeatures and chart-local fingerprint calculations.
- Route scientific decisions/report plots to canonical 17-feature definitions where applicable.
- Preserve compatibility adapters only where necessary.
- Version feature schema.
**Acceptance:** deterministic feature tests; same session produces identical feature values across analysis/report paths.

### Stage 3 — Measurement quality and live gas monitor
**Status:** NOT_STARTED  
**Prerequisite:** Stage 2.
**Goal:** Improve live operational warning without claiming calibrated exposure.
**Tasks:**
- Combine warm-up, power, channel validity, baseline, noise, drift, persistence and multisensor consensus.
- Preserve offline operation.
- Separate operational response warning from IDLH/OEL/LFL quantitative claims.
- Add fail-safe unavailable/degraded states.
**Acceptance:** tests for spike, persistent response, missing sensor, warm-up, baseline fallback, drift and de-escalation.

### Stage 4 — 144-compound screening engine
**Status:** NOT_STARTED  
**Prerequisite:** Stages 1–2.
**Goal:** Produce an explicit result for every compound in the operational 144 panel.
**Tasks:**
- Implement stable 144 membership/version.
- Map library/model evidence to every target.
- Define statuses: SUPPORTED, WEAK_MATCH, REJECTED, OUT_OF_DOMAIN, INSUFFICIENT_EVIDENCE, UNSUPPORTED_BY_CURRENT_ARRAY (final names fixed in Stage 1).
- Persist reason/evidence/source for every row.
- Guarantee exactly 144 rows for a valid panel version.
**Acceptance:** deterministic 144-row output; no unsupported compound fabricated as detected; complete source/evidence trace.

### Stage 5 — Unified inference, open-set, mixture and quantitative gates
**Status:** NOT_STARTED  
**Prerequisite:** Stage 4.
**Goal:** Integrate candidate ranking, joint curve fitting and scientific decision gates into Contract v4.
**Tasks:**
- Preserve InverseMolecularSolver joint concentration fit.
- Integrate open-set rejection, OOD and mixture/interference guards.
- Keep posterior as relative ranking unless probability calibration is validated.
- Enforce M4 for reportable PPM.
- Add uncertainty/prediction interval only where scientifically supported.
**Acceptance:** UNKNOWN, mixture, OOD and M0–M4 test matrix passes; no forced identity/PPM.

### Stage 6 — Exposure and flammability interpretation
**Status:** NOT_STARTED  
**Prerequisite:** Stage 5.
**Goal:** Bind accepted quantitative findings to authoritative exposure/flammability references.
**Tasks:**
- Preserve source, unit, limit type, duration and authority.
- Distinguish TWA, STEL, Ceiling, IDLH and LFL semantics.
- Prevent compliance claims without required temporal/method coverage.
- Maintain hazard-priority sorting independent of identity ranking.
**Acceptance:** duration/unit/source tests and quantitative-gate tests pass.

### Stage 7 — Analysis UI redesign
**Status:** NOT_STARTED  
**Prerequisite:** Stages 4–6.
**Goal:** One-button analysis with progressive disclosure.
**Tasks:**
- Keep CONDITION → BASELINE → EXPOSURE → FEATURE/DECISION → RECOVERY workflow.
- Primary summary: measurement quality, chemical response, top candidates, evidence tier, PPM state and hazard.
- Add 144 screening view/search/filter.
- Add candidate detail with CAS/formula/evidence/source/domain/gate reasons.
- Label posterior as relative analytical ranking, not calibrated identity probability.
**Acceptance:** UI reads Contract v4 only; no scientific result recomputed in presentation layer.

### Stage 8 — Measurement UI refinement
**Status:** NOT_STARTED  
**Prerequisite:** Stage 3.
**Goal:** Simplify live operator view.
**Tasks:**
- Main gas-state card.
- Sensor readiness/health summary.
- Clear operational alert wording.
- Progressive details for individual MQ channels and environment.
**Acceptance:** direct and remote modes behave consistently; no dependency on Internet for live warning.

### Stage 9 — Analytical visualization layer
**Status:** NOT_STARTED  
**Prerequisite:** Stages 2 and 5.
**Goal:** Make every chart explain evidence used by the decision.
**Tasks:**
- Rs/R0 time traces.
- log10 response with phase boundaries.
- canonical signed fingerprint.
- slope/t10/t50/t90/AUC/SNR.
- candidate ranking.
- candidate×sensor residual map.
- measured point on validated calibration curve.
- concentration-vs-time only when quantitatively valid.
- exposure/LFL ratios and environmental context.
**Acceptance:** charts use persisted/canonical data; no decorative or contradictory derived fingerprint.

### Stage 10 — ReportsTab unification
**Status:** NOT_STARTED  
**Prerequisite:** Stage 7.
**Goal:** Make report history use the same ChemCore session result as exports.
**Tasks:**
- Remove legacy analytics-result count as primary truth.
- Resolve result by session/Contract v4.
- Show top finding, candidate count, hazard, PPM status, library/model provenance.
**Acceptance:** on-screen report summary equals exported report for same session.

### Stage 11 — PDF/XLSX v4
**Status:** NOT_STARTED  
**Prerequisite:** Stages 9–10.
**Goal:** Produce concise operator summary plus auditable scientific evidence.
**Tasks:**
- Page 1 executive analytical result.
- Detailed quality, candidates, curves/residuals and safety interpretation.
- 144-compound screening appendix.
- XLSX Screening_144 sheet plus session/raw/feature/provenance sheets as defined.
- Bind exact library/model/calibration/source provenance.
**Acceptance:** UI/PDF/XLSX semantic parity; 144 rows present; old session provenance not replaced by current library.

### Stage 12 — Drift and longitudinal sensor-health layer
**Status:** NOT_STARTED  
**Prerequisite:** Stage 3.
**Goal:** Detect sensor drift and protect scientific gates.
**Tasks:**
- Persist baseline/R0/noise history.
- Calculate drift indicators.
- Warn/recalibrate when limits exceeded.
- Withhold M4 when calibration validity is no longer established.
**Acceptance:** synthetic/recorded drift tests; fail-closed quantitative behavior.

### Stage 13 — Full regression, scientific validation and integration audit
**Status:** NOT_STARTED  
**Prerequisite:** Stages 1–12.
**Goal:** Prove Android/ChemCore scientific coherence before final robot integration.
**Mandatory scenarios:**
- clean air/no response
- known single-compound response
- weak evidence
- unknown/open-set
- mixture/interference
- out-of-domain
- missing MQ channel(s)
- environment unavailable
- noisy/spike response
- drifted sensor
- M0/M1/M2/M3 result
- complete M4 quantified result
- short capture against TWA/STEL
- LFL reference present/absent
- old saved session
- Direct and Remote operation
**Acceptance:** all Android/ChemCore scientific audits/tests pass. Firmware build/modification/integration is explicitly out of scope in Stage 13.

### Stage 14 — Release candidate and field-validation package
**Status:** NOT_STARTED  
**Prerequisite:** Stage 13.
**Goal:** Produce controlled test build for physical experiments.
**Tasks:**
- Assign new HR build ID.
- Build APK + Main firmware + CAM firmware only if firmware changed.
- Generate SHA-256.
- Publish change log, known limitations and test protocol.
- Preserve HR066 recovery baseline.
**Acceptance:** reproducible successful build and documented field-test package.

## 4. Mandatory completion log
Append one row whenever a stage changes state.

| UTC date | Stage | New status | Commit SHA | Tests/audits | Notes |
|---|---:|---|---|---|---|
| 2026-09-18 | 0 | COMPLETE | 9a15a10fb7b537af122a6d5d8e4167653965628a | Reference creation | HR066 baseline; execution ledger established |
| 2026-09-18 | 1 | IN_PROGRESS | 6b58c498ebb5ec4723e0452e6623833b64102158 | Contract spec + v4 envelope + tests added | Validation/build pending |
| 2026-09-18 | 1 | COMPLETE | 0bcb09db49caadcf6fbdbcabb32d8f78872d6f53 | v4/V3 regression + audit JSON + M4 completeness; lintDebug; assembleRelease; validation deployment baecaf6e-1759-498c-a99d-b163a6234120 SUCCESS | Analytical Contract v4 accepted; firmware frozen and unchanged |
| 2026-09-18 | 2 | IN_PROGRESS | — | Consumer inventory starting | Canonicalize Android analytical feature paths onto chemcore-features/1; no firmware changes |\n| 2026-09-18 | 2 | COMPLETE | 3557bb4c945dced33d8f8f1800f9670ca063718b | deterministic canonical feature tests; lint/build validation deployment c073febd-67f6-4c82-a2fd-d8d967e55ecd SUCCESS; firmware diff audit clean | TemporalFeatureExtractor, SignalFeatures and analytical fingerprint consolidated onto chemcore-features/1 |\n| 2026-09-18 | 3 | IN_PROGRESS | 3557bb4c945dced33d8f8f1800f9670ca063718b | live-state engine + coordinator + UI integration tests underway | Immediate WARNING preserved; DANGER requires persistent multisensor response; no identity/ppm claim |

## 5. Resume protocol after interruption
1. Open this file first.
2. Read the completion log from bottom upward.
3. Verify the last COMPLETE commit exists on the active branch.
4. Do NOT rerun completed implementation stages unless a regression or dependency change requires it.
5. Resume the first IN_PROGRESS stage; if none, start the first NOT_STARTED stage whose prerequisites are COMPLETE.
6. Before starting new code, inspect the last stage commit and its tests.
7. At stage completion: run acceptance tests → update status → append completion row → commit this file with the implementation checkpoint.
8. If a completed stage must change, mark REOPENED and record why before modifying it.

## 6. Definition of Done for the whole program
The program is complete only when:
- one persisted analytical contract drives Analysis UI, ReportsTab, PDF and XLSX;
- every valid operational analysis explicitly evaluates all 144 target compounds;
- unsupported/unknown/mixture/OOD outcomes are represented without forced identity;
- PPM and occupational quantitative interpretation remain withheld until M4 gates pass;
- exposure limits preserve authority/unit/duration semantics;
- canonical features and analytical plots are mathematically consistent;
- exact library/model/calibration/source provenance is reproducible per session;
- full HR066 integration regression passes;
- a documented release candidate is produced for field validation.


### Stage 3 completion — 2026-09-18
- Status: COMPLETE.
- Canonical app-side live MQ operational state implemented with immediate WARNING for a strong single-channel response and persistent multi-channel escalation.
- Warm-up, missing/invalid channels, clean-air baseline requirement, drift/degradation and de-escalation covered by focused tests.
- Live operational alerts remain non-identity and non-quantitative evidence.
- Railway validation deployment: f28aab6b-e7a3-4043-bf69-bde0bcd191f3 — SUCCESS.
- Validation passed focused unit tests, lintDebug and debug Java compilation.
- Firmware remained frozen; no Stage 3 firmware modification or integration was performed.

### Stage 4 start — 2026-09-18
- Status: IN_PROGRESS.
- Operational panel source: HAZROB-CHEM-144-v3 / ChemicalCatalog.COUNT.
- ChemCore144ScreeningEngine added and bound to chemcore-result/4 screening_144.
- Exactly one explicit row is produced per catalogue entry; unsupported coverage never fabricates detection or reportable ppm.


### Stage 4 completion — 2026-09-18
- Status: COMPLETE.
- ChemCore144ScreeningEngine is bound to chemcore-result/4 and emits exactly 144 explicit catalogue rows.
- Session-aware identity and quantitative gates are enforced; unsupported rows cannot fabricate detection or reportable ppm.
- Focused Railway unit-test command including ChemCore144ScreeningEngineTest: BUILD SUCCESSFUL in 31s.
- Final container packaging was still running when Stage 5 began; the Stage 4 acceptance test itself passed.
- Firmware remained frozen.

### Stage 5 start — 2026-09-18
- Status: IN_PROGRESS.
- Scope kept lean: consolidate existing inference/open-set/mixture/OOD/M4 quantitative gates; no duplicate inference stack.


### Stage 5 completion — 2026-09-18
- Status: COMPLETE by focused code audit; no redundant validation cycle added.
- Existing ChemCoreInferenceEngine retained as the single inference path.
- Confirmed fail-closed ordering for mixture/interference, open-set rejection, ambiguity, validated-domain rejection, M4 quantification, M3 identification, and screening fallback.
- Reportable ppm remains restricted to QUANTIFIED + M4; lower evidence fitted ppm remains diagnostic only.
- Firmware remained frozen.

### Stage 6 start — 2026-09-18
- Status: IN_PROGRESS.
- Scope: exposure and flammability interpretation using accepted ChemCore decisions only; keep occupational limits distinct (TWA/STEL/Ceiling/IDLH/LFL).


### Stage 6 completion — 2026-09-18
- Status: COMPLETE by focused integration audit; no redundant rebuild.
- Reused GasPpmExposureAssessment as the single fail-closed exposure/flammability interpretation path.
- Safety input remains gated to accepted M4 reportable ppm from ChemCore.
- ppm toxicology references remain separate from mass-concentration references; no silent unit conversion.
- TWA/STEL duration context, Ceiling, IDLH and LFL/UFL are kept distinct; LFL uses explicit vol%→ppmv ratio conversion.
- GasHazardPriority affects display ordering only and cannot alter analytical identity.
- Firmware remained frozen.

### Stage 7 start — 2026-09-18
- Status: IN_PROGRESS.
- Scope: bind Analysis UI directly to the persisted chemcore-result/4 decision and its 144 screening/safety evidence without recomputation.
