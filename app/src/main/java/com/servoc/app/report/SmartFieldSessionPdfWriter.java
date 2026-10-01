package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Evidence-driven PDF for one Field Session.
 *
 * The Field Session is the reporting root. Child measurement sessions are never concatenated into a
 * synthetic time-series. Every child is evaluated independently, then the strongest complete child
 * is used only for detailed plots while a separate cross-session page reports agreement/divergence.
 */
public final class SmartFieldSessionPdfWriter {
    private SmartFieldSessionPdfWriter(){}

    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=7,MAX_PAGES=10;
    private static final float MARGIN=28.35f;

    static final class Evidence {
        final String sessionId;
        final SessionSeries.Series series;
        final JSONObject decision;
        final int recordCount;
        final int score;
        Evidence(String sid,SessionSeries.Series series,JSONObject decision,int records){
            this.sessionId=sid;this.series=series;this.decision=decision;this.recordCount=records;this.score=score(series,decision,records);
        }
        boolean seriesOk(){return series!=null&&series.usable();}
        boolean chemOk(){return decision!=null;}
        int samples(){return series==null?0:series.samples.size();}
        long durationMs(){return series==null?0:series.durationMs();}
        String outcome(){return decision==null?"—":decision.optString("outcome","—");}
        String topCandidate(){return topCandidate(decision);}
        double posterior(){return topPosterior(decision);}
        int validMq(){return validMq(decision);}
    }

    private static final class Consensus {
        final String label;final int count,total;final double meanPosterior;
        Consensus(String label,int count,int total,double meanPosterior){this.label=label;this.count=count;this.total=total;this.meanPosterior=meanPosterior;}
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");
        FieldSessionReportExporter.Bundle b=FieldSessionReportExporter.resolve(db,fieldSessionId);
        List<Evidence> all=evidence(db,b.measurementSessionIds);Evidence primary=primary(all);
        SessionSeries.Series primarySeries=primary==null?null:primary.series;JSONObject primaryDecision=primary==null?null:primary.decision;
        ReportIntelligence.ReportPlan plan=ReportIntelligence.plan(primarySeries,primaryDecision,b.measurementSessionIds.size());
        JSONObject hazard=CandidateHazardContext.evaluate(db,primaryDecision);
        boolean anySeries=hasSeries(all),anyChem=hasChem(all),multi=all.size()>1;
        int total=7+(anySeries?1:0)+(anyChem?1:0)+(multi?1:0);total=Math.max(MIN_PAGES,Math.min(MAX_PAGES,total));
        Consensus consensus=consensus(all);

