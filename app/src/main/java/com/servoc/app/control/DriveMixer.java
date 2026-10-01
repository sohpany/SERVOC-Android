package com.servoc.app.control;

/**
 * Operator-requested two-axis differential-drive mixer shared by Joystick and "العجلة".
 *
 * The vertical axis is travel demand. Horizontal displacement follows one fixed operator rule
 * in BOTH forward and reverse:
 * - touch/right (+X) progressively slows the RIGHT side;
 * - touch/left  (-X) progressively slows the LEFT side.
 *
 * The slowed side remains at 40% at full steering deflection so translating turns never reverse
 * a side. With no meaningful translation, horizontal input is a true in-place pivot capped at
 * 60% motor command. Release/cancel is handled by the owning control view.
 */
public final class DriveMixer {
    public static final double MIN_INNER_RATIO = 0.40;
    public static final double PIVOT_MAX = 0.60;
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

        if(Math.abs(y)<=TRANSLATION_DEADZONE){
            double pivot=x*PIVOT_MAX;
            return new Command(pct(pivot),pct(-pivot));
        }

        double ratio=1.0-(1.0-MIN_INNER_RATIO)*Math.abs(x);
        double full=y;
        double slowed=y*ratio;

        // Deliberately identical side selection in forward and reverse:
        // steer right => slow right; steer left => slow left.
        if(x>0)return new Command(pct(full),pct(slowed));
        if(x<0)return new Command(pct(slowed),pct(full));
        return new Command(pct(full),pct(full));
    }
}
