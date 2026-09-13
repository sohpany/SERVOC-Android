package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreAnalysisPresenterTest {
    private static JSONObject screeningPayload()throws Exception{
        JSONObject c=new JSONObject().put("rank",1).put("label","Test gas").put("posterior",0.72)
                .put("screening_fit_ppm",55.0).put("residual_rms",0.31).put("contributors",3)
                .put("coverage_pct",75.0).put("agreement_pct",81.0).put("evidence_quality_pct",85.0)
                .put("calibrated_contributors",1).put("all_contributors_inside_domain",true)
                .put("residual_log10_by_sensor",new JSONObject().put("mq2",0.02).put("mq5",-0.03));
        return new JSONObject().put("schema",ChemCoreResultEnvelope.SCHEMA).put("session_id","S1")
                .put("outcome","IDENTIFIED").put("evidence_tier","M3_ARRAY_CONSISTENT").put("chemical","Test gas")
                .put("top_posterior",0.72).put("runner_up_posterior",0.20).put("posterior_separation_margin",0.52)
                .put("ppm_reportable",false).put("ppm_withheld_reason","m4_device_reference_calibration_required")
                .put("candidates",new JSONArray().put(c));
    }

    @Test public void screeningFitIsNeverPresentedAsReportablePpm()throws Exception{
        JSONObject p=screeningPayload();String summary=ChemCoreAnalysisPresenter.summary(p),rows=ChemCoreAnalysisPresenter.candidates(p,8);
        assertTrue(summary.contains("PPM: WITHHELD"));
        assertTrue(summary.contains("m4_device_reference_calibration_required"));
        assertTrue(rows.contains("Joint log-fit hypothesis: 55.00 ppm"));
        assertTrue(rows.contains("screening-only"));
    }

    @Test public void equationsExplainLogInverseResidualAndM4()throws Exception{
        String text=ChemCoreAnalysisPresenter.equationsAndResiduals(screeningPayload(),4);
        assertTrue(text.contains("log10(Rs/R0) = m"));
        assertTrue(text.contains("C = 10^"));
        assertTrue(text.contains("Joint fit"));
        assertTrue(text.contains("M4"));
        assertTrue(text.contains("MQ2"));
    }
}