package com.servoc.app.chemcore;

import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.measurement.SensorHealthEngine;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight live analytical layer for HazRob 0.10.2 / HR069.
 *
 * Safety order is deliberate:
 *  1) broad gas/hazard class from persistent multi-sensor response;
 *  2) periodic candidate refinement (about once per second);
 *  3) named identity only after a conservative live-fit open-set gate is accepted;
 *  4) ppm remains hidden unless ChemCore itself returns an M4 QUANTIFIED result.
 *
 * This is an operational driving aid. The deep session open-set/feature analysis in P9 remains the
 * higher-accuracy scientific result and is not replaced by this live-fit gate.
 */
public final class ChemCoreLiveSensingEngine {
    public static final String POLICY_VERSION="chemcore-live/1";
    public static final long REFINE_INTERVAL_MS=1000L;
    public static final long RECOVERY_HOLD_MS=3000L;
    static final double LIVE_MAX_RESIDUAL_RMS=2.0;
    static final double LIVE_MIN_POSTERIOR=0.55;
    static final double LIVE_MIN_POSTERIOR_MARGIN=0.20;
    static final int LIVE_MIN_SENSOR_BREADTH=2;

    public enum Phase { CLEAR, GAS_ACTIVITY, BROAD_HAZARD_ALERT, CANDIDATE_REFINING, IDENTIFIED, UNKNOWN_OR_AMBIGUOUS, RECOVERING }
    public enum BroadClass { NONE, FLAMMABLE_VAPORS, ALCOHOL_VOC, COMBUSTION_SMOKE, REDUCING_GAS, ABNORMAL_GAS_ACTIVITY }
    public enum Severity { INFO, WARNING, DANGER }

    public static final class Snapshot {
        public final Phase phase;
        public final Severity severity;
        public final BroadClass broadClass;
        /** Operational pattern strength, not identity confidence. */
        public final double broadPatternStrengthPct;
        public final int broadSupportSensors;
        public final long firstSeenMs,lastUpdatedMs,responseDurationMs;
        public final Long chemicalUid;
        public final String chemicalLabel;
        public final double analyticalConfidencePct;
        /** Audit-only relative model ranking weight; never shown as identity probability. */
        public final double posterior;
        public final double ppm;
        public final boolean ppmReportable;
        public final boolean openSetAccepted;
        public final String openSetReason;
        public final String note;

        Snapshot(Phase phase,Severity severity,BroadClass broadClass,double broadPatternStrengthPct,int broadSupportSensors,
                 long firstSeenMs,long lastUpdatedMs,long responseDurationMs,Long chemicalUid,String chemicalLabel,
                 double analyticalConfidencePct,double posterior,double ppm,boolean ppmReportable,
                 boolean openSetAccepted,String openSetReason,String note){
            this.phase=phase==null?Phase.CLEAR:phase;this.severity=severity==null?Severity.INFO:severity;
            this.broadClass=broadClass==null?BroadClass.NONE:broadClass;
            this.broadPatternStrengthPct=finiteClamp(broadPatternStrengthPct,0,100);this.broadSupportSensors=Math.max(0,broadSupportSensors);
            this.firstSeenMs=Math.max(0,firstSeenMs);this.lastUpdatedMs=Math.max(0,lastUpdatedMs);this.responseDurationMs=Math.max(0,responseDurationMs);
            this.chemicalUid=chemicalUid;this.chemicalLabel=chemicalLabel==null?"":chemicalLabel;
            this.analyticalConfidencePct=Double.isFinite(analyticalConfidencePct)?Math.max(0,Math.min(100,analyticalConfidencePct)):Double.NaN;
            this.posterior=posterior;this.ppm=ppm;this.ppmReportable=ppmReportable;
            this.openSetAccepted=openSetAccepted;this.openSetReason=openSetReason==null?"":openSetReason;this.note=note==null?"":note;
        }

        public String primaryAr(){
            if(phase==Phase.IDENTIFIED&&!chemicalLabel.isEmpty())return chemicalLabel;
            switch(broadClass){
                case ALCOHOL_VOC:return "رصد كحولات / مركبات عضوية متطايرة";
                case FLAMMABLE_VAPORS:return "رصد أبخرة قابلة للاشتعال";
                case COMBUSTION_SMOKE:return "رصد غازات احتراق / دخان";
                case REDUCING_GAS:return "رصد غازات مختزلة";
                case ABNORMAL_GAS_ACTIVITY:return "رصد نشاط غازي غير طبيعي";
                default:return phase==Phase.RECOVERING?"انخفاض الاستجابة الغازية":"الهواء ضمن النمط المرجعي";
            }
        }

