package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class GasPpmExposureAssessmentTest {
    private static JSONObject quantified(double ppm)throws Exception{return new JSONObject().put("outcome","QUANTIFIED").put("evidence_tier","M4_HAZROB_VALIDATED").put("chemical_uid",77).put("ppm",ppm).put("ppm_reportable",true).put("quantitative_occupational_interpretation_permitted",true);}
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
        assertEquals("IDLH_REFERENCE_CROSSED",a.getString("state"));assertEquals(2,a.getInt("crossed_reference_count"));assertEquals(1,a.getInt("action_relevant_crossed_count"));assertEquals("NOT_MADE",a.getString("compliance_determination"));
        assertFalse(a.getJSONArray("references").getJSONObject(1).getBoolean("duration_coverage_satisfied"));
    }

    @Test public void shortCaptureCannotTurnStelIntoComplianceDecision()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(20),Collections.singletonList(limit(1,"NIOSH","STEL",10,"ppm",15.0)),30_000);
        assertEquals("REFERENCE_CONTEXT_CROSSED",a.getString("state"));assertEquals(1,a.getInt("crossed_reference_count"));assertEquals(0,a.getInt("action_relevant_crossed_count"));assertFalse(a.getJSONArray("references").getJSONObject(0).getBoolean("duration_coverage_satisfied"));
    }

    @Test public void absenceOfPpmReferencesIsNotReportedAsSafe()throws Exception{
        JSONObject a=GasPpmExposureAssessment.evaluate(quantified(1),Collections.singletonList(limit(1,"OSHA","TWA",1,"mg/m3",480.0)),30_000);
        assertEquals("NO_PPM_REFERENCES",a.getString("state"));assertEquals(0,a.getInt("ppm_reference_count"));assertEquals(1,a.getInt("excluded_non_ppm_count"));
    }
}
