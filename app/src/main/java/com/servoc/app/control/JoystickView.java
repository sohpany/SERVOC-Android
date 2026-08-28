package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public final class JoystickView extends View {
    public interface Listener { void onDrive(int left, int right, boolean active); }

    private final Paint ring = new Paint(1);
    private final Paint knob = new Paint(1);
    private float x = 0, y = 0;
    private boolean active = false;
    private Listener listener;

    public JoystickView(Context context) {
        super(context);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(4);
        ring.setColor(0x88FFFFFF);
        knob.setColor(0xAAFFFFFF);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 8;
        c.drawCircle(cx, cy, r, ring);
        c.drawCircle(cx + x * r * .62f, cy - y * r * .62f, r * .25f, knob);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            active = false; x = y = 0; invalidate(); emit(); return true;
        }
        if (e.getAction() != MotionEvent.ACTION_DOWN && e.getAction() != MotionEvent.ACTION_MOVE) return true;
        active = true;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.max(1, Math.min(cx, cy) - 8);
        float nx = (e.getX() - cx) / radius;
        float ny = -(e.getY() - cy) / radius;
        double m = Math.hypot(nx, ny);
        if (m > 1) { nx /= m; ny /= m; m = 1; }
        if (m < .08) { nx = ny = 0; }
        else {
            double z = (m - .08) / .92;
            z *= z;
            nx = (float) (nx / m * z);
            ny = (float) (ny / m * z);
        }
        x = nx; y = ny; invalidate(); emit(); return true;
    }

    private void emit() {
        if (listener == null) return;
        double l = y + x, r = y - x;
        double q = Math.max(1, Math.max(Math.abs(l), Math.abs(r)));
        listener.onDrive((int) Math.round(l / q * 100), (int) Math.round(r / q * 100), active);
    }

    public void reset() { active = false; x = y = 0; invalidate(); emit(); }
}
