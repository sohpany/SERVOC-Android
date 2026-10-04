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
        ChemCoreOpenSetGate.Result near=gate.evaluate(new double[]{0.02,0.01});
        ChemCoreOpenSetGate.Result far=gate.evaluate(new double[]{2.0,2.0});
        assertTrue(near.accepted);
        assertFalse(far.accepted);
        assertEquals(ChemCoreOpenSetGate.GateKind.EMPIRICAL_TRAINING_QUANTILE,near.gateKind);
        assertFalse(near.independentlyValidatedThreshold);
        assertTrue(near.reason.contains("gate_kind=EMPIRICAL_TRAINING_QUANTILE"));
    }

    @Test public void validatedThresholdCarriesExplicitIndependentValidationProvenance(){
        double[][] train={
                {0.0,0.0},{0.1,0.0},{-0.1,0.0},{0.0,0.1},{0.0,-0.1},
                {0.05,0.04},{-0.04,0.03},{0.03,-0.05}
        };
        ChemCoreOpenSetGate empirical=ChemCoreOpenSetGate.fit(train,0.05,0.95,1.5,1.0);
        ChemCoreOpenSetGate validated=ChemCoreOpenSetGate.fromValidatedThreshold(empirical.model,empirical.threshold,1.0);
        ChemCoreOpenSetGate.Result r=validated.evaluate(new double[]{0.02,0.01});
        assertTrue(r.accepted);
        assertEquals(ChemCoreOpenSetGate.GateKind.INDEPENDENT_VALIDATION_THRESHOLD,r.gateKind);
        assertTrue(r.independentlyValidatedThreshold);
        assertTrue(r.reason.contains("independently_validated_threshold=true"));
    }

    @Test public void insufficientCoverageFailsClosed(){
        double[][] train={{0,0,0},{.1,0,0},{0,.1,0},{0,0,.1},{-.1,0,0}};
        ChemCoreOpenSetGate gate=ChemCoreOpenSetGate.fit(train,0.05,0.95,1.5,0.8);
        ChemCoreOpenSetGate.Result r=gate.evaluate(new double[]{0.01,Double.NaN,Double.NaN});
        assertFalse(r.accepted);
        assertTrue(r.reason.contains("coverage"));
        assertEquals(ChemCoreOpenSetGate.GateKind.EMPIRICAL_TRAINING_QUANTILE,r.gateKind);
    }

    @Test public void explicitRuntimeResultRemainsTaggedAsExternalPolicy(){
        ChemCoreOpenSetGate.Result r=new ChemCoreOpenSetGate.Result(true,0.5,1.0,1.0,"runtime gate");
        assertTrue(r.accepted);
        assertEquals(ChemCoreOpenSetGate.GateKind.EXTERNAL_RUNTIME_POLICY,r.gateKind);
        assertFalse(r.independentlyValidatedThreshold);
    }

    @Test public void entropyIsLowForDecisiveAndHighForAmbiguousPosterior(){
        double decisive=ChemCoreOpenSetGate.normalizedEntropy(new double[]{0.98,0.01,0.01});
        double ambiguous=ChemCoreOpenSetGate.normalizedEntropy(new double[]{0.34,0.33,0.33});
        assertTrue(decisive<ambiguous);
        assertTrue(ambiguous>0.99);
    }
}
