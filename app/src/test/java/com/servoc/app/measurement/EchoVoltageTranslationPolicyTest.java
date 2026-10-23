package com.servoc.app.measurement;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contracts. A real HC-SR04 signal must still be diagnosed electrically. */
public final class EchoVoltageTranslationPolicyTest {
    private static String read(String p)throws Exception{return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+p)),StandardCharsets.UTF_8);}
    @Test public void translatorMustNeverScaleRangeByVoltageRatio()throws Exception{
        String c=read("measurement/EchoReading.java");
        assertTrue(c.contains("CM_PER_MICROSECOND=0.0343/2.0"));
        assertFalse(c.contains("5.0/3.3"));
        assertTrue(c.contains("p.optBoolean(\"valid\",false)"));
        assertTrue(c.contains("rawCm>=2.0&&rawCm<=400.0"));
    }
    @Test public void operatorOffsetIsDisplayOnlyNotSafety()throws Exception{
        String c=read("measurement/EchoReading.java");
        String settings=read("ui/SettingsTab.java");
        String control=read("ui/ControlTab.java");
        assertTrue(settings.contains("saveEchoOffset()"));
        assertTrue(c.contains("PREF_OFFSET_CM=\"echo_display_offset_cm\""));
        assertTrue(control.contains("EchoReading.from(d,host.preferences())"));
        assertTrue(control.contains("if(landscape)actions.addView(relayButton"));
        assertTrue(control.contains("if(!landscape)cameraBox.addView(relayButton"));
    }
}
