package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.chemcore.ChemCoreLabQuality;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.InstantMqWarningEngine;
import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.measurement.LiveSensorDiagnostic;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurements.PhysicalCatalog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** HazRob 0.10.2 / HR069 operator measurement + ChemCore dashboard. */
public final class MeasurementsTab {
    private static final int BG=0xFF0E1116,CARD=0xFF171D24,CARD_ALT=0xFF11171D;
    private static final int TEXT=0xFFECEFF1,MUTED=0xFFB0BEC5,GOOD=0xFF81C784,WARN=0xFFFFB74D,BAD=0xFFFF7043,INFO=0xFF90CAF9;

    private final Context c;
    private final HazRobHost host;
    private final LinearLayout root;
    private final FrameLayout page;
    private final Button[] tabs=new Button[4];
    private int selected=1;
    private final PhysicalPane physical;
    private final LivePane live;
    private final AnalysisPane analysis;
    private final AnalysisResultsPane analysisResults;
    private final View[] views;

    public MeasurementsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        root=UiKit.vbox(c);root.setBackgroundColor(BG);root.setPadding(UiKit.dp(c,5),UiKit.dp(c,4),UiKit.dp(c,5),UiKit.dp(c,4));
        LinearLayout bar=UiKit.hbox(c);
        String[] names={"الفيزيائية","القياس الحي","التحليل","النتائج"};
        for(int i=0;i<4;i++){
            final int x=i;tabs[i]=UiKit.button(c,names[i]);tabs[i].setOnClickListener(v->show(x));bar.addView(tabs[i],UiKit.weight(1));
        }
        root.addView(bar,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        physical=new PhysicalPane();live=new LivePane();analysis=new AnalysisPane();analysisResults=new AnalysisResultsPane();
        views=new View[]{physical.view,live.view,analysis.view,analysisResults.view};show(1);
    }

    public View view(){return root;}
    public void refresh(){physical.refresh();live.refresh();analysis.refresh();analysisResults.refresh();}
    private void show(int i){
        selected=Math.max(0,Math.min(3,i));page.removeAllViews();page.addView(views[selected],new FrameLayout.LayoutParams(-1,-1));
        for(int n=0;n<tabs.length;n++){
            boolean on=n==selected;tabs[n].setTextColor(on?Color.WHITE:0xFF263238);tabs[n].setBackgroundColor(on?0xFF263238:0xFFECEFF1);
        }
        refresh();
    }

    private TextView card(String title){
        TextView t=UiKit.text(c,title);t.setTextColor(TEXT);t.setTextSize(13);t.setBackgroundColor(CARD);t.setPadding(UiKit.dp(c,10),UiKit.dp(c,9),UiKit.dp(c,10),UiKit.dp(c,9));return t;
    }
    private TextView kpi(String title){
        TextView t=card(title);t.setTextSize(14);t.setTypeface(Typeface.DEFAULT_BOLD);return t;
    }
    private void colorState(TextView t,boolean good,boolean warning){t.setTextColor(good?GOOD:warning?WARN:BAD);}

    private final class PhysicalPane {
        final ScrollView view;
        final TextView[] values=new TextView[PhysicalCatalog.COUNT],states=new TextView[PhysicalCatalog.COUNT];
        final TextView envDiagnostic,soundRms,soundRmsState,soundP2p,soundP2pState,irValue,irState;

