# HazRob Master Deck — Production Checkpoint

Status: **ACTIVE**
Tracking issue: **#9**
Branch: `docs/hazrob-master-deck`
Engineering baseline: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
Presenters / project owners: **شهاب الصهباني، مجدي ذيبان**
Primary deliverable: Arabic RTL master presentation for patent/invention discussions, scientific conferences, and funding presentations.

## Fixed presentation constraints

- Arabic-first, full RTL paragraphs and tables.
- 16:9 widescreen.
- No slide numbering.
- Large conference-readable typography: titles normally ≥30–36 pt; body normally 22–28 pt; tables normally ≥18–22 pt.
- Prefer diagrams, charts, state machines, fingerprints, process maps, comparison matrices and timelines over dense prose.
- Tables must use wide cells/columns and remain readable on a projector.
- Neutral/plain backgrounds in this technical master build; final branding/backgrounds are deferred to the project owners.
- PNG is acceptable; GIF only when motion materially improves explanation and compatibility is verified.
- Every external statistic, market price, prior-art statement and scientific performance statement must be traceable.
- Always distinguish **IMPLEMENTED / VALIDATED / RESEARCH-SUPPORTED / PROPOSED / FUTURE WORK**.
- Never present M3 as M4; ranking weight as calibrated probability; or literature/reference PPM as HR069 device M4 validation.
- Patent-oriented wording may identify **candidate invention families** but must not assert granted patent status, legal novelty or non-obviousness.

## Mandatory special slides

- terminology/abbreviations: Rs, R0, Rs/R0, ADC, PPM, MOX/MOS, M0–M4, OOD, Open-Set, AMCI, QSPR, UID/chemical_uid, SNR, AUC, UX, UI, API, MQTT, ESP32, and confidence/ranking/probability distinctions;
- blank/light presenter slide: `أصحاب المشروع / مقدمو العرض` with **شهاب الصهباني** and **مجدي ذيبان**;
- global need and human/asset/environment protection using WHO/ILO and Yemen context;
- prior studies / related work;
- comparable commercial products and sourced cost evidence;
- candidate invention-family map;
- APA 7 references (use multiple slides if needed for readability);
- final `شكرًا لكم` slide.

## Authoritative source hierarchy

1. RC2 source/runtime/tests at the pinned baseline.
2. HazRob full-system analysis report.
3. ChemCore system-analysis report.
4. `research_data/hr069_analytical_library/`, `external_mq_fingerprints/`, `chemical_coverage_hr069/`, `reference_bank/`.
5. `HazRob/build-harness` governance/evidence integration/release authority.
6. Original peer-reviewed papers/datasets cited by project source indices.
7. WHO/ILO and Yemen-context scientific literature.
8. Manufacturer pages and traceable public/distributor/procurement price sources for P2.

---

# Phase status

| Phase | Description | Status |
|---|---|---|
| P0 | Governance, baseline, constraints, checkpoint | COMPLETE |
| P1 | Scientific source inventory and APA 7 bibliography | COMPLETE |
| P2 | Prior art, related work, commercial comparators and cost evidence | COMPLETE |
| P3 | Patent-oriented innovation framing | **IN PROGRESS** |
| P4 | Final slide-by-slide scientific storyboard | NOT STARTED |
| P5 | Visual specification and figure production | NOT STARTED |
| P6 | First Arabic RTL PPTX build | NOT STARTED |
| P7 | Scientific/technical audit against RC2/ChemCore | NOT STARTED |
| P8 | Patent/public-disclosure audit | NOT STARTED |
| P9 | Conference readability and visual QA | NOT STARTED |
| P10 | Final PPTX/PDF, source manifest, hashes, completion | NOT STARTED |

---

## P0 — COMPLETE

Evidence:
- Issue #9 created.
- Branch `docs/hazrob-master-deck` created from pinned RC2 baseline.
- presentation rules and checkpoint protocol established.

## P1 — COMPLETE

Artifacts:
- `docs/presentation/P1_SOURCE_REGISTER_APA7.md`
- `docs/presentation/P1_CLAIM_TO_SOURCE_MATRIX.md`
- `docs/presentation/P1_AUTHOR_AND_YEMEN_CONTEXT_SOURCES.md`

Key decisions:
- project implementation evidence is separated from external scientific evidence;
- presenter ORCID `0000-0002-6144-4209` verified for use;
- ARID exact numeric ID and ResearcherID remain excluded until independently verified;
- Yemen occupational/environmental papers support context only, not HazRob validation;
- WHO/ILO statistics are wording-gated to prevent causal overstatement;
- key ChemCore/MQ/e-nose papers have working APA/DOI records;
- metadata conflicts are logged for P10 cleanup and do not block storyboard work.

## P2 — COMPLETE

Artifacts:
- `docs/presentation/P2_PRIOR_ART_MATRIX.md`
- `docs/presentation/P2_COMPARABLE_PRODUCTS_COSTS.md`
- `docs/presentation/P2_PRIOR_ART_GAP_MAP.md`

