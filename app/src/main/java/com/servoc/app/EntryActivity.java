package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.servoc.app.auth.ConnectionGate;
import com.servoc.app.communication.RobotApi;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Transport gate. Role selection remains in MainActivity after transport readiness succeeds. */
public final class EntryActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        SharedPreferences p=getSharedPreferences("servoc",MODE_PRIVATE);
        RobotApi local=new RobotApi(p.getString("robot","192.168.4.1"));
        ConnectionGate gate=new ConnectionGate(this,local,p,io,mode->{
            Intent i=new Intent(this,MainActivity.class);
            i.putExtra("connection_mode",mode==ConnectionGate.Mode.REMOTE?"remote":"direct");
            startActivity(i);finish();
        });
        setContentView(gate.view());
    }

    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
