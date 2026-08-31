package com.servoc.app.vision;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlameColorModelTest {
    private static int rgb(int r,int g,int b){return 0xFF000000|(r<<16)|(g<<8)|b;}

    @Test public void orangeRegionProducesCandidateEvidence(){
        int w=20,h=20;int[] p=new int[w*h];java.util.Arrays.fill(p,rgb(35,35,35));
        for(int y=6;y<14;y++)for(int x=5;x<13;x++)p[y*w+x]=rgb(255,145,20);
        FlameColorModel.Result r=FlameColorModel.analyze(p,w,h);
        assertTrue(r.candidatePixels>0);assertTrue(r.candidateRatio>0.10);assertTrue(r.score>0.70);assertTrue(r.hasRegion());
        assertTrue(r.left<0.35);assertTrue(r.right>0.60);assertTrue(r.top<0.40);assertTrue(r.bottom>0.60);
    }

    @Test public void blueAndWhiteScenesAreRejected(){
        int w=12,h=12;int[] blue=new int[w*h];java.util.Arrays.fill(blue,rgb(30,80,220));
        FlameColorModel.Result b=FlameColorModel.analyze(blue,w,h);assertEquals(0,b.candidatePixels);assertEquals(0,b.score,1e-12);
        int[] white=new int[w*h];java.util.Arrays.fill(white,rgb(245,245,245));FlameColorModel.Result q=FlameColorModel.analyze(white,w,h);assertEquals(0,q.candidatePixels);assertEquals(0,q.score,1e-12);
    }

    @Test public void invalidFrameIsSafe(){FlameColorModel.Result r=FlameColorModel.analyze(null,0,0);assertEquals(0,r.totalPixels);assertEquals(0,r.score,1e-12);assertFalse(r.hasRegion());}
}
