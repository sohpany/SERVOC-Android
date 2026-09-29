package com.servoc.app.fingerprint;

import org.junit.Test;

import static org.junit.Assert.*;

/** Scientific guard for the SJTU MQ-array 1-propanol transfer evidence. */
public class PropanolTransferEvidenceContractTest {
    private static final int PROPANOL_ID=104;

    @Test public void propanolHasNoFabricatedHr069NumericPrior(){
        assertFalse(ResearchIdentificationFingerprintRegistry.M4);
        assertFalse(ResearchIdentificationFingerprintRegistry.hasNumericPrior(PROPANOL_ID));
        assertEquals(0,ResearchIdentificationFingerprintRegistry.templateCountForChemical(PROPANOL_ID));
    }

    @Test public void propanolIsNotAccidentallyDeclaredAsNumericAnalyte(){
        for(int id:ResearchIdentificationFingerprintRegistry.NUMERIC_ANALYTES){
            assertNotEquals(PROPANOL_ID,id);
        }
    }
}
