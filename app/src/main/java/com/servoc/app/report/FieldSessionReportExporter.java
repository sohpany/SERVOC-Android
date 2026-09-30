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

/**
 * One compact user report per immutable field session.
 *
 * PDF contract: A4 landscape, 4 pages normally and 5 pages when analytical/provenance
 * evidence exists. A field session may contain several technical measurement sessions;
 * they remain individually traceable and are never merged into a synthetic time-series.
 */
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
            this.fieldSessionId=fieldSessionId;this.site=site;this.fieldSession=fieldSession;
            this.measurementSessionIds=measurementSessionIds;this.records=records;
            this.primaryMeasurementSessionId=primaryMeasurementSessionId;this.primarySeries=primarySeries;this.primaryDecision=primaryDecision;
        }
    }

    public static Bundle resolve(ExperimentsDb db,String fieldSessionId){
        if(db==null)throw new IllegalArgumentException("db required");String fs=required(fieldSessionId);
        FieldSessionRepository repo=new FieldSessionRepository(db);FieldSessionRepository.Site site=null;FieldSessionRepository.FieldSession field=null;
        for(FieldSessionRepository.Site s:repo.sites()){
            for(FieldSessionRepository.FieldSession q:repo.sessionsForSite(s.id))if(fs.equals(q.id)){site=s;field=q;break;}
            if(field!=null)break;
        }
        if(field==null||site==null)throw new IllegalArgumentException("field session not found");
        List<String> sessions=repo.measurementSessionIds(fs);List<ExperimentQueries.Record> out=new ArrayList<>();
        for(String sid:sessions)out.addAll(ExperimentQueries.bySessionId(db,sid));
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);String primary="";JSONObject decision=null;
        for(int i=sessions.size()-1;i>=0;i--){String sid=sessions.get(i);JSONObject p=decisions.latestPayloadForSession(sid);if(p!=null){primary=sid;decision=p;break;}}
        if(primary.isEmpty()&&!sessions.isEmpty())primary=sessions.get(sessions.size()-1);
        SessionSeries.Series series=primary.isEmpty()?null:SessionSeries.byId(db,primary);
        return new Bundle(fs,site,field,new ArrayList<>(sessions),out,primary,series,decision);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");Bundle b=resolve(db,fieldSessionId);
        boolean fifth=b.primaryDecision!=null||b.measurementSessionIds.size()>1;int total=fifth?5:4;
        PdfDocument pdf=new PdfDocument();
        PdfDocument.Page p=page(pdf,1);executive(p.getCanvas(),b);chrome(p.getCanvas(),1,total,b);pdf.finishPage(p);
        p=page(pdf,2);signals(p.getCanvas(),b);chrome(p.getCanvas(),2,total,b);pdf.finishPage(p);
        p=page(pdf,3);analysis(p.getCanvas(),b);chrome(p.getCanvas(),3,total,b);pdf.finishPage(p);
        p=page(pdf,4);safetyEnvironment(p.getCanvas(),b);chrome(p.getCanvas(),4,total,b);pdf.finishPage(p);
        if(fifth){p=page(pdf,5);traceability(p.getCanvas(),b);chrome(p.getCanvas(),5,total,b);pdf.finishPage(p);}
        pdf.writeTo(out);pdf.close();
    }

    /**
     * XLSX remains a lossless data companion. A multi-measurement field session is packaged
     * because the existing scientific workbook is measurement-bound; PDF is the single compact
     * field-session report requested by the operator.
     */
    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);if(b.records.isEmpty())throw new IllegalStateException("field session has no derived experiment workbook");
        if(b.records.size()==1){ReportExporter.writeXlsx(out,b.records.get(0),chemicals);return;}
        try(ZipOutputStream zip=new ZipOutputStream(out)){putManifest(zip,b);for(ExperimentQueries.Record r:b.records){ByteArrayOutputStream q=new ByteArrayOutputStream();ReportExporter.writeXlsx(q,r,chemicals);put(zip,"measurements/measurement-"+r.id+"-"+safe(r.sessionId)+".xlsx",q.toByteArray());}zip.finish();}
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}

    private static void executive(Canvas c,Bundle b){
        title(c,"ملخص الجلسة الميدانية",42);float y=76;
        String outcome=b.primaryDecision==null?"—":b.primaryDecision.optString("outcome","—"),chemical=b.primaryDecision==null?"—":b.primaryDecision.optString("chemical","—"),tier=b.primaryDecision==null?"—":b.primaryDecision.optString("evidence_tier","—");
        boolean ppmOk=b.primaryDecision!=null&&b.primaryDecision.optBoolean("ppm_reportable",false);double ppm=b.primaryDecision==null?Double.NaN:b.primaryDecision.optDouble("ppm",Double.NaN);
        String[][] rows={{"الموقع",b.site.name},{"الجلسة",b.fieldSession.name},{"Field Session ID",b.fieldSession.id},{"البدء",b.fieldSession.startedTs},{"الانتهاء",nz(b.fieldSession.endedTs).isEmpty()?"نشطة":b.fieldSession.endedTs},{"الحالة",b.fieldSession.status},{"جلسات القياس الفنية",String.valueOf(b.measurementSessionIds.size())},{"السجلات التحليلية المشتقة",String.valueOf(b.records.size())},{"جلسة الدليل الأساسية",b.primaryMeasurementSessionId.isEmpty()?"—":b.primaryMeasurementSessionId}};
        for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=22;}
        float x=455,top=82,w=330,h=66;box(c,x,top,w,h,0xFFF4F6F8);text(c,"القرار التحليلي",x+12,top+20,10,Color.DKGRAY,true);text(c,outcome+" • "+tier,x+12,top+43,13,Color.BLACK,true);
        box(c,x,top+78,w,h,0xFFF4F6F8);text(c,"المرشح الأعلى",x+12,top+98,10,Color.DKGRAY,true);text(c,chemical,x+12,top+121,13,Color.BLACK,true);
        box(c,x,top+156,w,h,ppmOk?0xFFE8F5E9:0xFFFFF3E0);text(c,"PPM الكمي",x+12,top+176,10,Color.DKGRAY,true);text(c,ppmOk&&Double.isFinite(ppm)?String.format(Locale.US,"%.3f ppm • قابل للتقرير",ppm):"محجوب — بوابة M4/الهوية غير مكتملة",x+12,top+199,11,ppmOk?0xFF1B5E20:0xFFE65100,true);
        JSONObject q=b.primaryDecision==null?null:first(b.primaryDecision.optJSONObject("lab_quality"),b.primaryDecision.optJSONObject("measurement_quality"));
        box(c,x,top+234,w,110,0xFFF4F6F8);text(c,"جودة الدليل",x+12,top+254,10,Color.DKGRAY,true);if(q==null)text(c,"لا توجد لقطة جودة مرتبطة",x+12,top+279,11,Color.DKGRAY,false);else{text(c,"MQ صالح: "+q.optInt("valid_mq_channels",q.optInt("valid_channels",0))+"/8",x+12,top+280,11,Color.BLACK,false);text(c,"R0: "+q.optInt("r0_normalized_channels",q.optInt("calibrated_r0_channels",0))+"/8 • M4-ref: "+q.optInt("reference_validated_channels",0)+"/8",x+12,top+302,11,Color.BLACK,false);text(c,"Capture: "+(q.optBoolean("capture_ready",false)?"READY":"WITHHELD"),x+12,top+324,11,Color.BLACK,false);}
        text(c,"هذا تقرير الجلسة الميدانية كاملة. لا تُدمج جلسات القياس الفنية في سلسلة زمنية مصطنعة؛ الرسوم التفصيلية تستخدم جلسة دليل أساسية محددة ومُعرّفة أعلاه.",42,530,9,Color.DKGRAY,false);
    }

    private static void signals(Canvas c,Bundle b){
        title(c,"الإشارات والبصمة",42);if(b.primarySeries==null||!b.primarySeries.usable()){empty(c,"لا تتوفر سلسلة زمنية صالحة لجلسة الدليل الأساسية.");return;}
        AnalyticalCharts.mqRsR0(c,new RectF(36,70,806,300),b.primarySeries);
        AnalyticalCharts.fingerprint(c,new RectF(36,315,405,540),b.primarySeries);
        AnalyticalCharts.mqSignalQuality(c,new RectF(430,315,806,540),b.primarySeries);
    }

    private static void analysis(Canvas c,Bundle b){
        title(c,"التحليل الكيميائي",42);JSONObject q=b.primaryDecision;if(q==null){empty(c,"لا توجد نتيجة ChemCore محفوظة ضمن هذه الجلسة الميدانية.");return;}
        text(c,"Outcome: "+q.optString("outcome","—")+" • Evidence: "+q.optString("evidence_tier","—")+" • Posterior: "+pct(q.optDouble("top_posterior",Double.NaN))+" • Margin: "+pct(q.optDouble("posterior_separation_margin",Double.NaN))+" • OOD: "+fmt(q.optDouble("ood_score",Double.NaN))+" • Entropy: "+fmt(q.optDouble("normalized_entropy",Double.NaN)),42,70,9,Color.DKGRAY,false);
        AnalyticalCharts.candidates(c,new RectF(36,88,405,330),q);AnalyticalCharts.candidateResiduals(c,new RectF(430,88,806,330),q);
        AnalyticalCharts.candidateScatter(c,new RectF(36,345,405,548),q);AnalyticalCharts.screeningDonut(c,new RectF(430,345,806,548),q);
    }

    private static void safetyEnvironment(Canvas c,Bundle b){
        title(c,"السلامة والبيئة",42);JSONObject q=b.primaryDecision;JSONObject gas=q==null?null:q.optJSONObject("gas_exposure_ppm");
        if(q!=null)AnalyticalCharts.exposureRatios(c,new RectF(36,74,405,292),q);else emptyBox(c,new RectF(36,74,405,292),"لا توجد بيانات تعرض كمية");
        if(gas==null||!gas.optBoolean("input_gate_passed",false)){emptyBox(c,new RectF(430,74,806,292),"التفسير الكمي للتعرض/الاشتعال محجوب");}
        else{box(c,430,74,376,218,0xFFF4F6F8);text(c,"سلامة الغاز",445,98,12,Color.BLACK,true);text(c,"المادة: "+gas.optString("chemical","—"),445,125,10,Color.BLACK,false);text(c,"الرموز: "+gas.optString("hazard_symbols","—"),445,148,10,Color.BLACK,false);text(c,"أولوية الخطر: "+fmt(gas.optDouble("hazard_priority_percent",Double.NaN))+"%",445,171,10,Color.BLACK,false);text(c,"%LFL: "+fmt(gas.optDouble("percent_lfl",Double.NaN))+"%",445,194,10,Color.BLACK,false);text(c,"Toxicity: "+gas.optString("toxicity_state",gas.optString("state","—"))+" • Flammability: "+gas.optString("flammability_state","—"),445,217,9,Color.DKGRAY,false);text(c,"Compliance determination: "+gas.optString("compliance_determination","NOT_MADE"),445,244,9,Color.DKGRAY,false);}
        if(b.primarySeries!=null&&b.primarySeries.usable()){AnalyticalCharts.temperature(c,new RectF(36,316,282,540),b.primarySeries);AnalyticalCharts.humidity(c,new RectF(297,316,543,540),b.primarySeries);AnalyticalCharts.pressure(c,new RectF(558,316,806,540),b.primarySeries);}else emptyBox(c,new RectF(36,316,806,540),"لا تتوفر سلسلة بيئية مسجلة");
    }

    private static void traceability(Canvas c,Bundle b){
        title(c,"التتبّع والمصدر",42);float y=74;text(c,"كل القياسات التابعة للجلسة",42,y,12,Color.BLACK,true);y+=22;
        text(c,"#",42,y,9,Color.DKGRAY,true);text(c,"Measurement Session ID",72,y,9,Color.DKGRAY,true);text(c,"Derived records",420,y,9,Color.DKGRAY,true);text(c,"ChemCore",525,y,9,Color.DKGRAY,true);text(c,"Primary",700,y,9,Color.DKGRAY,true);y+=16;
        // Exact child measurement IDs are rendered from the already-resolved field-session mapping.
        // Non-primary children remain listed without inventing a merged analytical state.
        for(int i=0;i<b.measurementSessionIds.size()&&y<330;i++){
            String sid=b.measurementSessionIds.get(i);int records=0;for(ExperimentQueries.Record r:b.records)if(sid.equals(r.sessionId))records++;
            text(c,String.valueOf(i+1),42,y,8,Color.BLACK,false);text(c,shorten(sid,44),72,y,8,Color.BLACK,false);text(c,String.valueOf(records),420,y,8,Color.BLACK,false);text(c,sid.equals(b.primaryMeasurementSessionId)&&b.primaryDecision!=null?b.primaryDecision.optString("outcome","RECORDED"):"RECORDED",525,y,8,Color.BLACK,false);text(c,sid.equals(b.primaryMeasurementSessionId)?"YES":"",700,y,8,Color.BLACK,false);y+=18;
        }
        y=Math.max(360,y+16);text(c,"Provenance of primary analytical decision",42,y,12,Color.BLACK,true);y+=22;
        JSONObject q=b.primaryDecision,lib=q==null?null:q.optJSONObject("library");String[][] rows={{"Schema",q==null?"—":q.optString("schema","—")},{"Model",q==null?"—":q.optString("model_version","—")},{"Feature schema",q==null?"—":q.optString("feature_schema","—")},{"Sampling profile",q==null?"—":q.optString("sampling_profile_id","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":lib.optString("sha256","—")}};
        for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=20;}
        text(c,"Site ID: "+b.site.id+" • Field Session ID: "+b.fieldSession.id+" • Primary Measurement Session ID: "+(b.primaryMeasurementSessionId.isEmpty()?"—":b.primaryMeasurementSessionId),42,536,8,Color.DKGRAY,false);
    }

    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);}
    private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,9,Color.DKGRAY,true);text(c,v,190,y,10,Color.BLACK,false);}
    private static void box(Canvas c,float x,float y,float w,float h,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);}
    private static void empty(Canvas c,String message){emptyBox(c,new RectF(42,90,800,520),message);}
    private static void emptyBox(Canvas c,RectF r,String message){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFF3F3F3);p.setStyle(Paint.Style.FILL);c.drawRect(r,p);text(c,message,r.centerX(),r.centerY(),12,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void chrome(Canvas c,int page,int total,Bundle b){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION,MARGIN,23,11,Color.BLACK,true);text(c,b.site.name+" • "+b.fieldSession.name,PAGE_WIDTH/2f,23,11,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,9,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,9,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static JSONObject first(JSONObject a,JSONObject b){return a!=null?a:b;}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.1f%%",v<=1.000001?v*100.0:v);}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String shorten(String s,int n){String v=nz(s);return v.length()<=n?v:v.substring(0,Math.max(1,n-1))+"…";}
    private static String nz(String s){return s==null?"":s;}

    private static void putManifest(ZipOutputStream z,Bundle b)throws Exception{StringBuilder j=new StringBuilder("{\n  \"schema\": \"hazrob-field-report/2\",\n  \"site_id\": \"").append(json(b.site.id)).append("\",\n  \"field_session_id\": \"").append(json(b.fieldSessionId)).append("\",\n  \"measurement_count\": ").append(b.measurementSessionIds.size()).append(",\n  \"derived_record_count\": ").append(b.records.size()).append(",\n  \"measurements\": [");for(int i=0;i<b.records.size();i++){ExperimentQueries.Record r=b.records.get(i);if(i>0)j.append(",");j.append("\n    {\"experiment_id\":").append(r.id).append(",\"session_id\":\"").append(json(r.sessionId)).append("\",\"utc\":\"").append(json(r.ts)).append("\"}");}j.append("\n  ]\n}\n");put(z,"manifest.json",j.toString().getBytes(StandardCharsets.UTF_8));}
    private static void put(ZipOutputStream z,String name,byte[] bytes)throws Exception{ZipEntry e=new ZipEntry(name);z.putNextEntry(e);z.write(bytes);z.closeEntry();}
    private static String safe(String s){return (s==null?"unknown":s).replaceAll("[^A-Za-z0-9._-]","_");}
    private static String json(String s){if(s==null)return"";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private static String required(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("field session required");return v;}
}
