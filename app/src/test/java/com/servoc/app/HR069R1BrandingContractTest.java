package com.servoc.app;

import org.junit.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.Assert.*;

/** HR069-R1 launcher, splash and report branding must use the same 1024px PNG source. */
public final class HR069R1BrandingContractTest {
    @Test public void launcherAndFirstScreenUseNewTransparentLogo()throws Exception{
        String manifest=new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")),StandardCharsets.UTF_8);
        String gate=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/auth/ConnectionGate.java")),StandardCharsets.UTF_8);
        assertTrue(manifest.contains("android:icon=\"@drawable/ic_hazrob_hr069_r1\""));
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/ic_hazrob_hr069_r1\""));
        assertTrue(gate.contains("R.drawable.ic_hazrob_hr069_r1"));
        assertTrue(gate.contains("ImageView.ScaleType.CENTER_INSIDE"));
    }

    @Test public void pngIsHighResolutionAndOutsideIsTransparent()throws Exception{
        File f=new File("src/main/res/drawable/ic_hazrob_hr069_r1.png");
        assertTrue("branding generator failed to produce PNG",f.isFile());
        BufferedImage icon=ImageIO.read(f);
        assertNotNull(icon);
        assertEquals(1024,icon.getWidth());
        assertEquals(1024,icon.getHeight());
        assertEquals(0,icon.getRGB(0,0)>>>24);
        assertEquals(255,icon.getRGB(512,512)>>>24);
        assertEquals(0,icon.getRGB(255,420)&0xffffff); // inner hex background stays black
    }

    @Test public void preBuildBrandingCannotMutateLegacyPdfOrXlsxSources()throws Exception{
        String script=new String(Files.readAllBytes(Paths.get("../tools/apply_hazrob_branding.py")),StandardCharsets.UTF_8);
        assertTrue(script.contains("render_logo(1024)"));
        assertTrue(script.contains("ic_hazrob_hr069_r1.png"));
        assertFalse(script.contains("ReportExporter.java';s="));
        assertFalse(script.contains("XlsxReportWriter.java';s="));
    }
}
