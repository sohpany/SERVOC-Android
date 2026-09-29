package com.servoc.app.fingerprint;

import org.junit.Test;

import static org.junit.Assert.*;

public class MethanolTransferEvidenceContractTest {
    private static final int METHANOL_ID=102;

    @Test public void methanolRemainsTransferOnlyUntilRawOrHr069VectorExists(){
        assertFalse(ResearchIdentificationFingerprintRegistry.M4);
        assertFalse(ResearchIdentificationFingerprintRegistry.hasNumericPrior(METHANOL_ID));
        assertEquals(0,ResearchIdentificationFingerprintRegistry.templateCountForChemical(METHANOL_ID));

        ResearchIdentificationFingerprintRegistry.TargetStatus methanol=null;
        for(ResearchIdentificationFingerprintRegistry.TargetStatus s:ResearchIdentificationFingerprintRegistry.foodSpoilageTargets()){
            if(s.chemicalId==METHANOL_ID){methanol=s;break;}
        }
        assertNotNull(methanol);
        assertEquals(ResearchIdentificationFingerprintRegistry.TRANSFER_DATA_AVAILABLE,methanol.status);
        assertFalse(methanol.numericFingerprintAvailable);
        assertTrue(methanol.note.contains("all eight HR069 MQ models"));
        assertTrue(methanol.note.contains("Raw 11-channel time series"));
    }

    @Test public void methanolIsNotAccidentallyAddedToDeclaredNumericAnalytes(){
        for(int id:ResearchIdentificationFingerprintRegistry.NUMERIC_ANALYTES){
            assertNotEquals(METHANOL_ID,id);
        }
    }
}