        PhysicalPane(){
            view=new ScrollView(c);LinearLayout box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,8));view.addView(box);
            box.addView(UiKit.title(c,"القياسات الفيزيائية والتشخيصية"),UiKit.full(c));
            TableLayout table=new TableLayout(c);table.setStretchAllColumns(true);table.setShrinkAllColumns(true);table.addView(headerRow());
            for(int i=0;i<PhysicalCatalog.COUNT;i++){
                TableRow row=new TableRow(c);row.setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);
                row.addView(tableCell(PhysicalCatalog.NAMES_AR[i],false));values[i]=tableCell("—",false);row.addView(values[i]);row.addView(tableCell(PhysicalCatalog.UNITS[i],false));states[i]=tableCell("غير مركب",false);row.addView(states[i]);table.addView(row);
            }
            TableRow rms=auxRow("MAX4466 RMS","V");soundRms=(TextView)rms.getChildAt(1);soundRmsState=(TextView)rms.getChildAt(3);table.addView(rms);
            TableRow p2p=auxRow("MAX4466 P-P","V");soundP2p=(TextView)p2p.getChildAt(1);soundP2pState=(TextView)p2p.getChildAt(3);table.addView(p2p);
            TableRow ir=auxRow("IR أرض/حافة","حالة");irValue=(TextView)ir.getChildAt(1);irState=(TextView)ir.getChildAt(3);table.addView(ir);
            box.addView(table,UiKit.full(c));
            envDiagnostic=UiKit.small(c,"ENV • بانتظار الاتصال");envDiagnostic.setTextColor(MUTED);envDiagnostic.setTextIsSelectable(true);envDiagnostic.setBackgroundColor(CARD);box.addView(envDiagnostic,UiKit.full(c));
        }
        private TableRow headerRow(){TableRow r=new TableRow(c);r.addView(tableCell("القياس",true));r.addView(tableCell("القيمة",true));r.addView(tableCell("الوحدة",true));r.addView(tableCell("الحالة / المصدر",true));return r;}
        private TableRow auxRow(String name,String unit){TableRow r=new TableRow(c);r.setBackgroundColor(0xFF1B222B);r.addView(tableCell(name,false));r.addView(tableCell("—",false));r.addView(tableCell(unit,false));r.addView(tableCell("—",false));return r;}
        private TextView tableCell(String text,boolean header){TextView v=new TextView(c);v.setText(text);v.setTextColor(header?Color.WHITE:0xFFCFD8DC);v.setTextSize(header?12:11);v.setPadding(UiKit.dp(c,4),UiKit.dp(c,7),UiKit.dp(c,4),UiKit.dp(c,7));if(header){v.setBackgroundColor(0xFF263238);v.setTypeface(Typeface.DEFAULT_BOLD);}return v;}
        private void reset(String state){for(int i=0;i<PhysicalCatalog.COUNT;i++){values[i].setText("—");states[i].setText(state);states[i].setTextColor(0xFF90A4AE);}soundRms.setText("—");soundP2p.setText("—");irValue.setText("—");soundRmsState.setText(state);soundP2pState.setText(state);irState.setText(state);}
        private void setPhysical(int index,double value,String source){if(Double.isFinite(value)){values[index].setText(String.format(Locale.US,index==14?"%.1f":"%.2f",value));states[index].setText(source);states[index].setTextColor(GOOD);}else{values[index].setText("—");states[index].setText(source);states[index].setTextColor(WARN);}}
        void refresh(){
            JSONObject d=host.latestDiagnostics();if(d==null){reset("غير متصل");envDiagnostic.setText("ENV • غير متصل");return;}reset("غير مركب");
            JSONObject e=d.optJSONObject("env");envDiagnostic.setText(envDiagnosticText(e));
            if(e!=null&&e.optBoolean("ok",false)){
                String type=e.optString("type","ENV"),src=type+" • "+e.optString("address","—")+" • "+e.optString("chip_id","—");double t=e.optDouble("temperature",Double.NaN),h=e.optDouble("humidity",Double.NaN),p=e.optDouble("pressure",Double.NaN);
                setPhysical(0,t,src);setPhysical(1,h,Double.isFinite(h)?src:"غير متاح في "+type);setPhysical(2,p,src);if(Double.isFinite(p)&&p>0)setPhysical(14,44330.0*(1.0-Math.pow(p/1013.25,0.1903)),"مشتق من الضغط");else states[14].setText("ضغط غير صالح");
            }else{
                String reason=envReason(e);states[0].setText(reason);states[1].setText(reason);states[2].setText(reason);states[14].setText("يتطلب ضغطًا صالحًا");states[0].setTextColor(WARN);states[1].setTextColor(WARN);states[2].setTextColor(WARN);
            }
            JSONObject ldr=d.optJSONObject("ldr");if(ldr!=null&&ldr.optBoolean("installed",false)){states[3].setText("LDR • عتبة فقط");states[3].setTextColor(WARN);states[4].setText("لا توجد معايرة lm");}
            JSONObject s=d.optJSONObject("sound");double rms=s==null?Double.NaN:s.optDouble("rms_v",Double.NaN),p2p=s==null?Double.NaN:s.optDouble("p2p_v",Double.NaN);
            if(Double.isFinite(rms)){soundRms.setText(String.format(Locale.US,"%.4f",rms));soundRmsState.setText("MAX4466");soundRmsState.setTextColor(GOOD);}else soundRmsState.setText("لا توجد قراءة");
            if(Double.isFinite(p2p)){soundP2p.setText(String.format(Locale.US,"%.4f",p2p));soundP2pState.setText("MAX4466");soundP2pState.setTextColor(GOOD);}else soundP2pState.setText("لا توجد قراءة");
            double db=s==null?Double.NaN:s.optDouble("db_spl",Double.NaN);if(Double.isFinite(db)){values[5].setText(String.format(Locale.US,"%.1f",db));states[5].setText("MAX4466 • dB SPL تقديري");states[5].setTextColor(GOOD);}else{values[5].setText("—");states[5].setText(Double.isFinite(rms)||Double.isFinite(p2p)?"MAX4466 • بانتظار dB SPL":"MAX4466 غير متصل");states[5].setTextColor(WARN);}
            JSONObject ir=d.optJSONObject("ir");if(ir!=null){boolean cliff=ir.optBoolean("cliff_warning",ir.optBoolean("warning",false)),ground=ir.optBoolean("ground_detected",!cliff);irValue.setText(cliff?"⚠ لا توجد أرض — خطر سقوط":ground?"الأرض مكتشفة":"جارٍ التحقق");irState.setText("GPIO35 • مرجع "+String.format(Locale.US,"%.0f",ir.optDouble("reference_height_cm",8.0))+" cm • raw="+ir.optInt("raw_level",ir.optInt("level",-1)));irValue.setTextColor(cliff?BAD:ground?GOOD:WARN);irState.setTextColor(MUTED);}else irState.setText("غير متصل");
        }
    }

    private final class LivePane {
        final ScrollView view;final LinearLayout box,cards;
        final TextView powerCard,arrayCard,gasCard,sessionCard;
        final ProgressBar progress;
        final SensorCard[] sensorCards=new SensorCard[HazRobHardwareProfile.MQ.length];
        final Button powerToggle,start,cancel,save;

        LivePane(){
            view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            box.addView(UiKit.title(c,"القياس الحي • HR069"),UiKit.full(c));
            LinearLayout row1=UiKit.hbox(c);powerCard=kpi("MQ\n—");arrayCard=kpi("المصفوفة\n—");row1.addView(powerCard,UiKit.weight(1));row1.addView(arrayCard,UiKit.weight(1));box.addView(row1,UiKit.full(c));
            LinearLayout row2=UiKit.hbox(c);gasCard=kpi("الاستجابة\n—");sessionCard=kpi("الجلسة\n—");row2.addView(gasCard,UiKit.weight(1));row2.addView(sessionCard,UiKit.weight(1));box.addView(row2,UiKit.full(c));
            progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgress(0);box.addView(progress,UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);
            powerToggle=UiKit.button(c,"تشغيل MQ");powerToggle.setOnClickListener(v->togglePower());actions.addView(powerToggle,UiKit.weight(1));
            start=UiKit.button(c,"بدء القياس");start.setOnClickListener(v->measure(true));actions.addView(start,UiKit.weight(1));
            cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measure(false));actions.addView(cancel,UiKit.weight(.75f));
            save=UiKit.button(c,"حفظ");save.setOnClickListener(v->save());actions.addView(save,UiKit.weight(.75f));box.addView(actions,UiKit.full(c));
            TextView ch=UiKit.small(c,"قنوات MQ • الاستجابة والاتجاه وجودة الإشارة");ch.setTextColor(MUTED);box.addView(ch,UiKit.full(c));
            cards=UiKit.vbox(c);box.addView(cards,UiKit.full(c));for(int i=0;i<sensorCards.length;i++){sensorCards[i]=new SensorCard(HazRobHardwareProfile.MQ[i]);cards.addView(sensorCards[i].root,UiKit.full(c));}
        }
        private void togglePower(){JSONObject d=host.latestDiagnostics(),r=d==null?null:d.optJSONObject("relay");boolean on=r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);setPower(!on);}
        private void setPower(boolean on){powerToggle.setEnabled(false);host.io().execute(()->{try{host.robotApi().mqPower(on);box.post(()->{powerToggle.setEnabled(true);host.refreshAllTabs();});}catch(Exception e){box.post(()->{powerToggle.setEnabled(true);host.showToast("تعذر تغيير طاقة MQ");});}});}
        private void measure(boolean begin){
            if(begin){JSONObject d=host.latestDiagnostics(),adc=d==null?null:d.optJSONObject("adc");boolean a1=adc!=null&&adc.optBoolean("ads1",false),a2=adc!=null&&adc.optBoolean("ads2",false);if(!a1&&!a2){host.showToast("لا يمكن بدء الجلسة: ADS1 وADS2 غير مكتشفين");return;}}
            host.io().execute(()->{try{if(begin)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر تنفيذ أمر الجلسة: "+String.valueOf(e.getMessage()));}});
        }
        private void save(){
            JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات للحفظ");return;}
            host.io().execute(()->{try{String ts=isoNow();ChemicalRepository repo=new ChemicalRepository(host.database());SqliteCalibrationProvider cal=new SqliteCalibrationProvider(host.database());String analytics=AnalyticsSnapshot.toJson(d,repo,cal,false);JSONObject relay=d.optJSONObject("relay");int power=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0)?1:0;long id=host.database().save(ts,d.toString(),"HazRob "+ReleaseAudit.VERSION+" acquisition session • MQ Power "+(power==1?"ON":"OFF")+" • GPIO25",analytics);host.showToast("تم حفظ الجلسة #"+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ الجلسة");}});
        }
        void refresh(){
            JSONObject d=host.latestDiagnostics();if(d==null){powerCard.setText("MQ\nغير متصل");arrayCard.setText("المصفوفة\n—");gasCard.setText("الاستجابة\n—");sessionCard.setText("الجلسة\n—");progress.setProgress(0);powerToggle.setText("تشغيل MQ");for(SensorCard sc:sensorCards)sc.refresh(false,false,null,null,null);return;}
            JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),mq=d.optJSONObject("mq"),m=d.optJSONObject("measurement");
            boolean powered=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0),a1=adc!=null&&adc.optBoolean("ads1",false),a2=adc!=null&&adc.optBoolean("ads2",false);
            powerCard.setText("MQ "+(powered?"● ON":"○ OFF")+"\nADS "+((a1?1:0)+(a2?1:0))+"/2");colorState(powerCard,powered&&a1&&a2,powered);powerToggle.setText(powered?"إطفاء MQ":"تشغيل MQ");
            JSONObject sd=d.optJSONObject("sensor_power");if(sd==null)sd=LiveSensorDiagnostic.evaluate(relay,adc,mq).toJson();int valid=sd.optInt("valid_channels",0),available=sd.optInt("available_channels",0),unstable=0;
            for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){SensorHealthEngine.Snapshot h=host.sensorHealth(hw.key);if(h!=null&&(h.state==SensorHealthEngine.State.UNSTABLE||h.state==SensorHealthEngine.State.DRIFT_WARNING))unstable++;}
            arrayCard.setText("المصفوفة "+valid+"/"+available+"\nغير مستقرة "+unstable);colorState(arrayCard,valid>=6&&unstable==0,valid>=4);
            LiveGasStateEngine.Snapshot gas=host.liveGasState();String gs=gas==null?"غير متاح":liveGasStateAr(gas.state);gasCard.setText("الاستجابة\n"+gs+(gas==null?"":" • "+gas.respondingChannels+" قنوات"));boolean gasNormal=gas!=null&&gas.state==LiveGasStateEngine.State.NORMAL;boolean gasWatch=gas!=null&&(gas.state==LiveGasStateEngine.State.WARMING_UP||gas.state==LiveGasStateEngine.State.BASELINE_REQUIRED);colorState(gasCard,gasNormal,gasWatch);
            String st=m==null?"idle":m.optString("state","idle");int total=m==null?0:m.optInt("duration_s",0),elapsed=m==null?0:m.optInt("elapsed_s",0),remain=m==null?0:m.optInt("remaining_s",Math.max(0,total-elapsed)),validSession=m==null?0:m.optInt("valid_channels",0);String err=m==null?"":m.optString("error","");
            if("measuring".equals(st)){sessionCard.setText("الجلسة • قياس\nمتبقي "+remain+" s");progress.setProgress(total>0?Math.min(1000,Math.max(0,Math.round(1000f*elapsed/total))):0);}
            else if("ready".equals(st)){sessionCard.setText("الجلسة • مكتملة\nقنوات "+validSession);progress.setProgress(1000);}
            else if("error".equals(st)){sessionCard.setText("الجلسة • خطأ\n"+errorAr(err));progress.setProgress(0);}
            else if("warmup".equals(st)){sessionCard.setText("الجلسة • تسخين\nانتظار الاستقرار");progress.setProgress(0);}
            else{sessionCard.setText("الجلسة • خاملة\nالقراءة الحية مستمرة");progress.setProgress(0);}
            colorState(sessionCard,"ready".equals(st),"measuring".equals(st)||"warmup".equals(st)||"idle".equals(st));
            for(SensorCard sc:sensorCards)sc.refresh(true,powered,relay,adc,mq==null?null:mq.optJSONObject(sc.hw.key));
        }
    }

    private String liveGasStateAr(LiveGasStateEngine.State s){if(s==null)return "غير متاح";switch(s){case INSTANT_RESPONSE:return "استجابة فورية";case RESPONSE_WATCH:return "استجابة متعددة";case RESPONSE_ALERT:return "استجابة قوية";case WARMING_UP:return "تسخين";case BASELINE_REQUIRED:return "خط أساس مطلوب";case DEGRADED:return "قياس متدهور";case NORMAL:return "طبيعي";default:return "غير متاح";}}

    private final class SensorCard {
        final HazRobHardwareProfile.Channel hw;final LinearLayout root;final TextView head,data;final AlphaAnimation pulse;
        SensorCard(HazRobHardwareProfile.Channel hw){this.hw=hw;root=UiKit.card(c);head=UiKit.text(c,"");head.setTextSize(14);head.setTypeface(Typeface.DEFAULT_BOLD);root.addView(head,UiKit.full(c));data=UiKit.small(c,"");data.setTextColor(0xFFCFD8DC);root.addView(data,UiKit.full(c));pulse=new AlphaAnimation(.45f,1f);pulse.setDuration(450);pulse.setRepeatMode(Animation.REVERSE);pulse.setRepeatCount(Animation.INFINITE);}
        void refresh(boolean connected,boolean powered,JSONObject relay,JSONObject adc,JSONObject q){
            head.clearAnimation();if(!connected){head.setBackgroundColor(0xFF424242);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • غير متصل");data.setText("Raw — • Vadc — • Vao — • Rs/R0 —");return;}
            if(!powered){head.setBackgroundColor(0xFF455A64);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • MQ OFF");data.setText(InstantMqWarningEngine.target(hw.key));return;}
            boolean adsReady=adc!=null&&adc.optBoolean(hw.adsIndex==0?"ads1":"ads2",false);if(!adsReady){head.setBackgroundColor(0xFFB71C1C);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • ADS"+(hw.adsIndex+1)+" غير مكتشف");data.setText("تحقق من التغذية وSDA/SCL وADDR");return;}
            long powerMs=relay==null?0:relay.optLong("power_ms",0);boolean qValid=q!=null&&q.optBoolean("ok",q.optBoolean("valid",false));if(!qValid){boolean settling=powerMs>0&&powerMs<LiveSensorDiagnostic.SETTLE_DIAGNOSTIC_MS;head.setBackgroundColor(settling?0xFF616161:0xFFEF6C00);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+(settling?" • انتظار القراءة":" • إشارة غير صالحة"));data.setText("Raw "+num(q,"raw",0)+" • Vadc "+num(q,"adc",3)+" V • Vao "+num(q,"ao",3)+" V");return;}
            int warm=Math.max(0,host.preferences().getInt("warm",60));long remain=Math.max(0,warm*1000L-powerMs);boolean ready=remain==0;SensorHealthEngine.Snapshot health=host.sensorHealth(hw.key);InstantMqWarningEngine.Result warn=InstantMqWarningEngine.evaluate(hw.key,q,health);boolean baseline=health!=null&&Double.isFinite(health.baselineRatio)&&health.baselineRatio>0;
            if(!ready){head.setBackgroundColor(0xFF616161);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+" • تسخين "+clock(remain));}
            else if(warn.level==InstantMqWarningEngine.Level.HIGH){head.setBackgroundColor(0xFFC62828);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+" • "+warn.alert);head.startAnimation(pulse);}
            else if(warn.level==InstantMqWarningEngine.Level.ELEVATED){head.setBackgroundColor(0xFFD32F2F);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+" • "+warn.alert);}
            else if(warn.level==InstantMqWarningEngine.Level.RESPONSE){head.setBackgroundColor(0xFF00695C);head.setTextColor(Color.WHITE);head.setText("↕ "+hw.key.toUpperCase(Locale.US)+" • استجابة مرصودة");}
            else{head.setBackgroundColor(0xFF1565C0);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+(baseline?" • جاهز":" • baseline مدمج"));}
            double ratio=AcceptedR0Normalizer.ratio(hw.key,q),acceptedR0=FieldR0Baseline.value(hw.key),fwR0=q.optDouble("r0",Double.NaN);boolean fwSync=Double.isFinite(fwR0)&&Double.isFinite(acceptedR0)&&acceptedR0>0&&Math.abs(fwR0-acceptedR0)/acceptedR0<=0.001;String delta=Double.isFinite(warn.delta)?String.format(Locale.US,"%+.1f%%",100.0*warn.delta):"—";String trend="—";if(health!=null&&Double.isFinite(health.baselineRatio)&&health.baselineRatio>0&&Double.isFinite(ratio))trend=ratio>health.baselineRatio?"↑":"↓";
            data.setText("Δ "+delta+" • اتجاه "+trend+" • Noise CV "+(health!=null&&Double.isFinite(health.noiseCv)?String.format(Locale.US,"%.1f%%",100*health.noiseCv):"—")+"\nRaw "+num(q,"raw",0)+" • Vadc "+num(q,"adc",3)+" V • Vao "+num(q,"ao",3)+" V\nRs "+num(q,"rs",3)+" • Rs/R0 "+(Double.isFinite(ratio)?String.format(Locale.US,"%.3f",ratio):"—")+" • R0 "+String.format(Locale.US,"%.3f kΩ",acceptedR0)+"\n"+(fwSync?"R0 متزامن":"App R0 authority")+" • "+SensorHealthEngine.stateAr(health.state));
        }
    }

    private final class AnalysisPane {
        final ScrollView view;final LinearLayout box;
        final TextView readiness,topCandidate,confidence,evidence,quantitative,quality,candidatePreview;
        TextView technical;
        final Button technicalToggle;
        final ChemCoreDecisionRepository decisions;
        boolean technicalVisible=false;

        AnalysisPane(){
            view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            box.addView(UiKit.title(c,"ChemCore • لوحة التحليل"),UiKit.full(c));
            LinearLayout r1=UiKit.hbox(c);readiness=kpi("الجاهزية\n—");topCandidate=kpi("المرشح الأول\n—");r1.addView(readiness,UiKit.weight(1));r1.addView(topCandidate,UiKit.weight(1));box.addView(r1,UiKit.full(c));
            LinearLayout r2=UiKit.hbox(c);confidence=kpi("الثقة\n—");evidence=kpi("الدليل\n—");r2.addView(confidence,UiKit.weight(1));r2.addView(evidence,UiKit.weight(1));box.addView(r2,UiKit.full(c));
            quantitative=kpi("PPM الكمي\n—");box.addView(quantitative,UiKit.full(c));
            quality=card("جودة التحليل: —");box.addView(quality,UiKit.full(c));
            candidatePreview=card("المرشحون: —");candidatePreview.setTextIsSelectable(true);box.addView(candidatePreview,UiKit.full(c));
            LinearLayout nav=UiKit.hbox(c);Button openResults=UiKit.button(c,"فتح النتائج التفصيلية");openResults.setOnClickListener(v->show(3));nav.addView(openResults,UiKit.weight(1.4f));technicalToggle=UiKit.button(c,"التفاصيل التقنية");technicalToggle.setOnClickListener(v->{technicalVisible=!technicalVisible;technical.setVisibility(technicalVisible?View.VISIBLE:View.GONE);technicalToggle.setText(technicalVisible?"إخفاء التفاصيل":"التفاصيل التقنية");});nav.addView(technicalToggle,UiKit.weight(1));box.addView(nav,UiKit.full(c));
            technical=card("");technical.setTextIsSelectable(true);technical.setVisibility(View.GONE);box.addView(technical,UiKit.full(c));
            LinearLayout exp=UiKit.hbox(c);Button pdf=UiKit.button(c,"PDF");pdf.setOnClickListener(v->export("pdf"));exp.addView(pdf,UiKit.weight(1));Button xls=UiKit.button(c,"Excel");xls.setOnClickListener(v->export("xlsx"));exp.addView(xls,UiKit.weight(1));Button raw=UiKit.button(c,"Raw");raw.setOnClickListener(v->export("raw"));exp.addView(raw,UiKit.weight(1));box.addView(exp,UiKit.full(c));
            decisions=new ChemCoreDecisionRepository(host.database());
        }
        void refresh(){
            JSONObject d=host.latestDiagnostics();ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(d,host.driveLeft(),host.driveRight(),host.database());
            boolean ready=q.configurationReady&&q.captureReady;readiness.setText("الجاهزية\n"+(ready?"● READY":"● WAIT"));colorState(readiness,ready,q.configurationReady);
            quality.setText("MQ صالح "+q.validMqChannels+"/8 • R0 "+q.r0NormalizedChannels+"/8 • M4-ref "+q.referenceValidatedChannels+"/8 • ENV "+(q.environmentReady?"جاهز":"محجوب"));quality.setTextColor(q.validMqChannels>=6?GOOD:WARN);
            String id=host.lastMeasurementSessionId();JSONObject p=id==null||id.isEmpty()?null:decisions.latestPayloadForSession(id);if(p!=null&&p.optJSONObject("lab_quality")==null){decisions.attachLabQuality(id,q.toJson(),System.currentTimeMillis());p=decisions.latestPayloadForSession(id);}
            if(p==null){topCandidate.setText("المرشح الأول\n—");confidence.setText("الثقة\n—");evidence.setText("الدليل\n—");quantitative.setText("PPM الكمي\nلا توجد جلسة محللة");candidatePreview.setText("لا توجد نتيجة ChemCore مرتبطة بجلسة مكتملة.");technical.setText("لا توجد تفاصيل تقنية.");return;}
            String chemical=p.optString("chemical","—"),tier=p.optString("evidence_tier","—"),outcome=p.optString("outcome","—");double post=p.optDouble("top_posterior",Double.NaN),runner=p.optDouble("runner_up_posterior",Double.NaN),margin=p.optDouble("posterior_separation_margin",Double.NaN),ood=p.optDouble("ood_score",Double.NaN),entropy=p.optDouble("normalized_entropy",Double.NaN);
            topCandidate.setText("المرشح الأول\n"+chemical);topCandidate.setTextColor(INFO);
            confidence.setText("الثقة\n"+pct(post)+" • Δ "+pct(margin));confidence.setTextColor(Double.isFinite(post)&&post>=0.7?GOOD:Double.isFinite(post)&&post>=0.4?WARN:BAD);
            evidence.setText("الدليل\n"+tier+" • "+outcome);evidence.setTextColor("M4_HAZROB_VALIDATED".equals(tier)?GOOD:INFO);
            boolean reportable=p.optBoolean("ppm_reportable",false);double ppm=p.optDouble("ppm",Double.NaN);String ppmText=reportable&&Double.isFinite(ppm)?String.format(Locale.US,"%.2f ppm • قابل للتقرير",ppm):"محجوب • "+p.optString("ppm_withheld_reason","بوابة كمية غير مكتملة");quantitative.setText("PPM الكمي\n"+ppmText);quantitative.setTextColor(reportable?GOOD:WARN);
            quality.setText("جودة التحليل • MQ "+q.validMqChannels+"/8 • ENV "+(q.environmentReady?"READY":"WITHHELD")+" • OOD "+fmt(ood)+" • Entropy "+fmt(entropy)+" • Runner "+pct(runner));
            candidatePreview.setText(compactCandidates(p,4));
            technical.setText(ChemCoreAnalysisPresenter.equationsAndResiduals(p,4));
        }
        void export(String format){
            String sid=host.lastMeasurementSessionId();
            if(sid==null||sid.isEmpty()){host.showToast("لا توجد جلسة قياس محفوظة مطابقة للمعروض");return;}
            List<ExperimentQueries.Record> records=ExperimentQueries.bySessionId(host.database(),sid);
            if(records.isEmpty()){host.showToast("لم يُحفظ تقرير لهذه الجلسة بعد");return;}
            ExperimentQueries.Record latest=records.get(records.size()-1);
            if("raw".equals(format))host.requestRawExport(latest.id);
            else host.requestExport(format,latest.id);
        }
    }

    private final class AnalysisResultsPane {
        final ScrollView view;final LinearLayout box,candidateList,screeningList;
        final TextView decisionCard,screeningCard,safetyCard,tracking,selected;
        TextView technical;
        final Button startTracking,screeningToggle,technicalToggle;
        final ChemCoreDecisionRepository decisions;
        int selectedCandidateId=-1;String selectedCandidateName="";boolean showAll144=false,showTechnical=false;

        AnalysisResultsPane(){
            view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            box.addView(UiKit.title(c,"نتائج ChemCore"),UiKit.full(c));
            decisionCard=kpi("القرار: —");box.addView(decisionCard,UiKit.full(c));
            screeningCard=card("فحص 144: —");box.addView(screeningCard,UiKit.full(c));
            safetyCard=card("السلامة والتعرض: —");box.addView(safetyCard,UiKit.full(c));
            tracking=card("التتبع: لا يوجد");box.addView(tracking,UiKit.full(c));selected=card("المرشح المحدد: لا يوجد");box.addView(selected,UiKit.full(c));
            TextView ch=UiKit.small(c,"المرشحون الديناميكيون • اللون ترتيب تحليلي وليس مستوى خطورة");ch.setTextColor(MUTED);box.addView(ch,UiKit.full(c));
            candidateList=UiKit.vbox(c);box.addView(candidateList,UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);startTracking=UiKit.button(c,"بدء التتبع");startTracking.setEnabled(false);startTracking.setOnClickListener(v->{if(selectedCandidateId<0){host.showToast("اختر مرشحًا أولًا");return;}host.setTrackingTarget(selectedCandidateId,selectedCandidateName);host.showToast("بدأ تتبع: "+selectedCandidateName);refresh();});actions.addView(startTracking,UiKit.weight(1));Button stop=UiKit.button(c,"إيقاف التتبع");stop.setOnClickListener(v->{host.setTrackingTarget(-1,"");host.showToast("تم إيقاف التتبع");refresh();});actions.addView(stop,UiKit.weight(1));box.addView(actions,UiKit.full(c));
            screeningToggle=UiKit.button(c,"إظهار فحص 144");screeningToggle.setOnClickListener(v->{showAll144=!showAll144;screeningToggle.setText(showAll144?"إخفاء فحص 144":"إظهار فحص 144");refresh();});box.addView(screeningToggle,UiKit.full(c));screeningList=UiKit.vbox(c);box.addView(screeningList,UiKit.full(c));
            technicalToggle=UiKit.button(c,"إظهار البواقي والتفاصيل");technicalToggle.setOnClickListener(v->{showTechnical=!showTechnical;technical.setVisibility(showTechnical?View.VISIBLE:View.GONE);technicalToggle.setText(showTechnical?"إخفاء البواقي والتفاصيل":"إظهار البواقي والتفاصيل");});box.addView(technicalToggle,UiKit.full(c));technical=card("");technical.setVisibility(View.GONE);technical.setTextIsSelectable(true);box.addView(technical,UiKit.full(c));
            decisions=new ChemCoreDecisionRepository(host.database());
        }
        void refresh(){
            String id=host.lastMeasurementSessionId();JSONObject p=id==null||id.isEmpty()?null:decisions.latestPayloadForSession(id);candidateList.removeAllViews();screeningList.removeAllViews();tracking.setText(host.trackingTargetId()>=0?"التتبع • "+host.trackingTargetName()+" (#"+host.trackingTargetId()+") • فرضية مشغل":"التتبع • لا يوجد");
            if(p==null){decisionCard.setText("القرار: لا توجد نتيجة محفوظة");screeningCard.setText("فحص 144: —");safetyCard.setText("السلامة والتعرض: —");technical.setText("—");return;}
            String chemical=p.optString("chemical","—"),outcome=p.optString("outcome","—"),tier=p.optString("evidence_tier","—");double post=p.optDouble("top_posterior",Double.NaN),margin=p.optDouble("posterior_separation_margin",Double.NaN);boolean ppmOk=p.optBoolean("ppm_reportable",false);double ppm=p.optDouble("ppm",Double.NaN);
            decisionCard.setText("القرار • "+outcome+"\n"+chemical+" • "+pct(post)+" • Δ "+pct(margin)+" • "+tier+(ppmOk&&Double.isFinite(ppm)?" • "+String.format(Locale.US,"%.2f ppm",ppm):" • PPM محجوب"));decisionCard.setTextColor("M4_HAZROB_VALIDATED".equals(tier)?GOOD:INFO);
            JSONObject s=p.optJSONObject("screening_144");screeningCard.setText("فحص 144 • "+(s==null?"غير متاح":s.optString("state","—")+" • "+s.optInt("evaluated_count",0)+"/"+s.optInt("target_count",144)));
            JSONObject g=p.optJSONObject("gas_exposure_ppm");safetyCard.setText(g==null?"السلامة والتعرض • محجوب/غير متاح":"السلامة والتعرض • "+g.optString("toxicity_state",g.optString("state","—"))+" • الاشتعال "+g.optString("flammability_state","—")+" • أولوية "+fmt(g.optDouble("hazard_priority_percent",Double.NaN))+"%");
            renderCandidates(p);if(showAll144)renderScreening(s,p);technical.setText(ChemCoreAnalysisPresenter.equationsAndResiduals(p,8));
        }
        private void renderCandidates(JSONObject p){
            JSONArray a=p.optJSONArray("candidates");if(a==null||a.length()==0){candidateList.addView(card("لا توجد فرضيات قابلة للحل"),UiKit.full(c));return;}int n=Math.min(12,a.length());
            for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;int rank=q.optInt("rank",i+1);long uid=q.optLong("chemical_uid",-1);String name=q.optString("label","—");double posterior=q.optDouble("posterior",Double.NaN),coverage=q.optDouble("coverage_pct",Double.NaN),agreement=q.optDouble("agreement_pct",Double.NaN),residual=q.optDouble("residual_rms",Double.NaN);String tier=q.optString("evidence_tier",p.optString("evidence_tier","—"));int bg=candidateColor(rank-1,"");Button row=UiKit.button(c,(uid==selectedCandidateId?"◉ ":"")+"#"+rank+"  "+name+"\nPosterior "+pct(posterior)+" • Coverage "+pctAny(coverage)+" • Agreement "+pctAny(agreement)+" • Residual "+fmt(residual)+" • "+tier);row.setAllCaps(false);row.setTextColor(foregroundFor(bg));row.setBackgroundColor(bg);row.setOnClickListener(v->{selectedCandidateId=(int)uid;selectedCandidateName=name;selected.setText("المرشح المحدد • "+name+" (#"+uid+") • الاختيار لا يعني قبول الهوية");startTracking.setEnabled(uid>=0);renderCandidates(p);});candidateList.addView(row,UiKit.full(c));}
        }
        private void renderScreening(JSONObject screening,JSONObject p){
            JSONArray rows=screening==null?null:screening.optJSONArray("rows");if(rows==null){screeningList.addView(card("لا توجد مصفوفة فحص 144"),UiKit.full(c));return;}HashMap<Long,Integer> ranks=new HashMap<>();JSONArray candidates=p.optJSONArray("candidates");if(candidates!=null)for(int i=0;i<candidates.length();i++){JSONObject q=candidates.optJSONObject(i);if(q!=null)ranks.put(q.optLong("chemical_uid",-1),i);}
            for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;long uid=r.optLong("index",i);String name=r.optString("name","#"+uid),status=r.optString("status","");Integer rank=ranks.get(uid);int bg=candidateColor(rank,status);TextView row=card((rank==null?"":("#"+(rank+1)+"  "))+name+" • "+status);row.setBackgroundColor(bg);row.setTextColor(foregroundFor(bg));screeningList.addView(row,UiKit.full(c));}
        }
        private int candidateColor(Integer rank,String status){if("OUT_OF_DOMAIN".equals(status)||"UNSUPPORTED_BY_CURRENT_ARRAY".equals(status))return 0xFF616161;if(rank==null)return 0xFFF5F5F5;if(rank==0)return 0xFFEF5350;if(rank==1)return 0xFFFF9800;if(rank==2)return 0xFFFFEB3B;if(rank==3)return 0xFF66BB6A;return 0xFF42A5F5;}
        private int foregroundFor(int bg){int r=Color.red(bg),g=Color.green(bg),b=Color.blue(bg);double lum=.2126*r+.7152*g+.0722*b;return lum>150?Color.BLACK:Color.WHITE;}
    }

    private String compactCandidates(JSONObject p,int limit){
        JSONArray a=p==null?null:p.optJSONArray("candidates");if(a==null||a.length()==0)return "المرشحون: لا توجد فرضيات قابلة للحل";StringBuilder s=new StringBuilder("المرشحون الأعلى");int n=Math.min(Math.max(1,limit),a.length());for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;s.append("\n#").append(q.optInt("rank",i+1)).append(" ").append(q.optString("label","—")).append(" • ").append(pct(q.optDouble("posterior",Double.NaN))).append(" • residual ").append(fmt(q.optDouble("residual_rms",Double.NaN)));}return s.toString();
    }
    private static String pct(double v){if(!Double.isFinite(v))return "—";double q=v<=1.000001?v*100.0:v;return String.format(Locale.US,"%.1f%%",q);}
    private static String pctAny(double v){return pct(v);}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";}
    private static String envDiagnosticText(JSONObject e){if(e==null)return"ENV I²C • SDA GPIO13 / SCL GPIO14 • 100 kHz\nلا توجد بيانات تشخيصية";String type=e.optString("type","NONE"),addr=e.optString("address","—"),chip=e.optString("chip_id","—"),lastAddr=e.optString("last_address","—"),lastChip=e.optString("last_chip_id","—"),status=e.optString("status_code",e.optString("status","unknown"));boolean a76=e.optBoolean("ack_0x76",false),a77=e.optBoolean("ack_0x77",false);long fails=Math.max(0,e.optLong("read_failures",0)),age=Math.max(0,e.optLong("last_ok_age_ms",0));return"ENV I²C • SDA GPIO13 / SCL GPIO14 • 100 kHz\n0x76 "+(a76?"ACK":"NO ACK")+" • 0x77 "+(a77?"ACK":"NO ACK")+"\nSensor "+type+" • address "+addr+" • chip "+chip+"\nLast address "+lastAddr+" • last chip "+lastChip+"\nStatus "+status+" • read failures "+fails+" • last OK age "+age+" ms";}
    private static String envReason(JSONObject e){if(e==null)return"ENV: لا توجد بيانات تشخيصية";String s=e.optString("status_code",e.optString("status","unknown"));if("no_ack".equals(s))return"ENV: لا ACK على 0x76/0x77 • bus GPIO13/14";if("unsupported_chip_id".equals(s))return"ENV: العنوان يستجيب لكن Chip ID غير مدعوم • "+e.optString("last_chip_id","—");if("chip_id_read_failed".equals(s))return"ENV: ACK موجود وفشلت قراءة Chip ID";if("calibration_read_failed".equals(s))return"ENV: فشلت قراءة سجلات المعايرة";if("configuration_failed".equals(s))return"ENV: فشلت كتابة سجلات التهيئة";if("initial_read_failed".equals(s)||"read_failed".equals(s)||"runtime_read_failed_reprobe".equals(s))return"ENV: فشل قراءة البيانات • إعادة اكتشاف تلقائية";return"ENV غير جاهز • "+s+" • 0x76="+(e.optBoolean("ack_0x76",false)?"ACK":"—")+" 0x77="+(e.optBoolean("ack_0x77",false)?"ACK":"—");}
    private static String num(JSONObject o,String k,int d){if(o==null||o.isNull(k))return"—";double v=o.optDouble(k,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"—";}
    private static String clock(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    private static String errorAr(String e){if("mq_power_off".equals(e))return"طاقة MQ متوقفة";if("ads_missing".equals(e))return"ADS1/ADS2 غير مكتشفين";if("power_changed".equals(e))return"تغيرت طاقة MQ أثناء الجلسة";if("no_valid_samples".equals(e))return"لم تصل عينات MQ صالحة";return e==null||e.isEmpty()?"سبب غير محدد":e;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
