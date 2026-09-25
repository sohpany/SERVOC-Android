package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.servoc.app.auth.ConnectionGate;
import com.servoc.app.communication.LocalNetworkContract;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.field.HazRobUserLibrary;
import android.net.Uri;
import android.app.AlertDialog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HazRob entry: choose only the transport. Developer access is unlocked later inside the Developer tab. */
public final class EntryActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private static final int USER_LIBRARY_TREE=7101;
    private RobotApi local;
    private HazRobUserLibrary userLibrary;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("servoc",MODE_PRIVATE);userLibrary=new HazRobUserLibrary(this);
        // Direct transport is a fixed hardware contract. Never inherit a stale saved host
        // from a previous build, remote experiment, or developer setting.
        local=new RobotApi(LocalNetworkContract.ROBOT_HOST);
        if(userLibrary.attached())showConnection();else showFirstRunLibrarySetup();
    }

    private void showFirstRunLibrarySetup(){
        new AlertDialog.Builder(this)
            .setTitle("إعداد مكتبة HazRob")
            .setMessage("في أول تشغيل اختر مكانًا آمنًا في ذاكرة الهاتف، ثم أنشئ مجلدًا باسم HazRob أو اختر مجلد HazRob موجودًا. ستُحفظ فيه المواقع والجلسات وبياناتها الدائمة.")
            .setCancelable(false)
            .setPositiveButton("إنشاء / اختيار مجلد HazRob",(d,w)->startActivityForResult(HazRobUserLibrary.createPickerIntent(),USER_LIBRARY_TREE))
            .show();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=USER_LIBRARY_TREE)return;
        if(resultCode!=RESULT_OK||data==null||data.getData()==null){showFirstRunLibrarySetup();return;}
        try{Uri uri=data.getData();userLibrary.attach(uri);showConnection();}
        catch(Exception e){new AlertDialog.Builder(this).setTitle("تعذر إعداد مكتبة HazRob").setMessage("اختر مجلدًا صالحًا يمكن للتطبيق القراءة والكتابة فيه.").setPositiveButton("إعادة المحاولة",(d,w)->showFirstRunLibrarySetup()).setCancelable(false).show();}
    }

    private void showConnection(){
        ConnectionGate gate=new ConnectionGate(this,local,prefs,io,false,mode->{
            Intent i=new Intent(this,MainActivity.class);
            i.putExtra("connection_mode",mode==ConnectionGate.Mode.REMOTE?"remote":"direct");
            startActivity(i);
            finish();
        });
        setContentView(gate.view());
    }

    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}