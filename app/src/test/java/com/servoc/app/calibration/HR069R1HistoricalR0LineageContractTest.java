package com.servoc.app.calibration;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/** HR069-R1: every historical reader must use the frozen sample R0, never a new phone calibration. */
public final class HR069R1HistoricalR0LineageContractTest {
    private static String code(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get(name)),StandardCharsets.UTF_8);
    }

    @Test public void chemCoreFeatureReplayCannotRecomputeAgainstCurrentR0()throws Exception{
        String features=code("src/main/java/com/servoc/app/chemcore/ChemCoreSessionFeatureAdapter.java");
        assertTrue(features.contains("AcceptedR0Normalizer.recordedRatio(key,q)"));
        assertTrue(features.contains("AcceptedR0Normalizer.availableRecordedRatio(key,q)"));
        assertTrue(features.contains("AcceptedR0Normalizer.usesRecordedDeviceR0(key,q)"));
        assertFalse(features.contains("AcceptedR0Normalizer.ratio(key,q)"));
        assertFalse(features.contains("AcceptedR0Normalizer.availableRatio(key,q)"));
    }

    @Test public void smartReportsCannotOverrideHistoricalCalibrationMetadata()throws Exception{
        String series=code("src/main/java/com/servoc/app/data/SessionSeries.java");
        assertTrue(series.contains("AcceptedR0Normalizer.availableRecordedRatio(hw.key,q)"));
        assertTrue(series.contains("historical_calibration_frozen"));
        assertTrue(series.contains("persisted_rs_r0"));
        assertFalse(series.contains("q.put(\"r0\",FieldR0Baseline.value("));
        assertFalse(series.contains("q.put(\"r0_source\",AcceptedR0Normalizer.SOURCE)"));
    }

    @Test public void validatedQuantitativeReplayUsesRecordedR0()throws Exception{
        String exposure=code("src/main/java/com/servoc/app/exposure/ChemCoreQuantitativeTimeSeries.java");
        assertTrue(exposure.contains("AcceptedR0Normalizer.recordedRatio(key,q)"));
        assertFalse(exposure.contains("AcceptedR0Normalizer.ratio(key,q)"));
        assertTrue(exposure.contains("m4Only(source)"));
    }

    @Test public void r0EditorLocksOutActiveSessionsAndXlsxAllowsExploratoryResults()throws Exception{
        String r0=code("src/main/java/com/servoc/app/ui/DeveloperR0Tab.java");
        assertTrue(r0.contains("ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())"));
        assertTrue(r0.contains("if(calibrationChangeBlocked())return;"));
        String report=code("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
        assertFalse(report.contains("if(b.records.isEmpty())throw"));
        assertTrue(report.contains("SmartFieldSessionXlsxWriter.write(out,db,b,chemicals)"));
    }
}
