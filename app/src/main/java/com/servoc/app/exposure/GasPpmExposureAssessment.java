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
 * Fail-closed gas safety assessment for HazRob.
 *
 * Toxicological exposure references participate only when explicitly expressed in ppm. Mass-
 * concentration limits are retained in the knowledge base but are never silently converted.
 * Flammability references are handled separately as LFL/LEL and UFL/UEL percent-by-volume values;
 * 1 vol% is exactly 10,000 ppmv for the ratio calculation used here.
 *
 * Safety priority is dimensionless and relative to the substance-specific controlling reference:
 * max(toxicity ratio, flammability ratio). Absolute ppm is always reported but is never the safety
 * sort key. TWA/STEL comparisons remain temporal context unless their averaging duration is covered;
 * this class never declares legal compliance from a single HazRob capture.
 */
public final class GasPpmExposureAssessment {
    public static final String SCHEMA="hazrob-gas-exposure-ppm/2";
    public static final String KEY="gas_exposure_ppm";
    private static final double PPM_PER_VOL_PERCENT=10_000.0;
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
            JSONObject out=base(measurementDurationMs);out.put("input_gate_passed",true);out.put("chemical_uid",uid);out.put("chemical",chemcore.optString("chemical",""));out.put("ppm",ppm);out.put("measurement_source","chemcore_m4_reportable_ppm");

            JSONArray refs=new JSONArray(),flammabilityRefs=new JSONArray();Set<String> authorities=new LinkedHashSet<>();
            int nonPpm=0,invalid=0,crossed=0,actionable=0,ppmCount=0,lflCount=0,uflCount=0;
            double toxicMax=Double.NaN,lflMaxRatio=Double.NaN,uflPct=Double.NaN;JSONObject toxicMaxRef=null,lflMaxRef=null;
            boolean idlhCrossed=false,ceilingCrossed=false,actionableShortCrossed=false,contextCrossed=false;

            for(ExposureRepository.Limit l:limits){
                if(l==null||!l.numeric()||l.value==null||!Double.isFinite(l.value)||l.value<=0){invalid++;continue;}
                String flammabilityRole=flammabilityRole(l.type);
                if(flammabilityRole!=null){
                    if(!isVolPercentUnit(l.unit)){invalid++;continue;}
                    double boundaryPpm=l.value*PPM_PER_VOL_PERCENT;
                    double ratio=ppm/boundaryPpm;
                    JSONObject f=new JSONObject();f.put("authority",nz(l.authority));f.put("jurisdiction",nz(l.jurisdiction));f.put("limit_type",nz(l.type));f.put("role",flammabilityRole);f.put("value_vol_pct",l.value);f.put("boundary_ppm",boundaryPpm);f.put("ratio_to_boundary",ratio);f.put("percent_of_boundary",ratio*100.0);f.put("source_id",nz(l.sourceId));f.put("revision",nz(l.revision));f.put("notation",nz(l.notation));flammabilityRefs.put(f);
                    if("LFL".equals(flammabilityRole)){lflCount++;if(!Double.isFinite(lflMaxRatio)||ratio>lflMaxRatio){lflMaxRatio=ratio;lflMaxRef=new JSONObject(f.toString());}}
                    else{uflCount++;if(!Double.isFinite(uflPct)||l.value<uflPct)uflPct=l.value;}
                    if(l.authority!=null&&!l.authority.trim().isEmpty())authorities.add(l.authority.trim());
                    continue;
                }

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
                if(!Double.isFinite(toxicMax)||ratio>toxicMax){toxicMax=ratio;toxicMaxRef=new JSONObject(r.toString());}
            }

