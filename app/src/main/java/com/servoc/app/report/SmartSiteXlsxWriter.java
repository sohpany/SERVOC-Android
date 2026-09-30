package com.servoc.app.report;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Intelligent XLSX companion for one Site.
 * Cross-session summaries preserve Field Session independence: no pooled/averaged PPM is produced.
 */
final class SmartSiteXlsxWriter {
    private SmartSiteXlsxWriter(){}

    static void write(OutputStream out,SiteSummaryReportExporter.SiteBundle b)throws Exception{
        if(out==null||b==null)throw new IllegalArgumentException("output/site required");
        SiteReportIntelligence.SitePlan plan=SiteReportIntelligence.plan(b.sessions);
        List<SmartXlsxPackage.Sheet> sheets=new ArrayList<>();
        sheets.add(new SmartXlsxPackage.Sheet("الملخص التنفيذي",executive(b,plan)));
        sheets.add(new SmartXlsxPackage.Sheet("الجلسات",sessions(b)));
        sheets.add(new SmartXlsxPackage.Sheet("تكرار 144",recurrence(b,plan)));
        sheets.add(new SmartXlsxPackage.Sheet("سياق المخاطر",hazards(b)));
        sheets.add(new SmartXlsxPackage.Sheet("المنهجية والمصدر",methodology(b,plan)));
        SmartXlsxPackage.write(out,sheets);
    }