        public String secondaryAr(){
            if(phase==Phase.IDENTIFIED&&!chemicalLabel.isEmpty()){
                String s=Double.isFinite(analyticalConfidencePct)?String.format(Locale.US,"الثقة %.0f%%",analyticalConfidencePct):"تعرف كيميائي";
                if(ppmReportable&&Double.isFinite(ppm))s+=String.format(Locale.US," • %.1f ppm",ppm);
                return s;
            }
            if(phase==Phase.CANDIDATE_REFINING)return "جارٍ تحسين التعرف الكيميائي";
            if(phase==Phase.UNKNOWN_OR_AMBIGUOUS)return "الهوية غير مؤكدة — يستمر الإنذار الفئوي";
            if(broadClass!=BroadClass.NONE&&Double.isFinite(broadPatternStrengthPct))return String.format(Locale.US,"قوة النمط %.0f%% • %d حساس",broadPatternStrengthPct,broadSupportSensors);
            return "";
        }

        public JSONObject toJson(){
            JSONObject x=new JSONObject();try{
                x.put("policy",POLICY_VERSION);x.put("phase",phase.name());x.put("severity",severity.name());x.put("broad_class",broadClass.name());
                x.put("broad_pattern_strength_pct",Double.isFinite(broadPatternStrengthPct)?broadPatternStrengthPct:JSONObject.NULL);x.put("broad_support_sensors",broadSupportSensors);
                x.put("first_seen_ms",firstSeenMs);x.put("last_updated_ms",lastUpdatedMs);x.put("response_duration_ms",responseDurationMs);
                x.put("chemical_uid",chemicalUid==null?JSONObject.NULL:chemicalUid);x.put("chemical",chemicalLabel);
                x.put("analytical_confidence_pct",Double.isFinite(analyticalConfidencePct)?analyticalConfidencePct:JSONObject.NULL);x.put("posterior",Double.isFinite(posterior)?posterior:JSONObject.NULL);
                x.put("ppm",ppmReportable&&Double.isFinite(ppm)?ppm:JSONObject.NULL);x.put("ppm_reportable",ppmReportable);
                x.put("open_set_accepted",openSetAccepted);x.put("open_set_reason",openSetReason);x.put("note",note);
            }catch(Exception ignored){}return x;
        }
    }

    static final class BroadAssessment {
        final BroadClass broadClass;final double strengthPct;final int supportSensors;
        BroadAssessment(BroadClass c,double p,int n){broadClass=c;strengthPct=p;supportSensors=n;}
    }

    private Snapshot current=clearSnapshot(0);
    private long firstSeenMs=-1,lastActiveMs=-1,lastRefineClaimMs=-REFINE_INTERVAL_MS;
    private boolean refinementClaimed=false;
    private List<InverseMolecularSolver.Candidate> candidates=Collections.emptyList();

    public ChemCoreLiveSensingEngine(){refreshCandidates();}

    /** Refresh after analytical-library/QSPR rebuild; safe to call at any time. */
    public synchronized void refreshCandidates(){candidates=Collections.unmodifiableList(new ArrayList<>(ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates()));}

    public synchronized Snapshot snapshot(){return current;}

