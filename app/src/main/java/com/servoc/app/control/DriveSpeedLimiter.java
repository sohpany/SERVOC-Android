package com.servoc.app.control;

/** Pure command scaler for the operator speed column. Firmware safety remains independent. */
public final class DriveSpeedLimiter {
    public static final int DEFAULT_PERCENT = 50;

    private DriveSpeedLimiter() {}

    public static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public static int scale(int command, int percent) {
        int c = Math.max(-100, Math.min(100, command));
        int p = clampPercent(percent);
        return (int) Math.round(c * (p / 100.0));
    }
}
