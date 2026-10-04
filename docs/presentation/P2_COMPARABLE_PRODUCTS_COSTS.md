# P2 — HazRob Comparable Commercial Systems & Cost Evidence Register

Status: **IN PROGRESS — core comparator set established**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Market evidence cut-off: 2026-10-04

## Evidence policy

- Capability claims prefer manufacturer/official sources.
- Price claims use current distributor listings, public procurement records, or manufacturer-published service pricing.
- `Price on request` is recorded when the manufacturer does not publish a list price.
- Prices are configuration-, geography-, tax-, support- and date-dependent; slide language must say `published/observed price` rather than universal purchase cost.
- Comparators are not asserted to be direct substitutes for HazRob. They represent adjacent solution classes.

## Comparator register

### CP-01 — Dräger X-am 8000
Category: professional portable multi-gas detector.
Official capability source: Dräger product page.
Key capabilities:
- 1–7 gases simultaneously;
- CAT + IR + PID + electrochemical technologies;
- toxic/combustible gases, vapours and O2;
- internal pump, >24 h operation, IP68;
- ATEX / IECEx approvals.
Published cost evidence:
- public procurement record (Romania, 2024) listed market value per unit: **USD 5,048.59**;
- current distributor listing observed in 2026: **USD 6,278.46** for SKU 8325800.
Use in deck: example of mature professional portable detection costing several thousand USD per configured unit.
Important comparison boundary: X-am 8000 is a certified professional detector using selective/professional sensor technologies; HazRob RC2 is a research/robotic analytical platform and must not be presented as a certified replacement.

### CP-02 — Honeywell AreaRAE Plus
Category: transportable wireless multi-gas / area monitor.
Official capability source: Honeywell AreaRAE Plus/Pro product page.
Key capabilities:
- up to seven threats including VOCs, combustibles, toxics and oxygen;
- PID for VOC measurements;
- optional meteorological sensing;
- wireless/mesh area monitoring.
Published cost evidence:
- All Safe Industries current listing: **USD 12,463–15,006** depending configuration;
- R.S. Hughes AreaRAE Plus rapid deployment kit listing: **USD 74,167.04** for the full kit/configuration.
Use in deck: shows how professional area monitoring can move from low-five-figure single-unit pricing to substantially higher deployment-kit cost.
Comparison boundary: AreaRAE is stationary/transportable rather than a robotic analytical platform.

### CP-03 — FLIR GF77 optical gas imaging camera
Category: professional optical gas imaging / remote leak visualization.
Official capability source: Teledyne FLIR GF77 page/datasheet.
Key capabilities:
- optical visualization of selected gases such as CH4, SF6, C2H4 and NH3 depending lens/spectral range;
- radiometric temperature measurement;
- remote leak-survey use in utilities, oil/gas, chemical/manufacturing and first response.
Published cost evidence from current distributors:
- basic 25° configurations: **USD 37,950**;
- multi-lens configurations: approximately **USD 43,500–62,950**;
- example GF77 25H+6H listing: **USD 49,950**.
Use in deck: strong example of high-cost professional remote gas visualization instrumentation.
Comparison boundary: OGI visualizes spectrally detectable plumes; it is not equivalent to ChemCore multi-channel chemical inference and does not make HazRob an OGI substitute.

### CP-04 — ANYbotics ANYmal / ANYmal X + Gas sensing / Gas Guard
Category: autonomous industrial quadruped inspection robot.
Official capability sources: ANYbotics ANYmal, ANYmal X and Gas Leak Detection pages.
Key capabilities:
- autonomous industrial inspection with visual/thermal/acoustic/LiDAR sensing;
- optional continuous gas concentration payloads;
- gas leak detection/localization and trend monitoring;
- ANYmal X is ATEX/IECEx certified for Zone 1 IIB and IP67;
- remote/autonomous inspection in oil/gas and chemical facilities.
Price evidence:
- manufacturer does **not publish a simple public list price**; commercial engagement is quote-based.
Use in deck: one of the closest commercial functional comparators for mobile industrial gas inspection.
Comparison boundary: it is a mature industrial autonomous inspection platform with hazardous-zone certification; HazRob should not claim equivalent certification or deployment maturity.

### CP-05 — ExRobotics ExR platform
Category: Ex-rated mobile inspection robot.
Official capability/cost source: ExRobotics published materials.
Key capabilities:
- ATEX/IECEx Zone 1 robotic inspection;
- optional gas detectors (two detectors from a range of five in published configuration description);
- camera, microphone, thermal/acoustic options;
- autonomous docking/charging inside classified environment.
Published service cost:
- ExRobotics states its robot full-service package starts at **EUR 35,000 per year** for a basic ExR-1 Co-Operator under RaaS;
- company equates this to about **EUR 12/hour** at 3,000 operating hours/year.
Use in deck: directly demonstrates that mobile hazardous-area gas inspection is commercially established and can involve tens of thousands of euros per year even under service model.
Comparison boundary: do not infer outright purchase price from RaaS annual service pricing.

