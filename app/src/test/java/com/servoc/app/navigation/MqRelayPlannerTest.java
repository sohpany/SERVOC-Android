package com.servoc.app.navigation;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.junit.Test;
import static org.junit.Assert.*;

public class MqRelayPlannerTest {
    @Test public void hardwareProfileUsesOneRelayForAllEightMq(){
        assertEquals("hazrob-0.10-1r8mq-l298n6-batt-ultra-ldr-v1",HazRobHardwareProfile.ID);
        assertEquals(1,HazRobHardwareProfile.MQ_GROUPS);
        assertEquals(1,HazRobHardwareProfile.RELAY_CHANNELS);
        assertEquals(1,HazRobHardwareProfile.INSTALLED_RELAY_MASK);
        assertEquals(8,HazRobHardwareProfile.MQ.length);
        for(HazRobHardwareProfile.Channel c:HazRobHardwareProfile.MQ)assertEquals(1,c.relay);
    }

    @Test public void everyCataloguePlanMatchesInstalledPowerMapping(){
        for(int i=0;i<ChemicalCatalog.COUNT;i++){
            int sensorMask=MqSensorAffinity.forChemical(i).mask;
            int expected=HazRobHardwareProfile.relayMaskForSensorMask(sensorMask);
            int actual=MqRelayPlanner.forChemical(i);
            assertEquals("chemical "+i+" sensorMask="+sensorMask,expected,actual);
            assertTrue(actual==0||actual==1);
        }
    }

    @Test public void anyInstalledMqSensorRequestsPowerOn(){
        for(HazRobHardwareProfile.Channel c:HazRobHardwareProfile.MQ){
            assertEquals(1,HazRobHardwareProfile.relayMaskForSensorMask(1<<c.sensorId));
        }
        assertEquals(0,HazRobHardwareProfile.relayMaskForSensorMask(0));
    }

    @Test public void onlyOffAndOnAreValidStates(){
        assertTrue(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.OFF));
        assertTrue(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.ON));
        assertFalse(MqRelayPlanner.withinPowerBudget(2));
        assertFalse(MqRelayPlanner.withinPowerBudget(3));
        assertEquals("MQ Power OFF",MqRelayPlanner.rowsLabel(0));
        assertEquals("MQ Power ON — 8 sensors",MqRelayPlanner.rowsLabel(1));
    }
}