        PdfDocument pdf=new PdfDocument();int n=1;
        PdfDocument.Page p=page(pdf,n);executive(p.getCanvas(),b,all,primary,plan,hazard,consensus);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);
        p=page(pdf,n);measurementOverview(p.getCanvas(),b,all,primary,consensus);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);
        p=page(pdf,n);signalEvidence(p.getCanvas(),primary);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);
        p=page(pdf,n);chemicalEvidence(p.getCanvas(),primary,hazard);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);
        p=page(pdf,n);panelAndHazard(p.getCanvas(),primary,hazard);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);
        p=page(pdf,n);uncertaintySafetyContext(p.getCanvas(),primary,plan);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);

        if(anySeries){p=page(pdf,n);sensorDynamics(p.getCanvas(),primary,all);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);}
        if(anyChem){p=page(pdf,n);candidateDeepDive(p.getCanvas(),primary);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);}
        if(multi){p=page(pdf,n);crossSessionConsistency(p.getCanvas(),all,consensus);chrome(p.getCanvas(),n++,total,b,plan);pdf.finishPage(p);}

        p=page(pdf,n);methodologyProvenance(p.getCanvas(),b,all,primary,plan,chemicals,total);chrome(p.getCanvas(),n,total,b,plan);pdf.finishPage(p);
        pdf.writeTo(out);pdf.close();
    }

    static List<Evidence> evidence(ExperimentsDb db,List<String> ids){
        List<Evidence> out=new ArrayList<>();ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);
        if(ids==null)return out;
        for(String sid:ids){if(sid==null||sid.trim().isEmpty())continue;String id=sid.trim();SessionSeries.Series s=SessionSeries.byId(db,id);JSONObject d=decisions.latestPayloadForSession(id);int records=ExperimentQueries.bySessionId(db,id).size();out.add(new Evidence(id,s,d,records));}
        return out;
    }

    static Evidence primary(List<Evidence> all){Evidence best=null;for(Evidence e:all)if(best==null||e.score>=best.score)best=e;return best;}
    private static boolean hasSeries(List<Evidence> all){for(Evidence e:all)if(e.seriesOk())return true;return false;}
    private static boolean hasChem(List<Evidence> all){for(Evidence e:all)if(e.chemOk())return true;return false;}

    private static int score(SessionSeries.Series s,JSONObject d,int records){
        int x=Math.min(12,Math.max(0,records));
        if(s!=null&&s.usable()){x+=70;x+=Math.min(20,s.samples.size()/5);}
        if(d!=null){x+=100;JSONArray a=d.optJSONArray("candidates");if(a!=null)x+=Math.min(20,a.length()*3);double p=topPosterior(d);if(Double.isFinite(p))x+=(int)Math.round(Math.max(0,Math.min(1,p))*20);int v=validMq(d);if(v>0)x+=Math.min(8,v);String o=d.optString("outcome","");if("IDENTIFIED".equals(o))x+=20;if("QUANTIFIED".equals(o)||d.optBoolean("ppm_reportable",false))x+=40;}
        return x;
    }

    private static Consensus consensus(List<Evidence> all){
        Map<String,int[]> counts=new LinkedHashMap<>();Map<String,Double> sums=new LinkedHashMap<>();int total=0;
        for(Evidence e:all){if(!e.chemOk())continue;String label=e.topCandidate();if(label.isEmpty()||"—".equals(label))continue;total++;int[] n=counts.get(label);if(n==null){n=new int[]{0};counts.put(label,n);sums.put(label,0.0);}n[0]++;double p=e.posterior();if(Double.isFinite(p))sums.put(label,sums.get(label)+p);}
        String best="—";int bestN=0;double mean=Double.NaN;for(Map.Entry<String,int[]> q:counts.entrySet()){if(q.getValue()[0]>bestN){best=q.getKey();bestN=q.getValue()[0];mean=sums.get(q.getKey())/Math.max(1,bestN);}}
        return new Consensus(best,bestN,total,mean);
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}

    private static void executive(Canvas c,FieldSessionReportExporter.Bundle b,List<Evidence> all,Evidence primary,ReportIntelligence.ReportPlan plan,JSONObject hazard,Consensus consensus){
        title(c,"التقرير التحليلي الذكي للجلسة • "+plan.modeAr,42);float y=76;
        String[][] rows={{"الموقع",b.site.name},{"Site ID",b.site.id},{"الجلسة",b.fieldSession.name},{"الحالة",b.fieldSession.status},{"البدء",b.fieldSession.startedTs},{"الانتهاء",nz(b.fieldSession.endedTs).isEmpty()?"نشطة":b.fieldSession.endedTs},{"جلسات القياس الفنية",String.valueOf(all.size())},{"الجلسة التحليلية المرجعية",primary==null?"—":shorten(primary.sessionId,38)}};
        for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=22;}
        JSONObject q=primary==null?null:primary.decision;String chemical=q==null?"—":topCandidate(q),tier=q==null?"—":q.optString("evidence_tier","—");double ppm=q==null?Double.NaN:q.optDouble("ppm",Double.NaN);
        metric(c,420,72,178,72,"نمط التقرير",plan.modeAr,plan.executiveTitleAr,0xFFE3F2FD);
        metric(c,610,72,190,72,"المرشح الأعلى",shorten(chemical,22),tier,0xFFFFF3E0);
        metric(c,420,154,178,72,"ثبات الجلسة",consensus.count+" / "+consensus.total,shorten(consensus.label,24)+" • "+pct(consensus.meanPosterior),0xFFE8F5E9);
        metric(c,610,154,190,72,"الجودة",primary==null?"—":(primary.validMq()<0?"MQ —":"MQ "+primary.validMq()+"/8"),"Posterior "+pct(plan.topPosterior)+" • OOD "+fmt(plan.oodScore),0xFFF4F6F8);
        metric(c,420,236,178,72,"مصفوفة 144",plan.panelCandidateSupported+" دعم جلسة","Accepted "+plan.panelAcceptedIdentities+" • "+plan.panelEvaluated+"/"+plan.panelTarget,0xFFE0F7FA);
        metric(c,610,236,190,72,"القياس الكمي",plan.ppmReportable&&Double.isFinite(ppm)?fmt(ppm)+" ppm":"محجوب",plan.ppmReportable?"M4 reportable":"Screening لا يساوي PPM",plan.ppmReportable?0xFFE8F5E9:0xFFFFF3E0);
        text(c,"أهم الملاحظات الآلية",42,282,12,Color.BLACK,true);float fy=306;int shown=0;if(plan.findings.isEmpty())finding(c,42,fy,755,40,ReportIntelligence.Severity.INFO,"لا توجد ملاحظات منهجية حرجة.","");else for(ReportIntelligence.Finding f:plan.findings){if(shown++>=2)break;finding(c,42,fy,755,44,f.severity,f.titleAr,f.detailAr);fy+=50;}
        if(hazard!=null&&hazard.optJSONArray("candidates")!=null&&hazard.optJSONArray("candidates").length()>0)CandidateHazardCharts.executiveStrip(c,new RectF(42,430,800,526),hazard);
        text(c,"ثبات المرشح بين الجلسات مؤشر اتساق فقط؛ لا يحول Screening إلى هوية مؤكدة ولا يثبت PPM.",42,548,8.1f,Color.DKGRAY,false);
    }

    private static void measurementOverview(Canvas c,FieldSessionReportExporter.Bundle b,List<Evidence> all,Evidence primary,Consensus consensus){
        title(c,"خريطة جلسات القياس الفنية",42);text(c,"كل Measurement Session تبقى مستقلة زمنيًا؛ لا يتم دمجها في منحنى مصطنع.",42,62,8.5f,Color.DKGRAY,false);
        String[] h={"Measurement Session","Samples","Duration","Records","ChemCore","Top candidate","Posterior","MQ"};float[] x={38,238,300,365,425,515,665,745};for(int i=0;i<h.length;i++)text(c,h[i],x[i],88,7.8f,Color.DKGRAY,true);
        float y=110;int shown=0;for(Evidence e:all){if(shown++>=15)break;boolean p=e==primary;if(p)box(c,34,y-14,770,24,0xFFE3F2FD);text(c,(p?"★ ":"")+shorten(e.sessionId,30),x[0],y,7.5f,Color.BLACK,p);text(c,String.valueOf(e.samples()),x[1],y,7.5f,Color.BLACK,false);text(c,String.format(Locale.US,"%.1fs",e.durationMs()/1000.0),x[2],y,7.5f,Color.BLACK,false);text(c,String.valueOf(e.recordCount),x[3],y,7.5f,Color.BLACK,false);text(c,shorten(e.outcome(),16),x[4],y,7.5f,Color.BLACK,false);text(c,shorten(e.topCandidate(),20),x[5],y,7.5f,Color.BLACK,false);text(c,pct(e.posterior()),x[6],y,7.5f,Color.BLACK,false);text(c,e.validMq()<0?"—":e.validMq()+"/8",x[7],y,7.5f,Color.BLACK,false);y+=27;}
        if(all.size()>15)text(c,"+ "+(all.size()-15)+" جلسات إضافية موجودة في Excel التفصيلي",42,y+8,8,Color.DKGRAY,false);
        box(c,42,500,758,44,0xFFF4F6F8);text(c,"الاتساق العام",55,520,9,Color.BLACK,true);text(c,"المرشح الأعلى الأكثر تكرارًا: "+shorten(consensus.label,30)+" • "+consensus.count+" من "+consensus.total+" قرارات ChemCore • متوسط posterior "+pct(consensus.meanPosterior),170,520,8,Color.DKGRAY,false);
    }

    private static void signalEvidence(Canvas c,Evidence primary){
        title(c,"الإشارة الزمنية والبصمة الحسية",42);if(primary==null){empty(c,"لا توجد جلسة قياس يمكن استخدامها.");return;}if(primary.seriesOk()){AnalyticalCharts.mqRsR0(c,new RectF(36,70,806,310),primary.series);AnalyticalCharts.fingerprint(c,new RectF(36,326,405,548),primary.series);AnalyticalCharts.mqSignalQuality(c,new RectF(430,326,806,548),primary.series);}else if(primary.chemOk()){SmartReportCharts.decisionQuality(c,new RectF(36,78,806,272),primary.decision);SmartReportCharts.candidateEvidenceMatrix(c,new RectF(36,294,405,548),primary.decision);SmartReportCharts.evidenceBreadth(c,new RectF(430,294,806,548),primary.decision);}else empty(c,"لا توجد سلسلة زمنية صالحة ولا نتيجة ChemCore.");
    }

    private static void chemicalEvidence(Canvas c,Evidence primary,JSONObject hazard){
        title(c,"مرشحو ChemCore وترتيب الدليل",42);JSONObject q=primary==null?null:primary.decision;if(q==null){empty(c,"لا توجد نتيجة ChemCore محفوظة للجلسة المرجعية.");return;}AnalyticalCharts.candidates(c,new RectF(36,70,405,286),q);AnalyticalCharts.candidateScatter(c,new RectF(430,70,806,286),q);SmartReportCharts.decisionQuality(c,new RectF(36,304,405,548),q);CandidateHazardCharts.table(c,new RectF(430,304,806,548),hazard);
    }

    private static void panelAndHazard(Canvas c,Evidence primary,JSONObject hazard){
        title(c,"مصفوفة 144 • دعم الجلسة • سياق المخاطر",42);JSONObject q=primary==null?null:primary.decision;if(q==null){empty(c,"لا توجد نتيجة ChemCore؛ مصفوفة 144 غير متاحة.");return;}SmartReportCharts.candidateEvidenceMatrix(c,new RectF(36,70,405,284),q);SmartReportCharts.screening144ReliabilityMatrix(c,new RectF(430,70,806,284),q);SmartReportCharts.top144SessionSupport(c,new RectF(36,300,405,550),q);CandidateHazardCharts.table(c,new RectF(430,300,806,550),hazard);
    }

    private static void uncertaintySafetyContext(Canvas c,Evidence primary,ReportIntelligence.ReportPlan plan){
        title(c,plan.includeQuantitativeSafety?"القياس الكمي والسلامة وعدم اليقين":"عدم اليقين والسياق التحليلي",42);JSONObject q=primary==null?null:primary.decision;SessionSeries.Series s=primary==null?null:primary.series;
        if(q==null){if(s!=null&&s.usable()){AnalyticalCharts.temperature(c,new RectF(36,76,282,286),s);AnalyticalCharts.humidity(c,new RectF(297,76,543,286),s);AnalyticalCharts.pressure(c,new RectF(558,76,806,286),s);}emptyBox(c,new RectF(36,315,806,540),"لا توجد نتيجة ChemCore؛ لا يوجد تفسير كمي أو سلامة كيميائية.");return;}
        if(plan.ppmReportable){SmartReportCharts.quantitativeInterval(c,new RectF(36,72,405,286),q);AnalyticalCharts.exposureRatios(c,new RectF(430,72,806,286),q);gasCard(c,new RectF(36,305,405,548),q.optJSONObject("gas_exposure_ppm"));if(s!=null&&s.usable())AnalyticalCharts.mqEnvironmentMatrix(c,new RectF(430,305,806,548),s);else emptyBox(c,new RectF(430,305,806,548),"السياق البيئي الزمني غير متاح");}
        else{SmartReportCharts.decisionQuality(c,new RectF(36,72,405,270),q);AnalyticalCharts.candidateBubbleMap(c,new RectF(430,72,806,270),q);AnalyticalCharts.candidateResiduals(c,new RectF(36,292,405,548),q);if(s!=null&&s.usable())AnalyticalCharts.mqEnvironmentMatrix(c,new RectF(430,292,806,548),s);else AnalyticalCharts.evidenceTiers(c,new RectF(430,292,806,548),q);}
    }

    private static void sensorDynamics(Canvas c,Evidence primary,List<Evidence> all){
        title(c,"ديناميكية الاستجابة وجودة السلسلة الزمنية",42);Evidence e=primary!=null&&primary.seriesOk()?primary:firstSeries(all);if(e==null||!e.seriesOk()){empty(c,"لا توجد سلسلة زمنية صالحة.");return;}SessionSeries.Series s=e.series;AnalyticalCharts.fingerprintRadar(c,new RectF(36,70,282,286),s);AnalyticalCharts.analyticalQualityRadar(c,new RectF(297,70,543,286),s);AnalyticalCharts.mqResponseKinetics(c,new RectF(558,70,806,286),s);AnalyticalCharts.mqPersistenceMap(c,new RectF(36,304,405,548),s);AnalyticalCharts.temperature(c,new RectF(430,304,550,548),s);AnalyticalCharts.humidity(c,new RectF(558,304,678,548),s);AnalyticalCharts.pressure(c,new RectF(686,304,806,548),s);
    }

    private static void candidateDeepDive(Canvas c,Evidence primary){
        title(c,"تحليل المرشحين المتقدم",42);JSONObject q=primary==null?null:primary.decision;if(q==null){empty(c,"لا توجد نتيجة ChemCore.");return;}AnalyticalCharts.candidateBubbleMap(c,new RectF(36,70,405,286),q);AnalyticalCharts.candidateResiduals(c,new RectF(430,70,806,286),q);AnalyticalCharts.evidenceTiers(c,new RectF(36,304,405,548),q);AnalyticalCharts.screeningDonut(c,new RectF(430,304,806,548),q);
    }

    private static void crossSessionConsistency(Canvas c,List<Evidence> all,Consensus consensus){
        title(c,"الثبات والاختلاف بين جلسات القياس",42);text(c,"هذه الصفحة تقارن القرارات المستقلة ولا تدمج السلاسل الزمنية.",42,62,8.5f,Color.DKGRAY,false);candidateStability(c,new RectF(36,78,405,292),all);posteriorStability(c,new RectF(430,78,806,292),all);
        String[] h={"Session","Outcome","Top candidate","Posterior","Margin","OOD","MQ"};float[] x={38,245,350,520,590,660,735};for(int i=0;i<h.length;i++)text(c,h[i],x[i],326,7.8f,Color.DKGRAY,true);float y=350;int shown=0;for(Evidence e:all){if(!e.chemOk())continue;if(shown++>=7)break;JSONObject q=e.decision;text(c,shorten(e.sessionId,28),x[0],y,7.4f,Color.BLACK,false);text(c,shorten(e.outcome(),14),x[1],y,7.4f,Color.BLACK,false);text(c,shorten(e.topCandidate(),19),x[2],y,7.4f,Color.BLACK,false);text(c,pct(e.posterior()),x[3],y,7.4f,Color.BLACK,false);text(c,pct(q.optDouble("posterior_separation_margin",Double.NaN)),x[4],y,7.4f,Color.BLACK,false);text(c,fmt(q.optDouble("ood_score",Double.NaN)),x[5],y,7.4f,Color.BLACK,false);text(c,e.validMq()<0?"—":e.validMq()+"/8",x[6],y,7.4f,Color.BLACK,false);y+=27;}
        text(c,"المرشح الأكثر تكرارًا: "+shorten(consensus.label,32)+" • "+consensus.count+"/"+consensus.total+" • متوسط posterior "+pct(consensus.meanPosterior),42,548,8.5f,Color.DKGRAY,true);
    }

    private static void methodologyProvenance(Canvas c,FieldSessionReportExporter.Bundle b,List<Evidence> all,Evidence primary,ReportIntelligence.ReportPlan plan,ChemicalRepository chemicals,int totalPages){
        title(c,"المنهجية ومصدر القرار",42);JSONObject q=primary==null?null:primary.decision,lib=q==null?null:q.optJSONObject("library");
        text(c,"أساس التقرير",42,78,11,Color.BLACK,true);labelValue(c,"Site",b.site.name,103);labelValue(c,"Site ID",b.site.id,125);labelValue(c,"Field Session",b.fieldSession.id,147);labelValue(c,"Measurement Sessions",String.valueOf(all.size()),169);labelValue(c,"Primary analytical measurement",primary==null?"—":primary.sessionId,191);labelValue(c,"Derived records",String.valueOf(b.records.size()),213);labelValue(c,"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count()),235);
        text(c,"المصدر والمنهجية",42,270,11,Color.BLACK,true);float y=294;String[][] rows={{"Result schema",q==null?"—":q.optString("schema","—")},{"Model",q==null?"—":q.optString("model_version","—")},{"Feature schema",q==null?"—":q.optString("feature_schema","—")},{"Sampling profile",q==null?"—":q.optString("sampling_profile_id","—")},{"Evidence tier",q==null?"—":q.optString("evidence_tier","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":shorten(lib.optString("sha256","—"),54)}};for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=21;}
        float x=445,ty=78;text(c,"منطق التقرير",x,ty,11,Color.BLACK,true);ty+=25;text(c,"Pages: "+totalPages+" • Mode: "+plan.mode.name(),x,ty,8,Color.DKGRAY,false);ty+=21;text(c,"Primary selection: evidence completeness + ChemCore + usable series",x,ty,8,Color.DKGRAY,false);ty+=21;text(c,"144: "+plan.panelCandidateSupported+" session-supported • "+plan.panelEvaluated+"/"+plan.panelTarget,x,ty,8,Color.DKGRAY,false);ty+=21;text(c,"PPM reportable: "+plan.ppmReportable+" • Safety interpretable: "+plan.safetyInterpretable,x,ty,8,Color.DKGRAY,false);ty+=31;text(c,"قواعد التفسير",x,ty,10,Color.BLACK,true);ty+=22;text(c,"• كل Measurement Session تحلل مستقلًا؛ لا يوجد منحنى مدمج مصطنع.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• ثبات المرشح بين الجلسات يصف الاتساق ولا يثبت الهوية.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• Screening-fit concentration لا يساوي PPM مهني قابل للتقرير.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• M4 فقط يسمح بالتفسير الكمي المهني وفق بوابات ChemCore.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• سياق OEL/LFL في Screening لا ينشئ حكم امتثال أو إنذار تعرض.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• وظيفة Tracking التشغيلية غير مدرجة في التقرير.",x,ty,7.5f,Color.DKGRAY,false);ty+=30;text(c,"Planner rationale",x,ty,9,Color.BLACK,true);ty+=18;text(c,shorten(plan.rationale,82),x,ty,7.2f,Color.DKGRAY,false);
    }

    private static Evidence firstSeries(List<Evidence> all){for(Evidence e:all)if(e.seriesOk())return e;return null;}

    private static void candidateStability(Canvas c,RectF r,List<Evidence> all){
        box(c,r.left,r.top,r.width(),r.height(),0xFFF8FAFC);text(c,"تكرار المرشح الأعلى",r.left+12,r.top+22,10,Color.BLACK,true);Map<String,Integer> m=new LinkedHashMap<>();for(Evidence e:all){if(!e.chemOk())continue;String k=e.topCandidate();if(k.isEmpty()||"—".equals(k))continue;Integer n=m.get(k);m.put(k,n==null?1:n+1);}int max=1;for(Integer n:m.values())max=Math.max(max,n);float y=r.top+50;int shown=0;for(Map.Entry<String,Integer> q:m.entrySet()){if(shown++>=6)break;float w=(r.width()-145)*(q.getValue()/(float)max);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFF1565C0);c.drawRect(r.left+130,y-11,r.left+130+w,y+3,p);text(c,shorten(q.getKey(),18),r.left+10,y,7.5f,Color.DKGRAY,false);text(c,String.valueOf(q.getValue()),r.right-18,y,7.5f,Color.BLACK,true,Paint.Align.RIGHT);y+=25;}if(m.isEmpty())text(c,"لا توجد قرارات ChemCore",r.centerX(),r.centerY(),9,Color.DKGRAY,false,Paint.Align.CENTER);
    }

    private static void posteriorStability(Canvas c,RectF r,List<Evidence> all){
        box(c,r.left,r.top,r.width(),r.height(),0xFFF8FAFC);text(c,"Posterior عبر الجلسات",r.left+12,r.top+22,10,Color.BLACK,true);float base=r.bottom-34,left=r.left+36,right=r.right-18;Paint ax=new Paint(Paint.ANTI_ALIAS_FLAG);ax.setColor(0xFFB0BEC5);ax.setStrokeWidth(1);c.drawLine(left,base,right,base,ax);int n=0;for(Evidence e:all)if(e.chemOk()&&Double.isFinite(e.posterior()))n++;if(n==0){text(c,"لا توجد قيم posterior",r.centerX(),r.centerY(),9,Color.DKGRAY,false,Paint.Align.CENTER);return;}float step=(right-left)/Math.max(1,n-1);int i=0;for(Evidence e:all){double p=e.posterior();if(!e.chemOk()||!Double.isFinite(p))continue;float x=left+i++*step,y=base-(float)Math.max(0,Math.min(1,p))*(r.height()-70);Paint dot=new Paint(Paint.ANTI_ALIAS_FLAG);dot.setColor(0xFFC62828);dot.setStyle(Paint.Style.FILL);c.drawCircle(x,y,5,dot);}text(c,"0",left,base+18,7,Color.DKGRAY,false);text(c,"100%",left,r.top+45,7,Color.DKGRAY,false);
    }

    private static String topCandidate(JSONObject q){if(q==null)return"—";String c=q.optString("chemical","").trim();if(!c.isEmpty()&&!"—".equals(c))return c;JSONArray a=q.optJSONArray("candidates");if(a!=null&&a.length()>0){JSONObject x=a.optJSONObject(0);if(x!=null){String v=x.optString("label","").trim();if(!v.isEmpty())return v;}}return"—";}
    private static double topPosterior(JSONObject q){if(q==null)return Double.NaN;double p=q.optDouble("top_posterior",Double.NaN);if(Double.isFinite(p))return p;JSONArray a=q.optJSONArray("candidates");if(a!=null&&a.length()>0){JSONObject x=a.optJSONObject(0);if(x!=null){p=x.optDouble("posterior",Double.NaN);if(Double.isFinite(p))return p;}}return Double.NaN;}
    private static int validMq(JSONObject q){if(q==null)return-1;JSONObject x=q.optJSONObject("measurement_quality");if(x==null)x=q.optJSONObject("lab_quality");if(x==null)return-1;return x.optInt("valid_mq_channels",x.optInt("valid_channels",-1));}

    private static void metric(Canvas c,float x,float y,float w,float h,String label,String value,String note,int color){box(c,x,y,w,h,color);text(c,label,x+10,y+19,8.5f,Color.DKGRAY,true);text(c,shorten(value,27),x+10,y+43,11.5f,Color.BLACK,true);text(c,shorten(note,36),x+10,y+61,7.2f,Color.DKGRAY,false);}
    private static void finding(Canvas c,float x,float y,float w,float h,ReportIntelligence.Severity severity,String title,String detail){int color=severity==ReportIntelligence.Severity.CRITICAL?0xFFFFEBEE:severity==ReportIntelligence.Severity.WARNING?0xFFFFF3E0:severity==ReportIntelligence.Severity.NOTICE?0xFFE3F2FD:0xFFF4F6F8;box(c,x,y,w,h,color);text(c,shorten(title,54),x+10,y+16,8.5f,Color.BLACK,true);text(c,shorten(detail,118),x+10,y+33,7.2f,Color.DKGRAY,false);}
    private static void gasCard(Canvas c,RectF r,JSONObject gas){if(gas==null||!gas.optBoolean("input_gate_passed",false)){emptyBox(c,r,"تفسير التعرض/الاشتعال محجوب");return;}box(c,r.left,r.top,r.width(),r.height(),0xFFF4F6F8);text(c,"تفسير السلامة",r.left+12,r.top+24,11,Color.BLACK,true);text(c,"المادة: "+gas.optString("chemical","—")+"   "+gas.optString("hazard_symbols",""),r.left+12,r.top+52,9,Color.BLACK,false);text(c,"Toxicity: "+gas.optString("toxicity_state",gas.optString("state","—")),r.left+12,r.top+78,8.5f,Color.BLACK,false);text(c,"Flammability: "+gas.optString("flammability_state","—"),r.left+12,r.top+102,8.5f,Color.BLACK,false);text(c,"Hazard priority: "+pct100(gas.optDouble("hazard_priority_percent",Double.NaN)),r.left+12,r.top+128,8.5f,Color.BLACK,false);text(c,"%LFL: "+pct100(gas.optDouble("percent_lfl",Double.NaN)),r.left+12,r.top+152,8.5f,Color.BLACK,false);text(c,"Compliance: "+gas.optString("compliance_determination","NOT_MADE"),r.left+12,r.top+180,8,Color.DKGRAY,false);text(c,"لا يصدر HazRob حكم امتثال قانوني تلقائيًا.",r.left+12,r.bottom-18,7.5f,Color.DKGRAY,false);}
    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);}
    private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,8.5f,Color.DKGRAY,true);text(c,shorten(v,65),180,y,9,Color.BLACK,false);}
    private static void box(Canvas c,float x,float y,float w,float h,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);}
    private static void empty(Canvas c,String message){emptyBox(c,new RectF(42,90,800,520),message);}
    private static void emptyBox(Canvas c,RectF r,String message){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFF3F3F3);p.setStyle(Paint.Style.FILL);c.drawRect(r,p);text(c,message,r.centerX(),r.centerY(),11,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void chrome(Canvas c,int page,int total,FieldSessionReportExporter.Bundle b,ReportIntelligence.ReportPlan plan){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION+" • HR069",MARGIN,23,10,Color.BLACK,true);text(c,b.site.name+" ["+shorten(b.site.id,18)+"] • "+b.fieldSession.name+" • "+plan.modeAr,PAGE_WIDTH/2f,23,10,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.1f%%",v<=1.000001?v*100.0:v);}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String shorten(String s,int n){String v=nz(s);return v.length()<=n?v:v.substring(0,Math.max(1,n-1))+"…";}
    private static String nz(String s){return s==null?"":s;}
}