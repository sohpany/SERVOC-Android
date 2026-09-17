package com.servoc.app.control;

/**
 * Differential/skid-steer mixer used by the on-screen joystick.
 *
 * Steering is travel-relative. +X means turn toward the vehicle's right-hand side and -X toward
 * the left-hand side, regardless of whether translation is forward or reverse. Therefore the
 * inside physical side is the same side that is slowed in both directions of travel.
 *
 * With no translation demand, horizontal joystick motion becomes an in-place counter-rotation.
 * Outputs are normalized to the full -100..+100 command range without changing the requested
 * steering ratio.
 */
public final class DriveMixer {
    public static final class Command {
        public final int left;
        public final int right;
        Command(int left,int right){this.left=left;this.right=right;}
    }

    private DriveMixer() {}

    private static double clampUnit(double v){return Math.max(-1.0,Math.min(1.0,v));}

    public static Command mix(double x,double y){
        x=clampUnit(x);y=clampUnit(y);

        // While reversing, invert the steering contribution before differential mixing so a
        // right-hand command still slows the physical RIGHT side, and a left-hand command the LEFT.
        // At y==0 retain conventional skid-steer pivot: right = left forward/right reverse.
        double steer=y<0?-x:x;
        double left=y+steer;
        double right=y-steer;
        double scale=Math.max(1.0,Math.max(Math.abs(left),Math.abs(right)));
        return new Command((int)Math.round(left/scale*100.0),(int)Math.round(right/scale*100.0));
    }
}
