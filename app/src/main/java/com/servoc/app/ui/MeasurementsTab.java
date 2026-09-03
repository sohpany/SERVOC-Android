package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.InstantMqWarningEngine;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurements.PhysicalCatalog;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** HazRob 0.9 measurement workspace: Physical | Live | Analysis. */
public final class MeasurementsTab {
    private final Context c;private final HazRobHost host;private final LinearLayout root;private final FrameLayout page;
    private final Button[] tabs=new Button[3];private int selected=1;
    private final PhysicalPane physical;private final LivePane live;private final AnalysisPane analysis;private final View[] views;

    public MeasurementsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;root=UiKit.vbox(c);root.setBackgroundColor(0xFF0E1116);root.setPadding(UiKit.dp(c,5),UiKit.dp(c,4),UiKit.dp(c,5),UiKit.dp(c,4));
        LinearLayout bar=UiKit.hbox(c);String[] names={"الفيزيائية","القياس","التحليل"};for(int i=0;i<3;i++){final int x=i;tabs[i]=UiKit.button(c,names[i]);tabs[i].setOnClickListener(v->show(x));bar.addView(tabs[i],UiKit.weight(1));}root.addView(bar,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        physical=new PhysicalPane();live=new LivePane();analysis=new AnalysisPane();views=new View[]{physical.view,live.view,analysis.view};show(1);
    }
    public View view(){return root;}
    public void refresh(){physical.refresh();live.refresh();analysis.refresh();}
    private void show(int i){selected=Math.max(0,Math.min(2,i));page.removeAllViews();page.addView(views[selected],new FrameLayout.LayoutParams(-1,-1));for(int n=0;n<tabs.length;n++){tabs[n].setTextColor(n==selected?Color.WHITE:Color.DKGRAY);tabs[n].setBackgroundColor(n==selected?0xFF263238:0xFFE0E0E0);}refresh();}

