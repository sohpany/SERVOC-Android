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
import com.servoc.app.exposure.GasPpmExposureAssessment;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/** HazRob analytical report facade. All reports bind analytical provenance to the exact measurement session. */
public final class ReportExporter {
    private ReportExporter(){}
    private static final float PDF_MARGIN=28.35f; // 1 cm on A4
    private static final float AR_BODY_PT=13f, EN_BODY_PT=12f;
    private static final String REPORT_TITLE="تقرير القياسات";
    private static final String PROJECT_PREPARERS="شهاب الصهباني، مجدي ذيبان";

    public static void writePdf(OutputStream out,ExperimentQueries.Record r)throws Exception{writePdf(out,r,null);}
    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writePdf(out,rows.get(0),null);}

    public static void writePdf(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        JSONObject snapshot=json(r.payload),analytics=json(r.analytics);
        SessionSeries.Series series=chemicals==null?null:SessionSeries.forExperiment(chemicals.database(),r);
        JSONObject chemcore=chemCoreFor(chemicals,r,series);
        PdfDocument pdf=new PdfDocument();int pageNo=1;

        PdfDocument.Page page=page(pdf,pageNo++);Canvas c=page.getCanvas();summary(c,r,snapshot,analytics,chemcore,series,chemicals);reportChrome(c,pageNo-1,0);pdf.finishPage(page);

        if(resultCount(analytics)>0||analytics.optJSONObject("quality")!=null){page=page(pdf,pageNo++);c=page.getCanvas();advancedAnalysis(c,analytics,snapshot);reportChrome(c,pageNo-1,0);pdf.finishPage(page);}

        if(chemcore!=null){
            page=page(pdf,pageNo++);c=page.getCanvas();chemCoreProvenance(c,chemcore);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidates(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),chemcore);AnalyticalCharts.candidateScatter(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),chemcore);reportChrome(c,pageNo-1,0);pdf.finishPage(page);\n            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidateBubbleMap(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),chemcore);AnalyticalCharts.candidateResiduals(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),chemcore);reportChrome(c,pageNo-1,0);pdf.finishPage(page);\n            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.screening144(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),chemcore);AnalyticalCharts.screeningDonut(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),chemcore);reportChrome(c,pageNo-1,0);pdf.finishPage(page);\n            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.evidenceTiers(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),chemcore);AnalyticalCharts.exposureRatios(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),chemcore);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            JSONObject gas=chemcore.optJSONObject(GasPpmExposureAssessment.KEY);if(gas!=null){page=page(pdf,pageNo++);c=page.getCanvas();gasSafety(c,gas);reportChrome(c,pageNo-1,0);pdf.finishPage(page);}
        }

        if(series!=null&&series.usable()){
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.mqRsR0(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),series);AnalyticalCharts.fingerprint(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.fingerprintRadar(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),series);AnalyticalCharts.analyticalQualityRadar(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.mqSignalQuality(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),series);AnalyticalCharts.mqResponseKinetics(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.mqPersistenceMap(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),series);AnalyticalCharts.mqEnvironmentMatrix(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidates(c,new RectF(PDF_MARGIN,42,595-PDF_MARGIN,330),candidatePayload(analytics,chemcore));AnalyticalCharts.temperature(c,new RectF(PDF_MARGIN,350,595-PDF_MARGIN,555),series);AnalyticalCharts.humidity(c,new RectF(PDF_MARGIN,575,595-PDF_MARGIN,790),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.pressure(c,new RectF(PDF_MARGIN,48,595-PDF_MARGIN,390),series);AnalyticalCharts.sound(c,new RectF(PDF_MARGIN,410,595-PDF_MARGIN,785),series);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
        }else{
            page=page(pdf,pageNo++);c=page.getCanvas();AnalyticalCharts.candidates(c,new RectF(PDF_MARGIN,42,595-PDF_MARGIN,400),candidatePayload(analytics,chemcore));text(c,"No matching recorded time-series is available for this experiment; temporal charts were not generated.",PDF_MARGIN,445,EN_BODY_PT,Color.DKGRAY,false);reportChrome(c,pageNo-1,0);pdf.finishPage(page);
        }
        pdf.writeTo(out);pdf.close();
    }

    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r)throws Exception{writeXlsx(out,r,null);}
    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writeXlsx(out,rows.get(0),null);}
    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{if(r==null)throw new IllegalArgumentException("record");XlsxReportWriter.write(out,r,chemicals);}

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}

    private static void summary(Canvas c,ExperimentQueries.Record r,JSONObject d,JSONObject a,JSONObject chemcore,SessionSeries.Series s,ChemicalRepository chemicals){
        text(c,"HazRob "+ReleaseAudit.VERSION+" — Analytical Report",32,44,18,Color.BLACK,true);float y=78;
        JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),env=d.optJSONObject("env"),measurement=d.optJSONObject("measurement"),sensorPower=d.optJSONObject("sensor_power"),quality=a.optJSONObject("quality");boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long mqPowerMs=relay==null?0:relay.optLong("power_ms",0);
        boolean ads1=adc!=null&&adc.optBoolean("ads1",false),ads2=adc!=null&&adc.optBoolean("ads2",false);String envState=env!=null&&env.optBoolean("ok",false)?env.optString("type",env.optString("sensor","ENV"))+" • "+env.optString("address","—"):"not ready • "+(env==null?"no diagnostics":env.optString("status_code","unknown"));String measureState=measurement==null?"—":measurement.optString("state","—");String measureError=measurement==null?"":measurement.optString("error","");if(!measureError.isEmpty())measureState+=" • "+measureError;
        String liveDiagnostic="—";if(sensorPower!=null){liveDiagnostic=sensorPower.optString("code","—")+" • "+sensorPower.optInt("valid_channels",0)+"/"+sensorPower.optInt("available_channels",0)+" valid"+(sensorPower.optBoolean("power_cause_confirmed",false)?" • power cause confirmed":" • power cause inferred only");}
        String session=s==null?nz(r.sessionId):nz(s.sessionId);
        JSONObject gas=chemcore==null?null:chemcore.optJSONObject(GasPpmExposureAssessment.KEY);String gasSummary="—";if(gas!=null){gasSummary=gas.optBoolean("input_gate_passed",false)?gas.optString("hazard_symbols","")+" priority="+finite(gas,"hazard_priority_percent")+"% • toxic="+finite(gas,"toxicity_priority_percent")+"% • LFL="+finite(gas,"percent_lfl")+"%":"WITHHELD: "+gas.optString("reason","scientific_gate_not_passed");}
        String[][] rows={{"Experiment",String.valueOf(r.id)},{"UTC",nz(r.ts)},{"Protocol",nz(r.protocol)},{"Session",session},{"Capture",r.captureId>0?String.valueOf(r.captureId):"—"},{"Reference kind",nz(r.referenceKind)},{"Firmware",d.optString("firmware","—")},{"API",String.valueOf(d.optInt("api_version",0))},{"Hardware",d.optString("hardware_profile",HazRobHardwareProfile.ID)},{"ADS1 0x48",ads1?"OK":"NOT DETECTED"},{"ADS2 0x49",ads2?"OK":"NOT DETECTED"},{"Environment",envState},{"Live MQ diagnostic",liveDiagnostic},{"Measurement",measureState},{"MQ Power",(mqOn?"ON":"OFF")+" • GPIO25 • 8 sensors"},{"MQ Power Time",mqOn?mqPowerMs+" ms":"0 ms"},{"Time-series",s!=null&&s.usable()?s.samples.size()+" samples / "+String.format(Locale.US,"%.1f s",s.durationMs()/1000.0):"not available"},{"R0 standard",FieldR0Baseline.SESSION+" • "+FieldR0Baseline.SAMPLE_COUNT+" samples"},{"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count())},{"Analytical candidates",String.valueOf(resultCount(a))},{"Analysis version",a.optString("analysis_version","legacy")},{"Screening outcome",quality==null?"—":quality.optString("outcome","—")},{"Valid MQ channels",quality==null?"—":quality.optInt("valid_channels",0)+"/8"},{"Calibrated R0",quality==null?"—":quality.optInt("calibrated_r0_channels",0)+"/8"},{"Top score",quality==null?"—":finite(quality,"top_score_pct")+" %"},{"Separation margin",quality==null?"—":finite(quality,"separation_margin_pct")+" %"},{"ChemCore outcome",chemcore==null?"not recorded":chemcore.optString("outcome","—")},{"Evidence tier",chemcore==null?"—":chemcore.optString("evidence_tier","—")},{"PPM reportable",chemcore==null?"—":chemcore.optBoolean("ppm_reportable",false)?"YES":"NO"},{"Gas relative hazard",gasSummary}};
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

    private static void gasSafety(Canvas c,JSONObject g){
        text(c,"Gas Safety — Relative Hazard Priority",32,44,17,Color.BLACK,true);float y=78;
        boolean gate=g.optBoolean("input_gate_passed",false);String symbols=g.optString("hazard_symbols","");String[][] rows={{"Schema",g.optString("schema","—")},{"Chemical",g.optString("chemical","—")},{"Measured concentration",gate?finite(g,"ppm")+" ppm":"WITHHELD"},{"Hazard symbols",symbols.isEmpty()?"—":symbols},{"Priority ratio",gate?finite(g,"hazard_priority_percent")+" % of controlling reference":"—"},{"Priority basis",g.optString("hazard_priority_basis","—")},{"Toxicity state",g.optString("toxicity_state",g.optString("state","—"))},{"Toxicity ratio",finite(g,"toxicity_priority_percent")+" % of controlling ppm reference"},{"Flammability state",g.optString("flammability_state","—")},{"%LFL",finite(g,"percent_lfl")+" %"},{"LFL",g.has("lfl_vol_pct")&&!g.isNull("lfl_vol_pct")?finite(g,"lfl_vol_pct")+" vol% = "+finite(g,"lfl_ppm")+" ppm":"not available"},{"UFL",g.has("ufl_vol_pct")&&!g.isNull("ufl_vol_pct")?finite(g,"ufl_vol_pct")+" vol% = "+finite(g,"ufl_ppm")+" ppm":"not available"},{"PPM health references",String.valueOf(g.optInt("ppm_reference_count",0))},{"LFL references",String.valueOf(g.optInt("lfl_reference_count",0))},{"Excluded non-PPM toxicology",String.valueOf(g.optInt("excluded_non_ppm_count",0))},{"Compliance determination",g.optString("compliance_determination","NOT_MADE")}};
        for(String[] row:rows){text(c,row[0],36,y,8,Color.DKGRAY,true);text(c,200,y,8,Color.BLACK,false,row[1]);y+=21;}
        y+=8;text(c,"Symbol semantics",36,y,11,Color.BLACK,true);y+=19;text(c,"☠ measured ppm reached/exceeded at least one health reference. For TWA/STEL, short captures are context warnings unless the required averaging duration is covered.",36,y,7,Color.DKGRAY,false);y+=18;text(c,"🔥 measured concentration reached/exceeded 100% of the substance-specific LFL reference.",36,y,7,Color.DKGRAY,false);y+=28;
        text(c,"Health references (ppm only)",36,y,11,Color.BLACK,true);y+=18;JSONArray refs=g.optJSONArray("references");if(refs==null||refs.length()==0){text(c,"No usable ppm health references were bound to this result.",42,y,8,Color.DKGRAY,false);y+=18;}else for(int i=0;i<Math.min(8,refs.length())&&y<700;i++){JSONObject r=refs.optJSONObject(i);if(r==null)continue;String duration=(r.has("duration_min")&&!r.isNull("duration_min"))?" / "+finite(r,"duration_min")+" min":"";String line=r.optString("authority","—")+" "+r.optString("limit_type","—")+" = "+finite(r,"value_ppm")+" ppm • measured/reference "+finite(r,"percent_of_reference")+"%"+duration+(r.optBoolean("reference_crossed",false)?" • CROSSED":"");text(c,line,42,y,7,Color.BLACK,false);y+=17;}
        if(y<735){text(c,"Flammability reference",36,y+7,11,Color.BLACK,true);JSONObject lfl=g.optJSONObject("controlling_lfl_reference");String line=lfl==null?"No authoritative numeric LFL bound to this substance.":lfl.optString("authority","—")+" LFL = "+finite(lfl,"value_vol_pct")+" vol% = "+finite(lfl,"boundary_ppm")+" ppm • measured/LFL "+finite(lfl,"percent_of_boundary")+"%";text(c,line,42,y+27,7,Color.BLACK,false);}
        text(c,"Safety ordering uses relative hazard ratio, not absolute ppm. Identity ranking remains independent.",36,790,8,Color.DKGRAY,true);
    }

    private static JSONObject chemCoreFor(ChemicalRepository chemicals,ExperimentQueries.Record r,SessionSeries.Series series){if(chemicals==null)return null;String session=series!=null&&!nz(series.sessionId).isEmpty()?series.sessionId:r.sessionId;if(nz(session).isEmpty())return null;return new ChemCoreDecisionRepository(chemicals.database()).latestPayloadForSession(session);}
    private static JSONObject candidatePayload(JSONObject legacy,JSONObject chemcore){return chemcore!=null&&chemcore.optJSONArray("candidates")!=null?chemcore:legacy;}
    private static String finite(JSONObject q,String key){double v=q==null?Double.NaN:q.optDouble(key,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%.6g",v):"—";}
    private static void reportChrome(Canvas c,int pageNo,int totalPages){float left=PDF_MARGIN,right=595-PDF_MARGIN;Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);line.setColor(0xFFB0B0B0);line.setStrokeWidth(0.7f);c.drawLine(left,35,right,35,line);c.drawLine(left,808,right,808,line);text(c,"HazRob",right,25,13,Color.BLACK,true,Paint.Align.RIGHT);text(c,REPORT_TITLE,297.5f,25,13,Color.BLACK,true,Paint.Align.CENTER);drawLogoMark(c,left,11);String pages="صفحة "+pageNo+" من "+(totalPages>0?totalPages:"—");text(c,pages,left,829,12,Color.DKGRAY,false,Paint.Align.LEFT);text(c,PROJECT_PREPARERS,right,829,13,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void drawLogoMark(Canvas c,float x,float y){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(Color.DKGRAY);float cx=x+10,cy=y+10,r=9;android.graphics.Path h=new android.graphics.Path();for(int i=0;i<6;i++){double a=Math.PI/3*i-Math.PI/6;float px=cx+(float)(r*Math.cos(a)),py=cy+(float)(r*Math.sin(a));if(i==0)h.moveTo(px,py);else h.lineTo(px,py);}h.close();c.drawPath(h,p);text(c,"H",cx,cy+4,10,Color.DKGRAY,true,Paint.Align.CENTER);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,float x,float y,float size,int color,boolean bold,String s){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static int resultCount(JSONObject a){JSONArray q=a==null?null:a.optJSONArray("results");return q==null?0:q.length();}
    private static String nz(String s){return s==null?"":s;}
}
