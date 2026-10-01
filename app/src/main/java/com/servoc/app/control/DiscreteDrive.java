package com.servoc.app.control;

/** Differential-drive commands used by the eight-button driving mode. */
public final class DiscreteDrive {
    public enum Intent { FORWARD, FORWARD_RIGHT, FORWARD_LEFT, REVERSE, REVERSE_RIGHT, REVERSE_LEFT, PIVOT_RIGHT, PIVOT_LEFT, STOP }
    private DiscreteDrive(){}

    public static DriveMixer.Command command(Intent i){
        return command(i,DriveTuning.DEFAULT_TURN_PERCENT,DriveTuning.DEFAULT_PIVOT_PERCENT);
    }

    public static DriveMixer.Command command(Intent i,int turnPercent,int pivotPercent){
        if(i==null)i=Intent.STOP;
        int turn=DriveTuning.clampPercent(turnPercent),pivot=DriveTuning.clampPercent(pivotPercent);
        switch(i){
            case FORWARD:return new DriveMixer.Command(100,100);
            case FORWARD_RIGHT:return new DriveMixer.Command(100,turn);
            case FORWARD_LEFT:return new DriveMixer.Command(turn,100);
            case REVERSE:return new DriveMixer.Command(-100,-100);
            case REVERSE_RIGHT:return new DriveMixer.Command(-100,-turn);
            case REVERSE_LEFT:return new DriveMixer.Command(-turn,-100);
            case PIVOT_RIGHT:return new DriveMixer.Command(pivot,-pivot);
            case PIVOT_LEFT:return new DriveMixer.Command(-pivot,pivot);
            default:return new DriveMixer.Command(0,0);
        }
    }
}
