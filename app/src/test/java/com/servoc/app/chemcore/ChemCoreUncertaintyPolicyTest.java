package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCoreUncertaintyPolicyTest {
    @Test public void proxyValuesPreserveHr069ResearchBehavior(){
        assertEquals(0.20,ChemCoreUncertaintyPolicy.LITERATURE_POWER_SIGMA_LOG10,0.0);
        assertEquals(0.25,ChemCoreUncertaintyPolicy.LITERATURE_LINEAR_SIGMA_LOG10,0.0);
        assertEquals(0.22,ChemCoreUncertaintyPolicy.MANUFACTURER_PRIMARY_SIGMA_LOG10,0.0);
        assertEquals(0.27,ChemCoreUncertaintyPolicy.MANUFACTURER_SECONDARY_SIGMA_LOG10,0.0);
        assertEquals(0.32,ChemCoreUncertaintyPolicy.RESEARCH_CURVE_SIGMA_LOG10,0.0);
        assertEquals(0.20,ChemCoreUncertaintyPolicy.SOLVER_FALLBACK_SIGMA_LOG10,0.0);
        assertEquals(0.02,ChemCoreUncertaintyPolicy.MIN_SIGMA_LOG10,0.0);
        assertEquals(1.80,ChemCoreUncertaintyPolicy.OUT_OF_DOMAIN_SIGMA_MULTIPLIER,0.0);
    }

    @Test public void solverSigmaPreservesFloorFallbackAndDomainWidening(){
        assertEquals(0.20,ChemCoreUncertaintyPolicy.solverSigma(Double.NaN,true),1e-12);
        assertEquals(0.20,ChemCoreUncertaintyPolicy.solverSigma(0.20,true),1e-12);
        assertEquals(0.02,ChemCoreUncertaintyPolicy.solverSigma(0.001,true),1e-12);
        assertEquals(0.36,ChemCoreUncertaintyPolicy.solverSigma(0.20,false),1e-12);
    }

    @Test public void rSquaredCannotManufactureResponseUncertainty(){
        assertFalse(ChemCoreUncertaintyPolicy.acceptsR2AsSigmaEvidence());
        assertTrue(ChemCoreUncertaintyPolicy.SEMANTICS.contains("not_calibrated_prediction_interval"));
    }
}
