package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SensorsTab {
    private final HazRobHost host;
    private final ScrollView scroll;
    private final TextView cycle,envInfo;
    private final TextView[] mqRows=new TextView[RobotTelemetry.MQ_KEYS.length];

    public SensorsTab(Context c,HazRobHost host){
        this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout content=UiKit.vbox(c);content.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));content.setBackgroundColor(0xFF0E1116);scroll.addView(content);

        content.addView(UiKit.title(c,"الحساسات والقياس"),UiKit.full(c));
        LinearLayout envCard=UiKit.card(c);envCard.addView(UiKit.title(c,"الحساس البيئي BME280 / BMP280"),UiKit.full(c));envCard.addView(UiKit.small(c,"قراءة مباشرة ومستقلة عن زر بدء القياس. START خاص بدورة حساسات MQ فقط."),UiKit.full(c));envInfo=UiKit.text(c,"بانتظار بيانات ESP32...");envInfo.setBackgroundColor(0xFF151A20);envCard.addView(envInfo,UiKit.full(c));content.addView(envCard,UiKit.full(c));

        content.addView(UiKit.title(c,"دورة قياس MQ"),UiKit.full(c));cycle=UiKit.text(c,"الحالة: خامل");cycle.setBackgroundColor(0xFF151A20);content.addView(cycle,UiKit.full(c));
        LinearLayout run=UiKit.hbox(c);Button start=UiKit.button(c,"بدء");start.setOnClickListener(v->measurement(true));run.addView(start,UiKit.weight(1));Button cancel=UiKit.button(c,"إلغاء");cancel.setOnClickListener(v->measurement(false));run.addView(cancel,UiKit.weight(1));Button accept=UiKit.button(c,"اعتماد القياس");accept.setOnClickListener(v->accept());run.addView(accept,UiKit.weight(1));Button repeat=UiKit.button(c,"إعادة");repeat.setOnClickListener(v->measurement(true));run.addView(repeat,UiKit.weight(1));content.addView(run,UiKit.full(c));

        content.addView(UiKit.title(c,"قنوات MQ"),UiKit.full(c));for(int i=0;i<mqRows.length;i++){mqRows[i]=UiKit.text(c,RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+"   لم تُرسل بعد");mqRows[i].setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);mqRows[i].setTextIsSelectable(true);content.addView(mqRows[i],UiKit.full(c));}
        content.addView(UiKit.title(c,"صفوف الريليه"),UiKit.full(c));content.addView(UiKit.small(c,"التشغيل اليدوي للريليه يلغي دورة القياس التلقائية في Firmware الحالي."),UiKit.full(c));LinearLayout rel=UiKit.hbox(c);for(int g=1;g<=3;g++){final int x=g;Button b=UiKit.button(c,"الصف "+g);b.setOnClickListener(v->relay(x));rel.addView(b,UiKit.weight(1));}Button off=UiKit.button(c,"إطفاء الكل");off.setOnClickListener(v->relay(0));rel.addView(off,UiKit.weight(1));content.addView(rel,UiKit.full(c));content.addView(UiKit.small(c,"Raw وVadc وVao وRs وRs/R0 قيم استحواذ خام. تحليل PPM يستخدم مكتبات HazRob مع شروط الصلاحية الخاصة بكل حساس."),UiKit.full(c));
    }

    public View view(){return scroll;}
    private void measurement(boolean start){host.io().execute(()->{try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();host.showToast(start?"بدأت دورة قياس MQ":"أُلغيت دورة القياس");}catch(Exception e){host.showToast("تعذر إرسال أمر القياس");}});}
    private void relay(int group){host.io().execute(()->{try{host.robotApi().relay(group);host.showToast(group==0?"تم إطفاء جميع الريليهات":"تم تشغيل صف الريليه "+group);}catch(Exception e){host.showToast("تعذر التحكم بالريليه");}});}
    private void accept(){JSONObject d=host.latestDiagnostics();if(d==null){host.showToast("لا توجد بيانات حية لاعتمادها");return;}JSONObject m=d.optJSONObject("measurement");if(m==null||!"ready".equalsIgnoreCase(m.optString("state"))){host.showToast("أكمل دورة MQ التلقائية قبل اعتماد القياس");return;}final String ts=isoNow(),payload=d.toString(),analytics=AnalyticsSnapshot.toJson(d);host.io().execute(()->{try{long id=host.database().save(ts,payload,"دورة قياس MQ تلقائية بواسطة ESP32",analytics);host.showToast("تم اعتماد القياس رقم "+id);host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر حفظ القياس");}});}

    public void refresh(){JSONObject d=host.latestDiagnostics();if(d==null){cycle.setText("الحالة: الروبوت غير متصل");envInfo.setText("لا يوجد اتصال بالروبوت");for(int i=0;i<mqRows.length;i++)mqRows[i].setText(RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+"   لم تُرسل");return;}refreshEnv(d);JSONObject m=d.optJSONObject("measurement");if(m!=null){String s=m.optString("state","unknown");int g=m.optInt("group",0);double rem=m.optDouble("remaining",Double.NaN);String x="دورة MQ: "+stateAr(s)+(g>0?" • الصف "+g:"")+(Double.isFinite(rem)&&rem>0?String.format(Locale.US," • %.1f ثانية",rem):"")+" • العينات "+m.optInt("samples1",0)+"/"+m.optInt("samples2",0)+"/"+m.optInt("samples3",0);cycle.setText(x);cycle.setTextColor("ready".equalsIgnoreCase(s)?0xFF81C784:Color.WHITE);}JSONObject source=d.optJSONObject("mq");if(m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null)source=m.optJSONObject("mq");for(int i=0;i<mqRows.length;i++){String key=RobotTelemetry.MQ_KEYS[i];JSONObject r=source==null?null:source.optJSONObject(key);if(r==null){mqRows[i].setText(key.toUpperCase(Locale.US)+"   غير موجود في خريطة الاستحواذ الحالية");mqRows[i].setTextColor(0xFF78909C);continue;}StringBuilder s=new StringBuilder(key.toUpperCase(Locale.US));if(r.has("status"))s.append("  ").append(r.optString("status"));if(r.has("valid")&&!r.optBoolean("valid"))s.append("  غير صالح");if(!r.isNull("raw"))s.append("  raw ").append(r.optInt("raw"));add(s,r,"adc","Vadc");add(s,r,"ao","Vao");add(s,r,"rs","Rs");add(s,r,"ratio","Rs/R0");mqRows[i].setText(s.toString());mqRows[i].setTextColor(Color.WHITE);}}

    private void refreshEnv(JSONObject d){JSONObject e=d.optJSONObject("env");JSONObject legacy=d.optJSONObject("bme");if(e==null){if(legacy!=null&&legacy.optBoolean("ok",false)){envInfo.setText(String.format(Locale.US,"BME280 • الحرارة %.1f °C • الرطوبة %.1f %%RH • الضغط %.1f hPa",legacy.optDouble("temperature"),legacy.optDouble("humidity"),legacy.optDouble("pressure")));envInfo.setTextColor(0xFF81C784);}else{envInfo.setText("لم يكتشف Firmware الحالي الحساس البيئي. لا يحتاج هذا الحساس إلى START.");envInfo.setTextColor(0xFFFFB74D);}return;}boolean found=e.optBoolean("i2c_found",false),ok=e.optBoolean("ok",false);String type=e.optString("type","NONE"),addr=e.isNull("address")?"—":e.optString("address"),chip=e.isNull("chip_id")?"—":e.optString("chip_id");if(!found){envInfo.setText("I²C: لم يُعثر على جهاز عند 0x76/0x77 • تحقق من SDA=D21 وSCL=D22 والتغذية");envInfo.setTextColor(0xFFFFB74D);return;}if(!ok){envInfo.setText("I²C: جهاز موجود عند "+addr+" • Chip ID "+chip+" • النوع "+type+" غير مهيأ");envInfo.setTextColor(0xFFFFB74D);return;}String hum=e.optBoolean("humidity_available",true)&&Double.isFinite(e.optDouble("humidity",Double.NaN))?String.format(Locale.US,"%.1f %%RH",e.optDouble("humidity")):"غير متاحة";envInfo.setText(String.format(Locale.US,"%s • %s • Chip %s\nالحرارة %.1f °C • الرطوبة %s • الضغط %.1f hPa",type,addr,chip,e.optDouble("temperature"),hum,e.optDouble("pressure")));envInfo.setTextColor(0xFF81C784);}
    private static String stateAr(String s){if("idle".equals(s))return"خامل";if("ready".equals(s))return"جاهز للاعتماد";if("error".equals(s))return"خطأ";if(s.startsWith("warm"))return"تسخين";if(s.startsWith("measure"))return"قياس";return s;}
    private static void add(StringBuilder s,JSONObject r,String k,String label){double v=r.optDouble(k,Double.NaN);if(Double.isFinite(v))s.append(String.format(Locale.US,"  %s %.3f",label,v));}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
