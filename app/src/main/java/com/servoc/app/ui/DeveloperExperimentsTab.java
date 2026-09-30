package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
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
import com.servoc.app.chemical.ChemicalExperimentStatusRepository;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.github.GitHubCredentialStore;
import com.servoc.app.github.GitHubReferencePublisher;
import com.servoc.app.github.ReferencePublishAudit;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.models.CalibrationModelLab;
import com.servoc.app.models.CalibrationRegressionModels;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
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

/** Operational developer experiment workspace for HR069. */
public final class DeveloperExperimentsTab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout list;
    private final ChemicalRepository chemicals;
    private final ChemicalExperimentStatusRepository statusRepo;
    private final TextView selectedMaterial,statusLine,powerStatus,r0Status,healthSummary,modelStatus,modelResult,summary,details,uploadStatus,kindHelp;
    private final EditText knownPpm,experimentNotes;
    private final Spinner referenceKindSpinner,modelSensorSpinner;
    private final Button pdf,xlsx,approve,publish;
    private final String[] modelSensorKeys=ManualR0Calibration.keys();
    private ChemicalRepository.Chemical selectedChemical;
    private ChemicalExperimentStatusRepository.Row selectedChemicalStatus;
    private long selectedId=-1;

    public DeveloperExperimentsTab(Context context,HazRobHost host){
        this.context=context;this.host=host;ManualR0Calibration.load(host.preferences());
        chemicals=new ChemicalRepository(host.database());statusRepo=new ChemicalExperimentStatusRepository(host.database());
        scroll=new ScrollView(context);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(context);root.setPadding(UiKit.dp(context,8),UiKit.dp(context,6),UiKit.dp(context,8),UiKit.dp(context,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(context,"التجارب المرجعية • HR069"),UiKit.full(context));
        TextView intro=UiKit.small(context,"اختيار المادة → قياس averaged → حفظ مسودة → مراجعة واعتماد → نشر المرجع إلى مكتبة HazRob المركزية. R0 معايرة محلية للجهاز ولا تُنشر كمعرفة مركزية.");intro.setTextColor(0xFF90A4AE);root.addView(intro,UiKit.full(context));

        LinearLayout chooser=UiKit.card(context);chooser.addView(UiKit.title(context,"اختيار المادة وحالة المكتبة"),UiKit.full(context));
        Button choose=UiKit.button(context,"فتح قائمة المواد • بحث وفرز");choose.setOnClickListener(v->openChemicalMatrix());chooser.addView(choose,UiKit.full(context));
        selectedMaterial=UiKit.text(context,"لم يتم اختيار مادة");selectedMaterial.setBackgroundColor(0xFF151A20);chooser.addView(selectedMaterial,UiKit.full(context));
        statusLine=UiKit.small(context,"بصمة —   PPM —   M4 —");statusLine.setBackgroundColor(0xFF151A20);chooser.addView(statusLine,UiKit.full(context));root.addView(chooser,UiKit.full(context));

        LinearLayout power=UiKit.card(context);power.addView(UiKit.title(context,"طاقة وحالة MQ"),UiKit.full(context));LinearLayout pb=UiKit.hbox(context);Button off=UiKit.button(context,"MQ OFF");off.setOnClickListener(v->mqPower(false));pb.addView(off,UiKit.weight(1));Button on=UiKit.button(context,"MQ ON — 8");on.setOnClickListener(v->mqPower(true));pb.addView(on,UiKit.weight(1));power.addView(pb,UiKit.full(context));powerStatus=UiKit.small(context,"—");powerStatus.setBackgroundColor(0xFF151A20);power.addView(powerStatus,UiKit.full(context));root.addView(power,UiKit.full(context));

        LinearLayout capture=UiKit.card(context);capture.addView(UiKit.title(context,"تنفيذ تجربة"),UiKit.full(context));
        capture.addView(UiKit.small(context,"نوع التجربة"),UiKit.full(context));referenceKindSpinner=new Spinner(context);ArrayAdapter<String> kindAdapter=new ArrayAdapter<>(context,android.R.layout.simple_spinner_item,new String[]{"بصمة تعريفية","معايرة كمية PPM/M4"});kindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);referenceKindSpinner.setAdapter(kindAdapter);capture.addView(referenceKindSpinner,UiKit.full(context));
        kindHelp=UiKit.small(context,"");kindHelp.setBackgroundColor(0xFF151A20);capture.addView(kindHelp,UiKit.full(context));referenceKindSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateKindHelp();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        knownPpm=field(capture,"التركيز المرجعي ppm — مطلوب للمعايرة الكمية","");knownPpm.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        experimentNotes=field(capture,"ملاحظات / مصدر المادة / ظروف الإجراء","");
        LinearLayout run=UiKit.hbox(context);Button start=UiKit.button(context,"بدء averaging");start.setOnClickListener(v->measurement(true));run.addView(start,UiKit.weight(1));Button cancel=UiKit.button(context,"إلغاء");cancel.setOnClickListener(v->measurement(false));run.addView(cancel,UiKit.weight(.7f));Button save=UiKit.button(context,"حفظ التجربة");save.setOnClickListener(v->captureReference());run.addView(save,UiKit.weight(1));capture.addView(run,UiKit.full(context));root.addView(capture,UiKit.full(context));updateKindHelp();

        LinearLayout device=UiKit.card(context);device.addView(UiKit.title(context,"معايرة الجهاز المحلية"),UiKit.full(context));r0Status=UiKit.small(context,"R0: —");r0Status.setBackgroundColor(0xFF151A20);device.addView(r0Status,UiKit.full(context));Button r0=UiKit.button(context,"صيانة / إعادة معايرة R0");r0.setOnClickListener(v->openR0Maintenance());device.addView(r0,UiKit.full(context));
        healthSummary=UiKit.small(context,"Baseline/Drift: —");healthSummary.setBackgroundColor(0xFF151A20);device.addView(healthSummary,UiKit.full(context));LinearLayout hb=UiKit.hbox(context);Button base=UiKit.button(context,"اعتماد Baseline نظيف");base.setOnClickListener(v->captureBaseline());hb.addView(base,UiKit.weight(1));Button drift=UiKit.button(context,"فحص Drift");drift.setOnClickListener(v->checkDrift());hb.addView(drift,UiKit.weight(1));device.addView(hb,UiKit.full(context));root.addView(device,UiKit.full(context));

        LinearLayout models=UiKit.card(context);models.addView(UiKit.title(context,"نماذج التحليل الكمي"),UiKit.full(context));modelStatus=UiKit.small(context,modelLibraryStatus());modelStatus.setBackgroundColor(0xFF151A20);models.addView(modelStatus,UiKit.full(context));models.addView(UiKit.small(context,"اختر حساسًا لمقارنة النماذج مع نقاط المعايرة المعتمدة للمادة الحالية."),UiKit.full(context));modelSensorSpinner=new Spinner(context);String[] labels=new String[modelSensorKeys.length];for(int i=0;i<labels.length;i++)labels[i]=modelSensorKeys[i].toUpperCase(Locale.US);ArrayAdapter<String> sensorAdapter=new ArrayAdapter<>(context,android.R.layout.simple_spinner_item,labels);sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);modelSensorSpinner.setAdapter(sensorAdapter);models.addView(modelSensorSpinner,UiKit.full(context));Button compare=UiKit.button(context,"مقارنة النماذج");compare.setOnClickListener(v->compareCalibrationModels());models.addView(compare,UiKit.full(context));modelResult=UiKit.small(context,"—");modelResult.setBackgroundColor(0xFF151A20);modelResult.setTextIsSelectable(true);models.addView(modelResult,UiKit.full(context));root.addView(models,UiKit.full(context));

        LinearLayout records=UiKit.card(context);records.addView(UiKit.title(context,"السجلات والاعتماد"),UiKit.full(context));summary=UiKit.small(context,"—");summary.setBackgroundColor(0xFF151A20);records.addView(summary,UiKit.full(context));details=UiKit.text(context,"اختر تجربة محفوظة.");details.setBackgroundColor(0xFF11171D);details.setTextIsSelectable(true);records.addView(details,UiKit.full(context));LinearLayout ex=UiKit.hbox(context);pdf=UiKit.button(context,"PDF");pdf.setOnClickListener(v->export("pdf"));ex.addView(pdf,UiKit.weight(1));xlsx=UiKit.button(context,"Excel");xlsx.setOnClickListener(v->export("xlsx"));ex.addView(xlsx,UiKit.weight(1));records.addView(ex,UiKit.full(context));approve=UiKit.button(context,"اعتماد وبناء المرجع العلمي");approve.setOnClickListener(v->approveSelected());records.addView(approve,UiKit.full(context));
        uploadStatus=UiKit.small(context,"صلاحية الرفع: —");uploadStatus.setBackgroundColor(0xFF151A20);records.addView(uploadStatus,UiKit.full(context));LinearLayout auth=UiKit.hbox(context);Button provision=UiKit.button(context,"تهيئة/تغيير صلاحية الرفع");provision.setOnClickListener(v->promptCredential(false));auth.addView(provision,UiKit.weight(1));Button clear=UiKit.button(context,"مسح الصلاحية");clear.setOnClickListener(v->{GitHubCredentialStore.clear(context);refreshUploadStatus();host.showToast("تم مسح صلاحية الرفع من هذا الجهاز");});auth.addView(clear,UiKit.weight(.7f));records.addView(auth,UiKit.full(context));publish=UiKit.button(context,"رفع التجربة والمرجع إلى مكتبة HazRob");publish.setOnClickListener(v->publishSelected());records.addView(publish,UiKit.full(context));list=UiKit.vbox(context);records.addView(list,UiKit.full(context));root.addView(records,UiKit.full(context));

        setActionState(false);selectInitialChemical();refresh();
    }

    public View view(){return scroll;}

    private EditText field(LinearLayout parent,String label,String value){parent.addView(UiKit.small(context,label),UiKit.full(context));EditText e=UiKit.input(context,value);parent.addView(e,UiKit.full(context));return e;}
    private String selectedKind(){return referenceKindSpinner.getSelectedItemPosition()==1?"calibration":"fingerprint";}
    private void updateKindHelp(){boolean cal="calibration".equals(selectedKind());kindHelp.setText(cal?"معايرة كمية: أدخل ppm معلومًا. M4 لا تصبح معتمدة إلا من منحنيات HR069 المبنية من تجارب معتمدة.":"بصمة تعريفية: يبني HazRob متجه الاستجابة من الجلسة ويحفظ الأصل للتدقيق.");}

    private void selectInitialChemical(){List<ChemicalRepository.Chemical> rows=chemicals.search("",1);if(!rows.isEmpty())selectChemical(rows.get(0),statusRepo.status(rows.get(0)));}
    private void openChemicalMatrix(){new ChemicalExperimentMatrixDialog(context,statusRepo,this::selectChemical).show();}
    private void selectChemical(ChemicalRepository.Chemical c,ChemicalExperimentStatusRepository.Row s){selectedChemical=c;selectedChemicalStatus=s;selectedMaterial.setText(ChemicalExperimentStatusRepository.displayName(c));statusLine.setText("بصمة "+s.fingerprintMark()+"   PPM "+s.ppmMark()+"   M4 "+s.m4Mark()+"   • "+recommendation(s));statusLine.setTextColor(s.m4?0xFF81C784:s.ppm==ChemicalExperimentStatusRepository.Level.PARTIAL?0xFFFFB74D:Color.WHITE);}
    private static String recommendation(ChemicalExperimentStatusRepository.Row s){if(s.fingerprint==ChemicalExperimentStatusRepository.Level.NONE)return"الأولوية: بناء بصمة";if(s.ppm==ChemicalExperimentStatusRepository.Level.NONE)return"الأولوية: تجارب كمية";if(!s.m4)return"الأولوية: استكمال M4";return"التغطية التشغيلية مكتملة";}

    private void mqPower(boolean on){host.io().execute(()->{try{host.robotApi().mqPower(on);host.showToast(on?"تم تشغيل مجموعة MQ الثمانية":"تم إيقاف مجموعة MQ");host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر التحكم بطاقة MQ");}});}
    private void measurement(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.showToast(start?"بدأ averaging":"أُلغي averaging");}catch(Exception e){host.showToast("تعذر إرسال الأمر");}});}

    private void captureReference(){
        final ChemicalRepository.Chemical chemical=selectedChemical;if(chemical==null){host.showToast("اختر مادة من القائمة");return;}final String kind=selectedKind();Double ppm=null;String rawPpm=knownPpm.getText().toString().trim();if(!rawPpm.isEmpty())try{double x=Double.parseDouble(rawPpm);if(!Double.isFinite(x)||x<=0)throw new IllegalArgumentException();ppm=x;}catch(Exception e){host.showToast("قيمة ppm غير صالحة");return;}if("calibration".equals(kind)&&ppm==null){host.showToast("أدخل التركيز المرجعي ppm");return;}
        JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات حية من الروبوت");return;}JSONObject m=d.optJSONObject("measurement");boolean ready=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;if(!ready){host.showToast("أكمل دورة averaging حتى READY أولاً");return;}JSONObject source=m.optJSONObject("mq");String session=host.lastMeasurementSessionId();if(source==null||source.length()==0||session==null||session.isEmpty()||!host.database().sessionExists(session)){host.showToast("جلسة القياس المرجعية غير مكتملة");return;}
        JSONObject env=d.optJSONObject("env");Double t=env!=null&&Double.isFinite(env.optDouble("temperature",Double.NaN))?env.optDouble("temperature"):null;Double rh=env!=null&&Double.isFinite(env.optDouble("humidity",Double.NaN))?env.optDouble("humidity"):null;JSONObject relay=d.optJSONObject("relay");long powerMs=relay==null?0:Math.max(0,relay.optLong("power_ms",0));int mask=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0)?1:0;String ts=isoNow();String analytics=AnalyticsSnapshot.toJson(d);
        JSONObject meta=new JSONObject();try{meta.put("schema","hazrob-reference-capture-v0.10.2");meta.put("reference_kind",kind);meta.put("session_id",session);meta.put("hardware_profile",d.optString("hardware_profile",HazRobHardwareProfile.ID));meta.put("mq_power",mask==1);meta.put("mq_power_gpio",25);meta.put("mq_power_architecture","single_relay_8_sensors");meta.put("warmup_observed_ms",powerMs);meta.put("warmup_source","relay.power_ms");meta.put("capture_state","ready_average");if(t!=null)meta.put("temperature_c",t);if(rh!=null)meta.put("rh_pct",rh);meta.put("notes",experimentNotes.getText().toString().trim());meta.put("analytics_version",LibraryStatus.ANALYTICS_VERSION);meta.put("calibration_version",LibraryStatus.CALIBRATION_VERSION);meta.put("mqdatascience_version",MqDataScience76Catalog.VERSION);meta.put("r0_scope","DEVICE_LOCAL_NOT_DISTRIBUTED");meta.put("r0_channels_available",r0Count());}catch(Exception ignored){}
        final Double known=ppm;final String feature=meta.toString();final String protocol=("calibration".equals(kind)?"Reference calibration: ":"Reference fingerprint: ")+chemical.label()+(known==null?"":" @ "+known+" ppm")+" • ready_average • session "+session;
        host.io().execute(()->{try{long captureId=host.database().saveCaptureForChemical(chemical.uid,chemical.legacyId,known,ts,session,t,rh,"ready_average",source.toString(),feature);if(captureId<=0)throw new IllegalStateException("capture");long id=host.database().saveReferenceExperiment(ts,d.toString(),protocol,analytics,session,captureId,chemical.uid,kind,known);if(id<=0)throw new IllegalStateException("experiment");host.showToast("حُفظت التجربة #"+id+" كمسودة للمراجعة");host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ التجربة المرجعية");}});
    }

    private int r0Count(){int n=0;for(String key:ManualR0Calibration.keys()){double x=ManualR0Calibration.get(key);if(Double.isFinite(x)&&x>0)n++;}return n;}
    private void openR0Maintenance(){
        LinearLayout box=UiKit.vbox(context);box.setPadding(UiKit.dp(context,14),UiKit.dp(context,6),UiKit.dp(context,14),0);Map<String,EditText> fields=new LinkedHashMap<>();for(String key:ManualR0Calibration.keys()){double x=ManualR0Calibration.get(key);EditText e=UiKit.input(context,Double.isFinite(x)?String.format(Locale.US,"%.6f",x):"");e.setHint(key.toUpperCase(Locale.US)+" R0");e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(e,UiKit.full(context));fields.put(key,e);}TextView note=UiKit.small(context,"هذه القيم خاصة بهذا الروبوت/مجموعة الحساسات ولا تُرفع إلى المكتبة المركزية.");note.setTextColor(0xFFFFB74D);box.addView(note,UiKit.full(context));
        AlertDialog d=new AlertDialog.Builder(context).setTitle("صيانة R0 المحلية").setView(box).setNegativeButton("إلغاء",null).setNeutralButton("مسح R0",null).setPositiveButton("حفظ",null).create();d.setOnShowListener(x->{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{int saved=0;for(Map.Entry<String,EditText> e:fields.entrySet()){String s=e.getValue().getText().toString().trim();if(s.isEmpty())continue;try{double q=Double.parseDouble(s);if(ManualR0Calibration.save(host.preferences(),e.getKey(),q))saved++;}catch(Exception ignored){}}host.showToast("تم حفظ R0 لـ "+saved+" حساسات محليًا");refreshR0();d.dismiss();});d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{for(String key:ManualR0Calibration.keys())ManualR0Calibration.clear(host.preferences(),key);host.showToast("تم مسح R0 المحلية");refreshR0();d.dismiss();});});d.show();
    }
    private void captureBaseline(){int n=host.captureCleanAirBaseline();host.showToast(n>0?"تم اعتماد baseline نظيف لـ "+n+" حساسات":"يلزم هواء نظيف وقراءات صالحة");refreshHealth();}
    private void checkDrift(){int n=host.evaluateCleanAirDrift();host.showToast(n>0?"تم فحص drift لـ "+n+" حساسات":"لا يوجد baseline صالح للمقارنة");refreshHealth();}

    private String modelLibraryStatus(){return "MQDataScience "+ModelRuntimeRegistry.adoptedCount()+"/"+MqDataScience76Catalog.COUNT+" • Native "+ModelRuntimeRegistry.nativeCount()+" • Verified "+ModelRuntimeRegistry.verifiedCount()+" • Approx "+ModelRuntimeRegistry.approximationCount();}
    private void compareCalibrationModels(){if(selectedChemical==null){host.showToast("اختر مادة");return;}int p=modelSensorSpinner.getSelectedItemPosition();final String key=p>=0&&p<modelSensorKeys.length?modelSensorKeys[p]:null;if(key==null)return;modelResult.setText("جارٍ التقييم...");host.io().execute(()->{CalibrationModelLab.Report r=CalibrationModelLab.evaluate(host.database(),selectedChemical.uid,key);String text=formatModelReport(r);scroll.post(()->modelResult.setText(text));});}
    private static String formatModelReport(CalibrationModelLab.Report r){StringBuilder s=new StringBuilder();s.append(r.sensorKey.toUpperCase(Locale.US)).append(" • عينات ").append(r.samples).append(" • مستويات ").append(r.concentrationLevels);if(r.scores.isEmpty())return s.append("\nلا توجد نقاط معايرة كافية.").toString();int n=Math.min(5,r.scores.size());for(int i=0;i<n;i++){CalibrationRegressionModels.Score q=r.scores.get(i);s.append("\n").append(i+1).append(") ").append(q.fit.name).append(String.format(Locale.US," • R² %.4f • RMSE %.5f",q.r2,q.logRmse));}return s.toString();}

    public void refresh(){refreshPower();refreshR0();refreshHealth();refreshUploadStatus();modelStatus.setText(modelLibraryStatus());if(selectedChemical!=null)selectChemical(selectedChemical,statusRepo.status(selectedChemical));renderRecords();}
    private void refreshPower(){JSONObject d=host.latestDiagnostics(),r=d==null?null:d.optJSONObject("relay");boolean on=r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);long ms=r==null?0:r.optLong("power_ms",0);powerStatus.setText((on?"MQ ON • 8":"MQ OFF")+" • GPIO25"+(on?" • تشغيل "+Math.max(0,ms/1000)+" s":""));}
    private void refreshR0(){int n=r0Count();r0Status.setText("R0 المحلية: "+n+"/8"+(n==8?" ✓":" • صيانة مطلوبة")+" • لا تُنشر مركزيًا");r0Status.setTextColor(n==8?0xFF81C784:0xFFFFB74D);}
    private void refreshHealth(){String ts=host.cleanAirBaselineTimestamp();StringBuilder s=new StringBuilder("Baseline: ").append(ts==null||ts.isEmpty()?"غير معتمد":ts);int alerts=0;for(String key:SensorHealthEngine.INSTALLED){SensorHealthEngine.Snapshot h=host.sensorHealth(key);if(h!=null&&!"OK".equalsIgnoreCase(SensorHealthEngine.compact(h)))alerts++;}s.append(" • حساسات تحتاج انتباه: ").append(alerts);healthSummary.setText(s.toString());}
    private void refreshUploadStatus(){boolean ok=GitHubCredentialStore.available(context);uploadStatus.setText(ok?"صلاحية الرفع: مهيأة ومشفرة على هذا الجهاز ✓":"صلاحية الرفع: غير مهيأة");uploadStatus.setTextColor(ok?0xFF81C784:0xFFFFB74D);}

    private void renderRecords(){list.removeAllViews();List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);summary.setText("المواد "+host.database().chemicalCount()+" • مختبرة "+statusRepo.countTestedMaterials()+" • بصمات محلية "+statusRepo.countFingerprintMaterials()+" • PPM فعال "+statusRepo.countPpmMaterials()+" • M4 "+statusRepo.countM4Materials()+" • تجارب معتمدة "+host.database().approvedCount());if(rows.isEmpty()){selectedId=-1;setActionState(false);list.addView(UiKit.small(context,"لا توجد تجارب محفوظة."),UiKit.full(context));return;}if(selectedId<0)selectedId=rows.get(0).id;boolean found=false;for(ExperimentQueries.Record r:rows){if(r.id==selectedId)found=true;String kind="fingerprint".equals(r.referenceKind)?"بصمة":"calibration".equals(r.referenceKind)?"PPM/M4":"قياس";Button b=UiKit.button(context,(r.approved?"✓ ":"")+"#"+r.id+" • "+kind+" • "+safe(r.ts));b.setOnClickListener(v->{selectedId=r.id;showSelected();});list.addView(b,UiKit.full(context));}if(!found)selectedId=rows.get(0).id;showSelected();}
    private void showSelected(){ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){setActionState(false);return;}setActionState(true);StringBuilder s=new StringBuilder("السجل #").append(r.id).append("\nUTC: ").append(safe(r.ts)).append("\nالنوع: ").append(safe(r.referenceKind)).append("\nSession: ").append(safe(r.sessionId)).append("\nCapture: ").append(r.captureId>0?r.captureId:"—").append("\nالاعتماد: ").append(r.approved?"معتمد":"مسودة");if(r.knownPpm!=null)s.append("\nReference ppm: ").append(r.knownPpm);if(r.approved){ExperimentsDb.ReferenceArtifactRecord a=host.database().latestReferenceArtifact(r.chemicalUid,r.referenceKind);if(a!=null)s.append("\nDerived: ").append(a.version).append("\nSHA-256: ").append(a.sha256);}String pub=ReferencePublishAudit.latestStatus(host.database(),r.id);if(!pub.isEmpty())s.append("\nCentral library: ").append(pub);details.setText(s.toString());approve.setEnabled(r.isReference()&&!r.approved);approve.setText(r.approved?"معتمد — المرجع محدث":"اعتماد وبناء المرجع العلمي");publish.setEnabled(r.approved&&r.isReference());}
    private void setActionState(boolean on){pdf.setEnabled(on);xlsx.setEnabled(on);approve.setEnabled(on);publish.setEnabled(false);}
    private void export(String format){if(selectedId>=0)host.requestExport(format,selectedId);}

    private void approveSelected(){if(selectedId<0)return;ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null||!r.isReference()){host.showToast("هذا السجل ليس تجربة مرجعية");return;}if(!"ready_average".equals(host.database().capturePhase(r.captureId))){host.showToast("لا يعتمد إلا التعرض averaged في حالة READY");return;}if(r.approved){host.showToast("التجربة معتمدة مسبقًا");return;}String ts=isoNow(),name=ReferenceBankExporter.fileName(r.id,r.ts);if(!host.database().approveExperiment(r.id,ts,name)){host.showToast("تعذر اعتماد السجل");return;}host.io().execute(()->{try{ReferenceArtifactBuilder.Result built=ReferenceArtifactBuilder.rebuild(host.database(),r.chemicalUid,r.referenceKind);host.showToast(built.message);host.refreshAllTabs();}catch(Exception e){host.showToast("اعتمدت التجربة، لكن تعذر تحديث المرجع المشتق");host.refreshAllTabs();}});}

    private void promptCredential(boolean continuePublish){EditText token=UiKit.input(context,"");token.setHint("GitHub fine-grained token");token.setSingleLine(true);token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);AlertDialog d=new AlertDialog.Builder(context).setTitle("تهيئة صلاحية الرفع").setMessage("تُحفظ مشفرة بمفتاح Android Keystore خاص بهذا الجهاز ولا تُدمج في APK.").setView(token).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{GitHubCredentialStore.save(context,token.getText().toString());d.dismiss();refreshUploadStatus();host.showToast("تمت تهيئة صلاحية الرفع");if(continuePublish)publishSelected();}catch(Exception e){host.showToast("تعذر حفظ صلاحية الرفع");}}));d.show();}
    private void publishSelected(){if(selectedId<0)return;final ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null||!r.approved||!r.isReference()){host.showToast("اعتمد تجربة مرجعية أولًا");return;}final String token=GitHubCredentialStore.load(context);if(token.length()<20){promptCredential(true);return;}publish.setEnabled(false);host.showToast("جارٍ الرفع إلى مكتبة HazRob المركزية...");host.io().execute(()->{try{GitHubReferencePublisher.Result raw=GitHubReferencePublisher.publish(token,r);ExperimentsDb.ReferenceArtifactRecord a=host.database().latestReferenceArtifact(r.chemicalUid,r.referenceKind);GitHubReferencePublisher.Result derived=a==null?null:GitHubReferencePublisher.publishArtifact(token,a);boolean ok=raw.success&&(derived==null||derived.success);String ref=raw.path+(derived==null?"":" | "+derived.path);ReferencePublishAudit.record(host.database(),r.id,ok?"PUBLISHED":"PARTIAL_OR_FAILED",isoNow(),raw.payloadSha256,ref);scroll.post(()->{host.showToast(ok?"تم الرفع إلى المكتبة المركزية":"الرفع غير مكتمل؛ راجع حالة السجل");host.refreshAllTabs();});}catch(Exception e){try{ReferencePublishAudit.record(host.database(),r.id,"FAILED_"+e.getClass().getSimpleName(),isoNow(),null,null);}catch(Exception ignored){}scroll.post(()->{host.showToast("تعذر الرفع إلى GitHub");host.refreshAllTabs();});}});}

    private static String safe(String s){return s==null?"":s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
