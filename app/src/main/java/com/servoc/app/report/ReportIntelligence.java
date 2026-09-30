package com.servoc.app.report;

import com.servoc.app.data.SessionSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HazRob 0.10.2 / HR069 evidence-driven report planner.
 *
 * It decides what a report may say and which visual families are worth showing from the
 * evidence already stored in the session. It never invents missing measurements and never
 * upgrades screening evidence to identity or M4 concentration.
 */
public final class ReportIntelligence {
    private ReportIntelligence(){}

    public static final int CHART_COUNT=20;
    public enum State{EMPTY,SIGNAL_ONLY,ANALYTICAL_ONLY,SCREENING,IDENTIFIED,QUANTIFIED}
    public enum ReportMode{NO_EVIDENCE,SIGNAL_DIAGNOSTIC,SCREENING,IDENTIFIED,QUANTIFIED,OPEN_SET_OR_MIXTURE,INVALID_MEASUREMENT}
    public enum Severity{INFO,NOTICE,WARNING,CRITICAL}

    public static final class ChartAvailability{
        public final int index;public final boolean available;public final String source,reason;
        ChartAvailability(int index,boolean available,String source,String reason){this.index=index;this.available=available;this.source=source;this.reason=reason;}
    }

    public static final class Snapshot{
        public final State state;public final boolean timeSeriesAvailable,chemCoreAvailable,ppmReportable,safetyInterpretable;
        public final int candidateCount,screenedCount,screeningTarget,availableChartCount;
        public final List<Integer> recommendedCharts;
        Snapshot(State state,boolean series,boolean chem,boolean ppm,boolean safety,int candidates,int screened,int target,int available,List<Integer> recommended){
            this.state=state;timeSeriesAvailable=series;chemCoreAvailable=chem;ppmReportable=ppm;safetyInterpretable=safety;candidateCount=candidates;screenedCount=screened;screeningTarget=target;availableChartCount=available;recommendedCharts=Collections.unmodifiableList(new ArrayList<>(recommended));
        }
    }

    public static final class Finding{
        public final Severity severity;public final String code,titleAr,detailAr;
        Finding(Severity severity,String code,String titleAr,String detailAr){this.severity=severity;this.code=code;this.titleAr=titleAr;this.detailAr=detailAr;}
    }

    public static final class ReportPlan{
        public final ReportMode mode;public final String modeAr,executiveTitleAr;
        public final int pageCount,childMeasurementSessionCount;
        public final boolean includeSensorEvidence,includeChemistry,include144,includeQuantitativeSafety,includeUncertainty,includeTraceability;
        public final boolean ppmReportable,safetyInterpretable;
        public final int candidateCount,panelEvaluated,panelTarget,panelCandidateSupported,panelAcceptedIdentities,panelPpmReportable,arrayUnsupported;
        public final double topPosterior,separationMargin,oodScore,entropy,analyticalConfidencePct;
        public final List<Integer> sensorCharts,chemistryCharts,contextCharts;
        public final List<Finding> findings;
        public final List<JSONObject> top144SessionEvidence;
        public final String rationale;
        ReportPlan(ReportMode mode,String modeAr,String executiveTitleAr,int pageCount,int childCount,
                   boolean sensor,boolean chemistry,boolean panel,boolean quantitativeSafety,boolean uncertainty,boolean trace,
                   boolean ppm,boolean safety,int candidates,int evaluated,int target,int panelSupported,int accepted,int panelPpm,int unsupported,
                   double posterior,double margin,double ood,double entropy,double confidence,
                   List<Integer> sensorCharts,List<Integer> chemistryCharts,List<Integer> contextCharts,List<Finding> findings,List<JSONObject> top144,String rationale){
            this.mode=mode;this.modeAr=modeAr;this.executiveTitleAr=executiveTitleAr;this.pageCount=pageCount;this.childMeasurementSessionCount=childCount;
            includeSensorEvidence=sensor;includeChemistry=chemistry;include144=panel;includeQuantitativeSafety=quantitativeSafety;includeUncertainty=uncertainty;includeTraceability=trace;
            ppmReportable=ppm;safetyInterpretable=safety;candidateCount=candidates;panelEvaluated=evaluated;panelTarget=target;panelCandidateSupported=panelSupported;panelAcceptedIdentities=accepted;panelPpmReportable=panelPpm;arrayUnsupported=unsupported;
            topPosterior=posterior;separationMargin=margin;oodScore=ood;this.entropy=entropy;analyticalConfidencePct=confidence;
            this.sensorCharts=immutable(sensorCharts);this.chemistryCharts=immutable(chemistryCharts);this.contextCharts=immutable(contextCharts);this.findings=Collections.unmodifiableList(new ArrayList<>(findings));this.top144SessionEvidence=Collections.unmodifiableList(copyJson(top144));this.rationale=rationale;
        }
    }

