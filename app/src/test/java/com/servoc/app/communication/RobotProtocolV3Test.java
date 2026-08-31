package com.servoc.app.communication;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RobotProtocolV3Test {
    @Test public void compactPayloadNormalizesToApplicationSnapshot()throws Exception{
        JSONObject diag=new JSONObject("{\"fw\":\"HazRob\",\"api\":3,\"heap\":250000,\"clients\":2,\"failsafe\":false,\"envType\":1,\"envAddr\":118,\"envId\":96}");
        JSONObject data=new JSONObject("{\"env\":{\"ok\":true,\"t\":25.4,\"h\":48.2,\"p\":1007.5},\"relay\":1,\"mq\":{\"mq2\":{\"ok\":true,\"raw\":1800,\"adc\":1.45,\"ao\":1.45,\"rs\":24.48,\"ratio\":2.448}},\"measurement\":{\"state\":3,\"group\":0,\"mq\":{\"mq2\":{\"ok\":true,\"raw\":1790.0,\"adc\":1.44,\"ao\":1.44,\"rs\":24.7,\"ratio\":2.47}}}}");
        JSONObject x=RobotProtocolV3.normalize(diag,data,12,-34);
        assertEquals("HazRob",x.getString("firmware"));assertEquals(3,x.getInt("api_version"));assertEquals(2,x.getInt("clients"));
        assertEquals(12,x.getJSONObject("drive").getInt("left"));assertEquals(-34,x.getJSONObject("drive").getInt("right"));assertFalse(x.getJSONObject("drive").getBoolean("failsafe"));
        JSONObject env=x.getJSONObject("env");assertTrue(env.getBoolean("ok"));assertEquals("BME280",env.getString("type"));assertEquals("0x76",env.getString("address"));assertEquals("0x60",env.getString("chip_id"));assertEquals(25.4,env.getDouble("temperature"),1e-9);
        JSONObject relay=x.getJSONObject("relay");assertTrue(relay.getBoolean("group1"));assertFalse(relay.getBoolean("group2"));
        assertTrue(x.getJSONObject("mq").getJSONObject("mq2").getBoolean("valid"));assertEquals("ready",x.getJSONObject("measurement").getString("state"));assertTrue(x.getJSONObject("measurement").getJSONObject("mq").getJSONObject("mq2").getBoolean("valid"));
    }

    @Test public void bmp280MarksHumidityUnavailable()throws Exception{
        JSONObject diag=new JSONObject("{\"api\":3,\"envType\":2,\"envAddr\":119,\"envId\":88}");
        JSONObject data=new JSONObject("{\"env\":{\"ok\":true,\"t\":31.2,\"h\":null,\"p\":995.1},\"relay\":0,\"mq\":{},\"measurement\":{\"state\":0,\"group\":0,\"mq\":{}}}");
        JSONObject env=RobotProtocolV3.normalize(diag,data,0,0).getJSONObject("env");
        assertEquals("BMP280",env.getString("type"));assertFalse(env.getBoolean("humidity_available"));assertTrue(env.isNull("humidity"));
    }

    @Test public void allFirmwareMeasurementStatesHaveStableNames()throws Exception{
        String[] expected={"idle","warmup","measuring","ready","error"};
        for(int state=0;state<expected.length;state++){
            JSONObject data=new JSONObject();data.put("env",new JSONObject());data.put("relay",0);data.put("mq",new JSONObject());JSONObject m=new JSONObject();m.put("state",state);m.put("group",0);m.put("mq",new JSONObject());data.put("measurement",m);
            assertEquals(expected[state],RobotProtocolV3.normalize(new JSONObject(),data,0,0).getJSONObject("measurement").getString("state"));
        }
    }
}
