package com.servoc.app.report;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Candidate-level hazard context for intelligent reports.
 *
 * Scientific boundary:
 * - The accepted M4 identity may reuse the persisted gas_exposure_ppm interpretation.
 * - A candidate screening_fit_ppm is an analytical fit hypothesis only. It may be compared with
 *   directly comparable ppm and LFL/UFL references for prioritization/context, but it MUST NOT
 *   create an occupational exposure exceedance, compliance decision, or safety alarm.
 * - Mass-concentration references are preserved but are not converted for screening candidates;
 *   condition-aware conversion belongs to the validated M4 exposure path.
 */
public final class CandidateHazardContext {
    private CandidateHazardContext(){}

    public static final String SCHEMA="hazrob-candidate-hazard-context/1";
    public static final String SCREENING_SEMANTICS="screening_fit_ppm_context_only_not_reportable_exposure_measurement";
    public static final String M4_SEMANTICS="m4_reportable_ppm_with_persisted_exposure_and_flammability_assessment";

    public static JSONObject evaluate(ExperimentsDb db,JSONObject chemcore){
        JSONObject out=new JSONObject();
        try{
            out.put("schema",SCHEMA);
            JSONArray rows=new JSONArray();out.put("candidates",rows);
            if(db==null||chemcore==null){out.put("state","UNAVAILABLE");out.put("reason",db==null?"database_unavailable":"chemcore_result_missing");return out;}
            JSONArray candidates=chemcore.optJSONArray("candidates");if(candidates==null||candidates.length()==0){out.put("state","EMPTY");out.put("candidate_count",0);return out;}
            ExposureRepository exposure=new ExposureRepository(db);ChemicalRepository chemicals=new ChemicalRepository(db);
            long acceptedUid=chemcore.optLong("chemical_uid",-1);boolean acceptedM4=chemcore.optBoolean("ppm_reportable",false)&&"QUANTIFIED".equals(chemcore.optString("outcome",""))&&"M4_HAZROB_VALIDATED".equals(chemcore.optString("evidence_tier",""))&&Double.isFinite(chemcore.optDouble("ppm",Double.NaN));
            JSONObject acceptedGas=chemcore.optJSONObject("gas_exposure_ppm");
            int contextual=0,m4=0;
            for(int i=0;i<candidates.length();i++){
                JSONObject candidate=candidates.optJSONObject(i);if(candidate==null)continue;long uid=candidate.optLong("chemical_uid",-1);if(uid<=0)continue;
                ChemicalRepository.Chemical chemical=chemicals.byUid(uid);List<ExposureRepository.Limit> limits=exposure.forChemical(uid);
                boolean isAcceptedM4=acceptedM4&&uid==acceptedUid;double ppm=isAcceptedM4?chemcore.optDouble("ppm",Double.NaN):candidate.optDouble("screening_fit_ppm",candidate.optDouble("best_ppm",Double.NaN));
                JSONObject row=new JSONObject();row.put("rank",candidate.optInt("rank",i+1));row.put("chemical_uid",uid);row.put("label",candidate.optString("label",chemical==null?"":chemical.name));
                if(chemical!=null){row.put("name_en",nz(chemical.name));row.put("name_ar",nz(chemical.arabicName));row.put("cas",nz(chemical.cas));row.put("formula",nz(chemical.formula));row.put("chemical_class",nz(chemical.chemicalClass));if(chemical.molarMass!=null&&Double.isFinite(chemical.molarMass))row.put("molar_mass_g_mol",chemical.molarMass);}
                row.put("concentration_basis",isAcceptedM4?"M4_MEASURED_REPORTABLE":"SCREENING_FIT_ESTIMATE");row.put("concentration_semantics",isAcceptedM4?M4_SEMANTICS:SCREENING_SEMANTICS);putFiniteOrNull(row,"concentration_ppm",ppm);
                row.put("occupational_interpretation_permitted",isAcceptedM4);row.put("compliance_determination","NOT_MADE");
                putFiniteOrNull(row,"ranking_weight",candidate.optDouble("posterior",Double.NaN));putFiniteOrNull(row,"analytical_support_index_pct",candidate.optDouble("analytical_confidence_pct",Double.NaN));row.put("evidence_tier",candidate.optString("evidence_tier",""));

                JSONObject toxic=toxicityContext(limits,ppm,isAcceptedM4);JSONObject flame=flammabilityContext(limits,ppm,isAcceptedM4);row.put("toxicity",toxic);row.put("flammability",flame);
                row.put("hazard_types",hazardTypes(toxic,flame));row.put("hazard_symbols",symbols(toxic,flame));row.put("priority_class",priorityClass(toxic,flame,isAcceptedM4));
                if(isAcceptedM4&&acceptedGas!=null&&acceptedGas.optBoolean("input_gate_passed",false)){row.put("validated_gas_assessment",new JSONObject(acceptedGas.toString()));m4++;}else contextual++;
                rows.put(row);
            }
            out.put("state","EVALUATED");out.put("candidate_count",rows.length());out.put("m4_candidate_count",m4);out.put("screening_context_candidate_count",contextual);out.put("screening_warning","Estimated candidate ppm comparisons are prioritization context only; no occupational exceedance or flammability alarm is asserted without the M4 gate.");return out;
        }catch(Exception e){try{out.put("state","ERROR");out.put("reason","candidate_hazard_context_failure");}catch(Exception ignored){}return out;}
    }

