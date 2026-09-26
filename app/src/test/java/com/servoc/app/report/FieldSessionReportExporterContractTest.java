package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards exact-ID field-session report binding; no timestamp fallback is allowed. */
public class FieldSessionReportExporterContractTest {
 @Test public void reportRootIsFieldSessionAndResolutionIsExact() throws Exception {
  String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")),StandardCharsets.UTF_8);
  assertTrue(s.contains("measurementSessionIds(fs)"));
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("SessionSeries.nearest"));
  assertFalse(s.contains("ORDER BY ABS"));
  assertTrue(s.contains("new ZipOutputStream(out)"));
  assertTrue(s.contains("putManifest(zip,b)"));
 }
}
