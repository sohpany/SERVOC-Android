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
import android.widget.TextView;
import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.InstantMqWarningEngine;
import com.servoc.app.measurement.SensorHealthEngine;
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
        final ScrollView view;final TextView env,sound,ir;
        PhysicalPane(){view=new ScrollView(c);LinearLayout box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,5),UiKit.dp(c,5),UiKit.dp(c,5),UiKit.dp(c,8));view.addView(box);env=cell(box);sound=cell(box);ir=cell(box);}
        void refresh(){JSONObject d=host.latestDiagnostics();if(d==null){env.setText("البيئة   —");sound.setText("MAX4466   —");ir.setText("IR   —");return;}JSONObject e=d.optJSONObject("env");if(e!=null&&e.optBoolean("ok",false))env.setText(String.format(Locale.US,"الحرارة   %.2f °C\nالرطوبة   %s\nالضغط   %.1f hPa",e.optDouble("temperature",Double.NaN),Double.isFinite(e.optDouble("humidity",Double.NaN))?String.format(Locale.US,"%.1f %%",e.optDouble("humidity")):"—",e.optDouble("pressure",Double.NaN)));else env.setText("BME/BMP   ○ غير متصل");JSONObject s=d.optJSONObject("sound");sound.setText(s==null?"MAX4466   —":String.format(Locale.US,"MAX4466\nRMS   %.4f V\nP-P   %.4f V",s.optDouble("rms_v",0),s.optDouble("p2p_v",0)));JSONObject q=d.optJSONObject("ir");boolean w=q!=null&&q.optBoolean("warning",false);ir.setText("IR   "+(w?"⚠ عائق / قرب":"● واضح"));ir.setTextColor(w?0xFFFF7043:0xFF81C784);}
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
        void save(){JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات للحفظ");return;}host.io().execute(()->{try{String ts=isoNow(),analytics=AnalyticsSnapshot.toJson(d);JSONObject relay=d.optJSONObject("relay");int mask=relay==null?0:relay.optInt("mask",0)&3;long id=host.database().save(ts,d.toString(),"HazRob 0.9 session • mask "+mask,analytics);host.showToast("تم حفظ الجلسة #"+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ الجلسة");}});}
        void refresh(){JSONObject d=host.latestDiagnostics();if(d==null){powerState.setText("MQ   —");for(SensorCard sc:sensorCards)sc.root.setVisibility(View.GONE);sessionState.setText("الجلسة   —");return;}JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),mq=d.optJSONObject("mq"),m=d.optJSONObject("measurement");int mask=relay==null?0:relay.optInt("mask",0)&3;powerState.setText("R1 "+(((mask&1)!=0)?"● ON":"○ OFF")+"   ADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"● OK":"○ —")+"\nR2 "+(((mask&2)!=0)?"● ON":"○ OFF")+"   ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"● OK":"○ —"));for(SensorCard sc:sensorCards){boolean active=(mask&(1<<(sc.hw.relay-1)))!=0;sc.root.setVisibility(active?View.VISIBLE:View.GONE);if(active)sc.refresh(relay,mq==null?null:mq.optJSONObject(sc.hw.key));}String st=m==null?"idle":m.optString("state","idle");sessionState.setText("الجلسة   "+stateAr(st)+(m!=null&&m.optInt("duration_s",0)>0?"   "+m.optInt("duration_s",0)+" s":""));}
    }

    private final class SensorCard{
        final HazRobHardwareProfile.Channel hw;final LinearLayout root;final TextView head,data;final AlphaAnimation pulse;
        SensorCard(HazRobHardwareProfile.Channel hw){this.hw=hw;root=UiKit.card(c);head=UiKit.text(c,"");head.setTextSize(15);head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);root.addView(head,UiKit.full(c));data=UiKit.small(c,"");data.setTextColor(Color.WHITE);root.addView(data,UiKit.full(c));pulse=new AlphaAnimation(.45f,1f);pulse.setDuration(450);pulse.setRepeatMode(Animation.REVERSE);pulse.setRepeatCount(Animation.INFINITE);}
        void refresh(JSONObject relay,JSONObject q){long powerMs=relay==null?0:relay.optLong(hw.relay==1?"r1_power_ms":"r2_power_ms",0);int warm=Math.max(0,host.preferences().getInt("warm",60));long remain=Math.max(0,warm*1000L-powerMs);boolean ready=remain==0;SensorHealthEngine.Snapshot health=host.sensorHealth(hw.key);InstantMqWarningEngine.Result warn=ready?InstantMqWarningEngine.evaluate(hw.key,q,health):new InstantMqWarningEngine.Result(InstantMqWarningEngine.Level.UNAVAILABLE,InstantMqWarningEngine.target(hw.key),"",Double.NaN);
            head.clearAnimation();if(!ready){head.setBackgroundColor(0xFF616161);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   تسخين "+clock(remain)+"\n"+InstantMqWarningEngine.target(hw.key));}else if(warn.level==InstantMqWarningEngine.Level.HIGH){head.setBackgroundColor(0xFFC62828);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+"   "+warn.alert);head.startAnimation(pulse);}else if(warn.level==InstantMqWarningEngine.Level.ELEVATED){head.setBackgroundColor(0xFFEF6C00);head.setTextColor(Color.WHITE);head.setText("⚠ "+hw.key.toUpperCase(Locale.US)+"   "+warn.alert);}else{head.setBackgroundColor(0xFF1565C0);head.setTextColor(Color.WHITE);head.setText(hw.key.toUpperCase(Locale.US)+"   ● جاهز\n"+InstantMqWarningEngine.target(hw.key));}
            if(q==null){data.setText("Raw —   Vadc —   Vao —\nRs —   Rs/R0 —   معاير");return;}data.setText("Raw   "+num(q,"raw",0)+"   Vadc   "+num(q,"adc",3)+" V   Vao   "+num(q,"ao",3)+" V\nRs   "+num(q,"rs",3)+"   Rs/R0   "+num(q,"ratio",3)+(q.has("ppm")?"   PPM   "+num(q,"ppm",2):"")+"\nمعاير   •   "+SensorHealthEngine.stateAr(health.state));}
    }

    private final class AnalysisPane{
        final ScrollView view;final LinearLayout box,list;final TextView state;
        AnalysisPane(){view=new ScrollView(c);box=UiKit.vbox(c);box.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,10));view.addView(box);state=cell(box);list=UiKit.vbox(c);box.addView(list,UiKit.full(c));LinearLayout exp=UiKit.hbox(c);Button pdf=UiKit.button(c,"PDF");pdf.setOnClickListener(v->export("pdf"));exp.addView(pdf,UiKit.weight(1));Button xls=UiKit.button(c,"Excel");xls.setOnClickListener(v->export("xlsx"));exp.addView(xls,UiKit.weight(1));Button raw=UiKit.button(c,"Raw");raw.setOnClickListener(v->export("raw"));exp.addView(raw,UiKit.weight(1));box.addView(exp,UiKit.full(c));}
        void refresh(){JSONObject d=host.latestDiagnostics();list.removeAllViews();if(d==null){state.setText("التحليل   —");return;}Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d);List<ValidatedGasEngine.Result> rows=new ArrayList<>();for(ValidatedGasEngine.Result r:map.values())if(r!=null&&r.totalContributors>0)rows.add(r);rows.sort(CandidateRanker.COMPARATOR);state.setText("النتائج   "+rows.size()+"   •   هدف التتبع   "+(host.trackingTargetName().isEmpty()?"—":host.trackingTargetName()));ChemicalRepository repo=new ChemicalRepository(host.database());for(ValidatedGasEngine.Result r:rows){ChemicalRepository.Chemical ch=repo.byLegacyId(r.chemical);String name=ch==null?"Chemical #"+r.chemical:(ch.arabicName!=null&&!ch.arabicName.isEmpty()?ch.arabicName:ch.name);LinearLayout card=UiKit.card(c);LinearLayout row=UiKit.hbox(c);TextView t=UiKit.text(c,name+"\n"+(r.quantitative?String.format(Locale.US,"%.2f ppm   •   ",r.ppm):"بصمة   •   ")+String.format(Locale.US,"اتفاق %.0f%%   •   %s",r.agreement,UiKit.stars(r.confidence)));row.addView(t,new LinearLayout.LayoutParams(0,-2,1f));Button track=UiKit.button(c,host.trackingTargetId()==r.chemical?"إلغاء التتبع":"تتبع");final int id=r.chemical;final String n=name;track.setOnClickListener(v->{if(host.trackingTargetId()==id)host.setTrackingTarget(-1,"");else host.setTrackingTarget(id,n);host.refreshAllTabs();});row.addView(track,new LinearLayout.LayoutParams(UiKit.dp(c,105),-2));card.addView(row,UiKit.full(c));list.addView(card,UiKit.full(c));}}
        void export(String format){List<ExperimentQueries.Record> r=ExperimentQueries.recent(host.database(),1);if(r.isEmpty()){host.showToast("احفظ جلسة أولًا");return;}if("raw".equals(format))host.requestRawExport(r.get(0).id);else host.requestExport(format,r.get(0).id);}
    }

    private TextView cell(LinearLayout parent){TextView t=UiKit.text(c,"—");t.setBackgroundColor(0xFF151A20);parent.addView(t,UiKit.full(c));return t;}
    private static String num(JSONObject o,String k,int d){if(o==null||o.isNull(k))return"—";double v=o.optDouble(k,Double.NaN);return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"—";}
    private static String clock(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    private static String stateAr(String s){if("measuring".equals(s))return"قياس";if("ready".equals(s))return"مكتملة";if("error".equals(s))return"خطأ";if("warmup".equals(s))return"تسخين";return"خاملة";}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
