package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.fingerprint.FingerprintClassifier;
import com.servoc.app.fingerprint.ResearchIdentificationFingerprintRegistry;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.sensors.RobotTelemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HR069 identity-only fusion for source-backed fingerprints accepted as identification references.
 * It can promote a separated multi-channel match to IDENTIFIED/M3, but never creates M4 evidence
 * or reportable ppm. P5 also exposes the accepted match as a typed evidence observation.
 */
public final class AcceptedFingerprintIdentityEngine {
    private AcceptedFingerprintIdentityEngine() {}

    public static final String VERSION="accepted-id-fp-v1";
    public static final String ACCEPTANCE_STATE="ACCEPTED_FOR_IDENTIFICATION";
    public static final int MIN_CONTRIBUTORS=2;
    public static final double MAX_DISTANCE=1.50;
    public static final double MIN_SCORE_MARGIN=5.0;
    public static final double MIN_DISTANCE_MARGIN=0.20;

    public static final class Decision {
        public final boolean accepted;
        public final FingerprintClassifier.Match top,runnerUp;
        public final ChemCoreEvidenceObservation evidence;
        public final String reason;
        Decision(boolean accepted,FingerprintClassifier.Match top,FingerprintClassifier.Match runnerUp,String reason){this(accepted,top,runnerUp,null,reason);}
        Decision(boolean accepted,FingerprintClassifier.Match top,FingerprintClassifier.Match runnerUp,ChemCoreEvidenceObservation evidence,String reason){
            this.accepted=accepted;this.top=top;this.runnerUp=runnerUp;this.evidence=evidence;this.reason=reason==null?"":reason;
        }
    }

    public static Decision evaluate(Map<String,Double> ratios){
        double[] vector=ratioVector(ratios);boolean[] heater=heaterValidity();
        List<FingerprintClassifier.Match> rows=FingerprintClassifier.classifyResearchPriors(vector,heater);
        if(rows.isEmpty())return new Decision(false,null,null,"no source-backed fingerprint match");
        FingerprintClassifier.Match top=rows.get(0),runner=rows.size()>1?rows.get(1):null;
        if(top.contributors<MIN_CONTRIBUTORS)return new Decision(false,top,runner,"fewer than two HR069 sensor channels overlap the fingerprint");
        if(!Double.isFinite(top.distance)||top.distance>MAX_DISTANCE)return new Decision(false,top,runner,"fingerprint distance exceeds accepted identity gate");
        if(runner!=null){
            double scoreMargin=top.score-runner.score;double distanceMargin=runner.distance-top.distance;
            if(scoreMargin<MIN_SCORE_MARGIN&&distanceMargin<MIN_DISTANCE_MARGIN)return new Decision(false,top,runner,"top fingerprint is not sufficiently separated from the runner-up");
        }
        return new Decision(true,top,runner,fingerprintEvidence(top,ratios),"accepted source-backed multi-channel fingerprint");
    }

