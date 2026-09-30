package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static contract for the approved Site -> Field Sessions reporting hierarchy. */
public class SiteSummaryReportContractTest {
 @Test public void siteReportSummarizesAllChildFieldSessions() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SiteSummaryReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("sessionsForSite(id)"));
  assertTrue(s.contains("FieldSessionReportExporter.resolve(db,fs.id)"));
  assertTrue(s.contains("PAGE_WIDTH=842"));
  assertTrue(s.contains("PAGE_HEIGHT=595"));
  assertTrue(s.contains("لا يتم متوسط PPM بين الجلسات"));
 }

 @Test public void fieldWorkspaceExposesSessionAndSiteReports() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/FieldSessionsTab.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("تقرير الموقع PDF"));
  assertTrue(s.contains("تقرير الجلسة PDF • 4–5 صفحات أفقية"));
  assertTrue(s.contains("SiteSummaryExportActivity.start(c,selectedSiteId)"));
  assertTrue(s.contains("host.requestFieldSessionExport(\"pdf\",q.id)"));
 }

 @Test public void siteExportUsesDedicatedSafActivityRegisteredInManifest() throws Exception {
  String a=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/SiteSummaryExportActivity.java")),StandardCharsets.UTF_8);
  String m=new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")),StandardCharsets.UTF_8);
  assertTrue(a.contains("Intent.ACTION_CREATE_DOCUMENT"));
  assertTrue(a.contains("application/pdf"));
  assertTrue(a.contains("SiteSummaryReportExporter.writePdf"));
  assertTrue(m.contains(".SiteSummaryExportActivity"));
  assertTrue(m.contains("android:exported=\"false\""));
 }
}
