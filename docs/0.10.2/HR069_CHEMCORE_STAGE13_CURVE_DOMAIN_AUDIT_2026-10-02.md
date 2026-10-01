# HazRob 0.10.2 / HR069 — ChemCore Stage 13 Curve-Domain Audit

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: all 144 catalogue entries; detailed domain audit for the 14 entries with registered numeric response relations.

## Objective

Make concentration, environment, heater, and source-domain limits explicit before any curve can contribute to identity or ppm. No curve may be extrapolated silently.

## Global policy

1. Every numeric relation is valid only inside the domain declared by its own source/registry.
2. A higher-precedence relation that is out of domain is unavailable for that observation; fallback to a lower-precedence relation is permitted only when the fallback itself is in domain and the fallback source is recorded.
3. `EDGE_OF_DOMAIN` is distinct from `OUT_OF_DOMAIN`.
4. MQ7 and MQ9 relations are unavailable when their required heater protocol is not valid.
5. Manufacturer target/graph ranges describe sensor/analyte evidence envelopes; they do not create a numeric equation by themselves.
6. The general HR069 warm-up chosen for operation is 300 s. It is an acquisition-stability rule and does not replace source-specific heater requirements.
7. Missing T/RH/O2/load-resistance metadata is retained as a transfer/uncertainty gap; it is not silently assumed.

## Registered curve-bearing catalogue entries

| ID | Chemical | Current numeric sources | Declared concentration domains observed in registries | Domain note |
|---:|---|---|---|---|
| 20 | Carbon Monoxide | GasEngine; Literature MQ7; Research MQ2/3/4/5/7/8/9/135 | Literature MQ7 20–2000 ppm; research spans 10–10000 depending sensor/source | MQ7 heater validity mandatory; MQ135 literature/research is cross-response, not selective CO metrology |
| 21 | Ammonia | GasEngine MQ135; Literature MQ135; Research MQ135 | Literature 10–100 ppm; research 10–200 ppm; manufacturer target envelope 10–1000 ppm | Manufacturer envelope must not be used to extend a narrower numeric curve |
| 23 | Carbon Dioxide | GasEngine MQ135; Literature MQ135; Research MQ135 | Literature 10–100 ppm; research 10–200 ppm | Cross-response only; out-of-domain inverse concentration is not reportable CO2 ppm |
| 34 | Benzene | GasEngine MQ3; Research MQ3 | Research MQ3 5.15–500 ppm equivalent output domain | Research/cross-response identity support only; no selective benzene ppm authority |
| 75 | Hydrogen | GasEngine; Literature MQ8; Research multi-MQ | Literature MQ8 200–10000 ppm; research commonly 200–10000, MQ7 50–4000; manufacturer MQ8 target envelope 100–1000 ppm | Source domains remain separate; manufacturer target range does not truncate a literature study but is preserved as operating-context evidence |
| 76 | Methane | GasEngine; Literature MQ4; Research multi-MQ | Literature MQ4 200–10000 ppm; research commonly 200–10000, MQ7 50–4000 | MQ7/MQ9 contributions additionally require heater validity |
| 78 | Propane | GasEngine MQ2; Research MQ2 | 200–10000 ppm research; manufacturer MQ2 target envelope 300–10000 ppm | Use intersection when claiming target-backed operation; broader research range remains research only |
| 84 | n-Hexane | GasEngine MQ3; Research MQ3 | 5.15–500 ppm equivalent output domain | Single-channel research relation; no exact-compound M4 authority |
| 95 | Toluene | GasEngine MQ135; Literature MQ135; Research MQ135 | Literature 10–100 ppm; research 10–200 ppm; manufacturer target envelope 10–1000 ppm | Do not extend the literature fit to 1000 ppm merely because the manufacturer lists a wider target range |
| 103 | Ethanol | GasEngine; Literature MQ135; Research multi-MQ | Literature MQ135 10–100 ppm; research: MQ3 5.15–500, MQ135 10–200, several other MQ 200–10000 | Each sensor/source retains its own domain; cross-channel common-C fitting uses only simultaneously in-domain contributors |
| 110 | Acetone | GasEngine MQ135; Literature MQ135; Research MQ135 | Literature 10–100 ppm; research 10–200 ppm | MQ135 broad-response relation; no extrapolated ppm |
| 135 | LPG | GasEngine; Research multi-MQ | Research commonly 200–10000 ppm, MQ7 50–4000 | LPG is a mixture/aggregate; numeric value is composition-dependent operational response, not a universal pure-analyte concentration |
| 139 | Smoke | GasEngine/Research limited channels | Research MQ2/MQ4 200–10000 ppm-style operational axis | Smoke is a composite operational class; a universal pure-gas ppm claim is prohibited |
| 141 | NOx | LinearLiterature MQ135 | 1–150 ppm; reference 20 °C, 65% RH, 21% O2, RL=20 kΩ | Exact source environment is structured and must be preserved; relation is NOx aggregate, not selective NO2 identity |

## Environment-domain audit

- `LinearLiteratureResponseRegistry` explicitly stores the NOx reference environment: 20 °C, 65% RH, 21% O2 and RL=20 kΩ.
- `LiteratureCurveRegistry` stores concentration domains for all relations but not a complete structured T/RH/O2/load-resistance contract for every paper.
- `ResearchCurveRegistry` stores concentration domains but not a complete structured environmental domain.
- Missing environmental metadata is therefore a documented transfer gap rather than an invitation to assume 25 °C / 50% RH or other defaults.

## Domain decision states

For every curve contribution the runtime/analysis design shall support:

- `IN_DOMAIN`
- `EDGE_OF_DOMAIN`
- `OUT_OF_DOMAIN`
- `ENVIRONMENT_DOMAIN_UNKNOWN`
- `ENVIRONMENT_OUT_OF_DOMAIN`
- `HEATER_PROTOCOL_INVALID`
- `SOURCE_DOMAIN_METADATA_INCOMPLETE`

Only `IN_DOMAIN` and, with an explicit penalty, `EDGE_OF_DOMAIN` may contribute to analytical fitting. `OUT_OF_DOMAIN`, invalid heater, or environment failure cannot create ppm.

## Catalogue accounting

- Reviewed: 144/144.
- Curve-bearing chemicals audited in detail: 14.
- Entries with no registered numeric curve: 130; state `NOT_APPLICABLE_WITH_REASON` for curve-domain audit, not “zero response”.
- Silent extrapolation allowed: 0.

## Stage result

**COMPLETE — 144/144.**

Stage 14 must now audit R0 conventions and determine whether source `Rs/R0` definitions are actually transferable to the device-specific HR069 normalization baseline.