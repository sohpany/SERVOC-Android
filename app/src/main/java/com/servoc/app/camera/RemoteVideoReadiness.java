package com.servoc.app.camera;

import android.content.Context;
import android.content.SharedPreferences;

import com.servoc.app.communication.RemoteServiceProfile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** End-to-end readiness probe for the authenticated Render JPEG/WSS path. */
public final class RemoteVideoReadiness {
    public static final class Result{
        public final boolean service,publisher,wss,ready;
        public final String detail;
        Result(boolean service,boolean publisher,boolean wss,String detail){this.service=service;this.publisher=publisher;this.wss=wss;this.ready=service&&publisher&&wss;this.detail=detail==null?"":detail;}
    }

    public static Result probe(Context context,SharedPreferences prefs){
        RemoteServiceProfile.ensureDefaults(prefs);
        String viewerUrl=prefs.getString("remote_video_url",RemoteServiceProfile.VIDEO_VIEW_WSS).trim();
        String token=new RemoteVideoCredentialStore(prefs).load();
        if(token.isEmpty())return new Result(false,false,false,"VIEW_TOKEN غير مضبوط");
        if(viewerUrl.isEmpty())viewerUrl=RemoteServiceProfile.VIDEO_VIEW_WSS;

        boolean service=false,publisher=false,wss=false;
        try{
            JSONObject status=getJson(RemoteServiceProfile.VIDEO_STATUS_HTTPS,token);
            service=status.optBoolean("ok",false)&&"hazrob-video-relay".equals(status.optString("service",""))&&status.optInt("protocol",0)>=1;
            publisher=service&&status.optBoolean("publisher_online",false);
            if(!service)return new Result(false,false,false,"خدمة الفيديو غير جاهزة");
            if(!publisher)return new Result(true,false,false,"ESP32-CAM غير متصل بخادم الفيديو");

            RemoteJpegWebSocketClient client=new RemoteJpegWebSocketClient();
            try{
                client.stream(viewerUrl,token,new RemoteJpegWebSocketClient.Sink(){
                    @Override public boolean active(){return false;}
                    @Override public void frame(byte[] jpeg){}
                    @Override public void status(String state,String detail){}
                });
                wss=true;
            }finally{client.close();}
            return new Result(service,publisher,wss,wss?"remote video ready":"WSS غير جاهز");
        }catch(Exception e){return new Result(service,publisher,wss,"Video: "+e.getClass().getSimpleName());}
    }

    private static JSONObject getJson(String url,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setUseCaches(false);c.setRequestMethod("GET");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
        try{
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in,16384);
            if(code!=200)throw new IllegalStateException("HTTP "+code);
            return new JSONObject(body);
        }finally{c.disconnect();}
    }

    private static String read(InputStream in,int max)throws Exception{
        if(in==null)return"";try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int total=0,n;while((n=x.read(b))!=-1){total+=n;if(total>max)throw new IllegalStateException("response too large");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8.name());}
    }

    private RemoteVideoReadiness(){}
}
