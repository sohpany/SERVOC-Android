package com.servoc.app.communication;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        int code=c.getResponseCode();
        InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        StringBuilder s=new StringBuilder();
        if(in!=null){try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}}
        c.disconnect();
        if(code<200||code>=300)throw new IllegalStateException("HTTP "+code+" "+s);
        return s.toString();
    }

    public JSONObject getJson(String path,int timeoutMs)throws Exception{return new JSONObject(getText(path,timeoutMs));}

    public void drive(int left,int right)throws Exception{getText("/drive?l="+left+"&r="+right,450);}
    public void stop()throws Exception{getText("/stop",450);}

    /** Compact API v3 diagnostics. */
    public JSONObject diagnosticsRaw()throws Exception{return getJson("/diag",1000);}
    /** Compact API v3 acquisition payload. */
    public JSONObject dataRaw()throws Exception{return getJson("/data",1200);}

    /** Single normalized application snapshot assembled from /diag + /data. */
    public JSONObject snapshot(int commandedLeft,int commandedRight)throws Exception{
        JSONObject diag=diagnosticsRaw();
        JSONObject data=dataRaw();
        return RobotProtocolV3.normalize(diag,data,commandedLeft,commandedRight);
    }

    /** Kept for connection tests; returns the raw compact diagnostic document. */
    public JSONObject diagnostics()throws Exception{return diagnosticsRaw();}
    public JSONObject data()throws Exception{return dataRaw();}
    public JSONObject config()throws Exception{return getJson("/cfg",1200);}

    public JSONObject setConfig(String cameraIp,int speed,int neutralLeft,int neutralRight,int warmupSec,int measureSec)throws Exception{
        String cam=URLEncoder.encode(cameraIp==null?"":cameraIp,StandardCharsets.UTF_8.name());
        String p="/cfg?cam="+cam+"&speed="+speed+"&nl="+neutralLeft+"&nr="+neutralRight+"&warm="+warmupSec+"&measure="+measureSec;
        return getJson(p,1500);
    }

    public void startMeasurement()throws Exception{getText("/measure?cmd=start",800);}
    public void cancelMeasurement()throws Exception{getText("/measure?cmd=cancel",800);}
    public void relay(int group)throws Exception{
        if(group<0||group>3)throw new IllegalArgumentException("relay group 0..3");
        getText("/relay?g="+group,800);
    }
}
