package com.servoc.app.measurement;

import static org.junit.Assert.*;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class LiveGasStateEngineTest {
    private static SensorHealthEngine.Snapshot snap(String key,SensorHealthEngine.State state,double baseline,double ratio)throws Exception{
        Constructor<SensorHealthEngine.Snapshot> c=SensorHealthEngine.Snapshot.class.getDeclaredConstructor(String.class,SensorHealthEngine.State.class,int.class,int.class,double.class,double.class,double.class,double.class,double.class,double.class,String.class);
        c.setAccessible(true);return c.newInstance(key,state,24,0,1800,2,0.01,baseline,0,ratio,"test");
    }
    private static Map<String,SensorHealthEngine.Snapshot> array(double a,double b)throws Exception{
        Map<String,SensorHealthEngine.Snapshot> m=new LinkedHashMap<>();
        m.put("mq2",snap("mq2",SensorHealthEngine.State.HEALTHY,1,a));
        m.put("mq3",snap("mq3",SensorHealthEngine.State.HEALTHY,1,b));
        m.put("mq4",snap("mq4",SensorHealthEngine.State.HEALTHY,1,1));
        m.put("mq5",snap("mq5",SensorHealthEngine.State.HEALTHY,1,1));
        m.put("mq7",snap("mq7",SensorHealthEngine.State.HEALTHY,1,1));
        m.put("mq8",snap("mq8",SensorHealthEngine.State.HEALTHY,1,1));
        m.put("mq9",snap("mq9",SensorHealthEngine.State.HEALTHY,1,1));
        m.put("mq135",snap("mq135",SensorHealthEngine.State.HEALTHY,1,1));return m;
    }
    @Test public void disconnectedAndWarmupFailSafe()throws Exception{
        LiveGasStateEngine e=new LiveGasStateEngine();
        assertEquals(LiveGasStateEngine.State.UNAVAILABLE,e.evaluate(array(1,1),false,false).state);
        assertEquals(LiveGasStateEngine.State.WARMING_UP,e.evaluate(array(1,1),true,true).state);
    }
    @Test public void singleSpikeDoesNotAlert()throws Exception{
        LiveGasStateEngine e=new LiveGasStateEngine();
        for(int i=0;i<5;i++)assertEquals(LiveGasStateEngine.State.NORMAL,e.evaluate(array(1.35,1),true,false).state);
    }
    @Test public void persistentMultisensorResponseEscalatesAndDeescalates()throws Exception{
        LiveGasStateEngine e=new LiveGasStateEngine();
        assertEquals(LiveGasStateEngine.State.NORMAL,e.evaluate(array(1.30,1.30),true,false).state);
        assertEquals(LiveGasStateEngine.State.RESPONSE_WATCH,e.evaluate(array(1.30,1.30),true,false).state);
        assertEquals(LiveGasStateEngine.State.RESPONSE_ALERT,e.evaluate(array(1.30,1.30),true,false).state);
        assertEquals(LiveGasStateEngine.State.NORMAL,e.evaluate(array(1,1),true,false).state);
    }
    @Test public void insufficientUsableChannelsIsDegraded()throws Exception{
        Map<String,SensorHealthEngine.Snapshot> m=array(1,1);
        m.put("mq2",snap("mq2",SensorHealthEngine.State.MISSING,1,1));m.put("mq3",snap("mq3",SensorHealthEngine.State.INVALID,1,1));m.put("mq4",snap("mq4",SensorHealthEngine.State.SATURATED,1,1));m.put("mq5",snap("mq5",SensorHealthEngine.State.STUCK,1,1));m.put("mq7",snap("mq7",SensorHealthEngine.State.MISSING,1,1));
        assertEquals(LiveGasStateEngine.State.DEGRADED,new LiveGasStateEngine().evaluate(m,true,false).state);
    }
    @Test public void operationalAlertNeverPermitsIdentityOrQuantitativeSafety()throws Exception{
        LiveGasStateEngine e=new LiveGasStateEngine();e.evaluate(array(1.3,1.3),true,false);e.evaluate(array(1.3,1.3),true,false);
        LiveGasStateEngine.Snapshot s=e.evaluate(array(1.3,1.3),true,false);
        assertEquals(LiveGasStateEngine.State.RESPONSE_ALERT,s.state);assertFalse(s.chemicalIdentityPermitted());assertFalse(s.quantitativeSafetyInterpretationPermitted());
    }
}
