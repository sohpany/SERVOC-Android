package com.servoc.app.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;

import com.servoc.app.communication.RemoteServiceProfile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** End-to-end readiness probe for the authenticated Render JPEG/WSS path. */
public final class RemoteVideoReadiness {
    private static final long WAKE_TIMEOUT_MS=75000L;
    private static final long RETRY_MS=3000L;
    private static final long FIRST_FRAME_TIMEOUT_MS=9000L;

    public static final class Result{
        public final boolean service,publisher,wss,ready;
        public final String detail;
        Result(boolean service,boolean publisher,boolean wss,String detail){this.service=service;this.publisher=publisher;this.wss=wss;this.ready=service&&publisher&&wss;this.detail=detail==null?"":detail;}
    }

    /** Lightweight status authority for UI dashboards. It does not open a WSS viewer or decode a frame. */
    public static Result probeStatus(SharedPreferences prefs){
        RemoteServiceProfile.ensureDefaults(prefs);
        String token=new RemoteVideoCredentialStore(prefs).load();
        if(token.isEmpty())return new Result(false,false,false,"VIEW_TOKEN غير مضبوط");
        try{
            JSONObject status=getJson(RemoteServiceProfile.VIDEO_STATUS_HTTPS,token);
            boolean service=status.optBoolean("ok",false)&&"hazrob-video-relay".equals(status.optString("service",""))&&status.optInt("protocol",0)>=1;
            boolean publisher=service&&status.optBoolean("publisher_online",false);
            return new Result(service,publisher,false,publisher?"ESP32-CAM publisher online":service?"ESP32-CAM publisher offline":"video service not ready");
        }catch(Exception e){
            String m=String.valueOf(e.getMessage());
            if(m.contains("HTTP 401"))return new Result(false,false,false,"VIEW_TOKEN غير صحيح");
            return new Result(false,false,false,e.getClass().getSimpleName());
        }
    }

    public static Result probe(Context context,SharedPreferences prefs){
        RemoteServiceProfile.ensureDefaults(prefs);
        String viewerUrl=prefs.getString("remote_video_url",RemoteServiceProfile.VIDEO_VIEW_WSS).trim();
        String token=new RemoteVideoCredentialStore(prefs).load();
        if(token.isEmpty())return new Result(false,false,false,"VIEW_TOKEN غير مضبوط");
        if(viewerUrl.isEmpty())viewerUrl=RemoteServiceProfile.VIDEO_VIEW_WSS;

        boolean service=false,publisher=false;
        String last="";
        long deadline=System.currentTimeMillis()+WAKE_TIMEOUT_MS;
        do{
            try{
                JSONObject status=getJson(RemoteServiceProfile.VIDEO_STATUS_HTTPS,token);
                service=status.optBoolean("ok",false)&&"hazrob-video-relay".equals(status.optString("service",""))&&status.optInt("protocol",0)>=1;
                publisher=service&&status.optBoolean("publisher_online",false);
                if(service&&publisher)break;
                last=service?"بانتظار اتصال ESP32-CAM بخادم الفيديو":"بانتظار تشغيل خدمة الفيديو";
            }catch(Exception e){
                String m=String.valueOf(e.getMessage());
                if(m.contains("HTTP 401"))return new Result(false,false,false,"VIEW_TOKEN غير صحيح");
                last="تشغيل خدمة الفيديو...";
            }
            if(System.currentTimeMillis()>=deadline)break;
            try{Thread.sleep(RETRY_MS);}catch(InterruptedException e){Thread.currentThread().interrupt();return new Result(service,publisher,false,"تم إيقاف فحص الفيديو");}
        }while(System.currentTimeMillis()<deadline);

        if(!service)return new Result(false,false,false,last.isEmpty()?"خدمة الفيديو غير جاهزة":last);
        if(!publisher)return new Result(true,false,false,last.isEmpty()?"ESP32-CAM غير متصل بخادم الفيديو":last);

        Exception lastWs=null;
        for(int attempt=0;attempt<3;attempt++){
            RemoteJpegWebSocketClient client=new RemoteJpegWebSocketClient();
            final boolean[] gotDecodableJpeg={false};
            final long firstFrameDeadline=System.currentTimeMillis()+FIRST_FRAME_TIMEOUT_MS;
            try{
                client.stream(viewerUrl,token,new RemoteJpegWebSocketClient.Sink(){
                    @Override public boolean active(){return !gotDecodableJpeg[0]&&System.currentTimeMillis()<firstFrameDeadline;}
                    @Override public void frame(byte[] jpeg){
                        if(jpeg==null||jpeg.length<4)return;
                        if((jpeg[0]&0xFF)!=0xFF||(jpeg[1]&0xFF)!=0xD8||(jpeg[jpeg.length-2]&0xFF)!=0xFF||(jpeg[jpeg.length-1]&0xFF)!=0xD9)return;
                        // Readiness means the same payload the live view receives is actually
                        // decodable by Android, not merely that the WebSocket handshake succeeded.
                        gotDecodableJpeg[0]=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length)!=null;
                    }
                    @Override public void status(String state,String detail){}
                });
                if(gotDecodableJpeg[0])return new Result(true,true,true,"remote video ready • first JPEG decoded");
                lastWs=new IOException("no decodable JPEG frame");
            }catch(Exception e){lastWs=e;}
            finally{client.close();}
            try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
        return new Result(true,true,false,"WSS/JPEG: "+(lastWs==null?"not ready":lastWs.getClass().getSimpleName()));
    }

    private static JSONObject getJson(String url,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setUseCaches(false);c.setRequestMethod("GET");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
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
