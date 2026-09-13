package com.servoc.app.chemcore;

import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.exposure.GasHazardPriority;
import com.servoc.app.exposure.GasPpmExposureAssessment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

/**
 * Pure-Java presentation layer shared by operator UI and tests.
 *
 * It deliberately presents the joint log-space fit as a screening hypothesis unless the persisted
 * result explicitly passes the complete M4 quantitative gate. No UI caller is allowed to infer a
 * reportable ppm merely because a candidate contains screening_fit_ppm.
 */
public final class ChemCoreAnalysisPresenter {
    public static final String SCHEMA="chemcore-analysis-presentation/1";
    private ChemCoreAnalysisPresenter(){}

    public static String summary(JSONObject p){
        if(p==null)return "لا توجد نتيجة ChemCore مرتبطة بجلسة مكتملة.";
        boolean ppm=p.optBoolean("ppm_reportable",false);double value=p.optDouble("ppm",Double.NaN);
        StringBuilder s=new StringBuilder();
        s.append("Session: ").append(p.optString("session_id","—"));
        s.append("\nOutcome: ").append(p.optString("outcome","—")).append(" • Tier: ").append(p.optString("evidence_tier","—"));
        s.append("\nTop candidate: ").append(p.optString("chemical","—"));
        s.append("\nPPM: ");
        if(ppm&&Double.isFinite(value))s.append(fmt(value)).append(" ppm • REPORTABLE");
        else s.append("WITHHELD • ").append(p.optString("ppm_withheld_reason","scientific_gate_not_passed"));
        s.append("\nPosterior: ").append(pct01(p.optDouble("top_posterior",Double.NaN)));
        s.append(" • runner-up: ").append(pct01(p.optDouble("runner_up_posterior",Double.NaN)));
        s.append(" • margin: ").append(pct01(p.optDouble("posterior_separation_margin",Double.NaN)));
        s.append("\nOOD: ").append(num(p.optDouble("ood_score",Double.NaN))).append(" • entropy: ").append(num(p.optDouble("normalized_entropy",Double.NaN)));
        JSONObject lib=p.optJSONObject("library");
        s.append("\nModel: ").append(p.optString("model_version","—"));
        s.append("\nFeature schema: ").append(p.optString("feature_schema","—"));
        s.append(" • Library: ").append(lib==null?"—":lib.optString("version","—"));
        s.append("\nR0 standard: ").append(FieldR0Baseline.SESSION).append(" • ").append(FieldR0Baseline.SAMPLE_COUNT).append(" clean-air samples");
        JSONObject q=p.optJSONObject("lab_quality");if(q!=null){
            JSONObject e=q.optJSONObject("environment");
            s.append("\nLab: MQ ").append(q.optInt("valid_mq_channels",0)).append("/8 • R0 ").append(q.optInt("r0_normalized_channels",0)).append("/8 • M4-ref ").append(q.optInt("reference_validated_channels",0)).append("/8");
            s.append(" • ENV ").append(e!=null&&e.optBoolean("ready",false)?"READY":"WITHHELD");
        }
        JSONObject gas=p.optJSONObject(GasPpmExposureAssessment.KEY);if(gas!=null){
            s.append("\nGas safety: ").append(gas.optString("hazard_symbols","")).append(gas.optString("toxicity_state",gas.optString("state","—"))).append(" • ").append(gas.optString("flammability_state","—"));
            if(gas.optBoolean("input_gate_passed",false)){
                double h=gas.optDouble("hazard_priority_percent",Double.NaN);if(Double.isFinite(h))s.append(" • priority ").append(String.format(Locale.US,"%.1f%%",h)).append(" of controlling reference");
                s.append(" • basis ").append(gas.optString("hazard_priority_basis","—"));
                double toxic=gas.optDouble("toxicity_priority_percent",Double.NaN);if(Double.isFinite(toxic))s.append(" • toxic ref ").append(String.format(Locale.US,"%.1f%%",toxic));
                double lfl=gas.optDouble("percent_lfl",Double.NaN);if(Double.isFinite(lfl))s.append(" • LFL ").append(String.format(Locale.US,"%.2f%%",lfl));
            }else s.append(" • ").append(gas.optString("reason","withheld"));
        }
        String note=p.optString("note","");if(!note.isEmpty())s.append("\nDecision note: ").append(note);
        return s.toString();
    }

