package com.servoc.app.measurement;

import com.servoc.app.chemcore.ChemCoreLiveSensingEngine;
import org.json.JSONObject;

/**
 * Maps the canonical app-side live MQ state to robot/phone alert levels and hosts the ChemCore
 * live analytical layer. Broad safety warning remains independent of exact chemical identity.
 */
public final class GasAlertCoordinator {
    public static final int NONE=0,WARNING=1,DANGER=2;
    private static final long DOWNGRADE_HOLD_MS=3000;
    private LiveGasStateEngine live=new LiveGasStateEngine();
    private ChemCoreLiveSensingEngine chemCoreLive=new ChemCoreLiveSensingEngine();
    private int state=NONE; private long downgradeSince; private double lastMaxDelta=Double.NaN;
    private LiveGasStateEngine.Snapshot lastSnapshot;

    /**
     * Called from the existing 4 Hz telemetry worker. ChemCore broad classification is cheap on
     * every sample; exact candidate refinement is throttled to about 1 Hz by the live engine.
     * The MainActivity drive command dispatcher runs independently in the worker pool.
     */
    public int evaluate(JSONObject snapshot,SensorHealthEngine health,int warmupSec,long nowMs){
        if(snapshot==null||health==null)return clear();
        JSONObject relay=snapshot.optJSONObject("relay");
        boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        long powerMs=relay==null?0:relay.optLong("power_ms",0), required=Math.max(0,warmupSec)*1000L;
        boolean warming=powered&&powerMs<required;
        lastSnapshot=live.evaluate(health.snapshots(),true,warming);
        lastMaxDelta=lastSnapshot.strongestAbsLogDelta;

        chemCoreLive.updateBroad(snapshot,health.snapshots(),lastSnapshot,nowMs);
        if(chemCoreLive.claimRefinement(nowMs))chemCoreLive.refineIdentity(snapshot,nowMs);
        try{snapshot.put("live_chemcore",chemCoreLive.snapshot().toJson());}catch(Exception ignored){}

        int requested=NONE;
        if(powered&&!warming){
            if(lastSnapshot.state==LiveGasStateEngine.State.RESPONSE_ALERT)requested=DANGER;
            else if(lastSnapshot.state==LiveGasStateEngine.State.RESPONSE_WATCH||lastSnapshot.state==LiveGasStateEngine.State.INSTANT_RESPONSE)requested=WARNING;
        }
        if(requested>=state){state=requested;downgradeSince=0;return state;}
        if(downgradeSince==0){downgradeSince=nowMs;return state;}
        if(nowMs-downgradeSince>=DOWNGRADE_HOLD_MS){state=requested;downgradeSince=0;}
        return state;
    }
    public int level(){return state;}
    public double maxDelta(){return lastMaxDelta;}
    public LiveGasStateEngine.Snapshot liveSnapshot(){return lastSnapshot;}
    public ChemCoreLiveSensingEngine.Snapshot chemCoreSnapshot(){return chemCoreLive.snapshot();}
    public void refreshChemCoreCandidates(){chemCoreLive.refreshCandidates();}
    public int reset(){return clear();}
    private int clear(){
        state=NONE;downgradeSince=0;lastMaxDelta=Double.NaN;lastSnapshot=null;
        // Reset persistence and the last named compound so reconnect cannot resurrect stale live state.
        live=new LiveGasStateEngine();chemCoreLive=new ChemCoreLiveSensingEngine();
        return state;
    }
}
