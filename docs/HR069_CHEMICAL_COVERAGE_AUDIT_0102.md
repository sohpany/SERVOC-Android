# HazRob 0.10.2 / HR069 — Chemical-analysis coverage audit

Date: 2026-09-29  
Scope: legacy 144-entry analytical catalogue + pinned 652-record NIOSH identity seed + verified external MQ/MOS response evidence.

## Executive result

The external literature/data search improves analytical coverage, but it does **not** justify declaring the 144/652 chemicals quantitatively calibrated. The correct engineering gain is to separate laboratory work into:

1. **M4 validation/transfer**, where credible curves or concentration-labelled external data already exist;
2. **fingerprint transfer/repeatability**, where external fingerprints exist but the response is setup-dependent;
3. **true discovery experiments**, where the present MQ array has no verified analyte-specific external response record;
4. **unsupported/compatibility-review chemicals**, which should not be casually exposed to the HR069 MQ chamber and may require specialist sensors.

The 652-record NIOSH dataset is an authoritative identity/exposure reference. It is **not** a 652-gas sensor-response database. Sensor evidence is linked only where an analyte has an explicit source/provenance record.

## Locked evidence hierarchy

Highest to lowest:

- **M4 HazRob device-specific validation** — actual HR069 sensor instance, known reference concentration, accepted protocol, valid environmental/heater operating envelope. Only this tier can make a generic external prior authoritative for HR069 quantitative use.
- **Manufacturer target/tabulated evidence** — authoritative sensor capability/range evidence, still not an individual-sensor calibration certificate.
- **Peer-reviewed graph fit / independent known-concentration experiment** — useful quantitative prior or transfer evidence; never M4 by itself.
- **Pinned community/datasheet fit** — MQSensorsLib/MQDataScience-type graph regression; research prior.
- **Fingerprint/method evidence** — useful for identity, feature selection, mixture/drift methodology, and experiment design.

No external record in this release is allowed to set `m4=true` or to make `ValidatedGasEngine.Result.quantitative=true` without the existing device-calibration gate.

## Preferred published MQ135 Rs/R0 curves

A 2023 peer-reviewed paper digitized ten points for each of six MQ135 sensitivity curves and fitted the power model:

`Rs/R0 = a * ppm^b`

HazRob now prefers these fits over duplicate lower-tier MQ135 community coefficients **only within the published 10–100 ppm digitization domain**. No extrapolation is permitted. If the literature fit is rejected as out-of-domain, the engine may fall back to a lower-tier registered research curve if that source has its own valid domain.

| Legacy ID | Analyte | a | b | R² | Source |
|---:|---|---:|---:|---:|---|
| 110 | Acetone | 2.7295 | -0.2822 | 0.9996 | Umbu 2023, Eq. 16 |
| 95 | Toluene | 2.9630 | -0.2838 | 0.9998 | Umbu 2023, Eq. 17 |
| 103 | Alcohol/Ethanol mapping | 3.8659 | -0.3091 | 0.9996 | Umbu 2023, Eq. 18 |
| 23 | Carbon dioxide | 4.9999 | -0.3387 | 0.9998 | Umbu 2023, Eq. 19 |
| 21 | Ammonia | 6.4835 | -0.4060 | 0.9996 | Umbu 2023, Eq. 20 |
| 20 | Carbon monoxide | 5.2629 | -0.2668 | 0.9992 | Umbu 2023, Eq. 21 |

Citation: Ari Bangkit Sanjaya Umbu (2023), *Jurnal Teori dan Aplikasi Fisika*, 11(2), 49–60. DOI `10.23960/jtaf.v11i2.314`.

These are **datasheet-graph regressions**, not independent reference-gas M4 calibrations. The source itself concludes that standard-gas/instrument calibration remains necessary for accurate measurement.

## External concentration-labelled and fingerprint coverage added

### UCI low-concentration array (`UCI_GSALC_2023`)

