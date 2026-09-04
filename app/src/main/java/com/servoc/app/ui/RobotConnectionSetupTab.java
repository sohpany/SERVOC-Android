package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
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

/** Developer-only local provisioning panel for the robot external Wi-Fi connection. */
public final class RobotConnectionSetupTab {
    private static final long MIN_SCAN_DISPLAY_MS=3000L;

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
    private final Button scanButton;
    private final Button refreshButton;

    private boolean scanInProgress=false;
    private boolean statusInProgress=false;

    public RobotConnectionSetupTab(Context c,HazRobHost host){
        context=c.getApplicationContext();this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        root.addView(UiKit.title(c,"تهيئة اتصال الروبوت"),UiKit.full(c));
        TextView note=UiKit.small(c,"تهيئة الشبكة الخارجية واستكشافها تتم عبر اتصال مؤقت بشبكة HazRob المحلية. لا يوجد بحث تلقائي في هذه الصفحة؛ اضغط زر البحث مرة واحدة لعرض الشبكات المتاحة. الشبكة الجديدة تُختبر أولًا ولا تستبدل الشبكة المحفوظة إلا بعد نجاح Wi-Fi وDNS والإنترنت؛ عند الفشل يعود الروبوت تلقائيًا إلى الإعداد السابق.");
        note.setTextColor(0xFFB0BEC5);root.addView(note,UiKit.full(c));

        LinearLayout current=UiKit.card(c);current.addView(UiKit.title(c,"حالة الشبكة الحالية"),UiKit.full(c));
        status=UiKit.text(c,"اضغط «تحديث الحالة» أو «بحث عن الشبكات»");status.setTextColor(Color.WHITE);status.setBackgroundColor(0xFF151A20);current.addView(status,UiKit.full(c));
        LinearLayout stateButtons=UiKit.hbox(c);
        refreshButton=UiKit.button(c,"تحديث الحالة");refreshButton.setOnClickListener(v->refreshStatus());stateButtons.addView(refreshButton,UiKit.weight(1));
        Button useSaved=UiKit.button(c,"استخدام الإعداد الحالي");useSaved.setOnClickListener(v->loadRemembered());stateButtons.addView(useSaved,UiKit.weight(1));current.addView(stateButtons,UiKit.full(c));root.addView(current,UiKit.full(c));

        LinearLayout discovery=UiKit.card(c);discovery.addView(UiKit.title(c,"استكشاف الشبكات"),UiKit.full(c));
        scanButton=UiKit.button(c,"بحث عن الشبكات");scanButton.setOnClickListener(v->scanNetworks());discovery.addView(scanButton,UiKit.full(c));
        networks=new Spinner(c);networkAdapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,new ArrayList<>());networkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);networks.setAdapter(networkAdapter);discovery.addView(networks,UiKit.full(c));
        Button select=UiKit.button(c,"استخدام الشبكة المحددة");select.setOnClickListener(v->selectNetwork());discovery.addView(select,UiKit.full(c));root.addView(discovery,UiKit.full(c));

        LinearLayout form=UiKit.card(c);form.addView(UiKit.title(c,"إعداد شبكة جديدة"),UiKit.full(c));
        ssid=field(c,form,"اسم الشبكة / SSID","");
        password=field(c,form,"كلمة المرور","");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        remember=new CheckBox(c);remember.setText("تذكرني");remember.setTextColor(Color.WHITE);form.addView(remember,UiKit.full(c));
        Button connect=UiKit.button(c,"اختبار واتصال");connect.setOnClickListener(v->provision());form.addView(connect,UiKit.full(c));root.addView(form,UiKit.full(c));

        loadRemembered();
    }

    public View view(){return scroll;}

    /**
     * Intentionally no automatic network/status polling here.
     * MainActivity refreshes visible tabs at telemetry rate (4 Hz); polling from this method made
     * this setup screen continuously relayout/flicker. Status and Wi-Fi scans are user-triggered.
     */
    public void refresh(){ }

    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFFB0BEC5);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}

    private void loadRemembered(){RemoteWifiCredentialStore.Credentials x=new RemoteWifiCredentialStore(context,host.preferences()).load();if(x.present()){ssid.setText(x.ssid);password.setText(x.password);remember.setChecked(true);}else{remember.setChecked(false);host.showToast("لا توجد بيانات شبكة محفوظة في الهاتف");}}

    private void refreshStatus(){
        if(statusInProgress)return;
        statusInProgress=true;refreshButton.setEnabled(false);refreshButton.setText("جارٍ التحديث...");
        host.io().execute(()->{
            try{
                JSONObject j=host.robotApi().remoteStatus();boolean pending=j.optBoolean("provision_pending",false);long remain=j.optLong("provision_remaining_ms",0);
                String active=j.optString("active_ssid",j.optString("ssid","")),shown=j.optString("ssid","");
                String text=(pending?"اختبار شبكة جديدة: "+dash(shown)+"\nالشبكة المحفوظة: "+dash(active)+"   •   مهلة: "+String.format(java.util.Locale.US,"%.1f s",remain/1000.0):"SSID: "+dash(shown))+"\n"+
                        "Wi-Fi: "+yes(j.optBoolean("wifi",false))+"   •   IP: "+dash(j.optString("ip",""))+"\n"+
                        "RSSI: "+j.optInt("rssi",0)+" dBm   •   Internet: "+yes(j.optBoolean("internet",false))+"\n"+
                        "TLS: "+yes(j.optBoolean("tls",false))+"   •   MQTT: "+yes(j.optBoolean("mqtt",false))+"   •   Telemetry: "+yes(j.optBoolean("telemetry",false))+
                        (pending?"\nلن يُحفظ الإعداد الجديد إلا بعد نجاح اختبار الإنترنت؛ وإلا سيتم Rollback تلقائيًا.":"");
                root.post(()->{statusInProgress=false;refreshButton.setEnabled(true);refreshButton.setText("تحديث الحالة");status.setText(text);status.setTextColor(pending?0xFFFFB74D:j.optBoolean("internet",false)?0xFF81C784:0xFFFFB74D);});
            }catch(Exception e){root.post(()->{statusInProgress=false;refreshButton.setEnabled(true);refreshButton.setText("تحديث الحالة");status.setText("تعذر الوصول إلى إعداد الشبكة. اتصل بشبكة HazRob المحلية ثم أعد المحاولة.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private void scanNetworks(){
        if(scanInProgress)return;
        scanInProgress=true;
        final long started=SystemClock.elapsedRealtime();
        scanButton.setEnabled(false);scanButton.setText("جارٍ البحث... 3 ثوانٍ");
        status.setText("جارٍ تنفيذ بحث واحد عن الشبكات التي يراها الروبوت...");status.setTextColor(0xFFFFB74D);
        host.io().execute(()->{
            try{
                JSONObject j=host.robotApi().remoteScan();JSONArray a=j.optJSONArray("networks");List<String> labels=new ArrayList<>(),ssids=new ArrayList<>();
                if(a!=null)for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n==null)continue;String s=n.optString("ssid","").trim();if(s.isEmpty()||ssids.contains(s))continue;ssids.add(s);labels.add((n.optBoolean("secure",true)?"🔒 ":"○ ")+s+"   "+n.optInt("rssi",0)+" dBm");}
                long elapsed=SystemClock.elapsedRealtime()-started;long wait=Math.max(0L,MIN_SCAN_DISPLAY_MS-elapsed);
                root.postDelayed(()->{networkSsids.clear();networkSsids.addAll(ssids);networkAdapter.clear();networkAdapter.addAll(labels);networkAdapter.notifyDataSetChanged();scanInProgress=false;scanButton.setEnabled(true);scanButton.setText("بحث عن الشبكات");status.setText(labels.isEmpty()?"انتهى البحث: لم يعثر الروبوت على شبكات":"انتهى البحث: تم العثور على "+labels.size()+" شبكة. لن يتكرر البحث إلا عند الضغط على الزر مرة أخرى.");status.setTextColor(labels.isEmpty()?0xFFFFB74D:0xFF81C784);},wait);
            }catch(Exception e){
                long elapsed=SystemClock.elapsedRealtime()-started;long wait=Math.max(0L,MIN_SCAN_DISPLAY_MS-elapsed);
                root.postDelayed(()->{scanInProgress=false;scanButton.setEnabled(true);scanButton.setText("بحث عن الشبكات");status.setText("انتهى البحث دون نتائج. يجب أن يكون الهاتف متصلًا بشبكة HazRob المحلية.");status.setTextColor(0xFFFF7043);},wait);
            }
        });
    }

    private void selectNetwork(){int p=networks.getSelectedItemPosition();if(p<0||p>=networkSsids.size()){host.showToast("اختر شبكة من نتائج الاستكشاف");return;}ssid.setText(networkSsids.get(p));password.requestFocus();}

    private void provision(){
        final String s=ssid.getText().toString().trim(),p=password.getText().toString();final boolean save=remember.isChecked();if(s.isEmpty()){host.showToast("أدخل اسم الشبكة");return;}
        status.setText("جارٍ إرسال الشبكة للاختبار دون حذف الإعداد السابق...");
        host.io().execute(()->{
            try{
                host.robotApi().remoteProvision(s,p);RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(context,host.preferences());if(save)store.save(s,p);else store.clear();
                root.post(()->{status.setText("بدأ اختبار الشبكة الجديدة. استخدم «تحديث الحالة» بعد نحو 25 ثانية؛ سيعتمد الروبوت الشبكة عند نجاح الإنترنت أو يعود تلقائيًا إلى الإعداد السابق عند الفشل.");status.setTextColor(0xFFFFB74D);});
            }catch(Exception e){root.post(()->{status.setText("فشل إرسال إعداد الشبكة. تأكد من اتصال الهاتف بشبكة HazRob.");status.setTextColor(0xFFFF7043);});}
        });
    }

    private static String yes(boolean b){return b?"● نعم":"○ لا";}
    private static String dash(String s){return s==null||s.trim().isEmpty()?"—":s;}
}
