package com.servoc.app.measurement;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SensorHealthEngineTest {
    @Test public void validPoweredSamplesBecomeHealthy()throws Exception{
        SensorHealthEngine e=new SensorHealthEngine();
        for(int i=0;i<6;i++)e.accept(live(1,"mq2",1800+i,2.0+i*0.01,true),1000L*i);
        SensorHealthEngine.Snapshot s=e.snapshot("mq2");assertEquals(SensorHealthEngine.State.HEALTHY,s.state);assertEquals(6,s.validSamples);assertTrue(s.noiseCv<0.01);
    }

    @Test public void absentPoweredChannelIsMissing()throws Exception{
        SensorHealthEngine e=new SensorHealthEngine();JSONObject x=live(1,null,0,0,false);
        e.accept(x,0);e.accept(x,1000);e.accept(x,2000);
        assertEquals(SensorHealthEngine.State.MISSING,e.snapshot("mq2").state);
    }

    @Test public void repeatedAdsRailReadingsAreSaturation()throws Exception{
        SensorHealthEngine e=new SensorHealthEngine();e.accept(live(1,"mq2",32766,1.0,true),0);e.accept(live(1,"mq2",32765,1.0,true),1000);
        assertEquals(SensorHealthEngine.State.SATURATED,e.snapshot("mq2").state);
    }

    @Test public void driftRequiresExplicitCleanAirReference()throws Exception{
        SensorHealthEngine e=new SensorHealthEngine();JSONObject baseline=ready("mq2",1800,2.0);assertEquals(1,e.captureCleanAirBaseline(baseline));
        assertTrue(Double.isNaN(e.snapshot("mq2").driftPercent));
        assertEquals(1,e.evaluateCleanAirDrift(ready("mq2",1700,2.8)));
        SensorHealthEngine.Snapshot s=e.snapshot("mq2");assertEquals(40.0,s.driftPercent,1e-9);assertEquals(SensorHealthEngine.State.DRIFT_WARNING,s.state);
    }

    @Test public void arbitraryExposureDoesNotCreateDrift()throws Exception{
        SensorHealthEngine e=new SensorHealthEngine();e.setBaseline("mq2",2.0);
        for(int i=0;i<5;i++)e.accept(live(1,"mq2",1500+i,4.0,true),i*1000L);
        SensorHealthEngine.Snapshot s=e.snapshot("mq2");assertTrue(Double.isNaN(s.driftPercent));assertNotEquals(SensorHealthEngine.State.DRIFT_WARNING,s.state);
    }

    @Test public void allEightInstalledChannelsExist(){
        assertEquals(8,SensorHealthEngine.INSTALLED.length);
        assertNotNull(new SensorHealthEngine().snapshot("mq5"));
        assertNotNull(new SensorHealthEngine().snapshot("mq9"));
    }

    private static JSONObject live(int group,String key,double raw,double ratio,boolean ok)throws Exception{
        JSONObject root=new JSONObject(),relay=new JSONObject(),mq=new JSONObject();relay.put("mask",1<<(group-1));root.put("relay",relay);
        if(key!=null){JSONObject r=new JSONObject();r.put("valid",ok);r.put("ok",ok);r.put("raw",raw);r.put("ratio",ratio);mq.put(key,r);}root.put("mq",mq);return root;
    }
    private static JSONObject ready(String key,double raw,double ratio)throws Exception{
        JSONObject root=new JSONObject(),m=new JSONObject(),mq=new JSONObject(),r=new JSONObject();r.put("valid",true);r.put("ok",true);r.put("raw",raw);r.put("ratio",ratio);mq.put(key,r);m.put("state","ready");m.put("group",0);m.put("mq",mq);root.put("measurement",m);root.put("mq",new JSONObject());root.put("relay",new JSONObject().put("mask",0));return root;
    }
}
