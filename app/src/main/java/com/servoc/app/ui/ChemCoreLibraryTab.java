package com.servoc.app.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemcore.ChemCoreIdentityCurationRepository;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemcore.library.ChemCoreLibraryManifest;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
import com.servoc.app.github.GitHubSessionAuth;

/** Developer-facing control surface for independently versioned ChemCore scientific knowledge. */
public final class ChemCoreLibraryTab {
    private final Context context;
    private final HazRobHost host;
    private final ChemCoreLibraryManager manager;
    private final ScrollView scroll;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextView statusText, remoteText, operationText, endpointModeText, authText;
    private final EditText endpoint, githubToken;
    private final Button installButton, rollbackButton;
    private volatile ChemCoreLibraryManager.CheckSummary lastCheck;

    public ChemCoreLibraryTab(Context context, HazRobHost host) {
        this.context = context.getApplicationContext();
        this.host = host;
        this.manager = new ChemCoreLibraryManager(context);
        this.scroll = new ScrollView(context);
        scroll.setFillViewport(true);

        LinearLayout root = UiKit.vbox(context);
        root.setPadding(UiKit.dp(context, 8), UiKit.dp(context, 6), UiKit.dp(context, 8), UiKit.dp(context, 16));
        root.setBackgroundColor(0xFF0E1116);
        scroll.addView(root);

        LinearLayout status = UiKit.card(context);
        status.addView(UiKit.title(context, "مكتبة ChemCore العلمية"), UiKit.full(context));
        status.addView(UiKit.small(context,
                "المصدر المرجعي المباشر: مكتبة منظمة HazRob على GitHub. التحديثات محمية عبر HTTPS وSHA-256 وفحص التوافق والمحتوى؛ Token يبقى في ذاكرة الجلسة فقط ولا يُحفظ في التطبيق."), UiKit.full(context));
        statusText = UiKit.text(context, "—");
        statusText.setBackgroundColor(0xFF151A20);
        status.addView(statusText, UiKit.full(context));
        root.addView(status, UiKit.full(context));

        LinearLayout auth = UiKit.card(context);
        auth.addView(UiKit.title(context,"صلاحية GitHub للجلسة"),UiKit.full(context));
        auth.addView(UiKit.small(context,"مطلوبة لأن مكتبة HazRob الحالية private. نفس الاعتماد المؤقت يستخدم للتنزيل ولرفع التجارب المعتمدة. لا يُكتب إلى SharedPreferences أو الملفات."),UiKit.full(context));
        githubToken=UiKit.input(context,"");
        githubToken.setHint("GitHub token — session only");
        githubToken.setSingleLine(true);
        githubToken.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        auth.addView(githubToken,UiKit.full(context));
        LinearLayout ab=UiKit.hbox(context);
        Button use=UiKit.button(context,"اعتماد للجلسة");
        use.setOnClickListener(v->authorizeSession());ab.addView(use,UiKit.weight(1));
        Button clear=UiKit.button(context,"مسح الاعتماد");
        clear.setOnClickListener(v->{GitHubSessionAuth.clear();githubToken.setText("");lastCheck=null;operationText.setText("تم مسح اعتماد GitHub من الذاكرة.");refresh();});ab.addView(clear,UiKit.weight(1));
        auth.addView(ab,UiKit.full(context));
        authText=UiKit.small(context,"");authText.setBackgroundColor(0xFF151A20);auth.addView(authText,UiKit.full(context));
        root.addView(auth,UiKit.full(context));

        LinearLayout update = UiKit.card(context);
        update.addView(UiKit.title(context, "التحديث المباشر من HazRob GitHub"), UiKit.full(context));
        endpointModeText=UiKit.small(context,"");update.addView(endpointModeText,UiKit.full(context));
        endpoint = UiKit.input(context, manager.manifestUrl());
        endpoint.setHint("https://api.github.com/.../manifest.json");
        update.addView(UiKit.small(context, "المصدر الافتراضي مثبت على HazRob/build-harness/library/chemcore/stable. الحقل التالي للمطور لاختبار مصدر HTTPS بديل فقط."), UiKit.full(context));
        update.addView(endpoint, UiKit.full(context));

        LinearLayout endpointButtons = UiKit.hbox(context);
        Button save = UiKit.button(context, "حفظ مصدر اختباري");
        save.setOnClickListener(v -> saveEndpoint());
        endpointButtons.addView(save, UiKit.weight(1));
        Button reset = UiKit.button(context, "استعادة HazRob");
        reset.setOnClickListener(v -> resetEndpoint());
        endpointButtons.addView(reset, UiKit.weight(1));
        Button check = UiKit.button(context, "فحص التحديث");
        check.setOnClickListener(v -> checkUpdate());
        endpointButtons.addView(check, UiKit.weight(1));
        update.addView(endpointButtons, UiKit.full(context));

        remoteText = UiKit.small(context, "لم يتم فحص مكتبة المنظمة بعد.");
        remoteText.setBackgroundColor(0xFF151A20);
        update.addView(remoteText, UiKit.full(context));

        installButton = UiKit.button(context, "تنزيل وتثبيت التحديث المتحقق");
        installButton.setEnabled(false);
        installButton.setOnClickListener(v -> installUpdate());
        update.addView(installButton, UiKit.full(context));
        root.addView(update, UiKit.full(context));

        LinearLayout recovery = UiKit.card(context);
        recovery.addView(UiKit.title(context, "السلامة والاسترجاع"), UiKit.full(context));
        recovery.addView(UiKit.small(context,
                "يُمنع التحديث أو الرجوع أثناء warm-up/measurement/analysis. التثبيت يتم في staging ثم تفعيل ذري، وتبقى النسخة السابقة للاسترجاع بعد إعادة فحص SHA-256 والمحتوى."), UiKit.full(context));
        rollbackButton = UiKit.button(context, "الرجوع للنسخة السابقة المتحققة");
        rollbackButton.setOnClickListener(v -> rollback());
        recovery.addView(rollbackButton, UiKit.full(context));
        root.addView(recovery, UiKit.full(context));

        operationText = UiKit.small(context, "جاهز");
        operationText.setBackgroundColor(0xFF151A20);
        root.addView(operationText, UiKit.full(context));
        refresh();
    }

