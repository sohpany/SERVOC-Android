package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.servoc.app.auth.ConnectionGate;
import com.servoc.app.auth.RoleGate;
import com.servoc.app.communication.RobotApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Role gate first, then transport selection. Ordinary users enter remote mode with a revocable authorization code. */
public final class EntryActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private RobotApi local;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("servoc",MODE_PRIVATE);
        local=new RobotApi(prefs.getString("robot","192.168.4.1"));
        showRoleGate();
    }

    private void showRoleGate(){
        setContentView(RoleGate.build(this,new RoleGate.Listener(){
            @Override public void onUser(){showConnection(false);}
            @Override public void onDeveloper(){showConnection(true);}
        }));
    }

    private void showConnection(boolean developer){
        prefs.edit().putString("operator_role",developer?"DEVELOPER":"USER").apply();
        ConnectionGate gate=new ConnectionGate(this,local,prefs,io,developer,mode->{
            Intent i=new Intent(this,MainActivity.class);
            i.putExtra("connection_mode",mode==ConnectionGate.Mode.REMOTE?"remote":"direct");
            i.putExtra("developer_mode",developer);
            startActivity(i);finish();
        });
        setContentView(gate.view());
    }

    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
