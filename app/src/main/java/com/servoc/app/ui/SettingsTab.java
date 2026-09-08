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
import org.json.JSONObject;
import java.util.Locale;

/** Compact operator settings shared by Direct and Remote modes. */
public final class SettingsTab {
    private static final int DRIVE_FIRMWARE_LIMIT=100;
    private final HazRobHost host;private final ScrollView scroll;
    private final EditText robot,camera,warm,measure;
    private final TextView compatibility,cameraState,libraryInfo;

    public SettingsTab(Context c,HazRobHost host){
        this.host=host;ManualR0Calibration.load(host.preferences());scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"الشبكة"),UiKit.full(c));
        robot=field(c,net,"ESP32",host.preferences().getString("robot","192.168.4.1"));camera=field(c,net,"ESP32-CAM",host.preferences().getString("camera","192.168.4.2"));
        LinearLayout netButtons=UiKit.hbox(c);Button saveNet=UiKit.button(c,"حفظ");saveNet.setOnClickListener(v->saveNetwork());netButtons.addView(saveNet,UiKit.weight(1));Button testNet=UiKit.button(c,"اختبار");testNet.setOnClickListener(v->testConnections());netButtons.addView(testNet,UiKit.weight(1));net.addView(netButtons,UiKit.full(c));
        cameraState=UiKit.small(c,"CAM —");cameraState.setBackgroundColor(0xFF151A20);net.addView(cameraState,UiKit.full(c));root.addView(net,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"الضبط"),UiKit.full(c));
        warm=field(c,cfg,"التسخين s",String.valueOf(host.preferences().getInt("warm",60)));measure=field(c,cfg,"القياس s",String.valueOf(host.preferences().getInt("measure",30)));
        cfg.addView(UiKit.small(c,"سرعة حركة المركبة تضبط الآن من عمود السرعة في شاشة القيادة: 0–100%، والقيمة الافتراضية عند بدء التطبيق 50%."),UiKit.full(c));
        cfg.addView(UiKit.small(c,"منظومة الحركة: L298N • يسار GPIO18/27 • يمين GPIO19/32 • ENA/ENB مفعّلان على اللوحة. لا توجد إعدادات Neutral Servo في هذا الإصدار."),UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"مزامنة");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"تطبيق");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout compat=UiKit.card(c);compat.addView(UiKit.title(c,"النظام"),UiKit.full(c));compatibility=UiKit.text(c,"Firmware —");compat.addView(compatibility,UiKit.full(c));Button alertTest=UiKit.button(c,"اختبار تنبيه الروبوت");alertTest.setOnClickListener(v->testAlert());compat.addView(alertTest,UiKit.full(c));root.addView(compat,UiKit.full(c));

        LinearLayout science=UiKit.card(c);science.addView(UiKit.title(c,"المكتبة العلمية"),UiKit.full(c));Button info=UiKit.button(c,"الحالة");science.addView(info,UiKit.full(c));libraryInfo=UiKit.small(c,"");libraryInfo.setBackgroundColor(0xFF151A20);libraryInfo.setVisibility(View.GONE);science.addView(libraryInfo,UiKit.full(c));
        info.setOnClickListener(v->{boolean show=libraryInfo.getVisibility()!=View.VISIBLE;libraryInfo.setVisibility(show?View.VISIBLE:View.GONE);if(show)libraryInfo.setText("Chemicals "+host.database().chemicalCount()+"   •   MQDataScience "+MqDataScience76Catalog.COUNT+"   •   Native "+ModelRuntimeRegistry.nativeCount()+"   •   Verified "+ModelRuntimeRegistry.verifiedCount()+"   •   Pending "+ModelRuntimeRegistry.pendingPortCount());});root.addView(science,UiKit.full(c));
    }

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    public void refresh(){
        JSONObject d=host.latestDiagnostics();if(d==null){compatibility.setText("Firmware —   •   API —   •   Profile —");compatibility.setTextColor(0xFFFFB74D);return;}
        JSONObject adc=d.optJSONObject("adc"),ldr=d.optJSONObject("ldr"),alert=d.optJSONObject("alert"),drive=d.optJSONObject("drive");
        String driver=drive==null?HazRobHardwareProfile.DRIVE_DRIVER:drive.optString("driver",HazRobHardwareProfile.DRIVE_DRIVER);
        compatibility.setText("Firmware "+d.optString("firmware","—")+"   •   API "+d.optInt("api_version",0)+"\nProfile "+d.optString("hardware_profile",HazRobHardwareProfile.ID)+"\nDrive "+driver+" • L "+HazRobHardwareProfile.DRIVE_L_IN1+"/"+HazRobHardwareProfile.DRIVE_L_IN2+" • R "+HazRobHardwareProfile.DRIVE_R_IN1+"/"+HazRobHardwareProfile.DRIVE_R_IN2+"\nADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"OK":"—")+"   •   ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"OK":"—")+"\nLDR GPIO34 "+(ldr!=null&&ldr.optBoolean("installed",false)?"OK":"—")+"   •   ALERT GPIO"+(alert==null?26:alert.optInt("gpio",26))+" "+(alert!=null?"OK":"—"));compatibility.setTextColor(0xFF81C784);
    }

    private void saveNetwork(){host.setDriveCommand(0,0);String rh=robot.getText().toString().trim(),ch=camera.getText().toString().trim();host.robotApi().setRobotHost(rh);host.setCameraHost(ch);camera.setText(host.cameraHost());robot.setText(host.robotApi().getRobotHost());host.preferences().edit().putString("robot",host.robotApi().getRobotHost()).putString("camera",host.cameraHost()).apply();host.showToast("تم حفظ الشبكة");host.refreshAllTabs();}

    private void testConnections(){host.io().execute(()->{boolean robotOk=false;try{robotOk=host.robotApi().diagnostics()!=null;}catch(Exception ignored){}JSONObject cam=null;try{cam=host.cameraDiagnostics();}catch(Exception ignored){}boolean cameraOk=cam!=null;if(!cameraOk)try{cameraOk=host.cameraReachable();}catch(Exception ignored){}final boolean rOk=robotOk,cOk=cameraOk;final JSONObject cdiag=cam;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(cdiag!=null){String fps=cdiag.has("fps")?String.format(Locale.US,"%.2f",cdiag.optDouble("fps",0)):"—";String rssi=cdiag.has("rssi")?cdiag.optInt("rssi",0)+" dBm":"—";cameraState.setText("CAM "+(cdiag.optBoolean("camera_ok",cdiag.optBoolean("ok",true))?"OK":"FAIL")+"   •   FPS "+fps+"   •   RSSI "+rssi);cameraState.setTextColor(cOk?0xFF81C784:0xFFFF7043);}else{cameraState.setText("CAM "+(cOk?"HTTP OK":"OFFLINE"));cameraState.setTextColor(cOk?0xFF81C784:0xFFFF7043);}host.showToast("ESP32 "+(rOk?"OK":"OFFLINE")+" • CAM "+(cOk?"OK":"OFFLINE"));});});}
    private void testAlert(){host.io().execute(()->{try{host.robotApi().alertTest();host.showToast("تم إرسال اختبار التنبيه");}catch(Exception e){host.showToast("تعذر اختبار التنبيه");}});}
    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));}catch(Exception e){host.showToast("تعذر المزامنة");}});}
    private void apply(){final int w=clamp(parse(warm,60),0,3600),m=clamp(parse(measure,30),5,600);final String cam=host.cameraHost();host.setDriveCommand(0,0);host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,DRIVE_FIRMWARE_LIMIT,w,m);host.preferences().edit().remove("speed").putInt("warm",w).putInt("measure",m).remove("nl").remove("nr").apply();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));host.showToast("تم تطبيق الضبط");}catch(Exception e){host.showToast("فشل تطبيق الضبط");}});}
    private void showConfig(JSONObject x){if(x==null)return;String cam=x.optString("cam",camera.getText().toString());camera.setText(cam);host.setCameraHost(cam);warm.setText(String.valueOf(x.optInt("warm",parse(warm,60))));measure.setText(String.valueOf(x.optInt("measure",parse(measure,30))));}
    private static int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception ignored){return def;}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
