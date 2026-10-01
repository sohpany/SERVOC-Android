package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ChemCoreConfidenceMatrixEvidenceTest {
    @Test public void correlatedQsprChannelsCountAsOneIndependentEvidenceGroup(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(7,"mq2","QSPR:shared","qspr-runtime",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true),
                evidence(7,"mq3","QSPR:shared","qspr-runtime",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true),
                evidence(7,"mq135","QSPR:shared","qspr-runtime",ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(7,rows);
        assertEquals(1,s.qsprCount);
        assertEquals(20.0,s.qsprPct,0.0001);
        assertEquals(5.0,s.breadthPct,0.0001);
        assertEquals(25.0,s.totalPct,0.0001);
    }

    @Test public void strongestTierWinsInsideOneIndependenceGroup(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(11,"mq2","same-source","paper-a",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true),
                evidence(11,"mq3","same-source","HR069-campaign-a",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(11,rows);
        assertEquals(0,s.referenceCount);
        assertEquals(1,s.hr069Count);
        assertEquals(75.0,s.hr069Pct,0.0001);
        assertEquals(3.0,s.breadthPct,0.0001);
        assertEquals(78.0,s.totalPct,0.0001);
    }

    @Test public void distinctReferenceSourcesRemainIndependent(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(13,"mq2","literature:A","paper-A",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true),
                evidence(13,"mq3","manufacturer:B","manufacturer-B",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(13,rows);
        assertEquals(2,s.referenceCount);
        assertEquals(55.0,s.referencePct,0.0001);
        assertEquals(58.0,s.totalPct,0.0001);
    }

    @Test public void differentImmediateGroupsFromSameDirectSourceAreNotIndependent(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(13,"mq2","literature:A","same-paper",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true),
                evidence(13,"mq3","literature:B","same-paper",ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true));
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(13,rows);
        assertEquals(1,s.referenceCount);
        assertEquals(45.0,s.referencePct,0.0001);
        assertEquals(48.0,s.totalPct,0.0001);
    }

    @Test public void outOfDomainSameChemicalIsWeakCorroborationButOtherChemicalNeverBoostsCandidate(){
        List<ChemCoreEvidenceObservation> rows=Arrays.asList(
                evidence(17,"mq2","hr069:outside","HR069-outside",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,false),
                evidence(18,"mq3","hr069:other","HR069-other",ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,true));
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(17,rows);
        ChemCoreConfidenceMatrix.Score s=r.score;
        assertEquals(0,s.qsprCount);
        assertEquals(0,s.referenceCount);
        assertEquals(0,s.hr069Count);
        assertEquals(1,r.corroborativeGroups);
        assertTrue(s.corroborationPct>0.0);
        assertTrue(s.corroborationPct<=ChemCoreEvidenceFusion.MAX_CORROBORATION_BONUS);
        assertEquals(s.corroborationPct,s.totalPct,0.0001);
    }

    @Test public void m3IdentityEvidenceIsNonM4SupportedEvidenceUntilWeightCalibration(){
        ChemCoreConfidenceMatrix.Score s=ChemCoreConfidenceMatrix.score(21,Collections.singletonList(
                evidence(21,"mq3","fingerprint:exp-1","HR069-fingerprint-exp-1",ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,true)));
        assertEquals(1,s.referenceCount);
        assertEquals(0,s.hr069Count);
        assertEquals(45.0,s.totalPct,0.0001);
    }

    private static ChemCoreEvidenceObservation evidence(long uid,String sensor,String group,String source,
                                                          ChemCoreDecision.EvidenceTier tier,boolean insideDomain){
        ChemCoreEvidenceObservation.Type type;
        if(tier==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED)type=ChemCoreEvidenceObservation.Type.QSPR;
        else if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED)type=ChemCoreEvidenceObservation.Type.HR069_CALIBRATION;
        else if(tier==ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT)type=ChemCoreEvidenceObservation.Type.HR069_EXPERIMENTAL_FINGERPRINT;
        else type=ChemCoreEvidenceObservation.Type.LITERATURE_CURVE;
        return new ChemCoreEvidenceObservation(type,uid,"candidate",Collections.singletonList(sensor),"family","model",source,group,tier,insideDomain,
                false,false,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"test");
    }
}
