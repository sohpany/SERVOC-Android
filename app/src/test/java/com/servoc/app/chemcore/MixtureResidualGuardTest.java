package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.*;

public class MixtureResidualGuardTest {
    private static InverseMolecularSolver.Result r(String label,double rankingWeight,double residual){
        // Synthetic fully M4-supported result: four direct/reference contributors, all four device validated.
        InverseMolecularSolver.Result x=new InverseMolecularSolver.Result(
                1,label,true,50,residual,0,
                4,0,4,4,Double.NaN,Double.NaN,true,
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,
                new HashMap<String,Double>(),"");
        x.posterior=rankingWeight;return x;
    }

    @Test public void largeResidualFlagsPossibleMixtureButDoesNotClaimComponents(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluateDefault(List.of(r("A",.95,3.2),r("B",.05,1.2)));
        assertEquals(MixtureResidualGuard.Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE,x.verdict);
        assertTrue(x.reason.contains("not component identification"));
    }

    @Test public void smallRankingWeightMarginIsAmbiguousNotMixture(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluateDefault(List.of(r("A",.55,1.0),r("B",.42,1.1)));
        assertEquals(MixtureResidualGuard.Verdict.AMBIGUOUS_IDENTITY,x.verdict);
        assertTrue(x.reason.contains("not calibrated presence probabilities"));
    }

    @Test public void decisiveLowResidualCandidatePassesResearchPolicy(){
        MixtureResidualGuard.Result x=MixtureResidualGuard.evaluateDefault(List.of(r("A",.94,1.0),r("B",.04,1.3)));
        assertEquals(MixtureResidualGuard.Verdict.SINGLE_CANDIDATE_CONSISTENT,x.verdict);
        assertEquals(.94,x.topRankingWeight(),0.0);
        assertEquals(.04,x.secondRankingWeight(),0.0);
    }

    @Test public void defaultThresholdsAreFrozenResearchPolicyNotProbabilities(){
        assertEquals(2.50,MixtureResidualGuard.DEFAULT_MAX_STANDARDIZED_RESIDUAL_RMS,0.0);
        assertEquals(.80,MixtureResidualGuard.DEFAULT_MIN_TOP_RANKING_WEIGHT,0.0);
        assertEquals(.25,MixtureResidualGuard.DEFAULT_MIN_RANKING_WEIGHT_MARGIN,0.0);
        assertEquals("research_policy_thresholds_not_calibrated_presence_probabilities",MixtureResidualGuard.THRESHOLD_SEMANTICS);
        assertEquals("relative_softmax_model_weight_not_calibrated_probability",InverseMolecularSolver.RANKING_SEMANTICS);
    }
}
