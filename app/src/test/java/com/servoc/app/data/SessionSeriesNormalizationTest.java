package com.servoc.app.data;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.calibration.ManualR0Calibration;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionSeriesNormalizationTest {
    @Test public void reportViewPreservesStoredDeviceR0AndRepairsOnlyRecordedRatio()throws Exception{
        ManualR0Calibration.load(null);
        // Actual recorded device R0 is deliberately different from the current default.
        JSONObject mq2=new JSONObject().put("ok",true).put("rs",80.0).put("ratio",0.2)
            .put("r0",20.0).put("r0_source","manual_device_r0")
            .put("r0_session","MANUAL-R0-20261008").put("r0_utc","2026-10-08T22:00:00Z")
            .put("r0_provenance_complete",true).put("r0_reference_validated",false);
        JSONObject root=new JSONObject().put("mq2",mq2);
        SessionSeries.Sample sample=new SessionSeries.Sample(0,"{}",root.toString(),"{}");
        JSONObject q=SessionSeries.sensor(sample,"mq2");
        assertNotNull(q);
        assertEquals(4.0,q.getDouble("ratio"),1e-9);
        assertEquals(20.0,q.getDouble("r0"),1e-9);
        assertNotEquals(FieldR0Baseline.value("mq2"),q.getDouble("r0"),1e-9);
        assertEquals("manual_device_r0",q.getString("r0_source"));
        assertEquals("MANUAL-R0-20261008",q.getString("r0_session"));
        assertEquals("persisted_rs_r0",q.getString("historical_ratio_source"));
        assertTrue(q.getBoolean("historical_calibration_frozen"));
        assertTrue(q.getBoolean("r0_device_local"));
        assertFalse(q.getBoolean("r0_reference_validated"));
        assertFalse(q.getBoolean("r0_centrally_publishable"));
        // No mutation of the serialized sample or artificial replacement with today's default.
        assertEquals(0.2,mq2.getDouble("ratio"),1e-9);
        assertEquals(4.0,AcceptedR0Normalizer.recordedRatio("mq2",q),1e-9);
    }

    @Test public void legacyHistoricalRatioHasNoSyntheticR0OrSource()throws Exception{
        JSONObject mq2=new JSONObject().put("ok",true).put("ratio",2.5);
        SessionSeries.Sample sample=new SessionSeries.Sample(0,"{}",new JSONObject().put("mq2",mq2).toString(),"{}");
        JSONObject q=SessionSeries.sensor(sample,"mq2");
        assertNotNull(q);
        assertEquals(2.5,q.getDouble("ratio"),1e-9);
        assertFalse(q.has("r0"));
        assertFalse(q.has("r0_session"));
        assertFalse(q.getBoolean("r0_device_local"));
        assertEquals("persisted_ratio",q.getString("historical_ratio_source"));
    }
}
