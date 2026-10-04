package com.servoc.app.report;

import com.servoc.app.chemcore.ChemCoreReportContract;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SmartReportAnalyticalContractTest {
    @Test public void chemCoreAndSmartReportContractVersionsAreLockedTogether(){
        assertEquals(SmartReportAnalyticalContract.EXPECTED_CHEMCORE_VERSION,ChemCoreReportContract.VERSION);
        assertEquals("relative_softmax_model_weight_not_calibrated_probability",ChemCoreReportContract.RANKING_SEMANTICS);
    }

    @Test public void partialEvidenceIsAllowedButAvailabilityIsExplicit()throws Exception{
        JSONObject cc=new JSONObject().put("outcome","IDENTIFIED").put("chemical","Ethanol")
                .put("evidence_tier","M3_TRANSFER").put("ppm_reportable",false).put("top_posterior",0.82)
                .put("ood_score",0.14).put("candidates",new JSONArray().put(new JSONObject().put("label","Ethanol").put("posterior",0.82)));
        SmartReportAnalyticalContract c=SmartReportAnalyticalContract.bind(null,cc);
        assertTrue(c.availability(SmartReportAnalyticalContract.Capability.CANDIDATES).available);
        assertFalse(c.availability(SmartReportAnalyticalContract.Capability.RECORDED_TIME_SERIES).available);
        assertFalse(c.availability(SmartReportAnalyticalContract.Capability.QUANTITATIVE_PPM).available);
        assertEquals("m4_quantitative_gate_not_passed",c.availability(SmartReportAnalyticalContract.Capability.QUANTITATIVE_PPM).reason);
        assertEquals(SmartReportAnalyticalContract.VERSION,c.metadata().getString("smartreport_contract_version"));
        assertEquals(ChemCoreReportContract.VERSION,c.metadata().getString("chemcore_report_contract_version"));
        ReportIntelligence.ReportPlan p=ReportIntelligence.plan(c.series(),c.decision(),1);
        assertEquals(ReportIntelligence.ReportMode.IDENTIFIED,p.mode);
        assertFalse(p.ppmReportable);
    }

    @Test(expected=IllegalStateException.class) public void nonM4EvidenceCannotUnlockReportablePpm()throws Exception{
        SmartReportAnalyticalContract.bind(null,new JSONObject().put("outcome","QUANTIFIED").put("evidence_tier","M3_TRANSFER").put("ppm_reportable",true).put("ppm",25.0));
    }

    @Test(expected=IllegalStateException.class) public void gasSafetyGateCannotPassWhenPpmIsWithheld()throws Exception{
        SmartReportAnalyticalContract.bind(null,new JSONObject().put("outcome","IDENTIFIED").put("evidence_tier","M3_TRANSFER").put("ppm_reportable",false).put("gas_exposure_ppm",new JSONObject().put("input_gate_passed",true)));
    }

    @Test public void m4QuantitativeDecisionIsAccepted()throws Exception{
        JSONObject cc=new JSONObject().put("outcome","QUANTIFIED").put("evidence_tier",ChemCoreReportContract.REPORTABLE_PPM_TIER)
                .put("ppm_reportable",true).put("ppm",12.5).put("gas_exposure_ppm",new JSONObject().put("input_gate_passed",true));
        SmartReportAnalyticalContract c=SmartReportAnalyticalContract.bind(null,cc);
        assertTrue(c.availability(SmartReportAnalyticalContract.Capability.QUANTITATIVE_PPM).available);
        assertTrue(c.availability(SmartReportAnalyticalContract.Capability.GAS_SAFETY).available);
    }
}
