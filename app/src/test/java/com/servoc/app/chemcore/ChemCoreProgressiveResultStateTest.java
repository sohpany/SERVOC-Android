package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreProgressiveResultStateTest {
    @Test public void mapsOrdinaryLadderWithoutUsingPosteriorAsState(){
        assertState(envelope(ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,Collections.emptyList()),ChemCoreProgressiveResultState.State.SIGNAL,1);
        assertState(envelope(ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,2L,List.of(candidate(2,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED))),ChemCoreProgressiveResultState.State.AFFINITY,2);
        assertState(envelope(ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,3L,List.of(candidate(3,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED))),ChemCoreProgressiveResultState.State.CANDIDATE,3);
        assertState(envelope(ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,4L,List.of(candidate(4,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT))),ChemCoreProgressiveResultState.State.IDENTIFIED,4);
        assertState(envelope(ChemCoreDecision.Outcome.QUANTIFIED,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,5L,List.of(candidate(5,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED))),ChemCoreProgressiveResultState.State.QUANTIFIED,5);
    }

    @Test public void outOfDomainAndUnknownAreExplicitTerminalBranches(){
        ChemCoreProgressiveResultState.Result rejected=ChemCoreProgressiveResultState.project(
                envelope(ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,3L,List.of(candidate(3,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED))));
        assertEquals(ChemCoreProgressiveResultState.State.ANALYTICALLY_REJECTED,rejected.state);assertTrue(rejected.terminal);assertEquals(3,rejected.ladderLevel);
        ChemCoreProgressiveResultState.Result unknown=ChemCoreProgressiveResultState.project(
                envelope(ChemCoreDecision.Outcome.UNKNOWN,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,Collections.emptyList()));
        assertEquals(ChemCoreProgressiveResultState.State.UNKNOWN_OOD,unknown.state);assertTrue(unknown.terminal);assertEquals(1,unknown.ladderLevel);
    }

    @Test public void mixtureNeverBecomesNamedIdentity(){
        ChemCoreProgressiveResultState.Result r=ChemCoreProgressiveResultState.project(
                envelope(ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,4L,List.of(candidate(4,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT))));
        assertEquals(ChemCoreProgressiveResultState.State.CANDIDATE,r.state);assertEquals(3,r.ladderLevel);assertTrue(r.terminal);
    }

    @Test public void insufficientEvidenceCanStopAtAffinityOrCandidate(){
        ChemCoreProgressiveResultState.Result affinity=ChemCoreProgressiveResultState.project(
                envelope(ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,2L,List.of(candidate(2,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED))));
        assertEquals(ChemCoreProgressiveResultState.State.AFFINITY,affinity.state);assertTrue(affinity.terminal);
        ChemCoreProgressiveResultState.Result candidate=ChemCoreProgressiveResultState.project(
                envelope(ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,3L,List.of(candidate(3,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED))));
        assertEquals(ChemCoreProgressiveResultState.State.CANDIDATE,candidate.state);assertTrue(candidate.terminal);
    }

    private static void assertState(ChemCoreResultEnvelope e,ChemCoreProgressiveResultState.State state,int level){
        ChemCoreProgressiveResultState.Result r=ChemCoreProgressiveResultState.project(e);assertEquals(state,r.state);assertEquals(level,r.ladderLevel);
    }

    private static ChemCoreResultEnvelope envelope(ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier tier,Long uid,List<ChemCoreResultEnvelope.CandidateSummary> candidates){
        return new ChemCoreResultEnvelope("S",outcome,tier,uid,uid==null?"":"X",Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.99,Double.NaN,"p","f","m","test",candidates);
    }

    private static ChemCoreResultEnvelope.CandidateSummary candidate(long uid,ChemCoreDecision.EvidenceTier tier){
        return new ChemCoreResultEnvelope.CandidateSummary(uid,"X",0.99,100,0.5,2,tier);
    }
}
