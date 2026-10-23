package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guard for physical-measurements readability and the HR069 MAX4466 offset policy. */
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

    @Test public void max4466UsesOnePointReferenceMeterOffsetWithoutFixedGainAssumption() throws Exception {
        String calibration=source("app/src/main/java/com/servoc/app/calibration/MicrophoneDbCalibration.java");
        String settings=source("app/src/main/java/com/servoc/app/ui/SettingsTab.java");
        String enhancer=source("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java");
        String protocol=source("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java");
        String firmware=source("firmware/HazRob/Hardware.cpp");

        assertTrue(calibration.contains("PREF_OFFSET_DB=\"microphone_db_offset_db\""));
        assertTrue(calibration.contains("20.0*Math.log10(ratio)"));
        assertTrue(calibration.contains("referenceDb-hazRobRawDb"));
        assertTrue(calibration.contains("raw+offsetDb(p)"));
        assertTrue(settings.contains("تعويض الميكروفون (dB)"));
        assertTrue(settings.contains("قراءة جهاز القياس المعاير − قراءة HazRob الخام"));
        assertTrue(settings.contains("saveMicrophoneOffset()"));
        assertTrue(enhancer.contains("MicrophoneDbCalibration.rawDbFs(sound)"));
        assertTrue(enhancer.contains("MicrophoneDbCalibration.compensate(displayDb,offset)"));
        assertTrue(enhancer.contains("معوض بمرجع خارجي"));
        assertTrue(enhancer.contains("dBFS خام"));

        assertTrue(protocol.contains("x.put(\"db_spl\",db)"));
        assertTrue(firmware.contains("\\\"rms_v\\\":"));
        assertTrue(firmware.contains("\\\"p2p_v\\\":"));
        assertTrue(firmware.contains("\\\"db_spl\\\":null"));
        assertTrue(firmware.contains("\\\"calibrated_db_spl\\\":false"));
        assertTrue(firmware.contains("\\\"gain_reference\\\":null"));
        assertTrue(firmware.contains("trimmer_endpoint_fixed_unquantified"));
        assertFalse(firmware.contains("AMP_GAIN=25.0f"));
        assertFalse(enhancer.contains("25×"));
    }
}
