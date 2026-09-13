package com.servoc.app.data;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.FieldR0Baseline;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class SessionSeriesNormalizationTest {
    @Test public void reportViewRebindsStaleTelemetryRatioToAcceptedR0()throws Exception{
        JSONObject mq2=new JSONObject().put("ok",true).put("rs",FieldR0Baseline.value("mq2")).put("ratio",0.2).put("r0",10.0);
        JSONObject root=new JSONObject().put("mq2",mq2);
        SessionSeries.Sample sample=new SessionSeries.Sample(0,"{}",root.toString(),"{}");
        JSONObject q=SessionSeries.sensor(sample,"mq2");
        assertNotNull(q);
        assertEquals(1.0,q.getDouble("ratio"),1e-9);
        assertEquals(FieldR0Baseline.value("mq2"),q.getDouble("r0"),1e-9);
        assertEquals(AcceptedR0Normalizer.SOURCE,q.getString("r0_source"));
        assertEquals(FieldR0Baseline.SESSION,q.getString("r0_session"));
        assertFalse(q.getBoolean("r0_reference_validated"));
    }
}