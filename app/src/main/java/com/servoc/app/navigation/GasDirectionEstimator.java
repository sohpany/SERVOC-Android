package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Arrow-only directional aid for HazRob 0.10.2 / HR069.
 *
 * The estimator never sends drive, speed, relay or power commands. It only correlates
 * target-relevant MQ response changes with operator-commanded translation and returns a
 * relative bearing hypothesis for the video overlay. Until measured IMU/odometry exists,
 * heading is dead-reckoned from operator drive commands and must not be presented as
 * absolute source localization.
 */
public final class GasDirectionEstimator {
    public static final String POLICY_VERSION="gas-direction-assist/2-arrow-only";
    public static final long VOTE_WINDOW_MS=30_000L;
    public static final long MAX_TELEMETRY_GAP_MS=2_000L;
    public static final double MIN_TRANSLATION=0.12;
    public static final double MIN_SCORE_DELTA=0.004;
    public static final double MIN_DIRECTION_CONFIDENCE=0.18;

    public static final class Result {
        public final boolean valid;
        public final double angleRad,confidence,score,coherence,evidenceStrength;
        public final int voteCount,supportSensors,targetChemical;
        public final String reason;
        Result(boolean valid,double angleRad,double confidence,double score,double coherence,double evidenceStrength,
               int voteCount,int supportSensors,int targetChemical,String reason){
            this.valid=valid;this.angleRad=angleRad;this.confidence=confidence;this.score=score;
            this.coherence=coherence;this.evidenceStrength=evidenceStrength;this.voteCount=voteCount;
            this.supportSensors=supportSensors;this.targetChemical=targetChemical;this.reason=reason==null?"":reason;
        }
        public static Result none(double score,int target,int support,String reason){
            return new Result(false,Double.NaN,0,score,0,0,0,support,target,reason);
        }
    }

    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    /** Engineering dead-reckoning placeholder until measured angular velocity is available. */
    private static final double TURN_RATE_RAD_S=1.35;
    private final double[] baseline=new double[KEYS.length];
    private final boolean[] baselineReady=new boolean[KEYS.length];
    private final Deque<Vote> votes=new ArrayDeque<>();
    private long lastMs=0,lastSampleStamp=Long.MIN_VALUE,freshAt=0;private double lastScore=Double.NaN,heading=0;private int targetChemical=Integer.MIN_VALUE;private Result lastValidResult=null;

    /** Compatibility aggregate mode. It is still arrow-only and never has control authority. */
    public synchronized Result update(JSONObject snapshot,int left,int right,long nowMs){return update(snapshot,left,right,nowMs,-1);}

    public synchronized Result update(JSONObject snapshot,int left,int right,long nowMs,int chemical){
        if(chemical!=targetChemical){resetState();targetChemical=chemical;}
        if(chemical>=0&&MqSensorAffinity.forChemical(chemical).mask==0){resetState();targetChemical=chemical;return Result.none(Double.NaN,chemical,0,"TARGET_UNSUPPORTED_BY_CURRENT_ARRAY");}
        if(snapshot==null){resetTemporal();return Result.none(Double.NaN,chemical,0,"TELEMETRY_UNAVAILABLE");}

        long now=Math.max(0,nowMs);
        if(lastMs>0&&now-lastMs>MAX_TELEMETRY_GAP_MS)resetTemporal();
        long sampleStamp=sampleStamp(snapshot.optJSONObject("mq"),chemical);
        if(sampleStamp<=0)return Result.none(Double.NaN,chemical,0,"TARGET_SAMPLE_TIMESTAMP_UNAVAILABLE");
        if(sampleStamp==lastSampleStamp){
            if(freshAt>0&&now-freshAt<=450&&lastValidResult!=null)return lastValidResult;
            if(freshAt>0&&now-freshAt>750){resetTemporal();return Result.none(Double.NaN,chemical,0,"TARGET_SAMPLE_STALE");}
            return Result.none(lastScore,chemical,0,"WAITING_FOR_FRESH_TARGET_SAMPLE");
        }
        lastSampleStamp=sampleStamp;freshAt=now;
        double dt=lastMs>0?Math.max(0,Math.min(1.5,(now-lastMs)/1000.0)):0;lastMs=now;

        // Joystick convention: positive X => left command > right command => turn right.
        // Rotation updates the dead-reckoned heading but never creates a direction vote by itself.
        if(dt>0){double turn=(left-right)/200.0;heading=wrap(heading+turn*TURN_RATE_RAD_S*dt);}

        Score score=gasScore(snapshot.optJSONObject("mq"),chemical);
        trim(now);
        if(Double.isFinite(score.value)&&Double.isFinite(lastScore)&&dt>0){
            double linear=(left+right)/200.0,speed=Math.abs(linear),delta=score.value-lastScore;
            if(speed>=MIN_TRANSLATION&&Math.abs(delta)>=MIN_SCORE_DELTA){
                double travel=wrap(heading+(linear<0?Math.PI:0));
                double bearing=delta>0?travel:wrap(travel+Math.PI);
                double weight=Math.min(0.25,Math.abs(delta))*Math.min(1.0,speed);
                if(delta<0)weight*=0.55;
                if(weight>0.001)votes.addLast(new Vote(now,Math.cos(bearing)*weight,Math.sin(bearing)*weight,weight));
            }
        }
        lastScore=score.value;trim(now);

        if(!Double.isFinite(score.value))return Result.none(Double.NaN,chemical,score.supportSensors,score.reason);
        if(votes.size()<2)return Result.none(score.value,chemical,score.supportSensors,"INSUFFICIENT_TRANSLATIONAL_EVIDENCE");

        double x=0,y=0,sum=0;
        for(Vote v:votes){double age=Math.max(0,now-v.at)/1000.0,decay=Math.exp(-age/16.0);x+=v.x*decay;y+=v.y*decay;sum+=v.w*decay;}
        double mag=Math.hypot(x,y);
        if(sum<0.008||mag<1e-6)return Result.none(score.value,chemical,score.supportSensors,"INSUFFICIENT_DIRECTION_STRENGTH");
        double coherence=Math.min(1,mag/sum),strength=Math.min(1,sum/0.055),confidence=coherence*strength;
        if(confidence<MIN_DIRECTION_CONFIDENCE)
            return new Result(false,Double.NaN,confidence,score.value,coherence,strength,votes.size(),score.supportSensors,chemical,"LOW_DIRECTION_COHERENCE");
        double global=Math.atan2(y,x),relative=wrap(global-heading);
        lastValidResult=new Result(true,relative,confidence,score.value,coherence,strength,votes.size(),score.supportSensors,chemical,"DIRECTION_HYPOTHESIS");
        return lastValidResult;
    }

