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
        public final double temperatureC,rh;\n        public final boolean calibrationHealthValid;
        public final ChemCoreOpenSetGate.Result openSetResult;
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult){
            this.sessionId=sessionId==null?"":sessionId;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
            this.modelVersion=modelVersion==null?"":modelVersion;this.temperatureC=temperatureC;this.rh=rh;this.openSetResult=openSetResult;
        }
    }

    public static ChemCoreResultEnvelope analyze(Map<String,Double> observedRatios,List<InverseMolecularSolver.Candidate> candidates,
                                                  SamplingProfile samplingProfile,Context context){
        if(context==null)context=new Context("",samplingProfile==null?"":samplingProfile.profileId,"",Double.NaN,Double.NaN,null);
        int observedChannels=countObserved(observedRatios);
        if(observedChannels==0)return envelope(context,ChemCoreDecision.Outcome.INVALID_MEASUREMENT,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no valid MQ ratios",Collections.emptyList());
        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(observedRatios,candidates,context.temperatureC,context.rh,samplingProfile);
        List<InverseMolecularSolver.Result> solvable=new ArrayList<>();for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable)solvable.add(r);
        if(solvable.isEmpty())return envelope(context,ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no candidate has enough overlapping response evidence",summaries(ranked,observedChannels));

        InverseMolecularSolver.Result top=solvable.get(0);double[] p=new double[solvable.size()];for(int i=0;i<p.length;i++)p[i]=solvable.get(i).posterior;double entropy=ChemCoreOpenSetGate.normalizedEntropy(p);
        MixtureResidualGuard.Result consistency=MixtureResidualGuard.evaluate(solvable,2.5,.80,.25);
        boolean allContributorsDeviceCalibrated=top.contributors>0&&top.calibratedContributors==top.contributors;
        ChemCoreDecision.EvidenceTier tier=top.highestEvidenceTier;
        if(allContributorsDeviceCalibrated&&context.calibrationHealthValid)tier=ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
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
        }else if(allContributorsDeviceCalibrated&&openSetAccepted&&!top.allContributorsInsideDomain){
            outcome=ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN;note="device-calibrated response exists but the fitted concentration is outside one or more validated contributor domains";
        }else if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&openSetAccepted&&top.allContributorsInsideDomain){
            outcome=ChemCoreDecision.Outcome.QUANTIFIED;note="all contributing response curves are device/reference calibrated, identity is accepted, and the joint solution remains inside every validated domain";
        }else if(tier.ordinal()>=ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT.ordinal()&&openSetAccepted){
            outcome=ChemCoreDecision.Outcome.IDENTIFIED;note="multisensor identity is array-consistent and inside the trained identity domain; concentration is withheld until all contributing quantitative curves satisfy M4";
        }else{
            outcome=ChemCoreDecision.Outcome.SCREENING;note="model-supported candidate only; validated open-set identity and/or complete device reference calibration is not available";
        }

        // A fitted ppm helps explain/rank hypotheses at M1-M3, but only M4 QUANTIFIED exposes it as a measurement result.
        double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN;
        return envelope(context,outcome,tier,top.chemicalUid,top.label,reportablePpm,Double.NaN,Double.NaN,ood,top.posterior,entropy,note,summaries(solvable,observedChannels));
    }

    public static ChemCoreResultEnvelope analyzeLegacyManufacturerPriors(Map<String,Double> observedRatios,SamplingProfile samplingProfile,Context context){
        return analyze(observedRatios,ChemCoreLegacyCandidateRegistry.manufacturerSupportedCandidates(),samplingProfile,context);
    }

    private static int countObserved(Map<String,Double> ratios){int n=0;if(ratios!=null)for(Double v:ratios.values())if(v!=null&&Double.isFinite(v)&&v>0)n++;return n;}

    private static List<ChemCoreResultEnvelope.CandidateSummary> summaries(List<InverseMolecularSolver.Result> rows,int observedChannels){
        List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();if(rows!=null)for(InverseMolecularSolver.Result r:rows)if(r!=null&&r.solvable){
            double coverage=observedChannels>0?100.0*Math.min(r.contributors,observedChannels)/observedChannels:Double.NaN;
            // Residual-agreement index is a diagnostic, not a calibrated probability. A single sensor has no inter-sensor agreement.
            double agreement=r.contributors>=2&&Double.isFinite(r.standardizedResidualRms)?100.0*Math.exp(-0.5*r.standardizedResidualRms*r.standardizedResidualRms):Double.NaN;
            ChemCoreDecision.EvidenceTier rowTier=(r.contributors>0&&r.calibratedContributors==r.contributors)?ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED:r.highestEvidenceTier;
            double evidence=evidenceQuality(rowTier);if(!r.allContributorsInsideDomain)evidence*=0.5;
            out.add(new ChemCoreResultEnvelope.CandidateSummary(r.chemicalUid,r.label,r.posterior,r.bestPpm,r.standardizedResidualRms,r.contributors,rowTier,
                    coverage,agreement,evidence,r.calibratedContributors,r.allContributorsInsideDomain,r.residualLog10BySensor));
        }return out;
    }

    private static double evidenceQuality(ChemCoreDecision.EvidenceTier tier){
        if(tier==null)return 0;switch(tier){
            case M4_HAZROB_VALIDATED:return 100;
            case M3_ARRAY_CONSISTENT:return 85;
            case M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:return 60;
            case M1_MOLECULARLY_INFERRED:return 30;
            default:return 15;
        }
    }

    private static ChemCoreResultEnvelope envelope(Context c,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier tier,Long uid,String label,double ppm,double lo,double hi,double ood,double posterior,double entropy,String note,List<ChemCoreResultEnvelope.CandidateSummary> candidates){return new ChemCoreResultEnvelope(c.sessionId,outcome,tier,uid,label,ppm,lo,hi,ood,posterior,entropy,c.samplingProfileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,c.modelVersion,note,candidates);}
}
