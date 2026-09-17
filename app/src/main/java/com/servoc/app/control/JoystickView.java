package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Circular two-axis driving control.
 *
 * Command policy is intentionally direct and linear: there is no acceleration ramp,
 * reverse delay or steering smoothing in the operator path. The firmware remains the
 * final safety authority. Releasing/cancelling the touch always emits neutral 0/0.
 */
public final class JoystickView extends View {
    public interface Listener { void onDrive(int left, int right, boolean active); }

    private static final float KNOB_RADIUS_RATIO = 0.22f;
    private static final int INVALID_POINTER = -1;

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 0, y = 0;
    private boolean active = false;
    private int pointerId = INVALID_POINTER;
    private Listener listener;

    public JoystickView(Context context) {
        super(context);
        setClickable(true);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(4);
        ring.setColor(0xAAFFFFFF);
        guide.setStyle(Paint.Style.STROKE);
        guide.setStrokeWidth(2);
        guide.setColor(0x44FFFFFF);
        knob.setStyle(Paint.Style.FILL);
        knob.setColor(0xDDFFFFFF);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.max(1f, Math.min(cx, cy) - 8f);
        float knobRadius = Math.max(8f, r * KNOB_RADIUS_RATIO);
        float travel = Math.max(0f, r - knobRadius - 2f);
        c.drawCircle(cx, cy, r, ring);
        c.drawLine(cx - r * .72f, cy, cx + r * .72f, cy, guide);
        c.drawLine(cx, cy - r * .72f, cx, cy + r * .72f, guide);
        c.drawCircle(cx + x * travel, cy - y * travel, knobRadius, knob);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                pointerId = e.getPointerId(0);
                active = true;
                updateFromPointer(e, 0);
                return true;
            case MotionEvent.ACTION_MOVE: {
                int index = e.findPointerIndex(pointerId);
                if (index < 0) { resetInternal(true); return true; }
                updateFromPointer(e, index);
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                if (e.getPointerId(e.getActionIndex()) == pointerId) resetInternal(true);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                resetInternal(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                resetInternal(true);
                return true;
            default:
                return true;
        }
    }

    private void updateFromPointer(MotionEvent e, int index) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.max(1f, Math.min(cx, cy) - 8f);
        float nx = (e.getX(index) - cx) / radius;
        float ny = -(e.getY(index) - cy) / radius;
        double magnitude = Math.hypot(nx, ny);
        if (magnitude > 1.0) {
            nx /= magnitude;
            ny /= magnitude;
            magnitude = 1.0;
        }
        // No software dead-zone, acceleration ramp, reverse delay or steering smoothing:
        // every measurable finger displacement maps linearly to a drive command.
        if (magnitude <= 1.0e-6) {
            nx = 0f;
            ny = 0f;
        }
        x = nx;
        y = ny;
        invalidate();
        emit();
    }

    private void resetInternal(boolean emit) {
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
        pointerId = INVALID_POINTER;
        active = false;
        x = 0f;
        y = 0f;
        invalidate();
        if (emit) emit();
    }

    private void emit() {
        if (listener == null) return;
        DriveMixer.Command command = DriveMixer.mix(x, y);
        listener.onDrive(command.left, command.right, active);
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    public void reset() { resetInternal(true); }
}
