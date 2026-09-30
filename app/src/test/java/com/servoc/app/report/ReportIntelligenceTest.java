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
    }

    @Test public void chemCoreOnlyEnablesOnlyEvidenceCharts(){throwsNothing();}
    private static void throwsNothing(){
        try{
            JSONObject cc=new JSONObject();cc.put("outcome","IDENTIFIED");cc.put("chemical","Ethanol");cc.put("top_posterior",0.82);cc.put("ppm_reportable",false);
            JSONArray candidates=new JSONArray();candidates.put(new JSONObject().put("chemical_uid",104).put("label","Ethanol").put("posterior",0.82));cc.put("candidates",candidates);
            cc.put("screening_144",new JSONObject().put("evaluated_count",144).put("target_count",144).put("rows",new JSONArray()));
            cc.put("gas_exposure_ppm",new JSONObject().put("input_gate_passed",false));
            ReportIntelligence.Snapshot s=ReportIntelligence.evaluate(null,cc);
            assertEquals(ReportIntelligence.State.IDENTIFIED,s.state);
            assertTrue(ReportIntelligence.chart(3,null,cc).available);
            assertTrue(ReportIntelligence.chart(7,null,cc).available);
            assertFalse(ReportIntelligence.chart(0,null,cc).available);
            assertFalse(ReportIntelligence.chart(10,null,cc).available==false && s.safetyInterpretable);
            assertTrue(s.availableChartCount>=7);
        }catch(Exception e){throw new AssertionError(e);}
    }

    @Test public void ppmReportablePromotesQuantifiedState(){
        try{
            JSONObject cc=new JSONObject();cc.put("outcome","IDENTIFIED");cc.put("ppm_reportable",true);cc.put("ppm",12.5);cc.put("candidates",new JSONArray().put(new JSONObject().put("posterior",0.9)));
            ReportIntelligence.Snapshot s=ReportIntelligence.evaluate(null,cc);
            assertEquals(ReportIntelligence.State.QUANTIFIED,s.state);assertTrue(s.ppmReportable);
        }catch(Exception e){throw new AssertionError(e);}
    }
}
