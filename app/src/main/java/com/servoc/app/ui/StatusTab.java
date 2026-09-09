package com.servoc.app.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.BuildConfig;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.communication.RemoteConnection;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.update.AppUpdateManager;

import org.json.JSONObject;

import java.util.Locale;

/** Compact operational status dashboard; transport state is kept separate from sensor health. */
public final class StatusTab {
    private final HazRobHost host;private final ScrollView scroll;
    private final TextView master,network,hardware,sensors,science,camera;
    private final Button appUpdate;
    private final AppUpdateManager appUpdater;
    private volatile JSONObject cameraSnapshot;private volatile long cameraProbeAt=0;
    private volatile boolean updateBusy=false;

    public StatusTab(Context c,HazRobHost host){
        this.host=host;appUpdater=new AppUpdateManager(c);scroll=new ScrollView(c);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,10));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"الحالة"),UiKit.full(c));
        appUpdate=UiKit.button(c,initialUpdateLabel());appUpdate.setOnClickListener(v->runAppUpdate());root.addView(appUpdate,UiKit.full(c));
        TextView updateNote=UiKit.small(c,"تحديث التطبيق متاح لجميع المستخدمين. إدارة مكتبة ChemCore العلمية وتطويرها تبقى داخل تبويب المطور فقط.");updateNote.setTextColor(0xFFB0BEC5);root.addView(updateNote,UiKit.full(c));
        master=card(c,root);network=card(c,root);hardware=card(c,root);sensors=card(c,root);science=card(c,root);camera=card(c,root);
        science.setText("Scientific   MQ "+HazRobHardwareProfile.MQ.length+"/8   •   Curves "+GasEngine.curveCount()+"+"+ResearchCurveRegistry.count()+"   •   Models "+ModelRuntimeRegistry.nativeCount()+"/"+MqDataScience76Catalog.COUNT+" Native");
    }
    public View view(){return scroll;}
    private static TextView card(Context c,LinearLayout root){TextView t=UiKit.text(c,"—");t.setTextSize(14);t.setBackgroundColor(0xFF151A20);root.addView(t,UiKit.full(c));return t;}
    private boolean remoteMode(){return "remote".equalsIgnoreCase(host.preferences().getString("connection_mode","direct"));}

    private String initialUpdateLabel(){String completed=appUpdater.recentCompletionLabel();return completed.isEmpty()?"تحديث التطبيق • v"+BuildConfig.VERSION_NAME:completed;}

    private void runAppUpdate(){
        if(updateBusy)return;
        JSONObject d=host.latestDiagnostics(),m=d==null?null:d.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle").toLowerCase(Locale.US);
        if("warmup".equals(state)||"measuring".equals(state)){
            appUpdate.setText("أوقف القياس قبل تحديث التطبيق");
            scroll.postDelayed(()->{if(!updateBusy)appUpdate.setText(initialUpdateLabel());},3000);
            return;
        }
        updateBusy=true;appUpdate.setEnabled(false);appUpdate.setText("جار البحث عن تحديث...");
        host.io().execute(()->appUpdater.updateNow(s->scroll.post(()->renderUpdateState(s))));
    }

    private void renderUpdateState(AppUpdateManager.State s){
        if(s==null)return;appUpdate.setText(s.message);
        switch(s.phase){
            case CHECKING:
            case UPDATE_AVAILABLE:
            case DOWNLOADING:
            case READY_TO_INSTALL:
                appUpdate.setEnabled(false);break;
            case INSTALLER_OPENED:
                updateBusy=false;appUpdate.setEnabled(true);break;
            case INSTALL_PERMISSION_REQUIRED:
            case UP_TO_DATE:
            case COMPLETED:
            case ERROR:
            default:
                updateBusy=false;appUpdate.setEnabled(true);break;
        }
    }

    public void refresh(){
        if(!updateBusy){String completed=appUpdater.recentCompletionLabel();if(!completed.isEmpty())appUpdate.setText(completed);}
        int warm=Math.max(0,host.preferences().getInt("warm",60)),analysis=Math.max(0,host.preferences().getInt("measure",30));
        science.setText("Scientific   MQ "+HazRobHardwareProfile.MQ.length+"/8   •   Curves "+GasEngine.curveCount()+"+"+ResearchCurveRegistry.count()+"   •   Models "+ModelRuntimeRegistry.nativeCount()+"/"+MqDataScience76Catalog.COUNT+" Native\nTiming   Warm "+warm+" s   •   Analysis "+analysis+" s   •   Tracking CONTINUOUS");
        JSONObject d=host.latestDiagnostics();
        if(d==null){master.setText("HazRob App   v"+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMaster ESP32   ● OFFLINE");master.setTextColor(0xFFFF7043);renderNetwork(null);hardware.setText("ADS / ENV / MQ Power / LDR / Alert   —");sensors.setText("MQ   —");renderCamera();return;}
        master.setText("HazRob App   v"+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMaster ESP32   ● ONLINE\nFirmware   "+d.optString("firmware","HazRob")+"\nAPI   "+d.optInt("api_version",0)+"\nProfile   "+d.optString("hardware_profile",HazRobHardwareProfile.ID)+"\nHeap   "+d.optLong("heap",0)+" B");master.setTextColor(0xFF81C784);
        renderNetwork(d);
        JSONObject adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),env=d.optJSONObject("env"),ldr=d.optJSONObject("ldr"),alert=d.optJSONObject("alert");boolean mqOn=relay!=null&&(relay.optBoolean("on",false)||relay.optInt("mask",0)!=0);boolean ldrOk=ldr!=null&&ldr.optBoolean("installed",false);int alertLevel=alert==null?0:alert.optInt("level",0),alertGpio=alert==null?26:alert.optInt("gpio",26);String alertName=alert==null?"—":alert.optString("name","none");
        hardware.setText("ADS1 0x48   "+ok(adc!=null&&adc.optBoolean("ads1",false))+"\nADS2 0x49   "+ok(adc!=null&&adc.optBoolean("ads2",false))+"\nMQ POWER GPIO25   "+on(mqOn)+"   •   8 sensors\nENV   "+ok(env!=null&&env.optBoolean("ok",false))+"\nLDR GPIO34   "+ok(ldrOk)+(ldrOk?"   •   LEVEL "+ldr.optInt("level",-1):"")+"\nALERT GPIO"+alertGpio+"   "+(alert==null?"○ —":alertLevel>0?"● "+alertName.toUpperCase(Locale.US):"● READY"));
        JSONObject mq=d.optJSONObject("mq");StringBuilder s=new StringBuilder();for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){if(s.length()>0)s.append('\n');SensorHealthEngine.Snapshot h=host.sensorHealth(hw.key);JSONObject r=mq==null?null:mq.optJSONObject(hw.key);boolean valid=r!=null&&r.optBoolean("valid",r.optBoolean("ok",false));boolean calibrated=r==null||r.optBoolean("manual_calibration_confirmed",true);s.append(hw.key.toUpperCase(Locale.US)).append("   ADS").append(hw.ads).append(" A").append(hw.adcChannel).append("   ").append(valid?"● SIGNAL   ":"○ —   ").append(SensorHealthEngine.stateAr(h.state)).append(calibrated?"   •   CAL ●":"   •   CAL —");}sensors.setText(s.toString());
        renderCamera();long now=SystemClock.elapsedRealtime();if(now-cameraProbeAt>5000){cameraProbeAt=now;if(remoteMode()){cameraSnapshot=host.robotApi().remoteCameraStatus();scroll.post(this::renderCamera);}else host.io().execute(()->{JSONObject q=null;try{q=host.cameraDiagnostics();}catch(Exception ignored){}cameraSnapshot=q;scroll.post(this::renderCamera);});}
    }

    private void renderNetwork(JSONObject d){
        JSONObject link=d==null?null:d.optJSONObject("link");
        if(!remoteMode()){
            network.setText("الوضع   DIRECT / Wi-Fi\nLocal   "+(d==null?"○ OFFLINE":"● CONNECTED")+"\nHeartbeat   "+(link!=null&&link.optBoolean("lost",false)?"LOST":link!=null&&link.optBoolean("armed",false)?"ACTIVE":"LOCAL"));
            return;
        }
        RemoteConnection r=RemoteConnection.active();JSONObject st=r==null?null:r.status();boolean mqtt=r!=null&&r.isConnected(),internet=st!=null&&st.optBoolean("internet",false),napt=st!=null&&st.optBoolean("napt",false),telemetry=d!=null;
        network.setText("الوضع   REMOTE / Internet\nMQTT/TLS   "+ok(mqtt)+"\nTelemetry   "+ok(telemetry)+"\nWAN   "+ok(internet)+"   NAPT   "+ok(napt));
    }

    private void renderCamera(){
        JSONObject cam=cameraSnapshot;
        if(remoteMode()){
            if(cam!=null){camera.setText("ESP32-CAM / REMOTE   "+(cam.optBoolean("online",cam.optBoolean("ok",false))?"● ONLINE":"○ —")+"\nWSS   "+(host.preferences().getString("remote_video_url","").isEmpty()?"NOT CONFIGURED":"CONFIGURED"));}
            else camera.setText("ESP32-CAM / REMOTE\nWSS   "+(host.preferences().getString("remote_video_url","").isEmpty()?"○ NOT CONFIGURED":"● CONFIGURED")+"\nCamera status   —");
            return;
        }
        if(cam!=null)camera.setText("ESP32-CAM   "+(cam.optBoolean("camera_ok",cam.optBoolean("ok",true))?"● ONLINE":"● ERROR")+"\nFW   "+cam.optString("fw","—")+"\nFPS   "+String.format(Locale.US,"%.1f",cam.optDouble("fps",0))+"\nRSSI   "+cam.optInt("rssi",0)+" dBm");else camera.setText("ESP32-CAM   ○ OFFLINE");
    }
    private static String ok(boolean b){return b?"● OK":"○ —";}private static String on(boolean b){return b?"● ON":"○ OFF";}
}
