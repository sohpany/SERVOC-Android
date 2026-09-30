package com.servoc.app.control;

import static org.junit.Assert.*;
import org.junit.Test;

/** Current HazRob 0.10.2 / HR069 operator-drive acceptance matrix. */
public final class HR069OperatorDriveTest {
    private static void mix(double x,double y,int l,int r){DriveMixer.Command c=DriveMixer.mix(x,y);assertEquals(l,c.left);assertEquals(r,c.right);}
    private static void discrete(DiscreteDrive.Intent i,int l,int r){DriveMixer.Command c=DiscreteDrive.command(i);assertEquals(l,c.left);assertEquals(r,c.right);}

    @Test public void fourDrivePresentationsRemainIndependent(){
        assertEquals(4,DriveMode.values().length);
        assertEquals(DriveMode.TANK,DriveMode.fromKey("tank"));
        assertEquals(DriveMode.WHEEL,DriveMode.fromKey("wheel"));
        assertEquals(DriveMode.JOYSTICK,DriveMode.fromKey("joystick"));
        assertEquals(DriveMode.BUTTONS,DriveMode.fromKey("buttons"));
        assertEquals(DriveMode.WHEEL,DriveMode.TANK.next());
        assertEquals(DriveMode.JOYSTICK,DriveMode.WHEEL.next());
        assertEquals(DriveMode.BUTTONS,DriveMode.JOYSTICK.next());
        assertEquals(DriveMode.TANK,DriveMode.BUTTONS.next());
    }

    @Test public void wheelAndJoystickClockFaceContract(){
        // 12 / 6
        mix(0,1,100,100);
        mix(0,-1,-100,-100);
        // 3 / 9: in-place counter-rotation.
        mix(1,0,100,-100);
        mix(-1,0,-100,100);
        // 2 / 10: progressive forward turns; inside side is reduced.
        mix(0.25,0.75,100,50);
        mix(-0.25,0.75,50,100);
        // 4 / 8: progressive reverse turns; same physical inside side is reduced.
        mix(0.25,-0.75,-100,-50);
        mix(-0.25,-0.75,-50,-100);
    }

    @Test public void eightButtonContractMatchesRequestedDirections(){
        discrete(DiscreteDrive.Intent.FORWARD,100,100);
        discrete(DiscreteDrive.Intent.FORWARD_RIGHT,100,50);
        discrete(DiscreteDrive.Intent.FORWARD_LEFT,50,100);
        discrete(DiscreteDrive.Intent.REVERSE,-100,-100);
        discrete(DiscreteDrive.Intent.REVERSE_RIGHT,-100,-50);
        discrete(DiscreteDrive.Intent.REVERSE_LEFT,-50,-100);
        discrete(DiscreteDrive.Intent.PIVOT_RIGHT,100,-100);
        discrete(DiscreteDrive.Intent.PIVOT_LEFT,-100,100);
        discrete(DiscreteDrive.Intent.STOP,0,0);
    }
}
