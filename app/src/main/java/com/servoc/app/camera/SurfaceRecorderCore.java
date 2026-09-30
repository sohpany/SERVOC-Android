package com.servoc.app.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * H.264 recorder fed by the already-decoded SharedVideoFrame.
 * The same GLSL contract as the live surface is applied on the encoder input Surface.
 */
final class SurfaceRecorderCore {
    private static final String MIME="video/mp4";
    private static final int FPS=15;

    static final class StopResult {
        final File video,index;final Uri galleryUri;final String displayName;final long frames,dropped;final String error;
        StopResult(File v,File i,Uri u,String n,long f,long d,String e){video=v;index=i;galleryUri=u;displayName=n;frames=f;dropped=d;error=e;}
    }

    private final Context context;
    private final File directory;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final AtomicInteger queued=new AtomicInteger();
    private BufferedWriter index;
    private File videoFile,indexFile;
    private String displayName;
    private MediaCodec codec;
    private MediaMuxer muxer;
    private EncoderSurface encoderSurface;
    private int track=-1;
    private boolean muxerStarted=false,accepting=false,closing=false;
    private long firstElapsed=-1,lastPts=-1,frames=0,dropped=0;
    private String lastError;
    private Uri galleryUri;
    private StopResult lastResult;

    SurfaceRecorderCore(Context c){
        context=c.getApplicationContext();
        File d=c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(d==null)d=new File(c.getFilesDir(),"video");
        directory=new File(d,"HazRob");directory.mkdirs();
    }

