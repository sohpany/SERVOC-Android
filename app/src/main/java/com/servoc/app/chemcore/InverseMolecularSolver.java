package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fits each chemical hypothesis against the complete available MQ array by searching for
 * one concentration that jointly explains the observed log-response. This is more
 * restrictive than independently inverting each sensor curve.
 *
 * Candidate fit quality is compared using the weighted mean standardized residual loss rather
 * than the raw accumulated chi-square. This prevents a hypothesis with fewer usable MQ channels
 * from receiving an artificial ranking advantage merely because it contributes fewer residual
 * terms. Evidence breadth/independence remain separate decision and confidence dimensions.
 */
public final class InverseMolecularSolver {
    /** Softmax output is a relative model-ranking weight, not a calibrated probability of presence. */
    public static final String RANKING_SEMANTICS="relative_softmax_model_weight_not_calibrated_probability";

    private InverseMolecularSolver() {}

    public static final class Candidate {
        public final long chemicalUid;
        public final String label;
        public final List<ChemCoreResponseModel> models;
        public final double priorLogWeight;
        public final int minimumContributors;

        public Candidate(long chemicalUid,String label,List<ChemCoreResponseModel> models,double priorLogWeight,int minimumContributors){
            this.chemicalUid=chemicalUid;this.label=label==null?"":label;
            this.models=models==null?List.of():List.copyOf(models);
            this.priorLogWeight=Double.isFinite(priorLogWeight)?priorLogWeight:0;
            this.minimumContributors=Math.max(1,minimumContributors);
        }
    }

    public static final class Result {
        public final long chemicalUid;
        public final String label;
        public final boolean solvable;
        public final double bestPpm;
        public final double standardizedResidualRms;
        public final double logScore;
        /** Compatibility field: normalized relative ranking weight; never a calibrated identity probability. */
        public double posterior;
        public final int contributors;
        /** M1 molecular/QSPR sensor contributors used in the joint fit. */
        public final int molecularContributors;
        /** Direct/reference sensor contributors at M2 or above. M1 QSPR never counts here. */
        public final int referenceContributors;
        public final int calibratedContributors;
        /** QSPR-only standardized residual RMS across M1 sensor contributors. */
        public final double molecularResidualRms;
        /** Diagnostic fit-support index in percent; not a calibrated identity probability. */
        public final double qsprIndicatorPct;
        public final boolean allContributorsInsideDomain;
        public final ChemCoreDecision.EvidenceTier highestEvidenceTier;
        public final Map<String,Double> residualLog10BySensor;
        /** Typed P5 evidence observations. Sensor breadth and evidence independence remain distinct. */
        public final List<ChemCoreEvidenceObservation> evidenceObservations;
        public final int uniqueEvidenceGroups;
        public final int evidenceSensorBreadth;
        public final String note;

        /** Backward-compatible constructor retained for existing tests and callers. */
        Result(long chemicalUid,String label,boolean solvable,double bestPpm,double standardizedResidualRms,
               double logScore,int contributors,int molecularContributors,int referenceContributors,
               int calibratedContributors,double molecularResidualRms,double qsprIndicatorPct,
               boolean allContributorsInsideDomain,ChemCoreDecision.EvidenceTier highestEvidenceTier,
               Map<String,Double> residualLog10BySensor,String note){
            this(chemicalUid,label,solvable,bestPpm,standardizedResidualRms,logScore,contributors,molecularContributors,
                    referenceContributors,calibratedContributors,molecularResidualRms,qsprIndicatorPct,
                    allContributorsInsideDomain,highestEvidenceTier,residualLog10BySensor,Collections.emptyList(),note);
        }

        Result(long chemicalUid,String label,boolean solvable,double bestPpm,double standardizedResidualRms,
               double logScore,int contributors,int molecularContributors,int referenceContributors,
               int calibratedContributors,double molecularResidualRms,double qsprIndicatorPct,
               boolean allContributorsInsideDomain,ChemCoreDecision.EvidenceTier highestEvidenceTier,
               Map<String,Double> residualLog10BySensor,List<ChemCoreEvidenceObservation> evidenceObservations,String note){
            this.chemicalUid=chemicalUid;this.label=label;this.solvable=solvable;this.bestPpm=bestPpm;
            this.standardizedResidualRms=standardizedResidualRms;this.logScore=logScore;this.contributors=contributors;
            this.molecularContributors=Math.max(0,molecularContributors);this.referenceContributors=Math.max(0,referenceContributors);
            this.calibratedContributors=calibratedContributors;this.molecularResidualRms=molecularResidualRms;
            this.qsprIndicatorPct=qsprIndicatorPct;this.allContributorsInsideDomain=allContributorsInsideDomain;
            this.highestEvidenceTier=highestEvidenceTier;this.residualLog10BySensor=residualLog10BySensor;
            this.evidenceObservations=evidenceObservations==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(evidenceObservations));
            this.uniqueEvidenceGroups=ChemCoreEvidenceObservation.uniqueIndependenceGroups(this.evidenceObservations);
            this.evidenceSensorBreadth=ChemCoreEvidenceObservation.uniqueSensors(this.evidenceObservations);
            this.note=note==null?"":note;
        }

