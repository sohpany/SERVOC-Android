package com.servoc.app.measurement;

import com.servoc.app.chemcore.ChemCoreFeaturePipeline;
import org.junit.Test;
import static org.junit.Assert.*;

public class SignalFeaturesTest {
    @Test public void compatibilityViewMatchesCanonicalPipeline(){
        double[] t={0,1,2,3,4,5,6,7};
        double[] y={1,1,1.05,1.15,1.3,1.45,1.55,1.6};
        double[][] ratios=new double[y.length][1];for(int i=0;i<y.length;i++)ratios[i][0]=y[i];
        ChemCoreFeaturePipeline.Result c=ChemCoreFeaturePipeline.extract(t,ratios);
        SignalFeatures.Result r=SignalFeatures.extract(t,y);
        assertEquals(y.length,r.validSamples);
        assertEquals(c.feature(0,1),r.baseline,1e-12);
        assertEquals(c.feature(0,7),r.peak,1e-12);
        assertEquals(c.feature(0,3),r.delta,1e-12);
        assertEquals(c.feature(0,8),r.initialSlope,1e-12);
        assertEquals(c.feature(0,11),r.timeTo50,1e-12);
        assertEquals(c.feature(0,12),r.timeTo90,1e-12);
        assertEquals(c.feature(0,13),r.auc,1e-12);
        assertEquals(c.feature(0,15),r.noiseStd,1e-12);
    }
    @Test public void invalidTraceReturnsNoValidSamples(){
        SignalFeatures.Result r=SignalFeatures.extract(new double[]{0,1},new double[]{1,2});
        assertEquals(0,r.validSamples);assertFalse(Double.isFinite(r.baseline));
    }
}
