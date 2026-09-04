package com.servoc.app.ui;

import android.content.Context;
import android.text.InputType;
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
import com.servoc.app.github.GitHubReferencePublisher;
import com.servoc.app.github.ReferencePublishAudit;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.models.CalibrationModelLab;
import com.servoc.app.models.CalibrationRegressionModels;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.models.MqDataScienceModelProfile;
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

/** Developer-only laboratory for manual field experiments, R0 calibration and reference-bank work. */
public final class ExperimentsTab {
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout list;

    private final TextView summary;
    private final TextView details;
    private final TextView healthSummary;
    private final TextView r0Status;
    private final TextView powerStatus;
    private final TextView modelStatus;
    private final TextView modelResult;

    private final EditText knownPpm;
    private final EditText warmUsed;
    private final EditText experimentNotes;
    private final EditText githubToken;
    private final Button pdf;
    private final Button xlsx;
    private final Button approve;
    private final Button publish;

    private final ChemicalRepository chemicalRepo;
    private final Spinner chemicalSpinner;
    private final ArrayAdapter<String> chemicalAdapter;
    private final Spinner modelSensorSpinner;
    private final String[] modelSensorKeys=ManualR0Calibration.keys();

    private final List<ChemicalRepository.Chemical> chemicalOptions=new ArrayList<>();
    private final Map<String,EditText> r0Fields=new LinkedHashMap<>();

    private int loadedChemicalCount=-1;
    private long selectedId=-1;

