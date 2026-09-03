package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.MjpegRecorder;
import com.servoc.app.camera.MjpegView;
import com.servoc.app.camera.RemoteVideoCredentialStore;
import com.servoc.app.control.JoystickView;
import com.servoc.app.navigation.GasDirectionEstimator;
import com.servoc.app.navigation.MqRelayPlanner;
import com.servoc.app.vision.FlameEvidenceTracker;
import com.servoc.app.vision.FlameFrameAnalyzer;
import com.servoc.app.vision.HazardOverlayView;

import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** HazRob 0.9 drive workspace. Tracking target is owned by Analysis and shared through HazRobHost. */
public final class ControlTab {
    private final Context appContext;private final HazRobHost host;private final LinearLayout root;
    private final TextView status,drive,cameraBadge,flameBadge,irBadge,trackingBadge,recordInfo;
    private final Button flash,record,cancelTracking;
    private final MjpegView video;private final MjpegRecorder recorder;private final FlameFrameAnalyzer flameAnalyzer;private final HazardOverlayView overlay;private final GasDirectionEstimator gasDirection=new GasDirectionEstimator();private final JoystickView joystick;
    private volatile boolean recordingStopping=false;private FlameEvidenceTracker.State lastFlameState=FlameEvidenceTracker.State.CLEAR;private FlameFrameAnalyzer.Result lastFlameResult;private int lastTarget=-1;

