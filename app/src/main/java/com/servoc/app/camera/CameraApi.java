package com.servoc.app.camera;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Canonical HazRob 0.10.1 ESP32-CAM HTTP client. */
public final class CameraApi {
    private volatile String host;
    private volatile String lastProbe="not tested";

    public CameraApi(String host){setHost(host);}

    public void setHost(String h){
        if(h==null)h="";
        h=h.trim().replace("http://","").replace("https://","");
        int slash=h.indexOf('/');if(slash>=0)h=h.substring(0,slash);
        int colon=h.indexOf(':');if(colon>0)h=h.substring(0,colon);
        while(h.endsWith("/"))h=h.substring(0,h.length()-1);
        host=h;
    }

    public String host(){return host;}
    public String lastProbe(){return lastProbe;}

    public String[] streamUrls(){String h=host;return new String[]{"http://"+h+":81/stream","http://"+h+"/stream"};}

    public JSONObject status(){
        try{
            String text=getText(80,"/status",1800);
            if(text==null||text.trim().isEmpty())return null;
            JSONObject j=new JSONObject(text);lastProbe="HTTP 80 /status";return j;
        }catch(Exception ignored){return null;}
    }

    public boolean ping(){
        if(host==null||host.isEmpty()){lastProbe="camera host empty";return false;}
        if(status()!=null)return true;
        if(probe(80,"/",true)){lastProbe="HTTP 80 /";return true;}
        if(probe(81,"/stream",false)){lastProbe="HTTP 81 /stream";return true;}
        lastProbe="unreachable at "+host+" (80/81)";return false;
    }

    /**
     * Direct-mode acceptance is intentionally stronger than ping(): it proves the camera firmware
     * is healthy at the canonical local IP and that port 81 actually produces a JPEG frame.
     */
    public boolean directReady(){
        if(host==null||host.isEmpty()){lastProbe="camera host empty";return false;}
        JSONObject j=status();
        if(j==null||!j.optBoolean("camera_ok",false)){lastProbe="camera status not ready";return false;}
        String ip=j.optString("ip","").trim();
        if(!host.equals(ip)){lastProbe="camera IP mismatch: "+ip;return false;}
        return localStreamReady();
    }

    public boolean localStreamReady(){
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+":81/stream").openConnection();
            c.setConnectTimeout(1400);c.setReadTimeout(3500);c.setUseCaches(false);c.setInstanceFollowRedirects(true);c.setRequestMethod("GET");c.setRequestProperty("Connection","close");
            int code=c.getResponseCode();if(code<200||code>=300){lastProbe="MJPEG HTTP "+code;return false;}
            try(InputStream in=c.getInputStream()){
                int prev=-1,cur,count=0;while(count++<65536&&(cur=in.read())!=-1){if(prev==0xFF&&cur==0xD8){lastProbe="HTTP 81 /stream JPEG";return true;}prev=cur;}
            }
        }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
        lastProbe="MJPEG frame unavailable at "+host+":81";return false;
    }

    public boolean flash(String mode){String m=normalizeMode(mode);return host!=null&&!host.isEmpty()&&probe(80,"/flash?mode="+m,true);}

    public static String normalizeMode(String mode){if(mode==null)return"off";String m=mode.trim().toLowerCase();return"on".equals(m)||"auto".equals(m)?m:"off";}

    private String getText(int port,String path,int timeout)throws Exception{
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+(port==80?"":":"+port)+path).openConnection();c.setConnectTimeout(timeout);c.setReadTimeout(timeout);c.setUseCaches(false);c.setRequestMethod("GET");c.setRequestProperty("Connection","close");int code=c.getResponseCode();if(code<200||code>=300)return null;
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n,total=0;while(total<8192&&(n=in.read(b,0,Math.min(b.length,8192-total)))>0){out.write(b,0,n);total+=n;}return out.toString(StandardCharsets.UTF_8.name());}
        }finally{if(c!=null)c.disconnect();}
    }

    private boolean probe(int port,String path,boolean drain){
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+(port==80?"":":"+port)+path).openConnection();c.setConnectTimeout(1000);c.setReadTimeout(1200);c.setUseCaches(false);c.setRequestMethod("GET");c.setRequestProperty("Connection","close");int code=c.getResponseCode();
            if(drain){InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();if(in!=null)try{byte[] b=new byte[512];int total=0,n;while(total<2048&&(n=in.read(b))>0)total+=n;}finally{in.close();}}
            return code>=200&&code<400;
        }catch(Exception ignored){return false;}finally{if(c!=null)c.disconnect();}
    }
}
