package com.servoc.app.fingerprint;

import org.junit.Test;

import static org.junit.Assert.*;

/** Scientific guard: external MQ3 ethylene evidence is transfer/reference evidence, not M4. */
public class EthyleneTransferEvidenceContractTest {
    private static final int ETHYLENE_ID=89;

    @Test public void ethyleneRemainsTransferOnlyUntilNumericMq3ResponseOrHr069CalibrationExists(){
        assertFalse(ResearchIdentificationFingerprintRegistry.M4);
        assertFalse(ResearchIdentificationFingerprintRegistry.hasNumericPrior(ETHYLENE_ID));
        assertEquals(0,ResearchIdentificationFingerprintRegistry.templateCountForChemical(ETHYLENE_ID));

        ResearchIdentificationFingerprintRegistry.TargetStatus ethylene=null;
        for(ResearchIdentificationFingerprintRegistry.TargetStatus s:ResearchIdentificationFingerprintRegistry.foodSpoilageTargets()){
            if(s.chemicalId==ETHYLENE_ID){ethylene=s;break;}
        }
        assertNotNull(ethylene);
        assertEquals(ResearchIdentificationFingerprintRegistry.TRANSFER_DATA_AVAILABLE,ethylene.status);
        assertFalse(ethylene.numericFingerprintAvailable);
        assertTrue(ethylene.note.contains("MQ3"));
        assertTrue(ethylene.note.contains("10/25/40/50 ppm"));
        assertTrue(ethylene.note.contains("0/33/64% RH"));
    }

    @Test public void ethyleneIsNotAccidentallyDeclaredAsNumericAnalyte(){
        for(int id:ResearchIdentificationFingerprintRegistry.NUMERIC_ANALYTES){
            assertNotEquals(ETHYLENE_ID,id);
        }
    }
}
