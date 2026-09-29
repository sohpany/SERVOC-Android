package com.servoc.app.chemical;

import org.junit.Test;

import static org.junit.Assert.*;

public class OperationalHazardTransferBootstrapTest {
    @Test public void robofusionH1ContractIsOperationalOnlyAndSourcePinned(){
        assertEquals("ROBOFUSION_REAL_H1_2025",OperationalHazardTransferBootstrap.SOURCE_ID);
        assertEquals("d3907eda585cf202fc00c462b550d2512d5b9e6f",OperationalHazardTransferBootstrap.SOURCE_COMMIT);
        assertEquals("ae4637a1fbffbb0d7e0750f6623b17bcfecc184043df58702cea07a158546528",OperationalHazardTransferBootstrap.SOURCE_SHA256);
        assertEquals("052a6669d22a56c3d7d3ee1119fd5619a8c6e301b74b22e37784dd06d99669e6",OperationalHazardTransferBootstrap.DERIVED_SHA256);
        assertEquals(6,OperationalHazardTransferBootstrap.overlapCount());
        assertEquals(69120,OperationalHazardTransferBootstrap.SOURCE_ROWS);
        assertEquals(42704,OperationalHazardTransferBootstrap.ORIGINAL_ROWS_USED);
        assertEquals(26416,OperationalHazardTransferBootstrap.INTERPOLATED_ROWS_EXCLUDED);
        assertEquals(41537,OperationalHazardTransferBootstrap.NORMAL_ROWS);
        assertEquals(1167,OperationalHazardTransferBootstrap.HAZARD1_ROWS);
        assertFalse(OperationalHazardTransferBootstrap.PURE_ANALYTE_IDENTITY);
        assertFalse(OperationalHazardTransferBootstrap.QUANTITATIVE_PPM);
        assertFalse(OperationalHazardTransferBootstrap.M4);
    }

    @Test public void prototypesAreSixChannelFiniteAndSeparated(){
        String[] channels=OperationalHazardTransferBootstrap.channels();
        double[] normal=OperationalHazardTransferBootstrap.normalPrototype();
        double[] hazard=OperationalHazardTransferBootstrap.hazard1Prototype();
        double[] normalSigma=OperationalHazardTransferBootstrap.normalSigma();
        double[] hazardSigma=OperationalHazardTransferBootstrap.hazard1Sigma();
        assertArrayEquals(new String[]{"MQ2","MQ4","MQ5","MQ7","MQ8","MQ135"},channels);
        assertEquals(6,normal.length);assertEquals(6,hazard.length);
        assertEquals(6,normalSigma.length);assertEquals(6,hazardSigma.length);
        double squaredDistance=0.0;
        for(int i=0;i<6;i++){
            assertTrue(Double.isFinite(normal[i]));assertTrue(Double.isFinite(hazard[i]));
            assertTrue(Double.isFinite(normalSigma[i])&&normalSigma[i]>=0.05);
            assertTrue(Double.isFinite(hazardSigma[i])&&hazardSigma[i]>=0.05);
            double d=normal[i]-hazard[i];squaredDistance+=d*d;
        }
        assertTrue(squaredDistance>0.25);
    }

    @Test public void callersCannotMutateStoredPrototypes(){
        double[] a=OperationalHazardTransferBootstrap.hazard1Prototype();
        double original=a[0];a[0]=999.0;
        assertEquals(original,OperationalHazardTransferBootstrap.hazard1Prototype()[0],0.0);
    }
}
