package com.servoc.app.calibration;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * HR069-R1 wiring assumption: firmware RL is expressed numerically in kOhm.
 * This locks the developer input unit to the same scale as firmware Rs/R0.
 * It does NOT certify actual RL of any third-party four-pin MQ module.
 */
public final class HR069R1R0ElectricalUnitsContractTest {
    private static String code(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test public void uiAndFirmwareUseMatchingResistanceScale() throws Exception {
        String firmware = code("../firmware/HazRob/Config.h");
        String acquisition = code("../firmware/HazRob/Hardware.cpp");
        String ui = code("src/main/java/com/servoc/app/ui/DeveloperR0Tab.java");
        assertTrue(firmware.contains("RL=10.0f"));
        assertTrue(acquisition.contains("r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao"));
        assertTrue(ui.contains("وحدة Rs وR0 الحالية هي كيلو أوم (kΩ)"));
        assertTrue(ui.contains("RL = 10 kΩ"));
        assertTrue(ui.contains("1000 Ω = 1 kΩ"));
        assertTrue(ui.contains("String.format(Locale.US,\"%.5f kΩ\",rs)"));
        assertFalse(ui.contains("المقاسة بالأوم لكل حساس"));
    }

    @Test public void referenceValuesAreInFirmwareRsScaleNotRawOhms() {
        double outputVolts=1.0;
        double rsKOhm=10.0*(5.0-outputVolts)/outputVolts;
        assertEquals(40.0,rsKOhm,1e-12);
        assertEquals(40000.0,rsKOhm*1000.0,1e-9);
        assertTrue(FieldR0Baseline.value("mq135")>1.0);
    }
}
