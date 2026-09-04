package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** HazRob 0.9 analytical report facade. */
public final class ReportExporter {
    private ReportExporter(){}
    private static final String FOOTER="HazRob 0.9.0 • تصميم المستشار/ شهاب الصهباني.";

    public static void writePdf(OutputStream out,ExperimentQueries.Record r)throws Exception{writePdf(out,r,null);}
    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writePdf(out,rows.get(0),null);}

    public static void writePdf(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        JSONObject snapshot=json(r.payload),analytics=json(r.analytics);
        SessionSeries.Series series=chemicals==null?null:SessionSeries.nearest(chemicals.database(),r.ts);
        PdfDocument pdf=new PdfDocument();int pageNo=1;

        PdfDocument.Page page=page(pdf,pageNo++);Canvas c=page.getCanvas();summary(c,r,snapshot,analytics,series,chemicals);footer(c);pdf.finishPage(page);

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
            AnalyticalCharts.candidates(c,new RectF(28,42,567,330),analytics);
            AnalyticalCharts.temperature(c,new RectF(28,350,567,555),series);
            AnalyticalCharts.humidity(c,new RectF(28,575,567,790),series);
            footer(c);pdf.finishPage(page);

            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.pressure(c,new RectF(28,42,567,390),series);
            AnalyticalCharts.sound(c,new RectF(28,420,567,790),series);
            footer(c);pdf.finishPage(page);
        }else{
            page=page(pdf,pageNo++);c=page.getCanvas();
            AnalyticalCharts.candidates(c,new RectF(28,42,567,400),analytics);
            text(c,"لا توجد سلسلة زمنية مسجلة ومطابقة لهذه التجربة؛ لم يتم إنشاء منحنيات زمنية.",35,445,11,Color.DKGRAY,false);
            footer(c);pdf.finishPage(page);
        }
        pdf.writeTo(out);pdf.close();
    }

    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r)throws Exception{writeXlsx(out,r,null);}
    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writeXlsx(out,rows.get(0),null);}
    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{if(r==null)throw new IllegalArgumentException("record");XlsxReportWriter.write(out,r,chemicals);}

    private static final class PpmImage{final Bitmap bitmap;final int count;PpmImage(Bitmap b,int n){bitmap=b;count=n;}}
    private static PpmImage renderPpm(SessionSeries.Series series,ChemicalRepository chemicals,JSONObject analytics){
        Bitmap b=Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        int n=AnalyticalCharts.ppm(c,new RectF(10,10,1190,590),series,chemicals.database(),analytics);
        if(n<=0){b.recycle();return null;}return new PpmImage(b,n);
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}

    private static void summary(Canvas c,ExperimentQueries.Record r,JSONObject d,JSONObject a,SessionSeries.Series s,ChemicalRepository chemicals){
        text(c,"HazRob 0.9.0 — Analytical Report",32,44,18,Color.BLACK,true);float y=78;
        JSONObject relay=d.optJSONObject("relay");boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long mqPowerMs=relay==null?0:relay.optLong("power_ms",0);
        String[][] rows={{"Experiment",String.valueOf(r.id)},{"UTC",nz(r.ts)},{"Protocol",nz(r.protocol)},{"Firmware",d.optString("firmware","—")},{"API",String.valueOf(d.optInt("api_version",0))},{"Hardware",d.optString("hardware_profile",HazRobHardwareProfile.ID)},{"MQ Power",(mqOn?"ON":"OFF")+" • GPIO25 • 8 sensors"},{"MQ Power Time",mqOn?mqPowerMs+" ms":"0 ms"},{"Time-series",s!=null&&s.usable()?s.samples.size()+" samples / "+String.format(Locale.US,"%.1f s",s.durationMs()/1000.0):"not available"},{"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count())},{"Candidates",String.valueOf(resultCount(a))}};
        for(String[] row:rows){text(c,row[0],38,y,10,Color.DKGRAY,true);text(c,row[1],170,y,10,Color.BLACK,false);y+=25;}
        y+=12;text(c,"Top analytical candidates",38,y,13,Color.BLACK,true);y+=24;
        JSONArray rs=a.optJSONArray("results");
        if(rs==null||rs.length()==0)text(c,"No evidence-bearing analytical result",42,y,10,Color.DKGRAY,false);
        else{
            List<JSONObject> list=new ArrayList<>();for(int i=0;i<rs.length();i++){JSONObject x=rs.optJSONObject(i);if(x!=null)list.add(x);}list.sort((x,z)->Double.compare(z.optDouble("agreement",0),x.optDouble("agreement",0)));
            for(int i=0;i<Math.min(10,list.size());i++){JSONObject x=list.get(i);String name=x.optString("name_ar",x.optString("name",x.optString("name_en","Chemical #"+x.optInt("chemical_id",-1))));String value=String.format(Locale.US,"%.0f%% agreement • confidence %d%s",x.optDouble("agreement",0),x.optInt("confidence",0),x.optBoolean("quantitative",false)?String.format(Locale.US," • %.2f ppm",x.optDouble("ppm",Double.NaN)):" • fingerprint");text(c,(i+1)+". "+name,42,y,10,Color.BLACK,true);text(c,value,285,y,9,Color.DKGRAY,false);y+=22;}
        }
        y+=12;text(c,"Interpretation",38,y,12,Color.BLACK,true);y+=22;
        text(c,"التحذير اللحظي واستجابة MQ لا يمثلان وحدهما تحديداً كيميائياً قطعياً؛ التحليل النهائي يعتمد على الأدلة المتعددة والمعايرة المتاحة.",38,y,9,Color.DKGRAY,false);
    }

    private static void footer(Canvas c){text(c,FOOTER,297,826,8,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static int resultCount(JSONObject a){JSONArray q=a==null?null:a.optJSONArray("results");return q==null?0:q.length();}
    private static String nz(String s){return s==null?"":s;}
}
