package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiscreteDriveTuningTest {
    private static void assertCommand(DiscreteDrive.Intent intent,int turn,int pivot,int left,int right){
        DriveMixer.Command c=DiscreteDrive.command(intent,turn,pivot);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }

    @Test public void defaultsAreFortyPercentTurnAndSixtyPercentPivot(){
        assertCommand(DiscreteDrive.Intent.FORWARD_RIGHT,40,60,100,40);
        assertCommand(DiscreteDrive.Intent.FORWARD_LEFT,40,60,40,100);
        assertCommand(DiscreteDrive.Intent.REVERSE_RIGHT,40,60,-100,-40);
        assertCommand(DiscreteDrive.Intent.REVERSE_LEFT,40,60,-40,-100);
        assertCommand(DiscreteDrive.Intent.PIVOT_RIGHT,40,60,60,-60);
        assertCommand(DiscreteDrive.Intent.PIVOT_LEFT,40,60,-60,60);
    }

    @Test public void manualValuesControlButtonsExactly(){
        assertCommand(DiscreteDrive.Intent.FORWARD_RIGHT,70,25,100,70);
        assertCommand(DiscreteDrive.Intent.REVERSE_LEFT,70,25,-70,-100);
        assertCommand(DiscreteDrive.Intent.PIVOT_RIGHT,70,25,25,-25);
    }

    @Test public void valuesAreClampedToZeroThroughOneHundred(){
        assertCommand(DiscreteDrive.Intent.FORWARD_RIGHT,-10,130,100,0);
        assertCommand(DiscreteDrive.Intent.PIVOT_LEFT,-10,130,-100,100);
    }
}
