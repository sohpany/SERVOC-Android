package com.servoc.app.chemical;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqManufacturerOperatingContractTest {
    @Test public void allInstalledMqSensorsHaveManufacturerOperatingMetadata(){
        int[] sensors={GasEngine.MQ2,GasEngine.MQ3,GasEngine.MQ4,GasEngine.MQ5,
                GasEngine.MQ7,GasEngine.MQ8,GasEngine.MQ9,GasEngine.MQ135};
        for(int sensor:sensors){
            MqManufacturerOperatingContract.Spec s=MqManufacturerOperatingContract.get(sensor);
            assertNotNull(s);
            assertEquals(20.0,s.standardTemperatureC,0.0);
            assertEquals(2.0,s.standardTemperatureToleranceC,0.0);
            assertEquals(55.0,s.standardHumidityPct,0.0);
            assertEquals(5.0,s.standardHumidityTolerancePct,0.0);
            assertEquals(18.0,s.minimumOxygenPct,0.0);
            assertEquals(48,s.minimumInitialPreheatHours);
        }
    }

    @Test public void mq7AndMq9RequireTwoLevelHeaterCycle(){
        for(int sensor:new int[]{GasEngine.MQ7,GasEngine.MQ9}){
            MqManufacturerOperatingContract.Spec s=MqManufacturerOperatingContract.get(sensor);
            assertTrue(s.heaterCycleRequired);
            assertEquals(5.0,s.highHeaterV,0.0);
            assertEquals(60,s.highHeaterSeconds);
            assertEquals(1.5,s.lowHeaterV,0.0);
            assertEquals(90,s.lowHeaterSeconds);
        }
    }

    @Test public void remainingInstalledMqSensorsUseContinuousFiveVoltHeaters(){
        for(int sensor:new int[]{GasEngine.MQ2,GasEngine.MQ3,GasEngine.MQ4,GasEngine.MQ5,GasEngine.MQ8,GasEngine.MQ135}){
            MqManufacturerOperatingContract.Spec s=MqManufacturerOperatingContract.get(sensor);
            assertFalse(s.heaterCycleRequired);
            assertEquals(5.0,s.nominalHeaterV,0.0);
        }
    }
}
