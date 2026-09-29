# External MQ fingerprint datasets — HazRob 0.10.2 / HR069

Status: research-source registry only. These datasets are **not** treated as HazRob field calibration, safety-grade PPM, or M4 evidence.

## Scope and acceptance rule

Only datasets with an explicitly documented MQ-series sensor channel are admitted here as external fingerprint research sources. Sources based exclusively on other MOS sensor families (for example Figaro TGS arrays) may still be useful for algorithm/drift methodology, but they are excluded from this MQ fingerprint-source registry.

HazRob HR069 installed MQ array: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9, MQ135.

External data may be used for offline feature engineering, classifier benchmarking, cross-sensitivity studies, drift/mixture experiments, and prior/candidate generation. It must not be silently converted into authoritative HazRob concentration calibration because sensor instance, heater drive, load resistor, supply, chamber geometry, flow, temperature, humidity, ageing and acquisition electronics can all shift the response distribution.

## Verified MQ datasets

### MQDS-001 — Gas Sensor Measurement / Gas-Sensors-Measurements-Dataset

- Upstream: https://github.com/TakMashhido/Gas-Sensors-Measurements-Dataset
- Pinned upstream commit: `a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4`
- Citation DOI: `10.5281/zenodo.13283720`
- Release: 2024-08-09, version 1.0.0
- License: MIT (as declared by the upstream repository)
- Data file: `Gas_Sensors_Measurements.csv`
- MQ channels: MQ2, MQ3, MQ5, MQ6, MQ7, MQ8, MQ135
- Labels documented upstream: NoGas, Smoke, Perfume, Mixture
- HR069 overlap: 6/8 installed channels (MQ2, MQ3, MQ5, MQ7, MQ8, MQ135); MQ6 is external-only; MQ4 and MQ9 are absent.
- HazRob role: high-priority **external classification/fingerprint benchmark**; not direct PPM calibration.

### MQDS-002 — Food Freshness Electronic Nose Dataset

- Record: https://zenodo.org/records/17285312
- DOI: `10.5281/zenodo.17285312`
- Version: v1, published 2025-10-07
- Creator: Mehrab Mahdian
- Archive: `Food-Freshness-E-Nose-Dataset.zip`
- Published archive size: 10.3 MB
- Published MD5: `008b8ccb62d7a7d51ff9ffcfd698a1e5`
- MQ channels: MQ2, MQ3, MQ4, MQ5, MQ6, MQ7, MQ8, MQ9, MQ135
- Sampling information published by Zenodo: approximately 23 °C, 3-minute sampling intervals, samples taken at 24-hour intervals.
- HR069 overlap: **8/8 installed channels**, plus MQ6.
- HazRob role: highest-priority external time-series/e-nose source for testing the HR069 eight-channel fingerprint pipeline and transient feature extraction.
- Redistribution note: the Zenodo record is retained as the canonical data host. Do not vendor the binary archive into this repository until the record's redistribution/license metadata is explicitly confirmed and preserved.

### MQDS-003 — UCI Gas sensor array low-concentration

- Record: https://archive.ics.uci.edu/dataset/1081/gas%2Bsensor%2Barray%2Blow-concentration
- DOI: `10.24432/C5CK6F`
- License: CC BY 4.0
- Dataset size: 90 samples; six gases; three concentration levels (50, 100, 200 ppb); five replicates per gas/concentration.
- Sensor array includes: TGS2603, TGS2630, TGS813, TGS822, **MQ-135**, MQ-137, MQ-138, 2M012, VOCS-P, 2SH12.
- Gas classes: ethanol, acetone, toluene, ethyl acetate, isopropanol, n-hexane.
- Acquisition: 1 Hz; baseline 5 min, injection 10 min, cleaning 15 min; released data contain baseline + injection response.
- HR069 overlap: MQ135 only (1/8).
- HazRob role: partial-compatibility research source for low-concentration time-series methodology and MQ135 behavior; never treated as an eight-channel HazRob fingerprint.

## Deliberately excluded from the MQ fingerprint registry

The UCI Gas Sensor Array Drift / Drift at Different Concentrations datasets are valuable for drift-compensation methodology, but their sensor array is not an MQ-series HazRob array. They therefore belong in algorithm/method references, not in the external MQ fingerprint dataset registry.

## Provenance and storage policy

1. The canonical upstream location, DOI, version/commit and published checksum are the source of truth.
2. External datasets remain `RESEARCH_EXTERNAL` evidence. They cannot promote a HazRob result to quantitative/M4 status.
3. Raw-data snapshots may be mirrored only when redistribution terms are explicit and the original license/citation are stored with the snapshot.
4. Any preprocessing must create a derived artifact with its own schema version, transform log and checksum; never overwrite the upstream raw representation.
5. Sensor-channel mapping must be explicit. Missing HazRob channels remain missing; no synthetic channel may be presented as measured evidence.
6. Dataset labels such as Smoke/Perfume/Food freshness are classification classes, not pure-compound concentration truth unless the source explicitly provides reference-gas concentration.

## Proposed next integration stage

Before importing any source into the active fingerprint classifier, build an adapter that maps each dataset into a common research schema containing: source ID, sample/session ID, timestamp or sample index, sensor key, raw value, optional Rs/R0/log-ratio, temperature, RH, known analyte/class, known concentration/unit, acquisition phase, and provenance checksum. Adapters must preserve source-native data and mark unavailable fields as missing rather than inventing values.
