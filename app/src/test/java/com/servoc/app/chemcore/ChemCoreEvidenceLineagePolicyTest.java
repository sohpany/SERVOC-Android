package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreEvidenceLineagePolicyTest {
    @Test public void directReferencesFromDifferentSourcesRemainIndependent(){
        ChemCoreEvidenceObservation a=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,"REF:a","paper-a");
        ChemCoreEvidenceObservation b=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,"REF:b","paper-b");
        assertEquals(ChemCoreEvidenceLineagePolicy.Kind.DIRECT_EXTERNAL_REFERENCE,
                ChemCoreEvidenceLineagePolicy.classify(a).kind);
        assertEquals(ChemCoreEvidenceLineagePolicy.Relation.INDEPENDENT,
                ChemCoreEvidenceLineagePolicy.relation(a,b));
    }

    @Test public void repeatedDirectSourceIsNotIndependent(){
        ChemCoreEvidenceObservation a=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,"REF:a1","paper-a");
        ChemCoreEvidenceObservation b=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,"REF:a2","paper-a");
        assertEquals(ChemCoreEvidenceLineagePolicy.Relation.SAME_DIRECT_SOURCE,
                ChemCoreEvidenceLineagePolicy.relation(a,b));
    }

    @Test public void qsprAndModelDerivedFingerprintShareReferenceLibraryLineage(){
        ChemCoreEvidenceObservation q=ev(ChemCoreEvidenceObservation.Type.QSPR,"QSPR:runtime","pubchem-qspr-mox-runtime/3-ad");
        ChemCoreEvidenceObservation fp=new ChemCoreEvidenceObservation(
                ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT,1,"X",List.of("mq3","mq135"),
                "SOURCE_BACKED_REFERENCE_FINGERPRINT","fp","ResearchIdentificationFingerprintRegistry:0.10.2-HR069",
                "FP:model",ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,true,false,false,
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.4,"model derived");
        assertTrue(ChemCoreEvidenceLineagePolicy.correlatedDerivative(q));
        assertTrue(ChemCoreEvidenceLineagePolicy.correlatedDerivative(fp));
        assertEquals(ChemCoreEvidenceLineagePolicy.Relation.COMMON_DERIVED_REFERENCE_LIBRARY,
                ChemCoreEvidenceLineagePolicy.relation(q,fp));
    }

    @Test public void derivedModelIsCorrelatedWithDirectReferenceLibraryEvidence(){
        ChemCoreEvidenceObservation q=ev(ChemCoreEvidenceObservation.Type.QSPR,"QSPR:runtime","pubchem-qspr-mox-runtime/3-ad");
        ChemCoreEvidenceObservation direct=ev(ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE,"MFG:winsen","winsen-manual");
        assertEquals(ChemCoreEvidenceLineagePolicy.Relation.DERIVED_MODEL_VS_REFERENCE_SOURCE,
                ChemCoreEvidenceLineagePolicy.relation(q,direct));
        assertFalse(ChemCoreEvidenceLineagePolicy.independentConfirmation(q,direct));
    }

    @Test public void independentHr069CampaignCanBeIndependentOfExternalReference(){
        ChemCoreEvidenceObservation local=new ChemCoreEvidenceObservation(
                ChemCoreEvidenceObservation.Type.HR069_EXPERIMENTAL_FINGERPRINT,1,"X",List.of("mq2","mq3"),
                "HR069_EXPERIMENTAL_FINGERPRINT","m","HR069-CAMPAIGN-A","HR069:campaign-a",
                ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,true,false,false,
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.2,"empirical");
        ChemCoreEvidenceObservation external=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,"REF:a","paper-a");
        assertEquals(ChemCoreEvidenceLineagePolicy.Relation.INDEPENDENT,
                ChemCoreEvidenceLineagePolicy.relation(local,external));
    }

    private static ChemCoreEvidenceObservation ev(ChemCoreEvidenceObservation.Type type,String group,String source){
        ChemCoreDecision.EvidenceTier tier=type==ChemCoreEvidenceObservation.Type.QSPR
                ?ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED
                :ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;
        return new ChemCoreEvidenceObservation(type,1,"X",List.of("mq2"),type.name(),"m",source,group,tier,
                true,true,false,100,1,0,0,0.2,"test");
    }
}
