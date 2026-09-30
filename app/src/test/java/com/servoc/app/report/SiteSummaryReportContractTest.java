package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static contract for the approved Site -> Field Sessions intelligent reporting hierarchy. */
public class SiteSummaryReportContractTest {
 @Test public void siteReportSummarizesAllChildFieldSessionsWithoutPoolingPpm() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SiteSummaryReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("sessionsForSite(id)"));
  assertTrue(s.contains("FieldSessionReportExporter.resolve(db,fs.id)"));
  assertTrue(s.contains("SiteReportIntelligence.plan"));
  assertTrue(s.contains("PAGE_WIDTH=842"));assertTrue(s.contains("PAGE_HEIGHT=595"));
  assertTrue(s.contains("PPM يبقى مرتبطًا بكل جلسة"));
  assertFalse(s.contains("averagePpmAcrossSessions"));
 }

 @Test public void siteReportContains144RecurrenceAndCandidateHazardPriority() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SiteSummaryReportExporter.java")),StandardCharsets.UTF_8);
  String i=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SiteReportIntelligence.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("SiteReportCharts.recurrence144"));
  assertTrue(s.contains("SiteReportCharts.topRecurrence"));
  assertTrue(s.contains("CandidateHazardContext.evaluate"));
  assertTrue(s.contains("priorityHazard"));
  assertTrue(i.contains("supportedSessions"));
  assertTrue(i.contains("acceptedSessions"));
  assertTrue(i.contains("quantifiedSessions"));
  assertTrue(i.contains("medianSupportPct"));
 }

 @Test public void trackingDoesNotEnterSiteReport() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/SiteSummaryReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("ولا Tracking"));
  assertFalse(s.contains("Tracking history"));
  assertFalse(s.contains("trackedCandidate"));
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
