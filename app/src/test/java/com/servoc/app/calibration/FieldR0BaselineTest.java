package com.servoc.app.calibration;

import static org.junit.Assert.*;
import org.junit.Test;

public class FieldR0BaselineTest {
    @Test public void acceptedCampaignMetadataIsStable(){
        assertEquals("FIELD-R0-3SESSION-20260913",FieldR0Baseline.SESSION);
        assertEquals(3,FieldR0Baseline.SESSION_COUNT);
        assertEquals(216,FieldR0Baseline.SAMPLE_COUNT);
        assertEquals(30.0,FieldR0Baseline.WINDOW_SECONDS,1e-12);
        assertFalse(FieldR0Baseline.REFERENCE_GAS_VALIDATED);
        assertFalse(FieldR0Baseline.ENVIRONMENT_COMPENSATED);
    }

    @Test public void allEightChannelsHaveAcceptedDeviceBaselines(){
        String[] keys={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
        for(String key:keys){assertTrue(key,Double.isFinite(FieldR0Baseline.value(key)));assertTrue(key,FieldR0Baseline.value(key)>0);}
        assertEquals(91.7340228444,FieldR0Baseline.value("mq2"),1e-9);
        assertEquals(20.6860315768,FieldR0Baseline.value("mq3"),1e-9);
        assertEquals(65.2695876771,FieldR0Baseline.value("mq4"),1e-9);
        assertEquals(251.9647223909,FieldR0Baseline.value("mq5"),1e-9);
        assertEquals(160.6013515297,FieldR0Baseline.value("mq7"),1e-9);
        assertEquals(54.8886824742,FieldR0Baseline.value("mq8"),1e-9);
        assertEquals(69.9900854903,FieldR0Baseline.value("mq9"),1e-9);
        assertEquals(213.2390474819,FieldR0Baseline.value("mq135"),1e-9);
    }
}
