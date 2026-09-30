package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.*;

public class MixtureResidualGuardTest {
    private static InverseMolecularSolver.Result r(String label,double posterior,double residual){
        // Synthetic fully M4-supported result: four direct/reference contributors, all four device validated.
        InverseMolecularSolver.Result x=new InverseMolecularSolver.Result(
                1,label,true,50,residual,0,
                4,0,4,4,Double.NaN,Double.NaN,true,
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,
                new HashMap<String,Double>(),"");
        x.posterior=posterior;return x;
    }
    @Test public void largeResidualFlagsPossibleMixture(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluate(List.of(r("A",.95,3.2),r("B",.05,1.2)),2.5,.8,.25);
        assertEquals(MixtureResidualGuard.Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE,x.verdict);
    }
    @Test public void smallPosteriorMarginIsAmbiguousNotMixture(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluate(List.of(r("A",.55,1.0),r("B",.42,1.1)),2.5,.8,.25);
        assertEquals(MixtureResidualGuard.Verdict.AMBIGUOUS_IDENTITY,x.verdict);
    }
    @Test public void decisiveLowResidualCandidatePasses(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluate(List.of(r("A",.94,1.0),r("B",.04,1.3)),2.5,.8,.25);
        assertEquals(MixtureResidualGuard.Verdict.SINGLE_CANDIDATE_CONSISTENT,x.verdict);
    }
}
