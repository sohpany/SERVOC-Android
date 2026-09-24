# HazRob 0.10.2 — dev branch conflict audit

Status: CLOSED for selective migration review
Target: `release/hazrob-0.10.2`
Compared source: `dev/hazrob-0.10.1`

## Decision

Do not merge `dev/hazrob-0.10.1` wholesale into 0.10.2. The branches are historically divergent, but the inspected dev-side changes do not contain a proven fix that should replace the approved HR067/HR066-derived 0.10.2 baseline.

## Verified identical critical components

The following were SHA-identical across the compared refs when audited:
- Main runtime: `firmware/HazRob/HazRob.ino`, `Hardware.cpp`, `RemoteLink.cpp`, `RemoteLink.h`
- Android transport/control: `RobotApi`, `CameraApi`, `JoystickView`, `RobotConnectionSetupTab`, `HazardOverlayView`, AndroidManifest and base styles
- Scientific core: `ValidatedGasEngine`, `CandidateRanker`, `AnalyticsSnapshot`
- Fingerprints/models: `FingerprintTrainer`, `ChemicalFingerprint`, `FingerprintClassifier`, `MqDataScience76Catalog`, `MqDataScienceModelProfile`, `ModelRuntimeRegistry`, `CalibrationRegressionModels`, `CurveProvenance`, `ResearchCurveRegistry`
- Data/exposure: `ExperimentsDb`, `ChemicalRepository`, `ChemicalSeedBootstrap`, `ExposureSeedBootstrap`, `ExposureSeedImporter`, `ExposureRepository`
- Scientific pinning workflow: `.github/workflows/pin-scientific-data.yml`

## Keep 0.10.2

0.10.2 contains the newer integrated analytical/reporting architecture and must not be replaced by older dev copies:
- `ChemCoreFeaturePipeline` is the canonical feature source; compatibility layers delegate to it.
- `TemporalFeatureExtractor` and `SignalFeatures` avoid independent feature formulas.
- `ChemCoreInferenceEngine` includes calibration-health gating before M4 validation.
- `ChemCoreResultEnvelope` uses the newer result/evidence/quality contract.
- `GasAlertCoordinator` uses the unified live gas state engine without converting screening into authoritative occupational ppm.
- `ReportExporter`, `AnalyticalCharts`, and `XlsxReportWriter` contain the newer session-bound ChemCore reporting, 144 screening, provenance, evidence and gas-safety outputs.
- `ControlTab` contains the newer dual-tank-drive UI and must not regress to the older dev control surface.

## Firmware safety decision

No dev firmware runtime change is required.
- Direct remains boot/default and AP-only.
- Remote is explicit AP+STA only.
- Saved WAN credentials do not auto-enable STA/MQTT/Render.
- Returning to Direct revokes remote authority and disables remote transport/NAPT while retaining WAN credentials unless explicitly cleared.
- Direct drive is rejected in Remote mode.
- E-stop is enforced before drive.
- Command-loss timeout remains 500 ms.
- L298N mapping remains LEFT EN17/IN19/IN32 and RIGHT EN16/IN18/IN27 with no inversion.
- CAM remains STA-only toward the HazRob AP; this is not the Main ESP32 WAN STA path.

`Config.h` and CAM release identity are intentionally 0.10.2 instead of 0.10.1.

## Migration disposition

- IDENTICAL: no action.
- KEEP 0.10.2: retain newer release implementation.
- REJECT DEV: older release identity/report/control implementations that would regress 0.10.2.
- REVIEW-PORT: none found during this audit.
- Firmware behavior changes from dev: none required.

## Remaining release work

This audit does not constitute final physical acceptance. Remaining work is independent:
1. Coordinate remote-service/Render release identity without changing protocol 2 unless the wire contract changes.
2. Keep HiveMQ topic/ACL/protocol contract unchanged unless a real protocol requirement is introduced.
3. Validate Railway artifacts and record hashes.
4. Perform physical Direct/Remote/camera/E-stop/command-loss/reconnect acceptance before immutable `v0.10.2` tagging.
