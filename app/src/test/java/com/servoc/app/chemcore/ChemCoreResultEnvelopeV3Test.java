package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChemCoreResultEnvelopeV3Test {
    @Test public void singleSensorAgreementIsNullAndScreeningPpmIsWithheld()throws Exception{
        Map<String,Double> residuals=new LinkedHashMap<>();residuals.put("mq3",0.12);
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(1,"benzene",0.72,35.0,0.12,1,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,12.5,Double.NaN,60.0,0,true,residuals);
        JSONObject quality=new JSONObject().put("schema",ChemCoreLabQuality.SCHEMA).put("calibration_gate",new JSONObject()
                .put("field_r0_normalization_ready",true).put("reference_gas_validated_channels",0));
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("S1",ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                1L,"benzene",Double.NaN,Double.NaN,Double.NaN,2.0,.72,.4,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","screening",Collections.singletonList(c)).withLabQuality(quality);
        JSONObject json=r.toJson();assertEquals("chemcore-result/3",json.getString("schema"));assertTrue(json.isNull("ppm"));assertFalse(json.getBoolean("ppm_reportable"));
        assertEquals("m4_device_reference_calibration_required",json.getString("ppm_withheld_reason"));
        JSONObject row=json.getJSONArray("candidates").getJSONObject(0);assertTrue(row.isNull("agreement_pct"));assertEquals(35.0,row.getDouble("screening_fit_ppm"),1e-9);
        assertFalse(row.getBoolean("device_quantitative_support"));assertEquals(0.12,row.getJSONObject("residual_log10_by_sensor").getDouble("mq3"),1e-9);
        assertTrue(json.has("lab_quality"));assertTrue(json.has("calibration_gate"));
    }

    @Test public void candidateRankingExportsRunnerUpAndSeparation()throws Exception{
        ChemCoreResultEnvelope.CandidateSummary a=new ChemCoreResultEnvelope.CandidateSummary(1,"A",.75,10,.2,3,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,37.5,98,85,0,true,Collections.emptyMap());
        ChemCoreResultEnvelope.CandidateSummary b=new ChemCoreResultEnvelope.CandidateSummary(2,"B",.20,12,.5,2,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,25,88,60,0,true,Collections.emptyMap());
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("S2",ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,1L,"A",Double.NaN,Double.NaN,Double.NaN,1,.75,.2,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","id",Arrays.asList(a,b));
        JSONObject json=r.toJson();assertEquals(.20,json.getDouble("runner_up_posterior"),1e-9);assertEquals(.55,json.getDouble("posterior_separation_margin"),1e-9);assertEquals(2,json.getInt("candidate_count"));
        assertEquals(1,json.getJSONArray("candidates").getJSONObject(0).getInt("rank"));
    }
}
