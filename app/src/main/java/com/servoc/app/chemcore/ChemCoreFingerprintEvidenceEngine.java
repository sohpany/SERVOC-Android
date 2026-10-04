package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.FingerprintClassifier;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.sensors.RobotTelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic fingerprint-to-evidence adapter for ChemCore.
 *
 * New reference or HR069 experimental fingerprint libraries can be supplied as ordinary
 * ChemicalFingerprint lists plus a SourceProfile. No confidence, PPM or M4 promotion occurs here;
 * those decisions remain in P6/P10 and the device-validation gates.
 *
 * Fingerprints carry a legacy catalogue id. Evidence rows must carry ChemCore chemical_uid, so
 * all conversions pass through ChemCoreLegacyCandidateRegistry instead of repeating a local
 * legacy_id+1 assumption.
 */
public final class ChemCoreFingerprintEvidenceEngine {
    private ChemCoreFingerprintEvidenceEngine(){}

    public static final class SourceProfile {
        public final ChemCoreEvidenceObservation.Type type;
        public final ChemCoreDecision.EvidenceTier tier;
        public final String familyId,sourceId,independencePrefix;
        public final boolean requireManufacturerHeaterProtocol;

        public SourceProfile(ChemCoreEvidenceObservation.Type type,ChemCoreDecision.EvidenceTier tier,
                             String familyId,String sourceId,String independencePrefix,
                             boolean requireManufacturerHeaterProtocol){
            this.type=type==null?ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT:type;
            this.tier=tier==null?ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT:tier;
            this.familyId=clean(familyId,this.type.name());this.sourceId=clean(sourceId,"unknown-fingerprint-source");
            this.independencePrefix=clean(independencePrefix,this.type.name()+":"+this.sourceId);
            this.requireManufacturerHeaterProtocol=requireManufacturerHeaterProtocol;
        }

        public static SourceProfile hr069Experimental(String sourceRevision){
            String s=clean(sourceRevision,"HAZROB_EXPERIMENTAL");
            return new SourceProfile(ChemCoreEvidenceObservation.Type.HR069_EXPERIMENTAL_FINGERPRINT,
                    ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,"HR069_EXPERIMENTAL_FINGERPRINT",s,
                    "HR069_EXPERIMENTAL_FINGERPRINT:"+s,false);
        }
    }

    public static final class Decision {
        public final boolean accepted;
        public final FingerprintClassifier.Match top,runnerUp;
        public final ChemCoreEvidenceObservation evidence;
        public final String reason;
        Decision(boolean accepted,FingerprintClassifier.Match top,FingerprintClassifier.Match runnerUp,ChemCoreEvidenceObservation evidence,String reason){
            this.accepted=accepted;this.top=top;this.runnerUp=runnerUp;this.evidence=evidence;this.reason=reason==null?"":reason;
        }
    }

    public static Decision evaluate(Map<String,Double> ratios,List<ChemicalFingerprint> library,SourceProfile source,
                                    int minContributors,double maxDistance,double minScoreMargin,double minDistanceMargin){
        if(source==null)throw new IllegalArgumentException("fingerprint source profile required");
        if(library==null||library.isEmpty())return new Decision(false,null,null,null,"fingerprint library is empty");
        double[] vector=ratioVector(ratios,source.requireManufacturerHeaterProtocol);
        List<FingerprintClassifier.Match> rows=FingerprintClassifier.classify(vector,library);
        if(rows.isEmpty())return new Decision(false,null,null,null,"no fingerprint match");
        FingerprintClassifier.Match top=rows.get(0),runner=rows.size()>1?rows.get(1):null;
        int min=Math.max(1,minContributors);double distance=Double.isFinite(maxDistance)&&maxDistance>0?maxDistance:1.50;
        if(top.contributors<min)return new Decision(false,top,runner,null,"insufficient fingerprint sensor breadth");
        if(!Double.isFinite(top.distance)||top.distance>distance)return new Decision(false,top,runner,null,"fingerprint distance exceeds accepted gate");
        if(runner!=null){
            double scoreMargin=top.score-runner.score,distanceMargin=runner.distance-top.distance;
            if(scoreMargin<minScoreMargin&&distanceMargin<minDistanceMargin)return new Decision(false,top,runner,null,"fingerprint runner-up separation is insufficient");
        }
        long uid=ChemCoreLegacyCandidateRegistry.uidForLegacyChemical(top.fingerprint.chemicalId);
        if(uid<=0)return new Decision(false,top,runner,null,"ambiguous chemical_uid mapping for fingerprint identity");
        return new Decision(true,top,runner,toEvidence(top,ratios,source,uid),"accepted fingerprint evidence");
    }

