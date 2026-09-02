package com.servoc.app.camera;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    /** Candidate MJPEG locations used by old SERVOC and new HazRob camera firmware. */
    public String[] streamUrls(){
        String h=host;
        return new String[]{
                "http://"+h+":81/stream",
                "http://"+h+"/stream",
                "http://"+h+":81/",
                "http://"+h+"/mjpeg"
        };
    }

    public boolean ping(){
        if(host==null||host.isEmpty()){lastProbe="camera host empty";return false;}
        if(probe(80,"/status",true)){lastProbe="HTTP 80 /status";return true;}
        if(probe(80,"/",true)){lastProbe="HTTP 80 /";return true;}
        if(probe(81,"/stream",false)){lastProbe="HTTP 81 /stream";return true;}
        lastProbe="unreachable at "+host+" (80/81)";
        return false;
    }

    /** off/on are required. auto succeeds only when firmware explicitly implements it. */
    public boolean flash(String mode){
        String m=normalizeMode(mode);
        if(host==null||host.isEmpty())return false;

        if(probe(80,"/flash?mode="+m,true))return true;
        if("auto".equals(m))return false;

        boolean on="on".equals(m);int level=on?255:0;
        String bit=on?"1":"0";
        String[] paths={
                "/flash?on="+bit,
                "/led?on="+bit,
                "/led?state="+bit,
                "/control?var=led_intensity&val="+level,
                "/control?var=flash&val="+bit,
                "/control?var=led&val="+bit
        };
        for(String p:paths)if(probe(80,p,true))return true;
        return false;
    }

    public static String normalizeMode(String mode){
        if(mode==null)return "off";
        String m=mode.trim().toLowerCase();
        return "on".equals(m)||"auto".equals(m)?m:"off";
    }

    private boolean probe(int port,String path,boolean drain){
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+(port==80?"":":"+port)+path).openConnection();
            c.setConnectTimeout(1000);c.setReadTimeout(1200);c.setUseCaches(false);c.setRequestMethod("GET");
            c.setRequestProperty("Connection","close");
            int code=c.getResponseCode();
            if(drain){
                InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                if(in!=null)try{byte[] b=new byte[512];int total=0,n;while(total<2048&&(n=in.read(b))>0)total+=n;}finally{in.close();}
            }
            return code>=200&&code<400;
        }catch(Exception ignored){return false;}
        finally{if(c!=null)c.disconnect();}
    }
}
