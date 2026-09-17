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

    @Test public void purePivotUsesCounterRotation(){
        DriveMixer.Command right=DriveMixer.mix(1,0),left=DriveMixer.mix(-1,0);
        assertEquals(100,right.left);assertEquals(-100,right.right);
        assertEquals(-100,left.left);assertEquals(100,left.right);
        assertTrue(right.left*right.right<0);
        assertTrue(left.left*left.right<0);
    }

    @Test public void forwardSteeringSlowsInsidePhysicalSide(){
        // Right turn: RIGHT side becomes half speed; left remains full.
        assertMix(0.25,0.75,100,50);
        // Left turn: LEFT side becomes half speed; right remains full.
        assertMix(-0.25,0.75,50,100);
    }

    @Test public void reverseSteeringUsesSameInsidePhysicalSide(){
        // Same physical-side rule while reversing: right command still slows RIGHT side.
        assertMix(0.25,-0.75,-100,-50);
        assertMix(-0.25,-0.75,-50,-100);
    }

    @Test public void samePhysicalSideIsInsideForwardAndReverse(){
        DriveMixer.Command fr=DriveMixer.mix(0.25,0.75),rr=DriveMixer.mix(0.25,-0.75);
        DriveMixer.Command fl=DriveMixer.mix(-0.25,0.75),rl=DriveMixer.mix(-0.25,-0.75);
        assertTrue(Math.abs(fr.right)<Math.abs(fr.left));
        assertTrue(Math.abs(rr.right)<Math.abs(rr.left));
        assertTrue(Math.abs(fl.left)<Math.abs(fl.right));
        assertTrue(Math.abs(rl.left)<Math.abs(rl.right));
    }

    @Test public void normalizationPreservesFullScaleWithoutClippingCurvature(){
        assertMix(0.5,0.5,100,0);
        assertMix(-0.5,0.5,0,100);
        assertMix(0.5,-0.5,-100,0);
        assertMix(-0.5,-0.5,0,-100);
    }

    @Test public void inputsAreBoundedToUnitCommandDomain(){
        assertMix(2,0,100,-100);
        assertMix(0,2,100,100);
        assertMix(-2,0,-100,100);
        assertMix(0,-2,-100,-100);
    }
}
