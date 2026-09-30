#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    live=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreLiveSensingEngine.java')
    alert=txt('app/src/main/java/com/servoc/app/measurement/GasAlertCoordinator.java')
    infer=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')
    for n in ['FLAMMABLE_VAPORS','ALCOHOL_VOC','COMBUSTION_SMOKE','REDUCING_GAS','ABNORMAL_GAS_ACTIVITY']:
        req(live,n,'broad hazard classes')
    req(live,'REFINE_INTERVAL_MS=1000L','live refinement cadence')
    req(live,'RECOVERY_HOLD_MS=3000L','live recovery hold')
    req(live,'ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates()','local candidate registry')
    req(live,'InverseMolecularSolver.solve','live pre-fit')
    req(live,'ChemCoreInferenceEngine.analyze','canonical inference')
    req(live,'liveFitGate','live open-set gate')
    req(live,'ChemCoreDecision.permitsNamedIdentity','name gate')
    req(live,'permitsQuantitativeOccupationalInterpretation','ppm reportability gate')
    req(live,'result.analyticalConfidencePct','analytical confidence source')
    req(live,'open_set_accepted','live audit payload')
    req(live,'ppm_reportable','ppm audit payload')
    req(infer,'openSetAccepted=context.openSetResult!=null&&context.openSetResult.accepted','canonical open-set-first semantics')
    req(alert,'chemCoreLive.updateBroad','canonical gas-alert integration')
    req(alert,'chemCoreLive.claimRefinement','throttled refinement integration')
    req(alert,'snapshot.put("live_chemcore"','canonical snapshot live payload')
    # Live chemistry is fully local and must never stall on network or sleeps.
    for bad in ['http://','https://','Thread.sleep','SystemClock.sleep']:
        forbid(live,bad,'live engine locality/non-blocking')
    # Broad warning stays driven by the independent operational response monitor.
    req(alert,'LiveGasStateEngine live=new LiveGasStateEngine()','broad warning source')
    req(alert,'RESPONSE_ALERT','danger before exact identity')
    print('HazRob 0.10.2 / HR069 P7 live sensing contract: PASS')
    return 0
if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as e:
        print(f'HazRob 0.10.2 / HR069 P7 live sensing contract: FAIL: {e}')
        raise SystemExit(1)
