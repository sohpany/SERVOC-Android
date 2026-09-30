package com.servoc.app.report;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cross-session evidence planner for one Site. It never pools incompatible concentrations. */
public final class SiteReportIntelligence {
    private SiteReportIntelligence(){}

    public static final class MaterialRecurrence{
        public final int index;public final String name;public final int supportedSessions,acceptedSessions,quantifiedSessions;public final double maxSupportPct,medianSupportPct;
        MaterialRecurrence(int index,String name,int supported,int accepted,int quantified,double max,double median){this.index=index;this.name=name;this.supportedSessions=supported;this.acceptedSessions=accepted;this.quantifiedSessions=quantified;this.maxSupportPct=max;this.medianSupportPct=median;}
    }

    public static final class SitePlan{
        public final int totalSessions,analyticalSessions,identifiedSessions,quantifiedSessions,criticalSessions,openSetOrMixtureSessions,invalidSessions;
        public final int sessionsWith144,sessionsWithPositive144Evidence;
        public final List<MaterialRecurrence> materials,topRecurring;
        public final List<String> findings;
        public final String rationale;
        SitePlan(int total,int analytical,int identified,int quantified,int critical,int openSet,int invalid,int with144,int positive,List<MaterialRecurrence> materials,List<MaterialRecurrence> top,List<String> findings,String rationale){
            totalSessions=total;analyticalSessions=analytical;identifiedSessions=identified;quantifiedSessions=quantified;criticalSessions=critical;openSetOrMixtureSessions=openSet;invalidSessions=invalid;sessionsWith144=with144;sessionsWithPositive144Evidence=positive;this.materials=Collections.unmodifiableList(materials);topRecurring=Collections.unmodifiableList(top);this.findings=Collections.unmodifiableList(findings);this.rationale=rationale;
        }
    }

    private static final class Acc{
        final int index;String name="";int supported,accepted,quantified;final List<Double> support=new ArrayList<>();
        Acc(int index){this.index=index;}
    }

    public static SitePlan plan(List<SiteSummaryReportExporter.SessionSummary> sessions){
        List<SiteSummaryReportExporter.SessionSummary> src=sessions==null?Collections.emptyList():sessions;Map<Integer,Acc> map=new HashMap<>();int analytical=0,identified=0,quantified=0,critical=0,openSet=0,invalid=0,with144=0,positive144=0;
        for(SiteSummaryReportExporter.SessionSummary s:src){
            if(s==null)continue;JSONObject q=s.bundle.primaryDecision;if(q!=null){analytical++;String outcome=q.optString("outcome","");if("IDENTIFIED".equals(outcome))identified++;if("QUANTIFIED".equals(outcome)||s.ppmReportable)quantified++;if("UNKNOWN".equals(outcome)||"POSSIBLE_MIXTURE_OR_INTERFERENCE".equals(outcome)||"OUT_OF_VALIDATED_DOMAIN".equals(outcome)||"INSUFFICIENT_EVIDENCE".equals(outcome))openSet++;if("INVALID_MEASUREMENT".equals(outcome))invalid++;if(s.critical())critical++;}
            JSONObject panel=q==null?null:q.optJSONObject("screening_144");JSONArray rows=panel==null?null:panel.optJSONArray("rows");if(rows==null||rows.length()==0)continue;with144++;boolean sessionPositive=false;
            for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;int idx=r.optInt("index",i);Acc a=map.get(idx);if(a==null){a=new Acc(idx);map.put(idx,a);}if(a.name.isEmpty())a.name=r.optString("name","#"+(idx+1));boolean supported=r.optBoolean("session_candidate_evidence",false),accepted=r.optBoolean("detected",false),ppm=r.optBoolean("ppm_reportable",false);if(supported){a.supported++;sessionPositive=true;double v=r.optDouble("session_support_index_pct",Double.NaN);if(Double.isFinite(v))a.support.add(v);}if(accepted)a.accepted++;if(ppm)a.quantified++;}
            if(sessionPositive)positive144++;
        }
        List<MaterialRecurrence> materials=new ArrayList<>();for(Acc a:map.values()){double max=Double.NaN,med=Double.NaN;if(!a.support.isEmpty()){Collections.sort(a.support);max=a.support.get(a.support.size()-1);int n=a.support.size();med=n%2==1?a.support.get(n/2):(a.support.get(n/2-1)+a.support.get(n/2))/2.0;}materials.add(new MaterialRecurrence(a.index,a.name,a.supported,a.accepted,a.quantified,max,med));}
        Collections.sort(materials,new Comparator<MaterialRecurrence>(){public int compare(MaterialRecurrence a,MaterialRecurrence b){return Integer.compare(a.index,b.index);}});
        List<MaterialRecurrence> top=new ArrayList<>();for(MaterialRecurrence m:materials)if(m.supportedSessions>0)top.add(m);Collections.sort(top,new Comparator<MaterialRecurrence>(){public int compare(MaterialRecurrence a,MaterialRecurrence b){int n=Integer.compare(b.supportedSessions,a.supportedSessions);if(n!=0)return n;n=Integer.compare(b.acceptedSessions,a.acceptedSessions);if(n!=0)return n;if(Double.isFinite(a.maxSupportPct)&&Double.isFinite(b.maxSupportPct)){n=Double.compare(b.maxSupportPct,a.maxSupportPct);if(n!=0)return n;}return Integer.compare(a.index,b.index);}});if(top.size()>15)top=new ArrayList<>(top.subList(0,15));
        List<String> findings=new ArrayList<>();if(src.isEmpty())findings.add("لا توجد جلسات تحت الموقع.");if(analytical<src.size())findings.add("بعض الجلسات لا تحتوي نتيجة ChemCore؛ لا تدخل في استنتاجات الهوية.");if(openSet>0)findings.add(openSet+" جلسة ذات Unknown/خليط/خارج المجال/دليل غير كافٍ؛ لا تعامل كهويات مؤكدة.");if(invalid>0)findings.add(invalid+" جلسة غير صالحة تحليليًا ويجب التعامل معها كجودة قياس فقط.");if(critical>0)findings.add(critical+" جلسة تجاوزت مرجع سلامة صحي و/أو LFL وفق بوابة كمية صالحة.");if(with144>0&&positive144==0)findings.add("توجد مصفوفات 144 محفوظة ولكن لا توجد مواد ذات دعم جلسة إيجابي؛ لا يعني ذلك إثبات الغياب.");findings.add("لا يُحسب متوسط PPM عبر جلسات مستقلة؛ القيم الكمية تبقى مرتبطة بكل جلسة وبوابة M4 الخاصة بها.");
        String rationale="sessions="+src.size()+"; analytical="+analytical+"; identified="+identified+"; quantified="+quantified+"; 144="+with144+"; positive144="+positive144+"; critical="+critical;
        return new SitePlan(src.size(),analytical,identified,quantified,critical,openSet,invalid,with144,positive144,materials,top,findings,rationale);
    }
}
