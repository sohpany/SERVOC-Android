package com.servoc.app.ui;

import android.content.SharedPreferences;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.measurement.SensorHealthEngine;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;

/** Contract shared by independent HazRob UI tabs. */
public interface HazRobHost {
    RobotApi robotApi();
    SharedPreferences preferences();
    ExperimentsDb database();
    ExecutorService io();
    String operatorRole();
    JSONObject latestDiagnostics();
    void setLatestDiagnostics(JSONObject value);
    void showToast(String text);
    void emergencyStop();
    void setDriveCommand(int left,int right);
    int driveLeft();
    int driveRight();
    void setCameraHost(String host);
    String cameraHost();
    boolean cameraReachable();
    JSONObject cameraDiagnostics();
    void setFlashMode(String mode);
    String flashMode();
    SensorHealthEngine.Snapshot sensorHealth(String key);
    int captureCleanAirBaseline();
    int evaluateCleanAirDrift();
    String cleanAirBaselineTimestamp();
    /** Exact measurement-session identity completed by TelemetryRecorder. */
    String lastMeasurementSessionId();
    void setTrackingTarget(int chemicalId,String name);
    int trackingTargetId();
    String trackingTargetName();
    void requestExport(String format,long experimentId);
    void requestRawExport(long experimentId);
    void requestReferenceBankExport(long experimentId);
    void refreshAllTabs();
}
