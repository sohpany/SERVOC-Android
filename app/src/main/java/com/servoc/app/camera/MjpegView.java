package com.servoc.app.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compatibility-first ESP32-CAM MJPEG reader.
 *
 * The HTTP/frame acquisition path intentionally follows the proven 0.6.x reader:
 * getInputStream() directly, byte-level JPEG SOI/EOI detection and no assumptions about
 * multipart headers. Bitmap decoding remains on a second worker so slow Android decode,
 * recording or flame analysis cannot stall the camera socket.
 */
public final class MjpegView extends ImageView {
    public interface FrameListener { void onJpegFrame(byte[] jpeg,long elapsedMs); }
    public interface StatusListener { void onStatus(String state,String detail); }

    private static final int MAX_JPEG=1024*1024;
    private final ExecutorService streamWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService decodeWorker=Executors.newSingleThreadExecutor();
    private final Object decodeLock=new Object();

    private volatile boolean running=false,released=false;
    private volatile HttpURLConnection connection;
    private volatile long generation=0;
    private volatile FrameListener frameListener;
    private volatile StatusListener statusListener;
    private byte[] latestJpeg;
    private boolean decodeBusy=false;

    public MjpegView(Context c){
        super(c);
        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(0xFF000000);
    }

    public void setFrameListener(FrameListener listener){frameListener=listener;}
    public void setStatusListener(StatusListener listener){statusListener=listener;}
    public void start(String url){start(new String[]{url});}

    public synchronized void start(String[] urls){
        if(released)return;
        running=false;
        generation++;
        HttpURLConnection c=connection;
        if(c!=null)c.disconnect();
        connection=null;
        synchronized(decodeLock){latestJpeg=null;}
        if(urls==null||urls.length==0)return;
        running=true;
        final long token=generation;
        final String[] copy=urls.clone();
        status("connecting",copy[0]);
        streamWorker.execute(()->streamLoop(copy,token));
    }

    public synchronized void stop(){
        running=false;
        generation++;
        HttpURLConnection c=connection;
        if(c!=null)c.disconnect();
        connection=null;
        synchronized(decodeLock){latestJpeg=null;}
        status("stopped","");
    }

    public synchronized void release(){
        if(released)return;
        stop();
        released=true;
        frameListener=null;
        statusListener=null;
        streamWorker.shutdownNow();
        decodeWorker.shutdownNow();
    }

    private boolean active(long token){return !released&&running&&generation==token;}

    private void status(String state,String detail){
        StatusListener s=statusListener;
        if(s==null)return;
        post(()->{
            StatusListener x=statusListener;
            if(x!=null)x.onStatus(state,detail==null?"":detail);
        });
    }

    private void queueDecode(byte[] jpeg,long token){
        synchronized(decodeLock){
            latestJpeg=jpeg;
            if(decodeBusy)return;
            decodeBusy=true;
        }
        decodeWorker.execute(()->decodeLoop(token));
    }

    private void decodeLoop(long token){
        try{
            while(active(token)){
                byte[] jpeg;
                synchronized(decodeLock){
                    jpeg=latestJpeg;
                    latestJpeg=null;
                    if(jpeg==null){decodeBusy=false;return;}
                }
                Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);
                if(bitmap!=null&&active(token))post(()->{if(active(token))setImageBitmap(bitmap);});
            }
        }finally{
            synchronized(decodeLock){
                decodeBusy=false;
                if(!active(token))latestJpeg=null;
            }
        }
    }

    private void streamLoop(String[] urls,long token){
        int cycle=0;
        while(active(token)){
            boolean gotFrame=false;
            String lastError="";
            for(String u:urls){
                if(!active(token))return;
                status(cycle==0?"connecting":"retrying",u);
                try{
                    gotFrame=streamOnce(u,token);
                    if(gotFrame&&active(token))break;
                }catch(Exception e){
                    lastError=e.getClass().getSimpleName();
                }
            }
            if(!active(token))return;
            cycle++;
            status("retrying",gotFrame?"انقطع البث":("تعذر البث"+(lastError.isEmpty()?"":" • "+lastError)));
            try{Thread.sleep(gotFrame?450:700);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
    }

    private boolean streamOnce(String streamUrl,long token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(streamUrl).openConnection();
        connection=c;
        c.setConnectTimeout(2200);
        c.setReadTimeout(5500);
        c.setUseCaches(false);
        c.setInstanceFollowRedirects(true);
        c.setRequestMethod("GET");
        c.connect();

        boolean got=false,announced=false;
        long fpsAt=SystemClock.elapsedRealtime();
        int fpsFrames=0;

        try(InputStream in=c.getInputStream()){
            int prev=-1,cur;
            ByteArrayOutputStream frame=null;
            while(active(token)&&(cur=in.read())!=-1){
                if(frame==null){
                    if(prev==0xFF&&cur==0xD8){
                        frame=new ByteArrayOutputStream(64*1024);
                        frame.write(0xFF);
                        frame.write(0xD8);
                    }
                }else{
                    frame.write(cur);
                    if(frame.size()>MAX_JPEG){
                        frame=null;
                    }else if(prev==0xFF&&cur==0xD9){
                        byte[] bytes=frame.toByteArray();
                        long now=SystemClock.elapsedRealtime();
                        got=true;
                        fpsFrames++;

                        if(!announced){
                            announced=true;
                            fpsAt=now;
                            fpsFrames=0;
                            status("streaming",streamUrl+" • بدء البث");
                        }else if(now-fpsAt>=2000){
                            double fps=fpsFrames*1000.0/(now-fpsAt);
                            status("streaming",streamUrl+String.format(Locale.US," • %.2f FPS",fps));
                            fpsAt=now;
                            fpsFrames=0;
                        }

                        FrameListener listener=frameListener;
                        if(listener!=null&&active(token)){
                            try{listener.onJpegFrame(bytes,now);}catch(Exception ignored){}
                        }
                        if(active(token))queueDecode(bytes,token);
                        frame=null;
                    }
                }
                prev=cur;
            }
        }finally{
            c.disconnect();
            if(connection==c)connection=null;
        }
        return got;
    }

    @Override protected void onDetachedFromWindow(){
        stop();
        super.onDetachedFromWindow();
    }
}
