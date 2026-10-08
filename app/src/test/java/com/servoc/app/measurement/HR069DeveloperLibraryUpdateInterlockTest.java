package com.servoc.app.measurement;

import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** HR069-R1: a developer capture may be active even when firmware reports idle. */
public final class HR069DeveloperLibraryUpdateInterlockTest {
    @Test public void developerCaptureBlocksStableSnapshotUpdateDuringFirmwareIdle()throws Exception{
        JSONObject idle=new JSONObject().put("measurement",new JSONObject().put("state","idle"));
        assertFalse(ChemCoreLibraryManager.measurementOrAnalysisActive(idle));
        try{
            assertTrue(MeasurementSessionRegistry.requestChemCoreCapture(true));
            assertTrue(ChemCoreLibraryManager.measurementOrAnalysisActive(idle));
            assertTrue(ChemCoreLibraryManager.measurementOrAnalysisActive(null));
        }finally{
            MeasurementSessionRegistry.clearChemCoreCapture();
        }
        assertFalse(ChemCoreLibraryManager.measurementOrAnalysisActive(idle));
    }
    @Test public void firmwareWarmupStillBlocksWithoutDeveloperCapture()throws Exception{
        JSONObject warmup=new JSONObject().put("measurement",new JSONObject().put("state","warmup"));
        assertTrue(ChemCoreLibraryManager.measurementOrAnalysisActive(warmup));
    }
}
