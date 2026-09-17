package com.servoc.app.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LocalNetworkContractTest {
    @Test public void canonicalAddressesAreFixed(){
        assertEquals("HazRob",LocalNetworkContract.ROBOT_SSID);
        assertEquals("192.168.4.1",LocalNetworkContract.ROBOT_HOST);
        assertEquals("192.168.4.2",LocalNetworkContract.CAMERA_HOST);
        assertEquals("HazRob-CAM 0.10.1",LocalNetworkContract.CAMERA_FIRMWARE_BANNER);
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

    @Test public void directFirmwareIdentityAcceptsOnlyCanonicalReleaseBanner(){
        assertTrue(LocalNetworkContract.isExpectedFirmwareBanner("HazRob 0.10.1","0.10.1"));
        assertTrue(LocalNetworkContract.isExpectedFirmwareBanner("  HazRob 0.10.1  ","0.10.1"));
        assertFalse(LocalNetworkContract.isExpectedFirmwareBanner("HazRob 0.10.1 HR065","0.10.1"));
        assertFalse(LocalNetworkContract.isExpectedFirmwareBanner("HazRob 0.10.0","0.10.1"));
        assertFalse(LocalNetworkContract.isExpectedFirmwareBanner("router","0.10.1"));
        assertFalse(LocalNetworkContract.isExpectedFirmwareBanner(null,"0.10.1"));
        assertFalse(LocalNetworkContract.isExpectedFirmwareBanner("HazRob 0.10.1",null));
    }
}
