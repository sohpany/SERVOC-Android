package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class ReportExporterMultiplicityContractTest {
 private static String read()throws Exception{return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/report/ReportExporter.java")),StandardCharsets.UTF_8);}
 @Test public void legacyListOverloadsCannotDiscardAdditionalRecords()throws Exception{
  String s=read();
  assertTrue(s.contains("if(rows.size()!=1)throw new IllegalArgumentException(\"field-session exporter required\")"));
  assertEquals(2,count(s,"if(rows.size()!=1)"));
 }
 private static int count(String s,String q){int n=0,p=0;while((p=s.indexOf(q,p))>=0){n++;p+=q.length();}return n;}
}