- Public CC BY 4.0 dataset; DOI `10.24432/C5CK6F`.
- Contains MQ135 plus nine other MOS channels.
- Pure ethanol, acetone, toluene, ethyl acetate, isopropanol and n-hexane.
- 50, 100 and 200 ppb; five replicates at each level; 1 Hz; 5 min baseline + 10 min injection data.
- The source explicitly states these levels are below the sensors' nominal minimum detection limit. Consequently HazRob registers this as known-concentration **response/fingerprint evidence**, not as a transferable PPM curve.

This adds particularly useful prior evidence for legacy IDs **105 Isopropanol** and **115 Ethyl Acetate**, for which HR069 previously had no direct analyte-specific curve.

### Frontiers VOC array 2026 (`FRONTIERS_VOC_2026`)

- DOI `10.3389/fbioe.2026.1821312`.
- Array includes MQ135 and MQ3 among 16 channels.
- Pure acetone, ethanol, isopropanol and equimolar mixtures at 1, 3, 5, 7, 9, 11, 13 and 15 ppm.
- Controlled approximately 25 ± 3 °C and 50 ± 10% RH.

This strengthens the transfer evidence for IDs **103, 105 and 110**, including mixture concentration prediction, but the different array/electronics prevent treating its model as HR069 calibration.

### Exact/superset MQ-array fingerprint study (`VISUAL_SMELL_2026`)

- DOI `10.1016/j.foohum.2026.101079`.
- Uses MQ2, MQ3, MQ4, MQ5, MQ6, MQ7, MQ8, MQ9 and MQ135: all eight HR069 MQ models plus MQ6.
- Repeated pure acetone and ethanol measurements.
- Demonstrates end-signal magnitude, response slope and inter-sensor response ratios as useful fingerprint features.
- The paper also shows that slope/rate is apparatus- and diffusion-dependent. Therefore these fingerprints are valuable for feature design and transfer testing, not direct numeric transplantation.

### MQ3 methanol/ethanol mixture study (`MQ3_MEOH_ETOH_2026`)

- DOI `10.1016/j.snb.2026.139758`.
- Single commercial MQ3, methanol/ethanol mixtures, multiple humidity conditions, dynamic temperature modulation and concentration prediction.
- Gives useful prior evidence for **102 Methanol**, reducing the need for completely blind discovery.
- HR069 cannot reproduce this protocol today because its eight MQ heaters share the fixed switched 5 V rail. Methanol therefore remains a local transfer/fingerprint experiment, not a ready PPM curve.

### MQ135 ethanol environmental study (`MQ135_ENV_ETHANOL_2026`)

- DOI `10.3390/s26113525`.
- 0–500 ppm ethanol over 15/25/35 °C and 30/60/90% RH; reported log-linear response with R² ≥ 0.999.
- The model is voltage/platform specific, so it is registered to improve environmental-compensation planning rather than replacing Rs/R0 calibration.

### Winsen MQ5 butane statement

Current manufacturer material states high sensitivity to butane, propane and methane, while the explicit target/range specification is LPG/CH4 and 300–10000 ppm for CH4/C3H8. HR069 therefore registers **79 n-Butane** as a manufacturer qualitative target-family prior only; no butane-specific PPM equation is fabricated.

## Current external crosswalk for the 144 catalogue

The machine-readable registry currently has verified analyte-specific external evidence for these legacy entries:

- 20 Carbon Monoxide — peer-reviewed MQ135 Rs/R0 prior.
- 21 Ammonia — peer-reviewed MQ135 Rs/R0 prior.
- 23 Carbon Dioxide — peer-reviewed MQ135 cross-response prior; not a selective CO2 meter.
- 79 n-Butane — manufacturer qualitative sensitivity; compound-specific curve still required.
- 84 n-Hexane — UCI concentration-labelled MQ135 evidence plus an existing MQ3 community curve.
- 95 Toluene — peer-reviewed curve + UCI concentration-labelled evidence.
- 102 Methanol — MQ3 dynamic mixture/fingerprint evidence; HR069 transfer required.
- 103 Ethanol — multiple independent sources: published curve, UCI data, known-concentration VOC array, exact MQ-array fingerprint study, dynamic MQ3 study and environmental MQ135 study.
- 105 Isopropanol — UCI + peer-reviewed known-concentration array evidence.
- 110 Acetone — published curve + UCI + known-concentration array + exact MQ-array fingerprint evidence.
- 115 Ethyl Acetate — UCI concentration-labelled MQ135 evidence.

