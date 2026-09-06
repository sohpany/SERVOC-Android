package com.servoc.app.measurement;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;

/**
 * Converts the existing clean-air-baseline MQ response warnings into one robot alert level.
 * This is deliberately an operational response alarm, not a quantitative ppm/IDLH decision.
 * Escalation is immediate; de-escalation is held briefly to prevent audible chatter.
 */
public final class GasAlertCoordinator {
    public static final int NONE=0,WARNING=1,DANGER=2;
    private static final long DOWNGRADE_HOLD_MS=3000;
    private int state=NONE;
    private long downgradeSince=0;
    private double lastMaxDelta=Double.NaN;

    public int evaluate(JSONObject snapshot,SensorHealthEngine health,int warmupSec,long nowMs){
        if(snapshot==null||health==null)return clear();
        JSONObject relay=snapshot.optJSONObject("relay");
        boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        long powerMs=relay==null?0:relay.optLong("power_ms",0);
        long required=Math.max(0,warmupSec)*1000L;
        if(!powered||powerMs<required)return clear();

        JSONObject mq=snapshot.optJSONObject("mq");
        if(mq==null)return clear();
        int requested=NONE;double maxDelta=Double.NaN;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            InstantMqWarningEngine.Result r=InstantMqWarningEngine.evaluate(hw.key,mq.optJSONObject(hw.key),health.snapshot(hw.key));
            if(Double.isFinite(r.delta))maxDelta=!Double.isFinite(maxDelta)?r.delta:Math.max(maxDelta,r.delta);
            if(r.level==InstantMqWarningEngine.Level.HIGH)requested=DANGER;
            else if(r.level==InstantMqWarningEngine.Level.ELEVATED&&requested<DANGER)requested=WARNING;
        }
        lastMaxDelta=maxDelta;
        if(requested>=state){state=requested;downgradeSince=0;return state;}
        if(downgradeSince==0){downgradeSince=nowMs;return state;}
        if(nowMs-downgradeSince>=DOWNGRADE_HOLD_MS){state=requested;downgradeSince=0;}
        return state;
    }

    public int level(){return state;}
    public double maxDelta(){return lastMaxDelta;}
    public int reset(){return clear();}
    private int clear(){state=NONE;downgradeSince=0;lastMaxDelta=Double.NaN;return state;}
}
