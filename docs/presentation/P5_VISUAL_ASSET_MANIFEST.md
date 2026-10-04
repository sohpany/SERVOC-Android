# P5 — Visual Asset Manifest for HazRob Master Deck

Status: IN PROGRESS
Tracking issue: #9
Branch: `docs/hazrob-master-deck`

Legend:
- `SHAPE` = native editable PowerPoint shapes/connectors/text.
- `CHART` = editable PowerPoint chart or native-shape data graphic.
- `TABLE` = editable PowerPoint table.
- `IMAGE` = photo/render/source image; raster allowed.
- `MIXED` = combination, with analytical content still editable.

## Slide visual map

| ID | Primary type | Reusable asset / note |
|---|---|---|
| S01 | MIXED | robot image + editable integrated-system bands |
| S02 | SHAPE | two editable presenter placeholders |
| S03 | SHAPE | human-outside / robot-inside hazard-zone schematic |
| S04 | CHART | ILO burden big-number infographic |
| S05 | CHART | WHO air-pollution burden infographic |
| S06 | SHAPE | Lives–Assets–Environment triangle |
| S07 | MIXED | Yemen map/image + editable research callouts |
| S08 | CHART | Yemen measurement-study topic map |
| S09 | SHAPE | Detection → Identification → Quantification → Hazard interpretation |
| S10 | CHART | overlapping MQ sensitivity responses |
| S11 | SHAPE | e-nose research timeline |
| S12 | TABLE | study × MQ sensor overlap matrix |
| S13 | CHART | pure/mixed VOC exposure-recovery curves |
| S14 | SHAPE | closed-set vs open-set decision space |
| S15 | MIXED | robotic gas-sensing landscape |
| S16 | MIXED | portable multi-gas detector capability cards |
| S17 | MIXED | area-monitor / OGI cards |
| S18 | MIXED | industrial robot cards |
| S19 | CHART | logarithmic cost comparison |
| S20 | TABLE/SHAPE | prior-art gap matrix |
| S21 | SHAPE | **Integrated Invention Core** master map |
| S22 | SHAPE | six/seven-pillar radial map |
| S23 | SHAPE | sensor vector → hypothesis field |
| S24 | SHAPE | 144 operational targets + evidence filters |
| S25 | SHAPE | qualified result → exposure/hazard limits |
| S26 | SHAPE | remote operation: drive/video/telemetry/measurement |
| S27 | SHAPE | experiment/calibration laboratory loop |
| S28 | SHAPE | research → validation → governed update → runtime |
| S29 | CHART | prototype BOM/value graphic, pending BOM verification |
| S30 | SHAPE | **End-to-End HazRob Architecture** |
| S31 | MIXED | robot image + editable hardware callouts |
| S32 | SHAPE | eight-channel MQ sensor-array map |
| S33 | SHAPE | sampling-chamber airflow schematic |
| S34 | SHAPE | mobility/safety interlock diagram |
| S35 | SHAPE | direct vs remote connectivity architecture |
| S36 | SHAPE | video/control/data lane separation |
| S37 | SHAPE | Site → Session → Measurement → Analysis → Report hierarchy |
| S38 | SHAPE | MOX sensing mechanism schematic |
| S39 | SHAPE | ADC → Vout → Rs equation path |
| S40 | SHAPE | Rs and R0 concept |
| S41 | CHART | raw vs normalized multi-sensor curves |
| S42 | CHART | linear vs log-transformed response |
| S43 | CHART | radar / multi-channel spatial fingerprint |
| S44 | CHART | temporal fingerprint curve |
| S45 | SHAPE | temporal feature extraction callouts |
| S46 | CHART | 8×17 feature heatmap |
| S47 | SHAPE | ChemCore role architecture |
| S48 | SHAPE | **ChemCore Full Processing Flow** |
| S49 | SHAPE | operational 144 vs molecular 652 dual-library diagram |
| S50 | SHAPE | response/evidence-source hierarchy |
| S51 | CHART | reference Rs/R0↔ppm curve family |
| S52 | SHAPE/CHART | fingerprint matching concept |
| S53 | CHART | **Joint Inverse Solver** observed/predicted + loss landscape |
| S54 | SHAPE | QSPR molecule→descriptor→sensor prior |
| S55 | CHART | temporal-analysis features overlay |
| S56 | SHAPE | evidence-fusion overview |
| S57 | SHAPE | forced classification vs reject option |
| S58 | SHAPE | **OOD/Mahalanobis Decision Space** |
| S59 | SHAPE | fail-closed gate chain |
| S60 | SHAPE | **AMCI Closed Loop** |
| S61 | SHAPE | **ChemCore State Machine** |
| S62 | SHAPE | adaptive exposure/recovery timeline |
| S63 | SHAPE | uncertainty-layer stack |
| S64 | SHAPE | pure/ambiguous/mixture residual decision |
| S65 | SHAPE | **M0–M4 Evidence Ladder** |
| S66 | SHAPE | M3 identity vs M4 quantitative authority split |
| S67 | SHAPE | reportable-PPM gate tree |
| S68 | SHAPE | **Lineage-Aware Evidence Graph** |
| S69 | SHAPE | outcome wheel / decision endpoints |
| S70 | SHAPE | ResultEnvelope → SmartReport |
| S71 | SHAPE | **Patent Candidate / Integrated Invention Map** |
| S72 | SHAPE | application mosaic |
| S73 | SHAPE | **Development / Validation / TRL Roadmap** |
| S74 | TABLE/TEXT | APA 7 references; may split in final QA if text is too dense |
| S75 | MIXED | logo + robot hero image + thanks |

## Reusable master assets to build first
Priority A — architecture/invention assets:
1. `V01_INTEGRATED_INVENTION_CORE`
2. `V02_END_TO_END_ARCHITECTURE`
3. `V03_CHEMCORE_FULL_FLOW`
4. `V04_DIRECT_REMOTE_CONNECTIVITY`
5. `V05_RESEARCH_TO_RUNTIME_UPDATE`
6. `V06_EXPERIMENT_CALIBRATION_LOOP`

Priority B — analytical method assets:
7. `V07_MEASUREMENT_PIPELINE`
8. `V08_SPATIAL_TEMPORAL_FINGERPRINT`
9. `V09_INVERSE_SOLVER`
10. `V10_OOD_MAHALANOBIS`
11. `V11_AMCI_LOOP`
12. `V12_CHEMCORE_STATE_MACHINE`
13. `V13_M0_M4_LADDER`
14. `V14_EVIDENCE_LINEAGE`
15. `V15_OUTCOME_TREE`

Priority C — context/value assets:
16. `V16_LIVES_ASSETS_ENVIRONMENT`
17. `V17_PRIOR_ART_GAP`
18. `V18_COST_COMPARISON`
19. `V19_APPLICATION_MAP`
20. `V20_DEVELOPMENT_ROADMAP`

## Editing requirements
- All V01–V20 assets must remain editable inside PowerPoint.
- Do not rasterize Arabic labels.
- Use theme-aware neutral colors; avoid fixed decorative gradients.
- Every group must be recolorable by editing shape fills/lines/text.
- During P9 verify selection/edit/recolor behavior in the generated PPTX.

## Next action
Build V01–V06 as reusable native-shape components and then extend to analytical assets V07–V15.