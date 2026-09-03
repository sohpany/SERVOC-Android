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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.servoc.app.ReleaseAudit;
import com.servoc.app.communication.RemoteReadiness;
import com.servoc.app.communication.RemoteSessionManager;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.ui.UiKit;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;

/** First gate in HazRob 0.9: select and verify transport before role selection. */
public final class ConnectionGate {
    public enum Mode{DIRECT,REMOTE}
    public interface Listener{void onReady(Mode mode);}

    private final Activity a;private final RobotApi local;private final SharedPreferences prefs;private final ExecutorService io;private final Listener listener;private final Handler ui=new Handler(Looper.getMainLooper());
    private final LinearLayout root,content;private final TextView status;private final ProgressBar progress;

    public ConnectionGate(Activity a,RobotApi local,SharedPreferences prefs,ExecutorService io,Listener listener){
        this.a=a;this.local=local;this.prefs=prefs;this.io=io;this.listener=listener;
        root=UiKit.vbox(a);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(UiKit.dp(a,24),UiKit.dp(a,34),UiKit.dp(a,24),UiKit.dp(a,24));root.setBackgroundColor(0xFF0A0D11);
        TextView title=UiKit.title(a,"HazRob "+ReleaseAudit.VERSION);title.setGravity(Gravity.CENTER);title.setTextColor(Color.WHITE);root.addView(title,UiKit.full(a));
        content=UiKit.vbox(a);root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(10);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,UiKit.dp(a,12)));
        status=UiKit.small(a,"");status.setGravity(Gravity.CENTER);status.setTextColor(0xFF90CAF9);root.addView(status,UiKit.full(a));
        showChoice();
    }
    public View view(){return root;}

    private void showChoice(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        Button direct=UiKit.button(a,"التحكم المباشر عبر Wi-Fi");direct.setTextSize(17);direct.setOnClickListener(v->verifyDirect());content.addView(direct,new LinearLayout.LayoutParams(-1,UiKit.dp(a,64)));
        Button remote=UiKit.button(a,"التحكم عبر الإنترنت");remote.setTextSize(17);remote.setOnClickListener(v->openRemote());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,64));rp.topMargin=UiKit.dp(a,16);content.addView(remote,rp);
    }

    private void verifyDirect(){
        busy("فحص الاتصال المباشر...",1);io.execute(()->{try{JSONObject d=local.diagnosticsRaw();int api=d.optInt("api",d.optInt("api_version",0));if(api<3)throw new IllegalStateException("API");prefs.edit().putString("connection_mode","direct").apply();ui.post(()->listener.onReady(Mode.DIRECT));}catch(Exception e){ui.post(()->{status.setText("تعذر الوصول إلى HazRob عبر Wi-Fi");status.setTextColor(0xFFFF7043);showBack();});}});
    }

    private void openRemote(){
        busy("فحص إعداد الاتصال البعيد...",1);io.execute(()->{
            try{RemoteReadiness r=RemoteReadiness.fromRobot(local.remoteStatus());ui.post(()->{if(!r.configured)showProvision();else monitorProvisioned(r);});}
            catch(Exception localError){RemoteSessionManager cloud=new RemoteSessionManager(prefs);RemoteReadiness r=cloud.probe();ui.post(()->{if(r.ready)finishRemote(r);else{status.setText("لا يوجد مسار بعيد جاهز");status.setTextColor(0xFFFFB74D);showBack();}});}
        });
    }

    private void showProvision(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        TextView t=UiKit.title(a,"إعداد مودم المركبة");t.setGravity(Gravity.CENTER);content.addView(t,UiKit.full(a));
        EditText ssid=UiKit.input(a,"");ssid.setHint("اسم شبكة المودم");content.addView(ssid,UiKit.full(a));
        EditText pass=UiKit.input(a,"");pass.setHint("كلمة المرور");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(pass,UiKit.full(a));
        Button connect=UiKit.button(a,"اتصال");connect.setOnClickListener(v->{String s=ssid.getText().toString().trim(),p=pass.getText().toString();if(s.isEmpty()){status.setText("أدخل اسم الشبكة");return;}provision(s,p);});content.addView(connect,UiKit.full(a));
        Button back=UiKit.button(a,"رجوع");back.setOnClickListener(v->showChoice());content.addView(back,UiKit.full(a));
    }

    private void provision(String ssid,String password){
        busy("إرسال إعدادات الشبكة...",1);io.execute(()->{try{local.remoteProvision(ssid,password);prefs.edit().putBoolean("remote_wan_provisioned",true).apply();pollRobotRemote();}catch(Exception e){ui.post(()->{status.setText("تعذر إرسال إعدادات المودم");status.setTextColor(0xFFFF7043);showBack();});}});
    }

    private void pollRobotRemote(){
        for(int i=0;i<40;i++){
            try{RemoteReadiness r=RemoteReadiness.fromRobot(local.remoteStatus());ui.post(()->render(r));if(r.ready){ui.post(()->finishRemote(r));return;}if(r.internet&&!r.mqtt){ui.post(()->status.setText("الإنترنت جاهز • بانتظار إعداد MQTT/TLS الآمن"));}}
            catch(Exception ignored){}
            try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
        ui.post(()->{status.setText("لم تكتمل جاهزية الاتصال البعيد");status.setTextColor(0xFFFFB74D);showBack();});
    }

    private void monitorProvisioned(RemoteReadiness first){render(first);if(first.ready){finishRemote(first);return;}io.execute(this::pollRobotRemote);}

    private void render(RemoteReadiness r){
        progress.setVisibility(View.VISIBLE);progress.setProgress(r.completedCoreStages());
        StringBuilder s=new StringBuilder();stage(s,"إعداد الشبكة",r.configured);stage(s,"Wi-Fi",r.wifi);stage(s,"IP",r.ip);stage(s,"DNS",r.dns);stage(s,"الإنترنت/الوقت",r.internet&&r.time);stage(s,"TLS",r.tls);stage(s,"MQTT",r.mqtt);stage(s,"قناة الأوامر",r.commandAck);stage(s,"Telemetry",r.telemetry);stage(s,"الكاميرا",r.camera);status.setText(s.toString().trim());status.setTextColor(r.ready?0xFF81C784:0xFF90CAF9);
    }
    private static void stage(StringBuilder s,String name,boolean ok){if(s.length()>0)s.append('\n');s.append(ok?"✓ ":"○ ").append(name);}
    private void finishRemote(RemoteReadiness r){prefs.edit().putString("connection_mode","remote").apply();render(r);status.postDelayed(()->listener.onReady(Mode.REMOTE),350);}
    private void busy(String text,int stage){content.removeAllViews();progress.setVisibility(View.VISIBLE);progress.setProgress(stage);status.setText(text);status.setTextColor(0xFF90CAF9);}
    private void showBack(){content.removeAllViews();Button b=UiKit.button(a,"رجوع");b.setOnClickListener(v->showChoice());content.addView(b,UiKit.full(a));}
}
