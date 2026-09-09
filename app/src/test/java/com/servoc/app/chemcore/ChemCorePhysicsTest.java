package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCorePhysicsTest {
    @Test public void ppmAndMgM3RoundTripAtAmbientConditions(){
        double ppm=50.0,mw=46.06844,t=25.0,p=101.325;
        double mg=ChemCorePhysics.ppmToMgM3(ppm,mw,t,p);
        assertTrue(mg>0);
        assertEquals(ppm,ChemCorePhysics.mgM3ToPpm(mg,mw,t,p),1e-9);
    }

    @Test public void idealGasDensityAndRelativeDensityArePlausible(){
        double rho=ChemCorePhysics.idealGasDensityKgM3(44.0095,25,101.325);
        assertTrue(rho>1.7&&rho<1.9);
        assertEquals(44.0095/ChemCorePhysics.AIR_MOLAR_MASS_G_PER_MOL,
                ChemCorePhysics.relativeVaporDensityToAir(44.0095),1e-12);
    }

    @Test public void saturationPpmCannotExceedMillion(){
        assertEquals(100000.0,ChemCorePhysics.saturationPpm(10.0,100.0),1e-9);
        assertEquals(1_000_000.0,ChemCorePhysics.saturationPpm(150.0,100.0),1e-9);
    }

    @Test public void invalidInputsFailClosed(){
        assertTrue(Double.isNaN(ChemCorePhysics.idealGasDensityKgM3(-1,25,101.3)));
        assertTrue(Double.isNaN(ChemCorePhysics.ppmToMgM3(10,0,25,101.3)));
        assertTrue(Double.isNaN(ChemCorePhysics.saturationPpm(1,0)));
    }
}
