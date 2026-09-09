package com.servoc.app.auth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
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
import com.servoc.app.camera.RemoteVideoReadiness;
import com.servoc.app.communication.RemoteReadiness;
import com.servoc.app.communication.RemoteServiceProfile;
import com.servoc.app.communication.RemoteSessionManager;
import com.servoc.app.communication.RemoteWifiCredentialStore;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.ui.UiKit;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;

/**
 * HazRob transport selector. Direct and Remote are two transports for the same robot API.
 * No operator authorization code is required for Remote mode.
 */
public final class ConnectionGate {
    public enum Mode{DIRECT,REMOTE}
    public interface Listener{void onReady(Mode mode);}

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

    public ConnectionGate(Activity a,RobotApi local,SharedPreferences prefs,ExecutorService io,boolean developer,Listener listener){
        this.a=a;this.local=local;this.prefs=prefs;this.io=io;this.developer=developer;this.listener=listener;
        RemoteServiceProfile.ensureDefaults(prefs);
        root=UiKit.vbox(a);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(UiKit.dp(a,24),UiKit.dp(a,18),UiKit.dp(a,24),UiKit.dp(a,14));root.setBackgroundColor(Color.WHITE);

        ImageView robot=new ImageView(a);robot.setImageResource(R.drawable.ic_hazrob_app);robot.setScaleType(ImageView.ScaleType.CENTER_INSIDE);robot.setAdjustViewBounds(true);robot.setContentDescription("HazRob");
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,0,1f);imageParams.setMargins(0,0,0,UiKit.dp(a,12));root.addView(robot,imageParams);

