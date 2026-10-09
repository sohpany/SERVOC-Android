package com.servoc.app.calibration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class AcceptedR0NormalizerEvidenceTest {
    @Test public void staleMeasurementIsUnavailableToStrictPathButRetainedForCorroboration()throws Exception{
        JSONObject q=measurement(false,"STALE",2.5);
        assertFalse(Double.isFinite(AcceptedR0Normalizer.ratio("mq2",q)));
        assertEquals(2.5,AcceptedR0Normalizer.availableRatio("mq2",q),1e-9);
        assertEquals(0.55,AcceptedR0Normalizer.evidenceWeight(q),1e-9);
        assertEquals("DEGRADED",AcceptedR0Normalizer.evidenceQualityClass(q));
    }

    @Test public void saturatedAndInvalidMeasurementsRemainWeakChemicalSupport()throws Exception{
        JSONObject saturated=measurement(false,"RAIL_HIGH_OR_SATURATED",3.0);
        assertEquals(3.0,AcceptedR0Normalizer.availableRatio("mq3",saturated),1e-9);
        assertEquals(0.25,AcceptedR0Normalizer.evidenceWeight(saturated),1e-9);
        assertEquals("WEAK",AcceptedR0Normalizer.evidenceQualityClass(saturated));

        JSONObject invalid=measurement(false,"INVALID",1.8);
        assertEquals(0.35,AcceptedR0Normalizer.evidenceWeight(invalid),1e-9);
        assertEquals(1.8,AcceptedR0Normalizer.availableRatio("mq135",invalid),1e-9);
    }

    @Test public void powerOrAdcLossNeverManufacturesChemicalSupport()throws Exception{
        JSONObject off=measurement(false,"POWER_OFF",2.0);assertEquals(2.0,AcceptedR0Normalizer.availableRatio("mq2",off),1e-9);assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(off),1e-12);assertEquals("DIAGNOSTIC_ONLY",AcceptedR0Normalizer.evidenceQualityClass(off));
        JSONObject adc=measurement(false,"ADC_UNAVAILABLE",2.0);assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(adc),1e-12);
        JSONObject missing=measurement(false,"MISSING_SOURCE",2.0);assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(missing),1e-12);
    }

    @Test public void noiseReducesButDoesNotEraseOtherwiseValidEvidence()throws Exception{
        JSONObject q=measurement(true,"VALID",2.0);JSONObject quality=q.getJSONObject("quality");quality.put("noise_evaluable",true);quality.put("noise_flag",true);quality.put("temporal_state","UNSTABLE");
        assertTrue(Double.isFinite(AcceptedR0Normalizer.ratio("mq5",q)));
        assertEquals(0.39,AcceptedR0Normalizer.evidenceWeight(q),1e-9);
        assertEquals("WEAK",AcceptedR0Normalizer.evidenceQualityClass(q));
    }

    @Test public void contradictoryValidFlagsCannotTurnPowerOrAdcFailureIntoChemicalEvidence()throws Exception{
        for(String state:new String[]{"POWER_OFF","ADC_UNAVAILABLE","MISSING_SOURCE"}){
            JSONObject q=measurement(true,state,2.0);
            q.put("rs",FieldR0Baseline.value("mq2"));
            assertTrue(state,Double.isNaN(AcceptedR0Normalizer.ratio("mq2",q)));
            assertTrue(state,Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq2",q)));
            assertEquals(state,0.0,AcceptedR0Normalizer.evidenceWeight(q),0.0);
            assertEquals(state,"DIAGNOSTIC_ONLY",AcceptedR0Normalizer.evidenceQualityClass(q));
            assertFalse(state,AcceptedR0Normalizer.usesAcceptedDeviceR0("mq2",q));
            assertFalse(state,AcceptedR0Normalizer.usesRecordedDeviceR0("mq2",q));
            assertEquals(state,2.0,AcceptedR0Normalizer.availableRecordedRatio("mq2",q),1e-9);
        }
    }

    @Test public void nestedHealthFailureAndContradictoryValidTextStaySupportOnly()throws Exception{
        JSONObject nested=measurement(true,"VALID",2.0);
        nested.remove("status");
        nested.getJSONObject("quality").put("status","ADC_UNAVAILABLE");
        assertEquals(0.0,AcceptedR0Normalizer.evidenceWeight(nested),0.0);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq3",nested)));

        JSONObject contradictory=measurement(false,"VALID",2.0);
        assertEquals(0.35,AcceptedR0Normalizer.evidenceWeight(contradictory),1e-9);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.ratio("mq3",contradictory)));
    }

    private static JSONObject measurement(boolean valid,String status,double ratio)throws Exception{
        JSONObject q=new JSONObject();q.put("valid",valid);q.put("ok",valid);q.put("status",status);q.put("ratio",ratio);JSONObject quality=new JSONObject();quality.put("status",status);q.put("quality",quality);return q;
    }
}
