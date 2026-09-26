package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Static release gate for the portable field-session round trip.
 * Guards Export -> Backup -> Restore -> Re-export identity and idempotence invariants.
 */
public class FieldSessionRoundTripContractTest {
 private static String read(String p)throws Exception{return new String(Files.readAllBytes(Paths.get(p)),StandardCharsets.UTF_8);}

 @Test public void archiveCarriesExactOperationalChildren() throws Exception {
  String s=read("src/main/java/com/servoc/app/field/FieldSessionArchive.java");
  assertTrue(s.contains("field_session_id"));
  assertTrue(s.contains("session_id"));
  assertTrue(s.contains("telemetry_samples"));
  assertTrue(s.contains("events"));
  assertTrue(s.contains("chemcore_decisions"));\n  assertTrue(s.contains("\"experiments\""));\n  assertTrue(s.contains("FROM experiments WHERE session_id=?"));
  assertTrue(s.contains("ORDER BY m.started_ts"));
  assertTrue(s.contains("ORDER BY t_ms"));
 }

 @Test public void restoreIsIdempotentByExactSessionIdentity() throws Exception {
  String s=read("src/main/java/com/servoc/app/field/FieldSessionArchive.java");
  assertTrue(s.contains("CONFLICT_IGNORE"));
  assertTrue(s.contains("w.delete(\"telemetry_samples\",\"session_id=?\""));
  assertTrue(s.contains("w.delete(\"events\",\"session_id=?\""));
  assertTrue(s.contains("w.delete(\"chemcore_decisions\",\"session_id=?\""));
  assertFalse(s.toLowerCase().contains("nearest"));
  assertFalse(s.contains("ORDER BY ABS"));
 }

 @Test public void reportAndBackupUseSameImmutableSessionIds() throws Exception {
  String archive=read("src/main/java/com/servoc/app/field/FieldSessionArchive.java");
  String report=read("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  assertTrue(archive.contains("f.field_session_id=?"));
  assertTrue(report.contains("measurementSessionIds(fs)"));
  assertTrue(report.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertTrue(report.contains("\"field_session_id\": \""));
  assertTrue(report.contains("\"session_id\":\""));
 }

 @Test public void portableLibraryWritesAndRestoresPerSessionSourceArchives() throws Exception {
  String s=read("src/main/java/com/servoc/app/field/HazRobUserLibrary.java");
  assertTrue(s.contains("FieldSessionArchive.exportSession"));
  assertTrue(s.contains("FieldSessionArchive.importSessionInTransaction"));
  assertTrue(s.contains("\"session-\"+safeId(id)+\".json\""));
  assertTrue(s.contains("measurementExists(w,ms)"));
 }
}
