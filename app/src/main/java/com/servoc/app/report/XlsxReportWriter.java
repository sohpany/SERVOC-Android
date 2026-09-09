package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Creates a complete XLSX in memory and binds every scientific result to the exact recorded session. */
final class XlsxReportWriter {
    private XlsxReportWriter(){}
    private static final String FOOTER="HazRob "+ReleaseAudit.VERSION+" • تصميم المستشار/ شهاب الصهباني.";

    static void write(OutputStream destination,ExperimentQueries.Record r,ChemicalRepository chemicals)throws Exception{
        JSONObject snapshot=json(r.payload),analytics=json(r.analytics);
        SessionSeries.Series series=chemicals==null?null:SessionSeries.forExperiment(chemicals.database(),r);
        String session=series!=null&&!nz(series.sessionId).isEmpty()?series.sessionId:r.sessionId;
        JSONObject chemcore=chemicals==null||nz(session).isEmpty()?null:new ChemCoreDecisionRepository(chemicals.database()).latestPayloadForSession(session);
        List<byte[]> charts=renderCharts(series,analytics,chemicals);
        ByteArrayOutputStream packageBytes=new ByteArrayOutputStream(256*1024);
        try(ZipOutputStream z=new ZipOutputStream(packageBytes)){
            put(z,"[Content_Types].xml",contentTypes(!charts.isEmpty()));
            put(z,"_rels/.rels",rootRels());
            put(z,"xl/workbook.xml",workbook());
            put(z,"xl/_rels/workbook.xml.rels",workbookRels());
            put(z,"xl/styles.xml",styles());
            put(z,"xl/worksheets/sheet1.xml",summarySheet(r,snapshot,analytics,chemcore,series,chemicals));
            put(z,"xl/worksheets/sheet2.xml",timeSeriesSheet(series));
            put(z,"xl/worksheets/sheet3.xml",mqSheet(snapshot));
            put(z,"xl/worksheets/sheet4.xml",analysisSheet(analytics,chemcore));
            put(z,"xl/worksheets/sheet5.xml",hardwareSheet(snapshot));
            put(z,"xl/worksheets/sheet6.xml",chartSheet(!charts.isEmpty()));
            if(!charts.isEmpty()){
                put(z,"xl/worksheets/_rels/sheet6.xml.rels",sheetDrawingRels());
                put(z,"xl/drawings/drawing1.xml",drawing(charts.size()));
                put(z,"xl/drawings/_rels/drawing1.xml.rels",drawingRels(charts.size()));
                for(int i=0;i<charts.size();i++)putBytes(z,"xl/media/image"+(i+1)+".png",charts.get(i));
            }
            z.finish();
        }
        byte[] bytes=packageBytes.toByteArray();validate(bytes);destination.write(bytes);destination.flush();
    }