    public static String candidates(JSONObject p,int limit){
        JSONArray rows=p==null?null:p.optJSONArray("candidates");if(rows==null||rows.length()==0)return "المرشحات: لا توجد نتائج قابلة للحل.";
        StringBuilder s=new StringBuilder("المرشحات المرتبة — ChemCore v3\n");int n=Math.min(Math.max(1,limit),rows.length());
        for(int i=0;i<n;i++){
            JSONObject r=rows.optJSONObject(i);if(r==null)continue;
            s.append('\n').append(r.optInt("rank",i+1)).append(") ").append(r.optString("label","—"));
            s.append("\nPosterior ").append(pct01(r.optDouble("posterior",Double.NaN)));
            s.append(" • Coverage ").append(pct100(r.optDouble("coverage_pct",Double.NaN)));
            s.append(" • Agreement ").append(pct100(r.optDouble("agreement_pct",Double.NaN)));
            s.append(" • Evidence ").append(pct100(r.optDouble("evidence_quality_pct",Double.NaN)));
            s.append("\nResidual RMS ").append(num(r.optDouble("residual_rms",Double.NaN)));
            s.append(" • contributors ").append(r.optInt("contributors",0));
            s.append(" • M4 curves ").append(r.optInt("calibrated_contributors",0));
            s.append(" • domain ").append(r.optBoolean("all_contributors_inside_domain",false)?"IN":"NOT PROVEN / OUT");
            double fit=r.optDouble("screening_fit_ppm",Double.NaN);if(Double.isFinite(fit))s.append("\nJoint log-fit hypothesis: ").append(fmt(fit)).append(" ppm • screening-only unless final M4 gate passes");
        }
        return s.toString().trim();
    }

    public static String equationsAndResiduals(JSONObject p,int candidateLimit){
        StringBuilder s=new StringBuilder();
        s.append("الإطار الرياضي\n");
        s.append("log10(Rs/R0) = m × log10(C) + c\n");
        s.append("C = 10^((log10(Rs/R0) − c) / m)\n");
        s.append("Joint fit: اختر C واحدة تقلل مجموع البواقي المعيارية الموزونة عبر جميع منحنيات MQ المتاحة.\n");
        s.append("Posterior: تطبيع أوزان exp(logScore − max(logScore)) بين الفرضيات القابلة للحل.\n");
        s.append("Residual agreement: مؤشر اتساق من البواقي؛ ليس احتمال هوية معايرًا، ولا يُعرّف لمساهم واحد.\n");
        s.append("M4: كل المنحنيات الكمية المساهمة يجب أن تكون device/reference-gas validated، والهوية Open-Set مقبولة، والحل داخل جميع المجالات الموثقة.\n");
        JSONArray rows=p==null?null:p.optJSONArray("candidates");if(rows==null||rows.length()==0)return s.toString().trim();
        int n=Math.min(Math.max(1,candidateLimit),rows.length());
        for(int i=0;i<n;i++){
            JSONObject r=rows.optJSONObject(i);if(r==null)continue;JSONObject residuals=r.optJSONObject("residual_log10_by_sensor");
            s.append("\n#").append(r.optInt("rank",i+1)).append(' ').append(r.optString("label","—")).append(" residual log10: ");
            if(residuals==null||residuals.length()==0){s.append("—");continue;}
            Iterator<String> it=residuals.keys();boolean first=true;while(it.hasNext()){String key=it.next();double v=residuals.optDouble(key,Double.NaN);if(!Double.isFinite(v))continue;if(!first)s.append(" • ");first=false;s.append(key.toUpperCase(Locale.US)).append(' ').append(String.format(Locale.US,"%+.4f",v));}if(first)s.append("—");
        }
        return s.toString().trim();
    }

