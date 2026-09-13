package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class GasPpmExposureAssessmentTest {
    private static JSONObject quantified(double ppm)throws Exception{return new JSONObject().put("outcome","QUANTIFIED").put("evidence_tier","M4_HAZROB_VALIDATED").put("chemical_uid",77).put("chemical","Test gas").put("ppm",ppm).put("ppm_reportable",true).put("quantitative_occupational_interpretation_permitted",true);}
    private static ExposureRepository.Limit limit(long id,String authority,String type,double value,String unit,Double duration){return new ExposureRepository.Limit(id,77,authority,"US",type,value,unit,duration,"","SRC","R1");}

    @Test public void massConcentrationLimitsAreExcludedWithoutConversion()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(10),Arrays.asList(limit(1,"OSHA","TWA",5,"mg/m3",480.0),limit(2,"OSHA","TWA",50,"ppm",480.0)),30_000);
        assertEquals("BELOW_AVAILABLE_PPM_REFERENCES",a.getString("state"));assertEquals(1,a.getInt("ppm_reference_count"));assertEquals(1,a.getInt("excluded_non_ppm_count"));assertEquals(0.2,a.getDouble("max_ratio_to_reference"),1e-9);
        JSONArray refs=a.getJSONArray("references");assertEquals(1,refs.length());assertEquals("ppm",refs.getJSONObject(0).getString("unit"));
    }

    @Test public void m4AndReportablePpmAreMandatory()throws Exception{
        JSONObject p=quantified(10).put("evidence_tier","M3_ARRAY_CONSISTENT");JSONObject a=GasPpmExposureAssessment.evaluate(p,Collections.singletonList(limit(1,"OSHA","TWA",5,"ppm",480.0)),30_000);
        assertEquals("WITHHELD",a.getString("state"));assertEquals("m4_device_reference_calibration_required",a.getString("reason"));assertEquals(0,a.getJSONArray("references").length());
    }

    @Test public void idlhCrossingHasPriorityButNeverClaimsCompliance()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(60),Arrays.asList(limit(1,"NIOSH","IDLH",50,"ppm",30.0),limit(2,"OSHA","TWA",10,"ppm",480.0)),30_000);
        assertEquals("IDLH_REFERENCE_CROSSED",a.getString("state"));assertEquals(2,a.getInt("crossed_reference_count"));assertEquals(1,a.getInt("action_relevant_crossed_count"));assertEquals("NOT_MADE",a.getString("compliance_determination"));assertTrue(a.getBoolean("toxic_reference_crossed"));assertTrue(a.getString("hazard_symbols").contains("☠"));
        assertFalse(a.getJSONArray("references").getJSONObject(1).getBoolean("duration_coverage_satisfied"));
    }

    @Test public void shortCaptureCrossingStelShowsToxicContextButNotCompliance()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(20),Collections.singletonList(limit(1,"NIOSH","STEL",10,"ppm",15.0)),30_000);
        assertEquals("REFERENCE_CONTEXT_CROSSED",a.getString("state"));assertEquals(1,a.getInt("crossed_reference_count"));assertEquals(0,a.getInt("action_relevant_crossed_count"));assertFalse(a.getJSONArray("references").getJSONObject(0).getBoolean("duration_coverage_satisfied"));assertTrue(a.getBoolean("toxic_reference_crossed"));assertTrue(a.getBoolean("health_reference_context_warning"));assertTrue(a.getString("hazard_symbols").contains("☠"));assertEquals("NOT_MADE",a.getString("compliance_determination"));
    }

    @Test public void lflUsesVolPercentToPpmAndFlameOnlyAtFullLfl()throws Exception{
        ExposureRepository.Limit lfl=limit(3,"NIOSH","LFL",1.2,"%vol",null);
        JSONObject below=GasPpmExposureAssessment.evaluate(quantified(6000),Collections.singletonList(lfl),30_000);assertEquals(50.0,below.getDouble("percent_lfl"),1e-9);assertFalse(below.getBoolean("flammability_lfl_reached"));assertFalse(below.getString("hazard_symbols").contains("🔥"));assertEquals("BELOW_LFL",below.getString("flammability_state"));
        JSONObject at=GasPpmExposureAssessment.evaluate(quantified(12000),Collections.singletonList(lfl),30_000);assertEquals(100.0,at.getDouble("percent_lfl"),1e-9);assertTrue(at.getBoolean("flammability_lfl_reached"));assertTrue(at.getString("hazard_symbols").contains("🔥"));assertEquals("LFL_REACHED_OR_EXCEEDED",at.getString("flammability_state"));
    }

    @Test public void hazardPriorityUsesLargestRelativeHazardNotAbsolutePpm()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(6),Arrays.asList(limit(1,"NIOSH","CEILING",5,"ppm",null),limit(2,"NIOSH","LFL",2.0,"%vol",null)),30_000);
        assertEquals(1.2,a.getDouble("toxicity_priority_ratio"),1e-9);assertEquals(0.0003,a.getDouble("lfl_ratio"),1e-9);assertEquals(1.2,a.getDouble("hazard_priority_ratio"),1e-9);assertEquals("TOXICITY",a.getString("hazard_priority_basis"));
    }

    @Test public void absenceOfPpmAndLflReferencesIsNotReportedAsSafe()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(1),Collections.singletonList(limit(1,"OSHA","TWA",1,"mg/m3",480.0)),30_000);
        assertEquals("NO_PPM_REFERENCES",a.getString("state"));assertEquals("NO_LFL_REFERENCE",a.getString("flammability_state"));assertEquals(0,a.getInt("ppm_reference_count"));assertEquals(1,a.getInt("excluded_non_ppm_count"));assertTrue(a.isNull("hazard_priority_ratio"));
    }
}
