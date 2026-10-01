package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ChemCore decision layer over the inverse array solver.
 *
 * Primary/strict MQ ratios determine ranking, identity gates and reportable ppm. A second optional
 * corroborative map carries every physically interpretable degraded measurement with a 0..1 quality
 * weight. Those rows may strengthen the evidence trail/confidence but cannot upgrade outcome,
 * posterior ranking, M4 reportability or occupational quantitative interpretation.
 */
public final class ChemCoreInferenceEngine {
    private ChemCoreInferenceEngine() {}

    public static final class Context {
        public final String sessionId,samplingProfileId,modelVersion;
        public final double temperatureC,rh;
        public final boolean calibrationHealthValid;
        public final ChemCoreOpenSetGate.Result openSetResult;
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult){this(sessionId,samplingProfileId,modelVersion,temperatureC,rh,openSetResult,true);}
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult,boolean calibrationHealthValid){this.sessionId=sessionId==null?"":sessionId;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;this.modelVersion=modelVersion==null?"":modelVersion;this.temperatureC=temperatureC;this.rh=rh;this.openSetResult=openSetResult;this.calibrationHealthValid=calibrationHealthValid;}
    }

    /**
     * Standard application entry point. When the same session has just been extracted by
     * ChemCoreSessionFeatureAdapter, its degraded-but-interpretable measurements are injected as
     * support-only evidence automatically.
     */
    public static ChemCoreResultEnvelope analyze(Map<String,Double> observedRatios,List<InverseMolecularSolver.Candidate> candidates,SamplingProfile samplingProfile,Context context){
        String session=context==null?"":context.sessionId;
        return analyze(observedRatios,ChemCoreSessionFeatureAdapter.corroborativeRatiosFor(session),ChemCoreSessionFeatureAdapter.corroborativeWeightsFor(session),candidates,samplingProfile,context);
    }

    public static ChemCoreResultEnvelope analyze(Map<String,Double> observedRatios,
                                                  Map<String,Double> corroborativeRatios,
                                                  Map<String,Double> corroborativeQualityWeights,
                                                  List<InverseMolecularSolver.Candidate> candidates,
                                                  SamplingProfile samplingProfile,Context context){
        if(context==null)context=new Context("",samplingProfile==null?"":samplingProfile.profileId,"",Double.NaN,Double.NaN,null);
        if(corroborativeRatios==null)corroborativeRatios=Collections.emptyMap();if(corroborativeQualityWeights==null)corroborativeQualityWeights=Collections.emptyMap();
        int observedChannels=countObserved(observedRatios);
        if(observedChannels==0)return envelope(context,ChemCoreDecision.Outcome.INVALID_MEASUREMENT,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no valid MQ ratios",Collections.emptyList());
        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(observedRatios,candidates,context.temperatureC,context.rh,samplingProfile);
        List<InverseMolecularSolver.Result> solvable=new ArrayList<>();for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable)solvable.add(r);
        if(solvable.isEmpty()){
            ChemCoreDecision.Outcome emptyOutcome=context.openSetResult!=null&&!context.openSetResult.accepted?ChemCoreDecision.Outcome.UNKNOWN:ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;
            String emptyNote=context.openSetResult!=null&&!context.openSetResult.accepted?"open-set rejection with no solvable candidate: "+context.openSetResult.reason:"no candidate has enough overlapping response evidence";
            double emptyOod=context.openSetResult==null?Double.NaN:context.openSetResult.distance;
            ChemCoreResultEnvelope base=envelope(context,emptyOutcome,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,emptyOod,Double.NaN,Double.NaN,emptyNote,
                    summaries(ranked,observedChannels,candidates,corroborativeRatios,corroborativeQualityWeights,samplingProfile,context));
            return AcceptedFingerprintIdentityEngine.fuse(base,observedRatios,context.openSetResult);
        }

        InverseMolecularSolver.Result top=solvable.get(0);double[] p=new double[solvable.size()];for(int i=0;i<p.length;i++)p[i]=solvable.get(i).posterior;double entropy=ChemCoreOpenSetGate.normalizedEntropy(p);
        MixtureResidualGuard.Result consistency=MixtureResidualGuard.evaluate(solvable,2.5,.80,.25);
        ChemCoreSpecialistConflictEngine.Result specialist=ChemCoreSpecialistConflictEngine.evaluate(solvable);
        boolean allContributorsDeviceCalibrated=top.contributors>0&&top.calibratedContributors==top.contributors;
        ChemCoreDecision.EvidenceTier tier=top.highestEvidenceTier;
        if(allContributorsDeviceCalibrated&&context.calibrationHealthValid)tier=ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
        else if(hasReferenceBackedMultisensorSupport(top)&&consistency.verdict==MixtureResidualGuard.Verdict.SINGLE_CANDIDATE_CONSISTENT)tier=ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT;

        double ood=context.openSetResult==null?Double.NaN:context.openSetResult.distance;boolean openSetAccepted=context.openSetResult!=null&&context.openSetResult.accepted;ChemCoreDecision.Outcome outcome;String note;
        if(consistency.verdict==MixtureResidualGuard.Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE){outcome=ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE;note=consistency.reason;}
        else if(consistency.verdict==MixtureResidualGuard.Verdict.AMBIGUOUS_IDENTITY){outcome=ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;note=consistency.reason;}
        else if(specialist.verdict==ChemCoreSpecialistConflictEngine.Verdict.CROSS_SPECIALIST_CONFLICT){outcome=ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE;note=specialist.reason;}
        else if(specialist.holdNamedIdentity){outcome=ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;note=specialist.reason;}
        else if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&openSetAccepted&&top.allContributorsInsideDomain){outcome=ChemCoreDecision.Outcome.QUANTIFIED;note="all contributing response curves are device/reference calibrated, identity is accepted, and the joint solution remains inside every validated domain";}
        else if(tier.ordinal()>=ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT.ordinal()&&openSetAccepted&&top.allContributorsInsideDomain){outcome=ChemCoreDecision.Outcome.IDENTIFIED;note="multisensor identity is array-consistent with at least two direct/reference M2+ channels and inside every declared contributor domain; concentration is withheld until all contributing quantitative curves satisfy M4";}
        else if(context.openSetResult!=null&&!context.openSetResult.accepted){outcome=ChemCoreDecision.Outcome.SCREENING;note="OOD_LIKE diagnostic: "+context.openSetResult.reason+"; exploratory candidate retained for early-build review, but named identity and reportable ppm are not promoted";}
        else if(!top.allContributorsInsideDomain){outcome=ChemCoreDecision.Outcome.SCREENING;note="OUT_OF_DOMAIN diagnostic: best-supported candidate uses one or more response models outside a declared domain or lacks required environmental context; exploratory candidate retained, but validated ppm is withheld";}
        else{outcome=ChemCoreDecision.Outcome.SCREENING;note=openSetAccepted?"model-supported candidate only":"model-supported candidate only; open-set state unavailable, so candidate is retained for exploratory review without identity promotion";}
        if(specialist.verdict!=ChemCoreSpecialistConflictEngine.Verdict.NO_SPECIALIST_EVIDENCE)note+="; specialist_gate="+specialist.verdict.name()+"; "+specialist.reason;
        if(top.molecularContributors>0){String idx=Double.isFinite(top.qsprIndicatorPct)?String.format(Locale.US,"%.1f%%",top.qsprIndicatorPct):"—";note+="; PubChem/QSPR fit="+idx;}
        if(!corroborativeRatios.isEmpty())note+="; degraded/auxiliary MQ measurements retained as quality-weighted corroborative evidence";

        double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN;
        ChemCoreResultEnvelope base=envelope(context,outcome,tier,top.chemicalUid,top.label,reportablePpm,Double.NaN,Double.NaN,ood,top.posterior,entropy,note,
                summaries(solvable,observedChannels,candidates,corroborativeRatios,corroborativeQualityWeights,samplingProfile,context));
        // Specialist conflicts remain visible and prevent silent identity promotion while preserving
        // the ranked candidate for exploratory review during the early-build phase.
        if(specialist.holdNamedIdentity)return base;
        return AcceptedFingerprintIdentityEngine.fuse(base,observedRatios,context.openSetResult);
    }

    @Deprecated public static int reliabilityRating(ChemCoreDecision.EvidenceTier tier){if(tier==null)return 1;switch(tier){case M4_HAZROB_VALIDATED:return 5;case M3_ARRAY_CONSISTENT:return 4;case M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:return 3;case M1_MOLECULARLY_INFERRED:return 2;default:return 1;}}
    static boolean hasReferenceBackedMultisensorSupport(InverseMolecularSolver.Result r){return r!=null&&r.contributors>=2&&r.referenceContributors>=2&&r.allContributorsInsideDomain;}
    public static ChemCoreResultEnvelope analyzeLegacyManufacturerPriors(Map<String,Double> observedRatios,SamplingProfile samplingProfile,Context context){return analyze(observedRatios,ChemCoreLegacyCandidateRegistry.manufacturerSupportedCandidates(),samplingProfile,context);}
    public static ChemCoreResultEnvelope analyzeLegacyManufacturerPriors(Map<String,Double> observedRatios,Map<String,Double> corroborativeRatios,Map<String,Double> corroborativeQualityWeights,SamplingProfile samplingProfile,Context context){return analyze(observedRatios,corroborativeRatios,corroborativeQualityWeights,ChemCoreLegacyCandidateRegistry.manufacturerSupportedCandidates(),samplingProfile,context);}
    private static int countObserved(Map<String,Double> ratios){int n=0;if(ratios!=null)for(Double v:ratios.values())if(v!=null&&Double.isFinite(v)&&v>0)n++;return n;}

    private static List<ChemCoreResultEnvelope.CandidateSummary> summaries(List<InverseMolecularSolver.Result> rows,int observedChannels,
            List<InverseMolecularSolver.Candidate> candidates,Map<String,Double> corroborativeRatios,Map<String,Double> corroborativeQualityWeights,
            SamplingProfile samplingProfile,Context context){
        List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();if(rows!=null)for(InverseMolecularSolver.Result r:rows)if(r!=null&&r.solvable){
            double coverage=observedChannels>0?100.0*Math.min(r.contributors,observedChannels)/observedChannels:Double.NaN;
            double agreement=r.contributors>=2&&Double.isFinite(r.standardizedResidualRms)?100.0*Math.exp(-0.5*r.standardizedResidualRms*r.standardizedResidualRms):Double.NaN;
            ChemCoreDecision.EvidenceTier rowTier=(r.contributors>0&&r.calibratedContributors==r.contributors)?ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED:r.highestEvidenceTier;
            double evidence=evidenceQuality(rowTier);if(!r.allContributorsInsideDomain)evidence*=0.5;
            InverseMolecularSolver.Candidate candidate=findCandidate(candidates,r.chemicalUid);
            List<ChemCoreEvidenceObservation> allEvidence=withCorroboration(r,candidate,corroborativeRatios,corroborativeQualityWeights,samplingProfile,context);
            ChemCoreEvidenceFusion.Result confidence=ChemCoreEvidenceFusion.fuseForChemical(r.chemicalUid,allEvidence);
            out.add(new ChemCoreResultEnvelope.CandidateSummary(r.chemicalUid,r.label,r.posterior,r.bestPpm,r.standardizedResidualRms,r.contributors,rowTier,coverage,agreement,evidence,r.calibratedContributors,r.allContributorsInsideDomain,r.residualLog10BySensor,confidence.score.totalPct,confidence.uniqueEvidenceGroups,confidence.sensorBreadth,allEvidence));
        }return out;
    }

    private static List<ChemCoreEvidenceObservation> withCorroboration(InverseMolecularSolver.Result r,InverseMolecularSolver.Candidate candidate,
            Map<String,Double> ratios,Map<String,Double> quality,SamplingProfile profile,Context context){
        List<ChemCoreEvidenceObservation> out=new ArrayList<>(r.evidenceObservations);if(candidate==null||ratios==null||ratios.isEmpty()||!Double.isFinite(r.bestPpm)||r.bestPpm<=0)return out;
        for(ChemCoreResponseModel model:candidate.models){
            if(model==null)continue;String key=model.sensorKey();Double obs=lookup(ratios,key),qw=lookup(quality,key);if(obs==null||qw==null||!Double.isFinite(obs)||obs<=0||!Double.isFinite(qw)||qw<=0)continue;
            if(hasPrimarySensorEvidence(out,key)&&qw>=0.999)continue;
            ChemCoreResponseModel.Estimate p=model.predict(r.bestPpm,context.temperatureC,context.rh,profile);if(p==null||!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
            double sigma=ChemCoreUncertaintyPolicy.solverSigma(p.sigmaLog10,p.insideDomain);
            double residual=Math.log10(obs)-p.meanLog10Ratio,z=residual/sigma;
            out.add(ChemCoreEvidenceObservation.corroborativeFromResponseModel(r.chemicalUid,r.label,model,p,r.bestPpm,obs,residual,z,qw,"session measurement quality weight="+String.format(Locale.US,"%.3f",qw)));
        }
        return out;
    }

    private static boolean hasPrimarySensorEvidence(List<ChemCoreEvidenceObservation> rows,String sensor){if(rows==null||sensor==null)return false;for(ChemCoreEvidenceObservation e:rows)if(e!=null&&"PRIMARY".equals(e.supportRole)&&e.sensorKeys.contains(sensor.toLowerCase(Locale.US)))return true;return false;}
    private static InverseMolecularSolver.Candidate findCandidate(List<InverseMolecularSolver.Candidate> rows,long uid){if(rows!=null)for(InverseMolecularSolver.Candidate c:rows)if(c!=null&&c.chemicalUid==uid)return c;return null;}
    private static Double lookup(Map<String,Double> m,String key){if(m==null||key==null)return null;Double v=m.get(key);if(v!=null)return v;v=m.get(key.toLowerCase(Locale.US));if(v!=null)return v;return m.get(key.toUpperCase(Locale.US));}

    private static double evidenceQuality(ChemCoreDecision.EvidenceTier tier){if(tier==null)return 0;switch(tier){case M4_HAZROB_VALIDATED:return 100;case M3_ARRAY_CONSISTENT:return 85;case M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:return 60;case M1_MOLECULARLY_INFERRED:return 30;default:return 15;}}
    private static ChemCoreResultEnvelope envelope(ChemCoreInferenceEngine.Context c,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier tier,Long uid,String label,double ppm,double lo,double hi,double ood,double posterior,double entropy,String note,List<ChemCoreResultEnvelope.CandidateSummary> candidates){return new ChemCoreResultEnvelope(c.sessionId,outcome,tier,uid,label,ppm,lo,hi,ood,posterior,entropy,c.samplingProfileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,c.modelVersion,note,candidates);}
}
