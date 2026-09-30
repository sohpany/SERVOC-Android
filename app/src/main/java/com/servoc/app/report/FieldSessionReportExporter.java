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
import com.servoc.app.field.FieldSessionRepository;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Smart evidence-driven report for one immutable user field session. */
public final class FieldSessionReportExporter {
    private FieldSessionReportExporter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=4,MAX_PAGES=5;
    private static final float MARGIN=28.35f;

    public static final class Bundle {
        public final String fieldSessionId;
        public final FieldSessionRepository.Site site;
        public final FieldSessionRepository.FieldSession fieldSession;
        public final List<String> measurementSessionIds;
        public final List<ExperimentQueries.Record> records;
        public final String primaryMeasurementSessionId;
        public final SessionSeries.Series primarySeries;
        public final JSONObject primaryDecision;
        Bundle(String fieldSessionId,FieldSessionRepository.Site site,FieldSessionRepository.FieldSession fieldSession,
               List<String> measurementSessionIds,List<ExperimentQueries.Record> records,String primaryMeasurementSessionId,
               SessionSeries.Series primarySeries,JSONObject primaryDecision){
            this.fieldSessionId=fieldSessionId;this.site=site;this.fieldSession=fieldSession;this.measurementSessionIds=measurementSessionIds;
            this.records=records;this.primaryMeasurementSessionId=primaryMeasurementSessionId;this.primarySeries=primarySeries;this.primaryDecision=primaryDecision;
        }
    }

