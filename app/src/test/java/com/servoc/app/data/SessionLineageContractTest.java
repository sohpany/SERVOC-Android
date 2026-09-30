package com.servoc.app.data;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards 0.10.2 / HR069 scientific lineage from acquisition through field reporting. */
public class SessionLineageContractTest {
 @Test public void modernExperimentSeriesRequiresExactSessionId() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/SessionSeries.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("r.sessionId==null||r.sessionId.trim().isEmpty()"));
  assertTrue(s.contains("return byId(db,r.sessionId.trim())"));
  assertTrue(s.contains("@Deprecated"));
  assertTrue(s.contains("public static Series nearest"));
  assertFalse(s.contains("forExperiment(ExperimentsDb db,ExperimentQueries.Record r){\n        if(db==null||r==null)return null;\n        if(r.sessionId!=null&&!r.sessionId.trim().isEmpty())"));
 }

 @Test public void experimentQueriesUseExactSessionForeignKey() throws Exception {
  String q=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/ExperimentQueries.java")),StandardCharsets.UTF_8);
  assertTrue(q.contains("FROM experiments WHERE session_id=? ORDER BY id"));
  assertTrue(q.contains("public final String ts,payload,protocol,analytics,approvedTs,referenceName,sessionId,referenceKind"));
 }

 @Test public void fieldHierarchyOnlyAcceptsUserFieldMeasurements() throws Exception {
  String r=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionRepository.java")),StandardCharsets.UTF_8);
  String p=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionPolicy.java")),StandardCharsets.UTF_8);
  assertTrue(r.contains("FieldSessionPolicy.mayAttachMeasurement(kind,role)"));
  assertTrue(r.contains("field_session_id=? AND status=?"));
  assertTrue(p.contains("\"USER\".equals(r)&&k.startsWith(\"field_\")"));
 }

 @Test public void telemetryCarriesUtcMonotonicAndSessionElapsedTime() throws Exception {
  String t=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/measurement/TelemetryRecorder.java")),StandardCharsets.UTF_8);
  assertTrue(t.contains("SystemClock.elapsedRealtime()"));
  assertTrue(t.contains("sample_utc_ms"));
  assertTrue(t.contains("sample_monotonic_ms"));
  assertTrue(t.contains("session_t_ms"));
  assertTrue(t.contains("started_utc_ms"));
  assertTrue(t.contains("ended_utc_ms"));
  assertTrue(t.contains("started_monotonic_ms"));
  assertTrue(t.contains("ended_monotonic_ms"));
  assertTrue(t.contains("duration_monotonic_ms"));
  assertTrue(t.contains("UTC_EPOCH_MS_PLUS_ANDROID_ELAPSED_REALTIME"));
  assertTrue(t.contains("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
  assertTrue(t.contains("TimeZone.getTimeZone(\"UTC\")"));
  assertTrue(t.contains("if(!developer&&!chemCore)"));
  assertTrue(t.contains("fieldSessions.linkMeasurement(active.id,sessionId)"));
 }

 @Test public void fieldSessionLifecycleCannotDetachOrCloseAroundLiveAcquisition() throws Exception {
  String r=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionRepository.java")),StandardCharsets.UTF_8);
  assertTrue(r.contains("JOIN measurement_sessions ms ON ms.session_id=fsm.measurement_session_id"));
  assertTrue(r.contains("ms.ended_ts IS NULL OR TRIM(ms.ended_ts)=''"));
  assertTrue(r.contains("STATUS_ACTIVE.equals(c.getString(0))"));
  assertTrue(r.contains("ORDER BY linked_ts,measurement_session_id"));
  assertTrue(r.contains("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
 }

 @Test public void r0IsDeviceLocalAndNonPublishable() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/SessionSeries.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("r0_device_local"));
  assertTrue(s.contains("r0_centrally_publishable"));
  assertTrue(s.contains("q.put(\"r0_centrally_publishable\",false)"));
 }

 @Test public void fieldReportResolvesOnlyExplicitLinkedMeasurementSessions() throws Exception {
  String r=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(r.contains("repo.measurementSessionIds(fs)"));
  assertTrue(r.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertTrue(r.contains("SessionSeries.byId(db,primary)"));
  assertFalse(r.contains("SessionSeries.nearest"));
 }
}
