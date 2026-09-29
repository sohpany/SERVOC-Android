package com.servoc.app.chemical;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalEvidenceCoverageTest {
    @Test public void newExternalEvidenceReducesDiscoveryOnlyWhereVerified(){
        assertEquals(ExternalEvidenceCoverage.SAME_ARRAY_FINGERPRINT,ExternalEvidenceCoverage.bestLevel(102)); // methanol
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(54)); // acetaldehyde
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(89)); // ethylene
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(105)); // isopropanol
        assertEquals(ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE,ExternalEvidenceCoverage.bestLevel(115)); // ethyl acetate
        assertEquals(ExternalEvidenceCoverage.MANUFACTURER_QUALITATIVE_TARGET,ExternalEvidenceCoverage.bestLevel(79)); // n-butane
        assertEquals(ExternalEvidenceCoverage.SAME_ARRAY_FINGERPRINT,ExternalEvidenceCoverage.bestLevel(139)); // smoke canonical public dataset
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
        ChemicalCoverage.Status acetaldehyde=ChemicalCoverage.forChemical(54);
        assertEquals(ChemicalCoverage.FINGERPRINT_CANDIDATE,acetaldehyde.level);
        assertTrue(acetaldehyde.method.contains("External evidence"));
        ChemicalCoverage.Status ethylene=ChemicalCoverage.forChemical(89);
        assertEquals(ChemicalCoverage.FINGERPRINT_CANDIDATE,ethylene.level);
        assertTrue(ethylene.method.contains("External evidence"));
        ChemicalCoverage.Status smoke=ChemicalCoverage.forChemical(139);
        assertEquals(ChemicalCoverage.AGGREGATE,smoke.level);
        assertTrue(smoke.note.contains("MULTIMODALGASDATA_2022"));
        ChemicalCoverage.Status no2=ChemicalCoverage.forChemical(14);
        assertEquals(ChemicalCoverage.UNVALIDATED_OR_WEAK,no2.level);

        // Locked HR069 planning counts for the 144-entry legacy catalogue after registering
        // verified methanol/ethylene/acetaldehyde transfer evidence on 2026-09-29.
        assertEquals(8,ChemicalCoverage.directCurveCount());
        assertEquals(94,ChemicalCoverage.fingerprintCandidateCount());
        assertEquals(6,ChemicalCoverage.aggregateCount());
        assertEquals(36,ChemicalCoverage.weakOrUnvalidatedCount());
        assertEquals(14,ChemicalCoverage.externalEvidenceChemicalCount());
        assertEquals(11,ChemicalCoverage.externalKnownConcentrationOrBetterCount());
        assertEquals(9,ChemicalCoverage.transferValidationCandidateCount());
        assertEquals(4,ChemicalCoverage.genericCurveValidationCandidateCount());
        assertEquals(3,ChemicalCoverage.corroboratedTransferCandidateCount());
        assertEquals(84,ChemicalCoverage.labDiscoveryCandidateCount());
    }
}
