package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.MjpegRecorder;
import com.servoc.app.camera.MjpegView;
import com.servoc.app.control.JoystickView;
import com.servoc.app.data.EvidenceStore;
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
    private volatile String recordingSessionId,recordingStartedTs;
    private volatile long recordingStartedElapsed;
    private volatile boolean recordingStopping=false;
    private FlameEvidenceTracker.State lastFlameState=FlameEvidenceTracker.State.CLEAR;

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
        if(recordingStopping){host.showToast("جارٍ إنهاء التسجيل السابق");return;}
        if(recordingSessionId!=null){requestStopRecording("manual",true);return;}
        String session=null;
        try{
            session=EvidenceStore.startStandaloneSession(host.database(),host.operatorRole(),"video_capture");
            String name="HazRob_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());File f=recorder.start(name);
            recordingSessionId=session;recordingStartedTs=EvidenceStore.isoNow();recordingStartedElapsed=SystemClock.elapsedRealtime();record.setText("إيقاف التسجيل");recordInfo.setText("جارٍ التسجيل • "+f.getName()+"\nجلسة "+session);host.showToast("بدأ تسجيل الفيديو");
        }catch(Exception e){if(session!=null)EvidenceStore.finishStandaloneSession(host.database(),session,"{\"terminal_state\":\"start_error\"}");host.showToast("تعذر بدء تسجيل الفيديو");recordInfo.setText("التسجيل: خطأ في التخزين");}
    }

    private void requestStopRecording(String reason,boolean toastAfter){
        final String session=recordingSessionId,start=recordingStartedTs;if(session==null||recordingStopping)return;
        recordingStopping=true;record.setEnabled(false);recordInfo.setText("جارٍ إنهاء التسجيل وحفظ الإطارات المعلقة...");
        host.io().execute(()->{
            MjpegRecorder.StopResult result=recorder.stopGracefully(5000);String end=EvidenceStore.isoNow();boolean mine=claimSession(session);
            if(mine)persistRecording(session,start,end,reason,result);
            root.post(()->{recordingStopping=false;record.setEnabled(true);record.setText("تسجيل الفيديو");if(mine){String err=result.error==null?"":" • خطأ "+result.error;recordInfo.setText("تم الحفظ • "+result.frames+" إطار • مسقط "+result.dropped+err+"\n"+(result.video==null?"":result.video.getName()));if(toastAfter)host.showToast(result.clean()?"تم إيقاف تسجيل الفيديو":"تم حفظ التسجيل مع خطأ كتابة");}});
        });
    }

    private synchronized boolean claimSession(String expected){
        if(expected==null||!expected.equals(recordingSessionId))return false;
        recordingSessionId=null;recordingStartedTs=null;recordingStartedElapsed=0;return true;
    }

    private void persistRecording(String session,String start,String end,String reason,MjpegRecorder.StopResult r){
        JSONObject meta=new JSONObject();try{meta.put("format","HAZROB_RAW_MJPEG_V1");meta.put("frames",r.frames);meta.put("dropped_frames",r.dropped);meta.put("stop_reason",reason);meta.put("camera_host",host.cameraHost());meta.put("clean_close",r.clean());if(r.error!=null)meta.put("io_error",r.error);if(r.index!=null)meta.put("index_uri",Uri.fromFile(r.index).toString());}catch(Exception ignored){}
        String metadata=meta.toString();try{if(r.video!=null)EvidenceStore.saveMedia(host.database(),session,"video/x-hazrob-mjpeg",Uri.fromFile(r.video).toString(),start,end,metadata);EvidenceStore.finishStandaloneSession(host.database(),session,metadata);}catch(Exception ignored){}
    }

    private void showFlame(FlameFrameAnalyzer.Result r){
        if(r==null||r.frame==null||r.temporal==null)return;
        FlameEvidenceTracker.State now=r.temporal.state,previous=lastFlameState;lastFlameState=now;
        String state=now==FlameEvidenceTracker.State.PERSISTENT?"مستمر":now==FlameEvidenceTracker.State.CANDIDATE?"مرشح":"لا دليل";
        StringBuilder s=new StringBuilder("دليل اللهب البصري التجريبي: ").append(state)
                .append(" • score ").append(String.format(Locale.US,"%.0f%%",100*r.frame.score))
                .append(" • pixels ").append(String.format(Locale.US,"%.1f%%",100*r.frame.candidateRatio))
                .append(" • EMA ").append(String.format(Locale.US,"%.0f%%",100*r.temporal.ema));
        if(r.frame.hasRegion())s.append(String.format(Locale.US,"\nمنطقة تقريبية L%.2f T%.2f R%.2f B%.2f",r.frame.left,r.frame.top,r.frame.right,r.frame.bottom));
        flameInfo.setText(s.toString());
        flameInfo.setTextColor(now==FlameEvidenceTracker.State.PERSISTENT?0xFFFF7043:now==FlameEvidenceTracker.State.CANDIDATE?0xFFFFB74D:0xFF90A4AE);
        if(now==FlameEvidenceTracker.State.PERSISTENT&&previous!=FlameEvidenceTracker.State.PERSISTENT)persistFlameEvent("flame_visual_onset",r);
        else if(previous==FlameEvidenceTracker.State.PERSISTENT&&now==FlameEvidenceTracker.State.CLEAR)persistFlameEvent("flame_visual_clear",r);
    }

    private void persistFlameEvent(String type,FlameFrameAnalyzer.Result r){
        JSONObject p=new JSONObject();try{p.put("observed_utc",EvidenceStore.isoNow());p.put("model","FlameColorModel+TemporalEvidence/0.6");p.put("frame_score",r.frame.score);p.put("candidate_ratio",r.frame.candidateRatio);p.put("bright_candidate_ratio",r.frame.brightCandidateRatio);p.put("ema",r.temporal.ema);p.put("state",r.temporal.state.name());if(r.frame.hasRegion()){p.put("left",r.frame.left);p.put("top",r.frame.top);p.put("right",r.frame.right);p.put("bottom",r.frame.bottom);}}catch(Exception ignored){}
        final String session=recordingSessionId,payload=p.toString();final long started=recordingStartedElapsed,t=session!=null&&started>0?Math.max(0,r.elapsedMs-started):0;final double confidence=r.temporal.ema;
        host.io().execute(()->{try{EvidenceStore.saveEvent(host.database(),session,t,type,confidence,payload);}catch(Exception ignored){}});
    }

    public View view(){return root;}
    public void resetJoystick(){joystick.reset();}
    public void startCamera(){String h=host.cameraHost();if(h!=null&&!h.trim().isEmpty())video.start("http://"+h.trim()+":81/stream");}
    public void stopCamera(){video.stop();flameAnalyzer.reset();lastFlameState=FlameEvidenceTracker.State.CLEAR;flameInfo.setText("دليل اللهب البصري التجريبي: متوقف مع الكاميرا");flameInfo.setTextColor(0xFF90A4AE);if(recordingSessionId!=null&&!recordingStopping)requestStopRecording("camera_stop",false);}

    /** Lifecycle teardown fallback: close and persist synchronously before DB/executor shutdown. */
    public void releaseCamera(){
        video.stop();flameAnalyzer.release();
        String session=recordingSessionId,start=recordingStartedTs;if(session!=null&&claimSession(session)){MjpegRecorder.StopResult r=recorder.stopNow();persistRecording(session,start,EvidenceStore.isoNow(),"release",r);}recorder.release();video.release();
    }

    public void refresh(){
        String fm=host.flashMode();flash.setText("الفلاش: "+("on".equals(fm)?"تشغيل":"auto".equals(fm)?"تلقائي":"إيقاف"));
        if(recordingSessionId!=null&&!recordingStopping&&!recorder.isRecording()&&recorder.lastError()!=null){requestStopRecording("io_error",false);return;}
        if(recorder.isRecording())recordInfo.setText("جارٍ التسجيل • "+recorder.frameCount()+" إطار • مسقط "+recorder.droppedFrames()+"\nجلسة "+recordingSessionId);
        JSONObject d=host.latestDiagnostics();
        if(d==null){status.setText("الروبوت غير متصل");status.setTextColor(0xFFFFB74D);drive.setText("يسار "+host.driveLeft()+"   يمين "+host.driveRight()+"   أمان الاتصال —");return;}
        status.setText("متصل • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);
        JSONObject x=d.optJSONObject("drive");if(x!=null)drive.setText("يسار "+x.optInt("left",host.driveLeft())+"   يمين "+x.optInt("right",host.driveRight())+"   أمان الاتصال "+(x.optBoolean("failsafe",false)?"مفعّل":"سليم"));
    }
}
