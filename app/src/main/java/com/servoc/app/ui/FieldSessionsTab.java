package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.SiteSummaryExportActivity;
import com.servoc.app.field.FieldSessionRepository;

import java.util.List;
import java.util.Map;

/** User-only site and field-session workspace. Developer/lab captures are intentionally absent. */
public final class FieldSessionsTab {
    private static final String TAG="HazRobFieldSessions";
    private boolean refreshing=false;
    private List<FieldSessionRepository.Site> displayedSites=new ArrayList<>();
    private List<FieldSessionRepository.FieldSession> displayedSessions=new ArrayList<>();
    private final HazRobHost host;
    private final FieldSessionRepository repo;
    private final ScrollView scroll;
    private final LinearLayout sites,sessions;
    private final Spinner siteDropdown,sessionDropdown;
    private boolean selectingScope=false;private String selectedSessionId="";
    private final TextView state,selectedSiteSummary;
    private final Button siteReportPdf,siteReportXlsx,deleteSite;
    private String selectedSiteId="";

    public FieldSessionsTab(Context c,HazRobHost host){
        this.host=host;this.repo=new FieldSessionRepository(host.database());
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(UiKit.NON_DRIVING_BG);scroll.addView(root);
        root.addView(UiKit.title(c,"المواقع والجلسات"),UiKit.full(c));
        state=UiKit.text(c,"لا توجد جلسة ميدانية نشطة");state.setBackgroundColor(UiKit.HAZ_SURFACE);root.addView(state,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);
        Button addSite=UiKit.button(c,"إضافة موقع");addSite.setOnClickListener(v->ask(c,"اسم الموقع","",name->{try{selectedSiteId=repo.createSite(name,"");selectedSessionId="";host.showToast("تم إنشاء الموقع");refresh();}catch(Exception e){host.showToast("تعذر إنشاء الموقع");}}));actions.addView(addSite,UiKit.weight(1));
        Button addSession=UiKit.button(c,"بدء جلسة");addSession.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}ask(c,"اسم الجلسة","",name->{try{selectedSessionId=repo.startFieldSession(selectedSiteId,name);host.showToast("بدأت الجلسة الميدانية");refresh();}catch(Exception e){host.showToast("تعذر بدء الجلسة: "+e.getMessage());}});});actions.addView(addSession,UiKit.weight(1));
        Button close=UiKit.button(c,"إنهاء الجلسة");close.setOnClickListener(v->{try{FieldSessionRepository.FieldSession a=repo.activeFieldSession();if(a==null){host.showToast("لا توجد جلسة نشطة");return;}repo.closeFieldSession(a.id);host.showToast("تم إنهاء الجلسة");refresh();}catch(RuntimeException e){Log.e(TAG,"Failed to end active session",e);host.showToast("تعذّر إنهاء الجلسة؛ لم يتم مسح البيانات");}});actions.addView(close,UiKit.weight(1));
        root.addView(actions,UiKit.full(c));

        TextView localNote=UiKit.small(c,"المواقع والجلسات محفوظة محليًا داخل التطبيق. لا تُرفع بيانات جلسات المستخدم إلى المكتبة العلمية المركزية. إدارة المزامنة والنشر محصورة في واجهات المطور.");localNote.setTextColor(UiKit.HAZ_MUTED);root.addView(localNote,UiKit.full(c));
        root.addView(UiKit.title(c,"مكتبة المواقع والجلسات"),UiKit.full(c));
        sites=UiKit.vbox(c);sites.setVisibility(View.GONE);
        root.addView(UiKit.small(c,"اختر موقعًا من القائمة"),UiKit.full(c));
        siteDropdown=new Spinner(c);siteDropdown.setBackgroundColor(0xFFECEFF1);root.addView(siteDropdown,UiKit.full(c));
        siteDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||refreshing||pos!=p.getSelectedItemPosition())return;
                List<FieldSessionRepository.Site> all=displayedSites;
                if(pos>=0&&pos<all.size()&&!all.get(pos).id.equals(selectedSiteId)){
                    selectedSiteId=all.get(pos).id;selectedSessionId="";refresh();
                }
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        selectedSiteSummary=UiKit.text(c,"اختر موقعًا");selectedSiteSummary.setBackgroundColor(UiKit.HAZ_SURFACE);root.addView(selectedSiteSummary,UiKit.full(c));
        LinearLayout siteExports=UiKit.hbox(c);
        siteReportPdf=UiKit.button(c,"تقرير الموقع PDF");siteReportPdf.setEnabled(false);siteReportPdf.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}host.openReportsForSite(selectedSiteId,"");});siteExports.addView(siteReportPdf,UiKit.weight(1));
        siteReportXlsx=UiKit.button(c,"Excel ذكي للموقع");siteReportXlsx.setEnabled(false);siteReportXlsx.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}host.openReportsForSite(selectedSiteId,"");});siteExports.addView(siteReportXlsx,UiKit.weight(1));
        root.addView(siteExports,UiKit.full(c));
        deleteSite=UiKit.button(c,"حذف الموقع الفارغ");deleteSite.setEnabled(false);
        deleteSite.setOnClickListener(v->{if(selectedSiteId.isEmpty())return;
            final String id=selectedSiteId;new AlertDialog.Builder(c)
                .setTitle("حذف الموقع").setMessage("سيُحذف الموقع الفارغ نهائيًا من مكتبة HazRob. يجب حذف جميع جلساته أولًا.")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف", (d,w)->host.deleteEmptyUserSite(id)).show();
        });root.addView(deleteSite,UiKit.full(c));
        root.addView(UiKit.small(c,"جلسات الموقع المحدد"),UiKit.full(c));
        sessionDropdown=new Spinner(c);sessionDropdown.setBackgroundColor(0xFFECEFF1);root.addView(sessionDropdown,UiKit.full(c));
        sessionDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||refreshing||pos!=p.getSelectedItemPosition())return;
                List<FieldSessionRepository.FieldSession> all=displayedSessions;
                if(pos>=0&&pos<all.size()&&!all.get(pos).id.equals(selectedSessionId)){
                    selectedSessionId=all.get(pos).id;refresh();
                }
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        sessions=UiKit.vbox(c);root.addView(sessions,UiKit.full(c));
    }

    public View view(){return scroll;}

    public void refresh(){
        if(refreshing)return;
        refreshing=true;
        try{render();}
        catch(RuntimeException e){
            Log.e(TAG,"Cannot refresh field sessions",e);
            state.setText("تعذر عرض المواقع والجلسات. بياناتك محفوظة؛ يرجى إعادة فتح التبويب أو مراجعة سجل التشخيص.");
            state.setTextColor(0xFFC62828);
            host.showToast("تعذر تحميل الجلسات: "+e.getClass().getSimpleName());
        }finally{selectingScope=false;refreshing=false;}
    }

    private void render(){
        FieldSessionRepository.FieldSession active=repo.activeFieldSession();
        state.setText("تخزين محلي • لا مزامنة مركزية"+"\n"+(active==null?"لا توجد جلسة ميدانية نشطة":"جلسة نشطة: "+active.name+" • القياسات الجديدة ستُربط بهذه الجلسة"));
        state.setTextColor(active==null?0xFF955000:0xFF1B5E20);
        sites.removeAllViews();List<FieldSessionRepository.Site> ss=repo.sites();displayedSites=new ArrayList<>(ss);
        if(ss.isEmpty()){selectedSiteId="";selectedSessionId="";displayedSessions=new ArrayList<>();selectingScope=true;siteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد مواقع")));sessionDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد جلسات")));selectingScope=false;siteReportPdf.setEnabled(false);siteReportXlsx.setEnabled(false);deleteSite.setEnabled(false);selectedSiteSummary.setText("لا توجد مواقع محفوظة.");sites.addView(UiKit.small(sites.getContext(),"لا توجد مواقع محفوظة."),UiKit.full(sites.getContext()));sessions.removeAllViews();return;}
        boolean exists=false;for(FieldSessionRepository.Site s:ss)if(s.id.equals(selectedSiteId))exists=true;if(!exists)selectedSiteId=ss.get(0).id;
        FieldSessionRepository.Site selected=null;int siteIndex=0;
        ArrayList<String> siteNames=new ArrayList<>();for(int i=0;i<ss.size();i++){FieldSessionRepository.Site s=ss.get(i);siteNames.add(s.name);if(s.id.equals(selectedSiteId)){selected=s;siteIndex=i;}}
        selectingScope=true;siteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),siteNames));siteDropdown.setSelection(siteIndex);siteDropdown.setEnabled(true);selectingScope=false;
        List<FieldSessionRepository.FieldSession> children=repo.sessionsForSite(selectedSiteId);displayedSessions=new ArrayList<>(children);
        Map<String,Integer> countBySession=repo.measurementCountsForSite(selectedSiteId);
        int measurementCount=0;for(Integer n:countBySession.values())measurementCount+=n;selectedSiteSummary.setText("الموقع: "+(selected==null?selectedSiteId:selected.name)+"\nالجلسات: "+children.size()+" • القياسات الفنية: "+measurementCount+"\nتقارير الموقع تلخص الجلسات التابعة له دون دمج PPM بين الجلسات.");siteReportPdf.setEnabled(true);siteReportXlsx.setEnabled(true);deleteSite.setEnabled(children.isEmpty());
        ArrayList<String> names=new ArrayList<>();int chosen=0;boolean found=false;
        for(int i=0;i<children.size();i++){FieldSessionRepository.FieldSession q=children.get(i);names.add(q.name+(q.active()?" • نشطة":" • منتهية"));if(q.id.equals(selectedSessionId)){found=true;chosen=i;}}
        if(!found)selectedSessionId=children.isEmpty()?"":children.get(0).id;
        selectingScope=true;
        sessionDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),children.isEmpty()?java.util.Arrays.asList("لا توجد جلسات"):names));
        sessionDropdown.setSelection(chosen);sessionDropdown.setEnabled(!children.isEmpty());selectingScope=false;
        sessions.removeAllViews();if(children.isEmpty()){sessions.addView(UiKit.small(sessions.getContext(),"لا توجد جلسات تحت هذا الموقع."),UiKit.full(sessions.getContext()));return;}
        for(FieldSessionRepository.FieldSession q:children){if(!q.id.equals(selectedSessionId))continue;String status=q.active()?"نشطة":"منتهية";LinearLayout card=UiKit.card(sessions.getContext());TextView t=UiKit.text(sessions.getContext(),q.name+" • "+status+"\n"+q.startedTs+" • القياسات: "+countBySession.getOrDefault(q.id,0));t.setBackgroundColor(UiKit.HAZ_SURFACE);card.addView(t,UiKit.full(sessions.getContext()));LinearLayout ex=UiKit.hbox(sessions.getContext());Button p=UiKit.button(sessions.getContext(),"تقرير الجلسة PDF • 4–5 صفحات أفقية");p.setOnClickListener(v->host.openReportsForSite(selectedSiteId,q.id));ex.addView(p,UiKit.weight(1.35f));Button x=UiKit.button(sessions.getContext(),"Excel ذكي للجلسة");x.setOnClickListener(v->host.openReportsForSite(selectedSiteId,q.id));ex.addView(x,UiKit.weight(.65f));card.addView(ex,UiKit.full(sessions.getContext()));
            Button remove=UiKit.button(sessions.getContext(),"حذف الجلسة وبياناتها");remove.setEnabled(!q.active());
            remove.setOnClickListener(v->new AlertDialog.Builder(sessions.getContext())
                .setTitle("حذف الجلسة نهائيًا").setMessage("ستُحذف الجلسة «"+q.name+"» وجميع قياساتها وعيناتها ونتائج ChemCore المرتبطة بها من التخزين المحلي للتطبيق وأي أرشيف محلي مرتبط. لا يمكن التراجع عن ذلك. التقارير المصدرة مسبقًا خارج التطبيق لن تُحذف.")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف نهائي",(d,w)->host.deleteUserFieldSession(q.id)).show());
            card.addView(remove,UiKit.full(sessions.getContext()));sessions.addView(card,UiKit.full(sessions.getContext()));}
    }

    private interface NameResult{void accept(String name);}
    private static void ask(Context c,String title,String initial,NameResult result){EditText e=UiKit.input(c,initial);new AlertDialog.Builder(c).setTitle(title).setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())result.accept(n);}).show();}
}
