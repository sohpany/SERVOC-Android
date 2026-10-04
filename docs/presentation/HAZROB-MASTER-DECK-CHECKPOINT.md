# HazRob Master Deck — Phased Production Plan & Checkpoint

Status: ACTIVE
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
Primary deliverable: Arabic RTL master presentation for patent/invention discussions, scientific conferences, and funding presentations.
Presenters: شهاب الصهباني، مجدي ذيبان

## Non-negotiable presentation rules

- Arabic first; full RTL paragraph and table direction.
- 16:9 widescreen.
- No slide numbering.
- Large conference-readable typography. Working minimums: titles 30–36 pt or larger; main body 22–28 pt; tables normally 18–22 pt minimum unless a justified dense appendix slide is used.
- Prefer diagrams, process maps, charts, fingerprints, state machines, comparison matrices, timelines, and visual evidence over dense prose.
- Tables must be designed to use the available slide width; columns/cells must be sized for readable Arabic rather than compressed mechanically.
- Neutral/plain backgrounds in the master technical build; final background branding will be applied later by the project owners.
- PNG assets may be used; GIF only where motion materially improves scientific explanation and presentation software compatibility is verified.
- Every external number, market price, prior-art statement, or scientific performance statement must have traceable source metadata.
- Distinguish clearly: IMPLEMENTED / VALIDATED / RESEARCH-SUPPORTED / PROPOSED / FUTURE WORK.
- Never present M3 as M4, ranking weight as calibrated probability, or literature/reference PPM as HR069 M4 validation.
- Patent-oriented wording must identify candidate inventive concepts without asserting that a patent has been granted or that novelty/non-obviousness has been legally established.

## Required special slides

### Terminology and abbreviations
Include a dedicated Arabic glossary slide, or two slides if readability requires, covering at minimum:
- Rs — current sensor resistance
- R0 — reference resistance/baseline normalization reference
- Rs/R0 — normalized sensor ratio
- ADC — analog-to-digital conversion
- PPM — parts per million
- MOX/MOS — metal-oxide semiconductor gas sensor
- M0–M4 — ChemCore evidence tiers
- OOD — Out-of-Distribution
- Open-Set — open-set recognition
- AMCI — Adaptive Multisensor Chemical Interrogation
- QSPR — Quantitative Structure–Property Relationship
- UID / chemical_uid — stable chemical identity identifier
- SNR — signal-to-noise ratio
- AUC — area under curve
- UX — User Experience
- UI — User Interface
- API — Application Programming Interface
- MQTT — Message Queuing Telemetry Transport
- ESP32 — embedded controller platform used by HazRob
- CI/Confidence terminology used in the deck, with explicit note when an index is not a calibrated probability.

### Project owners / presenters
Dedicated slide titled `أصحاب المشروع / مقدمو العرض` containing only placeholders for:
- شهاب الصهباني
- مجدي ذيبان

Keep the slide intentionally incomplete so photos, qualifications, roles, affiliations, and contact details can be added later without redesign.

### Prior work and comparable products
One or more slides for:
- electronic noses and multi-sensor gas-identification research;
- robot-assisted hazardous-gas inspection/sampling;
- open-set/OOD sensing approaches;
- adaptive/active sensing;
- commercial multi-gas detectors, area monitors, gas-imaging/inspection platforms, mobile robots and other relevant products;
- documented market or quotation prices where traceable, otherwise `price on request` or clearly labeled indicative price.

### Need / human, asset and environmental protection
Use WHO/ILO and other authoritative sources to establish the burden of occupational injuries/disease, harmful exposures, air pollution, chemical hazards, fire/explosion risk, emissions and environmental consequences. Do not imply that a broad ILO/WHO statistic is caused solely by chemical exposure unless the source explicitly says so.

### References
One or more final scientific reference slides formatted APA 7. In-slide short citations may use compact source IDs linked to the reference list.

### Final slide
`شكرًا لكم` with HazRob identity; no slide number.

---

# Phase plan

## P0 — Governance, baseline and resumability
**Purpose:** freeze scope and ensure the task can resume safely.

Tasks:
- create tracking issue;
- create dedicated documentation branch;
- establish RC2 baseline and authoritative source hierarchy;
- lock presentational constraints and presenter names;
- establish checkpoint update protocol.

Exit criteria:
- issue exists;
- branch exists;
- checkpoint exists;
- baseline and rules recorded.

**Status: COMPLETE**
Evidence:
- Issue #9 created.
- Branch `docs/hazrob-master-deck` created from RC2 SHA `5250ea769615ec82c5dc944576ea3042736b3db2`.
- This checkpoint file created.

---

## P1 — Scientific source inventory and APA bibliography
**Purpose:** build the authoritative evidence base before drafting claims or slides.

