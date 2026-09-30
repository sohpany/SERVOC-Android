#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P10 quantification/exposure/evidence-preservation contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    envelope=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreResultEnvelope.java')
    repo=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreDecisionRepository.java')
    series=txt('app/src/main/java/com/servoc/app/exposure/ChemCoreQuantitativeTimeSeries.java')
    assessment=txt('app/src/main/java/com/servoc/app/exposure/GasPpmExposureAssessment.java')
    exposure_repo=txt('app/src/main/java/com/servoc/app/exposure/ExposureRepository.java')
    normalizer=txt('app/src/main/java/com/servoc/app/calibration/AcceptedR0Normalizer.java')
    adapter=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreSessionFeatureAdapter.java')
    evidence=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreEvidenceObservation.java')
    fusion=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreEvidenceFusion.java')
    inference=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')

    for n in ['evidence_observations','screening_fit_ppm','analytical_confidence_pct','posterior']:
        req(envelope,n,'ChemCore evidence envelope')
    req(repo,'hazrob-evidence-measurement-inventory/1','evidence/measurement inventory')
    req(repo,'PRESERVE_ALL_AVAILABLE_EVIDENCE_AND_MEASUREMENTS_SEPARATE_FROM_REPORTABILITY_GATES','preservation policy')
    req(repo,'M1_M2_M3_RETAINED_FOR_IDENTIFICATION_SCREENING_AND_AUDIT_NOT_REPORTABLE_PPM','lower-tier retention')
    req(repo,'raw_telemetry_source','raw telemetry lineage')
    req(repo,'candidate_count','candidate inventory')
    req(repo,'evidence_observation_count','evidence inventory')
    req(repo,'all_exposure_reference_evidence','all original exposure references retained')
    req(repo,'all_exposure_reference_count','all exposure reference count')
    req(repo,'recovery_telemetry_sample_count','recovery sample inventory')
    req(repo,'if(series!=null&&exposure!=null)payload.put("evidence_measurement_inventory"','post-recovery inventory refresh')

    # Degraded measurements are interpreted and weighted, not discarded.
    req(normalizer,'availableRatio','support-only physical ratio path')
    req(normalizer,'evidenceWeight','measurement quality weighting')
    req(normalizer,'evidenceQualityClass','measurement-quality classification')
    req(adapter,'corroborativeRatios','session corroborative ratios')
    req(adapter,'corroborativeQualityWeights','session corroborative weights')
    req(adapter,'bindCorroborativeSession','session-bound support cache')
    req(evidence,'measurementQualityWeight','evidence quality weight')
    req(evidence,'supportRole','primary/corroborative role')
    req(evidence,'corroborativeFromResponseModel','corroborative evidence factory')
    req(evidence,'CORROBORATIVE','corroborative evidence role')
    req(fusion,'MAX_CORROBORATION_BONUS=8.0','capped weak-evidence support')
    req(fusion,'supportWeight','quality-weighted support calculation')
    req(fusion,'isFullStrengthSupport','full-vs-weak evidence separation')
    req(inference,'degraded/auxiliary MQ measurements retained as quality-weighted corroborative evidence','inference corroboration note')
    req(inference,'corroborativeRatiosFor','session corroboration auto-binding')
    req(envelope,'measurement_quality_weight','serialized evidence quality')
    req(envelope,'support_role','serialized evidence role')
    req(envelope,'preserve_and_use_as_quality_weighted_corroborative_evidence_never_auto_promote_to_reportable_ppm','methodology policy')

    # Reportable temporal ppm is M4-only, but every valid derived point and every distinct averaging duration are retained.
    req(series,'M4_HAZROB_VALIDATED','M4 quantitative gate')
    req(series,'reportableQuantitative()','reportable model gate')
    req(series,'out.put("points",pointRows)','quantitative point preservation')
    req(series,'all_valid_quantitative_points_preserved','quantitative preservation marker')
    req(series,'raw_telemetry_preserved_elsewhere','raw telemetry preservation marker')
    req(series,'ORDER BY t_ms,id','deterministic time-series replay')
    req(series,'MAX_CONTIGUOUS_GAP_MS=2_000L','gap fail-closed integration')
    req(series,'WindowSpec','multiple temporal window support')
    req(series,'r+"@"+Long.toString(Math.round(l.durationMin*60_000.0))','distinct role-duration key')

    # Current ppm comparison remains immediate and distinct from exact-duration TWA/STEL integration.
    req(assessment,'CURRENT_CONCENTRATION_ABOVE_REFERENCE','current-reference warning')
    req(assessment,'TIME_INTEGRATED_REFERENCE_CROSSED','time-integrated state')
    req(assessment,'time_integrated_reference_crossed','temporal finding')
    req(assessment,'current_concentration_crossed','current concentration finding')
    req(assessment,'IDLH_REFERENCE_CROSSED','IDLH immediate state')
    req(assessment,'CEILING_REFERENCE_CROSSED','Ceiling immediate state')
    req(assessment,'Math.abs(wd-required)<=1e-9','exact temporal duration matching')

    # Flammability is an immediate concentration comparison and reports %LEL/LFL.
    req(assessment,'LFL_REACHED_OR_EXCEEDED','LFL state')
    req(assessment,'percent_lfl','percent LFL')
    req(assessment,'lfl_margin_ppm','LFL margin')
    req(assessment,'MEASURED_PPM_AT_OR_ABOVE_LOWER_FLAMMABILITY_LIMIT','LFL interpretation')
    req(assessment,'1 vol% = 10000 ppmv','flammability conversion provenance')

    # Mass concentration conversion is permitted only with MW + session T/P; no silent standard-condition substitution.
    req(exposure_repo,'SELECT molar_mass FROM chemicals','canonical molecular mass')
    req(assessment,'condition_aware_mass_conversion_available','mass conversion availability')
    req(assessment,'IDEAL_GAS_SESSION_T_P_MW','condition-aware conversion method')
    req(assessment,'R_PA_M3_PER_MOL_K','ideal gas constant')
    req(assessment,'canMassConvert','conversion gate')
    forbid(assessment,'24.45','silent 25C molar-volume shortcut')

    req(repo,'payload.put("quantitative_time_series",seriesCopy)','same-row temporal attachment')
    req(repo,'payload.put(GasPpmExposureAssessment.KEY,exposure)','same-row exposure attachment')
    req(repo,'payload.put("evidence_measurement_inventory"','same-row evidence inventory')

    print('HazRob 0.10.2 / HR069 P10 quantification/exposure/evidence contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P10 quantification/exposure/evidence contract: FAIL: {exc}')
        raise SystemExit(1)
