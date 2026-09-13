package com.servoc.app.chemcore;

import com.servoc.app.exposure.GasPpmExposureAssessment;

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

    @Test public void gasSafetyAssessmentRendersRelativePriorityTemporalWarningAndLfl()throws Exception{
        JSONObject p=screeningPayload();JSONObject g=new JSONObject().put("schema",GasPpmExposureAssessment.SCHEMA).put("input_gate_passed",true).put("state","REFERENCE_CONTEXT_CROSSED").put("toxicity_state","REFERENCE_CONTEXT_CROSSED").put("flammability_state","BELOW_LFL").put("ppm",20.0).put("measurement_duration_ms",30000).put("ppm_reference_count",1).put("lfl_reference_count",1).put("excluded_non_ppm_count",2).put("crossed_reference_count",1).put("action_relevant_crossed_count",0).put("max_ratio_to_reference",2.0).put("toxicity_priority_percent",200.0).put("percent_lfl",50.0).put("lfl_vol_pct",0.004).put("lfl_ppm",40.0).put("hazard_priority_percent",200.0).put("hazard_priority_basis","TOXICITY").put("toxic_reference_crossed",true).put("health_reference_context_warning",true).put("flammability_lfl_reached",false).put("hazard_symbols","☠");
        g.put("references",new JSONArray().put(new JSONObject().put("authority","NIOSH").put("limit_type","STEL").put("value_ppm",10.0).put("percent_of_reference",200.0).put("reference_crossed",true).put("temporal_role","STEL").put("duration_min",15.0).put("duration_coverage_satisfied",false)));p.put(GasPpmExposureAssessment.KEY,g);
        String summary=ChemCoreAnalysisPresenter.summary(p),detail=ChemCoreAnalysisPresenter.gasExposurePpm(p,5);assertTrue(summary.contains("Gas safety: ☠REFERENCE_CONTEXT_CROSSED"));assertTrue(summary.contains("priority 200.0%"));assertTrue(detail.contains("NIOSH STEL: 10.00 ppm"));assertTrue(detail.contains("%LFL: 50.00%"));assertTrue(detail.contains("duration not covered"));assertTrue(detail.contains("☠ يعني"));assertTrue(detail.contains("Compliance: NOT MADE"));
    }
}
