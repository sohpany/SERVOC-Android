package com.servoc.app.control;

/**
 * Operator-requested two-axis differential-drive mixer used by the HazRob joystick.
 *
 * The vertical axis is travel demand. While translating, horizontal displacement is curvature:
 * - forward + right progressively slows LEFT;
 * - forward + left progressively slows RIGHT;
 * - reverse preserves the requested path-relative steering feel used by the wheel control:
 *   reverse + right slows RIGHT, reverse + left slows LEFT.
 *
 * The slowed side remains at 40% at full steering lock so translating turns do not abruptly
 * stop/reverse one side. With no meaningful translation, horizontal input is a true in-place
 * pivot capped at 65% motor command. Release/cancel is handled by the owning control view.
 */
public final class DriveMixer {
    public static final double MIN_INNER_RATIO = 0.40;
    public static final double PIVOT_MAX = 0.65;
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

        if(y>0){
            if(x>0)return new Command(pct(slowed),pct(full)); // forward + right: slow LEFT
            if(x<0)return new Command(pct(full),pct(slowed)); // forward + left: slow RIGHT
        }else{
            if(x>0)return new Command(pct(full),pct(slowed)); // reverse + right: slow RIGHT
            if(x<0)return new Command(pct(slowed),pct(full)); // reverse + left: slow LEFT
        }
        return new Command(pct(y),pct(y));
    }
}
