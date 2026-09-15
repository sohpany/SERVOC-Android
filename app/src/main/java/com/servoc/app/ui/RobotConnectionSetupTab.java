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

/** Developer-only WAN provisioning panel. HR058 keeps runtime disconnect separate from destructive WAN reset. */
public final class RobotConnectionSetupTab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root;
    private final TextView status;
    private final EditText ssid,password;
    private final CheckBox remember;
    private final AtomicBoolean statusBusy=new AtomicBoolean(false);

    public RobotConnectionSetupTab(Context c,HazRobHost host){
        context=c.getApplicationContext();this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        root.addView(UiKit.title(c,"تهيئة اتصال الروبوت"),UiKit.full(c));
        TextView note=UiKit.small(c,"HR058 يفصل بين ثلاثة أوامر: فصل Remote مؤقتًا مع الاحتفاظ ببيانات WAN، إعادة الاتصال بالشبكة المحفوظة، ومسح WAN نهائيًا. Direct يحافظ على HazRob كـ AP ثابت ويستخدم HTTP؛ Remote يستخدم AP+STA ثم MQTT/Render. وجود WAN محفوظ يعني أن الروبوت سيحاول Remote تلقائيًا بعد إعادة التشغيل.");
        note.setTextColor(0xFFB0BEC5);root.addView(note,UiKit.full(c));

        LinearLayout current=UiKit.card(c);current.addView(UiKit.title(c,"حالة الشبكة والاتصال البعيد"),UiKit.full(c));
        status=UiKit.text(c,"اضغط تحديث الحالة");status.setTextColor(Color.WHITE);status.setBackgroundColor(0xFF151A20);current.addView(status,UiKit.full(c));
        LinearLayout stateButtons=UiKit.hbox(c);
        Button refresh=UiKit.button(c,"تشخيص الاتصال");refresh.setOnClickListener(v->refreshStatus());stateButtons.addView(refresh,UiKit.weight(1));
        Button useSaved=UiKit.button(c,"استخدام الإعداد الحالي");useSaved.setOnClickListener(v->loadRemembered());stateButtons.addView(useSaved,UiKit.weight(1));current.addView(stateButtons,UiKit.full(c));

        Button disconnect=UiKit.button(c,"فصل Remote مع الاحتفاظ بـ WAN");disconnect.setOnClickListener(v->disconnectRemote());current.addView(disconnect,UiKit.full(c));
        Button reconnect=UiKit.button(c,"إعادة الاتصال بـ WAN المحفوظ");reconnect.setOnClickListener(v->reconnectRemote());current.addView(reconnect,UiKit.full(c));
        Button resetWan=UiKit.button(c,"مسح WAN نهائيًا");resetWan.setOnClickListener(v->confirmClearRobotWan());current.addView(resetWan,UiKit.full(c));
        root.addView(current,UiKit.full(c));

        LinearLayout form=UiKit.card(c);form.addView(UiKit.title(c,"إعداد شبكة WAN يدويًا"),UiKit.full(c));
        TextView scanNote=UiKit.small(c,"لا يتم استكشاف الشبكات من ESP32 في Direct. أدخل SSID وكلمة المرور يدويًا؛ هذا يمنع scan/re-channel من التأثير على الهاتف أو ESP32-CAM أثناء التشغيل المحلي.");scanNote.setTextColor(0xFF80CBC4);form.addView(scanNote,UiKit.full(c));
        ssid=field(c,form,"اسم الشبكة / SSID","");
        password=field(c,form,"كلمة المرور","");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        remember=new CheckBox(c);remember.setText("تذكرني");remember.setTextColor(Color.WHITE);form.addView(remember,UiKit.full(c));
        Button connect=UiKit.button(c,"حفظ WAN والتحويل إلى Remote");connect.setOnClickListener(v->provision());form.addView(connect,UiKit.full(c));root.addView(form,UiKit.full(c));

        loadRemembered();
    }

    public View view(){return scroll;}
    public void refresh(){}

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}

    private void loadRemembered(){
        RemoteWifiCredentialStore.Credentials x=new RemoteWifiCredentialStore(context,host.preferences()).load();
        if(x.present()){ssid.setText(x.ssid);password.setText(x.password);remember.setChecked(true);}else remember.setChecked(false);
    }

    private void refreshStatus(){
        if(!statusBusy.compareAndSet(false,true))return;
        status.setText("جارٍ قراءة سلسلة الاتصال من الروبوت...");
        host.io().execute(()->{
            try{
                JSONObject j=host.robotApi().remoteStatus();
                boolean configured=j.optBoolean("configured",false),transport=j.optBoolean("transport_enabled",false),wifi=j.optBoolean("wifi",false),ip=j.optBoolean("ip_ok",false),dns=j.optBoolean("dns",false),time=j.optBoolean("time",false),internet=j.optBoolean("internet",false),tls=j.optBoolean("tls",false),mqtt=j.optBoolean("mqtt",false),ack=j.optBoolean("command_ack",false),telemetry=j.optBoolean("telemetry",false),ready=j.optBoolean("remote_ready",false),retryHold=j.optBoolean("retry_hold",false),napt=j.optBoolean("napt",false);
                String profile=j.optString("radio_profile","—");String first=firstFailure(configured,transport,wifi,ip,dns,time,internet,tls,mqtt,ack,telemetry);String detail=dash(j.optString("detail",""));
                String text="Profile: "+profile+"   •   Transport: "+yes(transport)+"   •   NAPT: "+yes(napt)+"\n"+
                        "SSID: "+dash(j.optString("ssid",""))+"   •   IP: "+dash(j.optString("ip",""))+"\n"+
                        "RSSI: "+j.optInt("rssi",0)+" dBm   •   قناة Wi-Fi: "+j.optInt("wifi_channel",0)+"   •   عملاء HazRob: "+j.optInt("ap_clients",0)+"\n"+
                        "محاولات WAN: "+j.optInt("connect_attempts",0)+"   •   Retry hold: "+yes(retryHold)+"\n\n"+
                        stage("Config",configured)+"   "+stage("STA",wifi)+"   "+stage("IP",ip)+"\n"+
                        stage("DNS",dns)+"   "+stage("Time",time)+"   "+stage("Internet",internet)+"\n"+
                        stage("TLS",tls)+"   "+stage("MQTT",mqtt)+"   "+stage("ACK",ack)+"   "+stage("Telemetry",telemetry)+"\n\n"+
                        "Remote ready: "+yes(ready)+"\nأول مرحلة غير مكتملة: "+first+"\nFirmware detail: "+detail;
                root.post(()->{status.setText(text);status.setTextColor(ready?0xFF81C784:"direct".equals(profile)?0xFF80CBC4:0xFFFFB74D);});
            }catch(Exception e){root.post(()->{status.setText("تعذر قراءة /remote/status. اتصل بشبكة HazRob المحلية ثم أعد التشخيص.\n"+e.getClass().getSimpleName()+": "+dash(e.getMessage()));status.setTextColor(0xFFFF7043);});}
            finally{statusBusy.set(false);}
        });
    }

    private void provision(){
        final String s=ssid.getText().toString().trim(),p=password.getText().toString();final boolean save=remember.isChecked();
        if(s.isEmpty()){host.showToast("أدخل اسم الشبكة");return;}
        status.setText("جارٍ حفظ WAN وتحويل الروبوت إلى Remote...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteProvision(s,p);
                RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(context,host.preferences());if(save)store.save(s,p);else store.clear();
                root.post(()->{status.setText("تم حفظ WAN. الروبوت سيحوّل الراديو إلى Remote (AP+STA) ويستخدم محاولات WAN محدودة. ستبقى البيانات محفوظة لمحاولة Remote تلقائيًا بعد إعادة التشغيل.");status.setTextColor(0xFF81C784);});
            }catch(Exception e){root.post(()->{status.setText("فشل إرسال إعداد WAN. يجب أن يكون الهاتف متصلًا بـ HazRob أثناء التهيئة.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private void disconnectRemote(){
        status.setText("جارٍ إيقاف الاتصال البعيد والعودة إلى Direct AP-only دون حذف WAN...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteDisconnect();
                root.post(()->{status.setText("تم طلب فصل Remote. ستتوقف الحركة أولًا ثم MQTT/NAPT/STA، وتبقى بيانات WAN محفوظة. عند إعادة تشغيل الروبوت سيحاول WAN المحفوظ تلقائيًا.");status.setTextColor(0xFF80CBC4);});
            }catch(Exception e){root.post(()->{status.setText("تعذر فصل Remote. اتصل محليًا بشبكة HazRob ثم أعد المحاولة.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private void reconnectRemote(){
        status.setText("جارٍ إعادة الاتصال باستخدام WAN المحفوظ...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteReconnect();
                root.post(()->{status.setText("تم طلب إعادة Remote. الروبوت سيستخدم بيانات WAN المحفوظة ويحوّل الراديو إلى AP+STA ثم يعيد MQTT/NAPT/Render عند جاهزية الإنترنت.");status.setTextColor(0xFF81C784);});
            }catch(Exception e){root.post(()->{status.setText("تعذر إعادة Remote. تأكد من وجود WAN محفوظ ومن الاتصال المحلي بشبكة HazRob.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private void confirmClearRobotWan(){
        new AlertDialog.Builder(scroll.getContext())
                .setTitle("مسح WAN نهائيًا")
                .setMessage("سيتم حذف اسم شبكة المودم وكلمة المرور من الروبوت. بعد ذلك لن يحاول Remote عند إعادة التشغيل حتى تعيد إدخال بيانات WAN. هل تريد المتابعة؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("مسح WAN",(d,w)->clearRobotWan())
                .show();
    }

    private void clearRobotWan(){
        status.setText("جارٍ مسح WAN نهائيًا وإعادة الراديو إلى Direct AP-only...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteClear();new RemoteWifiCredentialStore(context,host.preferences()).clear();
                root.post(()->{ssid.setText("");password.setText("");remember.setChecked(false);status.setText("تم مسح WAN نهائيًا. الروبوت سيبقى Direct: AP-only + HTTP، ولن يحاول الاتصال البعيد بعد إعادة التشغيل حتى تحفظ WAN جديدًا.");status.setTextColor(0xFF80CBC4);});
            }catch(Exception e){root.post(()->{status.setText("تعذر مسح WAN. تأكد من الاتصال المباشر بشبكة HazRob ثم أعد المحاولة.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private static String stage(String label,boolean ok){return (ok?"● ":"○ ")+label;}
    private static String firstFailure(boolean configured,boolean transport,boolean wifi,boolean ip,boolean dns,boolean time,boolean internet,boolean tls,boolean mqtt,boolean ack,boolean telemetry){
        if(!configured)return"Config / لم تُحفظ شبكة خارجية";if(!transport)return"Remote transport disabled (Direct)";if(!wifi)return"STA / اتصال الراوتر";if(!ip)return"IP / DHCP";if(!dns)return"DNS";if(!time)return"Time / NTP";if(!internet)return"Internet readiness";if(!tls)return"TLS إلى HiveMQ";if(!mqtt)return"MQTT session";if(!ack)return"Command ACK";if(!telemetry)return"Telemetry";return"لا يوجد فشل — السلسلة مكتملة";
    }
    private static String yes(boolean b){return b?"● نعم":"○ لا";}
    private static String dash(String s){return s==null||s.trim().isEmpty()?"—":s;}
}
