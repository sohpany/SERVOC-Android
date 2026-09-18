package com.servoc.app.measurement;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class GasAlertCoordinatorTest {
    private static JSONObject snapshot(double a,double b,long powerMs)throws Exception{
        JSONObject mq=new JSONObject();int i=0;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            double ratio=i==0?a:(i==1?b:1.0);mq.put(hw.key,new JSONObject().put("ok",true).put("valid",true).put("raw",1800+i).put("ratio",ratio));i++;
        }
        return new JSONObject().put("relay",new JSONObject().put("on",true).put("mask",3).put("power_ms",powerMs)).put("mq",mq);
    }
    private static SensorHealthEngine ready()throws Exception{
        SensorHealthEngine h=new SensorHealthEngine();for(String k:SensorHealthEngine.INSTALLED)h.setBaseline(k,1.0);
        for(int n=0;n<6;n++)h.accept(snapshot(1,1,60000),n*250L);return h;
    }
    @Test public void singleChannelResponseWarnsImmediatelyButNeverEscalatesToDanger()throws Exception{
        SensorHealthEngine h=ready();GasAlertCoordinator c=new GasAlertCoordinator();
        for(int n=0;n<5;n++){JSONObject s=snapshot(1.5,1,60000);h.accept(s,n*250L+2000);assertEquals(GasAlertCoordinator.WARNING,c.evaluate(s,h,60,n*250L+2000));}
    }
    @Test public void persistentMultisensorResponseEscalates()throws Exception{
        SensorHealthEngine h=ready();GasAlertCoordinator c=new GasAlertCoordinator();JSONObject s=snapshot(1.5,1.5,60000);
        h.accept(s,2000);assertEquals(GasAlertCoordinator.NONE,c.evaluate(s,h,60,2000));
        h.accept(s,2250);assertEquals(GasAlertCoordinator.WARNING,c.evaluate(s,h,60,2250));
        h.accept(s,2500);assertEquals(GasAlertCoordinator.DANGER,c.evaluate(s,h,60,2500));
        assertFalse(c.liveSnapshot().chemicalIdentityPermitted());assertFalse(c.liveSnapshot().quantitativeSafetyInterpretationPermitted());
    }
    @Test public void warmupBlocksOperationalAlert()throws Exception{
        SensorHealthEngine h=ready();GasAlertCoordinator c=new GasAlertCoordinator();JSONObject s=snapshot(1.5,1.5,10000);h.accept(s,2000);
        assertEquals(GasAlertCoordinator.NONE,c.evaluate(s,h,60,2000));assertEquals(LiveGasStateEngine.State.WARMING_UP,c.liveSnapshot().state);
    }
    @Test public void deescalationRetainsAudibleHold()throws Exception{
        SensorHealthEngine h=ready();GasAlertCoordinator c=new GasAlertCoordinator();JSONObject high=snapshot(1.5,1.5,60000);
        for(int n=0;n<3;n++){h.accept(high,2000+n*250);c.evaluate(high,h,60,2000+n*250);}
        assertEquals(GasAlertCoordinator.DANGER,c.level());JSONObject normal=snapshot(1,1,60000);h.accept(normal,3000);
        assertEquals(GasAlertCoordinator.DANGER,c.evaluate(normal,h,60,3000));h.accept(normal,6101);assertEquals(GasAlertCoordinator.NONE,c.evaluate(normal,h,60,6101));
    }
}
