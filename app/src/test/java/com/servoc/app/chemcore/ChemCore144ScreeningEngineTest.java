package com.servoc.app.chemcore;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;

public class ChemCore144ScreeningEngineTest {
    @Test public void evaluatesEntireOperationalPanel() {
        JSONObject p=ChemCore144ScreeningEngine.evaluate();
        assertEquals("EVALUATED",p.optString("state"));
        assertEquals(144,p.optInt("target_count"));
        assertEquals(144,p.optInt("evaluated_count"));
        assertEquals(ChemCore144ScreeningEngine.SESSION_SUPPORT_SEMANTICS,p.optString("session_support_semantics"));
        JSONArray rows=p.optJSONArray("rows");
        assertNotNull(rows); assertEquals(144,rows.length());
        for(int i=0;i<rows.length();i++){
            JSONObject r=rows.optJSONObject(i);
            assertNotNull(r); assertEquals(i,r.optInt("index",-1));
            assertFalse(r.optString("name").isEmpty());
            assertFalse(r.optString("status").isEmpty());
            assertFalse(r.optString("reason").isEmpty());
            assertFalse(r.optBoolean("detected",true));
            assertFalse(r.optBoolean("ppm_reportable",true));
            assertFalse(r.optBoolean("session_candidate_evidence",true));
            assertTrue(r.has("appearance_reliability_class"));
        }
    }

    @Test public void identifiedSessionMarksOnlyAcceptedRowDetected() {
        ChemCoreResultEnvelope e=new ChemCoreResultEnvelope("s",ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,
                20L,"Carbon Monoxide",Double.NaN,Double.NaN,Double.NaN,0.1,0.8,0.2,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","",null);
        JSONObject panel=ChemCore144ScreeningEngine.evaluate(e);JSONArray rows=panel.optJSONArray("rows");
        int detected=0;
        for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i).optBoolean("detected"))detected++;
        assertEquals(1,detected);
        assertTrue(rows.optJSONObject(20).optBoolean("detected"));
        assertFalse(rows.optJSONObject(20).optBoolean("ppm_reportable"));
        assertEquals("ACCEPTED_NAMED_IDENTITY",rows.optJSONObject(20).optString("session_evidence_state"));
    }

    @Test public void candidateEvidenceGetsSessionSupportWithoutBecomingDetectionProbability() {
        ChemCoreResultEnvelope.CandidateSummary co=new ChemCoreResultEnvelope.CandidateSummary(20L,"Carbon Monoxide",0.72,120,0.08,4,
                ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,90,82,88,0,false,Collections.emptyMap(),78,3,4,Collections.emptyList());
        ChemCoreResultEnvelope.CandidateSummary ethanol=new ChemCoreResultEnvelope.CandidateSummary(103L,"Ethanol",0.18,300,0.20,3,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,75,64,70,0,false,Collections.emptyMap(),55,2,3,Collections.emptyList());
        ChemCoreResultEnvelope e=new ChemCoreResultEnvelope("s",ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                null,"",Double.NaN,Double.NaN,Double.NaN,0.2,0.72,0.45,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","",Arrays.asList(co,ethanol));
        JSONObject panel=ChemCore144ScreeningEngine.evaluate(e);JSONArray rows=panel.optJSONArray("rows");
        JSONObject r=rows.optJSONObject(20);
        assertTrue(r.optBoolean("session_candidate_evidence"));
        assertFalse(r.optBoolean("detected"));
        assertEquals(1,r.optInt("candidate_rank"));
        assertEquals(78.0,r.optDouble("session_support_index_pct"),1e-9);
        assertEquals("STRONG_SUPPORT",r.optString("appearance_reliability_class"));
        assertEquals(ChemCore144ScreeningEngine.RELIABILITY_SEMANTICS,r.optString("appearance_reliability_semantics"));
        assertEquals(2,panel.optInt("session_candidate_supported_count"));
        assertEquals(0,panel.optInt("accepted_identity_count"));
    }
}
