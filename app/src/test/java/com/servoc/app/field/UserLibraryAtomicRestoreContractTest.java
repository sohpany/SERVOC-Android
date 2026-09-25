package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class UserLibraryAtomicRestoreContractTest {
 private static String read(String p)throws Exception{return new String(Files.readAllBytes(Paths.get(p)),StandardCharsets.UTF_8);}
 @Test public void completeUserRestoreUsesOneCallerOwnedTransaction()throws Exception{
  String s=read("src/main/java/com/servoc/app/field/HazRobUserLibrary.java");
  int a=s.indexOf("public synchronized RestoreSummary restoreUserIndex");
  int b=s.indexOf("private static String safeId",a);
  assertTrue(a>=0&&b>a);
  String q=s.substring(a,b);
  assertEquals(1,count(q,"beginTransaction()"));
  assertEquals(1,count(q,"setTransactionSuccessful()"));
  assertEquals(1,count(q,"endTransaction()"));
  assertTrue(q.contains("importSessionInTransaction(db,archive,w)"));
  assertFalse(q.contains("Re-evaluate exact links"));
 }
 @Test public void archiveSupportsCallerTransactionWithoutNestedTransaction()throws Exception{
  String s=read("src/main/java/com/servoc/app/field/FieldSessionArchive.java");
  int a=s.indexOf("static ImportSummary importSessionInTransaction");
  int b=s.indexOf("private static int restoreExperiments",a);
  assertTrue(a>=0&&b>a);
  String q=s.substring(a,b);
  assertFalse(q.contains("beginTransaction()"));
  assertFalse(q.contains("setTransactionSuccessful()"));
  assertFalse(q.contains("endTransaction()"));
 }
 private static int count(String s,String q){int n=0,p=0;while((p=s.indexOf(q,p))>=0){n++;p+=q.length();}return n;}
}
