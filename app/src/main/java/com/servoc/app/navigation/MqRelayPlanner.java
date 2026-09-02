package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.hardware.HazRobHardwareProfile;

/**
 * HazRob 0.8 MQ power recommendation layer.
 *
 * Physical profile:
 * R1 / ADS1 0x48 = MQ2, MQ4, MQ5, MQ8
 * R2 / ADS2 0x49 = MQ3, MQ7, MQ9, MQ135
 * R3/R4 are reserved and are never recommended for MQ sensing.
 *
 * This class ONLY recommends useful installed groups. It never switches power;
 * actual relay power remains under explicit operator control in HazRob 0.8.
 */
public final class MqRelayPlanner {
    private MqRelayPlanner(){}

    public static final int R1=1, R2=2, R3=4, R4=8;
    public static final int INSTALLED_MASK=HazRobHardwareProfile.INSTALLED_RELAY_MASK;
    public static final int MAX_ACTIVE_GROUPS=2;

    public static final int GROUP1_SENSORS=
            MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5|MqSensorAffinity.B_MQ8;
    public static final int GROUP2_SENSORS=
            MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9|MqSensorAffinity.B_MQ135;

    // Compatibility aliases retained for code written before the v0.8 terminology cleanup.
    public static final int ROW1_SENSORS=GROUP1_SENSORS;
    public static final int ROW2_SENSORS=GROUP2_SENSORS;
    public static final int ROW3_SENSORS=0;
    public static final int ROW4_SENSORS=0;
    public static final int MAX_ACTIVE_ROWS=MAX_ACTIVE_GROUPS;

    /** Returns the installed group(s) containing useful channels for this chemical. */
    public static int forChemical(int chemical){
        MqSensorAffinity.Plan p=MqSensorAffinity.forChemical(chemical);
        return p.mask==0?0:fromSensorMask(p.mask);
    }

    /** Maps any MQ sensor mask to the installed R1/R2 groups only. */
    public static int fromSensorMask(int sensors){
        return HazRobHardwareProfile.relayMaskForSensorMask(sensors)&INSTALLED_MASK;
    }

    /**
     * Electrical-policy guard for automatic/recommended masks.
     * R1+R2 is representable, but actual simultaneous use remains a manual operator decision.
     */
    public static boolean withinPowerBudget(int relayMask){
        return relayMask>=0 && (relayMask&~INSTALLED_MASK)==0 &&
                Integer.bitCount(relayMask)<=MAX_ACTIVE_GROUPS;
    }

    public static String rowsLabel(int mask){
        return HazRobHardwareProfile.groupLabel(mask&INSTALLED_MASK);
    }
}
