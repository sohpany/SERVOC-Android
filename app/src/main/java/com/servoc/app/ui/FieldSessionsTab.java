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
import java.util.HashSet;
import java.util.Set;

/** User-only site and field-session workspace. Developer/lab captures are intentionally absent. */
public final class FieldSessionsTab {
    private static final String TAG="HazRobFieldSessions";
    private boolean refreshing=false;
    private List<FieldSessionRepository.Site> displayedSites=new ArrayList<>();
    private List<FieldSessionRepository.FieldSession> displayedSessions=new ArrayList<>();
    private final HazRobHost host;
    private final FieldSessionRepository repo;
    private final ScrollView scroll;
    private final LinearLayout sites,sessions,adminPane,reportPane;
    private final Spinner reportSiteDropdown,reportApprovedDropdown;
    private final TextView reportScope;
    private final Button reportPdf,reportXlsx,managementTab,reportTab;
    private boolean reportView=false;
    private String reportApprovedId="";
    private List<FieldSessionRepository.FieldSession> approvedChoices=new ArrayList<>();
    private final Spinner siteDropdown,sessionDropdown;
    private boolean selectingScope=false;private String selectedSessionId="";
    private final TextView state,selectedSiteSummary;
    private final Button siteReportPdf,siteReportXlsx,deleteSite;
    private String selectedSiteId="";

