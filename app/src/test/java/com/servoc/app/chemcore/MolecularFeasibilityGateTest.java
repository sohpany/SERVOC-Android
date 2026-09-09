package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class MolecularFeasibilityGateTest {
    @Test public void rejectsConcentrationAboveSourceBackedVaporEnvelope(){
        MolecularFeasibilityGate.Result r=MolecularFeasibilityGate.evaluate(
                200_000,46.07,5.0,25,100.0,1.2);
        assertEquals(MolecularFeasibilityGate.Verdict.PHYSICALLY_IMPLAUSIBLE,r.verdict);
        assertEquals(50_000,r.saturationPpm,1e-9);
    }

    @Test public void doesNotClaimIdentityWhenPhysicalDataAreMissing(){
        MolecularFeasibilityGate.Result r=MolecularFeasibilityGate.evaluate(
                100,46.07,Double.NaN,25,101.325,1.25);
        assertEquals(MolecularFeasibilityGate.Verdict.INSUFFICIENT_DATA,r.verdict);
    }

    @Test public void plausibleCandidateOnlyPassesThePhysicsGate(){
        MolecularFeasibilityGate.Result r=MolecularFeasibilityGate.evaluate(
                100,46.07,5.0,25,101.325,1.25);
        assertEquals(MolecularFeasibilityGate.Verdict.SUPPORTED,r.verdict);
        assertTrue(r.reason.contains("not rejected"));
    }
}
