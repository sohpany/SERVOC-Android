package com.servoc.app.communication;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DirectDriveDatagramTest {
    @Test public void packetLayoutAndChecksumAreStable(){
        byte[] b=DirectDriveDatagram.encode(0x1234,100,-100);
        assertEquals(8,b.length);
        assertEquals('H',b[0]&0xFF);assertEquals('R',b[1]&0xFF);assertEquals(1,b[2]&0xFF);
        assertEquals(0x34,b[3]&0xFF);assertEquals(0x12,b[4]&0xFF);
        assertEquals(100,(int)b[5]);assertEquals(-100,(int)b[6]);
        int x=0;for(int i=0;i<7;i++)x^=b[i]&0xFF;assertEquals(x,b[7]&0xFF);
    }

    @Test public void commandsAreClampedToDriveRange(){
        byte[] b=DirectDriveDatagram.encode(7,180,-240);
        assertEquals(100,(int)b[5]);assertEquals(-100,(int)b[6]);
    }
}
