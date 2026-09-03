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

/** Direct/local HazRob HTTP API. Remote transport is layered separately in 0.9. */
public final class RobotApi {
    private volatile String robotHost;

    public RobotApi(String host){setRobotHost(host);}

    public void setRobotHost(String host){
        if(host==null||host.trim().isEmpty())host="192.168.4.1";
        host=host.trim().replace("http://","").replace("https://","");
        while(host.endsWith("/"))host=host.substring(0,host.length()-1);
        robotHost=host;
    }

    public String getRobotHost(){return robotHost;}
    private String url(String path){return "http://"+robotHost+(path.startsWith("/")?path:"/"+path);}

    public String getText(String path,int timeoutMs)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();
        c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("GET");
        return finish(c);
    }

    public String postForm(String path,String body,int timeoutMs)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();
        c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("Cache-Control","no-store");
        byte[] b=(body==null?"":body).getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream out=c.getOutputStream()){out.write(b);out.flush();}
        return finish(c);
    }

    private static String finish(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();
        if(in!=null){try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}}
        c.disconnect();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code+" "+s);return s.toString();
    }

    public JSONObject getJson(String path,int timeoutMs)throws Exception{return new JSONObject(getText(path,timeoutMs));}

    public void drive(int left,int right)throws Exception{getText("/drive?l="+left+"&r="+right,450);}

    /** API4 motion stop: never changes MQ relay state. */
    public void motionStop()throws Exception{try{getText("/motion/stop",450);}catch(Exception e){getText("/stop",450);}}
    public void stop()throws Exception{motionStop();}
    public void emergencyStop()throws Exception{try{getText("/estop",650);}catch(Exception e){getText("/stop",650);}}
    public void sensorPowerOff()throws Exception{try{getText("/sensor/off",650);}catch(Exception e){relayMask(0);}}

    /** First heartbeat arms the firmware communication-loss watchdog. */
    public void heartbeat()throws Exception{getText("/heartbeat",500);}
    public void disarmHeartbeat()throws Exception{getText("/heartbeat?arm=0",500);}

    public JSONObject diagnosticsRaw()throws Exception{return getJson("/diag",1000);}
    public JSONObject dataRaw()throws Exception{return getJson("/data",1200);}

    public JSONObject snapshot(int commandedLeft,int commandedRight)throws Exception{return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight);}

    public JSONObject diagnostics()throws Exception{return diagnosticsRaw();}
    public JSONObject data()throws Exception{return dataRaw();}
    public JSONObject config()throws Exception{return getJson("/cfg",1200);}

    public JSONObject remoteStatus()throws Exception{return getJson("/remote/status",1800);}
    public JSONObject remoteProvision(String ssid,String password)throws Exception{
        String body="ssid="+enc(ssid)+"&password="+enc(password);
        return new JSONObject(postForm("/remote/provision",body,3000));
    }
    public JSONObject remoteClear()throws Exception{return new JSONObject(postForm("/remote/clear","confirm=1",1500));}

    public JSONObject setConfig(String cameraIp,int speed,int neutralLeft,int neutralRight,int warmupSec,int measureSec)throws Exception{
        String cam=URLEncoder.encode(cameraIp==null?"":cameraIp,StandardCharsets.UTF_8.name());
        String p="/cfg?cam="+cam+"&speed="+speed+"&nl="+neutralLeft+"&nr="+neutralRight+"&warm="+warmupSec+"&measure="+measureSec;
        return getJson(p,1500);
    }

    public void startMeasurement()throws Exception{getText("/measure?cmd=start",800);}
    public void cancelMeasurement()throws Exception{getText("/measure?cmd=cancel",800);}

    public void relay(int group)throws Exception{if(group<0||group>2)throw new IllegalArgumentException("installed MQ relay group 0..2");getText("/relay?g="+group,800);}
    public void relayMask(int mask)throws Exception{if(mask<0||mask>3)throw new IllegalArgumentException("installed MQ relay mask 0..3");getText("/relay?mask="+mask,800);}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8.name());}
}
