package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.servoc.app.auth.ConnectionGate;
import com.servoc.app.communication.RobotApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HazRob entry: choose only the transport. Developer access is unlocked later inside the Developer tab. */
public final class EntryActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private RobotApi local;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("servoc",MODE_PRIVATE);
        local=new RobotApi(prefs.getString("robot","192.168.4.1"));
        showConnection();
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
