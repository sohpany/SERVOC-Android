package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.camera.MjpegView;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.control.JoystickView;

import org.json.JSONObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers = Executors.newScheduledThreadPool(3);
    private SharedPreferences prefs;
    private RobotApi api;
    private MjpegView video;
    private JoystickView joystick;
    private TextView status;
    private EditText robotIp;
    private EditText cameraIp;
    private volatile int left = 0, right = 0;
    private volatile boolean resumed = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("servoc", MODE_PRIVATE);
        api = new RobotApi(prefs.getString("robot", "192.168.4.1"));
        setContentView(buildUi());
        schedule();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        startCamera();
    }

    @Override protected void onPause() {
        hardStop();
        resumed = false;
        if (video != null) video.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        workers.shutdownNow();
        if (video != null) video.stop();
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout root = vbox();
        root.setBackgroundColor(0xFF101010);
        root.setPadding(dp(8), dp(8), dp(8), dp(12));

        TextView title = label("SERVOC CONTROL • v0.2");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = label("DISCONNECTED");
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout videoFrame = new FrameLayout(this);
        videoFrame.setBackgroundColor(Color.BLACK);
        root.addView(videoFrame, new LinearLayout.LayoutParams(-1, dp(310)));

        video = new MjpegView(this);
        videoFrame.addView(video, new FrameLayout.LayoutParams(-1, -1));

        joystick = new JoystickView(this);
        FrameLayout.LayoutParams joyParams = new FrameLayout.LayoutParams(dp(135), dp(135), Gravity.END | Gravity.BOTTOM);
        joyParams.setMargins(0, 0, dp(8), dp(8));
        videoFrame.addView(joystick, joyParams);
        joystick.setListener((l, r, active) -> {
            left = l; right = r;
            if (!active) { left = right = 0; sendDriveNow(); }
        });

        LinearLayout settings = vbox();
        settings.setPadding(dp(8), dp(8), dp(8), dp(8));
        settings.setBackgroundColor(0xFF1B1B1B);
        robotIp = input(prefs.getString("robot", "192.168.4.1"));
        cameraIp = input(prefs.getString("camera", "192.168.4.2"));
        settings.addView(label("Robot IP")); settings.addView(robotIp, new LinearLayout.LayoutParams(-1, -2));
        settings.addView(label("Camera IP")); settings.addView(cameraIp, new LinearLayout.LayoutParams(-1, -2));
        Button apply = new Button(this);
        apply.setText("Apply connection settings");
        apply.setAllCaps(false);
        apply.setOnClickListener(v -> applyNetwork());
        settings.addView(apply, new LinearLayout.LayoutParams(-1, -2));
        root.addView(settings, new LinearLayout.LayoutParams(-1, -2));

        Button analysis = new Button(this);
        analysis.setText("Open OFFLINE Analysis & Libraries");
        analysis.setAllCaps(false);
        analysis.setOnClickListener(v -> startActivity(new Intent(this, AnalysisActivity.class)));
        root.addView(analysis, new LinearLayout.LayoutParams(-1, -2));

        TextView note = label("ESP32 retains the real-time failsafe. The app sends drive heartbeat commands every 250 ms while active. Analytical libraries can now be tested without robot hardware.");
        note.setTextColor(0xFFBBBBBB);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private void schedule() {
        workers.scheduleAtFixedRate(() -> {
            if (!resumed) return;
            try { api.drive(left, right); }
            catch (Exception e) { ui.post(() -> status.setText("ROBOT OFFLINE")); }
        }, 0, 250, TimeUnit.MILLISECONDS);

        workers.scheduleAtFixedRate(() -> {
            if (!resumed) return;
            try {
                JSONObject d = api.getJson("/diag", 1200);
                int clients = d.optInt("clients", 0);
                ui.post(() -> status.setText("ROBOT ONLINE • clients " + clients));
            } catch (Exception e) {
                ui.post(() -> status.setText("ROBOT OFFLINE"));
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void applyNetwork() {
        api.setRobotHost(robotIp.getText().toString());
        String cam = cameraIp.getText().toString().trim();
        prefs.edit().putString("robot", api.getRobotHost()).putString("camera", cam).apply();
        startCamera();
    }

    private void startCamera() {
        if (!resumed || video == null) return;
        String cam = prefs.getString("camera", "192.168.4.2").trim();
        if (!cam.isEmpty()) video.start("http://" + cam + ":81/stream");
    }

    private void sendDriveNow() {
        workers.execute(() -> { try { api.drive(left, right); } catch (Exception ignored) {} });
    }

    private void hardStop() {
        left = right = 0;
        if (joystick != null) joystick.reset();
        workers.execute(() -> { try { api.stop(); } catch (Exception ignored) {} });
    }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        return l;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(7), dp(7), dp(7), dp(7));
        return t;
    }

    private EditText input(String text) {
        EditText e = new EditText(this);
        e.setText(text);
        e.setTextColor(Color.WHITE);
        e.setSingleLine(true);
        e.setBackgroundColor(0xFF222222);
        e.setPadding(dp(8), dp(8), dp(8), dp(8));
        return e;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
