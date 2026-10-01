package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveMixerTest {
    private static void assertMix(double x,double y,int left,int right){
        DriveMixer.Command c=DriveMixer.mix(x,y);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }

    @Test public void cardinalTravelAndPivotArePredictable(){
        assertMix(0,0,0,0);
        assertMix(0,1,100,100);
        assertMix(0,-1,-100,-100);
        assertMix(1,0,50,-50);
        assertMix(-1,0,-50,50);
    }

    @Test public void forwardRightSlowsRightSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(0.5,0.8,80,56);
        assertMix(1.0,0.8,80,32);
    }

    @Test public void forwardLeftSlowsLeftSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(-0.5,0.8,56,80);
        assertMix(-1.0,0.8,32,80);
    }

    @Test public void fullSteeringLockKeepsInsideSideAtFortyPercent(){
        DriveMixer.Command right=DriveMixer.mix(1,1);
        DriveMixer.Command left=DriveMixer.mix(-1,1);
        assertEquals(100,right.left);assertEquals(40,right.right);
        assertEquals(40,left.left);assertEquals(100,left.right);
        assertEquals(DriveMixer.MIN_INNER_RATIO,Math.abs(right.right/(double)right.left),0.001);
    }

    @Test public void reverseUsesSameTravelRelativeSteeringRule(){
        assertMix(1.0,-0.8,-80,-32);
        assertMix(-1.0,-0.8,-32,-80);
    }

    @Test public void pureHorizontalInputIsLimitedToFiftyPercentPivot(){
        assertMix(0.5,0,25,-25);
        assertMix(-0.5,0,-25,25);
        assertMix(2,0,50,-50);
    }

    @Test public void inputsAreBoundedToUnitCommandDomain(){
        assertMix(0,2,100,100);
        assertMix(0,-2,-100,-100);
        assertMix(2,2,100,40);
        assertMix(-2,2,40,100);
    }
}
