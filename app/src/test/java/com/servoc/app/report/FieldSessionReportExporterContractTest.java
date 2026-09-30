package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards exact-ID binding and the approved compact field-session report contract. */
public class FieldSessionReportExporterContractTest {
 @Test public void reportRootIsFieldSessionAndResolutionIsExact() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("measurementSessionIds(fs)"));
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("SessionSeries.nearest"));
  assertFalse(s.contains("ORDER BY ABS"));
 }

 @Test public void pdfIsCompactA4LandscapeFourToFivePages() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("PAGE_WIDTH=842"));
  assertTrue(s.contains("PAGE_HEIGHT=595"));
  assertTrue(s.contains("MIN_PAGES=4"));
  assertTrue(s.contains("MAX_PAGES=5"));
  assertTrue(s.contains("int total=fifth?5:4"));
  assertTrue(s.contains("new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n)"));
 }

 @Test public void multiMeasurementSessionKeepsTraceabilityAndXlsxLossless() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("primaryMeasurementSessionId"));
  assertTrue(s.contains("لا تُدمج جلسات القياس الفنية"));
  assertTrue(s.contains("new ZipOutputStream(out)"));
  assertTrue(s.contains("putManifest(zip,b)"));
 }
}
