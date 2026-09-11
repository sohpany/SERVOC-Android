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
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurement.TelemetryRecorder;
import com.servoc.app.navigation.MotionHistory;
import com.servoc.app.report.ReferenceBankExporter;
import com.servoc.app.report.ReportExporter;
import com.servoc.app.ui.ControlTab;
import com.servoc.app.ui.DeveloperTab;
import com.servoc.app.ui.HazRobHost;
import com.servoc.app.ui.MeasurementsTab;
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

/** HazRob 0.10.1 coordinator. Direct and Remote are transports for the same operator workspace. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101,EXPORT_PDF=8102,EXPORT_REFERENCE=8103,EXPORT_RAW=8104;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(7);
    private final AtomicBoolean driveDispatching=new AtomicBoolean(false);
    private final MotionHistory motionHistory=new MotionHistory();
    private final SensorHealthEngine sensorHealth=new SensorHealthEngine();
    private final GasAlertCoordinator gasAlertCoordinator=new GasAlertCoordinator();
    private SharedPreferences prefs;private RobotApi robotApi;private CameraApi cameraApi;private ExperimentsDb db;private TelemetryRecorder telemetryRecorder;
    private volatile JSONObject latestDiag;private volatile int left=0,right=0;private volatile boolean foreground=false;private volatile String flashMode="off";
    private volatile int trackingTarget=-1;private volatile String trackingName="";
    private volatile int lastGasAlertLevel=GasAlertCoordinator.NONE;private volatile long gasAlertSentAt=0;
    private boolean developerMode=false,entered=false,commsScheduled=false;private FrameLayout page;private TextView globalStatus;private Button[] tabButtons;private int selectedTab=0;private long pendingExperimentId=-1;
    private ControlTab controlTab;private MeasurementsTab measurementsTab;private StatusTab statusTab;private SettingsTab settingsTab;private DeveloperTab developerTab;private View[] tabViews;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("servoc",MODE_PRIVATE);robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));flashMode="on".equals(prefs.getString("flash_mode","off"))?"on":"off";trackingTarget=prefs.getInt("tracking_id",-1);trackingName=prefs.getString("tracking_name","");db=new ExperimentsDb(this);telemetryRecorder=new TelemetryRecorder(db);loadSensorBaselines();
        String mode=getIntent()==null?null:getIntent().getStringExtra("connection_mode");if(mode!=null&&!mode.trim().isEmpty())prefs.edit().putString("connection_mode",mode).apply();
        workers.execute(()->{ChemicalSeedBootstrap.ensureNiosh(getApplicationContext(),db);ExposureSeedBootstrap.ensureOshaZ1(getApplicationContext(),db);ui.post(this::refreshAllTabs);});
        enter();
    }

    private void loadSensorBaselines(){for(String key:SensorHealthEngine.INSTALLED){String v=db.getMeta("health_baseline_"+key,"");try{double q=Double.parseDouble(v);if(Double.isFinite(q)&&q>0)sensorHealth.setBaseline(key,q);}catch(Exception ignored){}}}
    private void enter(){entered=true;buildTabs();setContentView(buildShell());showTab(0);scheduleComms();startLinkService();if(foreground)controlTab.startCamera();}
    private void buildTabs(){controlTab=new ControlTab(this,this);measurementsTab=new MeasurementsTab(this,this);statusTab=new StatusTab(this,this);settingsTab=new SettingsTab(this,this);developerTab=new DeveloperTab(this,this,()->{developerMode=true;refreshAllTabs();});tabViews=new View[]{controlTab.view(),measurementsTab.view(),statusTab.view(),settingsTab.view(),developerTab.view()};}

    private View buildShell(){
        LinearLayout root=UiKit.vbox(this);root.setBackgroundColor(0xFF0A0D11);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout header=UiKit.hbox(this);header.setPadding(UiKit.dp(this,8),UiKit.dp(this,4),UiKit.dp(this,8),UiKit.dp(this,2));String mode="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"))?"عن بُعد":"مباشر";TextView title=UiKit.title(this,"HazRob "+ReleaseAudit.VERSION+" • "+mode);title.setTextColor(Color.WHITE);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));globalStatus=UiKit.small(this,"غير متصل");globalStatus.setTextColor(0xFFFFB74D);globalStatus.setGravity(Gravity.END);header.addView(globalStatus,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(this));
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tabs=UiKit.hbox(this);String[] names={"القيادة","القياسات","الحالة","الإعدادات","المطور"};tabButtons=new Button[names.length];for(int i=0;i<names.length;i++){final int x=i;Button bt=UiKit.button(this,names[i]);bt.setTextSize(12);bt.setOnClickListener(v->showTab(x));tabButtons[i]=bt;tabs.addView(bt,new LinearLayout.LayoutParams(UiKit.dp(this,110),UiKit.dp(this,44)));}tabsScroll.addView(tabs);root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));TextView footer=UiKit.small(this,"HazRob "+ReleaseAudit.VERSION+" • إعداد المشروع/ "+ReleaseAudit.PROJECT_PREPARERS+".");footer.setTextSize(10);footer.setGravity(Gravity.CENTER);footer.setTextColor(0xFF607D8B);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));return root;
    }

    private void showTab(int index){
        if(!entered||index<0||index>=tabViews.length)return;
        if(selectedTab==0&&index!=0)controlTab.stopCamera();
        selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}if(index==4&&!developerTab.isUnlocked())developerTab.requestUnlock();if(index==0&&foreground)controlTab.startCamera();refreshVisibleTab();
    }

    private void queueDriveSend(){
        if(!entered||!driveDispatching.compareAndSet(false,true))return;
        workers.execute(()->{try{robotApi.drive(left,right);}catch(Exception ignored){}finally{driveDispatching.set(false);}});
    }
    private void scheduleComms(){
        if(commsScheduled)return;commsScheduled=true;
        // 20 Hz refresh is a continuity stream; joystick changes also dispatch immediately.
        workers.scheduleAtFixedRate(()->{if(entered)queueDriveSend();},0,50,TimeUnit.MILLISECONDS);
        // 4 Hz state/telemetry refresh matches the accelerated remote telemetry cadence.
        workers.scheduleAtFixedRate(()->{if(!entered)return;try{long now=SystemClock.elapsedRealtime();JSONObject snap=robotApi.snapshot(left,right);latestDiag=snap;sensorHealth.accept(snap,now);updateGasAlert(snap,now);telemetryRecorder.accept(snap,left,right,developerMode);ui.post(this::onTelemetryUpdated);}catch(Exception e){latestDiag=null;gasAlertCoordinator.reset();lastGasAlertLevel=GasAlertCoordinator.NONE;gasAlertSentAt=0;ui.post(this::onTelemetryUpdated);}},0,250,TimeUnit.MILLISECONDS);
    }
    /** Audible gas warnings are baseline-response alarms only; they do not imply calibrated ppm or IDLH. */
    private void updateGasAlert(JSONObject snap,long now){
        int warm=Math.max(0,prefs.getInt("warm",60));int level=gasAlertCoordinator.evaluate(snap,sensorHealth,warm,now);boolean changed=level!=lastGasAlertLevel;boolean refresh=level>0&&now-gasAlertSentAt>=1000;
        if(!changed&&!refresh)return;
        try{robotApi.setAlertLevel(level,3000);lastGasAlertLevel=level;gasAlertSentAt=now;}catch(Exception ignored){}
    }
    private void onTelemetryUpdated(){JSONObject d=latestDiag;if(globalStatus==null)return;if(d==null){globalStatus.setText("غير متصل");globalStatus.setTextColor(0xFFFFB74D);}else{JSONObject ir=d.optJSONObject("ir"),drv=d.optJSONObject("drive"),alert=d.optJSONObject("alert");boolean warn=ir!=null&&ir.optBoolean("warning",false),stale=drv!=null&&drv.optBoolean("stale_stop",false),gas=alert!=null&&alert.optInt("gas",0)>0;globalStatus.setText("متصل"+(stale?" • توقف لانقطاع أوامر القيادة":gas?" • ⚠ غازات":warn?" • ⚠ IR":""));globalStatus.setTextColor(stale?0xFFFFB74D:gas?0xFFFF7043:warn?0xFFFF7043:0xFF81C784);}refreshVisibleTab();}
    private void refreshVisibleTab(){if(!entered)return;if(selectedTab==0)controlTab.refresh();else if(selectedTab==1)measurementsTab.refresh();else if(selectedTab==2)statusTab.refresh();else if(selectedTab==3)settingsTab.refresh();else if(selectedTab==4)developerTab.refresh();}

    @Override protected void onResume(){super.onResume();if(!"remote".equalsIgnoreCase(prefs.getString("connection_mode","direct")))WifiRoute.bindConnectedWifi(this);foreground=true;if(entered){startLinkService();if(selectedTab==0)controlTab.startCamera();refreshVisibleTab();}}
    @Override protected void onPause(){foreground=false;if(entered)controlTab.stopCamera();super.onPause();}
    @Override protected void onDestroy(){entered=false;if(controlTab!=null)controlTab.releaseCamera();try{if(lastGasAlertLevel!=GasAlertCoordinator.NONE)robotApi.setAlertLevel(GasAlertCoordinator.NONE,500);}catch(Exception ignored){}workers.shutdownNow();WifiRoute.unbind(this);if(db!=null)db.close();super.onDestroy();}
    private void startLinkService(){try{RobotLinkService.start(getApplicationContext());}catch(Exception ignored){}}

    @Override public RobotApi robotApi(){return robotApi;}@Override public SharedPreferences preferences(){return prefs;}@Override public ExperimentsDb database(){return db;}@Override public ExecutorService io(){return workers;}@Override public String operatorRole(){return developerMode?"DEVELOPER":"USER";}@Override public JSONObject latestDiagnostics(){return latestDiag;}@Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}@Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}
    @Override public void emergencyStop(){left=right=0;motionHistory.record(0,0,SystemClock.elapsedRealtime());if(controlTab!=null)controlTab.resetJoystick();workers.execute(()->{try{robotApi.emergencyStop();}catch(Exception ignored){}});}
    @Override public void setDriveCommand(int l,int r){int nl=Math.max(-100,Math.min(100,l)),nr=Math.max(-100,Math.min(100,r));left=nl;right=nr;motionHistory.record(nl,nr,SystemClock.elapsedRealtime());queueDriveSend();}
    @Override public int driveLeft(){return left;}@Override public int driveRight(){return right;}
    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(entered&&selectedTab==0&&foreground)controlTab.startCamera();}@Override public String cameraHost(){return cameraApi.host();}@Override public boolean cameraReachable(){return cameraApi.ping();}@Override public JSONObject cameraDiagnostics(){return cameraApi.status();}
    @Override public String flashMode(){return flashMode;}
    @Override public void setFlashMode(String mode){final String requested="on".equals(CameraApi.normalizeMode(mode))?"on":"off";workers.execute(()->{boolean ok=cameraApi.flash(requested);if(ok){flashMode=requested;prefs.edit().putString("flash_mode",requested).apply();if(controlTab!=null)ui.post(controlTab::refresh);showToast("الفلاش: "+flashAr(requested));}else showToast("تعذر إرسال أمر الفلاش");});}

    @Override public SensorHealthEngine.Snapshot sensorHealth(String key){return sensorHealth.snapshot(key);}
    @Override public int captureCleanAirBaseline(){if(!developerMode)return 0;JSONObject d=latestDiag;int n=sensorHealth.captureCleanAirBaseline(d);if(n>0){for(String key:SensorHealthEngine.INSTALLED){double q=sensorHealth.baseline(key);if(Double.isFinite(q)&&q>0)db.putMeta("health_baseline_"+key,Double.toString(q));}db.putMeta("health_baseline_ts",isoNow());}return n;}
    @Override public int evaluateCleanAirDrift(){if(!developerMode)return 0;return sensorHealth.evaluateCleanAirDrift(latestDiag);}
    @Override public String cleanAirBaselineTimestamp(){return db==null?"":db.getMeta("health_baseline_ts","");}
    @Override public String lastMeasurementSessionId(){return telemetryRecorder==null?"":telemetryRecorder.lastCompletedSessionId();}

    @Override public void setTrackingTarget(int chemicalId,String name){trackingTarget=chemicalId;trackingName=chemicalId<0?"":(name==null?"":name);prefs.edit().putInt("tracking_id",trackingTarget).putString("tracking_name",trackingName).apply();}
    @Override public int trackingTargetId(){return trackingTarget;}@Override public String trackingTargetName(){return trackingName;}

    @Override public void requestExport(String format,long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("السجل غير موجود");return;}pendingExperimentId=experimentId;boolean pdf="pdf".equalsIgnoreCase(format);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Report_%06d_%s%s",r.id,fileStamp(),pdf?".pdf":".xlsx"));startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);}
    @Override public void requestRawExport(long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("السجل غير موجود");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Raw_%06d_%s.json",r.id,fileStamp()));startActivityForResult(i,EXPORT_RAW);}
    @Override public void requestReferenceBankExport(long experimentId){if(!developerMode){showToast("هذه الوظيفة متاحة للمطور فقط");return;}ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,r.referenceName==null||r.referenceName.isEmpty()?ReferenceBankExporter.fileName(r.id,r.ts):r.referenceName);startActivityForResult(i,EXPORT_REFERENCE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX&&requestCode!=EXPORT_REFERENCE&&requestCode!=EXPORT_RAW)return;final Uri uri=data.getData();final int req=requestCode;final long id=pendingExperimentId;workers.execute(()->{ExperimentQueries.Record r=ExperimentQueries.byId(db,id);if(r==null){showToast("تعذر العثور على السجل");return;}try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IllegalStateException("output stream");ChemicalRepository chemicals=new ChemicalRepository(db);if(req==EXPORT_PDF)ReportExporter.writePdf(out,r,chemicals);else if(req==EXPORT_XLSX)ReportExporter.writeXlsx(out,r,chemicals);else if(req==EXPORT_REFERENCE)ReferenceBankExporter.writeJson(out,r);else writeRaw(out,r);showToast(req==EXPORT_PDF?"تم تصدير PDF":req==EXPORT_XLSX?"تم تصدير Excel":req==EXPORT_RAW?"تم تصدير Raw JSON":"تم تصدير سجل البنك المرجعي");}catch(Exception e){showToast("فشل التصدير: "+e.getClass().getSimpleName());}});}
    private static void writeRaw(OutputStream out,ExperimentQueries.Record r)throws Exception{JSONObject j=new JSONObject();j.put("id",r.id);j.put("timestamp",r.ts);j.put("protocol",r.protocol);j.put("session_id",r.sessionId);j.put("capture_id",r.captureId);j.put("chemical_uid",r.chemicalUid);j.put("reference_kind",r.referenceKind);if(r.knownPpm!=null)j.put("known_ppm",r.knownPpm);try{j.put("payload",new JSONObject(r.payload));}catch(Exception e){j.put("payload_text",r.payload);}try{j.put("analytics",new JSONObject(r.analytics));}catch(Exception e){j.put("analytics_text",r.analytics);}out.write(j.toString(2).getBytes(StandardCharsets.UTF_8));}
    @Override public void refreshAllTabs(){if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}refreshVisibleTab();}

    private static String flashAr(String m){return"on".equals(m)?"تشغيل":"إيقاف";}
    private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
