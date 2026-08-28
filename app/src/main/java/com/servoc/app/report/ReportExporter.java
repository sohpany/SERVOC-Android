package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Native structured XLSX/PDF exporter. No CSV masquerading as Excel and no external report library. */
public final class ReportExporter {
    private ReportExporter(){}

    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        ZipOutputStream z=new ZipOutputStream(out);
        entry(z,"[Content_Types].xml",contentTypes());
        entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        entry(z,"xl/workbook.xml",workbook());
        entry(z,"xl/_rels/workbook.xml.rels",workbookRels());
        entry(z,"xl/worksheets/sheet1.xml",summarySheet(rows));
        entry(z,"xl/worksheets/sheet2.xml",mqSheet(rows));
        entry(z,"xl/worksheets/sheet3.xml",chemicalSheet(rows));
        z.finish();z.flush();
    }

    private static String contentTypes(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"+
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"+
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"+
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"+
                "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"+
                "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>";
    }

    private static String workbook(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+
                "<sheet name=\"Experiments\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"MQ Readings\" sheetId=\"2\" r:id=\"rId2\"/><sheet name=\"Chemical Results\" sheetId=\"3\" r:id=\"rId3\"/></sheets></workbook>";
    }

    private static String workbookRels(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"+
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"+
                "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/></Relationships>";
    }

    private static String summarySheet(List<ExperimentQueries.Record> rows){
        StringBuilder s=sheetStart();
        String[] h={"Experiment ID","Timestamp UTC","Protocol","Measurement state","Temperature °C","Humidity %RH","Pressure hPa","Altitude derived m","Reported MQ channels","Safety interpretation"};
        row(s,1,h);int r=2;
        for(ExperimentQueries.Record x:rows){
            JSONObject d=json(x.payload);JSONObject m=d.optJSONObject("measurement");JSONObject b=d.optJSONObject("bme");
            double p=b==null?Double.NaN:b.optDouble("pressure",Double.NaN);
            double alt=Double.isFinite(p)&&p>0?44330.0*(1.0-Math.pow(p/1013.25,0.1903)):Double.NaN;
            String[] v={String.valueOf(x.id),n(x.ts),n(x.protocol),m==null?"":m.optString("state",""),
                    num(b,"temperature"),num(b,"humidity"),num(b,"pressure"),fmt(alt),String.valueOf(RobotTelemetry.availableMqCount(d)),"DISABLED until device calibration is validated"};
            row(s,r++,v);
        }
        return sheetEnd(s);
    }

    private static String mqSheet(List<ExperimentQueries.Record> rows){
        StringBuilder s=sheetStart();
        row(s,1,new String[]{"Experiment ID","Timestamp UTC","Sensor","Status","Raw","ADC V","AO V","Rs","Rs/R0","Completed average"});int r=2;
        for(ExperimentQueries.Record x:rows){
            JSONObject d=json(x.payload);JSONObject m=d.optJSONObject("measurement");boolean averaged=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;
            JSONObject src=averaged?m.optJSONObject("mq"):d.optJSONObject("mq");if(src==null)continue;
            for(String key:RobotTelemetry.MQ_KEYS){
                JSONObject q=src.optJSONObject(key);if(q==null)continue;
                String status=q.has("status")?q.optString("status",""):(q.has("valid")?(q.optBoolean("valid")?"valid":"invalid"):"");
                row(s,r++,new String[]{String.valueOf(x.id),n(x.ts),key.toUpperCase(Locale.US),status,num(q,"raw"),num(q,"adc"),num(q,"ao"),num(q,"rs"),num(q,"ratio"),averaged?"yes":"no"});
            }
        }
        return sheetEnd(s);
    }

    private static String chemicalSheet(List<ExperimentQueries.Record> rows){
        StringBuilder s=sheetStart();
        row(s,1,new String[]{"Experiment ID","Timestamp UTC","Chemical ID","Name","Formula","PPM model estimate","Agreement %","Confidence","Risk","Quantitative","Contributors","Primary","Manufacturer cross","Community cross","Model note"});int r=2;
        for(ExperimentQueries.Record x:rows){
            JSONObject a=json(x.analytics);JSONArray results=a.optJSONArray("results");if(results==null)continue;
            for(int i=0;i<results.length();i++){
                JSONObject q=results.optJSONObject(i);if(q==null)continue;
                int confidence=q.optInt("confidence",0),risk=q.optInt("risk",-1);
                row(s,r++,new String[]{String.valueOf(x.id),n(x.ts),String.valueOf(q.optInt("chemical_id",-1)+1),q.optString("name",""),q.optString("formula",""),num(q,"ppm"),num(q,"agreement"),stars(confidence),riskName(risk),String.valueOf(q.optBoolean("quantitative",false)),String.valueOf(q.optInt("contributors",0)),String.valueOf(q.optInt("primary",0)),String.valueOf(q.optInt("manufacturer_cross",0)),String.valueOf(q.optInt("community_cross",0)),q.optString("note","")});
            }
        }
        return sheetEnd(s);
    }

    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        PdfDocument pdf=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(9.5f);
        PdfWriter w=new PdfWriter(pdf,p);
        w.heading("HazRob Experiment Report",16);
        w.line("Accepted experiments: "+rows.size());
        w.line("Safety note: quantitative hazard interpretation remains disabled until device-specific calibration is validated.");w.space(8);
        for(ExperimentQueries.Record x:rows){
            JSONObject d=json(x.payload);JSONObject b=d.optJSONObject("bme");JSONObject m=d.optJSONObject("measurement");
            w.heading("Experiment #"+x.id+"  "+n(x.ts),12);
            w.line("Protocol: "+n(x.protocol));
            if(m!=null)w.line("Measurement: "+m.optString("state","")+" | samples "+m.optInt("samples1",0)+"/"+m.optInt("samples2",0)+"/"+m.optInt("samples3",0));
            if(b!=null&&b.optBoolean("ok",false))w.line("Environment: "+num(b,"temperature")+" °C | "+num(b,"humidity")+" %RH | "+num(b,"pressure")+" hPa");

            JSONObject src=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null?m.optJSONObject("mq"):d.optJSONObject("mq");
            if(src!=null){w.line("MQ readings:");for(String key:RobotTelemetry.MQ_KEYS){JSONObject q=src.optJSONObject(key);if(q==null)continue;w.line("  "+key.toUpperCase(Locale.US)+"  raw="+num(q,"raw")+"  Rs="+num(q,"rs")+"  Rs/R0="+num(q,"ratio"));}}

            JSONObject a=json(x.analytics);JSONArray results=a.optJSONArray("results");
            if(results!=null&&results.length()>0){w.line("Chemical model results:");for(int i=0;i<results.length();i++){JSONObject q=results.optJSONObject(i);if(q==null)continue;w.line("  "+q.optString("name","")+"  "+num(q,"ppm")+" ppm  "+stars(q.optInt("confidence",0))+"  "+riskName(q.optInt("risk",-1)));}}
            w.space(12);
        }
        w.finish();pdf.writeTo(out);pdf.close();
    }

    private static final class PdfWriter {
        final PdfDocument pdf;final Paint p;PdfDocument.Page page;Canvas canvas;int pageNo=0;float y=0;
        PdfWriter(PdfDocument pdf,Paint p){this.pdf=pdf;this.p=p;newPage();}
        void newPage(){if(page!=null)pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=30;}
        void ensure(float need){if(y+need>815)newPage();}
        void heading(String s,float size){ensure(28);float old=p.getTextSize();p.setTextSize(size);draw(s);p.setTextSize(old);y+=5;}
        void line(String s){draw(s);}
        void space(float n){y+=n;}
        void draw(String text){if(text==null)text="";String remaining=text;int guard=0;do{ensure(14);int cut=remaining.length();while(cut>1&&p.measureText(remaining.substring(0,cut))>535)cut--;if(cut<remaining.length()){int sp=remaining.lastIndexOf(' ',cut);if(sp>8)cut=sp;}String part=remaining.substring(0,cut);canvas.drawText(part,28,y,p);y+=13;remaining=remaining.substring(cut).trim();}while(!remaining.isEmpty()&&guard++<60);}
        void finish(){if(page!=null){pdf.finishPage(page);page=null;}}
    }

    private static StringBuilder sheetStart(){return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");}
    private static String sheetEnd(StringBuilder s){return s.append("</sheetData></worksheet>").toString();}
    private static void row(StringBuilder s,int row,String[] values){s.append("<row r=\"").append(row).append("\">");for(int i=0;i<values.length;i++)cell(s,col(i)+row,values[i]);s.append("</row>");}
    private static void cell(StringBuilder s,String ref,String v){s.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(v)).append("</t></is></c>");}
    private static String col(int i){StringBuilder s=new StringBuilder();int n=i+1;while(n>0){int r=(n-1)%26;s.insert(0,(char)('A'+r));n=(n-1)/26;}return s.toString();}
    private static void entry(ZipOutputStream z,String name,String text)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String num(JSONObject o,String key){if(o==null)return"";double v=o.optDouble(key,Double.NaN);return fmt(v);}
    private static String fmt(double v){if(!Double.isFinite(v))return"";return String.format(Locale.US,Math.abs(v)>=1000?"%.0f":Math.abs(v)>=10?"%.2f":"%.4f",v);}
    private static String stars(int c){return c<=0?"—":c==1?"★☆☆":c==2?"★★☆":"★★★";}
    private static String riskName(int r){switch(r){case 1:return"VERY SAFE";case 2:return"SAFE";case 3:return"ALERT";case 4:return"DANGER";case 5:return"CRITICAL";default:return"N/A / PROVISIONAL";}}
    private static String n(String s){return s==null?"":s;}
    private static String xml(String s){if(s==null)return"";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
