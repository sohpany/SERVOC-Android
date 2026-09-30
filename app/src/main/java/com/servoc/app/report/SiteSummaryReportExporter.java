package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldSessionRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Intelligent site-level report: independent field sessions plus cross-session chemical evidence. */
public final class SiteSummaryReportExporter {
    private SiteSummaryReportExporter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595;
    private static final float MARGIN=28.35f;
    private static final int ROWS_PER_MATRIX_PAGE=18;

    public static final class SessionSummary {
        public final FieldSessionReportExporter.Bundle bundle;
        public final String outcome,chemical,evidenceTier;
        public final double posterior,analyticalConfidencePct,ppm,hazardPriority,percentLfl;
        public final boolean ppmReportable,safetyGate;
        public final int panelSupportedCount,panelAcceptedCount;
        public final JSONObject candidateHazards;
        SessionSummary(ExperimentsDb db,FieldSessionReportExporter.Bundle b){
            bundle=b;JSONObject q=b.primaryDecision;
            outcome=q==null?"—":q.optString("outcome","—");chemical=q==null?"—":q.optString("chemical","—");evidenceTier=q==null?"—":q.optString("evidence_tier","—");posterior=q==null?Double.NaN:q.optDouble("top_posterior",Double.NaN);analyticalConfidencePct=q==null?Double.NaN:q.optDouble("analytical_confidence_pct",Double.NaN);ppmReportable=q!=null&&q.optBoolean("ppm_reportable",false);ppm=q==null?Double.NaN:q.optDouble("ppm",Double.NaN);
            JSONObject panel=q==null?null:q.optJSONObject("screening_144");panelSupportedCount=panel==null?0:panel.optInt("session_candidate_supported_count",0);panelAcceptedCount=panel==null?0:panel.optInt("accepted_identity_count",0);
            JSONObject g=q==null?null:q.optJSONObject("gas_exposure_ppm");safetyGate=g!=null&&g.optBoolean("input_gate_passed",false);hazardPriority=g==null?Double.NaN:g.optDouble("hazard_priority_percent",Double.NaN);percentLfl=g==null?Double.NaN:g.optDouble("percent_lfl",Double.NaN);candidateHazards=CandidateHazardContext.evaluate(db,q);
        }
        boolean critical(){return safetyGate&&((Double.isFinite(hazardPriority)&&hazardPriority>=100)||(Double.isFinite(percentLfl)&&percentLfl>=100));}
        JSONObject topHazard(){JSONArray a=candidateHazards==null?null:candidateHazards.optJSONArray("candidates");return a==null||a.length()==0?null:a.optJSONObject(0);}
    }

    public static final class SiteBundle {
        public final FieldSessionRepository.Site site;public final List<SessionSummary> sessions;
        SiteBundle(FieldSessionRepository.Site site,List<SessionSummary> sessions){this.site=site;this.sessions=Collections.unmodifiableList(sessions);}
    }