    private static JSONObject toxicityContext(List<ExposureRepository.Limit> limits,double ppm,boolean m4)throws Exception{
        JSONObject out=new JSONObject();JSONArray refs=new JSONArray();double maxRatio=Double.NaN;JSONObject controlling=null;int direct=0,mass=0;
        if(limits==null)limits=new ArrayList<>();
        for(ExposureRepository.Limit l:limits){if(l==null||!l.numeric()||l.value==null||l.value<=0)continue;if(isFlammability(l.type))continue;if(isPpm(l.unit)){direct++;double ratio=Double.isFinite(ppm)?ppm/l.value:Double.NaN;JSONObject r=reference(l);r.put("comparable_to_candidate_ppm",true);putFiniteOrNull(r,"ratio_to_reference",ratio);putFiniteOrNull(r,"percent_of_reference",Double.isFinite(ratio)?ratio*100.0:Double.NaN);r.put("interpretation",m4?"M4_CURRENT_CONCENTRATION_CONTEXT":"SCREENING_CONTEXT_ONLY_NO_TEMPORAL_EXPOSURE_CLAIM");refs.put(r);if(Double.isFinite(ratio)&&(!Double.isFinite(maxRatio)||ratio>maxRatio)){maxRatio=ratio;controlling=new JSONObject(r.toString());}}else if(isMass(l.unit)){mass++;JSONObject r=reference(l);r.put("comparable_to_candidate_ppm",false);r.put("interpretation","PRESERVED_NOT_CONVERTED_FOR_SCREENING_CANDIDATE");refs.put(r);}}
        out.put("reference_available",direct>0||mass>0);out.put("direct_ppm_reference_count",direct);out.put("mass_reference_count",mass);out.put("references",refs);putFiniteOrNull(out,"max_context_ratio",maxRatio);putFiniteOrNull(out,"max_context_percent",Double.isFinite(maxRatio)?maxRatio*100.0:Double.NaN);if(controlling!=null)out.put("controlling_reference",controlling);out.put("state",toxicityState(direct,maxRatio,m4));out.put("semantics",m4?"M4 context; temporal TWA/STEL conclusions remain governed by GasPpmExposureAssessment":"screening estimate versus directly comparable ppm references; no exposure exceedance/compliance conclusion");return out;
    }

    private static JSONObject flammabilityContext(List<ExposureRepository.Limit> limits,double ppm,boolean m4)throws Exception{
        JSONObject out=new JSONObject();JSONArray refs=new JSONArray();double bestLfl=Double.NaN,bestUfl=Double.NaN;JSONObject controlling=null;int lflCount=0,uflCount=0;
        if(limits==null)limits=new ArrayList<>();for(ExposureRepository.Limit l:limits){if(l==null||!l.numeric()||l.value==null||l.value<=0||!isVolPct(l.unit))continue;String t=upper(l.type);if(!("LFL".equals(t)||"LEL".equals(t)||"UFL".equals(t)||"UEL".equals(t)))continue;JSONObject r=reference(l);double boundaryPpm=l.value*10_000.0,ratio=Double.isFinite(ppm)?ppm/boundaryPpm:Double.NaN;r.put("boundary_ppm",boundaryPpm);putFiniteOrNull(r,"ratio_to_boundary",ratio);putFiniteOrNull(r,"percent_of_boundary",Double.isFinite(ratio)?ratio*100.0:Double.NaN);r.put("interpretation",m4?"M4_FLAMMABILITY_CONTEXT":"SCREENING_CONTEXT_ONLY_NO_FLAMMABILITY_ALARM");refs.put(r);if("LFL".equals(t)||"LEL".equals(t)){lflCount++;if(!Double.isFinite(bestLfl)||l.value<bestLfl){bestLfl=l.value;controlling=new JSONObject(r.toString());}}else{uflCount++;if(!Double.isFinite(bestUfl)||l.value<bestUfl)bestUfl=l.value;}}
        out.put("reference_available",lflCount>0||uflCount>0);out.put("lfl_reference_count",lflCount);out.put("ufl_reference_count",uflCount);putFiniteOrNull(out,"lfl_vol_pct",bestLfl);putFiniteOrNull(out,"ufl_vol_pct",bestUfl);double pctLfl=Double.isFinite(bestLfl)&&Double.isFinite(ppm)?ppm/(bestLfl*10_000.0)*100.0:Double.NaN;putFiniteOrNull(out,"percent_lfl_context",pctLfl);if(controlling!=null)out.put("controlling_lfl_reference",controlling);out.put("state",flammabilityState(lflCount,pctLfl,m4));out.put("semantics",m4?"M4 measured ppm flammability context; persisted gas assessment controls alarm interpretation":"screening-fit ppm versus LFL for prioritization only; no LFL alarm or compliance conclusion");out.put("references",refs);return out;
    }

