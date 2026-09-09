# ChemCore / AMCI — Peer-Reviewed Research Reference Supplement (2024–2026)

This supplement extends `REFERENCES_APA.md` for HazRob 0.10.0. It records the recent peer-reviewed literature that materially informed the ChemCore/AMCI architecture. References are presented in APA-style form as closely as practicable from publisher/index metadata available at the time of the 0.10.0 research freeze.

> **Scope note:** Results reported by the cited studies were obtained on their own sensor materials, arrays, datasets and experimental conditions. Their reported performance metrics must not be transferred to HazRob. HazRob uses the literature to justify methods and design choices, not to claim equivalent accuracy.

## Molecular and gas-sensing descriptors / QSPR direction

Guo, Y., Yang, M., Huang, G., & Zheng, Y. (2024). Machine-learning-enabled exploitation of gas-sensing descriptors: A case study of five pristine metal oxides. *Chemical Engineering Journal, 492*, 152280. https://doi.org/10.1016/j.cej.2024.152280

**HazRob relevance:** supports using gas/material descriptors and operating conditions as predictive inputs rather than treating response curves as isolated empirical objects. This informed the molecular-descriptor layer and QSPR-MOX research path.

Su, M., Guo, Y., Hong, X., & Zheng, Y. (2025). Machine-learning for discovery of descriptors for gas-sensing: A case study of doped metal oxides. *Talanta, 287*, 127594. https://doi.org/10.1016/j.talanta.2025.127594

**HazRob relevance:** literature-derived gas-sensing data, descriptor importance and interpretable ML support ChemCore's molecular/electronic descriptor contract and evidence-tiered predicted response priors.

## Pre-processing and transient feature engineering

Vajdi, M. (2025). Metal oxide semiconductor based electronic nose data pre-processing, review. *Engineering Applications of Artificial Intelligence, 149*, 110540. https://doi.org/10.1016/j.engappai.2025.110540

**HazRob relevance:** supports treating overlapping MOS responses as a multivariate electronic-nose pattern, retaining transient information and separating acquisition/pre-processing/feature engineering from downstream classification. This informed `chemcore-features/1` and the controlled baseline/exposure/recovery pipeline.

## Mixtures and cross-reactive arrays

Singh, S., S, S., Varma, P., Sreelekha, G., Adak, C., Shukla, R. P., & Kamble, V. B. (2024). Metal oxide-based gas sensor array for VOCs determination in complex mixtures using machine learning. *Microchimica Acta, 191*, 196. https://doi.org/10.1007/s00604-024-06258-8

**HazRob relevance:** reinforces that cross-reactive array responses can depart from single-gas behavior in mixtures and that mixture analysis requires mixture-specific evidence. This supports HazRob's conservative residual-based `POSSIBLE_MIXTURE_OR_INTERFERENCE` guard rather than assuming pure-gas additivity.

## Long-term drift and longitudinal datasets

Wörner, J., Eimler, J., & Pein-Hackelbusch, M. (2025). Long-term drift behavior in metal oxide gas sensor arrays: A one-year dataset from an electronic nose. *Scientific Data, 12*, 1628. https://doi.org/10.1038/s41597-025-05993-8

**HazRob relevance:** supports longitudinal validation, explicit drift tracking, raw time-series retention, environmental/sampling provenance and the separation of model-development data from later temporal validation.

Li, R., Li, Z., Kofi, B. A., Sun, J., He, Y., & Jiao, M. (2026). A comprehensive review of algorithms for drift compensation in metal oxide semiconductor gas sensor arrays. *Chemosensors, 14*(6), 143. https://doi.org/10.3390/chemosensors14060143

**HazRob relevance:** supports the DriftCore roadmap and the need for explicit transfer/domain-adaptation evaluation instead of assuming a calibration or classifier remains invariant over sensor age and environment.

## Open-set recognition / UNKNOWN handling

Liu, H., Yao, Y., Liu, C., Mou, Q., Li, C., & Chen, B. (2026). Gas open-set recognition and drift compensation via weighted adversarial fusion-domain adaptation network. *Knowledge-Based Systems, 331*, 114767. https://doi.org/10.1016/j.knosys.2025.114767

**HazRob relevance:** supports treating unknown-gas rejection and drift as first-class deployment problems. HazRob 0.10 therefore never requires the classifier to force every observation into a known class.

Chen, S., Song, Y., Wang, C., & Wang, Z. (2026). A universal and robust framework for multiple gas recognition based-on spherical normalization-coupled Mahalanobis algorithm. *Sensors and Actuators B: Chemical, 467*, 140444. https://doi.org/10.1016/j.snb.2026.140444

**HazRob relevance:** supports Mahalanobis/open-set geometry as a defensible baseline family. HazRob does not inherit the paper's reported benchmark performance; its thresholds must be calibrated on independent HazRob evidence.

## Uncertainty, selective classification and confirmatory measurement

Eo, S., Eum, J.-H., Kwon, S.-J., Sagong, M., Kim, D., & Kim, D.-H. (2026). Uncertainty-aware deep ensembles for robust and reliable chemical sensor arrays. *Advanced Science, 13*. https://doi.org/10.1002/advs.76134

**HazRob relevance:** supports explicit predictive uncertainty and reliability-aware decisions. ChemCore/AMCI uses uncertainty as a reason to extend/repeat/recover/reject rather than as a cosmetic confidence score.

Du, S., Chen, X., Wang, H., Guo, Y., & Ai, B. (2026). Coverage-aware calibration and selective classification for drifted gas-sensor arrays: A controlled shift simulation benchmark with longitudinal evaluation. *Journal of Chemometrics*. https://doi.org/10.1002/cem.70148

**HazRob relevance:** supports reporting accepted-sample performance together with coverage/selective rejection, and reinforces the rule that low-confidence samples should be deferred instead of forced into an identity.

## Calibration transfer / inter-unit variability

Sumanto, B., Triyana, K., & Kusumaatmaja, A. (2026). Optimized calibration for metal oxide gas sensor arrays to overcome tool variability. *Instrumentation Science & Technology, 54*(1), 118–135. https://doi.org/10.1080/10739149.2025.2504478

**HazRob relevance:** supports treating sensor-unit variability and calibration transfer as validation problems. Device-specific M4 status remains distinct from generic manufacturer/literature evidence.

## Design implications locked in HazRob 0.10.0

The literature above is reflected in the following release contracts:

1. cross-reactive 8-MQ signals are analyzed as a multivariate electronic-nose pattern;
2. baseline and transient kinetics are preserved in a canonical feature schema;
3. molecular/physical descriptors are priors/features, not direct molecular spectroscopy;
4. QSPR/model-predicted curves are labeled M1/M2 evidence and cannot impersonate device calibration;
5. `UNKNOWN`/open-set rejection is mandatory for safety-oriented inference;
6. uncertainty can trigger AMCI extension, recovery, repetition or spatial confirmation;
7. mixtures/interferents are guarded by residual consistency rather than assumed additive pure-gas calibration;
8. longitudinal drift, inter-unit variability and environmental domain are model-validity concerns;
9. M4 quantitative interpretation remains contingent on HazRob-specific metrological validation.

## Provenance note

This supplement was reviewed for **HazRob 0.10.0 on 9 September 2026**. The canonical legacy/manufacturer/standards/software reference set remains in `REFERENCES_APA.md`; this file adds the recent ChemCore-specific research basis.
