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
    uncertainty=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreUncertaintyPolicy.java')
    environment=txt('app/src/main/java/com/servoc/app/chemcore/ChemCoreEnvironmentDomainPolicy.java')
    mahal=txt('app/src/main/java/com/servoc/app/chemcore/MahalanobisModel.java')
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

    # One typed contract: independence, sensor breadth and measurement quality remain separate axes.
    for n in ['SCHEMA="chemcore-evidence/2"','independenceGroup','sensorKeys','quantitativeEstimateSupported','reportableQuantitative','measurementQualityWeight','supportRole','uniqueIndependenceGroups','uniqueSensors']:
        req(evidence,n,'typed evidence')
    req(evidence,'"CORROBORATIVE"','quality-weighted corroborative evidence role')
    req(evidence,'reportableQuantitative&&this.measurementQualityWeight>=0.999&&"PRIMARY".equals(this.supportRole)','degraded evidence cannot become reportable quantitative')
    for n in ['default ChemCoreEvidenceObservation.Type evidenceType()','default String evidenceFamilyId()','default String sourceId()','default String independenceGroup()','default boolean reportableQuantitative()']:
        req(contract,n,'response-model evidence contract')

    # QSPR stays M1/inferential and sensor channels share one evidentiary lineage.
    req(qspr,'M1_MOLECULARLY_INFERRED','QSPR M1 tier')
    req(qspr,'"QSPR:"+ChemCoreQsprRuntimeRegistry.VERSION','QSPR correlated independence group')
    req(qspr,'reportableQuantitative(){return false;}','QSPR cannot be reportable ppm')
    req(qreg,'leaveCompoundOut','QSPR leave-compound-out validation')
    req(qreg,'QSPR-only compounds are available for estimated identification','QSPR-only candidate path')

    # Reference uncertainty is explicit, centralized and remains a research proxy. R-squared is
    # never allowed to manufacture a sigma or a calibrated prediction interval.
    for n in [
        'VERSION="chemcore-uncertainty-policy/1"',
        'SEMANTICS="research_log10_response_sigma_proxy_not_calibrated_prediction_interval"',
        'LITERATURE_POWER_SIGMA_LOG10=0.20',
        'LITERATURE_LINEAR_SIGMA_LOG10=0.25',
        'MANUFACTURER_PRIMARY_SIGMA_LOG10=0.22',
        'MANUFACTURER_SECONDARY_SIGMA_LOG10=0.27',
        'RESEARCH_CURVE_SIGMA_LOG10=0.32',
        'SOLVER_FALLBACK_SIGMA_LOG10=0.20',
        'MIN_SIGMA_LOG10=0.02',
        'OUT_OF_DOMAIN_SIGMA_MULTIPLIER=1.80',
        'acceptsR2AsSigmaEvidence(){return false;}'
    ]: req(uncertainty,n,'uncertainty proxy policy')
    req(solver,'ChemCoreUncertaintyPolicy.solverSigma(p.sigmaLog10,p.insideDomain)','solver uses centralized uncertainty policy')
    req(infer,'ChemCoreUncertaintyPolicy.solverSigma(p.sigmaLog10,p.insideDomain)','corroborative inference uses centralized uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.LITERATURE_POWER_SIGMA_LOG10','literature power uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.LITERATURE_LINEAR_SIGMA_LOG10','literature linear uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.MANUFACTURER_PRIMARY_SIGMA_LOG10','manufacturer primary uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.MANUFACTURER_SECONDARY_SIGMA_LOG10','manufacturer secondary uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.RESEARCH_CURVE_SIGMA_LOG10','research curve uncertainty policy')
    req(ref,'ChemCoreUncertaintyPolicy.SEMANTICS','reference model records uncertainty semantics')
    forbid(ref,'estimate(ratio,0.20,','ad-hoc literature sigma is forbidden')
    forbid(ref,'estimate(ratio,0.25,','ad-hoc linear-literature sigma is forbidden')
    forbid(ref,'estimate(ratio,0.32,','ad-hoc research sigma is forbidden')

    # Environmental domain is fail-closed only where a response model actually declares a finite
    # T/RH domain. No generic compensation coefficient or invented tolerance is permitted.
    for n in [
        'VERSION="chemcore-environment-domain/1"',
        'SEMANTICS="declared_environment_domain_gate_no_generic_compensation"',
        'missingRequiredMeasurement',
        'tRequired&&!Double.isFinite(temperatureC)',
        'rhRequired&&!Double.isFinite(rh)'
    ]: req(environment,n,'environment domain policy')
    req(power,'ChemCoreEnvironmentDomainPolicy.evaluate(','power-law environment gate')
    req(piece,'ChemCoreEnvironmentDomainPolicy.evaluate(','piecewise environment gate')
    req(infer,'r.referenceContributors>=2&&r.allContributorsInsideDomain','M3 reference support requires all contributors inside domain')
    req(infer,'else if(openSetAccepted&&!top.allContributorsInsideDomain){outcome=ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN','out-of-domain named identity is withheld')
    forbid(environment,'EnvironmentalCompensationModel','domain gate must not silently activate compensation')

    # Missing-feature Mahalanobis must use the observed covariance marginal, not a principal block
    # of the full precision matrix. Coverage scaling is retained only for threshold compatibility.
    req(mahal,'MISSING_FEATURE_SEMANTICS="marginal_covariance_subspace_with_dimension_coverage_scaling"','missing-feature Mahalanobis semantics')
    req(mahal,'double[][] covarianceSubspace = new double[idx.length][idx.length]','observed covariance marginal')
    req(mahal,'marginalPrecision=invert(covarianceSubspace)','marginal covariance is inverted directly')
    req(mahal,'marginal*((double)mean.length/idx.length)','coverage scaling retained explicitly')
    forbid(mahal,'w[i][j] = inverseCovariance[idx[i]][idx[j]]','precision principal-block shortcut is forbidden')

    # Curves remain domain bounded and retain source-specific provenance.
    req(ref,'!c.inDeclaredRange(ppm)','literature domain bound')
    req(ref,'ChemCoreEvidenceObservation.Type.LITERATURE_CURVE','literature evidence type')
    req(ref,'ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE','manufacturer evidence type')
    req(ref,'ChemCoreEvidenceObservation.Type.RESEARCH_CURVE','research evidence type')
    req(ref,'reportableQuantitative(){return false;}','M2 reference curve not reportable')
    req(legacy,'MANUFACTURER_CURVE','legacy manufacturer provenance')
    req(power,'ppm<minPpm||ppm>maxPpm','power-law no extrapolation')
    req(piece,'no extrapolation outside calibrated/source range','piecewise no extrapolation')

    # Solver emits typed observations only after final fit. Cross-candidate ranking uses a
    # contributor-normalized weighted residual loss, while the compatibility `posterior` field is
    # explicitly retained only as a relative softmax model-ranking weight.
    for n in ['evidenceObservations','uniqueEvidenceGroups','evidenceSensorBreadth','evidenceAt(observedRatios,c,fittedPpm','ChemCoreEvidenceObservation.fromResponseModel']:
        req(solver,n,'solver evidence emission')
    req(solver,'RANKING_SEMANTICS="relative_softmax_model_weight_not_calibrated_probability"','ranking semantics are explicit')
    req(solver,'private static double fitLoss(Fit f)','normalized fit-loss helper')
    req(solver,'f.chi2/Math.max(1e-12,f.weightSum)','fit loss normalized by evidence weight')
    req(solver,'double logScore=c.priorLogWeight-0.5*normalizedLoss-domainPenalty;','candidate score uses normalized fit loss')
    req(solver,'normalizeRankingWeights(out)','relative ranking normalization retained')
    forbid(solver,'c.priorLogWeight-0.5*best.chi2-domainPenalty','raw contributor-count-biased chi-square ranking is forbidden')
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
