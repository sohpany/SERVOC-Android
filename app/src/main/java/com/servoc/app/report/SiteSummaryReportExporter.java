package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldSessionRepository;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compact site-level report summarizing all field sessions under one immutable Site. */
public final class SiteSummaryReportExporter {
    private SiteSummaryReportExporter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595;
    private static final float MARGIN=28.35f;
    private static final int ROWS_PER_MATRIX_PAGE=18;

    public static final class SessionSummary {
        public final FieldSessionReportExporter.Bundle bundle;
        public final String outcome,chemical,evidenceTier;
        public final double posterior,ppm,hazardPriority,percentLfl;
        public final boolean ppmReportable,safetyGate;
        SessionSummary(FieldSessionReportExporter.Bundle b){
            bundle=b;JSONObject q=b.primaryDecision;outcome=q==null?"—":q.optString("outcome","—");chemical=q==null?"—":q.optString("chemical","—");evidenceTier=q==null?"—":q.optString("evidence_tier","—");posterior=q==null?Double.NaN:q.optDouble("top_posterior",Double.NaN);ppmReportable=q!=null&&q.optBoolean("ppm_reportable",false);ppm=q==null?Double.NaN:q.optDouble("ppm",Double.NaN);JSONObject g=q==null?null:q.optJSONObject("gas_exposure_ppm");safetyGate=g!=null&&g.optBoolean("input_gate_passed",false);hazardPriority=g==null?Double.NaN:g.optDouble("hazard_priority_percent",Double.NaN);percentLfl=g==null?Double.NaN:g.optDouble("percent_lfl",Double.NaN);
        }
        boolean critical(){return safetyGate&&((Double.isFinite(hazardPriority)&&hazardPriority>=100)||(Double.isFinite(percentLfl)&&percentLfl>=100));}
    }

    public static final class SiteBundle {
        public final FieldSessionRepository.Site site;public final List<SessionSummary> sessions;
        SiteBundle(FieldSessionRepository.Site site,List<SessionSummary> sessions){this.site=site;this.sessions=Collections.unmodifiableList(sessions);}
    }

