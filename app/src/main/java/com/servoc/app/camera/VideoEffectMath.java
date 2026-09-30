package com.servoc.app.camera;

import android.graphics.Bitmap;

/** Small deterministic helpers shared by display/recording and unit tests. */
public final class VideoEffectMath {
    private VideoEffectMath(){}

    public static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}

    /** Cheap luma estimate: <=96 pixel reads regardless of frame size. */
    public static float sampleLuma(Bitmap b){
        if(b==null||b.getWidth()<1||b.getHeight()<1)return .5f;
        int sx=Math.max(1,b.getWidth()/12),sy=Math.max(1,b.getHeight()/8),n=0;double sum=0;
        for(int y=sy/2;y<b.getHeight();y+=sy)for(int x=sx/2;x<b.getWidth();x+=sx){int c=b.getPixel(x,y);float r=((c>>16)&255)/255f,g=((c>>8)&255)/255f,bl=(c&255)/255f;sum+=.2126*r+.7152*g+.0722*bl;n++;if(n>=96)break;}
        return n==0?.5f:(float)(sum/n);
    }

    public static float autoGain(float meanLuma){float m=clamp(meanLuma,.05f,.95f);return clamp(.50f/m,.72f,1.85f);}

    /** UV scale for center-crop modes. NORMAL uses vertex fit and therefore 1,1 UV scale. */
    public static float[] uvScale(VideoEffectProfile.DisplayMode mode,float sourceAspect,float viewAspect){
        float sx=1f,sy=1f;if(mode!=VideoEffectProfile.DisplayMode.NORMAL){if(viewAspect>sourceAspect)sy=sourceAspect/viewAspect;else if(viewAspect<sourceAspect)sx=viewAspect/sourceAspect;if(mode==VideoEffectProfile.DisplayMode.ZOOM){sx*=.62f;sy*=.62f;}}
        return new float[]{clamp(sx,.1f,1f),clamp(sy,.1f,1f)};
    }

    /** Reference transform used by tests; shader uses the same equations. */
    public static float[] transformUv(VideoEffectProfile.DisplayMode mode,float u,float v,float sourceAspect,float viewAspect){
        float[] s=uvScale(mode,sourceAspect,viewAspect);float x=(u-.5f)*s[0]+.5f,y=(v-.5f)*s[1]+.5f;
        if(mode==VideoEffectProfile.DisplayMode.FISHEYE){float px=(x-.5f)*2f,py=(y-.5f)*2f,r2=px*px+py*py,k=1f+.20f*r2+.035f*r2*r2;x=(px*k)*.5f+.5f;y=(py*k)*.5f+.5f;}
        return new float[]{x,y};
    }

    /** Reference single-pixel color transform. DETAIL's edge kernel is exercised in shader contract tests. */
    public static float[] transformColor(VideoEffectProfile.Enhancement e,float r,float g,float b,float gain){
        r=clamp(r,0,1);g=clamp(g,0,1);b=clamp(b,0,1);float y=.2126f*r+.7152f*g+.0722f*b;
        switch(e){
            case NIGHT_GREEN:{float q=clamp((float)Math.pow(y,.56)*1.28f,0,1);return new float[]{.05f*q,q,.13f*q};}
            case THERMAL:return thermal(y);
            case NIGHT:return new float[]{clamp((float)Math.pow(r,.58)*1.12f,0,1),clamp((float)Math.pow(g,.58)*1.12f,0,1),clamp((float)Math.pow(b,.58)*1.12f,0,1)};
            case BRIGHT:return new float[]{clamp((float)Math.pow(r,.86)*1.18f+.025f,0,1),clamp((float)Math.pow(g,.86)*1.18f+.025f,0,1),clamp((float)Math.pow(b,.86)*1.18f+.025f,0,1)};
            case DARK:return new float[]{r*.58f,g*.58f,b*.58f};
            case AUTO:return new float[]{clamp(r*gain,0,1),clamp(g*gain,0,1),clamp(b*gain,0,1)};
            default:return new float[]{r,g,b};
        }
    }

    private static float[] thermal(float x){
        x=clamp(x,0,1);float r=clamp(1.5f-Math.abs(4f*x-3f),0,1),g=clamp(1.5f-Math.abs(4f*x-2f),0,1),b=clamp(1.5f-Math.abs(4f*x-1f),0,1);return new float[]{r,g,b};
    }
}
