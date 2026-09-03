package com.servoc.app.communication;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** HazRob API facade: direct HTTP locally, secure MQTT automatically when a remote session owns transport. */
public final class RobotApi {
    private volatile String robotHost;
    public RobotApi(String host){setRobotHost(host);}

    public void setRobotHost(String host){if(host==null||host.trim().isEmpty())host="192.168.4.1";host=host.trim().replace("http://","").replace("https://","");while(host.endsWith("/"))host=host.substring(0,host.length()-1);robotHost=host;}
    public String getRobotHost(){return robotHost;}
    private String url(String path){return "http://"+robotHost+(path.startsWith("/")?path:"/"+path);}
    private static RemoteConnection remote(){return RemoteConnection.current();}

    public String getText(String path,int timeoutMs)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("GET");return finish(c);}
    public String postForm(String path,String body,int timeoutMs)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("Cache-Control","no-store");byte[] b=(body==null?"":body).getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream out=c.getOutputStream()){out.write(b);out.flush();}return finish(c);}
    private static String finish(HttpURLConnection c)throws Exception{int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();if(in!=null){try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}}c.disconnect();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code+" "+s);return s.toString();}
    public JSONObject getJson(String path,int timeoutMs)throws Exception{return new JSONObject(getText(path,timeoutMs));}

    public void drive(int left,int right)throws Exception{RemoteConnection r=remote();if(r!=null){r.drive(left,right);return;}getText("/drive?l="+left+"&r="+right,450);}
    public void motionStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.motionStop();return;}try{getText("/motion/stop",450);}catch(Exception e){getText("/stop",450);}}
    public void stop()throws Exception{motionStop();}
    public void emergencyStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.emergencyStop();return;}try{getText("/estop",650);}catch(Exception e){getText("/stop",650);}}
    public void sensorPowerOff()throws Exception{RemoteConnection r=remote();if(r!=null){r.sensorOff();return;}try{getText("/sensor/off",650);}catch(Exception e){relayMask(0);}}
    public void heartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.heartbeat();return;}getText("/heartbeat",500);}
    public void disarmHeartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.command("heartbeat_disarm",new JSONObject(),2000,false,0);return;}getText("/heartbeat?arm=0",500);}

    /** Raw endpoints intentionally remain local for provisioning and direct-mode compatibility checks. */
    public JSONObject diagnosticsRaw()throws Exception{return getJson("/diag",1000);}
    public JSONObject dataRaw()throws Exception{return getJson("/data",1200);}
    public JSONObject snapshot(int commandedLeft,int commandedRight)throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(commandedLeft,commandedRight);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight);}
    public JSONObject diagnostics()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return diagnosticsRaw();}
    public JSONObject data()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return dataRaw();}

    public JSONObject config()throws Exception{RemoteConnection r=remote();if(r!=null){JSONObject a=r.command("config_get",new JSONObject(),3000,true,2500);JSONObject p=a.optJSONObject("payload");return p==null?new JSONObject():p;}return getJson("/cfg",1200);}

    /** These three endpoints are intentionally local: they bootstrap the robot onto its 4G/WAN network. */
    public JSONObject remoteStatus()throws Exception{return getJson("/remote/status",1800);}
    public JSONObject remoteProvision(String ssid,String password)throws Exception{return new JSONObject(postForm("/remote/provision","ssid="+enc(ssid)+"&password="+enc(password),3000));}
    public JSONObject remoteClear()throws Exception{return new JSONObject(postForm("/remote/clear","confirm=1",1500));}

    public JSONObject setConfig(String cameraIp,int speed,int neutralLeft,int neutralRight,int warmupSec,int measureSec)throws Exception{
        RemoteConnection r=remote();if(r!=null){JSONObject p=new JSONObject();p.put("cam",cameraIp==null?"":cameraIp);p.put("speed",speed);p.put("nl",neutralLeft);p.put("nr",neutralRight);p.put("warm",warmupSec);p.put("measure",measureSec);JSONObject a=r.command("config_set",p,4000,true,3000);JSONObject q=a.optJSONObject("payload");return q==null?p:q;}
        String cam=URLEncoder.encode(cameraIp==null?"":cameraIp,StandardCharsets.UTF_8.name());return getJson("/cfg?cam="+cam+"&speed="+speed+"&nl="+neutralLeft+"&nr="+neutralRight+"&warm="+warmupSec+"&measure="+measureSec,1500);
    }

    public void startMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.startMeasurement();return;}getText("/measure?cmd=start",800);}
    public void cancelMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.cancelMeasurement();return;}getText("/measure?cmd=cancel",800);}
    public void relay(int group)throws Exception{if(group<0||group>2)throw new IllegalArgumentException("installed MQ relay group 0..2");relayMask(group==0?0:1<<(group-1));}
    public void relayMask(int mask)throws Exception{if(mask<0||mask>3)throw new IllegalArgumentException("installed MQ relay mask 0..3");RemoteConnection r=remote();if(r!=null){r.relayMask(mask);return;}getText("/relay?mask="+mask,800);}
    public void cameraFlash(String mode)throws Exception{RemoteConnection r=remote();if(r==null)throw new IllegalStateException("remote session unavailable");r.cameraFlash(mode);}
    public JSONObject remoteCameraStatus(){RemoteConnection r=remote();return r==null?null:r.cameraStatus();}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8.name());}
}
