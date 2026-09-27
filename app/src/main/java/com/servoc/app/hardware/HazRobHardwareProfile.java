package com.servoc.app.hardware;

import com.servoc.app.chemical.GasEngine;

/** Single source of truth for the field-wired HazRob 0.10.2 / HR069 sensor/power/drive topology. */
public final class HazRobHardwareProfile {
    private HazRobHardwareProfile(){}

    public static final String ID="hazrob-0.10.2-hr069-1r8mq-l298n6-batt-ultra-ir-ldr-env2-v2";
    public static final String DRIVE_DRIVER="L298N";

    /** Logical LEFT maps to L298N channel B. */
    public static final int DRIVE_L_EN=17;
    public static final int DRIVE_L_IN1=19;
    public static final int DRIVE_L_IN2=32;

    /** Logical RIGHT maps to L298N channel A. */
    public static final int DRIVE_R_EN=16;
    public static final int DRIVE_R_IN1=18;
    public static final int DRIVE_R_IN2=27;

    public static final int MQ_GROUPS=1;
    public static final int RELAY_CHANNELS=1;
    public static final int INSTALLED_RELAY_MASK=0x01;
    public static final int MQ_RELAY_GPIO=25;

    /** Reserved alert signal. The measured ~8 ohm speaker is not installed and must not be driven directly. */
    public static final int ALERT_GPIO=26;
    public static final boolean SPEAKER_INSTALLED=false;
    public static final int LDR_GPIO=34;
    public static final boolean LDR_QUANTITATIVE_LUX=false;
    public static final int IR_GPIO=35;
    public static final int IR_GROUND_ACTIVE_LEVEL=0;
    public static final int SOUND_GPIO=36;
    public static final int BATTERY_ADC_GPIO=33;
    public static final int ULTRASONIC_TRIG_GPIO=23;
    public static final int ULTRASONIC_ECHO_GPIO=39;
    public static final boolean INTERNAL_TEMP_INSTALLED=false;

    /** Analytical I2C bus: ADS1115 #1/#2 only. */
    public static final int I2C_SDA_GPIO=21;
    public static final int I2C_SCL_GPIO=22;

    /** Dedicated environmental I2C bus: BME280/BMP280 only. */
    public static final int ENV_I2C_SDA_GPIO=13;
    public static final int ENV_I2C_SCL_GPIO=14;

    /** Future quantitative illuminance requires an explicit bus assignment; no lux sensor is installed in 0.10.2 / HR069. */
    public static final boolean LUX_SENSOR_INSTALLED=false;

    /** Software acquisition contract assumes both ADS1115 boards are supplied from the regulated 5 V rail. */
    public static final float ADS_SUPPLY_V=5.0f;

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
