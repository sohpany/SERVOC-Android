package com.servoc.app.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Buffered MJPEG reader with latest-frame display decoding; recorder/analyzer still receive every complete JPEG. */
public final class MjpegView extends ImageView {
    public interface FrameListener { void onJpegFrame(byte[] jpeg,long elapsedMs); }

    private static final int MAX_JPEG=1024*1024;
    private final ExecutorService streamWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService decodeWorker=Executors.newSingleThreadExecutor();
    private final Object decodeLock=new Object();
    private volatile boolean running=false,released=false;
    private volatile HttpURLConnection connection;
    private volatile long generation=0;
    private volatile FrameListener frameListener;
    private byte[] latestJpeg;
    private boolean decodeBusy=false;

    public MjpegView(Context c){
        super(c);setScaleType(ScaleType.FIT_CENTER);setBackgroundColor(0xFF000000);
    }

    public void setFrameListener(FrameListener listener){frameListener=listener;}

    public synchronized void start(String url){
        if(released)return;
        running=false;generation++;
        HttpURLConnection c=connection;if(c!=null)c.disconnect();connection=null;
        synchronized(decodeLock){latestJpeg=null;}
        running=true;final long token=generation;
        streamWorker.execute(()->stream(url,token));
    }

    public synchronized void stop(){
        running=false;generation++;
        HttpURLConnection c=connection;if(c!=null)c.disconnect();connection=null;
        synchronized(decodeLock){latestJpeg=null;}
    }

    public synchronized void release(){
        if(released)return;stop();released=true;frameListener=null;
        streamWorker.shutdownNow();decodeWorker.shutdownNow();
    }

    private boolean active(long token){return !released&&running&&generation==token;}

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
                synchronized(decodeLock){jpeg=latestJpeg;latestJpeg=null;if(jpeg==null){decodeBusy=false;return;}}
                Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);
                if(bitmap!=null&&active(token))post(()->{if(active(token))setImageBitmap(bitmap);});
            }
        }finally{
            synchronized(decodeLock){decodeBusy=false;if(!active(token))latestJpeg=null;}
        }
    }

    private void stream(String streamUrl,long token){
        byte[] chunk=new byte[8192];
        ByteArrayOutputStream frame=new ByteArrayOutputStream(64*1024);
        while(active(token)){
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(streamUrl).openConnection();connection=c;
                c.setConnectTimeout(1800);c.setReadTimeout(5000);c.setUseCaches(false);c.setRequestMethod("GET");c.connect();
                try(InputStream raw=c.getInputStream();BufferedInputStream in=new BufferedInputStream(raw,32*1024)){
                    boolean inFrame=false;int prev=-1;
                    while(active(token)){
                        int n=in.read(chunk);if(n<0)break;
                        for(int i=0;i<n&&active(token);i++){
                            int cur=chunk[i]&0xFF;
                            if(!inFrame){
                                if(prev==0xFF&&cur==0xD8){frame.reset();frame.write(0xFF);frame.write(0xD8);inFrame=true;}
                            }else{
                                frame.write(cur);
                                if(frame.size()>MAX_JPEG){frame.reset();inFrame=false;}
                                else if(prev==0xFF&&cur==0xD9){
                                    byte[] bytes=frame.toByteArray();long now=SystemClock.elapsedRealtime();
                                    FrameListener listener=frameListener;
                                    if(listener!=null&&active(token))try{listener.onJpegFrame(bytes,now);}catch(Exception ignored){}
                                    if(active(token))queueDecode(bytes,token);
                                    frame.reset();inFrame=false;
                                }
                            }
                            prev=cur;
                        }
                    }
                }finally{c.disconnect();if(connection==c)connection=null;}
            }catch(Exception ignored){
                if(!active(token))return;
                try{Thread.sleep(500);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
            }
        }
    }

    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
