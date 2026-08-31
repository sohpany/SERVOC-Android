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
import java.util.concurrent.atomic.AtomicInteger;

/** Lossless recorder for the original ESP32-CAM JPEG frames plus a timestamp/index sidecar. */
public final class MjpegRecorder implements MjpegView.FrameListener {
    private final File directory;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final AtomicInteger queued=new AtomicInteger();
    private BufferedOutputStream video;
    private BufferedWriter index;
    private File videoFile,indexFile;
    private long firstElapsed=-1,offset=0,frames=0,dropped=0;

    public MjpegRecorder(Context context){
        File d=context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(d==null)d=new File(context.getFilesDir(),"video");
        directory=new File(d,"HazRob");directory.mkdirs();
    }

    public synchronized File start(String baseName)throws Exception{
        stopLocked();
        String safe=baseName==null?"HazRob":baseName.replaceAll("[^A-Za-z0-9_.-]","_");
        videoFile=new File(directory,safe+".mjpeg");indexFile=new File(directory,safe+".csv");
        video=new BufferedOutputStream(new FileOutputStream(videoFile),256*1024);
        index=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile),StandardCharsets.UTF_8),64*1024);
        index.write("frame,elapsed_ms,offset,length\n");firstElapsed=-1;offset=0;frames=0;dropped=0;return videoFile;
    }

    public synchronized void stop(){stopLocked();}

    private void stopLocked(){
        try{if(video!=null)video.flush();}catch(Exception ignored){}
        try{if(index!=null)index.flush();}catch(Exception ignored){}
        try{if(video!=null)video.close();}catch(Exception ignored){}
        try{if(index!=null)index.close();}catch(Exception ignored){}
        video=null;index=null;firstElapsed=-1;
    }

    public synchronized boolean isRecording(){return video!=null;}
    public synchronized File videoFile(){return videoFile;}
    public synchronized File indexFile(){return indexFile;}
    public synchronized long frameCount(){return frames;}
    public synchronized long droppedFrames(){return dropped;}

    @Override public void onJpegFrame(byte[] jpeg,long elapsedMs){
        if(jpeg==null||jpeg.length<4||!isRecording())return;
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
        }catch(Exception e){dropped++;stopLocked();}
    }

    public synchronized void release(){stopLocked();io.shutdownNow();}
}