        content=UiKit.vbox(a);root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(11);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,UiKit.dp(a,10)));
        status=UiKit.small(a,"");status.setGravity(Gravity.CENTER);status.setTextColor(0xFF245A7A);status.setBackgroundColor(Color.TRANSPARENT);root.addView(status,UiKit.full(a));

        TextView footer=UiKit.small(a,"HazRob "+ReleaseAudit.VERSION+" • إعداد المشروع/ "+ReleaseAudit.PROJECT_PREPARERS);footer.setGravity(Gravity.CENTER);footer.setTextColor(0xFF616161);footer.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams fp=UiKit.full(a);fp.topMargin=UiKit.dp(a,10);root.addView(footer,fp);
        showChoice();
    }

    public View view(){return root;}

    private void showChoice(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        Button direct=UiKit.button(a,"التحكم المباشر عبر Wi-Fi");direct.setTextSize(17);direct.setOnClickListener(v->verifyDirect());content.addView(direct,new LinearLayout.LayoutParams(-1,UiKit.dp(a,60)));
        Button remote=UiKit.button(a,"التحكم عبر الإنترنت");remote.setTextSize(17);remote.setOnClickListener(v->openRemote());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,60));rp.topMargin=UiKit.dp(a,10);content.addView(remote,rp);
        if(developer){
            Button provision=UiKit.button(a,"تهيئة إنترنت الروبوت");provision.setTextSize(15);provision.setOnClickListener(v->openProvisioning());LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,54));pp.topMargin=UiKit.dp(a,10);content.addView(provision,pp);
        }
    }

    private void verifyDirect(){
        busy("فحص الاتصال المباشر...",1);
        io.execute(()->{
            try{
                JSONObject d=local.diagnosticsRaw();int api=d.optInt("api",d.optInt("api_version",0));if(api<3)throw new IllegalStateException("API");
                new RemoteSessionManager(a,prefs).close();
                prefs.edit().putString("connection_mode","direct").apply();
                ui.post(()->listener.onReady(Mode.DIRECT));
            }catch(Exception e){ui.post(()->{status.setText("تعذر الوصول إلى HazRob عبر Wi-Fi");status.setTextColor(0xFFD84315);showBack();});}
        });
    }

    /** Remote mode uses the same operator command set as Direct mode; no HZR2/guest token is requested. */
    private void openRemote(){
        prefs.edit().putString("remote_operator_role","operator").apply();
        connectRemote(new RemoteSessionManager(a,prefs));
    }

    private void connectRemote(RemoteSessionManager cloud){
        busy("الاتصال بالروبوت عبر الإنترنت...",6);
        io.execute(()->{
            RemoteReadiness r=cloud.probe();
            ui.post(()->{
                render(r);
                if(r.ready)finishRemote(r);
                else{status.append("\nالروبوت غير متصل بالإنترنت أو قناة التحكم البعيد غير جاهزة");status.setTextColor(0xFFEF6C00);showBack();}
            });
        });
    }

    private void openProvisioning(){if(!developer){showChoice();return;}showProvision();}

    private void showProvision(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        TextView t=UiKit.title(a,"شبكة الإنترنت الخارجية للروبوت");t.setGravity(Gravity.CENTER);t.setTextColor(0xFF263238);t.setBackgroundColor(Color.TRANSPARENT);content.addView(t,UiKit.full(a));

        RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(a,prefs);RemoteWifiCredentialStore.Credentials saved=store.load();
        EditText ssid=UiKit.input(a,saved.ssid);ssid.setHint("اسم الشبكة / SSID");content.addView(ssid,UiKit.full(a));
        EditText pass=UiKit.input(a,saved.password);pass.setHint("كلمة السر");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(pass,UiKit.full(a));
        CheckBox remember=new CheckBox(a);remember.setText("تذكرني");remember.setChecked(saved.present());remember.setTextColor(0xFF263238);content.addView(remember,UiKit.full(a));

        TextView note=UiKit.small(a,"يمكن استخدام الإعداد المحفوظ أو إدخال شبكة جديدة. إرسال بيانات الشبكة يحتاج اتصالًا مؤقتًا بشبكة HazRob المحلية فقط؛ بعدها يتصل الروبوت بالشبكة الخارجية.");note.setTextColor(0xFF546E7A);note.setBackgroundColor(Color.TRANSPARENT);content.addView(note,UiKit.full(a));

        Button connect=UiKit.button(a,"دخول");connect.setOnClickListener(v->{String s=ssid.getText().toString().trim(),p=pass.getText().toString();if(s.isEmpty()){status.setText("أدخل اسم الشبكة");status.setTextColor(0xFFD84315);return;}provision(s,p,remember.isChecked());});content.addView(connect,UiKit.full(a));
        Button back=UiKit.button(a,"رجوع");back.setOnClickListener(v->showChoice());content.addView(back,UiKit.full(a));
    }

    private void provision(String ssid,String password,boolean remember){
        busy("إرسال إعدادات الشبكة إلى الروبوت...",1);
        io.execute(()->{
            try{
                local.remoteProvision(ssid,password);
                RemoteWifiCredentialStore store=new RemoteWifiCredentialStore(a,prefs);if(remember)store.save(ssid,password);else store.clear();
                prefs.edit().putBoolean("remote_wan_provisioned",true).apply();
                ui.post(()->{status.setText("تم إرسال الإعدادات. الروبوت ينتقل الآن إلى الشبكة الخارجية؛ أعد الهاتف إلى شبكة الإنترنت المعتادة أو البيانات الخلوية.");status.setTextColor(0xFF245A7A);});
                pollRobotRemote();
            }catch(Exception e){ui.post(()->{status.setText("تعذر إرسال الإعدادات. اتصل مؤقتًا بشبكة HazRob المحلية ثم حاول مرة أخرى.");status.setTextColor(0xFFD84315);showBack();});}
        });
    }

    private void pollRobotRemote(){
        RemoteSessionManager cloud=new RemoteSessionManager(a,prefs);
        for(int i=0;i<90;i++){
            RemoteReadiness r=cloud.probe();ui.post(()->render(r));
            if(r.ready){ui.post(()->finishRemote(r));return;}
            try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
        ui.post(()->{status.setText("تم حفظ إعدادات الشبكة، لكن الروبوت لم يظهر عبر الإنترنت بعد. تحقق من الشبكة الخارجية ثم أعد المحاولة.");status.setTextColor(0xFFEF6C00);showBack();});
    }

    private void render(RemoteReadiness r){
        progress.setVisibility(View.VISIBLE);progress.setProgress(r.completedCoreStages());StringBuilder s=new StringBuilder();
        stage(s,"إعداد الشبكة",r.configured);stage(s,"Wi-Fi",r.wifi);stage(s,"IP",r.ip);stage(s,"DNS",r.dns);stage(s,"الإنترنت/الوقت",r.internet&&r.time);stage(s,"TLS",r.tls);stage(s,"MQTT",r.mqtt);stage(s,"قناة الأوامر",r.commandAck);stage(s,"Telemetry",r.telemetry);stage(s,"الفيديو البعيد",r.camera);
        status.setText(s.toString().trim());status.setTextColor(r.ready&&r.camera?0xFF2E7D32:0xFF245A7A);
    }

    private static void stage(StringBuilder s,String name,boolean ok){if(s.length()>0)s.append('\n');s.append(ok?"✓ ":"○ ").append(name);}

    private void finishRemote(RemoteReadiness control){
        busy("فحص الفيديو البعيد...",10);
        io.execute(()->{
            RemoteVideoReadiness.Result v=RemoteVideoReadiness.probe(a,prefs);
            RemoteReadiness full=new RemoteReadiness(control.configured,control.wifi,control.ip,control.dns,control.time,control.internet,control.tls,control.mqtt,control.commandAck,control.telemetry,v.ready,control.ready&&v.ready,v.detail);
            ui.post(()->{
                render(full);
                if(!full.ready){status.append("\n"+v.detail);status.setTextColor(0xFFEF6C00);showBack();return;}
                prefs.edit().putString("connection_mode","remote").apply();
                status.postDelayed(()->listener.onReady(Mode.REMOTE),350);
            });
        });
    }

    private void busy(String text,int stage){content.removeAllViews();progress.setVisibility(View.VISIBLE);progress.setProgress(stage);status.setText(text);status.setTextColor(0xFF245A7A);}
    private void showBack(){content.removeAllViews();Button b=UiKit.button(a,"رجوع");b.setOnClickListener(v->showChoice());content.addView(b,UiKit.full(a));}
}
