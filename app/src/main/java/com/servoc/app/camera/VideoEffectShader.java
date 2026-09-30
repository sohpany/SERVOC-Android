package com.servoc.app.camera;

/** One shader contract is used by the live view and H.264 recorder. */
public final class VideoEffectShader {
    private VideoEffectShader(){}

    public static final String VERTEX=
            "attribute vec4 aPosition;attribute vec2 aTexCoord;varying vec2 vTex;uniform vec2 uVertexScale;"+
            "void main(){gl_Position=vec4(aPosition.xy*uVertexScale,0.0,1.0);vTex=aTexCoord;}";

    public static final String FRAGMENT=
            "precision mediump float;varying vec2 vTex;uniform sampler2D uTexture;uniform int uDisplayMode;uniform int uEnhancement;uniform vec2 uUvScale;uniform vec2 uTexel;uniform float uAutoGain;"+
            "float sat(float x){return clamp(x,0.0,1.0);}"+
            "vec3 thermal(float x){return vec3(sat(1.5-abs(4.0*x-3.0)),sat(1.5-abs(4.0*x-2.0)),sat(1.5-abs(4.0*x-1.0)));}"+
            "void main(){"+
            "vec2 uv=(vTex-vec2(.5))*uUvScale+vec2(.5);"+
            "if(uDisplayMode==1){vec2 p=(uv-vec2(.5))*2.0;float r2=dot(p,p);float k=1.0+.20*r2+.035*r2*r2;uv=p*k*.5+vec2(.5);}"+
            "if(uv.x<0.0||uv.x>1.0||uv.y<0.0||uv.y>1.0){gl_FragColor=vec4(0.0,0.0,0.0,1.0);return;}"+
            "vec3 c=texture2D(uTexture,uv).rgb;float y=dot(c,vec3(.2126,.7152,.0722));"+
            "if(uEnhancement==1){float q=sat(pow(y,.56)*1.28);c=vec3(.05*q,q,.13*q);}"+
            "else if(uEnhancement==2){c=thermal(y);}"+
            "else if(uEnhancement==3){c=clamp(pow(c,vec3(.58))*1.12,vec3(0.0),vec3(1.0));}"+
            "else if(uEnhancement==4){c=clamp(pow(c,vec3(.86))*1.18+vec3(.025),vec3(0.0),vec3(1.0));}"+
            "else if(uEnhancement==5){c*=.58;}"+
            "else if(uEnhancement==6){c=clamp(c*uAutoGain,vec3(0.0),vec3(1.0));}"+
            "else if(uEnhancement==7){vec3 n=texture2D(uTexture,uv+vec2(0.0,-uTexel.y)).rgb;vec3 s=texture2D(uTexture,uv+vec2(0.0,uTexel.y)).rgb;vec3 e=texture2D(uTexture,uv+vec2(uTexel.x,0.0)).rgb;vec3 w=texture2D(uTexture,uv+vec2(-uTexel.x,0.0)).rgb;c=clamp(c*1.60-(n+s+e+w)*.15,vec3(0.0),vec3(1.0));}"+
            "gl_FragColor=vec4(c,1.0);}";
}
