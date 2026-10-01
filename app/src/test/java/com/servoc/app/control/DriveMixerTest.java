package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveMixerTest {
    private static void assertMix(double x,double y,int left,int right){
        DriveMixer.Command c=DriveMixer.mix(x,y);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }
    private static void assertTuned(double x,double y,int turn,int pivot,int left,int right){
        DriveMixer.Command c=DriveMixer.mix(x,y,turn,pivot);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }

    @Test public void defaultCardinalTravelAndPivotArePredictable(){
        assertMix(0,0,0,0);
        assertMix(0,1,100,100);
        assertMix(0,-1,-100,-100);
        assertMix(1,0,60,-60);
        assertMix(-1,0,-60,60);
    }

    @Test public void defaultRightSteeringSlowsRightSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(0.5,0.8,80,56);
        assertMix(1.0,0.8,80,32);
    }

    @Test public void defaultLeftSteeringSlowsLeftSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(-0.5,0.8,56,80);
        assertMix(-1.0,0.8,32,80);
    }

    @Test public void defaultFullSteeringLockKeepsSlowedSideAtFortyPercent(){
        DriveMixer.Command right=DriveMixer.mix(1,1);
        DriveMixer.Command left=DriveMixer.mix(-1,1);
        assertEquals(100,right.left);assertEquals(40,right.right);
        assertEquals(40,left.left);assertEquals(100,left.right);
        assertEquals(DriveMixer.MIN_INNER_RATIO,Math.abs(right.right/(double)right.left),0.001);
    }

    @Test public void reverseKeepsSameScreenDirectionRule(){
        assertMix(1.0,-0.8,-80,-32);
        assertMix(-1.0,-0.8,-32,-80);
    }

    @Test public void defaultPureHorizontalInputIsLimitedToSixtyPercentPivot(){
        assertMix(0.5,0,30,-30);
        assertMix(-0.5,0,-30,30);
        assertMix(2,0,60,-60);
    }

    @Test public void operatorTurnPercentControlsInnerSideAtFullLock(){
        assertTuned(1,1,0,60,100,0);
        assertTuned(1,1,55,60,100,55);
        assertTuned(-1,1,75,60,75,100);
        assertTuned(1,-1,25,60,-100,-25);
    }

    @Test public void operatorPivotPercentControlsInPlaceRotation(){
        assertTuned(1,0,40,0,0,0);
        assertTuned(1,0,40,35,35,-35);
        assertTuned(-1,0,40,80,-80,80);
    }

    @Test public void tuningValuesAreClampedToPercentDomain(){
        assertTuned(1,1,-5,150,100,0);
        assertTuned(1,0,40,150,100,-100);
        assertTuned(1,1,150,60,100,100);
    }

    @Test public void inputsAreBoundedToUnitCommandDomain(){
        assertMix(0,2,100,100);
        assertMix(0,-2,-100,-100);
        assertMix(2,2,100,40);
        assertMix(-2,2,40,100);
    }
}
