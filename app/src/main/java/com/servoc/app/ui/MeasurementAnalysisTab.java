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
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurements.PhysicalCatalog;
import com.servoc.app.navigation.MqRelayPlanner;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Environment, two-group MQ acquisition, auxiliary sensors and chemical interpretation. */
public final class MeasurementAnalysisTab {
    private static final int TREND_POINTS=60;
    private final HazRobHost host;private final ChemicalRepository chemicals;private final ExposureRepository exposures;private final ScrollView scroll;
    private final TextView env,hardware,cycle,chem,coverage;
    private final TextView[] mqRows=new TextView[RobotTelemetry.MQ_KEYS.length];
    private final TextView[] physicalValues=new TextView[PhysicalCatalog.COUNT],physicalStates=new TextView[PhysicalCatalog.COUNT];
    private final HazChartView mqBars,mqTrend,candidateBars;private final double[][] trendRaw=new double[RobotTelemetry.MQ_KEYS.length][TREND_POINTS];private int trendSize,trendHead;private long trendLastAt;

    private static final class RankedCandidate{
        final ValidatedGasEngine.Result result;final ChemicalRepository.Chemical chemical;final ExposureRepository.Limit idlh;final double idlhRatio;
        RankedCandidate(ValidatedGasEngine.Result result,ChemicalRepository.Chemical chemical,ExposureRepository.Limit idlh,double idlhRatio){this.result=result;this.chemical=chemical;this.idlh=idlh;this.idlhRatio=idlhRatio;}
    }

