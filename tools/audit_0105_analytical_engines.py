#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P5 ChemCore analytical-engine contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    evidence=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreEvidenceObservation.java')
    contract=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreResponseModel.java')
    solver=txt('app/src/main/java/com/servoc/app/chemcore/InverseMolecularSolver.java')
    qspr=txt('app/src/main/java/com/servoc/app/chemcore/QsprResponseModelAdapter.java')
    qreg=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreQsprRuntimeRegistry.java')
    ref=txt('app/src/main/java/com/servoc/app/chemcore/AcceptedReferenceResponseModel.java')
    legacy=txt('app/src/main/java/com/servoc/app/chemcore/LegacyGasEngineResponseModel.java')
    power=txt('app/src/main/java/com/servoc/app/chemcore/PowerLawResponseModel.java')
    piece=txt('app/src/main/java/com/servoc/app/chemcore/PiecewiseLogResponseModel.java')
    fp=txt('app/src/main/java/com/servoc/app/chemcore/AcceptedFingerprintIdentityEngine.java')
    generic_fp=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreFingerprintEvidenceEngine.java')
    infer=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreInferenceEngine.java')
    openset=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreOpenSetGate.java')
    mixture=txt('app/src/main/java/com/servoc/app/chemcore/MixtureResidualGuard.java')

    # One typed contract, with independence separated from sensor breadth.
    for n in ['SCHEMA="chemcore-evidence/1"','independenceGroup','sensorKeys','quantitativeEstimateSupported','reportableQuantitative','uniqueIndependenceGroups','uniqueSensors']:
        req(evidence,n,'typed evidence')
    for n in ['default ChemCoreEvidenceObservation.Type evidenceType()','default String evidenceFamilyId()','default String sourceId()','default String independenceGroup()','default boolean reportableQuantitative()']:
        req(contract,n,'response-model evidence contract')

    # QSPR stays M1/inferential and sensor channels share one evidentiary lineage.
    req(qspr,'M1_MOLECULARLY_INFERRED','QSPR M1 tier')
    req(qspr,'"QSPR:"+ChemCoreQsprRuntimeRegistry.VERSION','QSPR correlated independence group')
    req(qspr,'reportableQuantitative(){return false;}','QSPR cannot be reportable ppm')
    req(qreg,'leaveCompoundOut','QSPR leave-compound-out validation')
    req(qreg,'QSPR-only compounds are available for estimated identification','QSPR-only candidate path')

    # Curves remain domain bounded and retain source-specific provenance.
    req(ref,'!c.inDeclaredRange(ppm)','literature domain bound')
    req(ref,'ChemCoreEvidenceObservation.Type.LITERATURE_CURVE','literature evidence type')
    req(ref,'ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE','manufacturer evidence type')
    req(ref,'ChemCoreEvidenceObservation.Type.RESEARCH_CURVE','research evidence type')
    req(ref,'reportableQuantitative(){return false;}','M2 reference curve not reportable')
    req(legacy,'MANUFACTURER_CURVE','legacy manufacturer provenance')
    req(power,'ppm<minPpm||ppm>maxPpm','power-law no extrapolation')
    req(piece,'no extrapolation outside calibrated/source range','piecewise no extrapolation')

    # Solver emits typed observations only after final fit and does not replace posterior semantics.
    for n in ['evidenceObservations','uniqueEvidenceGroups','evidenceSensorBreadth','evidenceAt(observedRatios,c,fittedPpm','ChemCoreEvidenceObservation.fromResponseModel']:
        req(solver,n,'solver evidence emission')
    req(solver,'normalizePosteriors(out)','posterior ranking retained')
    forbid(solver,'ChemCoreConfidenceMatrix','P5 must not perform P6 confidence fusion')

    # Fingerprint evidence is identity-only and future HR069 experimental fingerprints need no new engine.
    req(fp,'REFERENCE_FINGERPRINT','accepted reference fingerprint type')
    req(fp,'ChemCoreFingerprintEvidenceEngine.toEvidence','accepted fingerprint delegates to shared typed adapter')
    req(generic_fp,'SourceProfile hr069Experimental','future HR069 experimental fingerprint source profile')
    req(generic_fp,'HR069_EXPERIMENTAL_FINGERPRINT','experimental fingerprint type')
    req(generic_fp,'true,false,false,Double.NaN','fingerprint observations remain identity-only')
    req(generic_fp,'source.requireManufacturerHeaterProtocol','fingerprint evidence breadth follows heater gate')

    # Open-set and mixture/interference fail-safe outcomes remain explicit.
    req(openset,'outside trained feature domain','open-set rejection')
    req(infer,'Outcome.UNKNOWN','unknown outcome')
    req(mixture,'POSSIBLE_MIXTURE_OR_INTERFERENCE','mixture/interference verdict')
    req(infer,'Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE','mixture/interference outcome')
    req(infer,'Outcome.INSUFFICIENT_EVIDENCE','ambiguous/insufficient outcome')

    # Identification and fitted concentration remain separate from reportable quantitative output.
    req(infer,'double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN','reportable ppm gate')
    req(infer,'concentration is withheld until all contributing quantitative curves satisfy M4','identity/quantitation separation')

    print('HazRob 0.10.2 / HR069 P5 analytical engines contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P5 analytical engines contract: FAIL: {exc}')
        raise SystemExit(1)
