package com.servoc.app.navigation;

import com.servoc.app.chemical.MqSensorAffinity;
import org.junit.Test;
import static org.junit.Assert.*;

public class MqRelayPlannerTest {
    @Test public void mapsInstalledSensorsToTwoBanks(){
        assertEquals(1,MqRelayPlanner.fromSensorMask(MqSensorAffinity.B_MQ2));
        assertEquals(1,MqRelayPlanner.fromSensorMask(MqSensorAffinity.B_MQ4|MqSensorAffinity.B_MQ8));
        assertEquals(2,MqRelayPlanner.fromSensorMask(MqSensorAffinity.B_MQ3|MqSensorAffinity.B_MQ135));
        assertEquals(3,MqRelayPlanner.fromSensorMask(MqSensorAffinity.B_MQ5|MqSensorAffinity.B_MQ9));
        assertEquals(0,MqRelayPlanner.fromSensorMask(0));
    }
}
