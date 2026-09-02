package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.hardware.HazRobHardwareProfile;

/**
 * HazRob 0.8 MQ power recommendation layer.
 * R1/R2 are the two installed four-sensor groups; R3/R4 are reserved.
 * This class recommends required groups only. Actual relay power is operator-controlled.
 */
public final class MqRelayPlanner {
    private MqRelayPlanner(){}

    public static final int R1=1, R2=2, R3=4, R4=8;
    public static final int INSTALLED_MASK=HazRobHardwareProfile.INSTALLED_RELAY_MASK;
    public static final int MAX_ACTIVE_ROWS=2;

    public static final int ROW1_SENSORS=
            MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5|MqSensorAffinity.B_MQ8;
    public static final int ROW2_SENSORS=
            MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9|MqSensorAffinity.B_MQ135;
    public static final int ROW3_SENSORS=0;
    public static final int ROW4_SENSORS=0;

    /** Returns the installed relay group(s) containing useful channels for this chemical. */
    public static int forChemical(int chemical){
        MqSensorAffinity.Plan p=MqSensorAffinity.forChemical(chemical);
        return p.mask==0?0:fromSensorMask(p.mask);
    }

    public static int fromSensorMask(int sensors){
        return HazRobHardwareProfile.relayMaskForSensorMask(sensors);
    }

    public static boolean withinPowerBudget(int relayMask){
        return relayMask>=0 && (relayMask&~INSTALLED_MASK)==0 && Integer.bitCount(relayMask)<=MAX_ACTIVE_ROWS;
    }

    public static String rowsLabel(int mask){
        return HazRobHardwareProfile.groupLabel(mask);
    }
}
