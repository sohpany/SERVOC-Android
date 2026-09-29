# HR069 accepted transfer fingerprints — checkpoint

Date: 2026-09-29  
Release: 0.10.2  
Hardware revision: HR069  
Branch: `release/hazrob-0.10.2`

## Project decision

Available source-backed measurements, fingerprint datasets and PPM relations are accepted for current research identification, candidate ranking and provisional/reference concentration work. They may be recalibrated or replaced by stronger HR069 measurements later.

External/reference data do not self-promote to M4. Occupational/safety-grade quantitative interpretation remains gated by device/reference-gas validation and the existing ChemCore M4 rules.

## Sensor conditioning

The eight installed MQ sensors completed the recorded 48 h initial preheat campaign `HR069-MQ-48H-PREHEAT-2026-09` and are treated as ready for normal/raw/empirical acquisition.

MQ-7/MQ-9 remain a separate hardware-protocol case: their 48 h initial preheat does not reproduce the manufacturer high/low heater cycle. Their raw signals may be captured for empirical fingerprints, but manufacturer-cycle-dependent source curves remain fail-closed until cyclic-heater hardware/protocol validity is available.

## Runtime/reference layer already active

- 43 MQDataScience/datasheet-derived response curves are preserved.
- 6 preferred peer-reviewed MQ135 Rs/R0 curves are preserved.
- 76 MQDataScience model families are registered.
- 11 pure analyte numeric fingerprint families are source-backed: CO, NH3, CO2, benzene, H2, CH4, propane, n-hexane, toluene, ethanol, acetone.
- 2 operational numeric classes are retained: LPG and Smoke.
- `AcceptedReferenceResponseModel` accepts source-backed PPM libraries as provisional/reference fitting evidence using source priority and no extrapolation.
- `AcceptedFingerprintIdentityEngine` may promote separated multi-channel source-backed matches to IDENTIFIED/M3 but never creates M4.

## Newly committed transfer assets

### MQDS-001 — multi-MQ class fingerprints

Asset: `research_data/hr069_analytical_library/multimodal_mq_transfer_fingerprints_v1.json`

- source file SHA-256: `13c2c85cfbb23357a71555b9bc2cbe7dd68e931ffc94df30389c1a4631203745`
- derived asset SHA-256: `6e41294da4fbaed3e390dc2d5daff576dff07d1c67887d1d794012dea602ff8f`
- 6400 measurements, 1600/class
- classes: NoGas, Smoke, Perfume, Mixture
- HR069 overlap: MQ2, MQ3, MQ5, MQ7, MQ8, MQ135 = 6 channels
- MQ4/MQ9 are intentionally missing; no synthetic channels were created
- stored shapes: cross-sensor geomean-normalized and class-mean/NoGas-normalized log10 vectors
- acceptance: `ACCEPTED_TRANSFER_CLASS_FINGERPRINT`
- direct HR069 named identity: false until a transfer transform/local validation is available
- M4: false

### UCI MQ135 low-concentration transfer fingerprints

Asset: `research_data/hr069_analytical_library/uci_mq135_lowconcentration_transfer_fingerprints_v1.json`

- DOI: `10.24432/C5CK6F`
- source archive SHA-256: `813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab`
- derived asset SHA-256: `d58c12398380dd1ebf075c98b14b2e0758e801699d5b690680816a27bbac0417`
- 6 analytes: acetone, ethanol, ethyl acetate, n-hexane, isopropanol, toluene
- concentrations: 0.05 / 0.10 / 0.20 ppm
- 5 replicates per analyte/concentration
- 18 aggregate transfer records
- HR069 overlap: MQ135 only
- temporal features: baseline, early response, exposure mean, endpoint, endpoint/baseline, delta, peak and endpoint slope
- acceptance: `ACCEPTED_TRANSFER_REFERENCE`
- direct HR069 named identity: false because one overlapping channel cannot satisfy the >=2-channel named-identity gate
- M4: false

## Offline database integration

`AnalyticalTransferBootstrap` was added as an independent idempotent seed and is invoked by `ChemicalSeedBootstrap`.

It records:

- MQDS-001 class-fingerprint asset metadata in `sensor_evidence`;
- Smoke-linked transfer evidence for legacy Smoke;
- UCI MQ135 known-concentration transfer fingerprint metadata for all six mapped analytes;
- source/derived hashes, concentration domain, replicate counts and evidence states;
- an independent meta version so an existing 0.10.2 database can receive this newly added knowledge without deleting local experimental/M4 records.

The transfer seed does not write into local `reference_artifacts`, because that table is reserved for local reference experiments/artifacts and should not silently treat third-party data as device validation.

## Qualification history

Organization harness Run #7 (`36623895675`) successfully verified generation of both derived assets from canonical/public inputs and passed Android unit regression tests against source SHA `47a507534d7571d1531f166ccd900df24b274f3c`.

After that successful generation qualification, the two derived JSON assets, their central registry entries and the offline transfer bootstrap were committed to the source branch. Therefore a final qualification must be run against the new source HEAD after this checkpoint; Run #7 is evidence for derivation correctness but is not the final qualified source SHA.

## Resume point

1. Fetch the current `release/hazrob-0.10.2` HEAD.
2. Point `HazRob/build-harness` analytical qualification at that exact SHA.
3. Verify the two committed transfer JSON assets, their hashes/counts, `AnalyticalTransferBootstrap`, and its call from `ChemicalSeedBootstrap`.
4. Run Android unit regression tests.
5. Record the final qualified SHA/run in this checkpoint or a succeeding qualification record.
6. Continue ingesting additional pure-analyte datasets; prioritize multi-channel HR069-compatible fingerprints for methanol, ethyl acetate, H2S, acetaldehyde and ethylene.