    public static Snapshot evaluate(SessionSeries.Series series,JSONObject chemcore){
        boolean seriesOk=series!=null&&series.usable(),chemOk=chemcore!=null;
        JSONArray candidates=chemOk?chemcore.optJSONArray("candidates"):null;int candidateCount=candidates==null?0:candidates.length();
        JSONObject screening=chemOk?chemcore.optJSONObject("screening_144"):null;int screened=screening==null?0:screening.optInt("evaluated_count",0),target=screening==null?0:screening.optInt("target_count",144);
        boolean ppm=chemOk&&chemcore.optBoolean("ppm_reportable",false)&&Double.isFinite(chemcore.optDouble("ppm",Double.NaN));
        JSONObject gas=chemOk?chemcore.optJSONObject("gas_exposure_ppm"):null;boolean safety=gas!=null&&gas.optBoolean("input_gate_passed",false);
        String outcome=chemOk?chemcore.optString("outcome",""):"";State state;
        if(!seriesOk&&!chemOk)state=State.EMPTY;
        else if(!chemOk)state=State.SIGNAL_ONLY;
        else if(ppm||"QUANTIFIED".equals(outcome))state=State.QUANTIFIED;
        else if("IDENTIFIED".equals(outcome))state=State.IDENTIFIED;
        else if(candidateCount>0||screened>0)state=State.SCREENING;
        else state=State.ANALYTICAL_ONLY;
        int available=0;for(int i=0;i<CHART_COUNT;i++)if(chart(i,series,chemcore).available)available++;
        ReportPlan plan=plan(series,chemcore,1);List<Integer> rec=new ArrayList<>();rec.addAll(plan.chemistryCharts);rec.addAll(plan.sensorCharts);rec.addAll(plan.contextCharts);dedupe(rec);if(rec.size()>8)rec=new ArrayList<>(rec.subList(0,8));
        return new Snapshot(state,seriesOk,chemOk,ppm,safety,candidateCount,screened,target,available,rec);
    }

