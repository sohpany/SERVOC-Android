package com.servoc.app.navigation;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.MqSensorAffinity;
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

    @Test public void directOperationalPlansUseExpectedInstalledGroups(){
        assertEquals(MqRelayPlanner.R2,MqRelayPlanner.forChemical(20)); // CO -> MQ3/MQ7/MQ9/MQ135 group
        assertEquals(MqRelayPlanner.R1,MqRelayPlanner.forChemical(75)); // H2 -> MQ2/MQ4/MQ5/MQ8 group
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(76)); // methane spans both
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(139)); // smoke spans both
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
        assertEquals("R1+R2",MqRelayPlanner.rowsLabel(MqRelayPlanner.R1|MqRelayPlanner.R2));
    }
}
