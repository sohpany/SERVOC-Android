package com.servoc.app.navigation;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.MqSensorAffinity;
import org.junit.Test;
import static org.junit.Assert.*;

public class MqRelayPlannerTest {
    @Test public void physicalRowsMatchInstalledSensors(){
        assertEquals(MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ5,MqRelayPlanner.ROW1_SENSORS);
        assertEquals(MqSensorAffinity.B_MQ7|MqSensorAffinity.B_MQ9,MqRelayPlanner.ROW2_SENSORS);
        assertEquals(MqSensorAffinity.B_MQ2|MqSensorAffinity.B_MQ8,MqRelayPlanner.ROW3_SENSORS);
        assertEquals(MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ135,MqRelayPlanner.ROW4_SENSORS);
    }

    @Test public void directOperationalPlansUseExpectedRows(){
        assertEquals(MqRelayPlanner.R2|MqRelayPlanner.R4,MqRelayPlanner.forChemical(20)); // CO
        assertEquals(MqRelayPlanner.R3|MqRelayPlanner.R1,MqRelayPlanner.forChemical(75)); // H2
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,MqRelayPlanner.forChemical(76)); // methane
        assertEquals(MqRelayPlanner.R3|MqRelayPlanner.R4,MqRelayPlanner.forChemical(139)); // smoke
    }

    @Test public void everyCataloguePlanRespectsTwoRowPowerBudget(){
        for(int i=0;i<ChemicalCatalog.COUNT;i++){
            int mask=MqRelayPlanner.forChemical(i);
            assertTrue("chemical "+i+" mask="+mask,MqRelayPlanner.withinPowerBudget(mask));
        }
    }

    @Test public void genericFallbackChoosesAtMostTwoRows(){
        assertEquals(MqRelayPlanner.R1|MqRelayPlanner.R2,
                MqRelayPlanner.fromSensorMask(MqRelayPlanner.ROW1_SENSORS|MqRelayPlanner.ROW2_SENSORS|MqRelayPlanner.ROW3_SENSORS));
        assertFalse(MqRelayPlanner.withinPowerBudget(0x07));
        assertTrue(MqRelayPlanner.withinPowerBudget(0x0C));
        assertEquals("R3+R4",MqRelayPlanner.rowsLabel(0x0C));
    }
}
