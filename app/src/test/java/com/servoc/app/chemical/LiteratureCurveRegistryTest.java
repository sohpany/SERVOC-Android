package com.servoc.app.chemical;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class LiteratureCurveRegistryTest {
    @Test public void peerReviewedCurvesArePinnedAndMq135AcetoneRemainsInvertible(){
        // Six UMBU MQ135 curves + MQ8 H2 + MQ7 CO + MQ4 CH4.
        assertEquals(9,LiteratureCurveRegistry.count());
        LiteratureCurveRegistry.Curve acetone=null;
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)if(c.chemical==110)acetone=c;
        assertNotNull(acetone);
        assertEquals("10.23960/jtaf.v11i2.314",acetone.doi);
        double ratio=acetone.ratioFromPpm(50.0);
        assertTrue(Double.isFinite(ratio));
        assertEquals(50.0,acetone.ppmFromRatio(ratio),1e-9);
        assertTrue(acetone.inDeclaredRange(50.0));
        assertFalse(acetone.inDeclaredRange(150.0));
    }

    @Test public void peerReviewedCurveWinsDuplicateGenericMq135InsidePublishedDomain(){
        LiteratureCurveRegistry.Curve acetone=null;
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)if(c.chemical==110)acetone=c;
        assertNotNull(acetone);
        double[] ratios=new double[8];Arrays.fill(ratios,Double.NaN);
        ratios[GasEngine.MQ135]=acetone.ratioFromPpm(50.0);
        boolean[] heater={true,true,true,true,true,true,true,true};
        ValidatedGasEngine.Result r=ValidatedGasEngine.analyzeOne(ratios,110,heater);
        assertNotNull(r);
        assertEquals(1,r.totalContributors); // same MQ135 channel is not counted again from GasEngine/MQDataScience
        assertEquals(50.0,r.ppm,1e-8);
        assertFalse(r.quantitative);
        assertTrue(r.note.contains("Peer-reviewed datasheet-graph fit preferred"));
    }

    @Test public void peerReviewedCurveIsNotExtrapolatedAndDoesNotCreateM4(){
        LiteratureCurveRegistry.Curve acetone=null;
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)if(c.chemical==110)acetone=c;
        assertNotNull(acetone);
        double[] ratios=new double[8];Arrays.fill(ratios,Double.NaN);
        ratios[GasEngine.MQ135]=acetone.ratioFromPpm(150.0); // outside 10-100 ppm literature domain
        ValidatedGasEngine.Result r=ValidatedGasEngine.analyzeOne(ratios,110,new boolean[]{true,true,true,true,true,true,true,true});
        assertNotNull(r); // lower-tier registered research path may still screen within its own envelope
        assertFalse(r.quantitative);
        assertFalse(r.note.contains("Peer-reviewed datasheet-graph fit preferred"));
    }
}
