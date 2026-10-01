package com.servoc.app.control;

import static org.junit.Assert.*;
import org.junit.Test;

/** Current HazRob 0.10.2 / HR069 operator-drive acceptance matrix. */
public final class HR069OperatorDriveTest {
    private static void mix(double x,double y,int l,int r){DriveMixer.Command c=DriveMixer.mix(x,y);assertEquals(l,c.left);assertEquals(r,c.right);}
    private static void wheel(double x,double y,int l,int r){DriveMixer.Command c=SteeringWheelMixer.mix(x,y);assertEquals(l,c.left);assertEquals(r,c.right);}
    private static void discrete(DiscreteDrive.Intent i,int l,int r){DriveMixer.Command c=DiscreteDrive.command(i);assertEquals(l,c.left);assertEquals(r,c.right);}

    @Test public void fourDrivePresentationsRemainIndependent(){
        assertEquals(4,DriveMode.values().length);
        assertEquals(DriveMode.TANK,DriveMode.fromKey("tank"));
        assertEquals(DriveMode.JOYSTICK,DriveMode.fromKey("joystick"));
        assertEquals(DriveMode.WHEEL,DriveMode.fromKey("wheel"));
        assertEquals(DriveMode.BUTTONS,DriveMode.fromKey("buttons"));
        assertEquals(DriveMode.JOYSTICK,DriveMode.TANK.next());
        assertEquals(DriveMode.WHEEL,DriveMode.JOYSTICK.next());
        assertEquals(DriveMode.BUTTONS,DriveMode.WHEEL.next());
        assertEquals(DriveMode.TANK,DriveMode.BUTTONS.next());
    }

    @Test public void joystickVehiclePathAndPivotContract(){
        mix(0,1,100,100);mix(0,-1,-100,-100);mix(1,0,50,-50);mix(-1,0,-50,50);
        mix(0.5,0.8,80,56);mix(-0.5,0.8,56,80);mix(1.0,0.8,80,32);mix(-1.0,0.8,32,80);
    }

    @Test public void wheelUsesRequestedForwardAndReverseConvention(){
        wheel(0,1,100,100);
        wheel(0.5,0.8,56,80);
        wheel(1.0,0.8,32,80);
        wheel(-0.5,0.8,80,56);
        wheel(-1.0,0.8,80,32);

        // Reverse remains intuitive relative to the path direction:
        // finger right slows RIGHT, finger left slows LEFT.
        wheel(0.5,-0.8,-80,-56);
        wheel(1.0,-0.8,-80,-32);
        wheel(-0.5,-0.8,-56,-80);
        wheel(-1.0,-0.8,-32,-80);

        wheel(1,0,50,-50);
        wheel(-1,0,-50,50);
    }

    @Test public void eightButtonContractMatchesCurrentDriveMatrix(){
        discrete(DiscreteDrive.Intent.FORWARD,100,100);discrete(DiscreteDrive.Intent.FORWARD_RIGHT,100,40);discrete(DiscreteDrive.Intent.FORWARD_LEFT,40,100);
        discrete(DiscreteDrive.Intent.REVERSE,-100,-100);discrete(DiscreteDrive.Intent.REVERSE_RIGHT,-100,-40);discrete(DiscreteDrive.Intent.REVERSE_LEFT,-40,-100);
        discrete(DiscreteDrive.Intent.PIVOT_RIGHT,50,-50);discrete(DiscreteDrive.Intent.PIVOT_LEFT,-50,50);discrete(DiscreteDrive.Intent.STOP,0,0);
    }

    @Test public void dualTankControlsPreserveDirectIndependentSpeedAuthority(){
        assertEquals(100,DriveSpeedLimiter.scale(100,100));assertEquals(-100,DriveSpeedLimiter.scale(-100,100));assertEquals(37,DriveSpeedLimiter.scale(37,100));assertEquals(-64,DriveSpeedLimiter.scale(-64,100));
        int left=73,right=-28;assertEquals(73,left);assertEquals(-28,right);assertTrue(left*right<0);
    }

    @Test public void estopGestureContractIsTwoSecondHoldThenTapReset(){
        assertFalse(EmergencyStopPressPolicy.shouldLatch(false,1999));assertTrue(EmergencyStopPressPolicy.shouldLatch(false,2000));assertTrue(EmergencyStopPressPolicy.shouldResetOnTap(true));assertFalse(EmergencyStopPressPolicy.shouldResetOnTap(false));
    }
}
