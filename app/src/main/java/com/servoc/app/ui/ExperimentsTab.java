package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.analytics.LibraryStatus;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.ReferenceLineageGuard;
import com.servoc.app.measurement.MeasurementSessionRegistry;
import com.servoc.app.github.GitHubCredentialStore;
import com.servoc.app.github.GitHubReferencePublisher;
import com.servoc.app.github.ReferencePublishAudit;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.models.CalibrationModelLab;
import com.servoc.app.models.CalibrationRegressionModels;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.models.MqDataScienceModelProfile;
import com.servoc.app.reference.ReferenceArtifactBuilder;
import com.servoc.app.report.ReferenceBankExporter;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Developer-only laboratory for controlled material training, calibration and reference-bank publication. */
public final class ExperimentsTab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout list;
    private final TextView summary, details, healthSummary, r0Status, powerStatus, modelStatus, modelResult, kindHelp, publishCredentialStatus;
    private final EditText knownPpm, warmUsed, experimentNotes, chemicalSearch;
    private final Button pdf, xlsx, approve, publish;
    private final ChemicalRepository chemicalRepo;
    private final Spinner chemicalSpinner, referenceKindSpinner, modelSensorSpinner;
    private final ArrayAdapter<String> chemicalAdapter;
    private final String[] modelSensorKeys = ManualR0Calibration.keys();
    private final List<ChemicalRepository.Chemical> chemicalOptions = new ArrayList<>();
    private final Map<String, EditText> r0Fields = new LinkedHashMap<>();
    private int loadedChemicalCount = -1;
    private long selectedId = -1;

    public ExperimentsTab(Context c, HazRobHost host) {
        this.context = c.getApplicationContext();
        this.host = host;
        ManualR0Calibration.load(host.preferences());
        chemicalRepo = new ChemicalRepository(host.database());

        scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout root = UiKit.vbox(c);
        root.setPadding(UiKit.dp(c, 8), UiKit.dp(c, 6), UiKit.dp(c, 8), UiKit.dp(c, 12));
        root.setBackgroundColor(0xFF0E1116);
        scroll.addView(root);

        root.addView(UiKit.title(c, "التجارب العملية • تدريب HazRob"), UiKit.full(c));
        root.addView(UiKit.small(c, "اختر مادة من المكتبة، شغّل مجموعة MQ الثمانية، نفّذ دورة averaging كاملة، ثم احفظ التجربة وراجعها واعتمدها. التجارب المعتمدة فقط تبني بصمة أو منحنى معايرة موثق."), UiKit.full(c));

        LinearLayout power = UiKit.card(c);
        power.addView(UiKit.title(c, "طاقة MQ الموحدة"), UiKit.full(c));
        power.addView(UiKit.small(c, "Relay R1 / GPIO25 → جميع حساسات MQ الثمانية\nADS1 0x48 → MQ2, MQ4, MQ5, MQ8\nADS2 0x49 → MQ3, MQ7, MQ9, MQ135"), UiKit.full(c));
        LinearLayout pb = UiKit.hbox(c);
        Button off = UiKit.button(c, "MQ OFF");
        off.setOnClickListener(v -> mqPower(false));
        pb.addView(off, UiKit.weight(1));
        Button on = UiKit.button(c, "MQ ON — 8");
        on.setOnClickListener(v -> mqPower(true));
        pb.addView(on, UiKit.weight(1));
        power.addView(pb, UiKit.full(c));
        powerStatus = UiKit.small(c, "حالة الطاقة: —");
        powerStatus.setBackgroundColor(0xFF151A20);
        power.addView(powerStatus, UiKit.full(c));
        root.addView(power, UiKit.full(c));

        LinearLayout capture = UiKit.card(c);
        capture.addView(UiKit.title(c, "تجربة مرجعية للمادة"), UiKit.full(c));
        capture.addView(UiKit.small(c, "نوع التدريب"), UiKit.full(c));
        referenceKindSpinner = new Spinner(c);
        ArrayAdapter<String> kindAdapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, new String[]{"بصمة تعريفية", "معايرة كمية"});
        kindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        referenceKindSpinner.setAdapter(kindAdapter);
        capture.addView(referenceKindSpinner, UiKit.full(c));
        kindHelp = UiKit.small(c, "");
        kindHelp.setBackgroundColor(0xFF151A20);
        capture.addView(kindHelp, UiKit.full(c));
        referenceKindSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { updateKindHelp(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        capture.addView(UiKit.small(c, "المادة / المركب من المكتبة"), UiKit.full(c));
        chemicalSearch = UiKit.input(c, "");
        chemicalSearch.setHint("بحث تفاعلي: اسم المادة أو CAS");
        chemicalSearch.setSingleLine(true);
        capture.addView(chemicalSearch, UiKit.full(c));
        chemicalSpinner = new Spinner(c);
        chemicalAdapter = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, new ArrayList<>()) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(isFingerprintPosition(position) ? 0xFF42A5F5 : Color.BLACK);
                return v;
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) ((TextView) v).setTextColor(isFingerprintPosition(position) ? 0xFF1565C0 : Color.BLACK);
                return v;
            }
        };
        chemicalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chemicalSpinner.setAdapter(chemicalAdapter);
        capture.addView(chemicalSpinner, UiKit.full(c));
        chemicalSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { refreshChemicalOptions(s == null ? "" : s.toString(), true); }
            @Override public void afterTextChanged(Editable e) {}
        });

        knownPpm = field(c, capture, "التركيز المرجعي ppm — إلزامي للمعايرة، اختياري للبصمة", "");
        knownPpm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        warmUsed = field(c, capture, "زمن التسخين الفعلي المستخدم (ثانية)", String.valueOf(host.preferences().getInt("warm", 60)));
        warmUsed.setInputType(InputType.TYPE_CLASS_NUMBER);
        experimentNotes = field(c, capture, "ملاحظات / مصدر المادة / ظروف الإجراء", "");

        LinearLayout run = UiKit.hbox(c);
        Button start = UiKit.button(c, "بدء averaging");
        start.setOnClickListener(v -> measurement(true));
        run.addView(start, UiKit.weight(1));
        Button cancel = UiKit.button(c, "إلغاء");
        cancel.setOnClickListener(v -> measurement(false));
        run.addView(cancel, UiKit.weight(.7f));
        Button save = UiKit.button(c, "حفظ التجربة");
        save.setOnClickListener(v -> captureReference());
        run.addView(save, UiKit.weight(1));
        capture.addView(run, UiKit.full(c));
        root.addView(capture, UiKit.full(c));
        updateKindHelp();

        LinearLayout r0 = UiKit.card(c);
        r0.addView(UiKit.title(c, "R0 اليدوي لكل حساس"), UiKit.full(c));
        r0.addView(UiKit.small(c, "R0 معايرة محلية خاصة بمجموعة حساسات الروبوت. لا تستخدم Rs الحالية كـR0 إلا وفق بروتوكول معايرة موثق، ولا تُعامل R0 كمعرفة مركزية قابلة للتوزيع."), UiKit.full(c));
        for (String key : ManualR0Calibration.keys()) {
            double v = ManualR0Calibration.get(key);
            EditText e = field(c, r0, key.toUpperCase(Locale.US) + " R0", Double.isFinite(v) ? String.format(Locale.US, "%.6f", v) : "");
            e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            r0Fields.put(key, e);
        }
        LinearLayout rb = UiKit.hbox(c);
        Button saveR0 = UiKit.button(c, "حفظ R0");
        saveR0.setOnClickListener(v -> saveR0());
        rb.addView(saveR0, UiKit.weight(1));
        Button clearR0 = UiKit.button(c, "مسح R0");
        clearR0.setOnClickListener(v -> clearR0());
        rb.addView(clearR0, UiKit.weight(1));
        r0.addView(rb, UiKit.full(c));
        r0Status = UiKit.small(c, "بانتظار القراءات...");
        r0Status.setTextIsSelectable(true);
        r0Status.setBackgroundColor(0xFF151A20);
        r0.addView(r0Status, UiKit.full(c));
        root.addView(r0, UiKit.full(c));

        LinearLayout health = UiKit.card(c);
        health.addView(UiKit.title(c, "صحة الحساسات ومرجع الهواء النظيف"), UiKit.full(c));
        health.addView(UiKit.small(c, "Baseline/Drift للتحقق من الاستقرار ولا يحل محل معايرة الغاز أو R0."), UiKit.full(c));
        LinearLayout hb = UiKit.hbox(c);
        Button base = UiKit.button(c, "اعتماد Baseline نظيف");
        base.setOnClickListener(v -> captureBaseline());
        hb.addView(base, UiKit.weight(1));
        Button check = UiKit.button(c, "فحص Drift");
        check.setOnClickListener(v -> checkDrift());
        hb.addView(check, UiKit.weight(1));
        health.addView(hb, UiKit.full(c));
        healthSummary = UiKit.small(c, "لا يوجد تقييم بعد.");
        healthSummary.setTextIsSelectable(true);
        health.addView(healthSummary, UiKit.full(c));
        root.addView(health, UiKit.full(c));

        LinearLayout models = UiKit.card(c);
        models.addView(UiKit.title(c, "مختبر نماذج MQDataScience الـ76"), UiKit.full(c));
        modelStatus = UiKit.small(c, modelLibraryStatus());
        modelStatus.setTextIsSelectable(true);
        models.addView(modelStatus, UiKit.full(c));
        final TextView catalog = UiKit.small(c, "");
        catalog.setTextIsSelectable(true);
        catalog.setVisibility(View.GONE);
        catalog.setBackgroundColor(0xFF151A20);
        Button showCatalog = UiKit.button(c, "عرض خريطة الـ76 نموذجًا");
        showCatalog.setOnClickListener(v -> {
            boolean show = catalog.getVisibility() != View.VISIBLE;
            catalog.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) catalog.setText(formatModelCatalog());
        });
        models.addView(showCatalog, UiKit.full(c));
        models.addView(catalog, UiKit.full(c));
        models.addView(UiKit.small(c, "الحساس المراد تقييم منحنياته"), UiKit.full(c));
        modelSensorSpinner = new Spinner(c);
        String[] sensorLabels = new String[modelSensorKeys.length];
        for (int i = 0; i < modelSensorKeys.length; i++) sensorLabels[i] = modelSensorKeys[i].toUpperCase(Locale.US);
        ArrayAdapter<String> sensorAdapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, sensorLabels);
        sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSensorSpinner.setAdapter(sensorAdapter);
        models.addView(modelSensorSpinner, UiKit.full(c));
        Button compare = UiKit.button(c, "مقارنة النماذج مع نقاط المعايرة");
        compare.setOnClickListener(v -> compareCalibrationModels());
        models.addView(compare, UiKit.full(c));
        modelResult = UiKit.small(c, "سجل عدة تعرضات كمية عند تراكيز معلومة ثم قارن النماذج. الاعتماد التشغيلي يستخدم فقط التجارب المعتمدة ويحتفظ بالنقاط الأصلية للتدقيق.");
        modelResult.setTextIsSelectable(true);
        modelResult.setBackgroundColor(0xFF151A20);
        models.addView(modelResult, UiKit.full(c));
        root.addView(models, UiKit.full(c));

        summary = UiKit.text(c, "التجارب: 0");
        summary.setBackgroundColor(0xFF151A20);
        root.addView(summary, UiKit.full(c));
        details = UiKit.text(c, "اختر تجربة محفوظة لمراجعتها واعتمادها.");
        details.setTextIsSelectable(true);
        details.setBackgroundColor(0xFF11171D);
        root.addView(details, UiKit.full(c));

        LinearLayout export = UiKit.hbox(c);
        pdf = UiKit.button(c, "تقرير التجربة PDF");
        pdf.setOnClickListener(v -> export("pdf"));
        export.addView(pdf, UiKit.weight(1));
        xlsx = UiKit.button(c, "بيانات التجربة Excel");
        xlsx.setOnClickListener(v -> export("xlsx"));
        export.addView(xlsx, UiKit.weight(1));
        root.addView(export, UiKit.full(c));
        approve = UiKit.button(c, "اعتماد وبناء المرجع العلمي");
        approve.setOnClickListener(v -> approveSelected());
        root.addView(approve, UiKit.full(c));

        LinearLayout github = UiKit.card(c);
        github.addView(UiKit.title(c, "المكتبة المركزية • GitHub"), UiKit.full(c));
        github.addView(UiKit.small(c, "إرسال تجارب المطور المرجعية المعتمدة إلى منطقة مراجعة غير تشغيلية في مستودع إصدار HR069-R1. لا يفعّل الإرسال تحديث ChemCore أو مرتبة M4. الصلاحية مشفرة محليًا ولا تُدمج في APK."), UiKit.full(c));
        publishCredentialStatus = UiKit.small(c, "صلاحية الرفع: —");
        publishCredentialStatus.setBackgroundColor(0xFF151A20);
        github.addView(publishCredentialStatus, UiKit.full(c));
        LinearLayout authButtons = UiKit.hbox(c);
        Button configureCredential = UiKit.button(c, "تهيئة/استبدال صلاحية الرفع");
        configureCredential.setOnClickListener(v -> promptCredential(null));
        authButtons.addView(configureCredential, UiKit.weight(1));
        Button clearCredential = UiKit.button(c, "مسح الصلاحية");
        clearCredential.setOnClickListener(v -> {
            GitHubCredentialStore.clear(context);
            refreshCredentialStatus();
            host.showToast("تم مسح صلاحية رفع GitHub من هذا الجهاز");
        });
        authButtons.addView(clearCredential, UiKit.weight(.7f));
        github.addView(authButtons, UiKit.full(c));
        publish = UiKit.button(c, "إرسال مرجع المطور للمراجعة العلمية فقط");
        publish.setOnClickListener(v -> publishSelected());
        github.addView(publish, UiKit.full(c));
        root.addView(github, UiKit.full(c));

        list = UiKit.vbox(c);
        root.addView(list, UiKit.full(c));
        setActionState(false);
        refreshChemicalOptions();
        refreshCredentialStatus();
    }

    private EditText field(Context c, LinearLayout p, String label, String value) {
        p.addView(UiKit.small(c, label), UiKit.full(c));
        EditText e = UiKit.input(c, value);
        p.addView(e, UiKit.full(c));
        return e;
    }

    public View view() { return scroll; }

    private String selectedKind() { return referenceKindSpinner.getSelectedItemPosition() == 1 ? "calibration" : "fingerprint"; }

    private void updateKindHelp() {
        if (kindHelp == null) return;
        boolean cal = "calibration".equals(selectedKind());
        kindHelp.setText(cal
                ? "معايرة كمية: يجب إدخال ppm معلوم. كل تجربة معتمدة تضيف نقطة؛ تفعيل M4 يظل خاضعًا لبروتوكول HR069 ومعايير الجودة."
                : "بصمة تعريفية: التركيز اختياري. HazRob يبني متجه استجابة من الجلسة الزمنية ثم يحدّث المرجع من التجارب المعتمدة فقط.");
    }

    private void refreshChemicalOptions() { refreshChemicalOptions(chemicalSearch == null ? "" : chemicalSearch.getText().toString(), false); }

    private void refreshChemicalOptions(String query, boolean force) {
        int count = chemicalRepo.count();
        String q = query == null ? "" : query.trim();
        if (!force && q.isEmpty() && count == loadedChemicalCount && chemicalAdapter.getCount() > 0) return;
        long keep = -1;
        int old = chemicalSpinner.getSelectedItemPosition();
        if (old >= 0 && old < chemicalOptions.size()) keep = chemicalOptions.get(old).uid;
        List<ChemicalRepository.Chemical> rows = chemicalRepo.search(q, 5000);
        chemicalOptions.clear();
        chemicalOptions.addAll(rows);
        chemicalAdapter.clear();
        if (rows.isEmpty()) chemicalAdapter.add("لا توجد نتيجة مطابقة");
        else for (ChemicalRepository.Chemical x : rows) chemicalAdapter.add(optionLabel(x) + (hasApprovedFingerprint(x.uid) ? "   ● بصمة معتمدة" : ""));
        chemicalAdapter.notifyDataSetChanged();
        loadedChemicalCount = count;
        if (keep > 0) {
            for (int i = 0; i < chemicalOptions.size(); i++) {
                if (chemicalOptions.get(i).uid == keep) {
                    chemicalSpinner.setSelection(i);
                    return;
                }
            }
        }
        if (!rows.isEmpty()) chemicalSpinner.setSelection(0);
    }

    private boolean hasApprovedFingerprint(long uid) { return host.database().hasApprovedReference(uid, "fingerprint"); }
    private boolean isFingerprintPosition(int position) { return position >= 0 && position < chemicalOptions.size() && hasApprovedFingerprint(chemicalOptions.get(position).uid); }

    private static String optionLabel(ChemicalRepository.Chemical q) {
        String name = q.arabicName != null && !q.arabicName.trim().isEmpty() ? q.arabicName : q.name;
        StringBuilder s = new StringBuilder(name == null ? "" : name);
        if (q.formula != null && !q.formula.isEmpty()) s.append(" • ").append(q.formula);
        if (q.cas != null && !q.cas.isEmpty()) s.append(" • CAS ").append(q.cas);
        return s.toString();
    }

    private ChemicalRepository.Chemical selectedChemical() {
        int p = chemicalSpinner.getSelectedItemPosition();
        return p >= 0 && p < chemicalOptions.size() ? chemicalOptions.get(p) : null;
    }

    private void mqPower(boolean on) {
        host.io().execute(() -> {
            try {
                host.robotApi().mqPower(on);
                host.showToast("MQ: " + HazRobHardwareProfile.groupLabel(on ? 1 : 0));
                host.refreshAllTabs();
            } catch (Exception e) {
                host.showToast("تعذر التحكم بطاقة MQ");
            }
        });
    }

    private void measurement(boolean start) {
        host.io().execute(() -> {
            try {
                if (start) { MeasurementSessionRegistry.markDeveloperMqCycle(); host.robotApi().startMeasurement(); }
                else { MeasurementSessionRegistry.clearDeveloperMqCycle(); host.robotApi().cancelMeasurement(); }
                host.showToast(start ? "بدأ averaging؛ أبقِ MQ ON حتى READY" : "أُلغي averaging");
            } catch (Exception e) {
                host.showToast("تعذر إرسال الأمر");
            }
        });
    }

    private void captureReference() {
        final ChemicalRepository.Chemical chemical = selectedChemical();
        if (chemical == null) { host.showToast("اختر مادة من المكتبة"); return; }
        final String kind = selectedKind();
        Double ppm = null;
        String ppmText = knownPpm.getText().toString().trim();
        if (!ppmText.isEmpty()) {
            try {
                double x = Double.parseDouble(ppmText);
                if (Double.isFinite(x) && x > 0) ppm = x; else throw new IllegalArgumentException();
            } catch (Exception e) { host.showToast("قيمة ppm غير صالحة"); return; }
        }
        if ("calibration".equals(kind) && ppm == null) { host.showToast("أدخل التركيز المرجعي ppm للمعايرة الكمية"); return; }

        int warmSeconds;
        try { warmSeconds = Integer.parseInt(warmUsed.getText().toString().trim()); }
        catch (Exception e) { host.showToast("أدخل زمن التسخين الفعلي"); return; }
        if (warmSeconds < 0 || warmSeconds > 86400) { host.showToast("زمن التسخين غير صالح"); return; }

        JSONObject d = host.latestDiagnostics();
        if (d == null) { host.showToast("لا توجد بيانات حية من الروبوت"); return; }
        JSONObject m = d.optJSONObject("measurement");
        boolean ready = m != null && "ready".equalsIgnoreCase(m.optString("state")) && m.optJSONObject("mq") != null;
        if (!ready) { host.showToast("لا يمكن حفظ مرجع: أكمل دورة averaging حتى READY أولاً"); return; }
        JSONObject source = m.optJSONObject("mq");
        if (source == null || source.length() == 0) { host.showToast("لا توجد قراءات MQ averaged صالحة"); return; }

        String session = host.lastMeasurementSessionId();
        if (session == null || session.isEmpty() || !host.database().sessionExists(session)) {
            host.showToast("تعذر ربط التجربة بجلسة القياس الأصلية؛ أعد دورة averaging");
            return;
        }

        if (!ExperimentQueries.isDeveloperMeasurementSession(host.database(), session)) {
            host.showToast("قياسات المستخدم المحلية ليست جلسات مختبر للمطور");return;
        }
        JSONObject env = d.optJSONObject("env");
        Double t = env != null && Double.isFinite(env.optDouble("temperature", Double.NaN)) ? env.optDouble("temperature") : null;
        Double rh = env != null && Double.isFinite(env.optDouble("humidity", Double.NaN)) ? env.optDouble("humidity") : null;
        String ts = isoNow();
        String analytics = AnalyticsSnapshot.toJson(d);
        JSONObject relay = d.optJSONObject("relay");
        int mask = relay != null && relay.optBoolean("on", relay.optInt("mask", 0) != 0) ? 1 : 0;

        JSONObject meta = new JSONObject();
        try {
            meta.put("schema", "hazrob-reference-capture-v0.10.2");
            meta.put("reference_kind", kind);
            meta.put("session_id", session);
            meta.put("hardware_profile", d.optString("hardware_profile", HazRobHardwareProfile.ID));
            meta.put("relay_mask", mask);
            meta.put("mq_power", mask == 1);
            meta.put("mq_power_gpio", 25);
            meta.put("mq_power_architecture", "single_relay_8_sensors");
            meta.put("warmup_actual_s", warmSeconds);
            meta.put("capture_state", "ready_average");
            if (t != null) meta.put("temperature_c", t);
            if (rh != null) meta.put("rh_pct", rh);
            meta.put("notes", experimentNotes.getText().toString().trim());
            meta.put("analytics_version", LibraryStatus.ANALYTICS_VERSION);
            meta.put("calibration_version", LibraryStatus.CALIBRATION_VERSION);
            meta.put("mqdatascience_version", MqDataScience76Catalog.VERSION);
            meta.put("mqdatascience_commit", MqDataScience76Catalog.PINNED_COMMIT);
            // R0 is retained here only as local provenance for the originating physical robot.
            // It is not a central-library calibration to be applied to other devices.
            JSONObject r0 = new JSONObject();
            for (String k : ManualR0Calibration.keys()) {
                double rv = ManualR0Calibration.get(k);
                if (Double.isFinite(rv) && rv > 0) r0.put(k, rv);
            }
            meta.put("device_local_r0_provenance", r0);
            meta.put("device_local_r0_distributable", false);
        } catch (Exception ignored) {}

        final Double known = ppm;
        final String feature = meta.toString();
        final String protocol = ("calibration".equals(kind) ? "Reference calibration: " : "Reference fingerprint: ")
                + chemical.label() + (known == null ? "" : " @ " + known + " ppm")
                + " • ready_average • MQ Power ON 8 • GPIO25 • session " + session;

        host.io().execute(() -> {
            try {
                long captureId = host.database().saveCaptureForChemical(chemical.uid, chemical.legacyId, known, ts, session, t, rh, "ready_average", source.toString(), feature);
                if (captureId <= 0) throw new IllegalStateException("capture");
                long id = host.database().saveReferenceExperiment(ts, d.toString(), protocol, analytics, session, captureId, chemical.uid, kind, known);
                if (id <= 0) throw new IllegalStateException("experiment");
                host.showToast("حُفظت تجربة " + ("calibration".equals(kind) ? "المعايرة" : "البصمة") + " #" + id + " كمسودة للمراجعة");
                host.refreshAllTabs();
            } catch (Exception ex) {
                host.showToast("تعذر حفظ التجربة المرجعية");
            }
        });
    }

    private void saveR0() {
        int saved = 0;
        for (Map.Entry<String, EditText> x : r0Fields.entrySet()) {
            String s = x.getValue().getText().toString().trim();
            if (s.isEmpty()) continue;
            try {
                double v = Double.parseDouble(s);
                if (ManualR0Calibration.save(host.preferences(), x.getKey(), v)) saved++;
            } catch (Exception ignored) {}
        }
        host.showToast("تم حفظ R0 المحلي لـ " + saved + " حساسات");
        host.refreshAllTabs();
        refreshR0();
    }

    private void clearR0() {
        for (String k : ManualR0Calibration.keys()) {
            ManualR0Calibration.clear(host.preferences(), k);
            EditText e = r0Fields.get(k);
            if (e != null) e.setText("");
        }
        host.showToast("تم مسح قيم R0 المحلية");
        host.refreshAllTabs();
        refreshR0();
    }

    private void captureBaseline() {
        int n = host.captureCleanAirBaseline();
        if (n <= 0) { host.showToast("يلزم هواء نظيف وقراءات صالحة"); return; }
        host.showToast("تم اعتماد baseline نظيف لـ " + n + " حساسات");
        refreshHealth();
        host.refreshAllTabs();
    }

    private void checkDrift() {
        int n = host.evaluateCleanAirDrift();
        if (n <= 0) { host.showToast("لا يوجد baseline صالح للمقارنة"); return; }
        host.showToast("تم فحص drift لـ " + n + " حساسات");
        refreshHealth();
        host.refreshAllTabs();
    }

    private String modelLibraryStatus() {
        return "MQDataScience: " + ModelRuntimeRegistry.adoptedCount() + " / " + MqDataScience76Catalog.COUNT
                + " ADOPTED • Native " + ModelRuntimeRegistry.nativeCount()
                + " • Verified " + ModelRuntimeRegistry.verifiedCount()
                + " • Approx " + ModelRuntimeRegistry.approximationCount();
    }

    private String formatModelCatalog() {
        StringBuilder s = new StringBuilder();
        s.append("Source: ").append(MqDataScience76Catalog.SOURCE).append(" v").append(MqDataScience76Catalog.VERSION)
                .append(" @ ").append(MqDataScience76Catalog.PINNED_COMMIT).append("\n\n");
        for (int i = 0; i < MqDataScience76Catalog.COUNT; i++) {
            s.append('#').append(i + 1).append(" • ").append(MqDataScience76Catalog.NAMES[i]).append("\n   ")
                    .append(ModelRuntimeRegistry.runtimeStatus(i)).append(" • ")
                    .append(MqDataScienceModelProfile.requirementLabel(i)).append(" • recommended ≥")
                    .append(MqDataScienceModelProfile.recommendedMinimumSamples(i)).append(" samples\n");
        }
        return s.toString();
    }

    private void compareCalibrationModels() {
        final ChemicalRepository.Chemical chemical = selectedChemical();
        int p = modelSensorSpinner.getSelectedItemPosition();
        final String sensorKey = p >= 0 && p < modelSensorKeys.length ? modelSensorKeys[p] : null;
        if (chemical == null || sensorKey == null) { host.showToast("اختر المادة والحساس"); return; }
        modelResult.setText("جارٍ تقييم نقاط " + sensorKey.toUpperCase(Locale.US) + "...");
        host.io().execute(() -> {
            CalibrationModelLab.Report report = CalibrationModelLab.evaluate(host.database(), chemical.uid, sensorKey);
            String text = formatModelReport(chemical, report);
            scroll.post(() -> modelResult.setText(text));
        });
    }

    private String formatModelReport(ChemicalRepository.Chemical chemical, CalibrationModelLab.Report report) {
        StringBuilder s = new StringBuilder();
        s.append("المادة: ").append(optionLabel(chemical)).append('\n')
                .append("الحساس: ").append(report.sensorKey.toUpperCase(Locale.US)).append('\n')
                .append("العينات الصالحة: ").append(report.samples).append(" • مستويات التركيز: ").append(report.concentrationLevels).append("\n\n");
        if (report.scores.isEmpty()) return s.append("لا توجد نقاط كافية بعد. سجل تجارب معايرة averaged بتراكيز معلومة ثم اعتمدها.").toString();
        int shown = Math.min(10, report.scores.size());
        for (int i = 0; i < shown; i++) {
            CalibrationRegressionModels.Score q = report.scores.get(i);
            s.append(i + 1).append(") ").append(q.fit.name).append("\n   ").append(q.fit.equation())
                    .append(String.format(Locale.US, "\n   R²(log)=%.5f • log-RMSE=%.6f • n=%d • %s\n", q.r2, q.logRmse, q.samples, q.crossValidated ? "LOO CV" : "fit-only"));
        }
        return s.append("\nالمنحنى التشغيلي يحتفظ بالنقاط المعتمدة ويستخدم interpolation داخل نطاقها؛ ترتيب النماذج أداة تحليل وتوثيق إضافية.").toString();
    }

    public void refresh() {
        refreshChemicalOptions();
        refreshHealth();
        refreshR0();
        refreshPower();
        refreshCredentialStatus();
        modelStatus.setText(modelLibraryStatus());
        list.removeAllViews();
        List<ExperimentQueries.Record> rows = ExperimentQueries.recentDeveloper(host.database(), 100);
        summary.setText("المواد: " + host.database().chemicalCount()
                + " • السجلات: " + host.database().count()
                + " • التعرضات: " + host.database().captureCount()
                + " • المعتمدة: " + host.database().approvedCount()
                + " • المراجع المشتقة: " + host.database().referenceArtifactCount());
        if (rows.isEmpty()) {
            selectedId = -1;
            setActionState(false);
            list.addView(UiKit.small(list.getContext(), "لا توجد سجلات بعد."), UiKit.full(list.getContext()));
            return;
        }
        if (selectedId < 0) selectedId = rows.get(0).id;
        boolean still = false;
        for (ExperimentQueries.Record r : rows) {
            if (r.id == selectedId) still = true;
            String kind = "fingerprint".equals(r.referenceKind) ? "بصمة" : "calibration".equals(r.referenceKind) ? "معايرة" : "قياس";
            Button b = UiKit.button(list.getContext(), (r.approved ? "✓ " : "") + "#" + r.id + " • " + kind + " • " + safe(r.ts));
            b.setOnClickListener(v -> select(r.id));
            list.addView(b, UiKit.full(list.getContext()));
        }
        if (!still) selectedId = rows.get(0).id;
        showSelected();
    }

    private void refreshPower() {
        JSONObject d = host.latestDiagnostics(), r = d == null ? null : d.optJSONObject("relay");
        boolean on = r != null && r.optBoolean("on", r.optInt("mask", 0) != 0);
        long ms = r == null ? 0 : r.optLong("power_ms", 0);
        powerStatus.setText("الحالة: " + HazRobHardwareProfile.groupLabel(on ? 1 : 0) + " • GPIO25" + (on ? " • " + ms + " ms" : ""));
    }

    private void refreshR0() {
        JSONObject d = host.latestDiagnostics(), mq = d == null ? null : d.optJSONObject("mq");
        StringBuilder s = new StringBuilder("معايرة محلية للجهاز — لا تُزامن إلى المكتبة المركزية");
        for (String k : ManualR0Calibration.keys()) {
            s.append('\n');
            JSONObject q = mq == null ? null : mq.optJSONObject(k);
            double rs = q == null ? Double.NaN : q.optDouble("rs", Double.NaN);
            double ratio = q == null ? Double.NaN : q.optDouble("ratio", Double.NaN);
            double r0 = ManualR0Calibration.get(k);
            s.append(k.toUpperCase(Locale.US)).append(" • R0 ").append(Double.isFinite(r0) ? String.format(Locale.US, "%.5f", r0) : "—")
                    .append(" • Rs ").append(Double.isFinite(rs) ? String.format(Locale.US, "%.5f", rs) : "—")
                    .append(" • Rs/R0 ").append(Double.isFinite(ratio) ? String.format(Locale.US, "%.4f", ratio) : "—");
        }
        r0Status.setText(s.toString());
    }

    private void refreshHealth() {
        StringBuilder s = new StringBuilder();
        String ts = host.cleanAirBaselineTimestamp();
        s.append("Baseline: ").append(ts == null || ts.isEmpty() ? "غير معتمد" : ts);
        for (String key : SensorHealthEngine.INSTALLED) {
            SensorHealthEngine.Snapshot h = host.sensorHealth(key);
            s.append('\n').append(key.toUpperCase(Locale.US)).append(" • ").append(SensorHealthEngine.compact(h));
            if (h != null && Double.isFinite(h.noiseCv)) s.append(String.format(Locale.US, " • noise CV %.1f%%", 100 * h.noiseCv));
        }
        healthSummary.setText(s.toString());
    }

    private void refreshCredentialStatus() {
        boolean available = GitHubCredentialStore.available(context);
        publishCredentialStatus.setText(available
                ? "صلاحية الرفع: مهيأة ومشفرة على هذا الجهاز ✓"
                : "صلاحية الرفع: غير مهيأة — ستُطلب مرة واحدة عند أول رفع");
        publishCredentialStatus.setTextColor(available ? 0xFF81C784 : 0xFFFFB74D);
    }

    private void select(long id) { selectedId = id; showSelected(); }

    private void showSelected() {
        ExperimentQueries.Record r = ExperimentQueries.byId(host.database(), selectedId);
        if (r == null) { setActionState(false); return; }
        setActionState(true);
        JSONObject d = json(r.payload), relay = d.optJSONObject("relay");
        StringBuilder s = new StringBuilder("السجل #").append(r.id)
                .append("\nUTC: ").append(safe(r.ts))
                .append("\nالبروتوكول: ").append(safe(r.protocol))
                .append("\nالنوع: ").append(safe(r.referenceKind))
                .append("\nSession: ").append(safe(r.sessionId))
                .append("\nCapture: ").append(r.captureId > 0 ? r.captureId : "—")
                .append("\nMQ Power: ").append(relay != null && relay.optBoolean("on", relay.optInt("mask", 0) != 0) ? "ON • GPIO25 • 8" : "OFF • GPIO25")
                .append("\nالاعتماد: ").append(r.approved ? "معتمد" : "مسودة");
        if (r.knownPpm != null) s.append("\nReference ppm: ").append(r.knownPpm);
        if (r.approved) {
            s.append("\nاعتمد: ").append(safe(r.approvedTs));
            ExperimentsDb.ReferenceArtifactRecord a = host.database().latestReferenceArtifact(r.chemicalUid, r.referenceKind);
            if (a != null) s.append("\nDerived: #").append(a.id).append(" • ").append(a.version).append("\nSHA-256: ").append(a.sha256);
        }
        String published = ReferencePublishAudit.latestStatus(host.database(), r.id);
        if (!published.isEmpty()) s.append("\nGitHub: ").append(published);
        details.setText(s.toString());
        approve.setEnabled(r.isReference() && !r.approved && ExperimentQueries.isDeveloperReference(host.database(),r.id));
        approve.setText(r.approved ? "معتمد — المرجع المشتق محدث" : "اعتماد وبناء المرجع العلمي");
        publish.setEnabled(r.approved && r.isReference() && ExperimentQueries.isDeveloperReference(host.database(),r.id));
    }

    private void setActionState(boolean on) {
        pdf.setEnabled(on);
        xlsx.setEnabled(on);
        approve.setEnabled(on);
        publish.setEnabled(false);
    }

    private void export(String format) { if (selectedId >= 0) host.requestExport(format, selectedId); }

    private void approveSelected() {
        if (selectedId < 0) return;
        ExperimentQueries.Record r = ExperimentQueries.byId(host.database(), selectedId);
        if (r == null || !ExperimentQueries.isDeveloperReference(host.database(),r.id)) { host.showToast("السجل ليس تجربة مرجعية للمطور"); return; }
        if (!"ready_average".equals(host.database().capturePhase(r.captureId))) { host.showToast("لا يعتمد إلا التعرض averaged في حالة READY"); return; }
        if (r.approved) { host.showToast("التجربة معتمدة مسبقاً"); return; }
        String ts = isoNow(), name = ReferenceBankExporter.fileName(r.id, r.ts);
        if (!host.database().approveExperiment(r.id, ts, name)) { host.showToast("تعذر اعتماد السجل"); return; }
        host.io().execute(() -> {
            try {
                ReferenceArtifactBuilder.Result built = ReferenceArtifactBuilder.rebuild(host.database(), r.chemicalUid, r.referenceKind);
                host.showToast(built.message);
                host.refreshAllTabs();
            } catch (Exception e) {
                host.showToast("اعتمدت التجربة، لكن تعذر تحديث المرجع المشتق");
                host.refreshAllTabs();
            }
        });
    }

    private void publishSelected() {
        if (selectedId < 0) return;
        final ExperimentQueries.Record r = ExperimentQueries.byId(host.database(), selectedId);
        if (r == null || !r.approved || !ExperimentQueries.isDeveloperReference(host.database(),r.id) || !ReferenceLineageGuard.valid(host.database(),r)) { host.showToast("اعتمد تجربة مطور صالحة أولًا"); return; }
        String token = GitHubCredentialStore.load(context);
        if (token.length() < 20) {
            promptCredential(r);
            return;
        }
        publishWithToken(r, token);
    }

    private void promptCredential(ExperimentQueries.Record pendingPublish) {
        EditText tokenInput = UiKit.input(scroll.getContext(), "");
        tokenInput.setHint("GitHub fine-grained token");
        tokenInput.setSingleLine(true);
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout box = UiKit.vbox(scroll.getContext());
        box.setPadding(UiKit.dp(scroll.getContext(), 18), UiKit.dp(scroll.getContext(), 6), UiKit.dp(scroll.getContext(), 18), 0);
        box.addView(UiKit.small(scroll.getContext(), "هذه تهيئة محلية لجهاز المطور فقط. لا يُدمج التوكن في APK ولا يُرفع إلى المكتبة."), UiKit.full(scroll.getContext()));
        box.addView(tokenInput, UiKit.full(scroll.getContext()));
        AlertDialog dialog = new AlertDialog.Builder(scroll.getContext())
                .setTitle("تهيئة صلاحية رفع GitHub")
                .setView(box)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ مشفر", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String token = tokenInput.getText().toString().trim();
                GitHubCredentialStore.save(context, token);
                tokenInput.setText("");
                dialog.dismiss();
                refreshCredentialStatus();
                host.showToast("تم حفظ صلاحية الرفع مشفرة على هذا الجهاز");
                if (pendingPublish != null) publishWithToken(pendingPublish, token);
            } catch (Exception e) {
                tokenInput.setText("");
                host.showToast("تعذر حفظ صلاحية GitHub؛ تحقق من التوكن");
            }
        }));
        dialog.show();
    }

    private void publishWithToken(final ExperimentQueries.Record r, final String token) {
        if(r==null||!ExperimentQueries.isDeveloperReference(host.database(),r.id)
                ||!ReferenceLineageGuard.valid(host.database(),r)){
            host.showToast("رفض الإرسال: لا تتوافق التجربة مع جلسة مطور معتمدة");return;
        }
        publish.setEnabled(false);
        host.io().execute(() -> {
            try {
                GitHubReferencePublisher.Result raw = GitHubReferencePublisher.publish(token,host.database(),r);
                if(!raw.success)throw new IllegalStateException("staging HTTP "+raw.httpCode);
                ExperimentsDb.ReferenceArtifactRecord a = host.database().latestReferenceArtifact(r.chemicalUid, r.referenceKind);
                GitHubReferencePublisher.Result derived = null;
                if(a!=null&&ReferenceLineageGuard.artifactSourcesValid(host.database(),a))
                    derived=GitHubReferencePublisher.publishArtifact(token,host.database(),a);
                boolean complete=a==null||(derived!=null&&derived.success);
                String status=complete?"STAGED_FOR_REVIEW":"STAGED_EXPERIMENT_DERIVED_PENDING";
                String path=raw.path+(derived==null?"":" | "+derived.path);
                ReferencePublishAudit.record(host.database(),r.id,status,isoNow(),raw.payloadSha256,path);
                scroll.post(()->{host.showToast(complete?"أُرسلت التجربة للمراجعة دون تفعيل ChemCore":"أُرسلت التجربة؛ المرجع المشتق يحتاج مراجعة");host.refreshAllTabs();});
            } catch(Exception ex) {
                try{ReferencePublishAudit.record(host.database(),r.id,"REVIEW_STAGING_FAILED_"+ex.getClass().getSimpleName(),isoNow(),null,null);}catch(Exception ignored){}
                scroll.post(()->{host.showToast("تعذر الإرسال إلى منطقة المراجعة غير التشغيلية");host.refreshAllTabs();});
            }
        });
    }

    private static JSONObject json(String s) {
        try { return s == null ? new JSONObject() : new JSONObject(s); }
        catch (Exception e) { return new JSONObject(); }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String isoNow() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
