package com.servoc.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.MjpegRecorder;
import com.servoc.app.camera.MjpegView;
import com.servoc.app.camera.RemoteVideoCredentialStore;
import com.servoc.app.communication.WifiSignalReader;
import com.servoc.app.control.DirectionPadView;
import com.servoc.app.control.DriveMode;
import com.servoc.app.control.DriveSpeedLimiter;
import com.servoc.app.control.DualTankDriveControl;
import com.servoc.app.control.EmergencyStopPressPolicy;
import com.servoc.app.control.JoystickView;
import com.servoc.app.control.SteeringWheelView;
import com.servoc.app.control.VerticalSpeedControl;
import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.navigation.GasDirectionEstimator;
import com.servoc.app.vision.FlameEvidenceTracker;
import com.servoc.app.vision.FlameFrameAnalyzer;
import com.servoc.app.vision.HazardOverlayView;

import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** HazRob 0.10.2 drive workspace. Tracking is arrow-only and owns no drive authority. */
public final class ControlTab {
    private final Context appContext;private final Context uiContext;private final HazRobHost host;private final LinearLayout root;private final FrameLayout cameraBox;private final boolean landscape;
    private final TextView status,drive,cameraBadge,flameBadge,irBadge,trackingBadge,recordInfo,speedBadge,safetyWarning,gasBadge,wifiBadge,distanceBadge;
    private final Button stop,flash,record,cancelTracking,driveModeButton,relayButton;
    private final MjpegView video;private final MjpegRecorder recorder;private final FlameFrameAnalyzer flameAnalyzer;private final HazardOverlayView overlay;private final GasDirectionEstimator gasDirection=new GasDirectionEstimator();
    private final JoystickView joystick;private final SteeringWheelView wheel;private final DirectionPadView directionPad;private final VerticalSpeedControl speedControl;private final DualTankDriveControl tankLeft,tankRight;
    private volatile boolean recordingStopping=false,estopLatched=false;private FlameEvidenceTracker.State lastFlameState=FlameEvidenceTracker.State.CLEAR;private FlameFrameAnalyzer.Result lastFlameResult;private int lastTarget=-1;
    private int mappedLeftRaw=0,mappedRightRaw=0,tankLeftRaw=0,tankRightRaw=0,speedPercent=DriveSpeedLimiter.DEFAULT_PERCENT;private DriveMode driveMode;
    private Runnable estopHoldAction;private boolean estopPressActive=false,estopHoldTriggered=false;

