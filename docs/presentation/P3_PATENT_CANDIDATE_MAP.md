# P3 — HazRob Patent-Candidate Map for Master Presentation

Status: **COMPLETE — engineering framing ready for storyboard**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`

> Engineering map only. This is not a patent search report, freedom-to-operate opinion, legal claim set, or conclusion of novelty/inventive step.

## Front-stage invention families

### A — Adaptive Multisensor Chemical Interrogation (AMCI)
**One-sentence engineering statement:**
A controlled chemical-measurement controller that uses multichannel response stability, evidence state and analytical ambiguity to select the next measurement action—continue, extend, recover, repeat, seek confirmation, or terminate—rather than relying only on a fixed exposure duration.

**Why it belongs front-stage:**
- easy to visualize as a closed-loop intelligence mechanism;
- directly links sensing to decision and then back to sensing;
- differentiates HazRob from a passive data logger or one-shot classifier.

**Prior-art caution:** active sensing/adaptive sampling exist; novelty cannot be asserted without focused search on this specific chemical-session coupling.

**Public disclosure:** HIGH-LEVEL SAFE; exact thresholds/policy constants MASTER-ONLY pending P8.

**Preferred visual:** circular loop with state-machine inset.

---

### B — Joint Multi-Sensor Inverse Chemical / Concentration Inference
**One-sentence engineering statement:**
For each chemical hypothesis, ChemCore searches for one concentration that jointly explains the available multi-sensor log responses, weighting each response by uncertainty, evidence authority and domain validity, then preserves the residual structure for downstream consistency checks.

**Why it belongs front-stage:**
- scientifically intuitive and mathematically defensible;
- shows that HazRob does not simply average individual MQ PPM estimates;
- provides a bridge between cross-sensitive sensors and interpretable candidate ranking.

**Prior-art caution:** nonlinear least squares and sensor-fusion inverse models are established; the combination with evidence tiers/domain/reportability must be the focus.

**Public disclosure:** CONCEPT/EQUATION SAFE; implementation grid/refinement constants MASTER-ONLY.

**Preferred visual:** observed-vs-predicted sensor curves + concentration-loss landscape.

---

### C — Open-Set + Fingerprint + Evidence-Tier Gate
**One-sentence engineering statement:**
An unknown-aware decision chain in which a multi-channel fingerprint can promote a named chemical identity only after coverage and Open-Set/OOD acceptance, while the promoted M3 identity remains quantitatively blocked until an independent M4 device-validation gate is satisfied.

**Why it belongs front-stage:**
- directly addresses forced-classification risk;
- combines unknown rejection, fingerprint identity and metrological authority separation;
- strong safety/explainability narrative.

**Prior-art caution:** open-set gas recognition and fingerprint classification are prior art; the candidate distinction is their coupling to evidence tiers and quantitative authority.

**Public disclosure:** ARCHITECTURE SAFE; numerical thresholds/validation details MASTER-ONLY until P8 and deferred OOD campaign.

**Preferred visual:** funnel/gate diagram: coverage → OOD → fingerprint → M3 → M4.

---

### D — Lineage-Aware Evidence Fusion
**One-sentence engineering statement:**
A chemical evidence-fusion mechanism that tracks upstream scientific lineage so multiple artifacts derived from the same paper/dataset cannot be counted as independent proofs, while independent high-quality evidence and capped corroboration remain separately visible.

**Why it belongs front-stage:**
- unusual and highly explainable;
- supports scientific integrity and auditability;
- makes the project attractive to reviewers who care about trustworthy AI/scientific software.

**Prior-art caution:** provenance/dependence-aware fusion is broadly known; gas-sensor-specific technical effect must be investigated.

**Public disclosure:** PRINCIPLE SAFE; exact weighting/caps MASTER-ONLY.

**Preferred visual:** one source branching into curve/fingerprint/QSPR, then recombining with de-duplication barrier.

---

### E — Robot-State-Coupled Analytical Validity
**One-sentence engineering statement:**
A cross-layer measurement architecture in which robot motion, chamber/session phase, sensor heater validity, environmental domain, R0 readiness and drift/calibration health can determine whether chemical identity or quantification is analytically permitted.

**Why it belongs front-stage:**
- links robotics and chemistry at a deeper level than mounting sensors on a vehicle;
- demonstrates system-level integration;
- provides an explicit technical safety effect.

**Prior-art caution:** instrumentation interlocks and robotic safety states are known; focus on the analytical-validity coupling rather than generic E-stop/navigation logic.

**Public disclosure:** HIGH-LEVEL SAFE; exact interlock logic MASTER-ONLY where security/safety sensitive.

**Preferred visual:** robot physical state on left, inference-authority gate on right.

---

## Supporting invention families / architecture enablers

### S1 — M0–M4 Evidence-Tiered Identity / Quantification Separation
**Role:** presentation-strengthening architecture and possible claim limitation/supporting family.

One-sentence statement:
`ChemCore separates catalogue/inferred/literature/array-consistent/device-validated evidence so identity support can advance without automatically authorizing quantitative PPM.`

Public disclosure: SAFE and recommended.

### S2 — Fail-Closed Analytical Outcome Architecture
**Role:** safety/decision-governance family.

One-sentence statement:
`The analytical engine can return Unknown, Screening, Insufficient Evidence, Possible Mixture/Interference or Invalid Measurement instead of being forced to name or quantify a chemical.`

Public disclosure: SAFE.

### S3 — Research-to-Runtime Scientific Provenance
**Role:** trust/governance enabler; likely weaker standalone patent family but strong funding/conference differentiator.

One-sentence statement:
`External literature/datasets are converted into governed sensor/analyte evidence artifacts with source grade, domain, missing-channel policy, lineage and runtime reportability restrictions.`

Public disclosure: SAFE at conceptual level.

---

## Candidate architecture stack

### Layer 1 — Physical acquisition
Robot movement, sampling chamber, MQ array, environmental and safety sensors, telemetry.

### Layer 2 — Measurement validity
R0 readiness, channel quality, heater phase, environment domain, stationary state, physical time, session phase.

### Layer 3 — Analytical representation
Rs/R0, log10 transforms, temporal features, spatial/temporal fingerprints.

### Layer 4 — Multi-method inference
Reference response models, inverse solver, fingerprint classifier, QSPR transfer, screening library.

### Layer 5 — Unknown/conflict governance
OOD/Open-Set, candidate separation, specialist conflicts, mixture/interference residual guards.

### Layer 6 — Evidence authority
M0–M4 tiers, scientific lineage, analytical confidence/support index, calibration health.

### Layer 7 — Closed-loop decision
Outcome + AMCI next action + reporting/withholding reason.

The master deck should visually present the **stack as the integrated invention narrative**, while avoiding a claim that every layer is individually novel.

---

## Implemented vs proposed map

| Mechanism | Current status | Deck label |
|---|---|---|
| Rs/R0/log10 pipeline | implemented | IMPLEMENTED |
| 17 temporal features per sensor | implemented | IMPLEMENTED |
| controlled ChemCore state machine | implemented | IMPLEMENTED |
| AMCI research policy | implemented research policy | IMPLEMENTED / RESEARCH POLICY |
| inverse multi-sensor solver | implemented | IMPLEMENTED |
| research/reference fingerprints | implemented | IMPLEMENTED / RESEARCH-SUPPORTED |
| Open-Set/OOD gate | implemented; threshold external validation pending | IMPLEMENTED / VALIDATION PENDING |
| M3 fingerprint promotion | implemented | IMPLEMENTED |
| M4 quantitative calibration | architecture implemented; analyte/device campaigns incomplete | GATE IMPLEMENTED / VALIDATION INCOMPLETE |
| lineage-aware evidence fusion | implemented | IMPLEMENTED |
| mixture/interference guard | implemented | IMPLEMENTED GUARD |
| arbitrary mixture component decomposition | not implemented | FUTURE WORK |
| environment numerical compensation | intentionally not enabled absent device experiments | FUTURE / VALIDATION REQUIRED |
| full external OOD validation | deferred task documented | VALIDATION PENDING |
| Ex/ATEX robot certification | not implemented | FUTURE / PRODUCTIZATION |

---

## Patent/public-disclosure sensitivity map

### Public-conference safe by default
- global need and problem;
- high-level HazRob architecture;
- sensor-array concept;
- Rs/R0 concept;
- fingerprint concept;
- M0–M4 ladder;
- fail-closed outcome concept;
- high-level OOD concept;
- high-level AMCI loop;
- products/prior art with sources;
- applications and roadmap.

### Master-only until P8 review
- exact AMCI thresholds and repeat/extension constants;
- detailed inverse-solver search/refinement constants;
- exact evidence-fusion bonuses/caps;
- detailed UID/provenance implementation internals;
- specific software control paths that could define claim boundaries;
- security/remote-control implementation details;
- any unpublished M4 calibration protocol/result.

---

## Slide-ready Patent Candidate Map

Center: **HazRob — Closed-loop, evidence-governed robotic chemical inference**

Five primary spokes:
1. **AMCI adaptive interrogation**
2. **Joint inverse multi-sensor solver**
3. **OOD → Fingerprint → M3 → M4 gated identity/quantification**
4. **Lineage-aware evidence fusion**
5. **Robot/sample state coupled to analytical validity**

Supporting ring:
- M0–M4 evidence ladder
- fail-closed outcomes
- scientific provenance pipeline

Footer disclaimer:
`عائلات ابتكارية مرشحة للدراسة والحماية — وليست ادعاءات براءة نهائية قبل استكمال بحث السوابق والصياغة القانونية.`

---

## P3 completion decision

P3 exit criteria are met for storyboard purposes:
- technical problems and mechanisms are defined;
- implemented/proposed boundaries are explicit;
- known prior-art overlap is recorded;
- front-stage vs supporting candidate families are selected;
- public/master-only disclosure classes are defined;
- a slide-ready candidate map is specified.

**Next phase: P4 — final slide-by-slide scientific storyboard.**
