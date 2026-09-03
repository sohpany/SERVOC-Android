package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.SensorHealthEngine;
import org.json.JSONObject;
import java.util.Locale;

/** Compact operational status dashboard; no instructional prose. */
public final class StatusTab {
    private final HazRobHost host;private final ScrollView scroll;
    private final TextView master,network,hardware,sensors,camera;

    public StatusTab(Context c,HazRobHost host){
        this.host=host;scroll=new ScrollView(c);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,10));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"الحالة"),UiKit.full(c));
        master=card(c,root);network=card(c,root);hardware=card(c,root);sensors=card(c,root);camera=card(c,root);
    }
    public View view(){return scroll;}
    private static TextView card(Context c,LinearLayout root){TextView t=UiKit.text(c,"—");t.setTextSize(14);t.setBackgroundColor(0xFF151A20);root.addView(t,UiKit.full(c));return t;}

    public void refresh(){
        JSONObject d=host.latestDiagnostics();
        if(d==null){master.setText("Master ESP32   ● OFFLINE");master.setTextColor(0xFFFF7043);network.setText("الاتصال   —");hardware.setText("ADS / ENV / Relay   —");sensors.setText("MQ   —");camera.setText("ESP32-CAM   —");return;}
        master.setText("Master ESP32   ● ONLINE\nFirmware   "+d.optString("firmware","HazRob")+"\nAPI   "+d.optInt("api_version",0)+"\nHeap   "+d.optLong("heap",0)+" B");master.setTextColor(0xFF81C784);
        JSONObject link=d.optJSONObject("link");network.setText("Local   ● CONNECTED\nInternet   "+(link!=null&&link.optBoolean("remote_ready",false)?"● READY":"○ NOT CONFIGURED")+"\nHeartbeat   "+(link!=null&&link.optBoolean("lost",false)?"LOST":link!=null&&link.optBoolean("armed",false)?"ACTIVE":"LOCAL"));
        JSONObject adc=d.optJSONObject("adc"),relay=d.optJSONObject("relay"),env=d.optJSONObject("env");int mask=relay==null?0:relay.optInt("mask",0)&3;
        hardware.setText("ADS1 0x48   "+ok(adc!=null&&adc.optBoolean("ads1",false))+"\nADS2 0x49   "+ok(adc!=null&&adc.optBoolean("ads2",false))+"\nR1   "+on((mask&1)!=0)+"   R2   "+on((mask&2)!=0)+"\nENV   "+ok(env!=null&&env.optBoolean("ok",false)));
        StringBuilder s=new StringBuilder();for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){if(s.length()>0)s.append('\n');SensorHealthEngine.Snapshot h=host.sensorHealth(hw.key);JSONObject q=d.optJSONObject("mq");JSONObject r=q==null?null:q.optJSONObject(hw.key);boolean valid=r!=null&&r.optBoolean("valid",r.optBoolean("ok",false));s.append(hw.key.toUpperCase(Locale.US)).append("   R").append(hw.relay).append("   ").append(valid?"● SIGNAL":"○ ").append(SensorHealthEngine.stateAr(h.state));}sensors.setText(s.toString());
        JSONObject cam=host.cameraDiagnostics();if(cam!=null)camera.setText("ESP32-CAM   "+(cam.optBoolean("camera_ok",cam.optBoolean("ok",true))?"● ONLINE":"● ERROR")+"\nFW   "+cam.optString("fw","—")+"\nFPS   "+String.format(Locale.US,"%.1f",cam.optDouble("fps",0))+"\nRSSI   "+cam.optInt("rssi",0)+" dBm");else camera.setText("ESP32-CAM   ○ OFFLINE");
    }
    private static String ok(boolean b){return b?"● OK":"○ —";}private static String on(boolean b){return b?"● ON":"○ OFF";}
}
