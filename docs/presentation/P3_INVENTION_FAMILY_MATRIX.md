# P3 — HazRob Candidate Invention-Family Matrix

Status: **IN PROGRESS — engineering invention framing**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: HazRob 0.10.2 / HR069 RC2

> This document is an engineering/presentation analysis, not a legal patentability opinion. `Candidate invention family` means a technical mechanism/composite architecture worth prior-art and counsel review; it does not assert novelty, inventive step, ownership scope, or grantability.

## Evaluation scale

- **Implementation state:** IMPLEMENTED / RESEARCH-SUPPORTED / PROPOSED / FUTURE.
- **Engineering distinctiveness priority:** HIGH / MEDIUM / SUPPORTING. This is presentation priority, not patent strength.
- **Disclosure sensitivity:** HIGH / MEDIUM / LOW for master-vs-public deck planning.

---

## IF-01 — AMCI: Adaptive Multisensor Chemical Interrogation

**Engineering distinctiveness priority:** HIGH  
**Implementation state:** IMPLEMENTED research policy + controlled runtime state machine; scientific external validation incomplete  
**Disclosure sensitivity:** HIGH

### Technical problem
Fixed-duration gas measurements can stop too early, waste time after convergence, or produce weak/ambiguous evidence when sensor dynamics, concentration, recovery and environmental conditions differ between sessions.

### Implemented mechanism
- controlled session modes and state machine;
- states include CONDITION, BASELINE, EXPOSURE, STABILITY_CHECK, FEATURE_EXTRACTION, IDENTITY, OPEN_SET_GATE, QUANTIFICATION, CONSISTENCY_CHECK, REPORT and RECOVERY;
- analysis can extend exposure, repeat after recovery, seek additional confirmation or end with Unknown/Insufficient Evidence according to policy;
- stationary/motion interlock constrains controlled analytical states;
- AMCI policy currently includes ranking/entropy/coverage/stability/exposure/repeat limits.

### Input → process → output
`live multichannel response + session state + stability + coverage + ranking ambiguity → policy decision → continue / extend / repeat / recover / confirm / terminate → new evidence or fail-closed outcome`

### Technical effect
- adapts measurement action to analytical state rather than only elapsed time;
- reduces premature commitment under ambiguity;
- can avoid unnecessary exposure time when evidence is sufficient;
- creates an auditable closed-loop relationship between inference state and the next measurement action.

### Prior-art overlap
Active sensing, adaptive sampling, sequential experimental design and mobile-robot mission planning are established. E-nose systems with adaptive/self-calibration also exist.

### Combination-level differentiator to investigate
Whether the **specific coupling** of gas-sensor session phases, temporal-feature readiness, candidate ambiguity/OOD, evidence tiers and repeat/recovery logic into one chemical interrogation controller is disclosed in close prior art.

### Public-deck rule
Explain the adaptive loop and state machine. Do **not** disclose every policy threshold in the public conference derivative before P8 review.

---

## IF-02 — Joint Multi-Sensor Inverse Chemical / Concentration Solver

**Engineering distinctiveness priority:** HIGH  
**Implementation state:** IMPLEMENTED  
**Disclosure sensitivity:** HIGH

### Technical problem
Independent per-sensor PPM estimates can conflict because cross-sensitive MOX sensors respond to multiple chemicals and have different evidence quality, uncertainty and valid concentration domains.

### Implemented mechanism
For each candidate chemical:
- collect available response models across the array;
- express observations and model predictions in log10 response space;
- search for **one concentration** that jointly explains the available sensor responses;
- standardize residuals by model uncertainty proxy;
- weight contributions by evidence tier/reliability and penalize out-of-domain use;
- rank chemical hypotheses from their joint multi-channel fit;
- retain residual/evidence observations for audit.

### Simplified analytical form
`z_i(C) = [log10(Rs/R0)_observed,i − μ_i(C)] / σ_i`

`Loss(C) = Σ(w_i z_i²) / Σ(w_i)`

The solver searches the candidate concentration domain and refines the best joint fit.

### Technical effect
- forces one chemical/concentration hypothesis to explain the array coherently;
- exposes contradictory channels instead of silently averaging them;
- allows mixed source quality while preserving evidence tier and domain information;
- produces interpretable residuals and a fit trace.

### Prior-art overlap
Weighted nonlinear least squares, maximum-likelihood estimation, sensor fusion, e-nose regression and concentration prediction are established.

### Combination-level differentiator to investigate
The **heterogeneous evidence-tier response-model registry + one-concentration joint fit + domain/reliability weighting + downstream evidence/reportability gating** as a unified chemical inference mechanism.

### Public-deck rule
Show the conceptual equation and loss landscape. Keep implementation search-grid/refinement constants in master-only notes until P8.

---

