package com.servoc.app.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Unified HazRob camera surface. Camera bytes stay untouched; all presentation effects are phone-side GPU work. */
public final class MjpegView extends FrameLayout {
    public interface FrameListener { void onJpegFrame(byte[] jpeg,long elapsedMs); }
    public interface DecodedFrameListener { void onDecodedFrame(SharedVideoFrame frame); }
    public interface StatusListener { void onStatus(String state,String detail); }

    private static final int MAX_JPEG=1024*1024;
    private static final long AUTO_SAMPLE_INTERVAL_MS=2000;
    private final ExecutorService streamWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService decodeWorker=Executors.newSingleThreadExecutor();
    private final Object decodeLock=new Object();
    private final RemoteJpegWebSocketClient remoteClient=new RemoteJpegWebSocketClient();
    private final VideoEffectRenderer effectRenderer=new VideoEffectRenderer();
    private final GLSurfaceView surface;
    private final Button displayButton,enhancementButton;
    private final SharedPreferences prefs;
    private final AtomicLong receivedFrames=new AtomicLong(),decodedFrames=new AtomicLong();

    private volatile boolean running=false,released=false;
    private volatile HttpURLConnection connection;
    private volatile long generation=0;
    private volatile FrameListener frameListener;
    private volatile DecodedFrameListener decodedFrameListener;
    private volatile StatusListener statusListener;
    private byte[] latestJpeg;private long latestElapsedMs=0;
    private boolean decodeBusy=false;private long lastAutoSampleMs=0;
    private VideoEffectProfile.DisplayMode displayMode;
    private VideoEffectProfile.Enhancement enhancement;