    public static SiteBundle resolve(ExperimentsDb db,String siteId){
        if(db==null)throw new IllegalArgumentException("db required");String id=required(siteId);FieldSessionRepository repo=new FieldSessionRepository(db);FieldSessionRepository.Site site=null;
        for(FieldSessionRepository.Site q:repo.sites())if(id.equals(q.id)){site=q;break;}if(site==null)throw new IllegalArgumentException("site not found");
        List<SessionSummary> out=new ArrayList<>();for(FieldSessionRepository.FieldSession fs:repo.sessionsForSite(id))out.add(new SessionSummary(FieldSessionReportExporter.resolve(db,fs.id)));return new SiteBundle(site,out);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String siteId)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");SiteBundle b=resolve(db,siteId);int matrixPages=Math.max(1,(b.sessions.size()+ROWS_PER_MATRIX_PAGE-1)/ROWS_PER_MATRIX_PAGE);int total=2+matrixPages;
        PdfDocument pdf=new PdfDocument();int pageNo=1;PdfDocument.Page p=page(pdf,pageNo);overview(p.getCanvas(),b);chrome(p.getCanvas(),pageNo++,total,b);pdf.finishPage(p);
        for(int chunk=0;chunk<matrixPages;chunk++){p=page(pdf,pageNo);matrix(p.getCanvas(),b,chunk);chrome(p.getCanvas(),pageNo++,total,b);pdf.finishPage(p);}
        p=page(pdf,pageNo);aggregate(p.getCanvas(),b);chrome(p.getCanvas(),pageNo,total,b);pdf.finishPage(p);pdf.writeTo(out);pdf.close();
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}

    private static void overview(Canvas c,SiteBundle b){
        title(c,"ملخص الموقع",42);int active=0,closed=0,measurements=0,analytical=0,identified=0,quantified=0,critical=0;String first="",last="";
        for(SessionSummary s:b.sessions){if(s.bundle.fieldSession.active())active++;else closed++;measurements+=s.bundle.measurementSessionIds.size();if(s.bundle.primaryDecision!=null)analytical++;if("IDENTIFIED".equals(s.outcome))identified++;if(s.ppmReportable||"QUANTIFIED".equals(s.outcome))quantified++;if(s.critical())critical++;String t=s.bundle.fieldSession.startedTs;if(first.isEmpty()||t.compareTo(first)<0)first=t;if(last.isEmpty()||t.compareTo(last)>0)last=t;}
        labelValue(c,"الموقع",b.site.name,76);labelValue(c,"Site ID",b.site.id,100);labelValue(c,"ملاحظات",b.site.notes==null||b.site.notes.isEmpty()?"—":b.site.notes,124);labelValue(c,"الفترة",first.isEmpty()?"—":first+" → "+last,148);
        metric(c,42,190,170,86,"الجلسات",String.valueOf(b.sessions.size()),"نشطة "+active+" • منتهية "+closed,0xFFE3F2FD);
        metric(c,225,190,170,86,"القياسات",String.valueOf(measurements),"Technical sessions",0xFFF3E5F5);
        metric(c,408,190,170,86,"تحليل ChemCore",String.valueOf(analytical),"Identified "+identified,0xFFE8F5E9);
        metric(c,591,190,170,86,"كمّي M4",String.valueOf(quantified),"PPM reportable",0xFFFFF3E0);
        metric(c,42,300,170,86,"جلسات حرجة",String.valueOf(critical),"≥100% مرجع صحة/LFL",critical>0?0xFFFFEBEE:0xFFE8F5E9);
        int withSeries=0,withScreening=0;for(SessionSummary s:b.sessions){if(s.bundle.primarySeries!=null&&s.bundle.primarySeries.usable())withSeries++;if(s.bundle.primaryDecision!=null&&s.bundle.primaryDecision.optJSONObject("screening_144")!=null)withScreening++;}
        metric(c,225,300,170,86,"سلسلة زمنية",String.valueOf(withSeries),"جلسات قابلة للرسم",0xFFE0F7FA);metric(c,408,300,170,86,"فحص 144",String.valueOf(withScreening),"جلسات لديها Screening",0xFFF1F8E9);
        metric(c,591,300,170,86,"التغطية",b.sessions.isEmpty()?"—":String.format(Locale.US,"%.0f%%",100.0*analytical/b.sessions.size()),"جلسات محللة",0xFFF5F5F5);
        text(c,"يعرض هذا التقرير كل الجلسات التابعة للموقع ككيانات مستقلة. لا يتم متوسط PPM بين الجلسات ولا دمج السلاسل الزمنية إلا إذا توفرت قاعدة علمية موحدة لذلك؛ المقارنة هنا مقارنة حالة/دليل ومؤشرات جلسات.",42,455,9,Color.DKGRAY,false);
    }

    private static void matrix(Canvas c,SiteBundle b,int chunk){
        int start=chunk*ROWS_PER_MATRIX_PAGE,end=Math.min(b.sessions.size(),start+ROWS_PER_MATRIX_PAGE);title(c,"مصفوفة جلسات الموقع "+(chunk+1),42);float y=76;
        text(c,"#",34,y,8,Color.DKGRAY,true);text(c,"الجلسة",58,y,8,Color.DKGRAY,true);text(c,"البدء",190,y,8,Color.DKGRAY,true);text(c,"قياس",330,y,8,Color.DKGRAY,true);text(c,"القرار",382,y,8,Color.DKGRAY,true);text(c,"المادة",470,y,8,Color.DKGRAY,true);text(c,"Posterior",595,y,8,Color.DKGRAY,true);text(c,"الدليل",650,y,8,Color.DKGRAY,true);text(c,"PPM",722,y,8,Color.DKGRAY,true);text(c,"سلامة",780,y,8,Color.DKGRAY,true);y+=18;
        if(start>=end){text(c,"لا توجد جلسات تحت هذا الموقع.",42,112,11,Color.DKGRAY,false);return;}
        for(int i=start;i<end;i++){SessionSummary s=b.sessions.get(i);int bg=(i&1)==0?0xFFF7F8FA:0xFFFFFFFF;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(bg);p.setStyle(Paint.Style.FILL);c.drawRect(new RectF(30,y-13,812,y+7),p);text(c,String.valueOf(i+1),34,y,7,Color.BLACK,false);text(c,shorten(s.bundle.fieldSession.name,19),58,y,7,Color.BLACK,false);text(c,shorten(s.bundle.fieldSession.startedTs,19),190,y,7,Color.BLACK,false);text(c,String.valueOf(s.bundle.measurementSessionIds.size()),330,y,7,Color.BLACK,false);text(c,shorten(s.outcome,12),382,y,7,Color.BLACK,false);text(c,shorten(s.chemical,18),470,y,7,Color.BLACK,false);text(c,pct(s.posterior),595,y,7,Color.BLACK,false);text(c,shorten(s.evidenceTier,10),650,y,7,Color.BLACK,false);text(c,s.ppmReportable&&Double.isFinite(s.ppm)?fmt(s.ppm):"—",722,y,7,Color.BLACK,false);text(c,s.critical()?"⚠":s.safetyGate?"OK":"—",790,y,8,s.critical()?0xFFC62828:Color.DKGRAY,true,Paint.Align.CENTER);y+=24;}
        text(c,"Session IDs are retained in the exported field-session reports and site lineage. This matrix is an executive comparison, not a pooled exposure calculation.",42,548,8,Color.DKGRAY,false);
    }

    private static void aggregate(Canvas c,SiteBundle b){
        title(c,"النتائج المتكررة والأولوية",42);Map<String,Integer> chemicals=new HashMap<>(),outcomes=new HashMap<>();int safetyGate=0,critical=0,withPpm=0;
        for(SessionSummary s:b.sessions){if(!"—".equals(s.chemical)&&!s.chemical.trim().isEmpty())inc(chemicals,s.chemical);inc(outcomes,s.outcome);if(s.safetyGate)safetyGate++;if(s.critical())critical++;if(s.ppmReportable)withPpm++;}
        List<Map.Entry<String,Integer>> chem=sorted(chemicals),outs=sorted(outcomes);text(c,"أكثر المواد ترشيحًا عبر الجلسات",42,82,12,Color.BLACK,true);float y=108;if(chem.isEmpty())text(c,"لا توجد نتائج ChemCore مجمعة.",42,y,10,Color.DKGRAY,false);else for(int i=0;i<Math.min(10,chem.size());i++){Map.Entry<String,Integer> e=chem.get(i);text(c,(i+1)+". "+e.getKey(),42,y,9,Color.BLACK,false);bar(c,250,y-10,280,12,e.getValue(),Math.max(1,chem.get(0).getValue()));text(c,String.valueOf(e.getValue()),540,y,9,Color.DKGRAY,true);y+=28;}
        text(c,"حالات القرار",595,82,12,Color.BLACK,true);float oy=108;for(int i=0;i<Math.min(10,outs.size());i++){Map.Entry<String,Integer> e=outs.get(i);text(c,e.getKey()+" • "+e.getValue(),595,oy,9,Color.BLACK,false);oy+=25;}
        float sy=410;metric(c,42,sy,170,86,"PPM reportable",String.valueOf(withPpm),"جلسات ببوابة كمية مكتملة",0xFFE8F5E9);metric(c,225,sy,170,86,"Safety gate",String.valueOf(safetyGate),"جلسات صالحة للتفسير",0xFFE3F2FD);metric(c,408,sy,170,86,"Critical",String.valueOf(critical),"مرجع صحي/LFL متجاوز",critical>0?0xFFFFEBEE:0xFFE8F5E9);metric(c,591,sy,170,86,"Total sessions",String.valueOf(b.sessions.size()),"كل جلسات الموقع",0xFFF5F5F5);
        text(c,"الاستنتاج على مستوى الموقع هو نمط تكرار ومقارنة بين الجلسات. لا يُحوّل Screening-fit إلى PPM ولا يساوي بين جلسات ذات معايرة/ظروف غير متكافئة.",42,538,8,Color.DKGRAY,false);
    }

    private static void metric(Canvas c,float x,float y,float w,float h,String label,String value,String note,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),8,8,p);text(c,label,x+10,y+20,9,Color.DKGRAY,true);text(c,value,x+10,y+48,20,Color.BLACK,true);text(c,note,x+10,y+70,8,Color.DKGRAY,false);}
    private static void bar(Canvas c,float x,float y,float w,float h,int value,int max){Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);bg.setColor(0xFFE0E0E0);bg.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,bg);Paint fg=new Paint(Paint.ANTI_ALIAS_FLAG);fg.setColor(0xFF546E7A);fg.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w*Math.max(0,Math.min(1,value/(float)max)),y+h,fg);}
    private static void inc(Map<String,Integer> m,String k){String q=k==null||k.trim().isEmpty()?"—":k.trim();m.put(q,m.containsKey(q)?m.get(q)+1:1);}
    private static List<Map.Entry<String,Integer>> sorted(Map<String,Integer> m){List<Map.Entry<String,Integer>> x=new ArrayList<>(m.entrySet());Collections.sort(x,new Comparator<Map.Entry<String,Integer>>(){public int compare(Map.Entry<String,Integer>a,Map.Entry<String,Integer>b){int n=Integer.compare(b.getValue(),a.getValue());return n!=0?n:a.getKey().compareToIgnoreCase(b.getKey());}});return x;}
    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);}
    private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,9,Color.DKGRAY,true);text(c,v,190,y,10,Color.BLACK,false);}
    private static void chrome(Canvas c,int page,int total,SiteBundle b){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION,MARGIN,23,11,Color.BLACK,true);text(c,"تقرير الموقع • "+b.site.name,PAGE_WIDTH/2f,23,11,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,9,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,9,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.0f%%",v<=1.000001?v*100.0:v);}
    private static String shorten(String s,int n){String q=s==null?"":s;return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
    private static String required(String s){String q=s==null?"":s.trim();if(q.isEmpty())throw new IllegalArgumentException("site required");return q;}
}