    private static String executive(SiteSummaryReportExporter.SiteBundle b,SiteReportIntelligence.SitePlan p){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(28,32,55);
        identity(g,b,"HazRob HR069 — التقرير الذكي للموقع");g.blank();g.section("المؤشرات التنفيذية");g.header("المؤشر","القيمة","الدلالة");
        g.row("Field Sessions",p.totalSessions,"كل جلسة تبقى كيانًا مستقلاً");
        g.row("Analytical Sessions",p.analyticalSessions,"جلسات لديها ChemCore محفوظ");
        g.row("Identified",p.identifiedSessions,"هويات مقبولة وفق بوابات القرار");
        g.row("Quantified M4",p.quantifiedSessions,"PPM قابل للتقرير داخل الجلسة فقط");
        g.row("Critical Sessions",p.criticalSessions,"Safety gate صالح ومرجع صحي/LFL حرج");
        g.row("Open/Mixture/OOD",p.openSetOrMixtureSessions,"لا تعامل كهويات مؤكدة");
        g.row("Invalid Measurements",p.invalidSessions,"Quality-only");
        g.row("Sessions with 144",p.sessionsWith144,"جلسات تحتوي مصفوفة 144");
        g.row("Sessions with positive 144 evidence",p.sessionsWithPositive144Evidence,"دليل جلسة إيجابي وليس إثبات وجود شامل");
        g.blank();g.section("ملاحظات المخطط");g.header("#","الملاحظة");int i=1;for(String f:p.findings)g.row(i++,f);
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"قاعدة كمية","لا يتم حساب pooled PPM أو average PPM عبر Field Sessions مستقلة.");
        g.rowStyled(SmartXlsxPackage.WARN,"قاعدة EST","screening-fit ppm وسياق المرشحين لا ينشئان حكم تعرض أو LFL alarm دون بوابة M4.");
        return g.finish();
    }

    private static String sessions(SiteSummaryReportExporter.SiteBundle b){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(6,26,34,24,24,18,18,18,15,15,16,18,18,18,18);
        identity(g,b,"مصفوفة Field Sessions");g.blank();
        g.header("#","Field Session","Field Session ID","Started","Status","Measurement Sessions","Outcome","Chemical","Evidence tier","Support %","144 supported","144 accepted","PPM reportable","PPM","Safety gate");
        int i=1;for(SiteSummaryReportExporter.SessionSummary s:b.sessions){
            g.row(i++,s.bundle.fieldSession.name,s.bundle.fieldSession.id,s.bundle.fieldSession.startedTs,s.bundle.fieldSession.status,s.bundle.measurementSessionIds.size(),s.outcome,s.chemical,s.evidenceTier,finite(s.analyticalConfidencePct),s.panelSupportedCount,s.panelAcceptedCount,s.ppmReportable,s.ppmReportable&&Double.isFinite(s.ppm)?s.ppm:null,s.safetyGate);
        }
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"تفسير","PPM في هذا الجدول قيمة جلسة مستقلة فقط؛ لا تجمع ولا تتوسط على مستوى الموقع.");
        return g.finish();
    }

    private static String recurrence(SiteSummaryReportExporter.SiteBundle b,SiteReportIntelligence.SitePlan p){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(8,36,18,18,18,20,20);
        identity(g,b,"تكرار دليل مصفوفة 144 عبر الجلسات");g.blank();
        g.header("Index","Material","Supported sessions","Accepted sessions","Quantified sessions","Max support %","Median support %");
        for(SiteReportIntelligence.MaterialRecurrence m:p.materials)g.row(m.index+1,m.name,m.supportedSessions,m.acceptedSessions,m.quantifiedSessions,finite(m.maxSupportPct),finite(m.medianSupportPct));
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"قاعدة تفسير","التكرار يلخص وجود دليل إيجابي عبر جلسات مستقلة؛ لا يدمج time-series ولا يثبت غياب مادة عند عدم وجود الدعم.");
        return g.finish();
    }

    private static String hazards(SiteSummaryReportExporter.SiteBundle b){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(7,27,25,7,30,16,16,18,18,20,28);
        identity(g,b,"أعلى سياق خطر مرشح لكل Field Session");g.blank();
        g.header("#","Field Session","Candidate","Rank","Basis","ppm","Toxicity % ref","%LFL","Symbols","Priority","Interpretation");
        int i=1;for(SiteSummaryReportExporter.SessionSummary s:b.sessions){
            JSONObject h=s.priorityHazard();if(h==null)continue;JSONObject t=h.optJSONObject("toxicity"),f=h.optJSONObject("flammability");String basis=h.optString("concentration_basis","");boolean m4="M4_MEASURED_REPORTABLE".equals(basis)&&h.optBoolean("occupational_interpretation_permitted",false);
            g.row(i++,s.bundle.fieldSession.name,h.optString("label",""),h.optInt("rank",0),basis,number(h,"concentration_ppm"),t==null?null:number(t,"max_context_percent"),f==null?null:number(f,"percent_lfl_context"),h.optString("hazard_symbols",""),h.optString("priority_class",""),m4?"M4 reportable context":"EST/context only");
        }
        g.blank();g.rowStyled(SmartXlsxPackage.WARN,"تنبيه","EST هو سياق ترتيب للمراجعة فقط، ولا يعد تجاوزًا مهنيًا أو إنذار اشتعال.");
        return g.finish();
    }

    private static String methodology(SiteSummaryReportExporter.SiteBundle b,SiteReportIntelligence.SitePlan p){
        SmartXlsxPackage.Grid g=new SmartXlsxPackage.Grid(31,70);
        identity(g,b,"المنهجية ومصدر القرار");g.blank();g.header("البند","القيمة");
        g.row("Hierarchy","Site → Field Sessions → Measurement Sessions");
        g.row("Cross-session rule","Field Sessions are summarized but independent technical time series are never synthetically merged.");
        g.row("Concentration rule","No pooled PPM and no average PPM across sessions.");
        g.row("144 rule","Session-specific positive evidence is summarized as recurrence; absence of evidence is not evidence of absence.");
        g.row("Candidate hazard rule","Estimated screening-fit ppm is contextual only; occupational interpretation requires the M4 gate.");
        g.row("Compliance determination","NOT_MADE");
        g.row("Operational Tracking","Excluded from reports");
        g.row("Planner rationale",p.rationale);
        g.row("Workbook schema","hazrob-smart-site-xlsx/1");
        return g.finish();
    }

    private static void identity(SmartXlsxPackage.Grid g,SiteSummaryReportExporter.SiteBundle b,String title){
        g.title(title);g.row("Site",b.site.name);g.row("Site ID",b.site.id);g.row("Site notes",b.site.notes==null?"":b.site.notes);g.row("Field Session count",b.sessions.size());
    }
    private static Double finite(double v){return Double.isFinite(v)?v:null;}
    private static Double number(JSONObject q,String key){if(q==null)return null;double v=q.optDouble(key,Double.NaN);return Double.isFinite(v)?v:null;}
}