    public MjpegView(Context c){
        super(c);setBackgroundColor(0xFF000000);prefs=c.getSharedPreferences("servoc",Context.MODE_PRIVATE);
        displayMode=VideoEffectProfile.DisplayMode.fromKey(prefs.getString("video_display_mode","normal"));enhancement=VideoEffectProfile.Enhancement.fromKey(prefs.getString("video_enhancement_mode","normal"));VideoEffectState.set(displayMode,enhancement);
        surface=new GLSurfaceView(c);surface.setEGLContextClientVersion(2);surface.setPreserveEGLContextOnPause(true);surface.setRenderer(effectRenderer);surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);surface.setBackgroundColor(0xFF000000);addView(surface,new FrameLayout.LayoutParams(-1,-1));effectRenderer.setProfile(displayMode,enhancement);
        surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->VideoEffectState.setViewport(Math.max(1,r-l),Math.max(1,b-t)));

        displayButton=floating(c);displayButton.setOnClickListener(v->{displayMode=displayMode.next();applyProfile(true);});FrameLayout.LayoutParams dp=new FrameLayout.LayoutParams(dp(c,170),dp(c,42),Gravity.LEFT|Gravity.TOP);dp.setMargins(dp(c,6),dp(c,150),0,0);addView(displayButton,dp);
        enhancementButton=floating(c);enhancementButton.setOnClickListener(v->{enhancement=enhancement.next();if(enhancement==VideoEffectProfile.Enhancement.AUTO)lastAutoSampleMs=0;applyProfile(true);});FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(dp(c,170),dp(c,42),Gravity.LEFT|Gravity.TOP);ep.setMargins(dp(c,6),dp(c,198),0,0);addView(enhancementButton,ep);
        renderButtons();
    }
    private static Button floating(Context c){Button b=new Button(c);b.setAllCaps(false);b.setTextSize(11);b.setTextColor(0xFFFFFFFF);b.setBackgroundColor(0xCC263238);b.setPadding(dp(c,6),0,dp(c,6),0);return b;}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private void renderButtons(){displayButton.setText("عرض: "+displayMode.arabic);enhancementButton.setText("تحسين: "+enhancement.arabic);}
    private void applyProfile(boolean persist){effectRenderer.setProfile(displayMode,enhancement);VideoEffectState.set(displayMode,enhancement);if(persist)prefs.edit().putString("video_display_mode",displayMode.key).putString("video_enhancement_mode",enhancement.key).apply();renderButtons();surface.requestRender();}
    public void setVideoEffects(VideoEffectProfile.DisplayMode display,VideoEffectProfile.Enhancement effect){displayMode=display==null?VideoEffectProfile.DisplayMode.NORMAL:display;enhancement=effect==null?VideoEffectProfile.Enhancement.NORMAL:effect;if(enhancement==VideoEffectProfile.Enhancement.AUTO)lastAutoSampleMs=0;applyProfile(true);}
    public VideoEffectProfile.DisplayMode displayMode(){return displayMode;}public VideoEffectProfile.Enhancement enhancement(){return enhancement;}
    public long receivedFrameCount(){return receivedFrames.get();}public long decodedFrameCount(){return decodedFrames.get();}public long renderedFrameCount(){return effectRenderer.renderedFrameCount();}

    public void setFrameListener(FrameListener listener){frameListener=listener;}
    public void setDecodedFrameListener(DecodedFrameListener listener){decodedFrameListener=listener;}
    public void setStatusListener(StatusListener listener){statusListener=listener;}
    public void start(String url){start(new String[]{url});}
    public synchronized void start(String[] urls){if(released)return;resetTransport();if(urls==null||urls.length==0)return;running=true;final long token=generation;final String[] copy=urls.clone();status("connecting",copy[0]);streamWorker.execute(()->streamLoop(copy,token));}
    public synchronized void startWss(String url,String bearer){if(released)return;resetTransport();if(url==null||url.trim().isEmpty()){status("retrying","Remote video not configured");return;}running=true;final long token=generation;final String target=url.trim();final String auth=bearer==null?"":bearer;status("connecting",target);streamWorker.execute(()->remoteLoop(target,auth,token));}
    private void resetTransport(){running=false;generation++;HttpURLConnection c=connection;if(c!=null)c.disconnect();connection=null;remoteClient.close();synchronized(decodeLock){latestJpeg=null;latestElapsedMs=0;}}
    public synchronized void stop(){resetTransport();status("stopped","");}
    public synchronized void release(){if(released)return;stop();released=true;frameListener=null;decodedFrameListener=null;statusListener=null;effectRenderer.releasePending();streamWorker.shutdownNow();decodeWorker.shutdownNow();}
    private boolean active(long token){return !released&&running&&generation==token;}
    private void status(String state,String detail){StatusListener s=statusListener;if(s==null)return;post(()->{StatusListener x=statusListener;if(x!=null)x.onStatus(state,detail==null?"":detail);});}

    private void deliver(byte[] jpeg,long token){if(jpeg==null||!active(token))return;receivedFrames.incrementAndGet();long now=SystemClock.elapsedRealtime();FrameListener listener=frameListener;if(listener!=null)try{listener.onJpegFrame(jpeg,now);}catch(Exception ignored){}if(active(token))queueDecode(jpeg,now,token);}
    /** Decode queue is deliberately size one: old frames are replaced so effects never build latency. */
    private void queueDecode(byte[] jpeg,long elapsedMs,long token){synchronized(decodeLock){latestJpeg=jpeg;latestElapsedMs=elapsedMs;if(decodeBusy)return;decodeBusy=true;}decodeWorker.execute(()->decodeLoop(token));}
    private void decodeLoop(long token){
        try{
            while(active(token)){
                byte[] jpeg;long elapsed;
                synchronized(decodeLock){jpeg=latestJpeg;elapsed=latestElapsedMs;latestJpeg=null;latestElapsedMs=0;if(jpeg==null){decodeBusy=false;return;}}
                Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);if(bitmap==null)continue;decodedFrames.incrementAndGet();
                if(enhancement==VideoEffectProfile.Enhancement.AUTO&&(lastAutoSampleMs==0||elapsed-lastAutoSampleMs>=AUTO_SAMPLE_INTERVAL_MS)){VideoEffectState.setAutoGain(VideoEffectMath.autoGain(VideoEffectMath.sampleLuma(bitmap)));lastAutoSampleMs=elapsed;}
                DecodedFrameListener extra=decodedFrameListener;int consumers=extra==null?1:2;SharedVideoFrame shared=new SharedVideoFrame(bitmap,elapsed,consumers);
                if(active(token)){effectRenderer.submit(shared);surface.requestRender();}else{shared.release();if(extra!=null)shared.release();continue;}
                if(extra!=null){try{extra.onDecodedFrame(shared);}catch(Exception ignored){shared.release();}}
            }
        }finally{synchronized(decodeLock){decodeBusy=false;if(!active(token)){latestJpeg=null;latestElapsedMs=0;}}}
    }

    private static String pipelineDetail(String transport,long dt,long rx0,long dec0,long render0,long rx,long dec,long render){
        double k=dt>0?1000.0/dt:0;return String.format(Locale.US,"%s R%.1f D%.1f G%.1f",transport,(rx-rx0)*k,(dec-dec0)*k,(render-render0)*k);
    }

    private void remoteLoop(String url,String bearer,long token){
        int cycle=0;while(active(token)){
            status(cycle==0?"connecting":"retrying",url);
            try{
                final long[] at={SystemClock.elapsedRealtime()},rx0={receivedFrames.get()},dec0={decodedFrames.get()},ren0={effectRenderer.renderedFrameCount()};
                remoteClient.stream(url,bearer,new RemoteJpegWebSocketClient.Sink(){
                    @Override public boolean active(){return MjpegView.this.active(token);}
                    @Override public void frame(byte[] jpeg){deliver(jpeg,token);long now=SystemClock.elapsedRealtime();if(now-at[0]>=2000){long rx=receivedFrames.get(),dec=decodedFrames.get(),ren=effectRenderer.renderedFrameCount();status("streaming",pipelineDetail("WSS",now-at[0],rx0[0],dec0[0],ren0[0],rx,dec,ren));at[0]=now;rx0[0]=rx;dec0[0]=dec;ren0[0]=ren;}}
                    @Override public void status(String state,String detail){if(!"streaming".equals(state))MjpegView.this.status(state,detail);}
                });
            }catch(Exception e){if(active(token))status("retrying","WSS • "+e.getClass().getSimpleName());}
            if(!active(token))return;cycle++;try{Thread.sleep(800);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
    }

    private void streamLoop(String[] urls,long token){
        int cycle=0;while(active(token)){
            boolean gotFrame=false;String lastError="";
            for(String u:urls){if(!active(token))return;status(cycle==0?"connecting":"retrying",u);try{gotFrame=streamOnce(u,token);if(gotFrame&&active(token))break;}catch(Exception e){lastError=e.getClass().getSimpleName();}}
            if(!active(token))return;cycle++;status("retrying",gotFrame?"انقطع البث":("تعذر البث"+(lastError.isEmpty()?"":" • "+lastError)));try{Thread.sleep(gotFrame?450:700);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
        }
    }

    /**
     * Direct MJPEG parser uses the stream's Content-Length and buffered reads.
     * It deliberately avoids one-byte InputStream.read() loops, which can become an app-side FPS bottleneck.
     */
    private boolean streamOnce(String streamUrl,long token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(streamUrl).openConnection();connection=c;c.setConnectTimeout(2200);c.setReadTimeout(5500);c.setUseCaches(false);c.setInstanceFollowRedirects(true);c.setRequestMethod("GET");c.connect();
        boolean got=false;long at=SystemClock.elapsedRealtime(),rx0=receivedFrames.get(),dec0=decodedFrames.get(),ren0=effectRenderer.renderedFrameCount();
        try(BufferedInputStream in=new BufferedInputStream(c.getInputStream(),64*1024)){
            while(active(token)){
                String line=readAsciiLine(in);if(line==null)break;if(line.isEmpty())continue;if(!line.startsWith("--"))continue;
                int contentLength=-1;
                while(true){String h=readAsciiLine(in);if(h==null)return got;if(h.isEmpty())break;int p=h.indexOf(':');if(p>0&&"content-length".equalsIgnoreCase(h.substring(0,p).trim())){try{contentLength=Integer.parseInt(h.substring(p+1).trim());}catch(Exception ignored){contentLength=-1;}}}
                if(contentLength<4||contentLength>MAX_JPEG)throw new IllegalStateException("invalid MJPEG content length: "+contentLength);
                byte[] jpeg=new byte[contentLength];readFully(in,jpeg,contentLength);
                if(jpeg[0]!=(byte)0xFF||jpeg[1]!=(byte)0xD8||jpeg[contentLength-2]!=(byte)0xFF||jpeg[contentLength-1]!=(byte)0xD9)continue;
                got=true;deliver(jpeg,token);
                long now=SystemClock.elapsedRealtime();if(now-at>=2000){long rx=receivedFrames.get(),dec=decodedFrames.get(),ren=effectRenderer.renderedFrameCount();status("streaming",pipelineDetail("HTTP",now-at,rx0,dec0,ren0,rx,dec,ren));at=now;rx0=rx;dec0=dec;ren0=ren;}
            }
        }finally{c.disconnect();if(connection==c)connection=null;}
        return got;
    }

    private static String readAsciiLine(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream(96);int b;boolean any=false;
        while((b=in.read())!=-1){any=true;if(b=='\n')break;if(b!='\r'){if(out.size()>2048)throw new IllegalStateException("MJPEG header too long");out.write(b);}}
        if(!any&&b==-1)return null;return out.toString("US-ASCII");
    }
    private static void readFully(InputStream in,byte[] target,int len)throws Exception{int off=0;while(off<len){int n=in.read(target,off,len-off);if(n<0)throw new java.io.EOFException("MJPEG frame truncated");off+=n;}}

    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
