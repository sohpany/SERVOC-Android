package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class FieldArchiveReferenceIsolationContractTest {
 @Test public void fieldRestoreRejectsReferenceExperiments()throws Exception{
  String s=read();
  assertTrue(s.contains("field archive contains reference experiment"));
  assertTrue(s.contains("x.optInt(\"approved\",0)!=0"));
  assertTrue(s.contains("x.optString(\"approved_ts\",\"\")"));
  assertTrue(s.contains("x.optString(\"reference_name\",\"\")"));
  assertTrue(s.contains("x.optString(\"reference_kind\",\"\")"));
  assertTrue(s.contains("v.put(\"approved\",0)"));
 }
 @Test public void restoredFieldExperimentDoesNotRecreateReferenceMetadata()throws Exception{
  String s=read();int a=s.indexOf("private static int restoreExperiments"),b=s.indexOf("private static int restoreSamples",a);
  assertTrue(a>=0&&b>a);String q=s.substring(a,b);
  assertFalse(q.contains("putNullable(v,\"approved_ts\",x)"));
  assertFalse(q.contains("putNullable(v,\"reference_name\",x)"));
  assertFalse(q.contains("putNullable(v,\"reference_kind\",x)"));
 }
 private static String read()throws Exception{return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/FieldSessionArchive.java")),StandardCharsets.UTF_8);}
}
