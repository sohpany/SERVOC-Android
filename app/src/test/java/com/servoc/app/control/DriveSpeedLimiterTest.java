package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveSpeedLimiterTest {
    @Test public void defaultIsFiftyPercent(){assertEquals(50,DriveSpeedLimiter.DEFAULT_PERCENT);}
    @Test public void scalesBothDirectionsSymmetrically(){assertEquals(50,DriveSpeedLimiter.scale(100,50));assertEquals(-50,DriveSpeedLimiter.scale(-100,50));assertEquals(25,DriveSpeedLimiter.scale(50,50));}
    @Test public void zeroAndFullScaleAreExact(){assertEquals(0,DriveSpeedLimiter.scale(100,0));assertEquals(100,DriveSpeedLimiter.scale(100,100));assertEquals(-100,DriveSpeedLimiter.scale(-100,100));}
    @Test public void clampsPercentAndCommands(){assertEquals(100,DriveSpeedLimiter.scale(120,150));assertEquals(-100,DriveSpeedLimiter.scale(-120,100));assertEquals(0,DriveSpeedLimiter.scale(100,-10));}
}
