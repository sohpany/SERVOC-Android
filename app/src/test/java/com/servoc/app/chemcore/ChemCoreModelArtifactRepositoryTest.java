package com.servoc.app.chemcore;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreModelArtifactRepositoryTest {
    @Test public void sha256IsDeterministic(){
        assertEquals(
                "015abd7f5cc57a2dd94b7590f04ad8084273905ee33ec5cebeae62276a97f862",
                ChemCoreModelArtifactRepository.sha256("{\"a\":1}"));
        assertEquals(ChemCoreModelArtifactRepository.sha256("{\"a\":1}"),
                ChemCoreModelArtifactRepository.sha256("{\"a\":1}"));
        assertNotEquals(ChemCoreModelArtifactRepository.sha256("{\"a\":1}"),
                ChemCoreModelArtifactRepository.sha256("{\"a\":2}"));
    }
}
