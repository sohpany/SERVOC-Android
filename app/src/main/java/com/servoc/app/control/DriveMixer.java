package com.servoc.app.control;

/** Pure differential/skid-steer mixer used by the on-screen joystick.
 * X is yaw demand: +X always means turn the robot nose/camera right.
 * Y is translation demand: +Y forward, -Y reverse.
 * Outputs are normalized to the full -100..+100 command range without changing curvature.
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
        double left=y+x;
        double right=y-x;
        double scale=Math.max(1.0,Math.max(Math.abs(left),Math.abs(right)));
        return new Command((int)Math.round(left/scale*100.0),(int)Math.round(right/scale*100.0));
    }
}
