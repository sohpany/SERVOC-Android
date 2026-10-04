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

## Color independence
The current master build will use a neutral technical palette only as a temporary presentation aid. No diagram should depend on a single fixed color meaning that prevents later recoloring.

Color meaning must also be reinforced by:
- labels;
- icons/shapes;
- line style or position;
- section headings.

This prevents loss of meaning if the owners later apply a different conference/patent/investor theme.

## Master-shape conventions
- Rounded rectangle: process/module.
- Hexagon: evidence/chemical knowledge component where useful.
- Diamond: gate/decision.
- Cylinder: database/library.
- Circle/ellipse: state/domain/cluster.
- Solid arrow: primary flow.
- Dashed arrow: corroborative/reference/optional flow.
- Thick boundary: authority/safety gate.
- Bracket/ring: integration layer.

## Visual families to build as native editable shapes
1. HazRob end-to-end architecture.
2. Six-pillar integrated invention map.
3. Robot hardware exploded architecture (schematic labels may overlay a robot image, but analytical blocks remain shapes).
4. Sampling-chamber airflow schematic.
5. Direct/remote connectivity architecture.
6. Measurement pipeline: ADC → Rs → R0 → Rs/R0 → log10.
7. Spatial fingerprint/radar explanatory diagram.
8. Temporal fingerprint and feature-extraction map.
9. ChemCore full processing flow.
10. Response-model/evidence hierarchy.
11. Joint inverse-solver concept.
12. Open-Set/OOD gate diagram.
13. AMCI closed-loop diagram.
14. ChemCore state machine.
15. M0–M4 evidence ladder.
16. Lineage-aware evidence fusion graph.
17. Mixture/interference decision diagram.
18. Outcome decision tree.
19. Research-to-runtime provenance/update pipeline.
20. Prior-art gap matrix.
21. Comparable-products capability/cost matrix.
22. Patent-candidate/invention-family map.
23. Development/TRL roadmap.
24. Lives–Assets–Environment value triangle.

## QA requirement
During P9, verify that key diagrams can be individually selected, recolored and edited in PowerPoint without replacing an image.

## Next action
Create a visual asset manifest mapping every S01–S75 storyboard slide to one of:
- native editable diagram;
- editable chart;
- table;
- image/photo;
- mixed composition.
