package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class AcceptedR0NormalizerTest {
    @Test public void acceptedR0OverridesStaleFirmwareRatioWhenRsExists()throws Exception{
        JSONObject q=new JSONObject().put("ok",true).put("rs",FieldR0Baseline.value("mq2")).put("ratio",0.25).put("r0",10.0);
        assertEquals(1.0,AcceptedR0Normalizer.ratio("mq2",q),1e-9);
        assertTrue(AcceptedR0Normalizer.usesAcceptedFieldR0("mq2",q));
        assertTrue(AcceptedR0Normalizer.provenance("mq2",q).contains(FieldR0Baseline.SESSION));
    }

    @Test public void telemetryRatioIsCompatibilityFallbackOnly()throws Exception{
        JSONObject q=new JSONObject().put("valid",true).put("ratio",0.75);
        assertEquals(0.75,AcceptedR0Normalizer.ratio("mq2",q),1e-9);
        assertFalse(AcceptedR0Normalizer.usesAcceptedFieldR0("mq2",q));
        assertEquals("telemetry_ratio_fallback",AcceptedR0Normalizer.provenance("mq2",q));
    }

    @Test public void invalidChannelNeverProducesRatio()throws Exception{
        JSONObject q=new JSONObject().put("ok",false).put("rs",FieldR0Baseline.value("mq2"));
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
    }
}