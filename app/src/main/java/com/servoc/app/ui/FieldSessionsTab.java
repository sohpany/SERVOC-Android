package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.SiteSummaryExportActivity;
import com.servoc.app.field.FieldSessionRepository;

import java.util.List;

/** User-only site and field-session workspace. Developer/lab captures are intentionally absent. */
public final class FieldSessionsTab {
    private final HazRobHost host;
    private final FieldSessionRepository repo;
    private final ScrollView scroll;
    private final LinearLayout sites,sessions;
    private final TextView state,selectedSiteSummary;
    private final Button siteReportPdf,siteReportXlsx;
    private String selectedSiteId="";

    public FieldSessionsTab(Context c,HazRobHost host){
        this.host=host;this.repo=new FieldSessionRepository(host.database());
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"المواقع والجلسات"),UiKit.full(c));
        state=UiKit.text(c,"لا توجد جلسة ميدانية نشطة");state.setBackgroundColor(0xFF151A20);root.addView(state,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);
        Button addSite=UiKit.button(c,"إضافة موقع");addSite.setOnClickListener(v->ask(c,"اسم الموقع","",name->{try{selectedSiteId=repo.createSite(name,"");host.showToast("تم إنشاء الموقع");host.syncUserLibrary();refresh();}catch(Exception e){host.showToast("تعذر إنشاء الموقع");}}));actions.addView(addSite,UiKit.weight(1));
        Button addSession=UiKit.button(c,"بدء جلسة");addSession.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}ask(c,"اسم الجلسة","",name->{try{repo.startFieldSession(selectedSiteId,name);host.showToast("بدأت الجلسة الميدانية");host.syncUserLibrary();refresh();}catch(Exception e){host.showToast("تعذر بدء الجلسة: "+e.getMessage());}});});actions.addView(addSession,UiKit.weight(1));
        Button close=UiKit.button(c,"إنهاء الجلسة");close.setOnClickListener(v->{FieldSessionRepository.FieldSession a=repo.activeFieldSession();if(a==null){host.showToast("لا توجد جلسة نشطة");return;}repo.closeFieldSession(a.id);host.syncUserLibrary();host.showToast("تم إنهاء الجلسة");refresh();});actions.addView(close,UiKit.weight(1));
        root.addView(actions,UiKit.full(c));

        LinearLayout library=UiKit.card(c);library.addView(UiKit.title(c,"مكتبة HazRob الدائمة"),UiKit.full(c));Button folder=UiKit.button(c,"فتح / اختيار مجلد المكتبة");folder.setOnClickListener(v->host.requestUserLibraryFolder());library.addView(folder,UiKit.full(c));Button syncLibrary=UiKit.button(c,"مزامنة المكتبة");syncLibrary.setOnClickListener(v->host.syncUserLibrary());library.addView(syncLibrary,UiKit.full(c));Button restore=UiKit.button(c,"استعادة المواقع والجلسات");restore.setOnClickListener(v->host.restoreUserLibrary());library.addView(restore,UiKit.full(c));root.addView(library,UiKit.full(c));
        root.addView(UiKit.title(c,"مكتبة المواقع والجلسات"),UiKit.full(c));
        sites=UiKit.vbox(c);root.addView(sites,UiKit.full(c));
        selectedSiteSummary=UiKit.text(c,"اختر موقعًا");selectedSiteSummary.setBackgroundColor(0xFF151A20);root.addView(selectedSiteSummary,UiKit.full(c));
        LinearLayout siteExports=UiKit.hbox(c);
        siteReportPdf=UiKit.button(c,"تقرير الموقع PDF");siteReportPdf.setEnabled(false);siteReportPdf.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}SiteSummaryExportActivity.start(c,selectedSiteId,"pdf");});siteExports.addView(siteReportPdf,UiKit.weight(1));
        siteReportXlsx=UiKit.button(c,"Excel ذكي للموقع");siteReportXlsx.setEnabled(false);siteReportXlsx.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}SiteSummaryExportActivity.start(c,selectedSiteId,"xlsx");});siteExports.addView(siteReportXlsx,UiKit.weight(1));
        root.addView(siteExports,UiKit.full(c));
        sessions=UiKit.vbox(c);root.addView(sessions,UiKit.full(c));
    }

    public View view(){return scroll;}

    public void refresh(){
        FieldSessionRepository.FieldSession active=repo.activeFieldSession();
        state.setText((host.userLibraryAttached()?"المكتبة الدائمة: مرتبطة":"المكتبة الدائمة: غير مرتبطة")+"\n"+(active==null?"لا توجد جلسة ميدانية نشطة":"جلسة نشطة: "+active.name+" • القياسات الجديدة ستُربط بهذه الجلسة"));
        state.setTextColor(active==null?0xFFFFB74D:0xFF81C784);
        sites.removeAllViews();List<FieldSessionRepository.Site> ss=repo.sites();
        if(ss.isEmpty()){selectedSiteId="";siteReportPdf.setEnabled(false);siteReportXlsx.setEnabled(false);selectedSiteSummary.setText("لا توجد مواقع محفوظة.");sites.addView(UiKit.small(sites.getContext(),"لا توجد مواقع محفوظة."),UiKit.full(sites.getContext()));sessions.removeAllViews();return;}
        boolean exists=false;for(FieldSessionRepository.Site s:ss)if(s.id.equals(selectedSiteId))exists=true;if(!exists)selectedSiteId=ss.get(0).id;
        FieldSessionRepository.Site selected=null;
        for(FieldSessionRepository.Site s:ss){if(s.id.equals(selectedSiteId))selected=s;Button b=UiKit.button(sites.getContext(),(s.id.equals(selectedSiteId)?"● ":"")+s.name);b.setOnClickListener(v->{selectedSiteId=s.id;refresh();});sites.addView(b,UiKit.full(sites.getContext()));}
        List<FieldSessionRepository.FieldSession> children=repo.sessionsForSite(selectedSiteId);int measurementCount=0;for(FieldSessionRepository.FieldSession q:children)measurementCount+=repo.measurementSessionIds(q.id).size();selectedSiteSummary.setText("الموقع: "+(selected==null?selectedSiteId:selected.name)+"\nالجلسات: "+children.size()+" • القياسات الفنية: "+measurementCount+"\nتقارير الموقع تلخص الجلسات التابعة له دون دمج PPM بين الجلسات.");siteReportPdf.setEnabled(true);siteReportXlsx.setEnabled(true);
        sessions.removeAllViews();if(children.isEmpty()){sessions.addView(UiKit.small(sessions.getContext(),"لا توجد جلسات تحت هذا الموقع."),UiKit.full(sessions.getContext()));return;}
        for(FieldSessionRepository.FieldSession q:children){String status=q.active()?"نشطة":"منتهية";LinearLayout card=UiKit.card(sessions.getContext());TextView t=UiKit.text(sessions.getContext(),q.name+" • "+status+"\n"+q.startedTs+" • القياسات: "+repo.measurementSessionIds(q.id).size());t.setBackgroundColor(0xFF151A20);card.addView(t,UiKit.full(sessions.getContext()));LinearLayout ex=UiKit.hbox(sessions.getContext());Button p=UiKit.button(sessions.getContext(),"تقرير الجلسة PDF • 4–5 صفحات أفقية");p.setOnClickListener(v->host.requestFieldSessionExport("pdf",q.id));ex.addView(p,UiKit.weight(1.35f));Button x=UiKit.button(sessions.getContext(),"Excel ذكي للجلسة");x.setOnClickListener(v->host.requestFieldSessionExport("xlsx",q.id));ex.addView(x,UiKit.weight(.65f));card.addView(ex,UiKit.full(sessions.getContext()));sessions.addView(card,UiKit.full(sessions.getContext()));}
    }

    private interface NameResult{void accept(String name);}
    private static void ask(Context c,String title,String initial,NameResult result){EditText e=UiKit.input(c,initial);new AlertDialog.Builder(c).setTitle(title).setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())result.accept(n);}).show();}
}
