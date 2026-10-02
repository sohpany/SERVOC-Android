package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class ChemCoreDomainGatingTest {
    private static ChemCoreResponseModel model(String sensor){
        return new PowerLawResponseModel(sensor,"domain-test",2.0,-0.40,5,500,0.20,
                10,40,10,90,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"synthetic domain test");
    }

    private static InverseMolecularSolver.Candidate candidate(){
        return new InverseMolecularSolver.Candidate(1,"Test chemical",List.of(model("mq2"),model("mq3")),0,2);
    }

    private static Map<String,Double> observed(double ppm){
        double r=2.0*Math.pow(ppm,-0.40);
        return Map.of("mq2",r,"mq3",r);
    }

    private static ChemCoreOpenSetGate.Result acceptedGate(){
        return new ChemCoreOpenSetGate.Result(true,0.2,1.0,1.0,"synthetic accepted domain");
    }

    @Test public void missingRequiredEnvironmentKeepsCandidateVisibleButWithholdsIdentityAndPpm(){
        ChemCoreInferenceEngine.Context ctx=new ChemCoreInferenceEngine.Context(
                "domain-missing","prototype","test",Double.NaN,50,acceptedGate(),true);
        ChemCoreResultEnvelope r=ChemCoreInferenceEngine.analyze(observed(40),List.of(candidate()),
                SamplingProfile.hazRobPrototypeV1(),ctx);
        assertEquals(ChemCoreDecision.Outcome.SCREENING,r.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,r.evidenceTier);
        assertNotNull(r.chemicalUid);
        assertTrue(Double.isNaN(r.ppm));
        assertFalse(ChemCoreDecision.permitsNamedIdentity(r.outcome));
        assertTrue(r.note.contains("OUT_OF_DOMAIN diagnostic"));
    }

    @Test public void measuredEnvironmentInsideDomainAllowsM3IdentityButNotPpm(){
        ChemCoreInferenceEngine.Context ctx=new ChemCoreInferenceEngine.Context(
                "domain-valid","prototype","test",25,50,acceptedGate(),true);
        ChemCoreResultEnvelope r=ChemCoreInferenceEngine.analyze(observed(40),List.of(candidate()),
                SamplingProfile.hazRobPrototypeV1(),ctx);
        assertEquals(ChemCoreDecision.Outcome.IDENTIFIED,r.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,r.evidenceTier);
        assertTrue(Double.isNaN(r.ppm));
    }
}
