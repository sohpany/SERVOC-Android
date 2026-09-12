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
    }

    @Test public void explicitMeasurementErrorIsPreserved()throws Exception{
        JSONObject data=new JSONObject("{\"env\":{},\"relay\":{\"on\":true,\"mask\":1},\"adc\":{\"ads1\":false,\"ads2\":false},\"mq\":{},\"measurement\":{\"state\":4,\"duration_s\":30,\"elapsed_s\":0,\"remaining_s\":0,\"error\":\"ads_missing\",\"valid_channels\":0,\"mq\":{}}}");
        JSONObject x=RobotProtocolV4.normalize(new JSONObject(),data,0,0);JSONObject m=x.getJSONObject("measurement");
        assertEquals("error",m.getString("state"));assertEquals("ads_missing",m.getString("error"));assertEquals(0,m.getInt("valid_channels"));assertFalse(x.getJSONObject("adc").getBoolean("any_ready"));
    }
}
