package com.servoc.app.measurement;

import com.servoc.app.chemcore.ChemCoreEarlyBuildQualityPolicy;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChemCoreEarlyBuildQualityPolicyTest {
    @Test public void warningsRemainUsableDuringExploratoryBuild(){
        Map<String,SensorHealthEngine.Snapshot> h=new LinkedHashMap<>();
        h.put("mq2",snap("mq2",SensorHealthEngine.State.HEALTHY));
        h.put("mq3",snap("mq3",SensorHealthEngine.State.DRIFT_WARNING));
        h.put("mq4",snap("mq4",SensorHealthEngine.State.UNSTABLE));
        h.put("mq5",snap("mq5",SensorHealthEngine.State.MISSING));
        ChemCoreEarlyBuildQualityPolicy.Result r=ChemCoreEarlyBuildQualityPolicy.evaluate(h);
        assertEquals(ChemCoreEarlyBuildQualityPolicy.State.USABLE_WITH_WARNINGS,r.state);
        assertEquals(3,r.healthyOrUsableChannels);
        assertTrue(r.warningChannels>=3);
        assertTrue(ChemCoreEarlyBuildQualityPolicy.softDiagnosticByDefault());
    }

    @Test public void onlyMissingOrInvalidChannelsAreHardInvalid(){
        Map<String,SensorHealthEngine.Snapshot> h=new LinkedHashMap<>();
        h.put("mq2",snap("mq2",SensorHealthEngine.State.MISSING));
        h.put("mq3",snap("mq3",SensorHealthEngine.State.INVALID));
        ChemCoreEarlyBuildQualityPolicy.Result r=ChemCoreEarlyBuildQualityPolicy.evaluate(h);
        assertEquals(ChemCoreEarlyBuildQualityPolicy.State.INVALID,r.state);
        assertEquals(0,r.healthyOrUsableChannels);
    }

    private static SensorHealthEngine.Snapshot snap(String key,SensorHealthEngine.State state){
        return new SensorHealthEngine.Snapshot(key,state,12,0,1000,10,.01,1.0,0,1.0,"test");
    }
}
