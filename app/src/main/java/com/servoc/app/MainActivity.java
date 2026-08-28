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
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Central connection/heartbeat coordinator. Feature UI lives in independent tab classes. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101, EXPORT_PDF=8102;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(6);

    private SharedPreferences prefs;
    private RobotApi robotApi;
    private CameraApi cameraApi;
    private ExperimentsDb db;
    private volatile JSONObject latestDiag;
    private volatile int left=0,right=0;
    private volatile boolean foreground=false,flash=false;

    private FrameLayout page;
    private TextView globalStatus;
    private Button[] tabButtons;
    private int selectedTab=0;

    private ControlTab controlTab;
    private SensorsTab sensorsTab;
    private ChemicalsTab chemicalsTab;
    private PhysicalTab physicalTab;
    private ExperimentsTab experimentsTab;
    private SettingsTab settingsTab;
    private View[] tabViews;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("servoc",MODE_PRIVATE);
        robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));
        cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));
        db=new ExperimentsDb(this);
        buildTabs();
        setContentView(buildShell());
        showTab(0);
        scheduleComms();
    }

    private void buildTabs(){
        controlTab=new ControlTab(this,this);
        sensorsTab=new SensorsTab(this,this);
        chemicalsTab=new ChemicalsTab(this,this);
        physicalTab=new PhysicalTab(this,this);
        experimentsTab=new ExperimentsTab(this,this);
        settingsTab=new SettingsTab(this,this);
        tabViews=new View[]{controlTab.view(),sensorsTab.view(),chemicalsTab.view(),physicalTab.view(),experimentsTab.view(),settingsTab.view()};
    }

    private View buildShell(){
        LinearLayout root=UiKit.vbox(this);root.setBackgroundColor(0xFF0A0D11);
        LinearLayout header=UiKit.hbox(this);header.setPadding(UiKit.dp(this,8),UiKit.dp(this,4),UiKit.dp(this,8),UiKit.dp(this,2));
        TextView title=UiKit.title(this,"HazRob 0.4 • REMOTE HAZARD ROBOT");title.setTextColor(Color.WHITE);header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        globalStatus=UiKit.small(this,"OFFLINE");globalStatus.setTextColor(0xFFFFB74D);globalStatus.setGravity(Gravity.END);header.addView(globalStatus,new LinearLayout.LayoutParams(-2,-2));root.addView(header,UiKit.full(this));

        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=UiKit.hbox(this);String[] names={"CONTROL","SENSORS","CHEMICALS","PHYSICAL","EXPERIMENTS","SETTINGS"};tabButtons=new Button[names.length];
        for(int i=0;i<names.length;i++){final int x=i;Button b=UiKit.button(this,names[i]);b.setTextSize(11);b.setOnClickListener(v->showTab(x));tabButtons[i]=b;tabs.addView(b,new LinearLayout.LayoutParams(UiKit.dp(this,122),UiKit.dp(this,46)));}
        tabsScroll.addView(tabs);root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));
        page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));return root;
    }

    private void showTab(int index){
        if(index<0||index>=tabViews.length)return;
        if(selectedTab==0&&index!=0)controlTab.stopCamera();
        selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));
        for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}
        if(index==0&&foreground)controlTab.startCamera();
        refreshAllTabs();
    }

    private void scheduleComms(){
        workers.scheduleAtFixedRate(()->{
            if(!foreground)return;
            try{robotApi.drive(left,right);}catch(Exception ignored){}
        },0,250,TimeUnit.MILLISECONDS);

        workers.scheduleAtFixedRate(()->{
            if(!foreground)return;
            try{latestDiag=robotApi.diagnostics();ui.post(this::refreshDynamicTabs);}
            catch(Exception e){latestDiag=null;ui.post(this::refreshDynamicTabs);}
        },0,1,TimeUnit.SECONDS);
    }

    private void refreshDynamicTabs(){
        JSONObject d=latestDiag;
        if(d==null){globalStatus.setText("OFFLINE");globalStatus.setTextColor(0xFFFFB74D);}else{globalStatus.setText("ONLINE • "+d.optInt("clients",0));globalStatus.setTextColor(0xFF81C784);}
        controlTab.refresh();sensorsTab.refresh();chemicalsTab.refresh();physicalTab.refresh();
        if(selectedTab==4)experimentsTab.refresh();
    }

    @Override protected void onResume(){super.onResume();foreground=true;if(selectedTab==0)controlTab.startCamera();}
    @Override protected void onPause(){foreground=false;controlTab.stopCamera();emergencyStop();super.onPause();}
    @Override protected void onDestroy(){workers.shutdownNow();controlTab.stopCamera();if(db!=null)db.close();super.onDestroy();}

    // HazRobHost
    @Override public RobotApi robotApi(){return robotApi;}
    @Override public SharedPreferences preferences(){return prefs;}
    @Override public ExperimentsDb database(){return db;}
    @Override public ExecutorService io(){return workers;}
    @Override public JSONObject latestDiagnostics(){return latestDiag;}
    @Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}
    @Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}

    @Override public void emergencyStop(){
        left=right=0;controlTab.resetJoystick();workers.execute(()->{try{robotApi.stop();}catch(Exception ignored){}});
    }
    @Override public void setDriveCommand(int l,int r){left=Math.max(-100,Math.min(100,l));right=Math.max(-100,Math.min(100,r));}
    @Override public int driveLeft(){return left;}
    @Override public int driveRight(){return right;}

    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(selectedTab==0&&foreground)controlTab.startCamera();}
    @Override public String cameraHost(){return cameraApi.host();}
    @Override public boolean flashState(){return flash;}
    @Override public void setFlash(boolean on){
        workers.execute(()->{boolean ok=cameraApi.flash(on);if(ok){flash=on;ui.post(controlTab::refresh);showToast(on?"Camera flash ON":"Camera flash OFF");}else showToast("Flash control endpoint not available in current ESP32-CAM firmware");});
    }

    @Override public void requestExport(String format){
        boolean pdf="pdf".equalsIgnoreCase(format);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,"HazRob_Experiments_"+fileStamp()+(pdf?".pdf":".xlsx"));startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        final Uri uri=data.getData();final boolean pdf=requestCode==EXPORT_PDF;if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX)return;
        workers.execute(()->{
            try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                if(out==null)throw new IllegalStateException("output stream");List<ExperimentQueries.Record> rows=ExperimentQueries.recent(db,1000);
                if(pdf)ReportExporter.writePdf(out,rows);else ReportExporter.writeXlsx(out,rows);showToast(pdf?"PDF exported":"Excel workbook exported");
            }catch(Exception e){showToast("Export failed: "+e.getClass().getSimpleName());}
        });
    }

    @Override public void refreshAllTabs(){
        if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}
        refreshDynamicTabs();experimentsTab.refresh();
    }

    private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
}
