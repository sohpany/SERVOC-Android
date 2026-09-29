package com.servoc.app.chemical;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.junit.Test;

import static org.junit.Assert.*;

public class MqConditioningProfileTest {
    @Test public void allInstalledMqSensorsMeetInitial48hPreheatRequirement(){
        assertEquals(48,MqConditioningProfile.COMPLETED_INITIAL_PREHEAT_HOURS);
        assertTrue(MqConditioningProfile.INITIAL_PREHEAT_RECORDED);
        assertTrue(MqConditioningProfile.allInstalledSensorsConditioned());
        for(HazRobHardwareProfile.Channel c:HazRobHardwareProfile.MQ){
            assertTrue("initial preheat not satisfied for "+c.key,MqConditioningProfile.initialPreheatSatisfied(c.sensorId));
            assertTrue("sensor not acquisition-ready after conditioning: "+c.key,MqConditioningProfile.acquisitionReady(c.sensorId));
        }
    }

    @Test public void burnInDoesNotFakeMq7Mq9HeaterCycleValidity(){
        assertTrue(MqConditioningProfile.initialPreheatSatisfied(GasEngine.MQ7));
        assertTrue(MqConditioningProfile.initialPreheatSatisfied(GasEngine.MQ9));
        assertFalse(MqConditioningProfile.manufacturerReferenceProtocolReady(GasEngine.MQ7));
        assertFalse(MqConditioningProfile.manufacturerReferenceProtocolReady(GasEngine.MQ9));
        assertTrue(MqConditioningProfile.manufacturerReferenceProtocolReady(GasEngine.MQ2));
        assertTrue(MqConditioningProfile.manufacturerReferenceProtocolReady(GasEngine.MQ135));
    }
}
