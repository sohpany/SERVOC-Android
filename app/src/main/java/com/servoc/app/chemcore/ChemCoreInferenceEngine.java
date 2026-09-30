package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        public final boolean calibrationHealthValid;
        public final ChemCoreOpenSetGate.Result openSetResult;
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult){
            this(sessionId,samplingProfileId,modelVersion,temperatureC,rh,openSetResult,true);
        }
        public Context(String sessionId,String samplingProfileId,String modelVersion,double temperatureC,double rh,ChemCoreOpenSetGate.Result openSetResult,boolean calibrationHealthValid){
            this.sessionId=sessionId==null?"":sessionId;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
            this.modelVersion=modelVersion==null?"":modelVersion;this.temperatureC=temperatureC;this.rh=rh;this.openSetResult=openSetResult;
            this.calibrationHealthValid=calibrationHealthValid;
        }
    }

    public static ChemCoreResultEnvelope analyze(Map<String,Double> observedRatios,List<InverseMolecularSolver.Candidate> candidates,
                                                  SamplingProfile samplingProfile,Context context){
        if(context==null)context=new Context("",samplingProfile==null?"":samplingProfile.profileId,"",Double.NaN,Double.NaN,null);
        int observedChannels=countObserved(observedRatios);
        if(observedChannels==0)return envelope(context,ChemCoreDecision.Outcome.INVALID_MEASUREMENT,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no valid MQ ratios",Collections.emptyList());
        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(observedRatios,candidates,context.temperatureC,context.rh,samplingProfile);
        List<InverseMolecularSolver.Result> solvable=new ArrayList<>();for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable)solvable.add(r);
        if(solvable.isEmpty()){
            ChemCoreResultEnvelope base=envelope(context,ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"no candidate has enough overlapping response evidence",summaries(ranked,observedChannels));
            return AcceptedFingerprintIdentityEngine.fuse(base,observedRatios);
        }

        InverseMolecularSolver.Result top=solvable.get(0);double[] p=new double[solvable.size()];for(int i=0;i<p.length;i++)p[i]=solvable.get(i).posterior;double entropy=ChemCoreOpenSetGate.normalizedEntropy(p);
        MixtureResidualGuard.Result consistency=MixtureResidualGuard.evaluate(solvable,2.5,.80,.25);
        boolean allContributorsDeviceCalibrated=top.contributors>0&&top.calibratedContributors==top.contributors;
        ChemCoreDecision.EvidenceTier tier=top.highestEvidenceTier;
        if(allContributorsDeviceCalibrated&&context.calibrationHealthValid)tier=ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
        else if(hasReferenceBackedMultisensorSupport(top)&&consistency.verdict==MixtureResidualGuard.Verdict.SINGLE_CANDIDATE_CONSISTENT)
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
            outcome=ChemCoreDecision.Outcome.IDENTIFIED;note="multisensor identity is array-consistent with at least two direct/reference M2+ channels and inside the trained identity domain; concentration is withheld until all contributing quantitative curves satisfy M4";
        }else{
            outcome=ChemCoreDecision.Outcome.SCREENING;note="model-supported candidate only";
        }
        if(top.molecularContributors>0){
            String idx=Double.isFinite(top.qsprIndicatorPct)?String.format(Locale.US,"%.1f%%",top.qsprIndicatorPct):"—";
            note+="; PubChem/QSPR fit="+idx;
        }

        // A fitted ppm helps explain/rank hypotheses at M1-M3, but only M4 QUANTIFIED exposes it as a measurement result.
        double reportablePpm=outcome==ChemCoreDecision.Outcome.QUANTIFIED?top.bestPpm:Double.NaN;
        ChemCoreResultEnvelope base=envelope(context,outcome,tier,top.chemicalUid,top.label,reportablePpm,Double.NaN,Double.NaN,ood,top.posterior,entropy,note,summaries(solvable,observedChannels));
        return AcceptedFingerprintIdentityEngine.fuse(base,observedRatios);
    }

    /** Legacy 1..5 display mapping retained only for backward-compatible callers; P6 uses 0..100 Analytical Confidence. */
    @Deprecated
    public static int reliabilityRating(ChemCoreDecision.EvidenceTier tier){
        if(tier==null)return 1;
        switch(tier){
            case M4_HAZROB_VALIDATED:return 5;
            case M3_ARRAY_CONSISTENT:return 4;
            case M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:return 3;
            case M1_MOLECULARLY_INFERRED:return 2;
            default:return 1;
        }
    }

    static boolean hasReferenceBackedMultisensorSupport(InverseMolecularSolver.Result r){
        return r!=null&&r.contributors>=2&&r.referenceContributors>=2;
    }

    public static ChemCoreResultEnvelope analyzeLegacyManufacturerPriors(Map<String,Double> observedRatios,SamplingProfile samplingProfile,Context context){
        return analyze(observedRatios,ChemCoreLegacyCandidateRegistry.manufacturerSupportedCandidates(),samplingProfile,context);
    }

    private static int countObserved(Map<String,Double> ratios){int n=0;if(ratios!=null)for(Double v:ratios.values())if(v!=null&&Double.isFinite(v)&&v>0)n++;return n;}

    private static List<ChemCoreResultEnvelope.CandidateSummary> summaries(List<InverseMolecularSolver.Result> rows,int observedChannels){
        List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();if(rows!=null)for(InverseMolecularSolver.Result r:rows)if(r!=null&&r.solvable){
            double coverage=observedChannels>0?100.0*Math.min(r.contributors,observedChannels)/observedChannels:Double.NaN;
            double agreement=r.contributors>=2&&Double.isFinite(r.standardizedResidualRms)?100.0*Math.exp(-0.5*r.standardizedResidualRms*r.standardizedResidualRms):Double.NaN;
            ChemCoreDecision.EvidenceTier rowTier=(r.contributors>0&&r.calibratedContributors==r.contributors)?ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED:r.highestEvidenceTier;
            double evidence=evidenceQuality(rowTier);if(!r.allContributorsInsideDomain)evidence*=0.5;
            ChemCoreEvidenceFusion.Result confidence=ChemCoreEvidenceFusion.fuse(r.evidenceObservations);
            out.add(new ChemCoreResultEnvelope.CandidateSummary(r.chemicalUid,r.label,r.posterior,r.bestPpm,r.standardizedResidualRms,r.contributors,rowTier,
                    coverage,agreement,evidence,r.calibratedContributors,r.allContributorsInsideDomain,r.residualLog10BySensor,
                    confidence.score.totalPct,confidence.uniqueEvidenceGroups,confidence.sensorBreadth,r.evidenceObservations));
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

    private static ChemCoreResultEnvelope envelope(ChemCoreInferenceEngine.Context c,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier tier,Long uid,String label,double ppm,double lo,double hi,double ood,double posterior,double entropy,String note,List<ChemCoreResultEnvelope.CandidateSummary> candidates){return new ChemCoreResultEnvelope(c.sessionId,outcome,tier,uid,label,ppm,lo,hi,ood,posterior,entropy,c.samplingProfileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,c.modelVersion,note,candidates);}
}