## IF-03 — Open-Set/OOD + Multi-Channel Fingerprint + Evidence-Tier Identity Gate

**Engineering distinctiveness priority:** HIGH  
**Implementation state:** IMPLEMENTED; independent HR069 OOD threshold validation intentionally deferred  
**Disclosure sensitivity:** HIGH

### Technical problem
Closed-set classifiers can force an unknown chemical into the nearest known class. Conversely, a strong fingerprint match should not automatically become a quantitative device-calibrated claim.

### Implemented mechanism
- feature-coverage gate;
- Open-Set/OOD distance gate;
- multi-channel fingerprint match and runner-up separation;
- canonical chemical UID consistency checks;
- accepted fingerprint can promote identity only to **M3_ARRAY_CONSISTENT** under gates;
- OOD reject/missing evidence prevents named identity promotion;
- M3 identity remains non-quantitative;
- reportable PPM requires separate M4 gate.

### Input → process → output
`feature vector → coverage → OOD acceptance → fingerprint candidate + separation → UID consistency → M3 identity-only result → separate M4 quantitative decision`

### Technical effect
- reduces forced classification of unknowns;
- separates identity confidence from metrological authority;
- makes unknown/insufficient-evidence outcomes first-class outputs;
- prevents exact fingerprint matching from bypassing OOD or quantitative validation.

### Prior-art overlap
Open-set gas recognition is established (e.g. Qu et al., 2022). Fingerprint classification is established.

### Combination-level differentiator to investigate
The **decision coupling** `OOD acceptance → fingerprint promotion → M3 identity-only → independent M4 reportability gate`, including fail-closed behavior and canonical UID consistency.

### Public-deck rule
Show decision chain and safety rationale; do not present current threshold as independently validated until deferred OOD campaign is completed.

---

## IF-04 — Evidence-Tiered Identity / Quantification Separation (M0–M4)

**Engineering distinctiveness priority:** HIGH  
**Implementation state:** IMPLEMENTED  
**Disclosure sensitivity:** MEDIUM

### Technical problem
Chemical sensing systems can blur the distinction between catalogue knowledge, literature-supported response, pattern consistency and device-specific quantitative calibration, leading to overconfident identity or PPM statements.

### Implemented mechanism
ChemCore separates evidence into:
- M0 — catalogue only;
- M1 — molecularly inferred / QSPR;
- M2 — literature/manufacturer supported;
- M3 — array-consistent identity;
- M4 — HazRob device/reference-gas validated quantitative evidence.

Outcome/reportability logic uses the tier rather than simply the candidate score.

### Technical effect
- preserves scientific provenance and calibration authority;
- prevents literature curves from masquerading as device calibration;
- allows research-supported identification while withholding professional PPM;
- supports explainable progression from knowledge to validated measurement.

### Prior-art overlap
Evidence hierarchies and calibration grades exist broadly in metrology, diagnostics and safety engineering.

### Combination-level differentiator to investigate
The use of an explicit evidence ladder **inside** a robotic multi-sensor chemical inference engine, directly controlling named identity, PPM reportability and report semantics.

### Public-deck rule
Safe to explain clearly; it strengthens scientific credibility. Avoid calling M4 a regulatory certification unless such certification exists.

---

## IF-05 — Lineage-Aware Chemical Evidence Fusion

**Engineering distinctiveness priority:** HIGH  
**Implementation state:** IMPLEMENTED  
**Disclosure sensitivity:** HIGH

### Technical problem
A single upstream paper/dataset may generate a curve, derived fingerprint and QSPR-trained artifact. Treating these derivatives as independent evidence inflates apparent confidence.

### Implemented mechanism
- retain individual interpretable evidence observations;
- group immediate evidence dependence;
- track upstream scientific lineage;
- de-duplicate/cap correlated derived evidence;
- let full-quality independent evidence drive the analytical confidence index;
- degraded or out-of-domain evidence becomes support-only/capped corroboration;
- ranking/posterior semantics remain separate from analytical confidence.

### Technical effect
- prevents double counting of reused scientific evidence;
- improves auditability of why confidence changed;
- preserves useful corroboration without representing correlated derivatives as independent proofs;
- supports traceable scientific inference.

### Prior-art overlap
Data provenance, Bayesian dependence modelling, evidence graphs and correlated-feature handling are established fields.

### Combination-level differentiator to investigate
Application of **source-lineage dependence controls to chemical sensor inference**, especially where response curves, fingerprints and molecular/QSPR derivatives share the same scientific ancestry.

### Public-deck rule
Show a simple lineage tree and `one paper ≠ three independent proofs`. Keep exact bonuses/caps master-only pending P8.

---

## IF-06 — Robotic Sampling / Measurement State Coupled to Analytical Validity

