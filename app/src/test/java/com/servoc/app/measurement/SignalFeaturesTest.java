package com.servoc.app.measurement;

import org.junit.Test;
import static org.junit.Assert.*;

public class SignalFeaturesTest {
    @Test public void risingResponseProducesExpectedFeatures(){
        double[] t={0,1,2,3,4,5};double[] y={1,1,1.2,1.5,1.8,2.0};SignalFeatures.Result r=SignalFeatures.extract(t,y);
        assertEquals(6,r.validSamples);assertTrue(Double.isFinite(r.baseline));assertEquals(1.0,r.baseline,1e-9);assertEquals(2.0,r.peak,1e-9);assertEquals(1.0,r.delta,1e-9);assertTrue(r.initialSlope>0);assertTrue(Double.isFinite(r.timeTo50));assertTrue(Double.isFinite(r.timeTo90));assertTrue(r.auc>0);
    }
    @Test public void fallingResponseIsHandled(){
        double[] t={0,1,2,3,4,5};double[] y={2,2,1.8,1.5,1.2,1.0};SignalFeatures.Result r=SignalFeatures.extract(t,y);
        assertEquals(2.0,r.baseline,1e-9);assertEquals(1.0,r.peak,1e-9);assertEquals(-1.0,r.delta,1e-9);assertTrue(r.initialSlope<0);assertTrue(Double.isFinite(r.timeTo50));assertTrue(Double.isFinite(r.timeTo90));assertTrue(r.auc<0);
    }
    @Test public void invalidTraceReturnsNoValidSamples(){SignalFeatures.Result r=SignalFeatures.extract(new double[]{0,1},new double[]{1,2});assertEquals(0,r.validSamples);assertFalse(Double.isFinite(r.baseline));}
}
