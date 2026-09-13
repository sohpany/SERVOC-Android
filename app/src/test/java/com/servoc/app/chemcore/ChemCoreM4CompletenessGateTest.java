package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChemCoreM4CompletenessGateTest {
    private static PowerLawResponseModel model(String sensor,ChemCoreDecision.EvidenceTier tier,double a,double b){
        return new PowerLawResponseModel(sensor,"test",a,b,10,100,0.05,Double.NaN,Double.NaN,Double.NaN,Double.NaN,tier,"unit-test curve");
    }
    private static Map<String,Double> observed(){
        double ppm=50;Map<String,Double> x=new HashMap<>();x.put("mq2",10*Math.pow(ppm,-0.5));x.put("mq4",5*Math.pow(ppm,-0.4));return x;
    }
    private static ChemCoreInferenceEngine.Context context(){
        return new ChemCoreInferenceEngine.Context("S","P","M",25,40,new ChemCoreOpenSetGate.Result(true,0.2,1.0,1.0,"accepted"));
    }

    @Test public void oneM4PlusOneLiteratureCurveCannotQuantifyJointFit(){
        InverseMolecularSolver.Candidate c=new InverseMolecularSolver.Candidate(1,"X",Arrays.asList(
                model("mq2",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,10,-0.5),
                model("mq4",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,5,-0.4)),0,2);
        ChemCoreResultEnvelope r=ChemCoreInferenceEngine.analyze(observed(),Collections.singletonList(c),null,context());
        assertEquals(ChemCoreDecision.Outcome.IDENTIFIED,r.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,r.evidenceTier);
        assertFalse(Double.isFinite(r.ppm));
        assertEquals(1,r.candidates.get(0).calibratedContributors);
        assertEquals(2,r.candidates.get(0).contributors);
    }

    @Test public void allContributingM4CurvesCanQuantifyInsideAcceptedDomain(){
        InverseMolecularSolver.Candidate c=new InverseMolecularSolver.Candidate(1,"X",Arrays.asList(
                model("mq2",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,10,-0.5),
                model("mq4",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,5,-0.4)),0,2);
        ChemCoreResultEnvelope r=ChemCoreInferenceEngine.analyze(observed(),Collections.singletonList(c),null,context());
        assertEquals(ChemCoreDecision.Outcome.QUANTIFIED,r.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,r.evidenceTier);
        assertTrue(Double.isFinite(r.ppm));
        assertEquals(2,r.candidates.get(0).calibratedContributors);
        assertTrue(r.candidates.get(0).allContributorsInsideDomain);
    }
}
