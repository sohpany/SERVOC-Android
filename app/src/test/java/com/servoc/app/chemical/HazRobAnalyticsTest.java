package com.servoc.app.chemical;

import com.servoc.app.calibration.EmpiricalMqCurve;
import org.junit.Test;

import static org.junit.Assert.*;

public class HazRobAnalyticsTest {
    @Test public void chemicalCatalogHas144Entries(){
        assertEquals(144,ChemicalCatalog.COUNT);
        assertEquals(ChemicalCatalog.NAMES.length,ChemicalCatalog.FORMULAS.length);
    }

    @Test public void allCatalogueEntriesHaveCoverageState(){
        for(int i=0;i<ChemicalCatalog.COUNT;i++){
            ChemicalCoverage.Status s=ChemicalCoverage.forChemical(i);
            assertNotNull("coverage index "+i,s);
            assertNotNull(s.method);
            assertNotNull(s.note);
            MqSensorAffinity.Plan p=MqSensorAffinity.forChemical(i);
            assertNotNull("affinity index "+i,p);
            assertTrue("invalid mask at index "+i,p.mask>=0 && p.mask<=MqSensorAffinity.ALL);
        }
    }

    @Test public void empiricalCurveInterpolatesButDoesNotExtrapolate(){
        EmpiricalMqCurve c=new EmpiricalMqCurve(
                GasEngine.MQ8,75,
                new double[]{100,300,900},
                new double[]{1.0,0.50,0.25},
                1L,"reference H2","unit test");
        EmpiricalMqCurve.Estimate mid=c.estimatePpm(0.50);
        assertTrue(mid.valid());
        assertEquals(300.0,mid.ppm,1e-9);
        assertFalse(c.estimatePpm(1.2).valid());
        assertEquals(EmpiricalMqCurve.OUTSIDE_CALIBRATION_RANGE,c.estimatePpm(1.2).status);
        assertFalse(c.estimatePpm(0.20).valid());
    }

    @Test(expected=IllegalArgumentException.class)
    public void empiricalCurveRejectsNonMonotonicResponse(){
        new EmpiricalMqCurve(
                GasEngine.MQ135,21,
                new double[]{10,50,200},
                new double[]{2.0,1.0,1.4},
                1L,"reference NH3","unit test");
    }

    @Test public void targetBackedCurvesAreDirectAndResearchOnlyCurvesAreNot(){
        assertTrue(GasEngine.curveCount()>0);
        int direct=0,research=0;
        for(int chem:GasEngine.SUPPORTED){
            assertTrue(chem>=0 && chem<ChemicalCatalog.COUNT);
            if(CurveEligibility.hasTargetBackedCurve(chem)){
                assertEquals(ChemicalCoverage.DIRECT_CURVE,ChemicalCoverage.forChemical(chem).level);
                direct++;
            }else{
                assertNotEquals(ChemicalCoverage.DIRECT_CURVE,ChemicalCoverage.forChemical(chem).level);
                research++;
            }
        }
        assertTrue("expected target-backed curves",direct>0);
        assertTrue("expected research-only inherited curves",research>0);
    }

    @Test public void manufacturerEvidenceSeparatesTargetsFromCrossResponses(){
        assertEquals(ManufacturerEvidenceMatrix.TARGET,
                ManufacturerEvidenceMatrix.level(GasEngine.MQ7,20)); // CO
        assertEquals(ManufacturerEvidenceMatrix.GRAPH_RESPONSE,
                ManufacturerEvidenceMatrix.level(GasEngine.MQ7,75)); // H2
        assertEquals(ManufacturerEvidenceMatrix.TARGET,
                ManufacturerEvidenceMatrix.level(GasEngine.MQ8,75)); // H2
    }

    @Test public void unsupportedOxidizerDoesNotReceiveTrainingMask(){
        // Chlorine dioxide catalogue index 10 is intentionally compatibility-review only.
        assertEquals(0,MqSensorAffinity.forChemical(10).mask);
    }
}
