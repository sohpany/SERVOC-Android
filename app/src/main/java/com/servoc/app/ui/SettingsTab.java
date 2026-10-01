package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.ReleaseAudit;
import org.json.JSONObject;
import java.util.Locale;

/** Compact operator settings shared by Direct and Remote modes. */
public final class SettingsTab {
    private static final int DRIVE_FIRMWARE_LIMIT=100;
    private final HazRobHost host;private final ScrollView scroll;
    private final EditText robot,camera,warm,measure;
    private final CheckBox safetyAutoStop;
    private final TextView compatibility,cameraState,libraryInfo,analysisUpdateInfo;
    private final ChemCoreLibraryManager libraryManager;

    public SettingsTab(Context c,HazRobHost host){
        this.host=host;this.libraryManager=new ChemCoreLibraryManager(c);ManualR0Calibration.load(host.preferences());scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"الشبكة"),UiKit.full(c));
        robot=field(c,net,"ESP32",host.preferences().getString("robot","192.168.4.1"));camera=field(c,net,"ESP32-CAM",host.preferences().getString("camera","192.168.4.2"));
        LinearLayout netButtons=UiKit.hbox(c);Button saveNet=UiKit.button(c,"حفظ");saveNet.setOnClickListener(v->saveNetwork());netButtons.addView(saveNet,UiKit.weight(1));Button testNet=UiKit.button(c,"اختبار");testNet.setOnClickListener(v->testConnections());netButtons.addView(testNet,UiKit.weight(1));net.addView(netButtons,UiKit.full(c));
        cameraState=UiKit.small(c,"CAM —");cameraState.setBackgroundColor(0xFF151A20);net.addView(cameraState,UiKit.full(c));root.addView(net,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"الضبط"),UiKit.full(c));
        warm=field(c,cfg,"التسخين s",String.valueOf(host.preferences().getInt("warm",60)));measure=field(c,cfg,"القياس s",String.valueOf(host.preferences().getInt("measure",30)));
        cfg.addView(UiKit.small(c,"سرعة حركة المركبة تضبط من عمود السرعة في شاشة القيادة: 0–100%، والقيمة الافتراضية عند بدء التطبيق 100%."),UiKit.full(c));
        cfg.addView(UiKit.small(c,"منظومة الحركة: L298N • LEFT=B: EN17/IN19/32 • RIGHT=A: EN16/IN18/27 • تزال جمبرا ENA/ENB عند تحكم PWM من ESP32."),UiKit.full(c));
        safetyAutoStop=new CheckBox(c);safetyAutoStop.setText("التوقف التلقائي للحركة: ملغي — تنبيه فقط");safetyAutoStop.setTextColor(Color.LTGRAY);safetyAutoStop.setChecked(false);safetyAutoStop.setEnabled(false);cfg.addView(safetyAutoStop,UiKit.full(c));
        TextView safetyHelp=UiKit.small(c,"حساسا IR وEcho يعرضان التحذيرات فقط ولا يمنعان حركة المركبة. E-STOP وزر الإيقاف وwatchdog انقطاع أوامر القيادة تبقى فعالة ومستقلة.");safetyHelp.setTextColor(0xFFFFB74D);cfg.addView(safetyHelp,UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"مزامنة");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"تطبيق");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout compat=UiKit.card(c);compat.addView(UiKit.title(c,"النظام والتنبيهات"),UiKit.full(c));compatibility=UiKit.text(c,"Firmware —");compat.addView(compatibility,UiKit.full(c));
        LinearLayout alertButtons=UiKit.hbox(c);Button robotAlert=UiKit.button(c,"اختبار تنبيه الروبوت");robotAlert.setOnClickListener(v->testAlert());alertButtons.addView(robotAlert,UiKit.weight(1));Button phoneAlert=UiKit.button(c,"اختبار صوت الهاتف");phoneAlert.setOnClickListener(v->host.testPhoneAlert());alertButtons.addView(phoneAlert,UiKit.weight(1));compat.addView(alertButtons,UiKit.full(c));root.addView(compat,UiKit.full(c));

        LinearLayout science=UiKit.card(c);science.addView(UiKit.title(c,"المكتبة العلمية"),UiKit.full(c));Button info=UiKit.button(c,"الحالة");science.addView(info,UiKit.full(c));libraryInfo=UiKit.small(c,"");libraryInfo.setBackgroundColor(0xFF151A20);libraryInfo.setVisibility(View.GONE);science.addView(libraryInfo,UiKit.full(c));
        info.setOnClickListener(v->{boolean show=libraryInfo.getVisibility()!=View.VISIBLE;libraryInfo.setVisibility(show?View.VISIBLE:View.GONE);if(show)libraryInfo.setText("Chemicals "+host.database().chemicalCount()+"   •   MQDataScience "+MqDataScience76Catalog.COUNT+"   •   Native "+ModelRuntimeRegistry.nativeCount()+"   •   Verified "+ModelRuntimeRegistry.verifiedCount()+"   •   Pending "+ModelRuntimeRegistry.pendingPortCount());});root.addView(science,UiKit.full(c));

        LinearLayout updates=UiKit.card(c);updates.addView(UiKit.title(c,"تحديثات منظومة التحليل"),UiKit.full(c));analysisUpdateInfo=UiKit.small(c,"");analysisUpdateInfo.setBackgroundColor(0xFF151A20);updates.addView(analysisUpdateInfo,UiKit.full(c));Button checkUpdate=UiKit.button(c,"التحقق من تحديثات منظومة التحليل");checkUpdate.setOnClickListener(v->checkAnalysisUpdate());updates.addView(checkUpdate,UiKit.full(c));root.addView(updates,UiKit.full(c));
    }

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    public void refresh(){
        ChemCoreLibraryManager.Status ls=libraryManager.status();analysisUpdateInfo.setText("الإصدار المثبت: "+ls.version+" • "+(ls.remoteSnapshotActive?"مكتبة Stable مثبتة":"المكتبة المدمجة")+"\nمصدر المعرفة: "+ls.sourceLabel);
        JSONObject d=host.latestDiagnostics();if(d==null){compatibility.setText("Firmware —   •   API —   •   Profile —");compatibility.setTextColor(0xFFFFB74D);return;}
        JSONObject adc=d.optJSONObject("adc"),env=d.optJSONObject("env"),ldr=d.optJSONObject("ldr"),alert=d.optJSONObject("alert"),drive=d.optJSONObject("drive");
        showSafetyAutoStop(false);
        String driver=drive==null?HazRobHardwareProfile.DRIVE_DRIVER:drive.optString("driver",HazRobHardwareProfile.DRIVE_DRIVER);boolean envOk=env!=null&&env.optBoolean("ok",false);
        String envStatus=env==null?"—":env.optString("status_code",env.optString("status","NOT READY"));
        String envType=env==null?"—":env.optString("type",env.optString("sensor","—"));
        String envAddr=env==null?"—":env.optString("address","—");
        String envChip=env==null?"—":env.optString("chip_id","—");
        String envAck=env==null?"76:— 77:—":"76:"+(env.optBoolean("ack_0x76",false)?"ACK":"—")+" 77:"+(env.optBoolean("ack_0x77",false)?"ACK":"—");
        compatibility.setText("Firmware "+d.optString("firmware",d.optString("fw","—"))+"   •   API "+d.optInt("api_version",d.optInt("api",0))+"\nProfile "+d.optString("hardware_profile",HazRobHardwareProfile.ID)+"\nDrive "+driver+" • L "+HazRobHardwareProfile.DRIVE_L_IN1+"/"+HazRobHardwareProfile.DRIVE_L_IN2+" • R "+HazRobHardwareProfile.DRIVE_R_IN1+"/"+HazRobHardwareProfile.DRIVE_R_IN2+" • Auto-stop OFF\nADS bus SDA21/SCL22 • ADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"OK":"—")+" • ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"OK":"—")+"\nENV bus SDA13/SCL14 • "+(envOk?"OK":envStatus)+" • "+envType+" "+envAddr+" chip "+envChip+" • "+envAck+"\nLDR GPIO34 "+(ldr!=null&&ldr.optBoolean("installed",false)?"OK":"—")+"   •   ALERT GPIO"+(alert==null?26:alert.optInt("gpio",26))+" "+(alert!=null?"OK":"—"));compatibility.setTextColor(envOk?0xFF81C784:0xFFFFB74D);
    }

    private void checkAnalysisUpdate(){
        host.io().execute(()->{
            try{
                ChemCoreLibraryManager.CheckSummary c=libraryManager.check(ReleaseAudit.VERSION);
                if(!c.updateAvailable){host.showToast("منظومة التحليل محدثة");return;}
                if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){host.showToast("أوقف القياس أو التحليل قبل التحديث");return;}
                com.servoc.app.chemcore.library.ChemCoreLibraryStore.InstallResult r=libraryManager.install(c,ReleaseAudit.VERSION,false);
                host.showToast(r.installed?"تم تحديث منظومة التحليل إلى "+r.version:"لم يتم تثبيت التحديث: "+r.reason);
                host.refreshAllTabs();
            }catch(Exception e){host.showToast("تعذر تحديث منظومة التحليل: "+e.getClass().getSimpleName());}
        });
    }

    private void saveNetwork(){host.setDriveCommand(0,0);String rh=robot.getText().toString().trim(),ch=camera.getText().toString().trim();host.robotApi().setRobotHost(rh);host.setCameraHost(ch);camera.setText(host.cameraHost());robot.setText(host.robotApi().getRobotHost());host.preferences().edit().putString("robot",host.robotApi().getRobotHost()).putString("camera",host.cameraHost()).apply();host.showToast("تم حفظ الشبكة");host.refreshAllTabs();}

    private static String cameraDiagnosticText(JSONObject cdiag){
        boolean cameraOk=cdiag.optBoolean("camera_ok",false),wifiOk=cdiag.optBoolean("wifi_connected",true),psram=cdiag.optBoolean("psram",false);
        String err=cdiag.optString("camera_error","—"),mem=cdiag.optString("fb_memory",psram?"psram":"dram"),sensor=cdiag.optString("sensor","UNKNOWN"),ip=cdiag.optString("ip","—");
        int pid=cdiag.optInt("pid",0);String fps=cdiag.has("fps")?String.format(Locale.US,"%.2f",cdiag.optDouble("fps",0)):"—";String rssi=cdiag.has("rssi")?cdiag.optInt("rssi",0)+" dBm":"—";
        return "CAM "+(cameraOk?"OK":"FAIL")+" • Wi-Fi "+(wifiOk?"OK":"NO")+" • IP "+ip+"\nSensor "+sensor+" PID 0x"+String.format(Locale.US,"%04X",pid)+" • ERR "+err+"\nMemory "+mem.toUpperCase(Locale.US)+" • PSRAM "+(psram?"YES":"NO")+" • FPS "+fps+" • RSSI "+rssi;
    }

    private void testConnections(){host.io().execute(()->{boolean robotOk=false;JSONObject robotDiag=null;try{robotDiag=host.robotApi().diagnostics();robotOk=robotDiag!=null;}catch(Exception ignored){}JSONObject cam=null;try{cam=host.cameraDiagnostics();}catch(Exception ignored){}boolean cameraHttp=cam!=null;if(!cameraHttp)try{cameraHttp=host.cameraReachable();}catch(Exception ignored){}final boolean rOk=robotOk,cHttp=cameraHttp;final JSONObject rdiag=robotDiag,cdiag=cam;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(cdiag!=null){boolean actual=cdiag.optBoolean("camera_ok",false);cameraState.setText(cameraDiagnosticText(cdiag));cameraState.setTextColor(actual?0xFF81C784:0xFFFF7043);}else{int clients=rdiag==null?-1:rdiag.optInt("clients",-1);String hint=clients>=0?" • AP clients "+clients:"";cameraState.setText("CAM "+(cHttp?"HTTP OK":"OFFLINE")+hint+"\nإذا كان الهاتف والـCAM فقط متصلين، يفترض أن يكون AP clients ≥ 2. تحقق من إزالة وصلة GPIO0-GND بعد البرمجة ثم أعد التشغيل.");cameraState.setTextColor(cHttp?0xFFFFB74D:0xFFFF7043);}host.showToast("ESP32 "+(rOk?"OK":"OFFLINE")+" • CAM "+(cHttp?"HTTP":"OFFLINE"));});});}
    private void testAlert(){host.io().execute(()->{try{host.robotApi().alertTest();host.showToast("تم إرسال اختبار تنبيه الروبوت");}catch(Exception e){host.showToast("تعذر اختبار تنبيه الروبوت");}});}
    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();JSONObject s=host.robotApi().safetyAutoStopStatus();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{showConfig(x);showSafetyAutoStop(false);});}catch(Exception e){host.showToast("تعذر المزامنة");}});}
    private void apply(){final int w=clamp(parse(warm,60),0,3600),m=clamp(parse(measure,30),5,600);final String cam=host.cameraHost();host.setDriveCommand(0,0);host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,DRIVE_FIRMWARE_LIMIT,w,m);host.robotApi().setSafetyAutoStop(false);host.preferences().edit().remove("speed").putInt("warm",w).putInt("measure",m).putBoolean("safety_auto_stop",false).remove("nl").remove("nr").apply();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{showConfig(x);showSafetyAutoStop(false);});host.showToast("تم تطبيق الضبط • التوقف التلقائي معطّل");}catch(Exception e){host.showToast("فشل تطبيق الضبط");}});}
    private void showConfig(JSONObject x){if(x==null)return;String cam=x.optString("cam",camera.getText().toString());if(cam==null||cam.trim().isEmpty())cam="192.168.4.2";camera.setText(cam);host.setCameraHost(cam);warm.setText(String.valueOf(x.optInt("warm",parse(warm,60))));measure.setText(String.valueOf(x.optInt("measure",parse(measure,30))));}
    private void showSafetyAutoStop(boolean enabled){safetyAutoStop.setChecked(false);host.preferences().edit().putBoolean("safety_auto_stop",false).apply();}
    private static int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception ignored){return def;}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
