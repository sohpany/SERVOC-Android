package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.models.MqDataScience76Catalog;
import org.json.JSONObject;

public final class SettingsTab {
    private final Context c;private final HazRobHost host;private final ScrollView scroll;private final LinearLayout root;
    private final EditText robot,camera,speed,nl,nr,warm,measure;

    public SettingsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"SETTINGS / MODELS"),UiKit.full(c));

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"NETWORK"),UiKit.full(c));
        robot=UiKit.input(c,host.preferences().getString("robot","192.168.4.1"));camera=UiKit.input(c,host.preferences().getString("camera","192.168.4.2"));
        net.addView(UiKit.small(c,"Robot ESP32 IP"),UiKit.full(c));net.addView(robot,UiKit.full(c));net.addView(UiKit.small(c,"ESP32-CAM IP"),UiKit.full(c));net.addView(camera,UiKit.full(c));
        Button saveNet=UiKit.button(c,"SAVE / APPLY NETWORK");saveNet.setOnClickListener(v->saveNetwork());net.addView(saveNet,UiKit.full(c));root.addView(net,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"ESP32 ROBOT CONFIGURATION"),UiKit.full(c));
        speed=field(cfg,"Speed limit %",String.valueOf(host.preferences().getInt("speed",100)));
        nl=field(cfg,"Left neutral µs",String.valueOf(host.preferences().getInt("nl",1500)));
        nr=field(cfg,"Right neutral µs",String.valueOf(host.preferences().getInt("nr",1500)));
        warm=field(cfg,"Sensor warmup seconds",String.valueOf(host.preferences().getInt("warm",60)));
        measure=field(cfg,"Measurement seconds",String.valueOf(host.preferences().getInt("measure",30)));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"SYNC FROM ESP32");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"APPLY TO ESP32");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout models=UiKit.card(c);models.addView(UiKit.title(c,"MQDATASCIENCE v"+MqDataScience76Catalog.VERSION+" • "+MqDataScience76Catalog.COUNT+" MODELS"),UiKit.full(c));
        models.addView(UiKit.small(c,"Registered source models: "+MqDataScience76Catalog.COUNT+". Android-native status is shown explicitly; registered-only models are not falsely reported as executed."),UiKit.full(c));
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++){
            TextView t=UiKit.text(c,String.format("%02d  %s  [%s]",i+1,MqDataScience76Catalog.NAMES[i],MqDataScience76Catalog.runtime(i)==MqDataScience76Catalog.Runtime.ANDROID_NATIVE?"ANDROID NATIVE":"REGISTERED / PORT PENDING"));
            t.setTextSize(11);t.setBackgroundColor((i&1)==0?0xFF151B22:0xFF1B222A);t.setTextColor(MqDataScience76Catalog.runtime(i)==MqDataScience76Catalog.Runtime.ANDROID_NATIVE?0xFF81C784:0xFFB0BEC5);models.addView(t,UiKit.full(c));
        }
        root.addView(models,UiKit.full(c));
    }

    private EditText field(LinearLayout p,String label,String value){p.addView(UiKit.small(c,label),UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    public View view(){return scroll;}

    private void saveNetwork(){
        String rh=robot.getText().toString().trim();String ch=camera.getText().toString().trim();host.robotApi().setRobotHost(rh);host.setCameraHost(ch);
        host.preferences().edit().putString("robot",host.robotApi().getRobotHost()).putString("camera",ch).apply();host.showToast("Network settings applied");host.refreshAllTabs();
    }

    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();android.os.Handler h=new android.os.Handler(android.os.Looper.getMainLooper());h.post(()->showConfig(x));}catch(Exception e){host.showToast("ESP32 config read failed");}});}
    private void apply(){
        final int s=num(speed,100),l=num(nl,1500),r=num(nr,1500),w=num(warm,60),m=num(measure,30);final String cam=camera.getText().toString().trim();
        host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,s,l,r,w,m);host.preferences().edit().putInt("speed",s).putInt("nl",l).putInt("nr",r).putInt("warm",w).putInt("measure",m).apply();android.os.Handler h=new android.os.Handler(android.os.Looper.getMainLooper());h.post(()->showConfig(x));host.showToast("ESP32 configuration applied");}catch(Exception e){host.showToast("ESP32 configuration failed");}});
    }
    private void showConfig(JSONObject x){if(x==null)return;camera.setText(x.optString("cam",camera.getText().toString()));speed.setText(String.valueOf(x.optInt("speed",100)));nl.setText(String.valueOf(x.optInt("nl",1500)));nr.setText(String.valueOf(x.optInt("nr",1500)));warm.setText(String.valueOf(x.optInt("warm",60)));measure.setText(String.valueOf(x.optInt("measure",30)));}
    private int num(EditText e,int f){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return f;}}
}
