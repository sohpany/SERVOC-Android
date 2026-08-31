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
    private final HazRobHost host;
    private final LinearLayout root;
    private final TextView status,drive;
    private final Button flash;
    private final MjpegView video;
    private final JoystickView joystick;

    public ControlTab(Context c,HazRobHost host){
        this.host=host;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,8));root.setBackgroundColor(0xFF0E1116);

        LinearLayout header=UiKit.hbox(c);
        TextView title=UiKit.title(c,"التحكم / الكاميرا");header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        status=UiKit.text(c,"الروبوت غير متصل");status.setTextColor(0xFFFFB74D);status.setGravity(Gravity.END);header.addView(status,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(c));

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        joystick=new JoystickView(c);int size=UiKit.dp(c,150);
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);
        joystick.setListener((l,r,active)->host.setDriveCommand(l,r));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1f);cp.height=0;root.addView(cameraBox,cp);

        LinearLayout actions=UiKit.hbox(c);
        Button stop=UiKit.button(c,"إيقاف طارئ");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.3f));
        flash=UiKit.button(c,"الفلاش: إيقاف");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1f));root.addView(actions,UiKit.full(c));

        drive=UiKit.small(c,"يسار 0   يمين 0   أمان الاتصال —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));
        root.addView(UiKit.small(c,"تُرسل أوامر القيادة كل 250 ms أثناء بقاء HazRob في الواجهة. تبقى الحماية من فقد الاتصال والتسارع/التباطؤ وعكس الاتجاه داخل ESP32."),UiKit.full(c));
    }

    private void cycleFlash(){String m=host.flashMode();host.setFlashMode("off".equals(m)?"auto":"auto".equals(m)?"on":"off");}
    public View view(){return root;}
    public void resetJoystick(){joystick.reset();}
    public void startCamera(){String h=host.cameraHost();if(h!=null&&!h.trim().isEmpty())video.start("http://"+h.trim()+":81/stream");}
    public void stopCamera(){video.stop();}
    public void releaseCamera(){video.release();}

    public void refresh(){
        String fm=host.flashMode();flash.setText("الفلاش: "+("on".equals(fm)?"تشغيل":"auto".equals(fm)?"تلقائي":"إيقاف"));
        JSONObject d=host.latestDiagnostics();
        if(d==null){status.setText("الروبوت غير متصل");status.setTextColor(0xFFFFB74D);drive.setText("يسار "+host.driveLeft()+"   يمين "+host.driveRight()+"   أمان الاتصال —");return;}
        status.setText("متصل • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);
        JSONObject x=d.optJSONObject("drive");if(x!=null)drive.setText("يسار "+x.optInt("left",host.driveLeft())+"   يمين "+x.optInt("right",host.driveRight())+"   أمان الاتصال "+(x.optBoolean("failsafe",false)?"مفعّل":"سليم"));
    }
}
