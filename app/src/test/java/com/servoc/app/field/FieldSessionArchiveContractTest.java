package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class FieldSessionArchiveContractTest {
 @Test public void archiveIsUserFieldOnlyAndCarriesReportSources() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionArchive.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("m.operator_role='USER'"));
  assertTrue(s.contains("m.kind LIKE 'field_%'"));
  assertTrue(s.contains("telemetry_samples"));
  assertTrue(s.contains("events"));
  assertTrue(s.contains("chemcore_decisions"));
  assertFalse(s.contains("chemcore_feature_references"));
  assertFalse(s.contains("calibration_curves"));
  assertFalse(s.contains("reference_queue"));
  assertTrue(s.contains("importSession"));
  assertTrue(s.contains("w.delete(\"telemetry_samples\""));
  assertTrue(s.contains("w.delete(\"chemcore_decisions\""));
 }
 @Test public void librarySyncWritesOneSourceArchivePerFieldSession() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/HazRobUserLibrary.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("FieldSessionArchive.exportSession"));
  assertTrue(s.contains("\"session-\"+safeId(id)+\".json\""));
 }
}
