# ChemCore Feature Pipeline Migration

## Canonical contract
All scientific time-domain MQ feature extraction in HazRob Android uses `ChemCoreFeaturePipeline` and schema `chemcore-features/1`.

## Compatibility paths
- `TemporalFeatureExtractor` is deprecated and delegates to the canonical pipeline. Its 8-value legacy vector is a projection, not a second scientific definition.
- `SignalFeatures` is deprecated and delegates to the canonical pipeline for positive Rs/R0 traces.
- Report fingerprint charts are derived from canonical `abs_delta_log10` and no longer calculate an independent baseline/steady fingerprint.

## Semantics
Canonical input is positive Rs/R0. Valid samples are transformed to log10(Rs/R0). Physical time in seconds is used when valid; otherwise sample-index time is explicit in the result. The canonical 17 features remain the only training/reference/runtime feature contract.

## Migration rule
New inference, reference, report, visualization, export, or model code must not introduce local feature equations. It must consume `ChemCoreFeaturePipeline.Result` or persisted `chemcore-features/1` values. Legacy adapters may remain only while required for compatibility and must be tested against the canonical values.

## Firmware boundary
This migration is Android/ChemCore-only. No ESP32 or ESP32-CAM firmware changes are part of the feature pipeline work.
