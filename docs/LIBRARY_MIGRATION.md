# SERVOC analytical-library migration

The Android analytical layer reuses project logic from the ESP32 source instead of duplicating independent formulas.

| ESP32 project source | Android module | Migration |
|---|---|---|
| MeasurementCatalog.h | chemical/ChemicalCatalog.java | 144 chemical IDs, display order, names/formulas |
| MeasurementCatalog.h | measurements/PhysicalCatalog.java | 15 physical measurements and units |
| GasProfiles.h | chemical/GasEngine.java | MQ sensor mapping, 39 Rs/R0-to-PPM curves |
| GasAnalyzer.cpp/.h | chemical/GasEngine.java | log-median consensus, MAD agreement, confidence stars |
| ExposureLimits.h | exposure/ExposureLimits.java | source-backed TWA/STEL/Ceiling/IDLH/LEL screening references |
| MQArray.cpp/.h | calibration/CalibrationProfile.java | Android-side calibration metadata model; real R0 calibration still required |

## Added Android-side analytical modules

- fingerprint/ChemicalFingerprint.java
- fingerprint/FingerprintTrainer.java
- fingerprint/FingerprintClassifier.java
- simulation/AnalyticalSimulator.java
- analytics/LibraryStatus.java
- data/ExperimentsDb.java schema v2

The fingerprint classifier operates in log10(Rs/R0) space and is designed for controlled training sessions. Synthetic simulator fingerprints must never be treated as real chemical signatures.

## Safety/validation rule

The current ESP32 source contains placeholder R0 values. The application therefore labels OEL/LEL results as screening references rather than compliance alarms until calibration and independent validation are complete.
