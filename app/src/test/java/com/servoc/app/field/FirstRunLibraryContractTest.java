package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class FirstRunLibraryContractTest {
    @Test public void firstRunRequiresDurableLibraryBeforeConnectionGate() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/EntryActivity.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("if(userLibrary.attached())showConnection();else showFirstRunLibrarySetup();"));
        assertTrue(s.contains("HazRobUserLibrary.createPickerIntent()"));
        assertTrue(s.contains("userLibrary.attach(uri);showConnection();"));
        assertTrue(s.contains("setCancelable(false)"));
    }
}
