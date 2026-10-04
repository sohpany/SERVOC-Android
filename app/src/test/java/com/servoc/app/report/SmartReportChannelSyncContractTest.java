package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-level architecture lock: PDF and XLSX may render differently but may not bypass the shared analytical contract. */
public final class SmartReportChannelSyncContractTest {
    private static String read(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/"+name)),StandardCharsets.UTF_8);
    }

    @Test public void fieldFacadeValidatesAllSessionsAndExposesOneContract()throws Exception{
        String s=read("FieldSessionReportExporter.java");
        assertTrue(s.contains("public final SmartReportAnalyticalContract analyticalContract"));
        assertTrue(s.contains("for(String sid:sessions)"));
        assertTrue(s.contains("SmartReportAnalyticalContract.bind(candidateSeries,candidateDecision)"));
        assertTrue(s.contains("SmartReportPrimarySelector.score(candidateSeries,candidateDecision,candidateRecords)"));
        assertTrue(s.contains("SmartReportAnalyticalContract contract=SmartReportAnalyticalContract.bind(series,decision)"));
    }

    @Test public void primarySelectorIsCentralizedInFieldFacade()throws Exception{
        String selector=read("SmartReportPrimarySelector.java");
        String facade=read("FieldSessionReportExporter.java");
        String pdf=read("SmartFieldSessionPdfWriter.java");
        assertTrue(selector.contains("static int score(SessionSeries.Series s, JSONObject d, int records)"));
        assertTrue(selector.contains("\"QUANTIFIED\".equals(o)||d.optBoolean(\"ppm_reportable\",false)"));
        assertTrue(facade.contains("SmartReportPrimarySelector.score(candidateSeries,candidateDecision,candidateRecords)"));
        assertFalse("PDF must not maintain a second scoring implementation",pdf.contains("private static int score("));
    }

    @Test public void smartPdfConsumesExactlyTheFacadeContractAndPrimarySession()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("FieldSessionReportExporter.resolve(db,fieldSessionId)"));
        assertTrue(s.contains("SmartReportAnalyticalContract contract=b.analyticalContract"));
        assertTrue(s.contains("contract.series()"));
        assertTrue(s.contains("contract.decision()"));
        assertTrue(s.contains("b.primaryMeasurementSessionId"));
        assertFalse("PDF may not independently select a second primary session",s.contains("Evidence primary=primary(all)"));
    }

    @Test public void smartPdfRemainsSixChartsPerAnalyticalPage()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("CHARTS_PER_PAGE=6"));
        assertTrue(s.contains("3 columns x 2 rows = 6 charts"));
        assertTrue(s.contains("sensorSix("));
        assertTrue(s.contains("chemCoreSix("));
        assertTrue(s.contains("contextSix("));
    }

    @Test public void smartXlsxConsumesContractAndWritesContractMetadataSheet()throws Exception{
        String s=read("SmartFieldSessionXlsxWriter.java");
        assertTrue(s.contains("SmartReportAnalyticalContract contract=b.analyticalContract"));
        assertTrue(s.contains("ReportIntelligence.plan(contract.series(),cc,b.measurementSessionIds.size())"));
        assertTrue(s.contains("new SmartXlsxPackage.Sheet(\"عقد التحليل\",contractMetadata(b))"));
        assertTrue(s.contains("SmartReport contract"));
        assertTrue(s.contains("ChemCore report contract"));
    }
}
