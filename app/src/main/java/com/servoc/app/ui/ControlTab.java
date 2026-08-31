package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
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
    private final TextView status,drive,recordInfo,flameInfo,gasTrackInfo;
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
    private volatile String recordingSessionId,recordingStartedTs;
    private volatile long recordingStartedElapsed;
    private volatile boolean recordingStopping=false;
    private volatile String flameEvidenceSessionId;
    private volatile long flameEvidenceStartedElapsed;
    private volatile boolean flameEvidenceStandalone=false;
    private FlameEvidenceTracker.State lastFlameState=FlameEvidenceTracker.State.CLEAR;
    private FlameFrameAnalyzer.Result lastFlameResult;
    private String gasTargetSignature="",selectedGasName="";
    private int selectedGas=-1;
    private boolean updatingGasTarget=false;

    public ControlTab(Context c,HazRobHost host){
        this.appContext=c.getApplicationContext();this.host=host;this.chemicals=new ChemicalRepository(host.database());
        final boolean landscape=c.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));root.setBackgroundColor(0xFF0E1116);

        FrameLayout cameraBox=new FrameLayout(c);cameraBox.setBackgroundColor(Color.BLACK);cameraBox.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        video=new MjpegView(c);recorder=new MjpegRecorder(c);flameAnalyzer=new FlameFrameAnalyzer(r->root.post(()->showFlame(r)));
        video.setFrameListener((jpeg,t)->{recorder.onJpegFrame(jpeg,t);flameAnalyzer.onJpegFrame(jpeg,t);});cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HazardOverlayView(c);cameraBox.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        joystick=new JoystickView(c);int size=UiKit.dp(c,landscape?120:150);
        FrameLayout.LayoutParams jp=new FrameLayout.LayoutParams(size,size,Gravity.RIGHT|Gravity.BOTTOM);jp.setMargins(0,0,UiKit.dp(c,10),UiKit.dp(c,10));cameraBox.addView(joystick,jp);
        joystick.setListener((l,r,active)->host.setDriveCommand(l,r));

        status=UiKit.small(c,"الروبوت غير متصل");status.setTextColor(0xFFFFB74D);status.setBackgroundColor(0xCC151A20);status.setPadding(UiKit.dp(c,8),UiKit.dp(c,4),UiKit.dp(c,8),UiKit.dp(c,4));
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);sp.setMargins(0,UiKit.dp(c,6),UiKit.dp(c,6),0);cameraBox.addView(status,sp);

        ScrollView detailsScroll=new ScrollView(c);detailsScroll.setFillViewport(false);detailsScroll.setBackgroundColor(0xFF10151B);detailsScroll.setVisibility(View.GONE);
        LinearLayout details=UiKit.vbox(c);details.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));

        recordInfo=UiKit.small(c,"التسجيل: متوقف • MP4/H.264 • الحفظ في معرض الهاتف");recordInfo.setBackgroundColor(0xFF151A20);details.addView(recordInfo,UiKit.full(c));
        flameInfo=UiKit.small(c,"دليل اللهب البصري التجريبي: بانتظار الفيديو");flameInfo.setBackgroundColor(0xFF151A20);flameInfo.setTextColor(0xFF90A4AE);details.addView(flameInfo,UiKit.full(c));

        LinearLayout gasCard=UiKit.card(c);gasCard.addView(UiKit.title(c,"تتبع مصدر الغاز"),UiKit.full(c));
        gasCard.addView(UiKit.small(c,"اختر من المركبات التي اكتشفها محرك التحليل في القياسات الحية. السهم يحسب اتجاهًا نسبيًا خاصًا بالمركب المختار باستخدام قنوات MQ ذات الصلة به."),UiKit.full(c));
        gasTarget=new Spinner(c);gasTarget.setBackgroundColor(0xFF252B33);gasCard.addView(gasTarget,UiKit.full(c));
        gasTrackInfo=UiKit.small(c,"لا يوجد هدف محدد للتتبع");gasTrackInfo.setBackgroundColor(0xFF151A20);gasCard.addView(gasTrackInfo,UiKit.full(c));details.addView(gasCard,UiKit.full(c));
        details.addView(UiKit.small(c,"المربع الأحمر يحدد منطقة اشتباه اللهب بصريًا. السهم الأخضر يبين اتجاه المصدر المرجح للغاز المختار فقط من تغير MQ مع الحركة. هذه مساعدة اتجاهية ولا ترسل أوامر قيادة ذاتية؛ أوامر القيادة تبقى بيد المشغل وتبقى حماية فقد الاتصال داخل ESP32."),UiKit.full(c));
        detailsScroll.addView(details,new ScrollView.LayoutParams(-1,-2));

        Button detailsToggle=UiKit.button(c,"التتبع");
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(UiKit.dp(c,landscape?92:105),UiKit.dp(c,44),Gravity.LEFT|Gravity.TOP);tp.setMargins(UiKit.dp(c,6),UiKit.dp(c,6),0,0);cameraBox.addView(detailsToggle,tp);
        detailsToggle.setOnClickListener(v->{boolean show=detailsScroll.getVisibility()!=View.VISIBLE;detailsScroll.setVisibility(show?View.VISIBLE:View.GONE);detailsToggle.setText(show?"إغلاق":"التتبع");});

        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1f);root.addView(cameraBox,cp);

        LinearLayout actions=UiKit.hbox(c);
        Button stop=UiKit.button(c,"إيقاف طارئ");stop.setTextColor(Color.WHITE);stop.setBackgroundColor(0xFFC62828);stop.setOnClickListener(v->host.emergencyStop());actions.addView(stop,UiKit.weight(1.3f));
        flash=UiKit.button(c,"الفلاش: إيقاف");flash.setOnClickListener(v->cycleFlash());actions.addView(flash,UiKit.weight(1f));
        record=UiKit.button(c,"تسجيل الفيديو");record.setOnClickListener(v->toggleRecording());actions.addView(record,UiKit.weight(1f));root.addView(actions,UiKit.full(c));

        drive=UiKit.small(c,"يسار 0   يمين 0   أمان الاتصال —");drive.setBackgroundColor(0xFF151A20);root.addView(drive,UiKit.full(c));
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,UiKit.dp(c,landscape?108:190));root.addView(detailsScroll,dp);

        gasTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(updatingGasTarget||pos<0||pos>=gasTargetIds.size())return;
                int next=gasTargetIds.get(pos);if(next==selectedGas)return;selectedGas=next;selectedGasName=next<0?"":chemicalName(next);gasDirection.reset();overlay.clearGas();
                gasTrackInfo.setText(next<0?"لا يوجد هدف محدد للتتبع":"الهدف: "+selectedGasName+" • ابدأ الحركة للحصول على تدرج اتجاهي");
            }
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
    }

    private void cycleFlash(){String m=host.flashMode();host.setFlashMode("off".equals(m)?"auto":"auto".equals(m)?"on":"off");}

    private void toggleRecording(){
        if(recordingStopping){host.showToast("جارٍ إنهاء التسجيل السابق");return;}
        if(recordingSessionId!=null){requestStopRecording("manual",true);return;}
        String session=null;
        try{
            session=EvidenceStore.startStandaloneSession(host.database(),host.operatorRole(),"video_capture");
            String name="HazRob_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.US).format(new Date());File f=recorder.start(name);
            recordingSessionId=session;recordingStartedTs=EvidenceStore.isoNow();recordingStartedElapsed=SystemClock.elapsedRealtime();record.setText("إيقاف التسجيل");recordInfo.setText("جارٍ التسجيل • MP4/H.264 • "+f.getName()+"\nجلسة "+session);host.showToast("بدأ تسجيل الفيديو");
        }catch(Exception e){if(session!=null)EvidenceStore.finishStandaloneSession(host.database(),session,"{\"terminal_state\":\"start_error\"}");host.showToast("تعذر بدء تسجيل الفيديو");recordInfo.setText("التسجيل: خطأ في التخزين");}
    }

    private void requestStopRecording(String reason,boolean toastAfter){
        final String session=recordingSessionId,start=recordingStartedTs;if(session==null||recordingStopping)return;
        recordingStopping=true;record.setEnabled(false);recordInfo.setText("جارٍ إنهاء MP4 وحفظه في معرض الهاتف...");
        host.io().execute(()->{
            MjpegRecorder.StopResult result=recorder.stopGracefully(8000);String end=EvidenceStore.isoNow();boolean mine=claimSession(session);
            if(mine)persistRecording(session,start,end,reason,result);
            root.post(()->{recordingStopping=false;record.setEnabled(true);record.setText("تسجيل الفيديو");if(mine){String err=result.error==null?"":" • خطأ "+result.error;String location=result.galleryUri!=null?" • المعرض/Movies/HazRob":" • نسخة التطبيق فقط";recordInfo.setText("تم الحفظ • "+result.frames+" إطار • مسقط "+result.dropped+location+err+"\n"+(result.displayName==null?"":result.displayName));if(toastAfter)host.showToast(result.galleryUri!=null?"تم حفظ الفيديو في معرض الهاتف":result.clean()?"تم إيقاف التسجيل":"تم حفظ التسجيل مع خطأ");}});
        });
    }

    private synchronized boolean claimSession(String expected){if(expected==null||!expected.equals(recordingSessionId))return false;recordingSessionId=null;recordingStartedTs=null;recordingStartedElapsed=0;return true;}

    private void persistRecording(String session,String start,String end,String reason,MjpegRecorder.StopResult r){
        JSONObject meta=new JSONObject();try{meta.put("format","MP4_H264");meta.put("container","video/mp4");meta.put("video_codec","H.264/AVC");meta.put("frames",r.frames);meta.put("dropped_frames",r.dropped);meta.put("stop_reason",reason);meta.put("camera_host",host.cameraHost());meta.put("clean_close",r.clean());meta.put("display_name",r.displayName);if(r.galleryUri!=null)meta.put("gallery_uri",r.galleryUri.toString());if(r.error!=null)meta.put("io_error",r.error);if(r.index!=null)meta.put("frame_timing_index_uri",Uri.fromFile(r.index).toString());}catch(Exception ignored){}
        String mediaUri=r.galleryUri!=null?r.galleryUri.toString():r.video!=null?Uri.fromFile(r.video).toString():null;String metadata=meta.toString();try{if(mediaUri!=null)EvidenceStore.saveMedia(host.database(),session,"video/mp4",mediaUri,start,end,metadata);EvidenceStore.finishStandaloneSession(host.database(),session,metadata);}catch(Exception ignored){}
    }

    private void showFlame(FlameFrameAnalyzer.Result r){
        if(r==null||r.frame==null||r.temporal==null)return;lastFlameResult=r;
        FlameEvidenceTracker.State now=r.temporal.state,previous=lastFlameState;lastFlameState=now;
        boolean showRegion=now!=FlameEvidenceTracker.State.CLEAR&&r.frame.hasRegion();
        if(showRegion)overlay.setFlameRegion(r.frame.left,r.frame.top,r.frame.right,r.frame.bottom,true);else overlay.clearFlame();
        String state=now==FlameEvidenceTracker.State.PERSISTENT?"مستمر":now==FlameEvidenceTracker.State.CANDIDATE?"مرشح":"لا دليل";
        StringBuilder s=new StringBuilder("دليل اللهب البصري التجريبي: ").append(state).append(" • score ").append(String.format(Locale.US,"%.0f%%",100*r.frame.score)).append(" • pixels ").append(String.format(Locale.US,"%.1f%%",100*r.frame.candidateRatio)).append(" • EMA ").append(String.format(Locale.US,"%.0f%%",100*r.temporal.ema));
        if(r.frame.hasRegion())s.append(String.format(Locale.US,"\nمنطقة تقريبية L%.2f T%.2f R%.2f B%.2f",r.frame.left,r.frame.top,r.frame.right,r.frame.bottom));
        flameInfo.setText(s.toString());flameInfo.setTextColor(now==FlameEvidenceTracker.State.PERSISTENT?0xFFFF7043:now==FlameEvidenceTracker.State.CANDIDATE?0xFFFFB74D:0xFF90A4AE);
        if(now==FlameEvidenceTracker.State.PERSISTENT&&previous!=FlameEvidenceTracker.State.PERSISTENT)beginFlameEvidence(r);
        else if(previous==FlameEvidenceTracker.State.PERSISTENT&&now==FlameEvidenceTracker.State.CLEAR)endFlameEvidence(r,"clear");
    }

    private void beginFlameEvidence(FlameFrameAnalyzer.Result r){
        String session=recordingSessionId;boolean standalone=false;
        try{if(session==null){session=EvidenceStore.startStandaloneSession(host.database(),host.operatorRole(),"vision_flame_event");standalone=true;}}
        catch(Exception e){session=null;}
        flameEvidenceSessionId=session;flameEvidenceStandalone=standalone;flameEvidenceStartedElapsed=r.elapsedMs;
        persistFlameEvidence("flame_visual_onset",r,session,eventTime(session,r.elapsedMs),true,false,"persistent");
    }

    private void endFlameEvidence(FlameFrameAnalyzer.Result r,String reason){
        final String session=flameEvidenceSessionId;if(session==null)return;final boolean finish=flameEvidenceStandalone;long elapsed=r==null?SystemClock.elapsedRealtime():r.elapsedMs;persistFlameEvidence("flame_visual_clear",r,session,eventTime(session,elapsed),false,finish,reason);flameEvidenceSessionId=null;flameEvidenceStandalone=false;flameEvidenceStartedElapsed=0;
    }

    private long eventTime(String session,long elapsed){
        if(session!=null&&session.equals(recordingSessionId)&&recordingStartedElapsed>0)return Math.max(0,elapsed-recordingStartedElapsed);
        if(session!=null&&session.equals(flameEvidenceSessionId)&&flameEvidenceStartedElapsed>0)return Math.max(0,elapsed-flameEvidenceStartedElapsed);return 0;
    }

    private void persistFlameEvidence(String type,FlameFrameAnalyzer.Result r,String session,long t,boolean snapshot,boolean finishStandalone,String reason){
        final String observed=EvidenceStore.isoNow();final byte[] jpeg=snapshot&&r!=null?r.jpeg:null;final double confidence=r==null||r.temporal==null?Double.NaN:r.temporal.ema;
        host.io().execute(()->{
            JSONObject p=new JSONObject();try{p.put("observed_utc",observed);p.put("model","FlameColorModel+TemporalEvidence/0.6");p.put("reason",reason);if(r!=null&&r.frame!=null&&r.temporal!=null){p.put("frame_score",r.frame.score);p.put("candidate_ratio",r.frame.candidateRatio);p.put("bright_candidate_ratio",r.frame.brightCandidateRatio);p.put("ema",r.temporal.ema);p.put("state",r.temporal.state.name());if(r.frame.hasRegion()){p.put("left",r.frame.left);p.put("top",r.frame.top);p.put("right",r.frame.right);p.put("bottom",r.frame.bottom);}}if(jpeg!=null){EvidenceFileStore.SavedFile f=EvidenceFileStore.saveJpeg(appContext,"HazRob_flame_"+System.currentTimeMillis(),jpeg);p.put("snapshot_uri",f.uri);p.put("snapshot_sha256",f.sha256);p.put("snapshot_bytes",f.bytes);JSONObject im=new JSONObject();im.put("sha256",f.sha256);im.put("bytes",f.bytes);im.put("event",type);EvidenceStore.saveMedia(host.database(),session,"image/jpeg",f.uri,observed,observed,im.toString());}}catch(Exception e){try{p.put("snapshot_error",e.getClass().getSimpleName());}catch(Exception ignored){}}try{EvidenceStore.saveEvent(host.database(),session,t,type,Double.isFinite(confidence)?confidence:null,p.toString());if(finishStandalone)EvidenceStore.finishStandaloneSession(host.database(),session,p.toString());}catch(Exception ignored){}
        });
    }

    public View view(){return root;}
    public void resetJoystick(){joystick.reset();}
    public void startCamera(){String h=host.cameraHost();if(h!=null&&!h.trim().isEmpty())video.start("http://"+h.trim()+":81/stream");}
    public void stopCamera(){if(lastFlameState==FlameEvidenceTracker.State.PERSISTENT)endFlameEvidence(lastFlameResult,"camera_stop");video.stop();flameAnalyzer.reset();overlay.clearFlame();lastFlameState=FlameEvidenceTracker.State.CLEAR;lastFlameResult=null;flameInfo.setText("دليل اللهب البصري التجريبي: متوقف مع الكاميرا");flameInfo.setTextColor(0xFF90A4AE);if(recordingSessionId!=null&&!recordingStopping)requestStopRecording("camera_stop",false);}

    /** Lifecycle teardown fallback: close and persist synchronously before DB/executor shutdown. */
    public void releaseCamera(){
        if(lastFlameState==FlameEvidenceTracker.State.PERSISTENT)endFlameEvidence(lastFlameResult,"release");video.stop();flameAnalyzer.release();overlay.clearFlame();overlay.clearGas();gasDirection.reset();
        String session=recordingSessionId,start=recordingStartedTs;if(session!=null&&claimSession(session)){MjpegRecorder.StopResult r=recorder.stopNow();persistRecording(session,start,EvidenceStore.isoNow(),"release",r);}recorder.release();video.release();
    }

    public void refresh(){
        String fm=host.flashMode();flash.setText("الفلاش: "+("on".equals(fm)?"تشغيل":"auto".equals(fm)?"تلقائي":"إيقاف"));
        if(recordingSessionId!=null&&!recordingStopping&&!recorder.isRecording()&&recorder.lastError()!=null){requestStopRecording("io_error",false);return;}
        if(recorder.isRecording())recordInfo.setText("جارٍ التسجيل • MP4/H.264 • "+recorder.frameCount()+" إطار • مسقط "+recorder.droppedFrames()+"\nجلسة "+recordingSessionId);
        JSONObject d=host.latestDiagnostics();if(d==null){status.setText("الروبوت غير متصل");status.setTextColor(0xFFFFB74D);drive.setText("يسار "+host.driveLeft()+"   يمين "+host.driveRight()+"   أمان الاتصال —");overlay.clearGas();gasDirection.reset();gasTrackInfo.setText(selectedGas<0?"لا يوجد هدف محدد للتتبع":"الهدف: "+selectedGasName+" • الاتصال بالروبوت مفقود");return;}
        status.setText("متصل • "+d.optInt("clients",0));status.setTextColor(0xFF81C784);JSONObject x=d.optJSONObject("drive");if(x!=null)drive.setText("يسار "+x.optInt("left",host.driveLeft())+"   يمين "+x.optInt("right",host.driveRight())+"   أمان الاتصال "+(x.optBoolean("failsafe",false)?"مفعّل":"سليم"));
        refreshGasTargets(d);
        if(selectedGas<0){overlay.clearGas();gasDirection.reset();gasTrackInfo.setText("اختر غازًا/مركبًا من القائمة لبدء حساب اتجاه مصدره");return;}
        GasDirectionEstimator.Result g=gasDirection.update(d,host.driveLeft(),host.driveRight(),SystemClock.elapsedRealtime(),selectedGas);
        if(g.valid){overlay.setGasDirection(g.angleRad,g.confidence);gasTrackInfo.setText("الهدف: "+selectedGasName+" • اتجاه نسبي "+bearingName(g.angleRad)+String.format(Locale.US," (%+.0f°) • ثقة اتجاه %.0f%%",Math.toDegrees(g.angleRad),100*g.confidence));}
        else{overlay.clearGas();gasTrackInfo.setText("الهدف: "+selectedGasName+" • نجمع تغير الاستجابة أثناء الحركة لبناء اتجاه موثوق");}
    }

    private void refreshGasTargets(JSONObject d){
        Map<Integer,ValidatedGasEngine.Result> map=AnalyticsSnapshot.analyze(d);List<ValidatedGasEngine.Result> rows=new ArrayList<>();
        for(ValidatedGasEngine.Result r:map.values())if(r!=null&&r.totalContributors>0&&r.confidence>=GasEngine.SUSPECTED&&MqSensorAffinity.forChemical(r.chemical).mask!=0)rows.add(r);
        rows.sort(CandidateRanker.COMPARATOR);
        if(rows.size()>10)rows=rows.subList(0,10);
        StringBuilder sig=new StringBuilder();for(ValidatedGasEngine.Result r:rows)sig.append(r.chemical).append(',');if(sig.toString().equals(gasTargetSignature))return;gasTargetSignature=sig.toString();
        int keep=selectedGas,keepPos=0;ArrayList<String> labels=new ArrayList<>();gasTargetIds.clear();labels.add(rows.isEmpty()?"— لا توجد مركبات مكتشفة حاليًا —":"— اختر المركب المراد تتبع مصدره —");gasTargetIds.add(-1);
        for(ValidatedGasEngine.Result r:rows){int id=r.chemical;String name=chemicalName(id),level=CandidateRanker.evidenceLabel(r);String label=name+" • "+level+String.format(Locale.US," • اتفاق %.0f%%",r.agreement);labels.add(label);gasTargetIds.add(id);if(id==keep)keepPos=labels.size()-1;}
        updatingGasTarget=true;ArrayAdapter<String> a=new ArrayAdapter<>(appContext,android.R.layout.simple_spinner_item,labels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);gasTarget.setAdapter(a);gasTarget.setSelection(keepPos,false);updatingGasTarget=false;
        if(keep>=0&&keepPos==0){selectedGas=-1;selectedGasName="";gasDirection.reset();overlay.clearGas();}
    }

    private String chemicalName(int legacyId){ChemicalRepository.Chemical c=chemicals.byLegacyId(legacyId);if(c==null)return "Chemical #"+legacyId;String ar=c.arabicName==null?"":c.arabicName.trim(),en=c.name==null?"":c.name.trim();return !ar.isEmpty()?ar:!en.isEmpty()?en:"Chemical #"+legacyId;}
    private static String bearingName(double a){double d=Math.toDegrees(a);if(d>=-22.5&&d<22.5)return"أمام";if(d>=22.5&&d<67.5)return"أمام-يمين";if(d>=67.5&&d<112.5)return"يمين";if(d>=112.5&&d<157.5)return"خلف-يمين";if(d>=157.5||d<-157.5)return"خلف";if(d>=-157.5&&d<-112.5)return"خلف-يسار";if(d>=-112.5&&d<-67.5)return"يسار";return"أمام-يسار";}
}
