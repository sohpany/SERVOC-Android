package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guard for HR069 app-side video effects; no ESP-CAM or screen-capture coupling. */
public class VideoEffectsContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void effectsRemainGpuPresentationOnly() throws Exception {
        String view=source("src/main/java/com/servoc/app/camera/MjpegView.java");
        String renderer=source("src/main/java/com/servoc/app/camera/VideoEffectRenderer.java");
        String shader=source("src/main/java/com/servoc/app/camera/VideoEffectShader.java");
        assertTrue(view.contains("GLSurfaceView.RENDERMODE_WHEN_DIRTY"));
        assertTrue(renderer.contains("GLUtils.texImage2D"));
        assertTrue(shader.contains("uniform sampler2D uTexture"));
        assertFalse(view.contains("MediaProjection"));
        assertFalse(renderer.contains("MediaProjection"));
    }

    @Test public void detailModeIsBoundedEdgeAdaptive2xEnhancement() throws Exception {
        String profile=source("src/main/java/com/servoc/app/camera/VideoEffectProfile.java");
        String shader=source("src/main/java/com/servoc/app/camera/VideoEffectShader.java");
        assertTrue(profile.contains("DETAIL(\"detail\",\"تحسين الدقة 2×\",true)"));
        assertTrue(shader.contains("uEnhancement==7"));
        assertTrue(shader.contains("lap=c*4.0-(n+s+e+w)"));
        assertTrue(shader.contains("smoothstep(.02,.16,edge)"));
        assertTrue(shader.contains("mn-vec3(.025)"));
        assertTrue(shader.contains("mx+vec3(.025)"));
    }

    @Test public void autoEnhanceAndDirectStreamStayLatencyBounded() throws Exception {
        String view=source("src/main/java/com/servoc/app/camera/MjpegView.java");
        assertTrue(view.contains("AUTO_SAMPLE_INTERVAL_MS=2000"));
        assertTrue(view.contains("new BufferedInputStream"));
        assertTrue(view.contains("latestJpeg=jpeg"));
        assertTrue(view.contains("if(decodeBusy)return"));
    }

    @Test public void recorderConsumesRenderedSurfacePathOnly() throws Exception {
        String control=source("src/main/java/com/servoc/app/ui/ControlTab.java");
        String recorder=source("src/main/java/com/servoc/app/camera/MjpegRecorder.java");
        String renderer=source("src/main/java/com/servoc/app/camera/VideoEffectRenderer.java");
        assertTrue(control.contains("video.setFrameListener(flameAnalyzer::onJpegFrame)"));
        assertFalse(control.contains("video.setDecodedFrameListener(recorder::onDecodedFrame)"));
        assertFalse(control.contains("recorder.onJpegFrame"));
        assertTrue(recorder.contains("VideoEffectFrameHub.set(this)"));
        assertTrue(recorder.contains("VideoEffectFrameHub.clear(this)"));
        assertTrue(renderer.contains("VideoEffectFrameHub.get()"));
    }

    @Test public void advancedEffectsFailSoftToPassthroughInsteadOfBreakingStream() throws Exception {
        String renderer=source("src/main/java/com/servoc/app/camera/VideoEffectRenderer.java");
        assertTrue(renderer.contains("FALLBACK_FRAGMENT"));
        assertTrue(renderer.contains("catch(RuntimeException advancedFailure)"));
        assertTrue(renderer.contains("advancedEffectsAvailable=false"));
        assertTrue(renderer.contains("texture2D(uTexture,uv)"));
    }

    @Test public void noScreenCapturePermissionOrApiIsIntroduced() throws Exception {
        String manifest=source("src/main/AndroidManifest.xml");
        String control=source("src/main/java/com/servoc/app/ui/ControlTab.java");
        assertFalse(manifest.contains("CAPTURE_VIDEO_OUTPUT"));
        assertFalse(control.contains("MediaProjection"));
    }
}
