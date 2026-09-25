package com.servoc.app.communication;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RobotProtocolV4DiagnosticsTest {
    @Test public void measurementCountdownAndErrorSurviveNormalization()throws Exception{
        JSONObject diag=new JSONObject("{\"fw\":\"HazRob 0.10.1\",\"api\":4,\"adc\":{\"ads1\":true,\"ads2\":false},\"envType\":1,\"envAddr\":118,\"envId\":96}");
        JSONObject data=new JSONObject("{\"env\":{\"ok\":true,\"t\":25.0,\"h\":50.0,\"p\":1000.0},\"relay\":{\"on\":true,\"mask\":1,\"power_ms\":65000},\"adc\":{\"ads1\":true,\"ads2\":false},\"mq\":{},\"measurement\":{\"state\":2,\"mask\":1,\"power\":true,\"duration_s\":30,\"elapsed_s\":7,\"remaining_s\":23,\"error\":\"\",\"valid_channels\":4,\"mq\":{}}}");
        JSONObject x=RobotProtocolV4.normalize(diag,data,0,0);
        JSONObject adc=x.getJSONObject("adc");assertTrue(adc.getBoolean("ads1"));assertFalse(adc.getBoolean("ads2"));assertTrue(adc.getBoolean("any_ready"));assertFalse(adc.getBoolean("all_ready"));
        JSONObject m=x.getJSONObject("measurement");assertEquals("measuring",m.getString("state"));assertEquals(30,m.getInt("duration_s"));assertEquals(7,m.getInt("elapsed_s"));assertEquals(23,m.getInt("remaining_s"));assertEquals(4,m.getInt("valid_channels"));assertEquals("",m.getString("error"));
        JSONObject env=x.getJSONObject("env");assertTrue(env.getBoolean("ok"));assertEquals("BME280",env.getString("type"));assertEquals("0x76",env.getString("address"));assertEquals("0x60",env.getString("chip_id"));
    }

    @Test public void explicitMeasurementErrorIsPreserved()throws Exception{
        JSONObject data=new JSONObject("{\"env\":{},\"relay\":{\"on\":true,\"mask\":1},\"adc\":{\"ads1\":false,\"ads2\":false},\"mq\":{},\"measurement\":{\"state\":4,\"duration_s\":30,\"elapsed_s\":0,\"remaining_s\":0,\"error\":\"ads_missing\",\"valid_channels\":0,\"mq\":{}}}");
        JSONObject x=RobotProtocolV4.normalize(new JSONObject(),data,0,0);JSONObject m=x.getJSONObject("measurement");
        assertEquals("error",m.getString("state"));assertEquals("ads_missing",m.getString("error"));assertEquals(0,m.getInt("valid_channels"));assertFalse(x.getJSONObject("adc").getBoolean("any_ready"));
    }

    @Test public void environmentalFailureCauseSurvivesNormalization()throws Exception{
        JSONObject diag=new JSONObject("{\"envType\":0,\"envAddr\":0,\"envId\":0,\"envStatus\":\"unsupported_chip_id\",\"envAck76\":true,\"envAck77\":false,\"envReadFailures\":0}");
        JSONObject data=new JSONObject("{\"env\":{\"ok\":false,\"t\":null,\"h\":null,\"p\":null,\"type\":0,\"addr\":0,\"chip_id\":0,\"status\":\"unsupported_chip_id\",\"ack_0x76\":true,\"ack_0x77\":false,\"last_addr\":118,\"last_chip_id\":97,\"read_failures\":0,\"last_ok_age_ms\":0,\"reprobe_ms\":3000},\"mq\":{},\"measurement\":{}}");
        JSONObject env=RobotProtocolV4.normalize(diag,data,0,0).getJSONObject("env");
        assertFalse(env.getBoolean("ok"));assertTrue(env.getBoolean("i2c_found"));assertTrue(env.getBoolean("ack_0x76"));assertFalse(env.getBoolean("ack_0x77"));assertEquals("unsupported_chip_id",env.getString("status_code"));assertEquals("0x76",env.getString("last_address"));assertEquals("0x61",env.getString("last_chip_id"));assertEquals(3000,env.getLong("reprobe_ms"));
    }

    @Test public void bme280DiagnosticsAndValuesSurviveNormalization()throws Exception{
        JSONObject data=new JSONObject("{\"env\":{\"ok\":true,\"t\":26.4,\"h\":41.2,\"p\":874.6,\"type\":1,\"addr\":118,\"chip_id\":96,\"status\":\"ok\",\"ack_0x76\":true,\"ack_0x77\":false,\"last_addr\":118,\"last_chip_id\":96,\"read_failures\":0,\"last_ok_age_ms\":120},\"mq\":{},\"measurement\":{}}");
        JSONObject env=RobotProtocolV4.normalize(new JSONObject(),data,0,0).getJSONObject("env");
        assertTrue(env.getBoolean("ok"));assertEquals("BME280",env.getString("type"));assertEquals("0x76",env.getString("address"));assertEquals("0x60",env.getString("chip_id"));assertEquals(26.4,env.getDouble("temperature"),1e-9);assertEquals(41.2,env.getDouble("humidity"),1e-9);assertEquals(874.6,env.getDouble("pressure"),1e-9);assertEquals("ok",env.getString("status_code"));
    }

    @Test public void bmp280KeepsHumidityUnavailable()throws Exception{
        JSONObject data=new JSONObject("{\"env\":{\"ok\":true,\"t\":24.1,\"h\":null,\"p\":900.2,\"type\":2,\"addr\":119,\"chip_id\":88,\"status\":\"ok\",\"ack_0x76\":false,\"ack_0x77\":true},\"mq\":{},\"measurement\":{}}");
        JSONObject env=RobotProtocolV4.normalize(new JSONObject(),data,0,0).getJSONObject("env");
        assertTrue(env.getBoolean("ok"));assertEquals("BMP280",env.getString("type"));assertEquals("0x77",env.getString("address"));assertEquals("0x58",env.getString("chip_id"));assertTrue(env.isNull("humidity"));assertFalse(env.getBoolean("humidity_available"));
    }

    @Test public void noAckAndReadFailureRemainFailClosed()throws Exception{
        JSONObject noAck=new JSONObject("{\"env\":{\"ok\":false,\"status\":\"no_ack\",\"ack_0x76\":false,\"ack_0x77\":false,\"read_failures\":0},\"mq\":{},\"measurement\":{}}");
        JSONObject a=RobotProtocolV4.normalize(new JSONObject(),noAck,0,0).getJSONObject("env");
        assertFalse(a.getBoolean("ok"));assertFalse(a.getBoolean("i2c_found"));assertEquals("no_ack",a.getString("status_code"));
        JSONObject failed=new JSONObject("{\"env\":{\"ok\":false,\"type\":1,\"addr\":118,\"chip_id\":96,\"status\":\"runtime_read_failed_reprobe\",\"ack_0x76\":true,\"ack_0x77\":false,\"read_failures\":3,\"last_addr\":118,\"last_chip_id\":96},\"mq\":{},\"measurement\":{}}");
        JSONObject b=RobotProtocolV4.normalize(new JSONObject(),failed,0,0).getJSONObject("env");
        assertFalse(b.getBoolean("ok"));assertTrue(b.getBoolean("i2c_found"));assertEquals(3,b.getInt("read_failures"));assertEquals("runtime_read_failed_reprobe",b.getString("status_code"));
    }

    @Test public void absentInternalTemperatureDoesNotClaimEnvSclGpio()throws Exception{
        JSONObject data=new JSONObject("{\"device\":{\"internal_temp\":{\"installed\":false,\"c\":null,\"gpio\":null}},\"env\":{},\"mq\":{},\"measurement\":{}}");
        JSONObject internal=RobotProtocolV4.normalize(new JSONObject(),data,0,0).getJSONObject("device").getJSONObject("internal_temp");
        assertFalse(internal.getBoolean("installed"));assertTrue(internal.isNull("gpio"));
    }
}
