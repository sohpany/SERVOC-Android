package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
        setContentView(buildResponsiveUi());
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

    private View buildResponsiveUi() {
        boolean wide = isWide();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E1116);
        root.setPadding(dp(8), dp(6), dp(8), dp(8));
        root.addView(buildHeader(), new LinearLayout.LayoutParams(-1, -2));

        FrameLayout videoPanel = buildVideoPanel(wide);
        View controls = buildControls();

        if (wide) {
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.HORIZONTAL);
            body.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -1, 1.65f);
            leftParams.setMargins(0, dp(4), dp(6), 0);
            body.addView(videoPanel, leftParams);

            ScrollView sideScroll = new ScrollView(this);
            sideScroll.setFillViewport(true);
            sideScroll.addView(controls);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -1, 1.0f);
            rightParams.setMargins(dp(2), dp(4), 0, 0);
            body.addView(sideScroll, rightParams);
            root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
            return root;
        }

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int videoHeight = clamp((int)(screenWidth * 0.70f), dp(230), dp(430));
        LinearLayout body = vbox();
        body.addView(videoPanel, new LinearLayout.LayoutParams(-1, videoHeight));
        body.addView(controls, new LinearLayout.LayoutParams(-1, -2));
        root.addView(body, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        return scroll;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        TextView title = label("HazRob • CONTROL");
        title.setTextSize(isWide() ? 18 : 20);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        status = label("ROBOT OFFLINE");
        status.setTextSize(13);
        status.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        status.setTextColor(0xFFFFB74D);
        header.addView(status, new LinearLayout.LayoutParams(-2, -2));
        return header;
    }

    private FrameLayout buildVideoPanel(boolean wide) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);

        video = new MjpegView(this);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1));

        int shortSide = Math.min(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        int joy = clamp((int)(shortSide * (wide ? 0.28f : 0.34f)), dp(112), dp(180));
        joystick = new JoystickView(this);
        FrameLayout.LayoutParams joyParams = new FrameLayout.LayoutParams(joy, joy, Gravity.END | Gravity.BOTTOM);
        joyParams.setMargins(0, 0, dp(10), dp(10));
        frame.addView(joystick, joyParams);
        joystick.setListener((l, r, active) -> {
            left = l;
            right = r;
            if (!active) {
                left = right = 0;
                sendDriveNow();
            }
        });

        TextView cam = label("ESP32-CAM");
        cam.setTextSize(11);
        cam.setBackgroundColor(0x88000000);
        FrameLayout.LayoutParams camParams = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.TOP);
        camParams.setMargins(dp(6), dp(6), 0, 0);
        frame.addView(cam, camParams);
        return frame;
    }

    private View buildControls() {
        LinearLayout panel = vbox();
        panel.setPadding(dp(8), dp(6), dp(8), dp(10));
        panel.setBackgroundColor(0xFF181D24);

        Button stop = button("EMERGENCY STOP");
        stop.setTextColor(Color.WHITE);
        stop.setBackgroundColor(0xFFC62828);
        stop.setOnClickListener(v -> hardStop());
        LinearLayout.LayoutParams stopParams = full();
        stopParams.height = dp(54);
        panel.addView(stop, stopParams);

        TextView connectionTitle = label("CONNECTION");
        connectionTitle.setTextColor(0xFF90CAF9);
        panel.addView(connectionTitle, full());

        robotIp = input(prefs.getString("robot", "192.168.4.1"));
        cameraIp = input(prefs.getString("camera", "192.168.4.2"));
        panel.addView(label("Robot IP"), full());
        panel.addView(robotIp, full());
        panel.addView(label("Camera IP"), full());
        panel.addView(cameraIp, full());

        Button apply = button("Apply connection settings");
        apply.setOnClickListener(v -> applyNetwork());
        panel.addView(apply, full());

        Button analysis = button("Analysis • 144 chemical library");
        analysis.setOnClickListener(v -> startActivity(new Intent(this, AnalysisActivity.class)));
        panel.addView(analysis, full());

        TextView note = label("Control remains local-first. ESP32 retains real-time failsafe; HazRob transmits drive heartbeat commands every 250 ms while active.");
        note.setTextSize(12);
        note.setTextColor(0xFFB8C1CC);
        panel.addView(note, full());
        return panel;
    }

    private void schedule() {
        workers.scheduleAtFixedRate(() -> {
            if (!resumed) return;
            try {
                api.drive(left, right);
            } catch (Exception e) {
                ui.post(() -> setOffline());
            }
        }, 0, 250, TimeUnit.MILLISECONDS);

        workers.scheduleAtFixedRate(() -> {
            if (!resumed) return;
            try {
                JSONObject d = api.getJson("/diag", 1200);
                int clients = d.optInt("clients", 0);
                ui.post(() -> {
                    status.setText("ROBOT ONLINE • " + clients);
                    status.setTextColor(0xFF81C784);
                });
            } catch (Exception e) {
                ui.post(this::setOffline);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setOffline() {
        if (status == null) return;
        status.setText("ROBOT OFFLINE");
        status.setTextColor(0xFFFFB74D);
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
        workers.execute(() -> {
            try { api.drive(left, right); } catch (Exception ignored) {}
        });
    }

    private void hardStop() {
        left = right = 0;
        if (joystick != null) joystick.reset();
        workers.execute(() -> {
            try { api.stop(); } catch (Exception ignored) {}
        });
    }

    private boolean isWide() {
        Configuration c = getResources().getConfiguration();
        return c.orientation == Configuration.ORIENTATION_LANDSCAPE || c.screenWidthDp >= 700;
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
        t.setPadding(dp(7), dp(6), dp(7), dp(6));
        return t;
    }

    private EditText input(String text) {
        EditText e = new EditText(this);
        e.setText(text);
        e.setTextColor(Color.WHITE);
        e.setSingleLine(true);
        e.setBackgroundColor(0xFF232A33);
        e.setPadding(dp(10), dp(9), dp(10), dp(9));
        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams full() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(3), 0, dp(3));
        return p;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
