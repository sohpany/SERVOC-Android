package com.servoc.app.camera;

import android.content.Context;
import android.os.Environment;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Lossless recorder for original ESP32-CAM JPEG frames plus exact timestamp/index sidecar. */
public final class MjpegRecorder implements MjpegView.FrameListener {
    public static final class StopResult {
        public final File video,index;public final long frames,dropped;public final String error;
        StopResult(File video,File index,long frames,long dropped,String error){this.video=video;this.index=index;this.frames=frames;this.dropped=dropped;this.error=error;}
        public boolean clean(){return error==null||error.isEmpty();}
    }

    private final File directory;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final AtomicInteger queued=new AtomicInteger();
    private BufferedOutputStream video;
    private BufferedWriter index;
    private File videoFile,indexFile;
    private long firstElapsed=-1,offset=0,frames=0,dropped=0;
    private boolean accepting=false,closing=false;
    private String lastError;

    public MjpegRecorder(Context context){
        File d=context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(d==null)d=new File(context.getFilesDir(),"video");
        directory=new File(d,"HazRob");directory.mkdirs();
    }

    public synchronized File start(String baseName)throws Exception{
        if(closing||queued.get()>0)throw new IllegalStateException("previous recording is still closing");
        closeNowLocked();
        String safe=baseName==null?"HazRob":baseName.replaceAll("[^A-Za-z0-9_.-]","_");
        videoFile=new File(directory,safe+".mjpeg");indexFile=new File(directory,safe+".csv");
        video=new BufferedOutputStream(new FileOutputStream(videoFile),256*1024);
        index=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile),StandardCharsets.UTF_8),64*1024);
        index.write("frame,elapsed_ms,offset,length\n");firstElapsed=-1;offset=0;frames=0;dropped=0;lastError=null;accepting=true;return videoFile;
    }

    /** Stops accepting new frames, drains already accepted frames, then closes the files. Call off the UI thread. */
    public StopResult stopGracefully(long timeoutMs){
        synchronized(this){if(video==null)return snapshotLocked();accepting=false;if(closing)return snapshotLocked();closing=true;}
        Future<StopResult> barrier=io.submit(()->{synchronized(this){StopResult r=closeNowLocked();closing=false;return r;}});
        try{return barrier.get(Math.max(250,timeoutMs),TimeUnit.MILLISECONDS);}
        catch(Exception e){barrier.cancel(false);synchronized(this){if(lastError==null)lastError="close_timeout:"+e.getClass().getSimpleName();StopResult r=closeNowLocked();closing=false;return r;}}
    }

    /** Immediate close for lifecycle teardown only; queued tail frames may be discarded. */
    public synchronized StopResult stopNow(){accepting=false;closing=false;return closeNowLocked();}

    private StopResult closeNowLocked(){
        try{if(video!=null)video.flush();}catch(Exception e){remember(e);}
        try{if(index!=null)index.flush();}catch(Exception e){remember(e);}
        try{if(video!=null)video.close();}catch(Exception e){remember(e);}
        try{if(index!=null)index.close();}catch(Exception e){remember(e);}
        video=null;index=null;firstElapsed=-1;return snapshotLocked();
    }

    public synchronized boolean isRecording(){return accepting&&video!=null;}
    public synchronized boolean isClosing(){return closing;}
    public synchronized File videoFile(){return videoFile;}
    public synchronized File indexFile(){return indexFile;}
    public synchronized long frameCount(){return frames;}
    public synchronized long droppedFrames(){return dropped;}
    public synchronized String lastError(){return lastError;}

    @Override public void onJpegFrame(byte[] jpeg,long elapsedMs){
        if(jpeg==null||jpeg.length<4)return;
        synchronized(this){if(!accepting||video==null)return;}
        if(queued.incrementAndGet()>12){queued.decrementAndGet();synchronized(this){dropped++;}return;}
        io.execute(()->{try{writeFrame(jpeg,elapsedMs);}finally{queued.decrementAndGet();}});
    }

    private synchronized void writeFrame(byte[] jpeg,long elapsedMs){
        if(video==null||index==null)return;
        try{
            if(firstElapsed<0)firstElapsed=elapsedMs;
            long t=Math.max(0,elapsedMs-firstElapsed),frame=frames++;
            video.write(jpeg);
            index.write(Long.toString(frame));index.write(',');index.write(Long.toString(t));index.write(',');index.write(Long.toString(offset));index.write(',');index.write(Integer.toString(jpeg.length));index.write('\n');
            offset+=jpeg.length;
            if((frame&31)==31){video.flush();index.flush();}
        }catch(Exception e){dropped++;accepting=false;remember(e);closeNowLocked();}
    }

    private void remember(Exception e){if(lastError==null)lastError=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());}
    private StopResult snapshotLocked(){return new StopResult(videoFile,indexFile,frames,dropped,lastError);}

    public synchronized void release(){accepting=false;closing=false;closeNowLocked();io.shutdownNow();}
}
