package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreLabQualityTest {
    private static final String[] MQ={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};

    private static JSONObject diagnostic(boolean envOk,boolean referenceValidated)throws Exception{
        JSONObject d=new JSONObject();
        d.put("relay",new JSONObject().put("on",true).put("mask",1));
        d.put("adc",new JSONObject().put("ads1",true).put("ads2",true));
        JSONObject mq=new JSONObject();
        for(String key:MQ)mq.put(key,new JSONObject().put("ok",true).put("r0_calibrated",true).put("r0_loaded",true)
                .put("r0_source","field_clean_air_r0").put("r0_reference_validated",referenceValidated));
        d.put("mq",mq);
        JSONObject env=new JSONObject().put("ok",envOk).put("type","BME280").put("status_code",envOk?"ok":"no_ack")
                .put("address",envOk?"0x76":"").put("chip_id",envOk?"0x60":"");
        if(envOk)env.put("temperature",24.5).put("humidity",42.0).put("pressure",874.2);
        d.put("env",env);return d;
    }

    @Test public void cleanAirR0UnlocksFingerprintButNotReferenceGasCalibration()throws Exception{
        ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diagnostic(true,false),0,0);
        assertTrue(q.configurationReady);assertTrue(q.captureReady);assertTrue(q.fingerprintNormalizationReady());
        assertEquals(8,q.r0NormalizedChannels);assertEquals(8,q.fieldR0Channels);assertEquals(0,q.referenceValidatedChannels);
        assertFalse(q.anyReferenceGasCalibrationAvailable());assertTrue(q.environmentReady);
        JSONObject json=q.toJson();assertTrue(json.getJSONObject("calibration_gate").getBoolean("field_r0_normalization_ready"));
        assertEquals(0,json.getJSONObject("calibration_gate").getInt("reference_gas_validated_channels"));
    }

    @Test public void missingEnvironmentIsAdvisoryNotFingerprintBlocker()throws Exception{
        ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(diagnostic(false,false),0,0);
        assertTrue(q.configurationReady);assertTrue(q.captureReady);assertFalse(q.environmentReady);
        assertTrue(q.advisories.toString().contains("environmental_compensation_unavailable"));
    }

    @Test public void movementOrMissingAdsFailsConfigurationGate()throws Exception{
        JSONObject d=diagnostic(true,false);d.getJSONObject("adc").put("ads2",false);
        ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(d,20,0);
        assertFalse(q.configurationReady);assertFalse(q.captureReady);
        assertTrue(q.blockers.contains("ads1115_0x49_not_ready"));assertTrue(q.blockers.contains("robot_not_stationary"));
    }
}
