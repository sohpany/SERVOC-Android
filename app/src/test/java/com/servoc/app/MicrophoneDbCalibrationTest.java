package com.servoc.app;

import com.servoc.app.calibration.MicrophoneDbCalibration;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MicrophoneDbCalibrationTest {
    @Test public void rawDbFsUsesMeasuredRmsAgainstAdcFullScale(){
        assertEquals(0.0,MicrophoneDbCalibration.rawDbFs(3.3),1e-9);
        assertEquals(-6.0205999,MicrophoneDbCalibration.rawDbFs(1.65),1e-5);
        assertTrue(Double.isNaN(MicrophoneDbCalibration.rawDbFs(0.0)));
    }

    @Test public void referenceDifferenceBecomesAdditiveOffset(){
        double raw=-18.0,reference=72.0;
        double offset=MicrophoneDbCalibration.requiredOffset(raw,reference);
        assertEquals(90.0,offset,1e-9);
        assertEquals(reference,MicrophoneDbCalibration.compensate(raw,offset),1e-9);
    }

    @Test public void negativeCorrectionIsSupported(){
        assertEquals(67.0,MicrophoneDbCalibration.compensate(72.0,-5.0),1e-9);
    }

    @Test public void offsetIsBoundedAgainstAccidentalExtremeInput(){
        assertEquals(MicrophoneDbCalibration.MAX_OFFSET_DB,MicrophoneDbCalibration.clamp(999),0.0);
        assertEquals(MicrophoneDbCalibration.MIN_OFFSET_DB,MicrophoneDbCalibration.clamp(-999),0.0);
    }
}
