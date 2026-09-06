package com.servoc.app.hardware;

import com.servoc.app.chemical.GasEngine;

/** Single source of truth for the field-wired HazRob 0.9.3 sensor/power/ADS topology. */
public final class HazRobHardwareProfile {
    private HazRobHardwareProfile(){}

    public static final String ID="hazrob-0.9.3-1r8mq-alert-ldr-v1";
    public static final int MQ_GROUPS=1;
    public static final int RELAY_CHANNELS=1;
    public static final int INSTALLED_RELAY_MASK=0x01;
    public static final int MQ_RELAY_GPIO=25;
    public static final int ALERT_GPIO=26;
    public static final int LDR_GPIO=34;
    public static final int IR_GPIO=35;
    public static final int SOUND_GPIO=36;

    public static final class Channel {
        public final int sensorId;
        public final String key;
        /** All installed MQ sensors share relay 1 / GPIO25. */
        public final int relay;
        public final int adsIndex;
        public final int adsAddress;
        public final int adcChannel;
        /** Compatibility aliases for compact report/export code. */
        public final int ads;
        public final int channel;
        Channel(int sensorId,String key,int adsIndex,int adsAddress,int adcChannel){
            this.sensorId=sensorId;this.key=key;this.relay=1;this.adsIndex=adsIndex;this.adsAddress=adsAddress;this.adcChannel=adcChannel;
            this.ads=adsIndex+1;this.channel=adcChannel;
        }
    }

    public static final Channel[] MQ={
            new Channel(GasEngine.MQ2,  "mq2",  0,0x48,0),
            new Channel(GasEngine.MQ4,  "mq4",  0,0x48,1),
            new Channel(GasEngine.MQ5,  "mq5",  0,0x48,2),
            new Channel(GasEngine.MQ8,  "mq8",  0,0x48,3),
            new Channel(GasEngine.MQ3,  "mq3",  1,0x49,0),
            new Channel(GasEngine.MQ7,  "mq7",  1,0x49,1),
            new Channel(GasEngine.MQ9,  "mq9",  1,0x49,2),
            new Channel(GasEngine.MQ135,"mq135",1,0x49,3)
    };

    public static Channel forKey(String key){if(key==null)return null;for(Channel c:MQ)if(c.key.equalsIgnoreCase(key))return c;return null;}
    public static Channel forSensor(int sensorId){for(Channel c:MQ)if(c.sensorId==sensorId)return c;return null;}

    /** Any useful installed MQ channel requires the single MQ power relay. */
    public static int relayMaskForSensorMask(int sensorMask){
        if(sensorMask==0)return 0;
        for(Channel c:MQ)if((sensorMask&(1<<c.sensorId))!=0)return 1;
        return 0;
    }

    /** Kept as a compatibility API; mask is now strictly OFF/ON. */
    public static String groupLabel(int mask){return (mask&1)==0?"MQ Power OFF":"MQ Power ON — 8 sensors";}
}