            String toxicityState;if(ppmCount==0)toxicityState="NO_PPM_REFERENCES";else if(idlhCrossed)toxicityState="IDLH_REFERENCE_CROSSED";else if(ceilingCrossed)toxicityState="CEILING_REFERENCE_CROSSED";else if(actionableShortCrossed)toxicityState="DURATION_COVERED_REFERENCE_CROSSED";else if(contextCrossed)toxicityState="REFERENCE_CONTEXT_CROSSED";else toxicityState="BELOW_AVAILABLE_PPM_REFERENCES";
            boolean lflCrossed=Double.isFinite(lflMaxRatio)&&lflMaxRatio>=1.0;String flammabilityState=lflCount==0?"NO_LFL_REFERENCE":lflCrossed?"LFL_REACHED_OR_EXCEEDED":"BELOW_LFL";
            boolean skull=actionable>0,contextWarning=contextCrossed&&!skull,flame=lflCrossed;
            double hazard=maximumFinite(toxicMax,lflMaxRatio);String basis=hazardBasis(toxicMax,lflMaxRatio);

            out.put("state",toxicityState);out.put("toxicity_state",toxicityState);out.put("flammability_state",flammabilityState);out.put("source_limit_count",limits.size());out.put("ppm_reference_count",ppmCount);out.put("lfl_reference_count",lflCount);out.put("ufl_reference_count",uflCount);out.put("excluded_non_ppm_count",nonPpm);out.put("excluded_invalid_count",invalid);out.put("crossed_reference_count",crossed);out.put("action_relevant_crossed_count",actionable);
            putFiniteOrNull(out,"toxicity_priority_ratio",toxicMax);putFiniteOrNull(out,"toxicity_priority_percent",Double.isFinite(toxicMax)?toxicMax*100.0:Double.NaN);if(toxicMaxRef!=null){out.put("highest_ratio_reference",toxicMaxRef);out.put("controlling_toxicity_reference",new JSONObject(toxicMaxRef.toString()));}
            putFiniteOrNull(out,"lfl_ratio",lflMaxRatio);putFiniteOrNull(out,"percent_lfl",Double.isFinite(lflMaxRatio)?lflMaxRatio*100.0:Double.NaN);out.put("lfl_threshold_crossed",lflCrossed);if(lflMaxRef!=null){out.put("controlling_lfl_reference",lflMaxRef);out.put("lfl_vol_pct",lflMaxRef.optDouble("value_vol_pct",Double.NaN));out.put("lfl_ppm",lflMaxRef.optDouble("boundary_ppm",Double.NaN));}
            if(Double.isFinite(uflPct)){out.put("ufl_vol_pct",uflPct);out.put("ufl_ppm",uflPct*PPM_PER_VOL_PERCENT);}else{out.put("ufl_vol_pct",JSONObject.NULL);out.put("ufl_ppm",JSONObject.NULL);}
            putFiniteOrNull(out,"hazard_priority_ratio",hazard);putFiniteOrNull(out,"hazard_priority_percent",Double.isFinite(hazard)?hazard*100.0:Double.NaN);out.put("hazard_priority_basis",basis);
            out.put("toxic_action_relevant",skull);out.put("health_reference_context_warning",contextWarning);out.put("flammability_lfl_reached",flame);out.put("hazard_symbols",symbols(skull,flame,contextWarning));
            out.put("references",refs);out.put("flammability_references",flammabilityRefs);
            JSONArray auth=new JSONArray();for(String a:authorities)auth.put(a);out.put("authorities",auth);
            out.put("compliance_determination","NOT_MADE");out.put("interpretation","Safety priority is relative to substance-specific ppm exposure and LFL references, not absolute ppm. TWA/STEL legal compliance requires the applicable averaging duration and sampling method; no compliance claim is produced from a single HazRob capture.");
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