Primary sources:
- HazRob full-system analysis report;
- ChemCore system-analysis report;
- `research_data/hr069_analytical_library/`;
- `research_data/external_mq_fingerprints/`;
- `research_data/chemical_coverage_hr069/`;
- `reference_bank/`;
- ChemCore implementation and tests;
- firmware and Android runtime source;
- `HazRob/build-harness` evidence/governance files;
- original peer-reviewed papers/datasets referenced by `mq_experiment_source_index_v1.json` and related manifests;
- public presenter research records and Yemen-specific occupational/environmental studies where directly relevant.

Tasks completed:
- created consolidated source register with title/authors/year/type/DOI where verified;
- classified implementation, peer-reviewed, authority, presenter and Yemen-context evidence;
- verified core ChemCore/MQ DOI/title pairs and recorded usage limits;
- verified ORCID `0000-0002-6144-4209` for presenter use;
- registered presenter publications and Yemen environmental/occupational studies;
- registered WHO/ILO burden sources and wording restrictions;
- built a claim-to-source matrix that prevents M3/M4, probability/ranking and burden-statistic overclaim;
- assembled APA 7 working bibliography.

Artifacts:
- `docs/presentation/P1_SOURCE_REGISTER_APA7.md`
- `docs/presentation/P1_CLAIM_TO_SOURCE_MATRIX.md`
- `docs/presentation/P1_AUTHOR_AND_YEMEN_CONTEXT_SOURCES.md`

Residual metadata items:
- ARID exact numeric identifier not independently verified from a direct primary profile;
- ResearcherID not independently verified;
- AUT-03 DOI unresolved against publisher metadata;
- AUT-04 volume/issue notation requires final P10 cleanup.
These items do not block storyboard development and are explicitly excluded from unsupported display.

Exit criteria:
- every planned scientific claim category has one or more traceable sources;
- uncertain references are tagged and excluded from strong claims;
- APA register ready for slide citations.

**Status: COMPLETE**

---

## P2 — Prior art, related work, comparable products and cost evidence
**Purpose:** establish context and the unmet need without overstating novelty.

Tasks:
- survey electronic-nose research relevant to same/similar MQ/MOX arrays;
- survey robotic gas sensing/inspection platforms;
- survey adaptive sensing, active chemical interrogation and open-set/OOD literature;
- identify commercial comparators: portable multi-gas detectors, area monitors, gas-imaging/inspection platforms, mobile robots and relevant advanced analyzers;
- record capabilities, limitations, selectivity approach, mobility, remote operation, analytics, published/quoted cost and source date;
- separate list price, distributor price, indicative price, and price-on-request.

Deliverables:
- prior-art evidence matrix;
- comparable-product matrix;
- cost evidence register;
- slide-ready comparison charts.

Exit criteria:
- no unsupported `expensive` claim;
- all product/cost claims are sourced and dated;
- comparison uses fair categories and avoids false equivalence.

**Status: IN PROGRESS**

---

## P3 — Patent-oriented innovation framing
**Purpose:** identify candidate invention families and distinguish integration novelty from individual known components.

Candidate families to evaluate:
- adaptive multisensor chemical interrogation (AMCI);
- joint multi-sensor inverse chemical/concentration inference;
- Open-Set/OOD + multi-channel fingerprint + evidence-tier gating;
- lineage-aware chemical evidence fusion with fail-closed reporting;
- robotic sampling/measurement orchestration coupling movement, chamber state and inference;
- traceable evidence/provenance pipeline from external research to runtime decision;
- additional candidate families discovered during P1/P2.

Tasks:
- build problem → mechanism → technical effect → differentiator map;
- identify which aspects are implemented now versus proposed;
- map likely prior-art overlaps and avoid novelty overclaim;
- create `Patent Candidate Map` slide concept.

Exit criteria:
- each candidate invention family has technical problem, mechanism, output/effect, implementation evidence, and known overlap notes.

Status: NOT STARTED

---

## P4 — Final slide-by-slide scientific storyboard
**Purpose:** freeze narrative before graphic production.

Tasks:
- finalize deck length; target master deck approximately 60–75 slides but allow evidence-driven adjustment;
- write slide title, single scientific message, evidence source, visual type, and presenter note for every slide;
- include required glossary slide(s) and blank presenter slide;
- sequence narrative: need → prior art → gap → HazRob → sensing → ChemCore → adaptive intelligence → evidence → decisions → applications → invention families → roadmap → references → thanks;
- identify slides that are confidential/master-only versus suitable for a public conference derivative.

Exit criteria:
- every slide has a purpose and source;
- no duplicate slides;
- no content block that requires tiny text to fit.

Status: NOT STARTED

---

## P5 — Visual specification and figure production
**Purpose:** produce reusable visual assets before PPTX assembly.

