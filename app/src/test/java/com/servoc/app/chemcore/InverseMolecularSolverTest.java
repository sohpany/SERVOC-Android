package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class InverseMolecularSolverTest {
    private static ChemCoreResponseModel p(String sensor,double a,double b,ChemCoreDecision.EvidenceTier tier){
        return new PowerLawResponseModel(sensor,"test",a,b,5,500,.05,10,40,10,90,tier,"synthetic test");
    }

    @Test public void jointArrayFitRecoversConsistentCandidateAndConcentration(){
        double ppm=40;
        List<ChemCoreResponseModel> a=List.of(p("mq2",2.0,-.45,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED),p("mq3",1.5,-.30,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED),p("mq135",3.0,-.55,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED));
        List<ChemCoreResponseModel> b=List.of(p("mq2",.7,-.15,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED),p("mq3",4.0,-.75,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED),p("mq135",.8,-.10,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED));
        Map<String,Double> observed=Map.of(
                "mq2",2.0*Math.pow(ppm,-.45),
                "mq3",1.5*Math.pow(ppm,-.30),
                "mq135",3.0*Math.pow(ppm,-.55));
        List<InverseMolecularSolver.Result> r=InverseMolecularSolver.solve(observed,List.of(
                new InverseMolecularSolver.Candidate(1,"A",a,0,2),
                new InverseMolecularSolver.Candidate(2,"B",b,0,2)),25,50,SamplingProfile.hazRobPrototypeV1());
        assertEquals(2,r.size());
        assertEquals("A",r.get(0).label);
        assertEquals(ppm,r.get(0).bestPpm,0.5);
        assertTrue(r.get(0).posterior>r.get(1).posterior);
        // One M4 contributor does not validate a 3-sensor joint quantitative fit.
        assertFalse(r.get(0).hasDeviceValidatedQuantitativeSupport());
        assertEquals(1,r.get(0).calibratedContributors);
        assertEquals(3,r.get(0).contributors);
    }

    @Test public void insufficientOverlappingChannelsFailsClosed(){
        InverseMolecularSolver.Candidate c=new InverseMolecularSolver.Candidate(1,"A",List.of(p("mq2",2,-.4,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED)),0,2);
        InverseMolecularSolver.Result r=InverseMolecularSolver.solveOne(Map.of("mq2",.5),c,25,50,SamplingProfile.hazRobPrototypeV1());
        assertFalse(r.solvable);
    }
}
