package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AcceptedReferenceResponseModelTest {
    @Test public void acceptedReferenceRegistryNowExposesAllSourceBackedNumericFamilies(){
        List<InverseMolecularSolver.Candidate> rows=ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
        Set<Long> ids=new HashSet<>();for(InverseMolecularSolver.Candidate c:rows)ids.add(c.chemicalUid);
        // legacy id + 1 UID mapping
        int[] legacy={20,21,23,34,75,76,78,84,95,103,110,135,139};
        for(int id:legacy)assertTrue("missing accepted reference candidate "+id,ids.contains(id+1L));
        assertEquals(13,ids.size());
        assertFalse(AcceptedReferenceResponseModel.M4);
        assertEquals("ACCEPTED_PROVISIONAL_REFERENCE_PPM",AcceptedReferenceResponseModel.ACCEPTANCE_STATE);
    }

    @Test public void smokeAndLpgHaveUsefulMultichannelReferenceCoverage(){
        assertTrue(AcceptedReferenceResponseModel.acceptedContributorCount(139)>=2);
        assertTrue(AcceptedReferenceResponseModel.acceptedContributorCount(135)>=4);
    }

    @Test public void peerReviewedMq135CurveWinsInsideItsPublishedDomain(){
        AcceptedReferenceResponseModel m=new AcceptedReferenceResponseModel(GasEngine.MQ135,103);
        ChemCoreResponseModel.Estimate e=m.predict(50,20,55,null);
        assertTrue(e.available);assertTrue(e.insideDomain);
        assertTrue(e.note.contains("UMBU_MQ135_2023"));
        assertEquals(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,m.evidenceTier());
    }

    @Test public void lowerTierPinnedCurveCanExtendOnlyInsideItsOwnDeclaredDomain(){
        AcceptedReferenceResponseModel m=new AcceptedReferenceResponseModel(GasEngine.MQ135,110);
        ChemCoreResponseModel.Estimate inside=m.predict(150,20,55,null);
        assertTrue(inside.available);
        assertTrue(inside.note.contains("MQDataScience"));
        ChemCoreResponseModel.Estimate outside=m.predict(500,20,55,null);
        assertFalse(outside.available);
    }

    @Test public void mq7AndMq9ReferenceCurvesRemainBlockedWithoutCyclicDriver(){
        assertFalse(AcceptedReferenceResponseModel.eligible(GasEngine.MQ7,20));
        assertFalse(AcceptedReferenceResponseModel.eligible(GasEngine.MQ9,20));
        assertFalse(AcceptedReferenceResponseModel.eligible(GasEngine.MQ9,76));
    }
}
