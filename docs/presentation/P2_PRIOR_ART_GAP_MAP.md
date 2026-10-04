# P2 — Prior-Art Gap Map for HazRob Master Deck

Status: **COMPLETE — slide-ready landscape**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`

## Objective
Condense the P2 literature/product survey into a presentation-safe gap map. This is **not** a legal novelty opinion. It identifies combinations and technical mechanisms that require deeper patent-oriented analysis in P3.

## What is clearly established in prior art

### Established A — Mobile robotic gas sensing
Mobile robots have been used for gas source localization, gas distribution mapping, emergency-response chemical sensing and industrial inspection. Therefore:
- `robot + gas sensor` is not a defensible novelty statement;
- `robot + e-nose` is also established prior art.

Representative evidence: Francis et al. (2022); Fan et al. (2019); mobile MOX hazardous-environment work; commercial ANYmal/ExRobotics platforms.

### Established B — Multi-sensor electronic noses
Sensor arrays, feature extraction, classification, transient features, concentration prediction and multi-task learning are established areas.
Therefore:
- `multiple MQ sensors + classifier` is not enough to establish an inventive contribution.

Representative evidence: Wang et al. (2022); Mei et al. (2025); Paredes et al. (2016); Oshovsky et al. (2026).

### Established C — Open-set / unknown-gas rejection
Open-set gas recognition and unknown-class rejection have peer-reviewed prior art, including drift-aware work.
Therefore:
- HazRob must not claim generic OOD/Open-Set gas recognition as novel.

Representative evidence: Qu et al. (2022), DOI `10.1016/j.snb.2022.131652`; later drift/open-set studies.

### Established D — Industrial robot gas monitoring
Ex-certified/industrial robots already support gas concentration monitoring, leak detection/localization and autonomous inspection.
Therefore:
- mobility, remote control, gas payloads and hazardous-area inspection are established commercial functions.

Representative systems: ANYmal X / Gas Guard; ExRobotics ExR.

### Established E — Professional multi-gas and optical monitoring
Portable multi-gas detectors, area monitors and OGI cameras are mature solution classes.
Therefore:
- HazRob should be framed as a different integrated analytical/robotic architecture, not as a simple replacement.

Representative systems: Dräger X-am 8000; Honeywell AreaRAE Plus; FLIR GF77.

---

## Candidate combination-level gaps to investigate in P3

### GAP-01 — Governed multi-method chemical inference chain
Candidate combination:
`strict Rs/R0 measurements → temporal features → joint inverse response-model fit → fingerprint evidence → OOD gate → conflict/mixture guard → lineage-aware evidence fusion → M0–M4 outcome gate`.

Question for P3:
Is this complete governed chain disclosed as a unified gas-analysis architecture, especially with independent reportability semantics?

### GAP-02 — M3 identity-only promotion separated from M4 quantitative authority
Candidate mechanism:
`multi-channel array-consistent identity may promote to M3`, while `reportable PPM remains withheld until device/reference M4 validation`.

Potential technical effect:
Reduces false quantitative claims and separates identity support from metrological authority.

Question for P3:
Is an explicit evidence ladder with a quantitative hard gate known in related e-nose/robotic gas systems?

### GAP-03 — OOD as a gate inside a broader evidence architecture
Candidate mechanism:
OOD does not merely reject unknowns; it constrains whether fingerprint evidence can create named identity and whether downstream quantification is allowed.

Question for P3:
Is this OOD→identity-tier→quantification-gate coupling disclosed in prior art?

### GAP-04 — Joint inverse concentration hypothesis across heterogeneous sensor evidence
Candidate mechanism:
For each chemical hypothesis, search one concentration that jointly explains the available multi-sensor log responses using evidence-dependent uncertainty/reliability weights.

Potential technical effect:
Provides an interpretable alternative to independent per-sensor PPM estimates and black-box classification.

Question for P3:
How close is this to classical weighted nonlinear least squares / maximum-likelihood sensor fusion, and what HazRob-specific constraints are actually inventive?

### GAP-05 — Lineage-aware evidence fusion
Candidate mechanism:
Curves, fingerprints and QSPR derivatives sharing one upstream source are not counted as independent proofs; correlated evidence receives capped corroborative weight.

Potential technical effect:
Prevents confidence inflation caused by reusing the same scientific source through several derived models.

Question for P3:
Can this be framed as a technical inference-integrity mechanism rather than generic software bookkeeping?

### GAP-06 — Controlled adaptive measurement policy / AMCI
Candidate mechanism:
State-machine-driven measurement can extend exposure, repeat after recovery, request additional evidence, or stop with Unknown/Insufficient Evidence according to stability/uncertainty/evidence state.

Potential technical effect:
Measurement duration/action is selected by analytical state rather than a single fixed exposure period.

Question for P3:
Distinguish from established active sensing, sequential experimental design, adaptive sampling and robot mission planning.

### GAP-07 — Research-to-runtime scientific provenance pipeline
Candidate mechanism:
`paper/dataset → source grade → sensor-model overlap → analyte mapping → response/fingerprint artifact → lineage → runtime evidence object → result envelope`.

Potential technical effect:
Auditable scientific provenance at inference time and explicit prevention of M2/M3 evidence masquerading as M4.

Question for P3:
Patent relevance may be weaker than engineering/governance value; assess carefully.

### GAP-08 — Robotic sampling state coupled to chemical inference validity
Candidate mechanism:
Motion/interlock, chamber state, environmental domain, sensor heater validity, physical time and recovery state affect whether chemical analysis is permitted.

Potential technical effect:
The robot’s physical state becomes a formal validity condition for analytical inference.

Question for P3:
Compare with robotic sensing safety interlocks and laboratory sample-state machines.

---

## Commercial-cost message approved for storyboard

Evidence supports the following restrained statement:

> Published examples show professional gas/chemical safety solutions spanning several thousand US dollars for portable multi-gas detection, low-five to high-five figures for transportable monitoring and specialized chemical/optical systems, while industrial inspection robots may be quote-based or offered through annual service models. HazRob is being developed as a different integrated robotic analytical architecture; the comparison is functional/contextual, not a claim of metrological equivalence.

Do not use an average market price unless a formal market study is later added.

---

## Recommended master-deck prior-art/comparator slides

1. `الدراسات السابقة: من الأنف الإلكتروني إلى الروبوت الشمّي`
   - visual timeline: e-nose → mobile robot olfaction → open-set → adaptive/multitask systems.

2. `ما الذي أصبح معروفًا بالفعل؟`
   - five tiles: robot gas sensing / e-nose / OOD / mapping / industrial inspection.

3. `الفجوة التي يستهدفها HazRob`
   - layered architecture emphasizing governed analytical integration rather than individual components.

4. `حلول تجارية قريبة — القدرات والتكلفة المنشورة`
   - Dräger / AreaRAE / FLIR / ANYmal X / ExRobotics.

5. `المقارنة العادلة: ليست كل الأدوات تحل المشكلة نفسها`
   - capability matrix with certification and M4 caveat prominently shown.

6. Optional appendix: `مصادر الأسعار والمنهجية`.

## P2 completion decision

P2 exit criteria are met:
- major related-work families are documented;
- close robotic/e-nose/Open-Set prior art is explicit;
- representative commercial comparators are documented;
- published price evidence is available for multiple categories;
- unsourced `expensive` language is prohibited;
- the gap map is ready to feed P3.

**Next phase: P3 — patent-oriented innovation framing.**
