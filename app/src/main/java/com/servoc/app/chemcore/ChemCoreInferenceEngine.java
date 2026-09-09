package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ChemCore decision layer over the inverse array solver. It deliberately separates a
 * model-supported concentration that helps rank hypotheses from an operationally reportable
 * quantitative concentration. M1/M2 priors can make HazRob useful before local experiments,
 * but they cannot silently become M4 quantitative measurements.
 */
public final class ChemCoreInferenceEngine {
    private ChemCoreInferenceEngine() {}

    public static final class Context {
        public final String sessionId,samplingProfileId,modelVersion;
        public final double temperatureC,rh;
        public final ChemCoreOpenSetGate.Result openSetResult;
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult){
            this.sessionId=sessionId==null?"":sessionId;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
            this.modelVersion=modelVersion==null?"":modelVersion;this.temperatureC=temperatureC;this.rh=rh;this.openSetResult=openSetResult;
        }
    }

    public static ChemCoreResultEnvelope analyze(Map<String,Double> observedRatios,List<InverseMolecularSolver.Candidate> candidates,
                                                  SamplingProfile samplingProfile,Context context){
        if(context==null)context=new Context("",samplingProfile==null?"":samplingProfile.profileId,"",Double.NaN,Double.NaN,null);
        if(observedRatios==null||observedRatios.isEmpty())return envelope(context,ChemCoreDecision.Outcome.INVALID_MEASUREMENT,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no valid MQ ratios",Collections.emptyList());
        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(observedRatios,candidates,context.temperatureC,context.rh,samplingProfile);
        List<InverseMolecularSolver.Result> solvable=new ArrayList<>();for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable)solvable.add(r);
        if(solvable.isEmpty())return envelope(context,ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no candidate has enough overlapping response evidence",summaries(ranked));

        InverseMolecularSolver.Result top=solvable.get(0);double[] p=new double[solvable.size()];for(int i=0;i<p.length;i++)p[i]=solvable.get(i).posterior;double entropy=ChemCoreOpenSetGate.normalizedEntropy(p);
        MixtureResidualGuard.Result consistency=MixtureResidualGuard.evaluate(solvable,2.5,.80,.25);
        ChemCoreDecision.EvidenceTier tier=top.highestEvidenceTier;
        if(top.calibratedContributors>0)tier=ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
        else if(top.contributors>=2&&consistency.verdict==MixtureResidualGuard.Verdict.SINGLE_CANDIDATE_CONSISTENT)
            tier=ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT;

        double ood=context.openSetResult==null?Double.NaN:context.openSetResult.distance;
        boolean openSetAccepted=context.openSetResult!=null&&context.openSetResult.accepted;
        ChemCoreDecision.Outcome outcome;String note;
        if(consistency.verdict==MixtureResidualGuard.Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE){
            outcome=ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE;note=consistency.reason;
        }else if(context.openSetResult!=null&&!context.openSetResult.accepted){
            outcome=ChemCoreDecision.Outcome.UNKNOWN;note="open-set rejection: "+context.openSetResult.reason;
        }else if(consistency.verdict==MixtureResidualGuard.Verdict.AMBIGUOUS_IDENTITY){
            outcome=ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;note=consistency.reason;
        }else if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&openSetAccepted&&top.allContributorsInsideDomain){
            outcome=ChemCoreDecision.Outcome.QUANTIFIED;note="device-calibrated quantitative support plus accepted identity-domain gate";
        }else if(tier.ordinal()>=ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT.ordinal()&&openSetAccepted){
            outcome=ChemCoreDecision.Outcome.IDENTIFIED;note="multisensor identity is array-consistent and inside the trained identity domain; concentration is not M4 validated";
        }else{
            outcome=ChemCoreDecision.Outcome.SCREENING;note="model-supported candidate only; no validated open-set identity gate and/or device calibration";
        }

        // A fitted ppm helps explain/rank hypotheses at M1-M3, but only M4 QUANTIFIED exposes it as a measurement result.
        double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN;
        return envelope(context,outcome,tier,top.chemicalUid,top.label,reportablePpm,Double.NaN,Double.NaN,ood,top.posterior,entropy,note,summaries(solvable));
    }

    public static ChemCoreResultEnvelope analyzeLegacyManufacturerPriors(Map<String,Double> observedRatios,SamplingProfile samplingProfile,Context context){
        return analyze(observedRatios,ChemCoreLegacyCandidateRegistry.manufacturerSupportedCandidates(),samplingProfile,context);
    }

    private static List<ChemCoreResultEnvelope.CandidateSummary> summaries(List<InverseMolecularSolver.Result> rows){List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();if(rows!=null)for(InverseMolecularSolver.Result r:rows)if(r!=null&&r.solvable)out.add(new ChemCoreResultEnvelope.CandidateSummary(r.chemicalUid,r.label,r.posterior,r.bestPpm,r.standardizedResidualRms,r.contributors,r.highestEvidenceTier));return out;}
    private static ChemCoreResultEnvelope envelope(Context c,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier tier,Long uid,String label,double ppm,double lo,double hi,double ood,double posterior,double entropy,String note,List<ChemCoreResultEnvelope.CandidateSummary> candidates){return new ChemCoreResultEnvelope(c.sessionId,outcome,tier,uid,label,ppm,lo,hi,ood,posterior,entropy,c.samplingProfileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,c.modelVersion,note,candidates);}
}
