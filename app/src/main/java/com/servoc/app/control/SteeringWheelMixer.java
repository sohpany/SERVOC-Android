package com.servoc.app.control;

/**
 * HazRob "العجلة" mixer. The wheel and joystick intentionally share exactly the same
 * operator-requested steering contract so switching presentation never changes vehicle behavior.
 */
public final class SteeringWheelMixer {
    public static final double MIN_SLOWED_RATIO = DriveMixer.MIN_INNER_RATIO;
    public static final double PIVOT_MAX = DriveMixer.PIVOT_MAX;

    private SteeringWheelMixer() {}

    public static DriveMixer.Command mix(double x,double y){
        return DriveMixer.mix(x,y);
    }
}
