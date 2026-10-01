package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.EnumMap;

import static org.junit.Assert.*;

public class ChemCoreIdentityCeilingMapTest {
    @Test public void all144EntriesReceiveExactlyOneFlexibleCeiling(){
        EnumMap<ChemCoreIdentityCeilingMap.Ceiling,Integer> counts=new EnumMap<>(ChemCoreIdentityCeilingMap.Ceiling.class);
        for(ChemCoreIdentityCeilingMap.Ceiling c:ChemCoreIdentityCeilingMap.Ceiling.values())counts.put(c,0);
        for(int i=0;i<144;i++){
            ChemCoreIdentityCeilingMap.Ceiling c=ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(i);
            assertNotNull(c);counts.put(c,counts.get(c)+1);
        }
        assertEquals(Integer.valueOf(4),counts.get(ChemCoreIdentityCeilingMap.Ceiling.PROVISIONAL_IDENTITY_CAPABLE));
        assertEquals(Integer.valueOf(106),counts.get(ChemCoreIdentityCeilingMap.Ceiling.CANDIDATE));
        assertEquals(Integer.valueOf(19),counts.get(ChemCoreIdentityCeilingMap.Ceiling.AFFINITY));
        assertEquals(Integer.valueOf(15),counts.get(ChemCoreIdentityCeilingMap.Ceiling.UNSUPPORTED));
        assertFalse(ChemCoreIdentityCeilingMap.isRuntimeGate());
    }

    @Test public void anchorsRemainProvisionalRatherThanClaimedValidated(){
        assertEquals(ChemCoreIdentityCeilingMap.Ceiling.PROVISIONAL_IDENTITY_CAPABLE,ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(21)); // NH3
        assertEquals(ChemCoreIdentityCeilingMap.Ceiling.PROVISIONAL_IDENTITY_CAPABLE,ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(75)); // H2
        assertEquals(ChemCoreIdentityCeilingMap.Ceiling.PROVISIONAL_IDENTITY_CAPABLE,ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(76)); // CH4
        assertEquals(ChemCoreIdentityCeilingMap.Ceiling.PROVISIONAL_IDENTITY_CAPABLE,ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(103)); // Ethanol
    }
}