    /** Plans a 4–5 page field-session report. */
    public static ReportPlan plan(SessionSeries.Series series,JSONObject chemcore,int childMeasurementSessionCount){
        int child=Math.max(0,childMeasurementSessionCount);boolean seriesOk=series!=null&&series.usable(),chemOk=chemcore!=null;
        String outcome=chemOk?chemcore.optString("outcome",""):"";boolean ppm=chemOk&&chemcore.optBoolean("ppm_reportable",false)&&Double.isFinite(chemcore.optDouble("ppm",Double.NaN));
        JSONObject gas=chemOk?chemcore.optJSONObject("gas_exposure_ppm"):null;boolean safety=gas!=null&&gas.optBoolean("input_gate_passed",false);
        JSONArray candidates=chemOk?chemcore.optJSONArray("candidates"):null;int candidateCount=candidates==null?0:candidates.length();
        JSONObject panel=chemOk?chemcore.optJSONObject("screening_144"):null;int evaluated=panel==null?0:panel.optInt("evaluated_count",0),target=panel==null?0:panel.optInt("target_count",144),panelSupported=panel==null?0:panel.optInt("session_candidate_supported_count",countSessionSupported(panel)),accepted=panel==null?0:panel.optInt("accepted_identity_count",countBool(panel,"detected")),panelPpm=panel==null?0:panel.optInt("ppm_reportable_count",countBool(panel,"ppm_reportable")),unsupported=panel==null?0:panel.optInt("array_unsupported_count",countState(panel,"NOT_EVALUABLE_BY_CURRENT_ARRAY"));
        double posterior=chemOk?chemcore.optDouble("top_posterior",Double.NaN):Double.NaN,margin=chemOk?chemcore.optDouble("posterior_separation_margin",Double.NaN):Double.NaN,ood=chemOk?chemcore.optDouble("ood_score",Double.NaN):Double.NaN,entropy=chemOk?chemcore.optDouble("normalized_entropy",Double.NaN):Double.NaN,confidence=chemOk?chemcore.optDouble("analytical_confidence_pct",Double.NaN):Double.NaN;

        ReportMode mode=mode(seriesOk,chemOk,outcome,ppm);String modeAr=modeAr(mode);String title=titleAr(mode,chemcore);
        boolean chemistry=chemOk,include144=panel!=null&&evaluated>0,quant=ppm||safety,uncertainty=chemOk&&!ppm;
        List<Integer> sensorCharts=new ArrayList<>(),chemCharts=new ArrayList<>(),contextCharts=new ArrayList<>();
        if(seriesOk){add(sensorCharts,0);add(sensorCharts,1);add(sensorCharts,12);if(chemistry)add(sensorCharts,14);}
        if(chemistry){if(candidateCount>0){add(chemCharts,3);add(chemCharts,6);add(chemCharts,5);}if(include144){add(chemCharts,7);add(chemCharts,9);}if(safety)add(contextCharts,10);}
        if(seriesOk){add(contextCharts,11);add(contextCharts,16);add(contextCharts,17);add(contextCharts,18);}

        List<Finding> findings=new ArrayList<>();
        if(!seriesOk)addFinding(findings,Severity.WARNING,"NO_TIME_SERIES","السلسلة الزمنية غير متاحة","لن يعرض التقرير منحنيات زمنية أو حركية غير مسجلة.");
        if(!chemOk)addFinding(findings,Severity.WARNING,"NO_CHEMCORE","لا توجد نتيجة ChemCore","سيقتصر التقرير على الإشارة وجودة القياس دون استنتاج هوية كيميائية.");
        if(chemOk){
            if("UNKNOWN".equals(outcome))addFinding(findings,Severity.WARNING,"OPEN_SET_UNKNOWN","العينة خارج هوية موثوقة","بوابة Open-set لم تقبل هوية اسمية؛ تُعرض المرشحات كدليل استكشافي فقط.");
            else if("POSSIBLE_MIXTURE_OR_INTERFERENCE".equals(outcome))addFinding(findings,Severity.WARNING,"MIXTURE_INTERFERENCE","اشتباه خليط أو تداخل","نمط البواقي/الاتساق لا يدعم تفسير مادة مفردة موثوقًا.");
            else if("OUT_OF_VALIDATED_DOMAIN".equals(outcome))addFinding(findings,Severity.WARNING,"OUT_OF_DOMAIN","خارج المجال المعتمد","توجد استجابة مدعومة لكن التركيز الملائم خارج مجال واحد أو أكثر من النماذج المعتمدة.");
            else if("INSUFFICIENT_EVIDENCE".equals(outcome))addFinding(findings,Severity.WARNING,"INSUFFICIENT_EVIDENCE","الدليل غير كافٍ","لا توجد أدلة مستقلة كافية لترقية النتيجة إلى هوية اسمية.");
            else if("INVALID_MEASUREMENT".equals(outcome))addFinding(findings,Severity.CRITICAL,"INVALID_MEASUREMENT","القياس غير صالح تحليليًا","يجب التعامل مع التقرير كتقرير جودة/تشخيص لا كتقرير تعرف كيميائي.");
            if(!ppm)addFinding(findings,Severity.NOTICE,"PPM_WITHHELD","PPM غير قابل للتقرير","لا تُعرض فرضية fit concentration كقياس تعرض مهني؛ يلزم مسار M4 عند الحاجة للقياس الكمي.");
            if(include144&&panelSupported==0)addFinding(findings,Severity.NOTICE,"PANEL_NO_SESSION_SUPPORT","مصفوفة 144 بلا دعم جلسة إيجابي","المصفوفة ما زالت توضح قدرة المكتبة/المصفوفة، لكنها لا تعني نفي وجود المواد.");
            JSONObject q=first(chemcore.optJSONObject("lab_quality"),chemcore.optJSONObject("measurement_quality"));if(q!=null&&!q.optBoolean("capture_ready",true))addFinding(findings,Severity.WARNING,"QUALITY_GATE","بوابة جودة القياس غير مكتملة",q.optString("reason",q.optString("note","راجع حالة الحساسات وR0 والظروف البيئية.")));
            if(safety){double hp=gas.optDouble("hazard_priority_percent",Double.NaN),lfl=gas.optDouble("percent_lfl",Double.NaN);if((Double.isFinite(hp)&&hp>=100)||(Double.isFinite(lfl)&&lfl>=100))addFinding(findings,Severity.CRITICAL,"SAFETY_REFERENCE_CROSSED","مرجع سلامة متجاوز","تم تجاوز مرجع صحي و/أو LFL وفق القياس الكمي المسموح؛ راجع صفحة السلامة فورًا.");}
        }
        Collections.sort(findings,new Comparator<Finding>(){public int compare(Finding a,Finding b){return Integer.compare(b.severity.ordinal(),a.severity.ordinal());}});
        List<JSONObject> top144=top144(panel,12);
        boolean trace=chemOk&&(child>1||include144||hasProvenance(chemcore)||!findings.isEmpty());int pages=trace?5:4;
        String rationale="mode="+mode.name()+"; series="+seriesOk+"; chemcore="+chemOk+"; candidates="+candidateCount+"; panel="+evaluated+"/"+target+"; session-supported="+panelSupported+"; ppm-reportable="+ppm+"; safety="+safety+"; child-measurements="+child;
        return new ReportPlan(mode,modeAr,title,pages,child,seriesOk,chemistry,include144,quant,uncertainty,trace,ppm,safety,candidateCount,evaluated,target,panelSupported,accepted,panelPpm,unsupported,posterior,margin,ood,entropy,confidence,sensorCharts,chemCharts,contextCharts,findings,top144,rationale);
    }

