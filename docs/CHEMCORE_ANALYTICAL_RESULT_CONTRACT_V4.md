# ChemCore Analytical Result Contract v4

**Contract:** `chemcore-result/4`  
**Program:** HazRob 0.10.1 Measurement / Analysis / Reporting  
**Status:** normative specification  
**Baseline:** HR066  
**Feature contract:** `chemcore-features/1`

## 1. Purpose
This contract is the single session-bound analytical truth consumed by Analysis UI, ReportsTab, PDF and XLSX. Presentation layers may format or sort documented views, but must not recompute chemical identity, quantitative concentration, evidence tier or safety decisions.

## 2. Scientific invariants
- A chemical candidate is not a confirmed identity.
- Candidate `posterior` is a relative model-ranking weight unless a separately validated probability-calibration artifact explicitly says otherwise.
- `screening_fit_ppm` is a model-fit hypothesis and is never a reportable measurement by itself.
- `ppm_reportable=true` requires outcome `QUANTIFIED`, evidence tier `M4_HAZROB_VALIDATED`, accepted identity/open-set gate, complete device/reference-gas quantitative support for all contributing curves and validated-domain acceptance.
- UNKNOWN, mixture/interference, OOD and insufficient evidence are first-class valid outcomes.
- Occupational quantitative interpretation is forbidden when PPM is withheld.
- TWA/STEL/Ceiling/IDLH/LFL semantics remain distinct and retain source/unit/duration provenance.
- The 144 panel is an operational screening scope, not a claim that all 144 substances are selectively identifiable or quantitatively measurable by the installed MQ array.

## 3. Required top-level fields
- `schema`: exactly `chemcore-result/4`.
- `session_id`: immutable measurement-session identity.
- `outcome`: ChemCoreDecision.Outcome.
- `evidence_tier`: M0..M4.
- `measurement_quality`: session-bound quality object; may contain a compatibility copy of current `lab_quality`.
- `quality_gates`: explicit gate states/reason codes.
- `methodology`: analysis/feature/sampling/model method identifiers.
- `library`: exact version, SHA-256 and source commit.
- `candidates`: evidence-bearing ranked analytical hypotheses.
- `screening_144`: operational panel object.
- `accepted_findings`: only findings whose identity state permits the declared finding type.
- `safety_findings`: safety interpretations derived only from accepted quantitative inputs.
- `source_refs`: traceable scientific/reference source identities.
- `feature_summary`: canonical feature schema and persisted/session feature metadata.

Existing scalar fields such as `chemical_uid`, `chemical`, `ppm`, `prediction_low`, `prediction_high`, `ood_score`, `top_posterior`, `normalized_entropy`, `sampling_profile_id`, `feature_schema`, `model_version`, `note` remain available during v3→v4 migration.

## 4. screening_144
The object MUST contain:
- `panel_id`: stable panel identity.
- `panel_version`: version of exact 144 membership.
- `target_count`: exactly 144 for a valid operational panel.
- `evaluated_count`: number for which an explicit status row was produced.
- `rows`: exactly 144 rows once Stage 4 is activated.

Each row will contain:
- chemical UID and legacy/panel identity where available;
- Arabic/English label and CAS where available;
- `status`;
- `reason_code`;
- evidence tier;
- analytical ranking fields where evidence exists;
- quantitative state (`REPORTABLE`, `WITHHELD`, or `NOT_APPLICABLE`);
- source references.

Normative statuses:
`SUPPORTED`, `WEAK_MATCH`, `REJECTED`, `OUT_OF_DOMAIN`, `INSUFFICIENT_EVIDENCE`, `UNSUPPORTED_BY_CURRENT_ARRAY`.

Until Stage 4 is implemented, v4 may serialize `screening_144.state="PENDING_ENGINE"` with empty rows. UI/report code must not claim 144/144 evaluation in that state.

## 5. Candidate semantics
Every candidate preserves rank, chemical UID, label, relative posterior/ranking weight, screening-fit concentration, residual RMS, contributors, evidence tier, coverage, agreement diagnostic, evidence quality, calibrated contributor count, validated-domain state and per-sensor residuals.

`agreement_pct` is diagnostic consistency, not identity probability. It remains null when fewer than two contributors make inter-sensor agreement undefined.

## 6. Quantitative and withheld semantics
Non-finite scientific scalars serialize as JSON null.

If `ppm_reportable=false`, a `ppm_withheld_reason` is mandatory. Canonical reason codes include:
- `open_set_unknown`
- `mixture_or_interference`
- `outside_validated_domain`
- `m4_device_reference_calibration_required`
- `identity_or_quantification_gate_not_passed`
- `measurement_quality_gate_failed`
- `not_reportable`

No presentation layer may substitute `screening_fit_ppm` for a null/withheld final `ppm`.

## 7. Source traceability
`source_refs` entries identify the exact source record used by the decision or safety interpretation. A source reference should carry, where applicable: `source_id`, authority, dataset/library version, record identity/URL key, retrieval/pinning provenance and content/hash provenance. Source references are data provenance; presentation can render them as user-navigable citations where supported.

## 8. Safety findings
Safety findings are separate from identity ranking. Hazard sorting may use the existing dimensionless substance-specific hazard ratio or concentration display mode, but must never alter analytical identity order.

A safety finding must retain:
- accepted chemical UID/identity;
- reportable PPM;
- reference authority/type/value/unit;
- duration/temporal role;
- percent of reference;
- LFL/UFL where applicable;
- duration-coverage state;
- explicit `compliance_determination=NOT_MADE` unless a future separately validated compliance workflow is implemented.

## 9. Methodology and feature summary
`methodology` records the algorithms/contracts that produced the result, including joint inverse curve fit, open-set gate, mixture residual guard and evidence gate versions when used.

`feature_summary.schema` must identify the canonical ChemCore feature contract. Stage 2 will consolidate all scientific feature consumers onto `chemcore-features/1` or its explicitly versioned successor.

## 10. Backward compatibility
- Existing v3 decisions remain readable and reportable as historical records.
- v4 writers retain existing v3 scalar/candidate fields during migration.
- Missing v4 sections in a historical v3 payload mean `NOT_RECORDED`, never an inferred PASS.
- Historical provenance must not be rewritten using a newer library/model.
- Database schema changes are not required merely to introduce the JSON contract; indexed scalar columns may remain as compatibility/query fields.
- Reports must prefer the exact persisted session decision over recomputation with current library data.

## 11. Acceptance tests
Stage 1 passes only when tests prove:
1. schema is v4;
2. v4 sections serialize deterministically;
3. screening panel cannot claim 144 evaluated while engine is pending;
4. non-M4 PPM remains withheld;
5. UNKNOWN/mixture/OOD reason codes remain fail-closed;
6. candidate screening-fit PPM cannot become final reportable PPM;
7. library and session provenance survive serialization;
8. v3 historical semantics are explicitly documented and compatibility fields remain available.