    public ControlTab(Context c,HazRobHost host){
        uiContext=c;appContext=c.getApplicationContext();this.host=host;landscape=c.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        root=UiKit.vbox(c);root.setPadding(landscape?0:UiKit.dp(c,4),landscape?0:UiKit.dp(c,4),landscape?0:UiKit.dp(c,4),landscape?0:UiKit.dp(c,4));root.setBackgroundColor(Color.BLACK);
        cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);recorder=new MjpegRecorder(c);flameAnalyzer=new FlameFrameAnalyzer(r->root.post(()->showFlame(r)));video.setFrameListener((jpeg,elapsedMs)->{flameAnalyzer.onJpegFrame(jpeg,elapsedMs);recorder.onJpegFrame(jpeg,elapsedMs);});video.setStatusListener(this::showCameraStatus);cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HazardOverlayView(c);cameraBox.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        int controlSize=UiKit.dp(c,landscape?170:180),controlBottom=UiKit.dp(c,landscape?62:10);
        joystick=new JoystickView(c);FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(controlSize,controlSize,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),controlBottom);cameraBox.addView(joystick,jp);joystick.setListener((l,r,active)->{if(driveMode==DriveMode.JOYSTICK)acceptMappedDrive(l,r);});
        wheel=new SteeringWheelView(c);FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(controlSize,controlSize,Gravity.RIGHT|Gravity.BOTTOM);wp.setMargins(0,0,UiKit.dp(c,10),controlBottom);cameraBox.addView(wheel,wp);wheel.setListener((l,r,active)->{if(driveMode==DriveMode.WHEEL)acceptMappedDrive(l,r);});
        directionPad=new DirectionPadView(c);FrameLayout.LayoutParams dpp=new FrameLayout.LayoutParams(UiKit.dp(c,landscape?300:280),UiKit.dp(c,landscape?205:220),Gravity.RIGHT|Gravity.BOTTOM);dpp.setMargins(0,0,UiKit.dp(c,10),controlBottom);cameraBox.addView(directionPad,dpp);directionPad.setListener((l,r,active)->{if(driveMode==DriveMode.BUTTONS)acceptMappedDrive(l,r);});
        tankLeft=new DualTankDriveControl(c);tankRight=new DualTankDriveControl(c);FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(UiKit.dp(c,92),controlSize,Gravity.LEFT|Gravity.BOTTOM);tlp.setMargins(UiKit.dp(c,78),0,0,controlBottom);cameraBox.addView(tankLeft,tlp);FrameLayout.LayoutParams trp=new FrameLayout.LayoutParams(UiKit.dp(c,92),controlSize,Gravity.RIGHT|Gravity.BOTTOM);trp.setMargins(0,0,UiKit.dp(c,10),controlBottom);cameraBox.addView(tankRight,trp);tankLeft.setListener(v->{tankLeftRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();});tankRight.setListener(v->{tankRightRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();});
        boolean legacyTank=host.preferences().getBoolean("tank_drive_mode",false);String saved=host.preferences().getString("drive_mode","");driveMode=saved==null||saved.trim().isEmpty()?(legacyTank?DriveMode.TANK:DriveMode.JOYSTICK):DriveMode.fromKey(saved);host.preferences().edit().putString("drive_mode",driveMode.key).apply();applyDriveModeVisibility();

        speedBadge=badge(c,"السرعة "+DriveSpeedLimiter.DEFAULT_PERCENT+"%");FrameLayout.LayoutParams sbp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.BOTTOM);sbp.setMargins(UiKit.dp(c,6),0,0,controlSize+controlBottom+UiKit.dp(c,4));cameraBox.addView(speedBadge,sbp);
        speedControl=new VerticalSpeedControl(c);speedControl.setPercent(DriveSpeedLimiter.DEFAULT_PERCENT,false);FrameLayout.LayoutParams vsp=new FrameLayout.LayoutParams(UiKit.dp(c,64),controlSize,Gravity.LEFT|Gravity.BOTTOM);vsp.setMargins(UiKit.dp(c,8),0,0,controlBottom);cameraBox.addView(speedControl,vsp);speedControl.setListener(p->{speedPercent=DriveSpeedLimiter.clampPercent(p);speedBadge.setText("السرعة "+speedPercent+"%");if(driveMode!=DriveMode.TANK&&driveMode!=DriveMode.WHEEL)applyMappedDrive();});applySpeedControlVisibility();

        status=badge(c,"ROBOT —");FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);sp.setMargins(0,UiKit.dp(c,6),UiKit.dp(c,6),0);cameraBox.addView(status,sp);
        irBadge=badge(c,"أرض —");FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);ip.setMargins(0,UiKit.dp(c,40),UiKit.dp(c,6),0);cameraBox.addView(irBadge,ip);
        distanceBadge=badge(c,"أمام — cm");FrameLayout.LayoutParams distp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);distp.setMargins(0,UiKit.dp(c,74),UiKit.dp(c,6),0);cameraBox.addView(distanceBadge,distp);
        relayButton=UiKit.button(c,"MQ OFF");relayButton.setTextSize(11);relayButton.setTextColor(Color.WHITE);relayButton.setBackgroundColor(0xCC455A64);relayButton.setOnClickListener(v->toggleRelay());FrameLayout.LayoutParams rbp=new FrameLayout.LayoutParams(UiKit.dp(c,100),UiKit.dp(c,42),Gravity.RIGHT|Gravity.TOP);rbp.setMargins(0,UiKit.dp(c,108),UiKit.dp(c,6),0);cameraBox.addView(relayButton,rbp);

        safetyWarning=badge(c,"");safetyWarning.setTextSize(18);safetyWarning.setTextColor(Color.WHITE);safetyWarning.setBackgroundColor(0xDDC62828);safetyWarning.setGravity(Gravity.CENTER);safetyWarning.setVisibility(View.GONE);FrameLayout.LayoutParams swp=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.TOP);swp.setMargins(0,UiKit.dp(c,6),0,0);cameraBox.addView(safetyWarning,swp);
        gasBadge=badge(c,"غازات —");gasBadge.setGravity(Gravity.CENTER);FrameLayout.LayoutParams gbp=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.TOP);gbp.setMargins(0,UiKit.dp(c,42),0,0);cameraBox.addView(gasBadge,gbp);
        wifiBadge=badge(c,"Wi‑Fi —");FrameLayout.LayoutParams wifip=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);wifip.setMargins(UiKit.dp(c,6),UiKit.dp(c,6),0,0);cameraBox.addView(wifiBadge,wifip);
        cameraBadge=badge(c,"CAM —");FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);cp.setMargins(UiKit.dp(c,6),UiKit.dp(c,40),0,0);cameraBox.addView(cameraBadge,cp);
        trackingBadge=badge(c,"تتبع —");FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);tp.setMargins(UiKit.dp(c,6),UiKit.dp(c,74),0,0);cameraBox.addView(trackingBadge,tp);
        flameBadge=badge(c,"🔥 —");FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);fp.setMargins(UiKit.dp(c,6),UiKit.dp(c,108),0,0);cameraBox.addView(flameBadge,fp);

        LinearLayout actions=UiKit.hbox(c);actions.setBackgroundColor(0xB30E1116);stop=UiKit.button(c,"E-STOP");stop.setTextColor(Color.WHITE);estopHoldAction=()->{if(!estopLatched&&estopPressActive){estopHoldTriggered=true;latchEstopUi();host.emergencyStop();host.showToast("تم تفعيل E-STOP");}};stop.setOnTouchListener((v,e)->handleEstopTouch(e));stop.setOnClickListener(v->{if(EmergencyStopPressPolicy.shouldResetOnTap(estopLatched))requestEstopReset();});actions.addView(stop,UiKit.weight(1.25f));flash=UiKit.button(c,"فلاش OFF");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1));record=UiKit.button(c,"تسجيل");record.setOnClickListener(v->toggleRecording());actions.addView(record,UiKit.weight(1));cancelTracking=UiKit.button(c,"إلغاء التتبع");cancelTracking.setOnClickListener(v->{host.setTrackingTarget(-1,"");gasDirection.reset();overlay.clearGas();host.refreshAllTabs();});actions.addView(cancelTracking,UiKit.weight(1.05f));driveModeButton=UiKit.button(c,"");driveModeButton.setOnClickListener(v->cycleDriveMode());actions.addView(driveModeButton,UiKit.weight(1));renderDriveModeButton();
        drive=badge(c,"L 0   R 0   —");recordInfo=badge(c,"VIDEO —");

        root.addView(cameraBox,new LinearLayout.LayoutParams(-1,0,1f));
        if(landscape){FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(-1,UiKit.dp(c,54),Gravity.BOTTOM);ap.setMargins(UiKit.dp(c,4),0,UiKit.dp(c,4),UiKit.dp(c,2));cameraBox.addView(actions,ap);FrameLayout.LayoutParams dp=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);dp.setMargins(0,0,0,UiKit.dp(c,58));cameraBox.addView(drive,dp);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.BOTTOM);rp.setMargins(UiKit.dp(c,6),0,0,UiKit.dp(c,58));cameraBox.addView(recordInfo,rp);root.post(this::applyLandscapeFullscreenChrome);}else{root.addView(actions,UiKit.full(c));root.addView(drive,UiKit.full(c));root.addView(recordInfo,UiKit.full(c));}
        renderEstopButton();
    }

    private TextView badge(Context c,String text){TextView v=UiKit.small(c,text);v.setTextColor(0xFFECEFF1);v.setBackgroundColor(0xCC151A20);v.setPadding(UiKit.dp(c,8),UiKit.dp(c,4),UiKit.dp(c,8),UiKit.dp(c,4));return v;}
    private void showCameraStatus(String state,String detail){if("streaming".equals(state)){cameraBadge.setText("CAM ●");cameraBadge.setTextColor(0xFF81C784);}else if("connecting".equals(state)){cameraBadge.setText("CAM …");cameraBadge.setTextColor(0xFFFFB74D);}else if("retrying".equals(state)){cameraBadge.setText("CAM ↻");cameraBadge.setTextColor(0xFFFF7043);}else{cameraBadge.setText("CAM ○");cameraBadge.setTextColor(0xFFCFD8DC);}}

    private void acceptMappedDrive(int left,int right){mappedLeftRaw=left;mappedRightRaw=right;applyMappedDrive();}
    private void applyMappedDrive(){if(estopLatched){host.setDriveCommand(0,0);return;}if(driveMode==DriveMode.WHEEL){host.setDriveCommand(mappedLeftRaw,mappedRightRaw);return;}host.setDriveCommand(DriveSpeedLimiter.scale(mappedLeftRaw,speedPercent),DriveSpeedLimiter.scale(mappedRightRaw,speedPercent));}
    private void applyTankDrive(){if(estopLatched){host.setDriveCommand(0,0);return;}host.setDriveCommand(tankLeftRaw,tankRightRaw);}
    private void applyDriveModeVisibility(){tankLeft.setVisibility(driveMode==DriveMode.TANK?View.VISIBLE:View.GONE);tankRight.setVisibility(driveMode==DriveMode.TANK?View.VISIBLE:View.GONE);joystick.setVisibility(driveMode==DriveMode.JOYSTICK?View.VISIBLE:View.GONE);wheel.setVisibility(driveMode==DriveMode.WHEEL?View.VISIBLE:View.GONE);directionPad.setVisibility(driveMode==DriveMode.BUTTONS?View.VISIBLE:View.GONE);}
    private void applySpeedControlVisibility(){boolean visible=driveMode!=DriveMode.TANK&&driveMode!=DriveMode.WHEEL;speedControl.setVisibility(visible?View.VISIBLE:View.GONE);speedBadge.setVisibility(visible?View.VISIBLE:View.GONE);}
    private void resetDriveInputs(){mappedLeftRaw=mappedRightRaw=tankLeftRaw=tankRightRaw=0;joystick.reset();wheel.reset();directionPad.reset();tankLeft.reset();tankRight.reset();host.setDriveCommand(0,0);}
    private void cycleDriveMode(){resetDriveInputs();driveMode=driveMode.next();host.preferences().edit().putString("drive_mode",driveMode.key).putBoolean("tank_drive_mode",driveMode==DriveMode.TANK).apply();applyDriveModeVisibility();applySpeedControlVisibility();renderDriveModeButton();host.showToast("نمط القيادة: "+driveMode.arabicLabel);}
    private void renderDriveModeButton(){driveModeButton.setText("قيادة: "+driveMode.arabicLabel);}

    private boolean handleEstopTouch(MotionEvent e){if(estopLatched)return false;int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){estopPressActive=true;estopHoldTriggered=false;stop.removeCallbacks(estopHoldAction);stop.postDelayed(estopHoldAction,EmergencyStopPressPolicy.ARM_HOLD_MS);return true;}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){stop.removeCallbacks(estopHoldAction);estopPressActive=false;if(action==MotionEvent.ACTION_UP&&!estopHoldTriggered)host.showToast("استمر بالضغط على E-STOP لمدة ثانيتين");return true;}return true;}
    private void latchEstopUi(){estopLatched=true;estopPressActive=false;resetDriveInputs();renderEstopButton();}
    private void requestEstopReset(){resetDriveInputs();stop.setEnabled(false);host.io().execute(()->{try{host.robotApi().emergencyReset();root.post(()->{estopLatched=false;stop.setEnabled(true);renderEstopButton();host.refreshAllTabs();host.showToast("تم إلغاء E-STOP");});}catch(Exception e){root.post(()->{estopLatched=true;stop.setEnabled(true);renderEstopButton();host.showToast("تعذر إلغاء E-STOP");});}});}
    private void syncEstopFromDiagnostics(JSONObject d){if(d!=null){boolean latched=false,known=false;if(d.has("estop_latched")){known=true;latched=d.optBoolean("estop_latched",false);}JSONObject safety=d.optJSONObject("safety");if(safety!=null&&safety.has("estop_latched")){known=true;latched=latched||safety.optBoolean("estop_latched",false);}if(known&&latched){estopLatched=true;resetDriveInputs();}}renderEstopButton();}
    private void renderEstopButton(){if(estopLatched){stop.setText("E-STOP ●\nلمسة لإلغاء");stop.setBackgroundColor(0xFFC62828);}else{stop.setText("E-STOP\nضغط 2ث");stop.setBackgroundColor(0xFF2E7D32);}}

    private boolean remoteMode(){return "remote".equalsIgnoreCase(host.preferences().getString("connection_mode","direct"));}
    private String effectiveFlashMode(){return remoteMode()?host.preferences().getString("flash_mode","off"):host.flashMode();}
    private void cycleFlash(){String next="on".equals(effectiveFlashMode())?"off":"on";if(!remoteMode()){host.io().execute(()->{if(!host.cameraReachable()){host.showToast("الكاميرا غير متصلة");return;}host.setFlashMode(next);});return;}host.io().execute(()->{try{host.robotApi().cameraFlash(next);host.preferences().edit().putString("flash_mode",next).apply();root.post(this::refresh);host.showToast("الفلاش: "+("on".equals(next)?"تشغيل":"إيقاف"));}catch(Exception e){host.showToast("تعذر إرسال أمر الفلاش البعيد");}});}
    private void toggleRelay(){JSONObject d=host.latestDiagnostics();JSONObject r=d==null?null:d.optJSONObject("relay");boolean on=r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);boolean target=!on;relayButton.setEnabled(false);host.io().execute(()->{try{host.robotApi().mqPower(target);root.post(()->{relayButton.setEnabled(true);host.showToast(target?"تم تشغيل حساسات MQ":"تم إطفاء حساسات MQ");host.refreshAllTabs();});}catch(Exception e){root.post(()->{relayButton.setEnabled(true);host.showToast("تعذر تغيير حالة ريلاي MQ");});}});}

    private void toggleRecording(){if(recordingStopping)return;if(recorder.isRecording()){requestStopRecording(true);return;}try{String name="HazRob_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.US).format(new Date());File f=recorder.start(name);record.setText("إيقاف التسجيل");recordInfo.setText("REC ●   "+f.getName());}catch(Exception e){recordInfo.setText("VIDEO ERROR");host.showToast("تعذر بدء التسجيل");}}
    private void requestStopRecording(boolean toastAfter){if(recordingStopping)return;recordingStopping=true;record.setEnabled(false);recordInfo.setText("VIDEO SAVING…");host.io().execute(()->{MjpegRecorder.StopResult r=recorder.stopGracefully(8000);if(r.galleryUri!=null){try{if(r.video!=null&&r.video.isFile())r.video.delete();}catch(Exception ignored){}try{if(r.index!=null&&r.index.isFile())r.index.delete();}catch(Exception ignored){}}root.post(()->{recordingStopping=false;record.setEnabled(true);record.setText("تسجيل");recordInfo.setText("VIDEO   "+r.frames+" frames   drop "+r.dropped);if(toastAfter)host.showToast(r.galleryUri!=null?"تم حفظ الفيديو":"انتهى التسجيل");});});}

    private void showFlame(FlameFrameAnalyzer.Result r){if(r==null||r.frame==null||r.temporal==null)return;lastFlameResult=r;lastFlameState=r.temporal.state;if(r.temporal.state!=FlameEvidenceTracker.State.CLEAR&&r.frame.hasRegion())overlay.setFlameRegion(r.frame.left,r.frame.top,r.frame.right,r.frame.bottom,true);else overlay.clearFlame();if(r.temporal.state==FlameEvidenceTracker.State.PERSISTENT){flameBadge.setText("🔥 ●");flameBadge.setTextColor(0xFFFF7043);}else if(r.temporal.state==FlameEvidenceTracker.State.CANDIDATE){flameBadge.setText("🔥 ?");flameBadge.setTextColor(0xFFFFB74D);}else{flameBadge.setText("🔥 ○");flameBadge.setTextColor(0xFFCFD8DC);}}

    public View view(){return root;}public void resetJoystick(){resetDriveInputs();}
    public void startCamera(){if(remoteMode()){String url=host.preferences().getString("remote_video_url","").trim();String token=new RemoteVideoCredentialStore(host.preferences()).load();if(url.isEmpty()){showCameraStatus("retrying","Remote video not configured");return;}video.startWss(url,token);return;}String h=host.cameraHost();if(h==null||h.trim().isEmpty()){showCameraStatus("retrying","");return;}h=h.trim();video.start(new String[]{"http://"+h+":81/stream","http://"+h+"/stream"});}
    public void stopCamera(){video.stop();flameAnalyzer.reset();overlay.clearFlame();overlay.clearGas();lastFlameState=FlameEvidenceTracker.State.CLEAR;lastFlameResult=null;if(recorder.isRecording()&&!recordingStopping)requestStopRecording(false);}
    public void releaseCamera(){video.stop();stop.removeCallbacks(estopHoldAction);estopPressActive=false;flameAnalyzer.release();overlay.clearFlame();overlay.clearGas();gasDirection.reset();if(recorder.isRecording()||recorder.isClosing())recorder.stopNow();recorder.release();video.release();}

    public void refresh(){
        flash.setText("فلاش "+("on".equals(effectiveFlashMode())?"ON":"OFF"));if(recorder.isRecording())recordInfo.setText("REC ●   "+recorder.frameCount()+" frames   drop "+recorder.droppedFrames());renderDriveModeButton();
        WifiSignalReader.Snapshot ws=remoteMode()?null:WifiSignalReader.read(appContext);if(ws!=null&&ws.available){wifiBadge.setText("Wi‑Fi "+ws.qualityPercent+"%  "+ws.rssiDbm+" dBm");wifiBadge.setTextColor(ws.qualityPercent>=60?0xFF81C784:ws.qualityPercent>=30?0xFFFFB74D:0xFFFF7043);}else{wifiBadge.setText(remoteMode()?"REMOTE":"Wi‑Fi —");wifiBadge.setTextColor(0xFFCFD8DC);}
        LiveGasStateEngine.Snapshot gas=host.liveGasState();renderGas(gas);
        int target=host.trackingTargetId();String targetName=host.trackingTargetName();if(target!=lastTarget){lastTarget=target;gasDirection.reset();overlay.clearGas();}cancelTracking.setVisibility(target>=0?View.VISIBLE:View.GONE);trackingBadge.setText(target>=0?"تتبع ● "+targetName:"تتبع —");trackingBadge.setTextColor(target>=0?0xFF81C784:0xFFCFD8DC);
        JSONObject d=host.latestDiagnostics();syncEstopFromDiagnostics(d);if(d==null){status.setText("ROBOT ○");status.setTextColor(0xFFFFB74D);irBadge.setText("أرض —");irBadge.setTextColor(0xFFCFD8DC);distanceBadge.setText("أمام — cm");distanceBadge.setTextColor(0xFFCFD8DC);relayButton.setText("MQ —");relayButton.setBackgroundColor(0xCC455A64);safetyWarning.setVisibility(View.GONE);drive.setText("L "+host.driveLeft()+"   R "+host.driveRight()+"   LINK —");overlay.clearGas();gasDirection.reset();return;}
        status.setText("ROBOT ●");status.setTextColor(0xFF81C784);JSONObject relay=d.optJSONObject("relay");boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);relayButton.setText(mqOn?"MQ ON":"MQ OFF");relayButton.setBackgroundColor(mqOn?0xCC2E7D32:0xCC455A64);
        JSONObject ir=d.optJSONObject("ir");boolean cliff=ir!=null&&ir.optBoolean("cliff_warning",ir.optBoolean("warning",false)),ground=ir!=null&&ir.optBoolean("ground_detected",!cliff);irBadge.setText(cliff?"⚠ لا توجد أرضية":ground?"أرض ●":"أرض ؟");irBadge.setTextColor(cliff?0xFFFF7043:ground?0xFF81C784:0xFFFFB74D);
        JSONObject device=d.optJSONObject("device"),proximity=device==null?null:device.optJSONObject("proximity");boolean collision=proximity!=null&&proximity.optBoolean("warning",false),stopZone=proximity!=null&&proximity.optBoolean("stop_zone",false);Object cmObj=proximity==null?null:proximity.opt("distance_cm");if(cmObj instanceof Number){double cm=((Number)cmObj).doubleValue();if(Double.isFinite(cm)){distanceBadge.setText(String.format(Locale.US,"أمام %.1f cm",cm));distanceBadge.setTextColor(stopZone?0xFFFF5252:collision?0xFFFFB74D:0xFF81C784);}else distanceBadge.setText("أمام — cm");}else distanceBadge.setText("أمام — cm");
        if(cliff){safetyWarning.setText("⚠ خطر السقوط — لا توجد أرضية أسفل الروبوت");safetyWarning.setVisibility(View.VISIBLE);}else if(stopZone){safetyWarning.setText("⛔ جسم أمامي داخل منطقة التوقف");safetyWarning.setVisibility(View.VISIBLE);}else if(collision){safetyWarning.setText("⚠ جسم أمامي قريب");safetyWarning.setVisibility(View.VISIBLE);}else safetyWarning.setVisibility(View.GONE);
        JSONObject x=d.optJSONObject("drive");if(x!=null){boolean stale=x.optBoolean("stale_stop",false);drive.setText("L "+x.optInt("left",host.driveLeft())+"   R "+x.optInt("right",host.driveRight())+"   "+(estopLatched?"E-STOP":stale?"STALE STOP":"DIRECT"));drive.setTextColor(estopLatched?0xFFFF7043:stale?0xFFFFB74D:Color.WHITE);}
        if(target<0){overlay.clearGas();return;}GasDirectionEstimator.Result g=gasDirection.update(d,host.driveLeft(),host.driveRight(),SystemClock.elapsedRealtime(),target);if(g.valid)overlay.setGasDirection(g.angleRad,g.confidence);else overlay.clearGas();trackingBadge.setText("تتبع ● "+targetName);
    }

    private void renderGas(LiveGasStateEngine.Snapshot g){if(g==null){gasBadge.setText("غازات —");gasBadge.setTextColor(0xFFCFD8DC);return;}switch(g.state){case RESPONSE_ALERT:gasBadge.setText("⛔ استجابة غاز قوية • "+g.respondingChannels+" قنوات");gasBadge.setTextColor(0xFFFF5252);break;case RESPONSE_WATCH:gasBadge.setText("⚠ استجابة غاز متعددة • "+g.respondingChannels+" قنوات");gasBadge.setTextColor(0xFFFFB74D);break;case INSTANT_RESPONSE:gasBadge.setText("⚠ استجابة MQ فورية");gasBadge.setTextColor(0xFFFFB74D);break;case NORMAL:gasBadge.setText("غازات ● طبيعي");gasBadge.setTextColor(0xFF81C784);break;case WARMING_UP:gasBadge.setText("MQ • تسخين");gasBadge.setTextColor(0xFF90CAF9);break;case BASELINE_REQUIRED:gasBadge.setText("MQ • خط أساس مطلوب");gasBadge.setTextColor(0xFFFFB74D);break;case DEGRADED:gasBadge.setText("MQ • قنوات غير كافية");gasBadge.setTextColor(0xFFFF7043);break;default:gasBadge.setText("غازات —");gasBadge.setTextColor(0xFFCFD8DC);}}
    private void applyLandscapeFullscreenChrome(){if(!landscape)return;try{ViewParent p=root.getParent();if(p instanceof LinearLayout){LinearLayout wrapper=(LinearLayout)p;for(int i=0;i<wrapper.getChildCount();i++){View v=wrapper.getChildAt(i);if(v!=root)v.setVisibility(View.GONE);}ViewParent p2=wrapper.getParent();if(p2 instanceof FrameLayout){FrameLayout page=(FrameLayout)p2;ViewParent p3=page.getParent();if(p3 instanceof LinearLayout){LinearLayout shell=(LinearLayout)p3;for(int i=0;i<shell.getChildCount();i++){View v=shell.getChildAt(i);if(v!=page)v.setVisibility(View.GONE);}}}}if(uiContext instanceof Activity){Activity a=(Activity)uiContext;a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}}catch(Exception ignored){}}
}
