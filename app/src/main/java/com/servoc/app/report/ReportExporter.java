package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.data.ExperimentQueries;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** No external libraries: minimal valid XLSX plus Android-native PDF export. */
public final class ReportExporter {
    private ReportExporter(){}

    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        ZipOutputStream z=new ZipOutputStream(out);
        entry(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        entry(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"HazRob Experiments\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        entry(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        String[] h={"ID","Timestamp UTC","Protocol","Raw / Telemetry JSON","Chemical Analytics JSON"};
        s.append("<row r=\"1\">");for(int i=0;i<h.length;i++)cell(s,col(i)+"1",h[i]);s.append("</row>");
        int r=2;for(ExperimentQueries.Record x:rows){s.append("<row r=\"").append(r).append("\">");cell(s,"A"+r,String.valueOf(x.id));cell(s,"B"+r,n(x.ts));cell(s,"C"+r,n(x.protocol));cell(s,"D"+r,n(x.payload));cell(s,"E"+r,n(x.analytics));s.append("</row>");r++;}
        s.append("</sheetData></worksheet>");entry(z,"xl/worksheets/sheet1.xml",s.toString());z.finish();z.flush();
    }

    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        PdfDocument pdf=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(10f);
        int pageNo=1;PdfDocument.Page page=null;Canvas c=null;float y=0;
        for(ExperimentQueries.Record x:rows){
            String block="#"+x.id+"  "+n(x.ts)+"\nProtocol: "+n(x.protocol)+"\nTelemetry: "+compact(x.payload,240)+"\nAnalytics: "+compact(x.analytics,240);
            String[] lines=block.split("\\n");
            if(page==null||y+lines.length*16+20>800){if(page!=null)pdf.finishPage(page);PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,pageNo++).create();page=pdf.startPage(info);c=page.getCanvas();y=32;p.setTextSize(15);c.drawText("HazRob Experiment Report",28,y,p);y+=28;p.setTextSize(10);}
            for(String line:lines){drawWrapped(c,p,line,28,y,535);y+=lineHeight(line);}
            y+=14;
        }
        if(page==null){PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,1).create();page=pdf.startPage(info);c=page.getCanvas();p.setTextSize(15);c.drawText("HazRob Experiment Report",28,32,p);p.setTextSize(11);c.drawText("No accepted experiments.",28,64,p);}
        pdf.finishPage(page);pdf.writeTo(out);pdf.close();
    }

    private static void drawWrapped(Canvas c,Paint p,String text,float x,float y,float width){
        if(text==null)return;String remaining=text;float yy=y;int guard=0;
        while(!remaining.isEmpty()&&guard++<20){int cut=remaining.length();while(cut>1&&p.measureText(remaining.substring(0,cut))>width)cut--;if(cut<remaining.length()){int sp=remaining.lastIndexOf(' ',cut);if(sp>10)cut=sp;}String part=remaining.substring(0,cut);c.drawText(part,x,yy,p);yy+=13;remaining=remaining.substring(cut).trim();}
    }
    private static float lineHeight(String s){return 13f*Math.max(1,(s==null?0:s.length())/90+1);}
    private static String compact(String s,int max){if(s==null)return"";s=s.replace('\n',' ').replace('\r',' ');return s.length()>max?s.substring(0,max)+"…":s;}
    private static void entry(ZipOutputStream z,String name,String text)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    private static void cell(StringBuilder s,String ref,String v){s.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(v)).append("</t></is></c>");}
    private static String col(int i){return String.valueOf((char)('A'+i));}
    private static String n(String s){return s==null?"":s;}
    private static String xml(String s){if(s==null)return"";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
