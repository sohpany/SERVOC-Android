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

import com.servoc.app.auth.RoleGate;
import com.servoc.app.camera.CameraApi;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.communication.WifiRoute;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurement.TelemetryRecorder;
import com.servoc.app.navigation.MotionHistory;
import com.servoc.app.report.ReferenceBankExporter;
import com.servoc.app.report.ReportExporter;
import com.servoc.app.ui.ControlTab;
import com.servoc.app.ui.ExperimentsTab;
import com.servoc.app.ui.HazRobHost;
import com.servoc.app.ui.MeasurementAnalysisTab;
import com.servoc.app.ui.ReportsTab;
import com.servoc.app.ui.SettingsTab;
import com.servoc.app.ui.UiKit;

import org.json.JSONObject;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** HazRob 0.7.1 coordinator. ESP32 owns local motion safety; Android owns UI, analytics and independent video. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101,EXPORT_PDF=8102,EXPORT_REFERENCE=8103;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(6);
    private final MotionHistory motionHistory=new MotionHistory();
    private final SensorHealthEngine sensorHealth=new SensorHealthEngine();
    private SharedPreferences prefs;private RobotApi robotApi;private CameraApi cameraApi;private ExperimentsDb db;private TelemetryRecorder telemetryRecorder;
    private volatile JSONObject latestDiag;private volatile int left=0,right=0;private volatile boolean foreground=false;private volatile String flashMode="off";
    private boolean developerMode=false,entered=false,commsScheduled=false;private FrameLayout page;private TextView globalStatus;private Button[] tabButtons;private int selectedTab=0;private long pendingExperimentId=-1;
    private ControlTab controlTab;private MeasurementAnalysisTab measurementTab;private ReportsTab reportsTab;private ExperimentsTab experimentsTab;private SettingsTab settingsTab;private View[] tabViews;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("servoc",MODE_PRIVATE);robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));flashMode="on".equals(prefs.getString("flash_mode","off"))?"on":"off";db=new ExperimentsDb(this);telemetryRecorder=new TelemetryRecorder(db);loadSensorBaselines();
        workers.execute(()->{ChemicalSeedBootstrap.ensureNiosh(getApplicationContext(),db);ExposureSeedBootstrap.ensureOshaZ1(getApplicationContext(),db);ui.post(this::refreshAllTabs);});
        setContentView(RoleGate.build(this,new RoleGate.Listener(){@Override public void onUser(){enter(false);}@Override public void onDeveloper(){enter(true);}}));
    }

    private void loadSensorBaselines(){for(String key:SensorHealthEngine.INSTALLED){String v=db.getMeta("health_baseline_"+key,"");try{double q=Double.parseDouble(v);if(Double.isFinite(q)&&q>0)sensorHealth.setBaseline(key,q);}catch(Exception ignored){}}}
    private void enter(boolean developer){developerMode=developer;entered=true;buildTabs();setContentView(buildShell());showTab(0);scheduleComms();if(foreground)controlTab.startCamera();}
    private void buildTabs(){controlTab=new ControlTab(this,this);measurementTab=new MeasurementAnalysisTab(this,this);reportsTab=new ReportsTab(this,this);settingsTab=new SettingsTab(this,this);if(developerMode){experimentsTab=new ExperimentsTab(this,this);tabViews=new View[]{controlTab.view(),measurementTab.view(),reportsTab.view(),experimentsTab.view(),settingsTab.view()};}else{experimentsTab=null;tabViews=new View[]{controlTab.view(),measurementTab.view(),reportsTab.view(),settingsTab.view()};}}

    private View buildShell(){
        LinearLayout root=UiKit.vbox(this);root.setBackgroundColor(0xFF0A0D11);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout header=UiKit.hbox(this);header.setPadding(UiKit.dp(this,8),UiKit.dp(this,4),UiKit.dp(this,8),UiKit.dp(this,2));TextView title=UiKit.title(this,"HazRob "+ReleaseAudit.VERSION+" • "+(developerMode?"وضع المطور":"المستخدم"));title.setTextColor(Color.WHITE);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));globalStatus=UiKit.small(this,"غير متصل");globalStatus.setTextColor(0xFFFFB74D);globalStatus.setGravity(Gravity.END);header.addView(globalStatus,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(this));
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tabs=UiKit.hbox(this);String[] names=developerMode?new String[]{"التحكم","القياس والتحليل","السجل والتقارير","التجارب والمعايرة","الإعدادات"}:new String[]{"التحكم","القياس والتحليل","السجل والتقارير","الإعدادات"};tabButtons=new Button[names.length];for(int i=0;i<names.length;i++){final int x=i;Button bt=UiKit.button(this,names[i]);bt.setTextSize(12);bt.setOnClickListener(v->showTab(x));tabButtons[i]=bt;tabs.addView(bt,new LinearLayout.LayoutParams(UiKit.dp(this,i==1||developerMode&&i==3?145:120),UiKit.dp(this,46)));}tabsScroll.addView(tabs);root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));return root;
    }

    private void showTab(int index){if(!entered||index<0||index>=tabViews.length)return;if(selectedTab==0&&index!=0)controlTab.stopCamera();selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}if(index==0&&foreground)controlTab.startCamera();refreshVisibleTab();}

    private void scheduleComms(){if(commsScheduled)return;commsScheduled=true;workers.scheduleAtFixedRate(()->{if(!foreground||!entered)return;try{robotApi.drive(left,right);}catch(Exception ignored){}},0,250,TimeUnit.MILLISECONDS);workers.scheduleAtFixedRate(()->{if(!foreground||!entered)return;try{JSONObject snap=robotApi.snapshot(left,right);latestDiag=snap;sensorHealth.accept(snap,SystemClock.elapsedRealtime());telemetryRecorder.accept(snap,left,right,developerMode);ui.post(this::onTelemetryUpdated);}catch(Exception e){latestDiag=null;ui.post(this::onTelemetryUpdated);}},0,1,TimeUnit.SECONDS);}
    private void onTelemetryUpdated(){JSONObject d=latestDiag;if(globalStatus==null)return;if(d==null){globalStatus.setText("غير متصل");globalStatus.setTextColor(0xFFFFB74D);}else{JSONObject ir=d.optJSONObject("ir"),drv=d.optJSONObject("drive");boolean warn=ir!=null&&ir.optBoolean("warning",false),back=drv!=null&&drv.optBoolean("retrace",false);globalStatus.setText("متصل • "+d.optInt("clients",0)+(back?" • عودة":warn?" • ⚠ IR":""));globalStatus.setTextColor(back?0xFFFFB74D:warn?0xFFFF7043:0xFF81C784);}refreshVisibleTab();}
    private void refreshVisibleTab(){if(!entered)return;if(selectedTab==0)controlTab.refresh();else if(selectedTab==1)measurementTab.refresh();else if(selectedTab==2)reportsTab.refresh();else if(developerMode&&selectedTab==3)experimentsTab.refresh();else settingsTab.refresh();}

    @Override protected void onResume(){super.onResume();WifiRoute.bindConnectedWifi(this);foreground=true;if(entered&&selectedTab==0)controlTab.startCamera();if(entered)refreshVisibleTab();}
    @Override protected void onPause(){foreground=false;if(entered){controlTab.stopCamera();emergencyStop();}super.onPause();}
    @Override protected void onDestroy(){if(controlTab!=null)controlTab.releaseCamera();workers.shutdownNow();WifiRoute.unbind(this);if(db!=null)db.close();super.onDestroy();}

    @Override public RobotApi robotApi(){return robotApi;}@Override public SharedPreferences preferences(){return prefs;}@Override public ExperimentsDb database(){return db;}@Override public ExecutorService io(){return workers;}@Override public String operatorRole(){return developerMode?"DEVELOPER":"USER";}@Override public JSONObject latestDiagnostics(){return latestDiag;}@Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}@Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}
    @Override public void emergencyStop(){left=right=0;motionHistory.record(0,0,SystemClock.elapsedRealtime());if(controlTab!=null)controlTab.resetJoystick();workers.execute(()->{try{robotApi.stop();}catch(Exception ignored){}try{robotApi.cancelMeasurement();}catch(Exception ignored){}});}
    @Override public void setDriveCommand(int l,int r){int nl=Math.max(-100,Math.min(100,l)),nr=Math.max(-100,Math.min(100,r));left=nl;right=nr;motionHistory.record(nl,nr,SystemClock.elapsedRealtime());}
    @Override public int driveLeft(){return left;}@Override public int driveRight(){return right;}
    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(entered&&selectedTab==0&&foreground)controlTab.startCamera();}@Override public String cameraHost(){return cameraApi.host();}@Override public boolean cameraReachable(){return cameraApi.ping();}@Override public JSONObject cameraDiagnostics(){return cameraApi.status();}
    @Override public String flashMode(){return flashMode;}
    @Override public void setFlashMode(String mode){
        final String requested="on".equals(CameraApi.normalizeMode(mode))?"on":"off";
        workers.execute(()->{
            boolean ok=cameraApi.flash(requested);
            if(ok){flashMode=requested;prefs.edit().putString("flash_mode",requested).apply();if(controlTab!=null)ui.post(controlTab::refresh);showToast("الفلاش: "+flashAr(requested));}
            else if(!cameraApi.ping())showToast("الكاميرا غير متصلة على "+cameraApi.host()+"؛ لا يمكن إرسال أمر الفلاش");
            else showToast("الكاميرا متصلة لكن Firmware الحالي لا يدعم أوامر الفلاش المعروفة");
        });
    }

    @Override public SensorHealthEngine.Snapshot sensorHealth(String key){return sensorHealth.snapshot(key);}
    @Override public int captureCleanAirBaseline(){if(!developerMode)return 0;JSONObject d=latestDiag;int n=sensorHealth.captureCleanAirBaseline(d);if(n>0){for(String key:SensorHealthEngine.INSTALLED){double q=sensorHealth.baseline(key);if(Double.isFinite(q)&&q>0)db.putMeta("health_baseline_"+key,Double.toString(q));}db.putMeta("health_baseline_ts",isoNow());}return n;}
    @Override public int evaluateCleanAirDrift(){if(!developerMode)return 0;return sensorHealth.evaluateCleanAirDrift(latestDiag);}
    @Override public String cleanAirBaselineTimestamp(){return db==null?"":db.getMeta("health_baseline_ts","");}

    @Override public void requestExport(String format,long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("السجل غير موجود");return;}pendingExperimentId=experimentId;boolean pdf="pdf".equalsIgnoreCase(format);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Report_%06d_%s%s",r.id,fileStamp(),pdf?".pdf":".xlsx"));startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);}
    @Override public void requestReferenceBankExport(long experimentId){if(!developerMode){showToast("هذه الوظيفة متاحة للمطور فقط");return;}ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,r.referenceName==null||r.referenceName.isEmpty()?ReferenceBankExporter.fileName(r.id,r.ts):r.referenceName);startActivityForResult(i,EXPORT_REFERENCE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX&&requestCode!=EXPORT_REFERENCE)return;final Uri uri=data.getData();final int req=requestCode;final long id=pendingExperimentId;workers.execute(()->{ExperimentQueries.Record r=ExperimentQueries.byId(db,id);if(r==null){showToast("تعذر العثور على السجل");return;}try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IllegalStateException("output stream");ChemicalRepository chemicals=new ChemicalRepository(db);if(req==EXPORT_PDF)ReportExporter.writePdf(out,r,chemicals);else if(req==EXPORT_XLSX)ReportExporter.writeXlsx(out,r,chemicals);else ReferenceBankExporter.writeJson(out,r);showToast(req==EXPORT_PDF?"تم تصدير PDF":req==EXPORT_XLSX?"تم تصدير Excel":"تم تصدير سجل البنك المرجعي");}catch(Exception e){showToast("فشل التصدير: "+e.getClass().getSimpleName());}});}
    @Override public void refreshAllTabs(){if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}refreshVisibleTab();}

    private static String flashAr(String m){return"on".equals(m)?"تشغيل":"إيقاف";}
    private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
