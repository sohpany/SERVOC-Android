package com.servoc.app.communication;

import android.content.Context;
import android.content.SharedPreferences;
import com.servoc.app.camera.RemoteVideoCredentialStore;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Owns the Android side of the secure remote cloud session. */
public final class RemoteSessionManager {
    private static final Object CONNECT_LOCK=new Object();
    private static final String BOOTSTRAP_URL="https://hazrob-video-relay.onrender.com/hazrob/bootstrap";
    private static final String BOOTSTRAP_TOKEN="hazrob-prototype-bootstrap-v1";
    private static final int BOOTSTRAP_SCHEMA=2;
    private static final String PREF_BOOTSTRAP_SCHEMA="remote_bootstrap_schema";
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;RemoteServiceProfile.ensureDefaults(prefs);}

    public boolean serviceProfileConfigured(){
        RemoteServiceProfile.ensureDefaults(prefs);
        if(prefs.getInt(PREF_BOOTSTRAP_SCHEMA,0)<BOOTSTRAP_SCHEMA)return false;
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(context,prefs).load();String video=new RemoteVideoCredentialStore(prefs).load();
        return mqtt.present()&&!video.trim().isEmpty();
    }

    private void bootstrapIfNeeded()throws Exception{bootstrap(false);}
    private void bootstrap(boolean force)throws Exception{
        if(!force&&serviceProfileConfigured())return;
        HttpURLConnection c=(HttpURLConnection)new URL(BOOTSTRAP_URL).openConnection();
        c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setUseCaches(false);c.setRequestMethod("GET");
        c.setRequestProperty("Authorization","Bearer "+BOOTSTRAP_TOKEN);c.setRequestProperty("Accept","application/json");
        try{
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in,8192);
            if(code!=200)throw new IOException("bootstrap HTTP "+code);
            JSONObject j=new JSONObject(body);if(!j.optBoolean("ok",false))throw new IOException("bootstrap rejected");
            if(j.optInt("protocol",0)<BOOTSTRAP_SCHEMA)throw new IOException("bootstrap protocol obsolete");
            String robot=j.optString("robot","").trim(),user=j.optString("mqtt_user","").trim(),pass=j.optString("mqtt_password",""),view=j.optString("video_view_token","");
            if(!RemoteServiceProfile.ROBOT_ID.equals(robot)||user.isEmpty()||pass.length()<8||view.length()<8||pass.equals(view))throw new IOException("bootstrap incomplete");
            new RemoteCredentialStore(context,prefs).save(user,pass);new RemoteVideoCredentialStore(prefs).save(view);
            prefs.edit().putInt(PREF_BOOTSTRAP_SCHEMA,BOOTSTRAP_SCHEMA).apply();
        }finally{c.disconnect();}
    }

    private static String read(InputStream in,int max)throws Exception{
        if(in==null)return"";try(InputStream x=in;ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n,total=0;while((n=x.read(b))!=-1){total+=n;if(total>max)throw new IOException("bootstrap response too large");o.write(b,0,n);}return o.toString(StandardCharsets.UTF_8.name());}
    }

    private static boolean mqttAuthFailure(Throwable e){
        for(Throwable x=e;x!=null;x=x.getCause()){String m=x.getMessage();if(m!=null&&m.contains("MQTT CONNACK rejected"))return true;}return false;
    }

    private RemoteConnection connectAndProbe()throws Exception{
        RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();
        RemoteConnection r=RemoteConnection.connect(context,prefs);
        try{RemoteReadiness handshake=r.probe(3000);if(!handshake.commandAck)throw new IOException("remote session handshake failed");return r;}
        catch(Exception e){try{r.close();}catch(Exception ignored){}throw e;}
    }

    /** Returns a connected session whose new session id has been accepted by the robot through a probe ACK. */
    public RemoteConnection ensureConnected()throws Exception{
        bootstrapIfNeeded();if(!serviceProfileConfigured())throw new IllegalStateException("remote service profile not configured");
        synchronized(CONNECT_LOCK){
            RemoteConnection live=RemoteConnection.active();if(live!=null)return live;
            try{return connectAndProbe();}
            catch(Exception first){
                if(!mqttAuthFailure(first))throw first;
                RemoteConnection.releaseTransport();
                bootstrap(true);
                if(!serviceProfileConfigured())throw new IllegalStateException("remote service profile not configured");
                return connectAndProbe();
            }
        }
    }

    public RemoteReadiness probe(){
        try{bootstrapIfNeeded();}catch(Exception e){return new RemoteReadiness(false,false,false,false,false,false,false,false,false,false,false,false,"automatic remote setup failed");}
        if(!serviceProfileConfigured())return new RemoteReadiness(false,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        try{RemoteConnection r=ensureConnected();return r.probe(3500);}catch(Exception e){return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,e.getClass().getSimpleName());}
    }

    /** Developer connectivity check that never changes a Direct session into Remote. */
    public RemoteReadiness probeTemporary(){boolean retain="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));try{return probe();}finally{if(!retain)close();}}

    /** Explicitly leaves Remote mode and permits RobotApi to use local HTTP again. */
    public void close(){synchronized(CONNECT_LOCK){RemoteConnection.releaseTransport();}}
}
