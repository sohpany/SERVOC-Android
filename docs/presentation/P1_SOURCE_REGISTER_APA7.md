# P1 — HazRob Master Deck Scientific Source Register & APA 7 Working Bibliography

Status: **COMPLETE — working verified source base**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`
Baseline: HazRob 0.10.2 / HR069 RC2 @ `5250ea769615ec82c5dc944576ea3042736b3db2`
Verification cut-off for this register: 2026-10-04

## 1. Purpose and evidence policy

This register is the authoritative working bibliography for the HazRob master presentation. It separates implementation evidence, peer-reviewed scientific evidence, Yemen-context evidence, official burden statistics, and market/comparator evidence.

Rules:
- Project source code and reports may establish what HazRob **implements**; they do not by themselves establish external scientific novelty.
- Peer-reviewed literature may support scientific methods, transfer evidence, fingerprints, response curves, or related work; it does not create HR069 M4 validation.
- WHO/ILO evidence is contextual burden evidence only and must not be presented as HazRob performance evidence.
- Yemen-context studies establish local need and measurement context, not HazRob validation.
- A source-derived M2/M3 statement must never be worded as device-calibrated M4 PPM unless the explicit HR069 M4 gate is satisfied.
- Metadata conflicts are retained as flags and not silently resolved.

---

## 2. Internal HazRob / ChemCore authoritative sources

### INT-01 — RC2 engineering baseline
Repository: `sohpany/SERVOC-Android`
Ref: `release/hazrob-0.10.2-rc2` @ `5250ea769615ec82c5dc944576ea3042736b3db2`
Use: implemented architecture, firmware/app/ChemCore behavior, tests, libraries, reports, state machines, algorithms, database paths.

### INT-02 — HazRob full systems-analysis report
Artifact: `HazRob_RC2_Full_System_Analysis_0.10.2_HR069`
Use: system architecture, repository map, runtime layers, risks, governance, process/data flows.

### INT-03 — ChemCore systems-analysis report
Artifact: `HazRob_ChemCore_System_Analysis_0.10.2_HR069_RC2`
Use: ChemCore analytical pipeline, Rs/R0/log-space processing, temporal features, fingerprints, inverse solver, OOD, M0–M4, AMCI, confidence/evidence fusion, outcomes.

### INT-04 — HR069 analytical research library
Path: `research_data/hr069_analytical_library/`
Use: project-curated scientific transfer evidence, source grading, fingerprint/curve manifests, PubChem/QSPR linkage.

### INT-05 — MQ experiment source index
Path: `research_data/hr069_analytical_library/mq_experiment_source_index_v1.json`
Use: authoritative mapping from external papers/datasets to HR069 sensors/analytes and project evidence policy.

### INT-06 — Reference bank
Path: `reference_bank/`
Use: reference schema and approved-experiment governance.

### INT-07 — HazRob organization build harness
Repository: `HazRob/build-harness`
Use: release authority, evidence/data integration governance, qualification triggers, reproducibility/provenance story.

---

## 3. Presenter / project-owner publications

### AUT-01 — Yemen OSH curricula
Verified working APA 7:
Al-Sohpany, S. M. (2022). The availability of specialized and occupational health and safety curricula in Sana'a University-Yemen. *Journal of Social Studies, 28*(4), 68–81. https://doi.org/10.20428/jss.v28i4.2041

Use: Yemen OSH capacity/education gap; presenter research background.
Public identifier associated with the author in published/archived material: ORCID `0000-0002-6144-4209`.

### AUT-02 — Medical waste governance in Yemen
Verified working APA 7:
Al-Sohpany, S. (2021). The leading factors of medical waste management absence in Yemen. *Al-Razi University Journal for Medical Sciences, 5*(2), 9–18. https://doi.org/10.51610/rujms5.2.2021.104

Use: hazardous-waste, environmental/occupational control gaps.

### AUT-03 — Reality and causes of weak OSH in Yemen
Working bibliographic record:
الصهباني، شهاب محمد. (2023). واقع وأسباب ضعف مجال الصحة والسلامة المهنية في اليمن. *مجلة الجامعة الوطنية، 8*(25), 223–264.

Use: direct Yemen-specific rationale for stronger occupational detection, prevention and emergency capability.
Metadata flag: a DOI appears on an author-uploaded copy, but independent publisher-resolution was not confirmed in P1. **Do not display a DOI for this item in the deck until resolved.**

### AUT-04 — Chemical disinfectants / antiseptic agents in Yemen
Working bibliographic record:
Al-Sohpany, S. M. S. (2025). Chemical disinfectants & antiseptic agents efficacy testing in Yemen: Between international standards and real practices and parties' performance evaluation. *Civilization University Journal of Applied and Humanitarian Researches*, 169–202. https://doi.org/10.64295/cujahr.v9i09.54

Use: chemical-safety governance/context and presenter research background.
Metadata flag: publisher/public-index records differ on volume/issue notation. Keep DOI/title/year/pages; verify final volume/issue before final APA slide.

### Research identifiers
- ORCID: `0000-0002-6144-4209` — verified for deck use.
- ARID numeric/profile identifier — **not yet independently verified from a direct primary profile; do not display number**.
- ResearcherID — not yet independently verified; do not invent or display.

---

## 4. Core ChemCore / MQ / e-nose scientific literature

### SCI-01 — Large-scale multi-task e-nose
Wang, T., Zhang, H., Wu, Y., Jiang, W., Chen, X., Zeng, M., Yang, J., Su, Y., Hu, N., & Yang, Z. (2022). Target discrimination, concentration prediction, and status judgment of electronic nose system based on large-scale measurement and multi-task deep learning. *Sensors and Actuators B: Chemical, 351*, 130915. https://doi.org/10.1016/j.snb.2021.130915

Project relation: seven HR069-overlapping MQ models; multi-analyte classification/concentration-shape evidence; large-scale sensing dataset.

### SCI-02 — Binary VOC mixtures / multi-task learning
Mei, H., Yang, R., Peng, J., Meng, K., Wang, T., & Wang, L. (2025). Research on binary mixed VOCs gas identification method based on multi-task learning. *Sensors, 25*(8), 2355. https://doi.org/10.3390/s25082355

Project relation: pure and binary VOCs, transient/dynamic features, same/similar MQ array overlap; useful for mixture-method prior art and research support.

### SCI-03 — Linear alcohol e-nose
Paredes Doig, A. L., Sun Kou, M. del R., Picasso Escobar, G., Doig Camino, E., & Comina, G. (2016). Implementación y evaluación de una nariz electrónica para la detección de alcoholes lineales. *Revista Colombiana de Química, 45*(2), 12–18. https://doi.org/10.15446/rev.colomb.quim.v45n2.60393

Project relation: MQ2/MQ3 alcohol-series discrimination support.

### SCI-04 — MultimodalGasData
Narkhede, P., Walambe, R., Chandel, P., Mandaokar, S., & Kotecha, K. (2022). MultimodalGasData: Multimodal dataset for gas detection and classification. *Data, 7*(8), 112. https://doi.org/10.3390/data7080112

Project relation: multimodal benchmark and transfer-class fingerprints; internally marked as already ingested.

### SCI-05 — Low-cost air-quality MQ/IoT context
Alvear-Puertas, V. E., Burbano-Prado, Y. A., Rosero-Montalvo, P. D., Tözün, P., Marcillo, F., & Hernandez, W. (2022). Smart and portable air-quality monitoring IoT low-cost devices in Ibarra City, Ecuador. *Sensors, 22*(18), 7015. https://doi.org/10.3390/s22187015

Use restriction: air-quality/low-cost sensor context. **Do not describe this source as a dedicated HR069/MQ7 calibration study.**

### SCI-06 — Low-cost flammable gas / landfill measurements
Daugela, I., Suziedelyte Visockiene, J., Kumpiene, J., & Suzdalev, I. (2021). Measurements of flammable gas concentration in landfill areas with a low-cost sensor. *Energies, 14*(13), 3967. https://doi.org/10.3390/en14133967

Project relation: methane/flammable-gas response-model context.

### SCI-07 — MQ-135 sensitivity-curve analysis
Umbu, A. B. S. (2023). Analisis grafik karakteristik sensitivitas sensor MQ-135 untuk menentukan persamaan hubungan antara ppm dan Rs/Ro. *Jurnal Teori dan Aplikasi Fisika, 11*(2), 49–60. https://doi.org/10.23960/jtaf.v11i2.314

Project relation: literature-derived MQ135 response relations. Not HR069 device calibration.

### SCI-08 — Hydrogen / MQ8 context
Mango, E., Fantoni, A., Vieira, M., & Lobo, R. F. M. (2026). Quantification of hydrogen from electrolysis by combining a resistive electronic sensor with the standard volumetric method. *Applied Sciences, 16*(10), 4863. https://doi.org/10.3390/app16104863

Project relation: hydrogen/MQ8 literature response evidence; still not HR069 M4.

### SCI-09 — Multi-sensor CO e-nose
Kumar, D., Rabha, A. K., Mishra, A., Shrestha, R., & Rajput, N. S. (2026). A novel E-nose architecture based on virtual sensor-augmented embedded intelligence for a real-time in-vehicle carbon monoxide concentration estimation system. *Electronics, 15*(8), 1671. https://doi.org/10.3390/electronics15081671

Project relation: multi-channel CO inference and cross-sensitivity exploitation.

### SCI-10 — Visual smell fingerprinting with exact/similar MQ array
Oshovsky, G. V., Hijnen, H. J. M., Tempelman, C. H. L., Hagenaar, J. J., Stunnenberg, K. R., Willemse, W., Oudenes, Z., van Langerak, R., van der Velde, G., & Herselman, J. F. (2026). Visual smell fingerprinting with sensor arrays. *Food and Humanity, 6*, 101079. https://doi.org/10.1016/j.foohum.2026.101079

Project relation: eight-channel array/fingerprint feature design, response ratios and transient-response concepts.

### SCI-11 — Food freshness dataset
Mahdian et al. (2025). *Food Freshness Electronic Nose Dataset* [Data set]. Zenodo. https://doi.org/10.5281/zenodo.17285312

Use restriction: source/headspace/freshness classes and time-series methods. Do not use as pure-analyte identity validation without explicit analyte labels.

---

## 5. Yemen environmental / occupational measurement context

### YEM-01 — National multi-pollutant air analysis
Hael, M. A. (2023). Unveiling air pollution patterns in Yemen: A spatial–temporal functional data analysis. *Environmental Science and Pollution Research, 30*(17), 50067–50095. https://doi.org/10.1007/s11356-023-25790-3

Use: Yemen-wide air-pollution context using PM2.5, O3, CO and SO2 across multiple cities/years.

### YEM-02 — Mukalla PM10 / PM2.5 indoor-outdoor measurements
Ahmed, S. Z. (2018). Indoor/outdoor air quality and children health risk assessment for PM10 and PM2.5 on urban areas at Al-Mukalla City, Yemen. *Hadhramout University Journal of Natural & Applied Sciences, 15*(2).

Use: local direct environmental-measurement context. No DOI confirmed in P1.

### YEM-03 — Indoor radon in Sana'a
Abdo, M. A. S., Arhouni, F. E., Zaimi, M., Boukhair, A., & Fahad, M. (2024). Activity concentration of indoor radon and its short-lived progeny (218Po, 214Pb, 214Po) and their effect on atmospheric ionization rate in Sana'a, Yemen. *Environmental Pollution, 358*, 124518. https://doi.org/10.1016/j.envpol.2024.124518

Use: direct environmental-contaminant measurement context in Yemen.

### YEM-04 — Balhaf LNG industrial-pollution context
Laqal, K., Tawaer Kafi, S., Alsabah, Y., & Salem, A. A. M. (2022). Study of environmental pollution resulting from Balhaf liquid natural gas station using spectroscopy analysis, Shabwah Governorate – Yemen. *International Journal of Engineering and Applied Physics, 2*(2), 457–465.

Use: industrial-emissions/pollution context. DOI not confirmed in P1.

### YEM-05 — Occupational benzene exposure
Yaseen, A. S., Bomonther, O. M., Babsili, A. A., & Abdulmanea, K. S. (2026). The effect of occupational benzene exposure duration on hematological parameters among fuel station workers in Mukalla City, Yemen. *Electronic Journal of University of Aden for Basic and Applied Sciences, 7*(1), 67–73. https://doi.org/10.47372/ejua-ba.2026.1.500

Use: direct Yemen occupational chemical-exposure context.

### YEM-06 — Laboratory occupational injuries / chemical exposure
Al Eryani, Y. M., Nooradain, N., Alsharqi, K., Murtadha, A., Al Serouri, A., & Khader, Y. (2019). Unintentional injuries in the three reference laboratories: Sana'a, Yemen. *International Journal of Preventive Medicine, 10*, 174. https://doi.org/10.4103/ijpvm.IJPVM_160_17

Use: local occupational injury context including reported chemical exposure. Do not extrapolate to national incidence.

---

## 6. WHO / ILO authoritative burden sources

### AUTH-01 — WHO ambient air pollution
World Health Organization. (2024, October 24). *Ambient (outdoor) air pollution*. WHO.

Permitted deck facts from WHO source:
- 99% of the world's population in 2019 lived in places where WHO air-quality guideline levels were not met.
- combined ambient and household air pollution are associated with about 6.7 million premature deaths annually.
- ambient outdoor air pollution was estimated to cause about 4.2 million premature deaths worldwide in 2019.
- 89% of ambient-air-pollution premature deaths occurred in low- and middle-income countries.

### AUTH-02 — WHO public-health impact of chemicals
World Health Organization. (2016; 2021 addendum using 2019 data). *The public health impact of chemicals: Knowns and unknowns*. WHO.

Permitted deck fact:
- WHO estimated approximately 2 million lives and 53 million DALYs lost in 2019 from exposures to selected chemicals; the burden estimate covers only a subset of chemical exposures.

### AUTH-03 — ILO global occupational burden
International Labour Organization. (2023, November 26). *Nearly 3 million people die of work-related accidents and diseases*. ILO.

Permitted deck facts:
- about 2.93 million workers die annually from work-related factors;
- about 2.6 million deaths are associated with work-related diseases and about 330,000 with occupational accidents;
- about 395 million workers sustain non-fatal work injuries annually.

Restriction: **never state that the 2.93 million deaths are caused specifically by chemical exposure.**

### AUTH-04 — ILO Safety and Health at Work portal
International Labour Organization. (2026 access). *Safety and health at work*. ILO.

Use: current official summary figures and OSH policy context.

---

## 7. Slide citation strategy

- Main content slides: compact IDs such as `[SCI-01]`, `[AUTH-03]`, `[YEM-05]` in 10–12 pt footer/source strip or speaker notes, depending final readability.
- References section: full APA 7 entries distributed across multiple reference slides if necessary; do not shrink below conference-readable minimum merely to force all references onto one slide.
- Data/figure slides: source ID attached directly to figure/caption.
- Internal implementation diagrams: cite `INT-01/INT-02/INT-03` as appropriate.

---

## 8. Metadata conflicts / unresolved items that do not block P1

1. AUT-03: DOI seen on an author-uploaded copy is not independently resolved against publisher metadata; omit DOI for now.
2. AUT-04: volume/issue notation differs between publisher/index records; DOI/title/year/pages are stable enough for working deck, final volume/issue must be rechecked before P10.
3. ARID numeric ID not directly verified; do not display.
4. ResearcherID not verified; do not display.
5. Some dataset records (e.g., WIJNEN/BACTERIA_ODOR) need final publisher metadata only if they are selected for the final reference slides.

These are tagged as metadata-cleanup items, not blockers to storyboard development.

---

## 9. P1 completion decision

P1 exit criteria are met for presentation development:
- implementation evidence is traceable;
- core ChemCore/MQ scientific sources have DOI/title metadata;
- presenter research and Yemen-context sources are registered;
- WHO/ILO burden sources are separated from device-performance evidence;
- source-use limitations are explicit;
- a working APA 7 bibliography is available.

**Next phase: P2 — prior art, comparable products, and cost evidence.**
