package com.servoc.app.camera;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** ESP32-CAM HTTP compatibility client. Video remains a direct camera-to-phone stream. */
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

    public String[] streamUrls(){
        String h=host;
        return new String[]{"http://"+h+":81/stream","http://"+h+"/stream","http://"+h+":81/","http://"+h+"/mjpeg"};
    }

    /** Returns structured 0.7.1 status where available; null means no valid /status response. */
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

    public boolean flash(String mode){
        String m=normalizeMode(mode);if(host==null||host.isEmpty())return false;
        if(probe(80,"/flash?mode="+m,true))return true;
        if("auto".equals(m))return false;
        boolean on="on".equals(m);int level=on?255:0;String bit=on?"1":"0";
        String[] paths={"/flash?on="+bit,"/led?on="+bit,"/led?state="+bit,"/control?var=led_intensity&val="+level,"/control?var=flash&val="+bit,"/control?var=led&val="+bit};
        for(String p:paths)if(probe(80,p,true))return true;
        return false;
    }

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
