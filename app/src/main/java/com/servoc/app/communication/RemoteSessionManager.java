package com.servoc.app.communication;

import android.content.Context;
import android.content.SharedPreferences;

/** Owns the Android side of the secure remote cloud session. */
public final class RemoteSessionManager {
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;}

    public boolean serviceProfileConfigured(){
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        return new RemoteCredentialStore(context,prefs).load().present();
    }

    public RemoteReadiness probe(){
        if(!serviceProfileConfigured())return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        try{RemoteConnection old=RemoteConnection.active();RemoteConnection r=old!=null?old:RemoteConnection.connect(context,prefs);return r.probe(3500);}catch(Exception e){return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,e.getClass().getSimpleName());}
    }

    public void close(){RemoteConnection r=RemoteConnection.active();if(r!=null)r.close();}
}