    public static ChartAvailability chart(int index,SessionSeries.Series series,JSONObject chemcore){
        boolean seriesOk=series!=null&&series.usable(),chemOk=chemcore!=null;
        if(index<0||index>=CHART_COUNT)return new ChartAvailability(index,false,"NONE","unknown_chart");
        boolean needsChem=index>=3&&index<=10,needsSeries=index<=2||index>=11;
        if(needsChem&&!chemOk)return new ChartAvailability(index,false,"CHEMCORE","chemcore_result_missing");
        if(needsSeries&&!seriesOk)return new ChartAvailability(index,false,"RECORDED_SERIES","recorded_time_series_missing");
        if(index==7||index==8){JSONObject s=chemcore.optJSONObject("screening_144");if(s==null||s.optInt("evaluated_count",0)<=0)return new ChartAvailability(index,false,"CHEMCORE","screening_144_missing");}
        if(index==10){JSONObject g=chemcore.optJSONObject("gas_exposure_ppm");if(g==null||!g.optBoolean("input_gate_passed",false))return new ChartAvailability(index,false,"CHEMCORE_M4_SAFETY","gas_safety_gate_not_passed");}
        return new ChartAvailability(index,true,needsChem?"CHEMCORE":"RECORDED_SERIES","available");
    }

    private static ReportMode mode(boolean seriesOk,boolean chemOk,String outcome,boolean ppm){
        if(!seriesOk&&!chemOk)return ReportMode.NO_EVIDENCE;if(!chemOk)return ReportMode.SIGNAL_DIAGNOSTIC;if(ppm||"QUANTIFIED".equals(outcome))return ReportMode.QUANTIFIED;if("IDENTIFIED".equals(outcome))return ReportMode.IDENTIFIED;if("UNKNOWN".equals(outcome)||"POSSIBLE_MIXTURE_OR_INTERFERENCE".equals(outcome)||"OUT_OF_VALIDATED_DOMAIN".equals(outcome)||"INSUFFICIENT_EVIDENCE".equals(outcome))return ReportMode.OPEN_SET_OR_MIXTURE;if("INVALID_MEASUREMENT".equals(outcome))return ReportMode.INVALID_MEASUREMENT;return ReportMode.SCREENING;
    }
    private static String modeAr(ReportMode m){switch(m){case SIGNAL_DIAGNOSTIC:return"تشخيص الإشارة";case SCREENING:return"فحص كيميائي";case IDENTIFIED:return"تعرّف كيميائي";case QUANTIFIED:return"تعرف وقياس كمي M4";case OPEN_SET_OR_MIXTURE:return"عدم يقين / خليط / خارج المجال";case INVALID_MEASUREMENT:return"قياس غير صالح";default:return"لا توجد أدلة كافية";}}
    private static String titleAr(ReportMode m,JSONObject c){String chemical=c==null?"":c.optString("chemical","").trim();if(m==ReportMode.QUANTIFIED)return chemical.isEmpty()?"نتيجة كمية معتمدة M4":"نتيجة كمية M4 • "+chemical;if(m==ReportMode.IDENTIFIED)return chemical.isEmpty()?"هوية كيميائية مدعومة":"هوية كيميائية • "+chemical;if(m==ReportMode.SCREENING)return"نتيجة فحص وترتيب مرشحين";if(m==ReportMode.OPEN_SET_OR_MIXTURE)return"نتيجة تحليلية غير قابلة للترقية إلى هوية نهائية";if(m==ReportMode.INVALID_MEASUREMENT)return"تقرير جودة قياس غير صالح للتحليل";if(m==ReportMode.SIGNAL_DIAGNOSTIC)return"تقرير الإشارة والبصمة دون ChemCore";return"لا توجد نتيجة تحليلية قابلة للتقرير";}

