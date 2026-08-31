package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.MjpegRecorder;
import com.servoc.app.camera.MjpegView;
import com.servoc.app.control.JoystickView;
import com.servoc.app.vision.FlameEvidenceTracker;
import com.servoc.app.vision.FlameFrameAnalyzer;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ControlTab {
    private final HazRobHost host;
    private final LinearLayout root;
    private final TextView status,drive,recordInfo,flameInfo;
    private final Button flash,record;
    private final MjpegView video;
    private final MjpegRecorder recorder;
    private final FlameFrameAnalyzer flameAnalyzer;
    private final JoystickView joystick;

    public ControlTab(Context c,HazRobHost host){
        this.host=host;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,8));root.setBackgroundColor(0xFF0E1116);

        LinearLayout header=UiKit.hbox(c);
        TextView title=UiKit.title(c,"التحكم / الكاميرا");header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        status=UiKit.text(c,"الروبوت غير متصل");status.setTextColor(0xFFFFB74D);status.setGravity(Gravity.END);header.addView(status,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(c));

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);recorder=new MjpegRecorder(c);flameAnalyzer=new FlameFrameAnalyzer(r->root.post(()->showFlame(r)));
        video.setFrameListener((jpeg,t)->{recorder.onJpegFrame(jpeg,t);flameAnalyzer.onJpegFrame(jpeg,t);});cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        joystick=new JoystickView(c);int size=UiKit.dp(c,150);
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);
        joystick.setListener((l,r,active)->host.setDriveCommand(l,r));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1f);cp.height=0;root.addView(cameraBox,cp);

        LinearLayout actions=UiKit.hbox(c);
        Button stop=UiKit.button(c,"إيقاف طارئ");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.3f));
        flash=UiKit.button(c,"الفلاش: إيقاف");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1f));
        record=UiKit.button(c,"تسجيل الفيديو");record.setOnClickListener(v->toggleRecording());actions.addView(record,UiKit.weight(1f));root.addView(actions,UiKit.full(c));

        recordInfo=UiKit.small(c,"التسجيل: متوقف");recordInfo.setBackgroundColor(0xFF151A20);root.addView(recordInfo,UiKit.full(c));
        flameInfo=UiKit.small(c,"دليل اللهب البصري التجريبي: بانتظار الفيديو");flameInfo.setBackgroundColor(0xFF151A20);flameInfo.setTextColor(0xFF90A4AE);root.addView(flameInfo,UiKit.full(c));
        drive=UiKit.small(c,"يسار 0   يمين 0   أمان الاتصال —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));
        root.addView(UiKit.small(c,"يستخدم العرض والتسجيل والتحليل البصري نفس اتصال ESP32-CAM. تحليل اللهب تجريبي/مساعد ولا يرسل أوامر حركة أو قرارات سلامة. تُرسل أوامر القيادة كل 250 ms وتبقى الحماية من فقد الاتصال داخل ESP32."),UiKit.full(c));
    }

    private void cycleFlash(){String m=host.flashMode();host.setFlashMode("off".equals(m)?"auto":"auto".equals(m)?"on":"off");}

    private void toggleRecording(){
        if(recorder.isRecording()){
            File f=recorder.videoFile();recorder.stop();record.setText("تسجيل الفيديو");recordInfo.setText("تم الحفظ • "+recorder.frameCount()+" إطار • مسقط "+recorder.droppedFrames()+"\n"+(f==null?"":f.getName()));host.showToast("تم إيقاف تسجيل الفيديو");return;
        }
        try{
            String name="HazRob_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());File f=recorder.start(name);record.setText("إيقاف التسجيل");recordInfo.setText("جارٍ التسجيل • "+f.getName());host.showToast("بدأ تسجيل الفيديو");
        }catch(Exception e){host.showToast("تعذر بدء تسجيل الفيديو");recordInfo.setText("التسجيل: خطأ في التخزين");}
    }

    private void showFlame(FlameFrameAnalyzer.Result r){
        if(r==null||r.frame==null||r.temporal==null)return;
        String state=r.temporal.state==FlameEvidenceTracker.State.PERSISTENT?"مستمر":r.temporal.state==FlameEvidenceTracker.State.CANDIDATE?"مرشح":"لا دليل";
        StringBuilder s=new StringBuilder("دليل اللهب البصري التجريبي: ").append(state)
                .append(" • score ").append(String.format(Locale.US,"%.0f%%",100*r.frame.score))
                .append(" • pixels ").append(String.format(Locale.US,"%.1f%%",100*r.frame.candidateRatio))
                .append(" • EMA ").append(String.format(Locale.US,"%.0f%%",100*r.temporal.ema));
        if(r.frame.hasRegion())s.append(String.format(Locale.US,"\nمنطقة تقريبية L%.2f T%.2f R%.2f B%.2f",r.frame.left,r.frame.top,r.frame.right,r.frame.bottom));
        flameInfo.setText(s.toString());
        flameInfo.setTextColor(r.temporal.state==FlameEvidenceTracker.State.PERSISTENT?0xFFFF7043:r.temporal.state==FlameEvidenceTracker.State.CANDIDATE?0xFFFFB74D:0xFF90A4AE);
    }

    public View view(){return root;}
    public void resetJoystick(){joystick.reset();}
    public void startCamera(){String h=host.cameraHost();if(h!=null&&!h.trim().isEmpty())video.start("http://"+h.trim()+":81/stream");}
    public void stopCamera(){video.stop();flameAnalyzer.reset();flameInfo.setText("دليل اللهب البصري التجريبي: متوقف مع الكاميرا");flameInfo.setTextColor(0xFF90A4AE);if(recorder.isRecording()){recorder.stop();record.setText("تسجيل الفيديو");recordInfo.setText("تم إيقاف التسجيل مع إيقاف الكاميرا • "+recorder.frameCount()+" إطار");}}
    public void releaseCamera(){flameAnalyzer.release();recorder.release();video.release();}

    public void refresh(){
        String fm=host.flashMode();flash.setText("الفلاش: "+("on".equals(fm)?"تشغيل":"auto".equals(fm)?"تلقائي":"إيقاف"));
        if(recorder.isRecording())recordInfo.setText("جارٍ التسجيل • "+recorder.frameCount()+" إطار • مسقط "+recorder.droppedFrames());
        JSONObject d=host.latestDiagnostics();
        if(d==null){status.setText("الروبوت غير متصل");status.setTextColor(0xFFFFB74D);drive.setText("يسار "+host.driveLeft()+"   يمين "+host.driveRight()+"   أمان الاتصال —");return;}
        status.setText("متصل • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);
        JSONObject x=d.optJSONObject("drive");if(x!=null)drive.setText("يسار "+x.optInt("left",host.driveLeft())+"   يمين "+x.optInt("right",host.driveRight())+"   أمان الاتصال "+(x.optBoolean("failsafe",false)?"مفعّل":"سليم"));
    }
}