Key findings:
- mobile robotic gas sensing and mobile e-noses are established prior art;
- multi-sensor electronic noses, transient fingerprints and concentration prediction are established prior art;
- Open-Set/unknown-gas rejection is established prior art;
- industrial robots already support gas monitoring/leak detection, including Ex-certified platforms;
- professional portable detectors, area monitors and OGI cameras are mature comparator classes;
- HazRob patent framing therefore must focus on specific architecture/orchestration/decision mechanisms rather than `robot + gas sensor` or generic `electronic nose`.

Cost evidence registered from published/current sources includes representative examples:
- professional portable multi-gas instrument: several-thousand-USD range in sourced X-am 8000 records;
- AreaRAE Plus configurations: low-five-figure USD range, with a rapid-deployment-kit example much higher;
- FLIR GF77 OGI configurations: roughly high-USD-30k through low-USD-60k range in current distributor listings;
- specialized chemical detection/identification public-procurement examples extend into tens of thousands of USD;
- ExRobotics publishes RaaS starting at EUR 35k/year for a basic ExR-1 service package;
- ANYmal/ANYmal X is quote-based, so no fabricated price is used.

P2 gap-map candidates handed to P3:
- governed multi-method chemical inference chain;
- M3 identity-only promotion separated from M4 quantitative authority;
- OOD as a gate inside evidence/quantification architecture;
- joint inverse concentration hypothesis across multiple sensor response models;
- lineage-aware evidence fusion;
- AMCI controlled/adaptive measurement policy;
- research-to-runtime provenance pipeline;
- robotic physical/sampling state as analytical-validity condition.

---

# Current phase — P3 Patent-oriented innovation framing

## Purpose
Identify candidate invention families and technical differentiators **without** making a legal novelty conclusion.

## Required P3 work

For every candidate family build:
1. technical problem;
2. implemented mechanism/components;
3. input/process/output chain;
4. technical effect / safety effect;
5. exact evidence in RC2/ChemCore;
6. related-prior-art overlap from P2;
7. differentiating combination to investigate;
8. status: implemented / research-supported / proposed / future;
9. public-disclosure sensitivity;
10. recommended slide visualization.

Candidate families currently under evaluation:
- **IF-01 AMCI — Adaptive Multisensor Chemical Interrogation**;
- **IF-02 Joint Multi-Sensor Inverse Chemical/Concentration Solver**;
- **IF-03 Open-Set/OOD + fingerprint + evidence-tier identity gating**;
- **IF-04 Lineage-aware chemical evidence fusion and fail-closed reporting**;
- **IF-05 Robotic sampling/measurement orchestration coupled to inference validity**;
- **IF-06 Research-to-runtime traceable scientific provenance**;
- **IF-07 Evidence-tiered identity/quantification separation (M0–M4)**;
- any additional family discovered during detailed P3 decomposition.

## P3 exit criteria

- every family has problem → mechanism → technical effect → evidence → prior-art overlap → differentiator;
- no generic component is presented as the invention by itself;
- implemented and proposed material are visibly separated;
- a slide-ready `Patent Candidate Map` exists;
- sensitive master-only material is tagged for P8.

---

# Remaining phases

## P4 — Storyboard
Freeze approximately 60–75 evidence-driven slides (adjust if necessary). Every slide must have title, one scientific message, source IDs, visual type, and presenter note. Include glossary, presenter placeholder, WHO/ILO/Yemen need, prior art/products, full HazRob/ChemCore methodology, candidate invention map, roadmap, references, thanks.

## P5 — Visual engineering
Produce reusable architecture diagrams, sensor/fingerprint plots, state machines, flowcharts, OOD space, inverse-solver graphics, M0–M4 ladder, evidence-lineage graph, comparator/cost charts and innovation-stack diagrams.

## P6 — PPTX build
Build complete Arabic RTL 16:9 deck with large text, neutral background, no slide numbers and safe table/image placement.

## P7 — Scientific audit
Verify hardware, firmware, connectivity, telemetry, R0/Rs, features, ranking, OOD, fingerprints/M3, M4, QSPR, mixtures, evidence fusion, AMCI, reports and safety semantics against actual RC2.

## P8 — Patent/public disclosure audit
Separate master/confidential detail from future public-conference derivative; remove legal overclaim and flag counsel-review items.

## P9 — Visual QA
Render every slide, inspect Arabic shaping/RTL, equations, tables, clipping/overlap, source readability and projector-scale legibility; correct and rerender.

## P10 — Finalization
Deliver master `.pptx`, review `.pdf`, source/figure manifest, APA register and SHA-256 hashes; update Issue #9 and close only after final QA.

---

# Resume checkpoint

- Current phase: **P3**
- Last completed phase: **P2**
- Overall status: **ACTIVE**
- Last stable engineering baseline: `5250ea769615ec82c5dc944576ea3042736b3db2`
- Last completed artifact: `docs/presentation/P2_PRIOR_ART_GAP_MAP.md`
- Known blockers: **none for P3**; ARID/ResearcherID and two publication-metadata cleanup items remain non-blocking for P10.
- Exact next action: **construct the P3 invention-family matrix and Patent Candidate Map using P2 gap candidates, then classify each mechanism as implemented/research-supported/proposed/future and identify public-disclosure sensitivity.**

If work resumes in a new conversation, read this file and Issue #9 first and continue from `Exact next action`. Do not repeat P0–P2 unless the engineering baseline changes.
