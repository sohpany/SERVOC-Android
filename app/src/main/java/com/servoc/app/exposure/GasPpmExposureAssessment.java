package com.servoc.app.exposure;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fail-closed occupational gas exposure reference assessment for HazRob.
 *
 * Only concentration limits explicitly expressed in ppm participate. Mass-concentration
 * limits (mg/m3, ug/m3, fibres, dust, etc.) are retained in the scientific database but are
 * intentionally excluded here: this component never performs an implicit mass-to-ppm conversion.
 * A single HazRob capture is compared with authoritative reference values, but is never labelled
 * legal compliance/non-compliance because TWA/STEL semantics require the applicable averaging time.
 */
public final class GasPpmExposureAssessment {
    public static final String SCHEMA="hazrob-gas-exposure-ppm/1";
    public static final String KEY="gas_exposure_ppm";
    private GasPpmExposureAssessment(){}

    public static JSONObject evaluate(ExperimentsDb db,JSONObject chemcore,long measurementDurationMs){
        if(chemcore==null)return withheld("missing_chemcore_result",measurementDurationMs);
        String gate=gateReason(chemcore);if(gate!=null)return withheld(gate,measurementDurationMs);
        if(db==null)return withheld("exposure_database_unavailable",measurementDurationMs);
        long uid=chemcore.optLong("chemical_uid",-1);
        try{return evaluate(chemcore,new ExposureRepository(db).forChemical(uid),measurementDurationMs);}
        catch(Exception e){return withheld("exposure_repository_failure",measurementDurationMs);}
    }

    /** Pure-Java overload used by regression tests and deterministic report logic. */
    public static JSONObject evaluate(JSONObject chemcore,List<ExposureRepository.Limit> source,long measurementDurationMs){
        String gate=gateReason(chemcore);if(gate!=null)return withheld(gate,measurementDurationMs);
        List<ExposureRepository.Limit> limits=source==null?Collections.emptyList():source;
        try{
            double ppm=chemcore.getDouble("ppm");long uid=chemcore.getLong("chemical_uid");
            JSONObject out=base(measurementDurationMs);out.put("input_gate_passed",true);out.put("chemical_uid",uid);out.put("ppm",ppm);
            out.put("measurement_source","chemcore_m4_reportable_ppm");
            JSONArray refs=new JSONArray();Set<String> authorities=new LinkedHashSet<>();
            int nonPpm=0,invalid=0,crossed=0,actionable=0,ppmCount=0;double maxRatio=Double.NaN;JSONObject maxRef=null;
            boolean idlhCrossed=false,ceilingCrossed=false,actionableShortCrossed=false,contextCrossed=false;
            for(ExposureRepository.Limit l:limits){
                if(l==null||!l.numeric()||l.value==null||!Double.isFinite(l.value)||l.value<=0){invalid++;continue;}
                if(!isPpmUnit(l.unit)){nonPpm++;continue;}
                ppmCount++;String role=temporalRole(l.type);double ratio=ppm/l.value;boolean hit=ratio>=1.0;
                boolean durationCovered=durationCovered(role,l.durationMin,measurementDurationMs);
                boolean actionRelevant=hit&&(isImmediateRole(role)||durationCovered);
                if(hit){crossed++;contextCrossed=true;}if(actionRelevant)actionable++;
                if(hit&&"IDLH".equals(role))idlhCrossed=true;if(hit&&"CEILING".equals(role))ceilingCrossed=true;
                if(actionRelevant&&("STEL".equals(role)||"TWA".equals(role)||"OTHER".equals(role)))actionableShortCrossed=true;
                JSONObject r=new JSONObject();r.put("authority",nz(l.authority));r.put("jurisdiction",nz(l.jurisdiction));r.put("limit_type",nz(l.type));r.put("value_ppm",l.value);r.put("unit","ppm");
                if(l.durationMin!=null&&Double.isFinite(l.durationMin)&&l.durationMin>0)r.put("duration_min",l.durationMin);else r.put("duration_min",JSONObject.NULL);
                r.put("temporal_role",role);r.put("ratio_to_reference",ratio);r.put("percent_of_reference",ratio*100.0);r.put("reference_crossed",hit);r.put("duration_coverage_satisfied",durationCovered);r.put("action_relevant_reference_crossed",actionRelevant);
                r.put("compliance_interpretation",complianceInterpretation(role,durationCovered));r.put("notation",nz(l.notation));r.put("source_id",nz(l.sourceId));r.put("revision",nz(l.revision));refs.put(r);
                if(l.authority!=null&&!l.authority.trim().isEmpty())authorities.add(l.authority.trim());
                if(!Double.isFinite(maxRatio)||ratio>maxRatio){maxRatio=ratio;maxRef=new JSONObject(r.toString());}
            }
            String state;if(ppmCount==0)state="NO_PPM_REFERENCES";else if(idlhCrossed)state="IDLH_REFERENCE_CROSSED";else if(ceilingCrossed)state="CEILING_REFERENCE_CROSSED";else if(actionableShortCrossed)state="DURATION_COVERED_REFERENCE_CROSSED";else if(contextCrossed)state="REFERENCE_CONTEXT_CROSSED";else state="BELOW_AVAILABLE_PPM_REFERENCES";
            out.put("state",state);out.put("source_limit_count",limits.size());out.put("ppm_reference_count",ppmCount);out.put("excluded_non_ppm_count",nonPpm);out.put("excluded_invalid_count",invalid);out.put("crossed_reference_count",crossed);out.put("action_relevant_crossed_count",actionable);
            if(Double.isFinite(maxRatio))out.put("max_ratio_to_reference",maxRatio);else out.put("max_ratio_to_reference",JSONObject.NULL);if(maxRef!=null)out.put("highest_ratio_reference",maxRef);
            JSONArray auth=new JSONArray();for(String a:authorities)auth.put(a);out.put("authorities",auth);out.put("references",refs);
            out.put("compliance_determination","NOT_MADE");out.put("interpretation","Reference comparison only. TWA/STEL compliance requires the applicable averaging duration and sampling method; no legal compliance claim is produced from a single HazRob capture.");
            return out;
        }catch(Exception e){return withheld("assessment_serialization_failure",measurementDurationMs);}
    }

