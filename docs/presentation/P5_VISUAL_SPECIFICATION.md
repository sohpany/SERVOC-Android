# P5 — HazRob Master Deck Visual Specification

Status: **IN PROGRESS**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: HazRob 0.10.2 / HR069 RC2

## Core visual rule — editable/recolorable first
All architecture diagrams, flowcharts, state machines, evidence ladders, process maps, decision trees, comparison matrices, timelines and innovation maps must be created primarily as **native PowerPoint vector shapes**, not flattened raster images.

### Required implementation
- Use PowerPoint Shapes, Text Boxes, Lines, Arrows and Connectors.
- Group related components logically so they can be moved/recolored as one unit.
- Preserve individual sub-shapes inside groups for fine editing.
- Use theme-aware fill/line/text colors where practical so the project owners can recolor the full deck later.
- Avoid baking labels into PNG images.
- Use consistent connector semantics: data flow, control flow, evidence flow and gating should be visually distinguishable but still editable.
- Prefer simple geometric primitives over decorative artwork.
- Keep Arabic labels as editable text, RTL aligned.

### Raster/image exceptions
PNG/JPG/GIF may be used only when the source is inherently photographic or illustrative, for example:
- robot photographs/renders;
- product photographs;
- scientific photographs or source figures where reuse is permitted;
- screenshots where necessary;
- expressive images where editable vector recreation adds no value.

Data charts should remain editable PowerPoint charts or vector-like native shapes wherever feasible.

### SVG rule
SVG may be used only as a secondary asset when a complex icon/illustration cannot reasonably be rebuilt with native shapes. Critical architecture/invention diagrams must still be native editable shapes.

## Approved HazRob palette
Use the following visual family whenever color is needed:
- **Yellow** — primary emphasis / active path / highlighted analytical state.
- **Gray** — secondary layers / supporting evidence / neutral structure.
- **Black** — main text / strong boundaries / high-authority elements.
- **White** — background or negative space only when required for readability.

The deck should not introduce unrelated accent colors unless there is a scientific reason that cannot be conveyed by shape, label, line style or pattern.

### Palette behavior
- Yellow is the main project accent, not a warning-only color.
- Black should remain dominant for titles, main labels and high-contrast structural lines.
- Gray should use multiple light/dark values for hierarchy without leaving the HazRob identity.
- White may be used for clean space, inverse text or chart interiors.
- Avoid gradients unless they add analytical meaning.
- Avoid relying on hue alone for status; reinforce meaning using labels, icons, line style and position.
- All native shapes must remain recolorable after delivery.

## Hexagonal-cell visual language
Use **hexagonal cells as a preferred HazRob motif when they add semantic value**, especially for:
- chemical compounds / analytes;
- chemical knowledge and evidence nodes;
- ChemCore libraries and candidate fields;
- fingerprint or feature-group cells;
- multi-method analytical modules;
- integrated invention maps;
- research-to-runtime provenance stages;
- selected section/navigation graphics where the motif reinforces the chemical identity of the project.

### Hexagon rules
- Hexagons must remain native editable PowerPoint shapes.
- Use honeycomb/cluster arrangements only when they improve structure or comparison.
- Do not force hexagons into simple sequential workflows where rectangles/arrows are clearer.
- Avoid dense honeycomb backgrounds that compete with scientific content.
- Prefer a small number of large, legible hexagonal cells over many tiny cells.
- Hexagonal cells follow the same yellow/gray/black palette and remain recolorable.
- Text inside hexagons must remain large enough for conference projection.

## Color independence
Although yellow/gray/black is the approved working palette, no diagram should depend on one specific RGB/hex value to retain its meaning. This allows later theme changes for patent, conference or investor versions.

Color meaning must also be reinforced by:
- labels;
- icons/shapes;
- line style or position;
- section headings.

## Master-shape conventions
- Rounded rectangle: sequential process/module.
- **Hexagon: chemical/evidence/knowledge/analytical-method cell and preferred HazRob motif where appropriate.**
- Diamond: gate/decision.
- Cylinder: database/library when a storage metaphor is clearer than hexagonal knowledge cells.
- Circle/ellipse: state/domain/cluster.
- Solid arrow: primary flow.
- Dashed arrow: corroborative/reference/optional flow.
- Thick boundary: authority/safety gate.
- Bracket/ring: integration layer.

## Visual families to build as native editable shapes
1. HazRob end-to-end architecture.
2. Six/seven-pillar integrated invention map — preferential use of hexagonal cells.
3. Robot hardware exploded architecture (schematic labels may overlay a robot image, but analytical blocks remain shapes).
4. Sampling-chamber airflow schematic.
5. Direct/remote connectivity architecture.
6. Measurement pipeline: ADC → Rs → R0 → Rs/R0 → log10.
7. Spatial fingerprint/radar explanatory diagram.
8. Temporal fingerprint and feature-extraction map.
9. ChemCore full processing flow — analytical-method/evidence nodes may use hexagons.
10. Response-model/evidence hierarchy — use hexagonal evidence cells where useful.
11. Joint inverse-solver concept.
12. Open-Set/OOD gate diagram.
13. AMCI closed-loop diagram.
14. ChemCore state machine.
15. M0–M4 evidence ladder.
16. Lineage-aware evidence fusion graph — source/evidence nodes may use hexagons.
17. Mixture/interference decision diagram.
18. Outcome decision tree.
19. Research-to-runtime provenance/update pipeline — use staged hexagonal knowledge cells where useful.
20. Prior-art gap matrix.
21. Comparable-products capability/cost matrix.
22. Patent-candidate/invention-family map — preferential use of hexagonal cells.
23. Development/TRL roadmap.
24. Lives–Assets–Environment value triangle.

## QA requirement
During P9, verify that key diagrams can be individually selected, recolored and edited in PowerPoint without replacing an image. Also verify that switching among reasonable yellow/gray/black theme shades does not destroy semantic meaning or legibility, and that hexagonal-cell groups remain individually editable.

## Current P5 state
- 75-slide visual asset manifest created in `P5_VISUAL_ASSET_MANIFEST.md`.
- Editable vector-shape-first rule locked.
- Yellow / gray / black HazRob palette locked.
- Hexagonal-cell motif adopted where scientifically/structurally appropriate.

## Next action
Build the reusable master diagram family beginning with:
1. Integrated Invention Core — hexagonal pillar cells.
2. End-to-End HazRob Architecture.
3. ChemCore Full Flow — hexagonal analytical-method/evidence cells.
4. Direct/Remote Connectivity.
5. Research-to-Runtime Update Pipeline — hexagonal knowledge/provenance stages.
6. Experiment/Calibration Loop.