    public MeasurementAnalysisTab(Context c,HazRobHost host){
        this.host=host;chemicals=new ChemicalRepository(host.database());exposures=new ExposureRepository(host.database());for(double[] a:trendRaw)Arrays.fill(a,Double.NaN);
        scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"القياس والتحليل • HazRob 0.8"),UiKit.full(c));
        root.addView(UiKit.small(c,"BME/BMP + ADS1115×2 + MAX4466 + IR + مجموعتا MQ يدويتان. R1: MQ2/MQ4/MQ5/MQ8، R2: MQ3/MQ7/MQ9/MQ135."),UiKit.full(c));

        LinearLayout envCard=UiKit.card(c);envCard.addView(UiKit.title(c,"البيئة"),UiKit.full(c));env=UiKit.text(c,"بانتظار ESP32...");env.setBackgroundColor(0xFF151A20);envCard.addView(env,UiKit.full(c));root.addView(envCard,UiKit.full(c));

        LinearLayout hwCard=UiKit.card(c);hwCard.addView(UiKit.title(c,"الحساسات والحالة الكهربائية"),UiKit.full(c));hardware=UiKit.text(c,"بانتظار البيانات...");hardware.setBackgroundColor(0xFF151A20);hwCard.addView(hardware,UiKit.full(c));
        hwCard.addView(UiKit.small(c,"تشغيل MQ يتم يدويًا من الإعدادات. MAX4466 يعرض جهد RMS/P-P فقط قبل معايرة dB SPL. IR تحذير فقط. أي LDR لا يُعد مركبًا إلا إذا أرسله Firmware فعليًا."),UiKit.full(c));root.addView(hwCard,UiKit.full(c));

        LinearLayout runCard=UiKit.card(c);runCard.addView(UiKit.title(c,"الالتقاط والحفظ"),UiKit.full(c));cycle=UiKit.text(c,"الحالة: خامل");cycle.setBackgroundColor(0xFF151A20);runCard.addView(cycle,UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);Button start=UiKit.button(c,"بدء averaging");start.setOnClickListener(v->measurement(true));buttons.addView(start,UiKit.weight(1));Button cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measurement(false));buttons.addView(cancel,UiKit.weight(1));Button save=UiKit.button(c,"حفظ اللقطة");save.setOnClickListener(v->save());buttons.addView(save,UiKit.weight(1));runCard.addView(buttons,UiKit.full(c));
        runCard.addView(UiKit.small(c,"إذا كانت الدورة READY يُحفظ متوسطها؛ وإلا تُحفظ القراءة الحية كـmanual_live. طاقة R1/R2 لا يغيّرها هذا التبويب."),UiKit.full(c));root.addView(runCard,UiKit.full(c));

        LinearLayout mqCard=UiKit.card(c);mqCard.addView(UiKit.title(c,"مصفوفة MQ"),UiKit.full(c));
        for(int i=0;i<mqRows.length;i++){mqRows[i]=UiKit.small(c,RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+" —");mqRows[i].setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);mqRows[i].setTextIsSelectable(true);mqCard.addView(mqRows[i],UiKit.full(c));}
        mqCard.addView(UiKit.small(c,"Raw/Vadc/Vao/Rs/Rs-R0 هي بيانات الاستحواذ. علامة R0 معاير تعني إدخال R0 يدويًا للجهاز؛ ولا تكفي وحدها لاعتماد PPM دون نموذج ومعايرة مرجعية للمركب."),UiKit.full(c));
        mqBars=new HazChartView(c);mqBars.clear("بصمة MQ الحالية","بانتظار قراءات Rs/R0...");mqCard.addView(mqBars,UiKit.full(c));
        mqTrend=new HazChartView(c);mqTrend.clear("اتجاه الاستجابة خلال آخر دقيقة","يعرض التغير النسبي؛ ليس PPM.");mqCard.addView(mqTrend,UiKit.full(c));root.addView(mqCard,UiKit.full(c));

        LinearLayout chemCard=UiKit.card(c);chemCard.addView(UiKit.title(c,"التحليل الكيميائي وحدود التعرض"),UiKit.full(c));coverage=UiKit.small(c,"");chemCard.addView(coverage,UiKit.full(c));candidateBars=new HazChartView(c);candidateBars.clear("اتفاق الأدلة لأعلى المرشحات","بانتظار أدلة كيميائية كافية...");chemCard.addView(candidateBars,UiKit.full(c));chem=UiKit.text(c,"لا توجد بيانات تحليل بعد.");chem.setTextIsSelectable(true);chem.setBackgroundColor(0xFF151A20);chemCard.addView(chem,UiKit.full(c));root.addView(chemCard,UiKit.full(c));

        LinearLayout phyCard=UiKit.card(c);phyCard.addView(UiKit.title(c,"القياسات الفيزيائية والبيئية"),UiKit.full(c));TableLayout table=new TableLayout(c);table.setStretchAllColumns(true);table.setShrinkAllColumns(true);TableRow head=new TableRow(c);head.addView(tableCell(c,"القياس",true));head.addView(tableCell(c,"القيمة",true));head.addView(tableCell(c,"الوحدة",true));head.addView(tableCell(c,"الحالة",true));table.addView(head,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT));for(int i=0;i<PhysicalCatalog.COUNT;i++){TableRow row=new TableRow(c);row.setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);row.addView(tableCell(c,PhysicalCatalog.NAMES_AR[i],false));physicalValues[i]=tableCell(c,"—",false);row.addView(physicalValues[i]);row.addView(tableCell(c,PhysicalCatalog.UNITS[i],false));physicalStates[i]=tableCell(c,"غير مركب",false);row.addView(physicalStates[i]);table.addView(row,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT));}phyCard.addView(table,UiKit.full(c));phyCard.addView(UiKit.small(c,"وجود القياس في الكتالوج لا يعني أن حساسه مركب. التطبيق يملأ فقط المصادر الفعلية أو القيم المشتقة المعلنة."),UiKit.full(c));root.addView(phyCard,UiKit.full(c));
    }

    private static TextView tableCell(Context c,String text,boolean header){TextView v=new TextView(c);v.setText(text);v.setTextColor(header?Color.WHITE:0xFFCFD8DC);v.setTextSize(header?13:12);v.setPadding(UiKit.dp(c,5),UiKit.dp(c,7),UiKit.dp(c,5),UiKit.dp(c,7));if(header){v.setBackgroundColor(0xFF263238);v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}return v;}
    public View view(){return scroll;}

    private void measurement(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.showToast(start?"بدأ averaging؛ طاقة MQ تبقى تحت التحكم اليدوي وفق Firmware":"أُلغيت دورة averaging");}catch(Exception e){host.showToast("تعذر إرسال أمر القياس");}});}

    private void save(){
        JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات حية");return;}
        JSONObject m=d.optJSONObject("measurement"),mq=d.optJSONObject("mq");boolean ready=m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null;
        JSONObject source=ready?m.optJSONObject("mq"):mq;if(source==null||source.length()==0){host.showToast("لا توجد قراءات MQ للحفظ");return;}
        String mode=ready?"ready_average":"manual_live",ts=isoNow(),payload=d.toString(),analytics=AnalyticsSnapshot.toJson(d);JSONObject r=d.optJSONObject("relay");int mask=r==null?0:r.optInt("mask",0)&3;String protocol="HazRob field measurement • "+mode+" • "+HazRobHardwareProfile.groupLabel(mask);
        host.io().execute(()->{try{long id=host.database().save(ts,payload,protocol,analytics);host.showToast("تم حفظ القياس #"+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ القياس");}});
    }

    public void refresh(){
        coverage.setText("قاعدة المعرفة الكيميائية: "+chemicals.count()+" سجلًا • حدود التعرض: "+exposures.count()+" سجلًا • PPM الكمي لا يُعرض كاعتماد سلامة دون معايرة مرجعية صالحة.");
        JSONObject d=host.latestDiagnostics();if(d==null){env.setText("الروبوت غير متصل");hardware.setText("لا توجد بيانات ADS/IR/MAX4466");cycle.setText("الحالة: غير متصل");chem.setText("لا توجد بيانات تحليل.");setPhysicalUnavailable("غير متصل");for(int i=0;i<mqRows.length;i++){String key=RobotTelemetry.MQ_KEYS[i];SensorHealthEngine.Snapshot h=host.sensorHealth(key);mqRows[i].setText(key.toUpperCase(Locale.US)+" — • صحة "+SensorHealthEngine.compact(h));}mqBars.clear("بصمة MQ الحالية","الروبوت غير متصل");mqTrend.clear("اتجاه الاستجابة خلال آخر دقيقة","الروبوت غير متصل");candidateBars.clear("اتفاق الأدلة لأعلى المرشحات","لا توجد بيانات تحليل");return;}
        updateMqCharts(d.optJSONObject("mq"));refreshEnv(d);refreshHardware(d);refreshCycleAndMq(d);refreshChemistry(d);refreshPhysical(d);
    }

    private void refreshEnv(JSONObject d){JSONObject e=d.optJSONObject("env");if(e==null||!e.optBoolean("ok",false)){env.setText("الحساس البيئي غير متاح. BME/BMP يعمل مباشرة على I²C ولا يعتمد على طاقة MQ.");env.setTextColor(0xFFFFB74D);return;}String type=e.optString("type","ENV");String h=Double.isFinite(e.optDouble("humidity",Double.NaN))?String.format(Locale.US,"%.1f %%RH",e.optDouble("humidity")):"الرطوبة غير متاحة";env.setText(String.format(Locale.US,"%s • %s • Chip %s\nالحرارة %.2f °C • %s • الضغط %.1f hPa",type,e.optString("address","—"),e.optString("chip_id","—"),e.optDouble("temperature",Double.NaN),h,e.optDouble("pressure",Double.NaN)));env.setTextColor(0xFF81C784);}

    private void refreshHardware(JSONObject d){
        JSONObject s=d.optJSONObject("sound"),ir=d.optJSONObject("ir"),ldr=d.optJSONObject("ldr"),adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),drv=d.optJSONObject("drive");
        double rms=s==null?Double.NaN:s.optDouble("rms_v",Double.NaN),p2p=s==null?Double.NaN:s.optDouble("p2p_v",Double.NaN);
        boolean irWarn=ir!=null&&ir.optBoolean("warning",false),ldrInstalled=ldr!=null&&ldr.optBoolean("installed",false),ldrActive=ldrInstalled&&ldr.optBoolean("active",false),a1=adc!=null&&adc.optBoolean("ads1",false),a2=adc!=null&&adc.optBoolean("ads2",false),back=drv!=null&&drv.optBoolean("retrace",false);
        int mask=relay==null?0:relay.optInt("mask",0)&3;
        String sound=(Double.isFinite(rms)||Double.isFinite(p2p))?String.format(Locale.US,"MAX4466: RMS %.4f V • P-P %.4f V • غير معاير dB SPL",rms,p2p):"MAX4466: لا توجد قراءة";
        String ldrText=ldrInstalled?("LDR: "+(ldrActive?"Active":"Inactive")+" • عتبة فقط"):"LDR: غير مركب في profile الحالي";
        hardware.setText(sound+"\nADS1 0x48: "+(a1?"متصل":"غير مكتشف")+" • R1 MQ2/MQ4/MQ5/MQ8\nADS2 0x49: "+(a2?"متصل":"غير مكتشف")+" • R2 MQ3/MQ7/MQ9/MQ135\nطاقة MQ: "+MqRelayPlanner.rowsLabel(mask)+" • mask "+mask+" • يدوي\nIR: "+(irWarn?"⚠ قرب/عائق":"واضح")+" • "+ldrText+" • العودة: "+(back?"نشطة":"غير نشطة"));
        hardware.setTextColor(irWarn||!a1||!a2?0xFFFFB74D:0xFFCFD8DC);
    }

    private void refreshCycleAndMq(JSONObject d){
        JSONObject relay=d.optJSONObject("relay"),m=d.optJSONObject("measurement");int liveMask=relay==null?0:relay.optInt("mask",0)&3;String state=m==null?"idle":m.optString("state","idle");cycle.setText("الحالة: "+stateAr(state)+" • طاقة حالية "+HazRobHardwareProfile.groupLabel(liveMask)+" • تشغيل يدوي"+(m!=null&&m.optInt("phase",0)>0?" • phase "+m.optInt("phase",0):""));cycle.setTextColor("ready".equals(state)?0xFF81C784:Color.WHITE);
        JSONObject source=d.optJSONObject("mq");if(m!=null&&"ready".equals(state)&&m.optJSONObject("mq")!=null)source=m.optJSONObject("mq");
        for(int i=0;i<mqRows.length;i++){
            String key=RobotTelemetry.MQ_KEYS[i];JSONObject r=source==null?null:source.optJSONObject(key);SensorHealthEngine.Snapshot h=host.sensorHealth(key);HazRobHardwareProfile.Channel hw=HazRobHardwareProfile.forKey(key);
            if(r==null){mqRows[i].setText(key.toUpperCase(Locale.US)+" • "+(hw==null?"—":"R"+hw.relay+" 0x"+Integer.toHexString(hw.adsAddress).toUpperCase(Locale.US)+" A"+hw.adcChannel)+" • لا قراءة • صحة "+SensorHealthEngine.compact(h));mqRows[i].setTextColor(healthColor(h,false));continue;}
            boolean ok=r.optBoolean("valid",r.optBoolean("ok",false));StringBuilder s=new StringBuilder(key.toUpperCase(Locale.US)).append(" • R").append(r.optInt("relay",r.optInt("row",hw==null?0:hw.relay))).append(" • ").append(r.optString("ads_address",hw==null?"ADS?":String.format(Locale.US,"0x%02X",hw.adsAddress))).append(" A").append(r.optInt("ch",hw==null?0:hw.adcChannel)).append(ok?" • صالح":" • غير صالح");if(!r.isNull("raw"))s.append(" • raw ").append((int)Math.round(r.optDouble("raw",0)));add(s,r,"adc","Vadc");add(s,r,"ao","Vao");add(s,r,"rs","Rs");add(s,r,"ratio","Rs/R0");if(r.optBoolean("r0_calibrated",false)){double r0=r.optDouble("r0",Double.NaN);s.append(" • R0 معاير");if(Double.isFinite(r0))s.append(String.format(Locale.US," %.4f",r0));}else s.append(" • R0 غير معاير");s.append(" • صحة ").append(SensorHealthEngine.compact(h));mqRows[i].setText(s.toString());mqRows[i].setTextColor(healthColor(h,ok));
        }
    }

    private void updateMqCharts(JSONObject live){
        List<String> names=new ArrayList<>();List<Double> values=new ArrayList<>();double[] sample=new double[RobotTelemetry.MQ_KEYS.length];Arrays.fill(sample,Double.NaN);
        if(live!=null)for(int i=0;i<RobotTelemetry.MQ_KEYS.length;i++){String key=RobotTelemetry.MQ_KEYS[i];JSONObject r=live.optJSONObject(key);if(r==null)continue;boolean ok=r.optBoolean("valid",r.optBoolean("ok",false));double ratio=r.optDouble("ratio",Double.NaN);if(ok&&Double.isFinite(ratio)&&ratio>0){sample[i]=ratio;names.add(key.toUpperCase(Locale.US));values.add(ratio);}}
        if(values.isEmpty())mqBars.clear("بصمة MQ الحالية","لا توجد قيم Rs/R0 صالحة");else{double[] v=new double[values.size()];for(int i=0;i<v.length;i++)v[i]=values.get(i);mqBars.setBars("بصمة MQ الحالية","Rs/R0 لكل قناة فعالة — استجابة وليست PPM",names.toArray(new String[0]),v,"",Double.NaN);}
        long now=System.currentTimeMillis();if(now-trendLastAt<850)return;if(trendLastAt>0&&now-trendLastAt>7000)resetTrend();trendLastAt=now;for(int i=0;i<sample.length;i++)trendRaw[i][trendHead]=sample[i];trendHead=(trendHead+1)%TREND_POINTS;if(trendSize<TREND_POINTS)trendSize++;renderTrend();
    }

    private void renderTrend(){
        if(trendSize<2){mqTrend.clear("اتجاه الاستجابة خلال آخر دقيقة","بانتظار نقطتين زمنيتين على الأقل...");return;}int oldest=(trendHead-trendSize+TREND_POINTS)%TREND_POINTS;List<String> names=new ArrayList<>();List<double[]> series=new ArrayList<>();
        for(int s=0;s<trendRaw.length;s++){double[] raw=new double[trendSize];int valid=0;for(int i=0;i<trendSize;i++){double v=trendRaw[s][(oldest+i)%TREND_POINTS];raw[i]=v;if(Double.isFinite(v)&&v>0)valid++;}if(valid<2)continue;double base=Double.NaN;for(double v:raw)if(Double.isFinite(v)&&v>0){base=v;break;}if(!Double.isFinite(base)||base<=0)continue;double[] rel=new double[raw.length];for(int i=0;i<raw.length;i++)rel[i]=Double.isFinite(raw[i])&&raw[i]>0?100.0*(raw[i]/base-1.0):Double.NaN;names.add(RobotTelemetry.MQ_KEYS[s].toUpperCase(Locale.US));series.add(rel);}
        if(series.isEmpty()){mqTrend.clear("اتجاه الاستجابة خلال آخر دقيقة","لا توجد سلاسل صالحة");return;}mqTrend.setLines("اتجاه الاستجابة خلال آخر دقيقة","Δ% من أول قراءة صالحة — اتجاه فقط وليس PPM",names.toArray(new String[0]),series.toArray(new double[0][]),"%");
    }

    private void resetTrend(){for(double[] a:trendRaw)Arrays.fill(a,Double.NaN);trendSize=trendHead=0;}
    private static int healthColor(SensorHealthEngine.Snapshot h,boolean readingOk){if(h==null)return readingOk?Color.WHITE:0xFF78909C;switch(h.state){case MISSING:case INVALID:case SATURATED:return 0xFFEF5350;case STUCK:case UNSTABLE:case DRIFT_WARNING:return 0xFFFFB74D;case HEALTHY:return readingOk?0xFFB0BEC5:0xFF78909C;default:return readingOk?Color.WHITE:0xFF78909C;}}

    private void refreshChemistry(JSONObject d){
        Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d);List<RankedCandidate> rows=new ArrayList<>();
        for(ValidatedGasEngine.Result r:map.values()){if(r==null||r.totalContributors<=0)continue;ChemicalRepository.Chemical c=chemicals.byLegacyId(r.chemical);if(c==null)continue;ExposureRepository.Limit idlh=currentNioshIdlh(c.uid);double ratio=Double.NaN;if(idlh!=null&&r.quantitative&&"ppm".equalsIgnoreCase(idlh.unit)&&idlh.numeric()&&idlh.value>0&&Double.isFinite(r.ppm))ratio=r.ppm/idlh.value;rows.add(new RankedCandidate(r,c,idlh,ratio));}
        rows.sort((a,b)->{int c=Integer.compare(hazardBand(b.idlhRatio),hazardBand(a.idlhRatio));if(c!=0)return c;c=Double.compare(safeRatio(b.idlhRatio),safeRatio(a.idlhRatio));if(c!=0)return c;return CandidateRanker.COMPARATOR.compare(a.result,b.result);});
        if(rows.isEmpty()){chem.setText("لا توجد أدلة كافية من القنوات الحالية.");candidateBars.clear("اتفاق الأدلة لأعلى المرشحات","لا توجد أدلة كافية");return;}
        int chartN=Math.min(5,rows.size());String[] chartNames=new String[chartN];double[] chartValues=new double[chartN];for(int i=0;i<chartN;i++){RankedCandidate x=rows.get(i);chartNames[i]=x.chemical.arabicName!=null&&!x.chemical.arabicName.isEmpty()?x.chemical.arabicName:x.chemical.name;chartValues[i]=Math.max(0,Math.min(100,x.result.agreement));}candidateBars.setBars("اتفاق الأدلة لأعلى المرشحات","نسبة توافق الأدلة — لا تمثل التركيز",chartNames,chartValues,"%",100);
        StringBuilder s=new StringBuilder("المرشحات الحسية: ").append(rows.size()).append(" • نتائج الدليل والبصمة مفصولة عن القياس الكمي.\n\n");int shown=0;
        for(RankedCandidate x:rows){ValidatedGasEngine.Result r=x.result;ChemicalRepository.Chemical c=x.chemical;shown++;s.append(shown).append(". ").append(c.arabicName!=null&&!c.arabicName.isEmpty()?c.arabicName:c.name);if(c.formula!=null&&!c.formula.isEmpty())s.append(" • ").append(c.formula);if(c.cas!=null&&!c.cas.isEmpty())s.append(" • CAS ").append(c.cas);s.append("\n   الدليل: ").append(CandidateRanker.evidenceLabel(r)).append(" • الثقة ").append(UiKit.stars(r.confidence)).append(" • اتفاق ").append(String.format(Locale.US,"%.0f%%",r.agreement));if(r.quantitative)s.append(" • تقدير ").append(String.format(Locale.US,"%.2f ppm",r.ppm));else s.append(" • تعريف/بصمة فقط — PPM غير معتمد");if(x.idlh!=null){s.append("\n   NIOSH IDLH: ").append(limitText(x.idlh));if(Double.isFinite(x.idlhRatio))s.append(String.format(Locale.US," • التقدير %.1f%% من IDLH",100.0*x.idlhRatio));}s.append("\n\n");}
        chem.setText(s.toString().trim());
    }

    private static int hazardBand(double ratio){if(!Double.isFinite(ratio)||ratio<=0)return 0;if(ratio>=1.0)return 4;if(ratio>=0.5)return 3;if(ratio>=0.1)return 2;return 1;}
    private static double safeRatio(double ratio){return Double.isFinite(ratio)?ratio:-1;}
    private ExposureRepository.Limit currentNioshIdlh(long uid){ExposureRepository.Limit fallback=null;for(ExposureRepository.Limit l:exposures.forChemical(uid,"NIOSH")){if(!"IDLH".equalsIgnoreCase(l.type))continue;if(fallback==null)fallback=l;if(l.notation!=null&&l.notation.startsWith("CURRENT POLICY"))return l;}return fallback;}
    private static String limitText(ExposureRepository.Limit l){StringBuilder s=new StringBuilder();if(l.numeric())s.append(String.format(Locale.US,"%.4g",l.value));else s.append("—");if(l.unit!=null&&!l.unit.isEmpty())s.append(' ').append(l.unit);if(l.revision!=null&&!l.revision.isEmpty())s.append(" • ").append(l.revision);return s.toString();}

    private void refreshPhysical(JSONObject d){setPhysicalUnavailable("غير مركب");JSONObject e=d.optJSONObject("env");if(e==null||!e.optBoolean("ok",false)){physicalStates[0].setText("الحساس البيئي غير متاح");physicalStates[1].setText("الحساس البيئي غير متاح");physicalStates[2].setText("الحساس البيئي غير متاح");physicalStates[14].setText("يتطلب ضغطًا صالحًا");return;}String type=e.optString("type","ENV");setPhysical(0,e.optDouble("temperature",Double.NaN),type);setPhysical(1,e.optDouble("humidity",Double.NaN),Double.isFinite(e.optDouble("humidity",Double.NaN))?type:"غير متاحة في "+type);double p=e.optDouble("pressure",Double.NaN);setPhysical(2,p,type);if(Double.isFinite(p)&&p>0)setPhysical(14,44330.0*(1.0-Math.pow(p/1013.25,0.1903)),"مشتق من الضغط @ 1013.25 hPa");else physicalStates[14].setText("ضغط غير صالح");}
    private void setPhysicalUnavailable(String state){for(int i=0;i<PhysicalCatalog.COUNT;i++){physicalValues[i].setText("—");physicalStates[i].setText(state);physicalStates[i].setTextColor(0xFF90A4AE);}}
    private void setPhysical(int index,double value,String state){if(Double.isFinite(value)){physicalValues[index].setText(String.format(Locale.US,index==14?"%.1f":"%.2f",value));physicalStates[index].setText(state);physicalStates[index].setTextColor(0xFF81C784);}else{physicalValues[index].setText("—");physicalStates[index].setText(state);physicalStates[index].setTextColor(0xFFFFB74D);}}
    private static void add(StringBuilder s,JSONObject r,String key,String label){double v=r.optDouble(key,Double.NaN);if(Double.isFinite(v))s.append(String.format(Locale.US," • %s %.3f",label,v));}
    private static String stateAr(String s){if("idle".equals(s))return"خامل";if("warmup".equals(s))return"تسخين";if("measuring".equals(s))return"قياس";if("ready".equals(s))return"جاهز";if("error".equals(s))return"خطأ";return s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
