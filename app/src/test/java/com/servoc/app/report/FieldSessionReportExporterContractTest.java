package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards exact-ID binding and the evidence-driven field-session PDF/XLSX report contract. */
public class FieldSessionReportExporterContractTest {
 @Test public void reportRootIsFieldSessionAndResolutionIsExact() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("measurementSessionIds(fs)"));
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("SessionSeries.nearest"));
  assertFalse(s.contains("ORDER BY ABS"));
 }

 @Test public void siteIdentityIsVisibleThroughoutSessionReporting() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  String x=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("{\"الموقع\",b.site.name}"));
  assertTrue(s.contains("{\"Site ID\",b.site.id}"));
  assertTrue(s.contains("b.site.name+\" [\"+shorten(b.site.id,18)+\"]"));
  assertTrue(x.contains("g.header(\"Site\",\"Site ID\",\"Field Session\",\"Field Session ID\")"));
  assertTrue(x.contains("g.row(b.site.name,b.site.id,b.fieldSession.name,b.fieldSession.id)"));
 }

 @Test public void pdfIsSmartCompactA4LandscapeFourToFivePages() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("PAGE_WIDTH=842"));assertTrue(s.contains("PAGE_HEIGHT=595"));assertTrue(s.contains("MIN_PAGES=4"));assertTrue(s.contains("MAX_PAGES=5"));
  assertTrue(s.contains("ReportIntelligence.plan"));assertTrue(s.contains("plan.pageCount"));assertTrue(s.contains("new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n)"));
  assertTrue(s.contains("screening144ReliabilityMatrix"));assertTrue(s.contains("top144SessionSupport"));assertTrue(s.contains("candidateEvidenceMatrix"));assertTrue(s.contains("quantitativeInterval"));
 }

 @Test public void candidateHazardContextUsesExposureLibraryButKeepsEstimatedPpmNonReportable() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  String h=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/CandidateHazardContext.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("CandidateHazardContext.evaluate(db,b.primaryDecision)"));
  assertTrue(s.contains("CandidateHazardCharts.table"));
  assertTrue(h.contains("SCREENING_FIT_ESTIMATE"));
  assertTrue(h.contains("SCREENING_CONTEXT_ONLY_NO_TEMPORAL_EXPOSURE_CLAIM"));
  assertTrue(h.contains("SCREENING_CONTEXT_ONLY_NO_FLAMMABILITY_ALARM"));
  assertTrue(h.contains("new ExposureRepository(db)"));
  assertTrue(h.contains("M4_MEASURED_REPORTABLE"));
  assertTrue(h.contains("compliance_determination"));
 }

 @Test public void trackingIsOperationalAndExcludedFromReportContent() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  String x=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("وظيفة Tracking التشغيلية غير مدرجة في التقرير"));
  assertTrue(x.contains("Tracking وظيفة تشغيلية وغير مدرجة في التقرير"));
 }

 @Test public void smartXlsxIsSingleDetailedWorkbookAndPreservesIndependentMeasurementSessions() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  String x=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("SmartFieldSessionXlsxWriter.write(out,db,b,chemicals)"));
  assertFalse(s.contains("new ZipOutputStream(out)"));
  assertTrue(x.contains("السلسلة الزمنية — لا يتم دمج الجلسات الفنية في منحنى مصطنع"));
  assertTrue(x.contains("CandidateHazardContext.evaluate(db,cc)"));
  assertTrue(x.contains("مصفوفة 144"));
  assertTrue(x.contains("حدود التعرض"));
  assertTrue(x.contains("الجودة وعدم اليقين"));
  assertTrue(x.contains("المنهجية والمصدر"));
 }
}
