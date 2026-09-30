package com.servoc.app.camera;

import android.content.Context;
import android.net.Uri;

import java.io.File;

/** Compatibility facade: recording follows the same app-side effects shown to the operator. */
public final class MjpegRecorder {
    public static final class StopResult {
        public final File video,index;public final Uri galleryUri;public final String displayName;public final long frames,dropped;public final String error;
        StopResult(File video,File index,Uri galleryUri,String displayName,long frames,long dropped,String error){this.video=video;this.index=index;this.galleryUri=galleryUri;this.displayName=displayName;this.frames=frames;this.dropped=dropped;this.error=error;}
        public boolean clean(){return error==null||error.isEmpty();}
    }

    private final SurfaceRecorderCore delegate;
    private File currentVideo,currentIndex;private StopResult last;

    public MjpegRecorder(Context context){delegate=new SurfaceRecorderCore(context);}
    public synchronized File start(String baseName)throws Exception{
        currentVideo=delegate.start(baseName);String n=currentVideo.getName();currentIndex=new File(currentVideo.getParentFile(),n.substring(0,Math.max(0,n.length()-4))+"_frames.csv");last=null;return currentVideo;
    }
    /** Ownership of one SharedVideoFrame consumer reference transfers to the recorder. */
    public void onDecodedFrame(SharedVideoFrame frame){delegate.onDecodedFrame(frame);}
    public StopResult stopGracefully(long timeoutMs){SurfaceRecorderCore.StopResult r=delegate.stopGracefully(timeoutMs);last=map(r);return last;}
    public StopResult stopNow(){SurfaceRecorderCore.StopResult r=delegate.stopNow();last=map(r);return last;}
    private static StopResult map(SurfaceRecorderCore.StopResult r){return r==null?new StopResult(null,null,null,"",0,0,"no_result"):new StopResult(r.video,r.index,r.galleryUri,r.displayName,r.frames,r.dropped,r.error);}
    public boolean isRecording(){return delegate.isRecording();}public boolean isClosing(){return delegate.isClosing();}public long frameCount(){return delegate.frameCount();}public long droppedFrames(){return delegate.droppedFrames();}
    public File videoFile(){return currentVideo;}public File indexFile(){return currentIndex;}public String lastError(){return last==null?null:last.error;}public Uri galleryUri(){return last==null?null:last.galleryUri;}
    public void release(){delegate.release();}
}
