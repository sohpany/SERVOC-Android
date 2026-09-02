package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import org.json.JSONObject;
import java.util.Locale;

public final class SettingsTab {
    private final HazRobHost host;
    private final ScrollView scroll;
    private final EditText robot,camera,speed,nl,nr,warm,measure;
    private final TextView compatibility,cameraState,libraryInfo;

    public SettingsTab(Context c,HazRobHost host){
        this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"الإعدادات"),UiKit.full(c));

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"الشبكة والاتصال"),UiKit.full(c));
        robot=UiKit.input(c,host.preferences().getString("robot","192.168.4.1"));camera=UiKit.input(c,host.preferences().getString("camera","192.168.4.2"));
        net.addView(UiKit.small(c,"عنوان ESP32 الرئيسي"),UiKit.full(c));net.addView(robot,UiKit.full(c));
        net.addView(UiKit.small(c,"عنوان ESP32-CAM"),UiKit.full(c));net.addView(camera,UiKit.full(c));
        Button saveNet=UiKit.button(c,"حفظ وتطبيق إعدادات الشبكة");saveNet.setOnClickListener(v->saveNetwork());net.addView(saveNet,UiKit.full(c));
        Button testNet=UiKit.button(c,"اختبار الروبوت والكاميرا");testNet.setOnClickListener(v->testConnections());net.addView(testNet,UiKit.full(c));
        cameraState=UiKit.small(c,"تشخيص الكاميرا: لم يُختبر بعد");cameraState.setBackgroundColor(0xFF151A20);net.addView(cameraState,UiKit.full(c));
        net.addView(UiKit.small(c,"HazRob 0.7.1 يختبر تحكم الكاميرا على HTTP 80 والبث على 81 ويجرب عدة مسارات MJPEG. إذا نجح الروبوت وفشلت الكاميرا فغالبًا ESP-CAM لم تنضم إلى HazRob/Servoc أو ما زالت على Firmware قديم."),UiKit.full(c));
        root.addView(net,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"إعدادات ESP32"),UiKit.full(c));
        speed=field(c,cfg,"حد السرعة % (20–100)",String.valueOf(host.preferences().getInt("speed",100)));
        nl=field(c,cfg,"Neutral للمحرك الأيسر µs (1400–1600)",String.valueOf(host.preferences().getInt("nl",1500)));
        nr=field(c,cfg,"Neutral للمحرك الأيمن µs (1400–1600)",String.valueOf(host.preferences().getInt("nr",1500)));
        warm=field(c,cfg,"زمن تسخين MQ بالثواني (0–3600)",String.valueOf(host.preferences().getInt("warm",60)));
        measure=field(c,cfg,"زمن القياس بالثواني (5–600)",String.valueOf(host.preferences().getInt("measure",30)));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"مزامنة من ESP32");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"تطبيق على ESP32");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout compat=UiKit.card(c);compat.addView(UiKit.title(c,"حالة التوافق"),UiKit.full(c));compatibility=UiKit.text(c,"Firmware: غير معروف بعد");compat.addView(compatibility,UiKit.full(c));compat.addView(UiKit.small(c,"HazRob 0.7.1 يستخدم API v3 للروبوت مع إضافات relayMask/ADS1115/IR/sound/retrace، بينما الفيديو والفلاش يتصلان مباشرة بـESP32-CAM على 192.168.4.2."),UiKit.full(c));root.addView(compat,UiKit.full(c));

        LinearLayout science=UiKit.card(c);science.addView(UiKit.title(c,"المكتبة العلمية"),UiKit.full(c));science.addView(UiKit.small(c,"قاعدة المعرفة قابلة للتوسعة، بينما التغطية التحليلية تبقى مرتبطة بالدليل والمعايرة لكل حساس/مركب."),UiKit.full(c));Button info=UiKit.button(c,"عرض ملخص المكتبة العلمية");science.addView(info,UiKit.full(c));libraryInfo=UiKit.small(c,"");libraryInfo.setVisibility(View.GONE);science.addView(libraryInfo,UiKit.full(c));info.setOnClickListener(v->{boolean show=libraryInfo.getVisibility()!=View.VISIBLE;libraryInfo.setVisibility(show?View.VISIBLE:View.GONE);if(show)libraryInfo.setText("قاعدة المعرفة المحلية: "+host.database().chemicalCount()+" سجلًا\nMQDataScience: "+MqDataScience76Catalog.COUNT+" نموذجًا مسجلًا (v"+MqDataScience76Catalog.VERSION+")\nالنماذج Android Native حاليًا: "+ModelRuntimeRegistry.nativeCount()+"\nPPM الكمي يتطلب معايرة مرجعية صالحة؛ R0 الافتراضي لا يُعد معايرة.");});root.addView(science,UiKit.full(c));
    }

    private EditText field(Context c,LinearLayout p,String label,String value){p.addView(UiKit.small(c,label),UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    public void refresh(){JSONObject d=host.latestDiagnostics();if(d==null){compatibility.setText("Firmware: لا يوجد اتصال بالروبوت");return;}JSONObject adc=d.optJSONObject("adc");compatibility.setText("Firmware: "+d.optString("firmware","غير معرّف")+" • API "+d.optInt("api_version",0)+" • Heap "+d.optLong("heap",0)+"\nADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"OK":"—")+" • ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"OK":"—"));}

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
                    String fw=cdiag.optString("fw","ESP32-CAM"),ssid=cdiag.optString("ssid","—"),ip=cdiag.optString("ip",ch),pid=cdiag.has("pid")?String.format(Locale.US,"0x%04X",cdiag.optInt("pid",0)):"—";
                    String fps=cdiag.has("fps")?String.format(Locale.US,"%.2f",cdiag.optDouble("fps",0)):"—";
                    String err=cdiag.has("camera_error")?" • err "+cdiag.optString("camera_error",""):"";
                    cameraState.setText("تشخيص الكاميرا: "+fw+" • "+(sensorOk?"sensor OK":"sensor FAIL")+"\nSSID "+ssid+" • IP "+ip+" • PID "+pid+" • FPS "+fps+err);
                    cameraState.setTextColor(sensorOk?0xFF81C784:0xFFFF7043);
                }else{
                    cameraState.setText("تشخيص الكاميرا: "+(cOk?"استجابة HTTP قديمة بدون /status":"لا استجابة من "+ch+" على المنافذ المتوقعة"));cameraState.setTextColor(cOk?0xFFFFB74D:0xFFFF7043);
                }
                host.showToast("الروبوت "+(rOk?"متصل":"فشل")+" • الكاميرا "+(cOk?"متصلة":"فشل"));
            });
        });
    }

    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));}catch(Exception e){host.showToast("تعذر قراءة إعدادات ESP32");}});}

    private void apply(){host.emergencyStop();final int s=clamp(num(speed,100),20,100),l=clamp(num(nl,1500),1400,1600),r=clamp(num(nr,1500),1400,1600),w=clamp(num(warm,60),0,3600),m=clamp(num(measure,30),5,600);host.setCameraHost(camera.getText().toString().trim());final String cam=host.cameraHost();camera.setText(cam);host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,s,l,r,w,m);host.preferences().edit().putInt("speed",s).putInt("nl",l).putInt("nr",r).putInt("warm",w).putInt("measure",m).apply();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));host.showToast("تم تطبيق إعدادات ESP32 وإيقاف الحركة احترازيًا");}catch(Exception e){host.showToast("فشل تطبيق إعدادات ESP32");}});}

    private void showConfig(JSONObject x){if(x==null)return;camera.setText(x.optString("cam",camera.getText().toString()));speed.setText(String.valueOf(x.optInt("speed",100)));nl.setText(String.valueOf(x.optInt("nl",1500)));nr.setText(String.valueOf(x.optInt("nr",1500)));warm.setText(String.valueOf(x.optInt("warm",60)));measure.setText(String.valueOf(x.optInt("measure",30)));}
    private int num(EditText e,int f){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return f;}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
