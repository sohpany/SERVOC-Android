package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class AmciDecisionEngineTest {
    @Test public void decisiveInDomainEvidenceIsAccepted(){
        AmciDecisionEngine.Observation o=new AmciDecisionEngine.Observation(true,true,true,true,true,.96,.20,.95,.001,45,0);
        assertEquals(AmciDecisionEngine.Action.ACCEPT_RESULT,AmciDecisionEngine.decide(o,AmciDecisionEngine.Policy.researchV1()).action);
    }

    @Test public void unstableTraceRequestsMoreObservation(){
        AmciDecisionEngine.Observation o=new AmciDecisionEngine.Observation(true,true,true,true,true,.70,.70,.95,.01,30,0);
        assertEquals(AmciDecisionEngine.Action.WAIT_FOR_STABILITY,AmciDecisionEngine.decide(o,AmciDecisionEngine.Policy.researchV1()).action);
    }

    @Test public void exhaustedAmbiguousSequenceCanReturnUnknown(){
        AmciDecisionEngine.Observation o=new AmciDecisionEngine.Observation(true,true,false,false,false,.45,.95,.95,.001,120,2);
        assertEquals(AmciDecisionEngine.Action.RETURN_UNKNOWN,AmciDecisionEngine.decide(o,AmciDecisionEngine.Policy.researchV1()).action);
    }

    @Test public void informationGainRewardsEntropyReduction(){
        double gain=AmciDecisionEngine.expectedInformationGain(new double[]{.5,.5},List.of(new double[]{.95,.05},new double[]{.9,.1}),new double[]{.5,.5});
        assertTrue(gain>0);
    }
}
