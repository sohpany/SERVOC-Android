package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** HazRob analytical report facade. All new reports bind charts and ChemCore provenance to the exact measurement session. */
public final class ReportExporter {
    private ReportExporter(){}
    private static final String FOOTER="HazRob "+ReleaseAudit.VERSION+" • تصميم المستشار/ شهاب الصهباني.";

    public static void writePdf(OutputStream out,ExperimentQueries.Record r)throws Exception{writePdf(out,r,null);}
    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writePdf(out,rows.get(0),null);}

    public static void writePdf(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        JSONObject snapshot=json(r.payload),analytics=json(r.analytics);
        SessionSeries.Series series=chemicals==null?null:SessionSeries.forExperiment(chemicals.database(),r);
        JSONObject chemcore=chemCoreFor(chemicals,r,series);
        PdfDocument pdf=new PdfDocument();int pageNo=1;

        PdfDocument.Page page=page(pdf,pageNo++);Canvas c=page.getCanvas();summary(c,r,snapshot,analytics,chemcore,series,chemicals);footer(c);pdf.finishPage(page);

        if(chemcore!=null){
            page=page(pdf,pageNo++);c=page.getCanvas();chemCoreProvenance(c,chemcore);footer(c);pdf.finishPage(page);
        }

        if(series!=null&&series.usable()){
            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.mqRsR0(c,new RectF(28,42,567,405),series);
            AnalyticalCharts.fingerprint(c,new RectF(28,430,567,790),series);
            footer(c);pdf.finishPage(page);

            PpmImage ppm=chemicals==null?null:renderPpm(series,chemicals,analytics);
            if(ppm!=null&&ppm.count>0){
                page=page(pdf,pageNo++);c=page.getCanvas();
                c.drawBitmap(ppm.bitmap,null,new RectF(28,70,567,650),null);
                text(c,"يظهر هذا الرسم فقط للمركبات التي أعاد محرك المعايرة لها قياساً كمياً صالحاً عبر أكثر من عينة.",32,700,10,Color.DKGRAY,false);
                footer(c);pdf.finishPage(page);ppm.bitmap.recycle();
            }

            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.candidates(c,new RectF(28,42,567,330),candidatePayload(analytics,chemcore));
            AnalyticalCharts.temperature(c,new RectF(28,350,567,555),series);
            AnalyticalCharts.humidity(c,new RectF(28,575,567,790),series);
            footer(c);pdf.finishPage(page);

            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.pressure(c,new RectF(28,42,567,390),series);
            AnalyticalCharts.sound(c,new RectF(28,420,567,790),series);
            footer(c);pdf.finishPage(page);
        }else{
            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.candidates(c,new RectF(28,42,567,400),candidatePayload(analytics,chemcore));
            text(c,"لا توجد سلسلة زمنية مسجلة ومطابقة لهذه التجربة؛ لم يتم إنشاء منحنيات زمنية.",35,445,11,Color.DKGRAY,false);
            footer(c);pdf.finishPage(page);
        }
        pdf.writeTo(out);pdf.close();
    }

    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r)throws Exception{writeXlsx(out,r,null);}
    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writeXlsx(out,rows.get(0),null);}
    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{if(r==null)throw new IllegalArgumentException("record");XlsxReportWriter.write(out,r,chemicals);}

    private static final class PpmImage{final Bitmap bitmap;final int count;PpmImage(Bitmap b,int n){bitmap=b;count=n;}}
    private static PpmImage renderPpm(SessionSeries.Series series,ChemicalRepository chemicals,JSONObject analytics){Bitmap b=Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);int n=AnalyticalCharts.ppm(c,new RectF(10,10,1190,590),series,chemicals.database(),analytics);if(n<=0){b.recycle();return null;}return new PpmImage(b,n);}
    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}

    private static void summary(Canvas c,ExperimentQueries.Record r,JSONObject d,JSONObject a,JSONObject chemcore,SessionSeries.Series s,ChemicalRepository chemicals){
        text(c,"HazRob "+ReleaseAudit.VERSION+" — Analytical Report",32,44,18,Color.BLACK,true);float y=78;
        JSONObject relay=d.optJSONObject("relay");boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long mqPowerMs=relay==null?0:relay.optLong("power_ms",0);
        String session=s==null?nz(r.sessionId):nz(s.sessionId);
        String[][] rows={{"Experiment",String.valueOf(r.id)},{"UTC",nz(r.ts)},{"Protocol",nz(r.protocol)},{"Session",session},{"Capture",r.captureId>0?String.valueOf(r.captureId):"—"},{"Reference kind",nz(r.referenceKind)},{"Firmware",d.optString("firmware","—")},{"API",String.valueOf(d.optInt("api_version",0))},{"Hardware",d.optString("hardware_profile",HazRobHardwareProfile.ID)},{"MQ Power",(mqOn?"ON":"OFF")+" • GPIO25 • 8 sensors"},{"MQ Power Time",mqOn?mqPowerMs+" ms":"0 ms"},{"Time-series",s!=null&&s.usable()?s.samples.size()+" samples / "+String.format(Locale.US,"%.1f s",s.durationMs()/1000.0):"not available"},{"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count())},{"Legacy candidates",String.valueOf(resultCount(a))},{"ChemCore outcome",chemcore==null?"not recorded":chemcore.optString("outcome","—")},{"Evidence tier",chemcore==null?"—":chemcore.optString("evidence_tier","—")}};
        for(String[] row:rows){text(c,row[0],38,y,10,Color.DKGRAY,true);text(c,row[1],170,y,10,Color.BLACK,false);y+=22;}
        y+=8;text(c,"Analytical interpretation",38,y,13,Color.BLACK,true);y+=22;
        if(chemcore!=null){
            String top=chemcore.optString("chemical","");double p=chemcore.optDouble("top_posterior",Double.NaN),ood=chemcore.optDouble("ood_score",Double.NaN);
            String value=(top.isEmpty()?"No accepted identity":top)+(Double.isFinite(p)?String.format(Locale.US," • posterior %.3f",p):"")+(Double.isFinite(ood)?String.format(Locale.US," • OOD %.3f",ood):"");
            text(c,value,42,y,9,Color.BLACK,false);y+=20;
            text(c,"ChemCore provenance is printed on the following page and is bound to this exact session ID.",42,y,9,Color.DKGRAY,false);y+=20;
        }else{
            text(c,"No session-bound ChemCore decision exists for this experiment; legacy analytics are shown without promotion to ChemCore evidence.",42,y,9,Color.DKGRAY,false);y+=20;
        }
        y+=8;text(c,"Safety interpretation",38,y,12,Color.BLACK,true);y+=20;text(c,"التحذير اللحظي واستجابة MQ لا يمثلان وحدهما تحديداً كيميائياً قطعياً؛ التحليل النهائي يعتمد على الأدلة المتعددة والمعايرة المتاحة.",38,y,9,Color.DKGRAY,false);
    }

    private static void chemCoreProvenance(Canvas c,JSONObject q){
        text(c,"ChemCore — Session-Bound Analytical Provenance",32,44,17,Color.BLACK,true);float y=80;
        JSONObject lib=q.optJSONObject("library");
        String[][] rows={{"Schema",q.optString("schema","—")},{"Session",q.optString("session_id","—")},{"Outcome",q.optString("outcome","—")},{"Evidence tier",q.optString("evidence_tier","—")},{"Chemical UID",q.has("chemical_uid")?String.valueOf(q.optLong("chemical_uid")):"—"},{"Chemical",q.optString("chemical","—")},{"PPM",finite(q,"ppm")},{"Prediction interval",finite(q,"prediction_low")+" .. "+finite(q,"prediction_high")},{"Top posterior",finite(q,"top_posterior")},{"Normalized entropy",finite(q,"normalized_entropy")},{"OOD score",finite(q,"ood_score")},{"Sampling profile",q.optString("sampling_profile_id","—")},{"Feature schema",q.optString("feature_schema","—")},{"Model version",q.optString("model_version","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":lib.optString("sha256","—")},{"Library source commit",lib==null?"—":lib.optString("source_commit","—")},{"Occupational quantitative interpretation",q.optBoolean("quantitative_occupational_interpretation_permitted",false)?"PERMITTED by evidence gate":"NOT PERMITTED"},{"Note",q.optString("note","")}};
        for(String[] row:rows){text(c,row[0],36,y,9,Color.DKGRAY,true);text(c,221,y,9,Color.BLACK,false,row[1]);y+=22;}
        JSONArray candidates=q.optJSONArray("candidates");y+=6;text(c,"Candidate posterior ranking",36,y,12,Color.BLACK,true);y+=22;
        if(candidates==null||candidates.length()==0){text(c,"No ChemCore candidates recorded.",42,y,9,Color.DKGRAY,false);return;}
        for(int i=0;i<Math.min(8,candidates.length())&&y<790;i++){
            JSONObject x=candidates.optJSONObject(i);if(x==null)continue;
            String label=x.optString("label","Chemical "+x.optLong("chemical_uid",-1));
            String v="posterior "+finite(x,"posterior")+" • best ppm "+finite(x,"best_ppm")+" • residual "+finite(x,"residual_rms")+" • n="+x.optInt("contributors",0)+" • "+x.optString("evidence_tier","—");
            text(c,(i+1)+". "+label,42,y,9,Color.BLACK,true);text(c,v,255,y,8,Color.DKGRAY,false);y+=20;
        }
    }

    private static JSONObject chemCoreFor(ChemicalRepository chemicals,ExperimentQueries.Record r,SessionSeries.Series series){
        if(chemicals==null)return null;String session=series!=null&&!nz(series.sessionId).isEmpty()?series.sessionId:r.sessionId;if(nz(session).isEmpty())return null;return new ChemCoreDecisionRepository(chemicals.database()).latestPayloadForSession(session);
    }
    private static JSONObject candidatePayload(JSONObject legacy,JSONObject chemcore){return chemcore!=null&&chemcore.optJSONArray("candidates")!=null?chemcore:legacy;}
    private static String finite(JSONObject q,String key){double v=q==null?Double.NaN:q.optDouble(key,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%.6g",v):"—";}
    private static void footer(Canvas c){text(c,FOOTER,297,826,8,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,float x,float y,float size,int color,boolean bold,String s){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static int resultCount(JSONObject a){JSONArray q=a==null?null:a.optJSONArray("results");return q==null?0:q.length();}
    private static String nz(String s){return s==null?"":s;}
}
