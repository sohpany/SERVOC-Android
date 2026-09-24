package com.servoc.app.communication;

import android.content.SharedPreferences;

/** Canonical HazRob 0.10.2 remote-service coordinates. Secrets are stored separately. */
public final class RemoteServiceProfile {
    public static final String SERVICE_VERSION="0.10.2";
    public static final int PROTOCOL=2;
    public static final String ROBOT_ID="hazrob-001";
    public static final String MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud";
    public static final int MQTT_PORT=8883;
    public static final String VIDEO_HOST="hazrob-video-relay.onrender.com";
    public static final String BOOTSTRAP_HTTPS="https://"+VIDEO_HOST+"/hazrob/bootstrap";
    public static final String VIDEO_VIEW_WSS="wss://"+VIDEO_HOST+"/hazrob/video/view/"+ROBOT_ID;
    public static final String VIDEO_HEALTH_HTTPS="https://"+VIDEO_HOST+"/health";
    public static final String VIDEO_STATUS_HTTPS="https://"+VIDEO_HOST+"/hazrob/video/status/"+ROBOT_ID;

    /** Removes stale service coordinates left by older experimental builds. */
    public static void ensureDefaults(SharedPreferences prefs){
        if(prefs==null)return;
        SharedPreferences.Editor e=prefs.edit();boolean changed=false;
        if(!MQTT_HOST.equals(prefs.getString("remote_broker_host","").trim())){e.putString("remote_broker_host",MQTT_HOST);changed=true;}
        if(prefs.getInt("remote_broker_port",0)!=MQTT_PORT){e.putInt("remote_broker_port",MQTT_PORT);changed=true;}
        if(!ROBOT_ID.equals(prefs.getString("remote_robot_id","").trim())){e.putString("remote_robot_id",ROBOT_ID);changed=true;}
        if(!VIDEO_VIEW_WSS.equals(prefs.getString("remote_video_url","").trim())){e.putString("remote_video_url",VIDEO_VIEW_WSS);changed=true;}
        if(changed)e.apply();
    }

    private RemoteServiceProfile(){}
}
