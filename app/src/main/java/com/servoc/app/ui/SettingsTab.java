package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.models.MqDataScienceModelProfile;
import org.json.JSONObject;
import java.util.Locale;

public final class SettingsTab {
    private final HazRobHost host;
    private final ScrollView scroll;
    private final EditText robot,camera,speed,nl,nr,warm,measure;
    private final TextView compatibility,cameraState,libraryInfo,mqPowerState;

    public SettingsTab(Context c,HazRobHost host){
        this.host=host;ManualR0Calibration.load(host.preferences());scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"الإعدادات • HazRob 0.8"),UiKit.full(c));

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"الشبكة والاتصال"),UiKit.full(c));
        robot=UiKit.input(c,host.preferences().getString("robot","192.168.4.1"));camera=UiKit.input(c,host.preferences().getString("camera","192.168.4.2"));
        net.addView(UiKit.small(c,"عنوان ESP32 الرئيسي"),UiKit.full(c));net.addView(robot,UiKit.full(c));
        net.addView(UiKit.small(c,"عنوان ESP32-CAM"),UiKit.full(c));net.addView(camera,UiKit.full(c));
        Button saveNet=UiKit.button(c,"حفظ وتطبيق إعدادات الشبكة");saveNet.setOnClickListener(v->saveNetwork());net.addView(saveNet,UiKit.full(c));
        Button testNet=UiKit.button(c,"اختبار الروبوت والكاميرا");testNet.setOnClickListener(v->testConnections());net.addView(testNet,UiKit.full(c));
        cameraState=UiKit.small(c,"تشخيص الكاميرا: لم يُختبر بعد");cameraState.setBackgroundColor(0xFF151A20);net.addView(cameraState,UiKit.full(c));
        net.addView(UiKit.small(c,"الشبكة الرسمية: ESP32 الرئيسي 192.168.4.1 • ESP-CAM 192.168.4.2 • التطبيق يختبر HTTP 80 والبث MJPEG على 81."),UiKit.full(c));
        root.addView(net,UiKit.full(c));

        LinearLayout power=UiKit.card(c);power.addView(UiKit.title(c,"طاقة مجموعات MQ • تحكم يدوي"),UiKit.full(c));
        power.addView(UiKit.small(c,"R1 → ADS1 0x48: MQ2 / MQ4 / MQ5 / MQ8\nR2 → ADS2 0x49: MQ3 / MQ7 / MQ9 / MQ135\nR3 وR4 احتياط. وضع التتبع لا يشغل أي Relay تلقائيًا في 0.8."),UiKit.full(c));
        LinearLayout p1=UiKit.hbox(c);
        Button off=UiKit.button(c,"MQ OFF");off.setOnClickListener(v->setMqPower(0));p1.addView(off,UiKit.weight(1));
        Button r1=UiKit.button(c,"R1 فقط");r1.setOnClickListener(v->setMqPower(1));p1.addView(r1,UiKit.weight(1));
        Button r2=UiKit.button(c,"R2 فقط");r2.setOnClickListener(v->setMqPower(2));p1.addView(r2,UiKit.weight(1));
        Button both=UiKit.button(c,"R1 + R2");both.setOnClickListener(v->setMqPower(3));p1.addView(both,UiKit.weight(1));
        power.addView(p1,UiKit.full(c));
        mqPowerState=UiKit.small(c,"حالة MQ: بانتظار بيانات ESP32");mqPowerState.setBackgroundColor(0xFF151A20);power.addView(mqPowerState,UiKit.full(c));
        power.addView(UiKit.small(c,"تشغيل R1+R2 يعني تشغيل سخانات الحساسات الثمانية معًا. الاختيار يدوي ومسؤولية قرار الطاقة تبقى للمشغل إلى أن يكتمل اختبار خط 5V تحت الحمل."),UiKit.full(c));
        root.addView(power,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"إعدادات ESP32 والتسخين"),UiKit.full(c));
        speed=field(c,cfg,"حد السرعة % (20–100)",String.valueOf(host.preferences().getInt("speed",100)));
        nl=field(c,cfg,"Neutral للمحرك الأيسر µs (1400–1600)",String.valueOf(host.preferences().getInt("nl",1500)));
        nr=field(c,cfg,"Neutral للمحرك الأيمن µs (1400–1600)",String.valueOf(host.preferences().getInt("nr",1500)));
        warm=field(c,cfg,"زمن التسخين الذي يحدده المشغل بالثواني (0–3600)",String.valueOf(host.preferences().getInt("warm",60)));
        measure=field(c,cfg,"زمن أخذ القياس بالثواني (5–600)",String.valueOf(host.preferences().getInt("measure",30)));
        cfg.addView(UiKit.small(c,"في 0.8 زمن التسخين قيمة يسجلها ويقررها المشغل؛ Firmware لا ينتظرها ولا يبدل الريلايات تلقائيًا. اضغط بدء averaging فقط بعد اكتمال التسخين الذي قررته. هذه القيمة لا تثبت heater-cycle الخاص بـMQ7/MQ9."),UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"مزامنة من ESP32");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"تطبيق على ESP32");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout compat=UiKit.card(c);compat.addView(UiKit.title(c,"حالة التوافق والعتاد"),UiKit.full(c));compatibility=UiKit.text(c,"Firmware: غير معروف بعد");compat.addView(compatibility,UiKit.full(c));
        compat.addView(UiKit.small(c,"Hardware profile: "+HazRobHardwareProfile.ID+"\nADS1115×2 • مجموعتا MQ فعليتان • 4 حساسات لكل مجموعة • IR تحذير فقط • MAX4466 جهد خام • R3/R4 احتياط."),UiKit.full(c));root.addView(compat,UiKit.full(c));

        LinearLayout science=UiKit.card(c);science.addView(UiKit.title(c,"المكتبة العلمية ونماذج MQDataScience"),UiKit.full(c));
        science.addView(UiKit.small(c,"تم اعتماد كتالوج MQDataScience ذي 76 نموذجًا كاملًا ضمن HazRob 0.8. الاعتماد منفصل عن حالة النقل: Verified = port مطابق للمصدر المثبت، Approx = تنفيذ Android عامل يحتاج استكمال المطابقة، Pending = النموذج معتمد ولم يُنقل بعد."),UiKit.full(c));
        Button info=UiKit.button(c,"عرض حالة الـ76 نموذجًا والمكتبة");science.addView(info,UiKit.full(c));libraryInfo=UiKit.small(c,"");libraryInfo.setVisibility(View.GONE);science.addView(libraryInfo,UiKit.full(c));
        info.setOnClickListener(v->{
            boolean show=libraryInfo.getVisibility()!=View.VISIBLE;
            libraryInfo.setVisibility(show?View.VISIBLE:View.GONE);
            if(show)libraryInfo.setText(
                    "قاعدة المعرفة المحلية: "+host.database().chemicalCount()+" سجلًا\n"+
                    "MQDataScience: "+MqDataScience76Catalog.COUNT+" / "+MqDataScience76Catalog.COUNT+" ADOPTED • v"+MqDataScience76Catalog.VERSION+"\n"+
                    "Source commit: "+MqDataScience76Catalog.PINNED_COMMIT+"\n"+
                    "Android Native: "+ModelRuntimeRegistry.nativeCount()+" / "+MqDataScience76Catalog.COUNT+"\n"+
                    "Upstream-verified: "+ModelRuntimeRegistry.verifiedCount()+"\n"+
                    "Android approximation: "+ModelRuntimeRegistry.approximationCount()+"\n"+
                    "Port pending: "+ModelRuntimeRegistry.pendingPortCount()+"\n"+
                    "Domains: multivariable "+MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.MULTIVARIATE_ENV)+
                    " • 1-D curve "+MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.CURVE_1D)+
                    " • time-series "+MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.TIME_SERIES)+
                    " • ML "+MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.MACHINE_LEARNING)+"\n"+
                    "مختبر المطور يعرض حالة كل نموذج ومتطلبات بياناته ويقارن فقط النماذج الملائمة للـDataset المتاح.\n"+
                    "PPM الكمي يتطلب معايرة مرجعية صالحة؛ R0 وحده ليس معايرة تركيز.");
        });
        root.addView(science,UiKit.full(c));
    }

    private EditText field(Context c,LinearLayout p,String label,String value){p.addView(UiKit.small(c,label),UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    public void refresh(){
        JSONObject d=host.latestDiagnostics();
        if(d==null){compatibility.setText("Firmware: لا يوجد اتصال بالروبوت");mqPowerState.setText("حالة MQ: غير متصل");mqPowerState.setTextColor(0xFFFFB74D);return;}
        JSONObject adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay");int mask=relay==null?0:relay.optInt("mask",0)&3;
        compatibility.setText("Firmware: "+d.optString("firmware","غير معرّف")+" • API "+d.optInt("api_version",0)+" • Heap "+d.optLong("heap",0)+"\nProfile "+d.optString("hardware_profile",HazRobHardwareProfile.ID)+" • ADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"OK":"—")+" • ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"OK":"—")+" • MQ groups 2 • relay mask "+mask);
        mqPowerState.setText("حالة MQ: "+HazRobHardwareProfile.groupLabel(mask)+" • mask "+mask+" • تحكم يدوي");mqPowerState.setTextColor(mask==0?Color.LTGRAY:0xFF81C784);
    }

    private void setMqPower(int mask){host.io().execute(()->{try{host.robotApi().relayMask(mask);host.showToast("MQ: "+HazRobHardwareProfile.groupLabel(mask));host.refreshAllTabs();}catch(Exception e){host.showToast("تعذر تطبيق طاقة MQ");}});}
    private void saveNetwork(){host.emergencyStop();String rh=robot.getText().toString().trim(),ch=camera.getText().toString().trim();host.robotApi().setRobotHost(rh);host.setCameraHost(ch);camera.setText(host.cameraHost());host.preferences().edit().putString("robot",host.robotApi().getRobotHost()).putString("camera",host.cameraHost()).apply();host.showToast("تم تطبيق الشبكة وإيقاف الحركة احترازيًا");host.refreshAllTabs();}

    private void testConnections(){
        host.io().execute(()->{
            boolean robotOk=false;try{robotOk=host.robotApi().diagnostics()!=null;}catch(Exception ignored){}
            JSONObject cam=null;try{cam=host.cameraDiagnostics();}catch(Exception ignored){}
            boolean cameraOk=cam!=null;if(!cameraOk)try{cameraOk=host.cameraReachable();}catch(Exception ignored){}
            final boolean rOk=robotOk,cOk=cameraOk;final JSONObject cdiag=cam;final String ch=host.cameraHost();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                if(cdiag!=null){
                    boolean sensorOk=cdiag.optBoolean("camera_ok",cdiag.optBoolean("ok",true));
                    String fw=cdiag.optString("fw","ESP32-CAM"),ssid=cdiag.optString("ssid","—"),ip=cdiag.optString("ip",ch),sensor=cdiag.optString("sensor","—");
                    String pid=cdiag.has("pid")?String.format(Locale.US,"0x%04X",cdiag.optInt("pid",0)):"—";
                    String fps=cdiag.has("fps")?String.format(Locale.US,"%.2f",cdiag.optDouble("fps",0)):"—";
                    String enc=cdiag.has("encode_ms")?String.format(Locale.US,"%.1f ms",cdiag.optDouble("encode_ms",0)):"—";
                    String jpg=cdiag.has("jpeg_bytes")?cdiag.optLong("jpeg_bytes",0)+" B":"—";
                    String rssi=cdiag.has("rssi")?cdiag.optInt("rssi",0)+" dBm":"—";
                    cameraState.setText("تشخيص الكاميرا: "+fw+" • "+(sensorOk?"sensor OK":"sensor FAIL")+" • "+sensor+" / "+pid+"\nSSID "+ssid+" • IP "+ip+" • RSSI "+rssi+"\nCamera FPS "+fps+" • JPEG "+enc+" • "+jpg+" • capErr "+cdiag.optLong("capture_errors",0)+" • encErr "+cdiag.optLong("encode_errors",0));
                    cameraState.setTextColor(sensorOk?0xFF81C784:0xFFFF7043);
                }else{
                    cameraState.setText("تشخيص الكاميرا: "+(cOk?"استجابة HTTP قديمة بدون /status":"لا استجابة من "+ch+" على المنافذ المتوقعة"));
                    cameraState.setTextColor(cOk?0xFFFFB74D:0xFFFF7043);
                }
                host.showToast("الروبوت "+(rOk?"متصل":"فشل")+" • الكاميرا "+(cOk?"متصلة":"فشل"));
            });
        });
    }
    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));}catch(Exception e){host.showToast("تعذر قراءة إعدادات ESP32");}});}
    private void apply(){host.emergencyStop();final int s=clamp(num(speed,100),20,100),l=clamp(num(nl,1500),1400,1600),r=clamp(num(nr,1500),1400,1600),w=clamp(num(warm,60),0,3600),m=clamp(num(measure,30),5,600);host.setCameraHost(camera.getText().toString().trim());final String cam=host.cameraHost();camera.setText(cam);host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,s,l,r,w,m);host.preferences().edit().putInt("speed",s).putInt("nl",l).putInt("nr",r).putInt("warm",w).putInt("measure",m).apply();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));host.showToast("تم تطبيق إعدادات ESP32");}catch(Exception e){host.showToast("فشل تطبيق إعدادات ESP32");}});}
    private void showConfig(JSONObject x){if(x==null)return;camera.setText(x.optString("cam",camera.getText().toString()));speed.setText(String.valueOf(x.optInt("speed",100)));nl.setText(String.valueOf(x.optInt("nl",1500)));nr.setText(String.valueOf(x.optInt("nr",1500)));warm.setText(String.valueOf(x.optInt("warm",60)));measure.setText(String.valueOf(x.optInt("measure",30)));}
    private int num(EditText e,int f){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return f;}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
