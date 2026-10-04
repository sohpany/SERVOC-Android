package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreLegacyCandidateRegistryTest {
    @Test public void qsprDatabaseUidOverridesHistoricalLegacyOffsetForSameChemical(){
        InverseMolecularSolver.Candidate databaseBacked=new InverseMolecularSolver.Candidate(
                9104L,"Ethanol",Collections.emptyList(),0.0,1);
        long uid=ChemCoreLegacyCandidateRegistry.resolveUidForLegacyChemical(103,List.of(databaseBacked));
        assertEquals(9104L,uid);
    }

    @Test public void compatibilityOffsetRemainsOnlyWhenNoDatabaseBackedCandidateExists(){
        long uid=ChemCoreLegacyCandidateRegistry.resolveUidForLegacyChemical(103,Collections.emptyList());
        assertEquals(104L,uid);
    }

    @Test public void conflictingDatabaseUidsForSameNormalizedLabelFailClosed(){
        InverseMolecularSolver.Candidate a=new InverseMolecularSolver.Candidate(9104L,"Ethanol",Collections.emptyList(),0.0,1);
        InverseMolecularSolver.Candidate b=new InverseMolecularSolver.Candidate(9204L,"  ETHANOL  ",Collections.emptyList(),0.0,1);
        long uid=ChemCoreLegacyCandidateRegistry.resolveUidForLegacyChemical(103,List.of(a,b));
        assertEquals(-1L,uid);
    }

    @Test public void invalidLegacyIdFailsClosed(){
        assertEquals(-1L,ChemCoreLegacyCandidateRegistry.resolveUidForLegacyChemical(-1,Collections.emptyList()));
        assertEquals(-1L,ChemCoreLegacyCandidateRegistry.resolveUidForLegacyChemical(Integer.MAX_VALUE,Collections.emptyList()));
    }
}
