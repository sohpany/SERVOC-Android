package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.CameraApi;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.communication.WifiRoute;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.report.ReferenceBankExporter;
import com.servoc.app.report.ReportExporter;
import com.servoc.app.ui.ChemicalsTab;
import com.servoc.app.ui.ControlTab;
import com.servoc.app.ui.ExperimentsTab;
import com.servoc.app.ui.HazRobHost;
import com.servoc.app.ui.PhysicalTab;
import com.servoc.app.ui.SensorsTab;
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

/** Central connection/heartbeat coordinator. Feature UI lives in independent tab classes. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101,EXPORT_PDF=8102,EXPORT_REFERENCE=8103;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(6);
    private SharedPreferences prefs;private RobotApi robotApi;private CameraApi cameraApi;private ExperimentsDb db;
    private volatile JSONObject latestDiag;private volatile int left=0,right=0;private volatile boolean foreground=false;private volatile String flashMode="off";
    private FrameLayout page;private TextView globalStatus;private Button[] tabButtons;private int selectedTab=0;private long pendingExperimentId=-1;
    private ControlTab controlTab;private SensorsTab sensorsTab;private ChemicalsTab chemicalsTab;private PhysicalTab physicalTab;private ExperimentsTab experimentsTab;private SettingsTab settingsTab;private View[] tabViews;

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("servoc",MODE_PRIVATE);robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));flashMode=prefs.getString("flash_mode","off");db=new ExperimentsDb(this);buildTabs();setContentView(buildShell());showTab(0);scheduleComms();}
    private void buildTabs(){controlTab=new ControlTab(this,this);sensorsTab=new SensorsTab(this,this);chemicalsTab=new ChemicalsTab(this,this);physicalTab=new PhysicalTab(this,this);experimentsTab=new ExperimentsTab(this,this);settingsTab=new SettingsTab(this,this);tabViews=new View[]{controlTab.view(),sensorsTab.view(),chemicalsTab.view(),physicalTab.view(),experimentsTab.view(),settingsTab.view()};}
    private View buildShell(){LinearLayout root=UiKit.vbox(this);root.setBackgroundColor(0xFF0A0D11);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout header=UiKit.hbox(this);header.setPadding(UiKit.dp(this,8),UiKit.dp(this,4),UiKit.dp(this,8),UiKit.dp(this,2));TextView title=UiKit.title(this,"HazRob 0.5 • روبوت فحص المخاطر عن بُعد");title.setTextColor(Color.WHITE);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));globalStatus=UiKit.small(this,"غير متصل");globalStatus.setTextColor(0xFFFFB74D);globalStatus.setGravity(Gravity.END);header.addView(globalStatus,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(this));HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);LinearLayout tabs=UiKit.hbox(this);String[] names={"التحكم","الحساسات","المركبات","القياسات","التجارب","الإعدادات"};tabButtons=new Button[names.length];for(int i=0;i<names.length;i++){final int x=i;Button bt=UiKit.button(this,names[i]);bt.setTextSize(12);bt.setOnClickListener(v->showTab(x));tabButtons[i]=bt;tabs.addView(bt,new LinearLayout.LayoutParams(UiKit.dp(this,115),UiKit.dp(this,46)));}tabsScroll.addView(tabs);root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));return root;}
    private void showTab(int index){if(index<0||index>=tabViews.length)return;if(selectedTab==0&&index!=0)controlTab.stopCamera();selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}if(index==0&&foreground)controlTab.startCamera();refreshVisibleTab();}
    private void scheduleComms(){workers.scheduleAtFixedRate(()->{if(!foreground)return;try{robotApi.drive(left,right);}catch(Exception ignored){}},0,250,TimeUnit.MILLISECONDS);workers.scheduleAtFixedRate(()->{if(!foreground)return;try{latestDiag=robotApi.diagnostics();ui.post(this::onTelemetryUpdated);}catch(Exception e){latestDiag=null;ui.post(this::onTelemetryUpdated);}},0,1,TimeUnit.SECONDS);}
    private void onTelemetryUpdated(){JSONObject d=latestDiag;if(d==null){globalStatus.setText("غير متصل");globalStatus.setTextColor(0xFFFFB74D);}else{globalStatus.setText("متصل • "+d.optInt("clients",0));globalStatus.setTextColor(0xFF81C784);}refreshVisibleTab();}
    private void refreshVisibleTab(){switch(selectedTab){case 0:controlTab.refresh();break;case 1:sensorsTab.refresh();break;case 2:chemicalsTab.refresh();break;case 3:physicalTab.refresh();break;case 4:experimentsTab.refresh();break;case 5:settingsTab.refresh();break;default:break;}}
    @Override protected void onResume(){super.onResume();WifiRoute.bindConnectedWifi(this);foreground=true;if(selectedTab==0)controlTab.startCamera();refreshVisibleTab();}
    @Override protected void onPause(){foreground=false;controlTab.stopCamera();emergencyStop();super.onPause();}
    @Override protected void onDestroy(){workers.shutdownNow();controlTab.releaseCamera();WifiRoute.unbind(this);if(db!=null)db.close();super.onDestroy();}

    @Override public RobotApi robotApi(){return robotApi;}@Override public SharedPreferences preferences(){return prefs;}@Override public ExperimentsDb database(){return db;}@Override public ExecutorService io(){return workers;}@Override public JSONObject latestDiagnostics(){return latestDiag;}@Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}@Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}
    @Override public void emergencyStop(){left=right=0;controlTab.resetJoystick();workers.execute(()->{try{robotApi.stop();}catch(Exception ignored){}try{robotApi.cancelMeasurement();}catch(Exception ignored){}});}
    @Override public void setDriveCommand(int l,int r){left=Math.max(-100,Math.min(100,l));right=Math.max(-100,Math.min(100,r));}@Override public int driveLeft(){return left;}@Override public int driveRight(){return right;}
    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(selectedTab==0&&foreground)controlTab.startCamera();}@Override public String cameraHost(){return cameraApi.host();}@Override public boolean cameraReachable(){return cameraApi.ping();}
    @Override public String flashMode(){return flashMode;}@Override public void setFlashMode(String mode){final String requested=CameraApi.normalizeMode(mode);workers.execute(()->{boolean ok=cameraApi.flash(requested);if(ok){flashMode=requested;prefs.edit().putString("flash_mode",requested).apply();ui.post(controlTab::refresh);showToast("وضع الفلاش: "+flashAr(requested));}else showToast("أمر الفلاش غير مدعوم في Firmware الكاميرا الحالي");});}
    @Override public void requestExport(String format,long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;boolean pdf="pdf".equalsIgnoreCase(format);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Report_%06d_%s%s",r.id,fileStamp(),pdf?".pdf":".xlsx"));startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);}
    @Override public void requestReferenceBankExport(long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,r.referenceName==null||r.referenceName.isEmpty()?ReferenceBankExporter.fileName(r.id,r.ts):r.referenceName);startActivityForResult(i,EXPORT_REFERENCE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX&&requestCode!=EXPORT_REFERENCE)return;final Uri uri=data.getData();final int req=requestCode;final long id=pendingExperimentId;workers.execute(()->{ExperimentQueries.Record r=ExperimentQueries.byId(db,id);if(r==null){showToast("تعذر العثور على التجربة");return;}try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IllegalStateException("output stream");if(req==EXPORT_PDF)ReportExporter.writePdf(out,r);else if(req==EXPORT_XLSX)ReportExporter.writeXlsx(out,r);else ReferenceBankExporter.writeJson(out,r);showToast(req==EXPORT_PDF?"تم تصدير تقرير PDF":req==EXPORT_XLSX?"تم تصدير تقرير Excel":"تم تصدير ملف البنك المرجعي");}catch(Exception e){showToast("فشل التصدير: "+e.getClass().getSimpleName());}});}
    @Override public void refreshAllTabs(){if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}refreshVisibleTab();}
    private static String flashAr(String m){return"on".equals(m)?"تشغيل":"auto".equals(m)?"تلقائي":"إيقاف";}private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
}
