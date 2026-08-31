package com.servoc.app.vision;

/**
 * Explainable flame-candidate color model for HazRob 0.6.
 *
 * This is an experimental visual screening stage, not a calibrated fire detector.
 * It combines established RGB (R>G>=B) and YCbCr flame-pixel rules, then reports
 * spatial coverage to the temporal evidence layer. No control/safety action is
 * permitted from this model alone.
 *
 * References used for the rule family:
 * - Celik & Demirel, Fire Safety Journal 44 (2009) 147-158,
 *   doi:10.1016/j.firesaf.2008.05.005
 * - Mahmoud & Ren, Mathematical Problems in Engineering (2018) 7612487,
 *   doi:10.1155/2018/7612487
 */
public final class FlameColorModel {
    private FlameColorModel(){}

    public static final class Result {
        public final double score,candidateRatio,brightCandidateRatio;
        public final int candidatePixels,totalPixels;
        /** Normalized candidate bounding box; NaN when no candidate pixels exist. */
        public final double left,top,right,bottom;
        Result(double score,double candidateRatio,double brightCandidateRatio,int candidatePixels,int totalPixels,double left,double top,double right,double bottom){
            this.score=score;this.candidateRatio=candidateRatio;this.brightCandidateRatio=brightCandidateRatio;this.candidatePixels=candidatePixels;this.totalPixels=totalPixels;this.left=left;this.top=top;this.right=right;this.bottom=bottom;
        }
        public boolean hasRegion(){return candidatePixels>0&&Double.isFinite(left)&&Double.isFinite(top)&&Double.isFinite(right)&&Double.isFinite(bottom);}
    }

    public static Result analyze(int[] argb,int width,int height){
        if(argb==null||width<=0||height<=0||argb.length<width*height)return empty();
        final int n=width*height;
        double meanY=0,meanCb=0,meanCr=0;
        for(int i=0;i<n;i++){
            int c=argb[i],r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            meanY+=y(r,g,b);meanCb+=cb(r,g,b);meanCr+=cr(r,g,b);
        }
        meanY/=n;meanCb/=n;meanCr/=n;

        int candidate=0,bright=0,minX=width,minY=height,maxX=-1,maxY=-1;
        for(int yy=0;yy<height;yy++)for(int xx=0;xx<width;xx++){
            int c=argb[yy*width+xx],r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            double Y=y(r,g,b),Cb=cb(r,g,b),Cr=cr(r,g,b);
            boolean rgb=r>110&&r>g&&g>=b&&(r-b)>=50;
            boolean chroma=Y>Cb&&Cr>Cb&&(Cr-Cb)>=40;
            boolean relative=Y>=meanY&&Cb<=meanCb&&Cr>=meanCr;
            if(rgb&&chroma&&relative){
                candidate++;
                if(Y>=meanY+15&&r>=150)bright++;
                if(xx<minX)minX=xx;if(xx>maxX)maxX=xx;if(yy<minY)minY=yy;if(yy>maxY)maxY=yy;
            }
        }
        double ratio=candidate/(double)n,brightRatio=bright/(double)n;
        // Smooth saturation avoids a hard frame-area threshold. Values are evidence scores,
        // not empirical probabilities.
        double colorEvidence=1.0-Math.exp(-ratio/0.035);
        double brightEvidence=1.0-Math.exp(-brightRatio/0.020);
        double score=clamp01(0.72*colorEvidence+0.28*brightEvidence);
        if(candidate==0)return new Result(score,ratio,brightRatio,0,n,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        return new Result(score,ratio,brightRatio,candidate,n,minX/(double)width,minY/(double)height,(maxX+1)/(double)width,(maxY+1)/(double)height);
    }

    private static Result empty(){return new Result(0,0,0,0,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN);}
    private static double clamp01(double x){return x<0?0:x>1?1:x;}
    private static double y(int r,int g,int b){return 0.299*r+0.587*g+0.114*b;}
    private static double cb(int r,int g,int b){return 128.0-0.168736*r-0.331264*g+0.5*b;}
    private static double cr(int r,int g,int b){return 128.0+0.5*r-0.418688*g-0.081312*b;}
}
