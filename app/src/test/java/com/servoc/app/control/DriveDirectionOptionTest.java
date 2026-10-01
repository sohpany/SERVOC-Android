package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveDirectionOptionTest {
    private static void assertMapped(DriveMode mode,boolean reverse,int inLeft,int inRight,int outLeft,int outRight){
        DriveMixer.Command c=DriveDirectionOption.apply(mode,reverse,inLeft,inRight);
        assertEquals(outLeft,c.left);assertEquals(outRight,c.right);
    }

    @Test public void eachNonTankPresentationCanReverseIndependently(){
        assertMapped(DriveMode.JOYSTICK,true,100,40,40,100);
        assertMapped(DriveMode.WHEEL,true,-60,60,60,-60);
        assertMapped(DriveMode.BUTTONS,true,100,-100,-100,100);
    }

    @Test public void disabledReversalPreservesCommands(){
        assertMapped(DriveMode.JOYSTICK,false,100,40,100,40);
        assertMapped(DriveMode.WHEEL,false,-60,60,-60,60);
        assertMapped(DriveMode.BUTTONS,false,100,-100,100,-100);
    }

    @Test public void tankSlidersCanNeverBeReversedByThisOption(){
        assertFalse(DriveDirectionOption.supportsReverse(DriveMode.TANK));
        assertEquals("",DriveDirectionOption.preferenceKey(DriveMode.TANK));
        assertMapped(DriveMode.TANK,true,73,-28,73,-28);
    }

    @Test public void eachPresentationUsesItsOwnPreferenceKey(){
        assertNotEquals(DriveDirectionOption.PREF_REVERSE_JOYSTICK,DriveDirectionOption.PREF_REVERSE_WHEEL);
        assertNotEquals(DriveDirectionOption.PREF_REVERSE_JOYSTICK,DriveDirectionOption.PREF_REVERSE_BUTTONS);
        assertNotEquals(DriveDirectionOption.PREF_REVERSE_WHEEL,DriveDirectionOption.PREF_REVERSE_BUTTONS);
        assertEquals(DriveDirectionOption.PREF_REVERSE_JOYSTICK,DriveDirectionOption.preferenceKey(DriveMode.JOYSTICK));
        assertEquals(DriveDirectionOption.PREF_REVERSE_WHEEL,DriveDirectionOption.preferenceKey(DriveMode.WHEEL));
        assertEquals(DriveDirectionOption.PREF_REVERSE_BUTTONS,DriveDirectionOption.preferenceKey(DriveMode.BUTTONS));
    }
}
