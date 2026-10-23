package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public final class NoiseOneSecondPresentationContractTest {
    private static String read(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void oneSecondCadenceAndSingleNoiseWriter()throws Exception{
        String enhancer=read("PhysicalMeasurementsUiEnhancer.java");
        String physical=read("MeasurementsTab.java");
        assertTrue(enhancer.contains("REFRESH_MS=1000L"));
        assertTrue(enhancer.contains("postDelayed(this,REFRESH_MS)"));
        assertTrue(enhancer.contains("NOISE_EMA_ALPHA=0.35"));
        assertTrue(enhancer.contains("MicrophoneDbCalibration.compensate(displayDb,offset)"));
        assertTrue(physical.contains("if(i==5)continue;"));
        assertFalse(physical.contains("values[5].setText("));
    }
}
