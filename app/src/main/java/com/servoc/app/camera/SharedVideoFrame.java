package com.servoc.app.camera;

import android.graphics.Bitmap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable decoded camera frame shared by the live GPU surface and recorder.
 * The bitmap is never pixel-processed on CPU. It is recycled only after all consumers upload it.
 */
public final class SharedVideoFrame {
    private final Bitmap bitmap;
    public final long elapsedMs;
    private final AtomicInteger refs;

    public SharedVideoFrame(Bitmap bitmap,long elapsedMs,int consumers){
        if(bitmap==null)throw new IllegalArgumentException("bitmap required");
        if(consumers<1)throw new IllegalArgumentException("consumer count");
        this.bitmap=bitmap;this.elapsedMs=elapsedMs;this.refs=new AtomicInteger(consumers);
    }

    public Bitmap bitmap(){return bitmap;}
    public int width(){return bitmap.getWidth();}
    public int height(){return bitmap.getHeight();}
    public int remainingConsumers(){return Math.max(0,refs.get());}

    /** Adds one consumer before the current owner releases the frame. */
    public SharedVideoFrame retain(){
        while(true){
            int n=refs.get();
            if(n<=0)throw new IllegalStateException("SharedVideoFrame already released");
            if(refs.compareAndSet(n,n+1))return this;
        }
    }

    public void release(){
        int n=refs.decrementAndGet();
        if(n==0){if(!bitmap.isRecycled())bitmap.recycle();}
        else if(n<0)throw new IllegalStateException("SharedVideoFrame released too many times");
    }
}
