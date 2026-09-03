# HazRob 0.9 — Scientific Library Integration Audit

This audit records what is actually connected to the HazRob 0.9 runtime. It deliberately distinguishes registered sources, Android-native implementations and quantitative calibration.

## Installed MQ topology

The application source of truth is `HazRobHardwareProfile.MQ` and contains all eight installed channels:

- R1 / ADS1 0x48: MQ2 A0, MQ4 A1, MQ5 A2, MQ8 A3
- R2 / ADS2 0x49: MQ3 A0, MQ7 A1, MQ9 A2, MQ135 A3

The Measurements screen must always render eight cards. Relay power changes card state (`OFF -> WARMUP -> READY`) rather than removing sensors from the screen.

## Curve layers

### Operational inherited power-law layer

`GasEngine` currently registers 39 sensor/analyte equations across all eight MQ sensors. These equations remain generic evidence unless a device-specific validated calibration supports quantitative interpretation.

### Secondary research/MQDataScience graph-fit layer

`ResearchCurveRegistry` currently registers 43 graph-fit records across all eight installed sensors. Source provenance is MQDataScience/datasheet-derived graph fitting. HazRob 0.9 now consumes non-duplicate sensor/analyte pairs from this registry as low-weight fingerprint evidence.

Rules:

- Never duplicate a pair already represented by `GasEngine`.
- Never extrapolate outside the declared curve range.
- MQ7/MQ9 research estimates require a valid heater phase/cycle.
- Research-only evidence cannot make a result quantitative by itself.

## Device-specific calibration

`SqliteCalibrationProvider` reads active calibration curves and points from SQLite. HazRob 0.9 now passes this provider into `ValidatedGasEngine` in the active Measurements/Analysis flow and when saving normal measurement sessions.

Priority is therefore:

1. device-specific reference-gas calibration (highest analytical weight),
2. generic registered operational curve evidence,
3. non-duplicate research/MQDataScience graph-fit evidence (low-weight fingerprint only).

R0 calibration, analyte calibration and quantitative PPM validity remain separate concepts.

## MQDataScience 76-model catalogue

Pinned upstream source:

- Repository: `abcdaaaaaaaaa/MQDataScience`
- Version: 6.0.0
- Commit: `e88f876140587a708ee84a4d4d3acbd5720274ee`
- Ordered source catalogue: `DataScience/Requirements/PredictData.py`

The upstream implementation uses NumPy, SciPy, scikit-learn, statsmodels, XGBoost, LightGBM, CatBoost and pyGAM. Therefore HazRob does not label an algorithm executable on Android merely because its name is registered.

Current runtime status:

- Adopted/catalogued: 76 / 76
- Android-native: 14
- Upstream-verified Android ports: 12
- Android approximation implementations: 2
- Adopted / port pending: 62

Current Android-native model family:

- Linear Regression
- Weighted Linear / FGLS
- Huber
- Theil-Sen
- Ridge
- Lasso
- Elastic Net
- Deming
- Polynomial degree 2
- Polynomial degree 3
- Polynomial degree 4
- Exponential
- Logarithmic
- Power

The remaining adopted models stay visible in the developer model catalogue as `PORT PENDING`; they are not silently substituted by simpler algorithms.

## Physical Measurements screen

The Physical sub-tab renders the complete 15-entry `PhysicalCatalog` as a persistent table with value, unit and status/source. Currently available physical sources are filled when present:

- Temperature
- Humidity
- Pressure
- Derived altitude from valid pressure
- MAX4466 RMS voltage
- MAX4466 peak-to-peak voltage
- IR state

Uninstalled sensors remain visible as `غير مركب`. MAX4466 voltage is not mislabeled as calibrated dB SPL, and threshold-only LDR data is not mislabeled as lux.

## Regression protection

`HazRobScientificLibraryAuditTest` fails CI if:

- the installed MQ profile is no longer eight channels,
- the 39 operational curves change unexpectedly,
- the 43 secondary research curves change unexpectedly,
- any installed sensor loses all operational or research curves,
- the MQDataScience 76-model catalogue/runtime counts drift without review,
- the device calibration provider is no longer consulted by AnalyticsSnapshot,
- or a research-only curve can no longer contribute low-weight fingerprint evidence.

## Heater limitation

MQ7 and MQ9 quantitative use remains restricted until firmware/hardware exposes and validates the heater protocol/phase required for the exact purchased sensor/module. Their existence in the library does not override this physical limitation.
