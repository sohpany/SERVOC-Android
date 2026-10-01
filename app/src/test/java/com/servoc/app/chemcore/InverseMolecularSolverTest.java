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

    @Test public void equivalentPerChannelFitIsNotPenalizedByContributorCount(){
        double ppm=40.0;
        double predicted=2.0*Math.pow(ppm,-.40);
        // PowerLawResponseModel sigmaLog10 is 0.05 in helper p(). These observations create
        // symmetric +/-1 sigma residuals that cannot be removed by a common concentration shift.
        double up=predicted*Math.pow(10.0,0.05);
        double down=predicted/Math.pow(10.0,0.05);
        Map<String,Double> observed=Map.of("mq2",up,"mq3",down,"mq4",up,"mq5",down);
        ChemCoreDecision.EvidenceTier m2=ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;
        InverseMolecularSolver.Candidate two=new InverseMolecularSolver.Candidate(1,"two-channel",List.of(
                p("mq2",2.0,-.40,m2),p("mq3",2.0,-.40,m2)),0,2);
        InverseMolecularSolver.Candidate four=new InverseMolecularSolver.Candidate(2,"four-channel",List.of(
                p("mq2",2.0,-.40,m2),p("mq3",2.0,-.40,m2),p("mq4",2.0,-.40,m2),p("mq5",2.0,-.40,m2)),0,2);

        InverseMolecularSolver.Result a=InverseMolecularSolver.solveOne(observed,two,25,50,SamplingProfile.hazRobPrototypeV1());
        InverseMolecularSolver.Result b=InverseMolecularSolver.solveOne(observed,four,25,50,SamplingProfile.hazRobPrototypeV1());
        assertTrue(a.solvable);assertTrue(b.solvable);
        assertEquals(a.standardizedResidualRms,b.standardizedResidualRms,1e-6);
        assertEquals(a.logScore,b.logScore,1e-6);

        List<InverseMolecularSolver.Result> ranked=InverseMolecularSolver.solve(observed,List.of(two,four),25,50,SamplingProfile.hazRobPrototypeV1());
        assertEquals(2,ranked.size());
        assertEquals(1.0,ranked.get(0).posterior+ranked.get(1).posterior,1e-12);
        assertEquals(ranked.get(0).posterior,ranked.get(1).posterior,1e-6);
        assertEquals("relative_softmax_model_weight_not_calibrated_probability",InverseMolecularSolver.RANKING_SEMANTICS);
    }
}
