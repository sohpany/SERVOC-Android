package com.servoc.app.camera;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Small ESP32-CAM HTTP control client. Stream remains independent. */
public final class CameraApi {
    private volatile String host;
    public CameraApi(String host){setHost(host);}
    public void setHost(String h){
        if(h==null)h=""; h=h.trim().replace("http://","").replace("https://","");
        while(h.endsWith("/"))h=h.substring(0,h.length()-1); host=h;
    }
    public String host(){return host;}

    public boolean flash(boolean on){
        int level=on?255:0;
        // Arduino ESP32 CameraWebServer commonly exposes led_intensity through /control.
        if(get("/control?var=led_intensity&val="+level))return true;
        // Fallback for lightweight custom firmware used by some ESP32-CAM projects.
        if(get("/flash?on="+(on?1:0)))return true;
        return false;
    }

    private boolean get(String path){
        if(host==null||host.isEmpty())return false;
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("http://"+host+path).openConnection();
            c.setConnectTimeout(700);c.setReadTimeout(700);c.setUseCaches(false);c.setRequestMethod("GET");
            int code=c.getResponseCode();
            InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            if(in!=null)try{while(in.read()!=-1){} }finally{in.close();}
            return code>=200&&code<300;
        }catch(Exception ignored){return false;}
        finally{if(c!=null)c.disconnect();}
    }
}
