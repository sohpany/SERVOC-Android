package com.servoc.app.ui;

import android.content.SharedPreferences;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurement.LiveGasStateEngine;
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
    void testPhoneAlert();
    SensorHealthEngine.Snapshot sensorHealth(String key);
    LiveGasStateEngine.Snapshot liveGasState();
    int captureCleanAirBaseline();
    int evaluateCleanAirDrift();
    String cleanAirBaselineTimestamp();
    /** Exact measurement-session identity completed by TelemetryRecorder. */
    String lastMeasurementSessionId();
    void setTrackingTarget(int chemicalId,String name);
    int trackingTargetId();
    String trackingTargetName();
    void requestExport(String format,long experimentId);
    /** Export one complete immutable field session. PDF is one compact 4–5 page landscape report. */
    void requestFieldSessionExport(String format,String fieldSessionId);
    void openReportsForSite(String siteId,String fieldSessionId);
    void deleteUserFieldSession(String fieldSessionId);
    void deleteEmptyUserSite(String siteId);
    void requestRawExport(long experimentId);
    void requestReferenceBankExport(long experimentId);
    boolean userLibraryAttached();
    void requestUserLibraryFolder();
    void syncUserLibrary();
    void restoreUserLibrary();
    void refreshAllTabs();
}