    public ExperimentsTab(Context c,HazRobHost host){
        this.host=host;
        ManualR0Calibration.load(host.preferences());
        chemicalRepo=new ChemicalRepository(host.database());

        scroll=new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);
        root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));
        root.setBackgroundColor(0xFF0E1116);
        scroll.addView(root);

        root.addView(UiKit.title(c,"مختبر المطور • HazRob 0.9"),UiKit.full(c));
        root.addView(UiKit.small(c,
                "جميع حساسات MQ الثمانية تعمل من ريلاي طاقة واحد على GPIO25. التسخين واختيار تشغيل طاقة MQ يدويان، والمختبر يسجل حالة الطاقة الفعلية والبيئة والمعايرة مع كل تجربة، ولا يحول R0 وحده إلى اعتماد PPM."),UiKit.full(c));

        // ------------------------------------------------------------------
        // Manual MQ power
        // ------------------------------------------------------------------
        LinearLayout power=UiKit.card(c);
        power.addView(UiKit.title(c,"طاقة MQ اليدوية"),UiKit.full(c));
        power.addView(UiKit.small(c,
                "MQ Power Relay / GPIO25 → جميع الحساسات الثمانية\n"+
                "ADS1 0x48 → MQ2, MQ4, MQ5, MQ8\n"+
                "ADS2 0x49 → MQ3, MQ7, MQ9, MQ135"),UiKit.full(c));

        LinearLayout pb=UiKit.hbox(c);
        Button off=UiKit.button(c,"MQ OFF");off.setOnClickListener(v->mqPower(false));pb.addView(off,UiKit.weight(1));
        Button on=UiKit.button(c,"MQ ON — 8");on.setOnClickListener(v->mqPower(true));pb.addView(on,UiKit.weight(1));
        power.addView(pb,UiKit.full(c));

        powerStatus=UiKit.small(c,"حالة الطاقة: —");
        powerStatus.setBackgroundColor(0xFF151A20);
        power.addView(powerStatus,UiKit.full(c));
        root.addView(power,UiKit.full(c));

        // ------------------------------------------------------------------
        // Manual R0
        // ------------------------------------------------------------------
        LinearLayout r0=UiKit.card(c);
        r0.addView(UiKit.title(c,"R0 اليدوي لكل حساس"),UiKit.full(c));
        r0.addView(UiKit.small(c,
                "أدخل R0 الذي تم تحديده تجريبيًا لكل حساس. لا تستخدم Rs الحالية مباشرة كـR0 إلا إذا كان بروتوكول المعايرة الذي تتبعه يعرّف ذلك صراحة."),UiKit.full(c));

        for(String key:ManualR0Calibration.keys()){
            double v=ManualR0Calibration.get(key);
            EditText e=field(c,r0,key.toUpperCase(Locale.US)+" R0",
                    Double.isFinite(v)?String.format(Locale.US,"%.6f",v):"");
            e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            r0Fields.put(key,e);
        }

        LinearLayout rb=UiKit.hbox(c);
        Button saveR0=UiKit.button(c,"حفظ قيم R0");saveR0.setOnClickListener(v->saveR0());rb.addView(saveR0,UiKit.weight(1));
        Button clearR0=UiKit.button(c,"مسح R0");clearR0.setOnClickListener(v->clearR0());rb.addView(clearR0,UiKit.weight(1));
        r0.addView(rb,UiKit.full(c));

        r0Status=UiKit.small(c,"بانتظار قراءات الحساسات...");
        r0Status.setTextIsSelectable(true);
        r0Status.setBackgroundColor(0xFF151A20);
        r0.addView(r0Status,UiKit.full(c));
        root.addView(r0,UiKit.full(c));

        // ------------------------------------------------------------------
        // Controlled reference exposure
        // ------------------------------------------------------------------
        LinearLayout capture=UiKit.card(c);
        capture.addView(UiKit.title(c,"تجربة مرجعية عملية"),UiKit.full(c));
        capture.addView(UiKit.small(c,
                "اختر مركبًا وتركيزًا مرجعيًا معلومًا. سجل زمن التسخين الذي طبقته فعليًا قبل أخذ هذه النقطة؛ لا يفترض التطبيق أن قيمة الإعداد تعني أن التسخين تم فعليًا."),UiKit.full(c));

        capture.addView(UiKit.small(c,"المركب المرجعي"),UiKit.full(c));
        chemicalSpinner=new Spinner(c);
        chemicalAdapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,new ArrayList<>());
        chemicalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chemicalSpinner.setAdapter(chemicalAdapter);
        capture.addView(chemicalSpinner,UiKit.full(c));

        knownPpm=field(c,capture,"التركيز المرجعي ppm","");
        knownPpm.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        warmUsed=field(c,capture,"زمن التسخين الفعلي المستخدم لهذه النقطة (ثانية)",String.valueOf(host.preferences().getInt("warm",60)));
        warmUsed.setInputType(InputType.TYPE_CLASS_NUMBER);
        experimentNotes=field(c,capture,"ملاحظات التجربة / الغاز المرجعي / ظروف الإجراء","");

        LinearLayout run=UiKit.hbox(c);
        Button start=UiKit.button(c,"بدء averaging");start.setOnClickListener(v->measurement(true));run.addView(start,UiKit.weight(1));
        Button cancel=UiKit.button(c,"إلغاء averaging");cancel.setOnClickListener(v->measurement(false));run.addView(cancel,UiKit.weight(1));
        Button captureBtn=UiKit.button(c,"حفظ التعرض");captureBtn.setOnClickListener(v->captureReference());run.addView(captureBtn,UiKit.weight(1));
        capture.addView(run,UiKit.full(c));
        root.addView(capture,UiKit.full(c));

        // ------------------------------------------------------------------
        // Sensor health
        // ------------------------------------------------------------------
        LinearLayout health=UiKit.card(c);
        health.addView(UiKit.title(c,"صحة الحساسات ومرجع الهواء النظيف"),UiKit.full(c));
        health.addView(UiKit.small(c,"Baseline/Drift أداة صحة واستقرار وليست بديلًا عن R0 أو معايرة الغاز المرجعي."),UiKit.full(c));
        LinearLayout hb=UiKit.hbox(c);
        Button base=UiKit.button(c,"اعتماد Baseline نظيف");base.setOnClickListener(v->captureBaseline());hb.addView(base,UiKit.weight(1));
        Button check=UiKit.button(c,"فحص Drift");check.setOnClickListener(v->checkDrift());hb.addView(check,UiKit.weight(1));
        health.addView(hb,UiKit.full(c));
        healthSummary=UiKit.small(c,"لا يوجد تقييم بعد.");
        healthSummary.setTextIsSelectable(true);
        health.addView(healthSummary,UiKit.full(c));
        root.addView(health,UiKit.full(c));

        // ------------------------------------------------------------------
        // MQDataScience 76-model laboratory
        // ------------------------------------------------------------------
        LinearLayout models=UiKit.card(c);
        models.addView(UiKit.title(c,"مختبر نماذج MQDataScience الـ76"),UiKit.full(c));
        modelStatus=UiKit.small(c,modelLibraryStatus());
        modelStatus.setTextIsSelectable(true);
        models.addView(modelStatus,UiKit.full(c));
        models.addView(UiKit.small(c,
                "جميع النماذج الـ76 ADOPTED. UPSTREAM-VERIFIED يعني أن port Android يطابق الصيغة/الإجراء المثبت قدر الإمكان للبيانات الحالية، بينما APPROXIMATION يعني تنفيذًا عاملًا ما زال يحتاج مطابقة المصدر. لا يعتمد التطبيق نموذجًا تلقائيًا."),UiKit.full(c));

        final TextView catalogView=UiKit.small(c,"");
        catalogView.setTextIsSelectable(true);
        catalogView.setVisibility(View.GONE);
        catalogView.setBackgroundColor(0xFF151A20);
        Button showCatalog=UiKit.button(c,"عرض خريطة الـ76 نموذجًا وحالة النقل");
        showCatalog.setOnClickListener(v->{
            boolean show=catalogView.getVisibility()!=View.VISIBLE;
            catalogView.setVisibility(show?View.VISIBLE:View.GONE);
            if(show)catalogView.setText(formatModelCatalog());
        });
        models.addView(showCatalog,UiKit.full(c));
        models.addView(catalogView,UiKit.full(c));

        models.addView(UiKit.small(c,"الحساس المراد تقييم منحنياته"),UiKit.full(c));
        modelSensorSpinner=new Spinner(c);
        String[] sensorLabels=new String[modelSensorKeys.length];
        for(int i=0;i<modelSensorKeys.length;i++)sensorLabels[i]=modelSensorKeys[i].toUpperCase(Locale.US);
        ArrayAdapter<String> sensorAdapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,sensorLabels);
        sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSensorSpinner.setAdapter(sensorAdapter);
        models.addView(modelSensorSpinner,UiKit.full(c));

        Button compareModels=UiKit.button(c,"مقارنة نماذج المنحنى المتاحة مع التجارب المرجعية");
        compareModels.setOnClickListener(v->compareCalibrationModels());
        models.addView(compareModels,UiKit.full(c));

        modelResult=UiKit.small(c,
                "اختر المركب والحساس، ثم سجل عدة تعرضات مرجعية بتراكيز معلومة. المقارنة الحالية تستخدم فقط نماذج CURVE_1D المنفذة على Android؛ النماذج متعددة المتغيرات والزمنية وML لا تُشغّل على Dataset غير مناسب.");
        modelResult.setTextIsSelectable(true);
        modelResult.setBackgroundColor(0xFF151A20);
        models.addView(modelResult,UiKit.full(c));
        root.addView(models,UiKit.full(c));

        // ------------------------------------------------------------------
        // Stored experiments and exports
        // ------------------------------------------------------------------
        summary=UiKit.text(c,"التجارب المعتمدة: 0");
        summary.setBackgroundColor(0xFF151A20);
        root.addView(summary,UiKit.full(c));

        details=UiKit.text(c,"اختر تجربة محفوظة لمراجعتها وتصديرها أو اعتمادها للبنك المرجعي.");
        details.setTextIsSelectable(true);
        details.setBackgroundColor(0xFF11171D);
        root.addView(details,UiKit.full(c));

        LinearLayout export=UiKit.hbox(c);
        pdf=UiKit.button(c,"PDF");pdf.setOnClickListener(v->export("pdf"));export.addView(pdf,UiKit.weight(1));
        xlsx=UiKit.button(c,"Excel");xlsx.setOnClickListener(v->export("xlsx"));export.addView(xlsx,UiKit.weight(1));
        root.addView(export,UiKit.full(c));

        approve=UiKit.button(c,"اعتماد وتجهيز سجل البنك المرجعي");
        approve.setOnClickListener(v->approveSelected());
        root.addView(approve,UiKit.full(c));

        LinearLayout github=UiKit.card(c);
        github.addView(UiKit.title(c,"GitHub Reference Bank"),UiKit.full(c));
        github.addView(UiKit.small(c,
                "الهدف: "+GitHubReferencePublisher.REPOSITORY+" • branch: "+GitHubReferencePublisher.BRANCH+"\n"+
                "Fine-grained token يبقى في ذاكرة الشاشة فقط ولا يُحفظ."),UiKit.full(c));
        githubToken=UiKit.input(c,"");
        githubToken.setHint("GitHub token — session only");
        githubToken.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        github.addView(githubToken,UiKit.full(c));
        publish=UiKit.button(c,"نشر السجل المعتمد إلى GitHub");
        publish.setOnClickListener(v->publishSelected());
        github.addView(publish,UiKit.full(c));
        root.addView(github,UiKit.full(c));

        list=UiKit.vbox(c);
        root.addView(list,UiKit.full(c));
        setActionState(false);
        refreshChemicalOptions();
    }

    private EditText field(Context c,LinearLayout p,String label,String value){
        p.addView(UiKit.small(c,label),UiKit.full(c));
        EditText e=UiKit.input(c,value);
        p.addView(e,UiKit.full(c));
        return e;
    }

    public View view(){return scroll;}

    private void refreshChemicalOptions(){
        int count=chemicalRepo.count();
        if(count==loadedChemicalCount&&chemicalAdapter.getCount()>0)return;
        long keep=-1;
        int old=chemicalSpinner.getSelectedItemPosition();
        if(old>=0&&old<chemicalOptions.size())keep=chemicalOptions.get(old).uid;
        List<ChemicalRepository.Chemical> rows=chemicalRepo.search("",5000);
        chemicalOptions.clear();
        chemicalOptions.addAll(rows);
        chemicalAdapter.clear();
        if(rows.isEmpty())chemicalAdapter.add("لا توجد مركبات في قاعدة البيانات");
        else for(ChemicalRepository.Chemical c:rows)chemicalAdapter.add(optionLabel(c));
        chemicalAdapter.notifyDataSetChanged();
        loadedChemicalCount=count;
        if(keep>0){
            for(int i=0;i<chemicalOptions.size();i++){
                if(chemicalOptions.get(i).uid==keep){chemicalSpinner.setSelection(i);break;}
            }
        }
    }

    private static String optionLabel(ChemicalRepository.Chemical c){
        String name=c.arabicName!=null&&!c.arabicName.trim().isEmpty()?c.arabicName:c.name;
        StringBuilder s=new StringBuilder(name==null?"":name);
        if(c.formula!=null&&!c.formula.isEmpty())s.append(" • ").append(c.formula);
        if(c.cas!=null&&!c.cas.isEmpty())s.append(" • CAS ").append(c.cas);
        return s.toString();
    }

    private ChemicalRepository.Chemical selectedChemical(){
        int p=chemicalSpinner.getSelectedItemPosition();
        return p>=0&&p<chemicalOptions.size()?chemicalOptions.get(p):null;
    }

    private void mqPower(boolean on){
        host.io().execute(()->{
            try{
                host.robotApi().mqPower(on);
                host.showToast("MQ: "+HazRobHardwareProfile.groupLabel(on?1:0));
                host.refreshAllTabs();
            }catch(Exception e){host.showToast("تعذر التحكم بطاقة MQ");}
        });
    }

    private void measurement(boolean start){
        host.io().execute(()->{
            try{
                if(start)host.robotApi().startMeasurement();
                else host.robotApi().cancelMeasurement();
                host.showToast(start?"بدأ averaging؛ يجب أن تبقى طاقة MQ ON طوال الجلسة":"أُلغي averaging");
            }catch(Exception e){host.showToast("تعذر إرسال الأمر");}
        });
    }

    private void saveR0(){
        int saved=0;
        for(Map.Entry<String,EditText> x:r0Fields.entrySet()){
            String s=x.getValue().getText().toString().trim();
            if(s.isEmpty())continue;
            try{
                double v=Double.parseDouble(s);
                if(ManualR0Calibration.save(host.preferences(),x.getKey(),v))saved++;
            }catch(Exception ignored){}
        }
        host.showToast("تم حفظ R0 لـ "+saved+" حساسات");
        host.refreshAllTabs();
        refreshR0();
    }

    private void clearR0(){
        for(String k:ManualR0Calibration.keys()){
            ManualR0Calibration.clear(host.preferences(),k);
            EditText e=r0Fields.get(k);
            if(e!=null)e.setText("");
        }
        host.showToast("تم مسح قيم R0 اليدوية");
        host.refreshAllTabs();
        refreshR0();
    }

    private void captureBaseline(){
        int n=host.captureCleanAirBaseline();
        if(n<=0){host.showToast("يلزم قراءات صالحة في هواء نظيف قبل اعتماد baseline");return;}
        host.showToast("تم اعتماد baseline نظيف لـ "+n+" حساسات");
        refreshHealth();
        host.refreshAllTabs();
    }

    private void checkDrift(){
        int n=host.evaluateCleanAirDrift();
        if(n<=0){host.showToast("لا يوجد baseline صالح أو قراءة مناسبة للمقارنة");return;}
        host.showToast("تم فحص drift لـ "+n+" حساسات");
        refreshHealth();
        host.refreshAllTabs();
    }

    private void captureReference(){
        final ChemicalRepository.Chemical chemical=selectedChemical();
        double ppm;
        int warmSeconds;
        try{ppm=Double.parseDouble(knownPpm.getText().toString().trim());}
        catch(Exception e){host.showToast("أدخل تركيزًا مرجعيًا صالحًا");return;}
        try{warmSeconds=Integer.parseInt(warmUsed.getText().toString().trim());}
        catch(Exception e){host.showToast("أدخل زمن التسخين الفعلي بالثواني");return;}

        if(chemical==null){host.showToast("اختر مركبًا مرجعيًا");return;}
        if(!Double.isFinite(ppm)||ppm<=0){host.showToast("التركيز المرجعي غير صالح");return;}
        if(warmSeconds<0||warmSeconds>86400){host.showToast("زمن التسخين الفعلي غير صالح");return;}

        JSONObject d=host.latestDiagnostics();
        if(d==null){host.showToast("لا توجد بيانات حية من الروبوت");return;}
        JSONObject m=d.optJSONObject("measurement");
        boolean ready=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;
        JSONObject source=ready?m.optJSONObject("mq"):d.optJSONObject("mq");
        if(source==null||source.length()==0){host.showToast("لا توجد قراءات MQ صالحة للحفظ");return;}

        JSONObject e=d.optJSONObject("env");
        Double t=e!=null&&Double.isFinite(e.optDouble("temperature",Double.NaN))?e.optDouble("temperature"):null;
        Double rh=e!=null&&Double.isFinite(e.optDouble("humidity",Double.NaN))?e.optDouble("humidity"):null;
        String ts=isoNow();
        String session="DEV-"+System.currentTimeMillis();
        String sensor=source.toString();
        String state=ready?"ready_average":"manual_live";
        String analytics=AnalyticsSnapshot.toJson(d);
        JSONObject relay=d.optJSONObject("relay");
        int mask=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0)?1:0;

        JSONObject meta=new JSONObject();
        try{
            meta.put("schema","hazrob-reference-capture-v0.9");
            meta.put("hardware_profile",d.optString("hardware_profile",HazRobHardwareProfile.ID));
            meta.put("relay_mask",mask);
            meta.put("mq_power",mask==1);
            meta.put("mq_power_gpio",25);
            meta.put("mq_power_label",HazRobHardwareProfile.groupLabel(mask));
            meta.put("manual_power",true);
            meta.put("manual_warmup",true);
            meta.put("warmup_actual_s",warmSeconds);
            meta.put("warmup_setting_s",host.preferences().getInt("warm",60));
            meta.put("measurement_setting_s",host.preferences().getInt("measure",30));
            meta.put("capture_state",state);
            if(t!=null)meta.put("temperature_c",t);
            if(rh!=null)meta.put("rh_pct",rh);
            meta.put("notes",experimentNotes.getText().toString().trim());
            meta.put("analytics_version",LibraryStatus.ANALYTICS_VERSION);
            meta.put("calibration_version",LibraryStatus.CALIBRATION_VERSION);
            meta.put("mqdatascience_version",MqDataScience76Catalog.VERSION);
            meta.put("mqdatascience_commit",MqDataScience76Catalog.PINNED_COMMIT);
            JSONObject r0=new JSONObject();
            for(String k:ManualR0Calibration.keys()){
                double rv=ManualR0Calibration.get(k);
                if(Double.isFinite(rv)&&rv>0)r0.put(k,rv);
            }
            meta.put("manual_r0",r0);
        }catch(Exception ignored){}

        String protocol="Reference exposure: "+chemical.label()+" @ "+ppm+" ppm • "+state+" • "+HazRobHardwareProfile.groupLabel(mask)+" • GPIO25 • warm "+warmSeconds+" s";

        final double known=ppm;
        final String featurePayload=meta.toString();
        host.io().execute(()->{
            try{
                host.database().saveCaptureForChemical(chemical.uid,chemical.legacyId,known,ts,session,t,rh,state,sensor,featurePayload);
                long id=host.database().save(ts,d.toString(),protocol,analytics);
                host.showToast("حُفظ التعرض المرجعي للتجربة #"+id);
                host.refreshAllTabs();
            }catch(Exception ex){host.showToast("تعذر حفظ التجربة المرجعية");}
        });
    }

    private String modelLibraryStatus(){
        return "MQDataScience: "+ModelRuntimeRegistry.adoptedCount()+" / "+MqDataScience76Catalog.COUNT+" ADOPTED"+
                " • Native "+ModelRuntimeRegistry.nativeCount()+
                " • Verified "+ModelRuntimeRegistry.verifiedCount()+
                " • Approx "+ModelRuntimeRegistry.approximationCount()+
                " • Pending "+ModelRuntimeRegistry.pendingPortCount();
    }

    private String formatModelCatalog(){
        StringBuilder s=new StringBuilder();
        s.append("Source: ").append(MqDataScience76Catalog.SOURCE).append(" v").append(MqDataScience76Catalog.VERSION)
         .append(" @ ").append(MqDataScience76Catalog.PINNED_COMMIT).append("\n\n");
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++){
            s.append('#').append(i+1).append(" • ").append(MqDataScience76Catalog.NAMES[i]).append('\n')
             .append("   ").append(ModelRuntimeRegistry.runtimeStatus(i)).append(" • ")
             .append(MqDataScienceModelProfile.requirementLabel(i)).append(" • recommended ≥")
             .append(MqDataScienceModelProfile.recommendedMinimumSamples(i)).append(" samples\n");
        }
        return s.toString();
    }

    private void compareCalibrationModels(){
        final ChemicalRepository.Chemical chemical=selectedChemical();
        int p=modelSensorSpinner.getSelectedItemPosition();
        final String sensorKey=p>=0&&p<modelSensorKeys.length?modelSensorKeys[p]:null;
        if(chemical==null||sensorKey==null){host.showToast("اختر المركب والحساس");return;}

        modelResult.setText("جارٍ تقييم التجارب المرجعية لـ "+sensorKey.toUpperCase(Locale.US)+"...");
        host.io().execute(()->{
            final CalibrationModelLab.Report report=CalibrationModelLab.evaluate(host.database(),chemical.uid,sensorKey);
            final String text=formatModelReport(chemical,report);
            scroll.post(()->modelResult.setText(text));
        });
    }

    private String formatModelReport(ChemicalRepository.Chemical chemical,CalibrationModelLab.Report report){
        StringBuilder s=new StringBuilder();
        s.append("المركب: ").append(optionLabel(chemical)).append('\n');
        s.append("الحساس: ").append(report.sensorKey.toUpperCase(Locale.US)).append('\n');
        s.append("العينات الصالحة: ").append(report.samples).append(" • مستويات التركيز: ").append(report.concentrationLevels).append('\n');
        s.append("المكتبة: 76/76 ADOPTED • Native ").append(ModelRuntimeRegistry.nativeCount())
         .append(" • Verified ").append(ModelRuntimeRegistry.verifiedCount())
         .append(" • Approx ").append(ModelRuntimeRegistry.approximationCount()).append("\n\n");

        if(report.scores.isEmpty()){
            s.append("لا توجد نقاط كافية لتكوين نموذج صالح لهذا الزوج sensor/analyte.\n")
             .append("سجل تعرضات مرجعية عند عدة تراكيز معلومة مع R0 صالح، ثم أعد المقارنة.");
            return s.toString();
        }

        int shown=Math.min(14,report.scores.size());
        s.append("ترتيب نماذج CURVE_1D المنفذة حاليًا:\n");
        for(int i=0;i<shown;i++){
            CalibrationRegressionModels.Score q=report.scores.get(i);
            int idx=q.fit.catalogIndex;
            s.append('\n').append(i+1).append(") #").append(idx+1).append(" • ").append(q.fit.name).append('\n');
            s.append("   ").append(ModelRuntimeRegistry.runtimeStatus(idx)).append('\n');
            s.append("   DATA: ").append(MqDataScienceModelProfile.requirementLabel(idx))
             .append(" • recommended ≥").append(MqDataScienceModelProfile.recommendedMinimumSamples(idx)).append('\n');
            s.append("   ").append(q.fit.equation()).append('\n');
            s.append(String.format(Locale.US,"   R²(log)=%.5f • log-RMSE=%.6f • samples=%d • %s\n",
                    q.r2,q.logRmse,q.samples,q.crossValidated?"LOO cross-validated":"fit-only"));
        }
        s.append("\nلا يتم تفعيل أفضل نموذج تلقائيًا. الاعتماد يحتاج مراجعة نطاق التركيز، التكرارية، drift، زمن التسخين الفعلي، ظروف T/RH، وبروتوكول heater المناسب.");
        return s.toString();
    }

    public void refresh(){
        refreshChemicalOptions();
        refreshHealth();
        refreshR0();
        refreshPower();
        modelStatus.setText(modelLibraryStatus());

        list.removeAllViews();
        List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);
        summary.setText("قاعدة المركبات: "+host.database().chemicalCount()+
                " • التجارب/القياسات: "+host.database().count()+
                " • التعرضات المرجعية: "+host.database().captureCount()+
                " • المعتمدة: "+host.database().approvedCount());

        if(rows.isEmpty()){
            selectedId=-1;
            setActionState(false);
            list.addView(UiKit.small(list.getContext(),"لا توجد سجلات بعد."),UiKit.full(list.getContext()));
            return;
        }

        if(selectedId<0)selectedId=rows.get(0).id;
        boolean still=false;
        for(ExperimentQueries.Record r:rows){
            if(r.id==selectedId)still=true;
            Button b=UiKit.button(list.getContext(),(r.approved?"✓ مرجعي  ":"")+"#"+r.id+"   "+safe(r.ts)+"   "+safe(r.protocol));
            b.setOnClickListener(v->select(r.id));
            list.addView(b,UiKit.full(list.getContext()));
        }
        if(!still)selectedId=rows.get(0).id;
        showSelected();
    }

    private void refreshPower(){
        JSONObject d=host.latestDiagnostics();
        JSONObject r=d==null?null:d.optJSONObject("relay");
        boolean on=r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);
        long ms=r==null?0:r.optLong("power_ms",0);
        powerStatus.setText("الحالة: "+HazRobHardwareProfile.groupLabel(on?1:0)+" • GPIO25"+(on?" • "+ms+" ms":"")+" • يدوي");
    }

    private void refreshR0(){
        JSONObject d=host.latestDiagnostics();
        JSONObject mq=d==null?null:d.optJSONObject("mq");
        StringBuilder s=new StringBuilder();
        for(String k:ManualR0Calibration.keys()){
            if(s.length()>0)s.append('\n');
            JSONObject q=mq==null?null:mq.optJSONObject(k);
            double rs=q==null?Double.NaN:q.optDouble("rs",Double.NaN);
            double ratio=q==null?Double.NaN:q.optDouble("ratio",Double.NaN);
            double r0=ManualR0Calibration.get(k);
            s.append(k.toUpperCase(Locale.US))
             .append(" • R0 ").append(Double.isFinite(r0)?String.format(Locale.US,"%.5f",r0):"—")
             .append(" • Rs ").append(Double.isFinite(rs)?String.format(Locale.US,"%.5f",rs):"—")
             .append(" • Rs/R0 ").append(Double.isFinite(ratio)?String.format(Locale.US,"%.4f",ratio):"—");
        }
        r0Status.setText(s.toString());
    }

    private void refreshHealth(){
        StringBuilder s=new StringBuilder();
        String ts=host.cleanAirBaselineTimestamp();
        s.append("Baseline: ").append(ts==null||ts.isEmpty()?"غير معتمد":ts);
        for(String key:SensorHealthEngine.INSTALLED){
            SensorHealthEngine.Snapshot h=host.sensorHealth(key);
            s.append('\n').append(key.toUpperCase(Locale.US)).append(" • ").append(SensorHealthEngine.compact(h));
            if(h!=null&&Double.isFinite(h.noiseCv))s.append(String.format(Locale.US," • noise CV %.1f%%",100*h.noiseCv));
        }
        healthSummary.setText(s.toString());
    }

    private void select(long id){selectedId=id;showSelected();}

    private void showSelected(){
        ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);
        if(r==null){setActionState(false);return;}
        setActionState(true);

        JSONObject d=json(r.payload);
        JSONObject m=d.optJSONObject("measurement");
        JSONObject e=d.optJSONObject("env");
        JSONObject relay=d.optJSONObject("relay");
        JSONObject a=json(r.analytics);
        int results=a.optJSONArray("results")==null?0:a.optJSONArray("results").length();

        StringBuilder s=new StringBuilder("السجل #").append(r.id)
                .append("\nUTC: ").append(safe(r.ts))
                .append("\nالبروتوكول: ").append(safe(r.protocol))
                .append("\nHardware: ").append(d.optString("hardware_profile","legacy/unknown"))
                .append("\nMQ Power: ").append(relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0)?"ON • GPIO25":"OFF • GPIO25")
                .append("\nالاعتماد المرجعي: ").append(r.approved?"معتمد":"غير معتمد");
        if(r.approved)s.append("\nاعتمد: ").append(safe(r.approvedTs)).append("\nملف: ").append(safe(r.referenceName));
        String published=ReferencePublishAudit.latestStatus(host.database(),r.id);
        if(!published.isEmpty())s.append("\nGitHub: ").append(published);
        if(m!=null)s.append("\nدورة MQ: ").append(m.optString("state",""));
        if(e!=null&&e.optBoolean("ok",false)){
            s.append("\nالبيئة: ").append(e.optString("type","ENV"))
             .append(" • ").append(fmt(e.optDouble("temperature",Double.NaN))).append(" °C")
             .append(" • ").append(fmt(e.optDouble("humidity",Double.NaN))).append(" %RH")
             .append(" • ").append(fmt(e.optDouble("pressure",Double.NaN))).append(" hPa");
        }
        s.append("\nنتائج التحليل المسجلة: ").append(results);
        details.setText(s.toString());
        approve.setText(r.approved?"إعادة تصدير السجل المرجعي":"اعتماد وتجهيز سجل البنك المرجعي");
        publish.setEnabled(r.approved);
    }

    private void setActionState(boolean on){
        pdf.setEnabled(on);
        xlsx.setEnabled(on);
        approve.setEnabled(on);
        publish.setEnabled(false);
    }

    private void export(String format){if(selectedId>=0)host.requestExport(format,selectedId);}

    private void approveSelected(){
        if(selectedId<0)return;
        ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);
        if(r==null)return;
        if(!r.approved){
            String ts=isoNow();
            String name=ReferenceBankExporter.fileName(r.id,r.ts);
            if(!host.database().approveExperiment(r.id,ts,name)){host.showToast("تعذر اعتماد السجل");return;}
            host.showToast("تم الاعتماد المحلي؛ يمكن حفظ JSON أو نشره إلى GitHub");
        }
        host.refreshAllTabs();
        host.requestReferenceBankExport(selectedId);
    }

    private void publishSelected(){
        if(selectedId<0)return;
        final ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);
        if(r==null||!r.approved){host.showToast("اعتمد السجل أولًا");return;}
        final String token=githubToken.getText().toString().trim();
        if(token.length()<20){host.showToast("أدخل GitHub token صالحًا لهذه الجلسة");return;}
        publish.setEnabled(false);
        host.showToast("جارٍ نشر السجل المرجعي...");
        host.io().execute(()->{
            try{
                GitHubReferencePublisher.Result x=GitHubReferencePublisher.publish(token,r);
                String ref=x.success?GitHubReferencePublisher.BRANCH+":"+x.path+(x.commitSha.isEmpty()?"":" @ "+x.commitSha):x.message;
                ReferencePublishAudit.record(host.database(),r.id,x.success?"PUBLISHED":"FAILED_HTTP_"+x.httpCode,isoNow(),x.payloadSha256,ref);
                scroll.post(()->{
                    host.showToast(x.success?"تم نشر السجل إلى GitHub Reference Bank":"فشل النشر: "+x.message);
                    host.refreshAllTabs();
                });
            }catch(Exception ex){
                try{ReferencePublishAudit.record(host.database(),r.id,"FAILED_"+ex.getClass().getSimpleName(),isoNow(),null,null);}catch(Exception ignored){}
                scroll.post(()->{
                    host.showToast("تعذر النشر إلى GitHub");
                    host.refreshAllTabs();
                });
            }
        });
    }

    private static JSONObject json(String s){
        try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}
    }

    private static String safe(String s){return s==null?"":s;}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}
    private static String isoNow(){
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
