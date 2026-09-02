package com.servoc.app.navigation;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.junit.Test;
import static org.junit.Assert.*;

public class MqRelayPlannerTest {
    @Test public void physicalGroupsMatchInstalledSensors(){
        assertEquals(MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5|MqSensorAffinity.B_MQ8,
                MqRelayPlanner.GROUP1_SENSORS);
        assertEquals(MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9|MqSensorAffinity.B_MQ135,
                MqRelayPlanner.GROUP2_SENSORS);
        assertEquals(0,MqRelayPlanner.ROW3_SENSORS);
        assertEquals(0,MqRelayPlanner.ROW4_SENSORS);
    }

    @Test public void everyCataloguePlanMatchesTheInstalledHardwareMapping(){
        for(int i=0;i<ChemicalCatalog.COUNT;i++){
            int sensorMask=MqSensorAffinity.forChemical(i).mask;
            int expected=HazRobHardwareProfile.relayMaskForSensorMask(sensorMask);
            int actual=MqRelayPlanner.forChemical(i);
            assertEquals("chemical "+i+" sensorMask="+sensorMask,expected,actual);
        }
    }

    @Test public void representativeCrossGroupPlansRemainCrossGroup(){
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(20));  // CO: primary + fingerprint channels span both groups
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(75));  // H2: primary + fingerprint channels span both groups
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(76));  // methane
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(139)); // smoke
    }

    @Test public void everyCatalogueRecommendationUsesOnlyInstalledGroups(){
        for(int i=0;i<ChemicalCatalog.COUNT;i++){
            int mask=MqRelayPlanner.forChemical(i);
            assertEquals("chemical "+i+" mask="+mask,0,mask&~MqRelayPlanner.INSTALLED_MASK);
            assertTrue("chemical "+i+" mask="+mask,MqRelayPlanner.withinPowerBudget(mask));
        }
    }

    @Test public void genericMappingNeverUsesReservedRelays(){
        int allSensors=MqRelayPlanner.GROUP1_SENSORS|MqRelayPlanner.GROUP2_SENSORS;
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.fromSensorMask(allSensors));
        assertTrue(MqRelayPlanner.withinPowerBudget(0));
        assertTrue(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.R1));
        assertTrue(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.R2));
        assertTrue(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.R1|MqRelayPlanner.R2));
        assertFalse(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.R1|MqRelayPlanner.R3));
        assertFalse(MqRelayPlanner.withinPowerBudget(MqRelayPlanner.R3|MqRelayPlanner.R4));
        assertEquals(HazRobHardwareProfile.groupLabel(MqRelayPlanner.R1|MqRelayPlanner.R2),
                MqRelayPlanner.rowsLabel(MqRelayPlanner.R1|MqRelayPlanner.R2));
    }
}
