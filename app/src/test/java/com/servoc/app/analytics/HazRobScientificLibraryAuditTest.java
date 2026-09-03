package com.servoc.app.analytics;

import com.servoc.app.calibration.CalibrationCurveProvider;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import org.json.JSONObject;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public final class HazRobScientificLibraryAuditTest {
    @Test public void allEightInstalledMqChannelsHaveRegisteredCurves(){
        assertEquals(8,HazRobHardwareProfile.MQ.length);
        assertEquals(39,GasEngine.curveCount());
        for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){
            int n=0;for(int chem:GasEngine.SUPPORTED)if(GasEngine.hasCurve(ch.sensorId,chem))n++;
            assertTrue(ch.key+" lost all registered curves",n>0);
        }
    }

    @Test public void mqDataScienceCatalogueAndRuntimeAuditRemainExplicit(){
        assertEquals(76,MqDataScience76Catalog.COUNT);
        assertEquals(76,ModelRuntimeRegistry.adoptedCount());
        assertEquals(14,ModelRuntimeRegistry.nativeCount());
        assertEquals(12,ModelRuntimeRegistry.verifiedCount());
        assertEquals(2,ModelRuntimeRegistry.approximationCount());
        assertEquals(62,ModelRuntimeRegistry.pendingPortCount());
    }

    @Test public void analyticsPassesDeviceCalibrationProviderIntoEngine(){
        JSONObject root=new JSONObject(),mq=new JSONObject();
        for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){JSONObject q=new JSONObject();q.put("ratio",1.0);mq.put(ch.key,q);}root.put("mq",mq);
        AtomicInteger calls=new AtomicInteger();
        CalibrationCurveProvider provider=(sensor,chemical,ratio)->{calls.incrementAndGet();return null;};
        AnalyticsSnapshot.analyze(root,provider,false);
        assertTrue("device calibration provider was not consulted",calls.get()>0);
    }
}
