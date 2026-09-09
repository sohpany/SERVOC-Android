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
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;RemoteServiceProfile.ensureDefaults(prefs);}

    public boolean serviceProfileConfigured(){
        RemoteServiceProfile.ensureDefaults(prefs);
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(context,prefs).load();String video=new RemoteVideoCredentialStore(prefs).load();
        return mqtt.present()&&!video.trim().isEmpty();
    }

    private void bootstrapIfNeeded()throws Exception{
        if(serviceProfileConfigured())return;
        HttpURLConnection c=(HttpURLConnection)new URL(BOOTSTRAP_URL).openConnection();
        c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setUseCaches(false);c.setRequestMethod("GET");
        c.setRequestProperty("Authorization","Bearer "+BOOTSTRAP_TOKEN);c.setRequestProperty("Accept","application/json");
        try{
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in,8192);
            if(code!=200)throw new IOException("bootstrap HTTP "+code);
            JSONObject j=new JSONObject(body);if(!j.optBoolean("ok",false))throw new IOException("bootstrap rejected");
            String robot=j.optString("robot","").trim(),user=j.optString("mqtt_user","").trim(),pass=j.optString("mqtt_password",""),view=j.optString("video_view_token","");
            if(!RemoteServiceProfile.ROBOT_ID.equals(robot)||user.isEmpty()||pass.length()<8||view.length()<8)throw new IOException("bootstrap incomplete");
            new RemoteCredentialStore(context,prefs).save(user,pass);new RemoteVideoCredentialStore(prefs).save(view);
        }finally{c.disconnect();}
    }

    private static String read(InputStream in,int max)throws Exception{
        if(in==null)return"";try(InputStream x=in;ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n,total=0;while((n=x.read(b))!=-1){total+=n;if(total>max)throw new IOException("bootstrap response too large");o.write(b,0,n);}return o.toString(StandardCharsets.UTF_8.name());}
    }

    /** Returns one accepted remote session. Existing live sessions are reused. */
    public RemoteConnection ensureConnected()throws Exception{
        bootstrapIfNeeded();if(!serviceProfileConfigured())throw new IllegalStateException("remote service profile not configured");
        synchronized(CONNECT_LOCK){
            RemoteConnection live=RemoteConnection.active();if(live!=null)return live;
            RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();
            RemoteConnection r=RemoteConnection.connect(context,prefs);
            try{RemoteReadiness handshake=r.probe(2800);if(!handshake.commandAck)throw new IOException("remote session handshake failed");return r;}
            catch(Exception e){try{r.close();}catch(Exception ignored){}throw e;}
        }
    }

    /** Entry/readiness check performs exactly one command/telemetry probe, not two consecutive probes. */
    public RemoteReadiness probe(){
        try{bootstrapIfNeeded();}catch(Exception e){return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"automatic remote setup failed");}
        if(!serviceProfileConfigured())return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        synchronized(CONNECT_LOCK){
            RemoteConnection r=RemoteConnection.active();
            try{
                if(r==null){RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();r=RemoteConnection.connect(context,prefs);}
                RemoteReadiness h=r.probe(2800);if(!h.commandAck){try{r.close();}catch(Exception ignored){}}return h;
            }catch(Exception e){if(r!=null)try{r.close();}catch(Exception ignored){}return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,e.getClass().getSimpleName());}
        }
    }

    /** Developer connectivity check that never changes a Direct session into Remote. */
    public RemoteReadiness probeTemporary(){boolean retain="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));try{return probe();}finally{if(!retain)close();}}

    /** Explicitly leaves Remote mode and permits RobotApi to use local HTTP again. */
    public void close(){synchronized(CONNECT_LOCK){RemoteConnection.releaseTransport();}}
}
