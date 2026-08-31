package com.servoc.app.navigation;

import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lightweight directional aid for HazRob 0.6.
 * It does not claim absolute source localization. It correlates recent MQ response change
 * with the robot's commanded travel direction and returns a relative bearing hypothesis.
 */
public final class GasDirectionEstimator {
    public static final class Result {
        public final boolean valid;public final double angleRad,confidence,score;
        Result(boolean valid,double angleRad,double confidence,double score){this.valid=valid;this.angleRad=angleRad;this.confidence=confidence;this.score=score;}
        public static Result none(double score){return new Result(false,Double.NaN,0,score);}
    }

    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final long VOTE_WINDOW_MS=30_000L;
    private static final double TURN_RATE_RAD_S=1.35;
    private final double[] baseline=new double[KEYS.length];
    private final boolean[] baselineReady=new boolean[KEYS.length];
    private final Deque<Vote> votes=new ArrayDeque<>();
    private long lastMs=0;private double lastScore=Double.NaN,heading=0;

    public synchronized Result update(JSONObject snapshot,int left,int right,long nowMs){
        if(snapshot==null){return Result.none(Double.NaN);}double dt=lastMs>0?Math.max(0,Math.min(1.5,(nowMs-lastMs)/1000.0)):0;lastMs=nowMs;
        // Integrate only heading; no fake metric position is created.
        if(dt>0){double turn=(right-left)/200.0;heading=wrap(heading+turn*TURN_RATE_RAD_S*dt);}
        double score=gasScore(snapshot.optJSONObject("mq"));
        trim(nowMs);
        if(Double.isFinite(score)&&Double.isFinite(lastScore)&&dt>0){
            double linear=(left+right)/200.0;double speed=Math.abs(linear);double delta=score-lastScore;
            if(speed>=0.12&&Math.abs(delta)>=0.004){
                double travel=wrap(heading+(linear<0?Math.PI:0));
                double bearing=delta>0?travel:wrap(travel+Math.PI);
                double weight=Math.min(0.25,Math.abs(delta))*Math.min(1.0,speed);
                if(delta<0)weight*=0.55; // rising evidence is more informative than falling evidence.
                if(weight>0.001)votes.addLast(new Vote(nowMs,Math.cos(bearing)*weight,Math.sin(bearing)*weight,weight));
            }
        }
        lastScore=score;trim(nowMs);
        if(votes.size()<2)return Result.none(score);
        double x=0,y=0,sum=0;for(Vote v:votes){double age=Math.max(0,nowMs-v.at)/1000.0;double decay=Math.exp(-age/16.0);x+=v.x*decay;y+=v.y*decay;sum+=v.w*decay;}
        double mag=Math.hypot(x,y);if(sum<0.008||mag<1e-6)return Result.none(score);
        double coherence=Math.min(1,mag/sum);double strength=Math.min(1,sum/0.055);double confidence=coherence*strength;
        if(confidence<0.18)return Result.none(score);
        double global=Math.atan2(y,x);double relative=wrap(global-heading);
        return new Result(true,relative,confidence,score);
    }

    public synchronized void reset(){votes.clear();lastMs=0;lastScore=Double.NaN;heading=0;for(int i=0;i<baseline.length;i++){baseline[i]=0;baselineReady[i]=false;}}

    private double gasScore(JSONObject mq){
        if(mq==null)return Double.NaN;double sum=0;int n=0;
        for(int i=0;i<KEYS.length;i++){
            JSONObject r=mq.optJSONObject(KEYS[i]);if(r==null||!r.optBoolean("valid",r.optBoolean("ok",false)))continue;
            double ratio=r.optDouble("ratio",Double.NaN);if(!Double.isFinite(ratio)||ratio<=0)continue;
            if(!baselineReady[i]){baseline[i]=ratio;baselineReady[i]=true;continue;}
            double b=baseline[i];if(!Double.isFinite(b)||b<=0){baseline[i]=ratio;continue;}
            double response=Math.log(b/ratio); // MQ reducing-gas response normally lowers Rs/R0.
            if(response>0)sum+=Math.min(2.0,response);n++;
            // Very slow baseline adaptation limits long-term drift without following short exposure peaks.
            double a=response>0.08?0.0005:0.003;baseline[i]=b*(1-a)+ratio*a;
        }
        return n==0?Double.NaN:sum/n;
    }

    private void trim(long now){while(!votes.isEmpty()&&now-votes.peekFirst().at>VOTE_WINDOW_MS)votes.removeFirst();}
    private static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<-Math.PI)a+=2*Math.PI;return a;}
    private static final class Vote{final long at;final double x,y,w;Vote(long at,double x,double y,double w){this.at=at;this.x=x;this.y=y;this.w=w;}}
}
