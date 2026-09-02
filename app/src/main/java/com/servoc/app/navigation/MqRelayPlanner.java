package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;

/**
 * Maps the installed MQ array to the four physical relay rows used by HazRob 0.7.1.
 * At most two rows may be powered at once because the current 5 V / 4 A supply is a
 * temporary shared source for the servos, relays, electronics and MQ heaters.
 */
public final class MqRelayPlanner {
    private MqRelayPlanner(){}

    public static final int R1=1, R2=2, R3=4, R4=8;
    public static final int MAX_ACTIVE_ROWS=2;

    // Physical rows: two MQ sensors per relay.
    public static final int ROW1_SENSORS=MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5;
    public static final int ROW2_SENSORS=MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9;
    public static final int ROW3_SENSORS=MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ8;
    public static final int ROW4_SENSORS=MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ135;

    public static int forChemical(int chemical){
        MqSensorAffinity.Plan p=MqSensorAffinity.forChemical(chemical);
        if(p.mask==0)return 0;

        // Preserve the strongest installed channels for the principal direct-library analytes.
        switch(chemical){
            case 20: return R2|R4; // CO: MQ7/MQ9 + MQ135 support
            case 21: return R4;    // ammonia: MQ135
            case 23: return R4;    // CO2 operational MQ135 channel
            case 34: return R4;    // benzene/aromatic evidence
            case 75: return R3|R1; // H2: MQ8/MQ2 + MQ5 support
            case 76: return R1|R2; // methane: MQ4/MQ5 + MQ9
            case 78: return R1|R3; // LPG/propane family: MQ5 + MQ2/MQ8
            case 84: return R4|R1; // ethanol/alcohol: MQ3 + MQ5 support
            case 95: return R4;    // toluene/aromatic: MQ135/MQ3
            case 103:return R4|R3; // alcohol/VOC fingerprint
            case 110:return R4;    // acetone: MQ135/MQ3
            case 135:return R1|R2; // combustible mixture family
            case 139:return R3|R4; // smoke: MQ2 + MQ135
            default: break;
        }

        String c=p.className==null?"":p.className;
        if("combustible-hydrocarbon".equals(c))return R1|R2;
        if("alcohol/VOC".equals(c))return R4|R3;
        if("aromatic-VOC".equals(c))return R4|R3;
        if("oxygenated-VOC".equals(c))return R4|R3;
        if("sulfur/reducing".equals(c))return R4|R3;
        if("nitrogen-containing".equals(c))return R4;
        if("halogenated-organic".equals(c))return R4|R3;
        if("aggregate-air-quality".equals(c))return R3|R4;
        if("exploratory-MOS".equals(c))return R4|R3;

        return fromSensorMask(p.mask);
    }

    /** Generic fallback: choose the two rows containing the largest number of requested channels. */
    public static int fromSensorMask(int sensors){
        int[] rowSensors={ROW1_SENSORS,ROW2_SENSORS,ROW3_SENSORS,ROW4_SENSORS};
        int first=-1,second=-1,firstScore=0,secondScore=0;
        for(int i=0;i<rowSensors.length;i++){
            int score=Integer.bitCount(sensors&rowSensors[i]);
            if(score>firstScore){second=first;secondScore=firstScore;first=i;firstScore=score;}
            else if(score>secondScore){second=i;secondScore=score;}
        }
        int mask=firstScore>0?1<<first:0;
        if(secondScore>0)mask|=1<<second;
        return mask;
    }

    public static boolean withinPowerBudget(int relayMask){
        return relayMask>=0&&relayMask<=15&&Integer.bitCount(relayMask)<=MAX_ACTIVE_ROWS;
    }

    public static String rowsLabel(int mask){
        StringBuilder s=new StringBuilder();
        for(int i=0;i<4;i++)if((mask&(1<<i))!=0){if(s.length()>0)s.append('+');s.append('R').append(i+1);}
        return s.length()==0?"MQ OFF":s.toString();
    }
}
