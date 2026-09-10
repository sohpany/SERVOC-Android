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
    private static final String BOOTSTRAP_TOKEN="hazrob-prototype-bootstrap-v1";
    private static final String BOOTSTRAP_CLIENT="android";
    private static final String BOOTSTRAP_SERVICE_VERSION=RemoteServiceProfile.SERVICE_VERSION;
    private static final int BOOTSTRAP_PROTOCOL=RemoteServiceProfile.PROTOCOL;
    private static final String CONTRACT_PREF="remote_bootstrap_contract";
    private static final String CONTRACT_ID="hazrob-0.10.1-protocol-2";
    // Render Free can cold-start well after the TCP connection is accepted. Bootstrap runs on
    // the background I/O executor, so a long read window does not block the Android UI thread.
    private static final int BOOTSTRAP_CONNECT_TIMEOUT_MS=15000;
    private static final int BOOTSTRAP_READ_TIMEOUT_MS=70000;
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;RemoteServiceProfile.ensureDefaults(prefs);}

    public boolean serviceProfileConfigured(){
        RemoteServiceProfile.ensureDefaults(prefs);
        if(!CONTRACT_ID.equals(prefs.getString(CONTRACT_PREF,"")))return false;
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(context,prefs).load();String video=new RemoteVideoCredentialStore(prefs).load();
        return mqtt.present()&&!video.trim().isEmpty();
    }

    private void bootstrapIfNeeded()throws Exception{bootstrap(false);}
    private void bootstrap(boolean force)throws Exception{
        if(!force&&serviceProfileConfigured())return;
        HttpURLConnection c=(HttpURLConnection)new URL(RemoteServiceProfile.BOOTSTRAP_HTTPS).openConnection();
        c.setConnectTimeout(BOOTSTRAP_CONNECT_TIMEOUT_MS);c.setReadTimeout(BOOTSTRAP_READ_TIMEOUT_MS);c.setUseCaches(false);c.setRequestMethod("GET");
        c.setRequestProperty("Authorization","Bearer "+BOOTSTRAP_TOKEN);c.setRequestProperty("X-HazRob-Client",BOOTSTRAP_CLIENT);c.setRequestProperty("Accept","application/json");
        try{
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in,8192);
            if(code!=200)throw new IOException("bootstrap HTTP "+code);
            JSONObject j=new JSONObject(body);if(!j.optBoolean("ok",false))throw new IOException("bootstrap rejected");
            String version=j.optString("version","").trim();int protocol=j.optInt("protocol",-1);
            if(!BOOTSTRAP_SERVICE_VERSION.equals(version)||protocol!=BOOTSTRAP_PROTOCOL)throw new IOException("bootstrap contract mismatch");
            String role=j.optString("client","").trim();if(!role.isEmpty()&&!BOOTSTRAP_CLIENT.equals(role))throw new IOException("bootstrap role mismatch");
            String robot=j.optString("robot","").trim(),user=j.optString("mqtt_user","").trim(),pass=compact(j.optString("mqtt_password","")),view=compact(j.optString("video_view_token",""));
            if(!RemoteServiceProfile.ROBOT_ID.equals(robot)||user.isEmpty()||pass.length()<8||view.length()<8)throw new IOException("bootstrap incomplete");
            new RemoteCredentialStore(context,prefs).save(user,pass);new RemoteVideoCredentialStore(prefs).save(view);
            prefs.edit().putString(CONTRACT_PREF,CONTRACT_ID).apply();
        }finally{c.disconnect();}
    }

    private static String compact(String s){return s==null?"":s.replaceAll("\\s+","");}
    private static String read(InputStream in,int max)throws Exception{
        if(in==null)return"";try(InputStream x=in;ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n,total=0;while((n=x.read(b))!=-1){total+=n;if(total>max)throw new IOException("bootstrap response too large");o.write(b,0,n);}return o.toString(StandardCharsets.UTF_8.name());}
    }

    private RemoteConnection connectOnce()throws Exception{
        RemoteConnection live=RemoteConnection.active();if(live!=null)return live;
        RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();
        return RemoteConnection.connect(context,prefs);
    }

    /** Returns one accepted remote session. Existing live sessions are reused. If cached credentials are stale, refresh once from Render. */
    public RemoteConnection ensureConnected()throws Exception{
        bootstrapIfNeeded();if(!serviceProfileConfigured())throw new IllegalStateException("remote service profile not configured");
        synchronized(CONNECT_LOCK){
            Exception first=null;
            for(int attempt=0;attempt<2;attempt++){
                RemoteConnection r=null;
                try{
                    if(attempt==1)bootstrap(true);
                    r=connectOnce();RemoteReadiness handshake=r.probe(2800);if(!handshake.commandAck)throw new IOException("remote session handshake failed");return r;
                }catch(Exception e){first=e;if(r!=null)try{r.close();}catch(Exception ignored){}if(attempt==1)throw e;}
            }
            throw first==null?new IOException("remote session failed"):first;
        }
    }

    /** Entry/readiness check performs one probe; stale cached credentials trigger one forced bootstrap refresh. */
    public RemoteReadiness probe(){
        try{bootstrapIfNeeded();}catch(Exception e){return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"automatic remote setup failed: "+e.getMessage());}
        if(!serviceProfileConfigured())return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote service profile not configured");
        synchronized(CONNECT_LOCK){
            for(int attempt=0;attempt<2;attempt++){
                RemoteConnection r=null;
                try{
                    if(attempt==1)bootstrap(true);
                    r=connectOnce();RemoteReadiness h=r.probe(2800);if(h.commandAck)return h;
                    try{r.close();}catch(Exception ignored){}
                }catch(Exception e){if(r!=null)try{r.close();}catch(Exception ignored){}if(attempt==1)return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,e.getClass().getSimpleName());}
            }
            return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote handshake incomplete");
        }
    }

    /** Developer connectivity check that never changes a Direct session into Remote. */
    public RemoteReadiness probeTemporary(){boolean retain="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));try{return probe();}finally{if(!retain)close();}}

    /** Explicitly leaves Remote mode and permits RobotApi to use local HTTP again. */
    public void close(){synchronized(CONNECT_LOCK){RemoteConnection.releaseTransport();}}
}
