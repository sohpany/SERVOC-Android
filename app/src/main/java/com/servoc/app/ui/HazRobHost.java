package com.servoc.app.ui;

import android.content.SharedPreferences;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;

/** Contract shared by independent HazRob UI tabs. */
public interface HazRobHost {
    RobotApi robotApi();
    SharedPreferences preferences();
    ExperimentsDb database();
    ExecutorService io();
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
    void setFlashMode(String mode);
    String flashMode();
    void requestExport(String format,long experimentId);
    void requestReferenceBankExport(long experimentId);
    void refreshAllTabs();
}
