package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.servoc.app.communication.RemoteWifiCredentialStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Developer-only local provisioning panel for the robot external Wi-Fi connection. */
public final class RobotConnectionSetupTab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root;
    private final TextView status;
    private final Spinner networks;
    private final ArrayAdapter<String> networkAdapter;
    private final List<String> networkSsids=new ArrayList<>();
    private final EditText ssid,password;
    private final CheckBox remember;
    private final AtomicBoolean statusBusy=new AtomicBoolean(false),scanBusy=new AtomicBoolean(false);

    public RobotConnectionSetupTab(Context c,HazRobHost host){
        context=c.getApplicationContext();this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        root.addView(UiKit.title(c,"تهيئة اتصال الروبوت"),UiKit.full(c));
        TextView note=UiKit.small(c,"تهيئة الشبكة الخارجية واستكشافها تتم عبر اتصال مؤقت بشبكة HazRob المحلية. بعد حفظ الشبكة يستطيع الروبوت استخدام الإنترنت والاتصال البعيد، بينما تبقى شبكة HazRob متاحة للاتصال المباشر.");
        note.setTextColor(0xFFB0BEC5);root.addView(note,UiKit.full(c));

        LinearLayout current=UiKit.card(c);current.addView(UiKit.title(c,"حالة الشبكة والاتصال البعيد"),UiKit.full(c));
        status=UiKit.text(c,"اضغط تحديث الحالة");status.setTextColor(Color.WHITE);status.setBackgroundColor(0xFF151A20);current.addView(status,UiKit.full(c));
        LinearLayout stateButtons=UiKit.hbox(c);Button refresh=UiKit.button(c,"تشخيص الاتصال");refresh.setOnClickListener(v->refreshStatus());stateButtons.addView(refresh,UiKit.weight(1));Button useSaved=UiKit.button(c,"استخدام الإعداد الحالي");useSaved.setOnClickListener(v->loadRemembered());stateButtons.addView(useSaved,UiKit.weight(1));current.addView(stateButtons,UiKit.full(c));root.addView(current,UiKit.full(c));

        LinearLayout discovery=UiKit.card(c);discovery.addView(UiKit.title(c,"استكشاف الشبكات"),UiKit.full(c));
        Button scan=UiKit.button(c,"استكشاف الشبكات");scan.setOnClickListener(v->scanNetworks());discovery.addView(scan,UiKit.full(c));
        networks=new Spinner(c);networkAdapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,new ArrayList<>());networkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);networks.setAdapter(networkAdapter);discovery.addView(networks,UiKit.full(c));
        Button select=UiKit.button(c,"استخدام الشبكة المحددة");select.setOnClickListener(v->selectNetwork());discovery.addView(select,UiKit.full(c));root.addView(discovery,UiKit.full(c));

        LinearLayout form=UiKit.card(c);form.addView(UiKit.title(c,"إعداد شبكة جديدة"),UiKit.full(c));
        ssid=field(c,form,"اسم الشبكة / SSID","");
        password=field(c,form,"كلمة المرور","");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        remember=new CheckBox(c);remember.setText("تذكرني");remember.setTextColor(Color.WHITE);form.addView(remember,UiKit.full(c));
        Button connect=UiKit.button(c,"دخول");connect.setOnClickListener(v->provision());form.addView(connect,UiKit.full(c));root.addView(form,UiKit.full(c));

        loadRemembered();
    }

    public View view(){return scroll;}

    /**
     * Intentionally no automatic network refresh here.
     * MainActivity refreshes the visible operator tab at 4 Hz for live telemetry; using that cadence
     * for Wi-Fi status previously queued overlapping HTTP calls and repeatedly rewrote this screen.
     * Network status and scans are now explicit operator actions only.
     */
    public void refresh(){}

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}

    private void loadRemembered(){
        RemoteWifiCredentialStore.Credentials x=new RemoteWifiCredentialStore(context,host.preferences()).load();
        if(x.present()){ssid.setText(x.ssid);password.setText(x.password);remember.setChecked(true);}
        else{remember.setChecked(false);}
    }

    private void refreshStatus(){
        if(!statusBusy.compareAndSet(false,true))return;
        status.setText("جارٍ قراءة سلسلة الاتصال من الروبوت...");
        host.io().execute(()->{
            try{
                JSONObject j=host.robotApi().remoteStatus();
                boolean configured=j.optBoolean("configured",false),wifi=j.optBoolean("wifi",false),ip=j.optBoolean("ip_ok",false),dns=j.optBoolean("dns",false),time=j.optBoolean("time",false),internet=j.optBoolean("internet",false),tls=j.optBoolean("tls",false),mqtt=j.optBoolean("mqtt",false),ack=j.optBoolean("command_ack",false),telemetry=j.optBoolean("telemetry",false),ready=j.optBoolean("remote_ready",false);
                String first=firstFailure(configured,wifi,ip,dns,time,internet,tls,mqtt,ack,telemetry);
                String detail=dash(j.optString("detail",""));
                String text="SSID: "+dash(j.optString("ssid",""))+"   •   IP: "+dash(j.optString("ip",""))+"\n"+
                        "RSSI: "+j.optInt("rssi",0)+" dBm\n\n"+
                        stage("1. Config",configured)+"   "+stage("2. Wi-Fi",wifi)+"   "+stage("3. IP",ip)+"\n"+
                        stage("4. DNS",dns)+"   "+stage("5. Time",time)+"   "+stage("6. Internet",internet)+"\n"+
                        stage("7. TLS",tls)+"   "+stage("8. MQTT",mqtt)+"\n"+
                        stage("9. CMD ACK",ack)+"   "+stage("10. Telemetry",telemetry)+"\n\n"+
                        "Remote ready: "+yes(ready)+"\n"+
                        "أول مرحلة غير مكتملة: "+first+"\n"+
                        "Firmware detail: "+detail;
                root.post(()->{status.setText(text);status.setTextColor(ready?0xFF81C784:0xFFFFB74D);});
            }catch(Exception e){root.post(()->{status.setText("تعذر قراءة /remote/status من الروبوت. اتصل بشبكة HazRob المحلية ثم أعد التشخيص.\n"+e.getClass().getSimpleName()+": "+dash(e.getMessage()));status.setTextColor(0xFFFF7043);});}
            finally{statusBusy.set(false);}
        });
    }

    private void scanNetworks(){
        if(!scanBusy.compareAndSet(false,true))return;
        status.setText("جارٍ استكشاف الشبكات التي يراها الروبوت...");
        host.io().execute(()->{
            try{
                JSONObject j=host.robotApi().remoteScan();JSONArray a=j.optJSONArray("networks");
                List<String> labels=new ArrayList<>(),ssids=new ArrayList<>();
                if(a!=null)for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n==null)continue;String s=n.optString("ssid","").trim();if(s.isEmpty()||ssids.contains(s))continue;ssids.add(s);labels.add((n.optBoolean("secure",true)?"🔒 ":"○ ")+s+"   "+n.optInt("rssi",0)+" dBm");}
                root.post(()->{networkSsids.clear();networkSsids.addAll(ssids);networkAdapter.clear();networkAdapter.addAll(labels);networkAdapter.notifyDataSetChanged();status.setText(labels.isEmpty()?"لم يعثر الروبوت على شبكات":"تم العثور على "+labels.size()+" شبكة");status.setTextColor(labels.isEmpty()?0xFFFFB74D:0xFF81C784);});
            }catch(Exception e){root.post(()->{status.setText("تعذر استكشاف الشبكات. يجب الاتصال أولًا بشبكة HazRob المحلية.");status.setTextColor(0xFFFF7043);});}
            finally{scanBusy.set(false);}
        });
    }

    private void selectNetwork(){int p=networks.getSelectedItemPosition();if(p<0||p>=networkSsids.size()){host.showToast("اختر شبكة من نتائج الاستكشاف");return;}ssid.setText(networkSsids.get(p));password.requestFocus();}

    private void provision(){
        final String s=ssid.getText().toString().trim(),p=password.getText().toString();final boolean save=remember.isChecked();
        if(s.isEmpty()){host.showToast("أدخل اسم الشبكة");return;}
        status.setText("جارٍ إرسال إعداد الشبكة إلى الروبوت...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteProvision(s,p);
                RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(context,host.preferences());if(save)store.save(s,p);else store.clear();
                root.post(()->{status.setText("تم إرسال إعداد الشبكة. سيحاول الروبوت الاتصال بها الآن. أبقِ شبكة HazRob متاحة كقناة مباشرة ثم شغّل «تشخيص الاتصال» لمعرفة أول مرحلة متوقفة.");status.setTextColor(0xFF81C784);});
            }catch(Exception e){root.post(()->{status.setText("فشل إرسال إعداد الشبكة. تأكد من اتصال الهاتف بشبكة HazRob.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private static String stage(String label,boolean ok){return (ok?"● ":"○ ")+label;}
    private static String firstFailure(boolean configured,boolean wifi,boolean ip,boolean dns,boolean time,boolean internet,boolean tls,boolean mqtt,boolean ack,boolean telemetry){
        if(!configured)return"Config / لم تُحفظ شبكة خارجية";
        if(!wifi)return"Wi-Fi / اتصال الراوتر";
        if(!ip)return"IP / DHCP";
        if(!dns)return"DNS";
        if(!time)return"Time / NTP";
        if(!internet)return"Internet readiness";
        if(!tls)return"TLS إلى HiveMQ";
        if(!mqtt)return"MQTT authentication/session";
        if(!ack)return"Command ACK";
        if(!telemetry)return"Telemetry";
        return"لا يوجد فشل — السلسلة مكتملة";
    }
    private static String yes(boolean b){return b?"● نعم":"○ لا";}
    private static String dash(String s){return s==null||s.trim().isEmpty()?"—":s;}
}
