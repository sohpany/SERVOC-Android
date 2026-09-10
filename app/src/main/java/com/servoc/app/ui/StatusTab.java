package com.servoc.app.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.BuildConfig;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.camera.RemoteVideoReadiness;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
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
    private final Context context;
    private final HazRobHost host;private final ScrollView scroll;
    private final TextView master,network,hardware,sensors,science,camera;
    private final Button updateButton;
    private final AppUpdateManager appUpdater;
    private final ChemCoreLibraryManager libraryUpdater;
    private volatile JSONObject cameraSnapshot;private volatile RemoteVideoReadiness.Result remoteCameraSnapshot;private volatile long cameraProbeAt=0;
    private volatile boolean updateBusy=false;

    public StatusTab(Context c,HazRobHost host){
        this.context=c.getApplicationContext();this.host=host;appUpdater=new AppUpdateManager(c);libraryUpdater=new ChemCoreLibraryManager(c);scroll=new ScrollView(c);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,10));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"الحالة"),UiKit.full(c));
        updateButton=UiKit.button(c,initialUpdateLabel());updateButton.setOnClickListener(v->runUnifiedUpdate());root.addView(updateButton,UiKit.full(c));
        TextView updateNote=UiKit.small(c,"زر التحديث متاح لجميع المستخدمين ويبحث عن تحديثات التطبيق ومكتبة ChemCore المستقرة. إدارة المكتبة وتطويرها ومصدر التحديث والرجوع للإصدارات السابقة تبقى داخل تبويب المطور فقط.");updateNote.setTextColor(0xFFB0BEC5);root.addView(updateNote,UiKit.full(c));
        master=card(c,root);network=card(c,root);hardware=card(c,root);sensors=card(c,root);science=card(c,root);camera=card(c,root);
        science.setText("Scientific   MQ "+HazRobHardwareProfile.MQ.length+"/8   •   Curves "+GasEngine.curveCount()+"+"+ResearchCurveRegistry.count()+"   •   Models "+ModelRuntimeRegistry.nativeCount()+"/"+MqDataScience76Catalog.COUNT+" Native");
    }
    public View view(){return scroll;}
    private static TextView card(Context c,LinearLayout root){TextView t=UiKit.text(c,"—");t.setTextSize(14);t.setBackgroundColor(0xFF151A20);root.addView(t,UiKit.full(c));return t;}
    private boolean remoteMode(){return "remote".equalsIgnoreCase(host.preferences().getString("connection_mode","direct"));}

    private String initialUpdateLabel(){
        String completed=appUpdater.recentCompletionLabel();
        if(!completed.isEmpty())return completed;
        ChemCoreLibraryManager.Status lib=libraryUpdater.status();
        return "تحديث • App "+BuildConfig.VERSION_NAME+" • ChemCore "+(lib.version.isEmpty()?"—":lib.version);
    }

    private void runUnifiedUpdate(){
        if(updateBusy)return;
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            updateButton.setText("أوقف القياس/التحليل قبل التحديث");
            scroll.postDelayed(()->{if(!updateBusy)updateButton.setText(initialUpdateLabel());},3000);
            return;
        }
        updateBusy=true;updateButton.setEnabled(false);updateButton.setText("جار البحث عن تحديث...");
        host.io().execute(this::performUnifiedUpdate);
    }

    private void performUnifiedUpdate(){
        boolean libraryUpdated=false;String libraryVersion="";String libraryIssue="";
        try{
            scroll.post(()->updateButton.setText("جار فحص مكتبة ChemCore..."));
            ChemCoreLibraryManager.CheckSummary checked=libraryUpdater.check(ReleaseAudit.VERSION);
            if(checked.updateAvailable){
                scroll.post(()->updateButton.setText("جار تحديث مكتبة ChemCore..."));
                ChemCoreLibraryStore.InstallResult r=libraryUpdater.install(checked,ReleaseAudit.VERSION,
                        ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics()));
                if(r.installed){
                    libraryUpdated=true;libraryVersion=r.version;
                    ChemCoreMolecularSeedImporter.Result applied=ChemCoreMolecularSeedImporter.importVerifiedPayload(libraryUpdater.activePayload(),host.database());
                    if(!applied.available)libraryIssue="ثبتت المكتبة لكن لم توجد طبقة جزيئية قابلة للتطبيق: "+applied.message;
                }else libraryIssue=r.reason;
            }
        }catch(Exception e){libraryIssue=safe(e);}
        final boolean libUpdated=libraryUpdated;final String libVersion=libraryVersion;final String libIssue=libraryIssue;
        appUpdater.updateNow(s->scroll.post(()->renderUnifiedUpdateState(s,libUpdated,libVersion,libIssue)));
    }

    private void renderUnifiedUpdateState(AppUpdateManager.State s,boolean libraryUpdated,String libraryVersion,String libraryIssue){
        if(s==null)return;
        String message=s.message;
        if(s.phase==AppUpdateManager.Phase.UP_TO_DATE){
            if(libraryUpdated&&libraryIssue.isEmpty())message="تم التحديث • ChemCore "+libraryVersion;
            else if(libraryUpdated)message="تم تحديث ChemCore "+libraryVersion+" • تحذير تطبيق المعرفة";
            else if(!libraryIssue.isEmpty())message="لا يوجد تحديث للتطبيق • تعذر فحص/تحديث المكتبة";
            else message="لا يوجد تحديث جديد";
        }else if(s.phase==AppUpdateManager.Phase.ERROR){
            if(libraryUpdated)message="تم تحديث ChemCore • تعذر فحص/تحديث التطبيق";
            else if(!libraryIssue.isEmpty())message="تعذر التحديث";
        }
        updateButton.setText(message);
        switch(s.phase){
            case CHECKING:
            case UPDATE_AVAILABLE:
            case DOWNLOADING:
            case READY_TO_INSTALL:
                updateButton.setEnabled(false);break;
            case INSTALLER_OPENED:
            case INSTALL_PERMISSION_REQUIRED:
            case UP_TO_DATE:
            case COMPLETED:
            case ERROR:
            default:
                updateBusy=false;updateButton.setEnabled(true);break;
        }
    }

    public void refresh(){
        if(!updateBusy){String completed=appUpdater.recentCompletionLabel();if(!completed.isEmpty())updateButton.setText(completed);}
        int warm=Math.max(0,host.preferences().getInt("warm",60)),analysis=Math.max(0,host.preferences().getInt("measure",30));
        ChemCoreLibraryManager.Status lib=libraryUpdater.status();
        science.setText("Scientific   MQ "+HazRobHardwareProfile.MQ.length+"/8   •   Curves "+GasEngine.curveCount()+"+"+ResearchCurveRegistry.count()+"   •   Models "+ModelRuntimeRegistry.nativeCount()+"/"+MqDataScience76Catalog.COUNT+" Native\nChemCore Library   "+(lib.version.isEmpty()?"—":lib.version)+"   •   "+lib.sourceLabel+"\nTiming   Warm "+warm+" s   •   Analysis "+analysis+" s   •   Tracking CONTINUOUS");
        JSONObject d=host.latestDiagnostics();
        if(d==null){master.setText("HazRob App   v"+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMaster ESP32   ● OFFLINE");master.setTextColor(0xFFFF7043);renderNetwork(null);hardware.setText("ADS / ENV / MQ Power / LDR / Alert   —");sensors.setText("MQ   —");renderCamera();scheduleCameraProbe();return;}
        master.setText("HazRob App   v"+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\nMaster ESP32   ● ONLINE\nFirmware   "+d.optString("firmware","HazRob")+"\nAPI   "+d.optInt("api_version",0)+"\nProfile   "+d.optString("hardware_profile",HazRobHardwareProfile.ID)+"\nHeap   "+d.optLong("heap",0)+" B");master.setTextColor(0xFF81C784);
        renderNetwork(d);
        JSONObject adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),env=d.optJSONObject("env"),ldr=d.optJSONObject("ldr"),alert=d.optJSONObject("alert");boolean mqOn=relay!=null&&(relay.optBoolean("on",false)||relay.optInt("mask",0)!=0);boolean ldrOk=ldr!=null&&ldr.optBoolean("installed",false);int alertLevel=alert==null?0:alert.optInt("level",0),alertGpio=alert==null?26:alert.optInt("gpio",26);String alertName=alert==null?"—":alert.optString("name","none");
        hardware.setText("ADS1 0x48   "+ok(adc!=null&&adc.optBoolean("ads1",false))+"\nADS2 0x49   "+ok(adc!=null&&adc.optBoolean("ads2",false))+"\nADS VDD   "+String.format(Locale.US,"%.1f V (fixed design)",HazRobHardwareProfile.ADS_SUPPLY_V)+"\nMQ POWER GPIO25   "+on(mqOn)+"   •   8 sensors\nENV   "+ok(env!=null&&env.optBoolean("ok",false))+"\nLDR GPIO34   "+ok(ldrOk)+(ldrOk?"   •   LEVEL "+ldr.optInt("level",-1):"")+"\nALERT GPIO"+alertGpio+"   "+(alert==null?"○ —":alertLevel>0?"● "+alertName.toUpperCase(Locale.US):"● READY"));
        JSONObject mq=d.optJSONObject("mq");StringBuilder s=new StringBuilder();for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){if(s.length()>0)s.append('\n');SensorHealthEngine.Snapshot h=host.sensorHealth(hw.key);JSONObject r=mq==null?null:mq.optJSONObject(hw.key);boolean valid=r!=null&&r.optBoolean("valid",r.optBoolean("ok",false));boolean calibrated=r!=null&&r.optBoolean("manual_calibration_confirmed",r.optBoolean("r0_calibrated",false));s.append(hw.key.toUpperCase(Locale.US)).append("   ADS").append(hw.ads).append(" A").append(hw.adcChannel).append("   ").append(valid?"● SIGNAL   ":"○ —   ").append(SensorHealthEngine.stateAr(h.state)).append(calibrated?"   •   CAL ●":"   •   CAL —");}sensors.setText(s.toString());
        renderCamera();scheduleCameraProbe();
    }

    private void scheduleCameraProbe(){
        long now=SystemClock.elapsedRealtime();if(now-cameraProbeAt<=5000)return;cameraProbeAt=now;
        if(remoteMode()){
            host.io().execute(()->{RemoteVideoReadiness.Result q=RemoteVideoReadiness.probeStatus(host.preferences());remoteCameraSnapshot=q;scroll.post(this::renderCamera);});
        }else{
            host.io().execute(()->{JSONObject q=null;try{q=host.cameraDiagnostics();}catch(Exception ignored){}cameraSnapshot=q;scroll.post(this::renderCamera);});
        }
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
        if(remoteMode()){
            RemoteVideoReadiness.Result cam=remoteCameraSnapshot;boolean configured=!host.preferences().getString("remote_video_url","").isEmpty();
            if(cam!=null){camera.setText("ESP32-CAM / REMOTE   "+(cam.publisher?"● ONLINE":"○ —")+"\nRender service   "+ok(cam.service)+"\nPublisher   "+ok(cam.publisher)+"\nWSS viewer   "+(configured?"● CONFIGURED":"○ NOT CONFIGURED")+"\n"+cam.detail);}
            else camera.setText("ESP32-CAM / REMOTE\nRender status   —\nWSS viewer   "+(configured?"● CONFIGURED":"○ NOT CONFIGURED"));
            return;
        }
        JSONObject cam=cameraSnapshot;if(cam!=null)camera.setText("ESP32-CAM   "+(cam.optBoolean("camera_ok",cam.optBoolean("ok",true))?"● ONLINE":"● ERROR")+"\nFW   "+cam.optString("fw","—")+"\nFPS   "+String.format(Locale.US,"%.1f",cam.optDouble("fps",0))+"\nRSSI   "+cam.optInt("rssi",0)+" dBm");else camera.setText("ESP32-CAM   ○ OFFLINE");
    }
    private static String safe(Exception e){String s=e==null?"unknown":e.getMessage();return s==null||s.trim().isEmpty()?e.getClass().getSimpleName():s;}
    private static String ok(boolean b){return b?"● OK":"○ —";}private static String on(boolean b){return b?"● ON":"○ OFF";}
}
