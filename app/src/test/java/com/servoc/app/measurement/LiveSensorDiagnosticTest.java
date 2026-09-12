package com.servoc.app.measurement;

import com.servoc.app.communication.RobotProtocolV4;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveSensorDiagnosticTest {
    private static JSONObject allMq(boolean ok)throws Exception{
        JSONObject mq=new JSONObject();String[] keys={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};
        for(String k:keys)mq.put(k,new JSONObject().put("ok",ok).put("raw",ok?1200:0).put("adc",ok?0.75:0.0).put("ao",ok?0.75:0.0).put("rs",ok?56.0:JSONObject.NULL).put("ratio",ok?1.1:JSONObject.NULL).put("powered",true));
        return mq;
    }

    @Test public void liveAcquisitionDoesNotDependOnSessionState()throws Exception{
        JSONObject data=new JSONObject();data.put("relay",new JSONObject().put("on",true).put("mask",1).put("power_ms",15000));data.put("adc",new JSONObject().put("ads1",true).put("ads2",true));data.put("mq",allMq(true));
        data.put("measurement",new JSONObject().put("state",4).put("duration_s",30).put("elapsed_s",30).put("remaining_s",0).put("error","no_valid_samples").put("valid_channels",0).put("mq",new JSONObject()));
        JSONObject normalized=RobotProtocolV4.normalize(new JSONObject(),data,0,0);
        assertEquals("error",normalized.getJSONObject("measurement").getString("state"));
        assertEquals(LiveSensorDiagnostic.LIVE_OK,normalized.getJSONObject("sensor_power").getString("code"));
        assertTrue(normalized.getJSONObject("sensor_power").getBoolean("live_independent_of_session"));
        assertTrue(normalized.getJSONObject("mq").getJSONObject("mq9").getBoolean("ok"));
    }

    @Test public void zeroValidChannelsWithBothAdsOnlySuspectsPowerOrGround()throws Exception{
        JSONObject relay=new JSONObject().put("on",true).put("mask",1).put("power_ms",15000);
        JSONObject adc=new JSONObject().put("ads1",true).put("ads2",true);
        LiveSensorDiagnostic.Result r=LiveSensorDiagnostic.evaluate(relay,adc,allMq(false));
        assertEquals(LiveSensorDiagnostic.SUSPECT_POWER_OR_GROUND,r.code);
        assertEquals(0,r.validChannels);assertEquals(8,r.availableChannels);
        assertFalse(r.powerCauseConfirmed);
        assertEquals("inferred",r.toJson().getString("diagnostic_basis"));
    }

    @Test public void initialPowerOnWaitsBeforeDeclaringSignalFault()throws Exception{
        JSONObject relay=new JSONObject().put("on",true).put("mask",1).put("power_ms",1200);
        JSONObject adc=new JSONObject().put("ads1",true).put("ads2",true);
        LiveSensorDiagnostic.Result r=LiveSensorDiagnostic.evaluate(relay,adc,allMq(false));
        assertEquals(LiveSensorDiagnostic.WAITING_SAMPLES,r.code);
    }

    @Test public void missingOneAdsIsReportedWithoutStoppingOtherLiveChannels()throws Exception{
        JSONObject relay=new JSONObject().put("on",true).put("mask",1).put("power_ms",15000);
        JSONObject adc=new JSONObject().put("ads1",true).put("ads2",false);
        JSONObject mq=allMq(true);
        LiveSensorDiagnostic.Result r=LiveSensorDiagnostic.evaluate(relay,adc,mq);
        assertEquals(LiveSensorDiagnostic.ADS_PARTIAL,r.code);
        assertEquals(4,r.validChannels);assertEquals(4,r.availableChannels);assertEquals(8,r.expectedChannels);
    }
}
