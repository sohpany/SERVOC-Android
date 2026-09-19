package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

/** HR067 operator-drive contract. Dual controls bypass DriveMixer and map one-to-one to L/R. */
public class HR067DriveContractTest {
    private static int scaled(int raw){return DriveSpeedLimiter.scale(raw,100);}
    @Test public void dualCardinalAndPivotCommandsAreDirect(){
        assertEquals(100,scaled(100));assertEquals(-100,scaled(-100));assertEquals(0,scaled(0));
        int lf=scaled(100),rf=scaled(100);assertEquals(100,lf);assertEquals(100,rf);
        int lb=scaled(-100),rb=scaled(-100);assertEquals(-100,lb);assertEquals(-100,rb);
        assertTrue(scaled(100)*scaled(-100)<0);
    }
    @Test public void legacyJoystickCardinalsRemainValid(){
        DriveMixer.Command f=DriveMixer.mix(0,1),b=DriveMixer.mix(0,-1),r=DriveMixer.mix(1,0),l=DriveMixer.mix(-1,0);
        assertArrayEquals(new int[]{100,100},new int[]{f.left,f.right});
        assertArrayEquals(new int[]{-100,-100},new int[]{b.left,b.right});
        assertArrayEquals(new int[]{100,-100},new int[]{r.left,r.right});
        assertArrayEquals(new int[]{-100,100},new int[]{l.left,l.right});
    }
}