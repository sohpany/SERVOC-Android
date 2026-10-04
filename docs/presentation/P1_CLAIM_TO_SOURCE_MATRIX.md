# P1 — HazRob Master Deck Claim-to-Source Matrix

Status: **COMPLETE — working claim governance**
Tracking issue: #9
Branch: `docs/hazrob-master-deck`

## Purpose
Map the principal planned presentation claims to evidence sources and constrain wording so the master deck remains scientifically defensible and patent-discussion ready.

| Claim / slide theme | Primary sources | Allowed wording | Prohibited / caution |
|---|---|---|---|
| HazRob is an integrated robotic hazardous-gas sensing and analytical platform | INT-01, INT-02 | `HazRob integrates mobile sensing, telemetry, analysis, decision support and reporting.` | Do not call it commercially certified or validated beyond available evidence. |
| HR069 uses a multi-channel MQ/MOX array | INT-01, INT-03, INT-05 | Name the installed sensor set and explain cross-sensitive array operation. | Do not imply individual MQ sensors are selective analyte detectors. |
| Rs/R0 normalization is a core analytical representation | INT-03, INT-01 | `ChemCore normalizes sensor response as Rs/R0 and uses log10(Rs/R0) in analytical models/features.` | R0 is not zero, not maximum value, and not automatically M4 calibration. |
| Temporal fingerprinting adds information beyond steady-state response | INT-03, SCI-02, SCI-10 | Describe baseline, exposure, slopes, response/recovery timing, AUC/SNR and feature vector extraction. | Do not claim clinical/metrological accuracy from temporal features without validation. |
| Multi-channel fingerprints can support array-consistent M3 identity | INT-03, INT-01, SCI-01, SCI-10 | `Accepted multi-channel array consistency can support M3 identity under ChemCore gates.` | M3 must never be described as device-calibrated quantitative M4. |
| ChemCore performs joint inverse multi-sensor fitting | INT-03, INT-01 | Explain candidate + concentration hypothesis → predicted responses → weighted residual minimization across channels. | Do not describe ranking weight as calibrated probability of presence. |
| Open-Set/OOD prevents forced classification | INT-03, INT-01 | `OOD/Open-Set gates can reject samples outside the known feature domain and preserve Unknown/Screening outcomes.` | Do not claim externally validated OOD threshold performance until deferred validation campaign is completed. |
| AMCI is adaptive/closed-loop analysis | INT-03, INT-01 | `AMCI can extend, repeat, recover or request additional evidence according to analysis state/policy.` | Do not claim optimal experiment design or autonomous chemical certainty unless demonstrated. |
| Evidence tiers M0–M4 separate knowledge from device validation | INT-03, INT-01 | Explain M0 catalog, M1 inferred, M2 literature/manufacturer, M3 array consistency, M4 HazRob-validated. | Never collapse evidence tier into confidence probability. |
| QSPR is supplementary molecular inference | INT-03, INT-01 | `QSPR can provide M1 transfer/inference when applicability-domain criteria are met.` | QSPR alone cannot grant M3/M4 or reportable PPM. |
| Evidence fusion is lineage-aware | INT-03, INT-01 | `ChemCore avoids double-counting correlated derivatives that share upstream scientific lineage.` | Do not present analytical confidence index as calibrated posterior probability. |
| Mixture/interference guard prevents over-reporting poor pure-compound fits | INT-03, INT-01, SCI-02 | `The current system can flag possible mixture/interference or ambiguity.` | Do not claim it currently identifies individual components of arbitrary mixtures. |
| Reportable PPM is gated | INT-03, INT-01 | `Professional/reportable PPM requires the M4 quantitative gate, calibration health and domain acceptance.` | Literature/reference curves are not HR069 M4 calibration. |
| HazRob incorporates external scientific evidence through controlled provenance | INT-04, INT-05, INT-06, INT-07 | Show Paper/Dataset → provenance → grade → sensor mapping → fingerprint/curve → runtime evidence. | Do not imply every source is independently reproduced on HR069. |
| Prior literature supports multi-sensor gas classification/concentration approaches | SCI-01, SCI-02, SCI-03, SCI-04, SCI-09, SCI-10 | `Prior studies demonstrate feasibility of multi-sensor e-nose classification, concentration modelling and transient fingerprints.` | Do not claim these studies establish HazRob novelty or validate HazRob performance. |
| HazRob addresses a real global occupational-safety need | AUTH-03, AUTH-04 | State ILO work-related deaths/injuries exactly with source year/context. | Do not attribute all ILO deaths to chemical exposure. |
| Chemical exposure is a major public-health burden | AUTH-02 | State WHO estimate for selected chemical exposures and note incomplete coverage. | Do not combine with ILO numbers as if mutually exclusive or additive. |
| Air pollution is a major global health burden | AUTH-01 | Use WHO 99%, 6.7M combined pollution, 4.2M ambient figures with year/context. | Do not imply HazRob alone addresses all air-pollution sources or particulate burden. |
| Yemen has documented OSH-system weaknesses | AUT-01, AUT-03 | `Published Yemen research identifies gaps in OSH education/system capacity.` | Avoid national prevalence claims beyond study design. |
| Yemen has documented occupational chemical-exposure examples | AUT-02, YEM-05, YEM-06 | Cite medical-waste governance, benzene exposure, and laboratory chemical exposure as contextual evidence. | Do not extrapolate a single-city/study sample to all Yemen. |
| Yemen has documented environmental measurement challenges | YEM-01, YEM-02, YEM-03, YEM-04 | Use as examples of air, particulate, radon and industrial-pollution measurement needs. | These studies do not validate HazRob sensors or ChemCore. |
| Project owners have relevant published research background | AUT-01–AUT-04 | Mention selected publications/ORCID on presenter slide or context slide. | ARID/ResearcherID numbers remain excluded until verified. |
| HazRob may contain multiple patent-candidate invention families | INT-01–INT-07 + P2 prior art | Use `candidate invention family`, `technical differentiator`, `subject to prior-art/legal review`. | Do not state `patented`, `first`, `unique`, or `novel` as legal conclusions without patent search/counsel. |
| Comparable industrial systems can have high acquisition cost | P2 product/cost register | Only quote published/list/distributor prices with date/currency or state `price on request`. | Never use unsourced adjectives such as `very expensive` as a factual market claim. |

## Visual evidence rules

- Every numeric chart must have source ID(s) directly on the slide or in speaker notes.
- Concept diagrams must be labeled `schematic` where they could otherwise be mistaken for measured data.
- Simulated example curves/radar plots must be labeled `illustrative` unless derived from actual project/reference data.
- Product photographs must not imply endorsement or direct equivalence; captions should state `comparator / related commercial system`.
- Patent-family diagrams must separate `implemented`, `research-supported`, `proposed`, and `future` layers.

## P1 decision

This matrix is sufficient to govern the slide storyboard. Any new claim introduced during P4–P6 must be added here or mapped to an equivalent evidence rule before final QA.
