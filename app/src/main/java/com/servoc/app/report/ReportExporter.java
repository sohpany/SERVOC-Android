package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/** HazRob analytical report facade. All reports bind analytical provenance to the exact measurement session. */
public final class ReportExporter {
    private ReportExporter(){}
    private static final String FOOTER="HazRob "+ReleaseAudit.VERSION+" • إعداد المشروع/ "+ReleaseAudit.PROJECT_PREPARERS+".";

    public static void writePdf(OutputStream out,ExperimentQueries.Record r)throws Exception{writePdf(out,r,null);}
    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writePdf(out,rows.get(0),null);}

    public static void writePdf(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        JSONObject snapshot=json(r.payload),analytics=json(r.analytics);
        SessionSeries.Series series=chemicals==null?null:SessionSeries.forExperiment(chemicals.database(),r);
        JSONObject chemcore=chemCoreFor(chemicals,r,series);
        PdfDocument pdf=new PdfDocument();int pageNo=1;

        PdfDocument.Page page=page(pdf,pageNo++);Canvas c=page.getCanvas();summary(c,r,snapshot,analytics,chemcore,series,chemicals);footer(c);pdf.finishPage(page);

        if(resultCount(analytics)>0||analytics.optJSONObject("quality")!=null){page=page(pdf,pageNo++);c=page.getCanvas();advancedAnalysis(c,analytics,snapshot);footer(c);pdf.finishPage(page);}

        if(chemcore!=null){page=page(pdf,pageNo++);c=page.getCanvas();chemCoreProvenance(c,chemcore);footer(c);pdf.finishPage(page);}

        if(series!=null&&series.usable()){
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.mqRsR0(c,new RectF(28,42,567,405),series);AnalyticalCharts.fingerprint(c,new RectF(28,430,567,790),series);footer(c);pdf.finishPage(page);
            PpmImage ppm=chemicals==null?null:renderPpm(series,chemicals,analytics);
            if(ppm!=null&&ppm.count>0){page=page(pdf,pageNo++);c=page.getCanvas();c.drawBitmap(ppm.bitmap,null,new RectF(28,70,567,650),null);text(c,"Quantitative curves are shown only when the calibration engine returned a valid quantitative result across recorded samples.",32,700,9,Color.DKGRAY,false);footer(c);pdf.finishPage(page);ppm.bitmap.recycle();}
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidates(c,new RectF(28,42,567,330),candidatePayload(analytics,chemcore));AnalyticalCharts.temperature(c,new RectF(28,350,567,555),series);AnalyticalCharts.humidity(c,new RectF(28,575,567,790),series);footer(c);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.pressure(c,new RectF(28,42,567,390),series);AnalyticalCharts.sound(c,new RectF(28,420,567,790),series);footer(c);pdf.finishPage(page);
        }else{
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidates(c,new RectF(28,42,567,400),candidatePayload(analytics,chemcore));text(c,"No matching recorded time-series is available for this experiment; temporal charts were not generated.",35,445,10,Color.DKGRAY,false);footer(c);pdf.finishPage(page);
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
        JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),env=d.optJSONObject("env"),measurement=d.optJSONObject("measurement"),sensorPower=d.optJSONObject("sensor_power"),quality=a.optJSONObject("quality");boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long mqPowerMs=relay==null?0:relay.optLong("power_ms",0);
        boolean ads1=adc!=null&&adc.optBoolean("ads1",false),ads2=adc!=null&&adc.optBoolean("ads2",false);String envState=env!=null&&env.optBoolean("ok",false)?env.optString("type",env.optString("sensor","ENV"))+" • "+env.optString("address","—"):"not ready • "+(env==null?"no diagnostics":env.optString("status_code","unknown"));String measureState=measurement==null?"—":measurement.optString("state","—");String measureError=measurement==null?"":measurement.optString("error","");if(!measureError.isEmpty())measureState+=" • "+measureError;
        String liveDiagnostic="—";if(sensorPower!=null){liveDiagnostic=sensorPower.optString("code","—")+" • "+sensorPower.optInt("valid_channels",0)+"/"+sensorPower.optInt("available_channels",0)+" valid"+(sensorPower.optBoolean("power_cause_confirmed",false)?" • power cause confirmed":" • power cause inferred only");}
        String session=s==null?nz(r.sessionId):nz(s.sessionId);
        String[][] rows={{"Experiment",String.valueOf(r.id)},{"UTC",nz(r.ts)},{"Protocol",nz(r.protocol)},{"Session",session},{"Capture",r.captureId>0?String.valueOf(r.captureId):"—"},{"Reference kind",nz(r.referenceKind)},{"Firmware",d.optString("firmware","—")},{"API",String.valueOf(d.optInt("api_version",0))},{"Hardware",d.optString("hardware_profile",HazRobHardwareProfile.ID)},{"ADS1 0x48",ads1?"OK":"NOT DETECTED"},{"ADS2 0x49",ads2?"OK":"NOT DETECTED"},{"Environment",envState},{"Live MQ diagnostic",liveDiagnostic},{"Measurement",measureState},{"MQ Power",(mqOn?"ON":"OFF")+" • GPIO25 • 8 sensors"},{"MQ Power Time",mqOn?mqPowerMs+" ms":"0 ms"},{"Time-series",s!=null&&s.usable()?s.samples.size()+" samples / "+String.format(Locale.US,"%.1f s",s.durationMs()/1000.0):"not available"},{"R0 standard",FieldR0Baseline.SESSION+" • "+FieldR0Baseline.SAMPLE_COUNT+" samples"},{"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count())},{"Analytical candidates",String.valueOf(resultCount(a))},{"Analysis version",a.optString("analysis_version","legacy")},{"Screening outcome",quality==null?"—":quality.optString("outcome","—")},{"Valid MQ channels",quality==null?"—":quality.optInt("valid_channels",0)+"/8"},{"Calibrated R0",quality==null?"—":quality.optInt("calibrated_r0_channels",0)+"/8"},{"Top score",quality==null?"—":finite(quality,"top_score_pct")+" %"},{"Separation margin",quality==null?"—":finite(quality,"separation_margin_pct")+" %"},{"ChemCore outcome",chemcore==null?"not recorded":chemcore.optString("outcome","—")},{"Evidence tier",chemcore==null?"—":chemcore.optString("evidence_tier","—")},{"PPM reportable",chemcore==null?"—":chemcore.optBoolean("ppm_reportable",false)?"YES":"NO"}};
        for(String[] row:rows){if(y>675)break;text(c,row[0],38,y,9,Color.DKGRAY,true);text(c,row[1],180,y,9,Color.BLACK,false);y+=19;}
        y=Math.max(y+8,610);text(c,"Safety interpretation",38,y,12,Color.BLACK,true);y+=20;text(c,"MQ-array output is a screening/e-nose candidate result. Definitive identity and quantitative exposure claims require the documented calibration/evidence gates.",38,y,8,Color.DKGRAY,false);
    }

    private static void advancedAnalysis(Canvas c,JSONObject a,JSONObject d){
        text(c,"Advanced MQ Fingerprint Analysis",32,44,17,Color.BLACK,true);JSONObject q=a.optJSONObject("quality");float y=76;
        if(q!=null){String[][] meta={{"Analysis version",a.optString("analysis_version","—")},{"Outcome",q.optString("outcome","—")},{"Valid channels",q.optInt("valid_channels",0)+"/8"},{"Calibrated R0",q.optInt("calibrated_r0_channels",0)+"/8"},{"Environment",q.optBoolean("environment_available",false)?"available":"not available"},{"Quantitative gate",q.optBoolean("quantitative_permitted",false)?"permitted":"withheld"},{"Top score",finite(q,"top_score_pct")+" %"},{"Runner-up score",finite(q,"runner_up_score_pct")+" %"},{"Separation margin",finite(q,"separation_margin_pct")+" %"}};for(String[] r:meta){text(c,r[0],36,y,9,Color.DKGRAY,true);text(c,r[1],170,y,9,Color.BLACK,false);y+=18;}text(c,q.optString("note",""),36,y+4,8,Color.DKGRAY,false);y+=35;}
        JSONObject env=d.optJSONObject("env");if(env!=null){String ev="ENV "+env.optString("type","NONE")+" • status "+env.optString("status_code","unknown")+" • 0x76 "+(env.optBoolean("ack_0x76",false)?"ACK":"—")+" • 0x77 "+(env.optBoolean("ack_0x77",false)?"ACK":"—");text(c,ev,36,y,8,Color.DKGRAY,false);y+=24;}
        text(c,"Rank",36,y,8,Color.DKGRAY,true);text(c,"Candidate",70,y,8,Color.DKGRAY,true);text(c,"Score",275,y,8,Color.DKGRAY,true);text(c,"Coverage",325,y,8,Color.DKGRAY,true);text(c,"Evidence",385,y,8,Color.DKGRAY,true);text(c,"Agreement",445,y,8,Color.DKGRAY,true);text(c,"n",525,y,8,Color.DKGRAY,true);y+=16;
        JSONArray rs=a.optJSONArray("results");if(rs==null||rs.length()==0){text(c,"No evidence-bearing candidates recorded.",42,y,9,Color.DKGRAY,false);return;}
        for(int i=0;i<rs.length()&&y<760;i++){JSONObject r=rs.optJSONObject(i);if(r==null)continue;String name=r.optString("name_ar",r.optString("name",r.optString("name_en","Chemical #"+r.optInt("chemical_id",-1))));String agreement=r.optBoolean("agreement_defined",false)?String.format(Locale.US,"%.0f%%",r.optDouble("agreement",0)):"N/A";text(c,String.valueOf(r.optInt("rank",i+1)),36,y,8,Color.BLACK,false);text(c,name,70,y,8,Color.BLACK,false);text(c,String.format(Locale.US,"%.1f",r.optDouble("candidate_score_pct",0)),275,y,8,Color.BLACK,false);text(c,String.format(Locale.US,"%.1f",r.optDouble("coverage_pct",0)),325,y,8,Color.BLACK,false);text(c,String.format(Locale.US,"%.1f",r.optDouble("evidence_quality_pct",0)),385,y,8,Color.BLACK,false);text(c,agreement,445,y,8,Color.BLACK,false);text(c,String.valueOf(r.optInt("contributors",0)),525,y,8,Color.BLACK,false);y+=17;}
        text(c,"Agreement is intentionally undefined for a single contributing sensor and is not displayed as 100%.",36,792,8,Color.DKGRAY,false);
    }

    private static void chemCoreProvenance(Canvas c,JSONObject q){
        text(c,"ChemCore — Session-Bound Analytical Provenance",32,44,17,Color.BLACK,true);float y=76;JSONObject lib=q.optJSONObject("library"),lab=q.optJSONObject("lab_quality");
        boolean reportable=q.optBoolean("ppm_reportable",false);String ppm=reportable?finite(q,"ppm")+" ppm":"WITHHELD — "+q.optString("ppm_withheld_reason","scientific_gate_not_passed");
        String labState="—";if(lab!=null){JSONObject env=lab.optJSONObject("environment");labState="MQ "+lab.optInt("valid_mq_channels",0)+"/8 • R0 "+lab.optInt("r0_normalized_channels",0)+"/8 • M4-ref "+lab.optInt("reference_validated_channels",0)+"/8 • ENV "+(env!=null&&env.optBoolean("ready",false)?"READY":"WITHHELD");}
        String[][] rows={{"Schema",q.optString("schema","—")},{"Session",q.optString("session_id","—")},{"R0 standard",FieldR0Baseline.SESSION+" • "+FieldR0Baseline.SAMPLE_COUNT+" samples / "+FieldR0Baseline.SESSION_COUNT+" sessions"},{"Outcome",q.optString("outcome","—")},{"Evidence tier",q.optString("evidence_tier","—")},{"Chemical UID",q.has("chemical_uid")?String.valueOf(q.optLong("chemical_uid")):"—"},{"Chemical",q.optString("chemical","—")},{"PPM decision",ppm},{"Prediction interval",finite(q,"prediction_low")+" .. "+finite(q,"prediction_high")},{"Top posterior",finite(q,"top_posterior")},{"Runner-up posterior",finite(q,"runner_up_posterior")},{"Posterior margin",finite(q,"posterior_separation_margin")},{"Normalized entropy",finite(q,"normalized_entropy")},{"OOD score",finite(q,"ood_score")},{"Sampling profile",q.optString("sampling_profile_id","—")},{"Feature schema",q.optString("feature_schema","—")},{"Model version",q.optString("model_version","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":lib.optString("sha256","—")},{"Lab gate snapshot",labState},{"Quantitative occupational interpretation",q.optBoolean("quantitative_occupational_interpretation_permitted",false)?"PERMITTED by complete M4/identity/domain gate":"NOT PERMITTED"}};
        for(String[] row:rows){if(y>535)break;text(c,row[0],36,y,8,Color.DKGRAY,true);text(c,211,y,8,Color.BLACK,false,row[1]);y+=20;}
        text(c,"Model equations",36,558,11,Color.BLACK,true);text(c,"log10(Rs/R0) = m × log10(C) + c    ;    C = 10^((log10(Rs/R0) − c) / m)",36,578,8,Color.DKGRAY,false);text(c,"Candidate ppm below is a joint screening-fit hypothesis unless the final result is explicitly PPM-reportable.",36,594,8,Color.DKGRAY,false);
        JSONArray candidates=q.optJSONArray("candidates");y=620;text(c,"Candidate posterior ranking",36,y,11,Color.BLACK,true);y+=20;if(candidates==null||candidates.length()==0){text(c,"No ChemCore candidates recorded.",42,y,9,Color.DKGRAY,false);return;}
        for(int i=0;i<Math.min(7,candidates.length())&&y<790;i++){JSONObject x=candidates.optJSONObject(i);if(x==null)continue;String label=x.optString("label","Chemical "+x.optLong("chemical_uid",-1));String agreement=Double.isFinite(x.optDouble("agreement_pct",Double.NaN))?String.format(Locale.US,"%.1f%%",x.optDouble("agreement_pct")):"N/A";String v="post "+finite(x,"posterior")+" • screen-fit "+finite(x,"screening_fit_ppm")+" ppm • resid "+finite(x,"residual_rms")+" • n="+x.optInt("contributors",0)+" • M4="+x.optInt("calibrated_contributors",0)+" • agr "+agreement+" • domain "+(x.optBoolean("all_contributors_inside_domain",false)?"IN":"OUT/UNPROVEN");text(c,(i+1)+". "+label,42,y,8,Color.BLACK,true);text(c,v,190,y,7,Color.DKGRAY,false);y+=20;}
    }

    private static JSONObject chemCoreFor(ChemicalRepository chemicals,ExperimentQueries.Record r,SessionSeries.Series series){if(chemicals==null)return null;String session=series!=null&&!nz(series.sessionId).isEmpty()?series.sessionId:r.sessionId;if(nz(session).isEmpty())return null;return new ChemCoreDecisionRepository(chemicals.database()).latestPayloadForSession(session);}
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