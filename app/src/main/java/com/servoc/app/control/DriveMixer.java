package com.servoc.app.control;

/**
 * User-facing differential/skid-steer mixer for the on-screen wheel/joystick.
 *
 * The vertical axis is throttle. The horizontal axis is curvature only while the robot is
 * translating. A right steering command therefore keeps the LEFT side at the requested travel
 * speed and progressively slows the RIGHT side; a left command does the mirror image. Even at
 * full steering lock the inside side remains at 40% of the outside side, producing a predictable
 * driving arc instead of an abrupt one-wheel stop or reversal.
 *
 * With no meaningful translation demand, horizontal input becomes a true in-place pivot capped
 * at 50% motor command. The same travel-relative rule is retained in reverse.
 */
public final class DriveMixer {
    public static final double MIN_INNER_RATIO = 0.40;
    public static final double PIVOT_MAX = 0.50;
    private static final double TRANSLATION_DEADZONE = 0.08;

    public static final class Command {
        public final int left;
        public final int right;
        Command(int left,int right){this.left=left;this.right=right;}
    }

    private DriveMixer() {}

    private static double clampUnit(double v){return Math.max(-1.0,Math.min(1.0,v));}
    private static int pct(double v){return (int)Math.round(clampUnit(v)*100.0);}

    public static Command mix(double x,double y){
        x=clampUnit(x);y=clampUnit(y);

        // Horizontal-only control is an intentional pivot, limited to 50% for controllability.
        if(Math.abs(y)<=TRANSLATION_DEADZONE){
            double pivot=x*PIVOT_MAX;
            return new Command(pct(pivot),pct(-pivot));
        }

        // Car-like curvature steering. Steering never reverses the inside side while translating.
        double steer=Math.abs(x);
        double insideRatio=1.0-(1.0-MIN_INNER_RATIO)*steer;
        double outside=y;
        double inside=y*insideRatio;
        if(x>0)return new Command(pct(outside),pct(inside));       // right turn
        if(x<0)return new Command(pct(inside),pct(outside));       // left turn
        return new Command(pct(y),pct(y));
    }
}
