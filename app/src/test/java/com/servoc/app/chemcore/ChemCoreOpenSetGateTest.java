package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCoreOpenSetGateTest {
    @Test public void compactTrainingDomainAcceptsNearPointAndRejectsFarPoint(){
        double[][] train={
                {0.0,0.0},{0.1,0.0},{-0.1,0.0},{0.0,0.1},{0.0,-0.1},
                {0.05,0.04},{-0.04,0.03},{0.03,-0.05}
        };
        ChemCoreOpenSetGate gate=ChemCoreOpenSetGate.fit(train,0.05,0.95,1.5,1.0);
        assertTrue(gate.evaluate(new double[]{0.02,0.01}).accepted);
        assertFalse(gate.evaluate(new double[]{2.0,2.0}).accepted);
    }

    @Test public void insufficientCoverageFailsClosed(){
        double[][] train={{0,0,0},{.1,0,0},{0,.1,0},{0,0,.1},{-.1,0,0}};
        ChemCoreOpenSetGate gate=ChemCoreOpenSetGate.fit(train,0.05,0.95,1.5,0.8);
        ChemCoreOpenSetGate.Result r=gate.evaluate(new double[]{0.01,Double.NaN,Double.NaN});
        assertFalse(r.accepted);
        assertTrue(r.reason.contains("coverage"));
    }

    @Test public void entropyIsLowForDecisiveAndHighForAmbiguousPosterior(){
        double decisive=ChemCoreOpenSetGate.normalizedEntropy(new double[]{0.98,0.01,0.01});
        double ambiguous=ChemCoreOpenSetGate.normalizedEntropy(new double[]{0.34,0.33,0.33});
        assertTrue(decisive<ambiguous);
        assertTrue(ambiguous>0.99);
    }
}