    private static JSONObject reference(ExposureRepository.Limit l)throws Exception{JSONObject r=new JSONObject();r.put("authority",nz(l.authority));r.put("jurisdiction",nz(l.jurisdiction));r.put("limit_type",nz(l.type));r.put("value",l.value);r.put("unit",nz(l.unit));if(l.durationMin!=null&&Double.isFinite(l.durationMin))r.put("duration_min",l.durationMin);else r.put("duration_min",JSONObject.NULL);r.put("notation",nz(l.notation));r.put("source_id",nz(l.sourceId));r.put("revision",nz(l.revision));return r;}
    private static JSONArray hazardTypes(JSONObject toxic,JSONObject flame)throws Exception{JSONArray a=new JSONArray();if(toxic.optBoolean("reference_available",false))a.put("TOXICITY_EXPOSURE_REFERENCE");if(flame.optBoolean("reference_available",false))a.put("FLAMMABILITY_REFERENCE");return a;}
    private static String symbols(JSONObject toxic,JSONObject flame){StringBuilder s=new StringBuilder();if(toxic.optBoolean("reference_available",false))s.append("☠");if(flame.optBoolean("reference_available",false))s.append("🔥");return s.toString();}
    private static String priorityClass(JSONObject toxic,JSONObject flame,boolean m4){double t=toxic.optDouble("max_context_ratio",Double.NaN),l=flame.optDouble("percent_lfl_context",Double.NaN);double score=0;if(Double.isFinite(t))score=Math.max(score,t*100.0);if(Double.isFinite(l))score=Math.max(score,l);String p=score>=100?"CRITICAL_CONTEXT":score>=50?"HIGH_CONTEXT":score>=20?"ELEVATED_CONTEXT":score>=10?"CAUTION_CONTEXT":score>0?"LOW_CONTEXT":"REFERENCE_ONLY";return m4?"M4_"+p:"SCREENING_"+p;}
    private static String toxicityState(int direct,double ratio,boolean m4){if(direct<=0)return"NO_DIRECT_PPM_HEALTH_REFERENCE";if(!Double.isFinite(ratio))return"REFERENCE_AVAILABLE_CONCENTRATION_UNAVAILABLE";String p=ratio>=1?"AT_OR_ABOVE_REFERENCE_CONTEXT":ratio>=.5?"APPROACHING_REFERENCE_CONTEXT":ratio>=.2?"ELEVATED_REFERENCE_CONTEXT":"BELOW_REFERENCE_CONTEXT";return m4?"M4_"+p:"SCREENING_"+p;}
    private static String flammabilityState(int count,double pctLfl,boolean m4){if(count<=0)return"NO_LFL_REFERENCE";if(!Double.isFinite(pctLfl))return"LFL_REFERENCE_AVAILABLE_CONCENTRATION_UNAVAILABLE";String p=pctLfl>=100?"AT_OR_ABOVE_LFL_CONTEXT":pctLfl>=20?"HIGH_PERCENT_LFL_CONTEXT":pctLfl>=10?"ELEVATED_PERCENT_LFL_CONTEXT":pctLfl>=5?"CAUTION_PERCENT_LFL_CONTEXT":"LOW_PERCENT_LFL_CONTEXT";return m4?"M4_"+p:"SCREENING_"+p;}
    private static boolean isFlammability(String t){String x=upper(t);return"LFL".equals(x)||"LEL".equals(x)||"UFL".equals(x)||"UEL".equals(x);}
    private static boolean isPpm(String u){String x=upper(u).replace(" ","");return"PPM".equals(x)||"PPMV".equals(x);}
    private static boolean isMass(String u){String x=upper(u).replace("³","3").replace(" ","");return x.contains("MG/M3")||x.contains("MG/M^3")||x.contains("MGM-3");}
    private static boolean isVolPct(String u){String x=upper(u).replace(" ","");return x.contains("%VOL")||x.contains("VOL%")||"%".equals(x);}
    private static String upper(String s){return s==null?"":s.trim().toUpperCase(Locale.US);}
    private static String nz(String s){return s==null?"":s;}
    private static void putFiniteOrNull(JSONObject o,String k,double v)throws Exception{if(Double.isFinite(v))o.put(k,v);else o.put(k,JSONObject.NULL);}
}
