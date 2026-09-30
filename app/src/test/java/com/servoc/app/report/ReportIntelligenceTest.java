package com.servoc.app.report;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class ReportIntelligenceTest {
    @Test public void emptyEvidenceProducesNoCharts(){
        ReportIntelligence.Snapshot s=ReportIntelligence.evaluate(null,null);
        assertEquals(ReportIntelligence.State.EMPTY,s.state);
        assertEquals(0,s.availableChartCount);
        assertFalse(ReportIntelligence.chart(3,null,null).available);
        assertFalse(ReportIntelligence.chart(0,null,null).available);
        ReportIntelligence.ReportPlan p=ReportIntelligence.plan(null,null,0);
        assertEquals(ReportIntelligence.ReportMode.NO_EVIDENCE,p.mode);
        assertEquals(4,p.pageCount);
        assertFalse(p.includeChemistry);
    }

    @Test public void chemCoreOnlyEnablesEvidenceChartsAndSmartPlan(){
        try{
            JSONObject cc=new JSONObject();cc.put("outcome","IDENTIFIED");cc.put("chemical","Ethanol");cc.put("top_posterior",0.82);cc.put("posterior_separation_margin",0.31);cc.put("ood_score",0.14);cc.put("normalized_entropy",0.22);cc.put("analytical_confidence_pct",84.0);cc.put("ppm_reportable",false);
            JSONArray candidates=new JSONArray();candidates.put(new JSONObject().put("chemical_uid",104).put("label","Ethanol").put("posterior",0.82));cc.put("candidates",candidates);
            JSONArray rows=new JSONArray();rows.put(new JSONObject().put("index",103).put("name","Ethanol").put("session_candidate_evidence",true).put("session_support_index_pct",84.0).put("candidate_rank",1).put("detected",true).put("ppm_reportable",false));
            cc.put("screening_144",new JSONObject().put("evaluated_count",144).put("target_count",144).put("session_candidate_supported_count",1).put("accepted_identity_count",1).put("rows",rows));
            cc.put("gas_exposure_ppm",new JSONObject().put("input_gate_passed",false));
            ReportIntelligence.Snapshot s=ReportIntelligence.evaluate(null,cc);
            assertEquals(ReportIntelligence.State.IDENTIFIED,s.state);
            assertTrue(ReportIntelligence.chart(3,null,cc).available);
            assertTrue(ReportIntelligence.chart(7,null,cc).available);
            assertFalse(ReportIntelligence.chart(0,null,cc).available);
            assertFalse(ReportIntelligence.chart(10,null,cc).available);
            ReportIntelligence.ReportPlan p=ReportIntelligence.plan(null,cc,2);
            assertEquals(ReportIntelligence.ReportMode.IDENTIFIED,p.mode);
            assertTrue(p.include144);assertTrue(p.includeTraceability);assertEquals(5,p.pageCount);
            assertEquals(1,p.panelCandidateSupported);assertEquals(1,p.top144SessionEvidence.size());
            assertTrue(p.findings.stream().anyMatch(x->"PPM_WITHHELD".equals(x.code)));
        }catch(Exception e){throw new AssertionError(e);}
    }

    @Test public void ppmReportablePromotesQuantifiedStateAndSafetyPage(){
        try{
            JSONObject cc=new JSONObject();cc.put("outcome","QUANTIFIED");cc.put("chemical","Ethanol");cc.put("evidence_tier","M4_HAZROB_VALIDATED");cc.put("ppm_reportable",true);cc.put("ppm",12.5);cc.put("candidates",new JSONArray().put(new JSONObject().put("posterior",0.9)));
            cc.put("gas_exposure_ppm",new JSONObject().put("input_gate_passed",true).put("hazard_priority_percent",25).put("percent_lfl",2));
            ReportIntelligence.Snapshot s=ReportIntelligence.evaluate(null,cc);
            assertEquals(ReportIntelligence.State.QUANTIFIED,s.state);assertTrue(s.ppmReportable);assertTrue(s.safetyInterpretable);
            ReportIntelligence.ReportPlan p=ReportIntelligence.plan(null,cc,1);assertEquals(ReportIntelligence.ReportMode.QUANTIFIED,p.mode);assertTrue(p.includeQuantitativeSafety);assertFalse(p.includeUncertainty);
        }catch(Exception e){throw new AssertionError(e);}
    }

    @Test public void mixtureModeCannotMasqueradeAsIdentity(){
        try{
            JSONObject cc=new JSONObject().put("outcome","POSSIBLE_MIXTURE_OR_INTERFERENCE").put("ppm_reportable",false).put("candidates",new JSONArray().put(new JSONObject().put("posterior",0.55)));
            ReportIntelligence.ReportPlan p=ReportIntelligence.plan(null,cc,1);
            assertEquals(ReportIntelligence.ReportMode.OPEN_SET_OR_MIXTURE,p.mode);assertTrue(p.includeUncertainty);assertFalse(p.ppmReportable);
            assertTrue(p.findings.stream().anyMatch(x->"MIXTURE_INTERFERENCE".equals(x.code)));
        }catch(Exception e){throw new AssertionError(e);}
    }
}
