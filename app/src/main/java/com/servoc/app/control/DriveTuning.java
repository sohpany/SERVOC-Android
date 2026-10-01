package com.servoc.app.control;

/** Shared operator-tunable differential-drive percentages for non-tank controls. */
public final class DriveTuning {
    public static final String PREF_TURN_PERCENT = "drive_turn_percent";
    public static final String PREF_PIVOT_PERCENT = "drive_pivot_percent";
    public static final int DEFAULT_TURN_PERCENT = 40;
    public static final int DEFAULT_PIVOT_PERCENT = 60;

    private DriveTuning() {}

    public static int clampPercent(int value){return Math.max(0,Math.min(100,value));}
    public static double ratio(int percent){return clampPercent(percent)/100.0;}
}
