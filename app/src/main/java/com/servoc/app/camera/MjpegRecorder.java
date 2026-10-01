package com.servoc.app.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Converts the ESP32-CAM MJPEG frame stream to a standard H.264/MP4 recording.
 * A private working copy and timestamp audit CSV are retained for evidence handling;
 * the finalized MP4 is also exported to MediaStore Movies/HazRob for normal Gallery access.
 */
public final class MjpegRecorder implements MjpegView.FrameListener {
    private static final String MIME="video/mp4";
    private static final int FPS=15;

    public static final class StopResult {
        public final File video,index;
        public final Uri galleryUri;
        public final String displayName;
        public final long frames,dropped;
        public final String error;
        StopResult(File video,File index,Uri galleryUri,String displayName,long frames,long dropped,String error){
            this.video=video;this.index=index;this.galleryUri=galleryUri;this.displayName=displayName;this.frames=frames;this.dropped=dropped;this.error=error;
        }
        public boolean clean(){return error==null||error.isEmpty();}
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
    private int trackIndex=-1;
    private boolean muxerStarted=false;
    private long firstElapsed=-1,lastPtsUs=-1,frames=0,dropped=0;
    private boolean accepting=false,closing=false;
    private String lastError;
    private Uri galleryUri;
    private StopResult lastResult;

    public MjpegRecorder(Context context){
        this.context=context.getApplicationContext();
        File d=context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(d==null)d=new File(context.getFilesDir(),"video");
        directory=new File(d,"HazRob");directory.mkdirs();
    }

