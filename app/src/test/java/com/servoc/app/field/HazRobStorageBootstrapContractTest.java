package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class HazRobStorageBootstrapContractTest {
    @Test public void bootstrapPrecedesDatabaseAndServices() throws Exception {
        String main=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/MainActivity.java")),StandardCharsets.UTF_8);
        int storage=main.indexOf("HazRobStorageBootstrap.ensure(this)");
        int database=main.indexOf("new ExperimentsDb(this)");
        assertTrue(storage>=0);
        assertTrue(database>storage);
    }
    @Test public void bootstrapUsesScopedStorageNotLegacyPublicPath() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/HazRobStorageBootstrap.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("getExternalFilesDir"));
        assertFalse(s.contains("getExternalStorageDirectory"));
    }
}
