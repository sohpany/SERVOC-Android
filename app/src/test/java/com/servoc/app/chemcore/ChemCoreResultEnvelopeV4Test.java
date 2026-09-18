package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChemCoreResultEnvelopeV4Test {
    @Test public void singleSensorAgreementIsNullAndScreeningPpmIsWithheld()throws Exception{
        Map<String,Double> residuals=new LinkedHashMap<>();residuals.put("mq3",0.12);
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(1,"benzene",0.72,35.0,0.12,1,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,12.5,Double.NaN,60.0,0,true,residuals);
        JSONObject quality=new JSONObject().put("schema",ChemCoreLabQuality.SCHEMA).put("calibration_gate",new JSONObject()
                .put("field_r0_normalization_ready",true).put("reference_gas_validated_channels",0));
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("S1",ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                1L,"benzene",Double.NaN,Double.NaN,Double.NaN,2.0,.72,.4,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","screening",Collections.singletonList(c)).withLabQuality(quality);
        JSONObject json=r.toJson();assertEquals("chemcore-result/4",json.getString("schema"));
        assertEquals("PENDING_ENGINE",json.getJSONObject("screening_144").getString("state"));
        assertEquals(144,json.getJSONObject("screening_144").getInt("target_count"));
        assertEquals(0,json.getJSONObject("screening_144").getInt("evaluated_count"));
        assertEquals(0,json.getJSONObject("screening_144").getJSONArray("rows").length());
        assertEquals("chemcore-features/1",json.getJSONObject("feature_summary").getString("schema"));
        assertEquals("relative_model_ranking_weight_not_calibrated_identity_probability",json.getJSONObject("methodology").getString("posterior_semantics"));
        assertTrue(json.has("quality_gates"));assertTrue(json.has("source_refs"));assertTrue(json.has("accepted_findings"));assertTrue(json.has("safety_findings"));assertTrue(json.isNull("ppm"));assertFalse(json.getBoolean("ppm_reportable"));
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

    @Test public void candidateQuantitativeSupportRequiresEveryContributorM4()throws Exception{
        ChemCoreResultEnvelope.CandidateSummary partial=new ChemCoreResultEnvelope.CandidateSummary(1,"partial",.7,20,.2,3,
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,37.5,90,100,1,true,Collections.emptyMap());
        ChemCoreResultEnvelope.CandidateSummary complete=new ChemCoreResultEnvelope.CandidateSummary(2,"complete",.3,20,.2,3,
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,37.5,90,100,3,true,Collections.emptyMap());
        assertFalse(partial.hasDeviceQuantitativeSupport());assertTrue(complete.hasDeviceQuantitativeSupport());
        ChemCoreResultEnvelope r=new ChemCoreResultEnvelope("S3",ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,1L,"partial",Double.NaN,Double.NaN,Double.NaN,1,.7,.2,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","gate",Arrays.asList(partial,complete));
        JSONObject rows=r.toJson();assertFalse(rows.getJSONArray("candidates").getJSONObject(0).getBoolean("device_quantitative_support"));assertTrue(rows.getJSONArray("candidates").getJSONObject(1).getBoolean("device_quantitative_support"));
    }
}