    synchronized File start(String baseName)throws Exception{
        if(closing||queued.get()>0||accepting)throw new IllegalStateException("previous recording active");
        releaseCodec(false);
        String safe=baseName==null?"HazRob":baseName.replaceAll("[^A-Za-z0-9_.-]","_");
        if(safe.isEmpty())safe="HazRob";if(!safe.toLowerCase(Locale.US).endsWith(".mp4"))safe+=".mp4";
        displayName=safe;videoFile=new File(directory,safe);indexFile=new File(directory,safe.substring(0,safe.length()-4)+"_frames.csv");
        if(videoFile.exists())videoFile.delete();
        index=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile),StandardCharsets.UTF_8),64*1024);
        index.write("frame,elapsed_ms,presentation_us,display_mode,enhancement,auto_gain\n");
        firstElapsed=-1;lastPts=-1;frames=0;dropped=0;lastError=null;galleryUri=null;lastResult=null;track=-1;muxerStarted=false;accepting=true;closing=false;
        return videoFile;
    }

    /** Takes ownership of one SharedVideoFrame consumer reference. */
    void onDecodedFrame(SharedVideoFrame frame){
        if(frame==null)return;
        synchronized(this){if(!accepting){frame.release();return;}}
        if(queued.incrementAndGet()>2){queued.decrementAndGet();synchronized(this){dropped++;}frame.release();return;}
        final VideoEffectProfile.DisplayMode dm=VideoEffectState.display();
        final VideoEffectProfile.Enhancement en=VideoEffectState.enhancement();
        final float gain=VideoEffectState.autoGain();
        io.execute(()->{try{writeFrame(frame,dm,en,gain);}finally{queued.decrementAndGet();frame.release();}});
    }

    private synchronized void writeFrame(SharedVideoFrame frame,VideoEffectProfile.DisplayMode dm,VideoEffectProfile.Enhancement en,float gain){
        if(!accepting&&!closing)return;
        try{
            Bitmap b=frame.bitmap();
            if(codec==null)initEncoder(b.getWidth(),b.getHeight());
            if(firstElapsed<0)firstElapsed=frame.elapsedMs;
            long pts=Math.max(lastPts+1,Math.max(0,frame.elapsedMs-firstElapsed)*1000L);lastPts=pts;
            encoderSurface.draw(b,pts*1000L,dm,en,gain);drain(false);
            if(index!=null){index.write(frames+","+Math.max(0,frame.elapsedMs-firstElapsed)+","+pts+","+dm.key+","+en.key+","+String.format(Locale.US,"%.3f",gain)+"\n");if((frames&31)==31)index.flush();}
            frames++;
        }catch(Exception e){dropped++;accepting=false;remember(e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}
    }

    private void initEncoder(int sourceW,int sourceH)throws Exception{
        int[] wh=outputSize(VideoEffectState.viewportW(),VideoEffectState.viewportH());int w=wh[0],h=wh[1];
        MediaFormat f=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w,h);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        f.setInteger(MediaFormat.KEY_FRAME_RATE,FPS);f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);
        f.setInteger(MediaFormat.KEY_BIT_RATE,Math.max(900_000,Math.min(4_000_000,w*h*7)));
        codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);codec.configure(f,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface input=codec.createInputSurface();codec.start();muxer=new MediaMuxer(videoFile.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        encoderSurface=new EncoderSurface(input,w,h,sourceW,sourceH);
    }

    private static int[] outputSize(int vw,int vh){
        if(vw<1||vh<1)return new int[]{640,360};double a=vw/(double)vh;int w,h;
        if(a>=1){w=640;h=even((int)Math.round(w/a));h=Math.max(240,Math.min(640,h));}
        else{h=640;w=even((int)Math.round(h*a));w=Math.max(240,Math.min(640,w));}
        return new int[]{even(w),even(h)};
    }
    private static int even(int v){return Math.max(2,v&~1);}

    private void drain(boolean end)throws Exception{
        if(codec==null)return;if(end)codec.signalEndOfInputStream();MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();int idle=0;
        while(true){
            int out=codec.dequeueOutputBuffer(info,end?10000:0);
            if(out==MediaCodec.INFO_TRY_AGAIN_LATER){if(!end||++idle>100)break;continue;}
            if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){if(muxerStarted)throw new IllegalStateException("format changed twice");track=muxer.addTrack(codec.getOutputFormat());muxer.start();muxerStarted=true;continue;}
            if(out<0)continue;ByteBuffer b=codec.getOutputBuffer(out);if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0;
            if(info.size>0&&b!=null){if(!muxerStarted)throw new IllegalStateException("muxer not started");b.position(info.offset);b.limit(info.offset+info.size);muxer.writeSampleData(track,b,info);}
            boolean eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(out,false);if(eos)break;
        }
    }

    StopResult stopGracefully(long timeoutMs){
        synchronized(this){if(!accepting&&!closing)return lastResult!=null?lastResult:snapshot();accepting=false;if(closing)return lastResult!=null?lastResult:snapshot();closing=true;}
        Future<StopResult> f=io.submit(()->{synchronized(this){StopResult r=finish(true);closing=false;lastResult=r;return r;}});
        try{return f.get(Math.max(1000,timeoutMs),TimeUnit.MILLISECONDS);}catch(Exception e){f.cancel(false);synchronized(this){remember("close_timeout:"+e.getClass().getSimpleName());StopResult r=finish(true);closing=false;lastResult=r;return r;}}
    }
    StopResult stopNow(){return stopGracefully(2500);}
    synchronized boolean isRecording(){return accepting;}synchronized boolean isClosing(){return closing;}synchronized long frameCount(){return frames;}synchronized long droppedFrames(){return dropped;}

    private StopResult finish(boolean export){
        try{if(index!=null)index.flush();}catch(Exception e){remember(e.getClass().getSimpleName());}
        try{if(codec!=null)drain(true);}catch(Exception e){remember(e.getClass().getSimpleName());}
        releaseCodec(true);try{if(index!=null)index.close();}catch(Exception e){remember(e.getClass().getSimpleName());}index=null;
        if(frames==0)remember("no_frames");
        if(export&&videoFile!=null&&videoFile.isFile()&&videoFile.length()>0)try{galleryUri=exportGallery(videoFile,displayName);}catch(Exception e){remember("gallery_export:"+e.getClass().getSimpleName());}
        return snapshot();
    }

    private void releaseCodec(boolean stopMuxer){
        if(encoderSurface!=null){try{encoderSurface.release();}catch(Exception ignored){}encoderSurface=null;}
        if(codec!=null){try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}codec=null;}
        if(muxer!=null){if(stopMuxer&&muxerStarted)try{muxer.stop();}catch(Exception e){remember("muxer_stop");}try{muxer.release();}catch(Exception ignored){}muxer=null;}
        muxerStarted=false;track=-1;
    }
    private StopResult snapshot(){return new StopResult(videoFile,indexFile,galleryUri,displayName,frames,dropped,lastError);}
    private void remember(String s){if(lastError==null||lastError.isEmpty())lastError=s;else if(s!=null&&!s.isEmpty()&&!lastError.contains(s))lastError+=" | "+s;}
    void release(){try{stopGracefully(2500);}catch(Exception ignored){}io.shutdownNow();}

    private Uri exportGallery(File source,String name)throws Exception{
        if(Build.VERSION.SDK_INT>=29){
            ContentResolver r=context.getContentResolver();ContentValues v=new ContentValues();v.put(MediaStore.Video.Media.DISPLAY_NAME,name);v.put(MediaStore.Video.Media.TITLE,name.substring(0,name.length()-4));v.put(MediaStore.Video.Media.MIME_TYPE,MIME);v.put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/HazRob");v.put(MediaStore.Video.Media.DATE_TAKEN,System.currentTimeMillis());v.put(MediaStore.Video.Media.IS_PENDING,1);
            Uri u=r.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IllegalStateException("MediaStore insert");boolean ok=false;
            try(OutputStream out=r.openOutputStream(u,"w");FileInputStream in=new FileInputStream(source)){if(out==null)throw new IllegalStateException("MediaStore stream");copy(in,out);ok=true;}finally{if(!ok)r.delete(u,null,null);}
            ContentValues done=new ContentValues();done.put(MediaStore.Video.Media.IS_PENDING,0);r.update(u,done,null,null);return u;
        }
        File movies=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),dir=new File(movies,"HazRob");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Movies/HazRob unavailable");File dest=new File(dir,name);try(FileInputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(dest)){copy(in,out);}MediaScannerConnection.scanFile(context,new String[]{dest.getAbsolutePath()},new String[]{MIME},null);return Uri.fromFile(dest);
    }
    private static void copy(FileInputStream in,OutputStream out)throws Exception{byte[] buf=new byte[128*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);out.flush();}

    private static final class EncoderSurface {
        private final Surface androidSurface;private final android.opengl.EGLDisplay display;private final android.opengl.EGLContext context;private final android.opengl.EGLSurface egl;
        private final int width,height,sourceW,sourceH,program,texture,aPos,aTex,uTex,uVertexScale,uDisplay,uEnh,uUvScale,uTexel,uAuto;
        private final FloatBuffer vertices=buffer(new float[]{-1,-1,1,-1,-1,1,1,1}),tex=buffer(new float[]{0,1,1,1,0,0,1,0});
        EncoderSurface(Surface s,int w,int h,int sw,int sh){
            androidSurface=s;width=w;height=h;sourceW=Math.max(1,sw);sourceH=Math.max(1,sh);display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] ver=new int[2];if(display==EGL14.EGL_NO_DISPLAY||!EGL14.eglInitialize(display,ver,0,ver,1))throw new IllegalStateException("EGL init");
            int[] cfg={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_NONE};android.opengl.EGLConfig[] cs=new android.opengl.EGLConfig[1];int[] n=new int[1];if(!EGL14.eglChooseConfig(display,cfg,0,cs,0,1,n,0)||n[0]<1)throw new IllegalStateException("EGL config");
            context=EGL14.eglCreateContext(display,cs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);egl=EGL14.eglCreateWindowSurface(display,cs[0],s,new int[]{EGL14.EGL_NONE},0);if(!EGL14.eglMakeCurrent(display,egl,egl,context))throw new IllegalStateException("EGL current");
            program=link(VideoEffectShader.VERTEX,VideoEffectShader.FRAGMENT);aPos=GLES20.glGetAttribLocation(program,"aPosition");aTex=GLES20.glGetAttribLocation(program,"aTexCoord");uTex=GLES20.glGetUniformLocation(program,"uTexture");uVertexScale=GLES20.glGetUniformLocation(program,"uVertexScale");uDisplay=GLES20.glGetUniformLocation(program,"uDisplayMode");uEnh=GLES20.glGetUniformLocation(program,"uEnhancement");uUvScale=GLES20.glGetUniformLocation(program,"uUvScale");uTexel=GLES20.glGetUniformLocation(program,"uTexel");uAuto=GLES20.glGetUniformLocation(program,"uAutoGain");
            int[] t=new int[1];GLES20.glGenTextures(1,t,0);texture=t[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        }
        void draw(Bitmap b,long ptsNs,VideoEffectProfile.DisplayMode dm,VideoEffectProfile.Enhancement en,float gain){
            if(!EGL14.eglMakeCurrent(display,egl,egl,context))throw new IllegalStateException("EGL current");GLES20.glViewport(0,0,width,height);GLES20.glClearColor(0,0,0,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);
            float sa=sourceW/(float)sourceH,va=width/(float)height,vx=1f,vy=1f;if(dm==VideoEffectProfile.DisplayMode.NORMAL){if(va>sa)vx=sa/va;else if(va<sa)vy=va/sa;}float[] uv=VideoEffectMath.uvScale(dm,sa,va);
            GLES20.glUniform2f(uVertexScale,vx,vy);GLES20.glUniform1i(uDisplay,dm.ordinal());GLES20.glUniform1i(uEnh,en.ordinal());GLES20.glUniform2f(uUvScale,uv[0],uv[1]);GLES20.glUniform2f(uTexel,1f/sourceW,1f/sourceH);GLES20.glUniform1f(uAuto,gain);GLES20.glUniform1i(uTex,0);
            vertices.position(0);tex.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glEnableVertexAttribArray(aTex);GLES20.glVertexAttribPointer(aTex,2,GLES20.GL_FLOAT,false,0,tex);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(aPos);GLES20.glDisableVertexAttribArray(aTex);
            EGLExt.eglPresentationTimeANDROID(display,egl,ptsNs);EGL14.eglSwapBuffers(display,egl);
        }
        void release(){try{EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);}catch(Exception ignored){}try{EGL14.eglDestroySurface(display,egl);}catch(Exception ignored){}try{EGL14.eglDestroyContext(display,context);}catch(Exception ignored){}try{EGL14.eglReleaseThread();}catch(Exception ignored){}try{EGL14.eglTerminate(display);}catch(Exception ignored){}try{androidSurface.release();}catch(Exception ignored){}}
        private static FloatBuffer buffer(float[] a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
        private static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){String log=GLES20.glGetShaderInfoLog(s);GLES20.glDeleteShader(s);throw new IllegalStateException("shader: "+log);}return s;}
        private static int link(String v,String f){int vs=shader(GLES20.GL_VERTEX_SHADER,v),fs=shader(GLES20.GL_FRAGMENT_SHADER,f),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,vs);GLES20.glAttachShader(p,fs);GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs);if(ok[0]==0){String log=GLES20.glGetProgramInfoLog(p);GLES20.glDeleteProgram(p);throw new IllegalStateException("program: "+log);}return p;}
    }
}
