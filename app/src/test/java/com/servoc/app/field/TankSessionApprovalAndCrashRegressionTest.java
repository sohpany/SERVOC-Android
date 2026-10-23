package com.servoc.app.field;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;
/** Tank semantics with RC2 crash containment and source isolation. */
public final class TankSessionApprovalAndCrashRegressionTest {
 private static String src(String path)throws Exception{
  return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
 }
 @Test public void explicitApprovalIsNonDestructiveAndRequiresOneFinalCapture()throws Exception{
  String schema=src("data/ExperimentsDb.java"),r=src("field/FieldSessionRepository.java");
  assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS field_session_approvals"));
  assertTrue(schema.contains("if(!database.isReadOnly())createV9UserFieldTables(database);"));
  assertTrue(r.contains("public boolean canApprove(String fieldSessionId)"));
  assertTrue(r.contains("public boolean approve(String fieldSessionId)"));
  assertTrue(r.contains("c.getInt(0)!=1"));
  assertTrue(r.contains("approvedSessionsForSite(String siteId)"));
  assertTrue(r.contains("public boolean restartUnapproved(String fieldSessionId)"));
 }
 @Test public void twoPaneSessionTableMaintainsCrashContainment()throws Exception{
  String ui=src("ui/FieldSessionsTab.java"),activity=src("MainActivity.java");
  assertTrue(ui.contains("UiKit.button(c,\"الإدارة\")"));
  assertTrue(ui.contains("UiKit.button(c,\"التقارير\")"));
  assertTrue(ui.contains("approved?\"معتمدة\":\"جديدة\""));
  assertTrue(ui.contains("if(refreshing)return;"));
  assertTrue(ui.contains("List<FieldSessionRepository.Site> all=displayedSites"));
  assertTrue(ui.contains("List<FieldSessionRepository.FieldSession> all=displayedSessions"));
  assertTrue(ui.contains("private void renderReportScopes"));
  assertTrue(activity.contains("Session tab navigation failed"));
 }
 @Test public void archivedApprovalsAndExportsRespectStatus()throws Exception{
  String site=src("report/SiteSummaryReportExporter.java");
  String field=src("report/FieldSessionReportExporter.java");
  String reports=src("ui/ReportsTab.java");
  String archive=src("field/FieldSessionArchive.java");
  assertTrue(site.contains("repo.approvedSessionsForSite(id)"));
  assertTrue(field.contains("if(!repo.isApproved(fs))"));
  assertTrue(reports.contains("fieldSessions.approvedSessionsForSite(chosenSiteId)"));
  assertTrue(archive.contains("root.put(\"field_approval\",approval)"));
  assertTrue(archive.contains("w.delete(\"field_session_approvals\""));
  assertTrue(archive.contains("approved session has multiple attempts"));
 }
}