    public ControlTab(Context c,HazRobHost host){
        appContext=c.getApplicationContext();this.host=host;boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));root.setBackgroundColor(0xFF0E1116);

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);recorder=new MjpegRecorder(c);flameAnalyzer=new FlameFrameAnalyzer(r->root.post(()->showFlame(r)));video.setFrameListener((jpeg,t)->{recorder.onJpegFrame(jpeg,t);flameAnalyzer.onJpegFrame(jpeg,t);});video.setStatusListener(this::showCameraStatus);cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HazardOverlayView(c);cameraBox.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        joystick=new JoystickView(c);int size=UiKit.dp(c,landscape?120:150);FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);joystick.setListener((l,r,active)->host.setDriveCommand(l,r));

        status=badge(c,"ROBOT —");FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);sp.setMargins(0,UiKit.dp(c,6),UiKit.dp(c,6),0);cameraBox.addView(status,sp);
        irBadge=badge(c,"IR —");FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);ip.setMargins(0,UiKit.dp(c,40),UiKit.dp(c,6),0);cameraBox.addView(irBadge,ip);
        cameraBadge=badge(c,"CAM —");FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);cp.setMargins(UiKit.dp(c,6),UiKit.dp(c,6),0,0);cameraBox.addView(cameraBadge,cp);
        flameBadge=badge(c,"🔥 —");FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.BOTTOM);fp.setMargins(UiKit.dp(c,6),0,0,UiKit.dp(c,6));cameraBox.addView(flameBadge,fp);
        trackingBadge=badge(c,"تتبع —");FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);tp.setMargins(UiKit.dp(c,6),UiKit.dp(c,40),0,0);cameraBox.addView(trackingBadge,tp);
        root.addView(cameraBox,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout actions=UiKit.hbox(c);Button stop=UiKit.button(c,"إيقاف طارئ");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.25f));flash=UiKit.button(c,"فلاش OFF");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1));record=UiKit.button(c,"تسجيل");record.setOnClickListener(v->toggleRecording());actions.addView(record,UiKit.weight(1));cancelTracking=UiKit.button(c,"إلغاء التتبع");cancelTracking.setOnClickListener(v->{host.setTrackingTarget(-1,"");gasDirection.reset();overlay.clearGas();host.refreshAllTabs();});actions.addView(cancelTracking,UiKit.weight(1.05f));root.addView(actions,UiKit.full(c));
        drive=UiKit.small(c,"L 0   R 0   —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));recordInfo=UiKit.small(c,"VIDEO —");recordInfo.setBackgroundColor(0xFF151A20);root.addView(recordInfo,UiKit.full(c));
    }

    private TextView badge(Context c,String text){TextView v=UiKit.small(c,text);v.setTextColor(0xFF90A4AE);v.setBackgroundColor(0xCC151A20);v.setPadding(UiKit.dp(c,8),UiKit.dp(c,4),UiKit.dp(c,8),UiKit.dp(c,4));return v;}
    private void showCameraStatus(String state,String detail){if("streaming".equals(state)){cameraBadge.setText("CAM ●");cameraBadge.setTextColor(0xFF81C784);}else if("connecting".equals(state)){cameraBadge.setText("CAM …");cameraBadge.setTextColor(0xFFFFB74D);}else if("retrying".equals(state)){cameraBadge.setText("CAM ↻");cameraBadge.setTextColor(0xFFFF7043);}else{cameraBadge.setText("CAM ○");cameraBadge.setTextColor(0xFF90A4AE);}}

    private void cycleFlash(){String next="on".equals(host.flashMode())?"off":"on";host.io().execute(()->{if(!host.cameraReachable()){host.showToast("الكاميرا غير متصلة");return;}host.setFlashMode(next);});}

    private void toggleRecording(){
        if(recordingStopping)return;if(recorder.isRecording()){requestStopRecording(true);return;}
        try{String name="HazRob_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.US).format(new Date());File f=recorder.start(name);record.setText("إيقاف التسجيل");recordInfo.setText("REC ●   "+f.getName());}
        catch(Exception e){recordInfo.setText("VIDEO ERROR");host.showToast("تعذر بدء التسجيل");}
    }
    private void requestStopRecording(boolean toastAfter){
        if(recordingStopping)return;recordingStopping=true;record.setEnabled(false);recordInfo.setText("VIDEO SAVING…");host.io().execute(()->{MjpegRecorder.StopResult r=recorder.stopGracefully(8000);if(r.galleryUri!=null){try{if(r.video!=null&&r.video.isFile())r.video.delete();}catch(Exception ignored){}try{if(r.index!=null&&r.index.isFile())r.index.delete();}catch(Exception ignored){}}root.post(()->{recordingStopping=false;record.setEnabled(true);record.setText("تسجيل");recordInfo.setText("VIDEO   "+r.frames+" frames   drop "+r.dropped);if(toastAfter)host.showToast(r.galleryUri!=null?"تم حفظ الفيديو":"انتهى التسجيل");});});
    }

    private void showFlame(FlameFrameAnalyzer.Result r){
        if(r==null||r.frame==null||r.temporal==null)return;lastFlameResult=r;lastFlameState=r.temporal.state;
        if(r.temporal.state!=FlameEvidenceTracker.State.CLEAR&&r.frame.hasRegion())overlay.setFlameRegion(r.frame.left,r.frame.top,r.frame.right,r.frame.bottom,true);else overlay.clearFlame();
        if(r.temporal.state==FlameEvidenceTracker.State.PERSISTENT){flameBadge.setText("🔥 ●");flameBadge.setTextColor(0xFFFF7043);}else if(r.temporal.state==FlameEvidenceTracker.State.CANDIDATE){flameBadge.setText("🔥 ?");flameBadge.setTextColor(0xFFFFB74D);}else{flameBadge.setText("🔥 ○");flameBadge.setTextColor(0xFF90A4AE);}
    }

    public View view(){return root;}public void resetJoystick(){joystick.reset();}
    public void startCamera(){
        if("remote".equalsIgnoreCase(host.preferences().getString("connection_mode","direct"))){String url=host.preferences().getString("remote_video_url","").trim();String token=new RemoteVideoCredentialStore(host.preferences()).load();if(url.isEmpty()){showCameraStatus("retrying","Remote video not configured");return;}video.startWss(url,token);return;}
        String h=host.cameraHost();if(h==null||h.trim().isEmpty()){showCameraStatus("retrying","");return;}h=h.trim();video.start(new String[]{"http://"+h+":81/stream","http://"+h+"/stream","http://"+h+":81/","http://"+h+"/mjpeg"});
    }
    public void stopCamera(){video.stop();flameAnalyzer.reset();overlay.clearFlame();overlay.clearGas();lastFlameState=FlameEvidenceTracker.State.CLEAR;lastFlameResult=null;if(recorder.isRecording()&&!recordingStopping)requestStopRecording(false);}
    public void releaseCamera(){video.stop();flameAnalyzer.release();overlay.clearFlame();overlay.clearGas();gasDirection.reset();if(recorder.isRecording()||recorder.isClosing())recorder.stopNow();recorder.release();video.release();}

    public void refresh(){
        flash.setText("فلاش "+("on".equals(host.flashMode())?"ON":"OFF"));if(recorder.isRecording())recordInfo.setText("REC ●   "+recorder.frameCount()+" frames   drop "+recorder.droppedFrames());
        int target=host.trackingTargetId();String targetName=host.trackingTargetName();if(target!=lastTarget){lastTarget=target;gasDirection.reset();overlay.clearGas();}
        cancelTracking.setVisibility(target>=0?View.VISIBLE:View.GONE);trackingBadge.setText(target>=0?"تتبع ● "+targetName:"تتبع —");trackingBadge.setTextColor(target>=0?0xFF81C784:0xFF90A4AE);

        JSONObject d=host.latestDiagnostics();if(d==null){status.setText("ROBOT ○");status.setTextColor(0xFFFFB74D);irBadge.setText("IR —");drive.setText("L "+host.driveLeft()+"   R "+host.driveRight()+"   LINK —");overlay.clearGas();return;}
        status.setText("ROBOT ●");status.setTextColor(0xFF81C784);JSONObject ir=d.optJSONObject("ir");boolean irWarn=ir!=null&&ir.optBoolean("warning",false);irBadge.setText(irWarn?"⚠ IR":"IR ●");irBadge.setTextColor(irWarn?0xFFFF7043:0xFF81C784);
        JSONObject x=d.optJSONObject("drive");if(x!=null){boolean back=x.optBoolean("retrace",false),fail=x.optBoolean("failsafe",false);drive.setText("L "+x.optInt("left",host.driveLeft())+"   R "+x.optInt("right",host.driveRight())+"   "+(back?"RETRACE":fail?"LINK LOSS":"SAFE"));drive.setTextColor(back?0xFFFFB74D:Color.LTGRAY);}
        if(target<0){overlay.clearGas();return;}GasDirectionEstimator.Result g=gasDirection.update(d,host.driveLeft(),host.driveRight(),SystemClock.elapsedRealtime(),target);int mask=MqRelayPlanner.forChemical(target);if(g.valid){overlay.setGasDirection(g.angleRad,g.confidence);trackingBadge.setText("تتبع ● "+targetName+"   "+bearingName(g.angleRad)+String.format(Locale.US," %.0f%%",100*g.confidence));}else{overlay.clearGas();trackingBadge.setText("تتبع ● "+targetName+"   "+MqRelayPlanner.rowsLabel(mask));}
    }

    private static String bearingName(double a){double d=Math.toDegrees(a);if(d>=-22.5&&d<22.5)return"أمام";if(d>=22.5&&d<67.5)return"أمام-يمين";if(d>=67.5&&d<112.5)return"يمين";if(d>=112.5&&d<157.5)return"خلف-يمين";if(d>=157.5||d<-157.5)return"خلف";if(d>=-157.5&&d<-112.5)return"خلف-يسار";if(d>=-112.5&&d<-67.5)return"يسار";return"أمام-يسار";}
}
