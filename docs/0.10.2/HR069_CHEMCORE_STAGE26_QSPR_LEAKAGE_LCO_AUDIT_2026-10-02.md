# HazRob 0.10.2 / HR069 — Stage 26 QSPR Leakage / Leave-Compound-Out Audit

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: ChemCore source-backed MQ-QSPR validation.

## Core finding
The QSPR runtime bench is trained from deterministic concentration points generated from accepted source-backed M2 response relations. Multiple concentration points from one compound are therefore not independent experimental samples and must never be split randomly between training and validation.

## Leakage-resistant split
`QsprValidationProtocol` now uses `leave-compound-out/2-compound-balanced`.

For each held-out chemical UID:

- every concentration row from that compound is removed from training;
- the model is fitted only on the other compounds;
- every valid held-out point is predicted;
- the fold is summarized independently before pooled statistics are calculated.

This prevents a curve from a compound appearing on both sides of the validation split merely through different concentration samples.

## Validation metrics
The protocol records:

- compounds attempted;
- compounds successfully evaluated;
- total held-out predictions;
- pooled RMSE in log10(Rs/R0);
- pooled MAE;
- pooled bias;
- fraction inside the model training envelope;
- mean held-out-compound RMSE;
- worst held-out-compound RMSE;
- worst held-out-compound absolute bias.

The runtime registry now requires all attempted training compounds to be evaluable. A sensor-QSPR model is enabled only when all of the following pass:

- at least 5 distinct training compounds;
- all held-out compounds evaluated;
- at least 4 held-out predictions per evaluated compound in aggregate;
- pooled RMSE <= 0.60 log10;
- pooled absolute bias <= 0.40 log10;
- worst-compound RMSE <= 0.80 log10;
- worst-compound absolute bias <= 0.60 log10;
- inside-envelope fraction >= 0.50.

These are conservative research gates, not proof of clinical/industrial metrology performance.

## Same-sensor self-training analysis
For a given MQ sensor, runtime QSPR training includes only catalogue compounds that already have an eligible accepted M2 direct/reference model on that sensor. Therefore:

- if a candidate lacks a direct model on that sensor, it could not have supplied that sensor's own synthetic training curve;
- if a candidate has a direct model on that sensor, the direct M2 model supersedes M1 QSPR during candidate supplementation.

This substantially limits direct same-sensor self-leakage in runtime inference.

## Remaining correlation that must not be misrepresented
The above does not make QSPR statistically independent evidence. The fitted relationship is still learned from the same source-backed response library that supplies M2 curves. PubChem descriptors add molecular explanatory variables, but they do not convert the resulting prediction into an independent experimental measurement.

Consequently all sensor-specific QSPR models share a single QSPR evidence lineage/independence group, and Stage 27 must explicitly map their correlation with model-derived curve and fingerprint evidence.

## Independent-session field
The runtime synthetic training label has been changed to `source-model-M2` rather than language suggesting an independent laboratory session. True independent-session validation remains a future HR069 experimental requirement.

## Verification
Unit tests now assert compound-level fold diagnostics in addition to pooled LCO metrics. The RC2 Android unit-test and test-APK workflow passed at source head `2cda7bd744ad666ec33df6585e85fd0aa10a0329`.

## Stage status
Stage 26: COMPLETE for leakage-resistant model-transfer validation policy. Promotion beyond M1 still requires independent HR069 experimental validation on unseen compounds and environmental conditions.
