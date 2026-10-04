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
import java.util.Locale;
import java.util.Map;

/**
 * HR069 identity-only fusion for source-backed fingerprints accepted as identification references.
 * It can promote a separated multi-channel match to IDENTIFIED/M3 only after an explicit accepted
 * open-set/OOD gate. It never creates M4 evidence or reportable ppm. P6/P7 re-run
 * independence-aware confidence fusion after adding the fingerprint.
 *
 * UID hardening: a fingerprint stores the zero-based legacy catalogue id, not chemical_uid.
 * Promotion therefore resolves the UID through ChemCoreLegacyCandidateRegistry, which shares the
 * database-backed QSPR UID when available. The previous local chemicalId+1 assumption has been
 * removed from this component so fingerprint fusion and QSPR/reference candidates cannot silently
 * describe the same compound under two different identifiers.
 */
public final class AcceptedFingerprintIdentityEngine {
    private AcceptedFingerprintIdentityEngine() {}

    public static final String VERSION="accepted-id-fp-v2-uid-safe";
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
        Decision(boolean accepted,FingerprintClassifier.Match top,FingerprintClassifier.Match runnerUp,ChemCoreEvidenceObservation evidence,String reason){this.accepted=accepted;this.top=top;this.runnerUp=runnerUp;this.evidence=evidence;this.reason=reason==null?"":reason;}
    }

    public static Decision evaluate(Map<String,Double> ratios){
        double[] vector=ratioVector(ratios);boolean[] heater=heaterValidity();
        List<FingerprintClassifier.Match> rows=FingerprintClassifier.classifyResearchPriors(vector,heater);
        if(rows.isEmpty())return new Decision(false,null,null,"no source-backed fingerprint match");
        FingerprintClassifier.Match top=rows.get(0),runner=rows.size()>1?rows.get(1):null;
        if(top.contributors<MIN_CONTRIBUTORS)return new Decision(false,top,runner,"fewer than two HR069 sensor channels overlap the fingerprint");
        if(!Double.isFinite(top.distance)||top.distance>MAX_DISTANCE)return new Decision(false,top,runner,"fingerprint distance exceeds accepted identity gate");
        if(runner!=null){double scoreMargin=top.score-runner.score;double distanceMargin=runner.distance-top.distance;if(scoreMargin<MIN_SCORE_MARGIN&&distanceMargin<MIN_DISTANCE_MARGIN)return new Decision(false,top,runner,"top fingerprint is not sufficiently separated from the runner-up");}
        return new Decision(true,top,runner,fingerprintEvidence(top,ratios),"accepted source-backed multi-channel fingerprint");
    }

    /**
     * Legacy two-argument entry point retained only so old callers fail closed. It deliberately
     * cannot promote a candidate because no explicit open-set/OOD acceptance was supplied.
     */
    @Deprecated
    public static ChemCoreResultEnvelope fuse(ChemCoreResultEnvelope base,Map<String,Double> ratios){
        return base;
    }

    /**
     * P7 identity fusion. A named identity is permitted only after the independent open-set/OOD
     * gate explicitly accepts the session feature vector. Missing or rejected gate results fail
     * closed and preserve the base outcome.
     */
    public static ChemCoreResultEnvelope fuse(ChemCoreResultEnvelope base,Map<String,Double> ratios,ChemCoreOpenSetGate.Result openSetResult){
        if(base==null)return null;
        if(openSetResult==null||!openSetResult.accepted)return base;
        if(!promotable(base.outcome))return base;
        Decision d=evaluate(ratios);if(!d.accepted||d.top==null)return base;
        int chemical=d.top.fingerprint.chemicalId;
        long uid=ChemCoreLegacyCandidateRegistry.uidForLegacyChemical(chemical);
        if(uid<=0)return base; // ambiguous canonical identity mapping: fail closed
        String label=chemical>=0&&chemical<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemical]:d.top.fingerprint.label;
        List<ChemCoreResultEnvelope.CandidateSummary> merged=mergeCandidates(base.candidates,d.top,d.evidence,uid,label);double rankingWeight=identityRankingWeight(d.top);
        String source=d.evidence==null?"":d.evidence.sourceId;
        String note=append(base.note,"accepted fingerprint identity reference after open-set acceptance: "+label+"; "+d.top.contributors+" HR069 channels; distance="+fmt(d.top.distance)+"; score="+fmt(d.top.score)+"; source="+source+"; state="+ACCEPTANCE_STATE+"; uid_policy="+ChemCoreLegacyCandidateRegistry.UID_POLICY_VERSION+"; non-M4 identity only; reportable ppm remains withheld");
        String model=appendModel(base.modelVersion,VERSION);
        return new ChemCoreResultEnvelope(base.sessionId,ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,uid,label,Double.NaN,Double.NaN,Double.NaN,base.oodScore,rankingWeight,base.normalizedEntropy,base.samplingProfileId,base.featureSchema,model,note,merged,base.libraryVersion,base.librarySha256,base.librarySourceCommit);
    }

    public static boolean isAcceptedIdentity(ChemCoreResultEnvelope r){return r!=null&&r.outcome==ChemCoreDecision.Outcome.IDENTIFIED&&r.evidenceTier==ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT&&r.modelVersion!=null&&r.modelVersion.contains(VERSION)&&!Double.isFinite(r.ppm);}

    private static ChemCoreEvidenceObservation fingerprintEvidence(FingerprintClassifier.Match m,Map<String,Double> ratios){
        String source="ResearchIdentificationFingerprintRegistry:"+ResearchIdentificationFingerprintRegistry.RELEASE+"-"+ResearchIdentificationFingerprintRegistry.HARDWARE_REVISION;
        ChemCoreFingerprintEvidenceEngine.SourceProfile profile=new ChemCoreFingerprintEvidenceEngine.SourceProfile(ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,"SOURCE_BACKED_REFERENCE_FINGERPRINT",source,"REFERENCE_FINGERPRINT:"+source,true);
        return ChemCoreFingerprintEvidenceEngine.toEvidence(m,ratios,profile);
    }

    private static boolean promotable(ChemCoreDecision.Outcome o){return o==ChemCoreDecision.Outcome.SCREENING||o==ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE;}
    private static List<ChemCoreResultEnvelope.CandidateSummary> mergeCandidates(List<ChemCoreResultEnvelope.CandidateSummary> base,FingerprintClassifier.Match match,ChemCoreEvidenceObservation fingerprintEvidence,long uid,String label){
        Map<Long,ChemCoreResultEnvelope.CandidateSummary> byUid=new LinkedHashMap<>();
        ChemCoreResultEnvelope.CandidateSummary old=null;
        if(base!=null)for(ChemCoreResultEnvelope.CandidateSummary c:base)if(c!=null){
            if(c.chemicalUid==uid){old=c;continue;}
            // A stale pre-hardening candidate may carry the same label under legacy_id+1.
            // Do not emit two identities for one chemical; only the canonical UID survives.
            if(sameLabel(c.label,label))continue;
            byUid.put(c.chemicalUid,c);
        }
        double posterior=Math.max(old==null||!Double.isFinite(old.posterior)?0.0:old.posterior,identityRankingWeight(match));double bestPpm=old==null?Double.NaN:old.bestPpm;double residual=Double.isFinite(match.distance)?match.distance:(old==null?Double.NaN:old.residualRms);
        int contributors=Math.max(match.contributors,old==null?0:old.contributors);double coverage=100.0*Math.min(1.0,contributors/8.0);double agreement=Double.isFinite(match.distance)?100.0*Math.exp(-0.5*match.distance*match.distance):Double.NaN;
        double evidenceQuality=Math.max(old==null||!Double.isFinite(old.evidenceQualityPct)?0:old.evidenceQualityPct,Math.min(85.0,55.0+7.5*Math.min(4,match.contributors)));int calibrated=old==null?0:old.calibratedContributors;boolean inside=old==null||old.allContributorsInsideDomain;Map<String,Double> residuals=old==null?Collections.emptyMap():old.residualLog10BySensor;
        List<ChemCoreEvidenceObservation> observations=new ArrayList<>();if(old!=null)observations.addAll(old.evidenceObservations);if(fingerprintEvidence!=null)observations.add(fingerprintEvidence);
        ChemCoreEvidenceFusion.Result confidence=ChemCoreEvidenceFusion.fuseForChemical(uid,observations);
        ChemCoreResultEnvelope.CandidateSummary promoted=new ChemCoreResultEnvelope.CandidateSummary(uid,label,posterior,bestPpm,residual,contributors,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,coverage,agreement,evidenceQuality,calibrated,inside,residuals,confidence.score.totalPct,confidence.uniqueEvidenceGroups,confidence.sensorBreadth,observations);
        List<ChemCoreResultEnvelope.CandidateSummary> out=new ArrayList<>();out.add(promoted);out.addAll(byUid.values());return out;
    }

    private static double identityRankingWeight(FingerprintClassifier.Match m){double distance=Double.isFinite(m.distance)?m.distance:MAX_DISTANCE;double quality=Math.exp(-0.5*distance*distance);double channel=Math.min(1.0,m.contributors/4.0);return Math.min(0.99,Math.max(0.51,0.55+0.30*quality+0.14*channel));}
    private static double[] ratioVector(Map<String,Double> ratios){double[] out=new double[RobotTelemetry.MQ_KEYS.length];for(int i=0;i<out.length;i++){Double v=ratios==null?null:ratios.get(RobotTelemetry.MQ_KEYS[i]);out[i]=v!=null&&Double.isFinite(v)&&v>0?v:Double.NaN;}return out;}
    private static boolean[] heaterValidity(){boolean[] out=new boolean[RobotTelemetry.MQ_KEYS.length];for(int i=0;i<out.length;i++)out[i]=HazRobHardwareProfile.manufacturerHeaterProtocolValid(RobotTelemetry.MQ_KEYS[i]);return out;}
    private static boolean sameLabel(String a,String b){return normalize(a).equals(normalize(b));}
    private static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.US);}
    private static String append(String base,String addition){return base==null||base.trim().isEmpty()?addition:base+"; "+addition;}
    private static String appendModel(String base,String addition){return base==null||base.trim().isEmpty()?addition:base+"+"+addition;}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"NA";}
}