    public static Bundle resolve(ExperimentsDb db,String fieldSessionId){
        if(db==null)throw new IllegalArgumentException("db required");String fs=required(fieldSessionId);
        FieldSessionRepository repo=new FieldSessionRepository(db);FieldSessionRepository.Site site=null;FieldSessionRepository.FieldSession field=null;
        for(FieldSessionRepository.Site s:repo.sites()){for(FieldSessionRepository.FieldSession q:repo.sessionsForSite(s.id))if(fs.equals(q.id)){site=s;field=q;break;}if(field!=null)break;}
        if(field==null||site==null)throw new IllegalArgumentException("field session not found");
        List<String> sessions=repo.measurementSessionIds(fs);List<ExperimentQueries.Record> records=new ArrayList<>();for(String sid:sessions)records.addAll(ExperimentQueries.bySessionId(db,sid));
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);String primary="";JSONObject decision=null;
        for(int i=sessions.size()-1;i>=0;i--){String sid=sessions.get(i);JSONObject p=decisions.latestPayloadForSession(sid);if(p!=null){primary=sid;decision=p;break;}}
        if(primary.isEmpty()&&!sessions.isEmpty())primary=sessions.get(sessions.size()-1);SessionSeries.Series series=primary.isEmpty()?null:SessionSeries.byId(db,primary);
        return new Bundle(fs,site,field,new ArrayList<>(sessions),records,primary,series,decision);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");Bundle b=resolve(db,fieldSessionId);JSONObject hazard=CandidateHazardContext.evaluate(db,b.primaryDecision);
        ReportIntelligence.ReportPlan plan=ReportIntelligence.plan(b.primarySeries,b.primaryDecision,b.measurementSessionIds.size());int total=Math.max(MIN_PAGES,Math.min(MAX_PAGES,plan.pageCount));
        PdfDocument pdf=new PdfDocument();
        PdfDocument.Page p=page(pdf,1);executive(p.getCanvas(),b,plan,hazard);chrome(p.getCanvas(),1,total,b,plan);pdf.finishPage(p);
        p=page(pdf,2);sensorEvidence(p.getCanvas(),b);chrome(p.getCanvas(),2,total,b,plan);pdf.finishPage(p);
        p=page(pdf,3);chemicalEvidence(p.getCanvas(),b,hazard);chrome(p.getCanvas(),3,total,b,plan);pdf.finishPage(p);
        p=page(pdf,4);quantitationSafetyUncertainty(p.getCanvas(),b,plan);chrome(p.getCanvas(),4,total,b,plan);pdf.finishPage(p);
        if(total==5){p=page(pdf,5);methodologyProvenance(p.getCanvas(),b,plan);chrome(p.getCanvas(),5,total,b,plan);pdf.finishPage(p);}
        pdf.writeTo(out);pdf.close();
    }

    /** XLSX is the lossless data companion; the PDF is the compact intelligent narrative. */
    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);if(b.records.isEmpty())throw new IllegalStateException("field session has no derived experiment workbook");
        if(b.records.size()==1){ReportExporter.writeXlsx(out,b.records.get(0),chemicals);return;}
        try(ZipOutputStream zip=new ZipOutputStream(out)){putManifest(zip,b);for(ExperimentQueries.Record r:b.records){ByteArrayOutputStream q=new ByteArrayOutputStream();ReportExporter.writeXlsx(q,r,chemicals);put(zip,"measurements/measurement-"+r.id+"-"+safe(r.sessionId)+".xlsx",q.toByteArray());}zip.finish();}
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}

    private static void executive(Canvas c,Bundle b,ReportIntelligence.ReportPlan plan,JSONObject hazard){
        title(c,"الملخص التنفيذي • "+plan.modeAr,42);float y=76;
        String[][] rows={{"الموقع",b.site.name},{"الجلسة",b.fieldSession.name},{"الحالة",b.fieldSession.status},{"البدء",b.fieldSession.startedTs},{"الانتهاء",nz(b.fieldSession.endedTs).isEmpty()?"نشطة":b.fieldSession.endedTs},{"جلسات القياس الفنية",String.valueOf(b.measurementSessionIds.size())},{"مرجع القرار",b.primaryMeasurementSessionId.isEmpty()?"—":shorten(b.primaryMeasurementSessionId,42)}};for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=23;}
        JSONObject q=b.primaryDecision;String chemical=q==null?"—":q.optString("chemical","—"),tier=q==null?"—":q.optString("evidence_tier","—");double ppm=q==null?Double.NaN:q.optDouble("ppm",Double.NaN);
        metric(c,420,72,177,76,"نمط التقرير",plan.modeAr,plan.executiveTitleAr,0xFFE3F2FD);metric(c,610,72,190,76,"الهوية/المرشح",shorten(chemical,22),tier,plan.mode==ReportIntelligence.ReportMode.QUANTIFIED||plan.mode==ReportIntelligence.ReportMode.IDENTIFIED?0xFFE8F5E9:0xFFFFF3E0);
        metric(c,420,160,177,76,"الدعم التحليلي",pct100(plan.analyticalConfidencePct),"Ranking "+pct(plan.topPosterior),0xFFF3E5F5);metric(c,610,160,190,76,"الفصل / OOD",pct(plan.separationMargin),"OOD "+fmt(plan.oodScore)+" • H "+fmt(plan.entropy),0xFFF4F6F8);
        metric(c,420,248,177,76,"مصفوفة 144",plan.panelCandidateSupported+" دعم جلسة","Accepted "+plan.panelAcceptedIdentities+" • "+plan.panelEvaluated+"/"+plan.panelTarget,0xFFE0F7FA);metric(c,610,248,190,76,"القياس الكمي",plan.ppmReportable&&Double.isFinite(ppm)?fmt(ppm)+" ppm":"محجوب",plan.ppmReportable?"M4 reportable":"لا يُستخدم كقياس تعرض",plan.ppmReportable?0xFFE8F5E9:0xFFFFF3E0);
        text(c,"أهم الملاحظات الآلية",42,272,12,Color.BLACK,true);float fy=296;int shown=0;if(plan.findings.isEmpty())finding(c,42,fy,755,40,ReportIntelligence.Severity.INFO,"لا توجد ملاحظات منهجية حرجة أضافها مخطط التقرير.","");else for(ReportIntelligence.Finding f:plan.findings){if(shown++>=3)break;finding(c,42,fy,755,44,f.severity,f.titleAr,f.detailAr);fy+=50;}
        if(hazard!=null&&hazard.optJSONArray("candidates")!=null&&hazard.optJSONArray("candidates").length()>0)CandidateHazardCharts.executiveStrip(c,new RectF(42,448,800,531),hazard);
        text(c,"يفصل التقرير بين قدرة المكتبة، دعم الجلسة، الهوية المقبولة، والقياس الكمي M4. EST hazard context لا يساوي إنذار تعرض أو LFL.",42,548,8.1f,Color.DKGRAY,false);
    }

    private static void sensorEvidence(Canvas c,Bundle b){
        title(c,"دليل الحساسات والبصمة",42);if(b.primarySeries!=null&&b.primarySeries.usable()){AnalyticalCharts.mqRsR0(c,new RectF(36,70,806,292),b.primarySeries);AnalyticalCharts.fingerprint(c,new RectF(36,308,282,548),b.primarySeries);AnalyticalCharts.mqSignalQuality(c,new RectF(297,308,543,548),b.primarySeries);AnalyticalCharts.mqPersistenceMap(c,new RectF(558,308,806,548),b.primarySeries);}else if(b.primaryDecision!=null){SmartReportCharts.decisionQuality(c,new RectF(36,76,806,270),b.primaryDecision);SmartReportCharts.candidateEvidenceMatrix(c,new RectF(36,292,405,548),b.primaryDecision);SmartReportCharts.evidenceBreadth(c,new RectF(430,292,806,548),b.primaryDecision);}else empty(c,"لا توجد سلسلة زمنية ولا نتيجة ChemCore يمكن استخدامها في صفحة الدليل.");
    }

    private static void chemicalEvidence(Canvas c,Bundle b,JSONObject hazard){
        title(c,"الدليل الكيميائي • المرشحون • مصفوفة 144",42);JSONObject q=b.primaryDecision;if(q==null){empty(c,"لا توجد نتيجة ChemCore محفوظة؛ لا يمكن إنشاء مصفوفة دليل جلسة للمواد.");return;}
        SmartReportCharts.candidateEvidenceMatrix(c,new RectF(36,70,405,284),q);CandidateHazardCharts.table(c,new RectF(430,70,806,284),hazard);SmartReportCharts.screening144ReliabilityMatrix(c,new RectF(36,300,405,550),q);SmartReportCharts.top144SessionSupport(c,new RectF(430,300,806,550),q);
    }

    private static void quantitationSafetyUncertainty(Canvas c,Bundle b,ReportIntelligence.ReportPlan plan){
        JSONObject q=b.primaryDecision;title(c,plan.includeQuantitativeSafety?"القياس الكمي والسلامة والسياق":"عدم اليقين والسياق التحليلي",42);if(q==null){if(b.primarySeries!=null&&b.primarySeries.usable()){AnalyticalCharts.temperature(c,new RectF(36,78,282,300),b.primarySeries);AnalyticalCharts.humidity(c,new RectF(297,78,543,300),b.primarySeries);AnalyticalCharts.pressure(c,new RectF(558,78,806,300),b.primarySeries);}emptyBox(c,new RectF(36,330,806,540),"لا توجد نتيجة ChemCore؛ لا يوجد تفسير كمي أو سلامة كيميائية.");return;}
        if(plan.ppmReportable){SmartReportCharts.quantitativeInterval(c,new RectF(36,72,405,286),q);AnalyticalCharts.exposureRatios(c,new RectF(430,72,806,286),q);gasCard(c,new RectF(36,305,405,548),q.optJSONObject("gas_exposure_ppm"));if(b.primarySeries!=null&&b.primarySeries.usable()){AnalyticalCharts.temperature(c,new RectF(430,305,550,548),b.primarySeries);AnalyticalCharts.humidity(c,new RectF(558,305,678,548),b.primarySeries);AnalyticalCharts.pressure(c,new RectF(686,305,806,548),b.primarySeries);}else emptyBox(c,new RectF(430,305,806,548),"السياق البيئي الزمني غير متاح");}
        else{SmartReportCharts.decisionQuality(c,new RectF(36,72,806,270),q);AnalyticalCharts.candidateBubbleMap(c,new RectF(36,292,405,548),q);if(b.primarySeries!=null&&b.primarySeries.usable())AnalyticalCharts.mqEnvironmentMatrix(c,new RectF(430,292,806,548),b.primarySeries);else AnalyticalCharts.evidenceTiers(c,new RectF(430,292,806,548),q);}
    }

    /** Conditional page 5. Operational Tracking is deliberately excluded. */
    private static void methodologyProvenance(Canvas c,Bundle b,ReportIntelligence.ReportPlan plan){
        title(c,"المنهجية ومصدر القرار",42);JSONObject q=b.primaryDecision,lib=q==null?null:q.optJSONObject("library");
        text(c,"أساس التقرير",42,78,11,Color.BLACK,true);labelValue(c,"Field Session",b.fieldSession.id,103);labelValue(c,"Measurement Sessions",String.valueOf(b.measurementSessionIds.size()),125);labelValue(c,"Primary analytical measurement",b.primaryMeasurementSessionId.isEmpty()?"—":b.primaryMeasurementSessionId,147);labelValue(c,"Derived records",String.valueOf(b.records.size()),169);
        text(c,"المصدر والمنهجية",42,210,11,Color.BLACK,true);float y=234;String[][] rows={{"Result schema",q==null?"—":q.optString("schema","—")},{"Model",q==null?"—":q.optString("model_version","—")},{"Feature schema",q==null?"—":q.optString("feature_schema","—")},{"Sampling profile",q==null?"—":q.optString("sampling_profile_id","—")},{"Evidence tier",q==null?"—":q.optString("evidence_tier","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":shorten(lib.optString("sha256","—"),60)}};for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=21;}
        text(c,"منطق إعداد التقرير",445,78,11,Color.BLACK,true);float x=445,ty=103;text(c,"Mode: "+plan.mode.name()+" • Pages: "+plan.pageCount,x,ty,8,Color.DKGRAY,false);ty+=22;text(c,"144: "+plan.panelCandidateSupported+" session-supported • "+plan.panelAcceptedIdentities+" accepted • "+plan.arrayUnsupported+" array-unsupported",x,ty,8,Color.DKGRAY,false);ty+=22;text(c,"PPM reportable: "+plan.ppmReportable+" • Safety interpretable: "+plan.safetyInterpretable,x,ty,8,Color.DKGRAY,false);ty+=30;text(c,"قواعد التفسير",x,ty,10,Color.BLACK,true);ty+=22;text(c,"• الدعم التحليلي دليل إيجابي موزون؛ ليس Probability.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• Screening-fit concentration لا يساوي PPM مهني قابل للتقرير.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• مقارنات EST مع OEL/LFL سياق أولوية فقط ولا تنشئ إنذارًا.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• M4 فقط يسمح بالتفسير الكمي المهني وفق بوابات ChemCore.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• جلسات القياس الفرعية لا تُدمج في منحنى زمني مصطنع.",x,ty,7.5f,Color.DKGRAY,false);ty+=19;text(c,"• وظيفة Tracking التشغيلية غير مدرجة في التقرير.",x,ty,7.5f,Color.DKGRAY,false);ty+=28;text(c,"Planner rationale",x,ty,9,Color.BLACK,true);ty+=18;text(c,shorten(plan.rationale,82),x,ty,7.2f,Color.DKGRAY,false);
        text(c,"تحتفظ الصفحة فقط بالـprovenance اللازم للتدقيق العلمي؛ لا تعرض حالة Tracking أو تاريخه أو المرشح المتتبَّع.",42,538,8,Color.DKGRAY,false);
    }

    private static void metric(Canvas c,float x,float y,float w,float h,String label,String value,String note,int color){box(c,x,y,w,h,color);text(c,label,x+10,y+19,8.5f,Color.DKGRAY,true);text(c,shorten(value,27),x+10,y+44,12,Color.BLACK,true);text(c,shorten(note,35),x+10,y+64,7.5f,Color.DKGRAY,false);}
    private static void finding(Canvas c,float x,float y,float w,float h,ReportIntelligence.Severity severity,String title,String detail){int color=severity==ReportIntelligence.Severity.CRITICAL?0xFFFFEBEE:severity==ReportIntelligence.Severity.WARNING?0xFFFFF3E0:severity==ReportIntelligence.Severity.NOTICE?0xFFE3F2FD:0xFFF4F6F8;box(c,x,y,w,h,color);text(c,shorten(title,54),x+10,y+16,8.5f,Color.BLACK,true);text(c,shorten(detail,118),x+10,y+33,7.2f,Color.DKGRAY,false);}
    private static void gasCard(Canvas c,RectF r,JSONObject gas){if(gas==null||!gas.optBoolean("input_gate_passed",false)){emptyBox(c,r,"تفسير التعرض/الاشتعال محجوب");return;}box(c,r.left,r.top,r.width(),r.height(),0xFFF4F6F8);text(c,"تفسير السلامة",r.left+12,r.top+24,11,Color.BLACK,true);text(c,"المادة: "+gas.optString("chemical","—")+"   "+gas.optString("hazard_symbols",""),r.left+12,r.top+52,9,Color.BLACK,false);text(c,"Toxicity: "+gas.optString("toxicity_state",gas.optString("state","—")),r.left+12,r.top+78,8.5f,Color.BLACK,false);text(c,"Flammability: "+gas.optString("flammability_state","—"),r.left+12,r.top+102,8.5f,Color.BLACK,false);text(c,"Hazard priority: "+pct100(gas.optDouble("hazard_priority_percent",Double.NaN)),r.left+12,r.top+128,8.5f,Color.BLACK,false);text(c,"%LFL: "+pct100(gas.optDouble("percent_lfl",Double.NaN)),r.left+12,r.top+152,8.5f,Color.BLACK,false);text(c,"Compliance: "+gas.optString("compliance_determination","NOT_MADE"),r.left+12,r.top+180,8,Color.DKGRAY,false);text(c,"لا يصدر HazRob حكم امتثال قانوني تلقائيًا.",r.left+12,r.bottom-18,7.5f,Color.DKGRAY,false);}
    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);}
    private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,8.5f,Color.DKGRAY,true);text(c,shorten(v,65),180,y,9,Color.BLACK,false);}
    private static void box(Canvas c,float x,float y,float w,float h,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);}
    private static void empty(Canvas c,String message){emptyBox(c,new RectF(42,90,800,520),message);}
    private static void emptyBox(Canvas c,RectF r,String message){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFF3F3F3);p.setStyle(Paint.Style.FILL);c.drawRect(r,p);text(c,message,r.centerX(),r.centerY(),11,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void chrome(Canvas c,int page,int total,Bundle b,ReportIntelligence.ReportPlan plan){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION+" • HR069",MARGIN,23,10,Color.BLACK,true);text(c,b.site.name+" • "+b.fieldSession.name+" • "+plan.modeAr,PAGE_WIDTH/2f,23,10,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.1f%%",v<=1.000001?v*100.0:v);}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String shorten(String s,int n){String v=nz(s);return v.length()<=n?v:v.substring(0,Math.max(1,n-1))+"…";}
    private static String nz(String s){return s==null?"":s;}
    private static void putManifest(ZipOutputStream z,Bundle b)throws Exception{StringBuilder j=new StringBuilder("{\n  \"schema\": \"hazrob-field-report/3\",\n  \"site_id\": \"").append(json(b.site.id)).append("\",\n  \"field_session_id\": \"").append(json(b.fieldSessionId)).append("\",\n  \"measurement_count\": ").append(b.measurementSessionIds.size()).append(",\n  \"derived_record_count\": ").append(b.records.size()).append(",\n  \"measurements\": [");for(int i=0;i<b.records.size();i++){ExperimentQueries.Record r=b.records.get(i);if(i>0)j.append(",");j.append("\n    {\"experiment_id\":").append(r.id).append(",\"session_id\":\"").append(json(r.sessionId)).append("\",\"utc\":\"").append(json(r.ts)).append("\"}");}j.append("\n  ]\n}\n");put(z,"manifest.json",j.toString().getBytes(StandardCharsets.UTF_8));}
    private static void put(ZipOutputStream z,String name,byte[] bytes)throws Exception{ZipEntry e=new ZipEntry(name);z.putNextEntry(e);z.write(bytes);z.closeEntry();}
    private static String safe(String s){return (s==null?"unknown":s).replaceAll("[^A-Za-z0-9._-]","_");}
    private static String json(String s){if(s==null)return"";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private static String required(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("field session required");return v;}
}