Planned visual families:
- end-to-end system architecture;
- robot hardware exploded view;
- sampling chamber airflow diagram;
- direct vs remote connectivity;
- ADC → Rs → R0 → Rs/R0 → log10 pipeline;
- MQ multi-channel response/fingerprint radar/heatmap;
- temporal fingerprint curves and feature extraction;
- ChemCore processing map;
- candidate library and response-model hierarchy;
- inverse solver loss/concentration graphic;
- OOD/Mahalanobis decision space;
- AMCI state machine and closed-loop interrogation;
- M0–M4 evidence ladder;
- evidence-lineage graph;
- mixture/conflict decision graphic;
- final Outcome tree;
- prior-art landscape;
- comparator cost/capability chart;
- innovation-stack / patent-family map;
- development/TRL roadmap.

Rules:
- charts use project/source data when available;
- schematic graphics must not imply measured performance unless measured;
- prefer editable vector shapes/charts; use PNG only where appropriate;
- avoid decorative complexity that reduces scientific readability.

Exit criteria:
- visual asset manifest complete;
- figures are legible at presentation size;
- all data-derived charts have traceable source data.

Status: NOT STARTED

---

## P6 — First PPTX build
**Purpose:** produce complete master deck in Arabic RTL.

Tasks:
- build 16:9 PPTX;
- apply Arabic RTL consistently;
- enforce type-size rules;
- use neutral backgrounds;
- place diagrams/charts with concise explanatory text;
- add notes/source metadata as appropriate;
- create references and thanks slides.

Exit criteria:
- full deck opens without corruption;
- no slide numbering;
- no text overflow/cutoff;
- tables readable on projector-size view.

Status: NOT STARTED

---

## P7 — Scientific and technical audit
**Purpose:** verify that the deck matches actual RC2 implementation and ChemCore semantics.

Audit topics:
- hardware and sensor array;
- firmware responsibilities;
- remote/direct connectivity;
- telemetry/storage/reporting;
- R0/Rs semantics;
- temporal features;
- candidate/ranking semantics;
- OOD/Open-Set;
- fingerprints and M3;
- M4 quantitative gating;
- QSPR limitations;
- mixtures/interference;
- confidence/evidence fusion;
- AMCI;
- safety interlocks;
- release authority/provenance.

Exit criteria:
- each technical slide classified as correct, corrected, or removed;
- no unsupported performance/validation claim remains.

Status: NOT STARTED

---

## P8 — Patent/public-disclosure audit
**Purpose:** make the master deck useful for invention discussions without conflating engineering facts with legal conclusions.

Tasks:
- mark implemented vs proposed/future;
- identify sensitive implementation details that may be inappropriate for the public conference derivative;
- avoid statements such as `first`, `unique`, `patented`, `novel` unless independently substantiated;
- prepare master-only notes identifying candidate claim boundaries;
- flag content requiring patent-counsel review before public disclosure.

Exit criteria:
- master deck and future public derivative boundaries documented;
- no accidental legal overclaim.

Status: NOT STARTED

---

## P9 — Conference readability and visual QA
**Purpose:** verify real presentation usability.

Checks:
- render every slide to images/PDF;
- inspect Arabic shaping/RTL direction;
- inspect mixed Arabic/Latin equations and sensor labels;
- inspect all tables for clipping;
- inspect diagrams at full-slide and contact-sheet scale;
- verify contrast and text size;
- verify GIF/video behavior if used;
- check that references are readable;
- run overlap/out-of-bounds checks where tooling permits.

Exit criteria:
- all slides visually inspected;
- all critical defects corrected and rerendered.

Status: NOT STARTED

---

## P10 — Finalization and handoff
**Purpose:** produce traceable deliverables and close the tracked task.

Deliverables:
- master `.pptx`;
- review `.pdf`;
- source/figure manifest;
- APA reference register;
- optional public-conference derivative plan;
- SHA-256 hashes for final artifacts;
- final checkpoint completion record.

Exit criteria:
- PPTX and PDF verified;
- issue #9 updated with completion evidence;
- checkpoint states final baseline, output names, hashes, and known residual limitations.

Status: NOT STARTED

---

# Checkpoint protocol

- Current phase: **P2 — Prior art, related work, comparable products and cost evidence**
- Last completed phase: **P1 — Scientific source inventory and APA bibliography**
- Overall status: ACTIVE
- Last stable baseline: RC2 `5250ea769615ec82c5dc944576ea3042736b3db2`
- Last completed artifacts:
  - `docs/presentation/P1_SOURCE_REGISTER_APA7.md`
  - `docs/presentation/P1_CLAIM_TO_SOURCE_MATRIX.md`
  - `docs/presentation/P1_AUTHOR_AND_YEMEN_CONTEXT_SOURCES.md`
- Known non-blocking metadata items: ARID exact ID, ResearcherID, AUT-03 DOI resolution, AUT-04 final volume/issue notation.
- Exact next action: **build P2 prior-art and commercial-comparator evidence matrices, including sourced price/cost evidence and fair capability comparison; then record slide-ready prior-art gaps.**

If work resumes in a new conversation, read this file and Issue #9 first, then continue from `Exact next action` without repeating completed phases unless the baseline has changed.
