package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreAnalyticalTraceTest {
    @Test public void ethanolScreeningTraceKeepsFlexibleKnowledgeMap()throws Exception{
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(
                104L,"Ethanol",0.60,180,0.8,3,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED);
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("S",ChemCoreDecision.Outcome.SCREENING,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,104L,"Ethanol",
                Double.NaN,Double.NaN,Double.NaN,4.0,.60,Double.NaN,"p","f","m","OOD_LIKE diagnostic",List.of(c));
        JSONObject trace=ChemCoreAnalyticalTrace.build(r,new ChemCoreRejectedEvidenceAuditTrail.Result(Collections.emptyList()));
        assertEquals(ChemCoreAnalyticalTrace.DEVELOPMENT_MODE,trace.getString("development_mode"));
        assertEquals("CANDIDATE",trace.getJSONObject("progressive_state").getString("state"));
        JSONObject k=trace.getJSONObject("knowledge_map");
        assertEquals("PURE_ANALYTE",k.getString("object_type"));
        assertEquals("PROVISIONAL_IDENTITY_CAPABLE",k.getString("identity_ceiling"));
        assertFalse(k.getBoolean("identity_ceiling_is_runtime_gate"));
        assertEquals("TARGET_ANCHOR",k.getString("experimental_campaign"));
    }

    @Test public void mixtureTraceDoesNotInheritPureAnalytePpmSemantics()throws Exception{
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("M",ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,136L,"LPG",
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,.50,Double.NaN,"p","f","m","mixture warning",Collections.emptyList());
        JSONObject k=ChemCoreAnalyticalTrace.build(r,null).getJSONObject("knowledge_map");
        assertEquals("MIXTURE",k.getString("object_type"));
        assertFalse(k.getBoolean("pure_analyte_ppm_semantics"));
        assertEquals("MIXTURE_OR_AGGREGATE",k.getString("experimental_campaign"));
    }

    @Test public void rejectedEvidenceIsPreservedInTrace()throws Exception{
        ChemCoreRejectedEvidenceAuditTrail.Row row=ChemCoreRejectedEvidenceAuditTrail.unsupportedHardware(
                23L,"Oxygen","","current MQ array lacks primary route");
        ChemCoreRejectedEvidenceAuditTrail.Result rejected=new ChemCoreRejectedEvidenceAuditTrail.Result(List.of(row));
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("U",ChemCoreDecision.Outcome.SCREENING,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,23L,"Oxygen",Double.NaN,Double.NaN,Double.NaN,
                Double.NaN,.1,Double.NaN,"p","f","m","exploratory",Collections.emptyList());
        JSONObject trace=ChemCoreAnalyticalTrace.build(r,rejected);
        assertEquals(1,trace.getJSONArray("rejected_evidence").length());
        assertEquals("UNSUPPORTED_HARDWARE",trace.getJSONArray("rejected_evidence").getJSONObject(0).getString("reason"));
        assertEquals("UNSUPPORTED",trace.getJSONObject("knowledge_map").getString("identity_ceiling"));
    }
}
