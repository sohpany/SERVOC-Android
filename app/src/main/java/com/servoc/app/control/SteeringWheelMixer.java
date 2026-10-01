package com.servoc.app.control;

/**
 * HazRob "العجلة" mixer using the operator-requested steering convention.
 *
 * Forward travel:
 * - +X (finger/right) progressively slows the LEFT side.
 * - -X (finger/left) progressively slows the RIGHT side.
 * Reverse travel is path-relative so the visual finger direction remains intuitive:
 * - reverse + right progressively slows the RIGHT side;
 * - reverse + left progressively slows the LEFT side.
 *
 * At full steering deflection the slowed side remains at 40% of the other side.
 * Horizontal-only input is a 50% capped in-place pivot.
 */
public final class SteeringWheelMixer {
    public static final double MIN_SLOWED_RATIO = 0.40;
    public static final double PIVOT_MAX = 0.50;
    private static final double TRANSLATION_DEADZONE = 0.08;

    private SteeringWheelMixer() {}

    private static double clampUnit(double v){return Math.max(-1.0,Math.min(1.0,v));}
    private static int pct(double v){return (int)Math.round(clampUnit(v)*100.0);}

    public static DriveMixer.Command mix(double x,double y){
        x=clampUnit(x);y=clampUnit(y);
        if(Math.abs(y)<=TRANSLATION_DEADZONE){
            double pivot=x*PIVOT_MAX;
            return new DriveMixer.Command(pct(pivot),pct(-pivot));
        }
        double ratio=1.0-(1.0-MIN_SLOWED_RATIO)*Math.abs(x);
        double full=y;
        double slowed=y*ratio;

        if(y>0){
            if(x>0)return new DriveMixer.Command(pct(slowed),pct(full)); // forward + right: slow LEFT
            if(x<0)return new DriveMixer.Command(pct(full),pct(slowed)); // forward + left: slow RIGHT
        }else{
            if(x>0)return new DriveMixer.Command(pct(full),pct(slowed)); // reverse + right: slow RIGHT
            if(x<0)return new DriveMixer.Command(pct(slowed),pct(full)); // reverse + left: slow LEFT
        }
        return new DriveMixer.Command(pct(y),pct(y));
    }
}
