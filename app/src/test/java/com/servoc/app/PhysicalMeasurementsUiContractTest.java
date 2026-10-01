package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guard for physical-measurements readability and the current uncalibrated MAX4466 policy. */
public class PhysicalMeasurementsUiContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void responsivePhysicalColumnsAreInstalled() throws Exception {
        String app=source("app/src/main/java/com/servoc/app/HazRobApplication.java");
        String enhancer=source("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java");
        assertTrue(app.contains("PhysicalMeasurementsUiEnhancer.install(activity)"));
        assertTrue(enhancer.contains("setStretchAllColumns(false)"));
        assertTrue(enhancer.contains("setShrinkAllColumns(false)"));
        assertTrue(enhancer.contains("WEIGHTS={0.30f,0.18f,0.16f,0.36f}"));
        assertTrue(enhancer.contains("tv.setSingleLine(true)"));
        assertTrue(enhancer.contains("tv.setMinWidth(dp(44))"));
        assertTrue(enhancer.contains("tv.setTextSize(14)"));
        assertTrue(enhancer.contains("Typeface.DEFAULT,Typeface.BOLD"));
        assertTrue(enhancer.contains("tv.setMaxLines(3)"));
    }

    @Test public void max4466KeepsRawAmplitudeButWithholdsQuantitativeDbUntilCalibration() throws Exception {
        String protocol=source("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java");
        String enhancer=source("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java");
        String measurements=source("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java");
        String firmware=source("firmware/HazRob/Hardware.cpp");

        assertTrue(protocol.contains("x.put(\"db_spl\",db)"));
        assertTrue(protocol.contains("x.put(\"gain_reference\""));
        assertTrue(protocol.contains("x.put(\"calibrated_db_spl\",false)"));
        assertTrue(enhancer.contains("optDouble(\"db_spl\""));
        assertTrue(enhancer.contains("dB SPL غير معاير"));
        assertTrue(measurements.contains("s.optDouble(\"db_spl\""));

        assertTrue(firmware.contains("\\\"rms_v\\\":"));
        assertTrue(firmware.contains("\\\"p2p_v\\\":"));
        assertTrue(firmware.contains("\\\"db_spl\\\":null"));
        assertTrue(firmware.contains("\\\"calibrated_db_spl\\\":false"));
        assertTrue(firmware.contains("\\\"gain_reference\\\":null"));
        assertTrue(firmware.contains("trimmer_endpoint_fixed_unquantified"));
        assertFalse(firmware.contains("AMP_GAIN=25.0f"));
    }
}
