package com.servoc.app.communication;

import android.content.SharedPreferences;

/** Remote cloud session boundary. MQTT implementation plugs into this class without changing UI/navigation. */
public final class RemoteSessionManager {
    private final SharedPreferences prefs;
    public RemoteSessionManager(SharedPreferences prefs){this.prefs=prefs;}

    public boolean serviceProfileConfigured(){
        return !prefs.getString("remote_broker_host","").trim().isEmpty()
                &&!prefs.getString("remote_robot_id","").trim().isEmpty();
    }

    /** Until secure MQTT credentials are provisioned this deliberately refuses a false-ready state. */
    public RemoteReadiness probe(){
        if(!serviceProfileConfigured())return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"MQTT transport pending secure service handshake");
    }
}
