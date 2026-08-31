package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.exposure.ExposureRepository;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurements.PhysicalCatalog;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Single operational workflow for environment, MQ acquisition, chemical interpretation and field measurements. */
public final class MeasurementAnalysisTab {
    private final HazRobHost host;private final ChemicalRepository chemicals;private final ExposureRepository exposures;private final ScrollView scroll;private final TextView env,cycle,chem,coverage;private final TextView[] mqRows=new TextView[RobotTelemetry.MQ_KEYS.length];private final TextView[] physicalValues=new TextView[PhysicalCatalog.COUNT],physicalStates=new TextView[PhysicalCatalog.COUNT];

    private static final class RankedCandidate{
        final ValidatedGasEngine.Result result;final ChemicalRepository.Chemical chemical;final ExposureRepository.Limit idlh;final double idlhRatio;
        RankedCandidate(ValidatedGasEngine.Result result,ChemicalRepository.Chemical chemical,ExposureRepository.Limit idlh,double idlhRatio){this.result=result;this.chemical=chemical;this.idlh=idlh;this.idlhRatio=idlhRatio;}
    }

    public MeasurementAnalysisTab(Context c,HazRobHost host){
        this.host=host;chemicals=new ChemicalRepository(host.database());exposures=new ExposureRepository(host.database());scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"القياس والتحليل"),UiKit.full(c));
        root.addView(UiKit.small(c,"مسار موحد: البيئة ← دورة MQ ← جودة الاستحواذ ← الأدلة الكيميائية ← حدود التعرض الموثقة ← حفظ القياس والتقرير."),UiKit.full(c));

        LinearLayout envCard=UiKit.card(c);envCard.addView(UiKit.title(c,"البيئة"),UiKit.full(c));env=UiKit.text(c,"بانتظار ESP32...");env.setBackgroundColor(0xFF151A20);envCard.addView(env,UiKit.full(c));root.addView(envCard,UiKit.full(c));

        LinearLayout runCard=UiKit.card(c);runCard.addView(UiKit.title(c,"دورة MQ"),UiKit.full(c));cycle=UiKit.text(c,"الحالة: خامل");cycle.setBackgroundColor(0xFF151A20);runCard.addView(cycle,UiKit.full(c));LinearLayout buttons=UiKit.hbox(c);Button start=UiKit.button(c,"بدء القياس");start.setOnClickListener(v->measurement(true));buttons.addView(start,UiKit.weight(1));Button cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measurement(false));buttons.addView(cancel,UiKit.weight(1));Button save=UiKit.button(c,"حفظ النتيجة");save.setOnClickListener(v->save());buttons.addView(save,UiKit.weight(1));runCard.addView(buttons,UiKit.full(c));root.addView(runCard,UiKit.full(c));

        LinearLayout mqCard=UiKit.card(c);mqCard.addView(UiKit.title(c,"حالة مصفوفة MQ"),UiKit.full(c));for(int i=0;i<mqRows.length;i++){mqRows[i]=UiKit.small(c,RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+" —");mqRows[i].setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);mqRows[i].setTextIsSelectable(true);mqCard.addView(mqRows[i],UiKit.full(c));}mqCard.addView(UiKit.small(c,"Raw وVadc وVao وRs وRs/R0 هي بيانات استحواذ. صحة القناة ترصد فقد البيانات/التشبع/الثبات/التذبذب. انحراف baseline لا يُقيّم إلا مقابل مرجع هواء نظيف اعتمده المطور. عدم وجود معايرة مرجعية موثقة يمنع تحويل القراءة إلى PPM كمي."),UiKit.full(c));root.addView(mqCard,UiKit.full(c));

        LinearLayout chemCard=UiKit.card(c);chemCard.addView(UiKit.title(c,"التحليل الكيميائي وحدود التعرض"),UiKit.full(c));coverage=UiKit.small(c,"");chemCard.addView(coverage,UiKit.full(c));chem=UiKit.text(c,"لا توجد بيانات تحليل بعد.");chem.setTextIsSelectable(true);chem.setBackgroundColor(0xFF151A20);chemCard.addView(chem,UiKit.full(c));root.addView(chemCard,UiKit.full(c));

        LinearLayout phyCard=UiKit.card(c);phyCard.addView(UiKit.title(c,"القياسات الفيزيائية والبيئية"),UiKit.full(c));TableLayout table=new TableLayout(c);table.setStretchAllColumns(true);table.setShrinkAllColumns(true);TableRow head=new TableRow(c);head.addView(tableCell(c,"القياس",true));head.addView(tableCell(c,"القيمة",true));head.addView(tableCell(c,"الوحدة",true));head.addView(tableCell(c,"الحالة",true));table.addView(head,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT));for(int i=0;i<PhysicalCatalog.COUNT;i++){TableRow row=new TableRow(c);row.setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);row.addView(tableCell(c,PhysicalCatalog.NAMES_AR[i],false));physicalValues[i]=tableCell(c,"—",false);row.addView(physicalValues[i]);row.addView(tableCell(c,PhysicalCatalog.UNITS[i],false));physicalStates[i]=tableCell(c,"غير مركب",false);row.addView(physicalStates[i]);table.addView(row,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT));}phyCard.addView(table,UiKit.full(c));phyCard.addView(UiKit.small(c,"القيم غير المركبة تبقى ظاهرة في الجدول لتوضيح تغطية المنصة الحالية وما يمكن إضافته لاحقًا."),UiKit.full(c));root.addView(phyCard,UiKit.full(c));
        root.addView(UiKit.small(c,"بعد حفظ القياس يمكن إصدار PDF/Excel من تبويب السجل والتقارير."),UiKit.full(c));
    }

    private static TextView tableCell(Context c,String text,boolean header){TextView v=new TextView(c);v.setText(text);v.setTextColor(header?Color.WHITE:0xFFCFD8DC);v.setTextSize(header?13:12);v.setPadding(UiKit.dp(c,5),UiKit.dp(c,7),UiKit.dp(c,5),UiKit.dp(c,7));if(header){v.setBackgroundColor(0xFF263238);v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}return v;}
    public View view(){return scroll;}

    private void measurement(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.showToast(start?"بدأت دورة MQ":"أُلغيت دورة MQ");}catch(Exception e){host.showToast("تعذر إرسال أمر القياس");}});}

    private void save(){JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات حية");return;}JSONObject m=d.optJSONObject("measurement");if(m==null||!"ready".equalsIgnoreCase(m.optString("state"))){host.showToast("أكمل دورة MQ قبل حفظ القياس");return;}String ts=isoNow(),payload=d.toString(),analytics=AnalyticsSnapshot.toJson(d);host.io().execute(()->{try{long id=host.database().save(ts,payload,"HazRob 0.6 field measurement",analytics);host.showToast("تم حفظ القياس #"+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ القياس");}});}

    public void refresh(){
        coverage.setText("قاعدة المعرفة الكيميائية: "+chemicals.count()+" سجلًا • حدود التعرض: "+exposures.count()+" سجلًا • القائمة أدناه تعرض المرشحات ذات الدليل الحسي فقط، لا جميع مواد القاعدة.");
        JSONObject d=host.latestDiagnostics();if(d==null){env.setText("الروبوت غير متصل");cycle.setText("الحالة: غير متصل");chem.setText("لا توجد بيانات تحليل.");setPhysicalUnavailable("غير متصل");for(int i=0;i<mqRows.length;i++){String key=RobotTelemetry.MQ_KEYS[i];SensorHealthEngine.Snapshot h=host.sensorHealth(key);mqRows[i].setText(key.toUpperCase(Locale.US)+" — • صحة "+SensorHealthEngine.compact(h));}return;}
        refreshEnv(d);refreshCycleAndMq(d);refreshChemistry(d);refreshPhysical(d);
    }

    private void refreshEnv(JSONObject d){JSONObject e=d.optJSONObject("env");if(e==null||!e.optBoolean("ok",false)){env.setText("الحساس البيئي غير متاح. BME/BMP يعمل مباشرة على I²C ولا يعتمد على دورة MQ.");env.setTextColor(0xFFFFB74D);return;}String type=e.optString("type","ENV");String h=Double.isFinite(e.optDouble("humidity",Double.NaN))?String.format(Locale.US,"%.1f %%RH",e.optDouble("humidity")):"الرطوبة غير متاحة";env.setText(String.format(Locale.US,"%s • %s • Chip %s\nالحرارة %.2f °C • %s • الضغط %.1f hPa",type,e.optString("address","—"),e.optString("chip_id","—"),e.optDouble("temperature",Double.NaN),h,e.optDouble("pressure",Double.NaN)));env.setTextColor(0xFF81C784);}

    private void refreshCycleAndMq(JSONObject d){
        JSONObject m=d.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle");int group=m==null?0:m.optInt("group",0);cycle.setText("الحالة: "+stateAr(state)+(group>0?" • مجموعة "+group:""));cycle.setTextColor("ready".equals(state)?0xFF81C784:Color.WHITE);JSONObject source=d.optJSONObject("mq");if(m!=null&&"ready".equals(state)&&m.optJSONObject("mq")!=null)source=m.optJSONObject("mq");
        for(int i=0;i<mqRows.length;i++){
            String key=RobotTelemetry.MQ_KEYS[i];JSONObject r=source==null?null:source.optJSONObject(key);SensorHealthEngine.Snapshot h=host.sensorHealth(key);
            if(r==null){mqRows[i].setText(key.toUpperCase(Locale.US)+" • غير مركب/غير مرسل • صحة "+SensorHealthEngine.compact(h));mqRows[i].setTextColor(healthColor(h,false));continue;}
            boolean ok=r.optBoolean("valid",r.optBoolean("ok",false));StringBuilder s=new StringBuilder(key.toUpperCase(Locale.US)).append(ok?" • صالح":" • غير صالح");if(!r.isNull("raw"))s.append(" • raw ").append((int)Math.round(r.optDouble("raw",0)));add(s,r,"adc","Vadc");add(s,r,"ao","Vao");add(s,r,"rs","Rs");add(s,r,"ratio","Rs/R0");s.append(" • صحة ").append(SensorHealthEngine.compact(h));mqRows[i].setText(s.toString());mqRows[i].setTextColor(healthColor(h,ok));
        }
    }

    private static int healthColor(SensorHealthEngine.Snapshot h,boolean readingOk){if(h==null)return readingOk?Color.WHITE:0xFF78909C;switch(h.state){case MISSING:case INVALID:case SATURATED:return 0xFFEF5350;case STUCK:case UNSTABLE:case DRIFT_WARNING:return 0xFFFFB74D;case HEALTHY:return readingOk?0xFFB0BEC5:0xFF78909C;default:return readingOk?Color.WHITE:0xFF78909C;}}

    private void refreshChemistry(JSONObject d){
        Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d);List<RankedCandidate> rows=new ArrayList<>();
        for(ValidatedGasEngine.Result r:map.values()){
            if(r==null||r.totalContributors<=0)continue;ChemicalRepository.Chemical c=chemicals.byLegacyId(r.chemical);if(c==null)continue;ExposureRepository.Limit idlh=currentNioshIdlh(c.uid);double ratio=Double.NaN;
            if(idlh!=null&&r.quantitative&&"ppm".equalsIgnoreCase(idlh.unit)&&idlh.numeric()&&idlh.value>0&&Double.isFinite(r.ppm))ratio=r.ppm/idlh.value;
            rows.add(new RankedCandidate(r,c,idlh,ratio));
        }
        rows.sort((a,b)->{int c=Integer.compare(hazardBand(b.idlhRatio),hazardBand(a.idlhRatio));if(c!=0)return c;c=Double.compare(safeRatio(b.idlhRatio),safeRatio(a.idlhRatio));if(c!=0)return c;return CandidateRanker.COMPARATOR.compare(a.result,b.result);});
        if(rows.isEmpty()){chem.setText("لا توجد أدلة كافية من القنوات الحالية.");return;}
        StringBuilder s=new StringBuilder("المرشحات الحسية: ").append(rows.size()).append(" • مرتبة تنازليًا حسب الخطر الفوري القابل للحساب ثم قوة الدليل والتحقق بين الحساسات.\n\n");int shown=0;
        for(RankedCandidate x:rows){
            ValidatedGasEngine.Result r=x.result;ChemicalRepository.Chemical c=x.chemical;shown++;
            s.append(shown).append(". ").append(c.arabicName!=null&&!c.arabicName.isEmpty()?c.arabicName:c.name);if(c.formula!=null&&!c.formula.isEmpty())s.append(" • ").append(c.formula);if(c.cas!=null&&!c.cas.isEmpty())s.append(" • CAS ").append(c.cas);
            s.append("\n   الدليل: ").append(CandidateRanker.evidenceLabel(r)).append(" • الثقة ").append(UiKit.stars(r.confidence)).append(" • اتفاق ").append(String.format(Locale.US,"%.0f%%",r.agreement));
            if(r.quantitative)s.append(" • تقدير ").append(String.format(Locale.US,"%.2f ppm",r.ppm));else s.append(" • تعريف/بصمة فقط — PPM غير معتمد");
            if(x.idlh!=null){s.append("\n   NIOSH IDLH: ").append(limitText(x.idlh));if(Double.isFinite(x.idlhRatio))s.append(String.format(Locale.US," • التقدير %.1f%% من IDLH",100.0*x.idlhRatio));}
            s.append("\n\n");
        }
        chem.setText(s.toString().trim());
    }

    private static int hazardBand(double ratio){if(!Double.isFinite(ratio)||ratio<=0)return 0;if(ratio>=1.0)return 4;if(ratio>=0.5)return 3;if(ratio>=0.1)return 2;return 1;}
    private static double safeRatio(double ratio){return Double.isFinite(ratio)?ratio:-1;}

    private ExposureRepository.Limit currentNioshIdlh(long uid){
        ExposureRepository.Limit fallback=null;for(ExposureRepository.Limit l:exposures.forChemical(uid,"NIOSH")){if(!"IDLH".equalsIgnoreCase(l.type))continue;if(fallback==null)fallback=l;if(l.notation!=null&&l.notation.startsWith("CURRENT POLICY"))return l;}return fallback;
    }

    private static String limitText(ExposureRepository.Limit l){StringBuilder s=new StringBuilder();if(l.numeric())s.append(String.format(Locale.US,"%.4g",l.value));else s.append("—");if(l.unit!=null&&!l.unit.isEmpty())s.append(' ').append(l.unit);if(l.revision!=null&&!l.revision.isEmpty())s.append(" • ").append(l.revision);return s.toString();}

    private void refreshPhysical(JSONObject d){setPhysicalUnavailable("غير مركب");JSONObject e=d.optJSONObject("env");if(e==null||!e.optBoolean("ok",false)){physicalStates[0].setText("الحساس البيئي غير متاح");physicalStates[1].setText("الحساس البيئي غير متاح");physicalStates[2].setText("الحساس البيئي غير متاح");physicalStates[14].setText("يتطلب ضغطًا صالحًا");return;}String type=e.optString("type","ENV");setPhysical(0,e.optDouble("temperature",Double.NaN),type);setPhysical(1,e.optDouble("humidity",Double.NaN),Double.isFinite(e.optDouble("humidity",Double.NaN))?type:"غير متاحة في "+type);double p=e.optDouble("pressure",Double.NaN);setPhysical(2,p,type);if(Double.isFinite(p)&&p>0)setPhysical(14,44330.0*(1.0-Math.pow(p/1013.25,0.1903)),"مشتق من الضغط @ 1013.25 hPa");else physicalStates[14].setText("ضغط غير صالح");}
    private void setPhysicalUnavailable(String state){for(int i=0;i<PhysicalCatalog.COUNT;i++){physicalValues[i].setText("—");physicalStates[i].setText(state);physicalStates[i].setTextColor(0xFF90A4AE);}}
    private void setPhysical(int index,double value,String state){if(Double.isFinite(value)){physicalValues[index].setText(String.format(Locale.US,index==14?"%.1f":"%.2f",value));physicalStates[index].setText(state);physicalStates[index].setTextColor(0xFF81C784);}else{physicalValues[index].setText("—");physicalStates[index].setText(state);physicalStates[index].setTextColor(0xFFFFB74D);}}

    private static void add(StringBuilder s,JSONObject r,String key,String label){double v=r.optDouble(key,Double.NaN);if(Double.isFinite(v))s.append(String.format(Locale.US," • %s %.3f",label,v));}
    private static String stateAr(String s){if("idle".equals(s))return"خامل";if("warmup".equals(s))return"تسخين";if("measuring".equals(s))return"قياس";if("ready".equals(s))return"جاهز";if("error".equals(s))return"خطأ";return s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
