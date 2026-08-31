package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.report.ReferenceBankExporter;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Developer-only laboratory for known-reference captures, calibration datasets and reference-bank approval. */
public final class ExperimentsTab {
    private final HazRobHost host;private final ScrollView scroll;private final LinearLayout list;private final TextView summary,details,healthSummary;private final EditText chemicalQuery,knownPpm;private final Button pdf,xlsx,approve;private long selectedId=-1;
    public ExperimentsTab(Context c,HazRobHost host){
        this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);root.addView(UiKit.title(c,"مختبر المطور • التجارب والمعايرة"),UiKit.full(c));root.addView(UiKit.small(c,"هذه الشاشة مخصصة للتعرضات المرجعية المعروفة وبناء بيانات المعايرة والبصمات. لا تظهر في وضع المستخدم."),UiKit.full(c));

        LinearLayout capture=UiKit.card(c);capture.addView(UiKit.title(c,"تجربة مرجعية"),UiKit.full(c));chemicalQuery=field(c,capture,"المادة: UID أو CAS أو الاسم الدقيق","");knownPpm=field(c,capture,"التركيز المرجعي ppm","");LinearLayout run=UiKit.hbox(c);Button start=UiKit.button(c,"بدء دورة MQ");start.setOnClickListener(v->measurement(true));run.addView(start,UiKit.weight(1));Button cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measurement(false));run.addView(cancel,UiKit.weight(1));Button captureBtn=UiKit.button(c,"حفظ التعرض المرجعي");captureBtn.setOnClickListener(v->captureReference());run.addView(captureBtn,UiKit.weight(1));capture.addView(run,UiKit.full(c));
        LinearLayout rel=UiKit.hbox(c);for(int g=1;g<=3;g++){final int x=g;Button b=UiKit.button(c,"ريليه "+g);b.setOnClickListener(v->relay(x));rel.addView(b,UiKit.weight(1));}Button off=UiKit.button(c,"OFF");off.setOnClickListener(v->relay(0));rel.addView(off,UiKit.weight(1));capture.addView(rel,UiKit.full(c));root.addView(capture,UiKit.full(c));

        LinearLayout health=UiKit.card(c);health.addView(UiKit.title(c,"صحة الحساسات ومرجع الهواء النظيف"),UiKit.full(c));health.addView(UiKit.small(c,"استخدم هذه الوظيفة فقط في هواء نظيف معروف وبعد اكتمال دورة MQ."),UiKit.full(c));LinearLayout hb=UiKit.hbox(c);Button base=UiKit.button(c,"اعتماد Baseline نظيف");base.setOnClickListener(v->captureBaseline());hb.addView(base,UiKit.weight(1));Button check=UiKit.button(c,"فحص Drift في هواء نظيف");check.setOnClickListener(v->checkDrift());hb.addView(check,UiKit.weight(1));health.addView(hb,UiKit.full(c));healthSummary=UiKit.small(c,"لا يوجد تقييم بعد.");healthSummary.setTextIsSelectable(true);health.addView(healthSummary,UiKit.full(c));root.addView(health,UiKit.full(c));

        summary=UiKit.text(c,"التجارب المعتمدة: 0");summary.setBackgroundColor(0xFF151A20);root.addView(summary,UiKit.full(c));details=UiKit.text(c,"اختر تجربة محفوظة لمراجعتها وتصديرها أو اعتمادها للبنك المرجعي.");details.setTextIsSelectable(true);details.setBackgroundColor(0xFF11171D);root.addView(details,UiKit.full(c));LinearLayout export=UiKit.hbox(c);pdf=UiKit.button(c,"PDF");pdf.setOnClickListener(v->export("pdf"));export.addView(pdf,UiKit.weight(1));xlsx=UiKit.button(c,"Excel");xlsx.setOnClickListener(v->export("xlsx"));export.addView(xlsx,UiKit.weight(1));root.addView(export,UiKit.full(c));approve=UiKit.button(c,"اعتماد وتجهيز سجل البنك المرجعي");approve.setOnClickListener(v->approveSelected());root.addView(approve,UiKit.full(c));root.addView(UiKit.small(c,"السجل المرجعي JSON يتضمن SHA-256 ولا يحتوي أي GitHub token."),UiKit.full(c));list=UiKit.vbox(c);root.addView(list,UiKit.full(c));setActionState(false);
    }

    private EditText field(Context c,LinearLayout p,String label,String value){p.addView(UiKit.small(c,label),UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    private void measurement(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.showToast(start?"بدأت دورة التجربة":"أُلغيت الدورة");}catch(Exception e){host.showToast("تعذر إرسال الأمر");}});}
    private void relay(int group){host.io().execute(()->{try{host.robotApi().relay(group);host.showToast(group==0?"تم إطفاء الريليهات":"تم تشغيل مجموعة "+group);}catch(Exception e){host.showToast("تعذر التحكم بالريليه");}});}
    private void captureBaseline(){int n=host.captureCleanAirBaseline();if(n<=0){host.showToast("يلزم قياس READY في هواء نظيف قبل اعتماد baseline");return;}host.showToast("تم اعتماد baseline نظيف لـ "+n+" حساسات");refreshHealth();host.refreshAllTabs();}
    private void checkDrift(){int n=host.evaluateCleanAirDrift();if(n<=0){host.showToast("لا يوجد baseline صالح أو قياس READY نظيف للمقارنة");return;}host.showToast("تم فحص drift لـ "+n+" حساسات");refreshHealth();host.refreshAllTabs();}

    private void captureReference(){
        final ChemicalRepository.Chemical chemical=new ChemicalRepository(host.database()).resolve(chemicalQuery.getText().toString());double ppm;try{ppm=Double.parseDouble(knownPpm.getText().toString().trim());}catch(Exception e){host.showToast("أدخل تركيزًا مرجعيًا صالحًا");return;}if(chemical==null){host.showToast("لم أجد مادة واحدة مطابقة؛ استخدم UID أو CAS أو الاسم الدقيق");return;}if(!Double.isFinite(ppm)||ppm<=0){host.showToast("التركيز المرجعي غير صالح");return;}
        JSONObject d=host.latestDiagnostics();JSONObject m=d==null?null:d.optJSONObject("measurement");if(m==null||!"ready".equalsIgnoreCase(m.optString("state"))){host.showToast("أكمل دورة MQ قبل حفظ التعرض");return;}JSONObject e=d.optJSONObject("env");Double t=e!=null&&Double.isFinite(e.optDouble("temperature",Double.NaN))?e.optDouble("temperature"):null;Double rh=e!=null&&Double.isFinite(e.optDouble("humidity",Double.NaN))?e.optDouble("humidity"):null;String ts=isoNow(),session="DEV-"+System.currentTimeMillis(),sensor=m.optJSONObject("mq")==null?"{}":m.optJSONObject("mq").toString(),analytics=AnalyticsSnapshot.toJson(d);String protocol="Reference exposure: "+chemical.label()+" @ "+ppm+" ppm";
        host.io().execute(()->{try{host.database().saveCaptureForChemical(chemical.uid,chemical.legacyId,ppm,ts,session,t,rh,"ready",sensor,null);long id=host.database().save(ts,d.toString(),protocol,analytics);host.showToast("حُفظ التعرض المرجعي للتجربة #"+id);host.refreshAllTabs();}catch(Exception ex){host.showToast("تعذر حفظ التجربة المرجعية");}});
    }

    public void refresh(){refreshHealth();list.removeAllViews();List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);summary.setText("قاعدة المركبات: "+host.database().chemicalCount()+" • التجارب/القياسات: "+host.database().count()+" • التعرضات المرجعية: "+host.database().captureCount()+" • المعتمدة: "+host.database().approvedCount());if(rows.isEmpty()){selectedId=-1;setActionState(false);list.addView(UiKit.small(list.getContext(),"لا توجد سجلات بعد."),UiKit.full(list.getContext()));return;}if(selectedId<0)selectedId=rows.get(0).id;boolean still=false;for(ExperimentQueries.Record r:rows){if(r.id==selectedId)still=true;Button b=UiKit.button(list.getContext(),(r.approved?"✓ مرجعي  ":"")+"#"+r.id+"   "+safe(r.ts)+"   "+safe(r.protocol));b.setOnClickListener(v->select(r.id));list.addView(b,UiKit.full(list.getContext()));}if(!still)selectedId=rows.get(0).id;showSelected();}
    private void refreshHealth(){StringBuilder s=new StringBuilder();String ts=host.cleanAirBaselineTimestamp();s.append("Baseline: ").append(ts==null||ts.isEmpty()?"غير معتمد":ts);for(String key:SensorHealthEngine.INSTALLED){SensorHealthEngine.Snapshot h=host.sensorHealth(key);s.append("\n").append(key.toUpperCase(Locale.US)).append(" • ").append(SensorHealthEngine.compact(h));if(h!=null&&Double.isFinite(h.noiseCv))s.append(String.format(Locale.US," • noise CV %.1f%%",100*h.noiseCv));}healthSummary.setText(s.toString());}
    private void select(long id){selectedId=id;showSelected();}
    private void showSelected(){ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){setActionState(false);return;}setActionState(true);JSONObject d=json(r.payload),m=d.optJSONObject("measurement"),e=d.optJSONObject("env"),a=json(r.analytics);int results=a.optJSONArray("results")==null?0:a.optJSONArray("results").length();StringBuilder s=new StringBuilder("السجل #").append(r.id).append("\nUTC: ").append(safe(r.ts)).append("\nالبروتوكول: ").append(safe(r.protocol)).append("\nالاعتماد المرجعي: ").append(r.approved?"معتمد":"غير معتمد");if(r.approved)s.append("\nاعتمد: ").append(safe(r.approvedTs)).append("\nملف: ").append(safe(r.referenceName));if(m!=null)s.append("\nدورة MQ: ").append(m.optString("state",""));if(e!=null&&e.optBoolean("ok",false))s.append("\nالبيئة: ").append(e.optString("type","ENV")).append(" • ").append(fmt(e.optDouble("temperature",Double.NaN))).append(" °C • ").append(fmt(e.optDouble("humidity",Double.NaN))).append(" %RH • ").append(fmt(e.optDouble("pressure",Double.NaN))).append(" hPa");s.append("\nنتائج التحليل المسجلة: ").append(results);details.setText(s.toString());approve.setText(r.approved?"إعادة تصدير السجل المرجعي":"اعتماد وتجهيز سجل البنك المرجعي");}
    private void setActionState(boolean on){pdf.setEnabled(on);xlsx.setEnabled(on);approve.setEnabled(on);}private void export(String format){if(selectedId<0)return;host.requestExport(format,selectedId);}private void approveSelected(){if(selectedId<0)return;ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null)return;if(!r.approved){String ts=isoNow(),name=ReferenceBankExporter.fileName(r.id,r.ts);if(!host.database().approveExperiment(r.id,ts,name)){host.showToast("تعذر اعتماد السجل");return;}host.showToast("تم الاعتماد المحلي؛ اختر مكان حفظ سجل البنك المرجعي");}host.refreshAllTabs();host.requestReferenceBankExport(selectedId);}
    private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}private static String safe(String s){return s==null?"":s;}private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