    public synchronized File start(String baseName)throws Exception{
        if(closing||queued.get()>0||accepting)throw new IllegalStateException("previous recording is still active");
        releaseCodecLocked(false);
        String safe=baseName==null?"HazRob":baseName.replaceAll("[^A-Za-z0-9_.-]","_");
        if(safe.isEmpty())safe="HazRob";
        if(!safe.toLowerCase(Locale.US).endsWith(".mp4"))safe+=".mp4";
        displayName=safe;
        videoFile=new File(directory,displayName);
        indexFile=new File(directory,displayName.substring(0,displayName.length()-4)+"_frames.csv");
        if(videoFile.exists())videoFile.delete();
        index=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile),StandardCharsets.UTF_8),64*1024);
        index.write("frame,elapsed_ms,presentation_us\n");
        firstElapsed=-1;lastPtsUs=-1;frames=0;dropped=0;lastError=null;galleryUri=null;lastResult=null;trackIndex=-1;muxerStarted=false;accepting=true;closing=false;
        return videoFile;
    }

    /** Stops accepting frames, drains accepted frames, finalizes MP4, then exports it to Gallery. */
    public StopResult stopGracefully(long timeoutMs){
        synchronized(this){
            if(!accepting&&!closing)return lastResult!=null?lastResult:snapshotLocked();
            accepting=false;
            if(closing)return lastResult!=null?lastResult:snapshotLocked();
            closing=true;
        }
        Future<StopResult> barrier=io.submit(()->{
            synchronized(this){StopResult r=finishLocked(true);closing=false;lastResult=r;return r;}
        });
        try{return barrier.get(Math.max(1000,timeoutMs),TimeUnit.MILLISECONDS);}
        catch(Exception e){
            barrier.cancel(false);
            synchronized(this){remember("close_timeout:"+e.getClass().getSimpleName());StopResult r=finishLocked(true);closing=false;lastResult=r;return r;}
        }
    }

    public StopResult stopNow(){return stopGracefully(2500);}

    public synchronized boolean isRecording(){return accepting;}
    public synchronized boolean isClosing(){return closing;}
    public synchronized File videoFile(){return videoFile;}
    public synchronized File indexFile(){return indexFile;}
    public synchronized long frameCount(){return frames;}
    public synchronized long droppedFrames(){return dropped;}
    public synchronized String lastError(){return lastError;}
    public synchronized Uri galleryUri(){return galleryUri;}

    @Override public void onJpegFrame(byte[] jpeg,long elapsedMs){
        if(jpeg==null||jpeg.length<4)return;
        synchronized(this){if(!accepting)return;}
        if(queued.incrementAndGet()>6){queued.decrementAndGet();synchronized(this){dropped++;}return;}
        io.execute(()->{try{writeFrame(jpeg,elapsedMs);}finally{queued.decrementAndGet();}});
    }

    private synchronized void writeFrame(byte[] jpeg,long elapsedMs){
        if(!accepting&&closing==false)return;
        Bitmap bitmap=null;
        try{
            bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);
            if(bitmap==null){dropped++;return;}
            if(codec==null)initEncoderLocked(bitmap.getWidth(),bitmap.getHeight());
            if(firstElapsed<0)firstElapsed=elapsedMs;
            long ptsUs=Math.max(lastPtsUs+1,Math.max(0,elapsedMs-firstElapsed)*1000L);lastPtsUs=ptsUs;
            encoderSurface.draw(bitmap,ptsUs*1000L);
            drainLocked(false);
            if(index!=null){index.write(Long.toString(frames));index.write(',');index.write(Long.toString(Math.max(0,elapsedMs-firstElapsed)));index.write(',');index.write(Long.toString(ptsUs));index.write('\n');if((frames&31)==31)index.flush();}
            frames++;
        }catch(Exception e){dropped++;accepting=false;remember(e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}
        finally{if(bitmap!=null)bitmap.recycle();}
    }

    private void initEncoderLocked(int sourceW,int sourceH)throws Exception{
        int w=Math.max(2,sourceW&~1),h=Math.max(2,sourceH&~1);
        MediaFormat f=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w,h);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        f.setInteger(MediaFormat.KEY_FRAME_RATE,FPS);
        f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);
        int bitrate=Math.max(600_000,Math.min(4_000_000,w*h*6));f.setInteger(MediaFormat.KEY_BIT_RATE,bitrate);
        codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(f,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface input=codec.createInputSurface();
        codec.start();
        muxer=new MediaMuxer(videoFile.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        encoderSurface=new EncoderSurface(input,w,h);
    }

    private void drainLocked(boolean end)throws Exception{
        if(codec==null)return;
        if(end)codec.signalEndOfInputStream();
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        int idle=0;
        while(true){
            int out=codec.dequeueOutputBuffer(info,end?10_000:0);
            if(out==MediaCodec.INFO_TRY_AGAIN_LATER){if(!end||++idle>100)break;continue;}
            if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                if(muxerStarted)throw new IllegalStateException("encoder format changed twice");
                trackIndex=muxer.addTrack(codec.getOutputFormat());muxer.start();muxerStarted=true;continue;
            }
            if(out<0)continue;
            ByteBuffer b=codec.getOutputBuffer(out);
            if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0;
            if(info.size>0&&b!=null){
                if(!muxerStarted)throw new IllegalStateException("muxer has not started");
                b.position(info.offset);b.limit(info.offset+info.size);muxer.writeSampleData(trackIndex,b,info);
            }
            boolean eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
            codec.releaseOutputBuffer(out,false);
            if(eos)break;
        }
    }

    private StopResult finishLocked(boolean export){
        try{if(index!=null)index.flush();}catch(Exception e){remember(e.getClass().getSimpleName());}
        try{if(codec!=null)drainLocked(true);}catch(Exception e){remember(e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}
        releaseCodecLocked(true);
        try{if(index!=null)index.close();}catch(Exception e){remember(e.getClass().getSimpleName());}
        index=null;firstElapsed=-1;lastPtsUs=-1;
        if(frames==0)remember("no_frames");
        if(export&&videoFile!=null&&videoFile.isFile()&&videoFile.length()>0){
            try{galleryUri=exportGallery(videoFile,displayName);}catch(Exception e){remember("gallery_export:"+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}
        }
        return snapshotLocked();
    }

    private void releaseCodecLocked(boolean stopMuxer){
        if(encoderSurface!=null){try{encoderSurface.release();}catch(Exception ignored){}encoderSurface=null;}
        if(codec!=null){try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}codec=null;}
        if(muxer!=null){if(stopMuxer&&muxerStarted)try{muxer.stop();}catch(Exception e){remember("muxer_stop:"+e.getClass().getSimpleName());}try{muxer.release();}catch(Exception ignored){}muxer=null;}
        muxerStarted=false;trackIndex=-1;
    }

    private Uri exportGallery(File source,String name)throws Exception{
        if(Build.VERSION.SDK_INT>=29){
            ContentResolver resolver=context.getContentResolver();ContentValues v=new ContentValues();
            v.put(MediaStore.Video.Media.DISPLAY_NAME,name);v.put(MediaStore.Video.Media.TITLE,name.substring(0,name.length()-4));v.put(MediaStore.Video.Media.MIME_TYPE,MIME);v.put(MediaStore.Video.Media.RELATIVE_PATH,Environment.DIRECTORY_MOVIES+"/HazRob");v.put(MediaStore.Video.Media.DATE_TAKEN,System.currentTimeMillis());v.put(MediaStore.Video.Media.IS_PENDING,1);
            Uri uri=resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new IllegalStateException("MediaStore insert failed");
            boolean ok=false;try(OutputStream out=resolver.openOutputStream(uri,"w");FileInputStream in=new FileInputStream(source)){if(out==null)throw new IllegalStateException("MediaStore stream unavailable");copy(in,out);ok=true;}finally{if(!ok)resolver.delete(uri,null,null);}
            ContentValues done=new ContentValues();done.put(MediaStore.Video.Media.IS_PENDING,0);resolver.update(uri,done,null,null);return uri;
        }
        File movies=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);File dir=new File(movies,"HazRob");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Movies/HazRob unavailable");File dest=new File(dir,name);
        try(FileInputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(dest)){copy(in,out);}
        MediaScannerConnection.scanFile(context,new String[]{dest.getAbsolutePath()},new String[]{MIME},null);return Uri.fromFile(dest);
    }

    private static void copy(FileInputStream in,OutputStream out)throws Exception{byte[] buf=new byte[128*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);out.flush();}
    private void remember(String s){if(lastError==null||lastError.isEmpty())lastError=s;else if(s!=null&&!s.isEmpty()&&!lastError.contains(s))lastError+=" | "+s;}
    private StopResult snapshotLocked(){return new StopResult(videoFile,indexFile,galleryUri,displayName,frames,dropped,lastError);}
    public void release(){try{stopGracefully(2500);}catch(Exception ignored){}io.shutdownNow();}

    /** OpenGL ES 2.0 renderer for MediaCodec's hardware encoder input Surface. */
    private static final class EncoderSurface {
        private final Surface surface;private final android.opengl.EGLDisplay display;private final android.opengl.EGLContext context;private final android.opengl.EGLSurface eglSurface;private final int width,height,program,texture,aPos,aTex,uTex;private final FloatBuffer vertices,texCoords;
        EncoderSurface(Surface surface,int width,int height){
            this.surface=surface;this.width=width;this.height=height;
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);if(display==EGL14.EGL_NO_DISPLAY)throw new IllegalStateException("EGL display");int[] version=new int[2];if(!EGL14.eglInitialize(display,version,0,version,1))throw new IllegalStateException("EGL init");
            int[] cfg={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_NONE};android.opengl.EGLConfig[] configs=new android.opengl.EGLConfig[1];int[] num=new int[1];if(!EGL14.eglChooseConfig(display,cfg,0,configs,0,1,num,0)||num[0]<1)throw new IllegalStateException("EGL config");
            int[] ca={EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE};context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,ca,0);int[] sa={EGL14.EGL_NONE};eglSurface=EGL14.eglCreateWindowSurface(display,configs[0],surface,sa,0);if(!EGL14.eglMakeCurrent(display,eglSurface,eglSurface,context))throw new IllegalStateException("EGL current");
            program=link("attribute vec4 aPosition;attribute vec2 aTexCoord;varying vec2 vTex;void main(){gl_Position=aPosition;vTex=aTexCoord;}","precision mediump float;varying vec2 vTex;uniform sampler2D uTexture;void main(){gl_FragColor=texture2D(uTexture,vTex);}");aPos=GLES20.glGetAttribLocation(program,"aPosition");aTex=GLES20.glGetAttribLocation(program,"aTexCoord");uTex=GLES20.glGetUniformLocation(program,"uTexture");int[] t=new int[1];GLES20.glGenTextures(1,t,0);texture=t[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            vertices=buffer(new float[]{-1,-1,1,-1,-1,1,1,1});texCoords=buffer(new float[]{0,1,1,1,0,0,1,0});
        }
        void draw(Bitmap b,long ptsNs){if(!EGL14.eglMakeCurrent(display,eglSurface,eglSurface,context))throw new IllegalStateException("EGL current");GLES20.glViewport(0,0,width,height);GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);GLES20.glUniform1i(uTex,0);vertices.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,vertices);texCoords.position(0);GLES20.glEnableVertexAttribArray(aTex);GLES20.glVertexAttribPointer(aTex,2,GLES20.GL_FLOAT,false,0,texCoords);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);EGLExt.eglPresentationTimeANDROID(display,eglSurface,ptsNs);if(!EGL14.eglSwapBuffers(display,eglSurface))throw new IllegalStateException("EGL swap");}
        void release(){try{GLES20.glDeleteTextures(1,new int[]{texture},0);GLES20.glDeleteProgram(program);}catch(Exception ignored){}EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);EGL14.eglDestroySurface(display,eglSurface);EGL14.eglDestroyContext(display,context);EGL14.eglReleaseThread();EGL14.eglTerminate(display);surface.release();}
        private static FloatBuffer buffer(float[] x){FloatBuffer b=ByteBuffer.allocateDirect(x.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(x).position(0);return b;}
        private static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
        private static int link(String vs,String fs){int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}
    }
}