        /** Joint quantitative support is complete only when every contributor is M4 and the fit is inside domain. */
        public boolean hasDeviceValidatedQuantitativeSupport(){return contributors>0&&calibratedContributors==contributors&&allContributorsInsideDomain;}
    }

    private static final class Fit {
        double logPpm=Double.NaN,chi2=Double.POSITIVE_INFINITY,weightSum=0;
        double molecularChi2=0,molecularWeightSum=0;
        int contributors=0,molecular=0,reference=0,calibrated=0;boolean allInside=true;
        ChemCoreDecision.EvidenceTier tier=ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY;
        Map<String,Double> residuals=new HashMap<>();
        boolean valid(){return Double.isFinite(logPpm)&&Double.isFinite(chi2)&&weightSum>0&&contributors>0;}
    }

    public static List<Result> solve(Map<String,Double> observedRatios,List<Candidate> candidates,
                                     double temperatureC,double rh,SamplingProfile samplingProfile){
        List<Result> out=new ArrayList<>();
        if(observedRatios==null||candidates==null)return out;
        for(Candidate c:candidates)out.add(solveOne(observedRatios,c,temperatureC,rh,samplingProfile));
        normalizeRankingWeights(out);
        out.sort(Comparator.comparingDouble((Result r)->r.posterior).reversed());
        return out;
    }

    public static Result solveOne(Map<String,Double> observedRatios,Candidate c,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(c==null||c.models.isEmpty())return unsolved(c,"no response models");
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(ChemCoreResponseModel m:c.models){if(m!=null&&m.minPpm()>0&&m.maxPpm()>m.minPpm()){min=Math.min(min,m.minPpm());max=Math.max(max,m.maxPpm());}}
        if(!(min>0)||!(max>min))return unsolved(c,"invalid candidate concentration domain");
        double lo=Math.log10(min),hi=Math.log10(max);int grid=181;Fit best=null;double bestX=Double.NaN,step=(hi-lo)/(grid-1);
        for(int i=0;i<grid;i++){
            double x=lo+i*step;Fit f=scoreAt(observedRatios,c,x,temperatureC,rh,samplingProfile);
            if(f.contributors<c.minimumContributors)continue;
            if(best==null||fitLoss(f)<fitLoss(best)){best=f;bestX=x;}
        }
        if(best==null)return unsolved(c,"insufficient overlapping sensor evidence");

        double a=Math.max(lo,bestX-step),b=Math.min(hi,bestX+step);
        for(int iter=0;iter<36&&b-a>1e-6;iter++){
            double m1=a+(b-a)/3.0,m2=b-(b-a)/3.0;
            Fit f1=scoreAt(observedRatios,c,m1,temperatureC,rh,samplingProfile),f2=scoreAt(observedRatios,c,m2,temperatureC,rh,samplingProfile);
            double q1=f1.contributors>=c.minimumContributors?fitLoss(f1):Double.POSITIVE_INFINITY;
            double q2=f2.contributors>=c.minimumContributors?fitLoss(f2):Double.POSITIVE_INFINITY;
            if(q1<=q2)b=m2;else a=m1;
        }
        Fit refined=scoreAt(observedRatios,c,(a+b)/2.0,temperatureC,rh,samplingProfile);
        if(refined.contributors>=c.minimumContributors&&fitLoss(refined)<=fitLoss(best))best=refined;
        double normalizedLoss=fitLoss(best);
        double rms=Math.sqrt(normalizedLoss);
        double molecularRms=best.molecular>0&&best.molecularWeightSum>0?Math.sqrt(best.molecularChi2/best.molecularWeightSum):Double.NaN;
        double qsprIndex=best.molecular>=2&&Double.isFinite(molecularRms)?100.0*Math.exp(-0.5*molecularRms*molecularRms):Double.NaN;
        double domainPenalty=best.allInside?0:1.0;
        double logScore=c.priorLogWeight-0.5*normalizedLoss-domainPenalty;
        String note=best.allInside?"joint array fit inside contributing model domains":"joint fit uses at least one contributor outside its environmental domain";
        note+="; ranking_semantics="+RANKING_SEMANTICS+"; normalized_fit_loss="+String.format(Locale.US,"%.6f",normalizedLoss);
        if(best.molecular>0)note+="; QSPR M1="+best.molecular+" sensor channel(s), direct/reference M2+="+best.reference+" sensor channel(s)";
        double fittedPpm=Math.pow(10,best.logPpm);
        List<ChemCoreEvidenceObservation> evidence=evidenceAt(observedRatios,c,fittedPpm,temperatureC,rh,samplingProfile);
        int groups=ChemCoreEvidenceObservation.uniqueIndependenceGroups(evidence);
        note+="; evidence_groups="+groups+", sensor_breadth="+ChemCoreEvidenceObservation.uniqueSensors(evidence);
        return new Result(c.chemicalUid,c.label,true,fittedPpm,rms,logScore,best.contributors,
                best.molecular,best.reference,best.calibrated,molecularRms,qsprIndex,best.allInside,best.tier,best.residuals,evidence,note);
    }

    /** Weighted mean squared standardized residual used for cross-candidate fit comparability. */
    private static double fitLoss(Fit f){
        return f!=null&&f.valid()?f.chi2/Math.max(1e-12,f.weightSum):Double.POSITIVE_INFINITY;
    }

    private static Fit scoreAt(Map<String,Double> observed,Candidate c,double logPpm,double temperatureC,double rh,SamplingProfile samplingProfile){
        Fit f=new Fit();f.logPpm=logPpm;double ppm=Math.pow(10,logPpm);
        for(ChemCoreResponseModel m:c.models){
            if(m==null)continue;Double obs=lookup(observed,m.sensorKey());if(obs==null||!(obs>0)||!Double.isFinite(obs))continue;
            ChemCoreResponseModel.Estimate p=m.predict(ppm,temperatureC,rh,samplingProfile);if(p==null||!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
            double sigma=ChemCoreUncertaintyPolicy.solverSigma(p.sigmaLog10,p.insideDomain);
            ChemCoreDecision.EvidenceTier evidence=m.evidenceTier();
            double reliability=reliability(evidence);if(!p.insideDomain){reliability*=0.35;f.allInside=false;}
            double residual=Math.log10(obs)-p.meanLog10Ratio;double z=residual/sigma;
            if(f.contributors==0)f.chi2=0;
            double contribution=reliability*z*z;
            f.chi2+=contribution;f.weightSum+=reliability;f.contributors++;
            if(evidence==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED){f.molecular++;f.molecularChi2+=contribution;f.molecularWeightSum+=reliability;}
            if(rank(evidence)>=rank(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED))f.reference++;
            if(evidence==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED)f.calibrated++;
            if(rank(evidence)>rank(f.tier))f.tier=evidence;
            f.residuals.put(m.sensorKey().toLowerCase(Locale.US),residual);
        }
        return f;
    }

    /** Collect typed observations only for the final fit; the dense search loop remains allocation-light. */
    private static List<ChemCoreEvidenceObservation> evidenceAt(Map<String,Double> observed,Candidate c,double ppm,
                                                                 double temperatureC,double rh,SamplingProfile samplingProfile){
        List<ChemCoreEvidenceObservation> out=new ArrayList<>();
        for(ChemCoreResponseModel m:c.models){
            if(m==null)continue;Double obs=lookup(observed,m.sensorKey());if(obs==null||!(obs>0)||!Double.isFinite(obs))continue;
            ChemCoreResponseModel.Estimate p=m.predict(ppm,temperatureC,rh,samplingProfile);if(p==null||!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
            double sigma=ChemCoreUncertaintyPolicy.solverSigma(p.sigmaLog10,p.insideDomain);
            double residual=Math.log10(obs)-p.meanLog10Ratio;double z=residual/sigma;
            out.add(ChemCoreEvidenceObservation.fromResponseModel(c.chemicalUid,c.label,m,p,ppm,obs,residual,z));
        }
        return out;
    }

    private static Double lookup(Map<String,Double> m,String key){if(key==null)return null;Double x=m.get(key);if(x!=null)return x;x=m.get(key.toLowerCase(Locale.US));if(x!=null)return x;return m.get(key.toUpperCase(Locale.US));}
    private static double reliability(ChemCoreDecision.EvidenceTier t){if(t==null)return .25;switch(t){case M4_HAZROB_VALIDATED:return 1.0;case M3_ARRAY_CONSISTENT:return .85;case M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:return .60;case M1_MOLECULARLY_INFERRED:return .30;default:return .15;}}
    private static int rank(ChemCoreDecision.EvidenceTier t){return t==null?0:t.ordinal();}

    private static void normalizeRankingWeights(List<Result> rows){
        double max=Double.NEGATIVE_INFINITY;
        for(Result r:rows)if(r.solvable&&Double.isFinite(r.logScore))max=Math.max(max,r.logScore);
        if(!Double.isFinite(max))return;
        double sum=0;
        for(Result r:rows)if(r.solvable&&Double.isFinite(r.logScore)){
            r.posterior=Math.exp(Math.max(-700,r.logScore-max));sum+=r.posterior;
        }
        if(sum>0)for(Result r:rows)r.posterior/=sum;
    }

    private static Result unsolved(Candidate c,String why){return new Result(c==null?-1:c.chemicalUid,c==null?"":c.label,false,Double.NaN,Double.NaN,Double.NEGATIVE_INFINITY,0,0,0,0,Double.NaN,Double.NaN,false,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,new HashMap<>(),Collections.emptyList(),why);}
}
