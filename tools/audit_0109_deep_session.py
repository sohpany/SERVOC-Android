#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P9 deep-session analysis contract."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    feature=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreFeaturePipeline.java')
    adapter=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreSessionFeatureAdapter.java')
    stable=text('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreStableKnowledgeReader.java')
    dynamics=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreSessionDynamics.java')
    deep=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreDeepSessionResult.java')
    tab=text('app/src/main/java/com/servoc/app/ui/ChemCoreAnalysisTab.java')
    repo=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreDecisionRepository.java')
    result=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreResultEnvelope.java')
    inference=text('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')

    # Frozen Stable/open-set feature contract must remain backward compatible.
    req(feature,'SCHEMA_VERSION = "chemcore-features/1"','frozen feature schema')
    req(feature,'FEATURES_PER_SENSOR = 17','frozen per-sensor feature count')
    req(stable,'CANONICAL_FEATURE_DIMENSION=ChemCoreSessionFeatureAdapter.SENSOR_ORDER.length*ChemCoreFeaturePipeline.FEATURES_PER_SENSOR','Stable dimension binding')
    req(stable,'mean.length!=CANONICAL_FEATURE_DIMENSION','open-set exact dimension gate')

    # One exact persisted primary session with deterministic ordering.
    req(adapter,'WHERE session_id=? ORDER BY t_ms,id','deterministic exact-session extraction')
    req(adapter,'featurePayloadSha256','feature provenance hash')
    req(adapter,'acceptedR0Samples','R0 provenance coverage')

    # New dynamics are secondary and do not mutate the frozen vector.
    req(dynamics,'SCHEMA="chemcore-session-dynamics/1"','session dynamics schema')
    for n in ['recovery_slope_log10_per_s','recovery_t50_s','recovery_t90_s','recovery_fraction_to_baseline','primary_pairwise_log10_correlation','recovery_minus_primary']:
        req(dynamics,n,'deep-session dynamics')
    req(dynamics,'dynamics_audit_only_not_an_automatic_confidence_contributor','confidence separation')
    req(dynamics,'dynamics_do_not_unlock_reportable_ppm','quantitation separation')
    req(dynamics,'WHERE session_id=? ORDER BY t_ms,id','dynamics deterministic replay')

    # Recovery is persisted as its own telemetry capture and only then finalizes the deep session.
    req(tab,'MeasurementSessionRegistry.requestChemCoreCapture(true)','ChemCore capture request')
    req(tab,'chemcore_recovery_capture_complete','recovery capture stop')
    req(tab,'ChemCoreSessionDynamics.extract(host.database(),attachedSessionId,recoverySessionId)','primary/recovery dynamics binding')
    req(tab,'new ChemCoreDeepSessionResult(lastResult,dynamics,totalComputeMs)','immutable deep-session result')
    req(tab,'repo.attachDeepSession(attachedSessionId,finalDeepResult)','single decision-row deep-session attachment')
    req(tab,'recovery_stable_and_persisted','recovery completion gate')

    req(deep,'recovery_changes_identity_confidence",false','deep identity-confidence separation')
    req(deep,'recovery_unlocks_reportable_ppm",false','deep ppm separation')
    req(repo,'attachDeepSession','deep-session persistence method')
    req(repo,'payload.put("session_dynamics"','session-dynamics report projection')

    # Existing analytical gates remain in the same primary-session inference path.
    req(tab,'ChemCoreStableKnowledgeReader.candidates','Stable candidates')
    req(tab,'openSetArtifact','validated open-set artifact')
    req(tab,'ChemCoreInferenceEngine.analyze','canonical inference')
    req(tab,'POSSIBLE_MIXTURE_OR_INTERFERENCE','mixture/interference result preserved')
    req(result,'ppm_reportable','reportable ppm gate preserved')
    req(result,'ANALYTICAL_CONFIDENCE_SEMANTICS','identity confidence semantics preserved')
    forbid(deep,'analyticalConfidencePct=','deep result must not recompute confidence')

    print('HazRob 0.10.2 / HR069 P9 deep session contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P9 deep session contract: FAIL: {exc}')
        raise SystemExit(1)
