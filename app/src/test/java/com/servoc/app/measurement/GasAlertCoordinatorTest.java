package com.servoc.app.measurement;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class GasAlertCoordinatorTest {
    private static JSONObject snapshot(double ratio,long powerMs)throws Exception{
        JSONObject q=new JSONObject().put("ok",true).put("valid",true).put("ratio",ratio);
        JSONObject mq=new JSONObject().put("mq2",q);
        JSONObject relay=new JSONObject().put("on",true).put("mask",1).put("power_ms",powerMs);
        return new JSONObject().put("relay",relay).put("mq",mq);
    }

    @Test public void escalatesImmediatelyAndDowngradesWithHold()throws Exception{
        SensorHealthEngine health=new SensorHealthEngine();health.setBaseline("mq2",1.0);
        GasAlertCoordinator c=new GasAlertCoordinator();
        assertEquals(GasAlertCoordinator.DANGER,c.evaluate(snapshot(1.50,60000),health,60,1000));
        assertEquals(GasAlertCoordinator.DANGER,c.evaluate(snapshot(1.10,60000),health,60,2000));
        assertEquals(GasAlertCoordinator.NONE,c.evaluate(snapshot(1.10,60000),health,60,5001));
    }

    @Test public void elevatedResponseMapsToWarning()throws Exception{
        SensorHealthEngine health=new SensorHealthEngine();health.setBaseline("mq2",1.0);
        GasAlertCoordinator c=new GasAlertCoordinator();
        assertEquals(GasAlertCoordinator.WARNING,c.evaluate(snapshot(1.25,60000),health,60,1000));
    }

    @Test public void neverAlarmsBeforeWarmupOrWithoutBaseline()throws Exception{
        SensorHealthEngine health=new SensorHealthEngine();
        GasAlertCoordinator c=new GasAlertCoordinator();
        assertEquals(GasAlertCoordinator.NONE,c.evaluate(snapshot(1.80,10000),health,60,1000));
        assertEquals(GasAlertCoordinator.NONE,c.evaluate(snapshot(1.80,60000),health,60,2000));
    }

    @Test public void mqPowerOffClearsImmediately()throws Exception{
        SensorHealthEngine health=new SensorHealthEngine();health.setBaseline("mq2",1.0);
        GasAlertCoordinator c=new GasAlertCoordinator();
        assertEquals(GasAlertCoordinator.DANGER,c.evaluate(snapshot(1.50,60000),health,60,1000));
        JSONObject off=snapshot(1.50,60000);off.getJSONObject("relay").put("on",false).put("mask",0);
        assertEquals(GasAlertCoordinator.NONE,c.evaluate(off,health,60,1100));
    }
}
