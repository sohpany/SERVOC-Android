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
        assertTrue(s.contains("SmartReportAnalyticalContract.bind(SessionSeries.byId(db,id),decisions.latestPayloadForSession(id))"));
        assertTrue(s.contains("SmartReportAnalyticalContract contract=SmartReportAnalyticalContract.bind(series,decision)"));
    }

    @Test public void smartPdfRemainsSixChartsPerAnalyticalPageAndPassesFacadeGuard()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("CHARTS_PER_PAGE=6"));
        assertTrue(s.contains("FieldSessionReportExporter.resolve(db,fieldSessionId)"));
        assertTrue(s.contains("3 columns x 2 rows = 6 charts"));
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
