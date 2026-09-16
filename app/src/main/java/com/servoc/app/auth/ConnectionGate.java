package com.servoc.app.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.servoc.app.R;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.camera.CameraApi;
import com.servoc.app.communication.LocalNetworkContract;
import com.servoc.app.communication.RemoteReadiness;
import com.servoc.app.communication.RemoteServiceProfile;
import com.servoc.app.communication.RemoteSessionManager;
import com.servoc.app.communication.RemoteWifiCredentialStore;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.communication.WifiRoute;
import com.servoc.app.ui.UiKit;

import java.util.concurrent.ExecutorService;

/** HazRob transport selector. Direct and Remote are mutually exclusive control authorities. */
public final class ConnectionGate {
    public enum Mode{DIRECT,REMOTE}
    public interface Listener{void onReady(Mode mode);}
    private static final int WIFI_PERMISSION_REQUEST=7301;

    private final Activity a;
    private final RobotApi local;
    private final SharedPreferences prefs;
    private final ExecutorService io;
    private final boolean developer;
    private final Listener listener;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final LinearLayout root,content;
    private final TextView status;
    private final ProgressBar progress;
    private int remoteAttempt=0;

    public ConnectionGate(Activity a,RobotApi local,SharedPreferences prefs,ExecutorService io,boolean developer,Listener listener){
        this.a=a;this.local=local;this.prefs=prefs;this.io=io;this.developer=developer;this.listener=listener;
        RemoteServiceProfile.ensureDefaults(prefs);
        root=UiKit.vbox(a);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(UiKit.dp(a,24),UiKit.dp(a,18),UiKit.dp(a,24),UiKit.dp(a,14));root.setBackgroundColor(Color.WHITE);
        ImageView robot=new ImageView(a);robot.setImageResource(R.drawable.ic_hazrob_app);robot.setScaleType(ImageView.ScaleType.CENTER_INSIDE);robot.setAdjustViewBounds(true);robot.setContentDescription("HazRob");
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,0,1f);imageParams.setMargins(0,0,0,UiKit.dp(a,12));root.addView(robot,imageParams);
        content=UiKit.vbox(a);root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(10);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,UiKit.dp(a,10)));
        status=UiKit.small(a,"");status.setGravity(Gravity.CENTER);status.setTextColor(0xFF245A7A);status.setBackgroundColor(Color.TRANSPARENT);root.addView(status,UiKit.full(a));
        TextView footer=UiKit.small(a,"HazRob "+ReleaseAudit.VERSION+" • إعداد المشروع/ "+ReleaseAudit.PROJECT_PREPARERS);footer.setGravity(Gravity.CENTER);footer.setTextColor(0xFF616161);footer.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams fp=UiKit.full(a);fp.topMargin=UiKit.dp(a,10);root.addView(footer,fp);
        showChoice();
    }

    public View view(){return root;}

    private void showChoice(){
        remoteAttempt++;content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        Button direct=UiKit.button(a,"التحكم المباشر عبر Wi-Fi");direct.setTextSize(17);direct.setOnClickListener(v->enterDirect());content.addView(direct,new LinearLayout.LayoutParams(-1,UiKit.dp(a,60)));
        Button remote=UiKit.button(a,"التحكم عبر الإنترنت");remote.setTextSize(17);remote.setOnClickListener(v->openRemote());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,60));rp.topMargin=UiKit.dp(a,10);content.addView(remote,rp);
        if(developer){Button provision=UiKit.button(a,"تهيئة إنترنت الروبوت");provision.setTextSize(15);provision.setOnClickListener(v->openProvisioning());LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,54));pp.topMargin=UiKit.dp(a,10);content.addView(provision,pp);}
    }

    private boolean ensureWifiPermission(){
        if(WifiRoute.hasRequiredPermission(a))return true;
        String p=WifiRoute.requiredRuntimePermission();
        if(!p.isEmpty()){
            try{a.requestPermissions(new String[]{p},WIFI_PERMISSION_REQUEST);}catch(Exception ignored){}
            status.setText("يلزم إذن الاتصال بأجهزة Wi-Fi القريبة للمحافظة على شبكة HazRob. امنح الإذن ثم اضغط التحكم المباشر مرة أخرى.");status.setTextColor(0xFF245A7A);
        }
        return false;
    }

    private void enterDirect(){
        if(!ensureWifiPermission())return;
        remoteAttempt++;busy("طلب شبكة HazRob المحلية والتحقق من ESP32 والكاميرا...",1);
        io.execute(()->{
            prefs.edit().putString("robot",LocalNetworkContract.ROBOT_HOST).putString("camera",LocalNetworkContract.CAMERA_HOST).putString("connection_mode","direct").apply();
            local.setRobotHost(LocalNetworkContract.ROBOT_HOST);try{new RemoteSessionManager(a,prefs).close();}catch(Exception ignored){}
            CameraApi directCamera=new CameraApi(LocalNetworkContract.CAMERA_HOST);
            boolean network=WifiRoute.waitForRobotWifi(a,30000);boolean robotReady=false,accepted=false;String detail="";
            if(!network)detail="لم يمنح Android التطبيق شبكة HazRob المحلية";
            else{
                boolean switchNeeded=true;
                try{switchNeeded=!"direct".equalsIgnoreCase(local.remoteStatus().optString("radio_profile",""));}catch(Exception ignored){}
                if(switchNeeded){
                    try{
                        local.postForm("/connection/direct","",1500);
                        detail="تم طلب التحويل إلى Direct؛ إعادة امتلاك شبكة HazRob...";
                        WifiRoute.releaseRobotWifi(a);
                        try{Thread.sleep(450);}catch(InterruptedException x){Thread.currentThread().interrupt();}
                        WifiRoute.restartRobotWifiRequest(a);
                        network=WifiRoute.waitForRobotWifi(a,15000);
                    }catch(Exception e){network=false;detail="تم الوصول إلى الشبكة لكن تعذر تثبيت وضع Direct في الروبوت";}
                }
                if(network){
                    long robotDeadline=SystemClock.elapsedRealtime()+12000L;
                    while(SystemClock.elapsedRealtime()<robotDeadline&&!robotReady){
                        try{
                            WifiRoute.maintainRobotWifi(a);
                            String banner=local.getText("/",1200);
                            if(LocalNetworkContract.isExpectedFirmwareBanner(banner,ReleaseAudit.VERSION)){robotReady=true;detail="ESP32 الرئيسي Direct؛ انتظار ESP32-CAM على 192.168.4.2...";break;}
                            String got=banner==null?"":banner.trim();detail=got.isEmpty()?"وصل رد فارغ من 192.168.4.1":"تم الوصول إلى 192.168.4.1 لكن هوية البرنامج هي: "+got;break;
                        }catch(Exception e){detail="إعادة انتظار HazRob على 192.168.4.1 بعد استقرار نقطة الوصول...";try{Thread.sleep(500);}catch(InterruptedException x){Thread.currentThread().interrupt();break;}}
                    }
                }
                if(network&&robotReady){
                    long cameraDeadline=SystemClock.elapsedRealtime()+20000L;
                    while(SystemClock.elapsedRealtime()<cameraDeadline&&!accepted){
                        WifiRoute.maintainRobotWifi(a);
                        if(directCamera.directReady()){
                            accepted=true;detail="Direct جاهز: ESP32 192.168.4.1 + ESP32-CAM 192.168.4.2 + MJPEG :81";break;
                        }
                        detail="ESP32 الرئيسي جاهز؛ انتظار الكاميرا المحلية: "+directCamera.lastProbe();
                        try{Thread.sleep(650);}catch(InterruptedException x){Thread.currentThread().interrupt();break;}
                    }
                    if(!accepted)detail="ESP32 الرئيسي Direct لكن ESP32-CAM لم تصبح جاهزة للبث المحلي: "+directCamera.lastProbe();
                }
            }
            final boolean ready=accepted;final String result=detail;ui.post(()->{if(ready){status.setText(result);status.setTextColor(0xFF2E7D32);listener.onReady(Mode.DIRECT);}else showDirectRecovery(result);});
        });
    }

    private void showDirectRecovery(String detail){
        content.removeAllViews();progress.setVisibility(View.GONE);String reason=detail==null||detail.trim().isEmpty()?"تعذر التحقق من الاتصال المباشر":detail.trim();
        status.setText(reason+"\nوافق على طلب Android للاتصال بشبكة HazRob. يجب أن يستجيب ESP32 على 192.168.4.1 والكاميرا على 192.168.4.2 قبل فتح القيادة.");status.setTextColor(0xFFD84315);
        Button wifi=UiKit.button(a,"فتح إعدادات Wi-Fi");wifi.setOnClickListener(v->openWifiSettings());content.addView(wifi,UiKit.full(a));Button retry=UiKit.button(a,"إعادة الاختبار");retry.setOnClickListener(v->enterDirect());content.addView(retry,UiKit.full(a));Button back=UiKit.button(a,"رجوع");back.setOnClickListener(v->showChoice());content.addView(back,UiKit.full(a));
    }

    private void openWifiSettings(){try{a.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));status.setText("اختر شبكة HazRob، ثم ارجع إلى التطبيق واضغط إعادة الاختبار.");status.setTextColor(0xFF245A7A);}catch(Exception e){status.setText("تعذر فتح إعدادات Wi-Fi تلقائيًا. افتح إعدادات Wi-Fi واتصل بشبكة HazRob ثم أعد الاختبار.");status.setTextColor(0xFFD84315);}}

    private void openRemote(){
        prefs.edit().putString("remote_operator_role","operator").apply();
        busy("تحويل سلطة الاتصال إلى Remote...",1);
        io.execute(()->{
            // If the phone is currently on HazRob, explicitly switch the robot to REMOTE before
            // releasing the local-only NetworkRequest. If it is already remote, this is harmless.
            try{if(WifiRoute.bindRobotWifi(a))local.postForm("/connection/remote","",1500);}catch(Exception ignored){}
            WifiRoute.releaseRobotWifi(a);
            ui.post(()->connectRemote(new RemoteSessionManager(a,prefs)));
        });
    }

    private void connectRemote(RemoteSessionManager cloud){
        final int ticket=++remoteAttempt;busyRemote(cloud,ticket,"بدء الاتصال البعيد...",1);
        io.execute(()->{
            RemoteReadiness r=cloud.probe((text,stage)->ui.post(()->{if(ticket==remoteAttempt){progress.setProgress(Math.max(0,Math.min(10,stage)));status.setText(text);status.setTextColor(0xFF245A7A);}}));
            ui.post(()->{if(ticket!=remoteAttempt)return;render(r);if(r.ready)finishRemote(r);else{status.append("\n\nفشل اكتمال التحكم البعيد: "+r.detail+"\nإذا كان الروبوت في Direct، اتصل محليًا مرة واحدة واختر Remote أو أعد تهيئة WAN.");status.setTextColor(0xFFEF6C00);showBack();}});
        });
    }

    private void busyRemote(RemoteSessionManager cloud,int ticket,String text,int stage){
        content.removeAllViews();progress.setVisibility(View.VISIBLE);progress.setProgress(stage);status.setText(text);status.setTextColor(0xFF245A7A);
        Button cancel=UiKit.button(a,"إلغاء الاتصال");cancel.setOnClickListener(v->{if(ticket!=remoteAttempt)return;remoteAttempt++;showChoice();io.execute(cloud::close);});content.addView(cancel,UiKit.full(a));
    }

    private void openProvisioning(){if(!developer){showChoice();return;}showProvision();}
    private void showProvision(){
        remoteAttempt++;content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");TextView t=UiKit.title(a,"شبكة الإنترنت الخارجية للروبوت");t.setGravity(Gravity.CENTER);t.setTextColor(0xFF263238);t.setBackgroundColor(Color.TRANSPARENT);content.addView(t,UiKit.full(a));
        RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(a,prefs);RemoteWifiCredentialStore.Credentials saved=store.load();EditText ssid=UiKit.input(a,saved.ssid);ssid.setHint("اسم الشبكة / SSID");content.addView(ssid,UiKit.full(a));EditText pass=UiKit.input(a,saved.password);pass.setHint("كلمة السر");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(pass,UiKit.full(a));CheckBox remember=new CheckBox(a);remember.setText("تذكرني");remember.setChecked(saved.present());remember.setTextColor(0xFF263238);content.addView(remember,UiKit.full(a));
        TextView note=UiKit.small(a,"أدخل SSID يدويًا. لا نجري Wi-Fi scan أثناء Direct حتى لا نهز قناة HazRob. بعد إرسال البيانات يتحول الروبوت إلى Remote.");note.setTextColor(0xFF546E7A);note.setBackgroundColor(Color.TRANSPARENT);content.addView(note,UiKit.full(a));
        Button connect=UiKit.button(a,"دخول");connect.setOnClickListener(v->{String s=ssid.getText().toString().trim(),p=pass.getText().toString();if(s.isEmpty()){status.setText("أدخل اسم الشبكة");status.setTextColor(0xFFD84315);return;}provision(s,p,remember.isChecked());});content.addView(connect,UiKit.full(a));Button back=UiKit.button(a,"رجوع");back.setOnClickListener(v->showChoice());content.addView(back,UiKit.full(a));
    }

    private void provision(String ssid,String password,boolean remember){
        if(!ensureWifiPermission())return;
        busy("إرسال إعدادات الشبكة إلى الروبوت...",1);io.execute(()->{try{
            local.setRobotHost(LocalNetworkContract.ROBOT_HOST);
            if(!WifiRoute.waitForRobotWifi(a,30000))throw new IllegalStateException("HazRob Wi-Fi unavailable");
            local.remoteProvision(ssid,password);
            RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(a,prefs);if(remember)store.save(ssid,password);else store.clear();prefs.edit().putBoolean("remote_wan_provisioned",true).apply();
            ui.post(()->{status.setText("تم إرسال الإعدادات وتحويل الروبوت إلى Remote. أعد الهاتف إلى الإنترنت المعتاد أو البيانات الخلوية.");status.setTextColor(0xFF245A7A);});
            WifiRoute.releaseRobotWifi(a);pollRobotRemote();
        }catch(Exception e){ui.post(()->{status.setText("تعذر إرسال الإعدادات. اتصل بشبكة HazRob واسمح للتطبيق باستخدام Wi-Fi القريب ثم حاول مرة أخرى.");status.setTextColor(0xFFD84315);showBack();});}});
    }

    private void pollRobotRemote(){
        RemoteSessionManager cloud=new RemoteSessionManager(a,prefs);long deadline=SystemClock.elapsedRealtime()+45000L;
        while(SystemClock.elapsedRealtime()<deadline){RemoteReadiness r=cloud.probe();ui.post(()->render(r));if(r.ready){ui.post(()->finishRemote(r));return;}try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}}
        ui.post(()->{status.setText("تم حفظ إعدادات الشبكة، لكن التحكم البعيد لم يصبح جاهزًا خلال دورة التحقق. راجع مرحلة الفشل في شاشة الحالة ثم أعد المحاولة.");status.setTextColor(0xFFEF6C00);showBack();});
    }

    private void render(RemoteReadiness r){
        progress.setVisibility(View.VISIBLE);progress.setProgress(Math.min(progress.getMax(),r.completedCoreStages()));StringBuilder s=new StringBuilder();stage(s,"إعداد التحكم",r.configured);stage(s,"Wi-Fi",r.wifi);stage(s,"IP",r.ip);stage(s,"DNS",r.dns);stage(s,"الوقت",r.time);stage(s,"الإنترنت",r.internet);stage(s,"TLS",r.tls);stage(s,"MQTT",r.mqtt);stage(s,"قناة الأوامر",r.commandAck);stage(s,"Telemetry",r.telemetry);s.append("\n• الفيديو البعيد مستقل ولا يمنع دخول التحكم");if(!r.detail.isEmpty())s.append("\n• ").append(r.detail);status.setText(s.toString().trim());status.setTextColor(r.ready?0xFF2E7D32:0xFF245A7A);
    }
    private static void stage(StringBuilder s,String name,boolean ok){if(s.length()>0)s.append('\n');s.append(ok?"✓ ":"○ ").append(name);}

    private void finishRemote(RemoteReadiness control){WifiRoute.releaseRobotWifi(a);prefs.edit().putString("connection_mode","remote").apply();status.append("\nتم اتصال التحكم والقياسات. سيظهر الفيديو البعيد بصورة مستقلة عند جاهزية ESP32-CAM.");status.setTextColor(0xFF2E7D32);status.postDelayed(()->listener.onReady(Mode.REMOTE),250);}
    private void busy(String text,int stage){content.removeAllViews();progress.setVisibility(View.VISIBLE);progress.setProgress(stage);status.setText(text);status.setTextColor(0xFF245A7A);}
    private void showBack(){content.removeAllViews();Button b=UiKit.button(a,"رجوع");b.setOnClickListener(v->showChoice());content.addView(b,UiKit.full(a));}
}
