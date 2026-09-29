# HR069 48 h MQ conditioning + provisional PPM references — checkpoint

Date: 2026-09-29  
Release: 0.10.2  
Hardware revision: HR069  
Qualified source: `cec79ae4e55f0120d84d83e39a0e0ddbf31f3ac9`

## Project decision

The installed eight-sensor MQ array completed 48 hours of initial preheating. This is recorded as the completed initial conditioning campaign `HR069-MQ-48H-PREHEAT-2026-09` and satisfies the registered minimum initial preheat requirement for the installed MQ models.

Available source-backed PPM libraries are accepted now for candidate fitting, ranking and provisional reference-concentration hypotheses. These references may be transfer-recalibrated or replaced by stronger HR069 measurements later.

This acceptance does **not** set M4. Occupational/safety-grade quantitative interpretation remains gated by device/reference-gas validation and the existing ChemCore M4 contract.

## Heater-protocol distinction

The 48 h initial conditioning makes all eight installed MQ channels ready for raw/empirical acquisition. It does not reproduce the separate manufacturer high/low heater cycle required by the registered MQ-7B/MQ-9B reference curves. Therefore:

- MQ2, MQ3, MQ4, MQ5, MQ8 and MQ135 may use the accepted source-backed response references inside their documented domains.
- MQ7 and MQ9 raw signals remain available for HR069 empirical fingerprint acquisition.
- MQ7/MQ9 manufacturer/datasheet PPM response curves remain fail-closed until cyclic-heater hardware/protocol validity is available.

## Provisional PPM runtime

`AcceptedReferenceResponseModel` implements the accepted PPM-reference policy with this source priority for the same physical sensor/analyte and concentration:

1. peer-reviewed `LiteratureCurveRegistry` relation inside its published range;
2. `GasEngine` relation only when bounded by current manufacturer evidence;
3. pinned `ResearchCurveRegistry` datasheet/community fit inside its declared range.

No source is extrapolated beyond its registered concentration domain. The model is M2/reference evidence, not M4.

`ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates()` now exposes these accepted reference models to the normal ChemCore joint log-space solver. The resulting candidate `screening_fit_ppm` is therefore usable now as a provisional reference hypothesis while the top-level safety/reportable PPM gate remains unchanged.

## Fingerprint coverage after this checkpoint

### Pure analyte numeric families — 11

- Carbon Monoxide
- Ammonia
- Carbon Dioxide
- Benzene
- Hydrogen
- Methane
- Propane
- n-Hexane
- Toluene
- Ethanol
- Acetone

### Operational/composite numeric families — 2

- LPG — composite fuel-gas class; mixture composition affects response
- Smoke — operational smoke class/index; not a pure-analyte concentration

Total source-backed numeric fingerprint families: **13**.

The previously preserved Food Freshness library remains **59** measured headspace/session fingerprints over the eight HR069-overlap MQ channels. Those records remain headspace/class fingerprints and are not relabelled as pure chemical identities.

## Still transfer-only / pending exact HR069 numeric vectors

- Methanol — external MQ3 transfer evidence available; current published dynamic-heater protocol is not the present HR069 fixed-heater protocol.
- Ethyl acetate — external known-concentration MQ135-array evidence available; exact HR069 vector pending.
- Isopropanol — external known-concentration MQ135-array evidence available; exact multi-channel HR069 vector pending.
- H2S — manufacturer sulfide affinity evidence only; exact numerical H2S vector pending.
- Acetaldehyde — source-backed HR069-compatible numeric vector pending.
- Ethylene — source-backed HR069-compatible numeric vector pending.

No missing channels are fabricated to convert a single-channel or different-array source into an eight-channel HR069 fingerprint.

## Qualification

Organization-controlled workflow: `HazRob HR069 Analytical Integration Qualification`  
Run: `36621523345`  
Result: **PASS**

The run checked the exact qualified source SHA and passed:

- release/HR069 identity;
- chemical coverage audit;
- 43 registered research curves;
- 76 research model catalogue;
- 48 h conditioning state;
- 11 pure + 2 operational numeric fingerprint families;
- 59 Food Freshness headspace fingerprints;
- provisional PPM acceptance while external M4 remains false;
- Android compilation;
- complete unit regression suite;
- qualification evidence packaging/upload.

## Next enrichment work

1. Derive source-preserving transfer features from the UCI low-concentration MQ135 time series for ethanol, acetone, toluene, n-hexane, isopropanol and ethyl acetate without pretending its raw signal is HR069 Rs/R0.
2. Preserve low-concentration anchors (50/100/200 ppb) as transfer/research evidence only because the source itself states they are below nominal sensor detection limits.
3. Derive source-normalized Smoke/Perfume/Mixture class fingerprints from the canonical MultimodalGasData dataset, preserving the six-channel HR069 overlap and leaving MQ4/MQ9 missing rather than synthesizing them.
4. Promote transfer fingerprints into direct HR069 identity only after a defensible channel-space transformation or local HR069 transfer measurement exists.
5. Continue replacing provisional references with HR069 M4 curves as controlled reference-gas experiments are completed.
