package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.BuildConfig;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalExperimentStatusRepository;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemcore.library.ChemCoreLibraryManifest;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Developer dashboard for the canonical HazRob scientific library and its distribution channel. */
public final class CentralLibraryDashboardTab {
    private final Context context;
    private final HazRobHost host;
    private final ChemCoreLibraryManager manager;
    private final ChemicalExperimentStatusRepository statusRepo;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LinearLayout root,bar;
    private final FrameLayout page;
    private final View dashboardView,distributionView,platformsView,changelogView;
    private final Button[] tabs=new Button[4];
    private final TextView versionText,coverageText,distributionText,updateTelemetryText,operationText,platformText,changelogText;
    private int selected=0;

    public CentralLibraryDashboardTab(Context context,HazRobHost host){
        this.context=context.getApplicationContext();this.host=host;this.manager=new ChemCoreLibraryManager(context);this.statusRepo=new ChemicalExperimentStatusRepository(host.database());
        root=UiKit.vbox(context);root.setBackgroundColor(0xFF0E1116);root.setPadding(UiKit.dp(context,4),UiKit.dp(context,4),UiKit.dp(context,4),UiKit.dp(context,8));
        bar=UiKit.hbox(context);String[] names={"لوحة المكتبة","التوزيع","المنصات","السجل"};for(int i=0;i<names.length;i++){final int x=i;tabs[i]=UiKit.button(context,names[i]);tabs[i].setTextSize(9);tabs[i].setOnClickListener(v->show(x));bar.addView(tabs[i],UiKit.weight(1));}root.addView(bar,UiKit.full(context));page=new FrameLayout(context);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));

        ScrollView dashboard=scroll(context);LinearLayout d=(LinearLayout)dashboard.getChildAt(0);d.addView(UiKit.title(context,"المكتبة المركزية • HazRob"),UiKit.full(context));TextView canon=UiKit.small(context,"المصدر العلمي المرجعي: HazRob/build-harness • التحديث للمستخدم قراءة فقط • R0 لا تدخل في المكتبة المركزية.");canon.setTextColor(0xFF81C784);canon.setBackgroundColor(0xFF13251A);d.addView(canon,UiKit.full(context));LinearLayout vcard=UiKit.card(context);vcard.addView(UiKit.title(context,"الإصدار والمراجعات"),UiKit.full(context));versionText=box(context);vcard.addView(versionText,UiKit.full(context));d.addView(vcard,UiKit.full(context));LinearLayout ccard=UiKit.card(context);ccard.addView(UiKit.title(context,"تغطية المكتبة"),UiKit.full(context));coverageText=box(context);ccard.addView(coverageText,UiKit.full(context));d.addView(ccard,UiKit.full(context));dashboardView=dashboard;

        ScrollView distribution=scroll(context);LinearLayout u=(LinearLayout)distribution.getChildAt(0);u.addView(UiKit.title(context,"التحديث والتوزيع"),UiKit.full(context));distributionText=box(context);u.addView(distributionText,UiKit.full(context));Button check=UiKit.button(context,"فحص قناة GitHub للقراءة");check.setOnClickListener(v->checkDistribution());u.addView(check,UiKit.full(context));Button rollback=UiKit.button(context,"الرجوع للنسخة السابقة المتحققة");rollback.setOnClickListener(v->rollback());u.addView(rollback,UiKit.full(context));updateTelemetryText=box(context);u.addView(updateTelemetryText,UiKit.full(context));operationText=UiKit.small(context,"جاهز");operationText.setBackgroundColor(0xFF151A20);u.addView(operationText,UiKit.full(context));distributionView=distribution;

        ScrollView platforms=scroll(context);LinearLayout p=(LinearLayout)platforms.getChildAt(0);p.addView(UiKit.title(context,"المنصات والبنية السحابية"),UiKit.full(context));TextView note=UiKit.small(context,"هذه الصفحة للقراءة فقط. لا تُخزن مفاتيح Railway/Cloudflare/MQTT/Render/GitHub داخل APK؛ الأرصدة تُستورد من Admin Metrics API عندما تتوفر.");note.setTextColor(0xFFFFB74D);p.addView(note,UiKit.full(context));platformText=box(context);p.addView(platformText,UiKit.full(context));Button refresh=UiKit.button(context,"تحديث العرض المحلي");refresh.setOnClickListener(v->refresh());p.addView(refresh,UiKit.full(context));platformsView=platforms;

        ScrollView changes=scroll(context);LinearLayout ch=(LinearLayout)changes.getChildAt(0);ch.addView(UiKit.title(context,"سجل تغييرات المكتبة"),UiKit.full(context));changelogText=box(context);changelogText.setTextIsSelectable(true);ch.addView(changelogText,UiKit.full(context));changelogView=changes;

        show(0);
    }

    public View view(){return root;}

    private ScrollView scroll(Context c){ScrollView s=new ScrollView(c);s.setFillViewport(true);LinearLayout x=UiKit.vbox(c);x.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));s.addView(x);return s;}
    private static TextView box(Context c){TextView v=UiKit.text(c,"—");v.setBackgroundColor(0xFF151A20);return v;}

    private void show(int index){selected=Math.max(0,Math.min(3,index));page.removeAllViews();View[] views={dashboardView,distributionView,platformsView,changelogView};page.addView(views[selected],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabs.length;i++){tabs[i].setTextColor(i==selected?Color.WHITE:Color.DKGRAY);tabs[i].setBackgroundColor(i==selected?0xFF263238:0xFFE0E0E0);}refresh();}

    public void refresh(){
        ChemCoreLibraryManager.Status s=manager.status();
        versionText.setText("الإصدار الفعال: "+blank(s.version)+"\nالمصدر: "+blank(s.sourceLabel)+"\nSource commit: "+shortId(s.sourceCommit)+"\nData revision: "+shortId(s.dataRevision)+"\nEvidence revision: "+shortId(s.evidenceRevision)+"\nSHA-256: "+shortId(s.sha256));
        int total=host.database().chemicalCount(),tested=statusRepo.countTestedMaterials(),fp=statusRepo.countFingerprintMaterials(),ppm=statusRepo.countPpmMaterials(),m4=statusRepo.countM4Materials();int untested=Math.max(0,total-tested);
        coverageText.setText("إجمالي المواد: "+total+"\nمختبرة محليًا: "+tested+" ("+pct(tested,total)+")\nغير مختبرة محليًا: "+untested+" ("+pct(untested,total)+")\nمواد لها بصمة مشتقة محلية: "+fp+" ("+pct(fp,total)+")\nمواد لها منحنى PPM فعال: "+ppm+" ("+pct(ppm,total)+")\nمواد اجتازت M4 محليًا: "+m4+" ("+pct(m4,total)+")\nالتجارب المعتمدة: "+host.database().approvedCount()+"\nالمراجع المشتقة: "+host.database().referenceArtifactCount()+"\nمنحنيات المعايرة: "+host.database().calibrationCurveCount()+"\nEvidence revision: "+blank(statusRepo.libraryRevision()));
        String endpoint=BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL==null?"":BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL.trim();
        distributionText.setText("المصدر المركزي للكتابة: HazRob/build-harness (private)\nقناة قراءة المستخدم: "+(endpoint.isEmpty()?"غير منشورة بعد — Fail closed":"GitHub • "+endpoint)+"\nالسياسة: GitHub-only • لا Render • لا Token للمستخدم\nSampling profile: "+ChemCoreLibraryManager.REQUIRED_SAMPLING_PROFILE);
        updateTelemetryText.setText(formatUpdateTelemetry(host.database().getMeta("chemcore_update_telemetry_json","")));
        platformText.setText(formatPlatforms(host.database().getMeta("hazrob_platform_metrics_json","")));
        changelogText.setText(formatChangelog(host.database().getMeta("chemcore_changelog_summary",""),s));
    }

    private void checkDistribution(){operationText.setText("جارٍ فحص قناة GitHub...");String endpoint=manager.manifestUrl();if(endpoint==null||endpoint.trim().isEmpty()){operationText.setText("قناة القراءة العامة على GitHub لم تُنشر بعد؛ التحديث Fail-closed ولا يوجد fallback إلى Render.");return;}host.io().execute(()->{try{ChemCoreLibraryManager.CheckSummary c=manager.check(ReleaseAudit.VERSION);ChemCoreLibraryManifest m=c.remote.manifest;main.post(()->{operationText.setText("GitHub OK • "+m.libraryVersion+" • chemicals "+m.chemicalCount+" • models "+m.modelCount+" • calibrations "+m.calibrationCount+(c.updateAvailable?" • تحديث متاح":" • النسخة الحالية محدثة"));refresh();});}catch(Exception e){main.post(()->operationText.setText("تعذر فحص قناة GitHub: "+safe(e)));}});}

    private void rollback(){boolean active=ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics());ChemCoreLibraryStore.InstallResult r=manager.rollback(active,ReleaseAudit.VERSION);if(r.installed)ChemCoreMolecularSeedImporter.importBestAvailable(context,host.database());operationText.setText(r.installed?"تم الرجوع إلى "+r.version:"تعذر الرجوع: "+r.reason);if(r.installed)host.showToast("تم استرجاع مكتبة ChemCore السابقة");refresh();}

    private static String formatUpdateTelemetry(String raw){try{JSONObject j=new JSONObject(raw);return "إحصاءات التحديثات\nChecks: "+j.optInt("checks",0)+" • Downloads: "+j.optInt("downloads",0)+"\nInstall success: "+j.optInt("install_success",0)+" • Fail: "+j.optInt("install_fail",0)+" • Rollback: "+j.optInt("rollback",0)+"\nActive installations: "+j.optInt("active_installations",0)+"\nWindow: "+j.optString("window","—");}catch(Exception e){return "إحصاءات التحديثات: غير متاحة بعد. سيتم ملؤها من telemetry مجهولة الهوية بعد تفعيل قناة التوزيع العامة.";}}
    private static String formatPlatforms(String raw){try{JSONObject j=new JSONObject(raw);JSONArray a=j.optJSONArray("services");if(a==null||a.length()==0)throw new IllegalArgumentException();StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;if(s.length()>0)s.append('\n');s.append(q.optString("name","Service")).append(": ").append(q.optString("status","—"));String used=q.optString("used","");String limit=q.optString("limit","");String remaining=q.optString("remaining","");if(!used.isEmpty()||!limit.isEmpty())s.append(" • ").append(used).append(" / ").append(limit);if(!remaining.isEmpty())s.append(" • متبقٍ ").append(remaining);String reset=q.optString("reset_at","");if(!reset.isEmpty())s.append(" • reset ").append(reset);}return s.toString();}catch(Exception e){return "Railway: —\nGitHub: —\nMQTT: —\nCloudflare: —\nRender: —\nلم تتم مزامنة أرصدة المنصات بعد.";}}
    private static String formatChangelog(String raw,ChemCoreLibraryManager.Status s){if(raw!=null&&!raw.trim().isEmpty())return raw.trim();return "لا يوجد Changelog مركزي مستورد لهذه النسخة بعد.\nالإصدار الفعال: "+blank(s.version)+"\nData revision: "+shortId(s.dataRevision)+"\nEvidence revision: "+shortId(s.evidenceRevision);}
    private static String pct(int n,int total){return total<=0?"0%":String.format(Locale.US,"%.1f%%",100.0*n/total);}
    private static String blank(String s){return s==null||s.trim().isEmpty()?"—":s;}
    private static String shortId(String s){s=blank(s);return s.length()<=24?s:s.substring(0,24)+"…";}
    private static String safe(Exception e){String s=e==null?"unknown":e.getMessage();return s==null||s.trim().isEmpty()?e.getClass().getSimpleName():s;}
}
