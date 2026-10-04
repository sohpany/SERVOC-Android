#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P6 evidence-fusion and Analytical Confidence contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    matrix=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreConfidenceMatrix.java')
    fusion=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreEvidenceFusion.java')
    envelope=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreResultEnvelope.java')
    infer=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')
    fp=txt('app/src/main/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngine.java')
    legacy_test=txt('app/src/test/java/com/servoc/app/chemcore/ChemCoreConfidenceMatrixEvidenceTest.java')
    runtime_test=txt('app/src/test/java/com/servoc/app/chemcore/ChemCoreEvidenceFusionTest.java')

    # Runtime API remains typed and de-duplicated. Full-strength counts are independent evidence groups,
    # never raw MQ channels. Weak/degraded support has its own capped corroboration axis.
    req(matrix,'score(long chemicalUid,List<ChemCoreEvidenceObservation> evidence)','typed confidence API')
    req(matrix,'ChemCoreEvidenceFusion.fuseForChemical(chemicalUid,evidence).score','typed API delegates to runtime fusion')
    req(matrix,'scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth,double corroborationPct)','independent group + corroboration scoring schedule')
    req(matrix,'Counts represent independent full-strength evidence groups, not MQ sensor channels','independent count semantics')
    req(matrix,'corroborationPct','separate weak-support score field')
    req(matrix,'clamp(corroborationPct,0,8.0)','corroboration cap')

    # Positive-only fusion preserves every chemically interpretable observation but performs two
    # de-duplication layers: immediate evidence group first, then upstream scientific lineage.
    # Correlated derivatives are retained only as capped corroboration and never become extra proof.
    for n in [
        'FULL_SUPPORT_STANDARDIZED_RESIDUAL=2.50',
        'MAX_CORROBORATION_BONUS=8.0',
        'MAX_CORRELATED_DERIVED_BONUS=3.0',
        'filterChemical&&e.chemicalUid!=chemicalUid',
        'Map<String,ChemCoreEvidenceObservation> immediateRepresentative',
        'immediateRepresentative.get(key)',
        'immediateRepresentative.put(key,e)',
        'Map<String,ChemCoreEvidenceObservation> lineageRepresentative',
        'ChemCoreEvidenceLineagePolicy.confidenceLineageKey(e)',
        'lineageRepresentative.get(key)',
        'lineageRepresentative.put(key,e)',
        'Map<String,ChemCoreEvidenceObservation> weakRepresentative',
        'weakRepresentative.get(key)',
        'supportWeight(e)',
        'scoreIndependent(q,r,m4,breadth,corroboration)'
    ]:
        req(fusion,n,'runtime evidence fusion')
    req(fusion,'if(e==null||!e.insideDomain||!"PRIMARY".equals(e.supportRole)||e.measurementQualityWeight<0.75)return false','full-strength domain/quality gate')
    req(fusion,'if(!e.insideDomain)w*=0.40','out-of-domain retained only as weak support')
    req(fusion,'if("CORROBORATIVE".equals(e.supportRole))w=Math.min(w,0.70)','corroborative quality cap')
    req(fusion,'fullImmediateGroups.containsKey(e.independenceGroup)','weak copy cannot double-count immediate strong proof')
    req(fusion,'lineageRepresentative.containsKey(key)','weak copy cannot double-count strong scientific lineage')
    req(fusion,'case MODEL_DERIVED_REFERENCE_FINGERPRINT:','derived fingerprint lineage classification')
    req(fusion,'case QSPR_MODEL_TRANSFER:','QSPR transfer lineage classification')
    req(fusion,'if(hasDirectReference)correlatedDerived.put(entry.getKey(),e);else r++;','derived reference evidence is corroborative when direct reference exists')
    req(fusion,'derivedCorroboration=Math.min(MAX_CORRELATED_DERIVED_BONUS,derivedCorroboration)','correlated-derived corroboration cap')
    req(fusion,'int independentGroups=q+r+m4+weakGroups','reported independent confidence group count excludes correlated derivatives')
    req(fusion,'posterior/model ranking remains separate from the Analytical Confidence Index','posterior separation')

    # Result contract exposes confidence separately from posterior and retains the complete evidence trail.
    for n in ['ANALYTICAL_CONFIDENCE_SEMANTICS','analytical_confidence_pct','confidence_evidence_groups','confidence_sensor_breadth','evidence_observations','measurement_quality_weight','support_role']:
        req(envelope,n,'result confidence contract')
    req(envelope,'relative_model_ranking_weight_not_calibrated_identity_probability','posterior semantics preserved')
    req(envelope,'operational_positive_evidence_index_not_calibrated_probability','confidence semantics explicit')

    # Inverse-model confidence is computed after session corroborative measurements are merged;
    # fingerprint identity uses the same fusion engine. Corroboration never changes primary ranking/PPM.
    req(infer,'ChemCoreSessionFeatureAdapter.corroborativeRatiosFor(session)','automatic session corroborative ratios')
    req(infer,'ChemCoreSessionFeatureAdapter.corroborativeWeightsFor(session)','automatic session quality weights')
    req(infer,'List<ChemCoreEvidenceObservation> allEvidence=withCorroboration','candidate evidence merge before confidence')
    req(infer,'ChemCoreEvidenceFusion.fuseForChemical(r.chemicalUid,allEvidence)','solver + corroborative confidence fusion')
    req(infer,'corroborativeFromResponseModel','degraded measurements become support-only observations')
    req(infer,'double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN','corroboration cannot promote PPM')
    forbid(infer,'note+="; reliability="','legacy 1..5 reliability must not be primary result output')
    req(fp,'ChemCoreEvidenceFusion.fuseForChemical(uid,observations)','fingerprint confidence fusion')
    req(fp,'observations.add(fingerprintEvidence)','fingerprint evidence merged before scoring')

    # Regression suite protects immediate-group independence, upstream-lineage collapse,
    # weak-support retention, correlated-derivative capping and non-promotion to quantitative claims.
    for n in [
        'correlatedQsprChannelsCountAsOneIndependentEvidenceGroup',
        'strongestTierWinsInsideOneIndependenceGroup',
        'distinctReferenceSourcesRemainIndependent',
        'differentImmediateGroupsFromSameDirectSourceAreNotIndependent',
        'outOfDomainSameChemicalIsWeakCorroborationButOtherChemicalNeverBoostsCandidate',
        'm3IdentityEvidenceIsNonM4SupportedEvidenceUntilWeightCalibration'
    ]:
        req(legacy_test,n,'P6 matrix tests')
    for n in [
        'threeQsprSensorChannelsFromOneModelAreOneEvidenceGroup',
        'nominallyDifferentQsprGroupsFromSameUpstreamLibraryCollapseToOneLineage',
        'outOfDomainAndPoorFitAreRetainedAsSmallCorroborativeSupport',
        'degradedMeasurementCanSupportButNeverBecomesReportableQuantitative',
        'degradedCopyOfSameIndependenceGroupDoesNotDoubleCountStrongProof',
        'modelDerivedReferenceFingerprintIsCorroborationWhenDirectReferenceExists',
        'qsprAddsOnlyCorroborationWhenDirectReferenceExists',
        'resultEnvelopeKeepsPosteriorAndAnalyticalConfidenceSemanticallySeparate',
        'acceptedFingerprintProducesStructuredConfidenceButNeverReportablePpm'
    ]:
        req(runtime_test,n,'runtime P6 tests')

    print('HazRob 0.10.2 / HR069 P6 confidence fusion contract: PASS')
    print('Rule: confidence is de-duplicated by immediate independenceGroup and upstream scientific lineage; degraded/correlated evidence may add only capped corroboration; posterior and PPM remain separate.')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P6 confidence fusion contract: FAIL: {exc}')
        raise SystemExit(1)
