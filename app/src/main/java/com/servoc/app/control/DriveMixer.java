package com.servoc.app.control;

/**
 * Differential-drive mixer shared by Joystick and "العجلة".
 *
 * Vertical input is travel demand. Horizontal input progressively slows one side while the
 * other remains at the requested travel magnitude. The minimum slowed-side percentage and
 * pure-pivot percentage are operator tunable from Settings.
 */
public final class DriveMixer {
    public static final double MIN_INNER_RATIO = DriveTuning.DEFAULT_TURN_PERCENT / 100.0;
    public static final double PIVOT_MAX = DriveTuning.DEFAULT_PIVOT_PERCENT / 100.0;
    private static final double TRANSLATION_DEADZONE = 0.08;

    public static final class Command {
        public final int left;
        public final int right;
        public Command(int left,int right){this.left=left;this.right=right;}
    }

    private DriveMixer() {}

    private static double clampUnit(double v){return Math.max(-1.0,Math.min(1.0,v));}
    private static int pct(double v){return (int)Math.round(clampUnit(v)*100.0);}

    public static Command mix(double x,double y){
        return mix(x,y,DriveTuning.DEFAULT_TURN_PERCENT,DriveTuning.DEFAULT_PIVOT_PERCENT);
    }

    public static Command mix(double x,double y,int turnPercent,int pivotPercent){
        x=clampUnit(x);y=clampUnit(y);
        double pivotMax=DriveTuning.ratio(pivotPercent);
        double minInnerRatio=DriveTuning.ratio(turnPercent);

        if(Math.abs(y)<=TRANSLATION_DEADZONE){
            double pivot=x*pivotMax;
            return new Command(pct(pivot),pct(-pivot));
        }

        double ratio=1.0-(1.0-minInnerRatio)*Math.abs(x);
        double full=y;
        double slowed=y*ratio;

        // Default steering rule is identical in forward and reverse:
        // steer right => slow right; steer left => slow left.
        // Per-control reversal is applied later by DriveDirectionOption.
        if(x>0)return new Command(pct(full),pct(slowed));
        if(x<0)return new Command(pct(slowed),pct(full));
        return new Command(pct(full),pct(full));
    }
}
