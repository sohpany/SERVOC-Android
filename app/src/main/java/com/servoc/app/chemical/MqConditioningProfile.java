package com.servoc.app.chemical;

import com.servoc.app.hardware.HazRobHardwareProfile;

/**
 * Unit-specific MQ conditioning state for the current HazRob HR069 robot.
 *
 * The eight installed MQ sensors completed the manufacturer's minimum 48 h initial preheat.
 * This makes the array conditioned for normal acquisition. It does not replace the separate
 * cyclic heater protocol required by MQ-7B/MQ-9B when manufacturer response curves are used.
 */
public final class MqConditioningProfile {
    private MqConditioningProfile() {}

    public static final String RELEASE="0.10.2";
    public static final String HARDWARE_REVISION="HR069";
    public static final String CONDITIONING_CAMPAIGN="HR069-MQ-48H-PREHEAT-2026-09";
    public static final int COMPLETED_INITIAL_PREHEAT_HOURS=48;
    public static final boolean INITIAL_PREHEAT_RECORDED=true;

    /** True when the documented initial preheat meets the registered manufacturer minimum. */
    public static boolean initialPreheatSatisfied(int sensor){
        MqManufacturerOperatingContract.Spec spec=MqManufacturerOperatingContract.get(sensor);
        return INITIAL_PREHEAT_RECORDED&&spec!=null&&
                COMPLETED_INITIAL_PREHEAT_HOURS>=spec.minimumInitialPreheatHours;
    }

    /** All installed sensors are conditioned for raw/empirical acquisition after the 48 h preheat. */
    public static boolean acquisitionReady(int sensor){
        return HazRobHardwareProfile.forSensor(sensor)!=null&&initialPreheatSatisfied(sensor);
    }

    /**
     * Manufacturer-curve validity is stricter than acquisition readiness. MQ-7/MQ-9 still require
     * a physically valid high/low heater cycle; 48 h burn-in alone cannot satisfy that protocol.
     */
    public static boolean manufacturerReferenceProtocolReady(int sensor){
        return acquisitionReady(sensor)&&HazRobHardwareProfile.manufacturerHeaterProtocolValid(sensor);
    }

    public static boolean allInstalledSensorsConditioned(){
        for(HazRobHardwareProfile.Channel c:HazRobHardwareProfile.MQ)
            if(!acquisitionReady(c.sensorId))return false;
        return true;
    }
}