**Engineering distinctiveness priority:** MEDIUM–HIGH  
**Implementation state:** IMPLEMENTED in several gates/interlocks; some safety policies remain under refinement  
**Disclosure sensitivity:** MEDIUM

### Technical problem
A chemical result may be analytically invalid if the robot moves during a controlled sampling phase, if the heater cycle is invalid, if environmental data are outside the response-model domain, or if the session lacks stable baseline/recovery conditions.

### Implemented mechanism
Analytical readiness/validity can depend on:
- robot stationary state / motion interlock;
- MQ power and valid-channel count;
- device R0 availability;
- environmental-domain acceptance;
- heater-cycle validity for affected sensors;
- physical time / session state;
- baseline/exposure/stability/recovery progression;
- calibration-health and drift state.

### Technical effect
- converts robot physical state into a formal analytical-quality condition;
- reduces inference from mechanically/thermally invalid samples;
- supports reproducible sample acquisition and auditable reason-for-withholding.

### Prior-art overlap
Robotic interlocks, laboratory sequencing and instrument readiness gates are established.

### Combination-level differentiator to investigate
The specific **cross-layer coupling** between robot motion/sampling state and multi-tier chemical identity/quantification authority.

### Public-deck rule
Show the concept. Note current RC2 safety-policy divergence for IR/ultrasonic automatic stopping separately; do not imply all navigation safety logic is certified.

---

## IF-07 — Research-to-Runtime Scientific Provenance Pipeline

**Engineering distinctiveness priority:** SUPPORTING–MEDIUM  
**Implementation state:** IMPLEMENTED governance/data structures  
**Disclosure sensitivity:** LOW–MEDIUM

### Technical problem
Scientific sensor models often enter software without enough traceability to the original paper, sensor model, analyte, concentration domain, missing channels or validation grade.

### Implemented mechanism
`paper/dataset → source index/grade → exact sensor-model overlap → analyte/UID mapping → curve/fingerprint/model artifact → lineage metadata → runtime evidence observation → ResultEnvelope/report`

Policy examples include:
- same-model evidence vs different-sensor transfer;
- never synthesize missing channels;
- reportable PPM still requires HR069 M4;
- build-harness triggers govern external data/integration.

### Technical effect
- makes external scientific evidence traceable into runtime decisions;
- prevents silent upgrading of transfer evidence;
- supports reproducible audits and future model replacement.

### Prior-art overlap
MLOps, model cards, data lineage and regulated software provenance are established.

### Combination-level differentiator to investigate
Likely stronger as an enabling architecture/claim limitation than a standalone invention; assess whether gas-sensor-specific source grading and runtime reportability control creates a patent-relevant technical effect.

### Public-deck rule
Very useful for funding/scientific credibility; low sensitivity if exact internal file paths/build procedures are omitted.

---

## IF-08 — Fail-Closed Analytical Outcome Architecture

**Engineering distinctiveness priority:** MEDIUM–HIGH  
**Implementation state:** IMPLEMENTED  
**Disclosure sensitivity:** MEDIUM

### Technical problem
Safety-critical analytical software may be pressured to always return a named chemical even when data are weak, contradictory, out of domain or consistent with interference.

### Implemented mechanism
Explicit outcomes include:
- QUANTIFIED;
- IDENTIFIED;
- SCREENING;
- UNKNOWN/OOD;
- INSUFFICIENT_EVIDENCE;
- POSSIBLE_MIXTURE_OR_INTERFERENCE;
- INVALID_MEASUREMENT.

Conflict, mixture, OOD, domain and calibration gates can **downgrade or withhold** identity/PPM rather than forcing a result.

### Technical effect
- prevents forced certainty;
- makes failure/ambiguity machine-readable and reportable;
- preserves safety separation between screening and validated quantitative interpretation.

### Prior-art overlap
Fail-safe/fail-closed design and uncertainty-aware classification are established safety principles.

### Combination-level differentiator to investigate
Whether the exact outcome hierarchy tied to OOD, M0–M4, mixture residuals and M4 reportability is distinctive as part of the broader ChemCore architecture.

---

# Cross-family architecture

The strongest engineering narrative is not any single known component. It is the **closed, governed analytical loop**:

`robot/sample state → accepted multichannel measurement → normalized temporal fingerprint → candidate/inverse models → OOD/fingerprint identity → conflict/mixture checks → lineage-aware evidence fusion → M0–M4 authority → fail-closed outcome → AMCI next action / report`

This is the architecture to emphasize in the master deck and to subject to focused patent prior-art analysis.

# P3 next action

Create a compact `Patent Candidate Map` with:
- strongest families for front-stage presentation;
- supporting families;
- implemented vs proposed layers;
- prior-art overlap warnings;
- public/master-only disclosure classification;
- recommended diagrams and one-sentence invention statements.