    public View view() { return scroll; }

    public void refresh() {
        ChemCoreLibraryManager.Status s = manager.status();
        StringBuilder text = new StringBuilder();
        text.append("المصدر الفعال: ").append(s.sourceLabel).append('\n');
        text.append("إصدار المكتبة: ").append(s.version.isEmpty() ? "—" : s.version).append('\n');
        text.append("Source commit: ").append(shortId(s.sourceCommit)).append('\n');
        text.append("Data revision: ").append(shortId(s.dataRevision)).append('\n');
        text.append("Evidence revision: ").append(shortId(s.evidenceRevision)).append('\n');
        text.append("SHA-256: ").append(shortId(s.sha256));
        if (s.embedded != null && s.embedded.molecularSeedPresent) {
            text.append("\nEmbedded molecular seed: ").append(s.embedded.recordCount)
                    .append(" (resolved ").append(s.embedded.resolvedCount)
                    .append(" / unresolved ").append(s.embedded.unresolvedCount).append(')');
        }
        String molecularStatus=host.database().getMeta("chemcore_molecular_seed_status","—");
        int curated=new ChemCoreIdentityCurationRepository(host.database()).count();
        text.append("\nLocal molecular import: ").append(molecularStatus);
        text.append("\nCurated non-QSPR identities: ").append(curated).append(" / 37")
                .append(curated==37?" • POLICY LOCKED":" • CHECK REQUIRED");
        statusText.setText(text.toString());
        authText.setText(GitHubSessionAuth.available()?"GitHub: authorized for this app session":"GitHub: no session credential");
        endpointModeText.setText(manager.usingDeveloperManifestOverride()?"وضع المصدر: Developer override":"وضع المصدر: HazRob organization canonical library");
        boolean active = ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics());
        installButton.setEnabled(lastCheck != null && lastCheck.updateAvailable && !active && GitHubSessionAuth.available());
        rollbackButton.setEnabled(!active);
    }

    private void authorizeSession(){
        try{GitHubSessionAuth.set(githubToken.getText().toString());githubToken.setText("");operationText.setText("تم اعتماد GitHub لهذه الجلسة فقط.");host.showToast("تم اعتماد مكتبة HazRob للجلسة");refresh();}
        catch(Exception e){operationText.setText("تعذر اعتماد GitHub: "+safe(e));}
    }

    private void saveEndpoint() {
        try {
            manager.setManifestUrl(endpoint.getText().toString());
            endpoint.setText(manager.manifestUrl());
            lastCheck = null; installButton.setEnabled(false);
            operationText.setText("تم حفظ مصدر الاختبار للمطور.");
            host.showToast("تم حفظ مصدر مكتبة ChemCore"); refresh();
        } catch (Exception e) { operationText.setText("رفض المصدر: " + safe(e)); }
    }

    private void resetEndpoint(){
        manager.setManifestUrl("");endpoint.setText(manager.manifestUrl());lastCheck=null;installButton.setEnabled(false);
        operationText.setText("تمت استعادة مكتبة منظمة HazRob كمصدر مباشر.");refresh();
    }

    private void checkUpdate() {
        if(!GitHubSessionAuth.available()){operationText.setText("أدخل GitHub token واعتمده للجلسة أولاً لأن المكتبة private.");return;}
        operationText.setText("جارٍ فحص مكتبة HazRob مباشرة..."); installButton.setEnabled(false);
        host.io().execute(() -> {
            try {
                ChemCoreLibraryManager.CheckSummary c = manager.check(ReleaseAudit.VERSION); lastCheck = c;
                main.post(() -> {
                    ChemCoreLibraryManifest m = c.remote.manifest;
                    remoteText.setText("الإصدار المتاح: " + m.libraryVersion
                            + "\nالتوافق: " + ChemCoreLibraryManager.REQUIRED_SAMPLING_PROFILE
                            + "\nالمركبات: " + m.chemicalCount + " • النماذج: " + m.modelCount + " • المعايرات: " + m.calibrationCount
                            + "\nIntegrity: SHA-256"
                            + "\nData revision: " + shortId(m.dataRevision)
                            + "\nEvidence revision: " + shortId(m.evidenceRevision)
                            + "\nSource: " + shortId(m.sourceCommitSha));
                    operationText.setText(c.updateAvailable ? "يوجد تحديث مستقر متوافق في HazRob." : "لا يوجد إصدار أحدث متوافق."); refresh();
                });
            } catch (Exception e) {
                lastCheck = null;
                main.post(() -> { remoteText.setText("تعذر فحص مكتبة HazRob."); operationText.setText("خطأ: " + safe(e)); refresh(); });
            }
        });
    }

    private void installUpdate() {
        final ChemCoreLibraryManager.CheckSummary checked = lastCheck;
        if (checked == null || !checked.updateAvailable) { operationText.setText("نفّذ فحص التحديث أولاً."); return; }
        if(!GitHubSessionAuth.available()){operationText.setText("انتهى اعتماد GitHub؛ أعد اعتماده للجلسة.");return;}
        boolean active = ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics());
        if (active) { operationText.setText("التحديث موقوف حتى انتهاء جلسة القياس/التحليل."); return; }
        installButton.setEnabled(false); operationText.setText("جارٍ التنزيل المباشر من GitHub وفحص SHA-256 والمحتوى والتثبيت...");
        host.io().execute(() -> {
            try {
                ChemCoreLibraryStore.InstallResult r = manager.install(checked, ReleaseAudit.VERSION,
                        ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics()));
                ChemCoreMolecularSeedImporter.Result applied=r.installed?ChemCoreMolecularSeedImporter.importVerifiedPayload(manager.activePayload(),host.database()):null;
                main.post(() -> {
                    if(r.installed&&applied!=null&&!applied.available)
                        operationText.setText("تم تثبيت "+r.version+"؛ لا تحتوي الحزمة طبقة جزيئية قابلة للتطبيق: "+applied.message);
                    else operationText.setText(r.installed ? "تم تثبيت ChemCore Library " + r.version : "لم يتم التثبيت: " + r.reason);
                    if (r.installed) { lastCheck = null; host.showToast("تم تحديث مكتبة ChemCore إلى " + r.version); }
                    refresh();
                });
            } catch (Exception e) { main.post(() -> { operationText.setText("فشل التحديث: " + safe(e)); refresh(); }); }
        });
    }

    private void rollback() {
        boolean active = ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics());
        ChemCoreLibraryStore.InstallResult r = manager.rollback(active, ReleaseAudit.VERSION);
        if(r.installed)ChemCoreMolecularSeedImporter.importBestAvailable(context,host.database());
        operationText.setText(r.installed ? "تم الرجوع إلى " + r.version : "تعذر الرجوع: " + r.reason);
        if (r.installed) host.showToast("تم استرجاع مكتبة ChemCore السابقة");
        lastCheck = null; refresh();
    }

    private static String safe(Exception e) {
        String s = e == null ? "unknown" : e.getMessage();
        return s == null || s.trim().isEmpty() ? e.getClass().getSimpleName() : s;
    }

    private static String shortId(String s) {
        if (s == null || s.isEmpty()) return "—";
        return s.length() <= 24 ? s : s.substring(0, 24) + "…";
    }
}
