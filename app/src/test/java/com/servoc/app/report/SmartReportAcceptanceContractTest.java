package com.servoc.app.report;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

/** Acceptance locks for the HR069 SmartReport analytical expansion. */
public final class SmartReportAcceptanceContractTest {
    private static String read(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/"+name)),StandardCharsets.UTF_8);
    }
    private static int count(String text,String needle){int n=0,p=0;while((p=text.indexOf(needle,p))>=0){n++;p+=needle.length();}return n;}

    @Test public void fieldPdfTopologyIsCompactLandscapeFourToFivePagesWithSixChartGrid()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=4,MAX_PAGES=5,CHARTS_PER_PAGE=6"));
        assertTrue(s.contains("3 columns x 2 rows = 6 charts per analytical page"));
        assertTrue(s.contains("RectF[] r=new RectF[6]"));
        assertTrue(s.contains("int total=all.size()>1?5:4"));
    }

    @Test public void sensorPageCarriesSixDistinctAnalyticalViews()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("AnalyticalCharts.mqRsR0(c,r[0],s)"));
        assertTrue(s.contains("AnalyticalCharts.fingerprint(c,r[1],s)"));
        assertTrue(s.contains("AnalyticalCharts.mqSignalQuality(c,r[2],s)"));
        assertTrue(s.contains("AnalyticalCharts.fingerprintRadar(c,r[3],s)"));
        assertTrue(s.contains("AnalyticalCharts.mqResponseKinetics(c,r[4],s)"));
        assertTrue(s.contains("AnalyticalCharts.mqPersistenceMap(c,r[5],s)"));
    }

    @Test public void chemCorePageCarriesSixDistinctInferenceViews()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("AnalyticalCharts.candidates(c,r[0],q)"));
        assertTrue(s.contains("AnalyticalCharts.candidateScatter(c,r[1],q)"));
        assertTrue(s.contains("AnalyticalCharts.candidateBubbleMap(c,r[2],q)"));
        assertTrue(s.contains("AnalyticalCharts.candidateResiduals(c,r[3],q)"));
        assertTrue(s.contains("AnalyticalCharts.evidenceTiers(c,r[4],q)"));
        assertTrue(s.contains("SmartReportCharts.decisionQuality(c,r[5],q)"));
    }

    @Test public void contextPageUsesSessionEvidenceAndKeepsQuantitativeExposureBehindM4Gate()throws Exception{
        String s=read("SmartFieldSessionPdfWriter.java");
        assertTrue(s.contains("SmartReportCharts.candidateEvidenceMatrix(c,r[0],q)"));
        assertTrue(s.contains("SmartReportCharts.screening144ReliabilityMatrix(c,r[1],q)"));
        assertTrue(s.contains("SmartReportCharts.top144SessionSupport(c,r[2],q)"));
        assertTrue(s.contains("AnalyticalCharts.screeningDonut(c,r[3],q)"));
        assertTrue(s.contains("if(plan.ppmReportable)AnalyticalCharts.exposureRatios(c,r[5],q)"));
        assertTrue(s.contains("Screening لا يساوي هوية مؤكدة أو PPM؛ M4 وحده يسمح بالقياس الكمي المهني"));
    }

    @Test public void detailedXlsxIsTheSameContractCompanionAndKeepsElevenAuditableSheets()throws Exception{
        String s=read("SmartFieldSessionXlsxWriter.java");
        assertTrue(s.contains("SmartReportAnalyticalContract contract=b.analyticalContract"));
        assertEquals(11,count(s,"new SmartXlsxPackage.Sheet("));
        assertTrue(s.contains("\"الملخص التنفيذي\""));
        assertTrue(s.contains("\"مصفوفة 144\""));
        assertTrue(s.contains("\"الأدلة التحليلية\""));
        assertTrue(s.contains("\"الجودة وعدم اليقين\""));
        assertTrue(s.contains("\"السلسلة الزمنية\""));
        assertTrue(s.contains("\"عقد التحليل\""));
        assertTrue(s.contains("\"المنهجية والمصدر\""));
    }

    @Test public void smartXlsxPackageProducesAValidInspectableWorkbookContainer()throws Exception{
        SmartXlsxPackage.Grid a=new SmartXlsxPackage.Grid(18,22);a.header("البند","القيمة");a.row("Contract","HR069");
        SmartXlsxPackage.Grid b=new SmartXlsxPackage.Grid(18);b.header("Evidence");b.row("M4 gated");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        SmartXlsxPackage.write(out,Arrays.asList(new SmartXlsxPackage.Sheet("ملخص",a.finish()),new SmartXlsxPackage.Sheet("عقد التحليل",b.finish())));
        assertTrue(out.size()>500);
        Set<String> entries=new HashSet<>();
        try(ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))){ZipEntry e;while((e=zin.getNextEntry())!=null){entries.add(e.getName());zin.closeEntry();}}
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("xl/workbook.xml"));
        assertTrue(entries.contains("xl/styles.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet2.xml"));
    }
}
