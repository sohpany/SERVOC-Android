package com.servoc.app.control;

/** HazRob "العجلة" mixer. Uses the same differential-drive contract as Joystick. */
public final class SteeringWheelMixer {
    public static final double MIN_SLOWED_RATIO = DriveMixer.MIN_INNER_RATIO;
    public static final double PIVOT_MAX = DriveMixer.PIVOT_MAX;

    private SteeringWheelMixer() {}

    public static DriveMixer.Command mix(double x,double y){
        return DriveMixer.mix(x,y);
    }

    public static DriveMixer.Command mix(double x,double y,int turnPercent,int pivotPercent){
        return DriveMixer.mix(x,y,turnPercent,pivotPercent);
    }
}