    private static List<byte[]> renderCharts(SessionSeries.Series s,JSONObject analytics,ChemicalRepository chemicals)throws Exception{
        List<byte[]> out=new ArrayList<>();
        if(s!=null&&s.usable()){out.add(render((c,b)->AnalyticalCharts.mqRsR0(c,b,s)));out.add(render((c,b)->AnalyticalCharts.fingerprint(c,b,s)));}
        out.add(render((c,b)->AnalyticalCharts.candidates(c,b,analytics)));
        if(s!=null&&s.usable()&&chemicals!=null){byte[] ppm=renderPpm(s,chemicals,analytics);if(ppm!=null)out.add(ppm);}
        if(s!=null&&s.usable()){
            out.add(render((c,b)->AnalyticalCharts.temperature(c,b,s)));out.add(render((c,b)->AnalyticalCharts.humidity(c,b,s)));
            out.add(render((c,b)->AnalyticalCharts.pressure(c,b,s)));out.add(render((c,b)->AnalyticalCharts.sound(c,b,s)));
        }
        return out;
    }
    private interface Drawer{void draw(Canvas c,RectF b);}
    private static byte[] render(Drawer d)throws Exception{Bitmap b=Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);d.draw(c,new RectF(10,10,1190,590));ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,o);b.recycle();return o.toByteArray();}
    private static byte[] renderPpm(SessionSeries.Series s,ChemicalRepository chemicals,JSONObject analytics)throws Exception{Bitmap b=Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);int count=AnalyticalCharts.ppm(c,new RectF(10,10,1190,590),s,chemicals.database(),analytics);if(count<=0){b.recycle();return null;}ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,o);b.recycle();return o.toByteArray();}

    private static void validate(byte[] bytes)throws Exception{
        Set<String> names=new HashSet<>();try(ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=zin.getNextEntry())!=null){if(!names.add(e.getName()))throw new IllegalStateException("duplicate XLSX entry: "+e.getName());while(zin.read()!=-1){}zin.closeEntry();}}
        String[] required={"[Content_Types].xml","_rels/.rels","xl/workbook.xml","xl/_rels/workbook.xml.rels","xl/styles.xml","xl/worksheets/sheet1.xml","xl/worksheets/sheet2.xml","xl/worksheets/sheet3.xml","xl/worksheets/sheet4.xml","xl/worksheets/sheet5.xml","xl/worksheets/sheet6.xml"};for(String q:required)if(!names.contains(q))throw new IllegalStateException("missing XLSX entry: "+q);
    }

    private static String contentTypes(boolean drawings){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>");if(drawings)s.append("<Default Extension=\"png\" ContentType=\"image/png\"/>");s.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");for(int i=1;i<=6;i++)s.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");if(drawings)s.append("<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");return s.append("</Types>").toString();}
    private static String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";}
    private static String workbook(){String[] n={"ملخص","السلسلة الزمنية","MQ","التحليل","الحالة","الرسوم"};StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");for(int i=0;i<n.length;i++)s.append("<sheet name=\"").append(xml(n[i])).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");return s.append("</sheets></workbook>").toString();}
    private static String workbookRels(){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=1;i<=6;i++)s.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");return s.append("<Relationship Id=\"rId7\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>").toString();}
    private static String styles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"10\"/><name val=\"Arial\"/></font><font><b/><sz val=\"10\"/><name val=\"Arial\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs></styleSheet>";}

    private static String summarySheet(ExperimentQueries.Record r,JSONObject d,JSONObject a,JSONObject cc,SessionSeries.Series s,ChemicalRepository chemicals){
        StringBuilder x=sheet();int row=1;JSONObject relay=d.optJSONObject("relay");boolean on=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long powerMs=relay==null?0:relay.optLong("power_ms",0);String session=s!=null&&!nz(s.sessionId).isEmpty()?s.sessionId:r.sessionId;
        strRow(x,row++,true,"HazRob "+ReleaseAudit.VERSION,"القيمة");strRow(x,row++,false,"رقم التجربة",String.valueOf(r.id));strRow(x,row++,false,"UTC",nz(r.ts));strRow(x,row++,false,"البروتوكول",nz(r.protocol));strRow(x,row++,false,"Session",nz(session));strRow(x,row++,false,"Firmware",d.optString("firmware","—"));strRow(x,row++,false,"API",String.valueOf(d.optInt("api_version",0)));strRow(x,row++,false,"Hardware",d.optString("hardware_profile",HazRobHardwareProfile.ID));strRow(x,row++,false,"MQ Power",(on?"ON":"OFF")+" • GPIO25 • 8 sensors");strRow(x,row++,false,"MQ power time",on?powerMs+" ms":"0 ms");strRow(x,row++,false,"Time-series",s!=null&&s.usable()?s.samples.size()+" samples / "+String.format(Locale.US,"%.1f s",s.durationMs()/1000.0):"غير متاح");strRow(x,row++,false,"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count()));strRow(x,row++,false,"Legacy analytical candidates",String.valueOf(resultCount(a)));
        if(cc!=null){JSONObject lib=cc.optJSONObject("library");strRow(x,row++,false,"ChemCore outcome",cc.optString("outcome","—"));strRow(x,row++,false,"Evidence tier",cc.optString("evidence_tier","—"));strRow(x,row++,false,"Top posterior",finite(cc,"top_posterior"));strRow(x,row++,false,"OOD score",finite(cc,"ood_score"));strRow(x,row++,false,"Normalized entropy",finite(cc,"normalized_entropy"));strRow(x,row++,false,"Sampling profile",cc.optString("sampling_profile_id","—"));strRow(x,row++,false,"Feature schema",cc.optString("feature_schema","—"));strRow(x,row++,false,"Model version",cc.optString("model_version","—"));strRow(x,row++,false,"Library version",lib==null?"—":lib.optString("version","—"));strRow(x,row++,false,"Library SHA-256",lib==null?"—":lib.optString("sha256","—"));strRow(x,row++,false,"Library source commit",lib==null?"—":lib.optString("source_commit","—"));strRow(x,row++,false,"Occupational quantitative interpretation",cc.optBoolean("quantitative_occupational_interpretation_permitted",false)?"PERMITTED by evidence gate":"NOT PERMITTED");}
        else strRow(x,row++,false,"ChemCore","No session-bound ChemCore decision recorded");strRow(x,row++,false,"التذييل",FOOTER);return endSheet(x);
    }

    private static String timeSeriesSheet(SessionSeries.Series s){StringBuilder x=sheet();List<String> h=new ArrayList<>();h.add("t_s");h.add("temperature_C");h.add("humidity_RH");h.add("pressure_hPa");h.add("sound_rms_V");h.add("sound_pp_V");for(HazRobHardwareProfile.Channel q:HazRobHardwareProfile.MQ){h.add(q.key+"_ratio");h.add(q.key+"_raw");}strRow(x,1,true,h.toArray(new String[0]));if(s==null||!s.usable()){strRow(x,2,false,"No matching recorded time-series");return endSheet(x);}int r=2;for(SessionSeries.Sample q:s.samples){List<Object> v=new ArrayList<>();v.add(q.tMs/1000.0);v.add(SessionSeries.value(q.env,"temperature"));v.add(SessionSeries.value(q.env,"humidity"));v.add(SessionSeries.value(q.env,"pressure"));JSONObject sound=q.state.optJSONObject("sound");v.add(sound==null?Double.NaN:sound.optDouble("rms_v",Double.NaN));v.add(sound==null?Double.NaN:sound.optDouble("p2p_v",Double.NaN));for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){JSONObject m=SessionSeries.sensor(q,hw.key);v.add(m==null?Double.NaN:m.optDouble("ratio",Double.NaN));v.add(m==null?Double.NaN:m.optDouble("raw",Double.NaN));}mixedRow(x,r++,v);}return endSheet(x);}
    private static String mqSheet(JSONObject d){StringBuilder x=sheet();strRow(x,1,true,"الحساس","MQ Power","ADS","Channel","Raw","Vao","Rs","Rs/R0","الحالة");JSONObject mq=d.optJSONObject("mq"),relay=d.optJSONObject("relay");boolean on=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);int row=2;for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){JSONObject q=mq==null?null:mq.optJSONObject(hw.key);mixedRow(x,row++,list(hw.key.toUpperCase(Locale.US),on?"ON • GPIO25":"OFF • GPIO25","ADS"+(hw.adsIndex+1),"A"+hw.adcChannel,q==null?Double.NaN:q.optDouble("raw",Double.NaN),q==null?Double.NaN:q.optDouble("ao",Double.NaN),q==null?Double.NaN:q.optDouble("rs",Double.NaN),q==null?Double.NaN:q.optDouble("ratio",Double.NaN),q==null?"—":q.optString("status",q.optBoolean("ok",false)?"OK":"—")));}return endSheet(x);}

    private static String analysisSheet(JSONObject a,JSONObject cc){StringBuilder x=sheet();int row=1;
        if(cc!=null){strRow(x,row++,true,"ChemCore decision","Value");strRow(x,row++,false,"Schema",cc.optString("schema","—"));strRow(x,row++,false,"Session",cc.optString("session_id","—"));strRow(x,row++,false,"Outcome",cc.optString("outcome","—"));strRow(x,row++,false,"Evidence tier",cc.optString("evidence_tier","—"));strRow(x,row++,false,"Chemical UID",cc.has("chemical_uid")?String.valueOf(cc.optLong("chemical_uid")):"—");strRow(x,row++,false,"Chemical",cc.optString("chemical","—"));strRow(x,row++,false,"PPM",finite(cc,"ppm"));strRow(x,row++,false,"Prediction low",finite(cc,"prediction_low"));strRow(x,row++,false,"Prediction high",finite(cc,"prediction_high"));strRow(x,row++,false,"Top posterior",finite(cc,"top_posterior"));strRow(x,row++,false,"OOD score",finite(cc,"ood_score"));strRow(x,row++,false,"Normalized entropy",finite(cc,"normalized_entropy"));strRow(x,row++,false,"Sampling profile",cc.optString("sampling_profile_id","—"));strRow(x,row++,false,"Feature schema",cc.optString("feature_schema","—"));strRow(x,row++,false,"Model version",cc.optString("model_version","—"));JSONObject lib=cc.optJSONObject("library");strRow(x,row++,false,"Library version",lib==null?"—":lib.optString("version","—"));strRow(x,row++,false,"Library SHA-256",lib==null?"—":lib.optString("sha256","—"));strRow(x,row++,false,"Library source commit",lib==null?"—":lib.optString("source_commit","—"));strRow(x,row++,false,"Occupational quantitative interpretation",cc.optBoolean("quantitative_occupational_interpretation_permitted",false)?"PERMITTED by evidence gate":"NOT PERMITTED");strRow(x,row++,false,"Note",cc.optString("note",""));row++;
            strRow(x,row++,true,"ChemCore candidate","Chemical UID","Posterior","Best PPM","Residual RMS","Contributors","Evidence tier");JSONArray cs=cc.optJSONArray("candidates");if(cs!=null)for(int i=0;i<cs.length();i++){JSONObject q=cs.optJSONObject(i);if(q==null)continue;mixedRow(x,row++,list(q.optString("label","Chemical "+q.optLong("chemical_uid",-1)),q.optLong("chemical_uid",-1),q.optDouble("posterior",Double.NaN),q.optDouble("best_ppm",Double.NaN),q.optDouble("residual_rms",Double.NaN),q.optInt("contributors",0),q.optString("evidence_tier","—")));}row+=2;}
        strRow(x,row++,true,"Legacy analytical result","Formula","PPM","Quantitative","Agreement %","Confidence","Contributors","Note");JSONArray rs=a.optJSONArray("results");if(rs!=null)for(int i=0;i<rs.length();i++){JSONObject q=rs.optJSONObject(i);if(q==null)continue;String name=q.optString("name_ar",q.optString("name",q.optString("name_en","Chemical #"+q.optInt("chemical_id",-1))));mixedRow(x,row++,list(name,q.optString("formula",""),q.optBoolean("quantitative",false)?q.optDouble("ppm",Double.NaN):Double.NaN,q.optBoolean("quantitative",false)?"YES":"NO",q.optDouble("agreement",Double.NaN),q.optInt("confidence",0),q.optInt("contributors",0),q.optString("note","")));}return endSheet(x);}

    private static String hardwareSheet(JSONObject d){StringBuilder x=sheet();strRow(x,1,true,"العنصر","الحالة","القيمة");JSONObject adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),drive=d.optJSONObject("drive"),env=d.optJSONObject("env");boolean on=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long powerMs=relay==null?0:relay.optLong("power_ms",0);strRow(x,2,false,"ADS1115 #1",adc!=null&&adc.optBoolean("ads1",false)?"OK":"OFF","0x48");strRow(x,3,false,"ADS1115 #2",adc!=null&&adc.optBoolean("ads2",false)?"OK":"OFF","0x49");strRow(x,4,false,"MQ Power",on?"ON":"OFF","GPIO25 • 8 sensors • "+powerMs+" ms");strRow(x,5,false,"Environment",env!=null&&env.optBoolean("ok",false)?"OK":"OFF",env==null?"—":env.optString("type",env.optString("sensor","ENV")));strRow(x,6,false,"Drive",drive==null?"—":drive.optBoolean("failsafe",false)?"FAILSAFE":"OK",drive==null?"—":"L "+drive.optInt("left",0)+" / R "+drive.optInt("right",0));return endSheet(x);}
    private static String chartSheet(boolean drawing){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>HazRob "+xml(ReleaseAudit.VERSION)+" analytical charts</t></is></c></row></sheetData>"+(drawing?"<drawing r:id=\"rId1\"/>":"")+"</worksheet>";}
    private static String sheetDrawingRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>";}
    private static String drawing(int n){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");for(int i=0;i<n;i++){int col=(i%2)*9,row=(i/2)*17,id=i+1;s.append("<xdr:oneCellAnchor><xdr:from><xdr:col>").append(col).append("</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>").append(row).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from><xdr:ext cx=\"5486400\" cy=\"2743200\"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(id).append("\" name=\"Chart ").append(id).append("\"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed=\"rId").append(id).append("\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>");}return s.append("</xdr:wsDr>").toString();}
    private static String drawingRels(int n){StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");for(int i=1;i<=n;i++)s.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image").append(i).append(".png\"/>");return s.append("</Relationships>").toString();}

    private static StringBuilder sheet(){return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");}
    private static String endSheet(StringBuilder x){return x.append("</sheetData></worksheet>").toString();}
    private static void strRow(StringBuilder x,int row,boolean bold,String... v){x.append("<row r=\"").append(row).append("\">");for(int i=0;i<v.length;i++)x.append("<c r=\"").append(col(i)).append(row).append("\" t=\"inlineStr\" s=\"").append(bold?1:0).append("\"><is><t>").append(xml(nz(v[i]))).append("</t></is></c>");x.append("</row>");}
    private static void mixedRow(StringBuilder x,int row,List<Object> v){x.append("<row r=\"").append(row).append("\">");for(int i=0;i<v.size();i++){Object o=v.get(i);String ref=col(i)+row;if(o instanceof Number){double d=((Number)o).doubleValue();if(Double.isFinite(d))x.append("<c r=\"").append(ref).append("\"><v>").append(Double.toString(d)).append("</v></c>");else x.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t></t></is></c>");}else x.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>").append(xml(String.valueOf(o==null?"":o))).append("</t></is></c>");}x.append("</row>");}
    private static List<Object> list(Object...o){List<Object>x=new ArrayList<>();for(Object q:o)x.add(q);return x;}
    private static String col(int n){StringBuilder s=new StringBuilder();n++;while(n>0){int r=(n-1)%26;s.insert(0,(char)('A'+r));n=(n-1)/26;}return s.toString();}
    private static void put(ZipOutputStream z,String name,String s)throws Exception{putBytes(z,name,s.getBytes(StandardCharsets.UTF_8));}
    private static void putBytes(ZipOutputStream z,String name,byte[] b)throws Exception{ZipEntry e=new ZipEntry(name);z.putNextEntry(e);z.write(b);z.closeEntry();}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static int resultCount(JSONObject a){JSONArray q=a==null?null:a.optJSONArray("results");return q==null?0:q.length();}
    private static String finite(JSONObject q,String key){double v=q==null?Double.NaN:q.optDouble(key,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%.6g",v):"—";}
    private static String nz(String s){return s==null?"":s;}
    private static String xml(String s){return nz(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
