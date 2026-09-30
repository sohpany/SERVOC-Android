package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class ChemCoreConfidenceMatrixEvidenceTest {
    @Test public void correlatedQsprChannelsCountAsOneIndependentEvidenceGroup(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(7,"mq2","QSPR:shared",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true),
                evidence(7,"mq3","QSPR:shared",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true),
                evidence(7,"mq135","QSPR:shared",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(7,rows);
        assertEquals(1,s.qsprCount);
        assertEquals(20.0,s.qsprPct,0.0001);
        assertEquals(5.0,s.breadthPct,0.0001);
        assertEquals(25.0,s.totalPct,0.0001);
    }

    @Test public void strongestTierWinsInsideOneIndependenceGroup(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(11,"mq2","same-source",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true),
                evidence(11,"mq3","same-source",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(11,rows);
        assertEquals(0,s.referenceCount);
        assertEquals(1,s.hr069Count);
        assertEquals(75.0,s.hr069Pct,0.0001);
        assertEquals(3.0,s.breadthPct,0.0001);
        assertEquals(78.0,s.totalPct,0.0001);
    }

    @Test public void independentReferenceGroupsRemainIndependent(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(13,"mq2","literature:A",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true),
                evidence(13,"mq3","manufacturer:B",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(13,rows);
        assertEquals(2,s.referenceCount);
        assertEquals(55.0,s.referencePct,0.0001);
        assertEquals(58.0,s.totalPct,0.0001);
    }

    @Test public void outOfDomainAndOtherChemicalDoNotBoostCandidateConfidence(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(17,"mq2","hr069:outside",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,false),
                evidence(18,"mq3","hr069:other",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(17,rows);
        assertEquals(0,s.qsprCount);
        assertEquals(0,s.referenceCount);
        assertEquals(0,s.hr069Count);
        assertEquals(0.0,s.totalPct,0.0001);
    }

    @Test public void m3IdentityEvidenceIsNonM4SupportedEvidenceUntilWeightCalibration(){
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(21,Collections.singletonList(
                evidence(21,"mq3","fingerprint:exp-1",ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,true)));
        assertEquals(1,s.referenceCount);
        assertEquals(0,s.hr069Count);
        assertEquals(45.0,s.totalPct,0.0001);
    }

    private static ChemCoreEvidenceObservation evidence(long uid,String sensor,String group,
                                                          ChemCoreDecision.EvidenceTier tier,boolean insideDomain){
        return new ChemCoreEvidenceObservation(
                tier==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED?ChemCoreEvidenceObservation.Type.QSPR:
                        tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED?ChemCoreEvidenceObservation.Type.HR069_CALIBRATION:
                                ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT,
                uid,"candidate",Collections.singletonList(sensor),"family","model","source",group,tier,insideDomain,
                false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"test");
    }
}
