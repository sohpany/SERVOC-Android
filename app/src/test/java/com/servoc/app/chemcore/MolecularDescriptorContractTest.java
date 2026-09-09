package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MolecularDescriptorContractTest {
    @Test public void completeCoreVectorIsEligibleWithoutOptionalDescriptors() {
        Map<String,Double> values=new LinkedHashMap<>();
        double v=1;
        for(String key:MolecularDescriptorContract.CORE)values.put(key,v++);
        MolecularDescriptorContract.Vector x=MolecularDescriptorContract.from(values);
        assertTrue(x.coreComplete());
        assertEquals(1.0,x.coreCoverage,1e-12);
        assertEquals(0.0,x.optionalCoverage,1e-12);
    }

    @Test public void missingAndNonFiniteCoreValuesRemainExplicit() {
        Map<String,Double> values=new LinkedHashMap<>();
        for(String key:MolecularDescriptorContract.CORE)values.put(key,1.0);
        values.remove("TPSA");
        values.put("Complexity",Double.NaN);
        MolecularDescriptorContract.Vector x=MolecularDescriptorContract.from(values);
        assertFalse(x.coreComplete());
        assertTrue(x.missingCore.contains("TPSA"));
        assertTrue(x.missingCore.contains("Complexity"));
        assertEquals(6.0/8.0,x.coreCoverage,1e-12);
    }
}
