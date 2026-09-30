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

    # Existing P6 typed API is retained, but the runtime implementation is centralized in the fusion engine.
    req(matrix,'score(long chemicalUid,List<ChemCoreEvidenceObservation> evidence)','typed confidence API')
    req(matrix,'ChemCoreEvidenceFusion.fuseForChemical(chemicalUid,evidence).score','typed API delegates to runtime fusion')
    req(matrix,'scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth)','independent group scoring schedule')
    req(matrix,'Counts represent independent evidence groups, not MQ sensor channels','independent count semantics')

    # Positive-only fusion: candidate-isolated, in-domain, reasonable fit, de-duplicated lineage.
    for n in ['MAX_SUPPORT_STANDARDIZED_RESIDUAL=2.50','filterChemical&&e.chemicalUid!=chemicalUid','representative.get(e.independenceGroup)','representative.put(e.independenceGroup,e)','!e.insideDomain','Math.abs(e.standardizedResidual)>MAX_SUPPORT_STANDARDIZED_RESIDUAL','scoreIndependent(q,r,m4,breadth)']:
        req(fusion,n,'runtime evidence fusion')
    req(fusion,'REFERENCE_FINGERPRINT&&hasDirectReference','derived reference fingerprint double-count guard')
    req(fusion,'posterior/model ranking is intentionally not used here','posterior separation')

    # Result contract exposes confidence separately from posterior and retains the evidence trail.
    for n in ['ANALYTICAL_CONFIDENCE_SEMANTICS','analytical_confidence_pct','confidence_evidence_groups','confidence_sensor_breadth','evidence_observations']:
        req(envelope,n,'result confidence contract')
    req(envelope,'relative_model_ranking_weight_not_calibrated_identity_probability','posterior semantics preserved')
    req(envelope,'operational_positive_evidence_index_not_calibrated_probability','confidence semantics explicit')

    # Both inverse-model and fingerprint identity paths use the same fusion engine.
    req(infer,'ChemCoreEvidenceFusion.fuseForChemical(r.chemicalUid,r.evidenceObservations)','solver confidence fusion')
    forbid(infer,'note+="; reliability="','legacy 1..5 reliability must not be primary result output')
    req(fp,'ChemCoreEvidenceFusion.fuseForChemical(uid,observations)','fingerprint confidence fusion')
    req(fp,'observations.add(fingerprintEvidence)','fingerprint evidence merged before scoring')

    # Keep the initial P6 regressions and add runtime/result integration regressions.
    for n in ['correlatedQsprChannelsCountAsOneIndependentEvidenceGroup','strongestTierWinsInsideOneIndependenceGroup','independentReferenceGroupsRemainIndependent','outOfDomainAndOtherChemicalDoNotBoostCandidateConfidence','m3IdentityEvidenceIsNonM4SupportedEvidenceUntilWeightCalibration']:
        req(legacy_test,n,'initial P6 tests')
    for n in ['threeQsprSensorChannelsFromOneModelAreOneEvidenceGroup','assertEquals(25.0,r.score.totalPct','assertEquals(41.0,r.score.totalPct','outOfDomainOrPoorFitAddsNoPointsAndNeverSubtracts','derivedReferenceFingerprintDoesNotDoubleCountDirectReferenceCurve','resultEnvelopeKeepsPosteriorAndAnalyticalConfidenceSemanticallySeparate','acceptedFingerprintProducesStructuredConfidenceButNeverReportablePpm']:
        req(runtime_test,n,'runtime P6 tests')

    print('HazRob 0.10.2 / HR069 P6 confidence fusion contract: PASS')
    print('Rule: confidence is 0..100 positive support, de-duplicated by independenceGroup; posterior and PPM remain separate.')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P6 confidence fusion contract: FAIL: {exc}')
        raise SystemExit(1)
