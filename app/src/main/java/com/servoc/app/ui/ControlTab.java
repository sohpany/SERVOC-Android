package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.MjpegView;
import com.servoc.app.control.JoystickView;

import org.json.JSONObject;

public final class ControlTab {
    private final Context c;
    private final HazRobHost host;
    private final LinearLayout root;
    private final TextView status;
    private final TextView drive;
    private final Button flash;
    private final MjpegView video;

    public ControlTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,8));
        root.setBackgroundColor(0xFF0E1116);

        LinearLayout header=UiKit.hbox(c);
        TextView title=UiKit.title(c,"CONTROL / CAMERA");
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        status=UiKit.text(c,"ROBOT OFFLINE");status.setTextColor(0xFFFFB74D);status.setGravity(Gravity.END);
        header.addView(status,new LinearLayout.LayoutParams(-2,-2));
        root.addView(header,UiKit.full(c));

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);
        video=new MjpegView(c);cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        JoystickView joystick=new JoystickView(c);
        int size=UiKit.dp(c,150);
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.END|Gravity.BOTTOM);
        jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);
        joystick.setListener((l,r,active)->host.setDriveCommand(l,r));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1f);cp.height=0;root.addView(cameraBox,cp);

        LinearLayout actions=UiKit.hbox(c);
        Button stop=UiKit.button(c,"EMERGENCY STOP");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);
        stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.3f));
        flash=UiKit.button(c,"FLASH OFF");flash.setOnClickListener(v->host.setFlash(!host.flashState()));actions.addView(flash,UiKit.weight(1f));
        root.addView(actions,UiKit.full(c));

        drive=UiKit.small(c,"L 0   R 0   failsafe —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));
        root.addView(UiKit.small(c,"Drive commands are sent every 250 ms while HazRob is foreground. ESP32 keeps its local failsafe and motor ramp/reverse protection."),UiKit.full(c));
    }

    public View view(){return root;}

    public void startCamera(){
        String h=host.cameraHost(); if(h!=null&&!h.trim().isEmpty())video.start("http://"+h.trim()+":81/stream");
    }
    public void stopCamera(){video.stop();}

    public void refresh(){
        flash.setText(host.flashState()?"FLASH ON":"FLASH OFF");
        JSONObject d=host.latestDiagnostics();
        if(d==null){status.setText("ROBOT OFFLINE");status.setTextColor(0xFFFFB74D);drive.setText("L "+host.driveLeft()+"   R "+host.driveRight()+"   failsafe —");return;}
        status.setText("ROBOT ONLINE • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);
        JSONObject x=d.optJSONObject("drive");
        if(x!=null)drive.setText("L "+x.optInt("left",host.driveLeft())+"   R "+x.optInt("right",host.driveRight())+
                "   failsafe "+(x.optBoolean("failsafe",false)?"ACTIVE":"OK"));
    }
}