    public synchronized void reset(){targetChemical=Integer.MIN_VALUE;resetState();}

    private void resetState(){resetTemporal();for(int i=0;i<baseline.length;i++){baseline[i]=0;baselineReady[i]=false;}}
    private void resetTemporal(){votes.clear();lastMs=0;lastScore=Double.NaN;heading=0;lastSampleStamp=Long.MIN_VALUE;freshAt=0;lastValidResult=null;}

    /** The newest target-relevant MCU sample, not the age of an Android redraw. */
    private static long sampleStamp(JSONObject mq,int chemical){
        if(mq==null)return -1;
        int mask=chemical<0?MqSensorAffinity.ALL:MqSensorAffinity.forChemical(chemical).mask;
        long latest=-1;
        for(int i=0;i<KEYS.length;i++)if((mask&(1<<i))!=0){
            JSONObject r=mq.optJSONObject(KEYS[i]);
            if(r==null||!r.optBoolean("valid",r.optBoolean("ok",false)))continue;
            long t=r.optLong("sample_us",0);
            if(t>latest)latest=t;
        }
        return latest;
    }

    private Score gasScore(JSONObject mq,int chemical){
        if(mq==null)return new Score(Double.NaN,0,"MQ_UNAVAILABLE");
        int mask=chemical<0?MqSensorAffinity.ALL:MqSensorAffinity.forChemical(chemical).mask;
        if(mask==0)return new Score(Double.NaN,0,"TARGET_UNSUPPORTED_BY_CURRENT_ARRAY");
        double sum=0,weightSum=0;int support=0;
        for(int i=0;i<KEYS.length;i++){
            if((mask&(1<<i))==0)continue;
            JSONObject r=mq.optJSONObject(KEYS[i]);if(r==null||!r.optBoolean("valid",r.optBoolean("ok",false)))continue;
            JSONObject quality=r.optJSONObject("quality");
            if(quality!=null){
                if(!quality.optBoolean("valid",true))continue;
                String status=quality.optString("status","VALID");if(!"VALID".equals(status))continue;
                if(quality.has("manufacturer_heater_protocol_valid")&&!quality.optBoolean("manufacturer_heater_protocol_valid",false))continue;
            }
            double ratio=r.optDouble("ratio",Double.NaN);if(!Double.isFinite(ratio)||ratio<=0)continue;
            support++;
            if(!baselineReady[i]){baseline[i]=ratio;baselineReady[i]=true;continue;}
            double b=baseline[i];if(!Double.isFinite(b)||b<=0){baseline[i]=ratio;continue;}
            double response=Math.log(b/ratio);
            if(response>0){sum+=Math.min(2.0,response);weightSum+=1.0;}else weightSum+=0.35;
            // A slow baseline prevents long-term drift from dominating while retaining plume transients.
            double a=response>0.08?0.0005:0.003;baseline[i]=b*(1-a)+ratio*a;
        }
        if(support==0)return new Score(Double.NaN,0,"NO_VALID_TARGET_CHANNELS");
        return new Score(weightSum==0?Double.NaN:sum/weightSum,support,weightSum==0?"INSUFFICIENT_TARGET_SIGNAL":"OK");
    }

    private void trim(long now){while(!votes.isEmpty()&&now-votes.peekFirst().at>VOTE_WINDOW_MS)votes.removeFirst();}
    private static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<-Math.PI)a+=2*Math.PI;return a;}
    private static final class Vote{final long at;final double x,y,w;Vote(long at,double x,double y,double w){this.at=at;this.x=x;this.y=y;this.w=w;}}
    private static final class Score{final double value;final int supportSensors;final String reason;Score(double value,int supportSensors,String reason){this.value=value;this.supportSensors=supportSensors;this.reason=reason;}}
}
