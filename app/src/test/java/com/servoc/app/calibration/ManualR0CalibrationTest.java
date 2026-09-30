package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ManualR0CalibrationTest {
    @Test public void fieldFallbackCarriesCompleteTimeProvenanceWithoutQuantitativeValidation()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject q=new JSONObject();q.put("rs",FieldR0Baseline.value("mq2")*2.0);
        ManualR0Calibration.apply("mq2",q);
        assertEquals(FieldR0Baseline.SOURCE,q.getString("r0_source"));
        assertEquals(FieldR0Baseline.SESSION,q.getString("r0_session"));
        assertEquals(FieldR0Baseline.UTC,q.getString("r0_utc"));
        assertEquals(2.0,q.getDouble("ratio"),1e-9);
        assertTrue(q.getBoolean("r0_calibrated"));
        assertTrue(q.getBoolean("r0_provenance_complete"));
        assertTrue(q.getBoolean("r0_age_known"));
        assertTrue(q.getLong("r0_age_ms")>=0);
        assertFalse(q.getBoolean("r0_reference_validated"));
        assertFalse(q.getBoolean("r0_environment_compensated"));
    }

    @Test public void unknownChannelFailsClosed()throws Exception{
        ManualR0Calibration.load(null);JSONObject q=new JSONObject();q.put("rs",10.0);ManualR0Calibration.apply("unknown",q);
        assertFalse(q.getBoolean("r0_calibrated"));assertFalse(q.getBoolean("r0_provenance_complete"));assertFalse(q.getBoolean("r0_reference_validated"));
    }
}
