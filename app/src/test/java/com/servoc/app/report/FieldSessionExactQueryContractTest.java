package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class FieldSessionExactQueryContractTest {
 @Test public void exporterUsesCentralExactSessionLookup()throws Exception{
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("rawQuery("));
  assertFalse(s.contains("SELECT id FROM experiments"));
  assertFalse(s.contains("\\n"));
 }
 @Test public void queryHelperIsExactAndOrdered()throws Exception{
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/ExperimentQueries.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("bySessionId(ExperimentsDb db,String sessionId)"));
  assertTrue(s.contains("WHERE session_id=? ORDER BY id"));
  assertFalse(s.toLowerCase().contains("nearest"));
 }
}
