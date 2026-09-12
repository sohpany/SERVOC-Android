package com.servoc.app.chemical;

import com.servoc.app.analytics.AnalysisDecision;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AnalysisDecisionTest {
    private static ValidatedGasEngine.Result r(int chemical,double score,int contributors){
        double agreement=contributors>=2?80.0:0.0;
        double coverage=100.0*contributors/8.0;
        return new ValidatedGasEngine.Result(chemical,Double.NaN,agreement,coverage,75.0,score,contributors,contributors,0,0,0,0,contributors,contributors>=3?GasEngine.HIGH:GasEngine.STRONG,false,-1,"test");
    }

    @Test public void singleSensorEvidenceIsUnknown(){
        AnalysisDecision.Summary d=AnalysisDecision.evaluate(Arrays.asList(r(34,45,1)),8,0,true);
        assertEquals(AnalysisDecision.Outcome.UNKNOWN,d.outcome);assertFalse(d.quantitativePermitted);
    }

    @Test public void closeLeadingCandidatesAreAmbiguous(){
        AnalysisDecision.Summary d=AnalysisDecision.evaluate(Arrays.asList(r(75,72,4),r(76,67,4)),8,0,true);
        assertEquals(AnalysisDecision.Outcome.AMBIGUOUS,d.outcome);assertEquals(5.0,d.separationMargin,1e-9);
    }

    @Test public void strongSeparatedCandidatePassesScreeningGate(){
        AnalysisDecision.Summary d=AnalysisDecision.evaluate(Arrays.asList(r(75,82,5),r(76,58,4)),8,0,true);
        assertEquals(AnalysisDecision.Outcome.CANDIDATE,d.outcome);assertEquals(24.0,d.separationMargin,1e-9);assertEquals(75,d.topChemical);
    }

    @Test public void insufficientLiveChannelsForcesUnknown(){
        AnalysisDecision.Summary d=AnalysisDecision.evaluate(Arrays.asList(r(75,90,5)),1,0,true);
        assertEquals(AnalysisDecision.Outcome.UNKNOWN,d.outcome);
    }
}
