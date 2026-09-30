package com.servoc.app.camera;

/** Process-local state shared by live renderer and recorder. No camera/firmware side effects. */
public final class VideoEffectState {
    private VideoEffectState(){}
    private static volatile VideoEffectProfile.DisplayMode display=VideoEffectProfile.DisplayMode.NORMAL;
    private static volatile VideoEffectProfile.Enhancement enhancement=VideoEffectProfile.Enhancement.NORMAL;
    private static volatile int viewportW=640,viewportH=360;
    public static VideoEffectProfile.DisplayMode display(){return display;}
    public static VideoEffectProfile.Enhancement enhancement(){return enhancement;}
    public static int viewportW(){return viewportW;}public static int viewportH(){return viewportH;}
    public static void set(VideoEffectProfile.DisplayMode d,VideoEffectProfile.Enhancement e){display=d==null?VideoEffectProfile.DisplayMode.NORMAL:d;enhancement=e==null?VideoEffectProfile.Enhancement.NORMAL:e;}
    public static void setViewport(int w,int h){if(w>0&&h>0){viewportW=w;viewportH=h;}}
}
