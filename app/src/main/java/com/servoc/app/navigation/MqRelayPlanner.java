package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.hardware.HazRobHardwareProfile;

/** Recommends the single installed MQ power relay when any useful MQ channel is required. */
public final class MqRelayPlanner {
    private MqRelayPlanner(){}

    public static final int OFF=0, ON=1;
    public static final int INSTALLED_MASK=HazRobHardwareProfile.INSTALLED_RELAY_MASK;

    public static int forChemical(int chemical){
        MqSensorAffinity.Plan p=MqSensorAffinity.forChemical(chemical);
        return fromSensorMask(p.mask);
    }

    public static int fromSensorMask(int sensors){
        return HazRobHardwareProfile.relayMaskForSensorMask(sensors);
    }

    /** Compatibility guard: the only valid hardware states are 0=OFF and 1=ON. */
    public static boolean withinPowerBudget(int relayMask){return relayMask==OFF||relayMask==ON;}

    public static String rowsLabel(int mask){return HazRobHardwareProfile.groupLabel(mask);}
}
