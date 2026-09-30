package com.servoc.app.control;

/** Fixed differential-drive commands used by the eight-button driving mode. */
public final class DiscreteDrive {
    public enum Intent { FORWARD, FORWARD_RIGHT, FORWARD_LEFT, REVERSE, REVERSE_RIGHT, REVERSE_LEFT, PIVOT_RIGHT, PIVOT_LEFT, STOP }
    private DiscreteDrive(){}
    public static DriveMixer.Command command(Intent i){
        if(i==null)i=Intent.STOP;
        switch(i){
            case FORWARD:return new DriveMixer.Command(100,100);
            case FORWARD_RIGHT:return new DriveMixer.Command(100,50);
            case FORWARD_LEFT:return new DriveMixer.Command(50,100);
            case REVERSE:return new DriveMixer.Command(-100,-100);
            case REVERSE_RIGHT:return new DriveMixer.Command(-100,-50);
            case REVERSE_LEFT:return new DriveMixer.Command(-50,-100);
            case PIVOT_RIGHT:return new DriveMixer.Command(100,-100);
            case PIVOT_LEFT:return new DriveMixer.Command(-100,100);
            default:return new DriveMixer.Command(0,0);
        }
    }
}