    private static List<JSONObject> top144(JSONObject panel,int limit){List<JSONObject> out=new ArrayList<>();JSONArray rows=panel==null?null:panel.optJSONArray("rows");if(rows==null)return out;for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null&&r.optBoolean("session_candidate_evidence",false))try{out.add(new JSONObject(r.toString()));}catch(Exception ignored){}}Collections.sort(out,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){double x=a.optDouble("session_support_index_pct",Double.NaN),y=b.optDouble("session_support_index_pct",Double.NaN);if(Double.isFinite(x)||Double.isFinite(y)){if(!Double.isFinite(x))return 1;if(!Double.isFinite(y))return-1;int n=Double.compare(y,x);if(n!=0)return n;}return Integer.compare(a.optInt("candidate_rank",999),b.optInt("candidate_rank",999));}});return out.size()>limit?new ArrayList<>(out.subList(0,limit)):out;}
    private static int countSessionSupported(JSONObject panel){JSONArray r=panel==null?null:panel.optJSONArray("rows");int n=0;if(r!=null)for(int i=0;i<r.length();i++){JSONObject q=r.optJSONObject(i);if(q!=null&&q.optBoolean("session_candidate_evidence",false))n++;}return n;}
    private static int countBool(JSONObject panel,String key){JSONArray r=panel==null?null:panel.optJSONArray("rows");int n=0;if(r!=null)for(int i=0;i<r.length();i++){JSONObject q=r.optJSONObject(i);if(q!=null&&q.optBoolean(key,false))n++;}return n;}
    private static int countState(JSONObject panel,String state){JSONArray r=panel==null?null:panel.optJSONArray("rows");int n=0;if(r!=null)for(int i=0;i<r.length();i++){JSONObject q=r.optJSONObject(i);if(q!=null&&state.equals(q.optString("session_evidence_state","")))n++;}return n;}
    private static boolean hasProvenance(JSONObject c){if(c==null)return false;JSONObject l=c.optJSONObject("library");return l!=null&&(!l.optString("version","").isEmpty()||!l.optString("sha256","").isEmpty()||!l.optString("source_commit","").isEmpty());}
    private static JSONObject first(JSONObject a,JSONObject b){return a!=null?a:b;}
    private static void addFinding(List<Finding> a,Severity s,String code,String title,String detail){a.add(new Finding(s,code,title,detail==null?"":detail));}
    private static void add(List<Integer> a,int v){if(!a.contains(v))a.add(v);}
    private static void dedupe(List<Integer> a){List<Integer> x=new ArrayList<>();for(Integer i:a)if(i!=null&&!x.contains(i))x.add(i);a.clear();a.addAll(x);}
    private static List<Integer> immutable(List<Integer> a){return Collections.unmodifiableList(new ArrayList<>(a));}
    private static List<JSONObject> copyJson(List<JSONObject> a){List<JSONObject> out=new ArrayList<>();if(a!=null)for(JSONObject q:a)try{out.add(new JSONObject(q.toString()));}catch(Exception ignored){}return out;}
}
