package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;

/** Maps the eight installed MQ channels to the two powered sensor banks in HazRob 0.7. */
public final class MqRelayPlanner {
    private MqRelayPlanner(){}

    // Bank 1: MQ2, MQ4, MQ5, MQ8. Bank 2: MQ3, MQ7, MQ9, MQ135.
    public static final int BANK1_SENSORS = MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5|MqSensorAffinity.B_MQ8;
    public static final int BANK2_SENSORS = MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9|MqSensorAffinity.B_MQ135;

    public static int forChemical(int chemical){
        return fromSensorMask(MqSensorAffinity.forChemical(chemical).mask);
    }

    public static int fromSensorMask(int sensors){
        int relays=0;
        if((sensors&BANK1_SENSORS)!=0)relays|=1;
        if((sensors&BANK2_SENSORS)!=0)relays|=2;
        return relays;
    }
}
