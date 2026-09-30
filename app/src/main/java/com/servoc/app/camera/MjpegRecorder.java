package com.servoc.app.camera;

import android.content.Context;
import android.net.Uri;

import java.io.File;

/** Compatibility facade: recording now follows the same app-side video effects the operator sees. */
public final class MjpegRecorder implements MjpegView.FrameListener {
    public static final class StopResult {
        public final File video,index;public final Uri galleryUri;public final String displayName;public final long frames,dropped;public final String error;
        StopResult(File video,File index,Uri galleryUri,String displayName,long frames,long dropped,String error){this.video=video;this.index=index;this.galleryUri=galleryUri;this.displayName=displayName;this.frames=frames;this.dropped=dropped;this.error=error;}
        public boolean clean(){return error==null||error.isEmpty();}
    }

    private final EffectVideoRecorder delegate;
    private File currentVideo,currentIndex;private StopResult last;

    public MjpegRecorder(Context context){delegate=new EffectVideoRecorder(context);}
    public synchronized File start(String baseName)throws Exception{
        syncProfile();currentVideo=delegate.start(baseName);String n=currentVideo.getName();currentIndex=new File(currentVideo.getParentFile(),n.substring(0,Math.max(0,n.length()-4))+"_frames.csv");last=null;return currentVideo;
    }
    private void syncProfile(){delegate.setVideoEffects(VideoEffectState.display(),VideoEffectState.enhancement());delegate.setViewport(VideoEffectState.viewportW(),VideoEffectState.viewportH());}
    @Override public void onJpegFrame(byte[] jpeg,long elapsedMs){syncProfile();delegate.onJpegFrame(jpeg,elapsedMs);}
    public StopResult stopGracefully(long timeoutMs){EffectVideoRecorder.StopResult r=delegate.stopGracefully(timeoutMs);last=map(r);return last;}
    public StopResult stopNow(){EffectVideoRecorder.StopResult r=delegate.stopNow();last=map(r);return last;}
    private static StopResult map(EffectVideoRecorder.StopResult r){return r==null?new StopResult(null,null,null,"",0,0,"no_result"):new StopResult(r.video,r.index,r.galleryUri,r.displayName,r.frames,r.dropped,r.error);}
    public boolean isRecording(){return delegate.isRecording();}public boolean isClosing(){return delegate.isClosing();}public long frameCount(){return delegate.frameCount();}public long droppedFrames(){return delegate.droppedFrames();}
    public File videoFile(){return currentVideo;}public File indexFile(){return currentIndex;}public String lastError(){return last==null?null:last.error;}public Uri galleryUri(){return last==null?null:last.galleryUri;}
    public void release(){delegate.release();}
}