    public static ChemCoreResultEnvelope fuse(ChemCoreResultEnvelope base,Map<String,Double> ratios){
        if(base==null)return null;if(!promotable(base.outcome))return base;
        Decision d=evaluate(ratios);if(!d.accepted||d.top==null)return base;
        int chemical=d.top.fingerprint.chemicalId;long uid=chemical+1L;
        String label=chemical>=0&&chemical<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemical]:d.top.fingerprint.label;
        List<ChemCoreResultEnvelope.CandidateSummary> merged=mergeCandidates(base.candidates,d.top,uid,label);
        double rankingWeight=identityRankingWeight(d.top);
        String source=d.evidence==null?"":d.evidence.sourceId;
        String note=append(base.note,"accepted fingerprint identity reference: "+label+
                "; "+d.top.contributors+" HR069 channels; distance="+fmt(d.top.distance)+
                "; score="+fmt(d.top.score)+"; source="+source+"; state="+ACCEPTANCE_STATE+
                "; non-M4 identity only; reportable ppm remains withheld");
        String model=appendModel(base.modelVersion,VERSION);
        return new ChemCoreResultEnvelope(base.sessionId,ChemCoreDecision.Outcome.IDENTIFIED,
                ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,uid,label,
                Double.NaN,Double.NaN,Double.NaN,base.oodScore,rankingWeight,base.normalizedEntropy,
                base.samplingProfileId,base.featureSchema,model,note,merged,
                base.libraryVersion,base.librarySha256,base.librarySourceCommit);
    }

    public static boolean isAcceptedIdentity(ChemCoreResultEnvelope r){
        return r!=null&&r.outcome==ChemCoreDecision.Outcome.IDENTIFIED&&
                r.evidenceTier==ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT&&
                r.modelVersion!=null&&r.modelVersion.contains(VERSION)&&!Double.isFinite(r.ppm);
    }

    private static ChemCoreEvidenceObservation fingerprintEvidence(FingerprintClassifier.Match m,Map<String,Double> ratios){
        int chemical=m.fingerprint.chemicalId;long uid=chemical+1L;
        String label=chemical>=0&&chemical<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemical]:m.fingerprint.label;
        List<String> sensors=new ArrayList<>();
        int width=Math.min(RobotTelemetry.MQ_KEYS.length,m.fingerprint.meanLogRatio.length);
        for(int i=0;i<width;i++){
            Double r=ratios==null?null:ratios.get(RobotTelemetry.MQ_KEYS[i]);
            if(r!=null&&Double.isFinite(r)&&r>0&&Double.isFinite(m.fingerprint.meanLogRatio[i]))sensors.add(RobotTelemetry.MQ_KEYS[i]);
        }
        String source="ResearchIdentificationFingerprintRegistry:"+ResearchIdentificationFingerprintRegistry.RELEASE+"-"+ResearchIdentificationFingerprintRegistry.HARDWARE_REVISION;
        String modelId=VERSION+":chem-"+chemical+":"+m.fingerprint.label;
        String group="REFERENCE_FINGERPRINT:"+source+":chem-"+chemical;
        return new ChemCoreEvidenceObservation(ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT,uid,label,sensors,
                "SOURCE_BACKED_REFERENCE_FINGERPRINT",modelId,source,group,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,
                true,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,m.distance,
                "identity-only fingerprint; contributors="+m.contributors+"; score="+fmt(m.score));
    }

    private static boolean promotable(ChemCoreDecision.Outcome o){return o==ChemCoreDecision.Outcome.SCREENING||o==ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;}

    private static List<ChemCoreResultEnvelope.CandidateSummary> mergeCandidates(List<ChemCoreResultEnvelope.CandidateSummary> base,FingerprintClassifier.Match match,long uid,String label){
        Map<Long,ChemCoreResultEnvelope.CandidateSummary> byUid=new LinkedHashMap<>();if(base!=null)for(ChemCoreResultEnvelope.CandidateSummary c:base)if(c!=null)byUid.put(c.chemicalUid,c);
        ChemCoreResultEnvelope.CandidateSummary old=byUid.remove(uid);
        double posterior=Math.max(old==null||!Double.isFinite(old.posterior)?0.0:old.posterior,identityRankingWeight(match));
        double bestPpm=old==null?Double.NaN:old.bestPpm;double residual=Double.isFinite(match.distance)?match.distance:(old==null?Double.NaN:old.residualRms);
        double coverage=100.0*Math.min(1.0,match.contributors/8.0);double agreement=Double.isFinite(match.distance)?100.0*Math.exp(-0.5*match.distance*match.distance):Double.NaN;
        double evidence=Math.min(85.0,55.0+7.5*Math.min(4,match.contributors));int calibrated=old==null?0:old.calibratedContributors;
        boolean inside=old!=null&&old.allContributorsInsideDomain;Map<String,Double> residuals=old==null?Collections.emptyMap():old.residualLog10BySensor;
        ChemCoreResultEnvelope.CandidateSummary promoted=new ChemCoreResultEnvelope.CandidateSummary(uid,label,posterior,bestPpm,residual,match.contributors,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,coverage,agreement,evidence,calibrated,inside,residuals);
        List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();out.add(promoted);out.addAll(byUid.values());return out;
    }

    private static double identityRankingWeight(FingerprintClassifier.Match m){double distance=Double.isFinite(m.distance)?m.distance:MAX_DISTANCE;double quality=Math.exp(-0.5*distance*distance);double channel=Math.min(1.0,m.contributors/4.0);return Math.min(0.99,Math.max(0.51,0.55+0.30*quality+0.14*channel));}
    private static double[] ratioVector(Map<String,Double> ratios){double[] out=new double[RobotTelemetry.MQ_KEYS.length];for(int i=0;i<out.length;i++){Double v=ratios==null?null:ratios.get(RobotTelemetry.MQ_KEYS[i]);out[i]=v!=null&&Double.isFinite(v)&&v>0?v:Double.NaN;}return out;}
    private static boolean[] heaterValidity(){boolean[] out=new boolean[RobotTelemetry.MQ_KEYS.length];for(int i=0;i<out.length;i++)out[i]=HazRobHardwareProfile.manufacturerHeaterProtocolValid(RobotTelemetry.MQ_KEYS[i]);return out;}
    private static String append(String base,String addition){return base==null||base.trim().isEmpty()?addition:base+"; "+addition;}
    private static String appendModel(String base,String addition){return base==null||base.trim().isEmpty()?addition:base+"+"+addition;}
    private static String fmt(double v){return Double.isFinite(v)?String.format(java.util.Locale.US,"%.3f",v):"NA";}
}