    public FieldSessionsTab(Context c,HazRobHost host){
        this.host=host;this.repo=new FieldSessionRepository(host.database());
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(UiKit.NON_DRIVING_BG);scroll.addView(root);
        LinearLayout paneTabs=UiKit.hbox(c);
        managementTab=UiKit.button(c,"الإدارة");managementTab.setOnClickListener(v->switchPane(false));
        reportTab=UiKit.button(c,"التقارير");reportTab.setOnClickListener(v->switchPane(true));
        paneTabs.addView(managementTab,UiKit.weight(1));paneTabs.addView(reportTab,UiKit.weight(1));
        root.addView(paneTabs,UiKit.full(c));
        adminPane=UiKit.vbox(c);root.addView(adminPane,UiKit.full(c));
        reportPane=UiKit.vbox(c);root.addView(reportPane,UiKit.full(c));
        reportPane.setVisibility(View.GONE);UiKit.selected(managementTab,true);UiKit.selected(reportTab,false);
        adminPane.addView(UiKit.title(c,"إدارة المواقع والجلسات"),UiKit.full(c));
        state=UiKit.text(c,"لا توجد جلسة ميدانية نشطة");state.setBackgroundColor(UiKit.HAZ_SURFACE);adminPane.addView(state,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);
        Button addSite=UiKit.button(c,"إضافة موقع");addSite.setOnClickListener(v->ask(c,"اسم الموقع","",name->{try{selectedSiteId=repo.createSite(name,"");selectedSessionId="";host.showToast("تم إنشاء الموقع");refresh();}catch(Exception e){host.showToast("تعذر إنشاء الموقع");}}));actions.addView(addSite,UiKit.weight(1));
        Button addSession=UiKit.button(c,"بدء جلسة");addSession.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}ask(c,"اسم الجلسة","",name->{try{selectedSessionId=repo.startFieldSession(selectedSiteId,name);host.showToast("بدأت الجلسة الميدانية");refresh();}catch(Exception e){host.showToast("تعذر بدء الجلسة: "+e.getMessage());}});});actions.addView(addSession,UiKit.weight(1));
        Button close=UiKit.button(c,"إنهاء الجلسة");close.setOnClickListener(v->{try{FieldSessionRepository.FieldSession a=repo.activeFieldSession();if(a==null){host.showToast("لا توجد جلسة نشطة");return;}repo.closeFieldSession(a.id);host.showToast("تم إنهاء الجلسة");refresh();}catch(RuntimeException e){Log.e(TAG,"Failed to end active session",e);host.showToast("تعذّر إنهاء الجلسة؛ لم يتم مسح البيانات");}});actions.addView(close,UiKit.weight(1));
        adminPane.addView(actions,UiKit.full(c));

        TextView localNote=UiKit.small(c,"المواقع والجلسات محفوظة محليًا داخل التطبيق. لا تُرفع بيانات جلسات المستخدم إلى المكتبة العلمية المركزية. إدارة المزامنة والنشر محصورة في واجهات المطور.");localNote.setTextColor(UiKit.HAZ_MUTED);adminPane.addView(localNote,UiKit.full(c));
        adminPane.addView(UiKit.title(c,"مكتبة المواقع والجلسات"),UiKit.full(c));
        sites=UiKit.vbox(c);sites.setVisibility(View.GONE);
        adminPane.addView(UiKit.small(c,"اختر موقعًا من القائمة"),UiKit.full(c));
        siteDropdown=new Spinner(c);siteDropdown.setBackgroundColor(0xFFECEFF1);adminPane.addView(siteDropdown,UiKit.full(c));
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
        selectedSiteSummary=UiKit.text(c,"اختر موقعًا");selectedSiteSummary.setBackgroundColor(UiKit.HAZ_SURFACE);adminPane.addView(selectedSiteSummary,UiKit.full(c));
        LinearLayout siteExports=UiKit.hbox(c);
        siteReportPdf=UiKit.button(c,"تقرير الموقع PDF");siteReportPdf.setEnabled(false);siteReportPdf.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}host.openReportsForSite(selectedSiteId,"");});siteExports.addView(siteReportPdf,UiKit.weight(1));
        siteReportXlsx=UiKit.button(c,"Excel ذكي للموقع");siteReportXlsx.setEnabled(false);siteReportXlsx.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}host.openReportsForSite(selectedSiteId,"");});siteExports.addView(siteReportXlsx,UiKit.weight(1));
        adminPane.addView(siteExports,UiKit.full(c));
        deleteSite=UiKit.button(c,"حذف الموقع الفارغ");deleteSite.setEnabled(false);
        deleteSite.setOnClickListener(v->{if(selectedSiteId.isEmpty())return;
            final String id=selectedSiteId;new AlertDialog.Builder(c)
                .setTitle("حذف الموقع").setMessage("سيُحذف الموقع الفارغ نهائيًا من مكتبة HazRob. يجب حذف جميع جلساته أولًا.")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف", (d,w)->host.deleteEmptyUserSite(id)).show();
        });adminPane.addView(deleteSite,UiKit.full(c));
        adminPane.addView(UiKit.small(c,"جلسات الموقع المحدد"),UiKit.full(c));
        sessionDropdown=new Spinner(c);sessionDropdown.setBackgroundColor(0xFFECEFF1);adminPane.addView(sessionDropdown,UiKit.full(c));
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
        sessions=UiKit.vbox(c);adminPane.addView(sessions,UiKit.full(c));

        reportPane.addView(UiKit.title(c,"تقارير المواقع والجلسات المعتمدة"),UiKit.full(c));
        TextView guidance=UiKit.small(c,"حدد موقعًا ثم جميع جلساته المعتمدة أو جلسة معتمدة واحدة. الجلسات الجديدة لا تظهر في تقارير الاعتماد.");
        reportPane.addView(guidance,UiKit.full(c));
        reportPane.addView(UiKit.small(c,"الموقع"),UiKit.full(c));
        reportSiteDropdown=new Spinner(c);reportSiteDropdown.setBackgroundColor(UiKit.HAZ_SURFACE_ALT);
        reportPane.addView(reportSiteDropdown,UiKit.full(c));
        reportSiteDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||refreshing||pos<0||pos>=displayedSites.size())return;
                String siteId=displayedSites.get(pos).id;
                if(!siteId.equals(selectedSiteId)){selectedSiteId=siteId;selectedSessionId="";reportApprovedId="";refresh();}
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        reportPane.addView(UiKit.small(c,"الجلسة المعتمدة / نطاق التقرير"),UiKit.full(c));
        reportApprovedDropdown=new Spinner(c);reportApprovedDropdown.setBackgroundColor(UiKit.HAZ_SURFACE_ALT);
        reportPane.addView(reportApprovedDropdown,UiKit.full(c));
        reportApprovedDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||refreshing)return;
                reportApprovedId=pos>0&&pos<=approvedChoices.size()?approvedChoices.get(pos-1).id:"";
                updateReportScope();
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        reportScope=UiKit.text(c,"لا توجد جلسة معتمدة");reportScope.setBackgroundColor(UiKit.HAZ_SURFACE_ALT);
        reportPane.addView(reportScope,UiKit.full(c));
        LinearLayout reportActions=UiKit.hbox(c);
        reportPdf=UiKit.button(c,"تصدير PDF");reportPdf.setOnClickListener(v->exportApproved("pdf"));
        reportXlsx=UiKit.button(c,"تصدير Excel");reportXlsx.setOnClickListener(v->exportApproved("xlsx"));
        reportActions.addView(reportPdf,UiKit.weight(1));reportActions.addView(reportXlsx,UiKit.weight(1));
        reportPane.addView(reportActions,UiKit.full(c));
    }

    public View view(){return scroll;}

    private void switchPane(boolean reports){
        reportView=reports;adminPane.setVisibility(reports?View.GONE:View.VISIBLE);
        reportPane.setVisibility(reports?View.VISIBLE:View.GONE);
        UiKit.selected(managementTab,!reports);UiKit.selected(reportTab,reports);
        refresh();
    }

    private void exportApproved(String format){
        if(selectedSiteId.isEmpty()||approvedChoices.isEmpty()){host.showToast("لا توجد جلسات معتمدة للتصدير");return;}
        if(reportApprovedId.isEmpty())SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,format);
        else if(repo.isApproved(reportApprovedId))host.requestFieldSessionExport(format,reportApprovedId);
        else host.showToast("هذه الجلسة لم تعد معتمدة");
    }

    private void updateReportScope(){
        boolean allowed=!selectedSiteId.isEmpty()&&!approvedChoices.isEmpty();
        reportPdf.setEnabled(allowed);reportXlsx.setEnabled(allowed);
        reportScope.setText(!allowed?"لا توجد جلسات معتمدة في الموقع المحدد. لا يُصدر تقرير عن جلسات جديدة أو غير مكتملة.":
            reportApprovedId.isEmpty()?"تقرير الموقع • "+approvedChoices.size()+" جلسة معتمدة (دون دمج PPM)":
            "تقرير الجلسة المعتمدة • "+reportApprovedId);
    }

    private void renderReportScopes(List<FieldSessionRepository.Site> sites){
        selectingScope=true;
        try{
            if(sites.isEmpty()){
                approvedChoices=new ArrayList<>();reportApprovedId="";
                reportSiteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد مواقع")));
                reportApprovedDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد جلسات معتمدة")));
                reportSiteDropdown.setEnabled(false);reportApprovedDropdown.setEnabled(false);
                updateReportScope();return;
            }
            ArrayList<String> siteNames=new ArrayList<>();int siteIndex=0;
            for(int i=0;i<sites.size();i++){
                FieldSessionRepository.Site item=sites.get(i);siteNames.add(item.name);
                if(item.id.equals(selectedSiteId))siteIndex=i;
            }
            reportSiteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),siteNames));
            reportSiteDropdown.setSelection(siteIndex);reportSiteDropdown.setEnabled(true);
            approvedChoices=repo.approvedSessionsForSite(selectedSiteId);
            ArrayList<String> labels=new ArrayList<>();labels.add("كل الجلسات المعتمدة • تقرير الموقع");
            int approvedIndex=0;
            for(int i=0;i<approvedChoices.size();i++){
                FieldSessionRepository.FieldSession item=approvedChoices.get(i);
                labels.add(item.name+" • معتمدة");if(item.id.equals(reportApprovedId))approvedIndex=i+1;
            }
            reportApprovedId=approvedIndex>0?approvedChoices.get(approvedIndex-1).id:"";
            reportApprovedDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),labels));
            reportApprovedDropdown.setSelection(approvedIndex);reportApprovedDropdown.setEnabled(!approvedChoices.isEmpty());
            updateReportScope();
        }finally{selectingScope=false;}
    }

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
        if(ss.isEmpty()){selectedSiteId="";selectedSessionId="";displayedSessions=new ArrayList<>();selectingScope=true;siteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد مواقع")));sessionDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),java.util.Arrays.asList("لا توجد جلسات")));selectingScope=false;siteReportPdf.setEnabled(false);siteReportXlsx.setEnabled(false);deleteSite.setEnabled(false);selectedSiteSummary.setText("لا توجد مواقع محفوظة.");sites.addView(UiKit.small(sites.getContext(),"لا توجد مواقع محفوظة."),UiKit.full(sites.getContext()));sessions.removeAllViews();renderReportScopes(ss);return;}
        boolean exists=false;for(FieldSessionRepository.Site s:ss)if(s.id.equals(selectedSiteId))exists=true;if(!exists)selectedSiteId=ss.get(0).id;
        FieldSessionRepository.Site selected=null;int siteIndex=0;
        ArrayList<String> siteNames=new ArrayList<>();for(int i=0;i<ss.size();i++){FieldSessionRepository.Site s=ss.get(i);siteNames.add(s.name);if(s.id.equals(selectedSiteId)){selected=s;siteIndex=i;}}
        selectingScope=true;siteDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),siteNames));siteDropdown.setSelection(siteIndex);siteDropdown.setEnabled(true);selectingScope=false;
        List<FieldSessionRepository.FieldSession> children=repo.sessionsForSite(selectedSiteId);displayedSessions=new ArrayList<>(children);
        renderReportScopes(ss);
        Set<String> approvedIds=new HashSet<>();for(FieldSessionRepository.FieldSession approved:approvedChoices)approvedIds.add(approved.id);
        Map<String,Integer> countBySession=repo.measurementCountsForSite(selectedSiteId);
        int measurementCount=0;for(Integer n:countBySession.values())measurementCount+=n;selectedSiteSummary.setText("الموقع: "+(selected==null?selectedSiteId:selected.name)+"\nالجلسات: "+children.size()+" • القياسات الفنية: "+measurementCount+"\nتقارير الموقع تلخص الجلسات التابعة له دون دمج PPM بين الجلسات.");siteReportPdf.setEnabled(!approvedChoices.isEmpty());siteReportXlsx.setEnabled(!approvedChoices.isEmpty());deleteSite.setEnabled(children.isEmpty());
        ArrayList<String> names=new ArrayList<>();int chosen=0;boolean found=false;
        for(int i=0;i<children.size();i++){FieldSessionRepository.FieldSession q=children.get(i);names.add(q.name+(q.active()?" • نشطة":" • منتهية"));if(q.id.equals(selectedSessionId)){found=true;chosen=i;}}
        if(!found)selectedSessionId=children.isEmpty()?"":children.get(0).id;
        selectingScope=true;
        sessionDropdown.setAdapter(UiKit.readableOptions(scroll.getContext(),children.isEmpty()?java.util.Arrays.asList("لا توجد جلسات"):names));
        sessionDropdown.setSelection(chosen);sessionDropdown.setEnabled(!children.isEmpty());selectingScope=false;
        sessions.removeAllViews();
        if(children.isEmpty()){
            sessions.addView(UiKit.small(sessions.getContext(),"لا توجد جلسات تحت هذا الموقع."),UiKit.full(sessions.getContext()));
            return;
        }
        LinearLayout heading=UiKit.hbox(sessions.getContext());
        TextView hName=UiKit.text(sessions.getContext(),"اسم الجلسة");
        TextView hStatus=UiKit.text(sessions.getContext(),"الحالة");
        hName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);hStatus.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hName.setBackgroundColor(UiKit.HAZ_YELLOW);hStatus.setBackgroundColor(UiKit.HAZ_YELLOW);
        heading.addView(hName,UiKit.weight(2f));heading.addView(hStatus,UiKit.weight(1f));
        sessions.addView(heading,UiKit.full(sessions.getContext()));
        for(FieldSessionRepository.FieldSession q:children){
            final boolean approved=approvedIds.contains(q.id);
            LinearLayout row=UiKit.hbox(sessions.getContext());
            Button name=UiKit.button(sessions.getContext(),(q.id.equals(selectedSessionId)?"● ":"")+q.name);
            UiKit.selected(name,q.id.equals(selectedSessionId));
            name.setOnClickListener(v->{selectedSessionId=q.id;refresh();});
            TextView statusText=UiKit.text(sessions.getContext(),approved?"معتمدة":"جديدة");
            statusText.setTextColor(approved?0xFF1B5E20:0xFF955000);
            statusText.setBackgroundColor(q.id.equals(selectedSessionId)?0xFFFFF0CB:UiKit.HAZ_SURFACE);
            row.addView(name,UiKit.weight(2f));row.addView(statusText,UiKit.weight(1f));
            sessions.addView(row,UiKit.full(sessions.getContext()));
        }
        FieldSessionRepository.FieldSession chosenSession=null;
        for(FieldSessionRepository.FieldSession q:children)if(q.id.equals(selectedSessionId)){chosenSession=q;break;}
        if(chosenSession==null)return;
        final FieldSessionRepository.FieldSession selectedSession=chosenSession;
        final boolean approved=approvedIds.contains(selectedSession.id);
        TextView detail=UiKit.small(sessions.getContext(),
            "الجلسة المحددة: "+selectedSession.name+" • "+(approved?"معتمدة":"جديدة")+
            "\n"+(selectedSession.active()?"نشطة":"منتهية")+" • "+selectedSession.startedTs+
            "\nالقياسات المرتبطة: "+countBySession.getOrDefault(selectedSession.id,0));
        detail.setBackgroundColor(UiKit.HAZ_SURFACE_ALT);sessions.addView(detail,UiKit.full(sessions.getContext()));
        Button approve=UiKit.button(sessions.getContext(),approved?"تم اعتماد الجلسة":"اعتماد الجلسة المكتملة");
        approve.setEnabled(!approved&&repo.canApprove(selectedSession.id));
        approve.setOnClickListener(v->new AlertDialog.Builder(sessions.getContext())
            .setTitle("اعتماد جلسة ميدانية")
            .setMessage("سيُعتمد قياس مستخدم واحد مكتمل لهذه الجلسة، ويصبح متاحًا لتقارير PDF وExcel. لا يتم إثبات هوية مادة أو دقة PPM بهذا الإجراء.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("اعتماد",(d,w)->{
                try{if(repo.approve(selectedSession.id)){host.showToast("تم اعتماد الجلسة");refresh();}
                    else host.showToast("الاعتماد يتطلب قياسًا واحدًا مكتملًا مع إنهاء الجلسة");}
                catch(RuntimeException e){Log.e(TAG,"Approve failed",e);host.showToast("تعذر اعتماد الجلسة؛ البيانات محفوظة");}
            }).show());
        sessions.addView(approve,UiKit.full(sessions.getContext()));
        if(!approved&&!selectedSession.active()){
            Button retry=UiKit.button(sessions.getContext(),"إعادة القياس (استبدال المحاولات غير المعتمدة)");
            retry.setOnClickListener(v->new AlertDialog.Builder(sessions.getContext())
                .setTitle("إعادة القياس")
                .setMessage("سيتم حذف القياسات التجريبية غير المعتمدة لهذه الجلسة من التطبيق واستئناف جمع قياس جديد. لا يمس هذا الإجراء قياسات المطور أو البيانات المرجعية، ولا يمكن التراجع عنه.")
                .setNegativeButton("إلغاء",null).setPositiveButton("إعادة القياس",(d,w)->{
                    try{
                        if(repo.restartUnapproved(selectedSession.id)){host.showToast("أعيد فتح الجلسة لقياس جديد");refresh();}
                        else host.showToast("تعذر إعادة فتح الجلسة؛ تحقق من وجود جلسة نشطة");
                    }catch(RuntimeException e){Log.e(TAG,"Retry blocked",e);host.showToast("لم يتم استبدال القياسات: توجد بيانات محمية");}
                }).show());
            sessions.addView(retry,UiKit.full(sessions.getContext()));
        }
        if(!approved){
            TextView note=UiKit.small(sessions.getContext(),
                "تبقى الحالة «جديدة» حتى تكتمل جلسة قياس واحدة ويعتمدها المستخدم. لا تُنشأ تقارير نهائية من البيانات غير المعتمدة.");
            sessions.addView(note,UiKit.full(sessions.getContext()));
        }
        Button remove=UiKit.button(sessions.getContext(),"حذف الجلسة وبياناتها");
        remove.setEnabled(!selectedSession.active());
        remove.setOnClickListener(v->new AlertDialog.Builder(sessions.getContext())
            .setTitle("حذف الجلسة نهائيًا")
            .setMessage("سيُحذف سجل «"+selectedSession.name+"» وقياساته المحلية نهائيًا، بما فيها اعتماد التقرير إن وجد. لا يمكن التراجع عن ذلك.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حذف نهائي",(d,w)->host.deleteUserFieldSession(selectedSession.id)).show());
        sessions.addView(remove,UiKit.full(sessions.getContext()));
    }

    private interface NameResult{void accept(String name);}
    private static void ask(Context c,String title,String initial,NameResult result){EditText e=UiKit.input(c,initial);new AlertDialog.Builder(c).setTitle(title).setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())result.accept(n);}).show();}
}
