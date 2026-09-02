package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.camera.MjpegRecorder;
import com.servoc.app.camera.MjpegView;
import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.control.JoystickView;
import com.servoc.app.data.EvidenceFileStore;
import com.servoc.app.data.EvidenceStore;
import com.servoc.app.navigation.GasDirectionEstimator;
import com.servoc.app.navigation.MqRelayPlanner;
import com.servoc.app.vision.FlameEvidenceTracker;
import com.servoc.app.vision.FlameFrameAnalyzer;
import com.servoc.app.vision.HazardOverlayView;

import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ControlTab {
    private final Context appContext;
    private final HazRobHost host;
    private final LinearLayout root;
    private final TextView status,drive,recordInfo,cameraInfo,cameraBadge,flameInfo,flameBadge,gasTrackInfo,irBadge;
    private final Button flash,record;
    private final Spinner gasTarget;
    private final ArrayList<Integer> gasTargetIds=new ArrayList<>();
    private final ChemicalRepository chemicals;
    private final MjpegView video;
    private final MjpegRecorder recorder;
    private final FlameFrameAnalyzer flameAnalyzer;
    private final HazardOverlayView overlay;
    private final GasDirectionEstimator gasDirection=new GasDirectionEstimator();
    private final JoystickView joystick;
    private volatile boolean recordingStopping=false;
    private volatile String flameEvidenceSessionId;
    private volatile long flameEvidenceStartedElapsed;
    private FlameEvidenceTracker.State lastFlameState=FlameEvidenceTracker.State.CLEAR;
    private FlameFrameAnalyzer.Result lastFlameResult;
    private String gasTargetSignature="",selectedGasName="";
    private int selectedGas=-1,trackingRelayMask=0;
    private boolean updatingGasTarget=false;

    public ControlTab(Context c,HazRobHost host){
        this.appContext=c.getApplicationContext();this.host=host;this.chemicals=new ChemicalRepository(host.database());
        final boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));root.setBackgroundColor(0xFF0E1116);

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);recorder=new MjpegRecorder(c);flameAnalyzer=new FlameFrameAnalyzer(r->root.post(()->showFlame(r)));
        video.setFrameListener((jpeg,t)->{recorder.onJpegFrame(jpeg,t);flameAnalyzer.onJpegFrame(jpeg,t);});
        video.setStatusListener(this::showCameraStatus);
        cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HazardOverlayView(c);cameraBox.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        joystick=new JoystickView(c);int size=UiKit.dp(c,landscape?120:150);
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);
        joystick.setListener((l,r,active)->host.setDriveCommand(l,r));

        status=badge(c,"الروبوت غير متصل");
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);sp.setMargins(0,UiKit.dp(c,6),UiKit.dp(c,6),0);cameraBox.addView(status,sp);

        irBadge=badge(c,"IR: —");
        FrameLayout.LayoutParams ibp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);ibp.setMargins(0,UiKit.dp(c,40),UiKit.dp(c,6),0);cameraBox.addView(irBadge,ibp);

        cameraBadge=badge(c,"CAM: بانتظار الاتصال");
        FrameLayout.LayoutParams cbp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.TOP);cbp.setMargins(UiKit.dp(c,6),UiKit.dp(c,6),0,0);cameraBox.addView(cameraBadge,cbp);

        flameBadge=badge(c,"🔥 اللهب: بانتظار الفيديو");
        FrameLayout.LayoutParams fbp=new FrameLayout.LayoutParams(-2,-2,Gravity.LEFT|Gravity.BOTTOM);fbp.setMargins(UiKit.dp(c,6),0,0,UiKit.dp(c,6));cameraBox.addView(flameBadge,fbp);

        ScrollView detailsScroll=new ScrollView(c);detailsScroll.setFillViewport(false);detailsScroll.setBackgroundColor(0xFF10151B);detailsScroll.setVisibility(View.GONE);
        LinearLayout details=UiKit.vbox(c);details.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));

        cameraInfo=UiKit.small(c,"الكاميرا: لم يبدأ الاختبار");cameraInfo.setBackgroundColor(0xFF151A20);details.addView(cameraInfo,UiKit.full(c));
        recordInfo=UiKit.small(c,"التسجيل: متوقف • MP4/H.264 مستقل • Movies/HazRob • غير مرتبط بأي جلسة أو تقرير");recordInfo.setBackgroundColor(0xFF151A20);details.addView(recordInfo,UiKit.full(c));
        flameInfo=UiKit.small(c,"دليل اللهب البصري التجريبي: بانتظار الفيديو");flameInfo.setBackgroundColor(0xFF151A20);flameInfo.setTextColor(0xFF90A4AE);details.addView(flameInfo,UiKit.full(c));

        LinearLayout gasCard=UiKit.card(c);gasCard.addView(UiKit.title(c,"تتبع مصدر الغاز"),UiKit.full(c));
        gasCard.addView(UiKit.small(c,"اختر المركب المراد تتبعه. HazRob يختار تلقائيًا صف MQ أو صفين من R1–R4 بحسب الحساسات الأكثر فائدة للمركب، ولا يسمح بتجاوز صفين لحماية ميزانية طاقة 5V الحالية."),UiKit.full(c));
        gasTarget=new Spinner(c);gasTarget.setBackgroundColor(0xFF252B33);gasCard.addView(gasTarget,UiKit.full(c));
        gasTrackInfo=UiKit.small(c,"لا يوجد هدف محدد للتتبع");gasTrackInfo.setBackgroundColor(0xFF151A20);gasCard.addView(gasTrackInfo,UiKit.full(c));details.addView(gasCard,UiKit.full(c));
        details.addView(UiKit.small(c,"IR تحذير فقط ولا يوقف المركبة. الفيديو يسجل كملف MP4 مستقل في معرض الهاتف، بينما تحليل اللهب يبقى حدثًا تحليليًا مستقلًا عن ملف الفيديو."),UiKit.full(c));
        detailsScroll.addView(details,new ScrollView.LayoutParams(-1,-2));

        Button detailsToggle=UiKit.button(c,"التتبع");
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(UiKit.dp(c,landscape?92:105),UiKit.dp(c,44),Gravity.LEFT|Gravity.TOP);tp.setMargins(UiKit.dp(c,6),UiKit.dp(c,42),0,0);cameraBox.addView(detailsToggle,tp);
        detailsToggle.setOnClickListener(v->{boolean show=detailsScroll.getVisibility()!=View.VISIBLE;detailsScroll.setVisibility(show?View.VISIBLE:View.GONE);detailsToggle.setText(show?"إغلاق":"التتبع");});

        root.addView(cameraBox,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout actions=UiKit.hbox(c);
        Button stop=UiKit.button(c,"إيقاف طارئ");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.3f));
        flash=UiKit.button(c,"الفلاش: إيقاف");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1f));
        record=UiKit.button(c,"تسجيل الفيديو");record.setOnClickListener(v->toggleRecording());actions.addView(record,UiKit.weight(1f));root.addView(actions,UiKit.full(c));
        drive=UiKit.small(c,"يسار 0   يمين 0   أمان الاتصال —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));
        root.addView(detailsScroll,new LinearLayout.LayoutParams(-1,UiKit.dp(c,landscape?118:205)));

        gasTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(updatingGasTarget||pos<0||pos>=gasTargetIds.size())return;
                int next=gasTargetIds.get(pos);if(next==selectedGas)return;
                selectedGas=next;selectedGasName=next<0?"":chemicalName(next);gasDirection.reset();overlay.clearGas();applyTrackingPower(next);
                gasTrackInfo.setText(next<0?"لا يوجد هدف محدد للتتبع":"الهدف: "+selectedGasName+" • "+rowLabel(trackingRelayMask)+" • ابدأ الحركة بعد استقرار الحساسات");
            }
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
    }

    private TextView badge(Context c,String text){TextView v=UiKit.small(c,text);v.setTextColor(0xFF90A4AE);v.setBackgroundColor(0xCC151A20);v.setPadding(UiKit.dp(c,8),UiKit.dp(c,4),UiKit.dp(c,8),UiKit.dp(c,4));return v;}

    private void showCameraStatus(String state,String detail){
        if("streaming".equals(state)){cameraBadge.setText("CAM: بث مباشر");cameraBadge.setTextColor(0xFF81C784);cameraInfo.setText("الكاميرا متصلة • "+detail);}
        else if("connecting".equals(state)){cameraBadge.setText("CAM: اتصال...");cameraBadge.setTextColor(0xFFFFB74D);cameraInfo.setText("جارٍ اختبار مسار الفيديو • "+detail);}
        else if("retrying".equals(state)){cameraBadge.setText("CAM: إعادة محاولة");cameraBadge.setTextColor(0xFFFF7043);cameraInfo.setText("تعذر البث الحالي، يجري اختبار مسارات بديلة • "+detail);}
        else{cameraBadge.setText("CAM: متوقفة");cameraBadge.setTextColor(0xFF90A4AE);}
    }

    private void cycleFlash(){
        final String next="on".equals(host.flashMode())?"off":"on";
        host.io().execute(()->{
            if(!host.cameraReachable()){host.showToast("الكاميرا غير متصلة على "+host.cameraHost()+"؛ تحقق من SSID وFirmware الكاميرا");return;}
            host.setFlashMode(next);
        });
    }

    private void applyTrackingPower(int chemical){
        int mask=chemical<0?0:MqRelayPlanner.forChemical(chemical);
        if(!MqRelayPlanner.withinPowerBudget(mask)){host.showToast("خطة MQ تتجاوز حد صفين؛ لم يتم تشغيلها");return;}
        if(mask==trackingRelayMask)return;
        trackingRelayMask=mask;
        host.io().execute(()->{
            try{host.robotApi().relayMask(mask);}
            catch(Exception e){host.showToast("تعذر تطبيق صفوف MQ "+rowLabel(mask));}
        });
    }
    private static String rowLabel(int mask){return MqRelayPlanner.rowsLabel(mask);}

    private void toggleRecording(){
        if(recordingStopping){host.showToast("جارٍ إنهاء التسجيل السابق");return;}
        if(recorder.isRecording()){requestStopRecording(true);return;}
        try{
            String name="HazRob_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.US).format(new Date());File f=recorder.start(name);
            record.setText("إيقاف التسجيل");recordInfo.setText("جارٍ التسجيل • MP4/H.264 مستقل • "+f.getName());host.showToast("بدأ تسجيل الفيديو المستقل");
        }catch(Exception e){host.showToast("تعذر بدء تسجيل الفيديو");recordInfo.setText("التسجيل: خطأ في التخزين");}
    }

    private void requestStopRecording(boolean toastAfter){
        if(recordingStopping)return;recordingStopping=true;record.setEnabled(false);recordInfo.setText("جارٍ إنهاء MP4 وحفظه في Movies/HazRob...");
        host.io().execute(()->{
            MjpegRecorder.StopResult r=recorder.stopGracefully(8000);
            // Internal files are only a transient encoder workspace. Once MediaStore succeeds,
            // leave the Gallery MP4 as the sole persistent video artifact.
            if(r.galleryUri!=null){
                try{if(r.video!=null&&r.video.isFile())r.video.delete();}catch(Exception ignored){}
                try{if(r.index!=null&&r.index.isFile())r.index.delete();}catch(Exception ignored){}
            }
            root.post(()->{
                recordingStopping=false;record.setEnabled(true);record.setText("تسجيل الفيديو");
                String err=r.error==null?"":" • خطأ "+r.error;String where=r.galleryUri!=null?"Movies/HazRob":"ملف استرداد داخل التطبيق";
                recordInfo.setText("تم الحفظ • "+r.frames+" إطار • مسقط "+r.dropped+" • "+where+err+"\n"+(r.displayName==null?"":r.displayName));
                if(toastAfter)host.showToast(r.galleryUri!=null?"تم حفظ MP4 المستقل في معرض الهاتف":r.clean()?"تم إيقاف التسجيل":"تعذر تصدير MP4 إلى المعرض");
            });
        });
    }

    private void showFlame(FlameFrameAnalyzer.Result r){
        if(r==null||r.frame==null||r.temporal==null)return;lastFlameResult=r;
        FlameEvidenceTracker.State now=r.temporal.state,previous=lastFlameState;lastFlameState=now;
        if(now!=FlameEvidenceTracker.State.CLEAR&&r.frame.hasRegion())overlay.setFlameRegion(r.frame.left,r.frame.top,r.frame.right,r.frame.bottom,true);else overlay.clearFlame();
        String state=now==FlameEvidenceTracker.State.PERSISTENT?"مستمر":now==FlameEvidenceTracker.State.CANDIDATE?"مرشح":"لا دليل";
        int color=now==FlameEvidenceTracker.State.PERSISTENT?0xFFFF7043:now==FlameEvidenceTracker.State.CANDIDATE?0xFFFFB74D:0xFF90A4AE;
        flameBadge.setText("🔥 اللهب: "+state);flameBadge.setTextColor(color);
        StringBuilder s=new StringBuilder("دليل اللهب البصري التجريبي: ").append(state).append(" • score ").append(String.format(Locale.US,"%.0f%%",100*r.frame.score)).append(" • pixels ").append(String.format(Locale.US,"%.1f%%",100*r.frame.candidateRatio)).append(" • EMA ").append(String.format(Locale.US,"%.0f%%",100*r.temporal.ema));
        if(r.frame.hasRegion())s.append(String.format(Locale.US,"\nمنطقة تقريبية L%.2f T%.2f R%.2f B%.2f",r.frame.left,r.frame.top,r.frame.right,r.frame.bottom));
        flameInfo.setText(s.toString());flameInfo.setTextColor(color);
        if(now==FlameEvidenceTracker.State.PERSISTENT&&previous!=FlameEvidenceTracker.State.PERSISTENT)beginFlameEvidence(r);
        else if(previous==FlameEvidenceTracker.State.PERSISTENT&&now==FlameEvidenceTracker.State.CLEAR)endFlameEvidence(r,"clear");
    }

    private void beginFlameEvidence(FlameFrameAnalyzer.Result r){
        try{flameEvidenceSessionId=EvidenceStore.startStandaloneSession(host.database(),host.operatorRole(),"vision_flame_event");flameEvidenceStartedElapsed=r.elapsedMs;persistFlameEvidence("flame_visual_onset",r,flameEvidenceSessionId,0,true,false,"persistent");}
        catch(Exception ignored){flameEvidenceSessionId=null;flameEvidenceStartedElapsed=0;}
    }

    private void endFlameEvidence(FlameFrameAnalyzer.Result r,String reason){
        final String session=flameEvidenceSessionId;if(session==null)return;long elapsed=r==null?SystemClock.elapsedRealtime():r.elapsedMs;long t=Math.max(0,elapsed-flameEvidenceStartedElapsed);
        persistFlameEvidence("flame_visual_clear",r,session,t,false,true,reason);flameEvidenceSessionId=null;flameEvidenceStartedElapsed=0;
    }

    private void persistFlameEvidence(String type,FlameFrameAnalyzer.Result r,String session,long t,boolean snapshot,boolean finish,String reason){
        if(session==null)return;final String observed=EvidenceStore.isoNow();final byte[] jpeg=snapshot&&r!=null?r.jpeg:null;final double confidence=r==null||r.temporal==null?Double.NaN:r.temporal.ema;
        host.io().execute(()->{
            JSONObject p=new JSONObject();
            try{
                p.put("observed_utc",observed);p.put("model","FlameColorModel+TemporalEvidence/0.7.1");p.put("reason",reason);
                if(r!=null&&r.frame!=null&&r.temporal!=null){p.put("frame_score",r.frame.score);p.put("candidate_ratio",r.frame.candidateRatio);p.put("bright_candidate_ratio",r.frame.brightCandidateRatio);p.put("ema",r.temporal.ema);p.put("state",r.temporal.state.name());if(r.frame.hasRegion()){p.put("left",r.frame.left);p.put("top",r.frame.top);p.put("right",r.frame.right);p.put("bottom",r.frame.bottom);}}
                if(jpeg!=null){EvidenceFileStore.SavedFile f=EvidenceFileStore.saveJpeg(appContext,"HazRob_flame_"+System.currentTimeMillis(),jpeg);p.put("snapshot_uri",f.uri);p.put("snapshot_sha256",f.sha256);p.put("snapshot_bytes",f.bytes);JSONObject im=new JSONObject();im.put("sha256",f.sha256);im.put("bytes",f.bytes);im.put("event",type);EvidenceStore.saveMedia(host.database(),session,"image/jpeg",f.uri,observed,observed,im.toString());}
            }catch(Exception e){try{p.put("snapshot_error",e.getClass().getSimpleName());}catch(Exception ignored){}}
            try{EvidenceStore.saveEvent(host.database(),session,t,type,Double.isFinite(confidence)?confidence:null,p.toString());if(finish)EvidenceStore.finishStandaloneSession(host.database(),session,p.toString());}catch(Exception ignored){}
        });
    }

    public View view(){return root;}
    public void resetJoystick(){joystick.reset();}

    public void startCamera(){
        flameBadge.setText("🔥 اللهب: بانتظار الفيديو");flameBadge.setTextColor(0xFF90A4AE);
        String h=host.cameraHost();if(h==null||h.trim().isEmpty()){showCameraStatus("retrying","عنوان الكاميرا فارغ");return;}h=h.trim();
        video.start(new String[]{"http://"+h+":81/stream","http://"+h+"/stream","http://"+h+":81/","http://"+h+"/mjpeg"});
    }

    public void stopCamera(){
        if(lastFlameState==FlameEvidenceTracker.State.PERSISTENT)endFlameEvidence(lastFlameResult,"camera_stop");
        video.stop();flameAnalyzer.reset();overlay.clearFlame();lastFlameState=FlameEvidenceTracker.State.CLEAR;lastFlameResult=null;
        flameInfo.setText("دليل اللهب البصري التجريبي: متوقف مع الكاميرا");flameInfo.setTextColor(0xFF90A4AE);flameBadge.setText("🔥 اللهب: متوقف");flameBadge.setTextColor(0xFF90A4AE);
        if(recorder.isRecording()&&!recordingStopping)requestStopRecording(false);
        if(trackingRelayMask!=0){selectedGas=-1;selectedGasName="";gasDirection.reset();overlay.clearGas();applyTrackingPower(-1);}
    }

    public void releaseCamera(){
        if(lastFlameState==FlameEvidenceTracker.State.PERSISTENT)endFlameEvidence(lastFlameResult,"release");
        video.stop();flameAnalyzer.release();overlay.clearFlame();overlay.clearGas();gasDirection.reset();if(trackingRelayMask!=0)applyTrackingPower(-1);
        if(recorder.isRecording()||recorder.isClosing())recorder.stopNow();recorder.release();video.release();
    }

    public void refresh(){
        flash.setText("الفلاش: "+("on".equals(host.flashMode())?"تشغيل":"إيقاف"));
        if(!recordingStopping&&!recorder.isRecording()&&recorder.lastError()!=null)recordInfo.setText("التسجيل متوقف • "+recorder.lastError());
        if(recorder.isRecording())recordInfo.setText("جارٍ التسجيل • MP4 مستقل • "+recorder.frameCount()+" إطار • مسقط "+recorder.droppedFrames());

        JSONObject d=host.latestDiagnostics();
        if(d==null){status.setText("الروبوت غير متصل");status.setTextColor(0xFFFFB74D);irBadge.setText("IR: —");irBadge.setTextColor(0xFF90A4AE);drive.setText("يسار "+host.driveLeft()+"   يمين "+host.driveRight()+"   أمان الاتصال —");overlay.clearGas();gasDirection.reset();gasTrackInfo.setText(selectedGas<0?"لا يوجد هدف محدد للتتبع":"الهدف: "+selectedGasName+" • الاتصال بالروبوت مفقود");return;}

        status.setText("متصل • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);
        JSONObject ir=d.optJSONObject("ir");boolean irWarn=ir!=null&&ir.optBoolean("warning",false);irBadge.setText(irWarn?"⚠ IR: قرب/عائق":"IR: واضح");irBadge.setTextColor(irWarn?0xFFFF7043:0xFF81C784);
        JSONObject x=d.optJSONObject("drive");if(x!=null){boolean back=x.optBoolean("retrace",false),fail=x.optBoolean("failsafe",false);String safety=back?"عودة تلقائية":fail?"فقد اتصال":"سليم";drive.setText("يسار "+x.optInt("left",host.driveLeft())+"   يمين "+x.optInt("right",host.driveRight())+"   أمان الاتصال "+safety);drive.setTextColor(back?0xFFFFB74D:Color.LTGRAY);}

        refreshGasTargets(d);
        if(selectedGas<0){overlay.clearGas();gasDirection.reset();gasTrackInfo.setText("اختر غازًا/مركبًا من القائمة لبدء حساب اتجاه مصدره");return;}
        GasDirectionEstimator.Result g=gasDirection.update(d,host.driveLeft(),host.driveRight(),SystemClock.elapsedRealtime(),selectedGas);
        if(g.valid){overlay.setGasDirection(g.angleRad,g.confidence);gasTrackInfo.setText("الهدف: "+selectedGasName+" • "+rowLabel(trackingRelayMask)+" • اتجاه نسبي "+bearingName(g.angleRad)+String.format(Locale.US," (%+.0f°) • ثقة اتجاه %.0f%%",Math.toDegrees(g.angleRad),100*g.confidence));}
        else{overlay.clearGas();gasTrackInfo.setText("الهدف: "+selectedGasName+" • "+rowLabel(trackingRelayMask)+" • نجمع تغير الاستجابة أثناء الحركة بعد استقرار الحساسات");}
    }

    private void refreshGasTargets(JSONObject d){
        Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d);List<ValidatedGasEngine.Result> rows=new ArrayList<>();
        for(ValidatedGasEngine.Result r:map.values())if(r!=null&&r.totalContributors>0&&r.confidence>=GasEngine.SUSPECTED&&MqSensorAffinity.forChemical(r.chemical).mask!=0)rows.add(r);
        rows.sort(CandidateRanker.COMPARATOR);if(rows.size()>10)rows=rows.subList(0,10);
        StringBuilder sig=new StringBuilder();for(ValidatedGasEngine.Result r:rows)sig.append(r.chemical).append(',');sig.append('|').append(selectedGas);if(sig.toString().equals(gasTargetSignature))return;gasTargetSignature=sig.toString();
        int keep=selectedGas,keepPos=0;ArrayList<String> labels=new ArrayList<>();gasTargetIds.clear();labels.add(rows.isEmpty()?"— لا توجد مركبات مكتشفة حاليًا —":"— اختر المركب المراد تتبع مصدره —");gasTargetIds.add(-1);
        for(ValidatedGasEngine.Result r:rows){int id=r.chemical;String name=chemicalName(id),level=CandidateRanker.evidenceLabel(r);labels.add(name+" • "+level+String.format(Locale.US," • اتفاق %.0f%%",r.agreement));gasTargetIds.add(id);if(id==keep)keepPos=labels.size()-1;}
        if(keep>=0&&keepPos==0){labels.add((selectedGasName.isEmpty()?chemicalName(keep):selectedGasName)+" • هدف التتبع الحالي");gasTargetIds.add(keep);keepPos=labels.size()-1;}
        updatingGasTarget=true;ArrayAdapter<String> a=new ArrayAdapter<>(appContext,android.R.layout.simple_spinner_item,labels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);gasTarget.setAdapter(a);gasTarget.setSelection(keepPos,false);updatingGasTarget=false;
    }

    private String chemicalName(int legacyId){ChemicalRepository.Chemical c=chemicals.byLegacyId(legacyId);if(c==null)return"Chemical #"+legacyId;String ar=c.arabicName==null?"":c.arabicName.trim(),en=c.name==null?"":c.name.trim();return!ar.isEmpty()?ar:!en.isEmpty()?en:"Chemical #"+legacyId;}
    private static String bearingName(double a){double d=Math.toDegrees(a);if(d>=-22.5&&d<22.5)return"أمام";if(d>=22.5&&d<67.5)return"أمام-يمين";if(d>=67.5&&d<112.5)return"يمين";if(d>=112.5&&d<157.5)return"خلف-يمين";if(d>=157.5||d<-157.5)return"خلف";if(d>=-157.5&&d<-112.5)return"خلف-يسار";if(d>=-112.5&&d<-67.5)return"يسار";return"أمام-يسار";}
}
