package com.servoc.app.chemcore;

import com.servoc.app.hardware.HazRobHardwareProfile;

import org.junit.Test;

import static org.junit.Assert.*;

public class SamplingLayoutTest {

    @Test public void standardV2ContainsAllEightInstalledSensorsExactlyOnce() {
        SamplingLayout layout = SamplingLayout.hazRobStandardArrayV2();
        assertEquals(SamplingLayout.STANDARD_V2_LAYOUT_ID, layout.layoutId);
        assertEquals("hazrob-chemcore-chamber-v2", layout.samplingProfileId);
        assertEquals(SamplingLayout.PositionStatus.DESIGN_LOGICAL, layout.positionStatus);
        assertEquals(2, layout.rows);
        assertEquals(4, layout.columns);
        assertEquals(HazRobHardwareProfile.MQ.length, layout.slots.size());

        for (HazRobHardwareProfile.Channel hw : HazRobHardwareProfile.MQ) {
            SamplingLayout.Slot slot = layout.forSensor(hw.key);
            assertNotNull(hw.key, slot);
            assertEquals(hw.adsAddress, slot.adsAddress);
            assertEquals(hw.adcChannel, slot.adcChannel);
        }
    }

    @Test public void rowsPreserveAdsChannelOrder() {
        SamplingLayout layout = SamplingLayout.hazRobStandardArrayV2();
        String[] row0 = {"mq2", "mq4", "mq5", "mq8"};
        String[] row1 = {"mq3", "mq7", "mq9", "mq135"};
        for (int column = 0; column < 4; column++) {
            assertEquals(row0[column], layout.at(0, column).sensorKey);
            assertEquals(row1[column], layout.at(1, column).sensorKey);
        }
    }
}