This is an **evidence crosswalk**, not an assertion that only these eleven chemicals can be sensed. Manufacturer target curves and inherited MQ library curves already cover additional core analytes such as H2, methane, propane, LPG and smoke. The crosswalk specifically records newly verified external evidence that changes experimental planning.

## Duplicate-source policy

For one sensor/analyte pair, multiple regressions are not independent sensors. HR069 therefore does **not** average `a` and `b` coefficients. It now applies this rule:

1. device-specific accepted calibration wins;
2. inside its domain, a peer-reviewed published curve wins over duplicate generic/community coefficients;
3. otherwise fall back to the next valid evidence tier;
4. if multiple independent physical channels contribute, combine their **predicted log10(ppm)** values with the existing evidence-weighted median;
5. never count the same physical sensor twice merely because two papers fitted its datasheet graph.

This prevents artificial confidence inflation.

## Laboratory programme after this audit

### Group A — validate/transfer, do not rediscover from zero

Highest priority for controlled HR069 validation:

- Core existing quantitative candidates: CO, NH3, H2, CH4, propane, LPG, ethanol, toluene, smoke where applicable.
- Externally strengthened analytes: CO2 cross-response, n-hexane, acetone.
- New external evidence requiring HR069 transfer: **methanol, isopropanol, ethyl acetate**.

The objective is repeatability, HR069-specific response shape, environmental sensitivity and M4 calibration where scientifically justified—not re-deriving already published generic behavior blindly.

### Group B — true discovery experiments

Array-compatible VOCs/reducing gases for which `ChemicalCoverage` reports an empirical fingerprint candidate but neither a direct registered curve nor verified analyte-specific external evidence exists. `ChemicalCoverage.labDiscoveryCandidateCount()` is the code-level metric for this set and is CI-auditable.

### Group C — do not expose casually with current MQ chamber

Strong oxidizers/corrosives/reactive gases and current-array unsupported species retain `compatibility-review` or `unsupported-current-array`. External literature does not override material compatibility, sensor poisoning/corrosion risk or lack of a suitable sensor. Examples include halogens/acid gases, several highly reactive species, NOx without a defensible current-array quantitative channel, oxygen, many refrigerants and inert gases.

## 652-record NIOSH list

The pinned NIOSH seed currently contains **652 records**, not exactly 600. It is retained as the broad chemical identity/hazard/exposure layer. External sensor evidence is cross-linked by stable legacy ID/CAS where possible. The CI audit verifies:

- legacy catalogue count = 144;
- NIOSH seed count = 652;
- evidence crosswalk legacy IDs/names are valid;
- CAS cross-links can be resolved against the NIOSH seed;
- the six preferred published MQ135 curves remain pinned to the expected compounds/domain;
- no external source is marked M4.

Audit command:

```bash
python3 tools/audit_hr069_chemical_coverage.py --check
```

Machine-readable source of truth:

`research_data/chemical_coverage_hr069/evidence_registry.json`

## Hardware limitation that remains critical

HR069 currently reports `CYCLIC_MQ_HEATER_DRIVER_INSTALLED=false`. Therefore MQ7/MQ9 evidence that depends on the manufacturer's heater cycle cannot become valid quantitative evidence merely by importing an external curve or dataset. This is a physical qualification gate and remains unchanged by this audit.

## Implementation changes in 0.10.2

- `LiteratureCurveRegistry` stores the preferred peer-reviewed MQ135 curve priors and declared domains.
- `ValidatedGasEngine` evaluates accepted device-specific calibration first, then peer-reviewed curve priors, then inherited generic curves, then lower-weight MQDataScience research fits.
- Same-sensor duplicate curves are suppressed.
- Consensus remains a robust weighted median in log10(ppm), not coefficient averaging.
- `ExternalEvidenceCoverage` separates published curve / known-concentration / fingerprint / qualitative evidence.
- `ChemicalCoverage` now distinguishes transfer-validation candidates from true discovery candidates.
- External evidence cannot flip the M4/quantitative gate.
- Dedicated CI audit protects the 144/652 crosswalk and M4 isolation contract.
