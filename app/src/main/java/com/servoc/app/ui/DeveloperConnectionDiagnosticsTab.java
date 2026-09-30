package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.communication.RemoteWifiCredentialStore;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/** Developer-only connection diagnostics and remote transport provisioning. */
public final class DeveloperConnectionDiagnosticsTab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root,wanForm;
    private final TextView status,summary;
    private final EditText ssid,password;
    private final CheckBox remember;
    private final AtomicBoolean busy=new AtomicBoolean(false);

    public DeveloperConnectionDiagnosticsTab(Context c,HazRobHost host){
        context=c.getApplicationContext();this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"تشخيص الاتصال المتقدم"),UiKit.full(c));
        TextView note=UiKit.small(c,"Direct هو وضع الإقلاع المحلي. Remote يُفعّل صراحة ويستخدم نقل الإنترنت/MQTT دون تغيير وظائف القيادة المحلية أو الكاميرا.");note.setTextColor(0xFF90A4AE);root.addView(note,UiKit.full(c));

        LinearLayout diag=UiKit.card(c);diag.addView(UiKit.title(c,"سلسلة الاتصال"),UiKit.full(c));summary=UiKit.small(c,"—");summary.setBackgroundColor(0xFF151A20);diag.addView(summary,UiKit.full(c));status=UiKit.text(c,"اضغط تشخيص الاتصال");status.setBackgroundColor(0xFF151A20);diag.addView(status,UiKit.full(c));Button refresh=UiKit.button(c,"تشخيص الاتصال الآن");refresh.setOnClickListener(v->refreshStatus());diag.addView(refresh,UiKit.full(c));root.addView(diag,UiKit.full(c));

        LinearLayout remote=UiKit.card(c);remote.addView(UiKit.title(c,"التحكم في Remote"),UiKit.full(c));LinearLayout auth=UiKit.hbox(c);Button allow=UiKit.button(c,"سماح Remote");allow.setOnClickListener(v->authorize(true));auth.addView(allow,UiKit.weight(1));Button revoke=UiKit.button(c,"إلغاء السماح");revoke.setOnClickListener(v->authorize(false));auth.addView(revoke,UiKit.weight(1));remote.addView(auth,UiKit.full(c));LinearLayout state=UiKit.hbox(c);Button connect=UiKit.button(c,"تفعيل Remote");connect.setOnClickListener(v->reconnect());state.addView(connect,UiKit.weight(1));Button direct=UiKit.button(c,"العودة Direct");direct.setOnClickListener(v->disconnect());state.addView(direct,UiKit.weight(1));remote.addView(state,UiKit.full(c));root.addView(remote,UiKit.full(c));

        LinearLayout provisioning=UiKit.card(c);Button toggle=UiKit.button(c,"إعداد WAN المتقدم");provisioning.addView(toggle,UiKit.full(c));wanForm=UiKit.vbox(c);wanForm.setVisibility(View.GONE);TextView provNote=UiKit.small(c,"يُستخدم فقط لتغيير شبكة الإنترنت التي يعتمد عليها Remote. لا يغيّر SSID الخاص بشبكة HazRob المحلية.");provNote.setTextColor(0xFF90A4AE);wanForm.addView(provNote,UiKit.full(c));ssid=field(c,wanForm,"WAN SSID","");password=field(c,wanForm,"كلمة المرور","");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);remember=new CheckBox(c);remember.setText("حفظ بيانات WAN محليًا");remember.setTextColor(Color.WHITE);wanForm.addView(remember,UiKit.full(c));Button save=UiKit.button(c,"حفظ WAN");save.setOnClickListener(v->provision());wanForm.addView(save,UiKit.full(c));Button clear=UiKit.button(c,"مسح WAN من الروبوت والجهاز");clear.setOnClickListener(v->confirmClear());wanForm.addView(clear,UiKit.full(c));provisioning.addView(wanForm,UiKit.full(c));toggle.setOnClickListener(v->wanForm.setVisibility(wanForm.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));root.addView(provisioning,UiKit.full(c));
        loadRemembered();
    }

    public View view(){return scroll;}
    public void refresh(){refreshSummaryFromLatest();}

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}

    private void refreshSummaryFromLatest(){JSONObject d=host.latestDiagnostics();if(d==null){summary.setText("لا توجد بيانات حية من الروبوت");return;}JSONObject net=d.optJSONObject("network");if(net==null)net=d.optJSONObject("remote");StringBuilder s=new StringBuilder();s.append("وضع التطبيق: ").append(host.preferences().getString("connection_mode","direct"));if(net!=null){String profile=net.optString("radio_profile",net.optString("profile",""));if(!profile.isEmpty())s.append(" • Radio ").append(profile);if(net.has("rssi"))s.append(" • RSSI ").append(net.optInt("rssi",0)).append(" dBm");String ip=net.optString("ip","");if(!ip.isEmpty())s.append(" • IP ").append(ip);}summary.setText(s.toString());}

    private void loadRemembered(){RemoteWifiCredentialStore.Credentials x=new RemoteWifiCredentialStore(context,host.preferences()).load();if(x.present()){ssid.setText(x.ssid);password.setText(x.password);remember.setChecked(true);}else remember.setChecked(false);}

    private void refreshStatus(){if(!busy.compareAndSet(false,true))return;status.setText("جارٍ قراءة /remote/status...");host.io().execute(()->{try{JSONObject j=host.robotApi().remoteStatus();boolean configured=j.optBoolean("configured",false),authorized=j.optBoolean("developer_authorized",false),transport=j.optBoolean("transport_enabled",false),wifi=j.optBoolean("wifi",false),ip=j.optBoolean("ip_ok",false),dns=j.optBoolean("dns",false),time=j.optBoolean("time",false),internet=j.optBoolean("internet",false),tls=j.optBoolean("tls",false),mqtt=j.optBoolean("mqtt",false),ack=j.optBoolean("command_ack",false),telemetry=j.optBoolean("telemetry",false),ready=j.optBoolean("remote_ready",false),napt=j.optBoolean("napt",false);String profile=j.optString("radio_profile","—");String text="Profile: "+profile+" • Boot: "+j.optString("boot_default","direct")+" • Authorized: "+yes(authorized)+"\nTransport "+yes(transport)+" • NAPT "+yes(napt)+" • RSSI "+j.optInt("rssi",0)+" dBm\nSSID: "+dash(j.optString("ssid",""))+" • IP: "+dash(j.optString("ip",""))+"\n"+stage("Config",configured)+"  "+stage("STA",wifi)+"  "+stage("IP",ip)+"  "+stage("DNS",dns)+"\n"+stage("Time",time)+"  "+stage("Internet",internet)+"  "+stage("TLS",tls)+"  "+stage("MQTT",mqtt)+"\n"+stage("ACK",ack)+"  "+stage("Telemetry",telemetry)+"\nRemote ready: "+yes(ready)+" • First failure: "+firstFailure(configured,transport,wifi,ip,dns,time,internet,tls,mqtt,ack,telemetry)+"\nDetail: "+dash(j.optString("detail",""));root.post(()->{status.setText(text);status.setTextColor(ready?0xFF81C784:"direct".equalsIgnoreCase(profile)?0xFF80CBC4:0xFFFFB74D);summary.setText("Radio "+profile+" • Remote "+yes(ready)+" • MQTT "+yes(mqtt)+" • Telemetry "+yes(telemetry));});}catch(Exception e){root.post(()->{status.setText("تعذر قراءة حالة الاتصال: "+dash(e.getMessage()));status.setTextColor(0xFFFF7043);});}finally{busy.set(false);}});}

    private void authorize(boolean enabled){host.io().execute(()->{try{host.robotApi().remoteAuthorize(enabled);host.preferences().edit().putBoolean("remote_authorized_by_developer",enabled).apply();root.post(()->{host.showToast(enabled?"تم السماح بالاتصال البعيد":"تم إلغاء سماح الاتصال البعيد");refreshStatus();});}catch(Exception e){host.showToast("تعذر تغيير صلاحية Remote");}});}
    private void reconnect(){host.io().execute(()->{try{host.robotApi().remoteReconnect();host.preferences().edit().putString("connection_mode","remote").apply();root.post(()->{host.showToast("تم طلب Remote");refreshStatus();});}catch(Exception e){host.showToast("تعذر تفعيل Remote؛ تحقق من WAN والصلاحية");}});}
    private void disconnect(){host.io().execute(()->{try{host.robotApi().remoteDisconnect();host.preferences().edit().putString("connection_mode","direct").apply();root.post(()->{host.showToast("تم طلب Direct");refreshStatus();});}catch(Exception e){host.showToast("تعذر العودة إلى Direct");}});}

    private void provision(){String s=ssid.getText().toString().trim(),p=password.getText().toString();if(s.isEmpty()){host.showToast("أدخل WAN SSID");return;}host.io().execute(()->{try{host.robotApi().remoteProvision(s,p);RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(context,host.preferences());if(remember.isChecked())store.save(s,p);else store.clear();root.post(()->{host.showToast("تم حفظ WAN دون تغيير وضع الاتصال الحالي");refreshStatus();});}catch(Exception e){host.showToast("تعذر حفظ WAN؛ اتصل أولًا بشبكة HazRob المحلية");}});}
    private void confirmClear(){new AlertDialog.Builder(scroll.getContext()).setTitle("مسح WAN").setMessage("سيتم حذف بيانات WAN من الروبوت ومن التخزين المحلي. هل تريد المتابعة؟").setNegativeButton("إلغاء",null).setPositiveButton("مسح",(d,w)->clearWan()).show();}
    private void clearWan(){host.io().execute(()->{try{host.robotApi().remoteClear();new RemoteWifiCredentialStore(context,host.preferences()).clear();host.preferences().edit().putString("connection_mode","direct").apply();root.post(()->{ssid.setText("");password.setText("");remember.setChecked(false);host.showToast("تم مسح WAN والعودة إلى Direct");refreshStatus();});}catch(Exception e){host.showToast("تعذر مسح WAN");}});}

    private static String stage(String label,boolean ok){return(ok?"● ":"○ ")+label;}
    private static String yes(boolean v){return v?"YES":"NO";}
    private static String dash(String s){return s==null||s.trim().isEmpty()?"—":s;}
    private static String firstFailure(boolean configured,boolean transport,boolean wifi,boolean ip,boolean dns,boolean time,boolean internet,boolean tls,boolean mqtt,boolean ack,boolean telemetry){if(!configured)return"Config";if(!transport)return"Transport";if(!wifi)return"STA";if(!ip)return"IP";if(!dns)return"DNS";if(!time)return"Time";if(!internet)return"Internet";if(!tls)return"TLS";if(!mqtt)return"MQTT";if(!ack)return"ACK";if(!telemetry)return"Telemetry";return"none";}
}
