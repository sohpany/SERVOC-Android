package com.servoc.app.analytics;

import com.servoc.app.calibration.CalibrationCurveProvider;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public final class HazRobScientificLibraryAuditTest {
    @Test public void allEightInstalledMqChannelsHaveRegisteredCurves(){
        assertEquals(8,HazRobHardwareProfile.MQ.length);
        assertEquals(39,GasEngine.curveCount());
        assertEquals(43,ResearchCurveRegistry.count());
        for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){
            int primary=0;for(int chem:GasEngine.SUPPORTED)if(GasEngine.hasCurve(ch.sensorId,chem))primary++;
            assertTrue(ch.key+" lost all operational curves",primary>0);
            assertTrue(ch.key+" lost all research curves",ResearchCurveRegistry.forSensor(ch.sensorId).size()>0);
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

    @Test public void researchOnlySensorAnalytePairCanContributeFingerprintEvidence(){
        ResearchCurveRegistry.Curve chosen=null;for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.sensor==GasEngine.MQ2&&c.chemical==76){chosen=c;break;}
        assertNotNull(chosen);assertFalse(GasEngine.hasCurve(GasEngine.MQ2,76));
        double[] ratios=new double[8];Arrays.fill(ratios,Double.NaN);ratios[GasEngine.MQ2]=chosen.ratioFromPpm(1000.0);
        boolean[] heater={true,true,true,true,true,true,true,true};
        ValidatedGasEngine.Result r=ValidatedGasEngine.analyzeOne(ratios,76,heater);
        assertNotNull(r);assertTrue(r.communityCrossContributors>0);assertFalse(r.quantitative);
    }
}