    public synchronized Snapshot updateBroad(JSONObject canonicalSnapshot,Map<String,SensorHealthEngine.Snapshot> health,
                                               LiveGasStateEngine.Snapshot signal,long nowMs){
        long now=Math.max(0,nowMs);LiveGasStateEngine.State state=signal==null?LiveGasStateEngine.State.UNAVAILABLE:signal.state;
        boolean active=state==LiveGasStateEngine.State.INSTANT_RESPONSE||state==LiveGasStateEngine.State.RESPONSE_WATCH||state==LiveGasStateEngine.State.RESPONSE_ALERT;
        if(!active){
            if(lastActiveMs>=0&&now-lastActiveMs<RECOVERY_HOLD_MS&&current.phase!=Phase.CLEAR){
                current=new Snapshot(Phase.RECOVERING,Severity.INFO,current.broadClass,current.broadPatternStrengthPct,current.broadSupportSensors,
                        firstSeenMs<0?now:firstSeenMs,now,firstSeenMs<0?0:now-firstSeenMs,null,"",Double.NaN,Double.NaN,Double.NaN,false,false,"recovery_hold","live response recovering");
                return current;
            }
            reset(now);return current;
        }

        if(firstSeenMs<0)firstSeenMs=now;lastActiveMs=now;
        Map<String,Double> deviations=logDeviations(health);BroadAssessment broad=classifyBroad(deviations);
        Severity severity=state==LiveGasStateEngine.State.RESPONSE_ALERT?Severity.DANGER:Severity.WARNING;
        Phase next=broad.broadClass==BroadClass.NONE?Phase.GAS_ACTIVITY:Phase.BROAD_HAZARD_ALERT;
        if(current.phase==Phase.IDENTIFIED&&current.chemicalUid!=null){
            current=new Snapshot(Phase.IDENTIFIED,severity,broad.broadClass,broad.strengthPct,broad.supportSensors,firstSeenMs,now,now-firstSeenMs,
                    current.chemicalUid,current.chemicalLabel,current.analyticalConfidencePct,current.posterior,current.ppm,current.ppmReportable,
                    current.openSetAccepted,current.openSetReason,current.note);return current;
        }
        if(refinementClaimed)next=Phase.CANDIDATE_REFINING;
        current=new Snapshot(next,severity,broad.broadClass,broad.strengthPct,broad.supportSensors,firstSeenMs,now,now-firstSeenMs,
                null,"",Double.NaN,Double.NaN,Double.NaN,false,false,"","broad live hazard classification");
        return current;
    }

    /** Reserve a heavy refinement slot; called from the existing 250 ms telemetry loop. */
    public synchronized boolean claimRefinement(long nowMs){
        long now=Math.max(0,nowMs);if(refinementClaimed)return false;
        boolean active=current.phase==Phase.GAS_ACTIVITY||current.phase==Phase.BROAD_HAZARD_ALERT||current.phase==Phase.CANDIDATE_REFINING||current.phase==Phase.UNKNOWN_OR_AMBIGUOUS||current.phase==Phase.IDENTIFIED;
        if(!active||now-lastRefineClaimMs<REFINE_INTERVAL_MS)return false;
        refinementClaimed=true;lastRefineClaimMs=now;
        if(current.phase!=Phase.IDENTIFIED)current=new Snapshot(Phase.CANDIDATE_REFINING,current.severity,current.broadClass,current.broadPatternStrengthPct,current.broadSupportSensors,
                current.firstSeenMs,now,current.responseDurationMs,current.chemicalUid,current.chemicalLabel,current.analyticalConfidencePct,current.posterior,current.ppm,current.ppmReportable,current.openSetAccepted,current.openSetReason,current.note);
        return true;
    }

    /** Heavier candidate refinement; intended for the app worker pool, never the drive/UI thread. */
    public Snapshot refineIdentity(JSONObject canonicalSnapshot,long nowMs){
        Snapshot broad;List<InverseMolecularSolver.Candidate> localCandidates;
        synchronized(this){broad=current;localCandidates=candidates;}
        Snapshot result=refineInternal(broad,canonicalSnapshot,localCandidates,Math.max(0,nowMs));
        synchronized(this){refinementClaimed=false;if(lastActiveMs>=0&&nowMs-lastActiveMs<=RECOVERY_HOLD_MS)current=result;return current;}
    }

    private static Snapshot refineInternal(Snapshot broad,JSONObject snapshot,List<InverseMolecularSolver.Candidate> candidates,long now){
        if(snapshot==null||candidates==null||candidates.isEmpty())return unresolved(broad,now,"candidate_registry_unavailable",false);
        Map<String,Double> ratios=ratios(snapshot);if(ratios.size()<LIVE_MIN_SENSOR_BREADTH)return unresolved(broad,now,"insufficient_live_ratio_coverage",false);
        JSONObject env=snapshot.optJSONObject("env");double t=env==null?Double.NaN:env.optDouble("temperature",Double.NaN),rh=env==null?Double.NaN:env.optDouble("humidity",Double.NaN);
        SamplingProfile profile=SamplingProfile.hazRobStandardChamberV2();
        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(ratios,candidates,t,rh,profile);
        ChemCoreOpenSetGate.Result gate=liveFitGate(ranked);
        ChemCoreInferenceEngine.Context ctx=new ChemCoreInferenceEngine.Context("LIVE",profile.profileId,"chemcore-live/1",t,rh,gate,true);
        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyze(ratios,candidates,profile,ctx);
        return applyInferenceResult(broad,result,gate,now);
    }

