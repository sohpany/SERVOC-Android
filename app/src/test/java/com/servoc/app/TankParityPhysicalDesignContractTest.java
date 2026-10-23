package com.servoc.app;
import org.junit.Test;import java.nio.file.*;import java.nio.charset.*;import static org.junit.Assert.*;
public final class TankParityPhysicalDesignContractTest {
 private static String read(String p)throws Exception{return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/ui/"+p)),StandardCharsets.UTF_8);}
 @Test public void lightTableVisualParityIsScopedToPhysicalPane()throws Exception{
  String pane=read("MeasurementsTab.java"),ui=read("PhysicalMeasurementsUiEnhancer.java");
  assertTrue(pane.contains("view.setBackgroundColor(0xFFF7F8FA)"));
  assertTrue(pane.contains("0xFFF1F3F6:0xFFFFFFFF"));
  assertTrue(pane.contains("0xFFFFD36A"));
  assertTrue(ui.contains("WEIGHTS={0.30f,0.18f,0.16f,0.36f}"));
  assertTrue(ui.contains("tv.setTextColor(0xFF263238)"));
  assertTrue(ui.contains("REFRESH_MS=1000L"));
  assertFalse(pane.contains("values[5].setText("));
 }
}