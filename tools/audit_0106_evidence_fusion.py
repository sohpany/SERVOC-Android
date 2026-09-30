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
    test=txt('app/src/test/java/com/servoc/app/chemcore/ChemCoreEvidenceFusionTest.java')

    # Confidence schedule is now fed by independent evidence groups, not raw MQ channel counts.
    req(matrix,'scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth)','independent confidence matrix')
    req(matrix,'Counts represent independent evidence groups, not MQ sensor channels','independent count semantics')
    req(matrix,'@Deprecated','legacy raw-count API retained only for compatibility')

    # Fusion is positive-only, fail-closed outside model domain, and de-duplicates by lineage.
    for n in ['MAX_SUPPORT_STANDARDIZED_RESIDUAL=2.50','representative.get(e.independenceGroup)','representative.put(e.independenceGroup,e)','!e.insideDomain','Math.abs(e.standardizedResidual)>MAX_SUPPORT_STANDARDIZED_RESIDUAL','scoreIndependent(q,r,m4,breadth)']:
        req(fusion,n,'evidence fusion')
    req(fusion,'REFERENCE_FINGERPRINT&&hasDirectReference','derived fingerprint double-count guard')
    forbid(fusion,'posterior','confidence must not use posterior/model ranking')
    forbid(fusion,'-=', 'positive-only fusion must not subtract confidence')

    # Result payload exposes confidence as a separate operational index and retains audit evidence.
    for n in ['ANALYTICAL_CONFIDENCE_SEMANTICS','analytical_confidence_pct','confidence_evidence_groups','confidence_sensor_breadth','evidence_observations']:
        req(envelope,n,'result confidence contract')
    req(envelope,'relative_model_ranking_weight_not_calibrated_identity_probability','posterior semantics preserved')
    req(envelope,'operational_positive_evidence_index_not_calibrated_probability','confidence semantics explicit')

    # Both inverse-model and fingerprint paths run through the same fusion engine.
    req(infer,'ChemCoreEvidenceFusion.fuse(r.evidenceObservations)','solver confidence fusion')
    forbid(infer,'note+="; reliability="','legacy 1..5 reliability must not be primary result output')
    req(fp,'ChemCoreEvidenceFusion.fuse(observations)','fingerprint confidence fusion')
    req(fp,'observations.add(fingerprintEvidence)','fingerprint evidence merged before scoring')

    # Unit tests lock the critical scientific distinctions.
    for n in ['threeQsprSensorChannelsFromOneModelAreOneEvidenceGroup','assertEquals(25.0,r.score.totalPct','assertEquals(41.0,r.score.totalPct','outOfDomainOrPoorFitAddsNoPointsAndNeverSubtracts','derivedReferenceFingerprintDoesNotDoubleCountDirectReferenceCurve','resultEnvelopeKeepsPosteriorAndAnalyticalConfidenceSemanticallySeparate','acceptedFingerprintProducesStructuredConfidenceButNeverReportablePpm']:
        req(test,n,'P6 tests')

    print('HazRob 0.10.2 / HR069 P6 evidence fusion and confidence contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P6 evidence fusion and confidence contract: FAIL: {exc}')
        raise SystemExit(1)
