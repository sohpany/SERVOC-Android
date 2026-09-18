package com.servoc.app.measurement;

import org.json.JSONObject;

/**
 * Maps the canonical app-side live MQ state to robot/phone alert levels.
 * Operational response only: no chemical identity, ppm, OEL, IDLH or LFL claim.
 */
public final class GasAlertCoordinator {
    public static final int NONE=0,WARNING=1,DANGER=2;
    private static final long DOWNGRADE_HOLD_MS=3000;
    private final LiveGasStateEngine live=new LiveGasStateEngine();
    private int state=NONE; private long downgradeSince; private double lastMaxDelta=Double.NaN;
    private LiveGasStateEngine.Snapshot lastSnapshot;

    public int evaluate(JSONObject snapshot,SensorHealthEngine health,int warmupSec,long nowMs){
        if(snapshot==null||health==null)return clear();
        JSONObject relay=snapshot.optJSONObject("relay");
        boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        long powerMs=relay==null?0:relay.optLong("power_ms",0), required=Math.max(0,warmupSec)*1000L;
        boolean warming=powered&&powerMs<required;
        lastSnapshot=live.evaluate(health.snapshots(),true,warming);
        lastMaxDelta=lastSnapshot.strongestAbsLogDelta;
        int requested=NONE;
        if(powered&&!warming){
            if(lastSnapshot.state==LiveGasStateEngine.State.RESPONSE_ALERT)requested=DANGER;
            else if(lastSnapshot.state==LiveGasStateEngine.State.RESPONSE_WATCH)requested=WARNING;
        }
        if(requested>=state){state=requested;downgradeSince=0;return state;}
        if(downgradeSince==0){downgradeSince=nowMs;return state;}
        if(nowMs-downgradeSince>=DOWNGRADE_HOLD_MS){state=requested;downgradeSince=0;}
        return state;
    }
    public int level(){return state;}
    public double maxDelta(){return lastMaxDelta;}
    public LiveGasStateEngine.Snapshot liveSnapshot(){return lastSnapshot;}
    public int reset(){return clear();}
    private int clear(){state=NONE;downgradeSince=0;lastMaxDelta=Double.NaN;lastSnapshot=null;return state;}
}
