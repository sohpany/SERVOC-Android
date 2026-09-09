package com.servoc.app.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LocalNetworkContractTest {
    @Test public void canonicalAddressesAreFixed(){
        assertEquals("192.168.4.1",LocalNetworkContract.ROBOT_HOST);
        assertEquals("192.168.4.2",LocalNetworkContract.CAMERA_HOST);
    }

    @Test public void hazRobSubnetRecognitionAcceptsOnlyCanonicalLan(){
        assertTrue(LocalNetworkContract.isRobotSubnetAddress("192.168.4.1"));
        assertTrue(LocalNetworkContract.isRobotSubnetAddress("192.168.4.100"));
        assertTrue(LocalNetworkContract.isRobotSubnetAddress("192.168.4.254"));
        assertFalse(LocalNetworkContract.isRobotSubnetAddress("192.168.5.100"));
        assertFalse(LocalNetworkContract.isRobotSubnetAddress("10.0.0.4"));
        assertFalse(LocalNetworkContract.isRobotSubnetAddress("192.168.4.0"));
        assertFalse(LocalNetworkContract.isRobotSubnetAddress("192.168.4.255"));
        assertFalse(LocalNetworkContract.isRobotSubnetAddress(null));
    }
}