### CP-06 — MSA ALTAIR 5X
Category: professional portable multi-gas detector.
Official source: MSA product page.
Capabilities: professional multi-gas platform with electrochemical/IR/PID options depending configuration.
Price evidence: manufacturer currently states **contact customer service for pricing and availability**.
Use in deck: reinforces that professional instruments are configuration-dependent and often quote-based; do not invent a price.

### CP-07 — Environics ChemProX
Category: handheld chemical detector / CBRN-oriented chemical detection comparator.
Public procurement cost evidence (Romanian public record, 2024): **USD 22,995 per unit**.
Use in deck: adjacent chemical-identification/safety instrumentation demonstrating the cost of specialized chemical detection.
Comparison boundary: different sensing technology/application scope; not a direct HazRob gas-array equivalent.

### CP-08 — Thermo Fisher FirstDefender RMX
Category: handheld Raman chemical identification system.
Public procurement cost evidence (Romanian public record, 2024): **USD 75,500 per unit**.
Use in deck: adjacent high-end chemical identification equipment illustrating specialized analytical-system costs.
Comparison boundary: primarily Raman identification of materials, not equivalent to mobile airborne gas sensing.

## Slide-ready cost bands (illustrative, based on sourced examples above)

These bands should be presented as **examples from published prices**, not as universal market averages:

- Professional portable multi-gas detector: roughly **USD 5k–6k** in sourced X-am 8000 examples.
- Transportable multi-threat area monitor: roughly **USD 12.5k–15k** for AreaRAE Plus configurations; deployment kit example ~**USD 74k**.
- Specialized handheld chemical identification: sourced examples ~**USD 23k–75.5k**.
- Optical gas imaging: sourced FLIR GF77 examples ~**USD 38k–63k**.
- Ex-rated mobile inspection robot: ExRobotics published RaaS starts at **EUR 35k/year**; ANYmal/ANYmal X is quote-based.

## Fair-comparison feature dimensions for slides

| Dimension | Portable multi-gas | Area monitor | OGI camera | Industrial inspection robot | HazRob RC2 |
|---|---|---|---|---|---|
| Mobility | carried by worker | transportable/static | carried/remote view | autonomous/remote mobile | remote mobile 4WD |
| Worker removed from hazard | limited | partly | partly via stand-off imaging | yes | intended yes |
| Multi-gas sensing | yes, configured sensors | yes | selected spectral gases | optional gas payloads | multi-channel MQ + other sensors |
| Chemical identity inference | sensor-specific / configured gas | configured threats | plume visualization by spectral band | platform/payload dependent | ChemCore hypothesis/fingerprint/evidence engine |
| Open-set/OOD | generally not presented as core function | generally not presented as core function | not equivalent | platform dependent | implemented research gate; external validation pending |
| Adaptive measurement loop | device workflows | monitoring | operator survey | mission autonomy | AMCI analytical state machine/policy |
| Evidence provenance tiers | not comparable | not comparable | not comparable | not comparable | M0–M4 + lineage-aware evidence |
| Reportable occupational PPM | professional depending sensor/config | professional depending sensor/config | OGI-specific metrics, not same | payload dependent | only after M4; currently limited |
| Hazardous-area certification | product dependent | product dependent | product dependent | ANYmal X/ExR certified | not yet Ex-certified |
| Published price | several thousand USD example | low-five figures; kit higher | tens of thousands USD | quote/RaaS | prototype BOM to be evaluated separately |

## Presentation wording recommendation

Recommended:
> `تُظهر الأسعار المنشورة أن حلول الكشف والتحليل المتخصصة قد تمتد من عدة آلاف من الدولارات للأجهزة المحمولة إلى عشرات الآلاف لأجهزة المراقبة/التصوير والتحليل المتقدم، بينما تُسعّر بعض روبوتات التفتيش الصناعية بعروض خاصة أو بنماذج خدمة سنوية.`

Avoid:
> `جميع الأجهزة المنافسة باهظة الثمن`.

The deck should use 4–5 representative examples visually rather than an overcrowded catalogue.

## Source traceability notes

Capability sources:
- Dräger — X-am 8000 official product page/datasheet.
- Honeywell — AreaRAE Plus/Pro official product page/datasheet.
- Teledyne FLIR — GF77 official product page/datasheet.
- ANYbotics — ANYmal, ANYmal X and Gas Leak Detection official pages.
- ExRobotics — certification and published gas-detection/RaaS material.
- MSA — ALTAIR 5X official product page.

Price sources:
- Romanian public procurement/donation equipment value record (2024) for X-am 8000, ChemProX and FirstDefender RMX.
- WI Automation 2026 observed listing for X-am 8000.
- All Safe Industries 2026 observed listing for AreaRAE Plus.
- R.S. Hughes observed listing for AreaRAE Plus rapid deployment kit.
- Calcert / GlobalTestSupply / Mouser observed listings for FLIR GF77 configurations.
- ExRobotics published RaaS pricing statement.

## Next P2 action

- add a concise `prior-art gap map` for P3;
- decide which four/five products will appear in the master deck versus appendix/reference material;
- create visual chart specification using log-scale or categorical cost bands so price differences remain readable without implying apples-to-apples equivalence.