    static ChemCoreOpenSetGate.Result liveFitGate(List<InverseMolecularSolver.Result> ranked){
        InverseMolecularSolver.Result top=null,runner=null;if(ranked!=null)for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable){if(top==null)top=r;else{runner=r;break;}}
        if(top==null)return new ChemCoreOpenSetGate.Result(false,Double.NaN,LIVE_MAX_RESIDUAL_RMS,0,"live-fit: no solvable candidate");
        double margin=runner==null?top.posterior:top.posterior-runner.posterior;
        boolean accepted=top.contributors>=LIVE_MIN_SENSOR_BREADTH&&top.evidenceSensorBreadth>=LIVE_MIN_SENSOR_BREADTH&&top.allContributorsInsideDomain&&
                Double.isFinite(top.standardizedResidualRms)&&top.standardizedResidualRms<=LIVE_MAX_RESIDUAL_RMS&&
                Double.isFinite(top.posterior)&&top.posterior>=LIVE_MIN_POSTERIOR&&Double.isFinite(margin)&&margin>=LIVE_MIN_POSTERIOR_MARGIN;
        double coverage=Math.min(1.0,top.evidenceSensorBreadth/8.0);
        String reason="live-fit: contributors="+top.contributors+", breadth="+top.evidenceSensorBreadth+", residual="+fmt(top.standardizedResidualRms)+", posterior="+fmt(top.posterior)+", margin="+fmt(margin)+", inside_domain="+top.allContributorsInsideDomain;
        return new ChemCoreOpenSetGate.Result(accepted,top.standardizedResidualRms,LIVE_MAX_RESIDUAL_RMS,coverage,reason);
    }

    static Snapshot applyInferenceResult(Snapshot broad,ChemCoreResultEnvelope result,ChemCoreOpenSetGate.Result gate,long now){
        if(broad==null)broad=clearSnapshot(now);boolean accepted=gate!=null&&gate.accepted;
        if(result!=null&&accepted&&ChemCoreDecision.permitsNamedIdentity(result.outcome)&&result.chemicalUid!=null&&!result.chemicalLabel.isEmpty()){
            boolean ppmAllowed=ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(result.outcome,result.evidenceTier)&&Double.isFinite(result.ppm);
            return new Snapshot(Phase.IDENTIFIED,broad.severity,broad.broadClass,broad.broadPatternStrengthPct,broad.broadSupportSensors,
                    broad.firstSeenMs,now,Math.max(0,now-broad.firstSeenMs),result.chemicalUid,result.chemicalLabel,result.analyticalConfidencePct,result.posterior,
                    ppmAllowed?result.ppm:Double.NaN,ppmAllowed,true,gate.reason,"live identity accepted; "+result.outcome.name());
        }
        return new Snapshot(Phase.UNKNOWN_OR_AMBIGUOUS,broad.severity,broad.broadClass,broad.broadPatternStrengthPct,broad.broadSupportSensors,
                broad.firstSeenMs,now,Math.max(0,now-broad.firstSeenMs),null,"",Double.NaN,result==null?Double.NaN:result.posterior,Double.NaN,false,accepted,
                gate==null?"live-fit gate unavailable":gate.reason,result==null?"live inference unavailable":"live identity withheld; "+result.outcome.name());
    }

    static BroadAssessment classifyBroad(Map<String,Double> d){
        if(d==null||d.isEmpty())return new BroadAssessment(BroadClass.NONE,Double.NaN,0);
        Pattern alcohol=pattern(d,new String[]{"mq3","mq135","mq2"},new double[]{1.40,1.10,0.55});
        Pattern flammable=pattern(d,new String[]{"mq2","mq4","mq5","mq8","mq9"},new double[]{1.00,1.00,1.00,0.85,0.85});
        Pattern combustion=pattern(d,new String[]{"mq7","mq9","mq135","mq2"},new double[]{1.25,0.90,0.75,0.55});
        Pattern reducing=pattern(d,new String[]{"mq8","mq7","mq9","mq2"},new double[]{1.15,1.05,0.95,0.50});
        BroadClass best=BroadClass.ABNORMAL_GAS_ACTIVITY;Pattern p=null;
        if(alcohol.support>=2&&(p==null||alcohol.score>p.score)){best=BroadClass.ALCOHOL_VOC;p=alcohol;}
        if(flammable.support>=2&&(p==null||flammable.score>p.score)){best=BroadClass.FLAMMABLE_VAPORS;p=flammable;}
        if(combustion.support>=2&&(p==null||combustion.score>p.score)){best=BroadClass.COMBUSTION_SMOKE;p=combustion;}
        if(reducing.support>=2&&(p==null||reducing.score>p.score)){best=BroadClass.REDUCING_GAS;p=reducing;}
        if(p==null){int support=0;double max=0;for(double x:d.values())if(Double.isFinite(x)&&x>=0.045){support++;max=Math.max(max,x);}return new BroadAssessment(support>0?BroadClass.ABNORMAL_GAS_ACTIVITY:BroadClass.NONE,Math.min(100,100*max/0.08),support);}
        return new BroadAssessment(best,Math.min(100,100*p.meanDeviation/0.08),p.support);
    }

    static Map<String,Double> logDeviations(Map<String,SensorHealthEngine.Snapshot> health){
        Map<String,Double> out=new LinkedHashMap<>();if(health==null)return out;
        for(Map.Entry<String,SensorHealthEngine.Snapshot> e:health.entrySet()){SensorHealthEngine.Snapshot s=e.getValue();if(s==null)continue;
            boolean bad=s.state==SensorHealthEngine.State.MISSING||s.state==SensorHealthEngine.State.INVALID||s.state==SensorHealthEngine.State.SATURATED||s.state==SensorHealthEngine.State.STUCK;
            if(bad||!(s.lastRatio>0)||!(s.baselineRatio>0)||!Double.isFinite(s.lastRatio)||!Double.isFinite(s.baselineRatio))continue;
            out.put(e.getKey().toLowerCase(Locale.US),Math.abs(Math.log10(s.lastRatio/s.baselineRatio)));}
        return out;
    }

    private static Map<String,Double> ratios(JSONObject snapshot){Map<String,Double> out=new LinkedHashMap<>();JSONObject mq=snapshot==null?null:snapshot.optJSONObject("mq");if(mq==null)return out;
        for(String key:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"}){JSONObject r=mq.optJSONObject(key);if(r==null||!r.optBoolean("valid",r.optBoolean("ok",false)))continue;double q=r.optDouble("ratio",Double.NaN);if(Double.isFinite(q)&&q>0)out.put(key,q);}return out;}

    private synchronized void reset(long now){firstSeenMs=-1;lastActiveMs=-1;refinementClaimed=false;current=clearSnapshot(now);}
    private static Snapshot unresolved(Snapshot broad,long now,String why,boolean accepted){return new Snapshot(Phase.UNKNOWN_OR_AMBIGUOUS,broad.severity,broad.broadClass,broad.broadPatternStrengthPct,broad.broadSupportSensors,broad.firstSeenMs,now,Math.max(0,now-broad.firstSeenMs),null,"",Double.NaN,Double.NaN,Double.NaN,false,accepted,why,why);}
    private static Snapshot clearSnapshot(long now){return new Snapshot(Phase.CLEAR,Severity.INFO,BroadClass.NONE,Double.NaN,0,0,now,0,null,"",Double.NaN,Double.NaN,Double.NaN,false,false,"","clear");}
    private static final class Pattern{final int support;final double score,meanDeviation;Pattern(int n,double s,double m){support=n;score=s;meanDeviation=m;}}
    private static Pattern pattern(Map<String,Double> d,String[] sensors,double[] weights){int n=0;double weighted=0,w=0,sum=0;for(int i=0;i<sensors.length;i++){Double x=d.get(sensors[i]);if(x==null||!Double.isFinite(x)||x<0.045)continue;n++;weighted+=weights[i]*x;w+=weights[i];sum+=x;}return new Pattern(n,w>0?weighted/w:0,n>0?sum/n:0);}
    private static double finiteClamp(double x,double lo,double hi){return Double.isFinite(x)?Math.max(lo,Math.min(hi,x)):Double.NaN;}
    private static String fmt(double x){return Double.isFinite(x)?String.format(Locale.US,"%.3f",x):"—";}
}
