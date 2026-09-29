package com.servoc.app.chemical;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalEvidenceCoverageTest {
    @Test public void newExternalEvidenceReducesDiscoveryOnlyWhereVerified(){
        assertEquals(ExternalEvidenceCoverage.SAME_ARRAY_FINGERPRINT,ExternalEvidenceCoverage.bestLevel(102)); // methanol
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(105)); // isopropanol
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(115)); // ethyl acetate
        assertEquals(ExternalEvidenceCoverage.MANUFACTURER_QUALITATIVE_TARGET,ExternalEvidenceCoverage.bestLevel(79)); // n-butane
        assertEquals(ExternalEvidenceCoverage.NONE,ExternalEvidenceCoverage.bestLevel(14)); // NO2 remains unsupported by this registry
    }

    @Test public void noExternalRecordCanClaimM4(){
        assertFalse(ExternalEvidenceCoverage.anyExternalM4());
        for(ExternalEvidenceCoverage.Evidence e:ExternalEvidenceCoverage.RECORDS)assertFalse(e.m4);
    }

    @Test public void coveragePlannerSeparatesTransferValidationFromDiscovery(){
        ChemicalCoverage.Status iso=ChemicalCoverage.forChemical(105);
        assertEquals(ChemicalCoverage.FINGERPRINT_CANDIDATE,iso.level);
        assertTrue(iso.method.contains("External evidence"));
        assertTrue(iso.note.contains("transfer"));
        ChemicalCoverage.Status no2=ChemicalCoverage.forChemical(14);
        assertEquals(ChemicalCoverage.UNVALIDATED_OR_WEAK,no2.level);
        assertTrue(ChemicalCoverage.externalEvidenceChemicalCount()>0);
        assertTrue(ChemicalCoverage.externalKnownConcentrationOrBetterCount()>0);
        assertTrue(ChemicalCoverage.labDiscoveryCandidateCount()>0);
    }
}
