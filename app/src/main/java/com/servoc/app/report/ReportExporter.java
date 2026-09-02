package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurements.PhysicalCatalog;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** HazRob 0.8 one-experiment PDF/XLSX exporter aligned with the installed two-group MQ hardware. */
public final class ReportExporter {
    private ReportExporter(){}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r)throws Exception{writeXlsx(out,r,null);}
    public static void writeXlsx(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        try(ZipOutputStream z=new ZipOutputStream(out)){
            entry(z,"[Content_Types].xml",contentTypes());
            entry(z,"_rels/.rels",rootRels());
            entry(z,"xl/workbook.xml",workbook());
            entry(z,"xl/_rels/workbook.xml.rels",workbookRels());
            entry(z,"xl/styles.xml",styles());
            entry(z,"xl/worksheets/sheet1.xml",summarySheet(r,chemicals));
            entry(z,"xl/worksheets/sheet2.xml",physicalSheet(r));
            entry(z,"xl/worksheets/sheet3.xml",hardwareSheet(r));
            entry(z,"xl/worksheets/sheet4.xml",mqSheet(r));
            entry(z,"xl/worksheets/sheet5.xml",chemicalSheet(r,chemicals));
            z.finish();
        }
    }
    public static void writeXlsx(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writeXlsx(out,rows.get(0),null);
    }

    public static void writePdf(OutputStream out,ExperimentQueries.Record r)throws Exception{writePdf(out,r,null);}
    public static void writePdf(OutputStream out,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");
        PdfDocument pdf=new PdfDocument();
        JSONObject d=json(r.payload),a=json(r.analytics);
        PdfReport p=new PdfReport(pdf,chemicals);
        p.summary(r,d,a);p.physical(d);p.hardware(d);p.mq(d);p.chemicals(a);p.finish();
        pdf.writeTo(out);pdf.close();
    }
    public static void writePdf(OutputStream out,List<ExperimentQueries.Record> rows)throws Exception{
        if(rows==null||rows.isEmpty())throw new IllegalArgumentException("no experiment");writePdf(out,rows.get(0),null);
    }

    // -------------------------------------------------------------------------
    // XLSX package
    // -------------------------------------------------------------------------

    private static String contentTypes(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"+
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"+
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"+
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"+
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"+
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"+
                sheetType(1)+sheetType(2)+sheetType(3)+sheetType(4)+sheetType(5)+"</Types>";
    }
    private static String sheetType(int i){return "<Override PartName=\"/xl/worksheets/sheet"+i+".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>";}
    private static String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";}
    private static String workbook(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+
            "<sheet name=\"ملخص الفحص\" sheetId=\"1\" r:id=\"rId1\"/>"+
            "<sheet name=\"القياسات الفيزيائية\" sheetId=\"2\" r:id=\"rId2\"/>"+
            "<sheet name=\"العتاد والحساسات\" sheetId=\"3\" r:id=\"rId3\"/>"+
            "<sheet name=\"قراءات MQ\" sheetId=\"4\" r:id=\"rId4\"/>"+
            "<sheet name=\"المركبات والنتائج\" sheetId=\"5\" r:id=\"rId5\"/>"+
            "</sheets></workbook>";}
    private static String workbookRels(){
        StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for(int i=1;i<=5;i++)s.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        s.append("<Relationship Id=\"rId6\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");return s.toString();
    }
    private static String styles(){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"+
                "<fonts count=\"2\"><font><sz val=\"10\"/><name val=\"Arial\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/><name val=\"Arial\"/></font></fonts>"+
                "<fills count=\"8\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"+
                fill("FF263238")+fill("FF90CAF9")+fill("FF43A047")+fill("FFF9A825")+fill("FFEF6C00")+fill("FFC62828")+"</fills>"+
                "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"+
                "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"+
                "<cellXfs count=\"7\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"+
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"+
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"+
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"+
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"+
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"6\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"+
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"7\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/></cellXfs></styleSheet>";
    }
    private static String fill(String rgb){return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\""+rgb+"\"/></patternFill></fill>";}

    // -------------------------------------------------------------------------
    // XLSX sheets
    // -------------------------------------------------------------------------

    private static String summarySheet(ExperimentQueries.Record r,ChemicalRepository chemicals){
        JSONObject d=json(r.payload),m=d.optJSONObject("measurement"),e=env(d),a=json(r.analytics),relay=d.optJSONObject("relay");
        StringBuilder s=sheetStart();int x=1;
        row(s,x++,new String[]{"HazRob 0.8 - تقرير فحص","القيمة"},1);
        row(s,x++,new String[]{"رقم التجربة",String.valueOf(r.id)},0);
        row(s,x++,new String[]{"تاريخ الفحص UTC",n(r.ts)},0);
        row(s,x++,new String[]{"البروتوكول",n(r.protocol)},0);
        row(s,x++,new String[]{"الحالة المرجعية",r.approved?"معتمدة":"غير معتمدة"},0);
        row(s,x++,new String[]{"Firmware",d.optString("firmware","إصدار غير معرّف")},0);
        row(s,x++,new String[]{"API",String.valueOf(d.optInt("api_version",0))},0);
        row(s,x++,new String[]{"Hardware profile",d.optString("hardware_profile",HazRobHardwareProfile.ID)},0);
        row(s,x++,new String[]{"بنية MQ","R1/ADS1: MQ2 MQ4 MQ5 MQ8 • R2/ADS2: MQ3 MQ7 MQ9 MQ135 • R3/R4 احتياط"},0);
        row(s,x++,new String[]{"طاقة MQ",HazRobHardwareProfile.groupLabel(relay==null?0:relay.optInt("mask",0)&3)+" • تحكم يدوي"},0);
        row(s,x++,new String[]{"حالة الالتقاط",m==null?"manual/live":m.optString("state","manual/live")},0);
        row(s,x++,new String[]{"نوع الحساس البيئي",e==null?"غير مكتشف":e.optString("type",e.optString("sensor","ENV"))},0);
        if(chemicals!=null)row(s,x++,new String[]{"مواد قاعدة المعرفة",String.valueOf(chemicals.count())},0);
        row(s,x++,new String[]{"نتائج التحليل ذات الدليل",String.valueOf(resultCount(a))},0);
        row(s,x++,new String[]{"حدود التفسير","R0 اليدوي يطبع Rs/R0؛ PPM والخطر الكمي لا يعتمدان إلا عند وجود معايرة مرجعية ونموذج صالح. MQ7/MQ9 يتطلبان heater-cycle موثقًا للاستخدام الكمي."},0);
        return sheetEnd(s);
    }

    private static String physicalSheet(ExperimentQueries.Record r){
        JSONObject d=json(r.payload),e=env(d);StringBuilder s=sheetStart();
        row(s,1,new String[]{"#","القياس","القيمة","الوحدة","المصدر / الحالة"},1);
        for(int i=0;i<PhysicalCatalog.COUNT;i++){
            String value="",source="غير مركب";
            if(e!=null&&e.optBoolean("ok",false)){
                String type=e.optString("type",e.optString("sensor","ENV"));
                if(i==0){value=num(e,"temperature");source=type;}
                else if(i==1){value=num(e,"humidity");source=value.isEmpty()?"غير متاح في "+type:type;}
                else if(i==2){value=num(e,"pressure");source=type;}
                else if(i==14){double p=e.optDouble("pressure",Double.NaN);if(Double.isFinite(p)&&p>0){value=fmt(44330.0*(1.0-Math.pow(p/1013.25,0.1903)));source="مشتق من الضغط @ 1013.25 hPa";}}
            }
            row(s,i+2,new String[]{String.valueOf(i+1),PhysicalCatalog.NAMES_AR[i],value,PhysicalCatalog.UNITS[i],source},0);
        }
        return sheetEnd(s);
    }

    private static String hardwareSheet(ExperimentQueries.Record r){
        JSONObject d=json(r.payload),sound=d.optJSONObject("sound"),ir=d.optJSONObject("ir"),ldr=d.optJSONObject("ldr"),adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),drive=d.optJSONObject("drive");
        StringBuilder s=sheetStart();int x=1;row(s,x++,new String[]{"العنصر","القيمة","الحالة / التوصيل","ملاحظة"},1);
        row(s,x++,new String[]{"Hardware profile",d.optString("hardware_profile",HazRobHardwareProfile.ID),"HazRob 0.8","مصدر الحقيقة للتقرير"},0);
        row(s,x++,new String[]{"ADS1115 #1",yes(adc!=null&&adc.optBoolean("ads1",false)),"0x48 • Relay R1","A0 MQ2 • A1 MQ4 • A2 MQ5 • A3 MQ8"},0);
        row(s,x++,new String[]{"ADS1115 #2",yes(adc!=null&&adc.optBoolean("ads2",false)),"0x49 • Relay R2","A0 MQ3 • A1 MQ7 • A2 MQ9 • A3 MQ135"},0);
        row(s,x++,new String[]{"Relay R1",on((relay==null?0:relay.optInt("mask",0))&1),"GPIO25 • MQ group 1","تشغيل يدوي"},0);
        row(s,x++,new String[]{"Relay R2",on((relay==null?0:relay.optInt("mask",0))&2),"GPIO26 • MQ group 2","تشغيل يدوي"},0);
        row(s,x++,new String[]{"Relay R3","احتياط","GPIO27","غير مخصص لحساس MQ في profile الحالي"},0);
        row(s,x++,new String[]{"Relay R4","احتياط","GPIO32","غير مخصص لحساس MQ في profile الحالي"},0);
        row(s,x++,new String[]{"MAX4466 RMS",sound==null?"":num(sound,"rms_v"),"V","غير معاير إلى dB SPL"},0);
        row(s,x++,new String[]{"MAX4466 Peak-to-Peak",sound==null?"":num(sound,"p2p_v"),"V","جهد خام"},0);
        row(s,x++,new String[]{"IR proximity",ir!=null&&ir.optBoolean("warning",false)?"تحذير":"واضح","رقمي","تحذير فقط؛ لا يوجد interlock للحركة"},0);
        boolean ldrInstalled=ldr!=null&&ldr.optBoolean("installed",false);
        row(s,x++,new String[]{"LDR",ldrInstalled?(ldr.optBoolean("active",false)?"Active":"Inactive"):"غير مركب",ldrInstalled?"رقمي/عتبة":"—","لا يُعرض كـlux كمي"},0);
        row(s,x++,new String[]{"Retrace",drive!=null&&drive.optBoolean("retrace",false)?"نشطة":"غير نشطة","dead-reckoning","ليست odometry مغلقة الحلقة"},0);
        return sheetEnd(s);
    }

    private static String mqSheet(ExperimentQueries.Record r){
        JSONObject d=json(r.payload),m=d.optJSONObject("measurement");boolean averaged=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;
        JSONObject src=averaged?m.optJSONObject("mq"):d.optJSONObject("mq");StringBuilder s=sheetStart();
        row(s,1,new String[]{"الحساس","الحالة","Relay","ADS","العنوان","CH","Raw","ADC V","AO V","Rs","R0","Rs/R0","R0 معاير","مصدر R0","نوع اللقطة"},1);
        int rr=2;
        for(String key:RobotTelemetry.MQ_KEYS){
            HazRobHardwareProfile.Channel hw=HazRobHardwareProfile.forKey(key);JSONObject q=src==null?null:src.optJSONObject(key);
            String relay=hw==null?"":String.valueOf(hw.relay),ads=hw==null?"":String.valueOf(hw.adsIndex+1),addr=hw==null?"":String.format(Locale.US,"0x%02X",hw.adsAddress),ch=hw==null?"":String.valueOf(hw.adcChannel);
            if(q==null){row(s,rr++,new String[]{key.toUpperCase(Locale.US),"لا قراءة",relay,ads,addr,ch,"","","","","","","لا","",""},0);continue;}
            String status=q.optBoolean("valid",q.optBoolean("ok",false))?"صالح":"غير صالح";
            row(s,rr++,new String[]{key.toUpperCase(Locale.US),status,relay,ads,addr,ch,num(q,"raw"),num(q,"adc"),num(q,"ao"),num(q,"rs"),num(q,"r0"),num(q,"ratio"),q.optBoolean("r0_calibrated",false)?"نعم":"لا",q.optString("r0_source",""),averaged?"ready_average":"manual_live"},0);
        }
        return sheetEnd(s);
    }

    private static String chemicalSheet(ExperimentQueries.Record r,ChemicalRepository chemicals){
        JSONObject a=json(r.analytics);ResultIndex ix=new ResultIndex(a);StringBuilder s=sheetStart();
        row(s,1,new String[]{"#","المركب","CAS","الصيغة","حالة الدليل","PPM","التوافق %","الثقة","الخطر","كمي","المساهمون","ملاحظة النموذج"},1);
        int rr=2,index=0;
        if(chemicals!=null){
            List<ChemicalRepository.Chemical> catalog=chemicals.search("",5000);
            for(ChemicalRepository.Chemical c:catalog){
                index++;JSONObject q=ix.forChemical(c);boolean evidence=hasEvidence(q),quant=evidence&&q.optBoolean("quantitative",false);int risk=quant?q.optInt("risk",0):0;
                int style=evidence?(quant?riskStyle(risk):2):0;
                row(s,rr++,new String[]{String.valueOf(index),chemicalName(c),n(c.cas),n(c.formula),evidence?(quant?"قياس كمي":"دليل/بصمة"):"لا دليل",quant?num(q,"ppm"):"",evidence?num(q,"agreement"):"",evidence?stars(q.optInt("confidence",0)):"—",quant?riskName(risk):"غير متاح",quant?"نعم":"لا",evidence?String.valueOf(q.optInt("contributors",0)):"0",evidence?q.optString("note",""):""},style);
            }
        }else{
            JSONArray arr=results(a);for(int i=0;i<arr.length();i++){JSONObject q=arr.optJSONObject(i);if(!hasEvidence(q))continue;index++;boolean quant=q.optBoolean("quantitative",false);int risk=quant?q.optInt("risk",0):0;row(s,rr++,new String[]{String.valueOf(index),resultName(q),q.optString("cas",""),q.optString("formula",""),quant?"قياس كمي":"دليل/بصمة",quant?num(q,"ppm"):"",num(q,"agreement"),stars(q.optInt("confidence",0)),quant?riskName(risk):"غير متاح",quant?"نعم":"لا",String.valueOf(q.optInt("contributors",0)),q.optString("note","")},quant?riskStyle(risk):2);}
        }
        if(index==0)row(s,2,new String[]{"—","لا توجد نتائج ذات دليل","","","","","","","","","",""},0);
        return sheetEnd(s);
    }

    // -------------------------------------------------------------------------
    // PDF
    // -------------------------------------------------------------------------

    private static final class PdfReport {
        final PdfDocument pdf;final ChemicalRepository chemicals;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);PdfDocument.Page page;Canvas c;int pageNo;float y;
        PdfReport(PdfDocument pdf,ChemicalRepository chemicals){this.pdf=pdf;this.chemicals=chemicals;p.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.NORMAL));}
        void newPage(String title){if(page!=null)pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,++pageNo).create());c=page.getCanvas();c.drawColor(Color.WHITE);p.setTextAlign(Paint.Align.RIGHT);p.setColor(0xFF0D47A1);p.setTextSize(17);c.drawText(title,812,30,p);p.setTextAlign(Paint.Align.LEFT);p.setColor(Color.DKGRAY);p.setTextSize(8);c.drawText("HazRob 0.8 • page "+pageNo,30,30,p);y=52;}
        void finish(){if(page!=null){pdf.finishPage(page);page=null;}}
        void lineR(String text){p.setTextAlign(Paint.Align.RIGHT);p.setColor(Color.DKGRAY);p.setTextSize(10);c.drawText(n(text),812,y,p);p.setTextAlign(Paint.Align.LEFT);y+=17;}
        void note(String text){p.setTextSize(8);p.setColor(0xFF455A64);for(String line:wrap(text,125)){c.drawText(line,30,y,p);y+=12;}}

        void summary(ExperimentQueries.Record r,JSONObject d,JSONObject a){
            newPage("تقرير فحص HazRob 0.8");lineR("رقم التجربة: "+r.id);lineR("تاريخ الفحص UTC: "+n(r.ts));lineR("البروتوكول: "+n(r.protocol));lineR("الحالة المرجعية: "+(r.approved?"معتمدة":"غير معتمدة"));lineR("Firmware: "+d.optString("firmware","غير معرّف")+"   API: "+d.optInt("api_version",0));lineR("Hardware profile: "+d.optString("hardware_profile",HazRobHardwareProfile.ID));
            JSONObject relay=d.optJSONObject("relay"),m=d.optJSONObject("measurement"),e=env(d);lineR("طاقة MQ: "+HazRobHardwareProfile.groupLabel(relay==null?0:relay.optInt("mask",0)&3)+" • تحكم يدوي");if(m!=null)lineR("حالة الالتقاط: "+m.optString("state","manual/live"));if(e!=null&&e.optBoolean("ok",false))lineR("البيئة: "+e.optString("type","ENV")+" • "+num(e,"temperature")+" °C • "+num(e,"humidity")+" %RH • "+num(e,"pressure")+" hPa");if(chemicals!=null)lineR("مواد قاعدة المعرفة: "+chemicals.count());lineR("نتائج التحليل ذات الدليل: "+resultCount(a));y+=6;note("التركيب: R1/ADS1 0x48 = MQ2, MQ4, MQ5, MQ8. R2/ADS2 0x49 = MQ3, MQ7, MQ9, MQ135. R3/R4 احتياط. R0 اليدوي يستخدم لحساب Rs/R0 فقط؛ PPM الكمي وحدود الخطر لا تعتمد إلا مع معايرة مرجعية ونموذج صالح. MQ7/MQ9 يحتاجان heater-cycle موثقًا للاستخدام الكمي.");
        }

        void physical(JSONObject d){
            newPage("القياسات الفيزيائية والبيئية");header(new String[]{"#","القياس","القيمة","الوحدة","المصدر"},new float[]{35,245,120,90,290});JSONObject e=env(d);
            for(int i=0;i<PhysicalCatalog.COUNT;i++){String value="",source="غير مركب";if(e!=null&&e.optBoolean("ok",false)){String type=e.optString("type","ENV");if(i==0){value=num(e,"temperature");source=type;}else if(i==1){value=num(e,"humidity");source=value.isEmpty()?"غير متاح":type;}else if(i==2){value=num(e,"pressure");source=type;}else if(i==14){double pr=e.optDouble("pressure",Double.NaN);if(Double.isFinite(pr)&&pr>0){value=fmt(44330.0*(1.0-Math.pow(pr/1013.25,0.1903)));source="مشتق من الضغط";}}}tableRow(new String[]{String.valueOf(i+1),PhysicalCatalog.NAMES_AR[i],value,PhysicalCatalog.UNITS[i],source},new float[]{35,245,120,90,290},0);}
        }

        void hardware(JSONObject d){
            newPage("العتاد والحساسات");header(new String[]{"العنصر","القيمة","التوصيل / الملاحظة"},new float[]{190,160,430});JSONObject s=d.optJSONObject("sound"),ir=d.optJSONObject("ir"),ldr=d.optJSONObject("ldr"),adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),drv=d.optJSONObject("drive");int mask=relay==null?0:relay.optInt("mask",0)&3;
            tableRow(new String[]{"ADS1115 #1",yes(adc!=null&&adc.optBoolean("ads1",false)),"0x48 • R1 • A0 MQ2 • A1 MQ4 • A2 MQ5 • A3 MQ8"},new float[]{190,160,430},0);
            tableRow(new String[]{"ADS1115 #2",yes(adc!=null&&adc.optBoolean("ads2",false)),"0x49 • R2 • A0 MQ3 • A1 MQ7 • A2 MQ9 • A3 MQ135"},new float[]{190,160,430},0);
            tableRow(new String[]{"MQ power",HazRobHardwareProfile.groupLabel(mask),"يدوي • R3/R4 احتياط"},new float[]{190,160,430},0);
            tableRow(new String[]{"MAX4466 RMS",s==null?"":num(s,"rms_v")+" V","غير معاير إلى dB SPL"},new float[]{190,160,430},0);
            tableRow(new String[]{"MAX4466 P-P",s==null?"":num(s,"p2p_v")+" V","جهد خام"},new float[]{190,160,430},0);
            tableRow(new String[]{"IR",ir!=null&&ir.optBoolean("warning",false)?"تحذير":"واضح","تحذير فقط؛ لا يوقف الحركة"},new float[]{190,160,430},0);
            boolean li=ldr!=null&&ldr.optBoolean("installed",false);tableRow(new String[]{"LDR",li?(ldr.optBoolean("active",false)?"Active":"Inactive"):"غير مركب","إن وُجد فهو عتبة رقمية، وليس lux"},new float[]{190,160,430},0);
            tableRow(new String[]{"Retrace",drv!=null&&drv.optBoolean("retrace",false)?"نشطة":"غير نشطة","dead-reckoning محلي"},new float[]{190,160,430},0);
        }

        void mq(JSONObject d){
            newPage("قراءات حساسات MQ");header(new String[]{"MQ","R","ADS/CH","Raw","Rs","R0","Rs/R0","حالة"},new float[]{70,45,100,90,110,110,110,145});JSONObject m=d.optJSONObject("measurement");boolean averaged=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;JSONObject src=averaged?m.optJSONObject("mq"):d.optJSONObject("mq");
            for(String key:RobotTelemetry.MQ_KEYS){HazRobHardwareProfile.Channel hw=HazRobHardwareProfile.forKey(key);JSONObject q=src==null?null:src.optJSONObject(key);if(q==null){tableRow(new String[]{key.toUpperCase(Locale.US),hw==null?"":String.valueOf(hw.relay),hw==null?"":String.format(Locale.US,"0x%02X/A%d",hw.adsAddress,hw.adcChannel),"","","","","لا قراءة"},new float[]{70,45,100,90,110,110,110,145},0);continue;}boolean cal=q.optBoolean("r0_calibrated",false);tableRow(new String[]{key.toUpperCase(Locale.US),String.valueOf(hw==null?q.optInt("relay",0):hw.relay),hw==null?"":String.format(Locale.US,"0x%02X/A%d",hw.adsAddress,hw.adcChannel),num(q,"raw"),num(q,"rs"),num(q,"r0"),num(q,"ratio"),(q.optBoolean("valid",q.optBoolean("ok",false))?"صالح":"غير صالح")+" • "+(cal?"R0 معاير":"R0 غير معاير")},new float[]{70,45,100,90,110,110,110,145},0);}
            y+=8;note("نوع اللقطة: "+(averaged?"ready_average":"manual_live")+". لا تفسر Rs/R0 كتركيز دون منحنى ومعايرة مرجعية صالحين.");
        }

        void chemicals(JSONObject a){
            ResultIndex ix=new ResultIndex(a);List<ChemicalRepository.Chemical> catalog=chemicals==null?new ArrayList<>():chemicals.search("",5000);
            if(catalog.isEmpty()){JSONArray arr=results(a);newPage("النتائج الكيميائية");header(new String[]{"#","المركب","الدليل","PPM","اتفاق","الثقة","الخطر"},new float[]{35,260,130,100,80,80,95});int shown=0;for(int i=0;i<arr.length();i++){JSONObject q=arr.optJSONObject(i);if(!hasEvidence(q))continue;shown++;boolean quant=q.optBoolean("quantitative",false);tableRow(new String[]{String.valueOf(shown),resultName(q),quant?"كمي":"بصمة",quant?num(q,"ppm"):"—",num(q,"agreement"),stars(q.optInt("confidence",0)),quant?riskName(q.optInt("risk",0)):"غير متاح"},new float[]{35,260,130,100,80,80,95},quant?riskColor(q.optInt("risk",0)):0xFFE3F2FD);}return;}
            int perPage=18;for(int start=0;start<catalog.size();start+=perPage){int end=Math.min(catalog.size(),start+perPage);newPage("المركبات والنتائج • "+(start+1)+"–"+end+" / "+catalog.size());header(new String[]{"#","المركب","CAS","الدليل","PPM","اتفاق","الخطر"},new float[]{35,225,135,120,90,80,95});for(int i=start;i<end;i++){ChemicalRepository.Chemical ch=catalog.get(i);JSONObject q=ix.forChemical(ch);boolean ev=hasEvidence(q),quant=ev&&q.optBoolean("quantitative",false);int risk=quant?q.optInt("risk",0):0;tableRow(new String[]{String.valueOf(i+1),chemicalName(ch),n(ch.cas),ev?(quant?"كمي":"بصمة"):"لا دليل",quant?num(q,"ppm"):"—",ev?num(q,"agreement"):"—",quant?riskName(risk):"غير متاح"},new float[]{35,225,135,120,90,80,95},ev?(quant?riskColor(risk):0xFFE3F2FD):0);}}
        }

        void header(String[] a,float[] widths){p.setColor(0xFF263238);c.drawRect(25,y,817,y+22,p);float x=30;p.setTextColor(Color.WHITE);p.setTextSize(8);for(int i=0;i<a.length;i++){c.drawText(a[i],x,y+15,p);x+=widths[i];}y+=24;}
        void tableRow(String[] a,float[] widths,int bg){if(y>565)newPage("متابعة التقرير");if(bg!=0){p.setColor(bg);c.drawRect(25,y-2,817,y+16,p);}p.setColor(Color.DKGRAY);p.setTextSize(7.5f);float x=30;for(int i=0;i<a.length;i++){String v=n(a[i]);float max=Math.max(20,widths[i]-6);c.save();c.clipRect(x,y-3,x+max,y+15);c.drawText(v,x,y+10,p);c.restore();x+=widths[i];}y+=18;}
    }

    // -------------------------------------------------------------------------
    // Result indexing and formatting
    // -------------------------------------------------------------------------

    private static final class ResultIndex {
        final Map<Long,JSONObject> byUid=new HashMap<>();final Map<Integer,JSONObject> byLegacy=new HashMap<>();
        ResultIndex(JSONObject analytics){JSONArray a=results(analytics);for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;if(q.has("chemical_uid"))byUid.put(q.optLong("chemical_uid",-1),q);int id=q.has("legacy_id")?q.optInt("legacy_id",-1):q.optInt("chemical_id",-1);if(id>=0)byLegacy.put(id,q);}}
        JSONObject forChemical(ChemicalRepository.Chemical c){if(c==null)return null;JSONObject q=byUid.get(c.uid);if(q==null&&c.legacyId!=null)q=byLegacy.get(c.legacyId);return q;}
    }

    private static boolean hasEvidence(JSONObject q){return q!=null&&q.optBoolean("evidence_present",q.optInt("contributors",0)>0);}
    private static JSONArray results(JSONObject a){if(a==null)return new JSONArray();JSONArray x=a.optJSONArray("results");return x==null?new JSONArray():x;}
    private static int resultCount(JSONObject a){return results(a).length();}
    private static String resultName(JSONObject q){if(q==null)return"";String ar=q.optString("name_ar","");if(!ar.isEmpty())return ar;String en=q.optString("name_en",q.optString("name",""));return en.isEmpty()?"Chemical #"+q.optInt("chemical_id",-1):en;}
    private static String chemicalName(ChemicalRepository.Chemical c){if(c==null)return"";String ar=n(c.arabicName);return !ar.isEmpty()?ar:n(c.name);}
    private static int riskStyle(int risk){if(risk<=0)return 3;if(risk==ExposureLimits.VERY_SAFE||risk==ExposureLimits.SAFE)return 3;if(risk==ExposureLimits.ALERT)return 4;if(risk==ExposureLimits.DANGER)return 5;return 6;}
    private static int riskColor(int risk){if(risk<=0)return 0xFFE8F5E9;if(risk==ExposureLimits.VERY_SAFE||risk==ExposureLimits.SAFE)return 0xFFE8F5E9;if(risk==ExposureLimits.ALERT)return 0xFFFFF8E1;if(risk==ExposureLimits.DANGER)return 0xFFFFE0B2;return 0xFFFFCDD2;}
    private static String riskName(int r){if(r==ExposureLimits.VERY_SAFE)return"منخفض جدًا";if(r==ExposureLimits.SAFE)return"منخفض";if(r==ExposureLimits.ALERT)return"تنبيه";if(r==ExposureLimits.DANGER)return"خطر";if(r==ExposureLimits.CRITICAL)return"حرج";return"غير متاح";}
    private static String stars(int n){if(n<=0)return"—";StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(3,n);i++)s.append('★');return s.toString();}

    // -------------------------------------------------------------------------
    // XML helpers
    // -------------------------------------------------------------------------

    private static String sheetStart(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews><sheetData>";}
    private static String sheetEnd(StringBuilder s){return s.append("</sheetData></worksheet>").toString();}
    private static void row(StringBuilder s,int row,String[] vals,int style){s.append("<row r=\"").append(row).append("\">");for(int i=0;i<vals.length;i++){String ref=col(i+1)+row;s.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");if(style>0)s.append(" s=\"").append(style).append("\"");s.append("><is><t xml:space=\"preserve\">").append(xml(n(vals[i]))).append("</t></is></c>");}s.append("</row>");}
    private static String col(int n){StringBuilder s=new StringBuilder();while(n>0){n--;s.insert(0,(char)('A'+n%26));n/=26;}return s.toString();}
    private static String xml(String v){return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static void entry(ZipOutputStream z,String name,String body)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(body.getBytes(StandardCharsets.UTF_8));z.closeEntry();}

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static JSONObject env(JSONObject d){if(d==null)return null;JSONObject e=d.optJSONObject("env");if(e!=null)return e;return d.optJSONObject("bme");}
    private static String num(JSONObject o,String key){if(o==null||o.isNull(key))return"";double v=o.optDouble(key,Double.NaN);return Double.isFinite(v)?fmt(v):"";}
    private static String fmt(double v){if(!Double.isFinite(v))return"";double a=Math.abs(v);if(a>=10000||a>0&&a<0.001)return String.format(Locale.US,"%.4e",v);return String.format(Locale.US,"%.4f",v).replaceAll("0+$","").replaceAll("\\.$","");}
    private static String n(String s){return s==null?"":s;}
    private static String yes(boolean b){return b?"متصل":"غير مكتشف";}
    private static String on(int b){return b!=0?"ON":"OFF";}
    private static List<String> wrap(String text,int chars){List<String> out=new ArrayList<>();String t=n(text);while(t.length()>chars){int cut=t.lastIndexOf(' ',chars);if(cut<20)cut=chars;out.add(t.substring(0,cut));t=t.substring(cut).trim();}if(!t.isEmpty())out.add(t);return out;}
}