    public static String gasExposurePpm(JSONObject p,int limit){
        JSONObject g=p==null?null:p.optJSONObject(GasPpmExposureAssessment.KEY);if(g==null)return "تقييم سلامة الغاز PPM/LFL: غير مسجل لهذه الجلسة.";
        StringBuilder s=new StringBuilder("تقييم سلامة الغاز — PPM + LFL\n");s.append("State: ").append(g.optString("toxicity_state",g.optString("state","—"))).append(" • ").append(g.optString("flammability_state","—"));
        if(!g.optBoolean("input_gate_passed",false)){s.append("\nWITHHELD: ").append(g.optString("reason","scientific_gate_not_passed"));s.append("\nلا تتم مقارنة حدود التعرض أو LFL قبل اجتياز هوية M4 وPPM القابل للتقرير.");return s.toString();}
        s.append("\n").append(g.optString("hazard_symbols","")).append(" Measured: ").append(fmt(g.optDouble("ppm",Double.NaN))).append(" ppm");
        s.append(" • duration ").append(String.format(Locale.US,"%.1f s",g.optLong("measurement_duration_ms",0)/1000.0));
        double hazard=g.optDouble("hazard_priority_percent",Double.NaN);if(Double.isFinite(hazard))s.append("\nHazard priority: ").append(String.format(Locale.US,"%.1f%%",hazard)).append(" • ").append(g.optString("hazard_priority_basis","—"));else s.append("\nHazard priority: — (no usable reference)");
        double toxic=g.optDouble("toxicity_priority_percent",Double.NaN);s.append("\nToxicity reference ratio: ").append(Double.isFinite(toxic)?String.format(Locale.US,"%.1f%%",toxic):"—");
        double lflPct=g.optDouble("percent_lfl",Double.NaN);s.append(" • %LFL: ").append(Double.isFinite(lflPct)?String.format(Locale.US,"%.2f%%",lflPct):"—");
        if(g.has("lfl_vol_pct")&&!g.isNull("lfl_vol_pct"))s.append(" • LFL ").append(fmt(g.optDouble("lfl_vol_pct",Double.NaN))).append(" vol% = ").append(fmt(g.optDouble("lfl_ppm",Double.NaN))).append(" ppm");
        s.append("\nDisplay sort: ").append(GasHazardPriority.labelAr(GasHazardPriority.Mode.HAZARD)).append(" (default) / التركيز (operator option). Identity ranking is unaffected.");
        s.append("\nPPM references: ").append(g.optInt("ppm_reference_count",0)).append(" • non-PPM toxicology excluded: ").append(g.optInt("excluded_non_ppm_count",0));
        s.append(" • crossed: ").append(g.optInt("crossed_reference_count",0)).append(" • action-relevant: ").append(g.optInt("action_relevant_crossed_count",0));
        JSONArray refs=g.optJSONArray("references");if(refs!=null){int n=Math.min(Math.max(1,limit),refs.length());for(int i=0;i<n;i++){JSONObject r=refs.optJSONObject(i);if(r==null)continue;s.append("\n").append(r.optString("authority","—")).append(' ').append(r.optString("limit_type","—")).append(": ").append(fmt(r.optDouble("value_ppm",Double.NaN))).append(" ppm");s.append(" • ").append(String.format(Locale.US,"%.1f%%",r.optDouble("percent_of_reference",Double.NaN))).append(" of reference");s.append(r.optBoolean("reference_crossed",false)?" • REFERENCE CROSSED":" • below reference");s.append(" • ").append(r.optString("temporal_role","OTHER"));if(r.has("duration_min")&&!r.isNull("duration_min"))s.append("/").append(fmt(r.optDouble("duration_min",Double.NaN))).append(" min");if(!r.optBoolean("duration_coverage_satisfied",false)&&("TWA".equals(r.optString("temporal_role"))||"STEL".equals(r.optString("temporal_role"))))s.append(" • duration not covered");}}
        if(g.optBoolean("toxic_reference_crossed",false)&&g.optBoolean("health_reference_context_warning",false))s.append("\n☠ يعني أن التركيز بلغ/تجاوز مرجعًا صحيًا؛ مدة TWA/STEL غير مكتملة، لذا لا يمثل حكم امتثال.");
        if(g.optBoolean("flammability_lfl_reached",false))s.append("\n🔥 التركيز بلغ أو تجاوز 100% LFL المرجعي للمادة.");
        s.append("\nCompliance: NOT MADE. TWA/STEL require their averaging duration and applicable sampling method.");return s.toString();
    }

    public static double[] candidatePosteriorPct(JSONObject p,int limit){
        JSONArray rows=p==null?null:p.optJSONArray("candidates");if(rows==null)return new double[0];int n=Math.min(Math.max(0,limit),rows.length());double[] out=new double[n];for(int i=0;i<n;i++){JSONObject r=rows.optJSONObject(i);double v=r==null?Double.NaN:r.optDouble("posterior",Double.NaN);out[i]=Double.isFinite(v)?100.0*v:Double.NaN;}return out;
    }

    private static String fmt(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,v>=1000?"%.0f":v>=10?"%.2f":"%.4f",v);}
    private static String num(double v){return Double.isFinite(v)?String.format(Locale.US,"%.4f",v):"—";}
    private static String pct01(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",100.0*v):"—";}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
}
