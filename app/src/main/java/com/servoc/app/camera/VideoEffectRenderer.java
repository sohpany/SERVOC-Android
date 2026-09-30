package com.servoc.app.camera;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Latest-frame-wins OpenGL surface renderer. No per-pixel CPU processing. */
public final class VideoEffectRenderer implements GLSurfaceView.Renderer {
    private final Object frameLock=new Object();
    private SharedVideoFrame pending;
    private int program,texture,aPos,aTex,uTex,uVertexScale,uDisplay,uEnh,uUvScale,uTexel,uAuto;
    private int viewW=1,viewH=1,sourceW=1,sourceH=1;private boolean hasFrame=false;
    private volatile VideoEffectProfile.DisplayMode displayMode=VideoEffectProfile.DisplayMode.NORMAL;
    private volatile VideoEffectProfile.Enhancement enhancement=VideoEffectProfile.Enhancement.NORMAL;
    private final FloatBuffer vertices=buffer(new float[]{-1,-1,1,-1,-1,1,1,1});
    private final FloatBuffer texCoords=buffer(new float[]{0,1,1,1,0,0,1,0});

    public void setProfile(VideoEffectProfile.DisplayMode mode,VideoEffectProfile.Enhancement effect){displayMode=mode==null?VideoEffectProfile.DisplayMode.NORMAL:mode;enhancement=effect==null?VideoEffectProfile.Enhancement.NORMAL:effect;}
    public VideoEffectProfile.DisplayMode displayMode(){return displayMode;}public VideoEffectProfile.Enhancement enhancement(){return enhancement;}

    /** One shared decoded frame is uploaded to GPU; an older waiting frame is released immediately. */
    public void submit(SharedVideoFrame frame){if(frame==null)return;synchronized(frameLock){SharedVideoFrame old=pending;pending=frame;if(old!=null&&old!=frame)old.release();}}
    public void releasePending(){synchronized(frameLock){if(pending!=null)pending.release();pending=null;}}

    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        GLES20.glClearColor(0,0,0,1);program=link(VideoEffectShader.VERTEX,VideoEffectShader.FRAGMENT);
        aPos=GLES20.glGetAttribLocation(program,"aPosition");aTex=GLES20.glGetAttribLocation(program,"aTexCoord");uTex=GLES20.glGetUniformLocation(program,"uTexture");uVertexScale=GLES20.glGetUniformLocation(program,"uVertexScale");uDisplay=GLES20.glGetUniformLocation(program,"uDisplayMode");uEnh=GLES20.glGetUniformLocation(program,"uEnhancement");uUvScale=GLES20.glGetUniformLocation(program,"uUvScale");uTexel=GLES20.glGetUniformLocation(program,"uTexel");uAuto=GLES20.glGetUniformLocation(program,"uAutoGain");
        int[] t=new int[1];GLES20.glGenTextures(1,t,0);texture=t[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
    }
    @Override public void onSurfaceChanged(GL10 gl,int width,int height){viewW=Math.max(1,width);viewH=Math.max(1,height);GLES20.glViewport(0,0,viewW,viewH);}
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        SharedVideoFrame renderedFrame=null;
        synchronized(frameLock){if(pending!=null){renderedFrame=pending;pending=null;}}
        if(renderedFrame!=null){
            Bitmap b=renderedFrame.bitmap();sourceW=Math.max(1,b.getWidth());sourceH=Math.max(1,b.getHeight());GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);hasFrame=true;
        }
        if(!hasFrame){if(renderedFrame!=null)renderedFrame.release();return;}
        float sa=sourceW/(float)sourceH,va=viewW/(float)viewH,vx=1f,vy=1f;if(displayMode==VideoEffectProfile.DisplayMode.NORMAL){if(va>sa)vx=sa/va;else if(va<sa)vy=va/sa;}float[] uv=VideoEffectMath.uvScale(displayMode,sa,va);
        GLES20.glUseProgram(program);GLES20.glUniform2f(uVertexScale,vx,vy);GLES20.glUniform1i(uDisplay,displayMode.ordinal());GLES20.glUniform1i(uEnh,enhancement.ordinal());GLES20.glUniform2f(uUvScale,uv[0],uv[1]);GLES20.glUniform2f(uTexel,1f/sourceW,1f/sourceH);GLES20.glUniform1f(uAuto,VideoEffectState.autoGain());
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glUniform1i(uTex,0);vertices.position(0);texCoords.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glEnableVertexAttribArray(aTex);GLES20.glVertexAttribPointer(aTex,2,GLES20.GL_FLOAT,false,0,texCoords);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(aPos);GLES20.glDisableVertexAttribArray(aTex);

        // Only a newly rendered video frame is offered to the recorder. Controls, tabs and Android UI are never captured.
        if(renderedFrame!=null){
            VideoEffectFrameHub.Consumer recorder=VideoEffectFrameHub.get();
            if(recorder!=null){renderedFrame.retain();try{recorder.onDecodedFrame(renderedFrame);}catch(Exception ignored){renderedFrame.release();}}
            renderedFrame.release();
        }
    }
    private static FloatBuffer buffer(float[] values){ByteBuffer b=ByteBuffer.allocateDirect(values.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(values).position(0);return f;}
    private static int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){String log=GLES20.glGetShaderInfoLog(s);GLES20.glDeleteShader(s);throw new IllegalStateException("video shader compile: "+log);}return s;}
    private static int link(String vertex,String fragment){int v=shader(GLES20.GL_VERTEX_SHADER,vertex),f=shader(GLES20.GL_FRAGMENT_SHADER,fragment),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);if(ok[0]==0){String log=GLES20.glGetProgramInfoLog(p);GLES20.glDeleteProgram(p);throw new IllegalStateException("video program link: "+log);}return p;}
}
