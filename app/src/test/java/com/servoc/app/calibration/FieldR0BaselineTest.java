package com.servoc.app.calibration;

import static org.junit.Assert.*;
import org.junit.Test;

public class FieldR0BaselineTest {
    @Test public void acceptedSessionMetadataIsStable(){
        assertEquals("FIELD-1789255293867",FieldR0Baseline.SESSION);
        assertEquals(74,FieldR0Baseline.SAMPLE_COUNT);
        assertFalse(FieldR0Baseline.REFERENCE_GAS_VALIDATED);
        assertFalse(FieldR0Baseline.ENVIRONMENT_COMPENSATED);
    }

    @Test public void allEightChannelsHavePositiveDistinctDeviceBaselines(){
        String[] keys={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
        for(String key:keys){assertTrue(key,Double.isFinite(FieldR0Baseline.value(key)));assertTrue(key,FieldR0Baseline.value(key)>0);}
        assertEquals(79.3176036586,FieldR0Baseline.value("mq2"),1e-9);
        assertEquals(8.7149649220,FieldR0Baseline.value("mq3"),1e-9);
        assertEquals(36.1454126550,FieldR0Baseline.value("mq4"),1e-9);
        assertEquals(214.5398913808,FieldR0Baseline.value("mq5"),1e-9);
        assertEquals(119.2408579279,FieldR0Baseline.value("mq7"),1e-9);
        assertEquals(35.7836813368,FieldR0Baseline.value("mq8"),1e-9);
        assertEquals(32.3349994303,FieldR0Baseline.value("mq9"),1e-9);
        assertEquals(198.8868482045,FieldR0Baseline.value("mq135"),1e-9);
    }
}