    private final class PhysicalPane{
        final ScrollView view;final TextView[] values=new TextView[PhysicalCatalog.COUNT],states=new TextView[PhysicalCatalog.COUNT];
        final TextView soundRms,soundRmsState,soundP2p,soundP2pState,irValue,irState;
        PhysicalPane(){
            view=new ScrollView(c);LinearLayout box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,8));view.addView(box);
            TableLayout table=new TableLayout(c);table.setStretchAllColumns(true);table.setShrinkAllColumns(true);table.addView(headerRow());
            for(int i=0;i<PhysicalCatalog.COUNT;i++){TableRow row=new TableRow(c);row.setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);row.addView(tableCell(PhysicalCatalog.NAMES_AR[i],false));values[i]=tableCell("—",false);row.addView(values[i]);row.addView(tableCell(PhysicalCatalog.UNITS[i],false));states[i]=tableCell("غير مركب",false);row.addView(states[i]);table.addView(row);}
            TableRow rms=auxRow("MAX4466 RMS","V");soundRms=(TextView)rms.getChildAt(1);soundRmsState=(TextView)rms.getChildAt(3);table.addView(rms);
            TableRow p2p=auxRow("MAX4466 P-P","V");soundP2p=(TextView)p2p.getChildAt(1);soundP2pState=(TextView)p2p.getChildAt(3);table.addView(p2p);
            TableRow ir=auxRow("IR","حالة");irValue=(TextView)ir.getChildAt(1);irState=(TextView)ir.getChildAt(3);table.addView(ir);
            box.addView(table,UiKit.full(c));
        }
        private TableRow headerRow(){TableRow r=new TableRow(c);r.addView(tableCell("القياس",true));r.addView(tableCell("القيمة",true));r.addView(tableCell("الوحدة",true));r.addView(tableCell("الحالة / المصدر",true));return r;}
        private TableRow auxRow(String name,String unit){TableRow r=new TableRow(c);r.setBackgroundColor(0xFF1B222B);r.addView(tableCell(name,false));r.addView(tableCell("—",false));r.addView(tableCell(unit,false));r.addView(tableCell("—",false));return r;}
        private TextView tableCell(String text,boolean header){TextView v=new TextView(c);v.setText(text);v.setTextColor(header?Color.WHITE:0xFFCFD8DC);v.setTextSize(header?12:11);v.setPadding(UiKit.dp(c,4),UiKit.dp(c,7),UiKit.dp(c,4),UiKit.dp(c,7));if(header){v.setBackgroundColor(0xFF263238);v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}return v;}
        private void reset(String state){for(int i=0;i<PhysicalCatalog.COUNT;i++){values[i].setText("—");states[i].setText(state);states[i].setTextColor(0xFF90A4AE);}soundRms.setText("—");soundP2p.setText("—");irValue.setText("—");soundRmsState.setText(state);soundP2pState.setText(state);irState.setText(state);}
        private void setPhysical(int index,double value,String source){if(Double.isFinite(value)){values[index].setText(String.format(Locale.US,index==14?"%.1f":"%.2f",value));states[index].setText(source);states[index].setTextColor(0xFF81C784);}else{values[index].setText("—");states[index].setText(source);states[index].setTextColor(0xFFFFB74D);}}
        void refresh(){
            JSONObject d=host.latestDiagnostics();if(d==null){reset("غير متصل");return;}reset("غير مركب");
            JSONObject e=d.optJSONObject("env");if(e!=null&&e.optBoolean("ok",false)){String type=e.optString("type","ENV");double t=e.optDouble("temperature",Double.NaN),h=e.optDouble("humidity",Double.NaN),p=e.optDouble("pressure",Double.NaN);setPhysical(0,t,type);setPhysical(1,h,Double.isFinite(h)?type:"غير متاح");setPhysical(2,p,type);if(Double.isFinite(p)&&p>0)setPhysical(14,44330.0*(1.0-Math.pow(p/1013.25,0.1903)),"مشتق من الضغط");else states[14].setText("ضغط غير صالح");}else{states[0].setText("ENV غير متصل");states[1].setText("ENV غير متصل");states[2].setText("ENV غير متصل");states[14].setText("يتطلب ضغطًا صالحًا");}
            JSONObject ldr=d.optJSONObject("ldr");if(ldr!=null&&ldr.optBoolean("installed",false)){states[3].setText("LDR • عتبة فقط");states[3].setTextColor(0xFFFFB74D);states[4].setText("لا توجد معايرة lm");}
            JSONObject s=d.optJSONObject("sound");double rms=s==null?Double.NaN:s.optDouble("rms_v",Double.NaN),p2p=s==null?Double.NaN:s.optDouble("p2p_v",Double.NaN);if(Double.isFinite(rms)){soundRms.setText(String.format(Locale.US,"%.4f",rms));soundRmsState.setText("MAX4466");soundRmsState.setTextColor(0xFF81C784);}else soundRmsState.setText("لا توجد قراءة");if(Double.isFinite(p2p)){soundP2p.setText(String.format(Locale.US,"%.4f",p2p));soundP2pState.setText("MAX4466");soundP2pState.setTextColor(0xFF81C784);}else soundP2pState.setText("لا توجد قراءة");states[5].setText(Double.isFinite(rms)||Double.isFinite(p2p)?"MAX4466 • غير معاير dB SPL":"MAX4466 غير متصل");states[5].setTextColor(0xFFFFB74D);
            JSONObject ir=d.optJSONObject("ir");if(ir!=null){boolean w=ir.optBoolean("warning",false);irValue.setText(w?"تحذير":"واضح");irState.setText("GPIO35");irValue.setTextColor(w?0xFFFF7043:0xFF81C784);irState.setTextColor(0xFF81C784);}else irState.setText("غير متصل");
        }
    }

    private final class LivePane{
        final ScrollView view;final LinearLayout box,cards;final TextView powerState,sessionState;final SensorCard[] sensorCards=new SensorCard[HazRobHardwareProfile.MQ.length];
        LivePane(){
            view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);
            LinearLayout power=UiKit.hbox(c);int[] masks={0,1,2,3};String[] labels={"OFF","R1","R2","R1+R2"};for(int i=0;i<masks.length;i++){final int m=masks[i];Button b=UiKit.button(c,labels[i]);b.setOnClickListener(v->setPower(m));power.addView(b,UiKit.weight(1));}box.addView(power,UiKit.full(c));powerState=cell(box);
            cards=UiKit.vbox(c);box.addView(cards,UiKit.full(c));for(int i=0;i<sensorCards.length;i++){sensorCards[i]=new SensorCard(HazRobHardwareProfile.MQ[i]);cards.addView(sensorCards[i].root,UiKit.full(c));}
            LinearLayout actions=UiKit.hbox(c);Button start=UiKit.button(c,"بدء جلسة التحليل");start.setOnClickListener(v->measure(true));actions.addView(start,UiKit.weight(1.4f));Button cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measure(false));actions.addView(cancel,UiKit.weight(.8f));Button save=UiKit.button(c,"حفظ الجلسة");save.setOnClickListener(v->save());actions.addView(save,UiKit.weight(1));box.addView(actions,UiKit.full(c));sessionState=cell(box);
        }
        void setPower(int mask){host.io().execute(()->{try{host.robotApi().relayMask(mask);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر تغيير طاقة MQ");}});}
        void measure(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر تنفيذ أمر الجلسة");}});}
        void save(){JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات للحفظ");return;}host.io().execute(()->{try{String ts=isoNow();ChemicalRepository repo=new ChemicalRepository(host.database());SqliteCalibrationProvider cal=new SqliteCalibrationProvider(host.database());String analytics=AnalyticsSnapshot.toJson(d,repo,cal,false);JSONObject relay=d.optJSONObject("relay");int mask=relay==null?0:relay.optInt("mask",0)&3;long id=host.database().save(ts,d.toString(),"HazRob 0.9 session • mask "+mask,analytics);host.showToast("تم حفظ الجلسة #"+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ الجلسة");}});}
        void refresh(){JSONObject d=host.latestDiagnostics();if(d==null){powerState.setText("MQ   —");for(SensorCard sc:sensorCards){sc.root.setVisibility(View.VISIBLE);sc.refresh(false,false,null,null);}sessionState.setText("الجلسة   —");return;}JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),mq=d.optJSONObject("mq"),m=d.optJSONObject("measurement");int mask=relay==null?0:relay.optInt("mask",0)&3;powerState.setText("R1 "+(((mask&1)!=0)?"● ON":"○ OFF")+"   ADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"● OK":"○ —")+"\nR2 "+(((mask&2)!=0)?"● ON":"○ OFF")+"   ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"● OK":"○ —"));for(SensorCard sc:sensorCards){boolean powered=(mask&(1<<(sc.hw.relay-1)))!=0;sc.root.setVisibility(View.VISIBLE);sc.refresh(true,powered,relay,mq==null?null:mq.optJSONObject(sc.hw.key));}String st=m==null?"idle":m.optString("state","idle");sessionState.setText("الجلسة   "+stateAr(st)+(m!=null&&m.optInt("duration_s",0)>0?"   "+m.optInt("duration_s",0)+" s":""));}
    }

    private final class SensorCard{
        final HazRobHardwareProfile.Channel hw;final LinearLayout root;final TextView head,data;final AlphaAnimation pulse;
        SensorCard(HazRobHardwareProfile.Channel hw){this.hw=hw;root=UiKit.card(c);head=UiKit.text(c,"");head.setTextSize(15);head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);root.addView(head,UiKit.full(c));data=UiKit.small(c,"");data.setTextColor(Color.WHITE);root.addView(data,UiKit.full(c));pulse=new AlphaAnimation(.45f,1f);pulse.setDuration(450);pulse.setRepeatMode(Animation.REVERSE);pulse.setRepeatCount(Animation.INFINITE);}
        void refresh(boolean connected,boolean powered,JSONObject relay,JSONObject q){
            head.clearAnimation();if(!connected){head.setBackgroundColor(0xFF424242);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   — غير متصل\n"+InstantMqWarningEngine.target(hw.key));data.setText("Raw —   Vadc —   Vao —\nRs —   Rs/R0 —");return;}if(!powered){head.setBackgroundColor(0xFF455A64);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   ○ OFF   •   R"+hw.relay+"\n"+InstantMqWarningEngine.target(hw.key));data.setText("Raw —   Vadc —   Vao —\nRs —   Rs/R0 —   معاير");return;}
            long powerMs=relay==null?0:relay.optLong(hw.relay==1?"r1_power_ms":"r2_power_ms",0);int warm=Math.max(0,host.preferences().getInt("warm",60));long remain=Math.max(0,warm*1000L-powerMs);boolean ready=remain==0;SensorHealthEngine.Snapshot health=host.sensorHealth(hw.key);InstantMqWarningEngine.Result warn=ready?InstantMqWarningEngine.evaluate(hw.key,q,health):new InstantMqWarningEngine.Result(InstantMqWarningEngine.Level.UNAVAILABLE,InstantMqWarningEngine.target(hw.key),"",Double.NaN);
            if(!ready){head.setBackgroundColor(0xFF616161);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   تسخين "+clock(remain)+"\n"+InstantMqWarningEngine.target(hw.key));}else if(warn.level==InstantMqWarningEngine.Level.HIGH){head.setBackgroundColor(0xFFC62828);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+"   "+warn.alert);head.startAnimation(pulse);}else if(warn.level==InstantMqWarningEngine.Level.ELEVATED){head.setBackgroundColor(0xFFEF6C00);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+"   "+warn.alert);}else{head.setBackgroundColor(0xFF1565C0);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   ● جاهز\n"+InstantMqWarningEngine.target(hw.key));}
            if(q==null){data.setText("Raw —   Vadc —   Vao —\nRs —   Rs/R0 —   معاير");return;}data.setText("Raw   "+num(q,"raw",0)+"   Vadc   "+num(q,"adc",3)+" V   Vao   "+num(q,"ao",3)+" V\nRs   "+num(q,"rs",3)+"   Rs/R0   "+num(q,"ratio",3)+(q.has("ppm")?"   PPM   "+num(q,"ppm",2):"")+"\nمعاير   •   "+SensorHealthEngine.stateAr(health.state));}
    }

    private final class AnalysisPane{
        final ScrollView view;final LinearLayout box,list;final TextView state;
        AnalysisPane(){view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);state=cell(box);list=UiKit.vbox(c);box.addView(list,UiKit.full(c));LinearLayout exp=UiKit.hbox(c);Button pdf=UiKit.button(c,"PDF");pdf.setOnClickListener(v->export("pdf"));exp.addView(pdf,UiKit.weight(1));Button xls=UiKit.button(c,"Excel");xls.setOnClickListener(v->export("xlsx"));exp.addView(xls,UiKit.weight(1));Button raw=UiKit.button(c,"Raw");raw.setOnClickListener(v->export("raw"));exp.addView(raw,UiKit.weight(1));box.addView(exp,UiKit.full(c));}
        void refresh(){JSONObject d=host.latestDiagnostics();list.removeAllViews();if(d==null){state.setText("التحليل   —");return;}SqliteCalibrationProvider cal=new SqliteCalibrationProvider(host.database());Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d,cal,false);List<ValidatedGasEngine.Result> rows=new ArrayList<>();for(ValidatedGasEngine.Result r:map.values())if(r!=null&&r.totalContributors>0)rows.add(r);rows.sort(CandidateRanker.COMPARATOR);state.setText("النتائج   "+rows.size()+"   •   هدف التتبع   "+(host.trackingTargetName().isEmpty()?"—":host.trackingTargetName()));ChemicalRepository repo=new ChemicalRepository(host.database());for(ValidatedGasEngine.Result r:rows){ChemicalRepository.Chemical ch=repo.byLegacyId(r.chemical);String name=ch==null?"Chemical #"+r.chemical:(ch.arabicName!=null&&!ch.arabicName.isEmpty()?ch.arabicName:ch.name);LinearLayout card=UiKit.card(c);LinearLayout row=UiKit.hbox(c);TextView t=UiKit.text(c,name+"\n"+(r.quantitative?String.format(Locale.US,"%.2f ppm   •   ",r.ppm):"بصمة   •   ")+String.format(Locale.US,"اتفاق %.0f%%   •   %s",r.agreement,UiKit.stars(r.confidence)));row.addView(t,new LinearLayout.LayoutParams(0,-2,1f));Button track=UiKit.button(c,host.trackingTargetId()==r.chemical?"إلغاء التتبع":"تتبع");final int id=r.chemical;final String n=name;track.setOnClickListener(v->{if(host.trackingTargetId()==id)host.setTrackingTarget(-1,"");else host.setTrackingTarget(id,n);host.refreshAllTabs();});row.addView(track,new LinearLayout.LayoutParams(UiKit.dp(c,105),-2));card.addView(row,UiKit.full(c));list.addView(card,UiKit.full(c));}}
        void export(String format){List<ExperimentQueries.Record> r=ExperimentQueries.recent(host.database(),1);if(r.isEmpty()){host.showToast("احفظ جلسة أولًا");return;}if("raw".equals(format))host.requestRawExport(r.get(0).id);else host.requestExport(format,r.get(0).id);}
    }

    private TextView cell(LinearLayout parent){TextView t=UiKit.text(c,"—");t.setBackgroundColor(0xFF151A20);parent.addView(t,UiKit.full(c));return t;}
    private static String num(JSONObject o,String k,int d){if(o==null||o.isNull(k))return"—";double v=o.optDouble(k,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"—";}
    private static String clock(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    private static String stateAr(String s){if("measuring".equals(s))return"قياس";if("ready".equals(s))return"مكتملة";if("error".equals(s))return"خطأ";if("warmup".equals(s))return"تسخين";return"خاملة";}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
