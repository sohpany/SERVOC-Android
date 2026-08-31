package com.servoc.app.camera;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** ESP32-CAM HTTP control client. Stream remains independent from control traffic. */
public final class CameraApi {
    private volatile String host;
    public CameraApi(String host){setHost(host);}
    public void setHost(String h){
        if(h==null)h=""; h=h.trim().replace("http://","").replace("https://","");
        while(h.endsWith("/"))h=h.substring(0,h.length()-1); host=h;
    }
    public String host(){return host;}

    public boolean ping(){
        if(get("/status"))return true;
        return get("/");
    }

    /**
     * Flash modes: off / auto / on.
     * The first endpoint is the exact contract used by the historical SERVOC camera UI.
     */
    public boolean flash(String mode){
        String m=normalizeMode(mode);
        if(get("/flash?mode="+m))return true;
        if("auto".equals(m))return false;
        boolean on="on".equals(m);int level=on?255:0;
        if(get("/control?var=led_intensity&val="+level))return true;
        if(get("/control?var=flash&val="+(on?1:0)))return true;
        return get("/flash?on="+(on?1:0));
    }

    public static String normalizeMode(String mode){
        if(mode==null)return "off";String m=mode.trim().toLowerCase();
        return "on".equals(m)||"auto".equals(m)?m:"off";
    }

    private boolean get(String path){
        if(host==null||host.isEmpty())return false;
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+path).openConnection();
            c.setConnectTimeout(900);c.setReadTimeout(900);c.setUseCaches(false);c.setRequestMethod("GET");
            int code=c.getResponseCode();
            InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            if(in!=null)try{byte[] b=new byte[256];while(in.read(b)!=-1){} }finally{in.close();}
            return code>=200&&code<300;
        }catch(Exception ignored){return false;}
        finally{if(c!=null)c.disconnect();}
    }
}
