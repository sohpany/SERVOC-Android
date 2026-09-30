#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P7 mandatory open-set/OOD identity gate contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    fp=txt('app/src/main/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngine.java')
    infer=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')
    gate=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreOpenSetGate.java')
    calibrator=txt('app/src/main/java/com/servoc/app/chemcore/OpenSetThresholdCalibrator.java')
    test=txt('app/src/test/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngineTest.java')

    # Identity fusion itself must fail closed when no explicit open-set/OOD acceptance is supplied.
    req(fp,'@Deprecated\n    public static ChemCoreResultEnvelope fuse(ChemCoreResultEnvelope base,Map<String,Double> ratios){\n        return base;','legacy fingerprint entry point fails closed')
    req(fp,'fuse(ChemCoreResultEnvelope base,Map<String,Double> ratios,ChemCoreOpenSetGate.Result openSetResult)','open-set-aware fingerprint fusion')
    req(fp,'if(openSetResult==null||!openSetResult.accepted)return base;','missing/rejected open-set cannot create named identity')
    req(fp,'if(!promotable(base.outcome))return base;','UNKNOWN/OOD/mixture/quantified outcomes cannot be overwritten')
    req(fp,'non-M4 identity only; reportable ppm remains withheld','fingerprint remains identity-only')

    # Every live inference path supplying fingerprint promotion must pass the same explicit gate result.
    req(infer,'AcceptedFingerprintIdentityEngine.fuse(base,observedRatios,context.openSetResult)','live fingerprint fusion receives open-set result')
    if infer.count('AcceptedFingerprintIdentityEngine.fuse(base,observedRatios,context.openSetResult)') < 2:
        raise AssertionError('both no-solver and solved inference paths must use the guarded fingerprint fusion')
    forbid(infer,'AcceptedFingerprintIdentityEngine.fuse(base,observedRatios);','unguarded live fingerprint promotion')
    req(infer,'emptyOutcome=context.openSetResult!=null&&!context.openSetResult.accepted?ChemCoreDecision.Outcome.UNKNOWN','no-solver open-set rejection remains UNKNOWN')
    req(infer,'named identity withheld until an explicit open-set/OOD gate accepts the session','missing gate semantics are explicit')

    # Open-set calibration remains empirical: no production claim is created by the software guard.
    req(gate,'outside trained feature domain','OOD rejection reason')
    req(gate,'fromValidatedThreshold','validated-threshold factory')
    req(calibrator,'independent-known-vs-heldout-unknown/1','independent validation protocol')
    req(calibrator,'falseAcceptRate','unknown false-accept metric')
    req(calibrator,'falseRejectRate','known false-reject metric')

    for n in [
        'missingOpenSetGateFailsClosedEvenForExactAcceptedFingerprint',
        'rejectedOpenSetGateCannotBeOverriddenByExactAcceptedFingerprint',
        'acceptedFingerprintLayerCannotOverrideUnknownEvenWhenGateObjectIsAccepted',
        'inferenceWithoutOpenSetCannotPromoteFingerprintOnlyPath',
        'inferenceRejectedOpenSetReturnsUnknownBeforeFingerprintOnlyPath',
        'inferenceAcceptedOpenSetMayUseAcceptedFingerprintIdentityButStillNoPpm'
    ]:
        req(test,n,'P7 fail-closed regression')

    print('HazRob 0.10.2 / HR069 P7 open-set decision gate contract: PASS')
    print('Rule: named identity/PPM cannot bypass an explicit accepted open-set/OOD gate; empirical threshold validation remains pending HR069 sessions.')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P7 open-set decision gate contract: FAIL: {exc}')
        raise SystemExit(1)
