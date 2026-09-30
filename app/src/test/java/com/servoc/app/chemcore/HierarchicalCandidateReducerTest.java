package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class HierarchicalCandidateReducerTest {
    private static InverseMolecularSolver.Result result(long uid,double posterior){
        // Synthetic M2-only result: four direct/reference contributors, no QSPR and no M4 calibration.
        InverseMolecularSolver.Result r=new InverseMolecularSolver.Result(
                uid,"C"+uid,true,10.0,0.1,0.0,
                4,0,4,0,Double.NaN,Double.NaN,true,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                new HashMap<String,Double>(),"");
        r.posterior=posterior;return r;
    }

    @Test public void retainsHighestPosteriorFamiliesWithoutInventingTaxonomy(){
        List<InverseMolecularSolver.Result> compounds=Arrays.asList(
                result(1,0.40),result(2,0.25),result(3,0.20),result(4,0.10),result(5,0.05));
        List<HierarchicalCandidateReducer.FamilyAssignment> families=Arrays.asList(
                new HierarchicalCandidateReducer.FamilyAssignment(1,"alcohol","SRC"),
                new HierarchicalCandidateReducer.FamilyAssignment(2,"alcohol","SRC"),
                new HierarchicalCandidateReducer.FamilyAssignment(3,"ketone","SRC"),
                new HierarchicalCandidateReducer.FamilyAssignment(4,"acid","SRC"));
        HierarchicalCandidateReducer.Result r=HierarchicalCandidateReducer.reduce(compounds,families,0.80,3);
        assertEquals(HierarchicalCandidateReducer.PROTOCOL,r.protocol);
        assertEquals("ALCOHOL",r.families.get(0).family);
        assertEquals(0.65,r.families.get(0).posteriorMass,1e-12);
        assertTrue(r.retainedFamilies.contains("ALCOHOL"));
        assertTrue(r.retainedFamilies.contains("KETONE"));
        assertFalse(r.retainedFamilies.contains("ACID"));
        assertEquals(3,r.retainedCompounds.size());
    }

    @Test public void unassignedCompoundsRemainExplicit(){
        List<InverseMolecularSolver.Result> compounds=Arrays.asList(result(10,0.7),result(11,0.3));
        HierarchicalCandidateReducer.Result r=HierarchicalCandidateReducer.reduce(compounds,null,0.9,2);
        assertEquals(1,r.families.size());
        assertEquals("UNCLASSIFIED",r.families.get(0).family);
        assertEquals(2,r.retainedCompounds.size());
    }
}
