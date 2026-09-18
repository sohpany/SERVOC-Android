package com.servoc.app.measurement;

import java.util.Map;

/** App-side operational MQ response monitor; never identifies chemicals or produces quantitative safety claims. */
public final class LiveGasStateEngine {
    public enum State { UNAVAILABLE, WARMING_UP, DEGRADED, BASELINE_REQUIRED, NORMAL, INSTANT_RESPONSE, RESPONSE_WATCH, RESPONSE_ALERT }
    public static final class Snapshot {
        public final State state; public final int healthyChannels,usableChannels,respondingChannels,persistence; public final double strongestAbsLogDelta; public final String reason;
        Snapshot(State s,int h,int u,int r,int p,double d,String why){state=s;healthyChannels=h;usableChannels=u;respondingChannels=r;persistence=p;strongestAbsLogDelta=d;reason=why;}
        public boolean chemicalIdentityPermitted(){return false;}
        public boolean quantitativeSafetyInterpretationPermitted(){return false;}
    }
    private static final double WATCH_LOG10=0.045, ALERT_LOG10=0.080;
    private static final int WATCH_PERSISTENCE=2, ALERT_PERSISTENCE=3;
    private int persistence;
    public synchronized Snapshot evaluate(Map<String,SensorHealthEngine.Snapshot> health,boolean connected,boolean warmingUp){
        if(!connected){persistence=0;return out(State.UNAVAILABLE,0,0,0,0,Double.NaN,"telemetry_unavailable");}
        if(warmingUp){persistence=0;return out(State.WARMING_UP,0,0,0,0,Double.NaN,"sensor_warmup");}
        if(health==null||health.isEmpty()){persistence=0;return out(State.UNAVAILABLE,0,0,0,0,Double.NaN,"health_unavailable");}
        int healthy=0,usable=0,responding=0,baselines=0;double strongest=0;
        for(SensorHealthEngine.Snapshot s:health.values()){
            if(s==null)continue;
            boolean hardBad=s.state==SensorHealthEngine.State.MISSING||s.state==SensorHealthEngine.State.INVALID||s.state==SensorHealthEngine.State.SATURATED||s.state==SensorHealthEngine.State.STUCK;
            if(s.state==SensorHealthEngine.State.HEALTHY)healthy++;
            if(!hardBad&&Double.isFinite(s.lastRatio)&&s.lastRatio>0)usable++;
            if(Double.isFinite(s.baselineRatio)&&s.baselineRatio>0)baselines++;
            if(!hardBad&&Double.isFinite(s.lastRatio)&&s.lastRatio>0&&Double.isFinite(s.baselineRatio)&&s.baselineRatio>0){
                double d=Math.abs(Math.log10(s.lastRatio/s.baselineRatio));strongest=Math.max(strongest,d);if(d>=WATCH_LOG10)responding++;
            }
        }
        if(usable<4){persistence=0;return out(State.DEGRADED,healthy,usable,responding,0,strongest,"insufficient_usable_channels");}
        if(baselines<4){persistence=0;return out(State.BASELINE_REQUIRED,healthy,usable,responding,0,strongest,"clean_air_baseline_required");}
        boolean instant=responding>=1&&strongest>=ALERT_LOG10;\n        boolean watch=responding>=2&&strongest>=WATCH_LOG10,alert=responding>=2&&strongest>=ALERT_LOG10;
        if(watch)persistence=Math.min(99,persistence+1);else persistence=Math.max(0,persistence-1);
        if(alert&&persistence>=ALERT_PERSISTENCE)return out(State.RESPONSE_ALERT,healthy,usable,responding,persistence,strongest,"persistent_multisensor_response");
        if(watch&&persistence>=WATCH_PERSISTENCE)return out(State.RESPONSE_WATCH,healthy,usable,responding,persistence,strongest,"multisensor_response_watch");
        return out(State.NORMAL,healthy,usable,responding,persistence,strongest,"no_persistent_multisensor_response");
    }
    private static Snapshot out(State s,int h,int u,int r,int p,double d,String why){return new Snapshot(s,h,u,r,p,d,why);}
}
