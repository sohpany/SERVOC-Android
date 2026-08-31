package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lightweight directional aid for HazRob 0.6.
 * It does not claim absolute source localization. It correlates recent target-specific
 * MQ response change with the robot's commanded travel direction and returns a relative
 * bearing hypothesis. The selected chemical determines which MQ channels participate.
 *
 * Direction confidence is an internal evidence-coherence score, not a calibrated
 * probability that the physical gas source lies at the reported bearing. Until wheel
 * odometry/IMU feedback is available, heading is dead-reckoned from drive commands.
 */
public final class GasDirectionEstimator {
    public static final class Result {
        public final boolean valid;
        public final double angleRad,confidence,score,coherence,evidenceStrength;
        public final int voteCount;
        Result(boolean valid,double angleRad,double confidence,double score,double coherence,double evidenceStrength,int voteCount){this.valid=valid;this.angleRad=angleRad;this.confidence=confidence;this.score=score;this.coherence=coherence;this.evidenceStrength=evidenceStrength;this.voteCount=voteCount;}
        public static Result none(double score){return new Result(false,Double.NaN,0,score,0,0,0);}
    }

    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final long VOTE_WINDOW_MS=30_000L;
    /** Engineering dead-reckoning placeholder until measured angular velocity is available. */
    private static final double TURN_RATE_RAD_S=1.35;
    private final double[] baseline=new double[KEYS.length];
    private final boolean[] baselineReady=new boolean[KEYS.length];
    private final Deque<Vote> votes=new ArrayDeque<>();
    private long lastMs=0;private double lastScore=Double.NaN,heading=0;private int targetChemical=Integer.MIN_VALUE;

    /** Aggregate mode retained for compatibility; new UI should use the target-specific overload. */
    public synchronized Result update(JSONObject snapshot,int left,int right,long nowMs){return update(snapshot,left,right,nowMs,-1);}

    public synchronized Result update(JSONObject snapshot,int left,int right,long nowMs,int chemical){
        if(chemical!=targetChemical){resetState();targetChemical=chemical;}
        if(snapshot==null)return Result.none(Double.NaN);
        double dt=lastMs>0?Math.max(0,Math.min(1.5,(nowMs-lastMs)/1000.0)):0;lastMs=nowMs;
        // Joystick convention: positive X => left command > right command => turn right.
        if(dt>0){double turn=(left-right)/200.0;heading=wrap(heading+turn*TURN_RATE_RAD_S*dt);}
        double score=gasScore(snapshot.optJSONObject("mq"),chemical);
        trim(nowMs);
        if(Double.isFinite(score)&&Double.isFinite(lastScore)&&dt>0){
            double linear=(left+right)/200.0,speed=Math.abs(linear),delta=score-lastScore;
            if(speed>=0.12&&Math.abs(delta)>=0.004){
                double travel=wrap(heading+(linear<0?Math.PI:0));
                double bearing=delta>0?travel:wrap(travel+Math.PI);
                double weight=Math.min(0.25,Math.abs(delta))*Math.min(1.0,speed);
                if(delta<0)weight*=0.55;
                if(weight>0.001)votes.addLast(new Vote(nowMs,Math.cos(bearing)*weight,Math.sin(bearing)*weight,weight));
            }
        }
        lastScore=score;trim(nowMs);
        if(votes.size()<2)return Result.none(score);
        double x=0,y=0,sum=0;for(Vote v:votes){double age=Math.max(0,nowMs-v.at)/1000.0,decay=Math.exp(-age/16.0);x+=v.x*decay;y+=v.y*decay;sum+=v.w*decay;}
        double mag=Math.hypot(x,y);if(sum<0.008||mag<1e-6)return Result.none(score);
        double coherence=Math.min(1,mag/sum),strength=Math.min(1,sum/0.055),confidence=coherence*strength;
        if(confidence<0.18)return new Result(false,Double.NaN,confidence,score,coherence,strength,votes.size());
        double global=Math.atan2(y,x),relative=wrap(global-heading);
        return new Result(true,relative,confidence,score,coherence,strength,votes.size());
    }

    public synchronized void reset(){targetChemical=Integer.MIN_VALUE;resetState();}

    private void resetState(){votes.clear();lastMs=0;lastScore=Double.NaN;heading=0;for(int i=0;i<baseline.length;i++){baseline[i]=0;baselineReady[i]=false;}}

    private double gasScore(JSONObject mq,int chemical){
        if(mq==null)return Double.NaN;
        int mask=chemical<0?MqSensorAffinity.ALL:MqSensorAffinity.forChemical(chemical).mask;
        if(mask==0)return Double.NaN;
        double sum=0,weightSum=0;
        for(int i=0;i<KEYS.length;i++){
            if((mask&(1<<i))==0)continue;
            JSONObject r=mq.optJSONObject(KEYS[i]);if(r==null||!r.optBoolean("valid",r.optBoolean("ok",false)))continue;
            double ratio=r.optDouble("ratio",Double.NaN);if(!Double.isFinite(ratio)||ratio<=0)continue;
            if(!baselineReady[i]){baseline[i]=ratio;baselineReady[i]=true;continue;}
            double b=baseline[i];if(!Double.isFinite(b)||b<=0){baseline[i]=ratio;continue;}
            double response=Math.log(b/ratio);
            // Each target-relevant channel contributes evidence; the mask is chemical-specific.
            if(response>0){sum+=Math.min(2.0,response);weightSum+=1.0;}
            else weightSum+=0.35;
            // Very slow adaptive baseline reduces long-term drift without following a rapid plume rise.
            double a=response>0.08?0.0005:0.003;baseline[i]=b*(1-a)+ratio*a;
        }
        return weightSum==0?Double.NaN:sum/weightSum;
    }

    private void trim(long now){while(!votes.isEmpty()&&now-votes.peekFirst().at>VOTE_WINDOW_MS)votes.removeFirst();}
    private static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<-Math.PI)a+=2*Math.PI;return a;}
    private static final class Vote{final long at;final double x,y,w;Vote(long at,double x,double y,double w){this.at=at;this.x=x;this.y=y;this.w=w;}}
}
