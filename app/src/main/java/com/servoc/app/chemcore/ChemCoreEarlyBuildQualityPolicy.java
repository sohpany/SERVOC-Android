package com.servoc.app.chemcore;

import com.servoc.app.measurement.SensorHealthEngine;

import java.util.Map;

/**
 * Minimal Stage-37 quality policy for the current exploratory build.
 *
 * Hard invalidation is intentionally narrow. Most health/environment issues are preserved as
 * diagnostic quality flags so experiments can still teach us about the array.
 */
public final class ChemCoreEarlyBuildQualityPolicy {
    public enum State { USABLE, USABLE_WITH_WARNINGS, INVALID }

    public static final class Result {
        public final State state;
        public final int healthyOrUsableChannels;
        public final int warningChannels;
        public final String note;
        Result(State state,int usable,int warnings,String note){this.state=state;this.healthyOrUsableChannels=usable;this.warningChannels=warnings;this.note=note==null?"":note;}
    }

    private ChemCoreEarlyBuildQualityPolicy() {}

    public static Result evaluate(Map<String,SensorHealthEngine.Snapshot> health){
        if(health==null||health.isEmpty())return new Result(State.INVALID,0,0,"no installed MQ health observations available");
        int usable=0,warnings=0;
        for(SensorHealthEngine.Snapshot s:health.values()){
            if(s==null)continue;
            switch(s.state){
                case HEALTHY: usable++; break;
                case UNKNOWN: usable++; warnings++; break;
                case UNSTABLE:
                case DRIFT_WARNING:
                case SATURATED:
                case STUCK:
                    usable++;warnings++;break;
                case MISSING:
                case INVALID:
                    warnings++;break;
            }
        }
        if(usable==0)return new Result(State.INVALID,0,warnings,"no usable MQ channel remains");
        if(warnings>0)return new Result(State.USABLE_WITH_WARNINGS,usable,warnings,"retain analysis but propagate sensor-health warnings/quality weights");
        return new Result(State.USABLE,usable,0,"installed MQ health channels are usable");
    }

    /** Missing T/RH/P, incomplete general warm-up or heater-specific evidence are diagnostics here; source-specific models decide their own eligibility. */
    public static boolean softDiagnosticByDefault(){return true;}
}
