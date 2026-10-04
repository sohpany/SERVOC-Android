# HazRob Master Deck — Checkpoint

Status: ACTIVE
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
Presenters: شهاب الصهباني، مجدي ذيبان

## Fixed deck rules
- Arabic RTL, 16:9, no visible slide numbering.
- Large conference-readable typography.
- Prefer diagrams/charts/process maps over dense text.
- Tables must remain readable and use slide width efficiently.
- Neutral backgrounds in this build.
- All scientific statistics, prior-art statements and product prices require traceable sources.
- Distinguish IMPLEMENTED / VALIDATED / RESEARCH-SUPPORTED / PROPOSED / FUTURE WORK.
- Never present M3 as M4, ranking weight as calibrated probability, or literature/reference PPM as HR069 M4 validation.
- Patent-oriented content is framed as candidate invention families, not a legal conclusion.

## Mandatory slides
- terminology/abbreviations including Rs, R0, Rs/R0, ADC, PPM, MOX/MOS, M0–M4, OOD, Open-Set, AMCI, QSPR, UID, SNR, AUC, UX, UI, API, MQTT and ESP32;
- blank presenter slide for شهاب الصهباني and مجدي ذيبان;
- global/Yemen need;
- prior work and products/costs;
- HazRob/ChemCore methodology;
- candidate invention-family map;
- APA 7 references;
- final thanks slide.

## Phase status
- P0 Governance/baseline — COMPLETE
- P1 Scientific sources/APA — COMPLETE
- P2 Prior art/products/costs — COMPLETE
- P3 Invention-family framing — COMPLETE
- P4 Slide-by-slide storyboard — IN PROGRESS
- P5 Visual production — NOT STARTED
- P6 PPTX build — NOT STARTED
- P7 Scientific audit — NOT STARTED
- P8 Patent/public-disclosure audit — NOT STARTED
- P9 Visual QA — NOT STARTED
- P10 Finalization — NOT STARTED

## Completed artifacts
### P1
- `docs/presentation/P1_SOURCE_REGISTER_APA7.md`
- `docs/presentation/P1_CLAIM_TO_SOURCE_MATRIX.md`
- `docs/presentation/P1_AUTHOR_AND_YEMEN_CONTEXT_SOURCES.md`

### P2
- `docs/presentation/P2_PRIOR_ART_MATRIX.md`
- `docs/presentation/P2_COMPARABLE_PRODUCTS_COSTS.md`
- `docs/presentation/P2_PRIOR_ART_GAP_MAP.md`

### P3
- `docs/presentation/P3_INVENTION_FAMILY_MATRIX.md`
- `docs/presentation/P3_PATENT_CANDIDATE_MAP.md`

Front-stage P3 families:
1. AMCI adaptive multisensor chemical interrogation.
2. Joint multi-sensor inverse chemical/concentration inference.
3. OOD → fingerprint → M3 identity → separate M4 quantitative gate.
4. Lineage-aware evidence fusion.
5. Robot/sample state coupled to analytical validity.

Supporting families:
- M0–M4 identity/quantification separation.
- Fail-closed analytical outcomes.
- Research-to-runtime scientific provenance.

## Current P4 requirements
Create a master storyboard of approximately 60–75 slides. Internal production IDs may be used in documentation, but the actual deck will have no visible slide numbers.

Every storyboard entry must record:
- Arabic slide title;
- single core message;
- concise content;
- proposed visual;
- source IDs;
- implementation-status label where relevant;
- presenter note;
- public/master-only suitability where relevant.

Narrative sequence:
`need → Yemen/global context → prior art/products/cost → gap → HazRob concept → robot/hardware → measurement pipeline → fingerprints → ChemCore → inverse solver → OOD → AMCI → evidence tiers/fusion → outcomes/reporting → invention families → applications → roadmap → limitations → references → thanks`

## Resume point
Current phase: P4
Last completed phase: P3
Known blockers: none for P4.
Exact next action: create `P4_MASTER_STORYBOARD.md`, freeze slide content and visual requirements, then advance to P5.
