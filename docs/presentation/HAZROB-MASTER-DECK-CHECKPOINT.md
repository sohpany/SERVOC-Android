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
- **Key diagrams must be native editable/recolorable PowerPoint shapes, not flattened images.** Use Shapes + Connectors + editable Arabic text and logical grouping. PNG/JPG/GIF are reserved mainly for photographs/renders/source imagery.

## Owner-defined invention core
The dominant invention narrative is fixed as an **integrated low-cost robotic chemical-analysis system**, not any single algorithm or sensor.

Core integrated pillars:
1. multi-method chemical analysis inside one coordinated engine;
2. broad compound-hypothesis coverage across dozens of operational targets / wider molecular knowledge;
3. linkage of qualified chemical results to occupational exposure/hazard limits;
4. remote robot driving, video, telemetry and measurement;
5. developer/laboratory experimentation and calibration workflow;
6. controlled updateable analytical knowledge layer;
7. low-cost prototype architecture with a project target/current estimate below USD 200, subject to final BOM scope verification.

Supporting analytical mechanisms:
- AMCI;
- joint multi-sensor inverse solver;
- OOD/Open-Set;
- multi-channel fingerprints;
- M0–M4 evidence tiers;
- QSPR;
- lineage-aware evidence fusion;
- mixture/interference guards;
- fail-closed outcomes.

The low-cost point is a **commercial/accessibility differentiator**, not a standalone patent claim. Before final deck use, the `< USD 200` statement must be tied to a defined BOM scope.

## Mandatory slides
- terminology/abbreviations including Rs, R0, Rs/R0, ADC, PPM, MOX/MOS, M0–M4, OOD, Open-Set, AMCI, QSPR, UID, SNR, AUC, UX, UI, API, MQTT and ESP32;
- blank presenter slide for شهاب الصهباني and مجدي ذيبان;
- global/Yemen need;
- prior work and products/costs;
- HazRob/ChemCore methodology;
- integrated invention-core slide;
- exposure-limit integration slide;
- remote-driving/field-operation slide;
- experiment/calibration laboratory slide;
- updateable-knowledge-layer slide;
- low-cost/BOM-value slide;
- candidate invention-family map;
- APA 7 references;
- final thanks slide.

## Phase status
- P0 Governance/baseline — COMPLETE
- P1 Scientific sources/APA — COMPLETE
- P2 Prior art/products/costs — COMPLETE
- P3 Invention-family framing — COMPLETE
- P4 Slide-by-slide storyboard — COMPLETE — 75 slides frozen
- P5 Visual specification and figure production — IN PROGRESS
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
- `docs/presentation/P3_INTEGRATED_INVENTION_CORE.md`

### P4
- `docs/presentation/P4_MASTER_STORYBOARD.md` — frozen at 75 master slides with internal IDs S01–S75 only; no visible slide numbering in final deck.

### P5
- `docs/presentation/P5_VISUAL_SPECIFICATION.md` — editable vector-shape-first rule established.

## Current P5 requirements
- Build a visual asset manifest mapping S01–S75 to native diagram/chart/table/image/mixed composition.
- Critical architecture, flow, state, evidence and invention diagrams must be native PowerPoint shapes and connectors.
- Arabic labels remain editable RTL text.
- Use theme-aware/neutral temporary colors to enable full recoloring later.
- Group related components without flattening them.
- Data charts remain editable where feasible.
- PNG/JPG/GIF only when imagery is inherently photographic/illustrative or when animation materially improves explanation.

## Resume point
Current phase: P5
Last completed phase: P4
Known blocker: `< USD 200` final wording requires BOM scope verification before P7/P10.
Exact next action: create the P5 visual asset manifest for S01–S75 and begin production of the reusable editable diagrams, prioritizing the integrated invention map, end-to-end architecture, ChemCore flow, AMCI, OOD, M0–M4, evidence lineage, connectivity, and roadmap.
