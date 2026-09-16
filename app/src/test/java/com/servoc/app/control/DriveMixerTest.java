package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveMixerTest {
    private static void assertMix(double x,double y,int left,int right){
        DriveMixer.Command c=DriveMixer.mix(x,y);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }

    @Test public void cardinalDirectionsAreExact(){
        assertMix(0,0,0,0);
        assertMix(0,1,100,100);
        assertMix(0,-1,-100,-100);
        assertMix(1,0,100,-100);
        assertMix(-1,0,-100,100);
    }

    @Test public void forwardSteeringIsCorrect(){
        assertMix(0.25,0.75,100,50);
        assertMix(-0.25,0.75,50,100);
    }

    @Test public void reverseSteeringKeepsYawHandedness(){
        // +X means nose/camera turns right even while reversing.
        assertMix(0.25,-0.75,-50,-100);
        assertMix(-0.25,-0.75,-100,-50);
    }

    @Test public void rightAndLeftYawSignsAreConsistentAcrossForwardAndReverse(){
        DriveMixer.Command fr=DriveMixer.mix(0.25,0.75), rr=DriveMixer.mix(0.25,-0.75);
        DriveMixer.Command fl=DriveMixer.mix(-0.25,0.75), rl=DriveMixer.mix(-0.25,-0.75);
        assertTrue(fr.left-fr.right>0);assertTrue(rr.left-rr.right>0);
        assertTrue(fl.left-fl.right<0);assertTrue(rl.left-rl.right<0);
    }

    @Test public void normalizationPreservesFullScaleWithoutClippingCurvature(){
        assertMix(0.5,0.5,100,0);
        assertMix(-0.5,0.5,0,100);
        assertMix(0.5,-0.5,0,-100);
        assertMix(-0.5,-0.5,-100,0);
    }

    @Test public void centralSymmetryIsExact(){
        DriveMixer.Command a=DriveMixer.mix(0.31,0.67), b=DriveMixer.mix(-0.31,-0.67);
        assertEquals(-a.left,b.left);assertEquals(-a.right,b.right);
    }

    @Test public void inputsAreBoundedToUnitCircleCommandDomain(){
        assertMix(2,0,100,-100);
        assertMix(0,2,100,100);
        assertMix(-2,0,-100,100);
        assertMix(0,-2,-100,-100);
    }
}
