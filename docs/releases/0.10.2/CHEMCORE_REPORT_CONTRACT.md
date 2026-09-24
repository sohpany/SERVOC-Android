# ChemCore → Reports Contract — 0.10.2

Reviewed against HR067 commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

## Session binding
Reports retrieve the latest ChemCore payload for the exact measurement `session_id`. The report path must not substitute a decision from another session.

## Preserved decision gates
A ChemCore result is not automatically a reportable concentration. The report/exposure path preserves:
- `ppm_reportable`;
- `quantitative_occupational_interpretation_permitted`;
- prediction interval;
- posterior and separation information;
- normalized entropy;
- OOD score;
- sampling profile and feature schema;
- model version;
- analytical-library version/SHA-256/source provenance.

## Exposure safety contract
`GasPpmExposureAssessment` is downstream of ChemCore and accepts occupational/flammability interpretation only after the quantitative gate passes.

Toxicology policy:
- only explicitly ppm-based toxicological limits are compared with measured ppm;
- mass-concentration limits are not silently converted into ppm.

Flammability policy:
- LFL/LEL and UFL/UEL are treated separately as volume-percent boundaries;
- conversion uses 1 vol% = 10,000 ppmv;
- a single capture does not create a legal compliance determination.

## PDF/XLSX parity
Both export paths preserve analytical provenance rather than exporting only a candidate name/ppm value.

The report stack includes:
- HR067 advanced MQ fingerprint result;
- ChemCore session-bound provenance;
- candidate posterior ranking;
- PPM reportability decision;
- library/model provenance;
- gas exposure and flammability context;
- recorded time-series and environmental variables;
- MQ ratios/raw values and R0 metadata.

## Quantitative chart rule
PPM-vs-time charts are generated only from candidate analyses that remain `quantitative=true` for the corresponding recorded samples. Fingerprint-only candidates must not be drawn as quantitative concentration curves.

## 0.10.2 action
KEEP this contract intact. Versioning or UI changes must not weaken session binding, scientific gates, provenance, or report semantics.
