package com.servoc.app.data;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class FieldSchemaContractTest {
    @Test public void v9MigrationIsAdditiveAndKeepsScientificTables() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/ExperimentsDb.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("DB_VERSION=10"));
        assertTrue(s.contains("if(oldVersion<9)createV9UserFieldTables(db)"));
        assertTrue(s.contains("if(oldVersion<10)createV10FieldApprovalTable(db)"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS field_session_approvals"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS sites"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS field_sessions"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS field_session_measurements"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS calibration_curves"));
        assertTrue(s.contains("CREATE TABLE IF NOT EXISTS reference_artifacts"));
        assertFalse(s.contains("DROP TABLE"));
    }
}