    private static String gateReason(JSONObject p){
        if(p==null)return "missing_chemcore_result";
        if(!p.optBoolean("ppm_reportable",false))return "ppm_not_reportable";
        if(!p.optBoolean("quantitative_occupational_interpretation_permitted",false))return "occupational_quantitative_gate_not_passed";
        if(!"QUANTIFIED".equals(p.optString("outcome","")))return "quantified_identity_required";
        if(!"M4_HAZROB_VALIDATED".equals(p.optString("evidence_tier","")))return "m4_device_reference_calibration_required";
        if(!p.has("chemical_uid")||p.optLong("chemical_uid",-1)<0)return "chemical_uid_required";
        double ppm=p.optDouble("ppm",Double.NaN);if(!Double.isFinite(ppm)||ppm<0)return "finite_nonnegative_ppm_required";
        return null;
    }

    private static JSONObject withheld(String reason,long durationMs){try{JSONObject o=base(durationMs);o.put("input_gate_passed",false);o.put("state","WITHHELD");o.put("reason",reason);o.put("source_limit_count",0);o.put("ppm_reference_count",0);o.put("excluded_non_ppm_count",0);o.put("excluded_invalid_count",0);o.put("crossed_reference_count",0);o.put("action_relevant_crossed_count",0);o.put("references",new JSONArray());o.put("compliance_determination","NOT_MADE");return o;}catch(Exception e){return new JSONObject();}}
    private static JSONObject base(long durationMs)throws Exception{JSONObject o=new JSONObject();o.put("schema",SCHEMA);o.put("scope","gas_phase_occupational_exposure");o.put("measurement_unit","ppm");o.put("measurement_duration_ms",Math.max(0,durationMs));o.put("unit_policy","PPM_ONLY_NO_MASS_CONCENTRATION_CONVERSION");return o;}
    private static boolean isPpmUnit(String unit){if(unit==null)return false;String u=unit.trim().toLowerCase(Locale.US).replaceAll("\\s+"," ");return "ppm".equals(u)||"parts per million".equals(u);}
    private static String temporalRole(String type){String t=nz(type).trim().toUpperCase(Locale.US).replace('-','_');if(t.contains("IDLH"))return "IDLH";if(t.contains("CEILING")||"C".equals(t))return "CEILING";if(t.contains("STEL")||t.contains("SHORT_TERM"))return "STEL";if(t.contains("TWA")||t.contains("PEL")||t.contains("REL"))return "TWA";return "OTHER";}
    private static boolean isImmediateRole(String role){return "IDLH".equals(role)||"CEILING".equals(role);}
    private static boolean durationCovered(String role,Double durationMin,long measurementDurationMs){if(isImmediateRole(role))return true;if(durationMin==null||!Double.isFinite(durationMin)||durationMin<=0)return false;return measurementDurationMs>=Math.round(durationMin*60_000.0);}
    private static String complianceInterpretation(String role,boolean covered){if("IDLH".equals(role))return "emergency_reference_not_compliance_limit";if("CEILING".equals(role))return "ceiling_reference_screening_comparison";if("STEL".equals(role)||"TWA".equals(role))return covered?"averaging_duration_covered_but_sampling_method_still_requires_review":"duration_context_not_covered";return "reference_only_no_compliance_claim";}
    private static String nz(String s){return s==null?"":s;}
}
