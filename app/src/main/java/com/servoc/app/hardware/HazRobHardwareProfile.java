package com.servoc.app.hardware;

import com.servoc.app.chemical.GasEngine;

/** Single source of truth for the field-wired HazRob 0.8 sensor/relay/ADS topology. */
public final class HazRobHardwareProfile {
    private HazRobHardwareProfile(){}

    public static final String ID="hazrob_2g8mq_v1";
    public static final int MQ_GROUPS=2;
    public static final int RELAY_CHANNELS=4;
    public static final int INSTALLED_RELAY_MASK=0x03; // R1/R2 installed, R3/R4 reserved.

    public static final class Channel {
        public final int sensorId;
        public final String key;
        public final int relay;
        public final int adsIndex;
        public final int adsAddress;
        public final int adcChannel;
        Channel(int sensorId,String key,int relay,int adsIndex,int adsAddress,int adcChannel){
            this.sensorId=sensorId;this.key=key;this.relay=relay;this.adsIndex=adsIndex;this.adsAddress=adsAddress;this.adcChannel=adcChannel;
        }
    }

    public static final Channel[] MQ={
            // Relay 1 -> ADS1115 #1 @ 0x48
            new Channel(GasEngine.MQ2,  "mq2",  1,0,0x48,0),
            new Channel(GasEngine.MQ4,  "mq4",  1,0,0x48,1),
            new Channel(GasEngine.MQ5,  "mq5",  1,0,0x48,2),
            new Channel(GasEngine.MQ8,  "mq8",  1,0,0x48,3),
            // Relay 2 -> ADS1115 #2 @ 0x49
            new Channel(GasEngine.MQ3,  "mq3",  2,1,0x49,0),
            new Channel(GasEngine.MQ7,  "mq7",  2,1,0x49,1),
            new Channel(GasEngine.MQ9,  "mq9",  2,1,0x49,2),
            new Channel(GasEngine.MQ135,"mq135",2,1,0x49,3)
    };

    public static Channel forKey(String key){
        if(key==null)return null;for(Channel c:MQ)if(c.key.equalsIgnoreCase(key))return c;return null;
    }

    public static Channel forSensor(int sensorId){
        for(Channel c:MQ)if(c.sensorId==sensorId)return c;return null;
    }

    public static int relayMaskForSensorMask(int sensorMask){
        int out=0;for(Channel c:MQ)if((sensorMask&(1<<c.sensorId))!=0)out|=1<<(c.relay-1);return out&INSTALLED_RELAY_MASK;
    }

    public static String groupLabel(int mask){
        mask&=INSTALLED_RELAY_MASK;
        if(mask==0)return "MQ OFF";
        if(mask==1)return "R1 / ADS1 / MQ2+MQ4+MQ5+MQ8";
        if(mask==2)return "R2 / ADS2 / MQ3+MQ7+MQ9+MQ135";
        return "R1+R2 / جميع حساسات MQ";
    }
}