    private static JSONObject withheld(String reason,long durationMs){try{JSONObject o=base(durationMs);o.put("input_gate_passed",false);o.put("state","WITHHELD");o.put("toxicity_state","WITHHELD");o.put("flammability_state","WITHHELD");o.put("reason",reason);o.put("source_limit_count",0);o.put("ppm_reference_count",0);o.put("lfl_reference_count",0);o.put("ufl_reference_count",0);o.put("excluded_non_ppm_count",0);o.put("excluded_invalid_count",0);o.put("crossed_reference_count",0);o.put("action_relevant_crossed_count",0);o.put("hazard_priority_ratio",JSONObject.NULL);o.put("hazard_priority_percent",JSONObject.NULL);o.put("hazard_priority_basis","WITHHELD");o.put("hazard_symbols","");o.put("references",new JSONArray());o.put("flammability_references",new JSONArray());o.put("compliance_determination","NOT_MADE");return o;}catch(Exception e){return new JSONObject();}}
    private static JSONObject base(long durationMs)throws Exception{JSONObject o=new JSONObject();o.put("schema",SCHEMA);o.put("scope","gas_phase_occupational_and_flammability_safety");o.put("measurement_unit","ppm");o.put("measurement_duration_ms",Math.max(0,durationMs));o.put("unit_policy","TOXICOLOGY_PPM_ONLY_NO_MASS_CONCENTRATION_CONVERSION");o.put("flammability_conversion","1 vol% = 10000 ppmv");return o;}
    private static boolean isPpmUnit(String unit){if(unit==null)return false;String u=unit.trim().toLowerCase(Locale.US).replaceAll("\\s+"," ");return "ppm".equals(u)||"parts per million".equals(u);}
    private static boolean isVolPercentUnit(String unit){if(unit==null)return false;String u=unit.trim().toLowerCase(Locale.US).replaceAll("\\s+","");return "%vol".equals(u)||"vol%".equals(u)||"%v/v".equals(u)||"%".equals(u)||"percentbyvolume".equals(u);}
    private static String flammabilityRole(String type){String t=nz(type).trim().toUpperCase(Locale.US).replace('-','_').replace(' ','_');if("LFL".equals(t)||"LEL".equals(t)||t.contains("LOWER_FLAMMABLE")||t.contains("LOWER_EXPLOSIVE"))return "LFL";if("UFL".equals(t)||"UEL".equals(t)||t.contains("UPPER_FLAMMABLE")||t.contains("UPPER_EXPLOSIVE"))return "UFL";return null;}
    private static String temporalRole(String type){String t=nz(type).trim().toUpperCase(Locale.US).replace('-','_');if(t.contains("IDLH"))return "IDLH";if(t.contains("CEILING")||"C".equals(t))return "CEILING";if(t.contains("STEL")||t.contains("SHORT_TERM"))return "STEL";if(t.contains("TWA")||t.contains("PEL")||t.contains("REL"))return "TWA";return "OTHER";}
    private static boolean isImmediateRole(String role){return "IDLH".equals(role)||"CEILING".equals(role);}
    private static boolean durationCovered(String role,Double durationMin,long measurementDurationMs){if(isImmediateRole(role))return true;if(durationMin==null||!Double.isFinite(durationMin)||durationMin<=0)return false;return measurementDurationMs>=Math.round(durationMin*60_000.0);}
    private static String complianceInterpretation(String role,boolean covered){if("IDLH".equals(role))return "emergency_reference_not_compliance_limit";if("CEILING".equals(role))return "ceiling_reference_screening_comparison";if("STEL".equals(role)||"TWA".equals(role))return covered?"averaging_duration_covered_but_sampling_method_still_requires_review":"duration_context_not_covered";return "reference_only_no_compliance_claim";}
    private static double maximumFinite(double a,double b){if(Double.isFinite(a)&&Double.isFinite(b))return Math.max(a,b);if(Double.isFinite(a))return a;if(Double.isFinite(b))return b;return Double.NaN;}
    private static String hazardBasis(double toxic,double flame){if(!Double.isFinite(toxic)&&!Double.isFinite(flame))return "NO_REFERENCE";if(Double.isFinite(toxic)&&Double.isFinite(flame)){double d=Math.abs(toxic-flame);double scale=Math.max(1e-12,Math.max(Math.abs(toxic),Math.abs(flame)));if(d/scale<=1e-9)return "BOTH";return toxic>flame?"TOXICITY":"FLAMMABILITY";}return Double.isFinite(toxic)?"TOXICITY":"FLAMMABILITY";}
    private static String symbols(boolean skull,boolean flame,boolean context){StringBuilder s=new StringBuilder();if(skull)s.append("☠");if(flame)s.append("🔥");if(context&&!skull)s.append("⚠");return s.toString();}
    private static void putFiniteOrNull(JSONObject o,String key,double v)throws Exception{o.put(key,Double.isFinite(v)?v:JSONObject.NULL);}
    private static String nz(String s){return s==null?"":s;}
}