    public static SiteBundle resolve(ExperimentsDb db,String siteId){
        if(db==null)throw new IllegalArgumentException("db required");String id=required(siteId);FieldSessionRepository repo=new FieldSessionRepository(db);FieldSessionRepository.Site site=null;
        for(FieldSessionRepository.Site q:repo.sites())if(id.equals(q.id)){site=q;break;}if(site==null)throw new IllegalArgumentException("site not found");
        List<SessionSummary> out=new ArrayList<>();for(FieldSessionRepository.FieldSession fs:repo.sessionsForSite(id))out.add(new SessionSummary(db,FieldSessionReportExporter.resolve(db,fs.id)));return new SiteBundle(site,out);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String siteId)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");SiteBundle b=resolve(db,siteId);SiteReportIntelligence.SitePlan plan=SiteReportIntelligence.plan(b.sessions);int matrixPages=Math.max(1,(b.sessions.size()+ROWS_PER_MATRIX_PAGE-1)/ROWS_PER_MATRIX_PAGE);int total=3+matrixPages;
        PdfDocument pdf=new PdfDocument();int pageNo=1;PdfDocument.Page p=page(pdf,pageNo);overview(p.getCanvas(),b,plan);chrome(p.getCanvas(),pageNo++,total,b);pdf.finishPage(p);
        for(int chunk=0;chunk<matrixPages;chunk++){p=page(pdf,pageNo);matrix(p.getCanvas(),b,chunk);chrome(p.getCanvas(),pageNo++,total,b);pdf.finishPage(p);}
        p=page(pdf,pageNo);recurrence(p.getCanvas(),plan);chrome(p.getCanvas(),pageNo++,total,b);pdf.finishPage(p);
        p=page(pdf,pageNo);priority(p.getCanvas(),b,plan);chrome(p.getCanvas(),pageNo,total,b);pdf.finishPage(p);pdf.writeTo(out);pdf.close();
    }

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}

    private static void overview(Canvas c,SiteBundle b,SiteReportIntelligence.SitePlan plan){
        title(c,"الملخص التنفيذي للموقع",42);int active=0,closed=0,measurements=0;String first="",last="";for(SessionSummary s:b.sessions){if(s.bundle.fieldSession.active())active++;else closed++;measurements+=s.bundle.measurementSessionIds.size();String t=s.bundle.fieldSession.startedTs;if(first.isEmpty()||t.compareTo(first)<0)first=t;if(last.isEmpty()||t.compareTo(last)>0)last=t;}
        labelValue(c,"الموقع",b.site.name,76);labelValue(c,"Site ID",b.site.id,100);labelValue(c,"ملاحظات",b.site.notes==null||b.site.notes.isEmpty()?"—":shorten(b.site.notes,72),124);labelValue(c,"الفترة",first.isEmpty()?"—":first+" → "+last,148);
        metric(c,42,185,170,82,"الجلسات",String.valueOf(plan.totalSessions),"نشطة "+active+" • منتهية "+closed,0xFFE3F2FD);metric(c,225,185,170,82,"Measurement Sessions",String.valueOf(measurements),"مرتبطة بجلسات مستقلة",0xFFF3E5F5);metric(c,408,185,170,82,"جلسات محللة",String.valueOf(plan.analyticalSessions),"Identified "+plan.identifiedSessions,0xFFE8F5E9);metric(c,591,185,170,82,"كمّي M4",String.valueOf(plan.quantifiedSessions),"PPM reportable per session",0xFFFFF3E0);
        metric(c,42,282,170,82,"144 session evidence",String.valueOf(plan.sessionsWithPositive144Evidence),"من "+plan.sessionsWith144+" جلسة لديها panel",0xFFE0F7FA);metric(c,225,282,170,82,"Open/Mixture/OOD",String.valueOf(plan.openSetOrMixtureSessions),"لا تُعامل كهويات مؤكدة",0xFFFFF3E0);metric(c,408,282,170,82,"قياس غير صالح",String.valueOf(plan.invalidSessions),"Quality-only",plan.invalidSessions>0?0xFFFFEBEE:0xFFF5F5F5);metric(c,591,282,170,82,"جلسات حرجة",String.valueOf(plan.criticalSessions),"مرجع صحي/LFL",plan.criticalSessions>0?0xFFFFEBEE:0xFFE8F5E9);
        text(c,"قراءة التقرير",42,405,11,Color.BLACK,true);float y=427;for(int i=0;i<Math.min(4,plan.findings.size());i++){text(c,"• "+shorten(plan.findings.get(i),125),42,y,8.2f,i==0&&plan.criticalSessions>0?0xFFC62828:Color.DKGRAY,false);y+=22;}
        text(c,"يقارن التقرير الجلسات ككيانات مستقلة ويحلل تكرار الدليل عبر 144 مادة. لا متوسط PPM بين الجلسات، ولا Tracking، ولا تحويل EST إلى إنذار مهني.",42,540,8,Color.DKGRAY,false);
    }

    private static void matrix(Canvas c,SiteBundle b,int chunk){
        int start=chunk*ROWS_PER_MATRIX_PAGE,end=Math.min(b.sessions.size(),start+ROWS_PER_MATRIX_PAGE);title(c,"مصفوفة جلسات الموقع "+(chunk+1),42);float y=76;
        text(c,"#",29,y,7.2f,Color.DKGRAY,true);text(c,"الجلسة",48,y,7.2f,Color.DKGRAY,true);text(c,"البدء",155,y,7.2f,Color.DKGRAY,true);text(c,"قياس",272,y,7.2f,Color.DKGRAY,true);text(c,"Outcome",309,y,7.2f,Color.DKGRAY,true);text(c,"المادة",390,y,7.2f,Color.DKGRAY,true);text(c,"Support",486,y,7.2f,Color.DKGRAY,true);text(c,"144+",541,y,7.2f,Color.DKGRAY,true);text(c,"الخطر",580,y,7.2f,Color.DKGRAY,true);text(c,"PPM",697,y,7.2f,Color.DKGRAY,true);text(c,"سلامة",774,y,7.2f,Color.DKGRAY,true);y+=18;
        if(start>=end){text(c,"لا توجد جلسات تحت هذا الموقع.",42,112,11,Color.DKGRAY,false);return;}
        for(int i=start;i<end;i++){SessionSummary s=b.sessions.get(i);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor((i&1)==0?0xFFF7F8FA:Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawRect(new RectF(26,y-13,816,y+7),p);JSONObject h=s.topHazard();String hazard=h==null?"—":shorten(h.optString("hazard_symbols","")+" "+hazardLabel(h),16);text(c,String.valueOf(i+1),29,y,6.5f,Color.BLACK,false);text(c,shorten(s.bundle.fieldSession.name,14),48,y,6.5f,Color.BLACK,false);text(c,shorten(s.bundle.fieldSession.startedTs,16),155,y,6.5f,Color.BLACK,false);text(c,String.valueOf(s.bundle.measurementSessionIds.size()),272,y,6.5f,Color.BLACK,false);text(c,shorten(s.outcome,11),309,y,6.5f,Color.BLACK,false);text(c,shorten(s.chemical,13),390,y,6.5f,Color.BLACK,false);text(c,pct100(s.analyticalConfidencePct),486,y,6.5f,Color.BLACK,false);text(c,String.valueOf(s.panelSupportedCount),541,y,6.5f,Color.BLACK,false);text(c,hazard,580,y,6.3f,hazardColor(h),false);text(c,s.ppmReportable&&Double.isFinite(s.ppm)?fmt(s.ppm):"—",697,y,6.5f,Color.BLACK,false);text(c,s.critical()?"⚠":s.safetyGate?"OK":"—",788,y,7.3f,s.critical()?0xFFC62828:Color.DKGRAY,true,Paint.Align.CENTER);y+=24;}
        text(c,"Support = positive-evidence index، وليس احتمالًا. خانة الخطر قد تستخدم EST للمرشح كسياق فقط؛ الإنذار الفعلي يبقى M4-gated.",42,535,7.2f,Color.DKGRAY,false);text(c,"PPM يبقى مرتبطًا بكل جلسة ولا يُجمع أو يُتوسط على مستوى الموقع.",42,551,7.2f,Color.DKGRAY,false);
    }

    private static void recurrence(Canvas c,SiteReportIntelligence.SitePlan plan){
        title(c,"تكرار الأدلة الكيميائية عبر الموقع",42);SiteReportCharts.recurrence144(c,new RectF(36,70,405,548),plan);SiteReportCharts.topRecurrence(c,new RectF(430,70,806,548),plan);
    }

    private static void priority(Canvas c,SiteBundle b,SiteReportIntelligence.SitePlan plan){
        title(c,"الأولوية التحليلية ومخاطر المرشحين",42);SiteReportCharts.sessionOutcomeBars(c,new RectF(36,70,405,272),plan);hazardContextTable(c,new RectF(430,70,806,548),b.sessions);text(c,"الجداول التقديرية تساعد على ترتيب ما يحتاج إلى تأكيد؛ لا تستبدل تقييم التعرض M4 ولا تُنشئ قرار امتثال.",36,542,7.6f,Color.DKGRAY,false);
    }

    private static void hazardContextTable(Canvas c,RectF r,List<SessionSummary> sessions){
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);bg.setColor(Color.WHITE);bg.setStyle(Paint.Style.FILL);c.drawRect(r,bg);Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setColor(0xFFCFD8DC);border.setStyle(Paint.Style.STROKE);c.drawRect(r,border);text(c,"أعلى سياق خطر مرشح في كل جلسة",r.left+9,r.top+21,11,Color.BLACK,true);
        List<SessionSummary> rows=new ArrayList<>();for(SessionSummary s:sessions)if(s.topHazard()!=null)rows.add(s);Collections.sort(rows,new Comparator<SessionSummary>(){public int compare(SessionSummary a,SessionSummary b){return Double.compare(hazardScore(b.topHazard()),hazardScore(a.topHazard()));}});int n=Math.min(12,rows.size());float y=r.top+46,rowH=(r.height()-72)/Math.max(1,n);if(n==0){text(c,"لا توجد سياقات خطر للمرشحين.",r.centerX(),r.centerY(),8.5f,Color.DKGRAY,false,Paint.Align.CENTER);return;}
        for(int i=0;i<n;i++){SessionSummary s=rows.get(i);JSONObject h=s.topHazard();JSONObject t=h.optJSONObject("toxicity"),f=h.optJSONObject("flammability");double tox=t==null?Double.NaN:t.optDouble("max_context_percent",Double.NaN),lfl=f==null?Double.NaN:f.optDouble("percent_lfl_context",Double.NaN);String basis="M4_MEASURED_REPORTABLE".equals(h.optString("concentration_basis",""))?"M4":"EST";text(c,(i+1)+". "+shorten(s.bundle.fieldSession.name,14),r.left+8,y,6.7f,Color.BLACK,true);text(c,shorten(h.optString("label",""),15)+" "+h.optString("hazard_symbols",""),r.left+104,y,6.7f,Color.BLACK,false);text(c,basis+" "+ppmText(h),r.left+220,y,6.3f,"M4".equals(basis)?0xFF1B5E20:0xFFEF6C00,false);text(c,"☠ "+pct100(tox)+" • 🔥 "+pct100(lfl),r.left+295,y,6.2f,hazardColor(h),false);y+=rowH;}
        text(c,"EST = screening-fit hazard context only",r.centerX(),r.bottom-9,6.5f,Color.DKGRAY,false,Paint.Align.CENTER);
    }

    private static double hazardScore(JSONObject h){if(h==null)return-1;JSONObject t=h.optJSONObject("toxicity"),f=h.optJSONObject("flammability");double a=t==null?Double.NaN:t.optDouble("max_context_percent",Double.NaN),b=f==null?Double.NaN:f.optDouble("percent_lfl_context",Double.NaN);double x=0;if(Double.isFinite(a))x=Math.max(x,a);if(Double.isFinite(b))x=Math.max(x,b);return x;}
    private static String hazardLabel(JSONObject h){if(h==null)return"";String p=h.optString("priority_class","");return p.replace("SCREENING_","").replace("M4_","").replace("_CONTEXT","").replace('_',' ');}
    private static int hazardColor(JSONObject h){double s=hazardScore(h);if(s>=100)return 0xFFC62828;if(s>=50)return 0xFFD84315;if(s>=20)return 0xFFEF6C00;if(s>=10)return 0xFFF9A825;return Color.DKGRAY;}
    private static String ppmText(JSONObject h){double p=h.optDouble("concentration_ppm",Double.NaN);return Double.isFinite(p)?fmt(p)+"ppm":"—";}

    private static void metric(Canvas c,float x,float y,float w,float h,String label,String value,String note,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),8,8,p);text(c,label,x+10,y+20,9,Color.DKGRAY,true);text(c,value,x+10,y+48,20,Color.BLACK,true);text(c,note,x+10,y+70,8,Color.DKGRAY,false);}
    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);}
    private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,9,Color.DKGRAY,true);text(c,v,190,y,10,Color.BLACK,false);}
    private static void chrome(Canvas c,int page,int total,SiteBundle b){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION+" • HR069",MARGIN,23,10,Color.BLACK,true);text(c,"تقرير الموقع • "+b.site.name,PAGE_WIDTH/2f,23,10,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String shorten(String s,int n){String q=s==null?"":s;return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
    private static String required(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("site required");return v;}
}
