package com.servoc.app.measurement;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CanonicalMeasurementNormalizerTest {
    private static JSONObject channel(boolean ok,boolean powered,double ao,double rs,double r0,double ratio,long ageUs)throws Exception{
        JSONObject q=new JSONObject();q.put("ok",ok);q.put("valid",ok);q.put("powered",powered);q.put("ao",ao);q.put("rs",rs);q.put("r0",r0);q.put("ratio",ratio);q.put("r0_source","manual_device_r0");q.put("r0_calibrated",true);q.put("r0_reference_validated",false);q.put("age_us",ageUs);return q;
    }
    private static JSONObject raw(boolean ok,boolean powered,double ao,double r0,double ratio,long ageUs)throws Exception{
        JSONObject q=new JSONObject();q.put("ok",ok);q.put("powered",powered);q.put("ao",ao);q.put("r0",r0);q.put("ratio",ratio);q.put("r0_source","field_clean_air_r0");q.put("r0_session","FIELD");q.put("r0_utc","2026-09-13T01:00:47Z");q.put("r0_samples",216);q.put("r0_window_s",30);q.put("r0_reference_validated",false);q.put("r0_environment_compensated",false);q.put("age_us",ageUs);return q;
    }
    private static JSONObject snapshot(String key,JSONObject normalized,JSONObject source,boolean ads1,boolean ads2)throws Exception{
        JSONObject s=new JSONObject();JSONObject mq=new JSONObject();mq.put(key,normalized);s.put("mq",mq);
        JSONObject adc=new JSONObject();adc.put("ads1",ads1);adc.put("ads2",ads2);s.put("adc",adc);
        JSONObject rawMq=new JSONObject();if(source!=null)rawMq.put(key,source);JSONObject data=new JSONObject();data.put("mq",rawMq);s.put("_data_api",data);return s;
    }

    @Test public void preservesFirmwareR0AndMakesAndroidRatioCanonical()throws Exception{
        JSONObject s=snapshot("mq2",channel(true,true,2.5,20,10,99,100_000),raw(true,true,2.5,5,4,100_000),true,true);
        CanonicalMeasurementNormalizer.enrich(s,"DIRECT");JSONObject q=s.getJSONObject("mq").getJSONObject("mq2");
        assertEquals(CanonicalMeasurementNormalizer.SCHEMA,s.getString("measurement_schema"));
        assertEquals("DIRECT",s.getJSONObject("_transport").getString("mode"));
        assertEquals(5.0,q.getDouble("firmware_r0"),1e-12);assertEquals(4.0,q.getDouble("firmware_ratio"),1e-12);
        assertEquals(2.0,q.getDouble("ratio"),1e-12);assertEquals("ANDROID_ACTIVE_R0",q.getJSONObject("normalization").getString("owner"));
        assertFalse(q.getJSONObject("normalization").getBoolean("ratio_consistent"));
        assertEquals("VALID",q.getJSONObject("quality").getString("status"));assertTrue(q.getBoolean("valid"));
    }

    @Test public void distinguishesPowerAdcOpenAndSaturationStates()throws Exception{
        JSONObject off=snapshot("mq2",channel(false,false,0,1,1,1,0),raw(false,false,0,1,1,0),true,true);
        assertEquals("POWER_OFF",CanonicalMeasurementNormalizer.enrich(off,"DIRECT").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality").getString("status"));

        JSONObject adcMissing=snapshot("mq2",channel(false,true,1,1,1,1,0),raw(false,true,1,1,1,0),false,true);
        assertEquals("ADC_UNAVAILABLE",CanonicalMeasurementNormalizer.enrich(adcMissing,"DIRECT").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality").getString("status"));

        JSONObject open=snapshot("mq2",channel(false,true,0.005,1,1,1,0),raw(false,true,0.005,1,1,0),true,true);
        assertEquals("OPEN_OR_LOW_OUTPUT",CanonicalMeasurementNormalizer.enrich(open,"DIRECT").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality").getString("status"));

        JSONObject rail=snapshot("mq2",channel(false,true,5.0,1,1,1,0),raw(false,true,5.0,1,1,0),true,true);
        assertEquals("RAIL_HIGH_OR_SATURATED",CanonicalMeasurementNormalizer.enrich(rail,"DIRECT").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality").getString("status"));
    }

    @Test public void marksMissingAndStaleWithoutInventingNoiseEvidence()throws Exception{
        JSONObject missing=snapshot("mq2",channel(true,true,2,2,1,2,0),null,true,true);
        JSONObject qm=CanonicalMeasurementNormalizer.enrich(missing,"REMOTE").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality");
        assertEquals("MISSING_SOURCE",qm.getString("status"));assertFalse(qm.getBoolean("noise_evaluable"));assertTrue(qm.isNull("noise_flag"));

        JSONObject stale=snapshot("mq2",channel(true,true,2,2,1,2,700_000),raw(true,true,2,1,2,700_000),true,true);
        JSONObject qs=CanonicalMeasurementNormalizer.enrich(stale,"REMOTE").getJSONObject("mq").getJSONObject("mq2").getJSONObject("quality");
        assertEquals("STALE",qs.getString("status"));assertEquals("REMOTE",stale.getJSONObject("_transport").getString("mode"));
    }

    @Test public void exposesMq7HeaterQualificationSeparatelyFromSignalValidity()throws Exception{
        JSONObject s=snapshot("mq7",channel(true,true,2,20,10,2,100_000),raw(true,true,2,10,2,100_000),true,true);
        JSONObject q=CanonicalMeasurementNormalizer.enrich(s,"DIRECT").getJSONObject("mq").getJSONObject("mq7").getJSONObject("quality");
        assertEquals("VALID",q.getString("status"));assertFalse(q.getBoolean("manufacturer_heater_protocol_valid"));
    }

    @Test public void preservesEnvironmentalBusProvenance()throws Exception{
        JSONObject s=new JSONObject();JSONObject env=new JSONObject();env.put("ok",true);env.put("sensor","BME280");env.put("address","0x76");s.put("env",env);
        JSONObject rawEnv=new JSONObject();JSONObject bus=new JSONObject();bus.put("sda",13);bus.put("scl",14);bus.put("hz",100000);rawEnv.put("bus",bus);JSONObject data=new JSONObject();data.put("env",rawEnv);s.put("_data_api",data);
        CanonicalMeasurementNormalizer.enrich(s,"DIRECT");JSONObject e=s.getJSONObject("env");
        assertEquals("HR069_ENV_I2C",e.getString("source"));assertEquals(13,e.getJSONObject("bus").getInt("sda"));assertTrue(e.getBoolean("provenance_complete"));
    }
}
