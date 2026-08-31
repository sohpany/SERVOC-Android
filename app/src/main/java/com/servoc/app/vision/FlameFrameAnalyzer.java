package com.servoc.app.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.servoc.app.camera.MjpegView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-latency live-frame adapter for the explainable HazRob flame-candidate model.
 * Frames are downsampled and analyzed off the MJPEG/network thread. If analysis is
 * still busy, the next frame is skipped rather than delaying robot video/control.
 */
public final class FlameFrameAnalyzer implements MjpegView.FrameListener {
    public interface Listener{void onFlameEvidence(Result result);}

    public static final class Result {
        public final long elapsedMs;
        public final FlameColorModel.Result frame;
        public final FlameEvidenceTracker.Snapshot temporal;
        /** Original camera JPEG for auditable event snapshots; never modified by the analyzer. */
        public final byte[] jpeg;
        Result(long elapsedMs,FlameColorModel.Result frame,FlameEvidenceTracker.Snapshot temporal,byte[] jpeg){this.elapsedMs=elapsedMs;this.frame=frame;this.temporal=temporal;this.jpeg=jpeg;}
    }

    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private final FlameEvidenceTracker tracker=new FlameEvidenceTracker(.62,.34,.28,4,750,1200);
    private final Listener listener;
    private volatile boolean released=false;
    private volatile long lastAcceptedMs=-1;
    private static final long MIN_INTERVAL_MS=180;

    public FlameFrameAnalyzer(Listener listener){this.listener=listener;}

    @Override public void onJpegFrame(byte[] jpeg,long elapsedMs){
        if(released||jpeg==null||jpeg.length<4)return;
        long prev=lastAcceptedMs;if(prev>=0&&elapsedMs-prev<MIN_INTERVAL_MS)return;
        if(!busy.compareAndSet(false,true))return;
        lastAcceptedMs=elapsedMs;
        worker.execute(()->{try{analyze(jpeg,elapsedMs);}finally{busy.set(false);}});
    }

    private void analyze(byte[] jpeg,long elapsedMs){
        if(released)return;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inSampleSize=4;
        Bitmap b=null;
        try{
            b=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options);
            if(b==null){emit(new Result(elapsedMs,FlameColorModel.analyze(null,0,0),tracker.missing(elapsedMs),null));return;}
            int w=b.getWidth(),h=b.getHeight();int[] pixels=new int[w*h];b.getPixels(pixels,0,w,0,0,w,h);
            FlameColorModel.Result frame=FlameColorModel.analyze(pixels,w,h);
            FlameEvidenceTracker.Snapshot temporal=tracker.update(frame.score,elapsedMs);
            emit(new Result(elapsedMs,frame,temporal,jpeg));
        }catch(Exception ignored){emit(new Result(elapsedMs,FlameColorModel.analyze(null,0,0),tracker.missing(elapsedMs),null));}
        finally{if(b!=null)b.recycle();}
    }

    private void emit(Result result){if(!released&&listener!=null)try{listener.onFlameEvidence(result);}catch(Exception ignored){}}
    public void reset(){tracker.reset();lastAcceptedMs=-1;}
    public void release(){released=true;tracker.reset();worker.shutdownNow();}
}
