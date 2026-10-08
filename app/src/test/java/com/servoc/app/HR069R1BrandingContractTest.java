package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.InflaterInputStream;
import static org.junit.Assert.*;

/** HR069-R1: transparent PNG must not require desktop java.awt in Android unit tests. */
public final class HR069R1BrandingContractTest {
    private static int be32(byte[] x,int off){
        return ((x[off]&255)<<24)|((x[off+1]&255)<<16)|((x[off+2]&255)<<8)|(x[off+3]&255);
    }

    @Test public void launcherAndConnectionUseNewLogo()throws Exception{
        String manifest=new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")),StandardCharsets.UTF_8);
        String gate=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/auth/ConnectionGate.java")),StandardCharsets.UTF_8);
        assertTrue(manifest.contains("android:icon=\"@drawable/ic_hazrob_hr069_r1\""));
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/ic_hazrob_hr069_r1\""));
        assertTrue(gate.contains("R.drawable.ic_hazrob_hr069_r1"));
        assertTrue(gate.contains("ImageView.ScaleType.CENTER_INSIDE"));
    }

    @Test public void pngIs1024RgbaWithTransparentOutsideAndBlackHexCenter()throws Exception{
        byte[] b=Files.readAllBytes(Paths.get("src/main/res/drawable/ic_hazrob_hr069_r1.png"));
        assertTrue(b.length>1000);
        assertArrayEquals(new byte[]{(byte)137,80,78,71,13,10,26,10},java.util.Arrays.copyOf(b,8));
        assertEquals(1024,be32(b,16));
        assertEquals(1024,be32(b,20));
        assertEquals(8,b[24]&255);assertEquals(6,b[25]&255); // RGBA/8
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        for(int i=8;i+12<=b.length;){
            int n=be32(b,i);assertTrue(n>=0&&i+12L+n<=b.length);
            String type=new String(b,i+4,4,StandardCharsets.US_ASCII);
            if("IDAT".equals(type))compressed.write(b,i+8,n);
            i+=n+12;
            if("IEND".equals(type))break;
        }
        ByteArrayOutputStream raw=new ByteArrayOutputStream();
        try(InflaterInputStream in=new InflaterInputStream(new ByteArrayInputStream(compressed.toByteArray()))){
            byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)raw.write(buf,0,n);
        }
        byte[] scan=raw.toByteArray();
        int stride=1+1024*4;
        assertEquals(stride*1024,scan.length);
        assertEquals(0,scan[0]&255); // PNG filter type NONE
        assertEquals(0,scan[1+3]&255); // outside hex: alpha=0
        assertEquals(255,scan[512*stride+1+512*4+3]&255); // central cross opaque
        int p=420*stride+1+255*4;
        assertEquals(0,scan[p]&255);assertEquals(0,scan[p+1]&255);assertEquals(0,scan[p+2]&255);
        assertEquals(255,scan[p+3]&255); // black inside hex opaque
    }

    @Test public void brandBuildStepNeverRewritesReportSources()throws Exception{
        String script=new String(Files.readAllBytes(Paths.get("../tools/apply_hazrob_branding.py")),StandardCharsets.UTF_8);
        assertTrue(script.contains("render_logo(1024)"));
        assertTrue(script.contains("ic_hazrob_hr069_r1.png"));
        assertFalse(script.contains("ReportExporter.java';s="));
        assertFalse(script.contains("XlsxReportWriter.java';s="));
    }
}
