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
        this.a=a;this.local=local;this.prefs=prefs;this.io=io;this.listener=listener;RemoteServiceProfile.ensureDefaults(prefs);
        root=UiKit.vbox(a);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(UiKit.dp(a,24),UiKit.dp(a,18),UiKit.dp(a,24),UiKit.dp(a,14));root.setBackgroundColor(Color.WHITE);

        ImageView robot=new ImageView(a);robot.setImageResource(R.drawable.ic_hazrob_app);robot.setScaleType(ImageView.ScaleType.CENTER_INSIDE);robot.setAdjustViewBounds(true);robot.setContentDescription("HazRob");
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,0,1f);imageParams.setMargins(0,0,0,UiKit.dp(a,12));root.addView(robot,imageParams);

        content=UiKit.vbox(a);root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(11);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,UiKit.dp(a,10)));
        status=UiKit.small(a,"");status.setGravity(Gravity.CENTER);status.setTextColor(0xFF245A7A);status.setBackgroundColor(Color.TRANSPARENT);root.addView(status,UiKit.full(a));

        TextView footer=UiKit.small(a,"HazRob "+ReleaseAudit.VERSION+" • إعداد المستشار/ شهاب الصهباني");footer.setGravity(Gravity.CENTER);footer.setTextColor(0xFF616161);footer.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams fp=UiKit.full(a);fp.topMargin=UiKit.dp(a,10);root.addView(footer,fp);
        showChoice();
    }
    public View view(){return root;}

    private void showChoice(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");
        Button direct=UiKit.button(a,"التحكم المباشر عبر Wi-Fi");direct.setTextSize(17);direct.setOnClickListener(v->verifyDirect());content.addView(direct,new LinearLayout.LayoutParams(-1,UiKit.dp(a,62)));
        Button remote=UiKit.button(a,"التحكم عبر الإنترنت");remote.setTextSize(17);remote.setOnClickListener(v->openRemote());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,UiKit.dp(a,62));rp.topMargin=UiKit.dp(a,12);content.addView(remote,rp);
    }

    private void verifyDirect(){busy("فحص الاتصال المباشر...",1);io.execute(()->{try{JSONObject d=local.diagnosticsRaw();int api=d.optInt("api",d.optInt("api_version",0));if(api<3)throw new IllegalStateException("API");new RemoteSessionManager(a,prefs).close();prefs.edit().putString("connection_mode","direct").apply();ui.post(()->listener.onReady(Mode.DIRECT));}catch(Exception e){ui.post(()->{status.setText("تعذر الوصول إلى HazRob عبر Wi-Fi");status.setTextColor(0xFFD84315);showBack();});}});}

    private void openRemote(){
        busy("فحص الاتصال البعيد...",1);io.execute(()->{
            try{RemoteReadiness robot=RemoteReadiness.fromRobot(local.remoteStatus());ui.post(()->{if(!robot.configured)showProvision();else monitorProvisioned(robot);});}
            catch(Exception localError){RemoteReadiness cloud=new RemoteSessionManager(a,prefs).probe();ui.post(()->{if(cloud.ready)finishRemote(cloud);else{render(cloud);status.append("\nلا يوجد مسار بعيد جاهز");showBack();}});}
        });
    }

    private void showProvision(){
        content.removeAllViews();progress.setVisibility(View.GONE);status.setText("");TextView t=UiKit.title(a,"إعداد مودم المركبة");t.setGravity(Gravity.CENTER);t.setTextColor(0xFF263238);t.setBackgroundColor(Color.TRANSPARENT);content.addView(t,UiKit.full(a));
        EditText ssid=UiKit.input(a,"");ssid.setHint("اسم شبكة المودم");content.addView(ssid,UiKit.full(a));
        EditText pass=UiKit.input(a,"");pass.setHint("كلمة المرور");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(pass,UiKit.full(a));
        Button connect=UiKit.button(a,"اتصال");connect.setOnClickListener(v->{String s=ssid.getText().toString().trim(),p=pass.getText().toString();if(s.isEmpty()){status.setText("أدخل اسم الشبكة");return;}provision(s,p);});content.addView(connect,UiKit.full(a));Button back=UiKit.button(a,"رجوع");back.setOnClickListener(v->showChoice());content.addView(back,UiKit.full(a));
    }

    private void provision(String ssid,String password){busy("إرسال إعدادات الشبكة...",1);io.execute(()->{try{local.remoteProvision(ssid,password);prefs.edit().putBoolean("remote_wan_provisioned",true).apply();pollRobotRemote();}catch(Exception e){ui.post(()->{status.setText("تعذر إرسال إعدادات المودم");status.setTextColor(0xFFD84315);showBack();});}});}

    private void pollRobotRemote(){
        RemoteSessionManager cloud=new RemoteSessionManager(a,prefs);
        for(int i=0;i<45;i++){
            try{
                RemoteReadiness robot=RemoteReadiness.fromRobot(local.remoteStatus());ui.post(()->render(robot));
                if(robot.internet&&robot.mqtt&&cloud.serviceProfileConfigured()){
                    RemoteReadiness endToEnd=cloud.probe();RemoteReadiness combined=combine(robot,endToEnd);ui.post(()->render(combined));if(combined.ready){ui.post(()->finishRemote(combined));return;}
                }else if(robot.internet&&!cloud.serviceProfileConfigured())ui.post(()->status.setText("الإنترنت جاهز • بيانات دخول MQTT لم تُضبط بعد"));
            }catch(Exception ignored){}
            try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
        ui.post(()->{status.setText("لم تكتمل جاهزية الاتصال البعيد");status.setTextColor(0xFFEF6C00);showBack();});
    }

    private static RemoteReadiness combine(RemoteReadiness robot,RemoteReadiness cloud){boolean ready=robot.internet&&cloud.tls&&cloud.mqtt&&cloud.commandAck&&cloud.telemetry;return new RemoteReadiness(robot.configured,robot.wifi,robot.ip,robot.dns,robot.time,robot.internet,cloud.tls,cloud.mqtt,cloud.commandAck,cloud.telemetry,false,ready,ready?"remote control ready":"remote handshake incomplete");}
    private void monitorProvisioned(RemoteReadiness first){render(first);if(first.ready){io.execute(()->{RemoteReadiness cloud=new RemoteSessionManager(a,prefs).probe();RemoteReadiness combined=combine(first,cloud);ui.post(()->{render(combined);if(combined.ready)finishRemote(combined);else io.execute(this::pollRobotRemote);});});return;}io.execute(this::pollRobotRemote);}

    private void render(RemoteReadiness r){progress.setVisibility(View.VISIBLE);progress.setProgress(r.completedCoreStages());StringBuilder s=new StringBuilder();stage(s,"إعداد الشبكة",r.configured);stage(s,"Wi-Fi",r.wifi);stage(s,"IP",r.ip);stage(s,"DNS",r.dns);stage(s,"الإنترنت/الوقت",r.internet&&r.time);stage(s,"TLS",r.tls);stage(s,"MQTT",r.mqtt);stage(s,"قناة الأوامر",r.commandAck);stage(s,"Telemetry",r.telemetry);stage(s,"الفيديو البعيد",r.camera);status.setText(s.toString().trim());status.setTextColor(r.ready&&r.camera?0xFF2E7D32:0xFF245A7A);}
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
