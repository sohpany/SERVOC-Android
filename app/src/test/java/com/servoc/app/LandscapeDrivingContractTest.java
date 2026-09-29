package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source-level guard for the HR069 landscape driving and lifecycle-safety contract. */
public class LandscapeDrivingContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void manifestRegistersHazRobApplication() throws Exception {
        String manifest=source("src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\".HazRobApplication\""));
    }

    @Test public void landscapeSuppressesTabbedShellButRetainsControlPage() throws Exception {
        String app=source("src/main/java/com/servoc/app/HazRobApplication.java");
        assertTrue(app.contains("Configuration.ORIENTATION_LANDSCAPE"));
        assertTrue(app.contains("root.getChildAt(0).setVisibility(View.GONE)"));
        assertTrue(app.contains("root.getChildAt(1).setVisibility(View.GONE)"));
        assertTrue(app.contains("root.getChildAt(3).setVisibility(View.GONE)"));
        assertTrue(app.contains("box.getChildAt(0).setVisibility(View.GONE)"));
    }

    @Test public void activityPauseNeutralizesDrive() throws Exception {
        String app=source("src/main/java/com/servoc/app/HazRobApplication.java");
        assertTrue(app.contains("setDriveCommand(0,0)"));
        assertTrue(app.contains("onActivityPaused(Activity activity){neutralizeDrive(activity);}"));
    }
}
