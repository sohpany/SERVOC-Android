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

## Owner-defined invention core
The dominant invention narrative is now fixed as an **integrated low-cost robotic chemical-analysis system**, not any single algorithm or sensor.

Core integrated pillars:
1. multi-method chemical analysis inside one coordinated engine;
2. broad compound-hypothesis coverage across dozens of operational targets / wider molecular knowledge;
3. linkage of qualified chemical results to occupational exposure/hazard limits;
4. remote robot driving, video, telemetry and measurement;
5. developer/laboratory experimentation and calibration workflow;
6. controlled updateable analytical knowledge layer;
7. low-cost prototype architecture with a project target/current estimate below USD 200, subject to final BOM scope verification.

Supporting analytical mechanisms remain important subsystems:
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
- P4 Slide-by-slide storyboard — IN PROGRESS / REVISION REQUIRED TO REFLECT OWNER-DEFINED CORE
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
- `docs/presentation/P3_INTEGRATED_INVENTION_CORE.md`

## Current P4 requirements
The storyboard must be revised so the integrated invention core dominates the narrative. Algorithm-family slides remain as supporting subsystems, not the sole definition of the invention.

Required invention sequence inside the deck:
`multi-method analysis → broad compound coverage → evidence-qualified result → exposure-limit/hazard context → remote robot operation → experimental/calibration lab → controlled updates → low-cost accessibility → candidate protection map`

## Resume point
Current phase: P4
Last completed phase: P3
Known blocker: none; `< USD 200` final wording requires BOM scope verification before P7/P10.
Exact next action: revise `P4_MASTER_STORYBOARD.md` to center `P3_INTEGRATED_INVENTION_CORE.md`, keep the master at about 75 slides, then freeze visual assets for P5.
