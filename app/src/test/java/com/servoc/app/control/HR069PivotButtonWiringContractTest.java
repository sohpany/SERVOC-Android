package com.servoc.app.control;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * HR069 field correction: only the two on-screen rotation buttons change polarity.
 * All other directional commands, including turning while moving, keep their mapping.
 */
public final class HR069PivotButtonWiringContractTest {
    @Test public void onlyPivotButtonIntentsAreSwappedOnScreen() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/control/DirectionPadView.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("add(DiscreteDrive.Intent.PIVOT_RIGHT,\"↺\",1,0)"));
        assertTrue(s.contains("add(DiscreteDrive.Intent.PIVOT_LEFT,\"↻\",1,2)"));
        assertTrue(s.contains("add(DiscreteDrive.Intent.FORWARD_LEFT,\"↖\",0,0)"));
        assertTrue(s.contains("add(DiscreteDrive.Intent.FORWARD_RIGHT,\"↗\",0,2)"));
        assertTrue(s.contains("add(DiscreteDrive.Intent.REVERSE_LEFT,\"↙\",2,0)"));
        assertTrue(s.contains("add(DiscreteDrive.Intent.REVERSE_RIGHT,\"↘\",2,2)"));
        assertTrue(s.contains("emit(DiscreteDrive.Intent.STOP,false)"));
    }
}
