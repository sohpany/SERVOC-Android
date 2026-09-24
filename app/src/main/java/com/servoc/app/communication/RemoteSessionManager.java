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
    public interface Progress{void onStage(String detail,int stage);}
    private static final Object CONNECT_LOCK=new Object();
    private static final String BOOTSTRAP_TOKEN="hazrob-prototype-bootstrap-v1";
    private static final String BOOTSTRAP_CLIENT="android";
    private static final String BOOTSTRAP_SERVICE_VERSION=RemoteServiceProfile.SERVICE_VERSION;
    private static final int BOOTSTRAP_PROTOCOL=RemoteServiceProfile.PROTOCOL;
    private static final String CONTRACT_PREF="remote_bootstrap_contract";
    private static final String CONTRACT_ID="hazrob-0.10.2-protocol-2";
    // HR050: entry must fail visibly and promptly instead of inheriting a 15s/70s cold-start wait.
    private static final int BOOTSTRAP_CONNECT_TIMEOUT_MS=5000;
    private static final int BOOTSTRAP_READ_TIMEOUT_MS=8000;
    private final Context context;private final SharedPreferences prefs;
    public RemoteSessionManager(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;RemoteServiceProfile.ensureDefaults(prefs);}

    /** Control readiness intentionally excludes the independent video token/path. */
    public boolean serviceProfileConfigured(){
        RemoteServiceProfile.ensureDefaults(prefs);
        if(!CONTRACT_ID.equals(prefs.getString(CONTRACT_PREF,"")))return false;
        if(prefs.getString("remote_broker_host","").trim().isEmpty()||prefs.getString("remote_robot_id","").trim().isEmpty())return false;
        return new RemoteCredentialStore(context,prefs).load().present();
    }
    public boolean videoProfileConfigured(){return !new RemoteVideoCredentialStore(prefs).load().trim().isEmpty();}

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
            // MQTT/control is the admission authority. Video credentials are optional and saved only when present.
            if(!RemoteServiceProfile.ROBOT_ID.equals(robot)||user.isEmpty()||pass.length()<8)throw new IOException("bootstrap control profile incomplete");
            new RemoteCredentialStore(context,prefs).save(user,pass);if(view.length()>=8)new RemoteVideoCredentialStore(prefs).save(view);
            prefs.edit().putString(CONTRACT_PREF,CONTRACT_ID).apply();
        }finally{c.disconnect();}
    }

    private static String compact(String s){return s==null?"":s.replaceAll("\\s+","");}
    private static String detail(Throwable e){if(e==null)return"unknown";String m=e.getMessage();return e.getClass().getSimpleName()+(m==null||m.trim().isEmpty()?"":": "+m.trim());}
    private static String read(InputStream in,int max)throws Exception{
        if(in==null)return"";try(InputStream x=in;ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n,total=0;while((n=x.read(b))!=-1){total+=n;if(total>max)throw new IOException("bootstrap response too large");o.write(b,0,n);}return o.toString(StandardCharsets.UTF_8.name());}
    }
    private static void progress(Progress p,String text,int stage){if(p!=null)try{p.onStage(text,stage);}catch(Exception ignored){}}

    private RemoteConnection connectOnce()throws Exception{
        RemoteConnection live=RemoteConnection.active();if(live!=null)return live;
        RemoteConnection stale=RemoteConnection.current();if(stale!=null)stale.close();
        return RemoteConnection.connect(context,prefs);
    }

    /** Returns one accepted remote session. Existing live sessions are reused. If cached credentials are stale, refresh once from the 0.10.2 bootstrap service. */
    public RemoteConnection ensureConnected()throws Exception{
        bootstrapIfNeeded();if(!serviceProfileConfigured())throw new IllegalStateException("remote control profile not configured");
        synchronized(CONNECT_LOCK){
            Exception first=null;
            for(int attempt=0;attempt<2;attempt++){
                RemoteConnection r=null;
                try{
                    if(attempt==1)bootstrap(true);
                    r=connectOnce();RemoteReadiness handshake=r.probe(2800);if(!handshake.commandAck)throw new IOException("remote session handshake failed: "+handshake.detail);return r;
                }catch(Exception e){first=e;if(r!=null)try{r.close();}catch(Exception ignored){}if(attempt==1)throw e;}
            }
            throw first==null?new IOException("remote session failed"):first;
        }
    }

    /** Entry/readiness check. Control readiness does not depend on Railway video readiness. */
    public RemoteReadiness probe(){return probe(null);}
    public RemoteReadiness probe(Progress observer){
        progress(observer,"تهيئة ملف الاتصال البعيد",1);
        try{bootstrapIfNeeded();}catch(Exception e){return new RemoteReadiness(false,false,false,false,false,false,false,false,false,false,false,false,"automatic remote setup failed: "+detail(e));}
        if(!serviceProfileConfigured())return new RemoteReadiness(false,false,false,false,false,false,false,false,false,false,false,false,"remote control profile not configured");
        synchronized(CONNECT_LOCK){
            for(int attempt=0;attempt<2;attempt++){
                RemoteConnection r=null;
                try{
                    if(attempt==1){progress(observer,"تحديث بيانات التحكم من الخدمة",2);bootstrap(true);}
                    progress(observer,"فتح TLS/MQTT والاشتراك بالقنوات",6);
                    r=connectOnce();
                    progress(observer,"انتظار ACK وTelemetry من الروبوت",8);
                    RemoteReadiness h=r.probe(2800);progress(observer,h.ready?"قناة التحكم البعيد جاهزة":"اكتملت القناة جزئيًا: "+h.detail,h.ready?10:9);if(h.commandAck)return h;
                    try{r.close();}catch(Exception ignored){}
                }catch(Exception e){if(r!=null)try{r.close();}catch(Exception ignored){}if(attempt==1)return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,detail(e));}
            }
            return new RemoteReadiness(true,false,false,false,false,false,false,false,false,false,false,false,"remote handshake incomplete");
        }
    }

    /** Developer connectivity check that never changes a Direct session into Remote. */
    public RemoteReadiness probeTemporary(){boolean retain="remote".equalsIgnoreCase(prefs.getString("connection_mode","direct"));try{return probe();}finally{if(!retain)close();}}

    /** Explicitly leaves Remote mode and permits RobotApi to use local HTTP again. */
    public void close(){synchronized(CONNECT_LOCK){RemoteConnection.releaseTransport();}}
}
