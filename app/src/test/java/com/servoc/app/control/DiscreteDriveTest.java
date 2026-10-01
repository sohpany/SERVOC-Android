package com.servoc.app.control;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class DiscreteDriveTest {
    private static void assertCmd(DiscreteDrive.Intent i,int l,int r){DriveMixer.Command c=DiscreteDrive.command(i);assertEquals(l,c.left);assertEquals(r,c.right);}
    @Test public void buttonMatrixMatchesDifferentialDriveContract(){
        assertCmd(DiscreteDrive.Intent.FORWARD,100,100);
        assertCmd(DiscreteDrive.Intent.REVERSE,-100,-100);
        assertCmd(DiscreteDrive.Intent.PIVOT_RIGHT,60,-60);
        assertCmd(DiscreteDrive.Intent.PIVOT_LEFT,-60,60);
        assertCmd(DiscreteDrive.Intent.FORWARD_RIGHT,100,40);
        assertCmd(DiscreteDrive.Intent.FORWARD_LEFT,40,100);
        assertCmd(DiscreteDrive.Intent.REVERSE_RIGHT,-100,-40);
        assertCmd(DiscreteDrive.Intent.REVERSE_LEFT,-40,-100);
        assertCmd(DiscreteDrive.Intent.STOP,0,0);
    }
}
