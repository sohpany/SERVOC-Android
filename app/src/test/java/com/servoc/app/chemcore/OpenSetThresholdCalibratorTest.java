package com.servoc.app.chemcore;

import org.junit.Test;

import static org.junit.Assert.*;

public class OpenSetThresholdCalibratorTest {
    @Test public void separatedHeldOutUnknownsProduceUsefulGate(){
        double[][] train={
                {0.0,0.0},{0.1,-0.1},{-0.1,0.1},{0.05,0.05},{-0.05,-0.05},{0.08,0.02}
        };
        double[][] known={{0.03,-0.02},{-0.04,0.03},{0.07,0.01},{-0.02,-0.06}};
        double[][] unknown={{3.0,3.0},{-3.0,2.5},{2.5,-3.0},{4.0,0.5}};
        OpenSetThresholdCalibrator.Result r=OpenSetThresholdCalibrator.calibrate(train,known,unknown,1e-3,0.75);
        assertTrue(Double.isFinite(r.threshold));
        assertTrue(r.knownRecall>=0.75);
        assertTrue(r.unknownRecall>=0.75);
        assertTrue(r.balancedAccuracy>=0.75);
        assertEquals(OpenSetThresholdCalibrator.PROTOCOL,r.protocol);
        assertTrue(r.gate(1.0).evaluate(known[0]).accepted);
        assertFalse(r.gate(1.0).evaluate(unknown[0]).accepted);
    }
}
