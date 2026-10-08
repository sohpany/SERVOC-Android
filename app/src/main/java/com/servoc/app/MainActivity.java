package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.CameraApi;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.communication.RobotLinkService;
import com.servoc.app.communication.WifiRoute;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import com.servoc.app.measurement.GasAlertCoordinator;
import com.servoc.app.field.HazRobUserLibrary;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.field.HazRobStorageBootstrap;
import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.measurement.PhoneAlertTone;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurement.TelemetryRecorder;
import com.servoc.app.navigation.MotionHistory;
import com.servoc.app.report.ReferenceBankExporter;
import com.servoc.app.report.ReportExporter;
import com.servoc.app.report.FieldSessionReportExporter;
import com.servoc.app.ui.ControlTab;
import com.servoc.app.ui.DeveloperTab;
import com.servoc.app.ui.HazRobHost;
import com.servoc.app.ui.MeasurementsTab;
import com.servoc.app.ui.FieldSessionsTab;
import com.servoc.app.ui.ReportsTab;
import com.servoc.app.ui.SettingsTab;
import com.servoc.app.ui.StatusTab;
import com.servoc.app.ui.UiKit;

import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** HazRob 0.10.2 coordinator. Direct and Remote are transports for the same operator workspace. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101,EXPORT_PDF=8102,EXPORT_REFERENCE=8103,EXPORT_RAW=8104,USER_LIBRARY_TREE=8105,EXPORT_FIELD_PDF=8106,EXPORT_FIELD_XLSX=8107;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(7);
    private final AtomicBoolean driveDispatching=new AtomicBoolean(false),driveDirty=new AtomicBoolean(false),telemetryPolling=new AtomicBoolean(false);
    private final MotionHistory motionHistory=new MotionHistory();
    private final SensorHealthEngine sensorHealth=new SensorHealthEngine();
    private final GasAlertCoordinator gasAlertCoordinator=new GasAlertCoordinator();
    private SharedPreferences prefs;private HazRobUserLibrary userLibrary;private RobotApi robotApi;private CameraApi cameraApi;private ExperimentsDb db;private TelemetryRecorder telemetryRecorder;private PhoneAlertTone phoneAlertTone;
    private volatile JSONObject latestDiag;private volatile int left=0,right=0;private volatile boolean foreground=false;private volatile String flashMode="off";
    private volatile int trackingTarget=-1;private volatile String trackingName="";
    private volatile int lastGasAlertLevel=GasAlertCoordinator.NONE;private volatile long gasAlertSentAt=0;
    private boolean developerMode=false,entered=false,commsScheduled=false;private FrameLayout page;private TextView globalStatus;private Button[] tabButtons;private int selectedTab=0;private long pendingExperimentId=-1;private String pendingFieldSessionId="";
    private ControlTab controlTab;private FieldSessionsTab fieldSessionsTab;private MeasurementsTab measurementsTab;private ReportsTab reportsTab;private StatusTab statusTab;private SettingsTab settingsTab;private DeveloperTab developerTab;private View[] tabViews;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);HazRobStorageBootstrap.ensure(this);prefs=getSharedPreferences("servoc",MODE_PRIVATE);userLibrary=new HazRobUserLibrary(this);robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));flashMode="on".equals(prefs.getString("flash_mode","off"))?"on":"off";trackingTarget=prefs.getInt("tracking_id",-1);trackingName=prefs.getString("tracking_name","");db=new ExperimentsDb(this);telemetryRecorder=new TelemetryRecorder(db);try{phoneAlertTone=new PhoneAlertTone();}catch(Exception ignored){phoneAlertTone=null;}loadSensorBaselines();
        String mode=getIntent()==null?null:getIntent().getStringExtra("connection_mode");if(mode!=null&&!mode.trim().isEmpty())prefs.edit().putString("connection_mode",mode).apply();
        workers.execute(()->{ChemicalSeedBootstrap.ensureNiosh(getApplicationContext(),db);ExposureSeedBootstrap.ensureOshaZ1(getApplicationContext(),db);ui.post(this::refreshAllTabs);});
        enter();
    }

    private void loadSensorBaselines(){for(String key:SensorHealthEngine.INSTALLED){String v=db.getMeta("health_baseline_"+key,"");try{double q=Double.parseDouble(v);if(Double.isFinite(q)&&q>0)sensorHealth.setBaseline(key,q);}catch(Exception ignored){}}}
    private void enter(){entered=true;buildTabs();setContentView(buildShell());showTab(0);scheduleComms();startLinkService();if(foreground)controlTab.startCamera();}
    private void buildTabs(){controlTab=new ControlTab(this,this);fieldSessionsTab=new FieldSessionsTab(this,this);measurementsTab=new MeasurementsTab(this,this);reportsTab=new ReportsTab(this,this);statusTab=new StatusTab(this,this);settingsTab=new SettingsTab(this,this);developerTab=new DeveloperTab(this,this,()->{developerMode=true;refreshAllTabs();});tabViews=new View[]{withRefresh(controlTab.view()),withRefresh(fieldSessionsTab.view()),withRefresh(measurementsTab.view()),withRefresh(reportsTab.view()),withRefresh(statusTab.view()),withRefresh(settingsTab.view()),withRefresh(developerTab.view())};}
    private View withRefresh(View content){LinearLayout box=UiKit.vbox(this);LinearLayout bar=UiKit.hbox(this);Button refresh=UiKit.button(this,"↻ تحديث");refresh.setTextSize(12);refresh.setOnClickListener(v->requestManualRefresh());bar.addView(refresh,new LinearLayout.LayoutParams(UiKit.dp(this,110),UiKit.dp(this,40)));TextView hint=UiKit.small(this,"تحديث البيانات الحية");hint.setTextColor(0xFF90A4AE);hint.setGravity(Gravity.CENTER_VERTICAL);bar.addView(hint,new LinearLayout.LayoutParams(0,-2,1f));box.addView(bar,UiKit.full(this));box.addView(content,new LinearLayout.LayoutParams(-1,0,1f));return box;}

    private View buildShell(){
        LinearLayout root=UiKit.vbox(this);root.setBackgroundColor(0xFF0A0D11);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout header=UiKit.hbox(this);header.setPadding(UiKit.dp(this,8),UiKit.dp(this,4),UiKit.dp(this,8),UiKit.dp(this,2));String mode="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"))?"عن بُعد":"مباشر";TextView title=UiKit.title(this,"HazRob "+ReleaseAudit.VERSION+" • "+mode);title.setTextColor(Color.WHITE);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));globalStatus=UiKit.small(this,"غير متصل");globalStatus.setTextColor(0xFFFFB74D);globalStatus.setGravity(Gravity.END);header.addView(globalStatus,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(this));
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tabs=UiKit.hbox(this);String[] names={"القيادة","الجلسات","القياسات","التقارير","الحالة","الإعدادات","المطور"};tabButtons=new Button[names.length];for(int i=0;i<names.length;i++){final int x=i;Button bt=UiKit.button(this,names[i]);bt.setTextSize(12);bt.setOnClickListener(v->showTab(x));tabButtons[i]=bt;tabs.addView(bt,new LinearLayout.LayoutParams(UiKit.dp(this,110),UiKit.dp(this,44)));}tabsScroll.addView(tabs);root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));TextView footer=UiKit.small(this,"HazRob "+ReleaseAudit.VERSION+" • إعداد المشروع/ "+ReleaseAudit.PROJECT_PREPARERS+".");footer.setTextSize(10);footer.setGravity(Gravity.CENTER);footer.setTextColor(0xFF607D8B);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));return root;
    }

    private void showTab(int index){
        if(!entered||index<0||index>=tabViews.length)return;
        if(selectedTab==0&&index!=0)controlTab.stopCamera();
        selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}if(index==6&&!developerTab.isUnlocked())developerTab.requestUnlock();if(index==0&&foreground)controlTab.startCamera();if(index==1)fieldSessionsTab.refresh();else if(index==3)reportsTab.refresh();else refreshVisibleTab();
    }

    private void queueDriveSend(){
        if(!entered)return;
        driveDirty.set(true);
        if(!driveDispatching.compareAndSet(false,true))return;
        workers.execute(()->{
            try{
                do{
                    driveDirty.set(false);
                    int sendLeft=left,sendRight=right;
                    try{robotApi.drive(sendLeft,sendRight);}catch(Exception ignored){}
                }while(entered&&driveDirty.get());
            }finally{
                driveDispatching.set(false);
                if(entered&&driveDirty.get())queueDriveSend();
            }
        });
    }
    private void scheduleComms(){
        if(commsScheduled)return;commsScheduled=true;
        workers.scheduleAtFixedRate(()->{if(entered)queueDriveSend();},0,50,TimeUnit.MILLISECONDS);
        workers.scheduleAtFixedRate(()->pollTelemetry(false),0,150,TimeUnit.MILLISECONDS);
    }
    private void requestManualRefresh(){workers.execute(()->pollTelemetry(true));}
    private void pollTelemetry(boolean manual){
        if(!entered)return;if(!telemetryPolling.compareAndSet(false,true)){if(manual)showToast("التحديث جارٍ");return;}
        try{long now=SystemClock.elapsedRealtime();JSONObject snap=robotApi.snapshot(left,right);latestDiag=snap;sensorHealth.accept(snap,now);updateGasAlert(snap,now);telemetryRecorder.accept(snap,left,right,developerMode&&selectedTab==6);ui.post(this::onTelemetryUpdated);if(manual)showToast("تم تحديث البيانات الحية");}
        catch(Exception e){latestDiag=null;gasAlertCoordinator.reset();lastGasAlertLevel=GasAlertCoordinator.NONE;gasAlertSentAt=0;if(phoneAlertTone!=null)phoneAlertTone.clear();ui.post(this::onTelemetryUpdated);if(manual)showToast("تعذر التحديث: "+e.getClass().getSimpleName());}
        finally{telemetryPolling.set(false);}
    }
    private void updateGasAlert(JSONObject snap,long now){
        int warm=Math.max(0,prefs.getInt("warm",60));int level=gasAlertCoordinator.evaluate(snap,sensorHealth,warm,now);
        JSONObject ir=snap==null?null:snap.optJSONObject("ir"),device=snap==null?null:snap.optJSONObject("device"),proximity=device==null?null:device.optJSONObject("proximity");boolean hazard=(ir!=null&&ir.optBoolean("cliff_warning",ir.optBoolean("warning",false)))||(proximity!=null&&proximity.optBoolean("warning",false));int phoneLevel=Math.max(level,hazard?GasAlertCoordinator.WARNING:GasAlertCoordinator.NONE);if(phoneAlertTone!=null)phoneAlertTone.update(phoneLevel,now);
        boolean changed=level!=lastGasAlertLevel,refresh=level>0&&now-gasAlertSentAt>=1000;if(!changed&&!refresh)return;lastGasAlertLevel=level;gasAlertSentAt=now;try{robotApi.setAlertLevel(level,3000);}catch(Exception ignored){}
    }
    private void onTelemetryUpdated(){JSONObject d=latestDiag;if(globalStatus==null)return;if(d==null){globalStatus.setText("غير متصل");globalStatus.setTextColor(0xFFFFB74D);}else{JSONObject ir=d.optJSONObject("ir"),drv=d.optJSONObject("drive"),device=d.optJSONObject("device"),proximity=device==null?null:device.optJSONObject("proximity");boolean cliff=ir!=null&&ir.optBoolean("cliff_warning",ir.optBoolean("warning",false)),near=proximity!=null&&proximity.optBoolean("warning",false),stale=drv!=null&&drv.optBoolean("stale_stop",false),gas=lastGasAlertLevel>0;globalStatus.setText("متصل"+(stale?" • توقف لانقطاع أوامر القيادة":gas?" • ⚠ غازات":cliff?" • ⚠ خطر سقوط":near?" • ⚠ جسم قريب":""));globalStatus.setTextColor(stale?0xFFFFB74D:gas?0xFFFF7043:cliff||near?0xFFFF7043:0xFF81C784);}refreshVisibleTab();}
    private void refreshVisibleTab(){if(!entered)return;if(selectedTab==0)controlTab.refresh();else if(selectedTab==2)measurementsTab.refresh();else if(selectedTab==4)statusTab.refresh();else if(selectedTab==5)settingsTab.refresh();else if(selectedTab==6)developerTab.refresh();}

    @Override protected void onResume(){super.onResume();if(!"remote".equalsIgnoreCase(prefs.getString("connection_mode","direct")))WifiRoute.bindConnectedWifi(this);foreground=true;if(entered){startLinkService();if(selectedTab==0)controlTab.startCamera();refreshVisibleTab();}}
    @Override protected void onPause(){foreground=false;if(entered)controlTab.stopCamera();super.onPause();}
    @Override protected void onDestroy(){entered=false;if(controlTab!=null)controlTab.releaseCamera();try{if(lastGasAlertLevel!=GasAlertCoordinator.NONE)robotApi.setAlertLevel(GasAlertCoordinator.NONE,500);}catch(Exception ignored){}if(phoneAlertTone!=null)try{phoneAlertTone.close();}catch(Exception ignored){}workers.shutdownNow();WifiRoute.unbind(this);if(db!=null)db.close();super.onDestroy();}
    private void startLinkService(){try{RobotLinkService.start(getApplicationContext());}catch(Exception ignored){}}

    @Override public RobotApi robotApi(){return robotApi;}@Override public SharedPreferences preferences(){return prefs;}@Override public ExperimentsDb database(){return db;}@Override public ExecutorService io(){return workers;}@Override public String operatorRole(){return developerMode&&selectedTab==6?"DEVELOPER":"USER";}@Override public JSONObject latestDiagnostics(){return latestDiag;}@Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}@Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}
    @Override public void emergencyStop(){left=right=0;motionHistory.record(0,0,SystemClock.elapsedRealtime());if(controlTab!=null)controlTab.resetJoystick();workers.execute(()->{try{robotApi.emergencyStop();}catch(Exception ignored){}});}
    @Override public void setDriveCommand(int l,int r){int nl=Math.max(-100,Math.min(100,l)),nr=Math.max(-100,Math.min(100,r));left=nl;right=nr;motionHistory.record(nl,nr,SystemClock.elapsedRealtime());queueDriveSend();}
    @Override public int driveLeft(){return left;}@Override public int driveRight(){return right;}
    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(entered&&selectedTab==0&&foreground)controlTab.startCamera();}@Override public String cameraHost(){return cameraApi.host();}@Override public boolean cameraReachable(){return cameraApi.ping();}@Override public JSONObject cameraDiagnostics(){return cameraApi.status();}
    @Override public String flashMode(){return flashMode;}
    @Override public void setFlashMode(String mode){final String requested="on".equals(CameraApi.normalizeMode(mode))?"on":"off";workers.execute(()->{boolean ok=cameraApi.flash(requested);if(ok){flashMode=requested;prefs.edit().putString("flash_mode",requested).apply();if(controlTab!=null)ui.post(controlTab::refresh);showToast("الفلاش: "+flashAr(requested));}else showToast("تعذر إرسال أمر الفلاش");});}
    @Override public void testPhoneAlert(){if(phoneAlertTone==null){showToast("تنبيه الهاتف غير متاح على هذا الجهاز");return;}try{phoneAlertTone.test();showToast("اختبار صوت الهاتف — STREAM_ALARM");}catch(Exception e){showToast("تعذر تشغيل اختبار صوت الهاتف");}}

    @Override public SensorHealthEngine.Snapshot sensorHealth(String key){return sensorHealth.snapshot(key);}
    @Override public LiveGasStateEngine.Snapshot liveGasState(){return gasAlertCoordinator.liveSnapshot();}
    @Override public int captureCleanAirBaseline(){if(!developerMode)return 0;JSONObject d=latestDiag;int n=sensorHealth.captureCleanAirBaseline(d);if(n>0){for(String key:SensorHealthEngine.INSTALLED){double q=sensorHealth.baseline(key);if(Double.isFinite(q)&&q>0)db.putMeta("health_baseline_"+key,Double.toString(q));}db.putMeta("health_baseline_ts",isoNow());}return n;}
    @Override public int evaluateCleanAirDrift(){if(!developerMode)return 0;return sensorHealth.evaluateCleanAirDrift(latestDiag);}
    @Override public String cleanAirBaselineTimestamp(){return db==null?"":db.getMeta("health_baseline_ts","");}
    @Override public String lastMeasurementSessionId(){return telemetryRecorder==null?"":telemetryRecorder.lastCompletedSessionId();}

    @Override public void setTrackingTarget(int chemicalId,String name){trackingTarget=chemicalId;trackingName=chemicalId<0?"":(name==null?"":name);prefs.edit().putInt("tracking_id",trackingTarget).putString("tracking_name",trackingName).apply();}
    @Override public int trackingTargetId(){return trackingTarget;}@Override public String trackingTargetName(){return trackingName;}

    @Override public void requestExport(String format,long experimentId){
        ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);
        if(r==null){showToast("السجل غير موجود");return;}
        // Operator PDF/XLSX must NEVER silently fall back to the legacy one-chart exporter.
        // A persisted user measurement belongs to exactly one Field Session when reportable.
        String fs="";
        try{if(r.sessionId!=null&&!r.sessionId.isEmpty())
            fs=new FieldSessionRepository(db).fieldSessionIdForMeasurement(r.sessionId);}
        catch(Exception e){showToast("تعذر العثور على جلسة القياس");return;}
        if(fs!=null&&!fs.isEmpty()){requestFieldSessionExport(format,fs);return;}
        // Developer laboratory experiments have their own scientific export identity;
        // they are not field-session reports and cannot be relabeled as site/session data.
        if(!developerMode||!ExperimentQueries.isDeveloperMeasurementSession(db,r.sessionId)){
            showToast("التقرير الذكي يتطلب جلسة مستخدم ميدانية؛ اختر الموقع والجلسة من تبويب التقارير");
            return;
        }
        pendingExperimentId=experimentId;
        boolean pdf="pdf".equalsIgnoreCase(format);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Developer_Experiment_%06d_%s%s",r.id,fileStamp(),pdf?".pdf":".xlsx"));
        startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);
    }
    @Override public void requestFieldSessionExport(String format,String fieldSessionId){
        String fs=fieldSessionId==null?"":fieldSessionId.trim();
        if(fs.isEmpty()){showToast("الجلسة غير صالحة");return;}
        // Only a lightweight indexed ID lookup is allowed on the Android UI thread.
        // FieldSessionReportExporter.resolve + PDF/XLSX construction execute later on workers.
        FieldSessionRepository.FieldSession selected;
        try{selected=new FieldSessionRepository(db).fieldSessionById(fs);}
        catch(Exception e){showToast("تعذر قراءة الجلسة");return;}
        if(selected==null){showToast("هذه الجلسة غير موجودة");return;}
        pendingFieldSessionId=fs;
        boolean pdf="pdf".equalsIgnoreCase(format);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String safeName=selected.name.replaceAll("[^A-Za-z0-9._-]","_");
        i.putExtra(Intent.EXTRA_TITLE,"HazRob_FieldSession_"+safeName+"_"+fs.replaceAll("[^A-Za-z0-9._-]","_")+"_"+fileStamp()+(pdf?".pdf":".xlsx"));
        startActivityForResult(i,pdf?EXPORT_FIELD_PDF:EXPORT_FIELD_XLSX);
    }

    @Override public void openReportsForSite(String siteId,String fieldSessionId){
        ui.post(()->{if(reportsTab!=null){reportsTab.selectScope(siteId,fieldSessionId);showTab(3);}});
    }
    @Override public void deleteUserFieldSession(String fieldSessionId){
        workers.execute(()->{
            try{
                boolean ok=new FieldSessionRepository(db).deleteFieldSession(fieldSessionId);
                if(!ok){showToast("لا يمكن حذف جلسة نشطة أو غير موجودة");return;}
                boolean synced=true;
                if(userLibrary!=null&&userLibrary.attached()){
                    try{userLibrary.deleteStoredSessionArchive(fieldSessionId);userLibrary.syncUserIndex(db);}
                    catch(Exception e){synced=false;}
                }
                showToast(synced?"تم حذف الجلسة وقياساتها":"حذفت الجلسة محليًا؛ فشلت مزامنة المكتبة، يرجى إعادة المزامنة");
                refreshAllTabs();
            }catch(Exception e){showToast("رفض الحذف لحماية القياسات المرجعية أو تعذر الحذف: "+e.getClass().getSimpleName());}
        });
    }
    @Override public void deleteEmptyUserSite(String siteId){
        workers.execute(()->{
            try{
                boolean ok=new FieldSessionRepository(db).deleteEmptySite(siteId);
                if(!ok){showToast("لا يمكن حذف موقع يحوي جلسات");return;}
                boolean synced=true;
                if(userLibrary!=null&&userLibrary.attached())try{userLibrary.syncUserIndex(db);}catch(Exception e){synced=false;}
                showToast(synced?"تم حذف الموقع الفارغ":"حذف الموقع محليًا؛ يرجى مزامنة المكتبة");
                refreshAllTabs();
            }catch(Exception e){showToast("تعذر حذف الموقع: "+e.getClass().getSimpleName());}
        });
    }

    @Override public void requestRawExport(long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("السجل غير موجود");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Raw_%06d_%s.json",r.id,fileStamp()));startActivityForResult(i,EXPORT_RAW);}
    @Override public void requestReferenceBankExport(long experimentId){if(!developerMode){showToast("هذه الوظيفة متاحة للمطور فقط");return;}ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,r.referenceName==null||r.referenceName.isEmpty()?ReferenceBankExporter.fileName(r.id,r.ts):r.referenceName);startActivityForResult(i,EXPORT_REFERENCE);}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        if(requestCode==USER_LIBRARY_TREE){
            try{userLibrary.attach(data.getData());userLibrary.syncUserIndex(db);showToast("تم ربط ومزامنة مكتبة HazRob الدائمة");refreshAllTabs();}catch(Exception e){showToast("تعذر فتح مكتبة HazRob: "+e.getClass().getSimpleName());}
            return;
        }
        if(requestCode==EXPORT_FIELD_PDF||requestCode==EXPORT_FIELD_XLSX){
            final Uri uri=data.getData();final int req=requestCode;final String fieldId=pendingFieldSessionId;
            workers.execute(()->{
                if(fieldId==null||fieldId.trim().isEmpty()){showToast("تعذر تحديد الجلسة");return;}
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                    if(out==null)throw new IllegalStateException("output stream");
                    ChemicalRepository chemicals=new ChemicalRepository(db);
                    if(req==EXPORT_FIELD_PDF)FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals);else FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals);
                    showToast(req==EXPORT_FIELD_PDF?"تم تصدير تقرير الجلسة PDF":"تم تصدير تقرير الجلسة Excel");
                }catch(Exception e){showToast("فشل تصدير الجلسة: "+e.getClass().getSimpleName());}
            });
            return;
        }
        if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX&&requestCode!=EXPORT_REFERENCE&&requestCode!=EXPORT_RAW)return;
        final Uri uri=data.getData();final int req=requestCode;final long id=pendingExperimentId;
        workers.execute(()->{ExperimentQueries.Record r=ExperimentQueries.byId(db,id);if(r==null){showToast("تعذر العثور على السجل");return;}try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IllegalStateException("output stream");ChemicalRepository chemicals=new ChemicalRepository(db);if(req==EXPORT_PDF)ReportExporter.writePdf(out,r,chemicals);else if(req==EXPORT_XLSX)ReportExporter.writeXlsx(out,r,chemicals);else if(req==EXPORT_REFERENCE)ReferenceBankExporter.writeJson(out,r);else writeRaw(out,r);showToast(req==EXPORT_PDF?"تم تصدير PDF":req==EXPORT_XLSX?"تم تصدير Excel":req==EXPORT_RAW?"تم تصدير Raw JSON":"تم تصدير سجل البنك المرجعي");}catch(Exception e){showToast("فشل التصدير: "+e.getClass().getSimpleName());}});
    }

    private static void writeRaw(OutputStream out,ExperimentQueries.Record r)throws Exception{JSONObject j=new JSONObject();j.put("id",r.id);j.put("timestamp",r.ts);j.put("protocol",r.protocol);j.put("session_id",r.sessionId);j.put("capture_id",r.captureId);j.put("chemical_uid",r.chemicalUid);j.put("reference_kind",r.referenceKind);if(r.knownPpm!=null)j.put("known_ppm",r.knownPpm);try{j.put("payload",new JSONObject(r.payload));}catch(Exception e){j.put("payload_text",r.payload);}try{j.put("analytics",new JSONObject(r.analytics));}catch(Exception e){j.put("analytics_text",r.analytics);}out.write(j.toString(2).getBytes(StandardCharsets.UTF_8));}
    @Override public boolean userLibraryAttached(){return userLibrary!=null&&userLibrary.attached();}
    @Override public void requestUserLibraryFolder(){startActivityForResult(HazRobUserLibrary.createPickerIntent(),USER_LIBRARY_TREE);}
    @Override public void syncUserLibrary(){if(userLibrary==null||!userLibrary.attached()){showToast("اختر مجلد مكتبة HazRob أولًا");return;}workers.execute(()->{try{userLibrary.syncUserIndex(db);showToast("تمت مزامنة مكتبة المواقع والجلسات");}catch(Exception e){showToast("فشلت مزامنة مكتبة HazRob");}});}
    @Override public void restoreUserLibrary(){if(userLibrary==null||!userLibrary.attached()){showToast("اختر مجلد مكتبة HazRob أولًا");return;}workers.execute(()->{try{HazRobUserLibrary.RestoreSummary r=userLibrary.restoreUserIndex(db);showToast("استعادة المكتبة: "+r.sites+" موقع • "+r.sessions+" جلسة • "+r.measurements+" قياس • "+r.samples+" عينة"+(r.skippedLinks>0?" • "+r.skippedLinks+" رابط غير مستعاد":""));refreshAllTabs();}catch(Exception e){showToast("فشلت استعادة مكتبة HazRob");}});}
    @Override public void refreshAllTabs(){if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}if(fieldSessionsTab!=null)fieldSessionsTab.refresh();if(reportsTab!=null)reportsTab.refresh();refreshVisibleTab();}

    private static String flashAr(String m){return"on".equals(m)?"تشغيل":"إيقاف";}
    private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