    public static ChemCoreEvidenceObservation toEvidence(FingerprintClassifier.Match match,Map<String,Double> ratios,SourceProfile source){
        if(match==null||match.fingerprint==null)throw new IllegalArgumentException("fingerprint match required");
        long uid=ChemCoreLegacyCandidateRegistry.uidForLegacyChemical(match.fingerprint.chemicalId);
        if(uid<=0)throw new IllegalArgumentException("ambiguous chemical_uid mapping for fingerprint identity");
        return toEvidence(match,ratios,source,uid);
    }

    /** Explicit-UID overload for callers that already resolved the canonical ChemCore identity. */
    public static ChemCoreEvidenceObservation toEvidence(FingerprintClassifier.Match match,Map<String,Double> ratios,SourceProfile source,long chemicalUid){
        if(match==null||match.fingerprint==null)throw new IllegalArgumentException("fingerprint match required");
        if(source==null)throw new IllegalArgumentException("fingerprint source profile required");
        if(chemicalUid<=0)throw new IllegalArgumentException("positive canonical chemical_uid required");
        int chemical=match.fingerprint.chemicalId;long uid=chemicalUid;
        String label=chemical>=0&&chemical<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemical]:match.fingerprint.label;
        List<String> sensors=new ArrayList<>();int width=Math.min(RobotTelemetry.MQ_KEYS.length,match.fingerprint.meanLogRatio.length);
        for(int i=0;i<width;i++){
            String key=RobotTelemetry.MQ_KEYS[i];Double r=ratios==null?null:ratios.get(key);
            boolean allowed=!source.requireManufacturerHeaterProtocol||HazRobHardwareProfile.manufacturerHeaterProtocolValid(key);
            if(allowed&&r!=null&&Double.isFinite(r)&&r>0&&Double.isFinite(match.fingerprint.meanLogRatio[i]))sensors.add(key);
        }
        String modelId=source.familyId+":chem-"+chemical+":"+match.fingerprint.label;
        String group=source.independencePrefix+":chem-"+chemical;
        return new ChemCoreEvidenceObservation(source.type,uid,label,sensors,source.familyId,modelId,source.sourceId,group,source.tier,
                true,false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,match.distance,
                "identity-only fingerprint evidence; score="+fmt(match.score)+"; contributors="+match.contributors+
                        "; uid_policy="+ChemCoreLegacyCandidateRegistry.UID_POLICY_VERSION);
    }

    private static double[] ratioVector(Map<String,Double> ratios,boolean enforceHeaterProtocol){
        double[] out=new double[RobotTelemetry.MQ_KEYS.length];
        for(int i=0;i<out.length;i++){
            String key=RobotTelemetry.MQ_KEYS[i];Double v=ratios==null?null:ratios.get(key);
            boolean allowed=!enforceHeaterProtocol||HazRobHardwareProfile.manufacturerHeaterProtocolValid(key);
            out[i]=allowed&&v!=null&&Double.isFinite(v)&&v>0?v:Double.NaN;
        }
        return out;
    }

    private static String clean(String v,String fallback){return v==null||v.trim().isEmpty()?fallback:v.trim();}
    private static String fmt(double v){return Double.isFinite(v)?String.format(java.util.Locale.US,"%.3f",v):"NA";}
}
