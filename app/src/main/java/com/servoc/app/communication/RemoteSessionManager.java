package com.servoc.app.communication;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

/** Owns the Android side of the secure remote cloud session. */
public final class RemoteSessionManager {
    private static final Object CONNECT_LOCK=new Object();
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;RemoteServiceProfile.ensureDefaults(prefs);}

    public boolean serviceProfileConfigured(){
        RemoteServiceProfile.ensureDefaults(prefs);
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        return new RemoteCredentialStore(context,prefs).load().present();
    }

    /** Returns a connected session whose new session id has been accepted by the robot through a probe ACK. */
    public RemoteConnection ensureConnected()throws Exception{
        if(!serviceProfileConfigured())throw new IllegalStateException("remote service profile not configured");
        synchronized(CONNECT_LOCK){
            RemoteConnection live=RemoteConnection.active();if(live!=null)return live;
            RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();
            RemoteConnection r=RemoteConnection.connect(context,prefs);
            try{RemoteReadiness handshake=r.probe(3000);if(!handshake.commandAck)throw new IOException("remote session handshake failed");return r;}
            catch(Exception e){try{r.close();}catch(Exception ignored){}throw e;}
        }
    }

    public RemoteReadiness probe(){
        if(!serviceProfileConfigured())return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        try{RemoteConnection r=ensureConnected();return r.probe(3500);}catch(Exception e){return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,e.getClass().getSimpleName());}
    }

    /** Developer connectivity check that never changes a Direct session into Remote. */
    public RemoteReadiness probeTemporary(){boolean retain="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));try{return probe();}finally{if(!retain)close();}}

    /** Explicitly leaves Remote mode and permits RobotApi to use local HTTP again. */
    public void close(){synchronized(CONNECT_LOCK){RemoteConnection.releaseTransport();}}
}
